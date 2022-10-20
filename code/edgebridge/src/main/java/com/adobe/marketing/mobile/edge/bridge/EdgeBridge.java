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

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import java.util.Map;

/**
 * The Adobe Experience Platform Edge Bridge mobile extension provides functionality to facilitate
 * sending data from a mobile application to the Adobe Edge Network.
 * <p>
 * This extension supports the following use-cases:
 * <ul>
 * <li>Enables forwarding of {@link MobileCore#trackState(String, Map)}
 * and {@link MobileCore#trackAction(String, Map)} calls to the Adobe Edge Network. The configured
 * Data Collection datastream for the mobile application can define a mapping of the track event's
 * {@code contextdata} to an XDM schema using Data Prep for Data Collection.</li>
 * </ul>
 */
public class EdgeBridge {

	private EdgeBridge() {}

	/**
	 * Returns the version of the Edge Bridge extension.
	 * @return the version of the Edge Bridge extension
	 */
	public static String extensionVersion() {
		return EdgeBridgeConstants.EXTENSION_VERSION;
	}

	/**
	 * Registers the Edge Bridge extension with the Mobile Core.
	 * This method should be called before calling {@link MobileCore#start(AdobeCallback)}.
	 */
	public static void registerExtension() {
		MobileCore.registerExtension(
			EdgeBridgeExtension.class,
			new ExtensionErrorCallback<ExtensionError>() {
				@Override
				public void error(ExtensionError extensionError) {
					MobileCore.log(
						LoggingMode.ERROR,
						EdgeBridgeConstants.LOG_TAG,
						"There was an error registering the Edge Bridge extension: " + extensionError.getErrorName()
					);
				}
			}
		);
	}
}
