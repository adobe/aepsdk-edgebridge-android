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

package com.adobe.marketing.mobile.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import androidx.test.platform.app.InstrumentationRegistry;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.MobileCoreHelper;
import com.adobe.marketing.mobile.services.HttpConnecting;
import com.adobe.marketing.mobile.services.HttpMethod;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.services.ServiceProviderHelper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class FunctionalTestHelper {

	private static final String TAG = "FunctionalTestHelper";

	private static final FunctionalTestNetworkService testNetworkService = new FunctionalTestNetworkService();
	private static Application defaultApplication;

	// List of Shared Preferences file names to delete before each test run
	private static final List<String> knownDatastores = new ArrayList<String>();

	static {
		knownDatastores.add("AdobeMobile_ConfigState"); // Configuration
		knownDatastores.add("EdgeDataStorage"); // Edge
		knownDatastores.add("com.adobe.edge.identity"); // Edge Identity
	}

	// List of threads to wait for after test execution
	private static final List<String> sdkThreadPrefixes = new ArrayList<String>();

	static {
		sdkThreadPrefixes.add("pool"); // used for threads that execute the listeners code
		sdkThreadPrefixes.add("ADB"); // module internal threads
	}

	/**
	 * {@code TestRule} which sets up the MobileCore for testing before each test execution, and
	 * tearsdown the MobileCore after test execution.
	 *
	 * To use, add the following to your test class:
	 * <pre>
	 * 	&#064;Rule
	 * 	public FunctionalTestHelper.SetupCoreRule coreRule = new FunctionalTestHelper.SetupCoreRule();
	 * </pre>
	 */
	public static class SetupCoreRule implements TestRule {

		@Override
		public Statement apply(final Statement base, final Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					if (defaultApplication == null) {
						Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
						defaultApplication = Instrumentation.newApplication(CustomApplication.class, context);
					}

					//					MobileCoreHelper.setCore(null);
					MobileCoreHelper.resetSDK();
					ServiceProvider.getInstance().setNetworkService(testNetworkService);
					MobileCore.setLogLevel(LoggingMode.VERBOSE);
					MobileCore.setApplication(defaultApplication);
					clearAllDatastores();
					MobileCore.log(LoggingMode.DEBUG, "SetupCoreRule", "Execute '" + description.getMethodName() + "'");

					try {
						base.evaluate();
					} catch (Throwable e) {
						MobileCore.log(LoggingMode.DEBUG, "SetupCoreRule", "Wait after test failure.");
						throw e; // rethrow test failure
					} finally {
						// After test execution
						MobileCore.log(
							LoggingMode.DEBUG,
							"SetupCoreRule",
							"Finished '" + description.getMethodName() + "'"
						);
						waitForThreads(5000); // wait to allow thread to run after test execution
						//						MobileCoreHelper.shutdownCore();
						//						MobileCoreHelper.setCore(null);
						MobileCoreHelper.resetSDK();
						resetTestExpectations();
						resetServiceProvider();
					}
				}
			};
		}
	}

	public static class LogOnErrorRule implements TestRule {

		@Override
		public Statement apply(final Statement base, final Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					try {
						base.evaluate();
					} catch (Throwable t) {
						throw new Throwable(collectLogCat(description.getMethodName()), t);
					}
				}
			};
		}
	}

	/**
	 * Get the LogCat logs
	 * @return
	 */
	private static String collectLogCat(final String methodName) {
		Process process;
		StringBuilder log = new StringBuilder();

		try {
			// Setting to just last 50 lines as logs are passed as Throwable stack trace which
			// has a line limit. The SDK logs have many multi-line entries which blow up the logs quickly
			// If the log string is too long, it can crash the Throwable call.
			process = Runtime.getRuntime().exec("logcat -t 50 -d AdobeExperienceSDK:V TestRunner:I Hermetic:V *:S");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
			boolean ignoreLines = false; // "started" line may not be in last 50 lines

			while ((line = bufferedReader.readLine()) != null) {
				if (ignoreLines && line.matches(".*started: " + methodName + ".*")) {
					ignoreLines = false;
				}

				if (!ignoreLines) {
					log.append(line).append("\n");
				}
			}
		} catch (IOException e) {
			// ignore
		}

		return log.toString();
	}

	/**
	 * Waits for all the known SDK threads to finish or fails the test after timeoutMillis if some of them are still running
	 * when the timer expires. If timeoutMillis is 0, a default timeout will be set = 1000ms
	 *
	 * @param timeoutMillis max waiting time
	 */
	private static void waitForThreads(final int timeoutMillis) {
		int TEST_DEFAULT_TIMEOUT_MS = 1000;
		int TEST_DEFAULT_SLEEP_MS = 50;
		int TEST_INITIAL_SLEEP_MS = 100;

		long startTime = System.currentTimeMillis();
		int timeoutTestMillis = timeoutMillis > 0 ? timeoutMillis : TEST_DEFAULT_TIMEOUT_MS;
		int sleepTime = Math.min(timeoutTestMillis, TEST_DEFAULT_SLEEP_MS);

		sleep(TEST_INITIAL_SLEEP_MS);
		Set<Thread> threadSet = getEligibleThreads();

		while (threadSet.size() > 0 && ((System.currentTimeMillis() - startTime) < timeoutTestMillis)) {
			MobileCore.log(
				LoggingMode.DEBUG,
				TAG,
				"waitForThreads - Still waiting for " + threadSet.size() + " thread(s)"
			);

			for (Thread t : threadSet) {
				MobileCore.log(
					LoggingMode.DEBUG,
					TAG,
					"waitForThreads - Waiting for thread " + t.getName() + " (" + t.getId() + ")"
				);
				boolean done = false;
				boolean timedOut = false;

				while (!done && !timedOut) {
					if (
						t.getState().equals(Thread.State.TERMINATED) ||
						t.getState().equals(Thread.State.TIMED_WAITING) ||
						t.getState().equals(Thread.State.WAITING)
					) {
						//Cannot use the join() API since we use a cached thread pool, which
						//means that we keep idle threads around for 60secs (default timeout).
						done = true;
					} else {
						//blocking
						sleep(sleepTime);
						timedOut = (System.currentTimeMillis() - startTime) > timeoutTestMillis;
					}
				}

				if (timedOut) {
					MobileCore.log(
						LoggingMode.DEBUG,
						TAG,
						"waitForThreads - Timeout out waiting for thread " + t.getName() + " (" + t.getId() + ")"
					);
				} else {
					MobileCore.log(
						LoggingMode.DEBUG,
						TAG,
						"waitForThreads - Done waiting for thread " + t.getName() + " (" + t.getId() + ")"
					);
				}
			}

			threadSet = getEligibleThreads();
		}

		MobileCore.log(LoggingMode.DEBUG, TAG, "waitForThreads - All known SDK threads are terminated.");
	}

	/**
	 * Retrieves all the known SDK threads that are still running
	 * @return set of running tests
	 */
	private static Set<Thread> getEligibleThreads() {
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Set<Thread> eligibleThreads = new HashSet<Thread>();

		for (Thread t : threadSet) {
			if (
				isAppThread(t) &&
				!t.getState().equals(Thread.State.WAITING) &&
				!t.getState().equals(Thread.State.TERMINATED) &&
				!t.getState().equals(Thread.State.TIMED_WAITING)
			) {
				eligibleThreads.add(t);
			}
		}

		return eligibleThreads;
	}

	/**
	 * Checks if current thread is not a daemon and its name starts with one of the known SDK thread names specified here
	 * {@link #sdkThreadPrefixes}
	 *
	 * @param t current thread to verify
	 * @return true if it is a known thread, false otherwise
	 */
	private static boolean isAppThread(final Thread t) {
		if (t.isDaemon()) {
			return false;
		}

		for (String prefix : sdkThreadPrefixes) {
			if (t.getName().startsWith(prefix)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Resets the network and event test expectations.
	 */
	public static void resetTestExpectations() {
		MobileCore.log(
			LoggingMode.DEBUG,
			TAG,
			"Resetting functional test expectations for events and network requests"
		);
		testNetworkService.reset();
	}

	// ---------------------------------------------------------------------------------------------
	// Network Test Helpers
	// ---------------------------------------------------------------------------------------------

	/**
	 * Set a custom network response to an Edge network request.
	 * @param url the url string for which to return the response
	 * @param method the HTTP method for which to return the response
	 * @param responseConnection the network response to be returned when a request matching the
	 *                           {@code url} and {@code method} is received. If null is provided,
	 *                           a default '200' response is used.
	 */
	public static void setNetworkResponseFor(
		final String url,
		final HttpMethod method,
		final HttpConnecting responseConnection
	) {
		testNetworkService.setResponseConnectionFor(new TestableNetworkRequest(url, method), responseConnection);
	}

	/**
	 * Set a network request expectation.
	 * @param url the url string for which to set the expectation
	 * @param method the HTTP method for which to set the expectation
	 * @param expectedCount how many times a request with this {@code url} and {@code method} is expected to be sent
	 */
	public static void setExpectationNetworkRequest(
		final String url,
		final HttpMethod method,
		final int expectedCount
	) {
		testNetworkService.setExpectedNetworkRequest(new TestableNetworkRequest(url, method), expectedCount);
	}

	/**
	 * Asserts that the correct number of network requests were being sent, based on the previously set expectations.
	 * @throws InterruptedException
	 * @see #setExpectationNetworkRequest(String, HttpMethod, int)
	 */
	public static void assertNetworkRequestCount() throws InterruptedException {
		Map<TestableNetworkRequest, ADBCountDownLatch> expectedNetworkRequests = testNetworkService.getExpectedNetworkRequests();

		if (expectedNetworkRequests.isEmpty()) {
			fail(
				"There are no network request expectations set, use this API after calling setExpectationNetworkRequest"
			);
			return;
		}

		for (Map.Entry<TestableNetworkRequest, ADBCountDownLatch> expectedRequest : expectedNetworkRequests.entrySet()) {
			boolean awaitResult = expectedRequest.getValue().await(15, TimeUnit.SECONDS);
			assertTrue(
				"Time out waiting for network request with URL '" +
				expectedRequest.getKey().getUrl() +
				"' and method '" +
				expectedRequest.getKey().getMethod().name() +
				"'",
				awaitResult
			);
			int expectedCount = expectedRequest.getValue().getInitialCount();
			int receivedCount = expectedRequest.getValue().getCurrentCount();
			String message = String.format(
				"Expected %d network requests for URL %s (%s), but received %d",
				expectedCount,
				expectedRequest.getKey().getUrl(),
				expectedRequest.getKey().getMethod(),
				receivedCount
			);
			assertEquals(message, expectedCount, receivedCount);
		}
	}

	/**
	 * Returns the {@link TestableNetworkRequest}(s) sent through the
	 * Core NetworkService, or empty if none was found. Use this API after calling
	 * {@link #setExpectationNetworkRequest(String, HttpMethod, int)} to wait 2 seconds for each request.
	 *
	 * @param url The url string for which to retrieved the network requests sent
	 * @param method the HTTP method for which to retrieve the network requests
	 * @return list of network requests with the provided {@code url} and {@code method}, or empty if none was dispatched
	 * @throws InterruptedException
	 */
	public static List<TestableNetworkRequest> getNetworkRequestsWith(final String url, final HttpMethod method)
		throws InterruptedException {
		return getNetworkRequestsWith(url, method, 2000);
	}

	/**
	 * Returns the {@link TestableNetworkRequest}(s) sent through the
	 * Core NetworkService, or empty if none was found. Use this API after calling
	 * {@link #setExpectationNetworkRequest(String, HttpMethod, int)} to wait for each request.
	 *
	 * @param url The url string for which to retrieved the network requests sent
	 * @param method the HTTP method for which to retrieve the network requests
	 * @param timeoutMillis how long should this method wait for the expected network requests, in milliseconds
	 * @return list of network requests with the provided {@code url} and {@code command}, or empty if none was dispatched
	 * @throws InterruptedException
	 */
	public static List<TestableNetworkRequest> getNetworkRequestsWith(
		final String url,
		final HttpMethod method,
		final int timeoutMillis
	) throws InterruptedException {
		TestableNetworkRequest networkRequest = new TestableNetworkRequest(url, method);

		if (testNetworkService.isNetworkRequestExpected(networkRequest)) {
			assertTrue(
				"Time out waiting for network request(s) with URL '" +
				networkRequest.getUrl() +
				"' and method '" +
				networkRequest.getMethod().name() +
				"'",
				testNetworkService.awaitFor(networkRequest, timeoutMillis)
			);
		} else {
			sleep(timeoutMillis);
		}

		return testNetworkService.getReceivedNetworkRequestsMatching(networkRequest);
	}

	/**
	 * Create a network response to be used when calling {@link #setNetworkResponseFor(String, HttpMethod, HttpConnecting)}.
	 * @param responseString the network response string, returned by {@link HttpConnecting#getInputStream()}
	 * @param code the HTTP status code, returned by {@link HttpConnecting#getResponseCode()}
	 * @return an {@link HttpConnecting} object
	 * @see #setNetworkResponseFor(String, HttpMethod, HttpConnecting)
	 */
	public static HttpConnecting createNetworkResponse(final String responseString, final int code) {
		return createNetworkResponse(responseString, null, code, null, null);
	}

	/**
	 * Create a network response to be used when calling {@link #setNetworkResponseFor(String, HttpMethod, HttpConnecting)}.
	 * @param responseString the network response string, returned by {@link HttpConnecting#getInputStream()}
	 * @param errorString the network error string, returned by {@link HttpConnecting#getErrorStream()}
	 * @param code the HTTP status code, returned by {@link HttpConnecting#getResponseCode()}
	 * @param responseMessage the network response message, returned by {@link HttpConnecting#getResponseMessage()}
	 * @param propertyMap the network response header map, returned by {@link HttpConnecting#getResponsePropertyValue(String)}
	 * @return an {@link HttpConnecting} object
	 * @see #setNetworkResponseFor(String, HttpMethod, HttpConnecting)
	 */
	public static HttpConnecting createNetworkResponse(
		final String responseString,
		final String errorString,
		final int code,
		final String responseMessage,
		final Map<String, String> propertyMap
	) {
		return new HttpConnecting() {
			@Override
			public InputStream getInputStream() {
				if (responseString != null) {
					return new ByteArrayInputStream(responseString.getBytes(StandardCharsets.UTF_8));
				}

				return null;
			}

			@Override
			public InputStream getErrorStream() {
				if (errorString != null) {
					return new ByteArrayInputStream(errorString.getBytes(StandardCharsets.UTF_8));
				}

				return null;
			}

			@Override
			public int getResponseCode() {
				return code;
			}

			@Override
			public String getResponseMessage() {
				return responseMessage;
			}

			@Override
			public String getResponsePropertyValue(String responsePropertyKey) {
				if (propertyMap != null) {
					return propertyMap.get(responsePropertyKey);
				}

				return null;
			}

			@Override
			public void close() {}
		};
	}

	/**
	 * Pause test execution for the given {@code milliseconds}
	 * @param milliseconds the time to sleep the current thread.
	 */
	public static void sleep(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Dummy Application for the test instrumentation
	 */
	public static class CustomApplication extends Application {

		public CustomApplication() {}
	}

	/**
	 * Get bundled file from Assets folder as {@code InputStream}.
	 * Asset folder location "src/androidTest/assets/".
	 * @param filename file name of the asset
	 * @return an {@code InputStream} to the file asset, or null if the file could not be opened or
	 * no Application is set.
	 * @throws IOException
	 */
	public static InputStream getAsset(final String filename) throws IOException {
		if (defaultApplication == null) {
			return null;
		}

		AssetManager assetManager = defaultApplication.getApplicationContext().getAssets();
		return assetManager.open(filename);
	}

	/**
	 * Reset the {@link ServiceProvider} by clearing all files under the application cache folder,
	 * instantiate new instances of each service provider, and reset the app instance
	 */
	private static void resetServiceProvider() {
		ServiceProviderHelper.cleanCacheDir();
		ServiceProviderHelper.resetServiceProvider();
	}

	/**
	 * Clear shared preferences.
	 */
	private static void clearAllDatastores() {
		final Application application = defaultApplication;

		if (application == null) {
			fail("FunctionalTestHelper - Unable to clear datastores. Application is null, fast failing the test case.");
		}

		final Context context = application.getApplicationContext();

		if (context == null) {
			fail("FunctionalTestHelper - Unable to clear datastores. Context is null, fast failing the test case.");
		}

		for (String datastore : knownDatastores) {
			SharedPreferences sharedPreferences = context.getSharedPreferences(datastore, Context.MODE_PRIVATE);

			if (sharedPreferences == null) {
				fail(
					"FunctionalTestHelper - Unable to clear datastores. sharedPreferences is null, fast failing the test case."
				);
			}

			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.clear();
			editor.apply();
		}
	}
}
