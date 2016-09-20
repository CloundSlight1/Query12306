package com.wuyz.query12306;

import android.app.Application;

/**
 * Created by wuyz on 9/20/2016.
 */

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThreadExecutor.initExecutorService();
        try {
            Utils.initHttps();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
