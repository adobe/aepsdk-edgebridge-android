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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
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
		String moduleName = extension.getName();
		assertEquals(
			"getName should return the correct module name",
			EdgeBridgeTestConstants.EXTENSION_NAME,
			moduleName
		);
	}

	// ========================================================================================
	// getVersion
	// ========================================================================================
	@Test
	public void test_getVersion() {
		// test
		String moduleVersion = extension.getVersion();
		assertEquals(
			"getVersion should return the correct module version",
			EdgeBridgeTestConstants.EXTENSION_VERSION,
			moduleVersion
		);
	}

	// ========================================================================================
	// getFriendlyName
	// ========================================================================================
	@Test
	public void test_getFriendlyName() {
		// Test
		String moduleFriendlyName = extension.getFriendlyName();
		assertEquals(
			"getFriendlyName should return the correct module friendly name",
			EdgeBridgeTestConstants.EXTENSION_FRIENDLY_NAME,
			moduleFriendlyName
		);
	}

	// ========================================================================================
	// handleTrackRequest
	// ========================================================================================
	@Test
	public void test_handleTrackRequest_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(
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
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put("timestamp", formatDateIso8601(event.getTimestamp()));
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
	}

	@Test
	public void test_handleTrackRequest_withNullEventData_doesNotDispatchEvent() {
		final Event event = new Event.Builder("Test Track Event", EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT)
			.setEventData(null)
			.build();

		extension.handleTrackRequest(event);

		verify(mockExtensionApi, never()).dispatch(any(Event.class));
	}

	@Test
	public void test_handleTrackRequest_withEmptyEventData_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_dispatchesEdgeRequestEvent() {
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
				put(
					"xdm",
					new HashMap<String, Object>() {
						{
							put("eventType", EdgeBridgeTestConstants.JsonValues.EVENT_TYPE);
							put("timestamp", formatDateIso8601(event.getTimestamp()));
						}
					}
				);
			}
		};

		assertEquals(expectedData, responseEvent.getEventData());
	}

	@Test
	public void test_handleRulesEngineResponse_withNullEventData_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withEmptyEventData_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withoutTriggerConsequence_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withTriggerConsequenceWrongType_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withoutConsequenceId_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withConsequenceIdWrongType_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withWrongConsequenceType_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withoutConsequenceType_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withConsequenceTypeWrongType_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withoutConsequenceDetail_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withNullConsequenceDetail_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withEmptyConsequenceDetail_doesNotDispatchEvent() {
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
	public void test_handleRulesEngineResponse_withConsequenceDetailWrongType_doesNotDispatchEvent() {
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

	// ========================================================================================
	// Utility methods
	// ========================================================================================

	private static final SimpleDateFormat iso8601DateFormat;

	static {
		final Locale posixLocale = new Locale(Locale.US.getLanguage(), Locale.US.getCountry(), "POSIX");
		iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", posixLocale);
		iso8601DateFormat.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
	}

	static String formatDateIso8601(final long timestamp) {
		return iso8601DateFormat.format(new Date(timestamp));
	}
}
