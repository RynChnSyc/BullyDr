package com.example.chanoo.bullydr;


import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.nio.file.Path;

import com.example.drbully.Emotion;
import com.example.drbully.EmotionProbabilities;
import com.example.drbully.BullyDec;
import com.example.drbully.BullyDecAsyncResult;
import com.example.drbully.BullyDecException;
import com.example.drbully.Flac;
import com.example.drbully.SpeechAPI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.example.drbully.BullyDec.logD;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity
{

    private static final int PERMISSIONS_REQUEST_CODE = 5;
    private ProgressBar progressBarNeutrality;
    private ProgressBar progressBarHappiness;
    private ProgressBar progressBarSadness;
    private ProgressBar progressBarAnger;
    private ProgressBar progressBarFear;

    private TextView textViewNeutrality;
    private TextView textViewHappiness;
    private TextView textViewSadness;
    private TextView textViewAnger;
    private TextView textViewFear;
    private TextView sample;

    private TextView actionStatus;

    private PlayPauseButton playPauseButton;
    private SpeechAPI speechApi;
    private BullyDec vokaturiApi;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            logD("About to instantiate the library");
            vokaturiApi = BullyDec.getInstance(MainActivity.this);
            speechApi = new SpeechAPI(MainActivity.this);


        } catch (BullyDecException e) {
            e.printStackTrace();
        }

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);

        initializeViews();

        setListeners();
    }

    private void initializeViews()
    {
        sample = findViewById(R.id.sample);
        progressBarNeutrality = findViewById(R.id.progressBarNeutrality);
        progressBarHappiness = findViewById(R.id.progressBarHappiness);
        progressBarSadness = findViewById(R.id.progressBarSadness);
        progressBarAnger = findViewById(R.id.progressBarAnger);
        progressBarFear = findViewById(R.id.progressBarFear);
        playPauseButton = findViewById(R.id.playPauseButton);

        textViewNeutrality = findViewById(R.id.textViewNeutrality);
        textViewHappiness = findViewById(R.id.textViewHappiness);
        textViewSadness = findViewById(R.id.textViewSadness);
        textViewAnger = findViewById(R.id.textViewAnger);
        textViewFear = findViewById(R.id.textViewFear);

        actionStatus = findViewById(R.id.actionStatus);

    }

    private void setListeners()
    {
        playPauseButton.setOnControlStatusChangeListener(new PlayPauseButton.OnControlStatusChangeListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onStatusChange(View view, boolean state)
            {
                if(state)
                {
                    startListening();

                } else {
                    stopListening();
                }
            }

        });
    }

    @SuppressLint("SetTextI18n")
    private void startListening()
    {
        if(vokaturiApi != null)
        {
            try {
                setListeningUI();
                vokaturiApi.startListeningForSpeech();

            } catch (BullyDecException e) {
                setNotListeningUI();
                if(e.getErrorCode() == BullyDecException.VOKATURI_DENIED_PERMISSIONS)
                {
                    Toast.makeText(this, "Grant Microphone and Storage permissions in the app settings to proceed", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "There was some problem to start listening audio", Toast.LENGTH_SHORT).show();
                }
            } catch (IllegalStateException e)
            {
                setNotListeningUI();
                e.printStackTrace();
            }
        }
    }

    private void setListeningUI()
    {
        actionStatus.setText("Press again to stop listening and analyze emotions");
        progressBarNeutrality.setIndeterminate(true);
        progressBarHappiness.setIndeterminate(true);
        progressBarSadness.setIndeterminate(true);
        progressBarAnger.setIndeterminate(true);
        progressBarFear.setIndeterminate(true);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n")
    private void stopListening()
    {
        if(vokaturiApi != null)
        {

            setNotListeningUI();



            try {
                showMetrics(vokaturiApi.stopListeningAndAnalyze());
                convertFlac();

            } catch (BullyDecException e) {
                if(e.getErrorCode() == BullyDecException.VOKATURI_NOT_ENOUGH_SONORANCY)
                {
                    Toast.makeText(this, "Please speak a more clear and avoid noise around your environment", Toast.LENGTH_LONG).show();
                }
            } catch (IllegalStateException e)
            {
                e.printStackTrace();
            }
        }

    }

    private void setNotListeningUI()
    {
        actionStatus.setText("Press below button to start listening");
        progressBarNeutrality.setIndeterminate(false);
        progressBarHappiness.setIndeterminate(false);
        progressBarSadness.setIndeterminate(false);
        progressBarAnger.setIndeterminate(false);
        progressBarFear.setIndeterminate(false);
    }


    @SuppressLint("SetTextI18n")
    private void showMetrics(EmotionProbabilities emotionProbabilities)
    {
        emotionProbabilities.scaledValues(5);

        logD("showMetrics for, " + emotionProbabilities.toString());
        textViewNeutrality.setText("Neutrality: " + emotionProbabilities.Neutrality);
        textViewHappiness.setText("Happiness: " + emotionProbabilities.Happiness);
        textViewSadness.setText("Sadness: " + emotionProbabilities.Sadness);
        textViewAnger.setText("Anger: " + emotionProbabilities.Anger);
        textViewFear.setText("Fear: " + emotionProbabilities.Fear);

        progressBarNeutrality.setProgress(normalizeForProgressBar(emotionProbabilities.Neutrality));
        progressBarHappiness.setProgress(normalizeForProgressBar(emotionProbabilities.Happiness));
        progressBarSadness.setProgress(normalizeForProgressBar(emotionProbabilities.Sadness));
        progressBarAnger.setProgress(normalizeForProgressBar(emotionProbabilities.Anger));
        progressBarFear.setProgress(normalizeForProgressBar(emotionProbabilities.Fear));

    }

    private int normalizeForProgressBar(double val)
    {
        if(val < 1)
        {
            return (int)(val * 100);
        } else {
            return (int)(val * 10);
        }
    }
    private int getSize(BufferedReader reader) throws IOException {
        int sz = 0;
        while(reader.read() != -1) {
            sz += 1;
        }
        return sz;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private  void convertFlac() throws BullyDecException {
        Flac flacEncoder = new Flac();
        File inputFile = vokaturiApi.getRecordedAudio();
        File outputFile = null;
        try {
            outputFile = File.createTempFile("Converted", null, vokaturiApi.context.getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
        }
        flacEncoder.encode(inputFile, outputFile);
        int sampleRate = 16000;

        speechApi.addListener(mSpeechServiceListener);
        speechApi.startRecognizing(sampleRate);
        try {
            FileReader fr = new FileReader(inputFile);
            BufferedReader rd = new BufferedReader(fr);


            byte[] data = new byte[(int)inputFile.length()];
            FileInputStream fis = new FileInputStream(inputFile);
            fis.read(data);

            speechApi.recognize(data,data.length);
            System.out.println(outputFile);

            speechApi.finishRecognizing();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error in reading files.");
        }

        System.out.println("\n Converted");

        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(String.valueOf(outputFile)); // here file is the location of the audio file you wish to use an input
            mp.prepare();
            mp.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private final SpeechAPI.Listener mSpeechServiceListener =
            new SpeechAPI.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sample.setText(text);
                        }});
                }
            };



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        if (requestCode == PERMISSIONS_REQUEST_CODE)
        {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Audio recording permissions denied.", Toast.LENGTH_SHORT).show();
            }
        }

    }

}

