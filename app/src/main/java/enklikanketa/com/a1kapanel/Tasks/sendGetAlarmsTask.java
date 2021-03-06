package enklikanketa.com.a1kapanel.Tasks;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import enklikanketa.com.a1kapanel.Libraries.AlarmLib;
import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 07/12/2017.
 */

public class sendGetAlarmsTask extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Context ctx;

    public sendGetAlarmsTask(Context context) {
        ctx = context.getApplicationContext();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);

                if (obj.has("alarms")) {
                    JSONArray alarms = obj.getJSONArray("alarms");

                    for(int i = 0; i < alarms.length(); i++) {
                        JSONObject alarm = alarms.getJSONObject(i);

                        HashMap<String, String> data = new HashMap<>();
                        data.put("link", alarm.getString("link"));
                        data.put("ank_id", alarm.getString("ank_id"));
                        data.put("title", alarm.getString("title"));
                        data.put("srv_title", alarm.getString("srv_title"));
                        data.put("message", alarm.getString("message"));
                        data.put("sound", alarm.getString("sound"));
                        data.put("repeat", alarm.getString("repeat"));
                        data.put("repeater", alarm.getString("repeater"));

                        AlarmLib alib = new AlarmLib(ctx);
                        alib.setOrUpdateNewAlarm(data);
                    }
                    //remove all data of logs from DB
                    Database DB = (Database) Database.getInstance(ctx);
                    DB.deleteRows("things_to_send", "name = 'get_alarms'");
                } else {
                    saveGetAlarmsLog(ctx);
                    GeneralLib.reportCrash(new Exception("sendGetAlarmsTask no alarms in response"), result);
                }

            } catch (JSONException e) {
                saveGetAlarmsLog(ctx);
                GeneralLib.reportCrash(e, result);
            }
        }
        else {
            saveGetAlarmsLog(ctx);
        }
    }

    @Override
    protected String doInBackground(Object... params) {
        return postAlarmRequest();
    }


    private String postAlarmRequest() {
        Database DB = (Database) Database.getInstance(ctx);
        String[] user = DB.getRowData("uporabnik",
                new String[]{"identifier", "id_server"}, null);

        JSONObject object = new JSONObject();

        if(user != null) {
            try {
                JSONObject logIn = new JSONObject();
                logIn.put("identifier", user[0]);
                logIn.put("id_server", user[1]);
                object.put("Login", logIn);

            } catch (JSONException e) {
                GeneralLib.reportCrash(e, object.toString());
            }

            ServerCommunication SC = new ServerCommunication(ctx);
            return SC.PostGetAlarms(object);
        }
        return null;
    }

    /**
     * Saves log for refreshing alarms from server
     */
    public static void saveGetAlarmsLog(Context context){
        Database DB = (Database) Database.getInstance(context);
        ContentValues initialValues = new ContentValues();
        initialValues.put("name", "get_alarms");
        initialValues.put("value", "");
        initialValues.put("tsSec", (System.currentTimeMillis()/1000)+"");

        int isLogIdInDB = DB.countData("things_to_send", "name='get_alarms'");
        //insert if not exist
        if(isLogIdInDB == 0)
            DB.getWritableDatabase().insert("things_to_send", null, initialValues);
    }
}