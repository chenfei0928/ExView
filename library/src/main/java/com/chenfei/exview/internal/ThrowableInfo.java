package com.chenfei.exview.internal;

import java.io.File;
import java.io.Serializable;

/**
 * Created by MrFeng on 2017/3/20.
 */
public class ThrowableInfo implements Serializable {
    private Throwable throwable;
    private String tag;
    private transient File file;

    public ThrowableInfo() {
    }

    public ThrowableInfo(String tag, Throwable throwable) {
        this.tag = tag;
        this.throwable = throwable;
    }

    String getTag() {
        return tag;
    }

    Throwable getThrowable() {
        return throwable;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
