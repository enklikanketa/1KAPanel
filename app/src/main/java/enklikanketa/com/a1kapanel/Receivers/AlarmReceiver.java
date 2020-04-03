package enklikanketa.com.a1kapanel.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import enklikanketa.com.a1kapanel.Libraries.AlarmLib;
import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Libraries.NotificationLib;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.System.Database;

/**
 * Created by podkrizniku on 27/11/2017.
 */

public class AlarmReceiver extends BroadcastReceiver {
    private String TAG = "AlarmReceiver";
    private Context ctx;
    AlarmLib alib;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;
        alib = new AlarmLib(ctx);

        int howManyNew = 0;

        Bundle bundle = intent.getExtras();

        if (bundle != null) {
            Database DB = (Database) Database.getInstance(ctx);
            String srv_id = bundle.getString("srv_id");
            //get data of repeater
            HashMap<String, String> repeater = DB.getRowHashMapData("repeaters",
                    new String[]{"repeat_by", "time_in_day", "day_in_week", "every_which_day",
                            "datetime_start", "datetime_end", "datetime_last_check"},
                    "srv_id=" + srv_id);

            if (repeater != null) {
                String datetime_end = repeater.get("datetime_end");
                if(datetime_end != null && !datetime_end.equals("")) {
                    //set timestamp of now in seconds
                    long timestamp_now = System.currentTimeMillis() / 1000;

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        long timestamp_end = sdf.parse(datetime_end).getTime() / 1000;

                        //repeaters already ended by setted end date
                        if(timestamp_end < timestamp_now) {
                            //set howManyNew to 0 to not show notification
                            howManyNew = 0;
                            //cancel all future alarms
                            alib.cancelAndDeleteAlarms(srv_id);
                            alib.cancelAndDeleteRepeater(srv_id);
                        }
                        else
                            //count how many new
                            howManyNew = countSurveysFromLastCheck(repeater);
                    } catch (ParseException e){
                        Log.e(TAG, "AlarmReceiver.onReceive() timestamp_end - Error: " + e.getMessage());
                        GeneralLib.reportCrash(e, null);
                        //count how many new
                        howManyNew = countSurveysFromLastCheck(repeater);
                    }
                }
            }

            if (howManyNew > 0) {
                HashMap<String, String> data = new HashMap<>();
                data.put("link", "survey_list");
                data.put("title", ctx.getString(R.string.default_alarm_notification_title, howManyNew));
                data.put("message", bundle.getString("message"));
                data.put("sound", bundle.getString("sound"));
                data.put("sender_id", bundle.getString("sender_id"));

                //show notification
                NotificationLib nlib = new NotificationLib(ctx);
                nlib.showNotificationSurvey(data);
            }
        }
    }

    /**
     * Create and run alarm fo this alarm id/broadcast request code
     * If alarm for this id already exists, this will update it, if not it will create it
     *
     * @param data data of alarm
     */
    private int countSurveysFromLastCheck(HashMap<String, String> data) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long timestamp_last_check = 0, datetime_start = 0, oneDaySeconds = 86400;
        //counter of new surveys
        int howManyNew = 0;
        try {
            //get date when did user get access to this survey - if no data, set to repeater start
            String datetime_start_s = data.get("datetime_start");
            String datetime_user_started_s = data.get("datetime_user_started") != null ?
                    data.get("datetime_user_started") : data.get("datetime_start");

            //if data of users last check for this survey exist, set it. Otherwise set last check as start/access to survey
            if (data.get("datetime_last_check") != null && !data.get("datetime_last_check").equals("null"))
                timestamp_last_check = sdf.parse(data.get("datetime_last_check")).getTime() / 1000;
            else
                timestamp_last_check = getMidnightOfTimestampDay(sdf.parse(datetime_user_started_s).getTime() / 1000);

            //from when does user have access to this survey
            datetime_start = sdf.parse(datetime_start_s).getTime() / 1000;
        } catch (ParseException e) {
            Log.e(TAG, "AlarmReceiver.countSurveysFromLastCheck() timestamp_last_check - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }

        //set timestamp of now in seconds
        long timestamp_now = System.currentTimeMillis() / 1000;

        //set arraylist list of times in day
        ArrayList<String> time_in_day_arr = jsonStringToArray(data.get("time_in_day"));
        //timestamp to work with in calculations
        long timestamp_temp;
        //intervals of gasps between times in day
        ArrayList<Long> interval_arr;

        switch (data.get("repeat_by")) {
            case "everyday":
                //set timestamp to start of last check day
                timestamp_temp = getMidnightOfTimestampDay(timestamp_last_check);
                interval_arr = timeInDayToMillisIntervals(time_in_day_arr);

                //loop from start timestamp to now
                while (timestamp_temp < timestamp_now) {
                    //iterate trough intervals
                    for (long interval : interval_arr) {
                        //add interval to temporary timestamp
                        timestamp_temp += interval / 1000;

                        //if temporary timestamp is in countable range, add new survey version
                        if (timestamp_temp > timestamp_last_check && timestamp_temp <= timestamp_now)
                            howManyNew++;
                    }
                }
                break;
            case "daily":
                //set timestamp to start of survey (to get when count must start of every_which_day)
                timestamp_temp = getMidnightOfTimestampDay(datetime_start);
                //get every which day data
                int every_which_day = Integer.valueOf(data.get("every_which_day"));
                //get gasp between last check and start of counting surveys
                long subtract = timestamp_last_check - timestamp_temp;
                long addToStart = 0;
                if (subtract > 0) {
                    //how many every_which_days are between start of survey and last check
                    long shortcut = subtract / (every_which_day * oneDaySeconds);
                    //how many millis to add to start of survey, so we can start counting
                    addToStart = shortcut * every_which_day * oneDaySeconds;
                }

                //set daily counter timestamp to start calculate from
                timestamp_temp += addToStart;
                //set time_in_day intervals of gasps starting from midnight to last
                interval_arr = timeInDayToMillisIntervalsFromMidnight(time_in_day_arr);

                //loop until now timestamp
                while (timestamp_temp < timestamp_now) {
                    //set temporary timestamp to calculate in day intervals
                    long timestamp_temp1 = timestamp_temp;
                    //iterate trough intervals
                    for (long interval : interval_arr) {
                        //add interval to temporary timestamp
                        timestamp_temp1 += interval / 1000;
                        //if temporary timestamp is in countable range, add new survey version
                        if (timestamp_temp1 > timestamp_last_check && timestamp_temp1 <= timestamp_now)
                            howManyNew++;
                    }
                    //add to daily counter timestamp next every_which_days
                    timestamp_temp += every_which_day * oneDaySeconds;
                }
                break;
            case "weekly":
                //set timestamp to start of last check day
                timestamp_temp = getMidnightOfTimestampDay(timestamp_last_check);
                //set time_in_day intervals of gasps starting from midnight to last
                interval_arr = timeInDayToMillisIntervalsFromMidnight(time_in_day_arr);
                //set arrayList of days in week - which days are new surveys
                ArrayList<Integer> day_in_week_arr = jsonStringToIntegerArray(data.get("day_in_week"));

                //loop until now timestamp
                while (timestamp_temp < timestamp_now) {
                    //set temporary calendar to calculate with
                    Calendar cal_temp = Calendar.getInstance();
                    //set calendar to start of last check day
                    cal_temp.setTimeInMillis(timestamp_temp * 1000);
                    //which day in week is today
                    int dayInWeek = cal_temp.get(Calendar.DAY_OF_WEEK);
                    //convert start of week from sunday to monday
                    dayInWeek = (dayInWeek != 1) ? dayInWeek - 1 : 7;

                    //if this day of week is in arraylist of days_in_week, get trough, ignore otherwise
                    if (day_in_week_arr.contains(dayInWeek)) {
                        //set temporary timestamp to calculate time_in_days of this day
                        long timestamp_temp1 = timestamp_temp;
                        //iterate trough intervals
                        for (long interval : interval_arr) {
                            //add interval to temporary timestamp
                            timestamp_temp1 += interval / 1000;
                            //if temporary timestamp is in countable range, add new survey version
                            if (timestamp_temp1 > timestamp_last_check && timestamp_temp1 <= timestamp_now)
                                howManyNew++;
                        }
                    }
                    //add to daily counter timestamp one day
                    timestamp_temp += oneDaySeconds;
                }
                break;
        }
        return howManyNew;
    }

    /**
     * Get timestamp of midnight of a given timestamp's day (timestamp_day)
     *
     * @param timestamp_day - timestamp of a day in seconds to get midnight of
     * @return timestamp of midnight of given day in seconds
     */
    private long getMidnightOfTimestampDay(long timestamp_day) {
        Calendar midnightLastCheck = Calendar.getInstance();
        midnightLastCheck.setTimeInMillis(timestamp_day * 1000);
        // reset hour, minutes, seconds and millis
        midnightLastCheck.set(Calendar.HOUR_OF_DAY, 0);
        midnightLastCheck.set(Calendar.MINUTE, 0);
        midnightLastCheck.set(Calendar.SECOND, 0);
        midnightLastCheck.set(Calendar.MILLISECOND, 0);
        return midnightLastCheck.getTimeInMillis() / 1000;
    }

    /**
     * Calculate intervals of gasp in milliseconds between time_in_day array starting at midnight
     *
     * @param time_in_day_arr - string array of time_in_day HHmm
     * @return - Long array of gasp intervals between time_in_day, first is between midnight and first
     * of today, last is between one before last and last of today
     */
    private ArrayList<Long> timeInDayToMillisIntervalsFromMidnight(ArrayList<String> time_in_day_arr) {
        //add midnight to starting point - if midnight is already starting point, it is ok, first interval will be 0ms
        time_in_day_arr.add(0, "0000");
        ArrayList<Long> interval_arr = new ArrayList<>();
        for (int i = 0; i < time_in_day_arr.size() - 1; i++) {
            interval_arr.add(alib.millisToTimeInDay(time_in_day_arr.get(i), time_in_day_arr.get(i + 1)));
        }
        return interval_arr;
    }

    /**
     * Calculate intervals of gasp in milliseconds between time_in_day array
     *
     * @param time_in_day_arr - string array of time_in_day HHmm
     * @return - Long array of gasp intervals between time_in_day, first is between first end second
     * of today, last is between last of today and first of tomorrow
     */
    private ArrayList<Long> timeInDayToMillisIntervals(ArrayList<String> time_in_day_arr) {
        ArrayList<Long> interval_arr = new ArrayList<>();
        for (int i = 0; i < time_in_day_arr.size(); i++) {
            if (i != time_in_day_arr.size() - 1) {
                interval_arr.add(alib.millisToTimeInDay(time_in_day_arr.get(i), time_in_day_arr.get(i + 1)));
            } else {
                interval_arr.add(alib.millisToTimeInDay(time_in_day_arr.get(i), time_in_day_arr.get(0)));
            }
        }
        return interval_arr;
    }

    /**
     * Calculate intervals of gasp in days between day_in_week array
     * @param day_in_week_arr - string array of day_in_week ["1","5","7"] as monday, friday, sunday
     * @return - Integer array of gasp in days intervals between day_in_week, first is between first end second
     *      of today, last is between last of today and first of tomorrow
     */
    /*private ArrayList<Integer> dayInWeekIntervals(ArrayList<String> day_in_week_arr){
        ArrayList<Integer> interval_arr = new ArrayList<>();
        for (int i = 0; i < day_in_week_arr.size(); i++) {
            if(i != day_in_week_arr.size()-1) {
                interval_arr.add(Integer.valueOf(day_in_week_arr.get(i + 1)) -
                        Integer.valueOf(day_in_week_arr.get(i)));
            }
            else {
                interval_arr.add(Integer.valueOf(day_in_week_arr.get(0)) -
                        Integer.valueOf(day_in_week_arr.get(i)) + 7);
            }
        }
        return interval_arr;
    }*/

    /**
     * Converts String in shape of JSON to ArrayList and sort it ascending
     *
     * @param jsonString - json string to convert
     * @return - ArrayList - converted json string
     */
    ArrayList<String> jsonStringToArray(String jsonString) {
        ArrayList<String> stringArray = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                stringArray.add(jsonArray.getString(i));
            }

            Collections.sort(stringArray);
        } catch (Exception e) {
            Log.e(TAG, "AlarmReceiver.jsonStringToArray Exception: " + e);
            GeneralLib.reportCrash(e, null);
        }
        return stringArray;
    }

    /**
     * Converts String in shape of JSON to ArrayList and sort it ascending
     *
     * @param jsonString - json string to convert
     * @return - ArrayList - converted json string
     */
    ArrayList<Integer> jsonStringToIntegerArray(String jsonString) {
        ArrayList<Integer> stringArray = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                stringArray.add(Integer.parseInt(jsonArray.getString(i)));
            }

            Collections.sort(stringArray);
        } catch (Exception e) {
            Log.e(TAG, "AlarmReceiver.jsonStringToArray Exception: " + e);
            GeneralLib.reportCrash(e, null);
        }
        return stringArray;
    }

    /**
     * Check if now is in range in from - to hour (ex. 800 is 8:00; 840 is 8:40)
     *
     * @param from - start time hour*100 + minutes
     * @param to   - end time hour*100 + minutes
     * @return true if now is in range, false otherwise
     */
    private boolean isInHourRange(int from, int to) {
        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int t = c.get(Calendar.HOUR_OF_DAY) * 100 + c.get(Calendar.MINUTE);
        return to > from && t >= from && t <= to || to < from && (t >= from || t <= to);
    }
}
