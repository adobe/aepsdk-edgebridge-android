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

    // region addAnalyticsProperties tests
    // These tests validate:
    //   1. The modification of the input map results in the desired format and
    //   2. The values are correctly populated (without testing specific value variations)
    // It is implicitly expected that the caller will handle setting up the higher level hierarchy
    // of `__adobe.analytics`.

    // Validates method:
    // 1. Creates expected hierarchy when not present:
    //     1. `__adobe.analytics.contextData`
    // 2. Adds the expected values
    @Test
    fun testAnalyticsProperties_withEmptyMap_addsProperties() {
        val eventData = mutableMapOf<String, Any>()

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "cp" to "foreground",
            "contextData" to mapOf(
                "a.AppID" to "null"
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
            "linkName" to "action name",
            "linkType" to "other"
        )

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "cp" to "foreground",
            "linkName" to "action name",
            "linkType" to "other",
            "contextData" to mapOf(
                "a.AppID" to "null"
            )
        )
        assertEquals(expectedData, eventData)
    }

    // Validates method:
    // 1. Does not overwrite unrelated existing values in the hierarchy when already present:
    //     1. `__adobe.analytics`
    //     2. `__adobe.analytics.contextData`
    // 2. Adds the expected values
    @Test
    fun testAnalyticsProperties_withAdobeAnalytics_andContextData_addsProperties() {
        val eventData = mutableMapOf<String, Any>(
            "linkName" to "action name",
            "linkType" to "other",
            "contextData" to mutableMapOf(
                "key1" to "value1"
            )
        )

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "cp" to "foreground",
            "linkName" to "action name",
            "linkType" to "other",
            "contextData" to mapOf(
                "key1" to "value1",
                "a.AppID" to "null"
            )
        )
        assertEquals(expectedData, eventData)
    }

    // Validates method:
    // 1. Overwrites existing related values in the hierarchy when already present:
    //     1. `__adobe.analytics.cp`
    //     2. `__adobe.analytics.contextData.a.AppID`
    @Test
    fun testAnalyticsProperties_withExistingAnalyticsProperties_overwritesProperties() {
        val eventData = mutableMapOf<String, Any>(
            "cp" to "customValue",
            "contextData" to mutableMapOf(
                "a.AppID" to "customValue"
            )
        )

        extension.addAnalyticsProperties(eventData)

        val expectedData = mapOf(
            "cp" to "foreground",
            "contextData" to mapOf(
                "a.AppID" to "null"
            )
        )
        assertEquals(expectedData, eventData)
    }

    // endregion Map modification tests

    // region Properties tests
    // These tests validate that the property values are set correctly based on various possible
    // ServiceProvider values.

    // region getCustomerPerspective tests

    // Validates `AppState` `null`'s corresponding value is correctly returned
    @Test
    fun testAnalyticsProperties_withAppStateNull_addsCorrectValue() {
        configureAppContextService(null)

        val actual = EdgeBridgeProperties.getCustomerPerspective()

        assertEquals("foreground", actual)
    }

    // Validates `AppState.FOREGROUND`'s corresponding value is correctly returned
    @Test
    fun testAnalyticsProperties_withAppStateForeground_addsCorrectValue() {
        configureAppContextService(AppState.FOREGROUND)

        val actual = EdgeBridgeProperties.getCustomerPerspective()

        assertEquals("foreground", actual)
    }

    // Validates `AppState.BACKGROUND`'s corresponding value is correctly returned
    @Test
    fun testAnalyticsProperties_withAppStateBackground_addsCorrectValue() {
        configureAppContextService(AppState.BACKGROUND)

        val actual = EdgeBridgeProperties.getCustomerPerspective()

        assertEquals("background", actual)
    }

    // endregion getCustomerPerspective tests
    // region getApplicationIdentifier tests

    @Test
    fun testAnalyticsProperties_withFullDeviceInfo_addsCorrectValue() {
        configureDeviceInfoService("Test App Name", "1.2.3", "456")

        val actual = EdgeBridgeProperties.getApplicationIdentifier()

        assertEquals("Test App Name 1.2.3 (456)", actual)
    }

    @Test
    fun testAnalyticsProperties_withDeviceInfoAppName_addsCorrectValue() {
        configureDeviceInfoService("Test App Name", null, null)

        val actual = EdgeBridgeProperties.getApplicationIdentifier()

        assertEquals("Test App Name", actual)
    }

    @Test
    fun testAnalyticsProperties_withDeviceInfoAppVersion_addsCorrectValue() {
        configureDeviceInfoService(null, "1.2.3", null)

        val actual = EdgeBridgeProperties.getApplicationIdentifier()

        assertEquals("null 1.2.3", actual)
    }

    @Test
    fun testAnalyticsProperties_withDeviceInfoAppVersionCode_addsCorrectValue() {
        configureDeviceInfoService(null, null, "456")

        val actual = EdgeBridgeProperties.getApplicationIdentifier()

        assertEquals("null (456)", actual)
    }
    // endregion getApplicationIdentifier tests
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
