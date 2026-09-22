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
