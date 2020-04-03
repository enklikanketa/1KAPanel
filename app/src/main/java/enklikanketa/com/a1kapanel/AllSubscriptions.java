/*
 * Made by:
 * Uro� Podkri�nik
 * uros.podkriznik(at)gmail.com
 * Tel.: 041829380
 */

package enklikanketa.com.a1kapanel;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import enklikanketa.com.a1kapanel.Adapters.StudiesSimpleAdapter;
import enklikanketa.com.a1kapanel.Models.StudyList.Study;
import enklikanketa.com.a1kapanel.System.Network;
import enklikanketa.com.a1kapanel.Tasks.getSubscriptionsList;

public class AllSubscriptions extends Fragment {

    RecyclerView list_view;
    Activity ctx;
    boolean refreshList = true;
    TextView text_no_questionaires;
    StudiesSimpleAdapter adapter;
    String TAG = "AllSubscriptions";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.all_subscriptions, container, false);
        if (refreshList) {
            list_view = view.findViewById(R.id.allSubscriptionsList);
            text_no_questionaires = view.findViewById(R.id.ni_podatkov_subscriptions);

            RecyclerView.LayoutManager layoutManager = new GridLayoutManager(ctx, 2);
            list_view.setHasFixedSize(true);
            list_view.setLayoutManager(layoutManager);

            adapter = new StudiesSimpleAdapter(
                    ctx, R.layout.list_item_subscriptions, new ArrayList<Study>());
            list_view.setAdapter(adapter);

            refreshList = false;
        }

        final SwipeRefreshLayout swipeLayout = view.findViewById(R.id.swiperefresh_allsubscriptions);
        swipeLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                if (Network.checkMobileInternet(ctx, true)) {
                    updateSurveyList();
                }
                swipeLayout.setRefreshing(false);
            }
        });

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
        if (Network.checkMobileInternet(ctx, true)) {
            updateSurveyList();
        }
    }

    private void updateSurveyList(){
        new getSubscriptionsList(ctx, text_no_questionaires, list_view, adapter).execute();
    }
}