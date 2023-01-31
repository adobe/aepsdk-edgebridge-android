# Adobe Experience Platform Edge Bridge Mobile Extension


## About this project

The Adobe Experience Platform Edge Bridge mobile extension enables forwarding of Analytics track events to Experience Platform Edge Network when using the [Experience Platform Mobile SDK](https://developer.adobe.com/client-sdks) and the Edge Network extension. Once forwarded, the track event data is in a generic data format that can be converted into an Experience Data Model (XDM) format, which is a standard experience-driven data schema for Adobe and partner solutions. This conversion mapping can be set in the Data Collection datastream associated with the application's mobile property using [Data Prep for Data Collection](https://experienceleague.adobe.com/docs/platform-learn/data-collection/edge-network/data-prep.html).

### Installation

Integrate the Edge Network mobile extension into your app by following the [getting started guide](Documentation/getting-started.md).

### Development

**Open the project**

To open and run the project, open the `code/settings.gradle` file in Android Studio

**Run demo application**

To configure and run the test app for this project, follow the [getting started guide for the test app](Documentation/getting-started-test-app.md).

**View the platform events with Assurance**

Configure a new Assurance session by setting the Base URL to `testapp://main` and launch Assurance in the demo app by running the following command in your terminal:

```bash
$ adb shell am start -W -a  android.intent.action.VIEW -d "testapp://main?adb_validation_sessionid=ADD_YOUR_SESSION_ID_HERE" com.adobe.marketing.mobile.edge.bridge.testapp
```

Note: replace ADD_YOUR_SESSION_ID_HERE with your Assurance session identifier.

Once the connection is established and the events list starts getting populated, you can filter the events for this extension by typing `EdgeBridge` in the `Search Events` search box.

### Code Format

This project uses the code formatting tools [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle) with [Prettier](https://prettier.io/) and [ktlint](https://github.com/pinterest/ktlint). Formatting is applied when the project is built from Gradle and is checked when changes are submitted to the CI build system.

Prettier requires [Node version](https://nodejs.org/en/download/releases/) 10+

To enable the Git pre-commit hook to apply code formatting on each commit, run the following to update the project's git config `core.hooksPath`:
```
make init
```

## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Edge Network extension](https://github.com/adobe/aepsdk-edge-android) | The Edge Network extension allows you to send data to the Adobe Edge Network from a mobile application. |
| [Adobe Experience Platform sample app for Android](https://github.com/adobe/aepsdk-sample-app-android) | Contains a fully implemented Android sample app using the Experience Platform SDKs.                 |

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
