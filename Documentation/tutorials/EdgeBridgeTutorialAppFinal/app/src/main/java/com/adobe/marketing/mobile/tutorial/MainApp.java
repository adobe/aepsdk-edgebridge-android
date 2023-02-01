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

package com.adobe.marketing.mobile.tutorial;

import android.app.Application;
// Edge Bridge Tutorial - code section (1/2) */
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.Lifecycle;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
//* Edge Bridge Tutorial - code section (1/2)
import com.adobe.marketing.mobile.edge.bridge.EdgeBridge;
import com.adobe.marketing.mobile.edge.consent.Consent;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.services.Log;
import java.util.ArrayList;
import java.util.List;

public class MainApp extends Application {

	public static final String LOG_TAG = "EdgeBridgeTutorialApp";
	private static final String LOG_SOURCE = "MainApp";

	// TODO: Set the Environment File ID from your mobile property configured in Data Collection UI
	private final String ENVIRONMENT_FILE_ID = "";

	@Override
	public void onCreate() {
		super.onCreate();

		MobileCore.setApplication(this);
		MobileCore.setLogLevel(LoggingMode.VERBOSE);

		MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);

		List<Class<? extends Extension>> extensions = new ArrayList<>();
		extensions.add(Assurance.EXTENSION);
		extensions.add(Consent.EXTENSION);
		extensions.add(Edge.EXTENSION);
		extensions.add(Identity.EXTENSION); // Identity for Edge Network
		extensions.add(Lifecycle.EXTENSION);

		//* Edge Bridge Tutorial - code section (2/2)
		extensions.add(EdgeBridge.EXTENSION);
		// Edge Bridge Tutorial - code section (2/2) */

		// Register Adobe Experience Platform extensions
		MobileCore.registerExtensions(
			extensions,
			o -> Log.debug(LOG_TAG, LOG_SOURCE, "Adobe Experience Platform Mobile SDK initialized.")
		);
	}
}
