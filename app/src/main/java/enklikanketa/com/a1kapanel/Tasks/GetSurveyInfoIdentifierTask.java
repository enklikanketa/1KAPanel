package enklikanketa.com.a1kapanel.Tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.SurveyConsent;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

public class GetSurveyInfoIdentifierTask extends AsyncTask<Object, Object, String> {

    String TAG = "GetSurveyInfoIdentifierTask";
    private final WeakReference<Activity> act;
    ProgressDialog loader;
    String new_identifier;

    public GetSurveyInfoIdentifierTask (Activity act, String identifier){
        this.act = new WeakReference<>(act);
        this.new_identifier = identifier;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loader = ProgressDialog.show(act.get(), "",
                act.get().getResources().getString(R.string.gathering_data_progress), true);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result != null && GeneralLib.isActivityValid(act.get())) {
            try {
                Object json = new JSONTokener(result).nextValue();
                if (json instanceof JSONArray && !result.equals("[]")){
                    JSONArray arr = new JSONArray(result);

                    boolean tracking = false;
                    boolean sensors = false;
                    //data for surveys which needs permissions to be sent to server
                    ArrayList<String> tracking_surveys = new ArrayList<>();
                    ArrayList<String> activities_surveys = new ArrayList<>();

                    for(int i=0; i < arr.length(); i++){
                        JSONObject obj = arr.getJSONObject(i);
                        if((!obj.isNull("activities") && obj.getInt("activities")>0)){
                            activities_surveys.add(obj.getString("id"));
                        }
                        if((!obj.isNull("tracking_on") && obj.getInt("tracking_on")>0)){
                            tracking_surveys.add(obj.getString("id"));
                        }
                        if((!obj.isNull("geofences") && obj.getInt("geofences")>0) ||
                                (!obj.isNull("activities") && obj.getInt("activities")>0) ||
                                ((!obj.isNull("entry_on") && obj.getInt("entry_on")>0) &&
                                        (!obj.isNull("location_check") && obj.getInt("location_check")>0)) ||
                                (!obj.isNull("tracking_on") && obj.getInt("tracking_on")>0)){
                            tracking = true;
                        }
                        if((!obj.isNull("activities") && obj.getInt("activities")>0) ||
                                ((!obj.isNull("tracking_on") && obj.getInt("tracking_on")>0) &&
                                        (!obj.isNull("activity_recognition") && obj.getInt("activity_recognition")>0))){
                            sensors = true;
                        }
                    }
                    Intent myIntent = new Intent(act.get(), SurveyConsent.class);
                    myIntent.putExtra("new_identifier", new_identifier);
                    myIntent.putExtra("tracking", tracking);
                    myIntent.putExtra("sensors", sensors);
                    //send data for surveys which needs permissions to be sent to server
                    myIntent.putStringArrayListExtra("tracking_surveys", tracking_surveys);
                    myIntent.putStringArrayListExtra("activities_surveys", activities_surveys);
                    act.get().startActivity(myIntent);
                    act.get().finish();
                }
                else {
                    JSONObject obj = new JSONObject(result);
                    if (obj.has("note")) {
                        if (obj.getString("note").equals("already participant")) {
                            GeneralLib.dismissDialog(act.get(), loader);
                            Toast.makeText(act.get(), act.get().getString(R.string.already_has_access)
                                    , Toast.LENGTH_LONG).show();
                        }
                        else
                            GeneralLib.reportCrash(new Exception("GetSurveyInfoIdentifierTask.class onPostExecute() has unknown note: "
                                    + result), null);
                    } else {
                        Log.e(TAG, "GetSurveyInfoIdentifierTask.class onPostExecute() result: " + result);
                        if (obj.has("error")) {

                            if(obj.getString("error").equals("identifier does not exist")) {
                                GeneralLib.dismissDialog(act.get(), loader);
                                Toast.makeText(act.get(), act.get().getString(R.string.identifierNotExist)
                                        , Toast.LENGTH_LONG).show();
                            }
                            //no user in DB yet
                            else if(obj.getString("error").equals("no data")){}
                        } else
                            GeneralLib.reportCrash(new Exception("GetSurveyInfoIdentifierTask.class onPostExecute() has unknown response: "
                                    + result), null);
                    }
                }
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, result);
            }
        }
        //if null, server is not responding or poor connection - no response from server
        else {
            if(GeneralLib.isActivityValid(act.get()))
                GeneralLib.showErrorToUser(act.get(), act.get().getString(R.string.general_remote_server_error));
        }
        GeneralLib.dismissDialog(act.get(), loader);
    }

    @Override
    protected String doInBackground(Object... params) {
        if (GeneralLib.isActivityValid(act.get())) {
            Database DB = (Database) Database.getInstance(act.get());
            String[] user = DB.getRowData("uporabnik",
                    new String[]{"identifier", "id_server"}, null);
            if(user != null)
                return postInfoByIdentifier(act.get(), user[0], user[1]);
            else
                return postInfoByIdentifier(act.get(), new_identifier, "");
        }
        else
            return null;
    }

    private String postInfoByIdentifier(Context ctx, String identifier, String id_server) {
        JSONObject object = new JSONObject();

        try {
            JSONObject logIn = new JSONObject();
            logIn.put("identifier", identifier);
            logIn.put("id_server", id_server);
            //just in case, if registration id is not updated (cleared data...)
            object.put("identifierToMerge", new_identifier);
            object.put("Login", logIn);

        } catch (JSONException e) {
            GeneralLib.reportCrash(e, object.toString());
        }

        ServerCommunication SC = new ServerCommunication(ctx);
        return SC.PostGetSurveysInfoByIdentifier(object);
    }
}


