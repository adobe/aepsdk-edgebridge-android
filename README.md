# Adobe Experience Platform Edge Bridge Mobile Extension

## About this project

The Adobe Experience Platform Edge Bridge mobile extension enables forwarding of Analytics track events to Experience Platform Edge Network when using the [Experience Platform Mobile SDK](https://developer.adobe.com/client-sdks) and the Edge Network extension. The complete ingestion process occurs in two main steps:
1. Edge Bridge forwards the track event data to Edge Network in a generic data format. 
2. The generic data is converted into the Experience Data Model (XDM) format, which is a standard experience-driven data schema for Adobe and partner solutions. 
   * This conversion mapping can be set in the Data Collection datastream associated with the application's mobile property using [Data Prep for Data Collection](https://experienceleague.adobe.com/docs/platform-learn/data-collection/edge-network/data-prep.html).

### Installation

Integrate the Edge Bridge mobile extension into your app by following the [getting started guide](Documentation/getting-started.md).

### Development

**Open the project**

To open and run the project, open the `code/settings.gradle` file in Android Studio

### Development

#### Run the test application

To configure and run the test app for this project, follow the [getting started guide for the test app](Documentation/getting-started-test-app.md).

#### Code format

This project uses the code formatting tools [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle) with [Prettier](https://prettier.io/). Formatting is applied when the project is built from Gradle and is checked when changes are submitted to the CI build system.

Prettier requires [Node version](https://nodejs.org/en/download/releases/) 10+
To enable the Git pre-commit hook to apply code formatting on each commit, run the following to update the project's git config `core.hooksPath`:
```
make init
```

## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Core extensions](https://github.com/adobe/aepsdk-core-android)                                    | The Mobile Core represents the foundation of the Adobe Experience Platform Mobile SDK. |
| [Edge Network extension](https://github.com/adobe/aepsdk-edge-android) | The Edge Network extension allows you to send data to the Adobe Edge Network from a mobile application. |
| [Adobe Experience Platform Android sample app](https://github.com/adobe/aepsdk-sample-app-android) | Contains a fully implemented Android sample app using the Experience Platform SDKs.                 |

## Documentation

Information about Adobe Experience Platform Edge Bridge's implementation, API usage, and architecture can be found in the [Documentation](Documentation) directory.

Learn more about Edge Bridge and all other Mobile SDK extensions in the official [Adobe Experience Platform Mobile SDK documentation](https://developer.adobe.com/client-sdks/documentation/edge-network/).

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
