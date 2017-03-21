package com.chenfei.exview;

import android.util.Pair;

import com.chenfei.exview.internal.Exclusion;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by MrFeng on 2017/3/20.
 */
public class ExcludeRules {
    private static final Map<String, Pair<String, String>> packageName = new HashMap<>();

    static {
        exclude("dalvik.system.", "SystemApi", "this is Android System api.");
        exclude("com.android.", "SystemApi", "this is Android System api.");
        exclude("android.", "SystemApi", "this is Android System api.");
        exclude("java.", "SystemApi", "this is Android System api.");

        exclude("com.bumptech.", "Glide", "this is Glide api.");
        exclude("com.google.gson.", "Gson", "this is Json serialization api.");
        exclude("okhttp.", "OkHttp2", "this is http network api.");
        exclude("okhttp3.", "OkHttp3", "this is http network api.");
        exclude("retrofit.", "Retrofit", "this is http network api.");
        exclude("retrofit2.", "Retrofit2", "this is http network api.");
        exclude("rx.", "RxJava 1.0", "this is RxJava api.");
        exclude("io.reactivex.", "RxJava 2.0", "this is RxJava api.");
    }

    public static void exclude(String packageName, String name, String reason) {
        ExcludeRules.packageName.put(packageName, Pair.create(name, reason));
    }

    public static Exclusion isExclude(StackTraceElement element) {
        for (Map.Entry<String, Pair<String, String>> stringPairEntry : packageName.entrySet()) {
            if (element.getClassName().startsWith(stringPairEntry.getKey())) {
                return new Exclusion(stringPairEntry.getKey(),
                        stringPairEntry.getValue().first, stringPairEntry.getValue().second);
            }
        }
        return null;
    }
}
