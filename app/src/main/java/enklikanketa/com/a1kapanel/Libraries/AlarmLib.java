package enklikanketa.com.a1kapanel.Libraries;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import enklikanketa.com.a1kapanel.Receivers.AlarmReceiver;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.Tasks.sendGetAlarmsTask;

import static android.content.Context.ALARM_SERVICE;

/**
 * Created by podkrizniku on 06/12/2017.
 */

public class AlarmLib {

    private String TAG = "AlarmLib";
    private Context ctx;
    private Database DBH;

    public AlarmLib(Context base) {
        ctx = base;
        DBH = (Database) Database.getInstance(ctx);
    }

    /**
     * Create and run alarm fo this alarm id/broadcast requsr code
     * If alarm for this id already exists, this will update it, if not it will create it
     * @param data data of alarm
     */
    private void setAlarm(HashMap<String, String> data){
        int sender_id = Integer.valueOf(data.get("sender_id"));
        long triggerAtMillis = 0, intervalMillis = 0, oneDayMillis = 86400000, oneWeekMillis = 604800000;

        //get current time in HHmm string
        SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
        String currentTime = sdf.format(new Date());

        Intent mintent = new Intent(ctx, AlarmReceiver.class);
        mintent.putExtra("title", data.get("title"));
        mintent.putExtra("message", data.get("message"));
        mintent.putExtra("link", data.get("link"));
        mintent.putExtra("sound", data.get("sound"));
        mintent.putExtra("srv_id", data.get("srv_id"));
        mintent.putExtra("sender_id", sender_id+"");
        PendingIntent sender = PendingIntent.getBroadcast(ctx, sender_id, mintent, PendingIntent.FLAG_UPDATE_CURRENT);

        switch(data.get("repeat_by")){
            case "everyday":
                //get millis to trigger alarm
                triggerAtMillis = millisToTimeInDay(currentTime, data.get("time_in_day"));
                //24 hours in ms
                intervalMillis = oneDayMillis;
                break;
            case "daily":
                int every_which_day = Integer.valueOf(data.get("every_which_day"));
                //get millis to trigger alarm
                triggerAtMillis = millisToTimeInDay(currentTime, data.get("time_in_day")) + (every_which_day * oneDayMillis);
                //24 hours * every_which_day in ms
                intervalMillis = oneDayMillis * every_which_day;
                break;
            case "weekly":
                //which day in week is today
                int dayInWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                //convert start of week from sunday to monday
                dayInWeek = (dayInWeek != 1) ? dayInWeek-1 : 7;

                //how many days is from today to alarm day of week (days rounded up)
                int daysToAdd = (Integer.valueOf(data.get("day_in_week")) - dayInWeek + 7) % 7;

                //if time of day is not reachable today, subtract one day - If now is Monday and 15:00
                //and alarm must be triggered at Thursday at 11:00, alarm must be first triggered
                //in 2 days (2*24 hours) + 20 hours (from monday to thursday is 3 days)
                if (!isTimeReachableToday(currentTime, data.get("time_in_day"))) {
                    //if alarm is for today and we are past this time, alarm has to start in 6 days + rest of time
                    if(daysToAdd == 0)
                        daysToAdd = 6;
                    //remove a day, we are past this time
                    else
                        daysToAdd--;
                }

                //get millis to trigger alarm
                triggerAtMillis = millisToTimeInDay(currentTime, data.get("time_in_day")) + (daysToAdd * oneDayMillis);

                //one week in ms
                intervalMillis = oneWeekMillis;
                break;
        }

        AlarmManager alarm1 = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
        if(alarm1 != null)
            alarm1.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + triggerAtMillis, intervalMillis, sender);
    }

    /**
     * Calculates how many milliseconds is between first and second time.
     * If fromTime is greater than toTime, it calculates time to toTime of next day (tomorrow)
     * @param fromTime - string of HHmm od first time
     * @param toTime - string of HHmm of second time to calculate millisecond gasp from fromTime
     * @return long milliseconds of gasp between first time and next time in day (today or tomorrow)
     */
    public long millisToTimeInDay(String fromTime, String toTime){
        //if time is the same, count it as 1 day
        if(fromTime.equals(toTime))
            return 86400000;
        else {
            int timeM = Integer.valueOf(toTime.substring(toTime.length() - 2));
            int timeH = Integer.valueOf(toTime.substring(0, toTime.length() - 2));
            int cTimeM = Integer.valueOf(fromTime.substring(fromTime.length() - 2));
            int cTimeH = Integer.valueOf(fromTime.substring(0, fromTime.length() - 2));

            int subH;
            if (cTimeH <= timeH)
                subH = timeH - cTimeH;
            else
                subH = 24 - cTimeH + timeH;

            int subM;
            if (cTimeM <= timeM)
                subM = timeM - cTimeM;
            else {
                subM = 60 - cTimeM + timeM;
                if (subH > 0)
                    subH--;
                else
                    subH = 23;
            }

            //get milliseconds from calculated time
            return ((subH * 60) + subM) * 60000;
        }
    }

    /**
     * Determines if toTimeInDay time is reachable for today.
     *
     * @param currentTime - string of HHmm od current time
     * @param toTimeInDay - string of HHmm of time to calculate millisecond gasp from currentTime
     * @return true if currentTime is lesser than toTimeInDay, false otherwise
     */
    private boolean isTimeReachableToday(String currentTime, String toTimeInDay){
        int timeH = Integer.valueOf(toTimeInDay.substring(0, toTimeInDay.length() - 2));
        int cTimeH = Integer.valueOf(currentTime.substring(0, currentTime.length() - 2));

        if(cTimeH < timeH)
            return true;
        else if(cTimeH > timeH)
            return false;
        //cTimeH == timeH
        else {
            int cTimeM = Integer.valueOf(currentTime.substring(currentTime.length() - 2));
            int timeM = Integer.valueOf(toTimeInDay.substring(toTimeInDay.length() - 2));

            return (cTimeM <= timeM);
        }
    }

    /**
     * Check if there are alarms in DB, then check if they are running, if not, set them
     */
    public void reRunAlarms(){
        checkAndRunAlarms(null, true);
    }

    /**
     * Check if there are alarms in DB, then check if they are running, if not, set them
     */
    public void checkAndRunAlarms(){
        checkAndRunAlarms(null, false);
    }

    /**
     * Check if there are alarms for this survey in DB, then check if they are running, if not, set them
     * @param srv_id - id of survey
     */
    private void checkAndRunAlarms(String srv_id, boolean reRunMode){
        String where = srv_id == null ? null : "srv_id='"+srv_id+"'";
        String[][] alarms = DBH.getData("alarms",
                new String[]{"srv_id", "alarm_notif_title", "alarm_notif_message", "alarm_notif_sound",
                        "alarm_id", "repeat_by", "time_in_day", "day_in_week", "every_which_day"}, where);

        //are there alarms in DB?
        if(alarms != null)
            for(String[] arr : alarms) {
                if(!reRunMode) {
                    //is alarm for that id not registered yet?
                    if (PendingIntent.getBroadcast(ctx, Integer.parseInt(arr[4]),
                            new Intent(ctx, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE) == null)
                        executeSetAlarm(arr);
                }
                else{
                    //is alarm for that id not registered yet?
                    if(PendingIntent.getBroadcast(ctx, Integer.parseInt(arr[4]),
                            new Intent(ctx, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE) != null)
                        cancelAlarm(AlarmReceiver.class, Integer.parseInt(arr[4]));
                    executeSetAlarm(arr);
                }
            }
    }

    /**
     * Executes setting for alarm and sets it
     * @param arr - array of alarm data
     */
    private void executeSetAlarm(String[] arr){
        String[] survey = DBH.getRowData("surveys",
                new String[]{"link"}, "id=" + arr[0]);
        String link = null;
        if (survey != null)
            link = survey[0];

        HashMap<String, String> data = new HashMap<>();
        data.put("link", link);
        data.put("title", arr[1]);
        data.put("message", arr[2]);
        data.put("sound", arr[3]);
        data.put("sender_id", arr[4]);
        data.put("repeat_by", arr[5]);
        data.put("time_in_day", arr[6]);
        data.put("day_in_week", arr[7]);
        data.put("every_which_day", arr[8]);
        data.put("srv_id", arr[0]);

        //set alarm
        setAlarm(data);
    }

    /**
     * Set alarm - schedule a series of notifications
     * @param data - Data of needed values to show notification
     */
    public void setOrUpdateNewAlarm(Map<String, String> data){
        //you can get your text message here.
        String ank_id = data.get("ank_id");

        ContentValues initialValues = new ContentValues();
        initialValues.put("id",  ank_id);
        initialValues.put("link", data.get("link"));
        initialValues.put("title", data.get("srv_title"));

        //is alarm in DB for that survey
        String[] alarm = DBH.getRowData("alarms",
                new String[]{"alarm_id"}, "srv_id='"+ank_id+"'");

        //there is alarm in DB for that survey - remove it
        if(alarm != null)
            cancelAndDeleteAlarms(ank_id);

        //insert new survey in DB, if already exists, id is -1
        DBH.getWritableDatabase().insertWithOnConflict("surveys", null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);

        //set all new alams
        setAlarms(data);
    }

    /**
     * Create and run alarm fo this alarm id/broadcast requsr code
     * If alarm for this id already exists, this will update it, if not it will create it
     * @param data data of alarm/notification
     */
    private void setAlarms(Map<String, String> data){
        try{
            //set and store alarms
            JSONObject repeatObj = new JSONObject(data.get("repeat"));
            //String repeatby = repeatObj.getString("repeat_by");
            switch(repeatObj.getString("repeat_by")){
                case "everyday":
                    for(int i = 0; i < repeatObj.getJSONArray("time_in_day").length(); i++){
                        data.put("repeat_by", "everyday");
                        data.put("time_in_day", repeatObj.getJSONArray("time_in_day").getString(i));
                        data.put("day_in_week", null);
                        data.put("every_which_day", null);
                        insertAlarmDB(data, GeneralLib.getRandomInt()+"");
                    }
                    break;
                case "daily":
                    for(int i = 0; i < repeatObj.getJSONArray("time_in_day").length(); i++){
                        data.put("repeat_by", "daily");
                        data.put("time_in_day", repeatObj.getJSONArray("time_in_day").getString(i));
                        data.put("day_in_week", null);
                        data.put("every_which_day", repeatObj.getString("every_which_day"));
                        insertAlarmDB(data, GeneralLib.getRandomInt()+"");
                    }
                    break;
                case "weekly":
                    for(int j = 0; j < repeatObj.getJSONArray("day_in_week").length(); j++) {
                        for (int i = 0; i < repeatObj.getJSONArray("time_in_day").length(); i++) {
                            data.put("repeat_by", "weekly");
                            data.put("time_in_day", repeatObj.getJSONArray("time_in_day").getString(i));
                            data.put("day_in_week", repeatObj.getJSONArray("day_in_week").getString(j));
                            data.put("every_which_day", null);
                            insertAlarmDB(data, GeneralLib.getRandomInt() + "");
                        }
                    }
                    break;
            }

            JSONObject repeaterObj = new JSONObject(data.get("repeater"));
            String[] repeaterrow = DBH.getRowData("repeaters", new String[]{"datetime_start", "datetime_last_check"},
                    "srv_id='"+data.get("ank_id")+"'");

            //update datetime_last_check
            if(repeaterrow != null){
                if(repeaterObj.has("last_answered_version_datetime")) {
                    ContentValues cv = new ContentValues();
                    cv.put("datetime_last_check", repeaterObj.getString("last_answered_version_datetime"));
                    DBH.updateData("repeaters", cv, "srv_id='" + data.get("ank_id") + "'");
                }
            }
            //store repeater
            else{
                HashMap<String, String> repeaterData = new HashMap<>();
                repeaterData.put("srv_id", data.get("ank_id"));
                repeaterData.put("repeat_by", repeaterObj.getString("repeat_by"));
                repeaterData.put("time_in_day", repeaterObj.getString("time_in_day"));
                repeaterData.put("day_in_week", repeaterObj.getString("day_in_week"));
                repeaterData.put("every_which_day", repeaterObj.getString("every_which_day"));
                repeaterData.put("datetime_start", repeaterObj.getString("datetime_start"));
                repeaterData.put("datetime_end", repeaterObj.getString("datetime_end"));
                if(repeaterObj.has("datetime_user_started"))
                    repeaterData.put("datetime_user_started", repeaterObj.getString("datetime_user_started"));
                else
                    repeaterData.put("datetime_user_started", null);
                if(repeaterObj.has("last_answered_version_datetime"))
                    repeaterData.put("datetime_last_check", repeaterObj.getString("last_answered_version_datetime"));
                else
                    repeaterData.put("datetime_last_check", null);

                insertRepeaterDB(repeaterData);
            }
        } catch(JSONException e){
            Log.e(TAG, "AlarmLib.setAlarms() - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }

        //set alarm
        checkAndRunAlarms(data.get("ank_id"), false);
    }

    /**
     * Store alarm data in DB
     * @param data - hashmap of data of alarm to store in DB
     * @param alarm_id - generated int as id of alarm
     */
    private void insertAlarmDB(Map<String, String> data, String alarm_id){
        //vstavi v tabelo novegi alarm
        DBH.insertData("alarms", new String[][]{
                {"srv_id", data.get("ank_id")},
                {"alarm_notif_title", data.get("title")},
                {"alarm_notif_message", data.get("message")},
                {"repeat_by", data.get("repeat_by")},
                {"time_in_day", data.get("time_in_day")},
                {"day_in_week", data.get("day_in_week")},
                {"every_which_day", data.get("every_which_day")},
                {"alarm_notif_sound", data.get("sound")},
                {"alarm_id", alarm_id}});
    }

    /**
     * Store repeater in DB
     * @param repeaterData - hashmap of data to store
     */
    private void insertRepeaterDB(HashMap<String, String> repeaterData){
        //vstavi v tabelo novegi alarm
        DBH.insertData("repeaters", new String[][]{
                {"srv_id", repeaterData.get("srv_id")},
                {"datetime_start", repeaterData.get("datetime_start")},
                {"datetime_end", repeaterData.get("datetime_end")},
                {"repeat_by", repeaterData.get("repeat_by")},
                {"time_in_day", repeaterData.get("time_in_day")},
                {"day_in_week", repeaterData.get("day_in_week")},
                {"every_which_day", repeaterData.get("every_which_day")},
                {"datetime_user_started", repeaterData.get("datetime_user_started")},
                {"datetime_last_check", repeaterData.get("datetime_last_check")}});
    }

    /**
     * Cancel and delete all alarms for all surveys
     */
    private void cancelAndDeleteAllAlarms(){
        String[][] alarms = DBH.getData("alarms",
                new String[]{"alarm_id"}, null);

        if(alarms != null) {
            for(String[] alarm : alarms) {
                cancelAlarm(AlarmReceiver.class, Integer.parseInt(alarm[0]));
            }

            DBH.deleteAllRows("surveys");
        }
    }

    /**
     * Cancel and delete alarms for this survey
     *
     * @param srv_id - id of survey, parent of alarm(s)
     */
    public void cancelAndDeleteAlarms(String srv_id){
        String[][] alarms = DBH.getData("alarms",
                new String[]{"alarm_id"}, "srv_id='"+srv_id+"'");

        if(alarms != null) {
            for(String[] alarm : alarms) {
                cancelAlarm(AlarmReceiver.class, Integer.parseInt(alarm[0]));
            }

            deleteAlarmDB(srv_id);
        }
    }

    /**
     * Cancel and delete repeaters for this survey
     *
     * @param srv_id - id of survey, parent of alarm(s)
     */
    public void cancelAndDeleteRepeater(String srv_id){
        deleteRepeaterDB(srv_id);
        if(DBH.countData("geofences", "srv_id='"+srv_id+"'") == 0 &&
                DBH.countData("activities", "srv_id='" + srv_id + "'") == 0)
            GeneralLib.deleteSurveyDB(ctx, srv_id);
    }

    /**
     * Cancels alarm for given alarm id
     * @param receiverClass - class of receiver to cancel
     * @param alarm_id - id of alarm/sender
     */
    public void cancelAlarm(Class receiverClass, int alarm_id){
        PendingIntent sender = PendingIntent.getBroadcast(ctx, alarm_id,
                new Intent(ctx, receiverClass), PendingIntent.FLAG_NO_CREATE);

        //is alarm for that id registered? cancel if yes
        if (sender != null) {

            AlarmManager alarm1 = (AlarmManager) ctx.getSystemService(ALARM_SERVICE);
            if (alarm1 != null) {
                alarm1.cancel(sender);
            }
        }
    }

    /**
     * Delete alarm from DB
     *
     * @param srv_id - id of survey, parent of alarm(s)
     */
    private void deleteAlarmDB(String srv_id){
        DBH.deleteRows("alarms", "srv_id='"+srv_id+"'");
    }

    /**
     * Delete repeater from DB
     *
     * @param srv_id - id of survey, parent of alarm(s)
     */
    private void deleteRepeaterDB(String srv_id){
        DBH.deleteRows("repeaters", "srv_id='"+srv_id+"'");
    }

    /**
     * Refresh alarms with removing all and getting them from server
     */
    public void refreshAlarms(){
        cancelAndDeleteAllAlarms();
        new sendGetAlarmsTask(ctx).execute();
    }
}
