package com.ygorcesar.jamdroidfirechat.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ygorcesar.jamdroidfirechat.R;
import com.ygorcesar.jamdroidfirechat.utils.Constants;
import com.ygorcesar.jamdroidfirechat.utils.ConstantsFirebase;
import com.ygorcesar.jamdroidfirechat.utils.PushNotificationObject;
import com.ygorcesar.jamdroidfirechat.utils.Singleton;
import com.ygorcesar.jamdroidfirechat.utils.Utils;
import com.ygorcesar.jamdroidfirechat.view.activity.MainActivity;

import java.util.List;

public class FcmMessagingService extends FirebaseMessagingService {
    private long[] pattern = {300, 300, 300, 300, 300};

    @Override public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            sendDefaultNotification(remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody());
        } else {
            String currentUserEmail = "";
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null) {
                currentUserEmail = auth.getCurrentUser().getEmail();
            }

            String userName = remoteMessage.getData().get(Constants.KEY_USER_DISPLAY_NAME);
            String userEmail = remoteMessage.getData().get(Constants.KEY_USER_EMAIL);
            String chatKey = remoteMessage.getData().get(Constants.KEY_CHAT_KEY);
            String deviceId = remoteMessage.getData().get(Constants.KEY_USER_FCM_DEVICE_ID);
            String deviceIdSender = remoteMessage.getData().get(Constants.KEY_USER_FCM_DEVICE_ID_SENDER);
            String title = remoteMessage.getData().get(Constants.KEY_MSG_TITLE);
            String msg = remoteMessage.getData().get(Constants.KEY_MSG);
            String msgKey = remoteMessage.getData().get(Constants.KEY_MSG_KEY);

            if (chatKey.equals(ConstantsFirebase.FIREBASE_LOCATION_CHAT_GLOBAL)) {
                title = String.format("%s- %s", title, ConstantsFirebase.CHAT_GLOBAL_HELPER);
            } else {
                if (!currentUserEmail.equals(Utils.decodeEmail(userEmail))) {
                    setMessageReceived(FirebaseDatabase.getInstance().getReference()
                            .child(ConstantsFirebase.FIREBASE_LOCATION_CHAT).child(chatKey).child(msgKey)
                            .child(ConstantsFirebase.FIREBASE_PROPERTY_MESSAGE_STATUS));
                }
            }

            boolean notificationIsActive = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean(Constants.KEY_PREF_NOTIFICATION, false);
            if (auth.getCurrentUser() != null && notificationIsActive) {
                if (!currentUserEmail.equals(Utils.decodeEmail(userEmail))) {

                    Utils.setAdditionalData(new PushNotificationObject
                            .AdditionalData(title, msg, chatKey, msgKey, userName,
                            userEmail, deviceId, deviceIdSender));
                    sendNotification(title, msg);
                }
            }
        }
    }

    private void setMessageReceived(final DatabaseReference messageRef) {
        messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null &&
                        dataSnapshot.getValue(long.class) == ConstantsFirebase.MESSAGE_STATUS_SENDED) {
                    messageRef.setValue(ConstantsFirebase.MESSAGE_STATUS_RECEIVED);
                }
            }

            @Override public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void sendDefaultNotification(String messageTitle, String messageBody) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Bitmap largerIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_notification_large_default);

        Notification notification;
        if (messageTitle != null && !messageTitle.isEmpty()) {
            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification_stat_default)
                    .setLargeIcon(largerIcon)
                    .setContentTitle(messageTitle)
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setVibrate(pattern)
                    .setColor(getResources().getColor(R.color.colorAccent))
                    .setLights(Color.BLUE, 1, 1)
                    .setSound(defaultSoundUri)
                    .setContentIntent(createPendingIntent())
                    .setGroupSummary(true)
                    .build();
        } else {
            notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_notification_stat_default)
                    .setLargeIcon(largerIcon)
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setVibrate(pattern)
                    .setColor(getResources().getColor(R.color.colorAccent))
                    .setLights(Color.BLUE, 1, 1)
                    .setSound(defaultSoundUri)
                    .setContentIntent(createPendingIntent())
                    .setGroupSummary(true)
                    .build();
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    private void sendNotification(String messageTitle, String messageBody) {

        final String GROUP_KEY_MESSAGES = "chat_key_messages";
        int numMessages = Singleton.getInstance().getNumMessages();
        List<String> messages = Singleton.getInstance().getNotificationMessages();
        Singleton.getInstance().addMessage(messageBody);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Bitmap largerIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_notification_large_default);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

        inboxStyle.setBigContentTitle(getResources().getQuantityString(R.plurals.not_new_message, numMessages, numMessages));
        inboxStyle.setSummaryText("JamdroidFirechat");
        for (int i = 0; i < messages.size(); i++) {
            inboxStyle.addLine(String.format("%s: %s", messageTitle, messages.get(i)));
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification_stat_default)
                .setLargeIcon(largerIcon)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setVibrate(pattern)
                .setStyle(inboxStyle)
                .setNumber(numMessages)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setLights(Color.BLUE, 1, 1)
                .setSound(defaultSoundUri)
                .setContentIntent(createPendingIntent())
                .setGroup(GROUP_KEY_MESSAGES)
                .setGroupSummary(true)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    private PendingIntent createPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
