plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "app.tit.reader.novel.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.tit.reader.kmp"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("RELEASE_STORE_FILE")
            if (storeFilePath != null && file(storeFilePath).exists()) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

dependencies {
    implementation(project(":content-parser-core"))
    implementation(project(":novel-parsers"))
    implementation(project(":manga-parsers"))
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    implementation(libs.coil.compose)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
}
