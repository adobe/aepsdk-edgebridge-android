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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import android.app.Application;
import android.content.Context;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EdgeBridgeTest {

	@Mock
	Application mockApplication;

	@Mock
	Context mockContext;

	@Before
	public void setup() {
		Mockito.reset(mockApplication);
		Mockito.reset(mockContext);
	}

	@Test
	public void testRegisterExtension_deprecated() {
		try (MockedStatic<MobileCore> mobileCoreMockedStatic = Mockito.mockStatic(MobileCore.class)) {
			EdgeBridge.registerExtension();
			mobileCoreMockedStatic.verify(
				() ->
					MobileCore.registerExtension((Class<? extends Extension>) any(), any(ExtensionErrorCallback.class)),
				times(1)
			);
		}
	}

	@Test
	public void testExtensionVersion_verifyModuleVersionInPropertiesFile_asEqual() {
		Properties properties = loadProperties("../gradle.properties");

		assertNotNull(EdgeBridge.extensionVersion());
		assertFalse(EdgeBridge.extensionVersion().isEmpty());

		String moduleVersion = properties.getProperty("moduleVersion");
		assertNotNull(moduleVersion);
		assertFalse(moduleVersion.isEmpty());

		assertEquals(
			String.format(
				"Expected version to match in gradle.properties (%s) and extensionVersion API (%s)",
				moduleVersion,
				EdgeBridge.extensionVersion()
			),
			moduleVersion,
			EdgeBridge.extensionVersion()
		);
	}

	private Properties loadProperties(final String filepath) {
		Properties properties = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(filepath);

			properties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return properties;
	}
}
