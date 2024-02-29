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

package com.adobe.marketing.mobile.edge.bridge;

import static org.junit.Assert.assertEquals;

import com.adobe.marketing.mobile.ExtensionApi;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EdgeBridgeExtensionHelperTests {

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

	// Validates method:
	// 1. Creates expected hierarchy when not present:
	//     1. `__adobe.analytics`
	//     2. `__adobe.analytics.contextData`
	// 2. Adds the expected values
	@Test
	public void testAnalyticsProperties_withEmptyMap_addsProperties() {
		Map<String, Object> eventData = new HashMap<String, Object>() {};

		extension.addAnalyticsProperties(eventData);

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"__adobe",
					new HashMap<String, Object>() {
						{
							put(
								"analytics",
								new HashMap<String, Object>() {
									{
										put("cp", "foreground");
										put(
											"contextData",
											new HashMap<String, Object>() {
												{
													put("a.AppID", "null");
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
		};
		assertEquals(expectedData, eventData);
	}

	// Validates method:
	// 1. Does not overwrite existing values in the hierarchy when already present:
	//     1. `__adobe.analytics`
	// 2. Creates expected hierarchy when not present:
	//     1. `__adobe.analytics.contextData`
	// 3. Adds the expected values
	@Test
	public void testAnalyticsProperties_withAdobeAnalytics_noContextData_addsProperties() {
		Map<String, Object> eventData = new HashMap<String, Object>() {
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
		};

		extension.addAnalyticsProperties(eventData);

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"__adobe",
					new HashMap<String, Object>() {
						{
							put(
								"analytics",
								new HashMap<String, Object>() {
									{
										put("cp", "foreground");
										put("linkName", "action name");
										put("linkType", "other");
										put(
											"contextData",
											new HashMap<String, Object>() {
												{
													put("a.AppID", "null");
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
		};
		assertEquals(expectedData, eventData);
	}

	// Validates method:
	// 1. Does not overwrite existing values in the hierarchy when already present:
	//     1. `__adobe.analytics`
	//     2. `__adobe.analytics.contextData`
	// 2. Adds the expected values
	@Test
	public void testAnalyticsProperties_withAdobeAnalytics_andContextData_addsProperties() {
		Map<String, Object> eventData = new HashMap<String, Object>() {
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
		};

		extension.addAnalyticsProperties(eventData);

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put(
					"__adobe",
					new HashMap<String, Object>() {
						{
							put(
								"analytics",
								new HashMap<String, Object>() {
									{
										put("cp", "foreground");
										put("linkName", "action name");
										put("linkType", "other");
										put(
											"contextData",
											new HashMap<String, Object>() {
												{
													put("key1", "value1");
													put("a.AppID", "null");
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
		};
		assertEquals(expectedData, eventData);
	}

	// Validates method:
	// 1. Does not overwrite existing values in the hierarchy when already present:
	//     1. Values outside `__adobe.analytics`
	// 2. Creates expected hierarchy when not present:
	//     1. `__adobe.analytics`
	//     2. `__adobe.analytics.contextData`
	// 3. Adds the expected values
	@Test
	public void testAnalyticsProperties_withNoAdobeAnalytics_addsProperties() {
		Map<String, Object> eventData = new HashMap<String, Object>() {
			{
				put("key1", "value1");
			}
		};

		extension.addAnalyticsProperties(eventData);

		Map<String, Object> expectedData = new HashMap<String, Object>() {
			{
				put("key1", "value1");
				put(
					"__adobe",
					new HashMap<String, Object>() {
						{
							put(
								"analytics",
								new HashMap<String, Object>() {
									{
										put("cp", "foreground");
										put(
											"contextData",
											new HashMap<String, Object>() {
												{
													put("a.AppID", "null");
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
		};
		assertEquals(expectedData, eventData);
	}
}
