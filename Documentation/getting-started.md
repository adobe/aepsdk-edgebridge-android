# Adobe Experience Platform Edge Bridge for Android
The Adobe Experience Platform Edge Bridge mobile extension provides seamless routing of data to the Experience Platform Edge Network for existing Adobe Analytics mobile SDK implementations. Applications using the `MobileCore.trackAction` and/or `MobileCore.trackState` APIs to send data to Adobe Analytics can use Edge Bridge to automatically route those events to the Edge Network, which makes the data available for mapping to an Experience Data Model (XDM) schema using the [Data Prep for Data Collection](https://experienceleague.adobe.com/docs/experience-platform/data-prep/home.html).

> **Note**  
> For new implementations of the Adobe Experience Platform SDK, it's highly recommended to send event data that is already XDM formatted using the `Edge.sendEvent` API instead of converting events from the `MobileCore.trackState` and `MobileCore.trackAction` APIs using Edge Bridge. 
> 
> However, in cases where it is not easy to refactor an existing application, the Edge Bridge extension exists as a drop-in solution to send converted `trackState` and `trackAction` events to the Edge Network.

## Before starting

Edge Bridge has the following peer dependencies, which must be installed with it:
- [Mobile Core](https://github.com/adobe/aepsdk-core-android#readme) (installed by default)
- [Edge Network](https://github.com/adobe/aepsdk-edge-android#readme)
- [Identity for Edge Network](https://github.com/adobe/aepsdk-edgeidentity-android#readme) (a dependency for Edge Network)

## Add Edge Bridge to an app

### Download and import Edge Bridge

> **Note**  
> Experience Platform Edge Bridge does not have a corresponding extension card in the Data Collection UI; no changes to a Data Collection mobile property are required to use Edge Bridge.

### Install the extension

### Java

1. Add the Mobile Core and Edge extensions to your project using the app's Gradle file.

```java
implementation 'com.adobe.marketing.mobile:core:2.+'
implementation 'com.adobe.marketing.mobile:edge:2.+'
implementation 'com.adobe.marketing.mobile:edgebridge:2.+'
implementation 'com.adobe.marketing.mobile:edgeidentity:2.+'
```

2. Import the Mobile Core and Edge extensions in your Application class.

```java
import android.app.Application;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.bridge.EdgeBridge;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.services.Log;
import java.util.Arrays; // Only required in the Java example
```

3. Register the Edge Bridge and Identity for Edge Extension with MobileCore:

#### Java
```java
public class MobileApp extends Application {
  // Set up the preferred Environment File ID from your mobile property configured in Data Collection UI
  private final String ENVIRONMENT_FILE_ID = "";

  @Override
  public void onCreate() {
  	super.onCreate();
  	MobileCore.setApplication(this);

  	MobileCore.setLogLevel(LoggingMode.VERBOSE);
  	MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);

  	// Register Adobe extensions
  	MobileCore.registerExtensions(
  		Arrays.asList(Edge.EXTENSION, EdgeBridge.EXTENSION, Identity.EXTENSION),
  		o -> Log.debug("MobileApp", "MobileApp", "Mobile SDK was initialized")
  	);
  }
}
```
#### Kotlin
```kotlin
class MobileApp : Application() {
  // Set up the preferred Environment File ID from your mobile property configured in Data Collection UI
  private var ENVIRONMENT_FILE_ID: String = ""

  override fun onCreate() {
      super.onCreate()

      MobileCore.setApplication(this)
      MobileCore.setLogLevel(LoggingMode.VERBOSE)
      MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID)

      // Register Adobe extensions
      MobileCore.registerExtensions(
          arrayListOf(Edge.EXTENSION, EdgeBridge.EXTENSION, Identity.EXTENSION)
      ) {
          Log.debug("MobileApp", "MobileApp", "Mobile SDK was initialized.")
      }
  }
}
```