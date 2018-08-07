package sg.gowild.sademo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

import android.os.AsyncTask;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.onesignal.OneSignal;
import android.os.StrictMode;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    // URL Address
    //link for diagnosis
    String url = "https://www.healthline.com/symptom/";
    //link for describing something
    String describeText;
    String describeUrl = "https://en.wikipedia.org/wiki/";
    ProgressDialog mProgressDialog;
    // View Variables
    private Button button;
    private Button reminder;
    private Button patientlog;
    private TextView textView;
    private TextView symptomtext;
    private TextView currentStatetext;

    // ASR Variables
    private SpeechRecognizer speechRecognizer;

    // TTS Variables
    private TextToSpeech textToSpeech;

    // NLU Variables
    private AIDataService aiDataService;

    // Hotword Variables
    private boolean shouldDetect;
    private SnowboyDetect snowboyDetect;

    private boolean StillReading = false;
    private boolean HotWordReading = false;
    private DialogState CurrentState = DialogState.IDLE;
    private String ErrorText = "";

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference databaseRef = database.getReference();

    private int UserCount = 0;

    //Setting the tags for Current User.
    Switch careGiverSwitch;
    //OneSignal.sendTag("User_ID", LoggedIn_User_Email);

    List<Date> list_reminder;
    List<String> list_reminder_info;

    enum DialogState
    {
        IDLE,
        ENQUIRING,
        ENQUIRE_FINISH,
        ERROR,
        DIALOGSTATE_NO
    }

    static boolean firstrun = true;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            checkTime();
            handler.postDelayed(this, 6000);

        }
    };

    static {
        System.loadLibrary("snowboy-detect-android");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(firstrun)
            handler.postDelayed(runnable, 6000);
        firstrun = false;

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

        list_reminder = new ArrayList<Date>();
        list_reminder_info = new ArrayList<String>();

        //notification when startup
/*        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        cal.add(Calendar.SECOND, 5);
        Calendar cal = Calendar.getInstance();

        Intent notificationIntent = new Intent(this, Notification_Receiver.class);
        PendingIntent broadcast = PendingIntent.getBroadcast(this, 100, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), broadcast);*/

        // OneSignal Initialization
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        //firebase
        Query UsersQuery = databaseRef.child("users").orderByKey();
        UsersQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Usera> UserList = new ArrayList<Usera>();
                UserCount = 0;
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren())
                {
                    Usera newUser = new Usera();
                    newUser.SetNewUsera(postSnapshot);
                    UserList.add(newUser);
                    UserCount++;

                    //add the reminder to the list
                    if (newUser.Name.equalsIgnoreCase("Patient1"))
                    {
                        for (sg.gowild.sademo.Log tempLog : newUser.ReminderLogList)
                        {
                            list_reminder.add(tempLog.DateTime);
                            list_reminder_info.add(tempLog.Information);
                        }
                        SortReminder();
                        //t.start();
                    }
                }
                for (Usera temp : UserList) {
                    Log.v("User", temp.Name + " : " + temp.State);
                    //String note = ()
                    Log.v("Log", "" + temp.ReminderLogList.get(0).DateTime_s);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void  SortReminder()
    {
        int n = list_reminder.size();
        for (int i = 0; i < n-1; i++) {
            for (int j = 0; j < n - i - 1; j++){
                if (list_reminder.get(j).after(list_reminder.get(j + 1))) {
                    // swap temp and arr[i]
                    Date temp = list_reminder.get(j);
                    String temp_info = list_reminder_info.get(j);
                    list_reminder.set(j, list_reminder.get(j + 1));
                    list_reminder.set(j + 1, temp);

                    list_reminder_info.set(j, list_reminder_info.get(j + 1));
                    list_reminder_info.set(j + 1, temp_info);
                }
            }
        }
        for (Date tempDate : list_reminder)
        {
            android.util.Log.d("date",tempDate.toString());
        }
    }

    Thread t=new Thread(){
        @Override
        public void run(){

            //while(!isInterrupted()){

                try {
                    Thread.sleep(5000);  //1000ms = 1 sec

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            checkTime();
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        //}
    };



    private void setupViews() {
        // TODO: Setup Views
        textView = findViewById(R.id.textview);
        currentStatetext = findViewById(R.id.currentState);
        button = findViewById(R.id.button);
        reminder = findViewById(R.id.Reminder);
        patientlog = findViewById(R.id.PatientLog);
        //button2 = findViewById(R.id.button2);
        symptomtext = findViewById(R.id.symptomtext);
        careGiverSwitch = findViewById(R.id.careGiverButton);

        careGiverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b)
                {
                    Toast.makeText(getBaseContext(), "Caregiver mode", Toast.LENGTH_SHORT).show();
                    OneSignal.sendTag("CareGiver", "true");
                }
                else
                {
                    Toast.makeText(getBaseContext(), "AI mode", Toast.LENGTH_SHORT).show();
                    OneSignal.sendTag("CareGiver", "false");
                }
            }
        });

       /* button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){


            }
        });*/

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //if (!StillReading)
                //    shouldDetect = false;
                //**********************************************************************************
                //input to database

                Map date = new HashMap();
                DateFormat dateFormat = new SimpleDateFormat("dd, MM, yyyy");
                DateFormat timeFormat = new SimpleDateFormat("HH:mm");
                date.put(dateFormat.format(Calendar.getInstance().getTime()), timeFormat.format(Calendar.getInstance().getTime()));

                String key = String.valueOf(UserCount + 1);//databaseRef.child("users").push().getKey();
                Usera newUser = new Usera("Patient","healthy");
                newUser.AddReminderLog("Please Take XYZ medicine");

                HashMap<String, Object> result = new HashMap<>();
                result.put("Name", newUser.Name);
                result.put("State", newUser.State);
                result.put("Reminder Log", newUser.ReminderLogList);


                Map<String, Object> UserValues = result;

                HashMap<String, Object> TobeAdded = new HashMap<>();
                TobeAdded.put("/users/" + key, UserValues);

                databaseRef.updateChildren(TobeAdded);
            }
        });

        reminder.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(MainActivity.this, ReminderActivity.class));

            }
        });

        patientlog.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(MainActivity.this, PatientLogActivity.class));
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
                shouldDetect = false;

            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private void setupAsr() {
        // TODO: Setup ASR
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

                Log.e("asr", "Ready For Speech");
            }

            @Override
            public void onBeginningOfSpeech() {

                Log.e("asr", "Beginning Of Speech");
            }

            @Override
            public void onRmsChanged(float v) {

                Log.e("asr", "Rms Changed");
            }

            @Override
            public void onBufferReceived(byte[] bytes) {

                Log.e("asr", "Buffer Received");
            }

            @Override
            public void onEndOfSpeech() {
                if (CurrentState != DialogState.ENQUIRING){
                    resetURL();
                }

                StillReading = true;
                Log.e("asr", "End Of Speech");

                //startHotword();
            }

            @Override
            public void onError(int error) {
                Log.e("asr", "Error: " + Integer.toString(error));

                ErrorText = "";
                switch (error)
                {
                    case 6:
                        ErrorText = "Bad signal quality.";
                        break;
                    case 7:
                        ErrorText = "Too much background noise at the start.";
                        break;

                    default:
                        ErrorText = "Unknown Error occurred.";
                        break;
                }

                ErrorText += " Please try again";
                textView.setText(ErrorText);
                startNlu(ErrorText);
                //resetURL();
            }

            @Override
            public void onResults(Bundle results) {
                Log.e("asr", "On Result");
                List<String> texts = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if(texts == null || texts.isEmpty()){
                    textView.setText("Please try again");
                }
                else {
                    String text = texts.get(0);
                    textView.setText(text);

                    currentStatetext.setText(CurrentState.toString());

                    // Execute Title AsyncTask
                    if (CurrentState == DialogState.ENQUIRING && (!text.contains("done") ||!text.contains("no") ))
                        url += text + "/";
                    symptomtext.setText(url);
                    //new PossibleCondition().execute();

                    //String response;
                    /*if(text.equalsIgnoreCase("hello")){
                        response = "hi there";
                    }
                    else {
                        response = "APACHE HELICOPTER";
                    }
                    startTts(response); */

                    //NLU
                    startNlu(text);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

                Log.e("asr", "Partial Result");
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

                Log.e("asr", "Event");
            }
        });
    }

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

    private void setupTts() {
        // TODO: Setup TTS
        textToSpeech = new TextToSpeech(this, null);
    }

    public void startTts(String text) {
        // TODO: Start TTS
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH,null);
        // TODO: Wait for end and start hotword
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (textToSpeech.isSpeaking()) {
                    StillReading = true;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e("tts", e.getMessage(), e);
                    }
                }
                StillReading = false;
                startHotword();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    private void setupNlu() {
        // TODO: Change Client Access Token
        String clientAccessToken = "6ec2f6b724894c008489105c0b506c75";
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

                try {
                    AIResponse aiResponse = aiDataService.request(aiRequest);
                    Result result = aiResponse.getResult();
                    Fulfillment fulfillment = result.getFulfillment();
                    String speech = fulfillment.getSpeech();

                    if (speech.equalsIgnoreCase("weather_function")){
                        String weatherResponse = getWeather();
                        startTts(weatherResponse);
                    }
                    if(speech.contains("Describe_Function")){
                        String[] parts = speech.split("-");
                        String symptom = parts[1];
                        symptom = symptom.replace(" ", "_");
                        describeText = symptom;
                        new DescribeSymptom().execute();
                    }
                    else if(speech.equalsIgnoreCase("diagnosis_function")){
                        startTts("what symptom do you have");
                        CurrentState = DialogState.ENQUIRING;
                    }
                    else if(speech.equalsIgnoreCase("Finish_Asking") && CurrentState == DialogState.ENQUIRING){
                        CurrentState = DialogState.ENQUIRE_FINISH;
                        //if (CurrentState == DialogState.ENQUIRE_FINISH)
                            new PossibleCondition().execute();
                    }
                    else if(speech.equalsIgnoreCase("Error")){
                        startTts(ErrorText);
                        CurrentState = DialogState.ERROR;
                    }
                    else if (CurrentState == DialogState.ENQUIRING)
                    {
                        startTts("Are there any other symptoms?");
                    }
                    else if (speech.equalsIgnoreCase("send_help_function"))
                    {
                        startTts("We have notified the caregiver, please remain calm.");
                        //send help to caregiver
                        sendNotification();
                    }
                    else if (speech.equalsIgnoreCase("next_reminder_function"))
                    {
                        startTts("your next reminder is at " + DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(list_reminder.get(0)) + ". You are to " + list_reminder_info.get(0)); //+ string get_Next_Reminder();
                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                        Intent notificationIntent = new Intent(MainActivity.this, Notification_Receiver.class);
                        PendingIntent broadcast = PendingIntent.getBroadcast(MainActivity.this, 100, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.SECOND, 5);
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), broadcast);

                    }
                    else
                    {
                        //
                        startTts(speech);
                    }


                } catch (AIServiceException e) {
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
                if (HotWordReading)
                    return;
                shouldDetect = true;
                HotWordReading = true;
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                int bufferSize = 3200;
                byte[] audioBuffer = new byte[bufferSize];
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

                while (shouldDetect) {
                    audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    short[] shortArray = new short[audioBuffer.length / 2];
                    ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray);

                    if (CurrentState == DialogState.ENQUIRING || CurrentState == DialogState.ERROR)
                    {
                        shouldDetect = false;
                    }

                    int result = snowboyDetect.runDetection(shortArray, shortArray.length);
                    if (result > 0) {
                        Log.d("hotword", "detected");
                        shouldDetect = false;
                    }
                }

                audioRecord.stop();
                audioRecord.release();
                Log.d("hotword", "stop listening to hotword");

                HotWordReading = false;
                // TODO: Add action after hotword is detected
                startAsr();
            }
        };
        Threadings.runInBackgroundThread(runnable);
    }

    // Send Notification to caregiver
    private void sendNotification() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                int SDK_INT = android.os.Build.VERSION.SDK_INT;
                if (SDK_INT > 8) {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                            .permitAll().build();
                    StrictMode.setThreadPolicy(policy);
                    String send_email;

                    //This is a Simple Logic to Send Notification different Device Programmatically....
                    if (careGiverSwitch.isChecked()) {
                        send_email = "false";
                    } else {
                        send_email = "true";
                    }

                    try {
                        String jsonResponse;

                        URL url = new URL("https://onesignal.com/api/v1/notifications");
                        HttpURLConnection con = (HttpURLConnection)url.openConnection();
                        con.setUseCaches(false);
                        con.setDoOutput(true);
                        con.setDoInput(true);

                        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        con.setRequestProperty("Authorization", "Basic YzJiMWYyYTAtN2YyNy00YjRjLTk5YjktOGU1MTQ3ZDJlZTNk");
                        con.setRequestMethod("POST");

                        String strJsonBody = "{"
                                + "\"app_id\": \"dd9441bb-8e13-4ed0-80f2-fc72db3e94bf\","

                                + "\"filters\": [{\"field\": \"tag\", \"key\": \"CareGiver\", \"relation\": \"=\", \"value\": \"" + send_email + "\"}],"

                                + "\"data\": {\"foo\": \"bar\"},"
                                + "\"contents\": {\"en\": \"Your patient needs help\"}"
                                + "}";


                        System.out.println("strJsonBody:\n" + strJsonBody);

                        byte[] sendBytes = strJsonBody.getBytes("UTF-8");
                        con.setFixedLengthStreamingMode(sendBytes.length);

                        OutputStream outputStream = con.getOutputStream();
                        outputStream.write(sendBytes);

                        int httpResponse = con.getResponseCode();
                        System.out.println("httpResponse: " + httpResponse);

                        if (httpResponse >= HttpURLConnection.HTTP_OK
                                && httpResponse < HttpURLConnection.HTTP_BAD_REQUEST) {
                            Scanner scanner = new Scanner(con.getInputStream(), "UTF-8");
                            jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                            scanner.close();
                        } else {
                            Scanner scanner = new Scanner(con.getErrorStream(), "UTF-8");
                            jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                            scanner.close();
                        }
                        System.out.println("jsonResponse:\n" + jsonResponse);

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        });
    }

    // DescribeSymptom AsyncTask ================================================================================================
    private class DescribeSymptom extends AsyncTask<Void, Void, Void>{
        String desc = "";

/*        public DescribeSymptom(String text) {
            super();
            tempURL = describeUrl + text;
            TextView txtdesc = (TextView) findViewById(R.id.symptomtext);
            txtdesc.setText(tempURL);
        }*/

        @Override
        protected Void doInBackground(Void... params) {
            try{
                Document doc = Jsoup.connect(describeUrl + describeText).get();
                Elements paragraphs = doc.select("p:not(:has(#coordinates))");
                Element firstParagraph = paragraphs.first();
                desc = firstParagraph.text();
                desc = desc.replaceAll("\\[.*?\\]","");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Set description into TextView
            describeText = "";
            TextView txtdesc = (TextView) findViewById(R.id.symptomtext);
            txtdesc.setText(desc);
            startTts(desc);
        }
    }

    // PossibleCondition AsyncTask ================================================================================================
    private class PossibleCondition extends AsyncTask<Void, Void, Void> {
        String desc = "";

        /*@Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setTitle("XiaoBai Care");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }*/

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Connect to the web site
                Document document = Jsoup.connect(url).get();
                // Using Elements to get the Meta data
                Elements description = document.getElementsByClass("css-y1gt6f").select("h2");
/*                for(Element h2 : description) {
                    desc += h2.text() + ", ";
                }*/
                for (int i = 0; i < description.size(); i++) {
                    if (i < 5) {
                        Element h2 = description.get(i);
                        desc += h2.text() + ", ";
                    }
                    else {
                        desc =   desc.substring(0, desc.length() - 2);
                        desc += ".";
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Set description into TextView
            TextView txtdesc = (TextView) findViewById(R.id.symptomtext);
            desc = desc.replace("What Do You Want to Know About ", "").
                        replace("Everything You Want to Know About ", "").
                        replace(" Overview", "").
                        replace("Everything You Need to Know About ", "").
                        replace("?", "");
            if (desc != "")
                desc = "possible conditions may include: " + desc;
            else
                desc = "I cannot find the possible conditions, please try again.";
            txtdesc.setText(desc);
            startTts(desc);
            //mProgressDialog.dismiss();
        }
    }

    //Reset URL======================================================================================================
    private void resetURL() {
        url = "https://www.healthline.com/symptom/";
    }

    //Get Weather=====================================================================================================
    private String getWeather() {
        // TODO: (Optional) Get Weather Data via REST API
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.data.gov.sg/v1/environment/2-hour-weather-forecast")
                .addHeader("accept", "application/json")
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseString = response.body().string();

            JSONObject jsonObject = new JSONObject(responseString);
            JSONArray forecasts = jsonObject.getJSONArray("items")
                    .getJSONObject(0)
                    .getJSONArray("forecasts");

            for (int i = 0; i < forecasts.length(); i++)
            {
                JSONObject forecastObject = forecasts.getJSONObject(i);
                String area = forecastObject.getString("area");

                if(area.equalsIgnoreCase("Ang Mo Kio")) {
                    String forecast = forecastObject.getString("forecast");
                    return "The weather in Ang Mo Kio is " + forecast;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "No weather info feels bad man";
    }


    private void checkTime() {

        Date todayDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        //if(sdf.format(todayDate).equals(sdf.format(list_reminder.get(0))))
            //startTts(list_reminder_info.get(0));

        Date tdy = todayDate;
        for (int i = 0; i < list_reminder.size(); i++)
        {
               // hh:mm");
            if(list_reminder.get(i).getTime() <= tdy.getTime())
                startTts(list_reminder_info.get(i));
        }
    }
}
