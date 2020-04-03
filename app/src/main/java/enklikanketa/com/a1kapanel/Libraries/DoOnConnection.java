package enklikanketa.com.a1kapanel.Libraries;

import android.content.Context;

import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.Tasks.sendGetMyLocationsTask;
import enklikanketa.com.a1kapanel.Tasks.sendRegistrationIDTask;
import enklikanketa.com.a1kapanel.Tasks.sendTrakingLocationsTask;
import enklikanketa.com.a1kapanel.Tasks.sendTrakingLogTask;

/**
 * Created by podkrizniku on 18/12/2017.
 */

public class DoOnConnection {
    private Context ctx;

    public DoOnConnection(Context base) {
        ctx = base;
        checkThingsToSend();
    }

    private void checkThingsToSend(){
        Database DB = (Database) Database.getInstance(ctx);
        //does data of tracking log exists
        int stej = DB.countData("things_to_send", "name = 'tracking_log'");
        if(stej > 0) {
            new sendTrakingLogTask(ctx, null).execute();
        }

        //did FCM registrationID changed
        String[] reg_id = DB.getRowData("things_to_send",
                new String[]{"value"}, "name = 'registration_id'");
        if(reg_id != null) {
            new sendRegistrationIDTask(ctx, reg_id[0]).execute();
        }

        //does data of refresh alarms exists
        stej = DB.countData("things_to_send", "name = 'get_alarms'");
        if(stej > 0) {
            AlarmLib alib = new AlarmLib(ctx);
            alib.refreshAlarms();
        }

        //does data of refresh geofences exists
        stej = DB.countData("things_to_send", "name = 'get_geofences'");
        if(stej > 0) {
            GeofencingLib glib = new GeofencingLib(ctx);
            glib.refreshGeofences();
        }

        //does data of refresh tracking exists
        stej = DB.countData("things_to_send", "name = 'get_tracking'");
        if(stej > 0) {
            TrackingLib tlib = new TrackingLib(ctx);
            tlib.refreshTracking();
        }

        //do we have to download all users locations and ars
        stej = DB.countData("things_to_send", "name = 'get_my_locations'");
        if(stej > 0) {
            new sendGetMyLocationsTask(ctx).execute();
        }

        //does data of locations exists
        stej = DB.countData("locations", null);
        if(stej > 0) {
            new sendTrakingLocationsTask(ctx).execute();
        }

        //does data of refresh entry exists
        stej = DB.countData("things_to_send", "name = 'get_entry'");
        if(stej > 0) {
            EntryLib elib = new EntryLib(ctx);
            elib.refreshEntry();
        }
    }
}
