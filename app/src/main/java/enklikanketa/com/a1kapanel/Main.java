package enklikanketa.com.a1kapanel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import enklikanketa.com.a1kapanel.Libraries.DoOnConnection;
import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.Network;
import enklikanketa.com.a1kapanel.System.ServerCommunication;
import enklikanketa.com.a1kapanel.Tasks.sendGetAlarmsTask;
import enklikanketa.com.a1kapanel.Tasks.sendGetEntriesTask;
import enklikanketa.com.a1kapanel.Tasks.sendGetGeofencesTask;
import enklikanketa.com.a1kapanel.Tasks.sendGetMyLocationsTask;
import enklikanketa.com.a1kapanel.Tasks.sendGetTrackingTask;
import enklikanketa.com.a1kapanel.Tasks.sendSetTrackingPermission;

public class Main extends AppCompatActivity {

    private String TAG = "Main";

    Button tryAgain;
    ProgressBar progressBar;
    Database DB;
    //do login at app start - false if you dont have api key
    boolean doLogin = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);

        DB = (Database) Database.getInstance(this);

        tryAgain = findViewById(R.id.button_poskusi_znova);
        tryAgain.setOnClickListener(poskusiZnovaClick);
        progressBar = findViewById(R.id.main_progressBar);
    }

    //gumb poskusi znova
    View.OnClickListener poskusiZnovaClick = new View.OnClickListener() {
        public void onClick(View v) {
            tryAgain.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            //check if internet connection and login
            checkInternet();
        }
    };

    /**
     * Check if connected to web and send login data to server
     */
    private void checkInternet() {
        //check internet connection
        if (Network.checkMobileInternet(Main.this, true)) {
            if(doLogin)
                //login
                new LoginTask(Main.this).execute();
            else{
                Intent myIntent = new Intent(Main.this, Home.class);
                startActivity(myIntent);
                finish();
            }
        }
        //not connected
        else {
            tryAgain.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    //DEVICE INFO FUNCTIONS
    /**
     * Check if there is new device info (in case OS is updated)
     */
    private boolean newDeviceInfo() {
        //SQLiteDatabase DB = new Database(this).getWritableDatabase();
        //get release from DB
        String[][] release = DB.getPairData("device_info", new String[]{"value"}, "name='release'");
        //if release from DB is not equal to actual, update device info in DB
        if (release != null && !android.os.Build.VERSION.RELEASE.equals(release[0][0])) {
            DB.updateData("device_info", DB.convertToValues(
                    new String[][]{{"value", System.getProperty("os.version") + ""}}), "name='os_version'");
            DB.updateData("device_info", DB.convertToValues(
                    new String[][]{{"value", android.os.Build.VERSION.INCREMENTAL + ""}}), "name='incremental'");
            DB.updateData("device_info", DB.convertToValues(
                    new String[][]{{"value", android.os.Build.VERSION.SDK_INT + ""}}), "name='sdk_int'");
            DB.updateData("device_info", DB.convertToValues(
                    new String[][]{{"value", android.os.Build.VERSION.RELEASE + ""}}), "name='release'");

            return true;
        }
        return false;
    }

    /**
     * Get string of device data
     *
     * @return string for device data ready to put int in textview
     */
    private static String getDeviceInfoString(Context ctx) {
        JSONObject deviceInfo = new JSONObject();

        Database DB = (Database) Database.getInstance(ctx);
        JSONObject data = DB.getJSONDataFromNameValueTable("device_info", new String[]{"name", "value"}, null);

        try{
            if (data != null) {
                deviceInfo.put("device_info", data);
                deviceInfo.put("sensors", getSensorsInfo(ctx));
            }
        } catch (JSONException e){
            GeneralLib.reportCrash(e, "Parsing JSON in getDeviceInfoString()");
        }

        return deviceInfo.toString();
    }

    /**
     * Get info of al sensors in device
     * @return String of all sensors in device
     */
    private static JSONArray getSensorsInfo(Context ctx){
        SensorManager sensorManager;
        sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        JSONArray sensors = new JSONArray();
        if(sensorManager != null) {
            try {
                List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
                for (int i = 0; i < deviceSensors.size(); i++) {
                    JSONObject sensoro = new JSONObject();
                    Sensor sens = deviceSensors.get(i);
                    sensoro.put("vendor", sens.getVendor());
                    sensoro.put("name", sens.getName());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                        sensoro.put("type", sens.getStringType());
                    }
                    sensors.put(sensoro);
                }
            } catch (JSONException e){
                GeneralLib.reportCrash(e, "Parsing JSON in getSensorsInfo()");
            }
        }
        return sensors;
    }

    private static class DeviceInfoTask extends AsyncTask<Object, Object, String> {

        //application context cannot be leaked
        private Context ctx;

        /**
         *
         * @param ctx - has to be application context - getApplicationContext()
         */
        DeviceInfoTask (Context ctx){
            this.ctx = ctx;
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
                    } else {
                        GeneralLib.reportCrash(new Exception("DeviceInfoTask response not having note"), result);
                    }

                } catch (JSONException e) {
                    GeneralLib.reportCrash(e, result);
                }
            }
            else {
                GeneralLib.showErrorToUser(ctx, ctx.getString(R.string.general_remote_server_error));
            }
        }

        @Override
        protected String doInBackground(Object... params) {
            return postDeviceInfo(ctx);
        }
    }

    private static String postDeviceInfo(Context ctx) {
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

                object.put("deviceInfo", getDeviceInfoString(ctx));

            } catch (JSONException e) {
                GeneralLib.reportCrash(e, object.toString());
            }

            ServerCommunication SC = new ServerCommunication(ctx);
            return SC.PostDeviceInfo(object);
        }
        return null;
    }
    //END OF FUNCTIONS FOR DEVICE IFNO

    private class LoginTask extends AsyncTask<Object, Object, String> {

        private final WeakReference<Activity> act;

        LoginTask (Activity act){
            this.act = new WeakReference<>(act);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null && GeneralLib.isActivityValid(act.get())) {
                    try {
                        JSONObject obj = new JSONObject(result);

                        if (obj.has("note")) {
                            if (obj.getString("note").equals("login OK")) {
                                
                                if(obj.has("identifier") && obj.has("id_server"))
                                    firstLoginWithThisIdentifier(obj.getString("identifier"), obj.getString("id_server"));
                                
                                new DeviceInfoTask(act.get().getApplicationContext()).execute();
                                new DoOnConnection(act.get());

                                //run tracking if there are subscribed surveys using it
                                TrackingLib tlib = new TrackingLib(act.get());
                                if(tlib.areTrackingPermissionGrantedAndRunning()) {
                                    if(!tlib.serviceIsRunningInForeground())
                                        tlib.startTracking("app_opened");
                                }

                                Intent myIntent = new Intent(act.get(), Home.class);
                                startActivity(myIntent);
                                finish();
                            }
                            else
                                GeneralLib.reportCrash(new Exception("Main.class onPostExecute() has unknown note: "
                                        + result), null);
                        } else {
                            Log.e(TAG, "Main.class onPostExecute() result: " + result);
                            if (obj.has("error")) {
                                if(obj.getString("error").equals("login error") ||
                                        obj.getString("error").equals("login error")) {
                                    Toast.makeText(act.get(), Main.this.getString(R.string.identifierNotExist)
                                            , Toast.LENGTH_LONG).show();
                                    //clear all tables records from database (just for developing, will not come to this in production (when data is already in DB))
                                    DB.clearAllFromDatabase();
                                }
                            } else
                                GeneralLib.reportCrash(new Exception("Main.class onPostExecute() has unknown response: "
                                        + result), null);

                            Intent myIntent = new Intent(act.get(), vpis.class);
                            startActivity(myIntent);
                            finish();
                        }
                    } catch (JSONException e) {
                        GeneralLib.reportCrash(e, result);
                    }
            }
            else {
                if(GeneralLib.isActivityValid(act.get()))
                    GeneralLib.showErrorToUser(act.get(), act.get().getString(R.string.general_remote_server_error));
                tryAgain.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected String doInBackground(Object... params) {
            if (GeneralLib.isActivityValid(act.get())) {
                String[] user = DB.getRowData("uporabnik",
                        new String[]{"identifier", "id_server"}, null);
                if(user != null)
                    return postLoginMain(act.get(), user[0], user[1]);
                else
                    return "{error: \"no data\"}";
            }
            else
                return null;
        }
    }

    private String postLoginMain(Context ctx, String identifier, String id_server) {
        JSONObject object = new JSONObject();

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        
        try {
            JSONObject logIn = new JSONObject();
            logIn.put("identifier", identifier);
            logIn.put("id_server", id_server);
            //just in case, if registration id is not updated (cleared data...)
            logIn.put("registration_id", refreshedToken);
            object.put("Login", logIn);

        } catch (JSONException e) {
            GeneralLib.reportCrash(e, object.toString());
        }

        ServerCommunication SC = new ServerCommunication(ctx);
        return SC.PostLogin(object);
    }

    /**
     * Save user in user table, delete old data
     *
     * @param identifier - identifier of user
     * @param id_server - id of user on server DB
     */
    private void saveUser(String identifier, String id_server) {
        //if table  of user is not empty, empty it
        if (DB.countData("uporabnik", null) != 0) {
            DB.deleteAllRows("uporabnik");
        }

        //insert new user
        DB.insertData("uporabnik", new String[][]{
                {"identifier", identifier},
                {"id_server", id_server}});
    }

    /**
     * Procedures to do when user login with new identifier on this device
     *
     * @param identifier - identifier of user
     * @param id_server - id of user on server DB
     */
    private void firstLoginWithThisIdentifier(String identifier, String id_server){
        saveUser(identifier, id_server);
        //get data entry surveys of this user
        new sendGetEntriesTask(this).execute();
        //get tracking of surveys of this user
        new sendGetTrackingTask(this, 1).execute();
        //get geofences of surveys of this user
        new sendGetGeofencesTask(this).execute();
        //get alarms for surveys of this user
        new sendGetAlarmsTask(this).execute();
        //get all locations in case user was already active
        new sendGetMyLocationsTask(this).execute();

        //send accepted permissions to server (user had to accept all permissions before actual login)
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            ArrayList<String> tracking_surveys = bundle.getStringArrayList("tracking_surveys_permitted");
            ArrayList<String> activities_surveys = bundle.getStringArrayList("activities_surveys_permitted");

            if (tracking_surveys != null)
                for (String srv_id : tracking_surveys) {
                    new sendSetTrackingPermission(Main.this, srv_id, "1").execute();
                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkInternet();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
