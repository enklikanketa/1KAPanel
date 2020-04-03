package enklikanketa.com.a1kapanel.Tasks;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 07/12/2017.
 */

public class sendTrakingLocationsTask extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Context ctx;
    private Database DB;
    private String loc_id = null;
    private ArrayList<String> loc_ids = new ArrayList<>();
    private ArrayList<String> ar_ids = new ArrayList<>();
    private String TAG = "sendTrakingLocationsTask";
    private TrackingLib tlib;

    /**
     * @param context - Context
     */
    public sendTrakingLocationsTask(Context context) {
        ctx = context.getApplicationContext();
        tlib = new TrackingLib(ctx);

        DB = (Database) Database.getInstance(ctx);
    }

    /**
     * Force send particular location (even if it is already flaged as sended)
     * @param context - Context
     * @param loc_id1 - location id to send
     */
    public sendTrakingLocationsTask(Context context, String loc_id1) {
        ctx = context.getApplicationContext();
        tlib = new TrackingLib(ctx);
        loc_id = loc_id1;

        DB = (Database) Database.getInstance(ctx);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        //string of IDs of locations and AR processing/sending in this thread
        String whereLocIDs = tlib.convertArrayToStringForWhere(loc_ids);
        String whereARIDs = tlib.convertArrayToStringForWhere(ar_ids);

        if (result != null) {
            if(!result.equals("no locations")) {
                try {
                    JSONObject obj = new JSONObject(result);

                    if (obj.has("note")) {
                        ContentValues cvl = new ContentValues();
                        cvl.put("sending", "2");
                        //flag all rows on sending = 2 in locations table to mark id as saved on server
                        DB.updateData("locations", cvl, "sending=1 AND id in " + whereLocIDs);
                        //flag all rows on sending = 2 in AR table to mark id as saved on server
                        DB.updateData("activity_recognition", cvl, "sending=1 AND id in " + whereARIDs);
                        //delete all rows/locations that user has deleted
                        DB.deleteRows("locations", "sending=2 AND edited=2");
                    }
                } catch (JSONException e) {
                    GeneralLib.reportCrash(e, result);
                }
            }
        }
        else {
            //flag all rows back on sending = 0 in locations table
            ContentValues cvl = new ContentValues();
            cvl.put("sending", "0");
            DB.updateData("locations", cvl, "sending=1 AND id in " + whereLocIDs);
            //flag all rows back on sending = 0 in activity_recognition table
            DB.updateData("activity_recognition", cvl, "sending=1 id in " + whereARIDs);
        }
    }

    @Override
    protected String doInBackground(Object... params) {
        return postTrakingLoc();
    }


    private String postTrakingLoc() {
        String[] user = DB.getRowData("uporabnik",
                new String[]{"identifier", "id_server"}, null);

        JSONObject object = new JSONObject();

        if(user != null) {
            try {
                JSONObject logIn = new JSONObject();
                logIn.put("identifier", user[0]);
                logIn.put("id_server", user[1]);
                object.put("Login", logIn);

                if(loc_id == null) {
                    //get location data from DB
                    JSONArray locations_to_send = DB.getJSONData("locations", null, "sending=0 AND edited=0");
                    loc_ids = tlib.storeLocIDs(locations_to_send, loc_ids);

                    //get edited/deleted location data from DB
                    JSONArray edited_locations_to_send = DB.getJSONData("locations", null, "sending=0 AND edited=1");
                    loc_ids = tlib.storeLocIDs(edited_locations_to_send, loc_ids);

                    //get edited/deleted location data from DB
                    JSONArray deleted_locations_to_send = DB.getJSONData("locations", null, "sending=0 AND edited=2");
                    loc_ids = tlib.storeLocIDs(deleted_locations_to_send, loc_ids);

                    //get AR data from DB
                    JSONArray ar_to_send = DB.getJSONData("activity_recognition", null, "sending=0");
                    ar_ids = tlib.storeLocIDs(ar_to_send, ar_ids);

                    ContentValues cvl = new ContentValues();
                    cvl.put("sending", "1");
                    //flag all rows on sending = 1 in locations table to prevent double-sending in rare cases
                    DB.updateData("locations", cvl, "sending=0");
                    //flag all rows on sending = 1 in activity_recognition table to prevent double-sending in rare cases
                    DB.updateData("activity_recognition", cvl, "sending=0");

                    if ((locations_to_send == null || locations_to_send.length() == 0) &&
                            (ar_to_send == null || ar_to_send.length() == 0) &&
                            (edited_locations_to_send == null || edited_locations_to_send.length() == 0) &&
                            (deleted_locations_to_send == null || deleted_locations_to_send.length() == 0))
                        return "no locations";
                    else {
                        object.put("locations", locations_to_send != null ?
                                locations_to_send : new JSONArray());
                        object.put("activity_recognition", ar_to_send != null ?
                                ar_to_send : new JSONArray());
                        JSONObject edit_locations = new JSONObject();
                        edit_locations.put("edit", edited_locations_to_send != null ?
                                edited_locations_to_send : new JSONArray());
                        edit_locations.put("delete", deleted_locations_to_send != null ?
                                deleted_locations_to_send : new JSONArray());
                        object.put("edit_locations", edit_locations);
                    }
                }
                else{
                    JSONArray locations_to_send = DB.getJSONData("locations", null, "id="+loc_id);
                    loc_ids = tlib.storeLocIDs(locations_to_send, loc_ids);
                    object.put("locations", locations_to_send != null ?
                            locations_to_send : new JSONArray());
                }
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, object.toString());
            }

            ServerCommunication SC = new ServerCommunication(ctx);
            return SC.PostTrackingLocations(object);
        }
        return null;
    }
}