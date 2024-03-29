# Getting started with the test app

## Data Collection mobile property prerequisites

The test app needs to be configured with a Data Collection mobile property with the following extensions before it can be used:

* [Mobile Core](https://github.com/adobe/aepsdk-core-android) (installed by default)
* [Edge Network](https://github.com/adobe/aepsdk-edge-android)
* [Identity for Edge Network](https://github.com/adobe/aepsdk-edgeidentity-android)
* [Assurance](https://github.com/adobe/aepsdk-assurance-android)

> **Note**  
> Experience Platform Edge Bridge does not have a corresponding extension card in the Data Collection UI; no changes to a Data Collection mobile property are required to use Edge Bridge.

See [Configure the Edge Network extension in Data Collection UI](https://github.com/adobe/aepsdk-edge-android/blob/main/Documentation/getting-started.md) for instructions on setting up a mobile property.

## Run test application

1. In the test app, set your `ENVIRONMENT_FILE_ID` in `EdgeBridgeApplication.kt`.
2. Select the `app` runnable with the desired emulator and run the program.

## Validation with Assurance

Configure a new Assurance session by setting the Base URL to `testapp://main` and launch Assurance in the demo app by running the following command in your terminal:

```bash
$ adb shell am start -W -a  android.intent.action.VIEW -d "testapp://main?adb_validation_sessionid=ADD_YOUR_SESSION_ID_HERE" com.adobe.marketing.mobile.edge.bridge.testapp
```

> **Note**  
> Replace `ADD_YOUR_SESSION_ID_HERE` with your Assurance session identifier.

Once the connection is established and the events list starts getting populated, you can filter the events for this extension by typing `EdgeBridge` in the `Search Events` search box.