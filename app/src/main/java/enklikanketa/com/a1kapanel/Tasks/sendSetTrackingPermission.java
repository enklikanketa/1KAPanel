package enklikanketa.com.a1kapanel.Tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 19/4/2018.
 */

public class sendSetTrackingPermission extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Activity ctx;
    private ProgressDialog loader;
    private String srv_id, permission;
    Database DBH;

    public sendSetTrackingPermission(Activity act, String srv_id1, String permission1) {
        ctx = act;
        srv_id = srv_id1;
        permission = permission1;
        DBH = (Database) Database.getInstance(act);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loader = ProgressDialog.show(ctx, "",
                ctx.getResources().getString(R.string.gathering_data_progress), true);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        GeneralLib.dismissDialog(ctx, loader);

        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);

                if (obj.has("note")) {
                    TrackingLib tlib = new TrackingLib(ctx);
                    //run and save permitted tracking
                    if(permission.equals("1")) {
                        tlib.startTracking("new_tracking_subscription");
                        tlib.trackingPermissionGrantedDB(srv_id);
                    }
                } else {
                    GeneralLib.reportCrash(new Exception("sendSetTrackingPermission no note in response"), result);
                }

            } catch (JSONException e) {
                saveGetTrackingLog(ctx);
                GeneralLib.reportCrash(e, result);
            }
        }
        else {
            Toast.makeText(ctx, ctx.getText(R.string.general_remote_server_error)
                    .toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected String doInBackground(Object... params) {
        return postActivitiesRequest();
    }


    private String postActivitiesRequest() {
        String[] user = DBH.getRowData("uporabnik",
                new String[]{"identifier", "id_server"}, null);

        JSONObject object = new JSONObject();

        if(user != null) {
            try {
                JSONObject logIn = new JSONObject();
                logIn.put("identifier", user[0]);
                logIn.put("id_server", user[1]);
                object.put("Login", logIn);

                object.put("srv_id", srv_id);
                object.put("tracking_permission", permission);
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, object.toString());
            }

            ServerCommunication SC = new ServerCommunication(ctx);
            return SC.PostSetTrackingPermission(object);
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