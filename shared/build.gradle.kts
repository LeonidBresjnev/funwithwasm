import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(notation=libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlin.serialization)

}

kotlin {
    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            // put your Multiplatform dependencies here
            // https://mvnrepository.com/artifact/org.jetbrains.compose.material3/material3
            implementation("org.jetbrains.compose.material3:material3:1.9.0")
        implementation("io.github.koalaplot:koalaplot-core:0.9.1")
            implementation("org.apache.commons:commons-math3:3.6.1")
            //
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

