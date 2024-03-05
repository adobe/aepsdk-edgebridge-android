/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.bridge

import com.adobe.marketing.mobile.ExtensionApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EdgeBridgePropertiesTests {
    private lateinit var extension: EdgeBridgeExtension

    @Mock
    lateinit var mockExtensionApi: ExtensionApi

    // Performs pre-test setup before each test case
    // Resets the mocked ExtensionApi instance mockExtensionApi (handles event dispatch)
    // Creates a new instance of the EdgeBridgeExtension using the ExtensionApi mock
    @Before
    fun setup() {
        Mockito.reset(mockExtensionApi)
        extension = EdgeBridgeExtension(mockExtensionApi)
    }

    // Validates method:
    // 1. Creates expected hierarchy when not present:
    //     1. `__adobe.analytics`
    //     2. `__adobe.analytics.contextData`
    // 2. Adds the expected values
    @Test
    fun testAnalyticsProperties_withEmptyMap_addsProperties() {
        val eventData = mutableMapOf<String, Any>()

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "__adobe" to mapOf(
                "analytics" to mapOf(
                    "cp" to "foreground",
                    "contextData" to mapOf(
                        "a.AppID" to "null"
                    )
                )
            )
        )
        assertEquals(expectedData, eventData)
    }

    // Validates method:
    // 1. Does not overwrite existing values in the hierarchy when already present:
    //     1. `__adobe.analytics`
    // 2. Creates expected hierarchy when not present:
    //     1. `__adobe.analytics.contextData`
    // 3. Adds the expected values
    @Test
    fun testAnalyticsProperties_withAdobeAnalytics_noContextData_addsProperties() {
        val eventData = mutableMapOf<String, Any>(
            "__adobe" to mutableMapOf(
                "analytics" to mutableMapOf(
                    "linkName" to "action name",
                    "linkType" to "other"
                )
            )
        )

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "__adobe" to mapOf(
                "analytics" to mapOf(
                    "cp" to "foreground",
                    "linkName" to "action name",
                    "linkType" to "other",
                    "contextData" to mapOf(
                        "a.AppID" to "null"
                    )
                )
            )
        )
        assertEquals(expectedData, eventData)
    }

    // Validates method:
    // 1. Does not overwrite existing values in the hierarchy when already present:
    //     1. `__adobe.analytics`
    //     2. `__adobe.analytics.contextData`
    // 2. Adds the expected values
    @Test
    fun testAnalyticsProperties_withAdobeAnalytics_andContextData_addsProperties() {
        val eventData = mutableMapOf<String, Any>(
            "__adobe" to mutableMapOf(
                "analytics" to mutableMapOf(
                    "linkName" to "action name",
                    "linkType" to "other",
                    "contextData" to mutableMapOf(
                        "key1" to "value1"
                    )
                )
            )
        )

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "__adobe" to mapOf(
                "analytics" to mapOf(
                    "cp" to "foreground",
                    "linkName" to "action name",
                    "linkType" to "other",
                    "contextData" to mapOf(
                        "key1" to "value1",
                        "a.AppID" to "null"
                    )
                )
            )
        )
        assertEquals(expectedData, eventData)
    }

    // Validates method:
    // 1. Does not overwrite existing values in the hierarchy when already present:
    //     1. Values outside `__adobe.analytics`
    // 2. Creates expected hierarchy when not present:
    //     1. `__adobe.analytics`
    //     2. `__adobe.analytics.contextData`
    // 3. Adds the expected values
    @Test
    fun testAnalyticsProperties_withNoAdobeAnalytics_addsProperties() {
        val eventData = mutableMapOf<String, Any>(
            "key1" to "value1"
        )

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "key1" to "value1",
            "__adobe" to mapOf(
                "analytics" to mapOf(
                    "cp" to "foreground",
                    "contextData" to mapOf(
                        "a.AppID" to "null"
                    )
                )
            )
        )
        assertEquals(expectedData, eventData)
    }
}
