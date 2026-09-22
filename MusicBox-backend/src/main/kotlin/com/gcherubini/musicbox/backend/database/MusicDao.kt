package com.gcherubini.musicbox.backend.database
import com.gcherubini.musicbox.backend.model.Music
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.core.eq

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
