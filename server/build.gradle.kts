plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "percentile.project.demo"
version = "1.0.0"
application {
    mainClass.set("percentile.project.demo.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    runtimeOnly(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.server.cors)
    implementation (libs.ktor.contentnegotiation.json)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation("io.ktor:ktor-server-compression-jvm:3.3.1")
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
    implementation("org.apache.commons:commons-math3:3.6.1")
}

ktor {
    fatJar {
        this.archiveFileName.set("fat.jar")
        allowZip64 = true
    }
}

