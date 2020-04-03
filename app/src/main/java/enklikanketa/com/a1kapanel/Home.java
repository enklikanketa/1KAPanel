package enklikanketa.com.a1kapanel;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.GeofencingLib;
import enklikanketa.com.a1kapanel.Libraries.TrackingLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.Network;

public class Home extends AppCompatActivity {

    private String TAG = "Home";
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private DrawerLayout mDrawerLayout;
    private RelativeLayout slideMenu;
    private ActionBarDrawerToggle mDrawerToggle;

    //dialog for tracking permissions
    AlertDialog trackingPermissionDialog = null;

    // used to store app title
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        mTitle = getTitle();

        moveDrawerToTop();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions();
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // enabling action bar app icon and behaving it as toggle button
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        //menu drawer (hamburger)
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ) {
            public void onDrawerClosed(View view) {
                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            // on first time display view for first nav item
            if (getIntent().getExtras() != null)
                displayView(getIntent().getExtras().getInt("fragment"));
            else {
                displayView(3);
            }
        }

        //button for nmerge identifier
        LinearLayout merge = findViewById(R.id.layMerge);
        merge.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Network.checkMobileInternet(Home.this, true)) {
                    Intent intent = new Intent(Home.this, MergeIdentifier.class);
                    startActivity(intent);
                    mDrawerLayout.closeDrawer(slideMenu);
                }
            }
        });

        //button for all surveys
        LinearLayout vsi = findViewById(R.id.layVsi);
        vsi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Network.checkMobileInternet(Home.this, true)) {
                    displayView(0);
                }
            }
        });

        //button for all subscriptions
        LinearLayout subs = findViewById(R.id.laySubscriptions);
        subs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Network.checkMobileInternet(Home.this, true)) {
                    displayView(3);
                }
            }
        });

        //button for settings
        LinearLayout settings = findViewById(R.id.laySettings);
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Home.this, PrefsActivity.class);
                if (Network.checkMobileInternet(Home.this, true)) {
                    startActivity(intent);
                    mDrawerLayout.closeDrawer(slideMenu);
                }
            }
        });

        //button for aplication info
        LinearLayout abourapp = findViewById(R.id.layAbout);
        abourapp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(Home.this, About.class);
                if (Network.checkMobileInternet(Home.this, true)) {
                    startActivity(myIntent);
                    mDrawerLayout.closeDrawer(slideMenu);
                }
            }
        });

        //button for feedback survey
        LinearLayout feedback = findViewById(R.id.layFeedback);
        feedback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Network.checkMobileInternet(Home.this, true)) {
                    feedback();
                    mDrawerLayout.closeDrawer(slideMenu);
                }
            }
        });
    }

    //F for feedback
    private void feedback() {
        //open feedback questionnaire in webActivity
        Intent myIntent = new Intent(Home.this, WebResevanje.class);
        myIntent.putExtra("link", getString(R.string.feedbackLink));
        myIntent.putExtra("naslov", getResources().getString(R.string.feedback));
        if (Network.checkMobileInternet(Home.this, true)) {
            startActivity(myIntent);
        }
    }

    private void moveDrawerToTop() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        DrawerLayout drawer = (DrawerLayout) inflater.inflate(R.layout.drawer, null); // "null" is important.

        // HACK: "steal" the first child of decor view
        ViewGroup decor = (ViewGroup) getWindow().getDecorView();
        View child = decor.getChildAt(0);
        decor.removeView(child);
        LinearLayout container = (LinearLayout) drawer.findViewById(R.id.drawer_content); // This is the container we defined just now.
        container.addView(child);
        int SBH = getStatusBarHeight();
        drawer.setPadding(0, SBH, 0, 0);
        slideMenu = (RelativeLayout) drawer.findViewById(R.id.sliding_menu);
        slideMenu.setPadding(0, SBH, 0, 0);

        // Make the drawer replace the first child
        decor.addView(drawer);

        mDrawerLayout = drawer.findViewById(R.id.drawer_layout);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Diplaying fragment view for selected nav drawer list item
     */
    private void displayView(int position) {
        Fragment fragment = null;
        String fragmentTag = "";
        // update the main content by replacing fragments
        switch (position) {
            case 0:
                fragment = new SurveysToFillFragment();
                fragmentTag = getResources().getString(R.string.surveys);
                setTitle(R.string.surveys);
                break;
            case 1:
                fragment = new RegisteredGeofences();
                fragmentTag = getResources().getString(R.string.geofences);
                setTitle(R.string.geofences);
                break;
            case 2:
                fragment = new MyLocations();
                fragmentTag = getResources().getString(R.string.my_locations);
                setTitle(R.string.my_locations);
                break;
            case 3:
                fragment = new AllSubscriptions();
                fragmentTag = getResources().getString(R.string.subscriptions);
                setTitle(R.string.subscriptions);
                break;
            default:
                break;
        }
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.main_content, fragment, fragmentTag).commit();
            mDrawerLayout.closeDrawer(slideMenu);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (!GeneralLib.arePermissionsAdded(this)) {
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            Home.this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        boolean shouldProvideRationale =
                this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            Home.this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted.");

                TrackingLib tlib = new TrackingLib(this);
                //if you want to track cordinates/accelerometer and other things you can
                if(!tlib.serviceIsRunningInForeground()) {
                    //run tracking if there are subscribed surveys using it
                    if(tlib.areTrackingPermissionGrantedAndRunning()) {
                        tlib.startTracking("GPS_permission_granted");
                    }
                }

                new GeofencingLib(Home.this).reRunGeofences();
            } else {
                Log.i(TAG, "Permission else.");
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //if permission dialog is already showing, do not show another one
        if(trackingPermissionDialog == null || !trackingPermissionDialog.isShowing()) {
            //show alert for permissions for surveys which need it
            trackingPermissionDialog = new TrackingLib(this).showNonPermissionedTracking(this);
        }

        //button for my geofences
        Database DB = (Database) Database.getInstance(this);
        if(DB.countData("geofences", null) > 0) {
            LinearLayout myageofences = findViewById(R.id.layMyGeofences);
            myageofences.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (Network.checkMobileInternet(Home.this, true)) {
                        displayView(1);
                    }
                }
            });
            myageofences.setVisibility(View.VISIBLE);
        }

        //button for my locations
        if(DB.countData("locations", null) > 0) {
            LinearLayout myalocations = findViewById(R.id.layMyLocations);
            myalocations.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (Network.checkMobileInternet(Home.this, true)) {
                        displayView(2);
                    }
                }
            });
            myalocations.setVisibility(View.VISIBLE);
        }
    }//onResume
}
