package com.nori.kage.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;

@CapacitorPlugin(name="NoriVoice")
public final class NoriVoicePlugin extends Plugin {
    @Override public void load() { NoriVoiceService.setPluginBridge(this); }

    public static void emitFromService(String event, JSObject data) {
        if (instance != null) instance.notifyListeners(event, data);
    }

    private static NoriVoicePlugin instance;
    { instance = this; }

    @PluginMethod public void start(PluginCall call) {
        Intent i=new Intent(getContext(),NoriVoiceService.class)
            .setAction(NoriVoiceService.ACTION_START)
            .putExtra("wakeWord",call.getString("wakeWord","nori"))
            .putExtra("languageTag",call.getString("language","en-US"))
            .putExtra("doubleClap",call.getBoolean("doubleClap",true));
        try {
            if(Build.VERSION.SDK_INT>=26)getContext().startForegroundService(i); else getContext().startService(i);
            call.resolve();
        } catch(Exception e) { call.reject(e.getMessage()==null?"voice_start_failed":e.getMessage()); }
    }

    @PluginMethod public void stop(PluginCall call) {
        getContext().stopService(new Intent(getContext(),NoriVoiceService.class));
        call.resolve();
    }

    @PluginMethod public void speak(PluginCall call) {
        Intent i=new Intent(getContext(),NoriVoiceService.class)
            .setAction(NoriVoiceService.ACTION_SPEAK)
            .putExtra("text",call.getString("text",""));
        try {
            if(Build.VERSION.SDK_INT>=26)getContext().startForegroundService(i); else getContext().startService(i);
            call.resolve();
        } catch(Exception e) { call.reject(e.getMessage()==null?"voice_speak_failed":e.getMessage()); }
    }

    @PluginMethod public void openWorkspace(PluginCall call) {
        call.resolve(new JSObject().put("ok",true));
    }
}