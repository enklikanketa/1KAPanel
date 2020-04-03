package enklikanketa.com.a1kapanel;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import enklikanketa.com.a1kapanel.Models.StudyList.Study;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.Network;
import enklikanketa.com.a1kapanel.Tasks.getSurveyList;
import enklikanketa.com.a1kapanel.Tasks.sendTriggeredGeofencesTask;

public class StudyPage extends AppCompatActivity implements SurveysToFillFragment.OnCompleteListener {
    String TAG = "StudyPage";

    private ViewPager viewPager;
    public Study study;
    private SurveysToFillFragment tofill_fragment = new SurveysToFillFragment();
    private SurveysToFillFragment filled_fragment = new SurveysToFillFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.study_page);

        study = (Study) getIntent().getSerializableExtra("study");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView study_title = findViewById(R.id.study_page_study_name);
        String up_title = getString(R.string.preponed_study_title)+study.getTitle();
        study_title.setText(up_title);

        viewPager = findViewById(R.id.viewPager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);//setting tab over viewpager

        //Implementing tab selected listener over tablayout
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());//setting current selected item over viewpager
                switch (tab.getPosition()) {
                    case 0:
                        Log.v(TAG,"TAB1");
                        break;
                    case 1:
                        Log.v(TAG,"TAB2");
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Network.checkMobileInternet(StudyPage.this, true)) {
            updateSurveyList();
        }
    }

    public void onComplete() {
        // After the fragment completes, it calls this callback.
        // setup the rest of your layout now
        updateSurveyList();
    }

    public void updateSurveyList(){
        if(tofill_fragment.text_no_questionaires != null && filled_fragment.text_no_questionaires != null) {
            //does data of triggered geofences exists
            int stej = ((Database) Database.getInstance(StudyPage.this)).countData("things_to_send", "name = 'triggered_geofences'");
            //if there are triggered geofences, send them to server first
            if (stej > 0) {
                new sendTriggeredGeofencesTask(StudyPage.this, null, new sendTriggeredGeofencesTask.TaskListener() {
                    @Override
                    public void onFinished(int useles_here) {
                        //when triggered geofences are sended, post another request for surveys
                        new getSurveyList(StudyPage.this, tofill_fragment.text_no_questionaires, tofill_fragment.list_view, tofill_fragment.adapter, filled_fragment.text_no_questionaires, filled_fragment.list_view, filled_fragment.adapter, study.getSrvId()).execute();
                    }
                }).execute();
            }
            //there are no triggered geofences, request for surveys
            else {
                new getSurveyList(StudyPage.this, tofill_fragment.text_no_questionaires, tofill_fragment.list_view, tofill_fragment.adapter, filled_fragment.text_no_questionaires, filled_fragment.list_view, filled_fragment.adapter, study.getSrvId()).execute();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.study_page_menu, menu);
        MenuItem info = menu.getItem(0);
        info.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:
                Intent myIntent = new Intent(StudyPage.this, SubscriptionInfo.class);
                myIntent.putExtra("study", study);
                if (Network.checkMobileInternet(StudyPage.this, true)) {
                    StudyPage.this.startActivity(myIntent);
                }
                break;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return true;
        }
        return false;
    }

    //Setting View Pager
    private void setupViewPager(ViewPager viewPager) {
        Bundle bundle1 = new Bundle();
        bundle1.putSerializable("study", study);
        bundle1.putString("mode", "tofill");
        // set Fragmentclass Arguments
        tofill_fragment.setArguments(bundle1);

        Bundle bundle2 = new Bundle();
        bundle2.putSerializable("study", study);
        bundle2.putString("mode", "filled");
        // set Fragmentclass Arguments
        filled_fragment.setArguments(bundle2);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(tofill_fragment, getString(R.string.questionnaires_to_fill));
        adapter.addFrag(filled_fragment, getString(R.string.filled_questionnaires));
        viewPager.setAdapter(adapter);
    }


    //View Pager fragments setting adapter class
    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();//fragment arraylist
        private final List<String> mFragmentTitleList = new ArrayList<>();//title arraylist

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }


        //adding fragments and title method
        public void addFrag(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }
}
