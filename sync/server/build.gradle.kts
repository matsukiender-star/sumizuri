plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

// The headless sync server. It is run with its settings in the environment or in a .env file next to it, and the release packs it into a Docker image.
version = "0.1.0"

dependencies {
    implementation(project(":sync:core"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.forwardedHeader)
    implementation(libs.ktor.serialization.json)
    implementation(libs.logback)
    // The database, SQLite or Postgres as the .env picks, through one pool.
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.sqlite.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    // Argon2, for checking the proofs the app sends.
    implementation(libs.bouncycastle)
}

application {
    mainClass = "com.sumizuri.sync.server.MainKt"
}

// The version goes in the jar, where the server reads it to tell the app.
tasks.jar {
    manifest { attributes("Implementation-Version" to project.version) }
}

// A run from Gradle starts in the server's own folder, where a local .env and data folder can sit.
tasks.named<JavaExec>("run") {
    workingDir = projectDir
}
