package enklikanketa.com.a1kapanel;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import enklikanketa.com.a1kapanel.Libraries.AlarmLib;
import enklikanketa.com.a1kapanel.Libraries.GeofencingLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.Tasks.sendTrakingLocationsTask;


/**
 * Created by matejs on 1.12.2015.
 */
public class PreferencesFragment extends PreferenceFragment
{
    public static final String PREF_TOKEN = "PREF_TOKEN";
    public static final String PREF_DB_NUMBER = "PREF_DB_NUMBER";
    public static final String PREF_TRACKING_NUMBER = "PREF_TRACKING_NUMBER";
    public static final String PREF_START_STOP_ACTIVITY = "PREF_START_STOP_ACTIVITY";
    public static final String PREF_START_STOP_TRACKING = "PREF_START_STOP_TRACKING";
    public static final String PREF_REFRESH_ALARMS = "PREF_REFRESH_ALARMS";
    public static final String PREF_REFRESH_GEOFENCES = "PREF_REFRESH_GEOFENCES";
    public static final String PREF_REFRESH_ACTIVITIES = "PREF_REFRESH_ACTIVITIES";
    public static final String PREF_REFRESH_TRACKING = "PREF_REFRESH_TRACKING";

    // /DEV option
    public static final String PREF_DEV_SERVER = "PREF_DEV_SERVER";

    private static final String TAG = "PreferencesFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        final Activity act = getActivity();

        final Preference TrackingButton = findPreference(PREF_START_STOP_TRACKING);

        final TrackingLib tlib = new TrackingLib(act);
        if(!tlib.isCanTrack()){
            TrackingButton.setTitle(getString(R.string.Start_Tracking));
            TrackingButton.setSummary(getString(R.string.start_desc) + " ("+tlib.howManyTrackingPermissionGranted()+")");
        }

        TrackingButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                if (TrackingButton.getTitle().equals(getString(R.string.Stop_Tracking))){
                    tlib.setCanTrack(false);
                    tlib.stopTracking("user_preferences");

                    TrackingButton.setTitle(getString(R.string.Start_Tracking));
                    TrackingButton.setSummary(getString(R.string.start_desc) + " ("+tlib.howManyTrackingPermissionGranted()+")");
                    Log.d(TAG, "Stopping tracking");
                }
                else{
                    tlib.setCanTrack(true);
                    tlib.startTracking("user_preferences");

                    TrackingButton.setTitle(getString(R.string.Stop_Tracking));
                    TrackingButton.setSummary(getString(R.string.stop_desc) + " ("+tlib.howManyTrackingPermissionGranted()+")");
                    Log.d(TAG, "Starting tracking");
                }
                return true;
            }
        });

        final Preference refreshAlarms = findPreference(PREF_REFRESH_ALARMS);
        refreshAlarms.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlarmLib al = new AlarmLib(act);
                al.refreshAlarms();
                return true;
            }
        });

        final Preference refreshGeofences = findPreference(PREF_REFRESH_GEOFENCES);
        refreshGeofences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                GeofencingLib glib = new GeofencingLib(act);
                glib.refreshGeofences();
                return true;
            }
        });

        final Preference refreshTracking = findPreference(PREF_REFRESH_TRACKING);
        refreshTracking.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                tlib.refreshTracking();
                return true;
            }
        });

        final int lnum = tlib.getNumberOfUnsendedLocations();
        final int anum = tlib.getNumberOfUnsendedAR();
        final Preference sendTracking = findPreference(PREF_TRACKING_NUMBER);
        sendTracking.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(lnum > 0 || anum > 0) {
                    new sendTrakingLocationsTask(act).execute();
                    sendTracking.setSummary(getString(R.string.pref_tracking_number_sum, 0, 0));
                }
                return true;
            }
        });
        sendTracking.setSummary(getString(R.string.pref_tracking_number_sum, lnum, anum));
    }
}
