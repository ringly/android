package com.ringly.ringly.config;

import android.os.AsyncTask;

import com.ringly.ringly.RinglyApp;
import com.ringly.ringly.config.model.GuidedMeditation;
import com.ringly.ringly.ui.dfu.DoneFragment;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import rx.Observable;

/**
 * Created by Monica on 6/13/2017.
 */

public class GuidedMeditationsCache {

    private static final String TAG = GuidedMeditationsCache.class.getCanonicalName();

    private static GuidedMeditationsCache instance = null;
    private Observable<List<GuidedMeditation>> mGuidedMeditationsList;
    private Map<UUID, DownloadAudiosTask> mDownloadingAuidiosList;
    /**
     *
     * @return Singleton instance
     */
    public static GuidedMeditationsCache getInstance() {
        if (instance == null) {
            instance = new GuidedMeditationsCache();
        }
        return instance;
    }

    private GuidedMeditationsCache() {
        this.mGuidedMeditationsList = RinglyApp.getInstance().getApi().getGuidedMeditationsService().getGuidedMeditations().cache();
        this.mDownloadingAuidiosList = Collections.synchronizedMap(new HashMap<UUID, DownloadAudiosTask>());
    }


    public  Observable<List<GuidedMeditation>> getGuidedMeditationsList() {
        if (this.mGuidedMeditationsList == null) {
            this.mGuidedMeditationsList = RinglyApp.getInstance().getApi().getGuidedMeditationsService().getGuidedMeditations().cache();
        }
        return this.mGuidedMeditationsList;
    }

    public void reset() {
        //When an error happen in the request need to reset it to cache again
        mGuidedMeditationsList = null;
    }

    /**
     * Check if a DownloadingTask is running for the audioId
     * @param audioId
     * @return
     */
    public boolean isDownloading(UUID audioId) {
        if (this.mDownloadingAuidiosList.containsKey(audioId)) {
            return true;
        } else {
            return false;
        }
    }

    public DownloadAudiosTask getTask(UUID audioId) {
        if (this.mDownloadingAuidiosList.containsKey(audioId)) {
            return this.mDownloadingAuidiosList.get(audioId);
        } else {
            return null;
        }
    }

    public void addTask(DownloadAudiosTask task, UUID audioId) {
        this.mDownloadingAuidiosList.put(audioId, task);
    }

    /**
     * When DownloadTask finish remove from map
     * @param audioId
     */
    public void removeTask(UUID audioId) {
        if (this.mDownloadingAuidiosList.containsKey(audioId)) {
            this.mDownloadingAuidiosList.remove(audioId);
        }
    }

    public void cancelDownloadingTasks() {
        for (Map.Entry<UUID, DownloadAudiosTask> entry : this.mDownloadingAuidiosList.entrySet()) {
            try {
                DownloadAudiosTask task = entry.getValue();
                task.cancel(true);
            } catch (Exception e) {
                //Nothing to do
            }
        }
    }
}
