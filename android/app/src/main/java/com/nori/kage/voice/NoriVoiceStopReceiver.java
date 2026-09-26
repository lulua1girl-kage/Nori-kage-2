package com.nori.kage.voice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class NoriVoiceStopReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context,NoriVoiceService.class));
    }
}