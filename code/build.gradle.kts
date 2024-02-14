apply(plugin = "aep-license")
buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        mavenLocal()
    }
    dependencies {
        classpath("com.github.adobe:aepsdk-commons:884c937705")
    }
}