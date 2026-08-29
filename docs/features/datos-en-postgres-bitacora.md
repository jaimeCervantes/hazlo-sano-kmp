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

---

## Slice 3 — El catálogo se lee del sitio, no de un archivo compilado (2026-08-29)

- **Objetivo:** que el catálogo del app sea lo que está publicado en el sitio, y no 144 líneas
  escritas dentro del código. Spec: [`datos_en_postgres.feature`](../../features/datos_en_postgres.feature), etiquetas `@slice-3` (10 escenarios, dos de ellos con tabla de ejemplos).

- **Se adelantó respecto al orden del roadmap**, por decisión del usuario. No depende del login, no
  toca la base compartida y solo toca este repo, así que no bloqueaba nada ni bloqueó nada.

### La auditoría que cambió el slice antes de escribirlo

El usuario avisó de que los otros dos repos se habían movido, así que se releyeron enteros en vez de
confiar en la bitácora del 17 de agosto. Dos hallazgos cambiaron lo que se iba a construir:

1. **El endpoint ganó `?pillar=`** con los cuatro valores `sleep`, `nutrition`, `movement`,
   `mindSpirit` — que son **exactamente los cuatro pilares del app**. No existía al escribir el
   roadmap.
2. **`posts.kind` publica cuatro tipos** y solo dos garantizan precio. El plan decía "el catálogo
   muestra su nombre y su precio", lo que asumía que todo lo tiene.

Con eso encima se preguntó al usuario, porque las lecturas posibles daban trabajos muy distintos.
Eligió **todo el contenido, repartido por pilar**, contra producción.

Después preguntó si se podía mandar la ubicación del teléfono. Se comprobó que **sí, y sin tocar el
web**: el sitio la lee de la cookie `hs_location` en texto plano `lat,lng,ts`. Eligió mandarla por
cookie ahora y dejar anotado el query param como seguimiento, y sacarla de `lastLocation` sin
encender el GPS.

### Decisiones + porqué

- **La caché es por pilar, y se reemplaza entera.** `deleteByPillar` + insert en una transacción.
  Sin el borrado, una publicación retirada del sitio viviría en el dispositivo para siempre, porque
  `INSERT OR REPLACE` solo refresca lo que sigue llegando.
- **El sitio primero, la caché solo si el sitio no contesta.** Es al revés que una caché de
  rendimiento, y a propósito: leer lo local primero dejaría ver precios viejos teniendo cobertura,
  que es justo lo que este slice vino a quitar.
- **Solo la página 1 reemplaza la caché.** Guardar la página 3 borrando lo anterior dejaría al
  dispositivo con un trozo suelto del catálogo y sin su principio.
- **Tres finales distintos, no dos.** `CatalogPage` es `Fresh` / `Cached` / `Unavailable`. Una lista
  vacía porque el pilar está vacío y una lista vacía porque no hubo red **no son lo mismo**, y
  decidirlo mirando si la página trajo filas se equivoca en la última página de un pilar lleno: lo
  que lo decide es `countByPillar`.
- **Lo viejo se enseña diciendo que es viejo.** Sin el aviso, un precio de hace una semana y uno de
  hace un segundo se ven igual, que es la forma silenciosa de mentir.
- **Las filas sembradas no sobreviven la migración.** No eran caché: nada las leyó nunca de ninguna
  parte. Copiarlas habría hecho de "sin red, muestra lo último que leíste" una mentira, porque un
  teléfono que jamás alcanzó el sitio habría enseñado el seed llamándolo catálogo. **Una caché vacía
  es lo que permite decir "todavía no hay catálogo" con honestidad**, que es el criterio de
  aceptación que la migración existe para cumplir.
- **`price` pasa a nulable, y eso obliga a reconstruir la tabla** — SQLite no relaja `NOT NULL`. Un
  `0.0` sería indistinguible de algo que de verdad es gratis.
- **Un tipo desconocido cae en `ANNOUNCEMENT` en vez de descartarse.** `posts.kind` es `text` sin
  `CHECK`, así que el sitio puede publicar un tipo que esta versión no conoce. Esconder esa
  publicación es peor que pintarla sin decoración: el título y la imagen siguen siendo válidos.
- **Una fecha ilegible se descarta sin arrastrar la publicación.** Perder la fecha de un evento
  degrada la tarjeta; perder el evento lo esconde.
- **Cada pestaña de pilar enseña su catálogo**, en lugar de un segundo nivel de pestañas dentro de
  Nutrición. El nav inferior ya eran los cuatro pilares: meter otras cuatro pestañas dentro habría
  sido pintar dos veces la misma idea. De paso **Movimiento y Mente dejan de ser placeholders**.
- **`PillarType` gana `key`** — la clave estable que entiende la API y con la que la UI busca el
  rótulo traducido. `label` se queda marcado como deuda anterior a la regla de i18n.
- **El ViewModel no redacta texto.** `PillarCatalogUiState` es un tipo cerrado; la palabra la elige
  la pantalla del catálogo de cadenas. Es lo contrario de lo que hizo `RoutesViewModel` en el slice
  13, cuya deuda sigue anotada.
- **La ubicación entra por función y no por constructor** del ViewModel: leerla es una suspensión
  que puede dar `null` y cambia entre aperturas; fijarla al construir la congelaría.
- **`lastLocation` y no un fix nuevo.** Para ordenar por cercanía, una posición de hace un rato vale
  igual, y pedir un fix costaría segundos de espera y batería para afinar una distancia que se pinta
  redondeada. Basta con `ACCESS_COARSE_LOCATION`.

### El acoplamiento, declarado

Todo lo que sabe de la forma del sitio vive en **dos archivos**: `CatalogDto.kt` y
`CatalogMapper.kt`. Ese endpoint está modelado para el scroll infinito del web y **no es un contrato
público**. El acoplamiento a la cookie `hs_location` es al nombre y formato de algo interno del web;
**el fallo es suave** — `parseFix` devuelve nulo y el listado sale por fecha, sin romperse.
`ignoreUnknownKeys` está activado, así que el sitio puede añadir campos sin romper esta versión.

**Seguimiento anotado:** endpoint versionado propio con `lat`/`lng` explícitos, que exige un cambio
en `comida-justa`.

### La migración (`5.sqm`, versión 5 → 6)

Reconstruye `ProductEntity` con `price` nulable y añade `kind`, `pillar`, `startsAt`, `endsAt`,
`durationMinutes`, más un índice por `pillar`. `ProductEntity` no la referencia ninguna otra tabla,
así que soltarla no se lleva nada por delante — al revés que el caso de `1.sqm`. La clave foránea a
`SellerEntity` se redeclara como documentación de la relación, no como mecanismo: este proyecto no
activa claves foráneas en ningún driver.

### Archivos tocados

- core/commonMain: `model/PublicationKind.kt`, `model/VisitorLocation.kt`, `model/CatalogPage.kt`,
  `repository/CatalogRepository.kt`, `usecase/GetPillarCatalogUseCase.kt` (nuevos);
  `model/HazloProduct.kt`, `model/PillarType.kt` (ampliados)
- core/commonTest: `PublicationKindTest` (3), `PillarTypeTest` (3), `VisitorLocationTest` (4),
  `GetPillarCatalogUseCaseTest` (4)
- shared/commonMain: `data/catalog/` — `CatalogDto.kt`, `CatalogMapper.kt`, `CatalogApi.kt`,
  `CatalogRepositoryImpl.kt`, `CatalogRepositoryProvider.kt` (nuevos);
  `data/location/VisitorLocationProvider.kt` (expect); `feature/catalog/` — `PillarCatalogUiState`,
  `PillarCatalogViewModel`, `PillarCatalogViewModelFactory`, `PillarCatalogScreen`, `PillarText`
  (nuevos); `data/product/ProductDataSource.kt` y `SqlDelightProductDataSource.kt` (ampliados);
  `core/ui/components/cards/HazloProductCard.kt` y `sections/HazloExploreProductsSection.kt` (precio
  nulable); `App.kt`, `feature/main/ui/MainScreen.kt`
- shared/{android,jvm,js,ios}Main: `data/location/VisitorLocationProvider.<target>.kt` (nuevos; solo
  Android lo implementa)
- shared/commonMain sqldelight: `ProductEntity.sq` (ampliado), `5.sqm` (nuevo)
- shared/commonTest: `CatalogMapperTest` (12), `CatalogApiTest` (8), `CatalogRepositoryImplTest` (8),
  `PillarCatalogViewModelTest` (7)
- shared/jvmTest: `CatalogCacheMigrationTest` (8)
- **Borrados:** `data/product/SeedProducts.kt` (144 líneas), `feature/nutrition/` entera
  (`NutritionViewModel`, `NutritionScreen`), y `PlaceholderScreen` dentro de `MainScreen`
- `gradle/libs.versions.toml`, `build.gradle.kts`, `app/shared/build.gradle.kts`,
  `kotlin-js-store/yarn.lock`
- `composeResources/values/strings.xml`: 16 cadenas nuevas

### Comandos y resultados

`.\gradlew.bat :core:jvmTest` · `:app:shared:jvmTest` · `:core:check` · `:app:shared:check` ·
`:app:androidApp:assembleDebug` · `:app:desktopApp:check` · `:app:webApp:check` · `:server:test`

`:core` **92 tests, 0 fallos** (antes 78, +14). `:app:shared:jvmTest` **172 tests, 0 fallos** (antes
128, +44). Todo lo demás BUILD SUCCESSFUL.

**Dos tropiezos de herramienta, resueltos y anotados porque volverán:**

1. `kotlinStoreYarnLock` falló al añadir `ktor-client-js`: cambia el árbol npm. Se arregló con
   `.\gradlew.bat kotlinUpgradeYarnLock`, que reescribió `kotlin-js-store/yarn.lock` (−9/+4 líneas).
2. `compileTestDevelopmentExecutableKotlinJs` falló una vez con
   `IrClassSymbolImpl is already bound … org.w3c.dom.events/EventListener`. **No era un conflicto de
   dependencias**: era caché incremental obsoleta de Kotlin/JS. Con `--rerun-tasks` compiló y el
   check siguiente pasó limpio. El proyecto pasa `-Xir-incremental-disable`, y **el compilador ahora
   avisa de que esa bandera ya no existe** (`Flag is not supported by this version of the compiler`),
   lo que explica que la caché muerda. Limpiarla es el remedio; quitar la bandera muerta es un
   pendiente aparte.

### Sin cobertura de host (dicho explícitamente)

- `VisitorLocationProvider.android.kt` es código Android y no se prueba en JVM. Lo que sí está
  probado es **qué se manda** cuando hay ubicación y cuando no (`CatalogApiTest`).
- `PillarCatalogScreen` no tiene test: `runComposeUiTest` sigue sin configurarse (deuda transversal
  ya anotada). Todas las decisiones del ViewModel sí están cubiertas.
- `5.sqm` se prueba sobre SQLite en memoria vía JDBC, no sobre `AndroidSqliteDriver`.
- **Comprobación manual al instalar:** abrir las cuatro pestañas de pilar con red y ver publicaciones
  reales; poner el teléfono en modo avión y comprobar que sale lo descargado **con el aviso**;
  desinstalar, reinstalar sin red y comprobar que dice que todavía no hay catálogo.

### Desviaciones y deuda que este slice deja

- **`HazloProduct` se llama `Product` y ya no lo es**: representa los cuatro tipos. El renombrado a
  `HazloPublication` es mecánico, toca 14 archivos y no cabía aquí. Anotado en el propio KDoc.
- **La pestaña Movimiento ahora enseña el catálogo del pilar**, no el tracker. El tracker se sigue
  alcanzando desde Inicio, que es donde estaba. Si al usarlo resulta confuso, es un cambio de
  navegación de una línea.
- **El pilar de Sueño no enseña catálogo todavía.** Su pestaña tiene la pantalla de análisis, que es
  una funcionalidad real y no un placeholder; meterle el catálogo debajo significa componer dos
  ViewModels sin relación en un mismo `LazyColumn`, que es una decisión de UI y no de datos.
  `PillarCatalogScreen(PillarType.SLEEP)` ya funciona: falta decidir dónde ponerlo.
- **`BottomTab` sigue con los cuatro nombres en duro**, ahora que existen `Res.string.pillar_*`.
  `pillarLabel()` se promueve a `core/ui/` en cuanto se pague.
- **`SearchProductsUseCase`, `GetProductsByCategoryUseCase` y `ProductRepositoryImpl` se quedaron sin
  consumidor** al borrar la pantalla de nutrición. No se borran en este slice porque la búsqueda
  local sobre la caché es capacidad que el catálogo querrá; si en dos slices sigue sin llamarlas
  nadie, es código muerto y se va.

**Recap:** El catálogo del app deja de ser un archivo compilado y pasa a ser lo que está publicado en
el sitio, leído por pilar y ordenado por cercanía cuando el teléfono sabe dónde está. Sin red enseña
lo último que descargó **avisando de que es viejo**, y si nunca descargó ese pilar lo dice en vez de
inventarse una lista. Los cuatro tipos de publicación se pintan sin forzarle un precio a los que no
lo tienen, y uno que esta versión no conozca se enseña igual. Movimiento y Mente dejan de ser
pantallas vacías.

**Próximos pasos (opciones):** (1) decidir dónde va el catálogo de Sueño, que es lo único que queda
del slice; (2) el slice 1, el login, que sigue esperando aprobación y es el cuello de botella de
todo lo demás; (3) pagar la deuda de i18n de `BottomTab` y `RoutesViewModel`, que ya son dos.
