# Bitácora — Los datos del app viven en el Postgres compartido

Append-only. Entradas nuevas al final.
Roadmap: [`datos-en-postgres.md`](datos-en-postgres.md) · Spec: [`features/datos_en_postgres.feature`](../../features/datos_en_postgres.feature).

---

## Session handoff — 2026-08-17 (se continúa en otra sesión)

**Estado: checkpoint 2 escrito y sin aprobar. No se ha escrito una sola línea de código de la
feature.** Lo único que existe son el roadmap y la spec, ambos commiteados.

### Dónde está cada cosa (rutas absolutas, porque la feature cruza tres repos)

| Repo | Ruta | Qué posee |
|---|---|---|
| App KMP (este) | `C:\Users\S2G52\Desktop\jaimito\dev\HazloSano` | La grabación, SQLDelight local, la UI |
| Web Next.js | `C:\Users\S2G52\personal\DEV\salud-justa\comida-justa` | **NextAuth v5** (Google + Entra ID) y la API de lectura del catálogo |
| Backend FastAPI | `C:\Users\S2G52\Desktop\jaimito\HazloSano\bot-whatsapp\backend` | **Alembic**: las 45 migraciones. Dueño activo del DDL |

Cada uno tiene su propio `AGENTS.md` y sus propias reglas. El del backend exige cargar
`fastapi-bdd-feature` antes de tocar nada; el de este repo exige leer `.agents/skills/*/SKILL.md`.

### Los checkpoints

- **Checkpoint 1 — aprobado.** El usuario eligió, sobre tres preguntas:
  1. Ruta de escritura: **API en el backend FastAPI** (no Supabase directo, no el `server/` Ktor).
  2. Primer slice: **el catálogo se lee del backend** — *esto cambió después, ver abajo*.
  3. Identidad: **el mismo usuario de la web** (NextAuth).
- **Checkpoint 2 — escrito, pendiente de aprobación.** El roadmap y los escenarios están en disco.
  La pregunta abierta al usuario es literalmente "¿apruebo así y arranco el slice 1?".

### Lo que cambió durante la sesión, y por qué

1. **El catálogo dejó de ser el primer slice.** El usuario pidió "un inicio de sesión robusto y
   seguro, con Google y Microsoft", así que el login pasó a ser el slice 1 y el catálogo cayó al 3.
   El catálogo **no depende del login** (es público), así que se puede adelantar cuando convenga.
2. **La ruta del slice del catálogo se corrigió sobre la marcha.** Se propuso construir un endpoint
   en FastAPI y resultó estar **ya construido en el web**: `GET /api/posts/page/{n}/pageSize/{m}?locale=es`.
   Construirlo otra vez habría sido duplicarlo. La decisión de que **las escrituras** vayan por
   FastAPI sigue en pie: Alembic es suyo.
3. **El login se partió en dos slices** (entrar / rotar y revocar) para que el primero sea entregable.

### Hallazgos verificados (no volver a derivarlos)

Todos comprobados leyendo los repos, no supuestos:

- **NextAuth v5 beta** (`next-auth@^5.0.0-beta.20`) con adaptador Drizzle, en
  `comida-justa/src/infra/auth/index.ts`. Proveedores: **Google y Microsoft Entra ID, ya
  configurados**, por variables de entorno.
- **`session.strategy: "database"`**, puesto explícitamente. Por tanto **hoy no existe ningún
  token**: solo una cookie opaca que apunta a `sessions(session_token PK, user_id, expires)`.
  Cualquier credencial para el app hay que crearla.
- **`isAdmin` compara el correo** contra `HAZLO_SANO_ADMIN_EMAILS`, sin roles
  (`comida-justa/src/infra/auth/isAdmin.ts`). Esto encarece cualquier credencial filtrada y es la
  razón de la mitad de las decisiones de seguridad del roadmap.
- **`accounts` mapea `(provider, providerAccountId) → users.id`**, y `users.id` es **TEXT**, no un
  uuid de Supabase. **No hay `auth.users` ni Supabase Auth en ninguna parte**: Supabase aquí es un
  *host de Postgres*, no una plataforma de identidad.
- **API del web**: solo 5 rutas — `auth/[...nextauth]`, `posts/[...pagination]`, `search`,
  `storage/read-url`, `storage/signed-url`. `basePath` de auth sale de `CJ_AUTH_PATH`, así que la
  ruta real puede no ser `/api/auth`.
- **El backend FastAPI no tiene API para el app**: sus endpoints son webhooks de WhatsApp, Telegram
  y Facebook. Lo que sí posee son las migraciones.
- **El catálogo se unificó en `posts`** (`0023_unify_catalog_into_posts`, aditiva). `products` sigue
  viva pero es legacy. Los títulos están en `post_translations` por idioma y las imágenes en
  `post_media` — bastante más rico que el `HazloProduct` plano del app.
- **El esquema no tiene nada de lo que el app graba**: ni sesiones de movimiento, ni puntos GPS, ni
  sueño. `post_routes` (migración `0043`) es 1:1 con una **publicación**, PostGIS
  `geography(LINESTRING)` y trazado simplificado para dibujar: otra cosa.
- **Dos sistemas de migraciones sobre la misma base**: Alembic (45, todo lo reciente) y Drizzle (4,
  antiguas, en `comida-justa/src/infra/dataAccess/db/migrations`). Alembic manda; el web declara en
  Drizzle lo que consulta. **Confirmar con el usuario** — está deducido, no dicho por él.
- **`.env` del backend apunta a Supabase por el pooler.** No se abrió; solo se comprobó que las
  cadenas aparecen y que `.env` está en `.gitignore`.

### Opciones descartadas, para no volver a proponerlas

- **Supabase directo desde el app (PostgREST + RLS):** metería un segundo sistema de identidad al
  lado de NextAuth, con `auth.users.uuid` que reconciliar contra `users.id` TEXT.
- **Que el app guarde el `session_token` del navegador:** no puede obtenerlo (una Custom Tab guarda
  las cookies fuera del alcance de la app, y en WebView **Google rechaza el login** con
  `disallowed_useragent`); y si el web se lo entregara, con `isAdmin` por correo una filtración es
  administrador del sitio.
- **Login nativo de Google:** evita el navegador pero cuesta configuración de OAuth por proveedor y
  por plataforma, y Microsoft exige otro SDK. Con el login por web, añadir Facebook es **cero
  cambios en el app**.
- **Un endpoint de catálogo nuevo en FastAPI:** duplicaría el del web. Queda como remedio si el
  acoplamiento al mapper del web molesta.

### Pendiente del usuario

1. **Aprobar el checkpoint 2** (o mover slices antes de que salga caro).
2. **La URL base del sitio**, y si hay un entorno de pruebas. Es lo único que hace falta para el
   catálogo; para el login no hace falta configuración externa suya.
3. **Confirmar que Alembic manda en el DDL** y Drizzle solo declara.
4. **`assetlinks.json`** en el dominio, con la huella de firma del app, para los App Links. **No
   bloquea**: con PKCE el esquema propio ya es seguro; los App Links son la segunda capa.

### Aviso permanente

Aplicar las migraciones del slice 1 (`app_auth_codes`, `app_sessions`) contra la base compartida es
una acción grave según `AGENTS.md`: **se pregunta antes, siempre**, y no se ejecuta sin respuesta.

### Validación

Ninguna, y no procede: no se ha tocado código. Los ficheros escritos son documentación y una spec.

**Recap:** La feature está encuadrada y planificada, con el login como slice 1 por petición
explícita del usuario, y con su diseño de seguridad escrito pieza por pieza —código de un solo uso
en vez de token en el redirect, PKCE sobre el canje propio, token hasheado en la base, Keystore en
el dispositivo— en vez de resuelto con adjetivos. Los tres repos están inventariados y los hechos que
condicionan el plan, verificados leyéndolos. No hay código porque el checkpoint 2 no está aprobado.

**Próximos pasos (opciones):** (1) aprobar el checkpoint 2 y arrancar el slice 1 por las migraciones
de Alembic y el endpoint de canje; (2) adelantar el catálogo (slice 3), que no depende del login y
solo toca este repo; (3) mover el reparto de slices, que ahora todavía es barato.
