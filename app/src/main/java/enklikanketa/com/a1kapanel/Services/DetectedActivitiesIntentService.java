package enklikanketa.com.a1kapanel.Services;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

import enklikanketa.com.a1kapanel.Libraries.TrackingLib;

public class DetectedActivitiesIntentService  extends IntentService {

    protected static final String TAG = DetectedActivitiesIntentService.class.getSimpleName();

    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        String detected_activities = "";
        for (DetectedActivity activity : detectedActivities) {
            String label = "unknown";

            switch (activity.getType()) {
                case DetectedActivity.IN_VEHICLE: {
                    label = "in vehicle";
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    label = "on bicycle";
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    label = "on foot";
                    break;
                }
                case DetectedActivity.RUNNING: {
                    label = "running";
                    break;
                }
                case DetectedActivity.STILL: {
                    label = "still";
                    break;
                }
                case DetectedActivity.TILTING: {
                    label = "tilting";
                    break;
                }
                case DetectedActivity.WALKING: {
                    label = "walking";
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    label = "unknown";
                    break;
                }
            }

            detected_activities += label+": "+activity.getConfidence()+"; ";
        }
        broadcastActivity(detected_activities);
        new TrackingLib(this).storeARData(result);
    }

    private void broadcastActivity(String detected_activities) {
        Intent intent = new Intent(TrackingLib.AR_BROADCAST_DETECTED_ACTIVITY);
        intent.putExtra("detected_activities", detected_activities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
