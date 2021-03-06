package enklikanketa.com.a1kapanel.Receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import enklikanketa.com.a1kapanel.App;
import enklikanketa.com.a1kapanel.Libraries.DoOnConnection;
import enklikanketa.com.a1kapanel.R;

/**
 * Created by Developer on 18.12.2017.
 */

public class NetworkChangeReceiver extends BroadcastReceiver {

    private Snackbar snackDisdconnected;
    private Activity act;
    private String TAG = "NetworkChangeReceiver";

    public NetworkChangeReceiver(Activity act){
        this.act = act;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = null;

        if(cm != null)
            activeNetwork = cm.getActiveNetworkInfo();

        if(activeNetwork != null) {
            if(App.wasOffline) {
                hideSnackbar();
                new DoOnConnection(act);
            }
        }
        else {
            if(snackDisdconnected == null){
                snackDisdconnected = Snackbar.make(act.findViewById(android.R.id.content),
                        act.getString(R.string.general_mobile_network_error), Snackbar.LENGTH_INDEFINITE);

                //Get the view of the snackbar
                View sbView = snackDisdconnected.getView();
                //set background color
                sbView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
                //Get the textview of the snackbar text
                TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                //set text color
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.white));

                App.wasOffline = true;
            }

            snackDisdconnected.show();
        }
    }

    public void hideSnackbar(){
        if(snackDisdconnected != null && snackDisdconnected.isShown()){
            snackDisdconnected.dismiss();
        }
    }
}
