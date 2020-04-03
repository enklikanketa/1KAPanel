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

import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.Tasks.GetSurveyInfoIdentifierTask;


public class vpis extends AppCompatActivity {

    private String TAG = "vpis";
    EditText upoIme;
    Database DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vpis);

        DB = (Database) Database.getInstance(this);

        if (DB.countData("uporabnik", null) != 0) {
            DB.deleteAllRows("uporabnik");
        }

        upoIme = findViewById(R.id.upoIme);
        Button buttonVpis = findViewById(R.id.buttonVpis);
        buttonVpis.setOnClickListener(buttonVpisClick);
    }

    //sign in button
    OnClickListener buttonVpisClick = new OnClickListener() {
        public void onClick(View v) {
            new GetSurveyInfoIdentifierTask(vpis.this, upoIme.getText().toString()).execute();
        }
    };

    //create new user button
    /*OnClickListener buttonNewIdentifierClick = new OnClickListener() {
        public void onClick(View v) {
            saveUser("no_identifier");
        }
    };*/

    /**
     * Save user in user table, delete old data and go to Main.class
     *
     * @param identifier - identifier of user
     */
    /*private void saveUser(String identifier) {
        if (DB.countData("uporabnik", null) != 0) {
            DB.deleteAllRows("uporabnik");
        }

        DB.insertData("uporabnik", new String[][]{
                {"identifier", identifier},
                {"id_server", ""}});
    }*/
}
