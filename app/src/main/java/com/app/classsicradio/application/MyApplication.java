package com.app.classsicradio.application;

import android.content.Context;
import androidx.multidex.MultiDex;
import com.app.classsicradio.Config;
import app.utils.BaseApplication;
import app.utils.tools.ApiKeyProvider;

public class MyApplication extends BaseApplication implements ApiKeyProvider {


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public String getApiKey() {
        return Config.APP_ACCESS_KEY;
    }

    @Override
    public String getAppPackageName() {
        return getPackageName();
    }
}
