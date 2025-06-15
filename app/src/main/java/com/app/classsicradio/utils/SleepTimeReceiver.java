package com.app.classsicradio.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.media3.common.util.UnstableApi;

@UnstableApi
public class SleepTimeReceiver extends BroadcastReceiver {

    Preferences sharedPref;

    @Override
    public void onReceive(Context context, Intent intent) {

        sharedPref = new Preferences(context);

        if (sharedPref.getIsSleepTimeOn()) {
            sharedPref.setSleepTime(false, 0,0);
        }
    }
}
