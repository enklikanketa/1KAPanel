/*
 * Made by:
 * Uroš Podkrižnik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel.System;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.R;

public class Network {

    /**
     * Check if connected to internet
     *
     * @param con     Context
     * @param opozori true - show toast 'not connected' if not connected;
     *                false - don't show toast if not connected
     * @return true if connected, false otherwise
     */
    static public boolean checkMobileInternet(Context con, boolean opozori) {
        try {
            boolean HaveConnected = false;
            //get info for connectivity
            ConnectivityManager cm = (ConnectivityManager) con
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm != null) {
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (ni != null) {
                    if (ni.getType() == ConnectivityManager.TYPE_MOBILE
                            || ni.getType() == ConnectivityManager.TYPE_WIFI) {
                        if (ni.isConnected())
                            HaveConnected = true;
                    }
                }
                //if it is not connected to internet and opozori is true, show toast
                if (!HaveConnected && opozori) {
                    GeneralLib.showErrorToUser(con, con.getString(R.string.general_mobile_network_error));
                    return false;
                }
            }

            return HaveConnected;
        } catch (Exception e) {
            GeneralLib.reportCrash(e, null);
            return false;
        }
    }

    /**
     * F for posting data to server
     *
     * @param urlString URL to send request
     * @param obj       JSONObject of data to send
     * @return String of response
     */
    public static String postData(String urlString, JSONObject obj) {

        StringBuilder chaine = new StringBuilder("");

        try {
            //set URL and method
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "");
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.connect();

            //Send request
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(), "UTF-8"));
            bw.write(obj.toString());
            bw.flush();
            bw.close();

            //build response
            InputStream inputStream = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = rd.readLine()) != null) {
                chaine.append(line);
            }

        } catch (IOException e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
        return chaine.toString();
    }//postData

    /**
     * F for posting data to server
     *
     * @param ctx Context
     * @param urlString URL to send request
     * @param obj       JSONObject of data to send
     * @return String of response
     */
    public static String postNextPinData(Context ctx, String urlString, JSONObject obj) {

        String token = "";
        Database DB = (Database) Database.getInstance(ctx);
        //get identifier and set nextpinToken for user
        String[] user = DB.getRowData("uporabnik",
                new String[]{"identifier"}, null);
        if(user != null) {
            //if you dont define unique token for specific person/object you cant get data
            token = ctx.getString(R.string.nextPin_project_name) + "_" + user[0];
        }

        StringBuilder chaine = new StringBuilder("");

        try {
            //set URL and method
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("token", token);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.connect();

            //Send request
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    connection.getOutputStream(), "UTF-8"));
            bw.write(obj.toString());
            bw.flush();
            bw.close();

            //build response
            InputStream inputStream = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = rd.readLine()) != null) {
                chaine.append(line);
            }

        } catch (IOException e) {
            GeneralLib.reportCrash(e, null);
            return null;
        }
        return chaine.toString();
    }//postData
}
