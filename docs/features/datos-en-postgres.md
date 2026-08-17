# Feature: los datos del app viven en el Postgres compartido

Roadmap de slices. Escrito el **2026-08-16**, con el estado de los tres repos a esa fecha.
Spec: [`features/datos_en_postgres.feature`](../../features/datos_en_postgres.feature).

## Problema / Savings / Why

- **Problema:** los datos viven en dos sitios que no se hablan. El catálogo real está en el Postgres
  de Supabase, pero el app enseña una copia **hardcodeada** en `SeedProducts.kt` (144 líneas
  sembradas desde `App.kt:65`). Y lo que el app genera —sesiones, rutas, sueño— solo existe en el
  SQLite de un teléfono: se pierde al desinstalar y no se puede consultar desde fuera. Debajo de las
  dos cosas hay el mismo agujero: **el app no sabe quién eres**.
- **Savings:** dejar de publicar una versión para cambiar un dato; dejar de perder el historial por
  un cambio de teléfono; y poder atar lo grabado a una persona sin inventar un segundo padrón de
  usuarios.
- **Why:** los cuatro pilares prometen ver progreso en el tiempo. Un progreso anónimo y atado a un
  aparato no es progreso: es un archivo temporal.

## Los tres repos y quién manda en qué

| Repo | Qué es | Qué posee |
|---|---|---|
| `dev/HazloSano` (este) | App KMP | La grabación, el almacenamiento local (SQLDelight) y la UI |
| `personal/DEV/salud-justa/comida-justa` | Web Next.js | **NextAuth v5** (Google + Entra ID) y la API de lectura del catálogo |
| `HazloSano/bot-whatsapp/backend` | Backend FastAPI del chatbot | **Alembic**: las 45 migraciones. Es el dueño activo del DDL |

Hechos comprobados que condicionan el plan:

- **Google y Microsoft Entra ID ya están configurados** en el web (`createGoogleProvider`,
  `createMicrosoftEntraIDProvider`), con credenciales por variables de entorno. No hay que dar de
  alta ningún proveedor: el login que se quiere **ya existe**, lo que falta es que el app lo alcance.
- **NextAuth usa `session.strategy: "database"`**: hoy no existe ningún token, solo una cookie opaca
  que apunta a una fila de `sessions`. Cualquier credencial para el app hay que crearla.
- **`accounts` ya guarda `(provider, providerAccountId) → users.id`**: es lo que hace comprobable que
  "la persona del app" y "la persona del web" son la misma fila.
- **`isAdmin` compara el correo** contra `HAZLO_SANO_ADMIN_EMAILS`. No hay roles. Esto sube el precio
  de cualquier credencial filtrada y es la razón principal de varias decisiones de abajo.
- **La lectura del catálogo ya existe**: `GET /api/posts/page/{n}/pageSize/{m}?locale=es`, pública.
- **El catálogo ya no es `products`**: `0023_unify_catalog_into_posts` movió el destino a `posts` +
  `post_translations` + `post_media`. `products` sigue viva pero es la tabla legacy.
- **El esquema no tiene nada de lo que el app graba**: ni sesiones, ni puntos GPS, ni sueño.
- **Dos sistemas de migraciones sobre la misma base**: Alembic (45, todo lo reciente) y Drizzle (4,
  antiguas). Alembic manda; el web declara en Drizzle lo que consulta.

## Decisiones tomadas antes de empezar

1. **SQLDelight no se borra.** Sigue siendo el almacenamiento local; Postgres es el destino. La
   grabación en el monte no puede depender de la red — es la razón de existir del pilar.
2. **Las escrituras van por API, nunca directo a Postgres.** El app jamás ve credenciales de base
   de datos.
3. **Identidad: la misma persona que en el web**, vía NextAuth.
4. **El login del app pasa por el web y el app recibe una credencial propia**, en su propia tabla.
   Al app no se le entrega nunca la cookie de sesión del navegador.
5. **Las tablas nuevas se crean con Alembic**, y se declaran en Drizzle si el web las consulta.

### Por qué el login pasa por el web y no es nativo

El login nativo de Google evita el navegador, pero cuesta una configuración de OAuth **por proveedor
y por plataforma**, y Microsoft exige un SDK aparte. Pasando por el web, los proveedores se
configuran **una sola vez, donde ya lo están**, y el código nativo no cambia nunca: el día que se
añada Facebook es una línea en la config de NextAuth y una app de Facebook, con **cero cambios en el
app**. Con dos proveedores hoy y un tercero previsto, esto decide.

### Por qué no se le da al app la sesión del navegador

Era la opción tentadora: que el app guardara el `session_token` de NextAuth y que el API lo
resolviera contra `sessions`. Cero infraestructura nueva, revocación gratis. Se descartó por dos
motivos:

- **Mecánico:** el app no puede obtener esa cookie. Una Custom Tab guarda las cookies en el proceso
  del navegador, fuera del alcance de la app; y en un WebView, donde sí se leerían, **Google rechaza
  el login** (`disallowed_useragent`). Para que llegara al app, el web tendría que entregarla a
  propósito — que es justo el trabajo que esa opción pretendía ahorrarse.
- **Radio de daño:** con `isAdmin` por correo, un `session_token` filtrado desde el almacenamiento de
  una app no es "alguien ve mis rutas": es **administrador del sitio**. Y ataría FastAPI a las tablas
  internas de `next-auth@5.0.0-beta.20`, que es beta y se mueve.

---

## El diseño del login

Esta es la parte que se pidió robusta, así que va explícita: qué ataque evita cada pieza.

### El flujo

```
app                          navegador (Custom Tab)            web                     base
 │                                                              │                       │
 │ 1. genera verifier (32 B aleatorios)                          │                       │
 │    challenge = SHA256(verifier)                               │                       │
 │──2. abre /auth/app/start?challenge=…&state=… ────────────────▶│                       │
 │                                  3. login NextAuth (Google / Entra) ─────────────────▶│
 │                                                              │ 4. crea código de un   │
 │                                                              │    solo uso (60 s)     │
 │◀─5. redirige a https://<sitio>/app/auth/done?code=…&state=… ─┤                       │
 │                                                              │                       │
 │──6. POST /api/app/session  { code, verifier }  (TLS, directo) ─────────────────────────▶│
 │◀─7. token opaco de 256 bits ──────────────────────────────────────────────────────────┤
 │                                                                                       │
 │ 8. guarda en Keystore / Keychain                                                       │
```

### Qué protege cada decisión

| Decisión | Ataque que cierra |
|---|---|
| **El redirect lleva un código de un solo uso, no el token** | Interceptar el redirect no da acceso: el código sin el `verifier` no vale nada |
| **PKCE sobre nuestro propio canje** (`challenge` al empezar, `verifier` al canjear) | Otra app que registre el mismo destino y robe el redirect no puede completar el canje: nunca vio el `verifier`, y del `challenge` no se vuelve atrás |
| **Código de 60 s y un solo uso**, marcado como usado en la misma transacción | Reutilización del código, y la carrera de dos canjes simultáneos |
| **`state` atado al intento** | Que te inyecten una sesión ajena (login CSRF) |
| **Token opaco de 256 bits de un CSPRNG** | Adivinarlo o derivarlo |
| **En la base se guarda el `SHA-256` del token, no el token** | Un volcado de la base no entrega credenciales usables — misma razón por la que no se guardan contraseñas en claro |
| **Almacenamiento en Android Keystore / iOS Keychain**, no en preferencias planas | Backup del dispositivo, `adb` sobre un build de debug, teléfono robado |
| **El app nunca ve credenciales de Google ni de Microsoft** | Se quedan en el web. El app no puede filtrar lo que no tiene |
| **Caducidad propia, distinta de la del navegador** | Que cerrar el navegador tire la sesión del app a mitad de una salida, y al revés |
| **Rate limit en el canje** | Fuerza bruta sobre el código |
| **TLS del sistema, sin pinning** | Decisión consciente: el pinning convierte una rotación de certificado en un fallo remoto que solo se arregla publicando una versión |

### El destino del redirect: App Links, y qué hacer mientras no los haya

En Android **cualquier app puede registrar `hazlosano://`**, así que un esquema propio es
interceptable. Lo correcto es un **App Link verificado**: una URL `https://` del sitio, validada
contra un `assetlinks.json` publicado en el dominio con la huella de firma del app. El sistema
comprueba la propiedad del dominio y ninguna otra app puede reclamarla.

Eso requiere publicar ese fichero — configuración tuya, pero **en tu propio dominio**, sin consolas
de terceros. Y no bloquea el arranque: **con PKCE, un esquema propio interceptado sigue sin poder
canjear el código**. Así que el plan es empezar con el esquema y añadir los App Links en cuanto el
fichero se pueda publicar, como defensa en profundidad y no como el único muro.

---

## Slices

### Slice 1 — Entrar desde el app con Google o Microsoft

Cruza los tres repos. Es el cuello de botella real: sin identidad no se puede escribir nada.

- **Alembic (backend):**
  - `app_auth_codes(code_hash PK, challenge, user_id FK→users.id, expires_at, used_at)`
  - `app_sessions(token_hash PK, user_id FK→users.id ON DELETE CASCADE, device_label, created_at, expires_at, last_seen_at)`
- **Web:** `/auth/app/start` (arranca el login guardando `challenge` y `state`), el redirect de vuelta
  con el código, y `POST /api/app/session` que canjea código + `verifier` por el token. Declaración
  Drizzle de ambas tablas.
- **App:** genera `verifier`/`challenge`, abre Custom Tab (Android) y `ASWebAuthenticationSession`
  (iOS), recibe el redirect, canjea, guarda en Keystore/Keychain, y una pantalla que dice quién eres.

**Criterios de aceptación**

- Se entra con **Google** y con **Microsoft Entra ID**, y en ambos casos el app queda identificado
  como **el mismo `users.id`** que el web.
- Quien nunca entró al web y entra por primera vez desde el app queda con su fila en `users` y
  `accounts` creada por NextAuth, igual que si hubiera entrado por el navegador.
- Lo que guarda el app **no es** el `session_token` del navegador: valores distintos, caducidades
  distintas.
- Un código canjeado dos veces falla la segunda.
- Un código canjeado con un `verifier` que no corresponde falla.
- Un código de más de 60 segundos falla.
- En la base **no existe el token en claro**: solo su hash.

**Grave — te pregunto antes:** aplicar estas migraciones a la base compartida.

### Slice 2 — La sesión se ve, se renueva y se retira

Lo que convierte "funciona" en "es de fiar". Se separa del slice 1 para que el 1 sea entregable.

- Token de acceso corto + **token de refresco con rotación**: cada refresco invalida el anterior.
- **Detección de reuso**: si aparece un token de refresco ya rotado, se revoca la familia entera —
  es la señal de que alguien copió el almacenamiento del dispositivo.
- Pantalla de dispositivos con sesión abierta y "cerrar sesión aquí" / "en todos".

**Criterios de aceptación**

- Renovar deja el token anterior inservible.
- Presentar un token de refresco ya usado cierra **todas** las sesiones de esa persona.
- Retirar una sesión desde el web deja al app fuera en la siguiente petición, sin tocar la sesión del
  navegador.

### Slice 3 — El catálogo se lee del web, no de un seed

**Solo toca este repo, y no depende del login** — el catálogo es público. Se puede adelantar en
cualquier momento sin bloquear nada; queda aquí porque el login pesa más.

- Un origen remoto lee `GET /api/posts/page/{n}/pageSize/{m}?locale=es`; `ProductEntity` pasa a ser
  **caché** para que el app siga abriendo sin red; se borra `SeedProducts.kt` y su llamada en `App.kt`.
- Dependencias nuevas: `ktor-client-core`, `content-negotiation` y `kotlinx-serialization` en
  `app/shared` commonMain.

**Criterios de aceptación**

- Con red, el catálogo muestra lo publicado en el web, con su nombre y su precio.
- Sin red, muestra lo último que leyó — no una lista vacía y no el seed.
- Sin red y sin haber leído nunca, dice que todavía no hay catálogo en vez de inventar datos.
- Un precio corregido en el web se ve sin publicar una versión.

**Riesgo declarado:** ese endpoint está modelado para el scroll infinito del web
(`mapPostsToCardsForLocale`, ubicación leída de una cookie); **no es un contrato público**. Si el web
cambia el mapper, el app se entera rompiéndose. Mitigación: todo el mapeo vive en un único archivo
del app. Si molesta, el remedio es un endpoint versionado propio.

### Slice 4 — El sueño sube

Primer dato del usuario que viaja. Volumen bajo —una fila por noche— así que prueba la sincronización
sin el problema del volumen encima. Criterio central: **lo local sigue siendo la verdad mientras no
haya confirmación del servidor**; un envío fallido se reintenta, no se pierde ni se duplica.

### Slice 5 — Las salidas de movimiento suben

- ~3.600 puntos por salida de 2 h: se sube **por lotes, al terminar y con red**, nunca durante la
  grabación.
- Decisión abierta para ese momento: PostGIS `geography(LINESTRING)` como `post_routes`, o tabla de
  puntos. La primera es barata de leer y pierde el detalle por punto (precisión, veredicto del
  filtro); la segunda lo conserva y cuesta filas.

### Slice 6 — Las rutas guardadas suben

Reutiliza lo del slice 5. Abre la pregunta interesante: una ruta del app podría publicarse como `post`
con su `post_routes`, que es lo que `cuatro-pilares-vivos.md` del web quiere para
`movimiento_y_ejercicio`. Ahí el app y el web dejan de ser dos productos.

### Slice 7 — Facebook como proveedor `@future`

**Cero cambios en el app.** Config del web y una app de Facebook. Está en el roadmap para dejar
registrado que no cuesta nada del lado nativo — que es la razón por la que el login pasa por el web.

---

## Lo que este roadmap NO hace

- No borra SQLDelight ni convierte a Postgres en la fuente durante una grabación.
- No sincroniza en ambos sentidos: el app escribe y el servidor recibe. Editar una salida desde el
  web no está contemplado.
- No resuelve conflictos entre dos dispositivos grabando a la vez.
- No sube las trazas de diagnóstico (`traces/*.csv`): son de calibración y se sacan a mano.
- No introduce roles. `isAdmin` sigue siendo una lista de correos en una variable de entorno; este
  roadmap lo tiene en cuenta pero no lo cambia.

## Orden y por qué

El login va primero porque es el cuello de botella de todo lo demás y porque es lo que se pidió
robusto. El 2 lo sigue porque un login sin revocación ni rotación es un login a medias. El catálogo
cae al 3 aunque sea el más barato: no depende de nada, así que se puede adelantar el día que
convenga. El sueño antes que las salidas porque prueba la sincronización con una fila por noche en
vez de con 3.600 puntos.
