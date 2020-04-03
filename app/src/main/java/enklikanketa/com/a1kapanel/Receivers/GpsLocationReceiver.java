package enklikanketa.com.a1kapanel.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import enklikanketa.com.a1kapanel.Libraries.NotificationLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.PrefsActivity;
import enklikanketa.com.a1kapanel.R;

import static android.content.Context.LOCATION_SERVICE;

public class GpsLocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals("android.location.PROVIDERS_CHANGED")) {
                NotificationLib NL = new NotificationLib(context);
                TrackingLib tlib = new TrackingLib(context);
                if (!isGPSOn(context)) {
                    if (tlib.isCanTrack()) {
                        NL.showNotificationGPS();
                        PrefsActivity.postTrackingLog(context, "GPS", "off - tracking on" + getBestLocationProviderLog(context));
                    } else
                        PrefsActivity.postTrackingLog(context, "GPS", "off - tracking off" + getBestLocationProviderLog(context));
                } else {
                    NL.hideNotification(context.getResources().getInteger(R.integer.GPS_notification_id));
                    if (tlib.isCanTrack())
                        PrefsActivity.postTrackingLog(context, "GPS", "on - tracking on" + getBestLocationProviderLog(context));
                    else
                        PrefsActivity.postTrackingLog(context, "GPS", "on - tracking off" + getBestLocationProviderLog(context));
                }
            }
        }
    }

    /**
     * Check if GPS is on
     * @param ctx - Context
     * @return true if GPS is on, false otherwise
     */
    static public boolean isGPSOn(Context ctx) {
        LocationManager manager = (LocationManager) ctx.getSystemService(LOCATION_SERVICE);
        return manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Get best location provider available for logs
     * @param ctx - Context
     * @return string of best location provider available for logs
     */
    static public String getBestLocationProviderLog(Context ctx) {
        return " (providers: "+getBestLocationProvider(ctx)+")";
    }

    /**
     * Get best location provider available
     * @param ctx - Context
     * @return string of best location provider available
     */
    static public String getBestLocationProvider(Context ctx) {
        LocationManager manager = (LocationManager) ctx.getSystemService(LOCATION_SERVICE);
        String providers = manager.getProviders(true).toString();
        return providers.substring(1, providers.length() - 1);
    }
}
