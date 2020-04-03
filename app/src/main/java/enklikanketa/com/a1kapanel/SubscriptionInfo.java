/*
 * Made by:
 * Uro� Podkri�nik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import enklikanketa.com.a1kapanel.Models.StudyList.Study;
import enklikanketa.com.a1kapanel.System.Network;
import enklikanketa.com.a1kapanel.Tasks.sendUnsubscribeSurvey;

public class SubscriptionInfo extends AppCompatActivity {

    String TAG = "SubscriptionInfo";
    String srv_id = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.subscription_info);

        Study study = (Study) getIntent().getSerializableExtra("study");
        if(study != null) {
            if(study.getTitle() != null) {
                TextView study_title = findViewById(R.id.study_page_study_name);
                String up_title = getString(R.string.preponed_study_title) + study.getTitle();
                study_title.setText(up_title);
            }

            if(study.getSrvId() != null)
                srv_id = study.getSrvId();

            if(study.getDescription() != null && !study.getDescription().equals("null") && !study.getDescription().equals("")) {
                ((TextView) findViewById(R.id.subs_description)).setText(study.getDescription());
                findViewById(R.id.study_page_description).setVisibility(View.VISIBLE);
            }
            if(study.getLocationPermission()) {
                findViewById(R.id.permission_location).setVisibility(View.VISIBLE);
            }
            if(study.getARPermission()) {
                findViewById(R.id.permission_activity).setVisibility(View.VISIBLE);
            }
        }

        Button unsubscribe = findViewById(R.id.subs_unsubscribe);
        unsubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUnsubscribeDialog();
            }
        });
    }

    private void showUnsubscribeDialog(){
        final AlertDialog myDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_unsubscribe_title)
                .setMessage(getString(R.string.dialog_unsubscribe_message))
                .setNegativeButton(R.string.dialog_unsubscribe_no, null)
                .setPositiveButton(R.string.dialog_unsubscribe_yes, null).create();

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

                Button btpo = myDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                btpo.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        myDialog.dismiss();
                        new sendUnsubscribeSurvey(SubscriptionInfo.this, srv_id).execute();
                    }
                });
            }
        });
        myDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (Network.checkMobileInternet(SubscriptionInfo.this, true))
                    finish();
                break;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
