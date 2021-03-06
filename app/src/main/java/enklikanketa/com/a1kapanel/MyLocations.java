/*
 * Made by:
 * Uro� Podkri�nik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.System.Database;

public class MyLocations extends Fragment {

    Activity ctx;
    TextView last_ar;
    String TAG = "MyLocations";
    ClusterManager mClusterManager;
    boolean clusteringOn = true;
    GoogleMap mMap;
    ArrayList<HashMap<String, String>> locations;
    BroadcastReceiver broadcastReceiver;
    Marker clickedMarker;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.my_locations, container, false);

        last_ar = view.findViewById(R.id.last_ar);

        MapView mMapView = view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately

        MapsInitializer.initialize(getActivity().getApplicationContext());

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                mMap = map;

                //enable zooming control
                mMap.getUiSettings().setZoomControlsEnabled(true);

                //get geofences from DB
                Database DB = (Database) Database.getInstance(ctx);
                locations = DB.getListHashMapData("locations", null, null, null);

                fillMap();
            }
        });

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals(TrackingLib.AR_BROADCAST_DETECTED_ACTIVITY)) {
                    String detected_activities = intent.getStringExtra("detected_activities");
                    handleUserActivity(detected_activities);
                }
            }
        };

        return view;
    }

    private void handleUserActivity(String detected_activities) {
        if (detected_activities != null) {
            last_ar.setText(detected_activities);
        }
    }

    private void fillMap(){
        if(locations != null){
            //set builder of bounds of map
            LatLngBounds.Builder bbuilder = new LatLngBounds.Builder();

            if(clusteringOn) {
                // Initialize the manager with the context and the map.
                // (Activity extends context, so we can pass 'this' in the constructor.)
                mClusterManager = new ClusterManager<MyItem>(ctx, mMap);
                mClusterManager.setOnClusterItemInfoWindowClickListener(new ClusterManager.OnClusterItemInfoWindowClickListener<MyItem>() {
                    @Override
                    public void onClusterItemInfoWindowClick(MyItem myItem) {
                        showDeleteLocationAlert(myItem.getTag().toString());
                    }
                });

                // Point the map's listeners at the listeners implemented by the cluster
                // manager.
                mMap.setOnCameraIdleListener(mClusterManager);
                mMap.setOnMarkerClickListener(mClusterManager);
            }
            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                @Override
                public void onInfoWindowClick(Marker marker) {
                    clickedMarker = marker;
                    if(marker.getTag() != null)
                        showDeleteLocationAlert(marker.getTag().toString());
                }
            });

            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker arg0) {
                }
                @SuppressWarnings("unchecked")
                @Override
                public void onMarkerDragEnd(Marker arg0) {
                    if(arg0.getTag() != null)
                        new TrackingLib(ctx).editLocation(arg0);
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(arg0.getPosition()));
                }
                @Override
                public void onMarkerDrag(Marker arg0) {
                }
            });


            for(HashMap<String, String> location : locations) {
                //set latlng of center of geofence
                LatLng latlng = new LatLng(Double.parseDouble(location.get("lat")), Double.parseDouble(location.get("lng")));

                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
                String datetime = sdf.format(new Date(Long.parseLong(location.get("timestamp")) * 1000));

                //clustering is on, add locations to cluster
                if(clusteringOn) {
                    MyItem offsetItem = new MyItem(latlng, datetime, null, location.get("id"));
                    if (mClusterManager != null)
                        mClusterManager.addItem(offsetItem);
                }
                //clustering is off, add each marker to map
                else {
                    //set marker options
                    MarkerOptions markerOpt = new MarkerOptions().position(latlng)
                            .title(datetime).snippet(getString(R.string.my_location_infowindow_snippet)).draggable(true);
                    if(!location.get("sending").equals("2"))
                        markerOpt.icon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                    //add marker to map
                    mMap.addMarker(markerOpt).setTag(location.get("id"));
                }

                //add latlng of location to bounds builder
                bbuilder.include(latlng);
            }
            //set bounds
            LatLngBounds bounds = bbuilder.build();
            int padding = 150; // offset from edges of the map in pixels
            //set and animate map to bounds
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);
        }
    }

    public class MyItem implements ClusterItem {
        private final LatLng mPosition;
        private String mTitle;
        private String mSnippet;
        private Object mTag;

        public MyItem(LatLng latlng, String title) {
            mPosition = latlng;
            mTitle = title;
        }

        public MyItem(LatLng latlng, String title, String snippet, Object tag) {
            mPosition = latlng;
            mTitle = title;
            mSnippet = snippet;
            mTag = tag;
        }

        public MyItem(double lat, double lng) {
            mPosition = new LatLng(lat, lng);
        }

        public MyItem(double lat, double lng, String title, String snippet) {
            mPosition = new LatLng(lat, lng);
            mTitle = title;
            mSnippet = snippet;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        @Override
        public String getTitle() {
            return mTitle;
        }

        @Override
        public String getSnippet() {
            return mSnippet;
        }

        public Object getTag() {
            return mTag;
        }
    }

    private void showDeleteLocationAlert(final String location_id){
        final AlertDialog myDialog = new AlertDialog.Builder(ctx)
                .setTitle(R.string.location_delete_alert_title)
                .setMessage(getString(R.string.location_delete_alert_desc))
                .setNegativeButton(R.string.location_delete_alert_no, null)
                .setPositiveButton(R.string.location_delete_alert_yes, null).create();

        myDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button btne = myDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                btne.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        myDialog.dismiss();
                    }
                });

                Button btpo = myDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btpo.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        myDialog.dismiss();
                        clickedMarker.remove();
                        clickedMarker = null;
                        new TrackingLib(ctx).deleteLocation(location_id);
                    }
                });
            }
        });
        myDialog.show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.my_locations_menu, menu);
        MenuItem clustering = menu.getItem(0);
        clustering.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toggle_clustering:
                if(clusteringOn) {
                    item.setIcon(getResources().getDrawable(R.drawable.ic_mode_edit_white));
                    mClusterManager.clearItems();
                    mClusterManager.cluster();
                    clusteringOn = false;
                }
                else {
                    item.setIcon(getResources().getDrawable(R.drawable.ic_place_white));
                    mMap.clear();
                    clusteringOn = true;
                }
                fillMap();
                break;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(ctx).registerReceiver(broadcastReceiver,
                new IntentFilter(TrackingLib.AR_BROADCAST_DETECTED_ACTIVITY));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(ctx).unregisterReceiver(broadcastReceiver);
    }
}
