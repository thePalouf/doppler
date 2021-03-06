package com.example.lulu.doppler.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.lulu.doppler.R;
import com.example.lulu.doppler.io.FileChooser;
import com.example.lulu.doppler.io.SaveSound;
import com.example.lulu.doppler.io.SoundRecorder;
import com.example.lulu.doppler.listeners.OnSoundReadListener;
import com.example.lulu.doppler.tools.WaveletFilter;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.jtransforms.fft.DoubleFFT_1D;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ResultDisplayActivity extends ActionBarActivity {
    private LineChart chart;
    ArrayList xVals = new ArrayList();
    ArrayList<Short> completeSound = new ArrayList<Short>();
    Boolean record=false;
    AudioManager am;
    final int sampleRateInHz = 44100;
    SoundRecorder recorder = new SoundRecorder(sampleRateInHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    final int bufferSize = recorder.getBufferSize();

    AudioTrack at;
    private com.github.amlcurran.showcaseview.targets.Target t2;
    private com.github.amlcurran.showcaseview.targets.Target t3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_display);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Context context = getApplicationContext();
        am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        //am.setMode(AudioManager.MODE_IN_CALL);
        //am.setSpeakerphoneOn(true);
        //am.setMicrophoneMute(true);
        final boolean[] aide = {true};
        Button b2;
        b2 = (Button) findViewById(R.id.button2);
        Button b3;
        b3 = (Button) findViewById(R.id.boutonfiltre);
        t2 = new ViewTarget(R.id.register, this);
        t3 = new ViewTarget(R.id.boutonfiltre,this);
        final ShowcaseView scv1 = new ShowcaseView.Builder(this)
                .setTarget(com.github.amlcurran.showcaseview.targets.Target.NONE)
                .setContentTitle("Aide :")
                .setContentText("Cliquez sur le boutton pour enregistrer le bébé\n Le diagramme affiche le spectrogramme du signal")
                .hideOnTouchOutside()
                .build();
        scv1.setButtonText("Suivant");
        scv1.overrideButtonClick(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(aide[0]) {
                    scv1.setShowcase(t3, true);
                    scv1.setButtonText("OK");
                    scv1.setContentText("Cliquez sur ce boutton pour filtrer un enregistrement précédement effectué");
                    aide[0] =false;
                }
                else{
                    scv1.hide();
                    aide[0] =true;
                }


            }
        });
        scv1.hide();

        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scv1.setShowcase(t2, true);
                scv1.show();
            }
        });

        b3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ResultDisplayActivity.this, FilterActivity.class);
                recorder.stop();
                at.stop();
                startActivity(intent);
                scv1.hide();
            }
        });

        ImageButton ib = (ImageButton) findViewById(R.id.register);
        ib.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ImageButton b = (ImageButton) findViewById(R.id.register);
                scv1.hide();
                if(record==false) {
                    b.setImageResource(R.drawable.stop);
                    record=true;
                }else{
                    b.setImageResource(R.drawable.record);
                    record=false;
                    short[] total = new short[completeSound.size()];
                    //System.out.println("fgd" + total[96]);
                    for (int i = 0 ; i < total.length ; i++)
                        total[i]=completeSound.get(i);
                    SaveSound ss = new SaveSound(total, sampleRateInHz);
                    try {
                        ss.rawToWave();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    completeSound.clear();
                    Toast.makeText(getApplicationContext(), "Votre fichier a été enregistré avec succès",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
        chart = (LineChart) findViewById(R.id.chart);
        // no description text
        chart.setNoDataTextDescription("Branchez la sonde Doppler");
        // enable touch gestures
        chart.setTouchEnabled(true);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisRight().setDrawGridLines(false);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(true);
        LineData data = new LineData();
        chart.setData(data);



        at = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, AudioFormat.CHANNEL_OUT_MONO,
                recorder.getAudioFormat(), bufferSize, AudioTrack.MODE_STREAM);

        at.play();

        recorder.addListener(new OnSoundReadListener() {
            @Override
            public void OnReceive(short[] buffer, int nbRealValues) {
                at.write(buffer, 0, nbRealValues);


                //System.out.println("nbvalue" + nbRealValues);
                if(record) {
                    for (int i = 0; i < nbRealValues; i++) {
                        completeSound.add(buffer[i]);
                        //System.out.println("wtf" + completeSound.get(i));
                    }
                }
                //System.out.println("iki : ca feed : " + nbRealValues);
                DoubleFFT_1D fftDo = new DoubleFFT_1D(buffer.length);
                double[] fft = new double[buffer.length * 2];

                for (int i = 0; i < bufferSize; i++) {

                    fft[i] = (double) buffer[i];
                }

                fftDo.realForwardFull(fft);
                double max = 0.0;
                int argmax = 0;
                for (int j = 0; j < bufferSize; j++) {
                    double a = Math.sqrt(fft[2 * j] * fft[2 * j] + fft[2 * j + 1] * fft[2 * j + 1]);
                    if (a > max) {
                        max = a;
                        argmax = j;
                    }
                }
                double res = argmax * (sampleRateInHz) / bufferSize;
                //System.out.println("ikik : " + argmax);

                if (res < 15000) {
                    ajouterValeur(2 * res);
                }
            }
        });

        recorder.execute();
    }

    private void feedMultiple() {

        new Thread(new Runnable() {

            @Override
            public void run() {
                for(int i = 0; i < 30; i++) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            xVals.add("val");
                        }
                    });
                }
            }
        }).start();
    }

    protected void ajouterValeur(double data){
        LineData d= chart.getData();
        LineDataSet s=null;
        if(d != null) {
            s = d.getDataSetByIndex(0);
            if (s == null) {
                s = createSet();
                d.addDataSet(s);
            }
            d.addXValue("");
            d.addEntry(new Entry((float) data, s.getEntryCount()), 0);


            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            chart.setVisibleXRangeMaximum(50);

            // move to the latest entry

            chart.moveViewToX(d.getXValCount()+25);


        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, null);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setDrawCircles(false);
        set.setLineWidth(2f);
        set.setFillAlpha(255);
        set.setFillColor(Color.BLACK);
        set.setColor(Color.BLACK);
        //set.setDrawFilled(true);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        set.setDrawCubic(true);
        return set;
    }


    @Override
    protected void onResume() {
        super.onResume();
        //recorder.start();
        recorder = new SoundRecorder(sampleRateInHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        at.play();
    }

    @Override
    protected void onPause(){
        super.onPause();
        at.pause();
        //recorder.stop();
    }

}
