package enklikanketa.com.a1kapanel.Tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import enklikanketa.com.a1kapanel.Adapters.QuestionnairesSimpleAdapter;
import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.Models.SurveyList.Survey;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 19/4/2018.
 */

public class sendDeleteSrvUnit extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Activity ctx;
    private ProgressDialog loader;
    private Survey survey;
    private QuestionnairesSimpleAdapter adapter, adapter_tofill, adapter_filled;
    List<Survey> surveys_tofill;
    TextView nv_tofill, nv_filled;
    RecyclerView s_tofill, s_filled;
    private int position;
    private Database DBH;

    public sendDeleteSrvUnit(Activity act, Survey survey, QuestionnairesSimpleAdapter adapter, int position,
                             List<Survey> surveys_tofill, QuestionnairesSimpleAdapter adapter_tofill, QuestionnairesSimpleAdapter adapter_filled,
                             TextView nv_tofill, TextView nv_filled, RecyclerView s_tofill, RecyclerView s_filled) {
        ctx = act;
        this.survey = survey;
        this.adapter = adapter;
        this.position = position;
        this.adapter_tofill = adapter_tofill;
        this.adapter_filled = adapter_filled;
        this.surveys_tofill = surveys_tofill;
        this.nv_tofill = nv_tofill;
        this.nv_filled = nv_filled;
        this.s_tofill = s_tofill;
        this.s_filled = s_filled;
        DBH = (Database) Database.getInstance(act);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loader = ProgressDialog.show(ctx, "",
                ctx.getResources().getString(R.string.sendingRequest), true);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);

                if (obj.has("note")) {
                    if(survey.mode.equals("entry")){
                        //remove from list
                        adapter.removeSurvey(position);
                        if(adapter_filled.getItemCount() == 0){
                            nv_filled.setVisibility(View.VISIBLE);
                            s_filled.setVisibility(View.GONE);
                        }
                        if(adapter_tofill.getItemCount() == 0){
                            nv_tofill.setVisibility(View.VISIBLE);
                            s_tofill.setVisibility(View.GONE);
                        }
                    }
                    else{
                        if(survey.status.equals("6")){
                            survey.status = "";
                            survey.user_id = "";
                            surveys_tofill.add(survey);
                            Collections.sort(surveys_tofill, new Comparator<Survey>()
                            {
                                @Override
                                public int compare(Survey lhs, Survey rhs) {
                                    return Integer.valueOf(rhs.getTimestamp()).compareTo(Integer.valueOf(lhs.getTimestamp()));
                                }
                            });
                            adapter_tofill.changeDataAll(surveys_tofill);
                            adapter.removeSurvey(position);
                            nv_tofill.setVisibility(View.GONE);
                            s_tofill.setVisibility(View.VISIBLE);
                            if(adapter_filled.getItemCount() == 0){
                                nv_filled.setVisibility(View.VISIBLE);
                                s_filled.setVisibility(View.GONE);
                            }
                        }
                        else {
                            //set view as active/unanswered
                            adapter.setActiveOn(position);
                        }
                    }
                } else {
                    GeneralLib.reportCrash(new Exception("sendDeleteSrvUnit no note in response"), result);
                }
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, result);
            }
            GeneralLib.dismissDialog(ctx, loader);
        }
        else {
            GeneralLib.dismissDialog(ctx, loader);
            Toast.makeText(ctx, ctx.getText(R.string.general_remote_server_error)
                    .toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected String doInBackground(Object... params) {
        return postDeleteSrvUserRequest();
    }


    private String postDeleteSrvUserRequest() {
        String[] user = DBH.getRowData("uporabnik",
                new String[]{"identifier", "id_server"}, null);

        JSONObject object = new JSONObject();

        if(user != null) {
            try {
                JSONObject logIn = new JSONObject();
                logIn.put("identifier", user[0]);
                logIn.put("id_server", user[1]);
                object.put("Login", logIn);

                object.put("ank_id", survey.srv_id);
                object.put("srv_unit_id", survey.user_id);
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, object.toString());
            }

            ServerCommunication SC = new ServerCommunication(ctx);
            return SC.PostDeleteSurveyUnit(object);
        }
        return null;
    }
}