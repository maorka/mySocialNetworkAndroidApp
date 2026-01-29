package com.example.myfirebasetestapp1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.Manifest;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.atomic.AtomicInteger;

public class NotificationHelper {

    private static final String CHANNEL_ID = "posts_channel";
    private static final String CHANNEL_NAME = "New Posts";
    private static final String CHANNEL_DESC = "Notifications for new posts";

    private final Context context;
    private static final AtomicInteger notifIdGen = new AtomicInteger(0);

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void showNewPostNotification(Post post) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your R.drawable.notification_icon
                .setContentTitle("New Post!")
                .setContentText(post.authorFirstName + " posted a new post: " + post.title)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            // Runtime permission check on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted; skipping notify().");
                    return;
                }
            }

            int id = notifIdGen.incrementAndGet();
            notificationManager.notify(id, builder.build());
        } catch (SecurityException e) {
            Log.e("NotificationHelper", "Missing permission for notifications", e);
        }
    }
}
