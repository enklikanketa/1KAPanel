package enklikanketa.com.a1kapanel.Tasks;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 19/4/2018.
 */

public class sendGetTrackingTask extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Context ctx;
    int permission_granted;

    public sendGetTrackingTask(Context context, int permission_granted) {
        ctx = context.getApplicationContext();
        this.permission_granted = permission_granted;
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
                JSONArray surveys = new JSONArray(result);

                if (surveys.length() > 0) {

                    //list of all activities ids for this user
                    ArrayList<String> survey_ids = new ArrayList<>();
                    TrackingLib tlib = new TrackingLib(ctx);

                    for(int i = 0; i < surveys.length(); i++) {
                        JSONObject survey = surveys.getJSONObject(i);
                        HashMap<String, String> data = new HashMap<>();
                        data.put("link", survey.getString("link"));
                        data.put("ank_id", survey.getString("ank_id"));
                        data.put("srv_title", survey.getString("srv_title"));
                        String tracking = survey.getString("tracking");
                        data.put("tracking", tracking);
                        data.put("action", "tracking");
                        JSONObject trackingObj = survey.getJSONObject("tracking");
                        if(permission_granted == 1 ||
                                (trackingObj.has("permitted") && trackingObj.getString("permitted").equals("1")))
                            data.put("permission", "1");
                        else
                            data.put("permission", "0");

                        //store surveys ids in array to later compare which to delete (not active)
                        survey_ids.add(survey.getString("ank_id"));

                        tlib.setOrUpdateNewTracking(data);
                    }

                    //remove all data of logs from DB
                    Database DB = (Database) Database.getInstance(ctx);
                    DB.deleteRows("things_to_send", "name = 'get_tracking'");
                }
            } catch (JSONException e) {
                saveGetTrackingLog(ctx);
                GeneralLib.reportCrash(e, result);
            }
        }
        else {
            saveGetTrackingLog(ctx);
        }
    }

    @Override
    protected String doInBackground(Object... params) {
        return postTrackingRequest();
    }


    private String postTrackingRequest() {
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
            return SC.PostGetTracking(object);
        }
        return null;
    }

    /**
     * Saves log for refreshing alarms from server
     */
    public static void saveGetTrackingLog(Context context){
        Database DB = (Database) Database.getInstance(context);
        ContentValues initialValues = new ContentValues();
        initialValues.put("name", "get_tracking");
        initialValues.put("value", "");
        initialValues.put("tsSec", (System.currentTimeMillis()/1000)+"");

        int isLogIdInDB = DB.countData("things_to_send", "name='get_tracking'");
        //insert if not exist
        if(isLogIdInDB == 0)
            DB.getWritableDatabase().insert("things_to_send", null, initialValues);
    }
}