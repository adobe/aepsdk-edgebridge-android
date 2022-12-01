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

import androidx.annotation.NonNull;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.util.DataReader;
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

	@Override
	protected String getName() {
		return EdgeBridgeConstants.EXTENSION_NAME;
	}

	@Override
	protected String getFriendlyName() {
		return EdgeBridgeConstants.FRIENDLY_NAME;
	}

	@Override
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

		dispatchTrackRequest(eventData, event.getTimestamp());
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

		dispatchTrackRequest(detail, event.getTimestamp());
	}

	/**
	 * Helper to create and dispatch an experience event.
	 * @param data map containing free-form data to send to Edge Network
	 * @param timestamp timestamp of Event
	 */
	private void dispatchTrackRequest(final Map<String, Object> data, final long timestamp) {
		Map<String, Object> xdmData = new HashMap<>();
		xdmData.put("eventType", EdgeBridgeConstants.JsonValues.EVENT_TYPE);
		xdmData.put("timestamp", TimeUtils.getIso8601Date(new Date(timestamp), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

		Map<String, Object> eventData = new HashMap<>();
		eventData.put("xdm", xdmData);
		eventData.put("data", data);

		final Event event = new Event.Builder(
			EdgeBridgeConstants.EventNames.EDGE_BRIDGE_REQUEST,
			EventType.EDGE,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(eventData)
			.build();

		getApi().dispatch(event);
	}

	private boolean isNullOrEmpty(final Map map) {
		return map == null || map.isEmpty();
	}
}
