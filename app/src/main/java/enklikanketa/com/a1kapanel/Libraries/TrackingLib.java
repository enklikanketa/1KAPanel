package enklikanketa.com.a1kapanel.Libraries;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import enklikanketa.com.a1kapanel.PrefsActivity;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.Receivers.AlarmLocationsJobReceiver;
import enklikanketa.com.a1kapanel.Receivers.GpsLocationReceiver;
import enklikanketa.com.a1kapanel.Services.BackgroundDetectedActivitiesService;
import enklikanketa.com.a1kapanel.Services.LocationUpdatesService;
import enklikanketa.com.a1kapanel.Services.LocationsJobService;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.Tasks.sendGetTrackingTask;
import enklikanketa.com.a1kapanel.Tasks.sendSetTrackingPermission;
import enklikanketa.com.a1kapanel.Tasks.sendTrakingLocationsTask;
import enklikanketa.com.a1kapanel.Tasks.sendUnsubscribeSurvey;

import static android.content.Context.ALARM_SERVICE;

public class TrackingLib {
    private String TAG = "TrackingLib";
    private Context ctx;
    private Database DBH;

    public static String ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING";
    public static String ACTION_PAUSE_TRACKING = "ACTION_PAUSE_TRACKING";
    public static String ACTION_START_TRACKING = "ACTION_START_TRACKING";
    public static String ACTION_RUN_TRACKING = "ACTION_RUN_TRACKING";

    private int LOC_JOB_ID = 63940656;
    private int JOB_INTERVAL = 15*60*1000;

    //Activity Recognition constants
    public static final String AR_BROADCAST_DETECTED_ACTIVITY = "activity_intent";

    public TrackingLib(Context base) {
        ctx = base;
        DBH = (Database) Database.getInstance(ctx);
    }

    /**
     * Set, update activity based on new data
     *
     * @param data - Data of sended activity tracking
     */
    public void setOrUpdateNewTracking(Map<String, String> data){
        insertOrUpdateTrackingDB(data);
    }

    /**
     * Insert or update tracking in DB
     *
     * @param tracking - Map of data of tracking data to store in DB
     */
    private void insertOrUpdateTrackingDB(Map<String, String> tracking) {
        String ank_id = tracking.get("ank_id");

        try {
            ContentValues cv = new ContentValues();

            JSONObject trackingO = new JSONObject(tracking.get("tracking"));
            String id_act = trackingO.getString("id");
            cv.put("activity_recognition", trackingO.getString("activity_recognition"));
            cv.put("tracking_accuracy", trackingO.getString("tracking_accuracy"));
            cv.put("interval_wanted", trackingO.getString("interval_wanted"));
            cv.put("interval_fastes", trackingO.getString("interval_fastes"));
            cv.put("displacement_min", trackingO.getString("displacement_min"));
            cv.put("ar_interval_wanted", trackingO.getString("ar_interval_wanted"));
            cv.put("srv_id", tracking.get("ank_id"));
            if(tracking.get("permission") != null && tracking.get("permission").equals("1"))
                cv.put("permission", 1);

            int actExists = DBH.countData("tracking", "id="+id_act);

            if(actExists == 0) {
                ContentValues initialValues = new ContentValues();
                initialValues.put("id", ank_id);
                initialValues.put("link", tracking.get("link"));
                initialValues.put("title", tracking.get("srv_title"));

                //insert new survey in DB, if already exists, id is -1
                DBH.getWritableDatabase().insertWithOnConflict("surveys", null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);

                cv.put("id", id_act);
                DBH.insertData("tracking", cv);

                if(tracking.get("permission") == null || !tracking.get("permission").equals("1")) {
                    tracking.put("title", ctx.getString(R.string.tracking_notification_permission_title));
                    tracking.put("message", ctx.getString(R.string.tracking_notification_permission_desc) + tracking.get("srv_title"));
                    tracking.put("sound", "1");

                    //show notification
                    NotificationLib nlib = new NotificationLib(ctx);
                    nlib.showNotificationSurvey(tracking);
                }
            }
            else {
                DBH.updateData("tracking", cv, "id=" + id_act);

                //just in case, update survey data - pretty link or change of title
                ContentValues initialValues = new ContentValues();
                initialValues.put("link", tracking.get("link"));
                initialValues.put("title", tracking.get("srv_title"));
                DBH.updateData("surveys", initialValues, "id=" + ank_id);

                stopTracking(null);
                startTracking("Tracking updated");
            }

        } catch (JSONException e) {
            Log.e(TAG, "TrackingLib.insertTrackingDB() - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }
    }

    /**
     * Check granted tracking permission in DB
     *
     * @param srv_id - survey id of activity
     */
    public void trackingPermissionGrantedDB(String srv_id) {
        ContentValues cv = new ContentValues();
        cv.put("permission", 1);
        DBH.updateData("tracking", cv, "srv_id="+srv_id);
    }

    /**
     * Refresh tracking - removing/clearing all activities and getting them from server
     */
    public void refreshTracking() {
        new sendGetTrackingTask(ctx, 0).execute();
    }

    /**
     * Check if tracking is granted and running
     */
    public boolean areTrackingPermissionGrantedAndRunning() {
        int actExists = DBH.countData("tracking", "permission="+1);
        return actExists > 0;
    }

    /**
     * Check how many tracking surveys is granted
     */
    public int howManyTrackingPermissionGranted() {
        return DBH.countData("tracking", "permission="+1);
    }

    /**
     * Check if user did not permit tracking and show alert
     *
     * @param acti - Activity
     */
    public AlertDialog showNonPermissionedTracking(Activity acti) {
        AlertDialog myDialog = null;

        Database DBH = (Database) Database.getInstance(ctx);
        ArrayList<HashMap<String, String>> actList = DBH.getListHashMapData("tracking", new String[] {"srv_id"}, "permission=0", null);

        if(actList != null)
            for(HashMap<String, String> act : actList){
                //get title of survey
                String[] srv_title = DBH.getRowData("surveys", new String[]{"title"}, "id="+act.get("srv_id"));
                myDialog = trackingPermissionDialog(acti, srv_title[0], act.get("srv_id"));
            }
            return myDialog;
    }

    /**
     * Show dialog, for showing permission for tracking
     *
     * @param act - Activity
     * @param title - title of survey
     * @param srv_id - survey id
     */
    private AlertDialog trackingPermissionDialog(final Activity act, String title, final String srv_id) {
        final AlertDialog myDialog = new AlertDialog.Builder(ctx)
                .setTitle(ctx.getString(R.string.tracking_permission_alert_title))
                .setMessage(GeneralLib.fromHtml(ctx.getString(R.string.tracking_permission_alert_desc, title)))
                .setNegativeButton(R.string.unsubscribe, null)
                .setPositiveButton(R.string.iagree, null).create();

        myDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button btne = myDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                btne.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        myDialog.dismiss();
                        new sendSetTrackingPermission(act, srv_id, "0").execute();
                        new sendUnsubscribeSurvey(act, srv_id).execute();
                    }
                });

                Button btpo = myDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btpo.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        myDialog.dismiss();
                        new sendSetTrackingPermission(act, srv_id, "1").execute();
                    }
                });
            }
        });
        myDialog.show();

        return myDialog;
    }//end of permission

    /**
     * Returns true if this is a foreground service.
     */
    public boolean serviceIsRunningInForeground() {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(
                Context.ACTIVITY_SERVICE);
        if (manager != null)
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                    Integer.MAX_VALUE)) {
                if (LocationUpdatesService.CLASS_NAME.equals(service.service.getClassName())) {
                    if (service.foreground) {
                        return true;
                    }
                }
            }
        return false;
    }

    /**
     * Start service based on API level
     * @param mIntent - intent to start
     */
    public void startForegroundService(Intent mIntent){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ContextCompat.startForegroundService(ctx, mIntent);
        else
            ctx.startService(mIntent);
    }

    /**
     * Binds location update service and register receiver
     */
    public void startTracking(String event){
        if(isCanTrack()) {
            if (!serviceIsRunningInForeground()){
                Intent startIntent = new Intent(ctx, LocationUpdatesService.class);
                startIntent.setAction(LocationUpdatesService.STARTFOREGROUND_ACTION);
                startForegroundService(startIntent);
                Intent startIntent1 = new Intent(ctx, BackgroundDetectedActivitiesService.class);
                startIntent1.setAction(LocationUpdatesService.STARTFOREGROUND_ACTION);
                startForegroundService(startIntent1);
            }
            else{
                Intent startIntent = new Intent(ctx, LocationUpdatesService.class);
                startIntent.setAction(LocationUpdatesService.RESUMEFOREGROUND_ACTION);
                startForegroundService(startIntent);
                Intent startIntent1 = new Intent(ctx, BackgroundDetectedActivitiesService.class);
                startIntent1.setAction(LocationUpdatesService.RESUMEFOREGROUND_ACTION);
                startForegroundService(startIntent1);
            }

            LocalBroadcastManager.getInstance(ctx).registerReceiver(new TrackingLib.MyReceiver(),
                    new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));

            PrefsActivity.postTrackingLog(ctx, "START Tracking",
                    event + GpsLocationReceiver.getBestLocationProviderLog(ctx));

            //start scheduling job to periodically send data to server
            scheduleJob();
        }
        else {
            //show notification
            NotificationLib nlib = new NotificationLib(ctx);
            nlib.showNotificationRunLocationTracking();
        }
    }

    public HashMap<String, String> getLocationTrackingSettings(){
        return DBH.getRowHashMapData("tracking", new String[]{"tracking_accuracy",
                "interval_wanted", "interval_fastes", "displacement_min"}, null);
    }

    public HashMap<String, String> getARTrackingSettings(){
        return DBH.getRowHashMapData("tracking", new String[]{"activity_recognition",
                "ar_interval_wanted"}, null);
    }

    public void cancelTracking(String srv_id, String event){
        DBH.deleteRows("tracking", "srv_id = " + srv_id);
        stopTracking(event);
        new sendTrakingLocationsTask(ctx).execute();
    }

    public void stopTracking(String event){
        cancelForegroundTracking(event, LocationUpdatesService.STOPFOREGROUND_ACTION);

        //if there are permissions granted, show notification to start tracking
        if(areTrackingPermissionGrantedAndRunning()){
            //show notification
            NotificationLib nlib = new NotificationLib(ctx);
            nlib.showNotificationRunLocationTracking();
        }
    }

    public void pauseTracking(String event){
        cancelForegroundTracking(event, LocationUpdatesService.PAUSEFOREGROUND_ACTION);
    }

    private void cancelForegroundTracking(String event, String action){
        Intent stopIntent = new Intent(ctx, LocationUpdatesService.class);
        stopIntent.setAction(action);
        Intent stopIntent1 = new Intent(ctx, BackgroundDetectedActivitiesService.class);
        stopIntent1.setAction(action);

        startForegroundService(stopIntent);
        startForegroundService(stopIntent1);

        if(event != null)
            PrefsActivity.postTrackingLog(ctx, "STOP Tracking", event);
        cancelJobScheduler();
    }

    /**
     * Store location data in DB
     * @param location - location to store
     * @return ID of inserted location
     */
    public long storeLocationData(Location location){
        return storeLocationDataBasic(location, 0);
    }

    /**
     * Store location data in DB
     * @param location - location to store
     * @param sending_flag - int of sending flag (0-not sent, 1-in sending process, 2-already sent)
     * @return ID of inserted location
     */
    public long storeLocationData(Location location, int sending_flag){
        return storeLocationDataBasic(location, sending_flag);
    }

    /**
     * Store location data in DB
     * @param location - location to store
     * @param sending_flag - int of sending flag (0-not sent, 1-in sending process, 2-already sent)
     * @return ID of inserted location
     */
    private long storeLocationDataBasic (Location location, int sending_flag){
        //get all available data of location
        ContentValues cvl = new ContentValues();
        cvl.put("lat", location.getLatitude());
        cvl.put("lng", location.getLongitude());
        cvl.put("provider", location.getProvider());
        cvl.put("timestamp", location.getTime()/1000);
        cvl.put("is_mock", location.isFromMockProvider());
        cvl.put("sending", sending_flag);
        if(location.hasAccuracy())
            cvl.put("accuracy", location.getAccuracy()+"");
        if(location.hasAltitude())
            cvl.put("altitude", location.getAltitude()+"");
        if(location.hasBearing())
            cvl.put("bearing", location.getBearing()+"");
        if(location.hasSpeed())
            cvl.put("speed", location.getSpeed());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (location.hasBearingAccuracy())
                cvl.put("bearing_acc", location.getBearingAccuracyDegrees()+"");
            if (location.hasSpeedAccuracy())
                cvl.put("speed_acc", location.getSpeedAccuracyMetersPerSecond()+"");
            if (location.hasVerticalAccuracy())
                cvl.put("vertical_acc", location.getVerticalAccuracyMeters()+"");
        }

        //get extras of location if exists
        Bundle bundle = location.getExtras();
        if (bundle != null) {
            String extrass = "";
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if(!key.equals("mockLocation"))
                    extrass += String.format("%s: %s; ", key,
                            (value != null ? value.toString() : "null"));
            }
            cvl.put("extras", extrass);
        }
        //store location data in DB
        return DBH.insertData("locations", cvl);
    }

    /**
     * Store activity recognition data in DB
     * @param ARresult - result of activity recognition to store
     */
    public void storeARData (ActivityRecognitionResult ARresult){
        ArrayList<DetectedActivity> detectedActivities = (ArrayList<DetectedActivity>) ARresult.getProbableActivities();

        //get all available data of location
        ContentValues cva = new ContentValues();
        cva.put("timestamp", ARresult.getTime()/1000);

        //iterate trought activities >0
        for (DetectedActivity activity : detectedActivities) {
            switch (activity.getType()) {
                case DetectedActivity.IN_VEHICLE: {
                    cva.put("in_vehicle", activity.getConfidence());
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    cva.put("on_bicycle", activity.getConfidence());
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    cva.put("on_foot", activity.getConfidence());
                    break;
                }
                case DetectedActivity.RUNNING: {
                    cva.put("running", activity.getConfidence());
                    break;
                }
                case DetectedActivity.STILL: {
                    cva.put("still", activity.getConfidence());
                    break;
                }
                case DetectedActivity.TILTING: {
                    cva.put("tilting", activity.getConfidence());
                    break;
                }
                case DetectedActivity.WALKING: {
                    cva.put("walking", activity.getConfidence());
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    cva.put("unknown", activity.getConfidence());
                    break;
                }
            }
        }

        //store AR data in DB
        DBH.insertData("activity_recognition", cva);
    }

    /**
     * Get number of locations that has not been sended yet
     */
    public int getNumberOfUnsendedLocations() {
        return DBH.countData("locations", "sending=0");
    }

    /**
     * Get number of activity recognitions that has not been sended yet
     */
    public int getNumberOfUnsendedAR() {
        return DBH.countData("activity_recognition", "sending=0");
    }

    /**
     * Start scheduling job for AR and locations
     */
    private void scheduleJob() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler =
                    (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(jobScheduler != null)
                jobScheduler.schedule(new JobInfo.Builder(LOC_JOB_ID,
                        new ComponentName(ctx, LocationsJobService.class))
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPeriodic(JOB_INTERVAL)
                        .build());
        }
        else {
            Intent mintent = new Intent(ctx, AlarmLocationsJobReceiver.class);
            PendingIntent sender = PendingIntent.getBroadcast(ctx, LOC_JOB_ID, mintent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarm1 = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
            if(alarm1 != null)
                alarm1.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + JOB_INTERVAL, JOB_INTERVAL, sender);
        }
    }

    /**
     * cancel locations jobScheduler and alarms
     */
    private void cancelJobScheduler(){
        //cancel jobScheduler
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            JobScheduler jobScheduler =
                    (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if(jobScheduler != null)
                jobScheduler.cancel(LOC_JOB_ID);
        }
        if(PendingIntent.getBroadcast(ctx, LOC_JOB_ID,
                new Intent(ctx, AlarmLocationsJobReceiver.class), PendingIntent.FLAG_NO_CREATE) != null)
            new AlarmLib(ctx).cancelAlarm(AlarmLocationsJobReceiver.class, LOC_JOB_ID);
    }

    /**
     * Delete location from DB - server too
     * @param location_id - id of location in local DB
     */
    public void deleteLocation(String location_id){
        ContentValues cv = new ContentValues();
        cv.put("lat", "999");
        cv.put("lng", "999");
        cv.putNull("provider");
        cv.putNull("is_mock");
        cv.putNull("accuracy");
        cv.putNull("altitude");
        cv.putNull("bearing");
        cv.putNull("speed");
        cv.putNull("bearing_acc");
        cv.putNull("speed_acc");
        cv.putNull("vertical_acc");
        cv.put("extras", System.currentTimeMillis()/1000);
        cv.put("edited", 2);
        cv.put("sending", 0);
        DBH.updateData("locations", cv, "id="+location_id);

        if(!serviceIsRunningInForeground())
            new sendTrakingLocationsTask(ctx).execute();
    }

    /**
     * Edit location - server too
     * @param marker - marker of location to edit
     */
    public void editLocation(Marker marker){
        ContentValues cv = new ContentValues();
        cv.put("lat", marker.getPosition().latitude);
        cv.put("lng", marker.getPosition().longitude);
        cv.put("provider", "manual");
        cv.put("edited", 1);
        cv.put("sending", 0);
        DBH.updateData("locations", cv, "id="+marker.getTag());

        if(!serviceIsRunningInForeground())
            new sendTrakingLocationsTask(ctx).execute();
    }

    /**
     * Stores all ids of locations to arraylist
     * @param jsonArr - JSONArray from where we get IDs
     * @param listToStoreIn - ArrayList to add IDs in
     * @return Edited ArrayList (listToStoreIn)
     */
    public ArrayList<String> storeLocIDs(JSONArray jsonArr, ArrayList<String> listToStoreIn){
        if(jsonArr != null) {
            try {
                for (int i = 0; jsonArr.length() > i; i++) {
                    listToStoreIn.add(jsonArr.getJSONObject(i).getString("id"));
                }
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, jsonArr.toString());
            }
        }
        return listToStoreIn;
    }

    /**
     * Convert ArrayList to string and change brackets
     * @param arrToConvert ArrayList to convert
     * @return String of converted ArrayList
     */
    public String convertArrayToStringForWhere (ArrayList<String> arrToConvert) {
        return arrToConvert.toString().replace("[","(").replace("]",")");
    }

    public void setCanTrack(boolean doTrack) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("enklikanketa.com.a1kapanel.TRACK", doTrack);
        editor.apply();
    }

    public boolean isCanTrack() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return pref.getBoolean("enklikanketa.com.a1kapanel.TRACK", false);
    }

    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION);
            if (location != null) {
                //Log.d(TAG, LocationUpdatesService.getLocationText(location));
            }
        }
    }
}
