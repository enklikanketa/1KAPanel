package enklikanketa.com.a1kapanel.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import enklikanketa.com.a1kapanel.Libraries.AlarmLib;
import enklikanketa.com.a1kapanel.Libraries.GeofencingLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;

public class MyStartServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmLib alib = new AlarmLib(context);
        alib.checkAndRunAlarms();

        //run tracking if there are subscribed surveys using it
        TrackingLib tlib = new TrackingLib(context);
        if(tlib.areTrackingPermissionGrantedAndRunning()) {
            tlib.startTracking("boot");
        }

        //run all geofences stored in DB
        new GeofencingLib(context).runAllGeofencesFromDB();
    }
}