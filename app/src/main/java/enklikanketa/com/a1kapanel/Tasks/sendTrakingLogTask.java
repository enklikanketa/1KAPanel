package enklikanketa.com.a1kapanel.Tasks;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 07/12/2017.
 */

public class sendTrakingLogTask extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Context ctx;
    private JSONObject tracking_log;
    private Database DB;

    /**
     *
     * @param context - Context
     * @param tracking_log - string of logs of tracking with timestamp, separated with ";"
     */
    public sendTrakingLogTask(Context context, JSONObject tracking_log) {
        ctx = context.getApplicationContext();
        this.tracking_log = tracking_log;

        DB = (Database) Database.getInstance(ctx);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (result != null) {
            if(!result.equals("no logs")) {
                try {
                    JSONObject obj = new JSONObject(result);

                    if (obj.has("note")) {
                        //remove all data of logs from DB
                        DB.deleteRows("things_to_send", "name = 'tracking_log'");
                    } else {
                        saveLog();
                        GeneralLib.reportCrash(new Exception("sendTrakingLogTask no note in response"), result);
                    }

                } catch (JSONException e) {
                    GeneralLib.reportCrash(e, result);
                }
            }
        }
        else {
            saveLog();
        }
    }

    @Override
    protected String doInBackground(Object... params) {
        return postTrakingLog();
    }


    private String postTrakingLog() {
        String[] user = DB.getRowData("uporabnik",
                new String[]{"identifier", "id_server"}, null);

        JSONObject object = new JSONObject();

        if(user != null) {
            try {
                JSONObject logIn = new JSONObject();
                logIn.put("identifier", user[0]);
                logIn.put("id_server", user[1]);
                object.put("Login", logIn);

                JSONArray trackingLog = new JSONArray();

                JSONArray old_logs_to_send = DB.getJSONData("things_to_send",
                        new String[]{"value", "event", "tsSec"}, "name = 'tracking_log'");

                if(old_logs_to_send != null)
                    trackingLog = old_logs_to_send;

                //add new log if not null
                if(tracking_log != null)
                    trackingLog.put(tracking_log);

                if(trackingLog.length() == 0)
                    return "no logs";
                else
                    object.put("trackingLog", trackingLog);
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, object.toString());
            }

            ServerCommunication SC = new ServerCommunication(ctx);
            return SC.PostTrackingLog(object);
        }
        return null;
    }

    /**
     * Saves log in database for updating to server when connection available
     */
    private void saveLog() {

        //if tracking_log = null, means that it failed to send old data - they are already in DB, no need to save
        if (tracking_log != null) {
            try {
                DB.insertData("things_to_send",
                        new String[][]{{"name", "tracking_log"},
                                {"value", tracking_log.getString("value")},
                                {"event", tracking_log.getString("event")},
                                {"tsSec", tracking_log.getString("tsSec")}});

            } catch (JSONException e) {
                GeneralLib.reportCrash(e, tracking_log.toString());
            }
        }
    }
}