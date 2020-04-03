package enklikanketa.com.a1kapanel.Services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.GeofencingLib;
import enklikanketa.com.a1kapanel.Libraries.NotificationLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.Tasks.sendTriggeredGeofencesTask;

public class GeofenceTransitionsIS extends IntentService {
    private String TAG = "GeofenceTransitionsIS";
    GeofencingLib gLib;
    Database DBH;

    public GeofenceTransitionsIS() {
        super("GeofenceTransitionsIS");
    }

    protected void onHandleIntent(Intent intent) {
        gLib = new GeofencingLib(this);
        DBH = (Database) Database.getInstance(this);
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = gLib.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        executeTriggeedGeofences(geofencingEvent.getTriggeringGeofences(), geofenceTransition);
}

    /**
     * Execute procces when geofence is triggered (show notification ect)
     * @param triggeringGeofences - arraylist of geofences that were triggered
     * @param transition - geofences transition index
     */
    private void executeTriggeedGeofences(List<Geofence> triggeringGeofences, int transition){
        //iterate trough all triggering geofences
        for (Geofence geofence : triggeringGeofences) {
            //get id of geofence
            String id = getGeofenceIDFromRequestID(geofence.getRequestId());
            //check if geofence with that id exists in DB
            HashMap<String, String> geofenceMap = DBH.getRowHashMapData("geofences", null, "id="+id);

            if(geofenceMap != null){
                //we have to trigger on dwell
                if(geofenceMap.get("on_transition").equals("dwell") && transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                    geofenceMap.put("dwell_timestamp", System.currentTimeMillis()/1000+"");
                    executeAfterTriggered(geofenceMap);
                }
                //trigger is on exit, however, we have to save timestamp of dwell, so we know how much time are we in geofence
                else if(geofenceMap.get("on_transition").equals("exit") && transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                    ContentValues cv = new ContentValues();
                    cv.put("dwell_timestamp", System.currentTimeMillis()/1000);
                    DBH.updateData("geofences", cv, "id="+id);
                }
                //triger is on exit. if we were enought time dwelling in geofence, trigger it
                else if(geofenceMap.get("on_transition").equals("exit") && transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                    String[] dwell_timestamp = DBH.getRowData("geofences", new String[]{"dwell_timestamp"}, "id=" + id);
                    if (Integer.parseInt(dwell_timestamp[0]) > 0)
                        executeAfterTriggered(geofenceMap);
                }
                //save timestamp of enter just for more info
                else if(transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    ContentValues cv = new ContentValues();
                    cv.put("enter_timestamp", System.currentTimeMillis()/1000);
                    cv.put("dwell_timestamp", 0);
                    DBH.updateData("geofences", cv, "id="+id);
                }
            }
            //if geofence with this id does not exists, remove it from register
            else{
                ArrayList<String> geofences_ids = new ArrayList<>();
                geofences_ids.add(id);
                gLib.removeGeofences(geofences_ids);
            }
        }
    }

    /**
     * Get ID of geofence from its requestID
     * @param requestID - requestID of geofence
     * @return id of geofence (in DB - whitout prefix)
     */
    private String getGeofenceIDFromRequestID(String requestID){
        int prefix_length = getString(R.string.geofence_id_prefix).length();
        return requestID.substring(prefix_length);
    }

    /**
     * Execute stuff after geofence is triggered (show notification, send data to server,...)
     * @param geofence - hashmap of data of geofence
     */
    private void executeAfterTriggered (final HashMap<String, String> geofence){
        geofence.put("tsSec", System.currentTimeMillis()/1000+"");
        if(geofence.get("location_triggered").equals("0"))
            postTriggeredGeofence(geofence, -1);
        else{
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            try {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if(location != null) {
                            long loc_id = new TrackingLib(getBaseContext()).storeLocationData(location, 2);
                            postTriggeredGeofence(geofence, loc_id);
                        }
                    }
                });
            } catch (SecurityException unlikely) {
                Log.e(TAG, "Lost location permission." + unlikely);
                postTriggeredGeofence(geofence, -1);
            }
        }
    }

    /**
     * Show notification of triggered geofence
     * @param geofence - hashmap of data of geofence
     */
    private void showNotification(HashMap<String, String> geofence){
        HashMap<String, String> data = new HashMap<>();

        Database DB = (Database) Database.getInstance(this);
        String[] link = DB.getRowData("surveys", new String[]{"link"},
                "id=" + geofence.get("srv_id"));

        if (!geofence.isEmpty()) {
            data.put("link", link[0]);
            data.put("title", geofence.get("notif_title"));
            data.put("message", geofence.get("notif_message"));
            data.put("sound", geofence.get("notif_sound"));
            data.put("sender_id", null);
            data.put("geof_id", geofence.get("tgeof_id"));
            data.put("mode", "geofence");

            data.put("geof_version", geofence.get("tsSec"));
            //show notification
            NotificationLib nlib = new NotificationLib(this);
            nlib.showNotificationSurvey(data);
        }
    }

    /**
     * Post data of triggered geofence to server
     * @param geofence - hashmap of data of geofence
     * @param loc_id - location ID pined to triggered geofence
     */
    private void postTriggeredGeofence(final HashMap<String, String> geofence, long loc_id) {
        JSONObject tgeosObj = new JSONObject();
        try {
            tgeosObj.put("value", geofence.get("id"));
            tgeosObj.put("tsSec", Long.parseLong(geofence.get("tsSec")));
            tgeosObj.put("loc_id", loc_id);
            tgeosObj.put("enter_timestamp", geofence.get("enter_timestamp"));
            tgeosObj.put("dwell_timestamp", geofence.get("dwell_timestamp"));
        } catch (JSONException e) {
            GeneralLib.reportCrash(e, tgeosObj.toString());
        }

        sendTriggeredGeofencesTask.TaskListener taskListener = null;
        if(geofence.get("trigger_survey").equals("1"))
            taskListener = new sendTriggeredGeofencesTask.TaskListener() {
                @Override
                public void onFinished(int tgeof_id) {
                    Log.d(TAG, "wewe onFinished "+geofence);
                    geofence.put("tgeof_id", ""+tgeof_id);
                    if(tgeof_id != 0)
                        showNotification(geofence);
                }
            };
        new sendTriggeredGeofencesTask(this, tgeosObj, taskListener).execute();

        ContentValues cv = new ContentValues();
        cv.put("enter_timestamp", 0);
        cv.put("dwell_timestamp", 0);
        DBH.updateData("geofences", cv, "id="+geofence.get("id"));
    }
}