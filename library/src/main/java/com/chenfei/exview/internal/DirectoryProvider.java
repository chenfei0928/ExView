package com.chenfei.exview.internal;

import android.content.Context;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class DirectoryProvider {
    private static final String EX_SUFFIX = ".ex";
    private final Context context;

    public DirectoryProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<File> listFiles() {
        return Arrays.asList(appStorageDirectory().listFiles());
    }

    public File newHeapDumpFile() {

        File storageDirectory = appStorageDirectory();
        if (!directoryWritableAfterMkdirs(storageDirectory)) {
            CanaryLog.d("Could not create heap dump directory in app storage: [%s]",
                    storageDirectory.getAbsolutePath());
            return null;
        }
        // If two processes from the same app get to this step at the same time, they could both
        // create a heap dump. This is an edge case we ignore.
        return new File(storageDirectory, UUID.randomUUID().toString() + EX_SUFFIX);
    }

    public void clearLeakDirectory() {
        List<File> allFilesExceptPending = listFiles();
        for (File file : allFilesExceptPending) {
            boolean deleted = file.delete();
            if (!deleted) {
                CanaryLog.d("Could not delete file %s", file.getPath());
            }
        }
    }

    private File appStorageDirectory() {
        File appFilesDirectory = context.getFilesDir();
        return new File(appFilesDirectory, "exview");
    }

    private boolean directoryWritableAfterMkdirs(File directory) {
        boolean success = directory.mkdirs();
        return (success || directory.exists()) && directory.canWrite();
    }
}
