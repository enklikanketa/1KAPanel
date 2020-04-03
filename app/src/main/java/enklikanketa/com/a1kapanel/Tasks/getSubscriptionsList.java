package enklikanketa.com.a1kapanel.Tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import enklikanketa.com.a1kapanel.Adapters.StudiesSimpleAdapter;
import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Models.StudyList.Study;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;
import enklikanketa.com.a1kapanel.vpis;

public class getSubscriptionsList extends AsyncTask<Object, Object, String> {

    Activity ctx;
    private TextView text_no_questionaires;
    ProgressDialog loader;
    RecyclerView list_view;
    List<Study> studies = new ArrayList<>();
    private StudiesSimpleAdapter adapter;
    private String TAG = "getSubscriptionsList";

    public getSubscriptionsList(Activity context, TextView text_no_questionaires1,
                                RecyclerView list_view1, StudiesSimpleAdapter adapter) {

        ctx = context;
        text_no_questionaires = text_no_questionaires1;
        list_view = list_view1;
        this.adapter = adapter;
    }

    protected void onPreExecute() {
        super.onPreExecute();
        loader = ProgressDialog.show(ctx, "",
                ctx.getResources().getString(R.string.gathering_data_progress), true);
    }

    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (result == null) {
            GeneralLib.dismissDialog(ctx, loader);
            Toast.makeText(ctx, ctx.getText(R.string.general_remote_server_error)
                    .toString(), Toast.LENGTH_LONG).show();
        } else if (result.equals("OK")) {
            attachAdapter();
            if (text_no_questionaires != null)
                text_no_questionaires.setVisibility(View.GONE);
            list_view.setVisibility(View.VISIBLE);
        } else if (result.equals("empty") || result.equals("[]")) {
            if (text_no_questionaires != null)
                text_no_questionaires.setVisibility(View.VISIBLE);
            list_view.setVisibility(View.GONE);
        } else {
            GeneralLib.dismissDialog(ctx, loader);
            GeneralLib.reportCrash(new Exception("getSubscriptionsList onPostExecute() has error: "
                    + result), null);
        }

        GeneralLib.dismissDialog(ctx, loader);
    }

    protected String doInBackground(Object... params) {
        //get user data from DB
        Database DB = (Database) Database.getInstance(ctx);
        String[] user = DB.getRowData("uporabnik",
                new String[]{"identifier", "id_server"}, null);

        if (user != null) {
            JSONObject object = new JSONObject();
            JSONObject logIn = new JSONObject();
            String result = null;
            try {
                logIn.put("identifier", user[0]);
                logIn.put("id_server", user[1]);
                object.put("Login", logIn);
                object.put("timeZone", Calendar.getInstance().getTimeZone().getID());

                ServerCommunication SC = new ServerCommunication(ctx);
                result = SC.PostGetSubscriptionsList(object);

                if (result != null) {
                    JSONArray vprasalniki = new JSONArray(result);

                    if (vprasalniki.length() > 0) {
                        for (int i = 0; i < vprasalniki.length(); i++) {
                            JSONObject vprasalnik = new JSONObject(vprasalniki.getJSONObject(i).toString());
                            String ime = GeneralLib.fromHtml(vprasalnik.getString("naslov")).toString();
                            Study study = new Study();
                            study.setListItemTop(ime);
                            study.setTitle(ime);
                            study.setStart(vprasalnik.getString("starts"));
                            study.setEnd(vprasalnik.getString("expire"));
                            study.setLink(vprasalnik.getString("url"));
                            study.setSrvId(vprasalnik.getString("srv_id"));
                            study.setDescription(vprasalnik.getString("srv_description"));
                            DateFormat sdf = DateFormat.getDateInstance(DateFormat.DEFAULT);
                            String datetime = sdf.format(new Date(vprasalnik.getLong("unixstart")*1000));
                            study.setUnixstart(datetime);
                            study.setListItemBottom(datetime);
                            study.setUnfinishedCnt(vprasalnik.getString("unfinished_cnt"));
                            if((!vprasalnik.isNull("geofences") && vprasalnik.getInt("geofences")>0) ||
                                    (!vprasalnik.isNull("activities") && vprasalnik.getInt("activities")>0) ||
                                    ((!vprasalnik.isNull("entry_on") && vprasalnik.getInt("entry_on")>0) &&
                                            (!vprasalnik.isNull("location_check") && vprasalnik.getInt("location_check")>0)) ||
                                    (!vprasalnik.isNull("tracking_on") && vprasalnik.getInt("tracking_on")>0)){
                                study.setLocationPermission(true);
                            }
                            if((!vprasalnik.isNull("activities") && vprasalnik.getInt("activities")>0) ||
                                    ((!vprasalnik.isNull("tracking_on") && vprasalnik.getInt("tracking_on")>0) &&
                                            (!vprasalnik.isNull("activity_recognition") && vprasalnik.getInt("activity_recognition")>0))){
                                study.setARPermission(true);
                            }
                            studies.add(study);
                        }

                        /*Collections.sort(studies, new Comparator<Study>()
                        {
                            @Override
                            public int compare(Study lhs, Study rhs) {
                                return Integer.valueOf(rhs.getTimestamp()).compareTo(Integer.valueOf(lhs.getTimestamp()));
                            }
                        });*/

                        return "OK";
                    } else
                        return "empty";

                }
                else {
                    return null;
                }
            } catch (JSONException e) {
                Log.e(TAG, "getSubscriptionsList onPostExecute(): " + e);
                GeneralLib.reportCrash(e, result);
                GeneralLib.dismissDialog(ctx, loader);
                return result;
            }
        }
        else {
            Intent myIntent = new Intent(ctx, vpis.class);
            ctx.startActivity(myIntent);
            ctx.finish();
            return null;
        }
    }

    private void attachAdapter(){
        if(adapter == null){
            adapter = new StudiesSimpleAdapter(
                    ctx, R.layout.list_item_subscriptions, studies);
            list_view.setAdapter(adapter);
        }
        else
            adapter.changeDataAll(studies);
    }
}