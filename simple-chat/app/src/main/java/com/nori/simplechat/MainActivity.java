package com.nori.simplechat;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, messages;
    EditText input, keyInput;
    TextView status, degreeView, streakView;
    SharedPreferences prefs;
    final String RULES = "You are Nori, a simple accountability and reflection AI. Be direct, calm, honest and practical. Understand academic goals, commitments, actions, results and recovery. Never invent facts. Discipline is a tool, never humiliation or destructive punishment. Human judgment overrides rigid rules when safety, health or real constraints require it. Help interpret what happened after an action: expected result -> actual result -> deviation -> context -> consequence -> next useful action. Degree 0 normal, 1 notice, 2 correction, 3 structured recovery, 4 serious review, 5 reset. A degree is accountability, not punishment. Never recommend unsafe behavior or sleep deprivation. A daily streak counts consecutive days meeting the defined minimum; approved recovery days do not break it. Explain streak changes. Ask only for genuinely missing information.";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); prefs = getSharedPreferences("nori", MODE_PRIVATE); buildUi();
    }
    TextView tv(String s, int size) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE); t.setPadding(18,14,18,14); return t; }
    void buildUi() {
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.BLACK);
        TextView title=tv("NORI",28); root.addView(title,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout stats=new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL);
        degreeView=tv("Degree: "+prefs.getInt("degree",0),15); streakView=tv("Streak: "+prefs.getInt("streak",0),15);
        stats.addView(degreeView,new LinearLayout.LayoutParams(0,-2,1)); stats.addView(streakView,new LinearLayout.LayoutParams(0,-2,1)); root.addView(stats);
        messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.addView(messages); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bar=new LinearLayout(this); bar.setPadding(8,4,8,8);
        input=new EditText(this); input.setHint("Tell Nori what happened..."); input.setTextColor(Color.WHITE); input.setHintTextColor(Color.GRAY);
        Button send=new Button(this); send.setText("Send"); send.setOnClickListener(v->sendMessage()); bar.addView(input,new LinearLayout.LayoutParams(0,-2,1)); bar.addView(send,new LinearLayout.LayoutParams(-2,-2)); root.addView(bar);
        Button settings=new Button(this); settings.setText("API key / rules"); settings.setOnClickListener(v->showSettings()); root.addView(settings,new LinearLayout.LayoutParams(-1,-2));
        status=tv("Ready",12); root.addView(status); setContentView(root);
        addMessage("Nori", "Simple mode ready. Tell me what happened, what you expected, or what you completed.");
    }
    void addMessage(String who,String text){ TextView t=tv(who+": "+text,16); messages.addView(t); ((ScrollView)messages.getParent()).post(()->((ScrollView)messages.getParent()).fullScroll(View.FOCUS_DOWN)); }
    void sendMessage(){ String q=input.getText().toString().trim(); if(q.isEmpty())return; input.setText(""); addMessage("You",q); status.setText("Thinking..."); new Thread(()->callOpenAI(q)).start(); }
    void callOpenAI(String q){ try{
        String key=prefs.getString("apiKey",""); if(key.isEmpty()) throw new Exception("Add your OpenAI API key in API key / rules first.");
        URL u=new URL("https://api.openai.com/v1/responses"); HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setRequestMethod("POST"); c.setRequestProperty("Authorization","Bearer "+key); c.setRequestProperty("Content-Type","application/json"); c.setDoOutput(true);
        JSONObject body=new JSONObject(); body.put("model",prefs.getString("model","gpt-5.6-luna")); body.put("instructions",RULES); JSONArray in=new JSONArray(); in.put(new JSONObject().put("role","user").put("content",q)); body.put("input",in);
        OutputStream os=c.getOutputStream(); os.write(body.toString().getBytes(StandardCharsets.UTF_8)); os.close();
        InputStream is=c.getResponseCode()<400?c.getInputStream():c.getErrorStream(); String r=read(is); if(c.getResponseCode()>=400) throw new Exception(r);
        JSONObject o=new JSONObject(r); String reply=o.optString("output_text",""); if(reply.isEmpty()) reply="I received the response but could not extract its text.";
        String finalReply=reply; runOnUiThread(()->{ addMessage("Nori",finalReply); status.setText("Ready"); updateLocalLogic(q); });
    }catch(Exception e){ String m=e.getMessage()==null?"Request failed":e.getMessage(); runOnUiThread(()->{addMessage("Nori",m); status.setText("Error");}); }}
    String read(InputStream is)throws Exception{ BufferedReader br=new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8)); StringBuilder s=new StringBuilder(); String x; while((x=br.readLine())!=null)s.append(x); return s.toString(); }
    void updateLocalLogic(String q){ String s=q.toLowerCase(Locale.US); int d=prefs.getInt("degree",0); int st=prefs.getInt("streak",0); if(s.contains("completed")||s.contains("done")||s.contains("finished")){ st++; if(d>0)d--; } else if(s.contains("missed")||s.contains("failed")||s.contains("didn't")||s.contains("did not")){ d=Math.min(5,d+1); st=0; } prefs.edit().putInt("degree",d).putInt("streak",st).apply(); degreeView.setText("Degree: "+d); streakView.setText("Streak: "+st); }
    void showSettings(){ final LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); keyInput=new EditText(this); keyInput.setHint("OpenAI API key"); keyInput.setText(prefs.getString("apiKey","")); keyInput.setInputType(129); box.addView(keyInput); EditText model=new EditText(this); model.setHint("Model"); model.setText(prefs.getString("model","gpt-5.6-luna")); box.addView(model); new AlertDialog.Builder(this).setTitle("Nori settings").setView(box).setPositiveButton("Save",(d,w)->prefs.edit().putString("apiKey",keyInput.getText().toString().trim()).putString("model",model.getText().toString().trim()).apply()).setNegativeButton("Cancel",null).show(); }
}
