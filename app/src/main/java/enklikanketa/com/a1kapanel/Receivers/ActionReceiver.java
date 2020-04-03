package enklikanketa.com.a1kapanel.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import enklikanketa.com.a1kapanel.Libraries.NotificationLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;

import static enklikanketa.com.a1kapanel.Libraries.NotificationLib.NOTIFICATION_START_TRACKING_ID;

/**
 * Created by podkrizniku on 27/11/2017.
 */

public class ActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() != null) {
            TrackingLib tlib = new TrackingLib(context);
            if (intent.getAction().equals(TrackingLib.ACTION_STOP_TRACKING)) {
                tlib.stopTracking("user_notification_action_stop");
                tlib.setCanTrack(false);
            } else if (intent.getAction().equals(TrackingLib.ACTION_PAUSE_TRACKING)) {
                tlib.pauseTracking("user_notification_action_pause");
                tlib.setCanTrack(false);
            } else if (intent.getAction().equals(TrackingLib.ACTION_START_TRACKING)) {
                tlib.setCanTrack(true);
                tlib.startTracking("user_notification_action_start");
            }
            else if (intent.getAction().equals(TrackingLib.ACTION_RUN_TRACKING)) {
                NotificationLib nlib = new NotificationLib(context);
                nlib.hideNotification(NOTIFICATION_START_TRACKING_ID);
                tlib.setCanTrack(true);
                tlib.startTracking("user_notification_action_run");
            }
        }
    }
}
