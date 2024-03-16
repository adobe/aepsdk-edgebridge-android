/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.bridge

import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.ExtensionEventListener
import com.adobe.marketing.mobile.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class EdgeBridgeExtensionTests {
    private lateinit var extension: EdgeBridgeExtension

    @Mock
    lateinit var mockExtensionApi: ExtensionApi

    // Performs pre-test setup before each test case
    // Resets the mocked ExtensionApi instance mockExtensionApi (handles event dispatch)
    // Creates a new instance of the EdgeBridgeExtension using the ExtensionApi mock
    @Before
    fun setup() {
        reset(mockExtensionApi)
        extension = EdgeBridgeExtension(mockExtensionApi)
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    fun test_listenerRegistration() {
        extension.onRegistered()
        val eventTypeCaptor = ArgumentCaptor.forClass(String::class.java)
        val eventSourceCaptor = ArgumentCaptor.forClass(String::class.java)
        val extensionEventListenerArgumentCaptor = ArgumentCaptor.forClass(
            ExtensionEventListener::class.java
        )
        // Verify: 2 event listeners registered; capture values from registration
        verify(mockExtensionApi, times(2))
            .registerEventListener(
                eventTypeCaptor.capture(),
                eventSourceCaptor.capture(),
                extensionEventListenerArgumentCaptor.capture()
            )

        // Extract captured values into lists
        val eventTypes = eventTypeCaptor.allValues
        val eventSources = eventSourceCaptor.allValues
        val extensionEventListenerList = extensionEventListenerArgumentCaptor.allValues

        // Verify: 1st Edge Bridge event listener
        assertEquals(EventType.GENERIC_TRACK, eventTypes[0])
        assertEquals(EventSource.REQUEST_CONTENT, eventSources[0])
        assertNotNull(extensionEventListenerList[0])

        // Verify: 2nd Edge Bridge event listener
        assertEquals(EventType.RULES_ENGINE, eventTypes[1])
        assertEquals(EventSource.RESPONSE_CONTENT, eventSources[1])
        assertNotNull(extensionEventListenerList[1])
    }

    // ========================================================================================
    // getName
    // ========================================================================================
    @Test
    fun test_getName() {
        // Test
        val extensionName = extension.name
        assertEquals(
            "getName should return the correct extension name",
            EdgeBridgeTestConstants.EXTENSION_NAME,
            extensionName
        )
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    fun test_getVersion() {
        // Test
        val extensionVersion = extension.version
        assertEquals(
            "getVersion should return the correct extension version",
            EdgeBridgeConstants.EXTENSION_VERSION,
            extensionVersion
        )
    }

    // ========================================================================================
    // getFriendlyName
    // ========================================================================================
    @Test
    fun test_getFriendlyName() {
        // Test
        val extensionFriendlyName = extension.friendlyName
        assertEquals(
            "getFriendlyName should return the correct extension friendly name",
            EdgeBridgeTestConstants.EXTENSION_FRIENDLY_NAME,
            extensionFriendlyName
        )
    }

    // ========================================================================================
    // handleTrackRequest
    // ========================================================================================
    @Test
    fun testHandleTrackEvent_withActionField_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf("action" to "action name")
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "linkName" to "action name",
                        "linkType" to "other",
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withActionFieldWithEmptyValue_dispatchesEdgeRequestEvent_withoutAction() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "action" to "",
                    "contextdata" to mapOf("&&c1" to "propValue1")
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "c1" to "propValue1",
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withActionFieldWithNullValue_dispatchesEdgeRequestEvent_withoutAction() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "action" to null,
                    "contextdata" to mapOf("&&c1" to "propValue1")
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "c1" to "propValue1",
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withStateField_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf("state" to "state name")
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "pageName" to "state name",
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withStateFieldWithEmptyValue_dispatchesEdgeRequestEvent_withoutState() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "state" to "",
                    "contextdata" to mapOf("&&c1" to "propValue1")
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "c1" to "propValue1",
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withStateFieldWithNullValue_dispatchesEdgeRequestEvent_withoutState() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "state" to null,
                    "contextdata" to mapOf("&&c1" to "propValue1")
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "c1" to "propValue1",
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withContextDataFieldUsingReservedPrefix_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(mapOf("contextdata" to mapOf("&&c1" to "propValue1")))
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "c1" to "propValue1",
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withContextDataNotUsingReservedPrefix_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(mapOf("contextdata" to mapOf("key1" to "value1")))
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "key1" to "value1",
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withEmptyContextData_doesNotDispatchEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(mapOf("contextdata" to emptyMap<String, Any>()))
            .build()

        extension.handleTrackRequest(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleTrackEvent_withNullContextData_doesNotDispatchEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(mapOf("contextdata" to null))
            .build()

        extension.handleTrackRequest(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleTrackEvent_withUnexpectedContextDataType_doesNotDispatchEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(mapOf("contextdata" to 123))
            .build()

        extension.handleTrackRequest(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleTrackEvent_withDataField_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(mapOf("key2" to "value2"))
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf("key2" to "value2"),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_trackAction_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "action" to "action name",
                    "key2" to "value2",
                    "contextdata" to mapOf(
                        "&&events" to "event1,event2,event3,event4,event12,event13",
                        "&&products" to ";product1;1;5.99;event12=5.99;evar5=merchEvar5,;product2;2;10.99;event13=6;eVar6=mercheVar6",
                        "&&c1" to "propValue1",
                        "&&cc" to "USD",
                        "key1" to "value1"
                    )
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "linkName" to "action name",
                        "linkType" to "other",
                        "events" to "event1,event2,event3,event4,event12,event13",
                        "products" to ";product1;1;5.99;event12=5.99;evar5=merchEvar5,;product2;2;10.99;event13=6;eVar6=mercheVar6",
                        "c1" to "propValue1",
                        "cc" to "USD",
                        "contextData" to mapOf(
                            "key1" to "value1",
                            "a.AppID" to "null"
                        ),
                        "cp" to "foreground",
                    )
                ),
                "key2" to "value2"
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_trackState_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "state" to "state name",
                    "key2" to "value2",
                    "contextdata" to mapOf(
                        "&&events" to "event1,event2,event3,event4,event12,event13",
                        "&&c1" to "propValue1",
                        "&&v1" to "evarValue1",
                        "key1" to "value1"
                    )
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "pageName" to "state name",
                        "events" to "event1,event2,event3,event4,event12,event13",
                        "c1" to "propValue1",
                        "v1" to "evarValue1",
                        "contextData" to mapOf(
                            "key1" to "value1",
                            "a.AppID" to "null"
                        ),
                        "cp" to "foreground"
                    )
                ),
                "key2" to "value2"
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    // Tests event is not dispatched if no track data is available after filtering invalid state and action
    @Test
    fun testHandleTrackEvent_withNoMappedData_emptyString_doesNotDispatchEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(mapOf("state" to "", "action" to ""))
            .build()

        extension.handleTrackRequest(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    // Tests event is not dispatched if no track data is available after filtering invalid state and action
    // Although public track APIs should not allow for this case, this catches cases where track events
    // are dispatched outside of the public APIs.
    @Test
    fun testHandleTrackEvent_withNoMappedData_nullValues_doesNotDispatchEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(mapOf("state" to null, "action" to null))
            .build()

        extension.handleTrackRequest(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleTrackEvent_withContextDataFieldUsingReservedPrefix_emptyKeyName_dispatchesEdgeRequestEvent_emptyKeysIgnored() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "contextdata" to mapOf(
                        "&&c1" to "propValue",
                        "&&" to "emptyKey"
                    )
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "c1" to "propValue",
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withReservedPrefix_onlyRemovesPrefix_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "contextdata" to mapOf(
                        "&&" to "value1",
                        "&&&" to "value2",
                        "&&&&" to "value3",
                        "&&1" to "value4",
                        "&&a" to "value5",
                        "&& " to "value6",
                        "&&-" to "value7",
                        "&&=" to "value8",
                        "&&\\" to "value9",
                        "&&." to "value10",
                        "&&?" to "value11",
                        "&&\n" to "value12"
                    )
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "&" to "value2",
                        "&&" to "value3",
                        "1" to "value4",
                        "a" to "value5",
                        "-" to "value7",
                        "=" to "value8",
                        "\\" to "value9",
                        "." to "value10",
                        "?" to "value11",
                        "cp" to "foreground",
                        "contextData" to mapOf(
                            "a.AppID" to "null"
                        )
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withCharacterBeforeReservedCharacters_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "contextdata" to mapOf(
                        "1&&" to "value1",
                        "a&&" to "value2",
                        " &&" to "value3",
                        "-&&" to "value4",
                        "=&&" to "value5",
                        "\\&&" to "value6",
                        ".&&" to "value7",
                        "?&&" to "value8",
                        "\n&&" to "value9"
                    )
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "contextData" to mapOf(
                            "1&&" to "value1",
                            "a&&" to "value2",
                            " &&" to "value3",
                            "-&&" to "value4",
                            "=&&" to "value5",
                            "\\&&" to "value6",
                            ".&&" to "value7",
                            "?&&" to "value8",
                            "\n&&" to "value9",
                            "a.AppID" to "null"
                        ),
                        "cp" to "foreground"
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_mapsNullAndEmptyValues_andClearsNullValues_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(
                mapOf(
                    "key3" to "",
                    "key4" to null,
                    "contextdata" to mapOf(
                        "&&key1" to "",
                        "&&key2" to null,
                        "key5" to "",
                        "key6" to null
                    )
                )
            )
            .build()

        extension.handleTrackRequest(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "key3" to "",
                "key4" to null,
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "key1" to "",
                        "contextData" to mapOf(
                            "key5" to "",
                            "a.AppID" to "null"
                        ),
                        "cp" to "foreground"
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleTrackEvent_withNullEventData_doesNotDispatchEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(null)
            .build()

        extension.handleTrackRequest(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleTrackEvent_withEmptyEventData_doesNotDispatchEvent() {
        val event = Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
            .setEventData(emptyMap<String, Any>())
            .build()

        extension.handleTrackRequest(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    // ========================================================================================
    // handleRulesEngineResponse
    // ========================================================================================

    @Test
    fun testHandleRulesEngineResponse_withTrackEvent_dispatchesEdgeRequestEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "type" to "an",
                        "id" to "some value",
                        "detail" to mapOf(
                            "action" to "Test Action",
                            "contextdata" to mapOf("testKey" to "testValue")
                        )
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        val responseEvent = captureAndAssertDispatchedEvent()

        val expectedData = mapOf(
            "data" to mapOf(
                "__adobe" to mapOf(
                    "analytics" to mapOf(
                        "linkName" to "Test Action",
                        "linkType" to "other",
                        "contextData" to mapOf(
                            "testKey" to "testValue",
                            "a.AppID" to "null"
                        ),
                        "cp" to "foreground"
                    )
                )
            ),
            "xdm" to mapOf(
                "eventType" to EdgeBridgeTestConstants.JsonValues.EVENT_TYPE,
                "timestamp" to TimeUtils.getISO8601UTCDateWithMilliseconds(Date(event.timestamp))
            )
        )

        assertEquals(expectedData, responseEvent.eventData)
        assertEquals(event.uniqueIdentifier, responseEvent.parentID)
    }

    @Test
    fun testHandleRulesEngineResponse_withNullEventData_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(null)
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withEmptyEventData_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(emptyMap<String, Any>())
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withoutTriggerConsequence_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "nottriggeredconsequence" to mapOf(
                        "type" to "an",
                        "id" to "some value",
                        "detail" to mapOf(
                            "action" to "Test Action",
                            "contextdata" to mapOf("testKey" to "testValue")
                        )
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withTriggerConsequenceWrongType_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf("triggeredconsequence" to "i should be a map")
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withoutConsequenceId_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "type" to "an",
                        "detail" to mapOf(
                            "action" to "Test Action",
                            "contextdata" to mapOf("testKey" to "testValue")
                        )
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withConsequenceIdWrongType_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "type" to "an",
                        "id" to mapOf("wrong" to "type"), // Should be String
                        "detail" to mapOf(
                            "action" to "Test Action",
                            "contextdata" to mapOf("testKey" to "testValue")
                        )
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withWrongConsequenceType_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "type" to "md", // not Analytics type
                        "id" to "some value",
                        "detail" to mapOf(
                            "action" to "Test Action",
                            "contextdata" to mapOf("testKey" to "testValue")
                        )
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withoutConsequenceType_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "id" to "some value",
                        "detail" to mapOf(
                            "action" to "Test Action",
                            "contextdata" to mapOf("testKey" to "testValue")
                        )
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withConsequenceTypeWrongType_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "type" to listOf("an"),
                        "id" to "some value",
                        "detail" to mapOf(
                            "action" to "Test Action",
                            "contextdata" to mapOf("testKey" to "testValue")
                        )
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withoutConsequenceDetail_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "type" to "an",
                        "id" to "some value"
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withNullConsequenceDetail_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "type" to "an",
                        "id" to "some value",
                        "detail" to null
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withEmptyConsequenceDetail_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "type" to "an",
                        "id" to "some value",
                        "detail" to emptyMap<String, Any>()
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    @Test
    fun testHandleRulesEngineResponse_withConsequenceDetailWrongType_doesNotDispatchEvent() {
        val event = Event.Builder("Test Rules Engine Event", EventType.RULES_ENGINE, EventSource.RESPONSE_CONTENT)
            .setEventData(
                mapOf(
                    "triggeredconsequence" to mapOf(
                        "type" to "an",
                        "id" to "some value",
                        "detail" to "wrong type"
                    )
                )
            )
            .build()

        extension.handleRulesEngineResponse(event)

        verify(mockExtensionApi, never()).dispatch(any())
    }

    // ========================================================================================
    // formatData
    // ========================================================================================
    @Test
    fun testFormatDataFailure() {
        val deeplyNested = createDeeplyNestedMap(260)
        // This call is expected to fail due to exceeding EventDataUtils.clone max depth size
        val result = extension.formatData(deeplyNested)
        assertNull(result)
    }

    // Private helper methods
    private fun captureAndAssertDispatchedEvent(
        expectedType: String = EventType.EDGE,
        expectedSource: String = EventSource.REQUEST_CONTENT,
        expectedName: String = EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST
    ): Event {
        val eventCaptor = ArgumentCaptor.forClass(Event::class.java)

        verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture())
        val responseEvent = eventCaptor.value

        assertEquals(expectedType, responseEvent.type)
        assertEquals(expectedSource, responseEvent.source)
        assertEquals(expectedName, responseEvent.name)

        return responseEvent
    }

    private fun createDeeplyNestedMap(depth: Int): Map<String, Any> {
        val map: MutableMap<String, Any> = HashMap()
        var currentLevel = map
        for (i in 0 until depth) {
            val nextLevel: MutableMap<String, Any> = HashMap()
            // Use a unique key for each level to avoid any potential optimizations
            // that could skip duplicating identical keys or values
            currentLevel["Level$i"] = nextLevel
            currentLevel = nextLevel
        }
        return map
    }
}
