import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.diffplug.spotless")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlin {
        target("src/*/java/**/*.kt")
        ktlint(BuildConstants.Versions.KTLINT)
        licenseHeader(BuildConstants.ADOBE_LICENSE_HEADER)
    }
}

android {
    namespace = "com.adobe.marketing.mobile.edge.bridge.testapp"

    defaultConfig {
        applicationId = "com.adobe.marketing.mobile.edge.bridge.testapp"
        minSdk = BuildConstants.Versions.MIN_SDK_VERSION
        compileSdk = BuildConstants.Versions.COMPILE_SDK_VERSION
        targetSdk = BuildConstants.Versions.TARGET_SDK_VERSION

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName(BuildConstants.BuildTypes.RELEASE) {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = BuildConstants.Versions.JAVA_SOURCE_COMPATIBILITY
        targetCompatibility = BuildConstants.Versions.JAVA_TARGET_COMPATIBILITY
    }

    kotlinOptions {
        jvmTarget = BuildConstants.Versions.KOTLIN_JVM_TARGET
        languageVersion = BuildConstants.Versions.KOTLIN_LANGUAGE_VERSION
        apiVersion = BuildConstants.Versions.KOTLIN_API_VERSION
    }
}

dependencies {
    implementation(project(":edgebridge"))
    // TODO: Use 3.x versions for testing
    implementation("com.adobe.marketing.mobile:core:2.+")
    implementation("com.adobe.marketing.mobile:assurance:2.+")
    implementation("com.adobe.marketing.mobile:edge:2.+")
    implementation("com.adobe.marketing.mobile:edgeidentity:2.+")

    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
}