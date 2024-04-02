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

final class EdgeBridgeConstants {

	private EdgeBridgeConstants() {}

	static final String LOG_TAG = "EdgeBridge";
	static final String EXTENSION_NAME = "com.adobe.edge.bridge";
	static final String FRIENDLY_NAME = "Edge Bridge";
	static final String EXTENSION_VERSION = "3.0.0";

	final class MobileCoreKeys {

		static final String ACTION = "action";
		static final String CONTEXT_DATA = "contextdata";
		static final String STATE = "state";

		private MobileCoreKeys() {}
	}

	final class AnalyticsKeys {

		static final String ADOBE = "__adobe";
		static final String ANALYTICS = "analytics";
		static final String APPLICATION_IDENTIFIER = "a.AppID";
		static final String CONTEXT_DATA = "contextData";
		static final String CUSTOMER_PERSPECTIVE = "cp";
		static final String LINK_NAME = "linkName";
		static final String LINK_TYPE = "linkType";
		static final String PAGE_NAME = "pageName";

		private AnalyticsKeys() {}
	}

	final class AnalyticsValues {

		static final String APP_STATE_BACKGROUND = "background";
		static final String APP_STATE_FOREGROUND = "foreground";
		static final String OTHER = "other";
		static final String PREFIX = "&&";

		private AnalyticsValues() {}
	}

	final class EventNames {

		static final String EDGE_BRIDGE_REQUEST = "Edge Bridge Request";

		private EventNames() {}
	}

	final class JsonValues {

		static final String EVENT_TYPE = "analytics.track";

		private JsonValues() {}
	}
}
