package enklikanketa.com.a1kapanel.Libraries;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.Tasks.sendGetEntriesTask;

public class EntryLib {
    private String TAG = "EntryLib";
    private Context ctx;
    private Database DBH;


    public EntryLib(Context base) {
        ctx = base;
        DBH = (Database) Database.getInstance(ctx);
    }

    /**
     * Set, update activity based on new data
     *
     * @param data - Data of sended entry
     */
    public void setOrUpdateNewEntry(Map<String, String> data){
        insertOrUpdateEntryDB(data);
    }

    /**
     * Insert or update entry in DB
     *
     * @param entry - Map of data of entry data to store in DB
     */
    private void insertOrUpdateEntryDB(Map<String, String> entry) {
        String ank_id = entry.get("ank_id");

        try {
            ContentValues cv = new ContentValues();

            JSONObject entry0 = new JSONObject(entry.get("entry"));
            String id_act = entry0.getString("id");
            cv.put("location_check", entry0.getString("location_check"));
            cv.put("srv_id", entry.get("ank_id"));

            int actExists = DBH.countData("entry", "id="+id_act);

            if(actExists == 0) {
                ContentValues initialValues = new ContentValues();
                initialValues.put("id", ank_id);
                initialValues.put("link", entry.get("link"));
                initialValues.put("title", entry.get("srv_title"));

                //insert new survey in DB, if already exists, id is -1
                DBH.getWritableDatabase().insertWithOnConflict("surveys", null, initialValues, SQLiteDatabase.CONFLICT_IGNORE);

                cv.put("id", id_act);
                DBH.insertData("entry", cv);
            }
            else {
                DBH.updateData("entry", cv, "id=" + id_act);

                //just in case, update survey data - pretty link or change of title
                ContentValues initialValues = new ContentValues();
                initialValues.put("link", entry.get("link"));
                initialValues.put("title", entry.get("srv_title"));
                DBH.updateData("surveys", initialValues, "id=" + ank_id);
            }

        } catch (JSONException e) {
            Log.e(TAG, "EntryLib.insertEntryDB() - Error: " + e.getMessage());
            GeneralLib.reportCrash(e, null);
        }
    }

    /**
     * Check if location check is turn on for giver entry
     * @param srv_id - id of survey for entry
     * @return true if location check is turn on for this entry, false otherwise
     */
    public boolean isLocationCheckEntry(String srv_id){
        return srv_id != null ?
                DBH.countData("entry", "id=" + srv_id + " AND location_check=1") > 0 :
                DBH.countData("entry", "location_check=1") > 0;
    }

    public void cancelEntry(String srv_id){
        DBH.deleteRows("entry", "srv_id = " + srv_id);
    }

    /**
     * Refresh tracking - removing/clearing all activities and getting them from server
     */
    public void refreshEntry() {
        new sendGetEntriesTask(ctx).execute();
    }
}
