import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

// What the app and the sync server both use, so the two can never disagree. It holds no screens and nothing of a system beyond the targets.
kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.sumizuri.sync.core"
        compileSdk = libs.versions.android.compileSdk
            .get()
            .toInt()
        minSdk = libs.versions.android.minSdk
            .get()
            .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }
        // Android and the desktop run on Java, so they use the Java crypto classes, with Argon2 from Bouncy Castle.
        jvmMain.dependencies {
            implementation(libs.bouncycastle)
        }
        androidMain.dependencies {
            implementation(libs.bouncycastle)
        }
        // An iPhone uses the cryptography library for AES and HMAC, and its own Argon2 and zlib. The app brings the engine of the library.
        iosMain.dependencies {
            implementation(libs.cryptography.core)
        }
    }
}
