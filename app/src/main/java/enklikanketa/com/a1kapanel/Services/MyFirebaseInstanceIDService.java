package enklikanketa.com.a1kapanel.Services;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;

import enklikanketa.com.a1kapanel.Tasks.sendRegistrationIDTask;

/**
 * Created by Developer on 27.8.2017.
 */

public class MyFirebaseInstanceIDService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        
        Log.d("a1kapanel", "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String refreshedToken){
        new sendRegistrationIDTask(this, refreshedToken).execute();
    }
}
