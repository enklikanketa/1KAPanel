package enklikanketa.com.a1kapanel.Libraries;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import java.util.Map;

import enklikanketa.com.a1kapanel.Home;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.Receivers.ActionReceiver;

import static android.content.Context.NOTIFICATION_SERVICE;
import static enklikanketa.com.a1kapanel.App.NOTIFICATION_PERSISTANT;
import static enklikanketa.com.a1kapanel.App.NOTIFICATION_SURVEY_PUSH;

/**
 * Created by podkrizniku on 06/12/2017.
 */

public class NotificationLib {

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    public static final int NOTIFICATION_PERSISTANT_TRACKING_ID = 684351054;
    public static final int NOTIFICATION_START_TRACKING_ID = 689412054;

    private Context ctx;

    public NotificationLib(Context base) {
        ctx = base;
    }

    /**
     * Show notification for survey
     * @param data - map of pairs for title, message, link, sound and sender_id
     */
    public void showNotificationSurvey(Map<String, String> data) {
        if(data != null) {
            //you can get your text message here.
            String message = data.get("message");
            String title = data.get("title");
            String link = data.get("link");
            String action = data.get("action");
            String sender_id = data.get("sender_id");

            Intent intent;
            if(link.equals("survey_list"))
                intent = new Intent(ctx, Home.class);
            else if(action != null && action.equals("tracking"))
                intent = new Intent(ctx, Home.class);
            else {
                intent = new Intent(ctx, Home.class);
            }

            int id = (sender_id != null) ? Integer.parseInt(sender_id) : GeneralLib.getRandomInt();

            PendingIntent pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx, NOTIFICATION_SURVEY_PUSH)
                    .setTicker(ctx.getString(R.string.app_name))
                    .setSmallIcon(android.R.drawable.ic_menu_myplaces)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setContentIntent(pi)
                    .setAutoCancel(true);//removable on touch action

            notificationBuilder.setDefaults(Notification.DEFAULT_ALL);

            Notification notification = notificationBuilder.build();

            NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null)
                notificationManager.notify(id, notification);
        }
    }

    private static Notification notificationLocationTracking;
    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     * @param forceCreation - force notification to recreate (because start/pause button)
     */
    public Notification notificationLocationTracking(boolean forceCreation) {
        if(notificationLocationTracking == null || forceCreation) {
            Intent stopIntent = new Intent(ctx, ActionReceiver.class);
            stopIntent.setAction(TrackingLib.ACTION_STOP_TRACKING);
            Intent pauseIntent = new Intent(ctx, ActionReceiver.class);
            pauseIntent.setAction(TrackingLib.ACTION_PAUSE_TRACKING);
            Intent startIntent = new Intent(ctx, ActionReceiver.class);
            startIntent.setAction(TrackingLib.ACTION_START_TRACKING);

            PendingIntent pistop = PendingIntent.getBroadcast(ctx, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent pipouse = PendingIntent.getBroadcast(ctx, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent pistart = PendingIntent.getBroadcast(ctx, 2, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Action action;
            String desc;

            TrackingLib tlib = new TrackingLib(ctx);

            if (tlib.isCanTrack()) {
                action = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause,
                        ctx.getString(R.string.tracking_notification_pause), pipouse).build();
                desc = ctx.getString(R.string.tracking_notification_desc_on);
            }
            else{
                action = new NotificationCompat.Action.Builder(android.R.drawable.ic_media_play,
                        ctx.getString(R.string.tracking_notification_start), pistart).build();
                desc = ctx.getString(R.string.tracking_notification_desc_off);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, NOTIFICATION_PERSISTANT)
                    .addAction(action)
                    .addAction(android.R.drawable.ic_lock_power_off, ctx.getString(R.string.tracking_notification_stop), pistop)
                    .setContentText(desc)
                    .setContentTitle(ctx.getString(R.string.tracking_notification_title))
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setSmallIcon(android.R.drawable.ic_menu_myplaces)
                    .setTicker(ctx.getString(R.string.tracking_notification_title))
                    .setWhen(System.currentTimeMillis());

            notificationLocationTracking = builder.build();
        }
        return notificationLocationTracking;
    }

    /**
     * Shows notification with action button for starting tracking
     */
    public void showNotificationRunLocationTracking() {
        Intent startIntent = new Intent(ctx, ActionReceiver.class);
        startIntent.setAction(TrackingLib.ACTION_RUN_TRACKING);
        PendingIntent pistart = PendingIntent.getBroadcast(ctx, 3, startIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, NOTIFICATION_SURVEY_PUSH)
                .addAction(android.R.drawable.ic_media_play, ctx.getString(R.string.tracking_notification_start), pistart)
                .setContentText(ctx.getString(R.string.tracking_notification_start_desc))
                .setContentTitle(ctx.getString(R.string.tracking_notification_start_title))
                .setPriority(Notification.DEFAULT_ALL)
                .setSmallIcon(android.R.drawable.ic_menu_myplaces);

        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.notify(NOTIFICATION_START_TRACKING_ID, notification);
    }

    /**
     * Show notification about "GPS is off"
     */
    public void showNotificationGPS() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx, NOTIFICATION_PERSISTANT)
                .setTicker(ctx.getString(R.string.app_name))
                .setSmallIcon(android.R.drawable.ic_menu_myplaces)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle(ctx.getString(R.string.turn_gps_on));

        Notification notification = notificationBuilder.build();

        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
        if(notificationManager != null)
            notificationManager.notify(ctx.getResources().getInteger(R.integer.GPS_notification_id), notification);
    }

    public void hideNotification(int notification_id){
        NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotificationManager != null)
            mNotificationManager.cancel(notification_id);
    }
}
