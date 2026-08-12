import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

fun loadDotEnv(file: java.io.File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    return file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val idx = line.indexOf('=')
            val key = line.substring(0, idx).trim()
            val raw = line.substring(idx + 1).trim()
            val value = raw
                .removePrefix("\"")
                .removeSuffix("\"")
                .removePrefix("'")
                .removeSuffix("'")
            key to value
        }
}

fun escapeBuildConfigString(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

val localSecrets = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}
val localEnv = loadDotEnv(rootProject.file(".env"))
val openAiApiKey: String =
    localEnv["OPENAI_API_KEY"]
        ?: localSecrets.getProperty("OPENAI_API_KEY")
        ?: ""
val deepSeekApiKey: String =
    localEnv["DEEPSEEK_API_KEY"]
        ?: localSecrets.getProperty("DEEPSEEK_API_KEY")
        ?: ""

android {
    namespace = "com.waynelinnn.voiceagent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.waynelinnn.voiceagent"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${escapeBuildConfigString(openAiApiKey)}\"",
        )
        buildConfigField(
            "String",
            "DEEPSEEK_API_KEY",
            "\"${escapeBuildConfigString(deepSeekApiKey)}\"",
        )
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    val okhttpVersion = "4.12.0"
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("com.squareup.okhttp3:okhttp-sse:$okhttpVersion")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // Local bilingual STT (SenseVoice via Sherpa-ONNX)
    implementation(files("libs/sherpa-onnx-1.13.5.aar"))
    implementation("org.apache.commons:commons-compress:1.26.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
