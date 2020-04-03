package enklikanketa.com.a1kapanel.Tasks;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 07/12/2017.
 */

public class sendRegistrationIDTask extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Context ctx;
    private String registration_id;
    private Database DB;

    /**
     *
     * @param context - Context
     * @param registration_id - registration id token for firebase messaging
     */
    public sendRegistrationIDTask(Context context, String registration_id) {
        ctx = context.getApplicationContext();
        this.registration_id = registration_id;
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
            try {
                JSONObject obj = new JSONObject(result);

                if (obj.has("note")) {
                    //all ok
                    //delete regid from DB, if is there
                    deleteRegID();
                } else {
                    saveRegID();
                    GeneralLib.reportCrash(new Exception("sendRegistrationIDTask no note in response"), result);
                }

            } catch (JSONException e) {
                saveRegID();
                GeneralLib.reportCrash(e, result);
            }
        }
        else {
            saveRegID();
        }
    }

    @Override
    protected String doInBackground(Object... params) {
        return postRegistrationID();
    }


    private String postRegistrationID() {
        String[] user = DB.getRowData("uporabnik",
                new String[]{"identifier", "id_server"}, null);

        JSONObject object = new JSONObject();

        if(user != null && user[1] != null && !user[1].equals("")) {
            try {
                JSONObject logIn = new JSONObject();
                logIn.put("identifier", user[0]);
                logIn.put("id_server", user[1]);
                object.put("Login", logIn);

                object.put("registration_id", registration_id);

            } catch (JSONException e) {
                GeneralLib.reportCrash(e, object.toString());
            }

            ServerCommunication SC = new ServerCommunication(ctx);
            return SC.PostRegistrationID(object);
        }
        return null;
    }

    /**
     * Saves regid in database for updating to server when connection available
     */
    private void saveRegID(){
        ContentValues initialValues = new ContentValues();
        initialValues.put("name", "registration_id");
        initialValues.put("value", registration_id);
        initialValues.put("tsSec", (System.currentTimeMillis()/1000)+"");

        int isRegIdInDB = DB.countData("things_to_send", "name='registration_id'");
        //insert if not exist, update if exist
        if(isRegIdInDB == 0)
            DB.getWritableDatabase().insert("things_to_send", null, initialValues);
        else
            DB.getWritableDatabase().update("things_to_send", initialValues, "name=?", new String[] {"registration_id"});
    }

    /**
     * Deletes regid from DB, call it when sending to server is success
     */
    private void deleteRegID(){
        DB.deleteRows("things_to_send", "name='registration_id'");
    }
}