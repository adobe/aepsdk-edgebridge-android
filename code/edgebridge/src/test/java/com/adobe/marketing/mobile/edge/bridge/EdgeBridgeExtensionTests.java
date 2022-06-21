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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Application;
import android.content.Context;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MobileCore.class, ExtensionApi.class })
public class EdgeBridgeExtensionTests {

	private EdgeBridgeExtension extension;

	@Mock
	ExtensionApi mockExtensionApi;

	@Mock
	Application mockApplication;

	@Mock
	Context mockContext;

	@Before
	public void setup() {
		PowerMockito.mockStatic(MobileCore.class);
		Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
		Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);

		extension = new EdgeBridgeExtension(mockExtensionApi);
	}

	// ========================================================================================
	// constructor
	// ========================================================================================
	@Test
	public void test_listenerRegistration() {
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(
			ExtensionErrorCallback.class
		);

		verify(mockExtensionApi, times(2))
			.registerEventListener(anyString(), anyString(), any(Class.class), any(ExtensionErrorCallback.class));

		verify(mockExtensionApi, times(1))
			.registerEventListener(
				eq(EdgeBridgeTestConstants.EventType.GENERIC_TRACK),
				eq(EdgeBridgeTestConstants.EventSource.REQUEST_CONTENT),
				eq(ListenerGenericTrackRequestContent.class),
				callbackCaptor.capture()
			);
		Assert.assertNotNull(
			"The extension callback for Generic Track listener should not be null",
			callbackCaptor.getValue()
		);

		verify(mockExtensionApi, times(1))
			.registerEventListener(
				eq(EdgeBridgeTestConstants.EventType.RULES_ENGINE),
				eq(EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT),
				eq(ListenerRulesEngineResponseContent.class),
				callbackCaptor.capture()
			);
		Assert.assertNotNull(
			"The extension callback for Rules Engine listener should not be null",
			callbackCaptor.getValue()
		);
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
	// handleTrackRequest
	// ========================================================================================
	@Test
	public void test_handleTrackRequest_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder(
			"Test Track Event",
			EdgeBridgeTestConstants.EventType.GENERIC_TRACK,
			EdgeBridgeTestConstants.EventSource.REQUEST_CONTENT
		)
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

		final ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		extension.handleTrackRequest(event);

		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(dispatchedEventCaptor.capture(), any(ExtensionErrorCallback.class));

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EdgeBridgeTestConstants.EventType.EDGE.toLowerCase(), responseEvent.getType());
		assertEquals(EdgeBridgeTestConstants.EventSource.REQUEST_CONTENT.toLowerCase(), responseEvent.getSource());
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
		final Event event = new Event.Builder(
			"Test Track Event",
			EdgeBridgeTestConstants.EventType.GENERIC_TRACK,
			EdgeBridgeTestConstants.EventSource.REQUEST_CONTENT
		)
			.setEventData(null)
			.build();

		extension.handleTrackRequest(event);

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleTrackRequest_withEmptyEventData_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Track Event",
			EdgeBridgeTestConstants.EventType.GENERIC_TRACK,
			EdgeBridgeTestConstants.EventSource.REQUEST_CONTENT
		)
			.setEventData(Collections.<String, Object>emptyMap())
			.build();

		extension.handleTrackRequest(event);

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	// ========================================================================================
	// handleRulesEngineResponse
	// ========================================================================================

	@Test
	public void test_handleRulesEngineResponse_dispatchesEdgeRequestEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		final ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		extension.handleRulesEngineResponse(event);

		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(dispatchedEventCaptor.capture(), any(ExtensionErrorCallback.class));

		Event responseEvent = dispatchedEventCaptor.getAllValues().get(0);
		assertEquals(EdgeBridgeTestConstants.EventType.EDGE.toLowerCase(), responseEvent.getType());
		assertEquals(EdgeBridgeTestConstants.EventSource.REQUEST_CONTENT.toLowerCase(), responseEvent.getSource());
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
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
		)
			.setEventData(null)
			.build();

		extension.handleRulesEngineResponse(event);

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withEmptyEventData_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
		)
			.setEventData(Collections.<String, Object>emptyMap())
			.build();

		extension.handleRulesEngineResponse(event);

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withoutTriggerConsequence_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withTriggerConsequenceWrongType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withoutConsequenceId_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withConsequenceIdWrongType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withWrongConsequenceType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withoutConsequenceType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withConsequenceTypeWrongType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withoutConsequenceDetail_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withNullConsequenceDetail_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withEmptyConsequenceDetail_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	@Test
	public void test_handleRulesEngineResponse_withConsequenceDetailWrongType_doesNotDispatchEvent() {
		final Event event = new Event.Builder(
			"Test Rules Engine Event",
			EdgeBridgeTestConstants.EventType.RULES_ENGINE,
			EdgeBridgeTestConstants.EventSource.RESPONSE_CONTENT
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

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
	}

	// ========================================================================================
	// Utility methods
	// ========================================================================================

	private static final SimpleDateFormat iso8601DateFormat;

	static {
		final Locale posixLocale = new Locale(Locale.US.getLanguage(), Locale.US.getCountry(), "POSIX");
		iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", posixLocale);
		iso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	static String formatDateIso8601(final long timestamp) {
		return iso8601DateFormat.format(new Date(timestamp));
	}
}
