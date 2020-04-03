package enklikanketa.com.a1kapanel.Tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 19/4/2018.
 */

public class sendGetMyLocationsTask extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Context ctx;

    public sendGetMyLocationsTask(Context context) {
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
                JSONObject mainarr = new JSONObject(result);
                JSONArray locations = mainarr.getJSONArray("locations");
                JSONArray ar = mainarr.getJSONArray("ar");

                if (locations.length() > 0 || ar.length() > 0) {
                    Database DBH = (Database) Database.getInstance(ctx);
                    SQLiteDatabase DB = DBH.getWritableDatabase();
                    DB.beginTransaction();

                    //insert locations
                    if(locations.length() > 0) {
                        for (int i = 0; i < locations.length(); i++) {
                            JSONObject location = locations.getJSONObject(i);
                            ContentValues cvl = new ContentValues();
                            cvl.put("lat", location.getString("lat"));
                            cvl.put("lng", location.getString("lng"));
                            if(!location.isNull("provider"))
                                cvl.put("provider", location.getString("provider"));
                            else cvl.putNull("provider");
                            cvl.put("timestamp", location.getString("timestamp"));
                            if(!location.isNull("accuracy"))
                                cvl.put("accuracy", location.getString("accuracy"));
                            else cvl.putNull("accuracy");
                            if(!location.isNull("altitude"))
                                cvl.put("altitude", location.getString("altitude"));
                            else cvl.putNull("altitude");
                            if(!location.isNull("bearing"))
                                cvl.put("bearing", location.getString("bearing"));
                            else cvl.putNull("bearing");
                            if(!location.isNull("speed"))
                                cvl.put("speed", location.getString("speed"));
                            else cvl.putNull("speed");
                            if(!location.isNull("vertical_acc"))
                                cvl.put("vertical_acc", location.getString("vertical_acc"));
                            else cvl.putNull("vertical_acc");
                            if(!location.isNull("bearing_acc"))
                                cvl.put("bearing_acc", location.getString("bearing_acc"));
                            else cvl.putNull("bearing_acc");
                            if(!location.isNull("speed_acc"))
                                cvl.put("speed_acc", location.getString("speed_acc"));
                            else cvl.putNull("speed_acc");
                            if(!location.isNull("extras"))
                                cvl.put("extras", location.getString("extras"));
                            else cvl.putNull("extras");
                            if(!location.isNull("is_mock"))
                                cvl.put("is_mock", location.getString("is_mock"));
                            else cvl.putNull("is_mock");
                            cvl.put("sending", 2);
                            DBH.insertData("locations", cvl);
                        }
                    }
                    //insert recognized activities
                    if(ar.length() > 0) {
                        for (int i = 0; i < ar.length(); i++) {
                            JSONObject ra = ar.getJSONObject(i);
                            ContentValues cvl = new ContentValues();
                            cvl.put("timestamp", ra.getString("timestamp"));
                            cvl.put("in_vehicle", ra.getString("in_vehicle"));
                            cvl.put("on_bicycle", ra.getString("on_bicycle"));
                            cvl.put("on_foot", ra.getString("on_foot"));
                            cvl.put("still", ra.getString("still"));
                            cvl.put("unknown", ra.getString("unknown"));
                            cvl.put("tilting", ra.getString("tilting"));
                            cvl.put("running", ra.getString("running"));
                            cvl.put("walking", ra.getString("walking"));
                            cvl.put("sending", 2);
                            DBH.insertData("activity_recognition", cvl);
                        }
                    }

                    //excecute transaction
                    DB.setTransactionSuccessful();
                    DB.endTransaction();

                    //remove all data of logs from DB
                    DBH.deleteRows("things_to_send", "name = 'get_my_locations'");
                } else {
                    saveGetTrackingLog(ctx);
                    GeneralLib.reportCrash(new Exception("sendGetMyLocationsTask no tracking in response"), result);
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
            return SC.PostGetMyLocations(object);
        }
        return null;
    }

    /**
     * Saves log for refreshing alarms from server
     */
    public static void saveGetTrackingLog(Context context){
        Database DB = (Database) Database.getInstance(context);
        ContentValues initialValues = new ContentValues();
        initialValues.put("name", "get_my_locations");
        initialValues.put("value", "");
        initialValues.put("tsSec", (System.currentTimeMillis()/1000)+"");

        int isLogIdInDB = DB.countData("things_to_send", "name='get_my_locations'");
        //insert if not exist
        if(isLogIdInDB == 0)
            DB.getWritableDatabase().insert("things_to_send", null, initialValues);
    }
}