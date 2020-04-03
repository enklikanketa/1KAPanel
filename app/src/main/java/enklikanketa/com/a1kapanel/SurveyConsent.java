/*
 * Made by:
 * Uroš Podkrižnik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;
import enklikanketa.com.a1kapanel.Tasks.sendGetAlarmsTask;
import enklikanketa.com.a1kapanel.Tasks.sendGetGeofencesTask;
import enklikanketa.com.a1kapanel.Tasks.sendGetTrackingTask;
import enklikanketa.com.a1kapanel.Tasks.sendSetTrackingPermission;


public class SurveyConsent extends AppCompatActivity {

    private String TAG = "SurveyConsent";
    Database DB;
    boolean first_login = false;
    String new_identifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.survey_consent);

        boolean tracking = false, sensors = false;

        DB = (Database) Database.getInstance(this);

        final Bundle bundle = getIntent().getExtras();
        if(bundle != null && bundle.getString("new_identifier") != null &&
                !bundle.getString("new_identifier").equals("")) {
            new_identifier = bundle.getString("new_identifier");

            String[] user = DB.getRowData("uporabnik",
                    new String[]{"identifier"}, null);

            //if user in database is empty, it is first login, otherwise it is merge
            if(user == null)
                first_login = true;

            tracking = bundle.getBoolean("tracking");
            sensors = bundle.getBoolean("sensors");
        }
        else{
            GeneralLib.reportCrash(new Exception("SurveyConsent.class onCreate() problem in bundle or new_identifier key"), null);
            finish();
        }

        if(tracking)
            findViewById(R.id.consent_permission_locat).setVisibility(View.VISIBLE);
        if(sensors)
            findViewById(R.id.consent_permission_sens).setVisibility(View.VISIBLE);

        Button buttonConsent = findViewById(R.id.consent_accept);
        buttonConsent.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //first login
                if(first_login) {
                    saveUser(new_identifier);
                    Intent myIntent = new Intent(SurveyConsent.this, Main.class);
                    //send data for surveys which needs permissions to be sent to server
                    if(bundle != null) {
                        myIntent.putStringArrayListExtra("tracking_surveys_permitted",
                                bundle.getStringArrayList("tracking_surveys"));
                        myIntent.putStringArrayListExtra("activities_surveys_permitted",
                                bundle.getStringArrayList("activities_surveys"));
                    }
                    startActivity(myIntent);
                    finish();
                }
                //merge
                else {
                    new MergeTask(SurveyConsent.this).execute();
                    if (bundle != null) {
                        ArrayList<String> tracking_surveys = bundle.getStringArrayList("tracking_surveys_permitted");

                        if (tracking_surveys != null)
                            for (String srv_id : tracking_surveys) {
                                new sendSetTrackingPermission(SurveyConsent.this, srv_id, "1").execute();
                            }
                    }
                }

            }
        });
        Button buttondecline = findViewById(R.id.consent_decline);
        buttondecline.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //first login
                if(first_login) {
                    Intent myIntent = new Intent(SurveyConsent.this, vpis.class);
                    startActivity(myIntent);
                }
                finish();
            }
        });
    }

    private class MergeTask extends AsyncTask<Object, Object, String> {

        private final WeakReference<Activity> act;
        ProgressDialog loader;

        MergeTask (Activity act){
            this.act = new WeakReference<>(act);
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
                    JSONObject obj = new JSONObject(result);

                    if (obj.has("note")) {
                        if (obj.getString("note").equals("merge OK")) {
                            //check and save new repeaters if exists
                            new sendGetAlarmsTask(act.get()).execute();
                            //get geofences of surveys of this user
                            new sendGetGeofencesTask(act.get()).execute();
                            //get tracking of surveys of this user
                            new sendGetTrackingTask(act.get(), 1).execute();

                            GeneralLib.dismissDialog(act.get(), loader);
                            Toast.makeText(act.get(), act.get().getString(R.string.identifiers_merged)
                                , Toast.LENGTH_LONG).show();
                            finish();
                        }
                        else
                            GeneralLib.reportCrash(new Exception("SurveyConsent.class onPostExecute() has unknown note: "
                                    + result), null);
                    } else {
                        Log.e(TAG, "SurveyConsent.class onPostExecute() result: " + result);
                        if (obj.has("error")) {

                            if(obj.getString("error").equals("identifier does not exist")) {
                                GeneralLib.dismissDialog(act.get(), loader);
                                Toast.makeText(act.get(), act.get().getString(R.string.identifierNotExist)
                                        , Toast.LENGTH_LONG).show();
                            }
                            //no user in DB yet
                            else if(obj.getString("error").equals("no data")){}
                        } else
                            GeneralLib.reportCrash(new Exception("SurveyConsent.class onPostExecute() has unknown response: "
                                    + result), null);
                    }
                } catch (JSONException e) {
                    GeneralLib.reportCrash(e, result);
                }
            }
            else {
                if(GeneralLib.isActivityValid(act.get()))
                    GeneralLib.showErrorToUser(act.get(), act.get().getString(R.string.general_remote_server_error));
            }
            GeneralLib.dismissDialog(act.get(), loader);
        }

        @Override
        protected String doInBackground(Object... params) {
            if (GeneralLib.isActivityValid(act.get())) {
                String[] user = DB.getRowData("uporabnik",
                        new String[]{"identifier", "id_server"}, null);
                if(user != null && new_identifier != null)
                    return postMergeIdentifier(act.get(), user[0], user[1]);
                else
                    return "{error: \"no data\"}";
            }
            else
                return null;
        }
    }

    private String postMergeIdentifier(Context ctx, String identifier, String id_server) {
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
        return SC.PostMergeIdentifier(object);
    }

    /**
     * Save user in user table, delete old data and go to Main.class
     *
     * @param identifier - identifier of user
     */
    private void saveUser(String identifier) {
        if (DB.countData("uporabnik", null) != 0) {
            DB.deleteAllRows("uporabnik");
        }

        DB.insertData("uporabnik", new String[][]{
                {"identifier", identifier},
                {"id_server", ""}});
    }
}
