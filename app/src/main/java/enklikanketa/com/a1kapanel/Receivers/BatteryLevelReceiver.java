package enklikanketa.com.a1kapanel.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import enklikanketa.com.a1kapanel.Libraries.TrackingLib;

public class BatteryLevelReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            TrackingLib tlib = new TrackingLib(context);
            if (intent.getAction().matches(Intent.ACTION_BATTERY_LOW)) {
                tlib.stopTracking("battery_low");
            } else if (intent.getAction().matches(Intent.ACTION_BATTERY_OKAY)) {
                tlib.startTracking("battery_okay");
            }
        }
    }
}