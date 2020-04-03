package enklikanketa.com.a1kapanel.Tasks;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.GeofencingLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 19/4/2018.
 */

public class sendGetGeofencesTask extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Context ctx;

    public sendGetGeofencesTask(Context context) {
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
                JSONArray surveys = new JSONArray(result);

                if (surveys.length() > 0) {
                    for(int i = 0; i < surveys.length(); i++) {
                        JSONObject survey = surveys.getJSONObject(i);
                        HashMap<String, String> data = new HashMap<>();
                        data.put("link", survey.getString("link"));
                        data.put("ank_id", survey.getString("ank_id"));
                        data.put("srv_title", survey.getString("srv_title"));
                        data.put("geofences", survey.getString("geofences"));

                        GeofencingLib glib = new GeofencingLib(ctx);
                        glib.setOrUpdateNewGeofences(data);
                    }

                    //remove all data of logs from DB
                    Database DB = (Database) Database.getInstance(ctx);
                    DB.deleteRows("things_to_send", "name = 'get_geofences'");
                }
            } catch (JSONException e) {
                saveGetGeofencesLog(ctx);
                GeneralLib.reportCrash(e, result);
            }
        }
        else {
            saveGetGeofencesLog(ctx);
        }
    }

    @Override
    protected String doInBackground(Object... params) {
        return postGeofencesRequest();
    }


    private String postGeofencesRequest() {
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
            return SC.PostGetGeofences(object);
        }
        return null;
    }

    /**
     * Saves log for refreshing geofences from server
     */
    public static void saveGetGeofencesLog(Context context){
        Database DB = (Database) Database.getInstance(context);
        ContentValues initialValues = new ContentValues();
        initialValues.put("name", "get_geofences");
        initialValues.put("value", "");
        initialValues.put("tsSec", (System.currentTimeMillis()/1000)+"");

        int isLogIdInDB = DB.countData("things_to_send", "name='get_geofences'");
        //insert if not exist
        if(isLogIdInDB == 0)
            DB.getWritableDatabase().insert("things_to_send", null, initialValues);
    }
}