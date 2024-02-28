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

package com.adobe.marketing.mobile.edge.bridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.util.CloneFailedException;
import com.adobe.marketing.mobile.util.TimeUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EdgeBridgeExtensionTests {

	private EdgeBridgeExtension extension;

	@Mock
	ExtensionApi mockExtensionApi;

	// Performs pre-test setup before each test case
	// Resets the mocked ExtensionApi instance mockExtensionApi (handles event dispatch)
	// Creates a new instance of the EdgeBridgeExtension using the ExtensionApi mock
	@Before
	public void setup() {
		Mockito.reset(mockExtensionApi);
		extension = new EdgeBridgeExtension(mockExtensionApi);
	}

	// ========================================================================================
	// constructor
	// ========================================================================================
	@Test
	public void test_listenerRegistration() {
		extension.onRegistered();
		ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> eventSourceCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ExtensionEventListener> extensionEventListenerArgumentCaptor = ArgumentCaptor.forClass(
			ExtensionEventListener.class
		);
		// Verify: 2 event listeners registered; capture values from registration
		verify(mockExtensionApi, times(2))
			.registerEventListener(
				eventTypeCaptor.capture(),
				eventSourceCaptor.capture(),
				extensionEventListenerArgumentCaptor.capture()
			);

		// Extract captured values into lists
		List<String> eventTypes = eventTypeCaptor.getAllValues();
		List<String> eventSources = eventSourceCaptor.getAllValues();
		List<ExtensionEventListener> extensionEventListenerList = extensionEventListenerArgumentCaptor.getAllValues();

		// Verify: 1st Edge Bridge event listener
		assertEquals(EventType.GENERIC_TRACK, eventTypes.get(0));
		assertEquals(EventSource.REQUEST_CONTENT, eventSources.get(0));
		assertNotNull(extensionEventListenerList.get(0));

		// Verify: 2nd Edge Bridge event listener
		assertEquals(EventType.RULES_ENGINE, eventTypes.get(1));
		assertEquals(EventSource.RESPONSE_CONTENT, eventSources.get(1));
		assertNotNull(extensionEventListenerList.get(1));
	}

	// ========================================================================================
	// getName
	// ========================================================================================
	@Test
	public void test_getName() {
		// test
		String extensionName = extension.getName();
		assertEquals(
			"getName should return the correct extension name",
			EdgeBridgeTestConstants.EXTENSION_NAME,
			extensionName
		);
	}

	// ========================================================================================
	// getVersion
	// ========================================================================================
	@Test
	public void test_getVersion() {
		// test
		String extensionVersion = extension.getVersion();
		assertEquals(
			"getVersion should return the correct extension version",
			EdgeBridgeConstants.EXTENSION_VERSION,
			extensionVersion
		);
	}

	// ========================================================================================
	// getFriendlyName
	// ========================================================================================
	@Test
	public void test_getFriendlyName() {
		// Test
		String extensionFriendlyName = extension.getFriendlyName();
		assertEquals(
			"getFriendlyName should return the correct extension friendly name",
			EdgeBridgeTestConstants.EXTENSION_FRIENDLY_NAME,
			extensionFriendlyName
		);
	}

	// ========================================================================================
	// handleTrackRequest
	// ========================================================================================
	@Test
	public void testHandleTrackEvent_withActionField_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("action", "action name");
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put("linkName", "action name");
													put("linkType", "other");
												}
											}
										);
									}
								}
							);
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_withStateField_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("state", "state name");
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put("pageName", "state name");
												}
											}
										);
									}
								}
							);
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_withContextDataFieldUsingReservedPrefix_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"contextdata",
							new HashMap<String, Object>() {
								{
									put("&&c1", "propValue1");
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put("c1", "propValue1");
												}
											}
										);
									}
								}
							);
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_withContextDataNotUsingReservedPrefix_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"contextdata",
							new HashMap<String, Object>() {
								{
									put("key1", "value1");
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put(
														"contextData",
														new HashMap<String, Object>() {
															{
																put("key1", "value1");
															}
														}
													);
												}
											}
										);
									}
								}
							);
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_withEmptyContextData_doesNotDispatchEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("contextdata", Collections.<String, Object>emptyMap());
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleTrackEvent_withNullContextData_doesNotDispatchEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("contextdata", null);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleTrackEvent_withUnexpectedContextDataType_doesNotDispatchEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("contextdata", 123);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleTrackEvent_withDataField_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("key2", "value2");
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put("key2", "value2");
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_trackAction_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("action", "action name");
						put("key2", "value2");
						put(
							"contextdata",
							new HashMap<String, String>() {
								{
									put("&&events", "event1,event2,event3,event4,event12,event13");
									put(
										"&&products",
										";product1;1;5.99;event12=5.99;evar5=merchEvar5,;product2;2;10.99;event13=6;eVar6=mercheVar6"
									);
									put("&&c1", "propValue1");
									put("&&cc", "USD");
									put("key1", "value1");
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put("linkName", "action name");
													put("linkType", "other");
													put("events", "event1,event2,event3,event4,event12,event13");
													put(
														"products",
														";product1;1;5.99;event12=5.99;evar5=merchEvar5,;product2;2;10.99;event13=6;eVar6=mercheVar6"
													);
													put("c1", "propValue1");
													put("cc", "USD");
													put(
														"contextData",
														new HashMap<String, String>() {
															{
																put("key1", "value1");
															}
														}
													);
												}
											}
										);
									}
								}
							);
							put("key2", "value2");
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_trackState_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("state", "state name");
						put("key2", "value2");
						put(
							"contextdata",
							new HashMap<String, String>() {
								{
									put("&&events", "event1,event2,event3,event4,event12,event13");
									put("&&c1", "propValue1");
									put("&&v1", "evarValue1");
									put("key1", "value1");
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put("pageName", "state name");
													put("events", "event1,event2,event3,event4,event12,event13");
													put("c1", "propValue1");
													put("v1", "evarValue1");
													put(
														"contextData",
														new HashMap<String, String>() {
															{
																put("key1", "value1");
															}
														}
													);
												}
											}
										);
									}
								}
							);
							put("key2", "value2");
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_withNoMappedData_emptyString_doesNotDispatchEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("state", "");
						put("action", "");
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleTrackEvent_withNoMappedData_nullValues_doesNotDispatchEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("state", null);
						put("action", null);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleTrackEvent_withReservedPrefix_onlyRemovesPrefix_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"contextdata",
							new HashMap<String, Object>() {
								{
									put("&&", "value1");
									put("&&&", "value2");
									put("&&&&", "value3");
									put("&&1", "value4");
									put("&&a", "value5");
									put("&& ", "value6");
									put("&&-", "value7");
									put("&&=", "value8");
									put("&&\\", "value9");
									put("&&.", "value10");
									put("&&?", "value11");
									put("&&\n", "value12");
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put("&", "value2");
													put("&&", "value3");
													put("1", "value4");
													put("a", "value5");
													put("-", "value7");
													put("=", "value8");
													put("\\", "value9");
													put(".", "value10");
													put("?", "value11");
												}
											}
										);
									}
								}
							);
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_withCharacterBeforeReservedCharacters_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"contextdata",
							new HashMap<String, Object>() {
								{
									put("1&&", "value1");
									put("a&&", "value2");
									put(" &&", "value3");
									put("-&&", "value4");
									put("=&&", "value5");
									put("\\&&", "value6");
									put(".&&", "value7");
									put("?&&", "value8");
									put("\n&&", "value9");
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put(
														"contextData",
														new HashMap<String, String>() {
															{
																put("1&&", "value1");
																put("a&&", "value2");
																put(" &&", "value3");
																put("-&&", "value4");
																put("=&&", "value5");
																put("\\&&", "value6");
																put(".&&", "value7");
																put("?&&", "value8");
																put("\n&&", "value9");
															}
														}
													);
												}
											}
										);
									}
								}
							);
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_mapsNullAndEmptyValues_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("key3", "");
						put("key4", null);
						put(
							"contextdata",
							new HashMap<String, Object>() {
								{
									put("&&key1", "");
									put("&&key2", null);
									put("key5", "");
									put("key6", null);
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleTrackRequest(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put("key3", "");
							put("key4", null);
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put("key1", "");
													put(
														"contextData",
														new HashMap<String, String>() {
															{
																put("key5", "");
															}
														}
													);
												}
											}
										);
									}
								}
							);
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleTrackEvent_withNullEventData_doesNotDispatchEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(null)
			.build();

		extension.handleTrackRequest(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleTrackEvent_withEmptyEventData_doesNotDispatchEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(Collections.<String, Object>emptyMap())
			.build();

		extension.handleTrackRequest(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	// ========================================================================================
	// handleRulesEngineResponse
	// ========================================================================================

	@Test
	public void testHandleRulesEngineResponse_withTrackEvent_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("type", "an");
									put("id", "some value");
									put(
										"detail",
										new HashMap<String, Object>() {
											{
												put("action", "Test Action");
												put(
													"contextdata",
													new HashMap<String, Object>() {
														{
															put("testKey", "testValue");
														}
													}
												);
											}
										}
									);
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		// Verify: dispatched 1 event; capture dispatched event
		verify(mockExtensionApi, times(1)).dispatch(dispatchedEventCaptor.capture());

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EventType.EDGE, responseEvent.getType());
		assertEquals(EventSource.REQUEST_CONTENT, responseEvent.getSource());
		assertEquals(EdgeBridgeTestConstants.EventNames.EDGE_BRIDGE_REQUEST, responseEvent.getName());

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"data",
					new HashMap<String, Object>() {
						{
							put(
								"__adobe",
								new HashMap<String, Object>() {
									{
										put(
											"analytics",
											new HashMap<String, Object>() {
												{
													put("linkName", "Test Action");
													put("linkType", "other");
													put(
														"contextData",
														new HashMap<String, String>() {
															{
																put("testKey", "testValue");
															}
														}
													);
												}
											}
										);
									}
								}
							);
						}
					}
				);
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put(
								"timestamp",
								TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(event.getTimestamp()))
							);
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
		assertEquals(event.getUniqueIdentifier(), responseEvent.getParentID());
	}

	@Test
	public void testHandleRulesEngineResponse_withNullEventData_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(null)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withEmptyEventData_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(Collections.<String, Object>emptyMap())
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withoutTriggerConsequence_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"nottriggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("type", "an");
									put("id", "some value");
									put(
										"detail",
										new HashMap<String, Object>() {
											{
												put("action", "Test Action");
												put(
													"contextdata",
													new HashMap<String, Object>() {
														{
															put("testKey", "testValue");
														}
													}
												);
											}
										}
									);
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withTriggerConsequenceWrongType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put("triggeredconsequence", "i should be a map");
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withoutConsequenceId_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("type", "an");
									put(
										"detail",
										new HashMap<String, Object>() {
											{
												put("action", "Test Action");
												put(
													"contextdata",
													new HashMap<String, Object>() {
														{
															put("testKey", "testValue");
														}
													}
												);
											}
										}
									);
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withConsequenceIdWrongType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("type", "an");
									put(
										"id",
										new HashMap<String, Object>() {
											{ // Should be String
												put("wrong", "type");
											}
										}
									);
									put(
										"detail",
										new HashMap<String, Object>() {
											{
												put("action", "Test Action");
												put(
													"contextdata",
													new HashMap<String, Object>() {
														{
															put("testKey", "testValue");
														}
													}
												);
											}
										}
									);
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withWrongConsequenceType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("type", "md"); // not Analytics type
									put("id", "some value");
									put(
										"detail",
										new HashMap<String, Object>() {
											{
												put("action", "Test Action");
												put(
													"contextdata",
													new HashMap<String, Object>() {
														{
															put("testKey", "testValue");
														}
													}
												);
											}
										}
									);
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withoutConsequenceType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("id", "some value");
									put(
										"detail",
										new HashMap<String, Object>() {
											{
												put("action", "Test Action");
												put(
													"contextdata",
													new HashMap<String, Object>() {
														{
															put("testKey", "testValue");
														}
													}
												);
											}
										}
									);
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withConsequenceTypeWrongType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put(
										"type",
										new ArrayList<String>() {
											{
												add("an");
											}
										}
									);
									put("id", "some value");
									put(
										"detail",
										new HashMap<String, Object>() {
											{
												put("action", "Test Action");
												put(
													"contextdata",
													new HashMap<String, Object>() {
														{
															put("testKey", "testValue");
														}
													}
												);
											}
										}
									);
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withoutConsequenceDetail_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("type", "an");
									put("id", "some value");
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withNullConsequenceDetail_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("type", "an");
									put("id", "some value");
									put("detail", null);
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withEmptyConsequenceDetail_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("type", "an");
									put("id", "some value");
									put("detail", new HashMap<String, Object>());
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void testHandleRulesEngineResponse_withConsequenceDetailWrongType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EventType.RULES_ENGINE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(
				new HashMap<String, Object>() {
					{
						put(
							"triggeredconsequence",
							new HashMap<String, Object>() {
								{
									put("type", "an");
									put("id", "some value");
									put("detail", "wrong type");
								}
							}
						);
					}
				}
			)
			.build();

		extension.handleRulesEngineResponse(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test(expected = CloneFailedException.class)
	public void testDeepCopyFailure() throws CloneFailedException {
		Map<String, Object> deeplyNested = createDeeplyNestedMap(260);
		// This call is expected to throw CloneFailedException due to the depth of the map
		extension.deepCopy(deeplyNested);
	}

	private static Map<String, Object> createDeeplyNestedMap(int depth) {
		Map<String, Object> map = new HashMap<>();
		Map<String, Object> currentLevel = map;
		for (int i = 0; i < depth; i++) {
			Map<String, Object> nextLevel = new HashMap<>();
			// Use a unique key for each level to avoid any potential optimizations
			// that could skip duplicating identical keys or values
			currentLevel.put("Level" + i, nextLevel);
			currentLevel = nextLevel;
		}
		return map;
	}
}
