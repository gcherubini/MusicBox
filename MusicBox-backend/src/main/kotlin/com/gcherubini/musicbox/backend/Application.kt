package com.gcherubini.musicbox.backend

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import com.gcherubini.musicbox.backend.routes.musicRoutes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0") { module() }.start(wait = true)
}

fun Application.module() {
    com.gcherubini.musicbox.backend.database.DatabaseFactory.init()
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
        musicRoutes()
    }
}
