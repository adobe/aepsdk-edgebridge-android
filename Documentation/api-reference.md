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

------

### registerExtension
Registers the Edge Bridge extension with the Mobile Core extension.

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

------