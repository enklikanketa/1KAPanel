package enklikanketa.com.a1kapanel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import enklikanketa.com.a1kapanel.Libraries.GeneralLib;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.Tasks.sendTrakingLocationsTask;

import static enklikanketa.com.a1kapanel.R.id.webView;

/**
 * Created by Uroš on 30/11/2015.
 */
public class WebResevanje extends AppCompatActivity {

    boolean identifierAlreadyChecked = false;
    String loc_id = "", system_variables = "";
    //id of input/entry/respondent in server DB
    String server_user_id = null;
    private String TAG = "WebResevanje";
    Database DB;

    //questions using camera
    private static String file_type     = "*/*";    // file types to be allowed for upload
    private String cam_file_data = null;        // for storing camera file information
    private ValueCallback<Uri> file_data;       // data/header received after file selection
    private ValueCallback<Uri[]> file_path;     // received file(s) temp. location
    private final static int file_req_code = 5;

    private boolean multiple_files = true;         // allowing multiple file upload

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_resevanje);

        getSupportActionBar();

        String link = getString(R.string.feedbackLink);

        Bundle bundle = getIntent().getExtras();
        if(bundle != null && bundle.getString("link") != null &&
                !bundle.getString("link").equals(""))
            link = bundle.getString("link");

        DB = (Database) Database.getInstance(WebResevanje.this);

        setSystemVariables();

        final WebView browser = findViewById(webView);
        WebSettings ws = browser.getSettings();
        ws.setLoadsImagesAutomatically(true);
        ws.setJavaScriptEnabled(true);
        if(Build.VERSION.SDK_INT >= 21){
            ws.setMixedContentMode(0);
        }
        browser.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        ws.setAllowFileAccess(true);
        ws.setDomStorageEnabled(true);
        browser.addJavascriptInterface(new JSInterface(), "JSInterface");
        //browser.loadData("", "text/html", null);
        browser.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        ws.setBuiltInZoomControls(true);
        browser.loadUrl(link+system_variables);

        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setDisplayZoomControls(false);
        ws.setSupportZoom(true);
        ws.setDefaultTextEncodingName("utf-8");

        browser.setWebChromeClient(chromeClient);
        browser.setWebViewClient(webViewClient);

        final SwipeRefreshLayout swipeLayout = findViewById(R.id.swiperefresh_webinfo);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                browser.reload();
                swipeLayout.setRefreshing(false);
            }
        });
    }

    private WebViewClient webViewClient = new WebViewClient() {
        ProgressDialog loader;
        boolean notFinished = true;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (loader != null)
                GeneralLib.dismissDialog(WebResevanje.this, loader);

            if (notFinished)
                try {
                    //progressdialog when page is loading
                    loader = ProgressDialog.show(WebResevanje.this, "",
                            WebResevanje.this.getResources().getString(R.string.loadPage), true);
                } catch(Exception e){
                    Log.d(TAG, "WebResevanje can't dismiss loader");
                }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //if this url, survey is ended
            if(identifierAlreadyChecked && server_user_id != null &&
                    loc_id != null && !loc_id.equals("")){
                ContentValues cv = new ContentValues();
                cv.put("server_input_id", server_user_id);
                DB.updateData("locations", cv, "id="+loc_id);
                new sendTrakingLocationsTask(WebResevanje.this, loc_id).execute();
                //so the same location is not sended multiple times
                loc_id = null;
            }
            if (url.equals("https://www.1ka.si/") || url.equals("http://www.1ka.si/")  || url.equals("http://test.1ka.si/") || url.equals("https://test.1ka.si/") || url.equals(getString(R.string.server_url))) {
                notFinished = false;
                GeneralLib.dismissDialog(WebResevanje.this, loader);
                finish();
            } else if (url.equals("https://www.facebook.com/dialog/return/close?#_=_")) {
                notFinished = false;
                GeneralLib.dismissDialog(WebResevanje.this, loader);
                finish();
            }
            //add system variables to url
            else{
                setSystemVariables();
                view.loadUrl(url+system_variables);
            }

            return false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (notFinished) {
                GeneralLib.dismissDialog(WebResevanje.this, loader);

                if(!identifierAlreadyChecked) {
                    String s = "javascript:JSInterface.checkElement(_usr_id);";

                    //var has to be set
                    view.loadUrl(s);
                }
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.e(TAG, "WebResevanje WebViewClient onReceivedError " + description + " " + failingUrl + " " + errorCode);
        }
    };

    private WebChromeClient chromeClient = new WebChromeClient(){
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d(TAG, "JS console message: "+consoleMessage.message());
            return true;
        }

        /*-- handling input[type="file"] requests for android API 21+ --*/
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams
        fileChooserParams) {
            if(file_permission() && Build.VERSION.SDK_INT >= 21) {
                file_path = filePathCallback;
                Intent takePictureIntent = null;
                Intent takeVideoIntent = null;

                boolean includeVideo = false;
                boolean includePhoto = false;

                /*-- checking the accept parameter to determine which intent(s) to include --*/
                paramCheck:
                for (String acceptTypes : fileChooserParams.getAcceptTypes()) {
                    String[] splitTypes = acceptTypes.split(", ?+"); // although it's an array, it still seems to be the whole value; split it out into chunks so that we can detect multiple values
                    for (String acceptType : splitTypes) {
                        switch (acceptType) {
                            case "*/*":
                                includePhoto = true;
                                includeVideo = true;
                                break paramCheck;
                            case "image/*":
                                includePhoto = true;
                                break;
                            case "video/*":
                                includeVideo = true;
                                break;
                        }
                    }
                }

                if (fileChooserParams.getAcceptTypes().length == 0) {   //no `accept` parameter was specified, allow both photo and video
                    includePhoto = true;
                    includeVideo = true;
                }

                if (includePhoto) {
                    takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(WebResevanje.this.getPackageManager()) != null) {
                        File photoFile = null;
                        try {
                            photoFile = create_image();
                            takePictureIntent.putExtra("PhotoPath", cam_file_data);
                        } catch (IOException ex) {
                            Log.e(TAG, "Image file creation failed", ex);
                        }
                        if (photoFile != null) {
                            cam_file_data = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                        } else {
                            cam_file_data = null;
                            takePictureIntent = null;
                        }
                    }
                }

                if (includeVideo) {
                    takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    if (takeVideoIntent.resolveActivity(WebResevanje.this.getPackageManager()) != null) {
                        File videoFile = null;
                        try {
                            videoFile = create_video();
                        } catch (IOException ex) {
                            Log.e(TAG, "Video file creation failed", ex);
                        }
                        if (videoFile != null) {
                            cam_file_data = "file:" + videoFile.getAbsolutePath();
                            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(videoFile));
                        } else {
                            cam_file_data = null;
                            takeVideoIntent = null;
                        }
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType(file_type);
                if (multiple_files) {
                    contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }

                Intent[] intentArray;
                if (takePictureIntent != null && takeVideoIntent != null) {
                    intentArray = new Intent[]{takePictureIntent, takeVideoIntent};
                    contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                } else if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                    contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*"});
                } else if (takeVideoIntent != null) {
                    intentArray = new Intent[]{takeVideoIntent};
                    contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"video/*"});
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "File chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, file_req_code);
                return true;
            } else {
                return false;
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return true;
        }
    }

    private void setSystemVariables(){
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            String[] user = DB.getRowData("uporabnik",
                    new String[]{"identifier", "id_server"}, null);

            if (user != null && !identifierAlreadyChecked) {
                system_variables += user[1] != null ?
                        "&maza_user_id=" + user[1] : "";
                system_variables += user[0] != null ?
                        "&maza_identifier=" + user[0] : "";

                system_variables += ((bundle.getString("srv_version_timestamp") != null) &&
                        (!bundle.getString("srv_version_timestamp").equals("0"))) ?
                        "&maza_srv_version=" + bundle.getString("srv_version_timestamp") : "";
                system_variables += bundle.getString("tgeof_id") != null &&
                        !bundle.getString("tgeof_id").equals("") ?
                        "&maza_tgeofence_id=" + bundle.getString("tgeof_id") : "";
                system_variables += bundle.getString("tact_id") != null &&
                        !bundle.getString("tact_id").equals("") ?
                        "&maza_tactivity_id=" + bundle.getString("tact_id") : "";
                system_variables += bundle.getString("mode") != null &&
                        !bundle.getString("mode").equals("") ?
                        "&maza_mode=" + bundle.getString("mode") : "";
            }

            system_variables += bundle.getString("lat") != null ?
                    "&latitude=" + bundle.getString("lat") : "";
            system_variables += bundle.getString("lng") != null ?
                    "&longitude=" + bundle.getString("lng") : "";
        }
    }

    /**
     * JS Interface to check if element exist
     */
    class JSInterface {
        @JavascriptInterface
        public void checkElement(String usr_id) {
            if(usr_id != null){
                identifierAlreadyChecked = true;
                server_user_id = usr_id;
            }
        }
    }

    //needed for camera type question
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;

            /*-- if file request cancelled; exited camera. we need to send null value to make future attempts workable --*/
            if (resultCode == Activity.RESULT_CANCELED) {
                if (requestCode == file_req_code) {
                    file_path.onReceiveValue(null);
                    return;
                }
            }

            /*-- continue if response is positive --*/
            if(resultCode== Activity.RESULT_OK){
                if(requestCode == file_req_code){
                    if(null == file_path){
                        return;
                    }

                    ClipData clipData;
                    String stringData;
                    try {
                        clipData = intent.getClipData();
                        stringData = intent.getDataString();
                    }catch (Exception e){
                        clipData = null;
                        stringData = null;
                    }

                    if (clipData == null && stringData == null && cam_file_data != null) {
                        results = new Uri[]{Uri.parse(cam_file_data)};
                    }else{
                        if (clipData != null) { // checking if multiple files selected or not
                            final int numSelectedFiles = clipData.getItemCount();
                            results = new Uri[numSelectedFiles];
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                results[i] = clipData.getItemAt(i).getUri();
                            }
                        } else {
                            results = new Uri[]{Uri.parse(stringData)};
                        }
                    }
                }
            }
            file_path.onReceiveValue(results);
            file_path = null;
        }else{
            if(requestCode == file_req_code){
                if(null == file_data) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                file_data.onReceiveValue(result);
                file_data = null;
            }
        }
    }

    /*-- checking and asking for required file permissions --*/
    public boolean file_permission(){
        if(Build.VERSION.SDK_INT >=23 && ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED))) {
            ActivityCompat.requestPermissions(WebResevanje.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 6);
            return false;
        }else{
            return true;
        }
    }

    /*-- creating new image file here --*/
    private File create_image() throws IOException{
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }

    /*-- creating new video file here --*/
    private File create_video() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name    = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name     = "file_"+file_name+"_";
        File sd_directory   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".3gp", sd_directory);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }
}
