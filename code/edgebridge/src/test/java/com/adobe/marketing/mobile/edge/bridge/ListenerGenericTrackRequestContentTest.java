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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MobileCore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class ListenerGenericTrackRequestContentTest {

	private static final String eventName = "Track request content event";
	private static final String eventType = EdgeBridgeConstants.EventType.GENERIC_TRACK;
	private static final String eventSource = EdgeBridgeConstants.EventSource.REQUEST_CONTENT;

	@Mock
	private EdgeBridgeExtension mockExtension;

	private ListenerGenericTrackRequestContent listener;
	private ExecutorService testExecutor;

	@Before
	public void setup() {
		mockExtension = Mockito.mock(EdgeBridgeExtension.class);
		testExecutor = Executors.newSingleThreadExecutor();
		doReturn(testExecutor).when(mockExtension).getExecutor();
		MobileCore.start(null);
		listener = spy(new ListenerGenericTrackRequestContent(null, eventType, eventSource));
	}

	@Test
	public void testHear() throws InterruptedException {
		// setup
		Event event = new Event.Builder(eventName, eventType, eventSource).build();
		Mockito.doReturn(mockExtension).when(listener).getEdgeBridgeExtension();

		// test
		listener.hear(event);

		// verify
		testExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
		verify(mockExtension, times(1)).handleTrackRequest(event);
	}

	@Test
	public void testHear_WhenParentExtensionNull() throws InterruptedException {
		// setup
		Event event = new Event.Builder(eventName, eventType, eventSource).build();
		doReturn(null).when(listener).getEdgeBridgeExtension();

		// test
		listener.hear(event);

		// verify
		testExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
		verify(mockExtension, times(0)).handleTrackRequest(any(Event.class));
	}

	@Test
	public void testHear_WhenEventNull() throws InterruptedException {
		// setup
		doReturn(mockExtension).when(listener).getEdgeBridgeExtension();

		// test
		listener.hear(null);

		// verify
		testExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
		verify(mockExtension, times(0)).handleTrackRequest(any(Event.class));
	}
}
