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

import static org.junit.Assert.assertTrue;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobileCoreHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EdgeBridgeTests {

	private EdgeBridgeExtension extension;

	@Before
	public void setup() {
		MobileCoreHelper.resetSDK();
		MobileCore.setApplication(ApplicationProvider.getApplicationContext());
		Context context = ApplicationProvider.getApplicationContext();
	}

	@Test
	public void test_registerExtension() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		MobileCore.setLogLevel(LoggingMode.VERBOSE);
		List<Class<? extends Extension>> extensions = new ArrayList<>();
		extensions.add(EdgeBridge.EXTENSION);
		MobileCore.registerExtensions(extensions, o -> latch.countDown());
		assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
	}
}
