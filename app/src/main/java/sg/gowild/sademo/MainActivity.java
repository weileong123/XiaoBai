package sg.gowild.sademo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Result;
import ai.kitt.snowboy.SnowboyDetect;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    // View Variables
    private Button button;
    private TextView textView;

    // ASR Variables
    private SpeechRecognizer speechRecognizer;

    // TTS Variables
    private TextToSpeech textToSpeech;

    // NLU Variables
    private AIDataService aiDataService;

    // Hotword Variables
    private boolean shouldDetect;
    private SnowboyDetect snowboyDetect;

    static {
        System.loadLibrary("snowboy-detect-android");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Setup Components
        setupViews();
        setupXiaoBaiButton();
        setupAsr();
        setupTts();
        setupNlu();
        setupHotword();

        // TODO: Start Hotword
        startHotword();
    }

    private void setupViews() {
        // TODO: Setup Views
        textView = findViewById(R.id.textview);
        button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
               // textView.setText("Good Morning");
                shouldDetect = false;
            }
        });
    }

    private void setupXiaoBaiButton() {
        String BUTTON_ACTION = "com.gowild.action.clickDown_action";

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BUTTON_ACTION);

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO: Add action to do after button press is detected
               // textView.setText("Good Morning");
                //first event when the button is clicked
                startAsr();
              //  shouldDetect = false;
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void setupAsr() {
        // TODO: Setup ASR

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        //there are other state u can listen to
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                    Log.e("asr", "Error" + Integer.toString(error));
                    startHotword();
            }

            @Override
            public void onResults(Bundle results) {
                    List<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (texts == null || texts.isEmpty()){
                        textView.setText("Please try again");
                }
                else {
                    String text = texts.get(0);
                    textView.setText(text);
                    //third event
                    startNlu(text);
                }
            }

            @Override
            //don't understand
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
    }
//Automatic Speech Recognition
    private void startAsr() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // TODO: Set Language
                final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

                // Stop hotword detection in case it is still running
                shouldDetect = false;

                // TODO: Start ASR
                speechRecognizer.startListening(recognizerIntent);
            }
        };
        Threadings.runInMainThread(this, runnable);
    }
//text to speech
    private void setupTts() {
        // TODO: Setup TTS

        textToSpeech = new TextToSpeech(this, null);
    }

    private void startTts(String text) {
        // TODO: Start TTS

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);

        // TODO: Wait for end and start hotword
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (textToSpeech.isSpeaking()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e("tts", e.getMessage(), e);
                    }
                }
        //fifth event
                startHotword();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }
//natural language understanding
    private void setupNlu() {
        // TODO: Change Client Access Token
        String clientAccessToken = "5e0f7d6c4f7346e4a03463855925e459";
        AIConfiguration aiConfiguration = new AIConfiguration(clientAccessToken,
                AIConfiguration.SupportedLanguages.English);
        aiDataService = new AIDataService(aiConfiguration);
    }

    private void startNlu(final String text) {
        // TODO: Start NLU
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                AIRequest aiRequest = new AIRequest();
                aiRequest.setQuery(text);
                Log.d("testing",text);

                try {
                    AIResponse aiResponse = aiDataService.request(aiRequest);
                    Result result = aiResponse.getResult();
                    Fulfillment fulfillment = result.getFulfillment();
                    String speech = fulfillment.getSpeech();

                    //detected keyword in dialog flow
                    //once asr is completed; it will go to the feedback
                    //check if the text matches to dialog flow
                    //if matches start TTS
                    if (speech.equalsIgnoreCase("feedback_function")){
                        String firstResponse = getFeedback(text);
                        //fourth event. --> will return my reply and output as voice / speech
                        startTts(firstResponse);
                    }

                    else if (speech.equalsIgnoreCase("response_function")){
                        String secondResponse = getResponse(text);
                        startTts(secondResponse);
                    }

                    else if (speech.equalsIgnoreCase("response2_function")){
                        String thirdResponse = getResponse2(text);
                        startTts(thirdResponse);
                    }
                    else {
                        startTts(speech);
                    }
                } catch (AIServiceException e) {
                    Log.e("nlu", e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void setupHotword() {
        shouldDetect = false;
        SnowboyUtils.copyAssets(this);

        // TODO: Setup Model File
        File snowboyDirectory = SnowboyUtils.getSnowboyDirectory();
        File model = new File(snowboyDirectory, "alexa_02092017.umdl");
        File common = new File(snowboyDirectory, "common.res");

        // TODO: Set Sensitivity
        snowboyDetect = new SnowboyDetect(common.getAbsolutePath(), model.getAbsolutePath());
        snowboyDetect.setSensitivity("0.60");
        snowboyDetect.applyFrontend(true);
    }

    private void startHotword() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                shouldDetect = true;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                int bufferSize = 3200;
                byte[] audioBuffer = new byte[bufferSize];
                //take audio record from people
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        16000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                );

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e("hotword", "audio record fail to initialize");
                    return;
                }

                audioRecord.startRecording();
                Log.d("hotword", "start listening to hotword");
                //detected keyword will exit the while loop
                while (shouldDetect) {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    short[] shortArray = new short[audioBuffer.length / 2];

                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);
                    //detecting keyword
                    int result = snowboyDetect.runDetection(shortArray, shortArray.length);

                    if (result > 0 ) {
                        Log.d("hotword", "detected");
                        shouldDetect = false;
                    }
                }
                audioRecord.stop();
                audioRecord.release();
                Log.d("hotword", "stop listening to hotword");

                // TODO: Add action after hotword is detected
                startAsr();
                //repeat ASR again
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private String getFeedback(String text){
        if (text.contains("suggest") || text.contains("feedback") || text.contains("fitbit")) {
            return "Is it a positive or negative experience?";
        }

        else if (text.contains("bad feedback") || text.contains("negative experience") || text.contains("bad experience") || text.contains("bad fitbit")){
            return "Which area has done horribly or badly? Please elaborate.";
        }

        else if (text.contains("good feedback") || text.contains("positive experience") || text.contains("good experience") || text.contains("good fitbit")){
            return "Please let us know which area we done exceptionally well or excellent. Please elaborate.";
        }

        else if (text.contains("no feedback")) {
            return "Thank you.";
        }
        //improve / dry
        return "Has this resulted in a positive or negative experience?";
    }

    private String getResponse(String text){
        if (text.contains("negative") || text.contains("negative experience")) {
            return "Which area has done horribly or badly? Please elaborate.";
        }
        else if (text.contains("positive") || text.contains("positive experience")){
            return "Great! Please let us know which area we done exceptionally well or excellent. Please elaborate.";
        }
        return "Is it a negative or positive experience?";
    }

    private String getResponse2(String text){
        if (text.contains("horrible") || text.contains("horribly")) {
            return "I am sorry. Awarded service rating of 25 for being horrible.";
        }
        else if (text.contains("bad") || text.contains("badly")){
            return "I am sorry. Awarded service rating of 40 for being bad.";
        }
        else if (text.contains("excellent") || text.contains("excellence")) {
            return "Thank you. Awarded service rating of 90 for being excellent!";
        }
        else if (text.contains("good") || text.contains("done well")) {
            return "Thank you. Awarded service rating of 65 for being good!";
        }
        //sparkling / clean / smell / smelly
        return "Please give us a rating whether is it horrible, bad, excellent or good. Thank you.";
    }

/*    private String getWeather() {
        // TODO: (Optional) Get Weather Data via REST API
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url("https://api.data.gov.sg/v1/environment/2-hour-forecast").addHeader("accept","application/json")
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseString = response.body().string();

            JSONObject jsonObject = new JSONObject(responseString);
            JSONArray forecasts = jsonObject.getJSONArray("items").getJSONObject(0).getJSONArray("forecasts");

            for (int i = 0; i < forecasts.length(); i++){
                JSONObject forecastObject = forecasts.getJSONObject(i);
                String area = forecastObject.getString("area");

                //get Clementi area
                if (area.equalsIgnoreCase("clementi")) {
                    String forecast = forecastObject.getString("forecast");
                    return "The weather in clementi is now " + forecast;
                }
            }

        } catch (IOException | JSONException e){
            Log.e("weather", e.getMessage(),e);
        }
        return "No weather hahaha";
    }*/
}
