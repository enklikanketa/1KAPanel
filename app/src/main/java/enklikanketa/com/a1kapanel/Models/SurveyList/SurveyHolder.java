package enklikanketa.com.a1kapanel.Models.SurveyList;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.PickLocation;
import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.Network;
import enklikanketa.com.a1kapanel.WebResevanje;

public class SurveyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private String TAG = "SurveyHolder";

    private final View aktiv;
    private final TextView litem_zgoraj;
    private final TextView litem_spodaj;

    private Survey survey;
    private Activity ctx;

    public SurveyHolder(Activity context, View itemView) {
        super(itemView);

        // 1. Set the context
        this.ctx = context;

        // 2. Inflate the UI widgets of the holder
        this.aktiv = itemView.findViewById(R.id.vprAktiv);
        this.litem_zgoraj = itemView.findViewById(R.id.litem_zgoraj);
        this.litem_spodaj = itemView.findViewById(R.id.litem_spodaj);

        // 3. Set the "onClick" listener of the holder
        itemView.setOnClickListener(this);
    }

    public void bindSurvey(Survey survey) {

        // 4. Bind the data to the ViewHolder
        this.survey = survey;

        //empty
        if (survey.status.equals("")) {
            aktiv.setBackgroundResource(R.drawable.ic_action_survey_empty);
        }
        //filled out
        else if (survey.status.equals("6")) {
            aktiv.setBackgroundResource(R.drawable.ic_action_survey_done);
        }
        //partly filled out
        else {
            aktiv.setBackgroundResource(R.drawable.ic_action_survey_partly);
        }

        this.litem_zgoraj.setText(survey.litem_zgoraj);
        this.litem_spodaj.setText(survey.litem_spodaj);
    }

    @Override
    public void onClick(View v) {
        // Below line is just like a safety check, because sometimes holder could be null,
        // in that case, getAdapterPosition() will return RecyclerView.NO_POSITION
        if (getAdapterPosition() == RecyclerView.NO_POSITION) return;

        // 5. Handle the onClick event for the ViewHolder
        if (this.survey != null) {
            Database DB = (Database) Database.getInstance(ctx);

            long version_timestamp = 0;
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                version_timestamp = !survey.version_datetime.equals("null") ? sdf.parse(survey.version_datetime).getTime() : 0;

                if(!survey.srv_version.equals("")){
                    String[] repeaterrow = DB.getRowData("repeaters",
                            new String[]{"datetime_last_check"},
                            "srv_id=" + survey.srv_id);
                    if(repeaterrow != null) {
                        long saved_repeater_timestamp = (repeaterrow[0] != null && !repeaterrow[0].equals("null")) ? sdf.parse(repeaterrow[0]).getTime() : 0;

                        //save last_check_timestamp
                        if (!survey.version_datetime.equals("null")
                                && !survey.version_datetime.equals("null") && ((repeaterrow[0] != null && repeaterrow[0].equals("null")) ||
                                version_timestamp > saved_repeater_timestamp)) {
                            ContentValues cv = new ContentValues();
                            cv.put("datetime_last_check", survey.version_datetime);
                            DB.updateData("repeaters", cv, "srv_id='" + survey.srv_id + "'");
                        }
                    }
                }
            }catch (ParseException e){
                Log.e(TAG, "SurveyHolder.onClick() - Error: " + e.getMessage());
                GeneralLib.reportCrash(e, null);
            }

            Intent myIntent;
            if(!survey.mode.equals("entry")) {
                myIntent = new Intent(ctx, WebResevanje.class);
                myIntent.putExtra("tgeof_id", survey.tgeof_id);
                myIntent.putExtra("tact_id", survey.tact_id);
                myIntent.putExtra("mode", survey.mode);
                myIntent.putExtra("srv_version_timestamp", version_timestamp/1000+"");
            }
            else{
                myIntent = new Intent(ctx, PickLocation.class);
                myIntent.putExtra("latitude", survey.latitude);
                myIntent.putExtra("longitude", survey.longitude);
            }

            myIntent.putExtra("link", survey.link);

            if (Network.checkMobileInternet(ctx, true)) {
                ctx.startActivity(myIntent);
            }
        }
    }

    public Survey getSurvey(){
        return survey;
    }
}
