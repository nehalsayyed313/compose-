import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

final FlutterLocalNotificationsPlugin notificationsPlugin =
    FlutterLocalNotificationsPlugin();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize Android notification settings
  const initSettingsAndroid = AndroidInitializationSettings('@mipmap/ic_launcher');
  const initSettings = InitializationSettings(android: initSettingsAndroid);

  // Uses named argument: settings
  await notificationsPlugin.initialize(
    settings: initSettings,
  );

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

    // Uses named arguments: id, title, body, notificationDetails
    await notificationsPlugin.show(
      id: 0,
      title: 'Test Alert',
      body: 'Notification is working successfully!',
      notificationDetails: details,
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
