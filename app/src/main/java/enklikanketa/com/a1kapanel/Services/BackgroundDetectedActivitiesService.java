package enklikanketa.com.a1kapanel.Services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;

import enklikanketa.com.a1kapanel.Libraries.NotificationLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;

import static enklikanketa.com.a1kapanel.Libraries.NotificationLib.NOTIFICATION_PERSISTANT_TRACKING_ID;
import static enklikanketa.com.a1kapanel.Services.LocationUpdatesService.PAUSEFOREGROUND_ACTION;
import static enklikanketa.com.a1kapanel.Services.LocationUpdatesService.RESUMEFOREGROUND_ACTION;
import static enklikanketa.com.a1kapanel.Services.LocationUpdatesService.STARTFOREGROUND_ACTION;
import static enklikanketa.com.a1kapanel.Services.LocationUpdatesService.STOPFOREGROUND_ACTION;

public class BackgroundDetectedActivitiesService extends Service {
    private static final String TAG = BackgroundDetectedActivitiesService.class.getSimpleName();

    private Intent mIntentService;
    private PendingIntent mPendingIntent;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private long interval = 30000;

    public BackgroundDetectedActivitiesService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_PERSISTANT_TRACKING_ID, new NotificationLib(getBaseContext())
                .notificationLocationTracking(false));

        mActivityRecognitionClient = new ActivityRecognitionClient(this);
        mIntentService = new Intent(this, DetectedActivitiesIntentService.class);
        mPendingIntent = PendingIntent.getService(this, 1, mIntentService, PendingIntent.FLAG_UPDATE_CURRENT);
        HashMap<String, String> data = new TrackingLib(getBaseContext()).getARTrackingSettings();
        if(data != null && data.get("ar_interval_wanted").equals("1")) {
            interval = Long.parseLong(data.get("ar_interval_wanted")) * 1000;
            requestActivityUpdatesHandler();
        }
        //for test - id user turns tracking on from settings
        else if(data == null){
            requestActivityUpdatesHandler();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // We got here because the user decided to remove location updates from the notification.
        if (intent.getAction() != null && intent.getAction().equals(STOPFOREGROUND_ACTION)) {
            stop();
        }
        else if(intent.getAction() != null && intent.getAction().equals(PAUSEFOREGROUND_ACTION)) {
            removeActivityUpdatesHandler();
        }
        else if(intent.getAction() != null && intent.getAction().equals(RESUMEFOREGROUND_ACTION)) {
            requestActivityUpdatesHandler();
        }
        else if(intent.getAction() != null && intent.getAction().equals(STARTFOREGROUND_ACTION)) {
            startForeground(NOTIFICATION_PERSISTANT_TRACKING_ID, new NotificationLib(getBaseContext())
                    .notificationLocationTracking(false));
            requestActivityUpdatesHandler();
        }

        return START_STICKY;
    }

    public void stop(){
        removeActivityUpdatesHandler();
        stopForeground(true);
        stopSelf();
    }

    public void requestActivityUpdatesHandler() {
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                interval, mPendingIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Successfully requested activity updates");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Requesting activity updates failed to start");
            }
        });
    }

    public void removeActivityUpdatesHandler() {
        Task<Void> task = mActivityRecognitionClient.removeActivityUpdates(
                mPendingIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Removed activity updates successfully!");
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Failed to remove activity updates!");
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeActivityUpdatesHandler();
    }
}
