plugins {
    id("aep-library")
}

val mavenCoreVersion: String by project

aepLibrary {
    namespace = "com.adobe.marketing.mobile.edge.bridge"

    publishing {
        gitRepoName = "aepsdk-edgebridge-android"
        addCoreDependency(mavenCoreVersion)
    }

    enableSpotless = true
    enableSpotlessPrettierForJava = true
}

dependencies {
    // TODO: Use 3.x versions for testing
    // TODO: Remove -SNAPSHOT suffix after Core 3.0.0 is published
    implementation("com.adobe.marketing.mobile:core:$mavenCoreVersion-SNAPSHOT")

    // testImplementation dependencies provided by aep-library:
    // MOCKITO_CORE, MOCKITO_INLINE

    // TODO: Use 3.x versions for testing
    // TODO: change these to set version numbers for reliable testing
    androidTestImplementation("com.adobe.marketing.mobile:edge:2.+")
    androidTestImplementation("com.adobe.marketing.mobile:edgeidentity:2.+")

    // androidTestImplementation dependencies provided by aep-library:
    // ANDROIDX_TEST_EXT_JUNIT, ESPRESSO_CORE
    androidTestImplementation("com.fasterxml.jackson.core:jackson-databind:2.12.7")
}