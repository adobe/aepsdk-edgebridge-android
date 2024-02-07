import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("aep-library")
}

val mavenCoreVersion: String by project
val androidxAnnotationVersion: String by project

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
    implementation("com.adobe.marketing.mobile:core:$mavenCoreVersion")
    implementation("androidx.annotation:annotation:$androidxAnnotationVersion")

    testImplementation(BuildConstants.Dependencies.ANDROIDX_TEST_EXT_JUNIT)
    testImplementation(BuildConstants.Dependencies.MOCKITO_CORE)
    testImplementation(BuildConstants.Dependencies.MOCKITO_INLINE)

    // TODO: Use 3.x versions for testing
    // TODO: change these to set version numbers for reliable testing
    androidTestImplementation("com.adobe.marketing.mobile:edge:2.+")
    androidTestImplementation("com.adobe.marketing.mobile:edgeidentity:2.+")

    androidTestImplementation(BuildConstants.Dependencies.ANDROIDX_TEST_EXT_JUNIT)
    androidTestImplementation(BuildConstants.Dependencies.ESPRESSO_CORE)
    androidTestImplementation("com.fasterxml.jackson.core:jackson-databind:2.12.7")
}