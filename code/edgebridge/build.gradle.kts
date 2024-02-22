/*
 * Copyright 2024 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
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