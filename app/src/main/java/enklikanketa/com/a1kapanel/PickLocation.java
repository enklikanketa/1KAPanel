/*
 * Made by:
 * Uro� Podkri�nik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;

public class PickLocation extends AppCompatActivity {

    String TAG = "PickLocation";
    GoogleMap mMap;
    LatLng location;
    float mapZoom = 16;
    boolean editMode = false;
    private GoogleMap.OnCameraIdleListener onCameraIdleListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_location);

        getSupportActionBar();

        MapView mMapView = findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately

        MapsInitializer.initialize(this);

        configureCameraIdle();

        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            if(bundle.getString("latitude") != null && !bundle.getString("latitude").equals("") &&
                    bundle.getString("longitude") != null && !bundle.getString("longitude").equals("")) {
                double latitude = Double.valueOf(bundle.getString("latitude"));
                double longitude = Double.valueOf(bundle.getString("longitude"));
                location = new LatLng(latitude, longitude);
                editMode = true;
            }
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                mMap = map;
                //set padding to map overlay, so map item can all be above "DONE" button
                //because of that padding, center of map is moved up (so marker image has to be moved up too)
                mMap.setPadding(0, GeneralLib.convertDpToPixel(PickLocation.this, (int)
                        (getResources().getDimension(R.dimen.map_bottom_padding) / getResources().getDisplayMetrics().density)),
                        0, GeneralLib.convertDpToPixel(PickLocation.this, (int)
                        (getResources().getDimension(R.dimen.map_bottom_padding) / getResources().getDisplayMetrics().density)));

                //enable +/- zoom controls
                mMap.getUiSettings().setZoomControlsEnabled(true);

                if (ActivityCompat.checkSelfPermission(PickLocation.this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(PickLocation.this,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    //enable "my location" button on map
                    mMap.setMyLocationEnabled(true);

                    if(location == null)
                        //get last known location and center it on map
                        moveToLastKnownLocation();
                    else
                        //Move the camera to the user's location and zoom in!
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, mapZoom));
                }

                mMap.setOnCameraIdleListener(onCameraIdleListener);
            }
        });

        Button done = findViewById(R.id.locationPicked);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(location != null) {
                    if (!editMode) {
                        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(PickLocation.this);
                        try {
                            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    // Got last known location. In some rare situations this can be null.
                                    if (location != null) {
                                        long loc_id = new TrackingLib(getBaseContext()).storeLocationData(location, 2);
                                        moveToFillOut(loc_id + "");
                                    }
                                    //in case there is no last know location, don't include one
                                    //this is in case user hasn't turned on location services since reboot
                                    else
                                        moveToFillOut("");
                                }
                            });
                        } catch (SecurityException unlikely) {
                            Log.e(TAG, "Lost location permission." + unlikely);
                            moveToFillOut("");
                        }
                    } else
                        moveToFillOut("");
                }
            }
        });
    }

    /**
     * Redirect to WebResevanje.java to start filling out survey
     * @param loc_id - ID of location
     */
    private void moveToFillOut(String loc_id){
        Intent myIntent = new Intent(PickLocation.this, WebResevanje.class);
        if(!editMode)
            myIntent.putExtra("srv_version_timestamp", System.currentTimeMillis()/1000+"");
        myIntent.putExtra("lat", location.latitude+"");
        myIntent.putExtra("lng", location.longitude+"");
        myIntent.putExtra("mode", "entry");
        myIntent.putExtra("loc_id", loc_id);
        Bundle bundle = getIntent().getExtras();
        if(bundle != null)
            myIntent.putExtra("link", bundle.getString("link"));
        startActivity(myIntent);
        finish();
    }

    /**
     * Get last known location and center it on map
     */
    private void moveToLastKnownLocation(){
        // Getting LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Creating a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        String provider = null;
        if(locationManager != null)
            // Getting the name of the best provider
            provider = locationManager.getBestProvider(criteria, true);

        Location currentLocation = null;

        if (ActivityCompat.checkSelfPermission(PickLocation.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(PickLocation.this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                locationManager != null && provider != null)
        // Getting Current Location
        currentLocation = locationManager.getLastKnownLocation(provider);

        if (currentLocation != null) {
            // Getting latitude of the current location
            double latitude = currentLocation.getLatitude();

            // Getting longitude of the current location
            double longitude = currentLocation.getLongitude();

            location = new LatLng(latitude, longitude);

            //Move the camera to the user's location and zoom in!
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, mapZoom));
        }
    }

    /**
     * Set listener of changed center on map (when map stopped dragging or zooming)
     */
    private void configureCameraIdle() {
        onCameraIdleListener = new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mapZoom = mMap.getCameraPosition().zoom;
                location = mMap.getCameraPosition().target;
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
