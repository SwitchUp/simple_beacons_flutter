# beacons_plugin

[![pub package](https://img.shields.io/pub/v/beacons_plugin)](https://pub.dev/packages/beacons_plugin)

This plugin is developed to scan nearby iBeacons on both Android iOS. This library makes it easier to scan & range nearby BLE beacons and read their proximity values.

## Android
Your *minSdkVersion* must be at least 19:

```groovy
defaultConfig {
  ...
  minSdkVersion 19
  ...
}
```

*That's it for Android.*

## iOS

In your *AppDelegate.swift* file change it to like this:

```swift
    
    import UIKit
    import Flutter
    import CoreLocation
    
    @UIApplicationMain
    @objc class AppDelegate: FlutterAppDelegate {
    
        let locationManager = CLLocationManager()
    
        override func application(
            _ application: UIApplication,
            didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
        ) -> Bool {
    
            locationManager.requestAlwaysAuthorization()
            GeneratedPluginRegistrant.register(with: self)
    
            return super.application(application, didFinishLaunchingWithOptions: launchOptions)
        }
    }
```

In your *Info.plist* file add following lines:

```swift
    <dict>
      <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
    	<string>App needs location permissions to scan nearby beacons.</string>
    	<key>NSLocationWhenInUseUsageDescription</key>
    	<string>App needs location permissions to scan nearby beacons.</string>
    	<key>NSLocationAlwaysUsageDescription</key>
    	<string>App needs location permissions to scan nearby beacons.</string>
    </dict>
```


## Install
In your pubspec.yaml

```yaml
dependencies:
  beacons_plugin: [LATEST_VERSION]
```

```dart
import 'dart:async';
import 'dart:io' show Platform;
import 'package:flutter/services.dart';
import 'package:beacons_plugin/beacons_plugin.dart';
```

## Ranging Beacons & Setting Up

```dart
    // if you need to monitor also major and minor use the original version and not this fork
    BeaconsPlugin.addRegion("myBeacon", "01022022-f88f-0000-00ae-9605fd9bb620")
        .then((result) {
          print(result);
        });

    if (Platform.isAndroid) {
      // IMPORTANT: initialize will ask for all necessary permissions
      await BeaconsPlugin.initialize();
      await BeaconsPlugin.startMonitoring();
    } else if (Platform.isIOS) {
      await BeaconsPlugin.startMonitoring();
    }
    
```

## Listen To Beacon Scan Results as Stream

```dart
    
    final StreamController<String> beaconEventsController = StreamController<String>.broadcast();
    BeaconsPlugin.listenToBeacons(beaconEventsController);
    
    beaconEventsController.stream.listen(
        (data) {
          if (data.isNotEmpty) {
            setState(() {
              _beaconResult = data;
            });
            print("Beacons DataReceived: " + data);
          }
        },
        onDone: () {},
        onError: (error) {
          print("Error: $error");
        });
```

## Stop Listening to Beacons

```dart
     await BeaconsPlugin.stopMonitoring();
```

## Clear Regions

```dart
    await BeaconsPlugin.clearRegions();
```

## Set the level of debug messages 

```dart
    //Valid values: 0 = no messages, 1 = errors, 2 = all messages
    await BeaconsPlugin.setDebugLevel(int value);
```

## Scan Results

| Data | Android | iOS |
| ------------- | ------------- | ------------- |
| name | Yes  |  Yes |
| uuid | Yes  |  Yes |
| major | Yes  |  Yes |
| minor | Yes  |  Yes |
| distance | Yes  |  Yes |
| proximity | Yes  |  Yes |
| rssi | Yes  |  Yes |
| macAddress | Yes  |  No |
| txPower | Yes  |  No |


## Native Libraries

* For iOS: [CoreLocation](https://developer.apple.com/documentation/corelocation/)
* For Android: [Android-Beacon-Library](https://github.com/AltBeacon/android-beacon-library) 

# Author

Flutter Beacons plugin is developed by Umair Adil. You can email me at <m.umair.adil@gmail.com> for any queries.
