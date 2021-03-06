package enklikanketa.com.a1kapanel.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;

import enklikanketa.com.a1kapanel.Libraries.NotificationLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;

import static enklikanketa.com.a1kapanel.Libraries.NotificationLib.NOTIFICATION_PERSISTANT_TRACKING_ID;

public class LocationUpdatesService extends Service {
    private static final String PACKAGE_NAME =
            "enklikanketa.com.a1kapanel.Services";

    public static final String CLASS_NAME =
            PACKAGE_NAME + ".LocationUpdatesService";

    private static final String TAG = "LocationUpdatesService";

    public static final String STOPFOREGROUND_ACTION = "STOPFOREGROUND_ACTION";
    public static final String PAUSEFOREGROUND_ACTION = "PAUSEFOREGROUND_ACTION";
    public static final String STARTFOREGROUND_ACTION = "STARTFOREGROUND_ACTION";
    public static final String RESUMEFOREGROUND_ACTION = "RESUMEFOREGROUND_ACTION";

    static public final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static public final String EXTRA_LOCATION = PACKAGE_NAME + ".location";

    private final IBinder mBinder = new LocalBinder();

    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
     */
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    private Handler mServiceHandler;

    /**
     * The current location.
     */
    private Location mLocation;

    public LocationUpdatesService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundF();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction() != null) {
            // We got here because the user decided to remove location updates from the notification.
            if (intent.getAction().equals(STOPFOREGROUND_ACTION)){
                stopTracking();
            }
            else if (intent.getAction().equals(PAUSEFOREGROUND_ACTION)) {
                pauseTracking();
            } else if (intent.getAction().equals(RESUMEFOREGROUND_ACTION)) {
                requestLocationUpdates();
                startForegroundF();
            } else if (intent.getAction().equals(STARTFOREGROUND_ACTION)) {
                requestLocationUpdates();
                startForegroundF();
            }
        }

        // Tells the system to try to recreate the service after it has been killed.
        return START_STICKY;
    }

    private void startForegroundF(){
        startForeground(NOTIFICATION_PERSISTANT_TRACKING_ID, new NotificationLib(getBaseContext())
                .notificationLocationTracking(true));
    }

    public void stopTracking(){
        removeLocationUpdates();
        stopForeground(true);
        stopSelf();
    }

    public void pauseTracking(){
        removeLocationUpdates();
        TrackingLib tlib = new TrackingLib(this);
        if (tlib.serviceIsRunningInForeground()) {
            startForegroundF();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        setRequestingLocationUpdates(this, true);
        new TrackingLib(getBaseContext()).startForegroundService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            setRequestingLocationUpdates(this, false);
        } catch (SecurityException unlikely) {
            setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        TrackingLib tlib = new TrackingLib(getBaseContext());
        tlib.storeLocationData(location);

        mLocation = location;

        // Notify anyone listening for broadcasts about the new location.
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        HashMap<String, String> data = new TrackingLib(getBaseContext()).getLocationTrackingSettings();
        mLocationRequest = new LocationRequest();

        if(data != null) {
            int priority = data.get("tracking_accuracy").equals("high") ?
                    LocationRequest.PRIORITY_HIGH_ACCURACY : LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

            mLocationRequest.setInterval(Integer.parseInt(data.get("interval_wanted")) * 1000);
            mLocationRequest.setFastestInterval(Integer.parseInt(data.get("interval_fastes")) * 1000);
            mLocationRequest.setPriority(priority);
            mLocationRequest.setSmallestDisplacement(Integer.parseInt(data.get("displacement_min")));
        }
        //for test - when user turns tracking on from settings
        else{
            mLocationRequest.setInterval(30000);
            mLocationRequest.setFastestInterval(10000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationRequest.setSmallestDisplacement(0);
        }

        //mLocationRequest.setMaxWaitTime(10*60*1000);
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }

    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates";

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    private boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }

    /**
     * Returns the {@code location} object as a human readable string.
     * @param location  The {@link Location}.
     */
    static public String getLocationText(Location location) {
        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }
}
