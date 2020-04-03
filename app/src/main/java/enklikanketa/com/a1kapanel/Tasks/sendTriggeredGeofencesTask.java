package enklikanketa.com.a1kapanel.Tasks;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.NotificationLib;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 07/12/2017.
 */

public class sendTriggeredGeofencesTask extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Context ctx;
    private JSONObject triggered_geofence;
    private Database DB;
    private int old_geos_num = 0;

    // This is the reference to the associated listener
    private final TaskListener taskListener;

    /**
     * @param context - Context
     * @param triggered_geofence - JSONObject of geofence with timestamp
     */
    public sendTriggeredGeofencesTask(Context context, JSONObject triggered_geofence,
                                      TaskListener listener) {
        ctx = context.getApplicationContext();
        this.triggered_geofence = triggered_geofence;
        this.taskListener = listener;

        DB = (Database) Database.getInstance(ctx);
    }

    public interface TaskListener {
        void onFinished(int geof_id);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        int tgeof_id = 0;
        if (result != null) {
            if(!result.equals("no logs")) {
                try {
                    JSONObject obj = new JSONObject(result);

                    if (obj.has("note")) {
                        //remove all data of logs from DB
                        DB.deleteRows("things_to_send", "name = 'triggered_geofences'");
                        if (obj.has("tgeof_id")){
                            tgeof_id = obj.getInt("tgeof_id");
                        }
                        if(old_geos_num > 0){
                            Map<String, String> data = new HashMap<>();
                            data.put("link", "survey_list");
                            data.put("title", ctx.getString(R.string.default_alarm_notification_title, old_geos_num));
                            data.put("message", ctx.getString(R.string.notif_default_message));
                            new NotificationLib(ctx).showNotificationSurvey(data);
                        }
                    } else {
                        saveLog();
                        GeneralLib.reportCrash(new Exception("sendTriggeredGeofencesTask no note in response"), result);
                    }

                } catch (JSONException e) {
                    GeneralLib.reportCrash(e, result);
                }
            }
        }
        else {
            saveLog();
        }

        // And if it is we call the callback function on it.
        if(taskListener != null)
            this.taskListener.onFinished(tgeof_id);
    }

    @Override
    protected String doInBackground(Object... params) {
        return postTriggeredGeofences();
    }


    private String postTriggeredGeofences() {
        String[] user = DB.getRowData("uporabnik",
                new String[]{"identifier", "id_server"}, null);

        JSONObject object = new JSONObject();

        if(user != null) {
            try {
                JSONObject logIn = new JSONObject();
                logIn.put("identifier", user[0]);
                logIn.put("id_server", user[1]);
                object.put("Login", logIn);

                JSONArray triggeredGeofences = new JSONArray();

                ArrayList<HashMap<String, String>> old_tgeos_to_send = DB.getListHashMapData(
                        "things_to_send", new String[]{"value", "tsSec", "param1", "param2", "param3"},
                        "name = 'triggered_geofences'", null);

                if(old_tgeos_to_send != null) {
                    old_geos_num = old_tgeos_to_send.size();
                    for (HashMap<String, String> geo : old_tgeos_to_send) {
                        JSONObject geofinfo = new JSONObject();
                        geofinfo.put("value", geo.get("value"));
                        geofinfo.put("tsSec", geo.get("tsSec"));
                        geofinfo.put("enter_timestamp", geo.get("param2"));
                        geofinfo.put("dwell_timestamp", geo.get("param3"));

                        JSONObject triggeredGeofence = new JSONObject();
                        triggeredGeofence.put("geofence", geofinfo);
                        //id of locations are stored in param1 column (for now, only one location per geofence)
                        triggeredGeofence.put("locations",
                                DB.getJSONData("locations", null, "id="+geo.get("param1")));
                        triggeredGeofences.put(triggeredGeofence);
                    }
                }

                //add new log if not null
                if(triggered_geofence != null){
                    triggered_geofence.put("return_server_id", true);
                    JSONObject triggeredGeofence = new JSONObject();
                    triggeredGeofence.put("geofence", triggered_geofence);
                    triggeredGeofence.put("locations",
                            DB.getJSONData("locations", null, "id="+triggered_geofence.get("loc_id")));
                    triggeredGeofences.put(triggeredGeofence);
                }

                if(triggeredGeofences.length() == 0)
                    return "no logs";
                else
                    object.put("triggeredGeofences", triggeredGeofences);
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, object.toString());
            }

            ServerCommunication SC = new ServerCommunication(ctx);
            return SC.PostTriggeredGeofences(object);
        }
        return null;
    }

    /**
     * Saves log in database for updating to server when connection available
     */
    private void saveLog() {
        //if triggered_geofence = null, means that it failed to send old data - they are already in DB, no need to save
        if (triggered_geofence != null) {
            try {
                DB.insertData("things_to_send",
                        new String[][]{{"name", "triggered_geofences"},
                                {"value", triggered_geofence.getString("value")},
                                {"tsSec", triggered_geofence.getString("tsSec")},
                                {"param1", triggered_geofence.getString("loc_id")},
                                {"param2", triggered_geofence.getString("enter_timestamp")},
                                {"param3", triggered_geofence.getString("dwell_timestamp")}});

            } catch (JSONException e) {
                GeneralLib.reportCrash(e, triggered_geofence.toString());
            }
        }
    }
}