/*
 * Made by:
 * Uro� Podkri�nik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;

import enklikanketa.com.a1kapanel.System.Database;

public class RegisteredGeofences extends Fragment {

    Activity ctx;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.registered_geofences, container, false);

        MapView mMapView = view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately

        MapsInitializer.initialize(getActivity().getApplicationContext());

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap mMap) {
                //enable zooming control
                mMap.getUiSettings().setZoomControlsEnabled(true);

                //set builder of bounds of map
                LatLngBounds.Builder bbuilder = new LatLngBounds.Builder();

                int surveyNum = -1, allColorsNum = geoColors().size();
                //temporary survey id
                String temp_srv_id = "";

                //get geofences from DB
                Database DB = (Database) Database.getInstance(ctx);
                ArrayList<HashMap<String, String>> geofences = DB.getListHashMapData("geofences", null, null, "srv_id");

                for(HashMap<String, String> geofence : geofences){
                    //if new survey, change fill colour of geofence
                    if(!temp_srv_id.equals(geofence.get("srv_id"))){
                        surveyNum++;
                        temp_srv_id = geofence.get("srv_id");
                    }

                    //set latlng of center of geofence
                    LatLng latlng = new LatLng(Double.parseDouble(geofence.get("lat")), Double.parseDouble(geofence.get("lng")));

                    String name = geofence.get("name") != null ? geofence.get("name") : geofence.get("address");

                    //set circle options
                    CircleOptions circleOptions = new CircleOptions()
                            .center(latlng)
                            .clickable(true)
                            .radius(Double.parseDouble(geofence.get("radius"))) // In meters
                            .fillColor(geoColors().get(surveyNum%allColorsNum));

                    //add circe to map
                    mMap.addCircle(circleOptions).setTag(name);
                    mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
                            @Override
                            public void onCircleClick(Circle circle) {
                                Marker markerForInfoWindow = mMap.addMarker(new MarkerOptions()
                                        .alpha(0.0f)
                                        .infoWindowAnchor(.6f,1.0f)
                                        .position(circle.getCenter()));

                                if(circle.getTag() != null)
                                    markerForInfoWindow.setTitle(circle.getTag().toString());

                                markerForInfoWindow.showInfoWindow();
                            }
                    });
                    //add latlng of circle center to bounds boulder
                    bbuilder.include(latlng);

                    //set bounds
                    LatLngBounds bounds = bbuilder.build();
                    int padding = 50; // offset from edges of the map in pixels
                    //set and animate map to bounds
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mMap.animateCamera(cu);
                }
            }
        });

        return view;
    }

    /**
     * Get colors for geofences
     * @return ArrayList of integers of colors
     */
    //colors for geofences
    private ArrayList<Integer> geoColors() {
        ArrayList<Integer> colors = new ArrayList<>();

        //office iz 1ke
        for (String c : new String[]{"#454f81bd", "#45c0504d", "#459bbb59", "#458064a2", "#454bacc6", "#45f79646", "#4592a9cf"})
            colors.add(Color.parseColor(c));

        //pastel iz 1ke
        for (String c : new String[]{"#45799f0b", "#45d7a125", "#459264be", "#45188484", "#454cc68b", "#458a8823", "#456c99d2"})
            colors.add(Color.parseColor(c));

        //blaga iz 1ke
        for (String c : new String[]{"#45bce02e", "#45e0642e", "#45e0d62e", "#452e97e0", "#45b02ee0", "#4500fbff", "#455ce02e"})
            colors.add(Color.parseColor(c));

        return colors;
    }//barve

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
