package enklikanketa.com.a1kapanel.Tasks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import enklikanketa.com.a1kapanel.Home;
import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.SubscriptionInfo;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.ServerCommunication;

/**
 * Created by podkrizniku on 19/4/2018.
 */

public class sendUnsubscribeSurvey extends AsyncTask<Object, Object, String> {

    //application context cannot be leaked
    private Activity ctx;
    private ProgressDialog loader;
    private String srv_id;
    Database DBH;

    public sendUnsubscribeSurvey(Activity act, String srv_id1) {
        ctx = act;
        srv_id = srv_id1;
        DBH = (Database) Database.getInstance(act);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        loader = ProgressDialog.show(ctx, "",
                ctx.getResources().getString(R.string.gathering_data_progress), true);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        GeneralLib.dismissDialog(ctx, loader);

        if (result != null) {
            try {
                JSONObject obj = new JSONObject(result);

                if (obj.has("note")) {
                    showUnsubscribed();
                    DBH.deleteRows("surveys", "id="+srv_id);
                } else {
                    GeneralLib.reportCrash(new Exception("sendUnsubscribeSurvey no note in response"), result);
                }

            } catch (JSONException e) {
                GeneralLib.reportCrash(e, result);
            }
        }
        else {
            Toast.makeText(ctx, ctx.getText(R.string.general_remote_server_error)
                    .toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected String doInBackground(Object... params) {
        return postUnsubscribeRequest();
    }


    private String postUnsubscribeRequest() {
        String[] user = DBH.getRowData("uporabnik",
                new String[]{"identifier", "id_server"}, null);

        JSONObject object = new JSONObject();

        if(user != null) {
            try {
                JSONObject logIn = new JSONObject();
                logIn.put("identifier", user[0]);
                logIn.put("id_server", user[1]);
                object.put("Login", logIn);

                object.put("ank_id", srv_id);
            } catch (JSONException e) {
                GeneralLib.reportCrash(e, object.toString());
            }

            ServerCommunication SC = new ServerCommunication(ctx);
            return SC.PostUnsubscribeSurvey(object);
        }
        return null;
    }

    private void showUnsubscribed(){
        final AlertDialog myDialog = new AlertDialog.Builder(ctx)
                .setTitle(R.string.dialog_unsubscribed_title)
                .setMessage(ctx.getString(R.string.dialog_unsubscribed_message))
                .setNegativeButton(R.string.dialog_unsubscribed_ok, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(final DialogInterface dialog) {
                        if(ctx instanceof SubscriptionInfo)
                            ctx.finish();
                        else if(ctx instanceof Home) {
                            Intent myIntent = new Intent(ctx, Home.class);
                            ctx.startActivity(myIntent);
                        }
                    }
                }).create();

        myDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button btne = myDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                btne.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        myDialog.dismiss();
                    }
                });
            }
        });
        myDialog.show();
    }
}