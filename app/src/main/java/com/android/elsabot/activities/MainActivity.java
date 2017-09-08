package com.android.elsabot.activities;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.elsabot.R;
import com.android.elsabot.adapters.ApplicationAdapter;
import com.android.elsabot.adapters.ListAdapter;
import com.android.elsabot.adapters.ListObject;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;


public class MainActivity extends BaseActivity implements AIListener, TextToSpeech.OnInitListener,MediaPlayer.OnPreparedListener {

    //Here is your URL defined
    String URL = "http://vprbbc.streamguys.net/vprbbc24.mp3";

    private FloatingActionButton fab;
    private TextView resultTextView, queryTextView, idleTextView;
    //private AIButton aiButton;
    private AIService aiService;
    private Context context;
    private static String CLIENT_ACCESS_TOKEN = "8db863ad45ef489b89c53750279ceccf";
    public static final String TAG = MainActivity.class.getName();
    private Gson gson = new Gson();

    //Constants for vizualizator - HEIGHT 50dip
    private static final float VISUALIZER_HEIGHT_DIP = 50f;

    //Your MediaPlayer
    private MediaPlayer mp;

    //Visualization
    private Visualizer mVisualizer;
    private VisualizerView mVisualizerView;
    private LinearLayout mLinearLayout;
    private static TextToSpeech mTts;
    private int MY_DATA_CHECK_CODE = 0;

    private ListView list;
    private ListAdapter aAdapter;
    private List<ListObject> lArray;


    //List Applications
    private PackageManager packageManager = null;
    private List<ApplicationInfo> applist = null;
    private ApplicationAdapter listadaptor = null;
    private ListView nLv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        //TTS.init(context);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final AIConfiguration config = new AIConfiguration(CLIENT_ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        list = (ListView) findViewById(R.id.list);
        aAdapter = new ListAdapter(context,lArray);
        list.setAdapter(aAdapter);
        list.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        list.setStackFromBottom(true);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        mLinearLayout = (LinearLayout) findViewById(R.id.mLinearLayout);
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        queryTextView = (TextView) findViewById(R.id.queryTextView);
        idleTextView = (TextView) findViewById(R.id.idleTextView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listenButtonOnClick();
                initTTSIntent();
            }
        });

        // myMediaPlayer(URL);
        addVisualizerToActivity();

    }

    void fadeOutIdleText() {
        YoYo.with(Techniques.FadeOut)
                .duration(1500)
                .playOn(findViewById(R.id.idleTextView));

        findViewById(R.id.idleTextView).postDelayed(new Runnable() {
            @Override
            public void run() {
                fadeInIdleText();
            }
        }, 1500);
    }

    void fadeInIdleText() {
        YoYo.with(Techniques.FadeOut)
                .duration(1500)
                .playOn(findViewById(R.id.idleTextView));

        findViewById(R.id.idleTextView).postDelayed(new Runnable() {
            @Override
            public void run() {
                fadeOutIdleText();
            }
        }, 1500);
    }


    public void listenButtonOnClick() {
        fadeOutIdleText();
        idleTextView.setText("Now Listening...");
        idleTextView.setVisibility(View.VISIBLE);
        mVisualizerView.setVisibility(View.GONE);

        aiService.startListening();

    }

    public void initTTSIntent() {
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
    }



    @Override
    public void onResult(final AIResponse response) {

        Result result = response.getResult();
        // Get parameters
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }
        /*queryTextView.setText("Query:" + result.getResolvedQuery() +
                "\nAction: " + result.getAction() +
                "\nParameters: " + parameterString);*/

        final String qSpeech = result.getResolvedQuery();
        queryTextView.setText(qSpeech);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Log.d(TAG, "onResult");
                //resultTextView.setText(gson.toJson(response));

                Log.i(TAG, "Received success response");

                // this is example how to get different parts of result object
                final Status status = response.getStatus();
                Log.i(TAG, "Status code: " + status.getCode());
                Log.i(TAG, "Status type: " + status.getErrorType());

                final Result result = response.getResult();
                Log.i(TAG, "Resolved query: " + result.getResolvedQuery());

                Log.e(TAG, ">>>>>Action:>>>> " + result.getAction());
                if(result.getAction().equals("app.open")){
                    showAppsList();
                }else {

                }

                final String speech = result.getFulfillment().getSpeech();
                Log.i(TAG, "Speech: " + speech);
                //TTS.speak(speech);

                mVisualizerView.setVisibility(View.VISIBLE);
                idleTextView.setVisibility(View.GONE);
                resultTextView.setText(speech);
                aAdapter.add(new ListObject(qSpeech, speech));

                sayWhatIsSpoken(speech);

                final Metadata metadata = result.getMetadata();
                if (metadata != null) {
                    Log.i(TAG, "Intent id: " + metadata.getIntentId());
                    Log.i(TAG, "Intent name: " + metadata.getIntentName());
                }

                final HashMap<String, JsonElement> params = result.getParameters();
                if (params != null && !params.isEmpty()) {
                    Log.i(TAG, "Parameters: ");
                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                        Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
                    }
                }
            }

        });
    }


    void addVisualizerToActivity() {
        //You need to have something where to show Audio WAVE - in this case Canvas
        mVisualizerView = new VisualizerView(this);
        mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                (int) (VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
        mLinearLayout.addView(mVisualizerView);

    }

    private void theVisualizer() {

        // Create the Visualizer object and attach it to our media player.
        mVisualizer = new Visualizer(mp.getAudioSessionId());
        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate) {
                mVisualizerView.updateVisualizer(bytes);
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            }
        }, Visualizer.getMaxCaptureRate() / 2, true, false);

        //enable Visualizer
        mVisualizer.setEnabled(true);

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) { //the user has the necessary data - create the TTS
                mTts = new TextToSpeech(this, this);

                fab.setEnabled(true);

            } else {               //no data - install it now will no go to Google Play.
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        }
    }

    public void onInit(int status) {
        // TODO Auto-generated method stub
        if (status == TextToSpeech.SUCCESS) {
            // Set preferred language to US english.
            // Note that a language may not be available, and the result will indicate this.
            int result = mTts.setLanguage(Locale.US);
            // Try this someday for some interesting results.
            // int result mTts.setLanguage(Locale.FRANCE);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Lanuage data is missing or the language is not supported.
                Log.e("TAG", "Language is not available.");
            } else {
                // Check the documentation for other possible result codes.
                // For example, the language may be available for the locale,
                // but not for the specified country and variant.

                // The TTS engine has been successfully initialized.
            }
        } else {
            // Initialization failed.
            Log.e("TAG", "Could not initialize TextToSpeech.");
        }

    }

    private void sayWhatIsSpoken(final String whatIsSpoken) {
        final HashMap<String, String> myHashRender = new HashMap<String, String>();
        myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, whatIsSpoken);
        //myHashRender.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));

        String exStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File appTmpPath = new File(exStoragePath);
        appTmpPath.mkdirs();
        String tempFilename = "tmpaudio.wav";
        final String tempDestFile = appTmpPath.getAbsolutePath() + "/" + tempFilename;

        mTts.synthesizeToFile(whatIsSpoken, myHashRender,tempDestFile);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //myMediaPlayer(tempDestFile);

                mTts.speak(whatIsSpoken, TextToSpeech.QUEUE_ADD , myHashRender);

                Handler handler = new Handler();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        myMediaPlayer(tempDestFile);
                    }
                };
                handler.post(runnable);

            }
        });
    }


    //Our method that sets Vizualizer And MediaPlayer
    void myMediaPlayer(String url) {
        //start media player - like normal
        mp = new MediaPlayer();
        mp.setOnPreparedListener(this);

        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mp.setDataSource(url); // set data source our URL defined
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "1/" + e.toString(), Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "2/" + e.toString(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(">>>>Exception>>>>>", "" + e.toString());
            Toast.makeText(MainActivity.this, "3/" + e.toString(), Toast.LENGTH_SHORT).show();
        }

        try {   //tell your player to go to prepare state
            mp.prepare();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.e(">>>>Exception>>>>>", "" + e.toString());
            Toast.makeText(MainActivity.this, "4/" + e.toString(), Toast.LENGTH_SHORT).show();
        }
        //Start your stream / player
        mp.start();

        theVisualizer();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    /**
     * A simple class that draws waveform data received from a
     * {@link Visualizer.OnDataCaptureListener#onWaveFormDataCapture }
     */
    public class VisualizerView extends View {
        private byte[] mBytes;
        private float[] mPoints;
        private Rect mRect = new Rect();

        private Paint mForePaint = new Paint();

        public VisualizerView(Context context) {
            super(context);
            init();
        }

        private void init() {
            mBytes = null;

            mForePaint.setStrokeWidth(1f);
            mForePaint.setAntiAlias(true);
            mForePaint.setColor(Color.rgb(0, 0, 0));

        }

        public void updateVisualizer(byte[] bytes) {
            mBytes = bytes;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (mBytes == null) {
                return;
            }

            if (mPoints == null || mPoints.length < mBytes.length * 4) {
                mPoints = new float[mBytes.length * 4];
            }

            mRect.set(0, 0, getWidth(), getHeight());

            for (int i = 0; i < mBytes.length - 1; i++) {
                mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
                mPoints[i * 4 + 1] = mRect.height() / 2
                        + ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
                mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
                mPoints[i * 4 + 3] = mRect.height() / 2
                        + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
            }

            canvas.drawLines(mPoints, mForePaint);
        }
    }

    void showAppsList(){
        packageManager = getPackageManager();

        new LoadApplications().execute();

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Your Apps");
        alert.setCancelable(false);
        nLv = new ListView(this);
        nLv.setAdapter(listadaptor);
        nLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ApplicationInfo app = applist.get(position);
                try {
                    Intent intent = packageManager
                            .getLaunchIntentForPackage(app.packageName);

                    if (null != intent) {
                        startActivity(intent);
                    }
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, e.getMessage(),
                            Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(context, e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        alert.setView(nLv);
        alert.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        alert.show();
    }

    private List<ApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
        ArrayList<ApplicationInfo> applist = new ArrayList<ApplicationInfo>();
        for (ApplicationInfo info : list) {
            try {
                if (null != packageManager.getLaunchIntentForPackage(info.packageName)) {
                    applist.add(info);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return applist;
    }

    private class LoadApplications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;

        @Override
        protected Void doInBackground(Void... params) {
            applist = checkForLaunchIntent(packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
            listadaptor = new ApplicationAdapter(MainActivity.this,
                    R.layout.app_list, applist);

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            nLv.setAdapter(listadaptor);
            progress.dismiss();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(MainActivity.this, null,
                    "Loading application info...");
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(context, AISettingsActivity.class));
            return true;
        }if (id == R.id.action_sms) {
            startActivity(new Intent(context, SMSReader.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing() && mp != null) {
            mVisualizer.release();
            mp.release();
            mp = null;
        }
    }

    @Override
    public void onError(ai.api.model.AIError error) {
        resultTextView.setText(error.toString());
        mVisualizerView.setVisibility(View.VISIBLE);
        idleTextView.setVisibility(View.GONE);
    }


    @Override
    public void onAudioLevel(float level) {
    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {
    }

    @Override
    public void onListeningFinished() {
    }


}
