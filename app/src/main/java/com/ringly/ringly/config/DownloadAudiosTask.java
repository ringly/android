package com.ringly.ringly.config;

import android.os.AsyncTask;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.ringly.ringly.ui.screens.mindfulness.GuidedMeditationActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.github.mikephil.charting.charts.Chart.LOG_TAG;

/**
 * Created by Monica on 7/7/2017.
 */

public class DownloadAudiosTask extends AsyncTask<String, Integer, Boolean> {

    private File mediaFile;
    private String fileName;
    private GuidedMeditationActivity activity;
    private UUID audioId;
    private Exception exception;
    /**
     * Default constructor
     * @param activity
     */
    public DownloadAudiosTask(GuidedMeditationActivity activity, UUID audioId) {
        this.activity = activity;
        this.audioId = audioId;
    }

    @Override
    protected Boolean doInBackground(String... params) {

        OkHttpClient client = new OkHttpClient();
        String url = params[0];
        Call call = client.newCall(new Request.Builder().url(url).get().build());

        try {
            Response response = call.execute();
            if (response.code() == 200 || response.code() == 201) {

                InputStream inputStream = null;
                try {
                    inputStream = response.body().byteStream();

                    byte[] buff = new byte[1024 * 4];
                    long downloaded = 0;
                    long totalSize = response.body().contentLength();
                    fileName = params[1];
                    mediaFile = new File(fileName+"_temp");
                    OutputStream output = new FileOutputStream(mediaFile);

                    publishProgress(0);
                    while (true) {
                        int readed = inputStream.read(buff);

                        if (readed == -1) {
                            break;
                        }
                        output.write(buff, 0, readed);
                        //write buff
                        downloaded += readed;
                        publishProgress((int)(downloaded*100/totalSize));
                        if (isCancelled()) {
                            return false;
                        }
                    }

                    output.flush();
                    output.close();

                    return downloaded == totalSize; //check if allSize were saved
                } catch (IOException e) {
                    exception = e;
                    return false;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            exception = e;
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        activity.setDownloadedProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean fileSaved) {
        super.onPostExecute(fileSaved);
        GuidedMeditationsCache.getInstance().removeTask(this.audioId);
        if (fileSaved) {
            mediaFile.renameTo(new File(fileName));
            if (activity != null) {
                activity.setAudioFileDownloaded();
            }
        } else {
            if (activity != null) {
                activity.onAudioDownloadError(exception);
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        try {
            mediaFile.delete();
        } catch (Exception e) {
            Crashlytics.logException(e);
        }
    }

    public void setActivity (GuidedMeditationActivity activity) {
        this.activity = activity;
    }
}
