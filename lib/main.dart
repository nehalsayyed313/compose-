import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

final FlutterLocalNotificationsPlugin notificationsPlugin =
    FlutterLocalNotificationsPlugin();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Android notification settings
  const initSettingsAndroid = AndroidInitializationSettings('@mipmap/ic_launcher');
  const initSettings = InitializationSettings(android: initSettingsAndroid);

  await notificationsPlugin.initialize(initSettings);

  // Request Android 13+ runtime permission
  await notificationsPlugin
      .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
      ?.requestNotificationsPermission();

  runApp(const MaterialApp(
    debugShowCheckedModeBanner: false,
    home: NotificationScreen(),
  ));
}

class NotificationScreen extends StatelessWidget {
  const NotificationScreen({super.key});

  Future<void> _sendNotification() async {
    const androidDetails = AndroidNotificationDetails(
      'default_channel',
      'General Notifications',
      channelDescription: 'Main channel for app alerts',
      importance: Importance.max,
      priority: Priority.high,
      ticker: 'ticker',
    );
    const details = NotificationDetails(android: androidDetails);

    await notificationsPlugin.show(
      0,
      'Test Alert',
      'Notification is working successfully!',
      details,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Local Notification Demo'),
        centerTitle: true,
      ),
      body: Center(
        child: ElevatedButton.icon(
          onPressed: _sendNotification,
          icon: const Icon(Icons.notifications_active),
          label: const Text('Show Notification'),
        ),
      ),
    );
  }
}
