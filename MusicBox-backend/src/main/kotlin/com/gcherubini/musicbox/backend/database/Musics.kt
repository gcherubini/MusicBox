package com.gcherubini.musicbox.backend.database
import org.jetbrains.exposed.v1.core.Table

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
