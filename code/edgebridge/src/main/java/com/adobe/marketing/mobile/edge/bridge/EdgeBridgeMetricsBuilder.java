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

import static com.adobe.marketing.mobile.edge.bridge.EdgeBridgeConstants.LOG_TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.adobe.marketing.mobile.services.AppState;
import com.adobe.marketing.mobile.services.DeviceInforming;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.StringUtils;

public class EdgeBridgeMetricsBuilder {

	private static final String LOG_SOURCE = "EdgeBridgeMetricsBuilder";

	@NonNull static String getCustomerPerspective() {
		if (ServiceProvider.getInstance().getAppContextService() == null) {
			Log.trace(
				LOG_TAG,
				LOG_SOURCE,
				"getCustomerPerspective - Unable to access platform services to retrieve foreground/background state. Defaulting customer perspective to foreground."
			);
			return EdgeBridgeConstants.AnalyticsValues.APP_STATE_FOREGROUND;
		}

		AppState appState = ServiceProvider.getInstance().getAppContextService().getAppState();
		if (appState == AppState.BACKGROUND) {
			return EdgeBridgeConstants.AnalyticsValues.APP_STATE_BACKGROUND;
		}
		return EdgeBridgeConstants.AnalyticsValues.APP_STATE_FOREGROUND;
	}

	/**
	 * Generates the Application ID string from Application name, version and version code
	 *
	 * @return string representation of the Application ID
	 */
	@Nullable static String getApplicationIdentifier() {
		DeviceInforming deviceInfoService = ServiceProvider.getInstance().getDeviceInfoService();
		if (deviceInfoService == null) {
			return null;
		}

		final String applicationName = deviceInfoService.getApplicationName();
		final String applicationVersion = deviceInfoService.getApplicationVersion();
		final String applicationVersionCode = deviceInfoService.getApplicationVersionCode();
		return String.format(
			"%s%s%s",
			applicationName,
			!StringUtils.isNullOrEmpty(applicationVersion) ? String.format(" %s", applicationVersion) : "",
			!StringUtils.isNullOrEmpty(applicationVersionCode) ? String.format(" (%s)", applicationVersionCode) : ""
		);
	}
}
