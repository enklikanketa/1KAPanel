package enklikanketa.com.a1kapanel;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import enklikanketa.com.a1kapanel.System.Network;

/**
 * Created by uros on 15.1.2016.
 */
public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        getSupportActionBar();

        WebView webView = findViewById(R.id.textArrs);
        webView.loadData(getString(R.string.arrs_html),"text/html","utf-8");
        WebSettings webSettings = webView.getSettings();
        webSettings.setDefaultFontSize(15);
        webView.setBackgroundColor(Color.TRANSPARENT);

        WebView webViewFoot = findViewById(R.id.textCdi);
        webViewFoot.loadData(String.format(getText(R.string.cdi_html).toString(),
                BuildConfig.VERSION_NAME),"text/html","utf-8");
        webSettings = webViewFoot.getSettings();
        webSettings.setDefaultFontSize(14);
        webViewFoot.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (Network.checkMobileInternet(About.this, true))
                    finish();
                break;
        }
        return true;
    }

}
