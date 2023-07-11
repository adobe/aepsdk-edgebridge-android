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

public class EdgeBridgeTestConstants {

	private EdgeBridgeTestConstants() {}

	static final String EXTENSION_NAME = "com.adobe.edge.bridge";
	static final String EXTENSION_FRIENDLY_NAME = "Edge Bridge";

	final class EventNames {

		static final String EDGE_BRIDGE_REQUEST = "Edge Bridge Request";

		private EventNames() {}
	}

	final class JsonValues {

		static final String EVENT_TYPE = "analytics.track";

		private JsonValues() {}
	}
}
