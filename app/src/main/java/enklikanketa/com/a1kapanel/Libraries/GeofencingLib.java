package enklikanketa.com.a1kapanel.Libraries;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.Services.GeofenceTransitionsIS;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.Tasks.sendGetGeofencesTask;
import enklikanketa.com.a1kapanel.Tasks.sendTrakingLocationsTask;

import static enklikanketa.com.a1kapanel.Tasks.sendGetGeofencesTask.saveGetGeofencesLog;

/**
 * Created by podkrizniku on 06/12/2017.
 */

public class GeofencingLib {

    private String TAG = "GeofencingLib";
    private Context ctx;
    private Database DBH;
    private GeofencingClient mGeofencingClient;
    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;

    public GeofencingLib(Context base) {
        ctx = base;
        DBH = (Database) Database.getInstance(base);
        mGeofencingClient = LocationServices.getGeofencingClient(base);
        mGeofenceList = new ArrayList<>();
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    /**
     * Add geofence to list to later run it
     * @param geofence JSONObject of geofence to add
     */
    private void addGeofenceToList(JSONObject geofence){
        try {
            int after_ms = geofence.getInt("after_seconds");
            Geofence.Builder geoBuilder = new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(ctx.getString(R.string.geofence_id_prefix)+geofence.getString("id"))

                    .setCircularRegion(
                            Double.parseDouble(geofence.getString("lat")),
                            Double.parseDouble(geofence.getString("lng")),
                            Float.parseFloat(geofence.getString("radius"))
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(after_ms*100)
                    .setLoiteringDelay(after_ms*1000);

            //if transition is only dwell
            if(geofence.getString("on_transition").equals("dwell"))
                geoBuilder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_DWELL);
            //if transition is exit, we need both dwell and exit
            else
                geoBuilder.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_DWELL |
                        Geofence.GEOFENCE_TRANSITION_EXIT);

            mGeofenceList.add(geoBuilder.build());
        } catch(JSONException e){
            Log.e(TAG, "GeofencingLib.addGeofenceToList() - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(ctx, GeofenceTransitionsIS.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(ctx, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    /**
     * Get all geofences stored in DB
     * @return JSONArray of all geofences stored in DB
     */
    private JSONArray getGeofencesFromDB(){
        return DBH.getJSONData("geofences", null, null);
    }

    /**
     * Re run all geofences stored in DB
     */
    public void reRunGeofences(){
        removeAllGeofences();
        JSONArray geofences = getGeofencesFromDB();
        if(geofences != null) {
            try {
                for (int i = 0; i < geofences.length(); i++) {
                    //geofences.getJSONObject(i).put("ank_id", geofences.getJSONObject(i).getString("srv_id"));
                    addGeofenceToList(geofences.getJSONObject(i));
                }
                //if geofences were added to list, run them
                if(!mGeofenceList.isEmpty())
                    addGeofences();
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, geofences.toString());
            }
        }
    }

    /**
     * Run all previously added geofences in list (with function addGeofenceToList)
     */
    private void addGeofences() {
        try {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Geofences added
                            Log.d(TAG, "geofences added");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to add geofences
                            Log.d(TAG, "geofences failed to add: "+e);
                            saveGetGeofencesLog(ctx);
                        }
                    });
        } catch(SecurityException e){
            Log.e(TAG, "GeofencingLib.addGeofences() - Error: " + e.getMessage());
            saveGetGeofencesLog(ctx);
        }
    }

    /**
     * Removes all registered geofences of this app
     */
    private void removeAllGeofences(){
        mGeofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences removed
                        // All ok
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to remove geofences
                        Log.d(TAG, "geofences failed to remove: "+e);
                        saveGetGeofencesLog(ctx);
                    }
                });
    }

    /**
     * Removes geofences by their request IDs. Request ID is specified when you create a Geofence by calling setRequestId(String).
     * @param geofenceRequestIds - arraylist of request IDs of geofences to remove
     */
    public void removeGeofences(ArrayList<String> geofenceRequestIds){
        mGeofencingClient.removeGeofences(addPrefixGeofenceIds(geofenceRequestIds))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences removed
                        // All ok
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to remove geofences
                        // Save log to refresh all geofences latter
                        Log.d(TAG, "geofences failed to remove: "+e);
                        saveGetGeofencesLog(ctx);
                    }
                });
    }

    /**
     * Add prefixes to geofences ids to get request IDs for those geofences
     * @param ids - ids of geofences
     * @return - arraylist of geofences request IDs (ids with prefixes)
     */
    private ArrayList<String> addPrefixGeofenceIds (ArrayList<String> ids){
        ArrayList<String> prefixedIds = new ArrayList<>();
        for (String id : ids)
            prefixedIds.add(ctx.getString(R.string.geofence_id_prefix) + id);
        return prefixedIds;
    }

    /**
     * Returns the error string for a geofencing error code.
     */
    public String getErrorString(Context context, int errorCode) {
        Resources mResources = context.getResources();
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return mResources.getString(R.string.geofence_not_available);
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return mResources.getString(R.string.geofence_too_many_geofences);
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return mResources.getString(R.string.geofence_too_many_pending_intents);
            default:
                return mResources.getString(R.string.unknown_geofence_error);
        }
    }

    /**
     * Set, update or delete geofences based on new data
     *
     * @param data - Data of sended geofences
     */
    public void setOrUpdateNewGeofences(Map<String, String> data) {
        String ank_id = data.get("ank_id");

        ContentValues initialValues = new ContentValues();
        initialValues.put("id", ank_id);
        initialValues.put("link", data.get("link"));
        initialValues.put("title", data.get("srv_title"));

        //insert new survey in DB, if already exists, id is -1
        DBH.getWritableDatabase().insertWithOnConflict("surveys", null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);

        //is geofence in DB for that survey
        ArrayList<String> geofences_ids = DBH.getListOfOneColumn("geofences",
                "id", "srv_id='" + ank_id + "'");

        try {
            JSONArray geofences = new JSONArray(data.get("geofences"));
            if (geofences.length() > 0) {
                for (int i = 0; i < geofences.length(); i++) {
                    JSONObject geofence = geofences.getJSONObject(i);
                    geofence.put("ank_id", ank_id);
                    if (geofences_ids.contains(geofence.getString("id"))) {
                        updateGeofenceDB(geofence);
                        geofences_ids.remove(geofence.getString("id"));
                        addGeofenceToList(geofence);
                    } else {
                        insertGeofenceDB(geofence);
                        addGeofenceToList(geofence);
                    }
                }
                //if geofences were added to list, run them
                if(!mGeofenceList.isEmpty())
                    addGeofences();
                //delete all geofences from DB and register that remains in geofences_ids
                //that means geofences were deleted on server app
                if(!geofences_ids.isEmpty()) {
                    for (String id : geofences_ids)
                        deleteGeofenceDB(id, ank_id);
                    removeGeofences(geofences_ids);
                }
            }
            //empty, remove//canel all geofences for this survey
            else
                cancelGeofences(ank_id);

        } catch (JSONException e) {
            Log.e(TAG, "GeofencingLib.setOrUpdateNewGeofences() - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }
    }

    /**
     * Calcel and delete all geofences for this survey
     * @param srv_id - survey to cancel all geofences of
     */
    public void cancelGeofences(String srv_id){
        clearAllGeofences(srv_id);
    }

    /**
     * Insert geofence in DB
     *
     * @param geofence - JSONObject of data of geofence to store in DB
     */
    private void insertGeofenceDB(JSONObject geofence) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("id", geofence.getString("id"));
            cv.put("srv_id", geofence.getString("ank_id"));
            cv.put("address", geofence.getString("address"));
            if(!geofence.getString("name").equals("null") && !geofence.getString("name").equals(""))
                cv.put("name", geofence.getString("name"));
            else
                cv.putNull("name");
            cv.put("lat", geofence.getString("lat"));
            cv.put("lng", geofence.getString("lng"));
            cv.put("radius", geofence.getString("radius"));
            cv.put("notif_title", geofence.getString("notif_title"));
            cv.put("notif_message", geofence.getString("notif_message"));
            cv.put("notif_sound", geofence.getString("notif_sound"));
            cv.put("on_transition", geofence.getString("on_transition"));
            cv.put("after_seconds", geofence.getString("after_seconds"));
            cv.put("location_triggered", geofence.getString("location_triggered"));
            //trigger_survey can be null or datetime!
            cv.put("trigger_survey", geofence.isNull("trigger_survey") ?
                    0 : geofence.getString("trigger_survey").equals("0") ? 0 : 1);
            DBH.insertData("geofences", cv);
        } catch (JSONException e) {
            Log.e(TAG, "GeofencingLib.insertGeofenceDB() - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }
    }

    /**
     * Update geofence in DB
     *
     * @param geofence - JSONObject of data of geofence to update in DB
     */
    private void updateGeofenceDB(JSONObject geofence) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("address", geofence.getString("address"));
            if(!geofence.getString("name").equals("null") && !geofence.getString("name").equals(""))
                cv.put("name", geofence.getString("name"));
            else
                cv.putNull("name");
            cv.put("lat", geofence.getString("lat"));
            cv.put("lng", geofence.getString("lng"));
            cv.put("radius", geofence.getString("radius"));
            cv.put("notif_title", geofence.getString("notif_title"));
            cv.put("notif_message", geofence.getString("notif_message"));
            cv.put("notif_sound", geofence.getString("notif_sound"));
            cv.put("on_transition", geofence.getString("on_transition"));
            cv.put("after_seconds", geofence.getString("after_seconds"));
            cv.put("location_triggered", geofence.getString("location_triggered"));
            //trigger_survey can be null or datetime!
            cv.put("trigger_survey", geofence.isNull("trigger_survey") ?
                    0 : geofence.getString("trigger_survey").equals("0") ? 0 : 1);
            DBH.updateData("geofences", cv, "id='" + geofence.getInt("id") + "'");
        } catch (JSONException e) {
            Log.e(TAG, "GeofencingLib.updateGeofenceDB() - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }
    }

    /**
     * Delete geofence from DB
     *
     * @param id     - id of geofence
     * @param srv_id - id of survey
     */
    private void deleteGeofenceDB(String id, String srv_id) {
        DBH.deleteRows("geofences", "id='" + id + "'");
        if (DBH.countData("repeaters", "srv_id='" + srv_id + "'") == 0 &&
                DBH.countData("geofences", "srv_id='" + srv_id + "'") == 0 &&
                DBH.countData("activities", "srv_id='" + srv_id + "'") == 0)
            GeneralLib.deleteSurveyDB(ctx, srv_id);
    }

    /**
     * Delete all geofences from DB for this survey
     *
     * @param srv_id - id of survey, parent of geofences
     */
    private void deleteAllGeofencesDB(String srv_id) {
        DBH.deleteRows("geofences", "srv_id='" + srv_id + "'");
        if (DBH.countData("repeaters", "srv_id='" + srv_id + "'") == 0 &&
            DBH.countData("activities", "srv_id='" + srv_id + "'") == 0)
            GeneralLib.deleteSurveyDB(ctx, srv_id);
    }

    /**
     * Delete all geofences from DB for this survey
     *
     * @param srv_ids - arrasyslist of all survey ids to remove from DB
     */
    private void deleteAllGeofencesDB(ArrayList <String> srv_ids) {
        if(!srv_ids.isEmpty()) {
            String ids = srv_ids.toString().substring(1, srv_ids.toString().length()-1);
            DBH.deleteRows("geofences", "srv_id IN ("+ids+")");
            if (DBH.countData("repeaters", "srv_id IN ("+ids+")") == 0)
                GeneralLib.deleteSurveysDB(ctx, ids);
        }
    }

    /**
     * Delete all geofences from DB
     */
    private void deleteAllGeofencesDB() {
        //a row(s) of survey(s) in sueveys table can hang if there is no repeaters and no geofences
        // for a survey(s)
        DBH.deleteAllRows("geofences");
    }

    /**
     * Creal/remove all geofences from DB and register
     */
    private void clearAllGeofences(){
        deleteAllGeofencesDB();
        removeAllGeofences();
    }

    /**
     * Creal/remove all geofences from DB and register for given survey
     * @param srv_id - survey id to remove geofences of
     */
    private void clearAllGeofences(String srv_id){
        //get arraylist of geofences id for this survey
        ArrayList<String> ids = DBH.getListOfOneColumn("geofences", "id", "srv_id='"+srv_id+"'");
        //delete all geofences of this survey from DB
        deleteAllGeofencesDB(srv_id);
        //remove all geofences for this survey from register
        removeGeofences(ids);
    }

    /**
     * Run all geofences stored in DB
     */
    public void runAllGeofencesFromDB(){
        //is geofence in DB for that survey
        JSONArray geofences = DBH.getJSONData("geofences",
                null, null);

        try {
            if (geofences !=null && geofences.length() > 0) {
                for (int i = 0; i < geofences.length(); i++) {
                    JSONObject geofence = geofences.getJSONObject(i);
                    //geofence.put("ank_id", geofence.getString("srv_id"));
                    addGeofenceToList(geofence);
                }
                //if geofences were added to list, run them
                if (!mGeofenceList.isEmpty())
                    addGeofences();
            }
        } catch (JSONException e) {
            Log.e(TAG, "GeofencingLib.runAllGeofencesFromDB() - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }
    }

    /**
     * Refresh geofences - removing/clearing all geofences and getting them from server
     */
    public void refreshGeofences() {
        clearAllGeofences();
        new sendGetGeofencesTask(ctx).execute();
    }

    /**
     * Save last known location and pin it to geofence
     */
    public void saveLastKnownLocation(final String geo_id){
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(ctx);
        try {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if(location != null) {
                        new TrackingLib(ctx).storeLocationData(location);
                        new sendTrakingLocationsTask(ctx).execute();
                    }
                }
            });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }
}
