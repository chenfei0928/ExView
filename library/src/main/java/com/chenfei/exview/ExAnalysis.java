package com.chenfei.exview;

import android.app.PendingIntent;
import android.content.Context;
import android.os.SystemClock;

import com.chenfei.exview.internal.CanaryLog;
import com.chenfei.exview.internal.DirectoryProvider;
import com.chenfei.exview.internal.DisplayLeakActivity;
import com.chenfei.exview.internal.ExViewInternals;
import com.chenfei.exview.internal.ThrowableInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Locale;

/**
 * Created by MrFeng on 2017/3/20.
 */
public class ExAnalysis {
    private static Context sContext;
    private static DirectoryProvider sLeakDirectoryProvider;

    public static void init(Context context) {
        sContext = context.getApplicationContext();
    }

    public static DirectoryProvider getLeakDirectoryProvider() {
        if (sLeakDirectoryProvider == null)
            sLeakDirectoryProvider = new DirectoryProvider(sContext);
        return sLeakDirectoryProvider;
    }

    public static void holderEx(Throwable t) {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        String title = String.format(Locale.getDefault(), "%s.%s(%s:%d)",
                callerClazzName, caller.getMethodName(),
                caller.getFileName(), caller.getLineNumber());

        holderEx(title, t);
    }

    public static void holderEx(String tag, Throwable t) {
        File resultFile = getLeakDirectoryProvider().newHeapDumpFile();
        boolean resultSaved = false;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(resultFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(new ThrowableInfo(tag, t));
            resultSaved = true;
        } catch (IOException e) {
            CanaryLog.d(e, "Could not save leak analysis result to disk.");
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
        PendingIntent pendingIntent;
        String contentTitle;
        String contentText;


        if (resultSaved) {
            pendingIntent = DisplayLeakActivity.createPendingIntent(sContext, resultFile.getName());

            contentTitle = sContext.getString(R.string.exview_has_ex, tag, t.getMessage());
            contentText = sContext.getString(R.string.exview_notification_message);
        } else {
            contentTitle = sContext.getString(R.string.exview_could_not_save_title);
            contentText = sContext.getString(R.string.exview_could_not_save_text);
            pendingIntent = null;
        }
        // New notification id every second.
        int notificationId = (int) (SystemClock.uptimeMillis() / 1000);
        ExViewInternals.showNotification(sContext, contentTitle, contentText, pendingIntent, notificationId);
    }
}
