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

import static com.adobe.marketing.mobile.services.HttpMethod.GET;
import static com.adobe.marketing.mobile.services.HttpMethod.POST;
import static com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch;
import static com.adobe.marketing.mobile.util.NodeConfig.Scope.Subtree;
import static com.adobe.marketing.mobile.util.TestHelper.LogOnErrorRule;
import static com.adobe.marketing.mobile.util.TestHelper.SetupCoreRule;
import static com.adobe.marketing.mobile.util.TestHelper.getAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.ElementCount;
import com.adobe.marketing.mobile.util.MockNetworkService;
import com.adobe.marketing.mobile.util.TestHelper;
import com.adobe.marketing.mobile.util.ValueTypeMatch;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EdgeBridgeFunctionalTests {

	private static final MockNetworkService mockNetworkService = new MockNetworkService();

	// Fake Edge Configuration ID, needed for Edge extension initialization
	private static final String CONFIG_ID = "1234abcd-abcd-1234-5678-123456abcdef";
	// Edge Network interact endpoint
	private static final String EDGE_INTERACT_ENDPOINT = "https://edge.adobedc.net/ee/v1/interact";

	@Rule
	public RuleChain rule = RuleChain.outerRule(new LogOnErrorRule()).around(new SetupCoreRule());

	@Before
	public void setup() throws Exception {
		ServiceProvider.getInstance().setNetworkService(mockNetworkService);

		HashMap<String, Object> config = new HashMap<String, Object>() {
			{
				put("edge.configId", CONFIG_ID);
			}
		};
		MobileCore.updateConfiguration(config);

		final CountDownLatch latch = new CountDownLatch(1);
		MobileCore.registerExtensions(
			Arrays.asList(Edge.EXTENSION, Identity.EXTENSION, EdgeBridge.EXTENSION),
			o -> latch.countDown()
		);

		latch.await();
		resetTestExpectations();
	}

	@After
	public void tearDown() {
		mockNetworkService.reset();
	}

	@Test
	public void testTrackState_sendsEdgeExperienceEvent() throws InterruptedException {
		mockNetworkService.setExpectationForNetworkRequest(EDGE_INTERACT_ENDPOINT, POST, 1);
		//setExpectationNetworkRequest(EDGE_INTERACT_ENDPOINT, HttpMethod.POST, 1);

		MobileCore.trackState(
			"state name",
			new HashMap<String, String>() {
				{
					put("key1", "value1");
					put("&&c1", "propValue1");
				}
			}
		);

		mockNetworkService.assertAllNetworkRequestExpectations();

		String expected =
			"{" +
			"  \"events\": [" +
			"    {" +
			"      \"xdm\": {" +
			"        \"eventType\": \"analytics.track\"," +
			"        \"timestamp\": \"STRING_TYPE\"," +
			"        \"_id\": \"STRING_TYPE\"" +
			"      }," +
			"      \"data\": {" +
			"        \"__adobe\": {" +
			"          \"analytics\": {" +
			"            \"cp\": \"foreground\"," +
			"            \"pageName\": \"state name\"," +
			"            \"c1\": \"propValue1\"," +
			"            \"contextData\": {" +
			"              \"key1\": \"value1\"," +
			"              \"a.AppID\": \"com.adobe.marketing.mobile.edge.bridge.test\"" +
			"            }" +
			"          }" +
			"        }" +
			"      }" +
			"    }" +
			"  ]" +
			"}";

		List<com.adobe.marketing.mobile.services.TestableNetworkRequest> networkRequests = mockNetworkService.getNetworkRequestsWith(
			EDGE_INTERACT_ENDPOINT,
			POST,
			1000
		);
		assertEquals(1, networkRequests.size());
		assertExactMatch(
			expected,
			networkRequests.get(0).getBodyJson(),
			new ElementCount(17, Subtree),
			new ValueTypeMatch("events[0].xdm.timestamp", "events[0].xdm._id")
		);
	}

	@Test
	public void testTrackAction_sendsCorrectRequestEvent() throws InterruptedException {
		mockNetworkService.setExpectationForNetworkRequest(EDGE_INTERACT_ENDPOINT, HttpMethod.POST, 1);

		MobileCore.trackAction(
			"action name",
			new HashMap<String, String>() {
				{
					put("key1", "value1");
					put("&&c1", "propValue1");
				}
			}
		);

		mockNetworkService.assertAllNetworkRequestExpectations();

		List<com.adobe.marketing.mobile.services.TestableNetworkRequest> networkRequests = mockNetworkService.getNetworkRequestsWith(
			EDGE_INTERACT_ENDPOINT,
			HttpMethod.POST
		);
		assertEquals(1, networkRequests.size());

		String expected =
			"{" +
			"\"events\": [" +
			"    {" +
			"        \"xdm\": {" +
			"            \"eventType\": \"analytics.track\"," +
			"            \"timestamp\": \"STRING_STYLE\"," +
			"            \"_id\": \"STRING_STYLE\"" +
			"        }," +
			"        \"data\": {" +
			"            \"__adobe\": {" +
			"                \"analytics\": {" +
			"                    \"cp\": \"foreground\"," +
			"                    \"linkName\": \"action name\"," +
			"                    \"linkType\": \"other\"," +
			"                    \"contextData\": {" +
			"                        \"key1\": \"value1\"," +
			"                        \"a.AppID\": \"com.adobe.marketing.mobile.edge.bridge.test\"" +
			"                    }" +
			"                }" +
			"            }" +
			"        }" +
			"    }" +
			"]" +
			"}";

		assertExactMatch(
			expected,
			networkRequests.get(0).getBodyJson(),
			new ElementCount(18, Subtree),
			new ValueTypeMatch("events[0].xdm.timestamp", "events[0].xdm._id")
		);
	}

	@Test
	public void testRulesEngineResponse_sendsCorrectRequestEvent() throws InterruptedException, IOException {
		updateConfigurationWithRules("rules_analytics");
		resetTestExpectations();

		mockNetworkService.setExpectationForNetworkRequest(EDGE_INTERACT_ENDPOINT, HttpMethod.POST, 1);

		// Triggers Analytics rule
		MobileCore.collectPii(
			new HashMap<String, String>() {
				{
					put("key", "value");
				}
			}
		);

		mockNetworkService.assertAllNetworkRequestExpectations();

		List<com.adobe.marketing.mobile.services.TestableNetworkRequest> networkRequests = mockNetworkService.getNetworkRequestsWith(
			EDGE_INTERACT_ENDPOINT,
			POST,
			1000
		);

		String expected =
			"{" +
			"\"events\": [" +
			"    {" +
			"        \"xdm\": {" +
			"            \"eventType\": \"analytics.track\"," +
			"            \"timestamp\": \"STRING_TYPE\"," +
			"            \"_id\": \"STRING_TYPE\"" +
			"        }," +
			"        \"data\": {" +
			"            \"__adobe\": {" +
			"                \"analytics\": {" +
			"                    \"cp\": \"foreground\"," +
			"                    \"linkName\": \"Rule Action\"," +
			"                    \"linkType\": \"other\"," +
			"                    \"pageName\": \"Rule State\"," +
			"                    \"contextData\": {" +
			"                        \"testKey\": \"testValue\"," +
			"                        \"a.AppID\": \"com.adobe.marketing.mobile.edge.bridge.test\"" +
			"                    }" +
			"                }" +
			"            }" +
			"        }" +
			"    }" +
			"]" +
			"}";

		assertExactMatch(
			expected,
			networkRequests.get(0).getBodyJson(),
			new ElementCount(18, Subtree),
			new ValueTypeMatch("events[0].xdm.timestamp", "events[0].xdm._id")
		);
	}

	/**
	 * Helper function to update configuration with rules URL and mock response with a local zip file.
	 * @param localRulesName name of bundled Assets file with rules definition, without '.zip' extension.
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void updateConfigurationWithRules(final String localRulesName) throws InterruptedException, IOException {
		final InputStream rules = getAsset(localRulesName + ".zip");
		assertNotNull("Local rules file '" + localRulesName + "' was not found!", rules);

		HttpConnecting response = new HttpConnecting() {
			// Date format RFC 2822 used to set Last-Modified HTTP header
			private final SimpleDateFormat format;

			{
				final String pattern = "EEE, dd MMM yyyy HH:mm:ss z";
				format = new SimpleDateFormat(pattern, Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
			}

			// Configuration requires Last-Modified set for remote download
			private final Map<String, String> props = new HashMap<String, String>() {
				{
					put("Last-Modified", format.format(new Date()));
				}
			};

			@Override
			public InputStream getInputStream() {
				return rules;
			}

			@Override
			public InputStream getErrorStream() {
				return null;
			}

			@Override
			public int getResponseCode() {
				return 200;
			}

			@Override
			public String getResponseMessage() {
				return null;
			}

			@Override
			public String getResponsePropertyValue(String s) {
				return props.get(s);
			}

			@Override
			public void close() {}
		};

		final String rulesUrl = "https://rules.com/" + localRulesName + ".zip";
		mockNetworkService.setMockResponseFor(rulesUrl, GET, response);
		mockNetworkService.setExpectationForNetworkRequest(rulesUrl, GET, 1);

		MobileCore.updateConfiguration(
			new HashMap<String, Object>() {
				{
					put("rules.url", rulesUrl);
				}
			}
		);
		mockNetworkService.assertAllNetworkRequestExpectations();
	}

	private void resetTestExpectations() {
		mockNetworkService.reset();
		TestHelper.resetTestExpectations();
	}
}
