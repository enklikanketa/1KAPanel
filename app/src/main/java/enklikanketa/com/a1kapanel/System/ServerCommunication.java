/*
 * Made by:
 * Uroš Podkrižnik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel.System;

import android.content.Context;

import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.R;

public class ServerCommunication {
    private String TAG = "ServerCommunication";

    private Context con;

    public ServerCommunication(Context ctx){
        con = ctx;
    }

    //Login
    public String PostLogin(JSONObject obj) {
        return APIexecutePost(obj, "checkLoginApp", null);
    }

    //Send device info
    public String PostDeviceInfo(JSONObject obj) {
        return APIexecutePost(obj, "mazaUpdateDeviceInfo", null);
    }

    //Send device registration ID
    public String PostRegistrationID(JSONObject obj) {
        return APIexecutePost(obj, "mazaUpdateRegistrationId", null);
    }

    //Send tracking logs
    public String PostTrackingLog(JSONObject obj) {
        return APIexecutePost(obj, "mazaUpdateTrackingLog", null);
    }

    //Send tracking logs
    public String PostTrackingLocations(JSONObject obj) {
        return APIexecutePost(obj, "mazaInsertTrackingLocations", null);
    }

    //Send triggered geofences
    public String PostTriggeredGeofences(JSONObject obj) {
        return APIexecutePost(obj, "mazaInsertTriggeredGeofences", null);
    }

    //Send request for alarms
    public String PostGetAlarms(JSONObject obj) {
        return APIexecutePost(obj, "mazaGetAlarms", null);
    }

    //Send request for geofences
    public String PostGetGeofences(JSONObject obj) {
        return APIexecutePost(obj, "mazaGetGeofences", null);
    }

    //Send request for tracking
    public String PostGetTracking(JSONObject obj) {
        return APIexecutePost(obj, "mazaGetTracking", null);
    }

    //Send request for entry
    public String PostGetEntry(JSONObject obj) {
        return APIexecutePost(obj, "mazaGetEntries", null);
    }

    //Send request for all my locations and AR
    public String PostGetMyLocations(JSONObject obj) {
        return APIexecutePost(obj, "mazaGetMyLocations", null);
    }

    //Send tracking permission
    public String PostSetTrackingPermission(JSONObject obj) {
        return APIexecutePost(obj, "mazaSetTrackingPermission", null);
    }

    //Send request for survey unit delete
    public String PostDeleteSurveyUnit(JSONObject obj) {
        return APIexecutePost(obj, "mazaDeleteSurveyUnit", null);
    }

    //Send request for survey unit delete
    public String PostUnsubscribeSurvey(JSONObject obj) {
        return APIexecutePost(obj, "mazaUnsubscribeSurvey", null);
    }

    //get list of surveys
    public String PostGetSurveyList(JSONObject obj) {
        return APIexecutePost(obj, "mazaGetSurveyList", null);
    }

    //get list of subscriptions
    public String PostGetSubscriptionsList(JSONObject obj) {
        return APIexecutePost(obj, "mazaGetSubscriptionsList", null);
    }

    //merge identifier
    public String PostMergeIdentifier(JSONObject obj) {
        return APIexecutePost(obj, "mazaMergeIdentifier", null);
    }

    //merge identifier
    public String PostGetSurveysInfoByIdentifier(JSONObject obj) {
        return APIexecutePost(obj, "mazaGetSurveysInfoByIdentifier", null);
    }

    /**
     * Create link and hmac token and post it to server - for API
     *
     * @param obj    - JSONObject - data to send to server
     * @param action - String - action name of API
     * @param params - String - additional params to send in link - null if not set
     * @return String - server answer
     */
    private String APIexecutePost(JSONObject obj, String action, String params) {
        if (Network.checkMobileInternet(con, true)) {

            //create request link
            String request = con.getString(R.string.server_url) +
                    "admin/survey/api/api.php?" + "action=" + action;

            //add aditional params if not null
            if (params != null)
                request += params;

            //create hmac hash
            String hmac = HMAC_SHA256(con.getString(R.string.enka_api_private_key),
                    "POST" + request + obj.toString());

            //return server answer
            return Network.postData(request + "&identifier=mazaApp&token=" + hmac, obj);

        } else
            return null;
    }

    /**
     * Make HMAC SHA256 String of given message
     *
     * @param secret  - String - private key
     * @param message - String - Message to hash
     * @return - String - HMAC SHA256
     */
    private String HMAC_SHA256(String secret, String message) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);

            return bytesToHex(sha256_HMAC.doFinal(message.getBytes("UTF-8")));
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
    }

    /**
     * Stringify byte array
     *
     * @param in -byte[] - byte array to stringify
     * @return - String - Stringified bytes
     */
    private String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
