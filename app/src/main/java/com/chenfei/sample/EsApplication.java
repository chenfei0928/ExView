package com.chenfei.sample;

import android.app.Application;

import com.chenfei.exview.ExAnalysis;

/**
 * Created by MrFeng on 2017/3/21.
 */
public class EsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ExAnalysis.init(this);
    }
}
