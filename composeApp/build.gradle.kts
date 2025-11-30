import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {

        this.jsMain {
            dependencies {// https://mvnrepository.com/artifact/org.jetbrains.compose.html/html-core
                implementation("org.jetbrains.compose.html:html-core:1.9.3")
            }
        }

        this.webMain {
            dependencies {

            implementation(npm(name="@stdlib/stats-base-dists-beta-pdf", "0.2.2"))
                implementation(npm(name="jstat",version="v1.9.3"))
                implementation("io.github.koalaplot:koalaplot-core:0.9.1")
        }
        }
        this.wasmJsMain {
            dependencies {
                implementation(libs.kotlinx.browser)

                implementation("org.jetbrains.kotlinx:kotlinx-browser-wasm-js:0.5.0")
            }
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content)
            implementation(libs.ktor.serialization.kotlinx.json)

            //implementation("io.github.koalaplot:koalaplot-core:0.9.1")


            // implementation("io.ktor:ktor-client-js:3.3.0")
         /*   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("com.patrykandpatrick.vico:core:2.2.1")
            implementation("com.patrykandpatrick.vico:compose:2.2.1")
            implementation("com.patrykandpatrick.vico:views:2.2.1")
            implementation("com.patrykandpatrick.vico:compose-m2:2.2.1")
            implementation("com.patrykandpatrick.vico:compose-m3:2.2.1")*/

            /*
            implementation("org.jetbrains.lets-plot:lets-plot-kotlin-jvm:4.11.1")
            runtimeOnly("org.jetbrains.lets-plot:lets-plot-batik:4.7.2")
            implementation("org.jetbrains.lets-plot:base-portable-js:3.2.0")*/
            implementation(compose.materialIconsExtended) // Add this line

        }




        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}


