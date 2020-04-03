package enklikanketa.com.a1kapanel.Services;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import enklikanketa.com.a1kapanel.Libraries.AlarmLib;
import enklikanketa.com.a1kapanel.Libraries.EntryLib;
import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.GeofencingLib;
import enklikanketa.com.a1kapanel.Libraries.NotificationLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public MyFirebaseMessagingService() {
        super();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ

        Map<String, String> data = remoteMessage.getData();
        //you can get your text message here.
        String action = data.get("action");

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

        try {
            //message to show one notification
            if (action.equals("notification")) {
                NotificationLib nlib = new NotificationLib(this);
                //replace link to redirect user to survey list instead of fill out form
                data.put("link", "survey_list");
                nlib.showNotificationSurvey(data);
            }

            //message to set alarm
            else if (action.equals("alarm")) {
                AlarmLib alib = new AlarmLib(this);
                alib.setOrUpdateNewAlarm(data);
            }

            //message to cancel alarm
            else if (action.equals("cancel_alarm")) {
                AlarmLib alib = new AlarmLib(this);
                alib.cancelAndDeleteAlarms(data.get("srv_id"));
            }

            //message to cancel repeater
            else if (action.equals("stop_repeater")) {
                AlarmLib alib = new AlarmLib(this);
                alib.cancelAndDeleteRepeater(data.get("srv_id"));
                alib.cancelAndDeleteAlarms(data.get("srv_id"));
            }

            //message to cancel geofencing
            else if (action.equals("cancel_geofencing")) {
                GeofencingLib glib = new GeofencingLib(this);
                glib.cancelGeofences(data.get("srv_id"));
            }

            //message to set geofences
            else if (action.equals("geofencing")) {
                GeofencingLib glib = new GeofencingLib(this);
                glib.setOrUpdateNewGeofences(data);
            }

            //message to set tracking
            else if (action.equals("tracking")) {
                TrackingLib tlib = new TrackingLib(this);
                tlib.setOrUpdateNewTracking(data);
            }

            //message to cancel tracking
            else if (action.equals("cancel_tracking")) {
                TrackingLib tlib = new TrackingLib(this);
                tlib.cancelTracking(data.get("srv_id"), "server cancel");
            }

            //message to set data entry
            else if (action.equals("entry")) {
                EntryLib elib = new EntryLib(this);
                elib.setOrUpdateNewEntry(data);
            }

            //message to cancel data entry
            else if (action.equals("cancel_entry")) {
                EntryLib elib = new EntryLib(this);
                elib.cancelEntry(data.get("srv_id"));
            }

            //message to cancel all things for survey (survey is deactivated)
            else if (action.equals("cancel_all")) {
                String srv_id = data.get("srv_id");
                EntryLib elib = new EntryLib(this);
                elib.cancelEntry(srv_id);
                TrackingLib tlib = new TrackingLib(this);
                tlib.cancelTracking(srv_id, "server cancel (survey deactivated)");
                GeofencingLib glib = new GeofencingLib(this);
                glib.cancelGeofences(srv_id);
                AlarmLib alib = new AlarmLib(this);
                alib.cancelAndDeleteRepeater(srv_id);
                alib.cancelAndDeleteAlarms(srv_id);
                GeneralLib.deleteSurveyDB(this, srv_id);
            }
        } catch (NullPointerException e){
            Log.e("1kapanel", "MyFirebaseMessagingService - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }
    }

    @Override
    public void onDeletedMessages() {
        /*
         * In some situations, FCM may not deliver a message. This occurs when there are too many messages (>100)
         * pending for your app on a particular device at the time it connects or if the device hasn't connected to
         * FCM in more than one month. In these cases, you may receive a callback to FirebaseMessagingService.onDeletedMessages()
         * When the app instance receives this callback, it should perform a full sync with your app server.
         * If you haven't sent a message to the app on that device within the last 4 weeks, FCM won't call onDeletedMessages().
         * Vir: https://firebase.google.com/docs/cloud-messaging/android/send-multiple
         */
    }
}
