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
import com.adobe.marketing.mobile.services.AppContextService
import com.adobe.marketing.mobile.services.AppState
import com.adobe.marketing.mobile.services.DeviceInforming
import com.adobe.marketing.mobile.services.ServiceProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class EdgeBridgePropertiesTests {
    private lateinit var extension: EdgeBridgeExtension

    @Mock
    lateinit var mockExtensionApi: ExtensionApi

    @Mock
    private lateinit var mockAppContextService: AppContextService

    @Mock
    private lateinit var mockDeviceInfoService: DeviceInforming

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>

    // Performs pre-test setup before each test case
    // Resets the mocked ExtensionApi instance mockExtensionApi (handles event dispatch)
    // Creates a new instance of the EdgeBridgeExtension using the ExtensionApi mock
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this) // Initialize mocks annotated with @Mock

        // Mock the static method to return mocked ServiceProvider
        mockedStaticServiceProvider = Mockito.mockStatic(ServiceProvider::class.java)
        mockedStaticServiceProvider.`when`<Any> { ServiceProvider.getInstance() }.thenReturn(mockServiceProvider)

        // Set up service provider to return mock services
        whenever(mockServiceProvider.appContextService).thenReturn(mockAppContextService)
        whenever(mockServiceProvider.deviceInfoService).thenReturn(mockDeviceInfoService)

        Mockito.reset(mockExtensionApi)
        extension = EdgeBridgeExtension(mockExtensionApi)
    }

    @After
    fun teardown() {
        mockedStaticServiceProvider.close()
    }

    // region Map modification tests
    // These tests validate the modification of the input map results in the desired format,
    // and that the values are correctly populated, without testing what specific values are set.

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

    // endregion Map modification tests

    // region Properties tests
    // These tests validate that the property values are set correctly based on various possible
    // ServiceProvider values.

    // Validates `AppState` `null` is correctly added to the payload
    @Test
    fun testAnalyticsProperties_withAppStateNull_addsCorrectValue() {
        configureAppContextService(null)

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

    // Validates `AppState.FOREGROUND` is correctly added to the payload
    @Test
    fun testAnalyticsProperties_withAppStateForeground_addsCorrectValue() {
        configureAppContextService(AppState.FOREGROUND)

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

    // Validates `AppState.BACKGROUND` is correctly added to the payload
    @Test
    fun testAnalyticsProperties_withAppStateBackground_addsCorrectValue() {
        configureAppContextService(AppState.BACKGROUND)

        val eventData = mutableMapOf<String, Any>()

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "__adobe" to mapOf(
                "analytics" to mapOf(
                    "cp" to "background",
                    "contextData" to mapOf(
                        "a.AppID" to "null"
                    )
                )
            )
        )
        assertEquals(expectedData, eventData)
    }

    @Test
    fun testAnalyticsProperties_withFullDeviceInfo_addsCorrectValue() {
        configureDeviceInfoService("Test App Name", "1.2.3", "456")

        val eventData = mutableMapOf<String, Any>()

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "__adobe" to mapOf(
                "analytics" to mapOf(
                    "cp" to "foreground",
                    "contextData" to mapOf(
                        "a.AppID" to "Test App Name 1.2.3 (456)"
                    )
                )
            )
        )
        assertEquals(expectedData, eventData)
    }

    @Test
    fun testAnalyticsProperties_withDeviceInfoAppName_addsCorrectValue() {
        configureDeviceInfoService("Test App Name", null, null)

        val eventData = mutableMapOf<String, Any>()

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "__adobe" to mapOf(
                "analytics" to mapOf(
                    "cp" to "foreground",
                    "contextData" to mapOf(
                        "a.AppID" to "Test App Name"
                    )
                )
            )
        )
        assertEquals(expectedData, eventData)
    }

    @Test
    fun testAnalyticsProperties_withDeviceInfoAppVersion_addsCorrectValue() {
        configureDeviceInfoService(null, "1.2.3", null)

        val eventData = mutableMapOf<String, Any>()

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "__adobe" to mapOf(
                "analytics" to mapOf(
                    "cp" to "foreground",
                    "contextData" to mapOf(
                        "a.AppID" to "null 1.2.3"
                    )
                )
            )
        )
        assertEquals(expectedData, eventData)
    }

    @Test
    fun testAnalyticsProperties_withDeviceInfoAppVersionCode_addsCorrectValue() {
        configureDeviceInfoService(null, null, "456")

        val eventData = mutableMapOf<String, Any>()

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "__adobe" to mapOf(
                "analytics" to mapOf(
                    "cp" to "foreground",
                    "contextData" to mapOf(
                        "a.AppID" to "null (456)"
                    )
                )
            )
        )
        assertEquals(expectedData, eventData)
    }

    // endregion Properties tests

    // Private helpers
    private fun configureAppContextService(appState: AppState?) {
        whenever(mockAppContextService.appState).thenReturn(appState)
    }

    private fun configureDeviceInfoService(applicationName: String?, applicationVersion: String?, applicationVersionCode: String?) {
        whenever(mockDeviceInfoService.applicationName).thenReturn(applicationName)
        whenever(mockDeviceInfoService.applicationVersion).thenReturn(applicationVersion)
        whenever(mockDeviceInfoService.applicationVersionCode).thenReturn(applicationVersionCode)
    }
}
