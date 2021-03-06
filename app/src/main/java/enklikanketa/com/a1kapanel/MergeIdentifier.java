/*
 * Made by:
 * Uroš Podkrižnik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.Tasks.GetSurveyInfoIdentifierTask;


public class MergeIdentifier extends AppCompatActivity {

    private String TAG = "MergeIdentifier";
    EditText mergeIdentifier;
    Database DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.merge_identifier);

        DB = (Database) Database.getInstance(this);

        mergeIdentifier = findViewById(R.id.midentifier);
        Button buttonVpis = findViewById(R.id.buttonMerge);
        buttonVpis.setOnClickListener(buttonMergeClick);
    }

    //button to pass new identifier
    OnClickListener buttonMergeClick = new OnClickListener() {
        public void onClick(View v) {
        String[] user = DB.getRowData("uporabnik",
                new String[]{"identifier"}, null);
        if(user != null) {
            if (mergeIdentifier.getText().length() > 0 &&
                    !mergeIdentifier.getText().toString().equals(user[0])) {
                new GetSurveyInfoIdentifierTask(MergeIdentifier.this, mergeIdentifier.getText().toString()).execute();
            }
            else
                Toast.makeText(MergeIdentifier.this,
                        MergeIdentifier.this.getString(R.string.invalid_identifier)
                        , Toast.LENGTH_LONG).show();
        }
        }
    };
}
