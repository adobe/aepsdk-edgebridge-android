# Adobe Experience Platform Edge Bridge for Android

## API Reference

| APIs                                           |
| ---------------------------------------------- |
| [extensionVersion](#extensionversion)	         |
| [registerExtension](#registerextension)	     |

------

### extensionVersion
Returns the version of the Edge Bridge extension.

#### Java

##### Syntax
```java
public static String extensionVersion()
```

##### Examples
```java
String extensionVersion = EdgeBridge.extensionVersion();
```

#### Kotlin

##### Examples
```kotlin
val extensionVersion = EdgeBridge.extensionVersion()
```

------

### registerExtension
Registers the Edge Bridge extension with the Mobile Core extension.

> **Warning**  
> Deprecated as of 2.0.0. Use [MobileCore.registerExtensions API](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/api-reference.md) instead.

#### Java

##### Syntax
```java
public static void registerExtension()
```

##### Examples
```java
import com.adobe.marketing.mobile.edge.bridge.EdgeBridge;

...

EdgeBridge.registerExtension();
```

#### Kotlin

##### Examples
```kotlin
import com.adobe.marketing.mobile.edge.bridge.EdgeBridge

...

EdgeBridge.registerExtension()
```

------