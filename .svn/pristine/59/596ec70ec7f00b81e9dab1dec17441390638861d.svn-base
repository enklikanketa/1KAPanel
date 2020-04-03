package enklikanketa.com.a1kapanel.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import enklikanketa.com.a1kapanel.Libraries.AlarmLib;

/**
 * Created by podkrizniku on 07/02/2018.
 */

public class TimeZoneChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        AlarmLib alib = new AlarmLib(context);
        alib.reRunAlarms();
    }
}
