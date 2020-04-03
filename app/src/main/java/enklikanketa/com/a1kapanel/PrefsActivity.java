package enklikanketa.com.a1kapanel;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.System.Network;
import enklikanketa.com.a1kapanel.Tasks.sendTrakingLogTask;


public class PrefsActivity extends AppCompatActivity {

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
       // addPreferencesFromResource(R.xml.preferences);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PreferencesFragment()).commit();
    }

    public static void postTrackingLog(Context con, String value, String event) {
        JSONObject trackingObj = new JSONObject();
        try {
            trackingObj.put("value", value);
            trackingObj.put("event", event);
            trackingObj.put("tsSec", System.currentTimeMillis()/1000);
        } catch (JSONException e) {
            GeneralLib.reportCrash(e, trackingObj.toString());
        }

        new sendTrakingLogTask(con, trackingObj).execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (Network.checkMobileInternet(PrefsActivity.this, true))
                    finish();
                break;
        }
        return true;
    }
}
