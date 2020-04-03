package enklikanketa.com.a1kapanel.Tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import enklikanketa.com.a1kapanel.Adapters.QuestionnairesSimpleAdapter;
import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Models.SurveyList.Survey;
import enklikanketa.com.a1kapanel.Models.SurveyList.SurveyHolder;
import enklikanketa.com.a1kapanel.Models.SurveyList.SwipeToDeleteCallback;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;
import enklikanketa.com.a1kapanel.vpis;

public class getSurveyList extends AsyncTask<Object, Object, String> {

    Activity ctx;
    private TextView text_no_questionaires_tofill, text_no_questionaires_filled;
    private ProgressDialog loader;
    RecyclerView list_view_tofill, list_view_filled;
    private String srv_id;
    private ArrayList<Survey> surveys_tofill = new ArrayList<>();
    private ArrayList<Survey> surveys_filled = new ArrayList<>();
    private QuestionnairesSimpleAdapter adapter_tofill, adapter_filled;
    private String TAG = "getSurveyList";

    public getSurveyList(Activity context, TextView text_no_questionaires_tofill,
                         RecyclerView list_view_tofill, QuestionnairesSimpleAdapter adapter_tofill, TextView text_no_questionaires_filled,
                         RecyclerView list_view_filled, QuestionnairesSimpleAdapter adapter_filled, String srv_id) {

        ctx = context;
        this.text_no_questionaires_tofill = text_no_questionaires_tofill;
        this.text_no_questionaires_filled = text_no_questionaires_filled;
        this.list_view_tofill = list_view_tofill;
        this.list_view_filled = list_view_filled;
        this.srv_id = srv_id;
        this.adapter_tofill = adapter_tofill;
        this.adapter_filled = adapter_filled;
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
            if (!surveys_tofill.isEmpty() && text_no_questionaires_tofill != null) {
                text_no_questionaires_tofill.setVisibility(View.GONE);
                list_view_tofill.setVisibility(View.VISIBLE);
                attachAdapter(surveys_tofill, adapter_tofill, list_view_tofill);
            }
            else if(text_no_questionaires_tofill != null) {
                text_no_questionaires_tofill.setVisibility(View.VISIBLE);
                list_view_tofill.setVisibility(View.GONE);
            }
            if (!surveys_filled.isEmpty() && text_no_questionaires_filled != null) {
                text_no_questionaires_filled.setVisibility(View.GONE);
                list_view_filled.setVisibility(View.VISIBLE);
                attachAdapter(surveys_filled, adapter_filled, list_view_filled);
            }
            else if(text_no_questionaires_filled != null) {
                text_no_questionaires_filled.setVisibility(View.VISIBLE);
                list_view_filled.setVisibility(View.GONE);
            }
        } else {
            GeneralLib.dismissDialog(ctx, loader);
            GeneralLib.reportCrash(new Exception("getSurveyList onPostExecute() has error: "
                    + result), null);
        }

        GeneralLib.dismissDialog(ctx, loader);
    }

    protected String doInBackground(Object... params) {
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
                object.put("srv_id", srv_id);

                ServerCommunication SC = new ServerCommunication(ctx);
                result = SC.PostGetSurveyList(object);

                if (result != null) {
                    JSONArray surveys_arr = new JSONArray(result);

                    if (surveys_arr.length() > 0) {
                        for (int i = 0; i < surveys_arr.length(); i++) {
                            JSONObject raziskava = surveys_arr.getJSONObject(i);
                            //String ime = GeneralLib.fromHtml(raziskava.getString("naslov")).toString();
                            String srv_id = raziskava.getString("srv_id");

                            JSONArray vprasalniki_arr = raziskava.getJSONArray("surveys");
                            for (int j = 0; j < vprasalniki_arr.length(); j++) {
                                JSONObject vprasalnik = vprasalniki_arr.getJSONObject(j);
                                String mode = vprasalnik.has("mode") ? vprasalnik.getString("mode") : "";
                                String top;
                                String bottom;
                                DateFormat sdf = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);
                                String datetime = sdf.format(new Date(vprasalnik.getLong("timestamp")*1000));
                                switch(mode){
                                    case "entry":
                                        top = ctx.getString(R.string.litem_entry);
                                        bottom = datetime;
                                        break;
                                    case "geofence":
                                        top = ctx.getString(R.string.litem_geofence);
                                        bottom = vprasalnik.has("name") && !vprasalnik.getString("name").equals("null") ?
                                                vprasalnik.getString("name")+": " :
                                                (vprasalnik.has("address") ? vprasalnik.getString("address")+": " : "");
                                        bottom += datetime;
                                        break;
                                    case "repeater":
                                        top = ctx.getString(R.string.litem_time);
                                        bottom = datetime;
                                        break;
                                    default:
                                        top = ctx.getString(R.string.litem_questionnaire);
                                        bottom = "";
                                        break;
                                }

                                Survey survey = new Survey();
                                survey.setListItemTop(top);
                                survey.setListItemBottom(bottom);
                                String status = vprasalnik.getString("status");
                                survey.setStatus(status);
                                survey.setVerDatetime(vprasalnik.getString("datetime"));
                                survey.setTGeoId(vprasalnik.has("tgeof_id") ? vprasalnik.getString("tgeof_id") : null);
                                survey.setTActId(vprasalnik.has("tact_id") ? vprasalnik.getString("tact_id") : null);
                                survey.setMode(mode);
                                survey.setLatitude(vprasalnik.has("latitude") && !vprasalnik.getString("latitude").equals("null") ?
                                        vprasalnik.getString("latitude") : null);
                                survey.setLongitude(vprasalnik.has("longitude") && !vprasalnik.getString("longitude").equals("null") ?
                                        vprasalnik.getString("longitude") : null);
                                survey.setSrvVersion(vprasalnik.getString("srv_version"));
                                survey.setLink(vprasalnik.get("link").toString());
                                survey.setSrvId(srv_id);
                                survey.setUserId(vprasalnik.getString("srv_user_id"));
                                survey.setTimestamp(vprasalnik.getString("timestamp"));
                                if(!status.equals("6"))
                                    surveys_tofill.add(survey);
                                else
                                    surveys_filled.add(survey);
                            }
                        }

                        /*Collections.sort(surveys, new Comparator<Survey>()
                        {
                            @Override
                            public int compare(Survey lhs, Survey rhs) {
                                return Integer.valueOf(lhs.getSrvId()).compareTo(Integer.valueOf(rhs.getSrvId()));
                            }
                        });*/

                        Collections.sort(surveys_tofill, new Comparator<Survey>()
                        {
                            @Override
                            public int compare(Survey lhs, Survey rhs) {
                                return Integer.valueOf(rhs.getTimestamp()).compareTo(Integer.valueOf(lhs.getTimestamp()));
                            }
                        });
                        Collections.sort(surveys_filled, new Comparator<Survey>()
                        {
                            @Override
                            public int compare(Survey lhs, Survey rhs) {
                                return Integer.valueOf(rhs.getTimestamp()).compareTo(Integer.valueOf(lhs.getTimestamp()));
                            }
                        });


                        return "OK";
                    } else
                        return "OK";

                }
                else {
                    return null;
                }
            } catch (JSONException e) {
                Log.e(TAG, "getSurveyList onPostExecute(): " + e);
                GeneralLib.reportCrash(e, result);
                GeneralLib.dismissDialog(ctx, loader);
                return result;
            }
        }
        // not loged in - go to login page
        else {
            Intent myIntent = new Intent(ctx, vpis.class);
            ctx.startActivity(myIntent);
            ctx.finish();
            return null;
        }
    }

    private void attachAdapter(ArrayList<Survey> surveys, QuestionnairesSimpleAdapter adapter, RecyclerView list_view){
        if(adapter == null){
            adapter = new QuestionnairesSimpleAdapter(
                    ctx, R.layout.list_item_vprasalniki, surveys);
            list_view.setAdapter(adapter);
        }
        else
            adapter.changeDataAll(surveys);

        final QuestionnairesSimpleAdapter endadapter = adapter;

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(new SwipeToDeleteCallback(ctx) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                final int position = viewHolder.getAdapterPosition();
                Survey survey = ((SurveyHolder)viewHolder).getSurvey();
                showDeleteUnitAlert(survey, position, endadapter);
            }
        });
        itemTouchhelper.attachToRecyclerView(list_view);
    }

    /**
     * Show alert dialog to delete answer
     * @param survey - survey from Survey class
     * @param position - position of item in adapter/recyclerview
     */
    private void showDeleteUnitAlert(final Survey survey, final int position, final QuestionnairesSimpleAdapter adapter){
        final String mode = survey.getMode();
        String zgoraj = survey.getZgoraj();
        String spodaj = survey.getSpodaj();

        String title, desc;
        if(mode.equals("entry")){
            title = ctx.getString(R.string.unit_delete_alert_title_entry);
            desc = ctx.getString(R.string.unit_delete_alert_desc_entry);
        }
        else {
            title = ctx.getString(R.string.unit_delete_alert_title);
            desc = ctx.getString(R.string.unit_delete_alert_desc);
        }

        final AlertDialog myDialog = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(desc+zgoraj+", "+spodaj)
                .setNegativeButton(R.string.location_delete_alert_no, null)
                .setPositiveButton(R.string.location_delete_alert_yes, null)
                .setCancelable(false)
                .create();

        myDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btne = myDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                btne.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        adapter.notifyItemChanged(position);
                        myDialog.dismiss();
                    }
                });

                Button btpo = myDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btpo.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        myDialog.dismiss();
                        new sendDeleteSrvUnit(ctx, survey, adapter, position, surveys_tofill, adapter_tofill, adapter_filled,
                                text_no_questionaires_tofill, text_no_questionaires_filled, list_view_tofill, list_view_filled).execute();
                    }
                });
            }
        });
        myDialog.show();
    }
}