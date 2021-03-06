package enklikanketa.com.a1kapanel.Libraries;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.util.Random;

import enklikanketa.com.a1kapanel.System.Database;

/**
 * Created by podkrizniku on 06/12/2017.
 */

public class GeneralLib {
    private static String TAG = "GeneralLib";

    /**
     * Creates random integer. Best for random int IDs
     * @return integet in range of 1000000 to 2147483646
     */
    public static int getRandomInt(){
        Random r = new Random();
        return r.nextInt(2147483646-1000000) + 1000000;
    }

    /**
     * Report an exception to FirebaseCrash
     *
     * @param e                - exception to report on FirebaseCrash
     * @param additionalString - additional text to add to end of FirebaseCrash report e.g. to add
     *                         a string that can not be transformed in JSONObject (JSONException
     *                         only report first word)
     */
    public static void reportCrash(Exception e, String additionalString) {
        String additionalStringPrefix = "\n\nADDITIONAL FROM DEV: ";

        if (additionalString != null) {
            if (additionalString.equals(""))
                additionalString = additionalStringPrefix + "[empty string]";
            else
                additionalString = additionalStringPrefix + additionalString;
        } else
            additionalString = "";

        Crashlytics.log(e + additionalString);
        Log.e(TAG, e + additionalString);
    }

    /**
     * Show error to user by toast
     *
     * @param ctx - Context
     * @param errorMessage - message that will show in toast
     */
    public static void showErrorToUser(Context ctx, String errorMessage) {
        if(ctx != null)
            Toast.makeText(ctx, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * More safe closing or dismissing of dialog
     *
     * @param act    - activity
     * @param dialog - dialog to close or dismiss
     */
    public static void dismissDialog(Activity act, Dialog dialog) {
        try {
            if (((dialog != null) && dialog.isShowing()) && getValidActivity(act) != null) {
                dialog.dismiss();
            }
        } catch (final IllegalArgumentException e) {
            // Handle or log or ignore
        }
    }

    /**
     * Check if activity is valid and return it
     *
     * @param act    - activity to check
     */
    private static Activity getValidActivity(Activity act) {
        try {
            if (act != null && (!act.isFinishing() || !act.isDestroyed()))
                return act;
            else
                return null;

        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Check if activity is valid
     *
     * @param act    - activity to check
     */
    public static boolean isActivityValid(Activity act) {
        return getValidActivity(act) != null;
    }

    /**
     * Get Spanned from html according to android version
     *
     * @param html - string to go spanned
     * @return spanned of html
     */
    public static Spanned fromHtml(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        else
            return Html.fromHtml(html);
    }

    /**
     * Delete survey from DB - because of foreign keys, it deletes alarms, repeaters and geofences for this survey
     * @param ctx - Context
     * @param srv_id - id of survey
     */
    public static void deleteSurveyDB(Context ctx, String srv_id){
        Database DB = (Database) Database.getInstance(ctx);
        DB.deleteRows("surveys", "id='"+srv_id+"'");
    }

    /**
     * Delete survey from DB - because of foreign keys, it deletes alarms, repeaters and geofences for this survey
     * @param ctx - Context
     * @param ids - ids of surveys - ie "2, 12, 13, 14, 24"
     */
    public static void deleteSurveysDB(Context ctx, String ids){
        Database DB = (Database) Database.getInstance(ctx);
        DB.deleteRows("surveys", "id IN ("+ids+")");
    }

    /**
     * Return the current state of the permissions needed.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static boolean arePermissionsAdded(Context ctx) {
        int permissionState = ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param ctx Context
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static int convertDpToPixel(Context ctx, float dp){
        return (int)(dp * ((float) ctx.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
