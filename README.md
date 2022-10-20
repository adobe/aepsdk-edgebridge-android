# Adobe Experience Platform Edge Bridge Mobile Extension


## About this project

The AEP Edge Bridge mobile extension enables forwarding of Analytics track events to Adobe Edge Network when using the [Adobe Experience Platform SDK](https://aep-sdks.gitbook.io/docs/) and the Edge Network extension. The configured Data Collection datastream for the mobile application can define a mapping of the track event's contextdata to an XDM schema using [Data Prep for Data Collection](https://experienceleague.adobe.com/docs/platform-learn/data-collection/edge-network/data-prep.html).

### Prerequisites

The Edge Bridge extension has the following peer dependencies, which must be installed prior to installing the Edge Bridge Extension:
- [Mobile Core](https://aep-sdks.gitbook.io/docs/foundation-extensions/mobile-core)
- [Edge Network](https://aep-sdks.gitbook.io/docs/foundation-extensions/experience-platform-extension)
- [Identity for Edge Network](https://aep-sdks.gitbook.io/docs/foundation-extensions/identity-for-edge-network) (a dependency for Edge Network)

### Installation

Integrate the Edge Bridge extension into your app by including the following in your gradle file's `dependencies`:

```
implementation 'com.adobe.marketing.mobile:core:1.+'
implementation 'com.adobe.marketing.mobile:edge:1.+'
implementation 'com.adobe.marketing.mobile:edgebridge:1.+'
```

### Development

**Open the project**

To open and run the project, open the `code/settings.gradle` file in Android Studio

**Run demo application**

Once you opened the project in Android Studio (see above), select the `app` runnable and your favorite simulator and run the program.

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
| [AEP Edge Extension](https://aep-sdks.gitbook.io/docs/foundation-extensions/experience-platform-extension) | The AEP Edge extension allows you to send data to the Adobe Experience Platform (AEP) from a mobile application. |
| [AEP SDK Sample App for Android](https://github.com/adobe/aepsdk-sample-app-android) | Contains Android sample app for the AEP SDK.                 |

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
