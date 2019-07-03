package com.ringly.ringly.ui.screens.mindfulness;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.crashlytics.android.Crashlytics;
import com.ringly.ringly.Preferences;
import com.ringly.ringly.R;
import com.ringly.ringly.config.DownloadAudiosTask;
import com.ringly.ringly.config.GuidedMeditationsCache;
import com.ringly.ringly.config.model.GuidedMeditation;
import com.ringly.ringly.ui.BaseActivity;
import com.ringly.ringly.ui.Utilities;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by Monica on 6/12/2017.
 */

public class GuidedMeditationActivity extends BaseActivity implements  MediaPlayer.OnPreparedListener {


    private PowerManager.WakeLock wakeLock;
    private GuidedMeditation guidedMeditation;
    private MediaPlayer mediaPlayer;
    private boolean playingAudio = false;
    private PlayPauseButton playPauseButton;
    private Timer updateTimer;
    private TimerTask updateTimerTask;
    private int duration;
    private int seconds;
    private TextView timerTextView;
    private boolean audioFileDownloaded = false;
    private boolean playerPrepared = false;
    private boolean userStartedAudio = false;
    private RelativeLayout timerLayout;
    private File audioFile;
    private UUID audioId;

    private Snackbar snackbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_meditation);
        setDefaultTypeface(findViewById(R.id.layoutMain));

        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if (getIntent().hasExtra(GuidedMeditation.GUIDED_MEDITATION_ID)) {
            guidedMeditation = (GuidedMeditation) getIntent().getSerializableExtra(GuidedMeditation.GUIDED_MEDITATION_ID);
            setValues();
        }

        findViewById(R.id.layoutBeginning).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutListening).setVisibility(View.GONE);
        timerLayout = (RelativeLayout) findViewById(R.id.layoutTimer);
    }


    private void setValues() {
        if (guidedMeditation == null) {
            return;
        }
        playPauseButton = (PlayPauseButton)findViewById(R.id.btnPlayPause);
        //Disable playPauseButton until the media player is prepeared and the audioFile is downloaded
        playPauseButton.setEnabled(false);
        mediaPlayer.setOnPreparedListener(this);

        audioId = UUID.nameUUIDFromBytes(guidedMeditation.audioFile.getBytes());
        checkAndDownloadFile();

        final Button beginButton = (Button) findViewById(R.id.btnBegin);
        Glide.with(this).load(guidedMeditation.iconImage3x).into(new SimpleTarget<Drawable>() {
            @Override
            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                beginButton.setCompoundDrawablesWithIntrinsicBounds(null,resource, null, null);
            }
        });

        TextView titleTV = (TextView)findViewById(R.id.txtTitle);
        titleTV.setText(guidedMeditation.title);

        TextView subTitleTV = (TextView)findViewById(R.id.txtSubTitle);
        subTitleTV.setText(guidedMeditation.sessionDescription);

        TextView minutesTV = (TextView)findViewById(R.id.txtMinutes);
        minutesTV .setText(guidedMeditation.lengthSeconds/60 + " " +getString(R.string.minutes));

        if (guidedMeditation.author!= null) {
            TextView ledTV = (TextView) findViewById(R.id.txtLeadBy);
            ledTV.setText(getString(R.string.led_by) + " " + guidedMeditation.author.name);
            ImageView ledIV = (ImageView)findViewById(R.id.imgLead);
            Glide.with(this).load(guidedMeditation.author.image).into(ledIV);
        }
    }

    /**
     * Check if the file has been downloaded or its downloading, if not download.
     */
    private void checkAndDownloadFile() {
        try {
            audioFile = new File(getCacheDir()+"/"+audioId);
            if (GuidedMeditationsCache.getInstance().isDownloading(audioId)) {
                DownloadAudiosTask task = GuidedMeditationsCache.getInstance().getTask(audioId);
                if (task != null) {
                    task.setActivity(this);
                } else {
                    checkAndDownloadFile();
                }
            } else if (!audioFile.exists()) {
                DownloadAudiosTask task = new DownloadAudiosTask(this, audioId);
                task.execute(guidedMeditation.audioFile, audioFile.getPath());
                GuidedMeditationsCache.getInstance().addTask(task, audioId);
            } else {
                audioFileDownloaded = true;
                mediaPlayer.setDataSource(audioFile.getPath());
                mediaPlayer.prepareAsync();
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
        }
    }
    /**
     * This show the listening screen
     */
    public void startListening(View view) {
        findViewById(R.id.layoutBeginning).setVisibility(View.GONE);
        timerTextView = (TextView)findViewById(R.id.txtTimer);

        LinearLayout listeningLayout = (LinearLayout) findViewById(R.id.layoutListening);
        listeningLayout.setVisibility(View.VISIBLE);
        acquireWakeLock();
        userStartedAudio = true;
        if (playerPrepared && audioFileDownloaded) {
            doPlayPause();
        }
    }

    public void playPause(View view) {
        this.doPlayPause();
    }

    private void doPlayPause() {
        if (!playingAudio) {
            playPauseButton.play();
            mediaPlayer.start();
            scheduleTimerTask();
            playingAudio = true;
        } else {
            playPauseButton.pause();
            cancelTimer();
            mediaPlayer.pause();
            playingAudio = false;
        }
    }

    private void scheduleTimerTask() {
        updateTimer = new Timer();
        updateTimerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        seconds --;
                        timerTextView.setText(Utilities.formatSeconds(seconds));
                        playPauseButton.incrementProgress(360f/(mediaPlayer.getDuration()/1000));

                        if (seconds == duration - 5) {
                            timerLayout.setVisibility(View.INVISIBLE);

                        } else if (seconds==0) {
                            updateTimerTask.cancel();
                            updateTimer = null;
                            Preferences.addMindfulMinutesCount(GuidedMeditationActivity.this, duration);
                            findViewById(R.id.layoutListening).setClickable(false);
                            playPauseButton.setVisibility(View.INVISIBLE);
                            findViewById(R.id.btnCheckDone).setVisibility(View.VISIBLE);
                            findViewById(R.id.imgDone).setVisibility(View.VISIBLE);
                            releaseWakeLock();
                        }

                    }
                });
            }
        };
        updateTimer.schedule(updateTimerTask, 1000, 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMediaPlayer();
        releaseWakeLock();
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    private void stopMediaPlayer() {
        cancelTimer();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
            }catch (Exception e) {
                //Nothing to do
            }
        }
    }

    private void cancelTimer() {
        if (updateTimer != null) {
            try {
                updateTimer.cancel();
                Preferences.addMindfulMinutesCount(GuidedMeditationActivity.this, duration-seconds);
                updateTimer = null;
            } catch (Exception e) {
                //this should never fail
                Crashlytics.logException(e);
            }
        }
    }

    private void acquireWakeLock() {
        if (wakeLock != null) {
            try{
                wakeLock.acquire();
            } catch (Exception e) {
                Crashlytics.logException(e);
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            try{
                wakeLock.release();
            } catch (Exception e) {
                //Nothing to do
            }
        }
    }


    public void closeScreen(View view) {
        this.finish();
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        playerPrepared = true;
        duration = (int)Math.ceil(mediaPlayer.getDuration()/1000);
        seconds = duration;
        if (audioFileDownloaded) {
            playPauseButton.setPercentageProgress(0);
            playPauseButton.setEnabled(true);
            if (userStartedAudio) {
                doPlayPause();
            }
        }
    }

    public void setDownloadedProgress(int progress) {
        playPauseButton.setPercentageProgress(progress);
    }

    public void setAudioFileDownloaded() {
        audioFileDownloaded = true;
        try {
            if (mediaPlayer!= null) {
                mediaPlayer.setDataSource(audioFile.getPath());
                mediaPlayer.prepareAsync();
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
        }
    }

    public void onAudioDownloadError(Exception e) {
        String message = "";
        if (e != null & e instanceof IOException) {
            message = getString(R.string.error_no_connection);
        } else {
            message = getString(R.string.error_general);
        }
        snackbar = Snackbar
                .make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        checkAndDownloadFile();
                    }
                });

        snackbar.show();
    }

    public void showHideTimer(View view) {
        if (timerLayout.getVisibility() == View.VISIBLE) { //hide
            timerLayout.setVisibility(View.INVISIBLE);
        } else {
            timerLayout.setVisibility(View.VISIBLE);
        }
    }

}
