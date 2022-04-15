import 'dart:async';

import 'package:flutter/services.dart';

class BeaconsPlugin {
  static const MethodChannel channel = const MethodChannel('beacons_plugin');
  static const event_channel = EventChannel('beacons_plugin_stream');

  // 0 = no messages, 1 = only errors, 2 = all
  static int _debugLevel = 0;

  /// Set the message level value [value] for debugging purpose. 0 = no messages, 1 = errors, 2 = all
  static void setDebugLevel(int value) {
    _debugLevel = value;
  }

  // Send the message [msg] with the [msgDebugLevel] value. 1 = error, 2 = info
  static void printDebugMessage(String? msg, int msgDebugLevel) {
    if (_debugLevel >= msgDebugLevel) {
      print('beacons_plugin: $msg');
    }
  }

  static Future<bool> initialize() async {
    final result = await channel.invokeMethod<bool>('initialize');
    printDebugMessage('initialized = $result', 2);
    return result ?? false;
  }

  static Future<void> startMonitoring() async {
    final result = await channel.invokeMethod<String>('startMonitoring');
    printDebugMessage(result, 2);
  }

  static Future<void> stopMonitoring() async {
    final result = await channel.invokeMethod<String>('stopMonitoring');
    printDebugMessage(result, 2);
  }

  static Future<void> addRegion(String identifier, String uuid) async {
    final result = await channel.invokeMethod<String>(
      'addRegion',
      {'identifier': identifier, 'uuid': uuid},
    );
    printDebugMessage(result, 2);
  }

  static Future<void> clearRegions() async {
    final result = await channel.invokeMethod<String>('clearRegions');
    printDebugMessage(result, 2);
  }

  static Future<void> setNotification(String title, String text) async {
    final result = await channel.invokeMethod<String>(
      'setNotification',
      {'title': title, 'text': text},
    );
    printDebugMessage(result, 2);
  }

  static listenToBeacons(StreamController controller) async {
    event_channel.receiveBroadcastStream().listen((dynamic event) {
      printDebugMessage('Received: $event', 2);
      controller.add(event);
    }, onError: (dynamic error) {
      printDebugMessage('Received error: ${error.message}', 1);
    });
  }
}
