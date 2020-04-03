package enklikanketa.com.a1kapanel.Models.SurveyList;

import android.support.v7.util.DiffUtil;

import java.util.List;

public class MyDiffCallback extends DiffUtil.Callback{

    private List<Survey> newSurveys;
    private List<Survey> oldSurveys;

    public MyDiffCallback(List<Survey> newSurveys, List<Survey> oldSurveys) {
        this.newSurveys = newSurveys;
        this.oldSurveys = oldSurveys;
    }

    @Override
    public int getOldListSize() {
        return oldSurveys.size();
    }

    @Override
    public int getNewListSize() {
        return newSurveys.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldSurveys.get(oldItemPosition).item_id == newSurveys.get(newItemPosition).item_id;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldSurveys.get(oldItemPosition).equals(newSurveys.get(newItemPosition));
    }

    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        //you can return particular field for changed item.
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
