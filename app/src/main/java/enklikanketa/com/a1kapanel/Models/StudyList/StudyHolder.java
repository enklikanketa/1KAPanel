package enklikanketa.com.a1kapanel.Models.StudyList;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import enklikanketa.com.a1kapanel.R;
import enklikanketa.com.a1kapanel.StudyPage;
import enklikanketa.com.a1kapanel.System.Network;

public class StudyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private String TAG = "SurveyHolder";

    private final TextView unfinished;
    private final TextView litem_zgoraj;
    private final TextView litem_spodaj;
    private final RelativeLayout litem_study;

    private Study study;
    private Activity ctx;

    public StudyHolder(Activity context, View itemView) {
        super(itemView);

        // 1. Set the context
        this.ctx = context;

        // 2. Inflate the UI widgets of the holder
        this.unfinished = itemView.findViewById(R.id.litem_unfinished_cnt);
        this.litem_zgoraj = itemView.findViewById(R.id.litem_zgoraj);
        this.litem_spodaj = itemView.findViewById(R.id.litem_spodaj);
        this.litem_study = itemView.findViewById(R.id.litem_study);

        // 3. Set the "onClick" listener of the holder
        itemView.setOnClickListener(this);
    }

    public void bindStudy(Study study) {

        // 4. Bind the data to the ViewHolder
        this.study = study;

        //neizpolnjen
        if (study.unfinished_cnt != null && !study.unfinished_cnt.equals("0") && !study.unfinished_cnt.equals("null"))
            unfinished.setText(study.unfinished_cnt);
        else {
            unfinished.setVisibility(View.GONE);
            litem_study.setBackgroundResource(R.drawable.study_item_grey);
        }

        this.litem_zgoraj.setText(study.litem_top);
        this.litem_spodaj.setText(study.litem_bottom);
    }

    @Override
    public void onClick(View v) {
        // Below line is just like a safety check, because sometimes holder could be null,
        // in that case, getAdapterPosition() will return RecyclerView.NO_POSITION
        if (getAdapterPosition() == RecyclerView.NO_POSITION) return;

        // 5. Handle the onClick event for the ViewHolder
        if (this.study != null) {
            Intent myIntent = new Intent(ctx, StudyPage.class);
            myIntent.putExtra("study", study);

            if (Network.checkMobileInternet(ctx, true)) {
                ctx.startActivity(myIntent);
            }
        }
    }

    public Study getStudy() {
        return study;
    }
}
