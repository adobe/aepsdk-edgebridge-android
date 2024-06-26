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

import static com.adobe.marketing.mobile.edge.bridge.EdgeBridgeConstants.LOG_TAG;
import static com.adobe.marketing.mobile.util.MapUtils.isNullOrEmpty;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.CloneFailedException;
import com.adobe.marketing.mobile.util.DataReader;
import com.adobe.marketing.mobile.util.EventDataUtils;
import com.adobe.marketing.mobile.util.StringUtils;
import com.adobe.marketing.mobile.util.TimeUtils;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class EdgeBridgeExtension extends Extension {

	private static final String LOG_SOURCE = "EdgeBridgeExtension";

	protected EdgeBridgeExtension(final ExtensionApi extensionApi) {
		super(extensionApi);
	}

	@NonNull @Override
	protected String getName() {
		return EdgeBridgeConstants.EXTENSION_NAME;
	}

	@NonNull @Override
	protected String getFriendlyName() {
		return EdgeBridgeConstants.FRIENDLY_NAME;
	}

	@NonNull @Override
	protected String getVersion() {
		return EdgeBridgeConstants.EXTENSION_VERSION;
	}

	@Override
	protected void onRegistered() {
		getApi().registerEventListener(EventType.GENERIC_TRACK, EventSource.REQUEST_CONTENT, this::handleTrackRequest);
		getApi()
			.registerEventListener(
				EventType.RULES_ENGINE,
				EventSource.RESPONSE_CONTENT,
				this::handleRulesEngineResponse
			);
	}

	/**
	 * Handles generic Analytics track events coming from the public APIs.
	 * @param event the generic track request event
	 */
	void handleTrackRequest(@NonNull final Event event) {
		final Map<String, Object> eventData = event.getEventData();

		if (isNullOrEmpty(eventData)) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Unable to handle track request event with id '%s': event data is missing or empty.",
				event.getUniqueIdentifier()
			);
			return;
		}

		dispatchTrackRequest(eventData, event);
	}

	/**
	 * Handles Analytics track events generated by a rule consequence.
	 * @param event the rules engine response event
	 */
	void handleRulesEngineResponse(@NonNull final Event event) {
		final Map<String, Object> eventData = event.getEventData();

		if (isNullOrEmpty(eventData)) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Ignoring Rules Engine response event with id '%s': event data is missing or empty.",
				event.getUniqueIdentifier()
			);
			return;
		}

		final Map<String, Object> consequence = DataReader.optTypedMap(
			Object.class,
			eventData,
			"triggeredconsequence",
			null
		);

		if (isNullOrEmpty(consequence)) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Ignoring Rule Engine response event with id '%s': consequence data is invalid or empty.",
				event.getUniqueIdentifier()
			);
			return;
		}

		final String type = DataReader.optString(consequence, "type", null);

		if (!"an".equals(type)) {
			// Not an Analytics rules consequence
			return;
		}

		final String id = DataReader.optString(consequence, "id", null);

		if (StringUtils.isNullOrEmpty(id)) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Ignoring Rule Engine response event with id '%s': consequence id is invalid or empty.",
				event.getUniqueIdentifier()
			);
			return;
		}

		final Map<String, Object> detail = DataReader.optTypedMap(Object.class, consequence, "detail", null);

		if (isNullOrEmpty(detail)) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"Ignoring Rule Engine response event with id '%s': consequence detail is invalid or empty.",
				event.getUniqueIdentifier()
			);
			return;
		}

		dispatchTrackRequest(detail, event);
	}

	/**
	 * Helper to create and dispatch an experience event.
	 *
	 * Track events will not be dispatched in any of the following cases:
	 * 1. Deep copy of the event data map fails.
	 * 2. Event data map does not have any data or valid action/state.
	 *
	 * @param data map containing free-form data to send to Edge Network
	 * @param parentEvent the triggering parent event used for event chaining; its timestamp is set as xdm.timestamp
	 */
	private void dispatchTrackRequest(final Map<String, Object> data, final Event parentEvent) {
		Map<String, Object> formattedData = formatData(data);
		if (formattedData == null) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"Event '" +
				parentEvent.getUniqueIdentifier() +
				"' did not contain any mappable data. Experience event not dispatched."
			);
			return;
		}

		Map<String, Object> xdmData = new HashMap<>();
		xdmData.put("eventType", EdgeBridgeConstants.JsonValues.EVENT_TYPE);
		xdmData.put("timestamp", TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(parentEvent.getTimestamp())));

		Map<String, Object> eventData = new HashMap<>();
		eventData.put("xdm", xdmData);
		eventData.put("data", formattedData);

		final Event event = new Event.Builder(
			EdgeBridgeConstants.EventNames.EDGE_BRIDGE_REQUEST,
			EventType.EDGE,
			EventSource.REQUEST_CONTENT
		)
			.chainToParentEvent(parentEvent)
			.setEventData(eventData)
			.build();

		getApi().dispatch(event);
	}

	/**
	 * Formats track event data to the required Analytics Edge translator format under the `data.__adobe.analytics` object.
	 *
	 * The following is the mapping logic:
	 * - The "action" field is mapped to "data.__adobe.analytics.linkName", and "data.__adobe.analytics.linkType" is set to "other".
	 * - The "state" field is mapped to "data.__adobe.analytics.pageName".
	 * - Any "contextData" keys that start with the "&&" prefix are mapped to "data.__adobe.analytics" with the prefix removed.
	 * - Any "contextData" keys without the "&&" prefix are mapped to "data.__adobe.analytics.contextData".
	 * - Any additional fields are passed through and left directly under the "data" object.
	 *
	 * As an example, the following track event data:
	 * ```
	 * {
	 *    "action": "action name",
	 *    "contextdata": {
	 *       "&&c1": "propValue1",
	 *       "key1": "value1"
	 *    },
	 *    "key2": "value2"
	 * }
	 * ```
	 * Is mapped to:
	 * ```
	 * {
	 *   "data": {
	 *     "__adobe": {
	 *       "analytics": {
	 *         "linkName": "action name",
	 *         "linkType": "other",
	 *         "c1": "propValue1",
	 *         "contextData": {
	 *           "key1": "value1"
	 *         }
	 *       }
	 *     },
	 *     "key2": "value2"
	 *   }
	 * }
	 * ```
	 *
	 * @param data track event data
	 * @return data formatted for the Analytics Edge translator. {@code null} if there is no data in
	 * the payload after format rules are applied, OR if the cloning process fails.
	 */
	@VisibleForTesting
	Map<String, Object> formatData(final Map<String, Object> data) {
		Map<String, Object> mutableData;

		// Create a mutable copy of data - can throw exception if deep copy fails
		try {
			mutableData = EventDataUtils.clone(data);
		} catch (CloneFailedException e) {
			Log.warning(LOG_TAG, LOG_SOURCE, "Failed to format data due to map clone failure: " + e.getMessage());
			return null;
		}

		// If there is no data to format, early exit and return null
		if (isNullOrEmpty(mutableData)) {
			return null;
		}

		// __adobe.analytics data container
		Map<String, Object> analyticsData = new HashMap<>();

		// Extract contextData
		final Map<String, Object> extractedContextData = DataReader.optTypedMap(
			Object.class,
			mutableData,
			EdgeBridgeConstants.MobileCoreKeys.CONTEXT_DATA,
			null
		);
		mutableData.remove(EdgeBridgeConstants.MobileCoreKeys.CONTEXT_DATA);

		// Extract action
		String actionValue = DataReader.optString(mutableData, EdgeBridgeConstants.MobileCoreKeys.ACTION, null);
		mutableData.remove(EdgeBridgeConstants.MobileCoreKeys.ACTION);
		// Extract state
		String stateValue = DataReader.optString(mutableData, EdgeBridgeConstants.MobileCoreKeys.STATE, null);
		mutableData.remove(EdgeBridgeConstants.MobileCoreKeys.STATE);

		boolean actionIsValid = !StringUtils.isNullOrEmpty(actionValue);
		boolean stateIsValid = !StringUtils.isNullOrEmpty(stateValue);

		// Check for required event payload conditions
		// `mutableData` check is still required here because there can be properties outside of the
		// remapped ones that would cause this to still be a valid event
		if (isNullOrEmpty(mutableData) && isNullOrEmpty(extractedContextData) && !actionIsValid && !stateIsValid) {
			return null;
		}

		if (!isNullOrEmpty(extractedContextData)) {
			final Map<String, String> contextData = cleanContextData(extractedContextData);
			Map<String, Object> prefixedData = new HashMap<>();
			Map<String, Object> nonPrefixedData = new HashMap<>();

			for (Map.Entry<String, String> entry : contextData.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				// Filter out invalid keys
				if (StringUtils.isNullOrEmpty(key)) {
					Log.debug(
						LOG_TAG,
						LOG_SOURCE,
						"formatData - Dropping Key(" +
						key +
						") with Value(" +
						value +
						"). Key must be a non-empty String."
					);
					continue;
				}
				// Check if the key starts with the specified prefix and add to corresponding map
				if (key.startsWith(EdgeBridgeConstants.AnalyticsValues.PREFIX)) {
					String newKey = key.substring(EdgeBridgeConstants.AnalyticsValues.PREFIX.length());
					// After modifying key by removing prefix, filter out invalid keys
					if (StringUtils.isNullOrEmpty(newKey)) {
						Log.debug(
							LOG_TAG,
							LOG_SOURCE,
							"formatData - Dropping Key(" +
							key +
							" -> " +
							newKey +
							") with Value(" +
							value +
							"). Key must be a non-empty String."
						);
						continue;
					}
					prefixedData.put(newKey, value);
				} else {
					nonPrefixedData.put(key, value);
				}
			}

			// If there are prefixed data entries, add them to analyticsData
			if (!prefixedData.isEmpty()) {
				analyticsData.putAll(prefixedData);
			}

			// If there are non-prefixed data entries, add them under the contextData key
			if (!nonPrefixedData.isEmpty()) {
				analyticsData.put(EdgeBridgeConstants.AnalyticsKeys.CONTEXT_DATA, nonPrefixedData);
			}
		}

		// Process action
		if (actionIsValid) {
			analyticsData.put(EdgeBridgeConstants.AnalyticsKeys.LINK_NAME, actionValue);
			analyticsData.put(EdgeBridgeConstants.AnalyticsKeys.LINK_TYPE, EdgeBridgeConstants.AnalyticsValues.OTHER);
		}

		// Process state
		if (stateIsValid) {
			analyticsData.put(EdgeBridgeConstants.AnalyticsKeys.PAGE_NAME, stateValue);
		}

		// If analyticsData is not empty, add it to mutableData under __adobe.analytics
		if (!analyticsData.isEmpty()) {
			addAnalyticsProperties(analyticsData);
			Map<String, Object> adobeAnalytics = new HashMap<>();
			adobeAnalytics.put(EdgeBridgeConstants.AnalyticsKeys.ANALYTICS, analyticsData);
			mutableData.put(EdgeBridgeConstants.AnalyticsKeys.ADOBE, adobeAnalytics);
		}

		return mutableData;
	}

	/**
	 * Remove entries with values which cannot be converted to String.
	 */
	private Map<String, String> cleanContextData(final Map<String, Object> eventData) {
		Map<String, String> cleanedData = new HashMap<>();
		for (Map.Entry<String, Object> entry : eventData.entrySet()) {
			if (entry.getValue() instanceof String) {
				cleanedData.put(entry.getKey(), (String) entry.getValue());
			}
		}
		return cleanedData;
	}

	/**
	 * Adds the following keys to the given data map:
	 * <p>__adobe.analytics.cp</p>
	 * <p>__adobe.analytics.contextData.a.AppId</p>
	 * Creates required paths if they are not already present in the original data map.
	 * This should be used only after first validating the Analytics data map is valid and should have
	 * the additional properties added.
	 *
	 * @param analyticsData the Analytics data mutable map that will have Analytics properties added.
	 */
	@VisibleForTesting
	void addAnalyticsProperties(final Map<String, Object> analyticsData) {
		// Analytics original implementation: Customer perspective defaults to foreground when unknown and is always present
		analyticsData.put(
			EdgeBridgeConstants.AnalyticsKeys.CUSTOMER_PERSPECTIVE,
			EdgeBridgeProperties.getCustomerPerspective()
		);

		// Analytics original implementation: AppID is only populated if it passes `StringUtils.isNullOrEmpty`
		// Note that since AppID is the only property dependent on `contextData`, it being invalid
		// triggers an early exit here; if other metrics are added later, this early exit logic should
		// be updated accordingly.
		String appId = EdgeBridgeProperties.getApplicationIdentifier();
		if (StringUtils.isNullOrEmpty(appId)) {
			return;
		}
		// Access to the `contextData` map
		Map<String, Object> contextDataMap = DataReader.optTypedMap(
			Object.class,
			analyticsData,
			EdgeBridgeConstants.AnalyticsKeys.CONTEXT_DATA,
			new HashMap<>()
		);
		if (contextDataMap.isEmpty()) {
			analyticsData.put(EdgeBridgeConstants.AnalyticsKeys.CONTEXT_DATA, contextDataMap);
		}

		contextDataMap.put(EdgeBridgeConstants.AnalyticsKeys.APPLICATION_IDENTIFIER, appId);
	}
}
