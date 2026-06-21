plugins {
    alias(libs.plugins.android.library)
    id("maven-publish") // Plugin ufficiale per la pubblicazione
}

android {
    namespace = "com.network.adswap_sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Diciamo ad Android di preparare il componente "release"
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// CONFIGURAZIONE JITPACK
publishing {
    publications {
        register<MavenPublication>("release") {
            // Coordinate esatte per GitHub e JitPack
            groupId = "com.github.adswap-network"
            artifactId = "adswap-sdk"
            version = "1.0.10" // Versione aggiornata

            // Avvolgiamo solo l'estrazione del componente nell'afterEvaluate
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}