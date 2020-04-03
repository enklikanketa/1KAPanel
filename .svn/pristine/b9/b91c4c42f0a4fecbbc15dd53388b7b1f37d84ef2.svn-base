package enklikanketa.com.a1kapanel.Services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

import enklikanketa.com.a1kapanel.Tasks.sendTrakingLocationsTask;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class LocationsJobService extends JobService {
    private static final String TAG = "SyncService";

    @Override
    public boolean onStartJob(JobParameters params) {
        new sendTrakingLocationsTask(this).execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
