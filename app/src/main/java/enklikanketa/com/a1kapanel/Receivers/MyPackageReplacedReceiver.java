package enklikanketa.com.a1kapanel.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import enklikanketa.com.a1kapanel.Libraries.AlarmLib;
import enklikanketa.com.a1kapanel.Libraries.GeofencingLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.System.Network;
import enklikanketa.com.a1kapanel.Tasks.sendGetAlarmsTask;
import enklikanketa.com.a1kapanel.Tasks.sendGetGeofencesTask;
import enklikanketa.com.a1kapanel.Tasks.sendGetTrackingTask;

/**
 * Created by podkrizniku on 09/02/2018.
 */

public class MyPackageReplacedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Network.checkMobileInternet(context, false)) {
            AlarmLib alib = new AlarmLib(context);
            alib.refreshAlarms();

            GeofencingLib glib = new GeofencingLib(context);
            glib.refreshGeofences();

            TrackingLib tlib = new TrackingLib(context);
            tlib.refreshTracking();
        }
        else {
            sendGetAlarmsTask.saveGetAlarmsLog(context);
            sendGetGeofencesTask.saveGetGeofencesLog(context);
            sendGetTrackingTask.saveGetTrackingLog(context);
        }
    }
}