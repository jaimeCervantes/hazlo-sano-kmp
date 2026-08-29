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

---

## Slice 3b — El pilar se lee como un tablero, no como una lista (2026-08-29)

- **Objetivo:** que Nutrición, Movimiento y Mente usen el mismo lenguaje visual que Sueño e Inicio.
  La queja fue literal: «el único que veo más o menos bien es la sección del home y el sueño».
  Spec nueva: [`catalogo_por_pilar.feature`](../../features/catalogo_por_pilar.feature) (10 escenarios).

### Antes de nada, dos desviaciones de proceso que este slice corrige

1. **El skill de entrega no se había leído.** `AGENTS.md` manda abrir
   `.agents/skills/feature-delivery/SKILL.md` antes de una tarea de comportamiento, y el slice 3 se
   entregó sin hacerlo. La causa es concreta: Claude Code solo descubre skills en `.claude/skills/`,
   y ese directorio no existía. Ahora `.claude/skills/` es una copia de `.agents/skills/`, ignorada
   por git para que no haya dos copias versionadas que se separen — `.agents/skills/` sigue siendo
   la fuente. Se recrea con `cp -r .agents/skills/. .claude/skills/`.
2. **Rama equivocada.** El skill exige `feat/<feature>` por cambio de comportamiento, y los cinco
   commits del catálogo se hicieron en `chore/agent-instructions`, que es de otra cosa. Este slice
   va en `feat/pillar-catalog-design`. Los cinco anteriores se quedan donde están: moverlos ahora
   reescribiría historia ya commiteada para arreglar una etiqueta.

### La deuda de tests de UI, saldada

`runComposeUiTest` llevaba sin configurar desde el slice 6, y la bitácora lo arrastraba como «deuda
transversal» en cada entrada. El skill dice pedir aprobación de tooling **una vez**; se pidió y se
aprobó. Ya no vuelve a preguntarse.

- `compose.uiTest` + `compose.desktop.currentOs` en **`jvmTest`, no en `commonTest`**. Puestos en
  común, los mismos tests corren también contra ChromeHeadless en el target de navegador, donde
  `runComposeUiTest` no compone nada y **fallaron los nueve**. El skill ya lo decía —«Compose UI
  test on the JVM»— y costó una corrida de `:app:shared:check` aprenderlo.
- **Es la primera pantalla del proyecto con cobertura.** Hasta ahora ninguna la tenía, que es cómo
  el render del mapa pudo estar roto tres slices sin que nada fallara.

### Decisiones + porqué

- **Se copió el esqueleto de Sueño, no se inventó uno.** `LazyColumn` con `contentPadding` de
  `top = default` y `bottom = xl`, tarjeta de resumen arriba con margen `gutter`, secciones
  separadas por `Spacer(md)`, cada una con `SectionHeader` a `gutter` y un `LazyRow` que sangra
  hasta el borde con `contentPadding` de `gutter`. Parecerse era el objetivo, no un atajo.
- **`PillarSummaryCard` no conoce el dominio.** Recibe `List<PillarMetric>` con textos ya resueltos,
  en lugar de un tipo del dominio. `SleepSummaryCard` toma un `SleepAnalysis` y por eso `AGENTS.md`
  la señala como deuda; copiar el aspecto no obligaba a copiar el error.
- **Los carruseles reutilizan `HazloProductCard` con ancho fijo.** Una tarjeta nueva para el
  carrusel habría sido el segundo componente casi idéntico que `AGENTS.md` llama fallo de diseño.
  Lo único que el carrusel necesita de verdad es un ancho; lo que sí se le añadió a la tarjeta es un
  `overlineLabel` opcional, resuelto por el llamante para que la tarjeta siga sin leer recursos.
- **La agrupación en secciones es una función pura fuera del Composable.** `catalogSections()` toma
  las publicaciones y `nowEpochMillis`. Qué cuenta como «próximo» es una regla, y una regla dentro
  de un `@Composable` no se puede probar sin levantar una pantalla. El reloj entra por parámetro:
  una función que consulta la hora por su cuenta no se puede situar antes ni después de un evento.
- **Un evento sigue vigente hasta que termina, no hasta que empieza.** Un taller de tres horas al
  que llegas a la segunda sigue en curso; descartarlo al empezar lo escondería justo cuando más
  sirve. Sin hora de fin, el inicio hace de final — igual que hace el sitio.
- **Un evento sin fecha va al final y no al principio.** `sortedBy` habría puesto el nulo delante,
  tapando lo que de verdad ocurre pronto.
- **Una sección sin contenido no se dibuja.** Un encabezado sobre una fila vacía promete contenido
  que no llega, que es peor que no estar.
- **El aviso de «sin conexión» va encima de todo**, incluida la tarjeta de resumen: enterarse de que
  el tablero es viejo después de haberlo leído no sirve de nada.
- **Un evento pasado desaparece de «Próximos eventos» pero sigue en la rejilla.** Dejar de
  anunciarlo no es esconderlo.
- **Las fechas se componen con `formatDate` y `formatClockTime`**, que ya estaban en
  `core/ui/util`. Escribir otro formateador aquí habría dado dos formas de pintar el mismo día en
  la misma app.

### El fallo que el test de UI cazó, y que ningún otro habría cazado

`publication_duration_minutes` estaba escrito como `%d min`. **compose-resources no sustituye un
`%d` ni un `%s` sueltos**: los deja literales en la pantalla, sin lanzar, sin avisar y sin que
ningún test unitario lo note. El test e2e falló al no encontrar «60 min» y ahí salió. Los
argumentos tienen que ser **posicionales** (`%1$d`, `%1$s`).

**`catalog_search_placeholder` tenía el mismo defecto** (`Buscar en %s…`) y se habría publicado
enseñando `%s` literal en el buscador de las cuatro pestañas. No lo cazó su propio test —no lo
tiene— sino arreglar el otro y mirar si había más.

### Archivos tocados

- `.gitignore`, `.claude/skills/` (copia ignorada de `.agents/skills/`)
- `features/catalogo_por_pilar.feature` (nuevo, 10 escenarios)
- shared/commonMain: `feature/catalog/presentation/CatalogSections.kt` (nuevo),
  `PillarCatalogUiState.kt` (ahora lleva `CatalogSections`), `PillarCatalogViewModel.kt` (reloj
  inyectable), `feature/catalog/ui/PillarSummaryCard.kt` (nuevo), `PillarCatalogScreen.kt`
  (reescrito como tablero), `PillarText.kt` (+ `pillarIcon`),
  `core/ui/components/cards/HazloProductCard.kt` (+ `overlineLabel`)
- shared/commonTest: `CatalogSectionsTest` (13, nuevo)
- shared/jvmTest: `feature/catalog/ui/PillarCatalogScreenTest` (9, nuevo — primer test de pantalla
  del proyecto)
- `composeResources/values/strings.xml`: 8 cadenas nuevas y **2 corregidas a posicionales**
- `app/shared/build.gradle.kts`: `compose.uiTest` y `compose.desktop.currentOs` en `jvmTest`

### Comandos y resultados

`.\gradlew.bat :core:jvmTest` · `:app:shared:jvmTest` · `:core:check` · `:app:shared:check` ·
`:app:androidApp:assembleDebug` · `:app:desktopApp:check` · `:app:webApp:check` · `:server:test`

`:core` **92 tests, 0 fallos** (sin cambio). `:app:shared:jvmTest` **194 tests, 0 fallos** (antes
172, +22). Todo lo demás BUILD SUCCESSFUL, `:app:shared:check` incluido — que es el que compila para
los targets nativos y el que destapó lo del navegador.

### Sin cobertura de host (dicho explícitamente)

- `PillarSummaryCard` no tiene test propio: se ejerce a través del tablero, que afirma sus cifras.
- El aspecto —colores, tipografías, márgenes— no lo comprueba nada. Los tests afirman **estructura**
  (qué secciones existen) y **contenido derivado de datos** (cifras, «60 min»), nunca la redacción
  del catálogo de cadenas, que puede cambiar sin que el tablero esté roto.
- **Comprobación manual al instalar:** abrir las cuatro pestañas y comparar con Sueño; con un pilar
  sin eventos, comprobar que no aparece un encabezado suelto; en modo avión, que el aviso sale
  encima del resumen.

### Desviaciones y deuda

- **El pilar de Sueño sigue sin catálogo.** Ahora que existe `PillarCatalogContent`, componerlo bajo
  `SleepScreen` es más barato que antes, pero sigue siendo una decisión de UI sin tomar.
- **`BottomTab` sigue con nombres e iconos en duro**, y `pillarIcon()` los repite. Los dos sitios
  deberían leer de uno solo; se promueve `pillarLabel`/`pillarIcon` a `core/ui/` cuando se pague.
- **`SleepScreen` y `RoutesScreen` ya podrían tener test de pantalla** y no lo tienen. La tooling
  está puesta; es trabajo, no bloqueo.
- **`history_trend_prefix` sigue con `%s` suelto** en `strings.xml`. Hoy no lo usa nadie, así que no
  rompe nada, pero está mal escrito y romperá el día que se use.

**Recap:** Las tres pestañas que se veían como pantallas de relleno ahora son tableros con el mismo
lenguaje que Sueño: resumen con las cifras del pilar arriba, carruseles de próximos eventos,
servicios y lo más cercano, y la rejilla completa debajo. Las secciones vacías no se pintan, un
evento pasado deja de anunciarse sin desaparecer, y el aviso de contenido descargado va antes de
todo lo demás. De paso el proyecto tiene su primer test de pantalla, que en su primera ejecución
cazó dos cadenas mal formateadas que se habrían publicado enseñando `%d` y `%s` literales.

**Próximos pasos (opciones):** (1) decidir dónde va el catálogo de Sueño, que es lo único que le
falta al slice 3; (2) escribir el test de pantalla de `RoutesScreen`, que ya es posible y sigue sin
cobertura; (3) el slice 1, el login, que sigue esperando aprobación y bloquea todo lo que viene
después.

---

## Slice 3c — El pilar de Sueño enseña el catálogo de verdad (2026-08-29)

- **Objetivo:** cerrar lo único que le faltaba al slice 3. Spec: los cuatro escenarios de Sueño en
  [`catalogo_por_pilar.feature`](../../features/catalogo_por_pilar.feature).

### Lo que resultó ser, que no era lo que yo había dicho

En las dos entradas anteriores dejé escrito que a Sueño «le falta decidir dónde poner el catálogo»,
como si fuera una pantalla sin hueco. Al abrirla resultó que **ya tenía uno, y estaba lleno de
mentira**: `HazloExploreProductsSection` alimentada por `content.productsAndServices`, que sale de
`MockSleepRepository` — un «Antifaz de descanso» a 18.0, una «Consulta de sueño» a 45.0 y dos cosas
más, escritas dentro del código con fotos de Unsplash.

Es exactamente la misma ficción que `SeedProducts`, que el slice 3 vino a quitar, sobreviviendo en
la única pestaña que el slice no tocó. Así que esto no fue «añadir el catálogo debajo del análisis»
sino **sustituir el catálogo falso por el real**, que es un cambio bastante mejor: se va más ficción
de la que entra código.

`productsAndServices` se borra de `SleepContent`, de `MockSleepRepository` y de la pantalla. Sólo lo
usaban esos tres sitios.

### Decisiones + porqué

- **Sueño no usa `PillarCatalogScreen` entera, y es la única.** Ya tiene encabezado propio —el
  resumen de la última noche, que es funcionalidad real y no un hueco—, así que meterle además la
  tarjeta de resumen del pilar habría puesto un segundo héroe a mitad de pantalla. Hay un test que
  afirma justo eso: `PillarCatalogTags.SUMMARY` **no existe** dentro del tablero de sueño.
- **Las secciones se extrajeron a `LazyListScope.pillarCatalogSections()`, moviendo y no copiando.**
  Es la regla de promoción de `AGENTS.md` aplicada en cuanto una segunda pantalla las quiso: el
  tablero de pilar las sigue usando desde ahí, no hay dos versiones.
- **Un catálogo que no se puede leer no vacía la pantalla.** `pillarCatalogPlaceholder()` ocupa una
  fila, no la pantalla: el análisis del sueño no depende de la red y seguir viéndolo es lo correcto
  cuando no hay cobertura. En `PillarCatalogScreen`, donde el catálogo **es** la pantalla, el mismo
  estado sí ocupa todo.
- **`SleepDashboardContent` pasa a `internal`** para poder componerlo en un test sin levantar un
  `SleepViewModel` con sus tres dependencias. Mismo seam que `PillarCatalogContent`.
- **El ViewModel del catálogo de sueño se crea en `MainScreen`**, no dentro de `SleepScreen`, para
  que la pantalla siga recibiendo un estado y siga siendo testable.

### Archivos tocados

- core/commonMain: `model/SleepContent.kt` (pierde `productsAndServices`)
- shared/commonMain: `data/repository/MockSleepRepository.kt` (−38 líneas de catálogo inventado),
  `feature/sleep/ui/SleepScreen.kt` (recibe el estado del catálogo; `SleepDashboardContent` internal),
  `feature/catalog/ui/PillarCatalogScreen.kt` (`pillarCatalogSections`, `pillarCatalogPlaceholder` y
  `CatalogStaleNotice` extraídos y hechos públicos), `feature/main/ui/MainScreen.kt` (cableado)
- shared/jvmTest: `feature/sleep/ui/SleepScreenCatalogTest` (4, nuevo)
- `features/catalogo_por_pilar.feature`: 4 escenarios más

### Comandos y resultados

`.\gradlew.bat :core:jvmTest` · `:app:shared:jvmTest` · `:core:check` · `:app:shared:check` ·
`:app:androidApp:assembleDebug` · `:app:desktopApp:check` · `:app:webApp:check` · `:server:test`

`:core` **92 tests, 0 fallos** (sin cambio). `:app:shared:jvmTest` **198 tests, 0 fallos** (antes
194, +4). Todo lo demás BUILD SUCCESSFUL. Los cuatro tests nuevos pasaron a la primera.

### Sin cobertura de host

- El orden vertical —que el catálogo quede **debajo** del análisis y no encima— no lo comprueba
  nada: los tests afirman que ambos existen, no dónde. Comprobación manual al abrir la pestaña.
- **Comprobación manual al instalar:** abrir Sueño con red y ver publicaciones reales donde antes
  estaba el antifaz inventado; en modo avión con caché, que el aviso salga en la parte del catálogo
  y el análisis siga entero; sin caché, que el análisis siga y sólo el catálogo pida conexión.

### Deuda que queda

- **`BottomTab` sigue con nombres e iconos en duro** y `pillarIcon()` los repite. Sin cambios.
- **`RoutesScreen` sigue sin test de pantalla**, ahora que ya se puede.
- **`history_trend_prefix` sigue con `%s` suelto** en `strings.xml`: no lo usa nadie, pero romperá
  el día que se use.
- **`MockHomeRepository` y el resto de `MockSleepRepository`** siguen alimentando el inicio y los
  campeones con datos inventados. No es de este slice, pero ahora que el catálogo es real, son lo
  que queda de ficción en la app.

**Recap:** Los cuatro pilares enseñan ya el catálogo del sitio. Sueño era el que faltaba, y resultó
que no le faltaba sitio sino que el que tenía estaba ocupado por una lista de productos escrita en
el código; ahora enseña lo publicado debajo de su análisis, sin un segundo encabezado, y si no hay
red se queda sin catálogo pero no sin pantalla. El slice 3 queda cerrado.

**Próximos pasos (opciones):** (1) el slice 1, el login, que sigue esperando aprobación y bloquea
los slices 4, 5 y 6; (2) el test de pantalla de `RoutesScreen`, que ya es posible; (3) quitar
`MockHomeRepository` del inicio, que es la ficción que queda.
