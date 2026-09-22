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
