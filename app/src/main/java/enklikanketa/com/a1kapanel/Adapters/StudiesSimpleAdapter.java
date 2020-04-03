package enklikanketa.com.a1kapanel.Adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import enklikanketa.com.a1kapanel.Models.StudyList.Study;
import enklikanketa.com.a1kapanel.Models.StudyList.StudyHolder;

public class StudiesSimpleAdapter extends RecyclerView.Adapter<StudyHolder> {

    private List<Study> studies;
    private Activity context;
    private int itemResource;

    public StudiesSimpleAdapter(Activity context, int itemResource, List<Study> studies) {
        this.studies = studies;
        this.context = context;
        this.itemResource = itemResource;
    }

    @Override
    public StudyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(this.itemResource, parent, false);
        return new StudyHolder(this.context, view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudyHolder holder, int position) {
        Study study = this.studies.get(position);
        holder.bindStudy(study);
    }

    @Override
    public int getItemCount() {
        return this.studies.size();
    }

    public void changeDataAll(List<Study> newStudies){
        studies = newStudies;
        notifyDataSetChanged();
    }
}
