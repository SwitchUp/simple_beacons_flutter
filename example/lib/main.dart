import 'dart:async';

import 'package:beacons_plugin/beacons_plugin.dart';
import 'package:flutter/material.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _beaconResult = 'Not Scanned Yet.';
  String _debugMessage = '';
  int _nrMessagesReceived = 0;
  bool _isRunning = false;
  final List<String> _results = [];

  final ScrollController _scrollController = ScrollController();

  final StreamController<String> _beaconEventsController =
      StreamController<String>.broadcast();

  @override
  void initState() {
    super.initState();
    _initStateAsync();
  }

  @override
  void dispose() {
    _beaconEventsController.close();
    super.dispose();
  }

  Future<void> _initStateAsync() async {
    await _startMonitoring();

    BeaconsPlugin.listenToBeacons(_beaconEventsController);

    await BeaconsPlugin.addRegion(
        "BeaconType1", "909c3cf9-fc5c-4841-b695-380958a51a5a");
    await BeaconsPlugin.addRegion(
        "BeaconType2", "6a84c716-0f2a-1ce9-f210-6a63bd873dd9");

    _beaconEventsController.stream.listen(
      (data) {
        if (data.isNotEmpty && _isRunning) {
          setState(() {
            _beaconResult = data;
            _results.add(_beaconResult);
            _nrMessagesReceived++;
            _debugMessage = "Beacons DataReceived: $data";
          });

          print("Beacons DataReceived: " + data);
        }
      },
      onDone: () {},
      onError: (error) => print("Error: $error"),
    );

    if (!mounted) return;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Beacons Plugin'),
        ),
        body: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Center(
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: _buildText(_debugMessage),
              ),
            ),
            Center(
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: _buildText('Total Results: $_nrMessagesReceived'),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(8.0),
              child: ElevatedButton(
                onPressed: () async => _isRunning
                    ? await _stopMonitoring()
                    : await _startMonitoring(),
                child: Text(
                  _isRunning ? 'Stop Scanning' : 'Start Scanning',
                  style: TextStyle(fontSize: 20),
                ),
              ),
            ),
            Visibility(
              visible: _results.isNotEmpty,
              child: Padding(
                padding: const EdgeInsets.all(2.0),
                child: ElevatedButton(
                  onPressed: () => setState(() {
                    _nrMessagesReceived = 0;
                    _results.clear();
                  }),
                  child: Text(
                    "Clear Results",
                    style: TextStyle(fontSize: 20),
                  ),
                ),
              ),
            ),
            SizedBox(height: 20.0),
            Expanded(child: _buildResultsList())
          ],
        ),
      ),
    );
  }

  Future<void> _startMonitoring() async {
    final initialized = await BeaconsPlugin.initialize();
    if (initialized) {
      await BeaconsPlugin.startMonitoring();
      setState(() {
        _debugMessage = "Beacons monitoring started";
        _isRunning = true;
      });
    } else {
      setState(() {
        _debugMessage = "Beacons monitoring can't start: permissions denied";
      });
    }
  }

  Future<void> _stopMonitoring() async {
    await BeaconsPlugin.stopMonitoring();
    setState(() {
      _debugMessage = "Beacons monitoring stopped";
      _isRunning = false;
    });
  }

  Widget _buildResultsList() {
    return Scrollbar(
      isAlwaysShown: true,
      controller: _scrollController,
      child: ListView.separated(
        shrinkWrap: true,
        scrollDirection: Axis.vertical,
        physics: ScrollPhysics(),
        controller: _scrollController,
        itemCount: _results.length,
        separatorBuilder: (BuildContext context, int index) => Divider(
          height: 1,
          color: Colors.black,
        ),
        itemBuilder: (context, index) => ListTile(
          title: _buildText(
            "Time: ${DateTime.now()}\n${_results[index]}",
            textAlign: TextAlign.justify,
            fontWeight: FontWeight.normal,
          ),
          onTap: () {},
        ),
      ),
    );
  }

  Widget _buildText(
    String text, {
    TextAlign textAlign = TextAlign.start,
    FontWeight fontWeight = FontWeight.bold,
  }) {
    return Text(
      text,
      textAlign: textAlign,
      style: TextStyle(
        color: Colors.black,
        fontSize: 14,
        fontWeight: fontWeight,
      ),
    );
  }
}
