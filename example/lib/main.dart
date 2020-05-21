import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:io' show Platform;
import 'package:beacons_plugin/beacons_plugin.dart';
import 'package:background_fetch/background_fetch.dart';

void main() {
  runApp(MyApp());

  BackgroundFetch.registerHeadlessTask(
      BeaconsPlugin.backgroundFetchHeadlessTask);
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _beaconResult = 'Not Scanned Yet.';
  int _nrMessaggesReceived = 0;
  var isRunning = false;
  int _status = 0;
  List<DateTime> _events = [];

  final StreamController<String> beaconEventsController =
      StreamController<String>.broadcast();

  @override
  void initState() {
    super.initState();
    initPlatformState();

    BeaconsPlugin.setupBackgroundFetch((String taskId) async {
      // Use a switch statement to route task-handling.
      switch (taskId) {
        case 'com.transistorsoft.customtask':
          print(
              "[${BeaconsPlugin.TAG}] [BackgroundFetch] Received custom task");
          break;
        default:
          print("[${BeaconsPlugin.TAG}] [BackgroundFetch] Default fetch task");
      }

      // This is the fetch-event callback.
      print("[${BeaconsPlugin.TAG}] [BackgroundFetch] Event received $taskId");
      setState(() {
        _events.insert(0, new DateTime.now());
      });
      // IMPORTANT:  You must signal completion of your task or the OS can punish your app
      // for taking too long in the background.
      BackgroundFetch.finish(taskId);
    }).then((int status) {
      print(
          '[${BeaconsPlugin.TAG}] [BackgroundFetch] configure success: $status');
      setState(() {
        _status = status;
      });
    }).catchError((e) {
      print('[${BeaconsPlugin.TAG}] [BackgroundFetch] configure ERROR: $e');
      setState(() {
        _status = e;
      });
    });

    // Step 2:  Schedule a custom "oneshot" task "com.transistorsoft.customtask" to execute 5000ms from now.
    /*BackgroundFetch.scheduleTask(TaskConfig(
        taskId: "com.transistorsoft.customtask", delay: 5000 // <-- milliseconds
        ));*/
  }

  @override
  void dispose() {
    beaconEventsController.close();
    super.dispose();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    BeaconsPlugin.listenToBeacons(beaconEventsController);

    await BeaconsPlugin.addRegion(
        "BeaconType1", "909c3cf9-fc5c-4841-b695-380958a51a5a");
    await BeaconsPlugin.addRegion(
        "BeaconType2", "6a84c716-0f2a-1ce9-f210-6a63bd873dd9");

    beaconEventsController.stream.listen(
        (data) {
          if (data.isNotEmpty) {
            setState(() {
              _beaconResult = data;
              _nrMessaggesReceived++;
            });
            print("[${BeaconsPlugin.TAG}] Beacons DataReceived: " + data);
          }
        },
        onDone: () {},
        onError: (error) {
          print("[${BeaconsPlugin.TAG}] Error: $error");
        });

    //Send 'true' to run in background
    await BeaconsPlugin.runInBackground(true);

    //Scan after specific delay [1 min]
    await BeaconsPlugin.scanPeriodically(delayInMilliseconds: 1000 * 60);

    if (Platform.isAndroid) {
      BeaconsPlugin.channel.setMethodCallHandler((call) async {
        if (call.method == 'scannerReady') {
          await BeaconsPlugin.startMonitoring;
          setState(() {
            isRunning = true;
          });
        }
      });
    } else if (Platform.isIOS) {
      await BeaconsPlugin.startMonitoring;
      setState(() {
        isRunning = true;
      });
    }

    if (!mounted) return;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Monitoring Beacons'),
        ),
        body: Center(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text('$_beaconResult'),
              Padding(
                padding: EdgeInsets.all(10.0),
              ),
              Text('$_nrMessaggesReceived'),
              SizedBox(
                height: 20.0,
              ),
              Visibility(
                visible: isRunning,
                child: RaisedButton(
                  onPressed: () async {
                    if (Platform.isAndroid) {
                      await BeaconsPlugin.stopMonitoring;

                      setState(() {
                        isRunning = false;
                      });
                    }
                  },
                  child: Text('Stop Scanning', style: TextStyle(fontSize: 20)),
                ),
              ),
              SizedBox(
                height: 20.0,
              ),
              Visibility(
                visible: !isRunning,
                child: RaisedButton(
                  onPressed: () async {
                    initPlatformState();
                    await BeaconsPlugin.startMonitoring;

                    setState(() {
                      isRunning = true;
                    });
                  },
                  child: Text('Start Scanning', style: TextStyle(fontSize: 20)),
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}
