package enklikanketa.com.a1kapanel.Adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import enklikanketa.com.a1kapanel.Models.SurveyList.MyDiffCallback;
import enklikanketa.com.a1kapanel.Models.SurveyList.Survey;
import enklikanketa.com.a1kapanel.Models.SurveyList.SurveyHolder;

public class QuestionnairesSimpleAdapter extends RecyclerView.Adapter<SurveyHolder> {

    private List<Survey> surveys;
    private Activity context;
    private int itemResource;

    public QuestionnairesSimpleAdapter(Activity context, int itemResource, List<Survey> surveys) {
        this.surveys = surveys;
        this.context = context;
        this.itemResource = itemResource;
    }

    @Override
    public SurveyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(this.itemResource, parent, false);
        return new SurveyHolder(this.context, view);
    }

    @Override
    public void onBindViewHolder(@NonNull SurveyHolder holder, int position) {
        Survey survey = this.surveys.get(position);
        holder.bindSurvey(survey);
    }

    @Override
    public int getItemCount() {
        return this.surveys.size();
    }

    public void setActiveOn(int position){
        surveys.get(position).status = "";
        surveys.get(position).user_id = "";
        notifyItemChanged(position);
    }

    public void removeSurvey(int position){
        surveys.remove(position);
        notifyItemRemoved(position);
    }

    //sometimes crashes when lot of data is changed
    public void changeData(List<Survey> newSurveys){
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new MyDiffCallback(surveys, newSurveys));
        surveys = newSurveys;
        result.dispatchUpdatesTo(this);
    }

    public void changeDataAll(List<Survey> newSurveys){
        surveys = newSurveys;
        notifyDataSetChanged();
    }
}
