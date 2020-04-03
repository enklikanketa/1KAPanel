package enklikanketa.com.a1kapanel.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import enklikanketa.com.a1kapanel.Tasks.sendTrakingLocationsTask;

public class AlarmLocationsJobReceiver extends BroadcastReceiver {
    private String TAG = "AlarmLocationsJobReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        new sendTrakingLocationsTask(context).execute();
    }
}
