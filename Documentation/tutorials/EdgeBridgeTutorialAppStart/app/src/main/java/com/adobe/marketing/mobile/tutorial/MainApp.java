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
import android.util.Log;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.InvalidInitException;
import com.adobe.marketing.mobile.Lifecycle;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.consent.Consent;

/* Edge Bridge Tutorial - code section (1/2)
import com.adobe.marketing.mobile.edge.bridge.EdgeBridge;
// Edge Bridge Tutorial - code section (1/2) */

//* Edge Bridge Tutorial - remove section (1/2)
import com.adobe.marketing.mobile.Analytics;
import com.adobe.marketing.mobile.Identity;
// Edge Bridge Tutorial - remove section (1/2) */

public class MainApp extends Application {
    private static final String LOG_TAG = "Test Application";

    // TODO: Set the Environment File ID from your mobile property configured in Data Collection UI
    private final String ENVIRONMENT_FILE_ID = "";

    @Override
    public void onCreate() {
        super.onCreate();

        MobileCore.setApplication(this);
        MobileCore.setLogLevel(LoggingMode.VERBOSE);

        MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);

        try {
            // Register AEP extensions
            Assurance.registerExtension();
            Lifecycle.registerExtension();
            Consent.registerExtension();
            Edge.registerExtension();
            com.adobe.marketing.mobile.edge.identity.Identity.registerExtension();

            /* Edge Bridge Tutorial - code section (2/2)
            EdgeBridge.registerExtension();
            // Edge Bridge Tutorial - code section (2/2) */

            //* Edge Bridge Tutorial - remove section (2/2)
            com.adobe.marketing.mobile.Identity.registerExtension();
            Analytics.registerExtension();
            // Edge Bridge Tutorial - remove section (2/2) */

            // Once all the extensions are registered, call MobileCore.start(...) to start processing the events
            MobileCore.start(new AdobeCallback() {

                @Override
                public void call(Object o) {
                    Log.d(LOG_TAG, "AEP Mobile SDK is initialized");

                }
            });
        } catch (InvalidInitException e) {
            e.printStackTrace();
        }

    }
}
