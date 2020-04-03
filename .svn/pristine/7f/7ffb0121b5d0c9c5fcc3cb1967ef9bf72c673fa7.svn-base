package enklikanketa.com.a1kapanel;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;

import enklikanketa.com.a1kapanel.Receivers.NetworkChangeReceiver;
import io.fabric.sdk.android.Fabric;

/**
 * Created by podkrizniku on 14/12/2017.
 */

public class App extends Application implements Application.ActivityLifecycleCallbacks{
    private NetworkChangeReceiver mNetworkChangeReceiver;
    private IntentFilter intentFilter;
    //was it offline before? setter is in NetworkChangeReceiver, when we get offline
    public static boolean wasOffline = false;

    public static String NOTIFICATION_SURVEY_PUSH = "survey_push";
    public static String NOTIFICATION_PERSISTANT = "persistant";

    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
        Fabric.with(this, new Crashlytics());
        registerActivityLifecycleCallbacks(this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        //notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            /* Create or update. */
            NotificationChannel channel1 = new NotificationChannel(NOTIFICATION_SURVEY_PUSH,
                    getString(R.string.notif_push_channel_title),
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationChannel channel2 = new NotificationChannel(NOTIFICATION_PERSISTANT,
                    getString(R.string.notif_persistant_channel_title),
                    NotificationManager.IMPORTANCE_HIGH);
            channel2.setSound(null, null);
            if(notificationManager != null) {
                notificationManager.createNotificationChannel(channel1);
                notificationManager.createNotificationChannel(channel2);
            }
        }
    }
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }
    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mNetworkChangeReceiver = new NetworkChangeReceiver(activity);
        //onReceive is always called with CONNECTIVITY_ACTION when registerReceiver is set
        registerReceiver(mNetworkChangeReceiver, intentFilter);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mNetworkChangeReceiver.hideSnackbar();
        unregisterReceiver(mNetworkChangeReceiver);
    }
    @Override
    public void onActivityStopped(Activity activity) {
    }
    @Override
    public void onActivityDestroyed(Activity activity) {
    }
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }
}
