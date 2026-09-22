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
