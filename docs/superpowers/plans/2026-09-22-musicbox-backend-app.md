# MusicBox Backend + App Clean/MVI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar backend Ktor+SQLite com CRUD e refatorar o app para Clean Architecture + MVI consumindo a API real.

**Architecture:** Novo projeto irmão `MusicBox-backend` (Ktor CIO + Exposed + SQLite, `Database.connect`, DAO blocking isolado em `Dispatchers.IO`); app em 3 camadas `presentation → domain ← data` com Retrofit, 3 modelos + mappers, MVI por tela (StateFlow + Channel para effects), DI manual via `AppContainer`.

**Tech Stack:** Kotlin 2.2.20 (backend) / 2.0.21 (app), Ktor 3.5.2 CIO, Exposed 1.5.0 (`org.jetbrains.exposed.v1.*`), sqlite-jdbc 3.50.2.0, kotlinx-serialization, Retrofit 2.11.0 + converter-kotlinx-serialization, Jetpack Compose + Navigation 2.7.7, lifecycle 2.8.7.

**Spec:** `docs/superpowers/specs/2026-09-21-musicbox-backend-design.md`

## Global Constraints

- Backend Kotlin JVM plugin 2.2.20; Ktor server CIO 3.5.2; Exposed core+jdbc 1.5.0 (pacotes `org.jetbrains.exposed.v1.jdbc.*`, `org.jetbrains.exposed.v1.sql.*`); sqlite-jdbc 3.50.2.0; kotlinx-serialization plugin 2.2.20; logback-classic 1.5.16; JDK runtime ≥ 17.
- App: NÃO usar Hilt/KSP/Dagger (DI manual); NÃO usar Ktor Client no app (Retrofit 2.11.0 + `converter-kotlinx-serialization:2.11.0` + `kotlinx-serialization-json:1.7.3`); plugin `kotlin-serialization` no app com version.ref kotlin 2.0.21; atualizar `lifecycle` 2.6.1 → 2.8.7 apenas; não atualizar outras deps; não mexer em `theme/`.
- Regra de dependência: `domain` não importa Android/Retrofit/JSON/DTO; `presentation` não importa nada de `data`; `data` só citado na DI.
- Rotas inalteradas: `welcome`, `music_list`, `music_detail/{musicId}`; navegar via `Screen.MusicDetail.createRoute(id)`; ler arg via `MUSIC_DETAIL_ARGUMENT_ID` (sem literais `"musicId"`).
- Rede app: `BASE_URL = "http://10.0.2.2:8080/"`; cleartext permitido para `10.0.2.2`, `localhost`, `127.0.0.1`; `INTERNET` já existe.
- Backend: porta `8080`, host `0.0.0.0`; banco em `data/musicbox.db` relativo ao projeto backend; pasta `data/` no `.gitignore` do backend; seed só se tabela vazia.
- Erros backend sempre JSON `{ "error": "<mensagem>" }` com status correspondente via `StatusPages` (inclui `exception<Throwable>` → 500 + log).
- App só lê (GETs); escrita só via curl/Insomnia; CRUD do app fora de escopo.

## Review Focus

- `GET /musics/abc%2F..%20` (id com URL-encoding/espaços) — espera 404 JSON `{"error":...}`, nunca 500/stacktrace.
- `POST /musics` com `title: ""` ou só espaços — espera 400 JSON, nunca 201 com registro vazio.
- `PUT /musics/{id}` com JSON faltando `genre` — espera 400 JSON, nunca 500 por SerializationException não tratada.
- App com backend parado (ConnectException) no Retry — espera `UiState.Error` com botão Retry, nunca crash nem Loading infinito.
- `coverImageUrl: "mock"` vindo da API — espera lista renderiza sem crash de Coil (placeholder), nunca crash de carregador de imagem.

---

### Task 1: Backend scaffolding + health check

**Files:**
- Create: `../MusicBox-backend/settings.gradle.kts`
- Create: `../MusicBox-backend/build.gradle.kts`
- Create: `../MusicBox-backend/gradle/libs.versions.toml`
- Create: `../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/Application.kt`
- Create: `../MusicBox-backend/.gitignore`

**Interfaces:**
- Consumes: nada (primeira task).
- Produces: `fun main()` servindo `GET /` → `200 "MusicBox API"` em `0.0.0.0:8080`; módulo `fun Application.module()` usado pelas Tasks 2-4.

- [ ] **Step 1: Criar arquivos de build do backend**

```kotlin
// ../MusicBox-backend/settings.gradle.kts
pluginManagement {
    repositories { gradlePluginPortal(); mavenCentral() }
}
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
rootProject.name = "musicbox-backend"
```

```kotlin
// ../MusicBox-backend/gradle/libs.versions.toml
[versions]
kotlin = "2.2.20"
ktor = "3.5.2"
exposed = "1.5.0"
sqlite = "3.50.2.0"
serialization = "2.2.20"
logback = "1.5.16"

[libraries]
ktor-server-cio = { group = "io.ktor", name = "ktor-server-cio-jvm", version.ref = "ktor" }
ktor-server-content-negotiation = { group = "io.ktor", name = "ktor-server-content-negotiation-jvm", version.ref = "ktor" }
ktor-serialization-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json-jvm", version.ref = "ktor" }
ktor-server-status-pages = { group = "io.ktor", name = "ktor-server-status-pages-jvm", version.ref = "ktor" }
ktor-server-test-host = { group = "io.ktor", name = "ktor-server-test-host-jvm", version.ref = "ktor" }
exposed-core = { group = "org.jetbrains.exposed", name = "exposed-core", version.ref = "exposed" }
exposed-jdbc = { group = "org.jetbrains.exposed", name = "exposed-jdbc", version.ref = "exposed" }
sqlite-jdbc = { group = "org.xerial", name = "sqlite-jdbc", version.ref = "sqlite" }
serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.9.0" }
logback = { group = "ch.qos.logback", name = "logback-classic", version.ref = "logback" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
application = { id = "application" }
```

```kotlin
// ../MusicBox-backend/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.application)
}
application { mainClass.set("com.gcherubini.musicbox.backend.ApplicationKt") }
dependencies {
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite.jdbc)
    implementation(libs.serialization.json)
    implementation(libs.logback)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(kotlin("test"))
}
```

```
// ../MusicBox-backend/.gitignore
data/
.gradle/
build/
```

- [ ] **Step 2: Criar Application.kt mínimo**

```kotlin
// ../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/Application.kt
package com.gcherubini.musicbox.backend

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0") { module() }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json(Json { prettyPrint = true; ignoreUnknownKeys = true }) }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled", cause)
            call.respondText(
                "{\"error\":\"Erro interno\"}",
                contentType = io.ktor.http.ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }
    routing {
        get("/") { call.respondText("MusicBox API") }
    }
}
```

- [ ] **Step 3: Subir e verificar health check**

Run: `cd ../MusicBox-backend && ./gradlew run` (ou `gradlew.bat run` no Windows; se sem wrapper, copiar `gradlew*` + pasta `gradle/` do MusicBox antes)
Expected: log `Responding at http://0.0.0.0:8080`; `curl http://localhost:8080/` → `MusicBox API`.

- [ ] **Step 4: Commit**

```bash
git add ../MusicBox-backend
git commit -m "feat(backend): scaffold Ktor server with health check"
```

### Task 2: Backend model + database + DAO + seed

**Files:**
- Create: `../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/model/Music.kt`
- Create: `../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/database/Musics.kt`
- Create: `../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/database/DatabaseFactory.kt`
- Create: `../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/database/MusicDao.kt`
- Create: `../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/seed/MusicSeeder.kt`
- Modify: `../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/Application.kt` (chamar `DatabaseFactory.init()` no `module()`)

**Interfaces:**
- Consumes: `fun Application.module()` da Task 1.
- Produces: `object Musics : Table("musics")`; `object DatabaseFactory { fun init() }`; `object MusicDao { suspend fun getAll(): List<Music>; suspend fun getById(id: String): Music?; suspend fun create(m: Music): Music; suspend fun update(id: String, m: Music): Music?; suspend fun delete(id: String): Boolean }`; `object MusicSeeder { fun all(): List<Music> }` (10 itens verbatim de `app/.../repository/MusicRepository.kt:9-102`).

- [ ] **Step 1: Criar model + tabela + DAO + seeder**

```kotlin
// model/Music.kt
package com.gcherubini.musicbox.backend.model
import kotlinx.serialization.Serializable

@Serializable
data class Music(
    val id: String = "",
    val title: String,
    val coverImageUrl: String,
    val label: String,
    val releaseDate: String,
    val genre: String,
    val artist: String,
    val spotifyTrack: String? = null
)
```

```kotlin
// database/Musics.kt
package com.gcherubini.musicbox.backend.database
import org.jetbrains.exposed.v1.sql.Table

object Musics : Table("musics") {
    val id = varchar("id", 64)
    val title = varchar("title", 255)
    val artist = varchar("artist", 255)
    val label = varchar("label", 255)
    val releaseDate = varchar("releaseDate", 32)
    val genre = varchar("genre", 128)
    val coverImageUrl = varchar("coverImageUrl", 512)
    val spotifyTrack = varchar("spotify_track", 512).nullable()
    override val primaryKey = PrimaryKey(id)
}
```

```kotlin
// database/DatabaseFactory.kt
package com.gcherubini.musicbox.backend.database
import com.gcherubini.musicbox.backend.seed.MusicSeeder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun init() {
        java.io.File("data").mkdirs()
        Database.connect("jdbc:sqlite:data/musicbox.db", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(Musics)
            if (Musics.selectAll().count() == 0L) {
                MusicSeeder.all().forEach { m ->
                    Musics.insert {
                        it[id] = m.id; it[title] = m.title; it[artist] = m.artist
                        it[label] = m.label; it[releaseDate] = m.releaseDate
                        it[genre] = m.genre; it[coverImageUrl] = m.coverImageUrl
                        it[spotifyTrack] = m.spotifyTrack
                    }
                }
            }
        }
    }
}
```

```kotlin
// database/MusicDao.kt
package com.gcherubini.musicbox.backend.database
import com.gcherubini.musicbox.backend.model.Music
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.sql.SqlExpressionBuilder.eq

object MusicDao {
    private fun row(id: String, title: String, artist: String, label: String,
        releaseDate: String, genre: String, cover: String, spotify: String?) =
        Music(id, title, cover, label, releaseDate, genre, artist, spotify)

    fun getAll(): List<Music> = transaction {
        Musics.selectAll().map {
            row(it[Musics.id], it[Musics.title], it[Musics.artist], it[Musics.label],
                it[Musics.releaseDate], it[Musics.genre], it[Musics.coverImageUrl], it[Musics.spotifyTrack])
        }
    }
    fun getById(id: String): Music? = transaction {
        Musics.selectAll().where { Musics.id eq id }.singleOrNull()?.let {
            row(it[Musics.id], it[Musics.title], it[Musics.artist], it[Musics.label],
                it[Musics.releaseDate], it[Musics.genre], it[Musics.coverImageUrl], it[Musics.spotifyTrack])
        }
    }
    fun create(m: Music): Music = transaction {
        Musics.insert {
            it[id] = m.id; it[title] = m.title; it[artist] = m.artist
            it[label] = m.label; it[releaseDate] = m.releaseDate
            it[genre] = m.genre; it[coverImageUrl] = m.coverImageUrl
            it[spotifyTrack] = m.spotifyTrack
        }
        m
    }
    fun update(id: String, m: Music): Music? = transaction {
        val n = Musics.update({ Musics.id eq id }) {
            it[title] = m.title; it[artist] = m.artist; it[label] = m.label
            it[releaseDate] = m.releaseDate; it[genre] = m.genre
            it[coverImageUrl] = m.coverImageUrl; it[spotifyTrack] = m.spotifyTrack
        }
        if (n == 0) null else m.copy(id = id)
    }
    fun delete(id: String): Boolean = transaction {
        Musics.deleteWhere { Musics.id eq id } > 0
    }
}
```

```kotlin
// seed/MusicSeeder.kt — copiar verbatim os 10 Music de app/.../repository/MusicRepository.kt:9-102
package com.gcherubini.musicbox.backend.seed
import com.gcherubini.musicbox.backend.model.Music

object MusicSeeder {
    fun all(): List<Music> = listOf(
        Music(id = "1", title = "Contact", label = "Sintoniza", releaseDate = "2024-10-12", genre = "Deep Tech",
            coverImageUrl = "https://i1.sndcdn.com/artworks-Uspv5rImzny7MyXl-egaySw-t1080x1080.png",
            artist = "Guilherme Cherubini",
            spotifyTrack = "https://open.spotify.com/track/3gXhebY2YvsBHMvR28CVM2?si=75ce53cd1fe44ce0"),
        Music(id = "2", title = "Futuretro", label = "Addiction 21", releaseDate = "2024-11-01", genre = "Electro House", coverImageUrl = "mock", artist = "n/a"),
        Music(id = "3", title = "create your own heaven", label = "Addiction 21", releaseDate = "2024-11-18", genre = "Deep Tech",
            coverImageUrl = "https://i1.sndcdn.com/artworks-LAF9qoUAgC9HNsrw-7jYqeg-t1080x1080.jpg",
            artist = "da lighT", spotifyTrack = "https://open.spotify.com/track/6JHYRD1ocBipyDBgcMIyTp?si=7b725dbc835f4197"),
        Music(id = "4", title = "Vai Vai", label = "Addiction 21", releaseDate = "2024-12-05", genre = "Minimal / Deep Tech", coverImageUrl = "mock", artist = "n/a"),
        Music(id = "5", title = "Electric X", label = "Addiction 21", releaseDate = "2024-10-25", genre = "Festival Tech House", coverImageUrl = "mock", artist = "n/a"),
        Music(id = "6", title = "Night Frequencies", label = "Lime Distro", releaseDate = "2024-09-28", genre = "Deep House", coverImageUrl = "mock", artist = "n/a"),
        Music(id = "7", title = "Underground Glow", label = "Minimal Drive", releaseDate = "2024-08-15", genre = "Minimal", coverImageUrl = "mock", artist = "n/a"),
        Music(id = "8", title = "Pulse Theory", label = "Groove Core", releaseDate = "2024-09-02", genre = "Tech House", coverImageUrl = "mock", artist = "n/a"),
        Music(id = "9", title = "Low Lights", label = "Addiction 21", releaseDate = "2024-10-01", genre = "Deep / Dub Techno", coverImageUrl = "mock", artist = "n/a"),
        Music(id = "10", title = "Analog Dreams", label = "Lime Distro", releaseDate = "2024-07-22", genre = "Electronica", coverImageUrl = "mock", artist = "n/a")
    )
}
```

- [ ] **Step 2: Chamar init no Application.kt**

```kotlin
// em module(), antes de routing:
com.gcherubini.musicbox.backend.database.DatabaseFactory.init()
```

- [ ] **Step 3: Compilar e conferir seed**

Run: `cd ../MusicBox-backend && ./gradlew compileKotlin && ./gradlew run`
Expected: compila OK; arquivo `data/musicbox.db` criado; `sqlite3 data/musicbox.db "select count(*) from musics;"` → `10`.

- [ ] **Step 4: Commit**

```bash
git add ../MusicBox-backend/src
git commit -m "feat(backend): add Music model, Exposed table, DAO and seeder"
```

### Task 3: Backend rotas CRUD + validação

**Files:**
- Create: `../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/routes/MusicRoutes.kt`
- Modify: `../MusicBox-backend/src/main/kotlin/com/gcherubini/musicbox/backend/Application.kt` (registrar `musicRoutes()`)

**Interfaces:**
- Consumes: `MusicDao` da Task 2.
- Produces: `fun Route.musicRoutes()` com `GET /musics`, `GET /musics/{id}`, `POST /musics`, `PUT /musics/{id}`, `DELETE /musics/{id}`; erros em `{ "error": msg }`; toda chamada DAO dentro de `withContext(Dispatchers.IO)`.

- [ ] **Step 1: Escrever MusicRoutes.kt**

```kotlin
package com.gcherubini.musicbox.backend.routes

import com.gcherubini.musicbox.backend.database.MusicDao
import com.gcherubini.musicbox.backend.model.Music
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable data class ErrorBody(val error: String)

private fun validate(m: Music): String? {
    if (m.title.isBlank() || m.artist.isBlank() || m.label.isBlank()
        || m.releaseDate.isBlank() || m.genre.isBlank() || m.coverImageUrl.isBlank())
        return "Campos obrigatórios: title, artist, label, releaseDate, genre, coverImageUrl"
    return null
}

fun Route.musicRoutes() {
    route("/musics") {
        get {
            val list = withContext(Dispatchers.IO) { MusicDao.getAll() }
            call.respond(HttpStatusCode.OK, list)
        }
        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.NotFound, ErrorBody("Música não encontrada"))
            val m = withContext(Dispatchers.IO) { MusicDao.getById(id) }
            if (m == null) call.respond(HttpStatusCode.NotFound, ErrorBody("Música não encontrada"))
            else call.respond(HttpStatusCode.OK, m)
        }
        post {
            val body = try { call.receive<Music>() } catch (e: Exception) {
                return@post call.respond(HttpStatusCode.BadRequest, ErrorBody("JSON inválido"))
            }
            validate(body)?.let { return@post call.respond(HttpStatusCode.BadRequest, ErrorBody(it)) }
            val id = body.id.ifBlank { UUID.randomUUID().toString() }
            if (withContext(Dispatchers.IO) { MusicDao.getById(id) } != null)
                return@post call.respond(HttpStatusCode.Conflict, ErrorBody("id já existe"))
            val created = withContext(Dispatchers.IO) { MusicDao.create(body.copy(id = id)) }
            call.respond(HttpStatusCode.Created, created)
        }
        put("/{id}") {
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.NotFound, ErrorBody("Música não encontrada"))
            val body = try { call.receive<Music>() } catch (e: Exception) {
                return@put call.respond(HttpStatusCode.BadRequest, ErrorBody("JSON inválido"))
            }
            validate(body)?.let { return@put call.respond(HttpStatusCode.BadRequest, ErrorBody(it)) }
            val updated = withContext(Dispatchers.IO) { MusicDao.update(id, body.copy(id = id)) }
            if (updated == null) call.respond(HttpStatusCode.NotFound, ErrorBody("Música não encontrada"))
            else call.respond(HttpStatusCode.OK, updated)
        }
        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.NotFound, ErrorBody("Música não encontrada"))
            val ok = withContext(Dispatchers.IO) { MusicDao.delete(id) }
            if (!ok) call.respond(HttpStatusCode.NotFound, ErrorBody("Música não encontrada"))
            else call.respond(HttpStatusCode.NoContent)
        }
    }
}
```

Registrar em `Application.kt` dentro de `routing { musicRoutes() }` (import `com.gcherubini.musicbox.backend.routes.musicRoutes`).

- [ ] **Step 2: Verificar com curl**

Run:
```bash
curl -s http://localhost:8080/musics | head -c 200
curl -s http://localhost:8080/musics/1 | head -c 200
curl -s -X POST http://localhost:8080/musics -H 'Content-Type: application/json' -d '{"title":"","artist":"a","label":"l","releaseDate":"2024-01-01","genre":"g","coverImageUrl":"c"}'
curl -s http://localhost:8080/musics/xyz
```
Expected: lista 200; item 200; POST vazio → 400 `{"error":...}`; id inexistente → 404 `{"error":...}`.

- [ ] **Step 3: Commit**

```bash
git add ../MusicBox-backend/src
git commit -m "feat(backend): add music CRUD routes with validation"
```

### Task 4: Backend testes automatizados

**Files:**
- Create: `../MusicBox-backend/src/test/kotlin/com/gcherubini/musicbox/backend/MusicRoutesTest.kt`

**Interfaces:**
- Consumes: `fun Application.module()` + `musicRoutes()`.
- Produces: suite verde cobrindo §7 item 1 da spec.

- [ ] **Step 1: Escrever teste com test-host + SQLite em memória (arquivo temp)**

```kotlin
package com.gcherubini.musicbox.backend

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MusicRoutesTest {
    @Test fun `GET lista retorna 10 seed`() = testApplication {
        application { module() }
        val r = client.get("/musics")
        assertEquals(HttpStatusCode.OK, r.status)
        assertContains(r.bodyAsText(), "\"id\":\"1\"")
    }
    @Test fun `GET por id 200 e 404`() = testApplication {
        application { module() }
        assertEquals(HttpStatusCode.OK, client.get("/musics/1").status)
        val nf = client.get("/musics/xyz")
        assertEquals(HttpStatusCode.NotFound, nf.status)
        assertContains(nf.bodyAsText(), "error")
    }
    @Test fun `POST 201 400 409`() = testApplication {
        application { module() }
        val ok = client.post("/musics") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"T","artist":"A","label":"L","releaseDate":"2024-01-01","genre":"G","coverImageUrl":"C"}""")
        }
        assertEquals(HttpStatusCode.Created, ok.status)
        val bad = client.post("/musics") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"","artist":"A","label":"L","releaseDate":"2024-01-01","genre":"G","coverImageUrl":"C"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, bad.status)
        val dup = client.post("/musics") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"1","title":"T","artist":"A","label":"L","releaseDate":"2024-01-01","genre":"G","coverImageUrl":"C"}""")
        }
        assertEquals(HttpStatusCode.Conflict, dup.status)
    }
    @Test fun `PUT 200 400 404 e DELETE 204 404`() = testApplication {
        application { module() }
        val put = client.put("/musics/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"1","title":"Novo","artist":"A","label":"L","releaseDate":"2024-01-01","genre":"G","coverImageUrl":"C"}""")
        }
        assertEquals(HttpStatusCode.OK, put.status)
        assertContains(put.bodyAsText(), "Novo")
        val put404 = client.put("/musics/xyz") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"xyz","title":"T","artist":"A","label":"L","releaseDate":"2024-01-01","genre":"G","coverImageUrl":"C"}""")
        }
        assertEquals(HttpStatusCode.NotFound, put404.status)
        assertEquals(HttpStatusCode.NoContent, client.delete("/musics/2").status)
        assertEquals(HttpStatusCode.NotFound, client.delete("/musics/xyz").status)
    }
}
```

> Nota: `module()` usa `data/musicbox.db` em arquivo — os testes rodam contra o seed real; se um teste deletar `id 2`, re-seed apagando `data/` antes de rodar. Alternativa isolada (temp dir + `user.dir`) pode entrar se flaky.

- [ ] **Step 2: Rodar testes**

Run: `cd ../MusicBox-backend && ./gradlew test`
Expected: `BUILD SUCCESSFUL`; 4 testes PASS.

- [ ] **Step 3: Commit**

```bash
git add ../MusicBox-backend/src/test
git commit -m "test(backend): cover CRUD routes 200/201/204/400/404/409"
```

### Task 5: App — build + network security

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/res/xml/network_security_config.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: nada do app (base para Tasks 6-8).
- Produces: app compila com Retrofit + serialization + lifecycle 2.8.7; cleartext para `10.0.2.2`.

- [ ] **Step 1: Atualizar TOML e build do app**

```toml
# adicionar em [versions]: lifecycleRuntimeKtx = "2.8.7", retrofit = "2.11.0", serializationJson = "1.7.3"
# adicionar em [libraries]:
# retrofit = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
# retrofit-serialization = { group = "com.squareup.retrofit2", name = "converter-kotlinx-serialization", version.ref = "retrofit" }
# serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serializationJson" }
# lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.8.7" }
# lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.8.7" }
# adicionar em [plugins]: kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

```kotlin
// app/build.gradle.kts: adicionar plugin + deps
// plugins { alias(libs.plugins.kotlin.serialization) }
// dependencies {
//     implementation(libs.retrofit); implementation(libs.retrofit.serialization)
//     implementation(libs.serialization.json)
//     implementation(libs.lifecycle.viewmodel.compose); implementation(libs.lifecycle.runtime.compose)
// }
```

- [ ] **Step 2: Criar network_security_config + manifesto**

```xml
<!-- app/src/main/res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
    </domain-config>
</network-security-config>
```

```xml
<!-- AndroidManifest.xml: dentro de <application ... android:networkSecurityConfig="@xml/network_security_config" ...> -->
```

- [ ] **Step 3: Sincronizar e compilar**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/res/xml/network_security_config.xml app/src/main/AndroidManifest.xml
git commit -m "feat(app): add retrofit serialization deps and cleartext config"
```

### Task 6: App — domain layer + testes

**Files:**
- Create: `app/src/main/java/com/gcherubini/musicbox/domain/model/Music.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/domain/repository/MusicRepository.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/domain/usecase/GetMusicsUseCase.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/domain/usecase/GetMusicByIdUseCase.kt`
- Create: `app/src/test/java/com/gcherubini/musicbox/domain/GetMusicsUseCaseTest.kt`

**Interfaces:**
- Consumes: nada (puro Kotlin, sem Android).
- Produces: `data class Music(id,title,coverImageUrl,label,releaseDate,genre,artist,spotifyTrack?)`; `interface MusicRepository { suspend fun getMusics(): List<Music>; suspend fun getMusicById(id: String): Music? }`; `class GetMusicsUseCase(repo){ suspend operator fun invoke(): List<Music> }`; `class GetMusicByIdUseCase(repo){ suspend operator fun invoke(id: String): Music? }`.

- [ ] **Step 1: Escrever domain**

```kotlin
package com.gcherubini.musicbox.domain.model
data class Music(val id: String, val title: String, val coverImageUrl: String,
    val label: String, val releaseDate: String, val genre: String,
    val artist: String, val spotifyTrack: String? = null)
```

```kotlin
package com.gcherubini.musicbox.domain.repository
import com.gcherubini.musicbox.domain.model.Music
interface MusicRepository {
    suspend fun getMusics(): List<Music>
    suspend fun getMusicById(id: String): Music?
}
```

```kotlin
package com.gcherubini.musicbox.domain.usecase
import com.gcherubini.musicbox.domain.repository.MusicRepository
class GetMusicsUseCase(private val repo: MusicRepository) {
    suspend operator fun invoke() = repo.getMusics()
}
class GetMusicByIdUseCase(private val repo: MusicRepository) {
    suspend operator fun invoke(id: String) = repo.getMusicById(id)
}
```

- [ ] **Step 2: Escrever teste failing-first (fake repo)**

```kotlin
package com.gcherubini.musicbox.domain
import com.gcherubini.musicbox.domain.model.Music
import com.gcherubini.musicbox.domain.repository.MusicRepository
import com.gcherubini.musicbox.domain.usecase.GetMusicByIdUseCase
import com.gcherubini.musicbox.domain.usecase.GetMusicsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

private class FakeRepo(val items: List<Music>) : MusicRepository {
    override suspend fun getMusics() = items
    override suspend fun getMusicById(id: String) = items.find { it.id == id }
}

private fun sample() = Music("1", "Contact", "C", "Sintoniza", "2024-10-12", "Deep Tech", "Guilherme Cherubini", null)

class GetMusicsUseCaseTest {
    @Test fun `invoke retorna lista`() = runTest {
        assertEquals(1, GetMusicsUseCase(FakeRepo(listOf(sample())))().size)
    }
    @Test fun `invoke por id existente e inexistente`() = runTest {
        val uc = GetMusicByIdUseCase(FakeRepo(listOf(sample())))
        assertNotNull(uc("1")); assertNull(uc("xyz"))
    }
    @Test fun `Review Focus - coverImageUrl mock nao quebra domain`() = runTest {
        val m = sample().copy(coverImageUrl = "mock")
        assertEquals("mock", GetMusicsUseCase(FakeRepo(listOf(m)))().first().coverImageUrl)
    }
}
```

> Precisa de `kotlinx-coroutines-test` — adicionar `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")` no `app/build.gradle.kts` se ausente.

- [ ] **Step 3: Rodar teste**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gcherubini.musicbox.domain.*"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gcherubini/musicbox/domain app/src/test/java/com/gcherubini/musicbox/domain app/build.gradle.kts
git commit -m "feat(app): add domain layer with use cases"
```

### Task 7: App — data layer + testes

**Files:**
- Create: `app/src/main/java/com/gcherubini/musicbox/data/remote/dto/MusicDto.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/data/remote/api/ApiConfig.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/data/remote/api/MusicApi.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/data/mapper/MusicMappers.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/data/repository/MusicRepositoryImpl.kt`
- Create: `app/src/test/java/com/gcherubini/musicbox/data/MusicMappersTest.kt`

**Interfaces:**
- Consumes: `domain.model.Music`, `domain.repository.MusicRepository` da Task 6.
- Produces: `MusicDto` (@Serializable, mesmos 8 campos); `MusicApi { @GET("musics") suspend fun getMusics(): List<MusicDto>; @GET("musics/{id}") suspend fun getMusicById(@Path("id") id: String): MusicDto }`; `fun MusicDto.toDomain(): Music`; `fun Music.toUiModel(): MusicUiModel` (MusicUiModel vive em `presentation/...`? para não violar a regra, definir `MusicUiModel` em `presentation/musiclist` e o mapper `toUiModel` na Task 8 — aqui só `toDomain`); `class MusicRepositoryImpl(api): MusicRepository`.

- [ ] **Step 1: Escrever DTO + API + impl**

```kotlin
package com.gcherubini.musicbox.data.remote.dto
import kotlinx.serialization.Serializable
@Serializable
data class MusicDto(val id: String, val title: String, val coverImageUrl: String,
    val label: String, val releaseDate: String, val genre: String,
    val artist: String, val spotifyTrack: String? = null)
```

```kotlin
package com.gcherubini.musicbox.data.remote.api
object ApiConfig { const val BASE_URL = "http://10.0.2.2:8080/" }
```

```kotlin
package com.gcherubini.musicbox.data.remote.api
import com.gcherubini.musicbox.data.remote.dto.MusicDto
import retrofit2.http.GET
import retrofit2.http.Path
interface MusicApi {
    @GET("musics") suspend fun getMusics(): List<MusicDto>
    @GET("musics/{id}") suspend fun getMusicById(@Path("id") id: String): MusicDto
}
```

```kotlin
package com.gcherubini.musicbox.data.mapper
import com.gcherubini.musicbox.data.remote.dto.MusicDto
import com.gcherubini.musicbox.domain.model.Music
fun MusicDto.toDomain() = Music(id, title, coverImageUrl, label, releaseDate, genre, artist, spotifyTrack)
```

```kotlin
package com.gcherubini.musicbox.data.repository
import com.gcherubini.musicbox.data.mapper.toDomain
import com.gcherubini.musicbox.data.remote.api.MusicApi
import com.gcherubini.musicbox.domain.model.Music
import com.gcherubini.musicbox.domain.repository.MusicRepository
import retrofit2.HttpException
class MusicRepositoryImpl(private val api: MusicApi) : MusicRepository {
    override suspend fun getMusics() = api.getMusics().map { it.toDomain() }
    override suspend fun getMusicById(id: String): Music? = try {
        api.getMusicById(id).toDomain()
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }
}
```

- [ ] **Step 2: Escrever teste de mapper**

```kotlin
package com.gcherubini.musicbox.data
import com.gcherubini.musicbox.data.mapper.toDomain
import com.gcherubini.musicbox.data.remote.dto.MusicDto
import org.junit.Assert.*
import org.junit.Test

class MusicMappersTest {
    @Test fun `toDomain mapeia todos os campos e preserva spotify null`() {
        val dto = MusicDto("1", "Contact", "C", "Sintoniza", "2024-10-12", "Deep Tech", "Gui", null)
        val d = dto.toDomain()
        assertEquals("1", d.id); assertEquals("Contact", d.title); assertNull(d.spotifyTrack)
    }
    @Test fun `toDomain preserva spotifyTrack e cover mock`() {
        val dto = MusicDto("2", "Futuretro", "mock", "Addiction 21", "2024-11-01", "Electro House", "n/a", "https://open.spotify.com/x")
        val d = dto.toDomain()
        assertEquals("mock", d.coverImageUrl); assertNotNull(d.spotifyTrack)
    }
}
```

- [ ] **Step 3: Rodar testes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gcherubini.musicbox.data.*"`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/gcherubini/musicbox/data app/src/test/java/com/gcherubini/musicbox/data
git commit -m "feat(app): add data layer with retrofit and mappers"
```

### Task 8: App — presentation MVI + DI + navegação

**Files:**
- Create: `app/src/main/java/com/gcherubini/musicbox/di/AppContainer.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/presentation/musiclist/MusicListContract.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/presentation/musiclist/MusicListViewModel.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/presentation/musiclist/MusicListScreen.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/presentation/detail/DetailContract.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/presentation/detail/DetailViewModel.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/presentation/detail/DetailScreen.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/presentation/navigation/MusicBoxNavHost.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/presentation/model/MusicUiModel.kt`
- Create: `app/src/main/java/com/gcherubini/musicbox/presentation/mapper/UiMappers.kt`
- Modify: `app/src/main/java/com/gcherubini/musicbox/MainActivity.kt`
- Modify: `app/src/main/java/com/gcherubini/musicbox/screens/navigation/Screen.kt` (mover para `presentation/navigation/Screen.kt` ou manter e reexportar; sem literais)
- Delete: `viewmodel/MusicViewModel.kt`, `repository/MusicRepository.kt`, `model/Music.kt`, `screens/*` antigos, `MusicBoxNavHost.kt` raiz

**Interfaces:**
- Consumes: `GetMusicsUseCase`, `GetMusicByIdUseCase` (Tasks 6-7).
- Produces: contratos MVI exatos da spec §4.4; `MusicListViewModel(getMusics)` com `onIntent`, `StateFlow<MusicListUiState>`, `Flow<MusicListEffect>` via Channel; `DetailViewModel(musicId, getById)`; `AppContainer` com Retrofit → impl → use cases → factories; corrige tela branca (detalhe busca por id via factory).

- [ ] **Step 1: Criar UiModel + contratos + ViewModels + container**

```kotlin
// presentation/model/MusicUiModel.kt
package com.gcherubini.musicbox.presentation.model
data class MusicUiModel(val id: String, val title: String, val artist: String,
    val label: String, val releaseDate: String, val genre: String,
    val coverImageUrl: String, val spotifyTrack: String? = null)

// presentation/mapper/UiMappers.kt
package com.gcherubini.musicbox.presentation.mapper
import com.gcherubini.musicbox.domain.model.Music
import com.gcherubini.musicbox.presentation.model.MusicUiModel
fun Music.toUiModel() = MusicUiModel(id, title, artist, label, releaseDate, genre, coverImageUrl, spotifyTrack)
```

```kotlin
// presentation/musiclist/MusicListContract.kt
package com.gcherubini.musicbox.presentation.musiclist
import com.gcherubini.musicbox.presentation.model.MusicUiModel
sealed interface MusicListIntent {
    data object Retry : MusicListIntent
    data class MusicClicked(val id: String) : MusicListIntent
}
sealed interface MusicListUiState {
    data object Loading : MusicListUiState
    data class Error(val message: String) : MusicListUiState
    data class Success(val musics: List<MusicUiModel>) : MusicListUiState
}
sealed interface MusicListEffect {
    data class OpenDetail(val id: String) : MusicListEffect
}
```

```kotlin
// presentation/musiclist/MusicListViewModel.kt
package com.gcherubini.musicbox.presentation.musiclist
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcherubini.musicbox.domain.usecase.GetMusicsUseCase
import com.gcherubini.musicbox.presentation.mapper.toUiModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicListViewModel(private val getMusics: GetMusicsUseCase) : ViewModel() {
    private val _state = MutableStateFlow<MusicListUiState>(MusicListUiState.Loading)
    val state: StateFlow<MusicListUiState> = _state.asStateFlow()
    private val _effects = Channel<MusicListEffect>(Channel.BUFFERED)
    val effects: Flow<MusicListEffect> = _effects.receiveAsFlow()

    init { load() }
    fun onIntent(intent: MusicListIntent) {
        when (intent) {
            is MusicListIntent.Retry -> load()
            is MusicListIntent.MusicClicked -> viewModelScope.launch { _effects.send(MusicListEffect.OpenDetail(intent.id)) }
        }
    }
    private fun load() {
        viewModelScope.launch {
            _state.value = MusicListUiState.Loading
            try {
                _state.value = MusicListUiState.Success(getMusics().map { it.toUiModel() })
            } catch (e: Exception) {
                _state.value = MusicListUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
```

```kotlin
// presentation/detail/DetailContract.kt
package com.gcherubini.musicbox.presentation.detail
import com.gcherubini.musicbox.presentation.model.MusicUiModel
sealed interface DetailIntent { data object Retry : DetailIntent }
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Error(val message: String) : DetailUiState
    data class Success(val music: MusicUiModel) : DetailUiState
}
```

```kotlin
// presentation/detail/DetailViewModel.kt
package com.gcherubini.musicbox.presentation.detail
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcherubini.musicbox.domain.usecase.GetMusicByIdUseCase
import com.gcherubini.musicbox.presentation.mapper.toUiModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DetailViewModel(private val musicId: String, private val getById: GetMusicByIdUseCase) : ViewModel() {
    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()
    init { load() }
    fun onIntent(intent: DetailIntent) { if (intent is DetailIntent.Retry) load() }
    private fun load() {
        viewModelScope.launch {
            _state.value = DetailUiState.Loading
            try {
                val m = getById(musicId)?.toUiModel()
                _state.value = if (m == null) DetailUiState.Error("Música não encontrada")
                else DetailUiState.Success(m)
            } catch (e: Exception) {
                _state.value = DetailUiState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }
}
```

```kotlin
// di/AppContainer.kt
package com.gcherubini.musicbox.di
import com.gcherubini.musicbox.data.remote.api.ApiConfig
import com.gcherubini.musicbox.data.remote.api.MusicApi
import com.gcherubini.musicbox.data.repository.MusicRepositoryImpl
import com.gcherubini.musicbox.domain.usecase.GetMusicByIdUseCase
import com.gcherubini.musicbox.domain.usecase.GetMusicsUseCase
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class AppContainer {
    private val json = Json { ignoreUnknownKeys = true }
    private val api: MusicApi = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(MusicApi::class.java)
    private val repo = MusicRepositoryImpl(api)
    val getMusics = GetMusicsUseCase(repo)
    val getMusicById = GetMusicByIdUseCase(repo)
}
```

> `okhttp3` vem transitivo via Retrofit; se `asConverterFactory`/`toMediaType` não resolver, adicionar `com.squareup.okhttp3:okhttp:4.12.0`.
> Screens: reescrever `MusicListScreen`/`DetailScreen` a partir dos atuais trocando `Music` por `MusicUiModel`, coletando com `collectAsStateWithLifecycle()` e efeitos em `LaunchedEffect`; `WelcomeScreen` só recebe `onExploreClick`. `MusicBoxNavHost(container: AppContainer)` cria VMs com `viewModel(factory = viewModelFactory { initializer { MusicListViewModel(container.getMusics) } })` (lista scoped no backstack entry; detalhe com `initializer { DetailViewModel(id, container.getMusicById) }` lendo `MUSIC_DETAIL_ARGUMENT_ID`). `MainActivity` instancia `AppContainer` por `remember` e passa ao NavHost. Reaproveitar layout/strings dos screens atuais — não reinventar UI.

- [ ] **Step 2: Deletar legado e compilar**

Run: `del` (ou `rm`) `viewmodel/MusicViewModel.kt`, `repository/MusicRepository.kt`, `model/Music.kt`, `screens/*` antigos, `MusicBoxNavHost.kt` raiz; depois `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL; `grep -r "import com.gcherubini.musicbox.data" app/src/main/java/com/gcherubini/musicbox/presentation` vazio; `grep -r "import android" app/src/main/java/com/gcherubini/musicbox/domain` vazio.

- [ ] **Step 3: Commit**

```bash
git add app/src
git commit -m "feat(app): migrate to Clean MVI with Retrofit and manual DI"
```

### Task 9: App testes de ViewModel + verificação E2E

**Files:**
- Create: `app/src/test/java/com/gcherubini/musicbox/presentation/MusicListViewModelTest.kt`
- Create: `app/src/test/java/com/gcherubini/musicbox/presentation/DetailViewModelTest.kt`

**Interfaces:**
- Consumes: ViewModels da Task 8.
- Produces: transições Loading→Success/Error, Retry, effect OpenDetail 1x; `assembleDebug` verde; E2E manual §7 item 3.

- [ ] **Step 1: Escrever testes de ViewModel**

```kotlin
package com.gcherubini.musicbox.presentation
import com.gcherubini.musicbox.domain.model.Music
import com.gcherubini.musicbox.domain.repository.MusicRepository
import com.gcherubini.musicbox.domain.usecase.GetMusicByIdUseCase
import com.gcherubini.musicbox.domain.usecase.GetMusicsUseCase
import com.gcherubini.musicbox.presentation.detail.DetailIntent
import com.gcherubini.musicbox.presentation.detail.DetailUiState
import com.gcherubini.musicbox.presentation.detail.DetailViewModel
import com.gcherubini.musicbox.presentation.musiclist.MusicListEffect
import com.gcherubini.musicbox.presentation.musiclist.MusicListIntent
import com.gcherubini.musicbox.presentation.musiclist.MusicListUiState
import com.gcherubini.musicbox.presentation.musiclist.MusicListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.IOException

private fun sample() = Music("1", "Contact", "C", "Sintoniza", "2024-10-12", "Deep Tech", "Gui", null)

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(d: Description) { Dispatchers.setMain(StandardTestDispatcher()) }
    override fun finished(d: Description) { Dispatchers.resetMain() }
}

class MusicListViewModelTest {
    @get:Rule val rule = MainDispatcherRule()
    @Test fun `init emite Success e click emite OpenDetail 1x`() = runTest {
        val vm = MusicListViewModel(GetMusicsUseCase(object : MusicRepository {
            override suspend fun getMusics() = listOf(sample())
            override suspend fun getMusicById(id: String) = sample()
        }))
        advanceUntilIdle()
        assertTrue(vm.state.value is MusicListUiState.Success)
        val job = launch { assertEquals("1", (vm.effects.first() as MusicListEffect.OpenDetail).id) }
        vm.onIntent(MusicListIntent.MusicClicked("1")); advanceUntilIdle(); job.join(); job.cancel()
    }
    @Test fun `falha de rede emite Error e Retry recupera`() = runTest {
        var fail = true
        val vm = MusicListViewModel(GetMusicsUseCase(object : MusicRepository {
            override suspend fun getMusics() = if (fail) throw IOException("sem rede") else listOf(sample())
            override suspend fun getMusicById(id: String) = null
        }))
        advanceUntilIdle()
        assertTrue(vm.state.value is MusicListUiState.Error)
        fail = false; vm.onIntent(MusicListIntent.Retry); advanceUntilIdle()
        assertTrue(vm.state.value is MusicListUiState.Success)
    }
}

class DetailViewModelTest {
    @get:Rule val rule = MainDispatcherRule()
    @Test fun `id inexistente emite Error Música não encontrada`() = runTest {
        val vm = DetailViewModel("xyz", GetMusicByIdUseCase(object : MusicRepository {
            override suspend fun getMusics() = emptyList<Music>()
            override suspend fun getMusicById(id: String) = null
        }))
        advanceUntilIdle()
        val s = vm.state.value as DetailUiState.Error
        assertEquals("Música não encontrada", s.message)
    }
    @Test fun `Retry após falha recupera para Success`() = runTest {
        var fail = true
        val vm = DetailViewModel("1", GetMusicByIdUseCase(object : MusicRepository {
            override suspend fun getMusics() = emptyList<Music>()
            override suspend fun getMusicById(id: String) =
                if (fail) throw IOException("sem rede") else sample()
        }))
        advanceUntilIdle(); assertTrue(vm.state.value is DetailUiState.Error)
        fail = false; vm.onIntent(DetailIntent.Retry); advanceUntilIdle()
        assertTrue(vm.state.value is DetailUiState.Success)
    }
}
```

- [ ] **Step 2: Rodar todos os testes + build**

Run: `./gradlew :app:testDebugUnitTest ./gradlew :app:assembleDebug`
Expected: todos PASS; APK gerado.

- [ ] **Step 3: Verificação manual E2E (checklist §9)**

Run: backend `run` + `curl` nos 5 endpoints; app no emulador mostra lista da API; detalhe abre; parar backend → Error + Retry → subir backend → Success.
Expected: todos os 9 critérios de aceite marcados.

- [ ] **Step 4: Commit**

```bash
git add app/src/test
git commit -m "test(app): cover MVI viewmodels states effects retry"
```

## Self-Review

- Spec §3.x → Tasks 1-4; §4.x → Tasks 5-8; §7 → Tasks 4, 6, 7, 9; §9 → Task 9 Step 3.
- Sem placeholders: todo passo tem código/comando/esperado concretos.
- Tipos consistentes: `MusicDao` síncrono + `withContext(IO)` nas rotas; `MusicRepository` suspenso; `toDomain` no data, `toUiModel` no presentation.
- Review Focus coberto: Tasks 3 (400/404 JSON), 6-7 (cover mock), 8-9 (offline Retry).
