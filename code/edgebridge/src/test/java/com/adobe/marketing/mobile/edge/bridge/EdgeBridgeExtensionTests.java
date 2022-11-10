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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Application;
import android.content.Context;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

// use simulate coming event to trigger desired behavior
// need testable extension class
// with the introduction of the new ExtensionApi -> dispatch API, unit tests can instead be self contained
// within the extension itself without having to mock and capture MobileCore dispatch calls

// so then the mockExtensionApi class will actually be the one capturing the events

@RunWith(PowerMockRunner.class)
@PrepareForTest({ MobileCore.class, ExtensionApi.class, ExtensionErrorCallback.class, ExtensionError.class })
public class EdgeBridgeExtensionTests {

	private EdgeBridgeExtension extension;

	@Mock
	ExtensionApi mockExtensionApi;

	@Mock
	Application mockApplication;

	@Mock
	Context mockContext;

	// note that in this setup for each test run the following are performed:
	// 1. mobilecore mock is created
	// * this mock is setup to return the application (deprecated)
	// 2. mock application returns a mock app context
	// 3. EdgeBridgeExtension to test is hooked up with a mock ExtenionApi instance

	// after going over some test cases, it seems split into two main categories:
	// 1. event based - testing event structure, values, number of dispatch or not (depending on case)
	// 2. getter based - testing EdgeBridgeExtension's getter methods (adherence to Extension protocol)
	// broadly, replace mobilecore dispatch with ExtensionApi dispatch
	// refactor tautological tests with

	// note: still not sure where mockApp or mockContext is used????? will apply this when encountered in a test case
	@Before
	public void setup() {
		// creates a static mock (? does this mean that none of the methods actually do anything?) of all methods in the MobileCore class
		// this can probably be replaced with the:
		// try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
		// not sure if this means
		PowerMockito.mockStatic(MobileCore.class);
		// in case MobileCore is still needed, apply these same when->then to the mockito mock
		Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
		Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);

		// this extension is the ACTUAL EdgeBridgeExtension (the thing we want to test), hooked up to
		// a mocked extensionApi; since ExtensionApi is not the class we want to test (that should be handled
		// by Core) AND ExtensionApi has the new dispatch API that fulfills the role of the old MobileCore dispatch
		extension = new EdgeBridgeExtension(mockExtensionApi);
	}

	// ========================================================================================
	// constructor
	// ========================================================================================

	// TODO: Update this test to test against the NEW registerEventListener API on ExtensionApi which
	// does not have the ExtensionErrorCallback (which is no longer supported)
	// essence: testing the onRegistered method of EdgeBridgeExtension actually making the calls to register the required listeners
	// note: this does not test listeners actually capture events etc. as that is outside the scope of the UNIT
	// of onRegistered for EdgeBridgeExtension
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
		assertNotNull(
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
		assertNotNull("The extension callback for Rules Engine listener should not be null", callbackCaptor.getValue());
	}

	// ========================================================================================
	// getName
	// ========================================================================================
	// to avoid tautological tests, hardcode the values you're looking for
	// if the test uses the exact same flow as the method itself, then that means you're just testing
	// the language works at calling the same path, not that the value is what you expect
	// apply the same logic for all the getter type tests below as well
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
	// TODO: refactor tautological test
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
	// TODO: refactor tautological test; event verification object should have hardcoded values?
	// remove mobileCore.dispatch mock in favor of ExtensionApi dispatch capture
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

	// TODO: replace mobilecore with extensionApi dispatch
	@Test(expected = Test.None.class)
	public void test_handleTrackRequest_withNullEvent_doesNotThrow() {
		extension.handleTrackRequest(null);

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
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

	@Test(expected = Test.None.class)
	public void test_handleRulesEngineResponse_withNullEvent_doesNotThrow() {
		extension.handleRulesEngineResponse(null);

		PowerMockito.verifyStatic(MobileCore.class, Mockito.never());
		MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
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

	@Test
	public void test_handleValidEvent_whenDispatchThrows_logsMessage() {
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

		extension.handleTrackRequest(event);

		final ArgumentCaptor<ExtensionErrorCallback> errorCallbackCaptor = ArgumentCaptor.forClass(
			ExtensionErrorCallback.class
		);

		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(any(Event.class), errorCallbackCaptor.capture());

		errorCallbackCaptor.getValue().error(null); // Mocking call to ExtensionErrorCallback with null, ExtensionError is not instantiable
		final ArgumentCaptor<String> logMessageCaptor = ArgumentCaptor.forClass(String.class);
		PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.log(any(LoggingMode.class), any(String.class), logMessageCaptor.capture());
		assertTrue(
			logMessageCaptor
				.getValue()
				.startsWith("EdgeBridgeExtension - Failed to dispatch Edge Bridge request event with id")
		);
	}

	// ========================================================================================
	// getExecutor
	// ========================================================================================
	@Test
	public void testGetExecutor_notNull() {
		assertNotNull(extension.getExecutor());
	}

	@Test
	public void testGetExecutor_singleton() {
		assertSame(extension.getExecutor(), extension.getExecutor());
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
