import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

// Pull SUPABASE_URL and SUPABASE_ANON_KEY out of root local.properties.
// The AGP KMP library plugin does not expose buildConfigField, so we generate
// a small Kotlin constants file and wire it into androidMain instead. iOS
// reads the same values from Info.plist (see SupabaseConfig.ios.kt).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val supabaseUrl: String = localProperties.getProperty("SUPABASE_URL", "")
val supabaseAnonKey: String = localProperties.getProperty("SUPABASE_ANON_KEY", "")

val generateSupabaseBuildConfig by tasks.registering {
    // Capture script values as locals — the doLast closure must NOT reference
    // script-level properties or the configuration cache fails to serialize it.
    val urlValue = supabaseUrl
    val keyValue = supabaseAnonKey
    val outDir = layout.buildDirectory.dir("generated/source/supabaseConfig/androidMain")
    inputs.property("supabaseUrl", urlValue)
    inputs.property("supabaseAnonKey", keyValue)
    outputs.dir(outDir)
    doLast {
        val pkgDir = outDir.get().asFile.resolve("jr/brian/inindy/data/remote")
        pkgDir.mkdirs()
        pkgDir.resolve("SupabaseBuildConfig.kt").writeText(
            """
            package jr.brian.inindy.data.remote

            internal object SupabaseBuildConfig {
                const val URL: String = "$urlValue"
                const val ANON_KEY: String = "$keyValue"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin {
    applyDefaultHierarchyTemplate()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    // js and wasmJs targets temporarily disabled — focus on Android + iOS.
    // To re-enable, uncomment these blocks plus the jsMain source-set wiring
    // and dependencies further down.
    // js {
    //     browser()
    // }
    //
    // @OptIn(ExperimentalWasmDsl::class)
    // wasmJs {
    //     browser()
    // }

    android {
       namespace = "jr.brian.inindy.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        // Intermediate source set: all targets except wasmJs.
        // SQLDelight has no wasmJs artifact, so it must live here, not in commonMain.
        val nonWasmCommonMain by creating {
            dependsOn(commonMain.get())
        }
        // Intermediate source set: android + ios only.
        // Libraries that don't publish js/wasmJs artifacts (e.g. adaptive-nav-bar)
        // live here so web targets aren't asked to resolve them.
        val mobileMain by creating {
            dependsOn(nonWasmCommonMain)
        }
        androidMain.get().dependsOn(mobileMain)
        iosMain.get().dependsOn(mobileMain)
        // jsMain.get().dependsOn(nonWasmCommonMain)

        mobileMain.dependencies {
            implementation(libs.adaptive.nav.bar)
        }

        nonWasmCommonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            // Supabase — BOM aligns versions across modules
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.functions)
            implementation(libs.supabase.realtime)
        }

        androidMain {
            kotlin.srcDir(generateSupabaseBuildConfig)
            dependencies {
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)
                implementation(libs.androidx.security.crypto)
                implementation(libs.datastore.preferences.core)
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.messaging)
            }
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.material)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            // Image loading
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            // Logging — Kermit is imported only by jr.brian.inindy.util.AppLog;
            // the rest of the codebase logs via the facade in that file.
            implementation(libs.kermit)
            // Coroutines & Serialization
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            // Navigation
            implementation(libs.navigation.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
            implementation(libs.datastore.preferences.core)
        }
        // jsMain.dependencies {
        //     implementation(libs.wrappers.browser)
        //     implementation(libs.ktor.client.js)
        // }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "jr.brian.inindy.resources"
}

sqldelight {
    databases {
        create("InIndyDatabase") {
            packageName.set("jr.brian.inindy.shared.db")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}