# Adobe Experience Platform Edge Bridge for Android
The AEP Edge Bridge mobile extension provides seamless routing of data to the Adobe Experience Platform Edge Network for existing SDK implementations. For applications which already make use of the [`MobileCore.trackAction`](https://aep-sdks.gitbook.io/docs/foundation-extensions/mobile-core/mobile-core-api-reference#trackaction) and/or [`MobileCore.trackState`](https://aep-sdks.gitbook.io/docs/foundation-extensions/mobile-core/mobile-core-api-reference#trackstate) APIs to send data to Adobe Analytics, this extension will automatically route those API calls to the Edge Network, making the data available for mapping to a user's XDM schema using the [Data Prep for Data Collection](https://experienceleague.adobe.com/docs/experience-platform/data-prep/home.html).

> **Note**
> It is recommended to send well formatted XDM data directly from an application using the [Edge.sendEvent](https://aep-sdks.gitbook.io/docs/foundation-extensions/experience-platform-extension/edge-network-api-reference#sendevent) API instead of using the `trackState` and `trackAction` APIs with the Edge Bridge extension. However, in cases where it is not feasible to refactor the application, the Edge Bridge extension is available as a drop-in solution to send `trackState` and `trackAction` data to the Edge Network.
>
>  For new implementations of the SDK, it it highly recommended to send XDM data directly using the [`Edge.sendEvent`](https://aep-sdks.gitbook.io/docs/foundation-extensions/experience-platform-extension/edge-network-api-reference#sendevent) API.
>

## Before starting

### Install AEP Edge extension

The Adobe Experience Platform Edge Bridge extension requires the Adobe Experience Platform Edge Network extension in order to operate. As a first step install and configure the [AEP Edge](https://aep-sdks.gitbook.io/docs/foundation-extensions/experience-platform-extension) extension, then continue with the steps below.

## Add the AEP Edge Bridge extension to an app

### Download and import the Edge Bridge extension

> **Note**
> The AEP Edge Bridge extension does not have a corresponding extension in the Data Collection UI. No changes to a Data Collection mobile property are required to use the AEP Edge Bridge extension.

### Install extension

### Java

1. Add the Mobile Core and Edge extensions to your project using the app's Gradle file.

```java
implementation 'com.adobe.marketing.mobile:core:1.+'
implementation 'com.adobe.marketing.mobile:edge:1.+'
implementation 'com.adobe.marketing.mobile:edgebridge:1.+'
implementation 'com.adobe.marketing.mobile:edgeidentity:1.+'
```

2. Import the Mobile Core and Edge extensions in your Application class.

```java
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.edge.bridge.EdgeBridge;
import com.adobe.marketing.mobile.edge.identity.Identity;
```

3. Register the Edge Bridge and Identity for Edge Extension with MobileCore:

```java
public class MobileApp extends Application {

    @Override
    public void onCreate() {
      super.onCreate();
      MobileCore.setApplication(this);
      try {
        Edge.registerExtension();
        EdgeBridge.registerExtension();
        Identity.registerExtension();
        // register other extensions
        MobileCore.start(new AdobeCallback () {
            @Override
            public void call(Object o) {
                MobileCore.configureWithAppID("yourEnvironmentFileID");
            }
        });      
      } catch (Exception e) {
        ...
      }
    }
}
```
