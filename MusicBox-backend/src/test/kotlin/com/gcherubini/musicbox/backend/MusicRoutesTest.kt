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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MusicRoutesTest {
    @BeforeTest fun resetFileDb() {
        // Suite shares the file DB at MusicBox-backend/data/musicbox.db (module() seeds
        // only when empty). Wipe it before every test so each test re-seeds from scratch:
        // repeat runs need no manual `data/` deletion and test order doesn't matter.
        java.io.File("data").deleteRecursively()
    }
    @Test fun `GET lista retorna 10 seed`() = testApplication {
        application { module() }
        val r = client.get("/musics")
        assertEquals(HttpStatusCode.OK, r.status)
        val body = r.bodyAsText()
        assertContains(body, "\"id\": \"1\"")
        assertEquals(10, Json.parseToJsonElement(body).jsonArray.size)
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
        val putBad = client.put("/musics/1") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"1","title":"","artist":"A","label":"L","releaseDate":"2024-01-01","genre":"G","coverImageUrl":"C"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, putBad.status)
        val put404 = client.put("/musics/xyz") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"xyz","title":"T","artist":"A","label":"L","releaseDate":"2024-01-01","genre":"G","coverImageUrl":"C"}""")
        }
        assertEquals(HttpStatusCode.NotFound, put404.status)
        assertEquals(HttpStatusCode.NoContent, client.delete("/musics/2").status)
        assertEquals(HttpStatusCode.NotFound, client.delete("/musics/xyz").status)
    }
}
