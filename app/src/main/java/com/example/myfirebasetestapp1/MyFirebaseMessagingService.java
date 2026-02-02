package com.example.myfirebasetestapp1;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // מטפל בהודעות שמגיעות כשהאפליקציה בפורגראונד (פתוחה)
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            // שימוש ב-Helper הקיים שלנו כדי להציג את ההתראה
            NotificationHelper helper = new NotificationHelper(this);
            // כאן יצרנו פוסט פיקטיבי רק בשביל התצוגה של ה-Helper
            Post dummyPost = new Post();
            dummyPost.authorFirstName = "";
            dummyPost.title = title != null ? title : "הודעה חדשה";
            
            // אפשר לשכלל את זה כדי שיציג את ה-Body האמיתי
            helper.showNewPostNotification(dummyPost);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // שלח את ה-Token לשרת שלך או שמור אותו ב-DB
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users")
                    .child(user.getUid())
                    .child("fcmToken");
            ref.setValue(token);
        }
    }
}
