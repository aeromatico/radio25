package com.app.classsicradio.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import androidx.media3.common.util.UnstableApi;
import com.app.classsicradio.utils.Utilities;

@UnstableApi
public class RadioManager {

    private static RadioManager instance = null;
    private static RadioService service;
    private boolean serviceBound;

    private RadioManager() {
        serviceBound = false;
    }

    public static synchronized RadioManager with() {
        if (instance == null) {
            instance = new RadioManager();
        }
        return instance;
    }

    public static RadioService getService() {
        return service;
    }

    public void playOrPause(String streamUrl) {
        if (service != null) {
            if (streamUrl == null) {
                service.stop();
            } else {
                service.playOrPause(streamUrl);
            }
        }
    }

    public void stopServices() {
        if (service != null) {
            service.stop();
        }
    }

    public void destroyServices() {
        if (service != null) {
            service.onDestroy();
        }
    }

    public boolean isPlaying() {
        return service != null && service.isPlaying();
    }

    public void bind(Context context) {
        if (!serviceBound) {
            Intent intent = new Intent(context, RadioService.class);
            context.startService(intent);
            boolean bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            if (bound) {
                serviceBound = true;
            }
        }
    }

    public void unbind(Context context) {
        if (serviceBound) {
            try {
                context.unbindService(serviceConnection);
                context.stopService(new Intent(context, RadioService.class));
                serviceBound = false;
            } catch (IllegalArgumentException e) {
                Log.e("RadioManager", "Error unbinding service: " + e.getMessage());
            }
        }
    }

    public boolean isBound() {
        return serviceBound;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((RadioService.LocalBinder) binder).getService();
            serviceBound = true;
            if (service != null) {
                Utilities.onEvent(service.getStatus());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
}