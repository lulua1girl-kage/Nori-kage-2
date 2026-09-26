package com.nori.kage.voice;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import java.util.ArrayList;
import java.util.Locale;

public final class NoriVoiceService extends Service implements RecognitionListener {
    public static final String ACTION_START="com.nori.kage.voice.START";
    public static final String ACTION_STOP="com.nori.kage.voice.STOP";
    public static final String ACTION_SPEAK="com.nori.kage.voice.SPEAK";
    private static final String CHANNEL="nori_voice";
    private static final int NOTIFICATION_ID=7411;
    private static final long MAX_SESSION_MS=30L*60L*1000L;
    private static volatile Plugin plugin;

    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean running,active,listening,doubleClap=true;
    private String wakeWord="nori",language="en-US";
    private long lastClapAt=0L;
    private int clapCount=0;
    private float rmsFloor=1.5f;
    private final Runnable expiry=()->stopVoice();

    public static void setPluginBridge(Plugin p){plugin=p;}

    @Override public void onCreate(){
        super.onCreate(); createChannel();
        tts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS)tts.setLanguage(Locale.US);});
    }

    @Override public int onStartCommand(Intent in,int flags,int id){
        if(in==null)return START_NOT_STICKY;
        String a=in.getAction();
        if(ACTION_STOP.equals(a)){stopVoice();return START_NOT_STICKY;}
        if(ACTION_SPEAK.equals(a)){
            if(!running){
                try{
                    if(Build.VERSION.SDK_INT>=29)ServiceCompat.startForeground(this,NOTIFICATION_ID,buildNotification("Nori is speaking"),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
                    else startForeground(NOTIFICATION_ID,buildNotification("Nori is speaking"));
                }catch(Exception ignored){}
            }
            speak(in.getStringExtra("text"));
            if(!running)handler.postDelayed(this::stopSelf,4000);
            return START_NOT_STICKY;
        }
        if(ACTION_START.equals(a)){
            String w=in.getStringExtra("wakeWord"); if(w!=null&&!w.trim().isEmpty())wakeWord=w.trim().toLowerCase(Locale.ROOT);
            String lang=in.getStringExtra("languageTag"); if(lang!=null&&!lang.trim().isEmpty())language=lang.trim();
            doubleClap=in.getBooleanExtra("doubleClap",true);
            startVoice();
        }
        return START_NOT_STICKY;
    }

    private void startVoice(){
        if(running){emit("state","already-listening");return;}
        running=true;active=false;clapCount=0;lastClapAt=0L;
        try{
            Notification n=buildNotification("Listening for “Nori” — tap STOP to end");
            if(Build.VERSION.SDK_INT>=29)ServiceCompat.startForeground(this,NOTIFICATION_ID,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            else startForeground(NOTIFICATION_ID,n);
            handler.removeCallbacks(expiry);handler.postDelayed(expiry,MAX_SESSION_MS);
            listenRound();emit("state","listening");
        }catch(Exception e){emit("state","error:"+String.valueOf(e.getMessage()));stopVoice();}
    }

    private void listenRound(){
        if(!running||listening)return;
        if(!SpeechRecognizer.isRecognitionAvailable(this)){emit("state","speech-unavailable");return;}
        try{
            if(recognizer!=null)recognizer.destroy();
            recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
            Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,language)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3)
                .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
            listening=true;recognizer.startListening(i);
        }catch(Exception e){listening=false;handler.postDelayed(this::listenRound,900);}
    }

    private void restartRound(){if(running)handler.postDelayed(this::listenRound,350);}

    private boolean isWake(String s){
        return s.equals(wakeWord)||s.startsWith(wakeWord+" ")||s.contains(" "+wakeWord+" ");
    }

    private void handleText(String raw){
        if(raw==null)return;
        String text=raw.trim();if(text.isEmpty())return;
        String lower=text.toLowerCase(Locale.ROOT);
        if(!active){if(isWake(lower))activate(text);}
        else{
            emit("speech",text);
            if(lower.equals("nori stop")||lower.equals("nori shut down")||lower.equals("nori shutdown")||lower.equals("nori close")){
                speak("Voice mode stopped.");stopVoice();
            }
        }
    }

    private void activate(String heard){
        active=true;emit("wakeWord",heard);emit("state","active");
        speak("I'm listening.");showNotification("Nori is listening — speak now");
    }

    private void detectDoubleClap(float rms){
        if(!doubleClap||active||!running)return;
        if(rms>8.0f&&rms>rmsFloor+5.0f){
            long now=System.currentTimeMillis();
            if(now-lastClapAt<800L&&now-lastClapAt>90L)clapCount++;else clapCount=1;
            lastClapAt=now;
            if(clapCount>=2){clapCount=0;activate("double clap");}
        }else if(rms>0&&rms<rmsFloor){
            rmsFloor=(rmsFloor*.97f)+(rms*.03f);
        }else if(rms>0&&rmsFloor<4f){
            rmsFloor=(rmsFloor*.995f)+(rms*.005f);
        }
    }

    private void emit(String event,String text){
        if(plugin==null)return;
        try{JSObject d=new JSObject();d.put("text",text==null?"":text);NoriVoicePlugin.emitFromService(event,d);}catch(Exception ignored){}
    }

    private void speak(String text){
        if(text==null||text.trim().isEmpty())return;
        if(tts!=null)tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"nori");
    }

    private void showNotification(String text){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm!=null)nm.notify(NOTIFICATION_ID,buildNotification(text));
    }

    private Notification buildNotification(String text){
        Intent launch=getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent content=launch==null?null:PendingIntent.getActivity(this,1,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stop=new Intent(this,NoriVoiceStopReceiver.class);
        PendingIntent stopPi=PendingIntent.getBroadcast(this,2,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=new Notification.Builder(this,CHANNEL)
            .setContentTitle("Nori Voice").setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"STOP",stopPi).build());
        if(content!=null)b.setContentIntent(content);
        return b.build();
    }

    private void createChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(nm!=null)nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Nori Voice",NotificationManager.IMPORTANCE_LOW));
        }
    }

    public void stopVoice(){
        running=false;active=false;listening=false;handler.removeCallbacks(expiry);
        if(recognizer!=null){try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}recognizer=null;}
        try{if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_REMOVE);else stopForeground(true);}catch(Exception ignored){}
        stopSelf();emit("state","stopped");
    }

    @Override public void onDestroy(){stopVoice();if(tts!=null){try{tts.stop();tts.shutdown();}catch(Exception ignored){}tts=null;}super.onDestroy();}
    @Nullable @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onReadyForSpeech(Bundle p){}
    @Override public void onBeginningOfSpeech(){}
    @Override public void onRmsChanged(float rms){detectDoubleClap(rms);}
    @Override public void onBufferReceived(byte[] b){}
    @Override public void onEndOfSpeech(){listening=false;restartRound();}
    @Override public void onError(int e){listening=false;restartRound();}
    @Override public void onResults(Bundle r){
        listening=false;
        ArrayList<String>a=r==null?null:r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(a!=null&&!a.isEmpty())handleText(a.get(0));restartRound();
    }
    @Override public void onPartialResults(Bundle r){
        ArrayList<String>a=r==null?null:r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(a==null||a.isEmpty())return;
        String p=a.get(0);
        if(active)emit("speech",p);else if(isWake(p.toLowerCase(Locale.ROOT)))activate(p);
    }
    @Override public void onEvent(int eventType,Bundle params){}
}