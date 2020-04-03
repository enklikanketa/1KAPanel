/*
 * Made by:
 * Uro� Podkri�nik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import enklikanketa.com.a1kapanel.Adapters.QuestionnairesSimpleAdapter;
import enklikanketa.com.a1kapanel.Models.StudyList.Study;
import enklikanketa.com.a1kapanel.Models.SurveyList.Survey;
import enklikanketa.com.a1kapanel.System.Database;
import enklikanketa.com.a1kapanel.System.Network;

public class SurveysToFillFragment extends Fragment {

    RecyclerView list_view;
    Activity ctx;
    private Study study;
    TextView text_no_questionaires;
    FloatingActionButton fab;
    QuestionnairesSimpleAdapter adapter;
    String TAG = "SurveysToFillFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.surveys_to_fill, container, false);

        if(getArguments() != null) {
            study = (Study)getArguments().getSerializable("study");
        }

        list_view = view.findViewById(R.id.vsiVprasalnikiList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(ctx);
        list_view.setHasFixedSize(true);
        list_view.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(list_view.getContext(),
                ((LinearLayoutManager) layoutManager).getOrientation());
        list_view.addItemDecoration(dividerItemDecoration);

        adapter = new QuestionnairesSimpleAdapter(
                ctx, R.layout.list_item_vprasalniki, new ArrayList<Survey>());
        list_view.setAdapter(adapter);

        text_no_questionaires = view.findViewById(R.id.ni_podatkov_vprasalniki);

        final SwipeRefreshLayout swipeLayout = view.findViewById(R.id.swiperefresh_vsivprasalniki);
        swipeLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                if (Network.checkMobileInternet(ctx, true)) {
                    mListener.onComplete();
                }
                swipeLayout.setRefreshing(false);
            }
        });

        fab = view.findViewById(R.id.fab);

        if(study != null)
            showEntry();

        mListener.onComplete();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public interface OnCompleteListener {
        void onComplete();
    }

    private OnCompleteListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnCompleteListener)context;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnCompleteListener");
        }
    }

    private void showEntry(){
        Database DBH = (Database) Database.getInstance(ctx);
        //get entry data
        final HashMap<String, String> entry = DBH.getRowHashMapData("entry", null, "srv_id="+study.getSrvId());
        if(entry != null){
            //there is at least one entry, make button visible
            ((View)fab).setVisibility(View.VISIBLE);

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //entry has set location check on
                    if(entry.get("location_check").equals("1")) {
                        Intent myIntent = new Intent(ctx, PickLocation.class);
                        myIntent.putExtra("link", study.getLink());
                        startActivity(myIntent);
                    }
                    //no location check needed
                    else{
                        Intent myIntent = new Intent(ctx, WebResevanje.class);
                        myIntent.putExtra("srv_version_timestamp", System.currentTimeMillis()/1000+"");
                        myIntent.putExtra("mode", "entry");
                        myIntent.putExtra("link", study.getLink());
                        startActivity(myIntent);
                    }
                }
            });
        } else
            //there is no data entry surveys, hide button
            ((View)fab).setVisibility(View.GONE);
    }
}
