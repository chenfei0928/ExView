package com.chenfei.exview.internal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.chenfei.exview.ExAnalysis;
import com.chenfei.exview.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_TIME;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.chenfei.exview.internal.ExViewInternals.newSingleThreadExecutor;

public final class ExViewActivity extends Activity {

    private static final String SHOW_LEAK_EXTRA = "show_latest";

    public static PendingIntent createPendingIntent(Context context) {
        return createPendingIntent(context, null);
    }

    public static PendingIntent createPendingIntent(Context context, String referenceKey) {
        Intent intent = new Intent(context, ExViewActivity.class);
        intent.putExtra(SHOW_LEAK_EXTRA, referenceKey);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, 1, intent, FLAG_UPDATE_CURRENT);
    }

    // null until it's been first loaded.
    List<ThrowableInfo> leaks;
    String visibleLeakRefKey;

    private ListView listView;
    private TextView failureView;
    private Button actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            visibleLeakRefKey = savedInstanceState.getString("visibleLeakRefKey");
        } else {
            Intent intent = getIntent();
            if (intent.hasExtra(SHOW_LEAK_EXTRA)) {
                visibleLeakRefKey = intent.getStringExtra(SHOW_LEAK_EXTRA);
            }
        }

        //noinspection unchecked
        leaks = (List<ThrowableInfo>) getLastNonConfigurationInstance();

        setContentView(R.layout.exview_display_leak);

        listView = (ListView) findViewById(R.id.leak_canary_display_leak_list);
        failureView = (TextView) findViewById(R.id.leak_canary_display_leak_failure);
        actionButton = (Button) findViewById(R.id.leak_canary_action);

        updateUi();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return leaks;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("visibleLeakRefKey", visibleLeakRefKey);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LoadLeaks.load(this, ExAnalysis.getLeakDirectoryProvider());
    }

    @Override
    public void setTheme(int resid) {
        // We don't want this to be called with an incompatible theme.
        // This could happen if you implement runtime switching of themes
        // using ActivityLifecycleCallbacks.
        if (resid != R.style.ExView_Base) {
            return;
        }
        super.setTheme(resid);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoadLeaks.forgetActivity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ThrowableInfo visibleLeak = getVisibleLeak();
        if (visibleLeak != null) {
            menu.add(R.string.exview_share_ex)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            shareLeak();
                            return true;
                        }
                    });
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            visibleLeakRefKey = null;
            updateUi();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (visibleLeakRefKey != null) {
            visibleLeakRefKey = null;
            updateUi();
        } else {
            super.onBackPressed();
        }
    }

    void shareLeak() {
        ThrowableInfo visibleLeak = getVisibleLeak();
        String leakInfo = visibleLeak.getThrowable().toString();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, leakInfo);
        startActivity(Intent.createChooser(intent, getString(R.string.exview_share_with)));
    }

    void deleteVisibleLeak() {
        ThrowableInfo visibleLeak = getVisibleLeak();
        File heapDumpFile = visibleLeak.getFile();
        boolean resultDeleted = heapDumpFile.delete();
        if (!resultDeleted) {
            CanaryLog.d("Could not delete result file %s", heapDumpFile.getPath());
        }
        visibleLeakRefKey = null;
        leaks.remove(visibleLeak);
        updateUi();
    }

    void deleteAllLeaks() {
        ExAnalysis.getLeakDirectoryProvider().clearLeakDirectory();
        leaks = Collections.emptyList();
        updateUi();
    }

    void updateUi() {
        if (leaks == null) {
            setTitle("Loading leaks...");
            return;
        }
        if (leaks.isEmpty()) {
            visibleLeakRefKey = null;
        }

        final ThrowableInfo visibleLeak = getVisibleLeak();
        if (visibleLeak == null) {
            visibleLeakRefKey = null;
        }

        ListAdapter listAdapter = listView.getAdapter();
        // Reset to defaults
        listView.setVisibility(VISIBLE);
        failureView.setVisibility(GONE);

        if (visibleLeak != null) {
            final ExViewAdapter adapter;
            if (listAdapter instanceof ExViewAdapter) {
                adapter = (ExViewAdapter) listAdapter;
            } else {
                adapter = new ExViewAdapter();
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        adapter.toggleRow(position);
                    }
                });
                invalidateOptionsMenu();
                getActionBar().setDisplayHomeAsUpEnabled(true);
                actionButton.setVisibility(VISIBLE);
                actionButton.setText(R.string.exview_delete);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteVisibleLeak();
                    }
                });
            }
            adapter.update(visibleLeak.getThrowable());
            setTitle(getString(R.string.exview_has_ex, visibleLeak.getTag(), visibleLeak.getThrowable().getMessage()));
        } else {
            if (listAdapter instanceof LeakListAdapter) {
                ((LeakListAdapter) listAdapter).notifyDataSetChanged();
            } else {
                LeakListAdapter adapter = new LeakListAdapter();
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        visibleLeakRefKey = leaks.get(position).getFile().getName();
                        updateUi();
                    }
                });
                invalidateOptionsMenu();
                setTitle(getString(R.string.exview_list_title, getPackageName()));
                getActionBar().setDisplayHomeAsUpEnabled(false);
                actionButton.setText(R.string.exview_delete_all);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(ExViewActivity.this).setIcon(
                                android.R.drawable.ic_dialog_alert)
                                .setTitle(R.string.exview_delete_all)
                                .setMessage(R.string.exview_delete_all_leaks_title)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteAllLeaks();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .show();
                    }
                });
            }
            actionButton.setVisibility(leaks.size() == 0 ? GONE : VISIBLE);
        }
    }

    ThrowableInfo getVisibleLeak() {
        if (leaks == null) {
            return null;
        }
        for (ThrowableInfo leak : leaks) {
            if (leak.getFile().getName().equals(visibleLeakRefKey)) {
                return leak;
            }
        }
        return null;
    }

    class LeakListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return leaks.size();
        }

        @Override
        public ThrowableInfo getItem(int position) {
            return leaks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ExViewActivity.this)
                        .inflate(R.layout.exview_leak_row, parent, false);
            }
            TextView titleView = (TextView) convertView.findViewById(R.id.exview_row_text);
            TextView timeView = (TextView) convertView.findViewById(R.id.leak_canary_row_time);
            ThrowableInfo leak = getItem(position);

            String index = (leaks.size() - position) + ". ";

            String title;
            title = getString(R.string.exview_has_ex, leak.getTag(), leak.getThrowable().getMessage());
            title = index + title;
            titleView.setText(title);
            String time =
                    DateUtils.formatDateTime(ExViewActivity.this, leak.getFile().lastModified(),
                            FORMAT_SHOW_TIME | FORMAT_SHOW_DATE);
            timeView.setText(time);
            return convertView;
        }
    }

    static class LoadLeaks implements Runnable {

        static final List<LoadLeaks> inFlight = new ArrayList<>();

        static final Executor backgroundExecutor = newSingleThreadExecutor("LoadLeaks");

        static void load(ExViewActivity activity, DirectoryProvider leakDirectoryProvider) {
            LoadLeaks loadLeaks = new LoadLeaks(activity, leakDirectoryProvider);
            inFlight.add(loadLeaks);
            backgroundExecutor.execute(loadLeaks);
        }

        static void forgetActivity() {
            for (LoadLeaks loadLeaks : inFlight) {
                loadLeaks.activityOrNull = null;
            }
            inFlight.clear();
        }

        ExViewActivity activityOrNull;
        private final DirectoryProvider leakDirectoryProvider;
        private final Handler mainHandler;

        LoadLeaks(ExViewActivity activity, DirectoryProvider leakDirectoryProvider) {
            this.activityOrNull = activity;
            this.leakDirectoryProvider = leakDirectoryProvider;
            mainHandler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void run() {
            final List<ThrowableInfo> leaks = new ArrayList<>();
            List<File> files = leakDirectoryProvider.listFiles();
            for (File resultFile : files) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(resultFile);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    ThrowableInfo info = (ThrowableInfo) ois.readObject();
                    info.setFile(resultFile);
                    leaks.add(info);
                } catch (IOException | ClassNotFoundException e) {
                    // Likely a change in the serializable result class.
                    // Let's remove the files, we can't read them anymore.
                    boolean deleted = resultFile.delete();
                    if (deleted) {
                        CanaryLog.d(e, "Could not read result file %s, deleted it.", resultFile);
                    } else {
                        CanaryLog.d(e, "Could not read result file %s, could not delete it either.",
                                resultFile);
                    }
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
            Collections.sort(leaks, new Comparator<ThrowableInfo>() {
                @Override
                public int compare(ThrowableInfo lhs, ThrowableInfo rhs) {
                    return Long.valueOf(rhs.getFile().lastModified())
                            .compareTo(lhs.getFile().lastModified());
                }
            });
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    inFlight.remove(LoadLeaks.this);
                    if (activityOrNull != null) {
                        activityOrNull.leaks = leaks;
                        activityOrNull.updateUi();
                    }
                }
            });
        }
    }

    static String classSimpleName(String className) {
        int separator = className.lastIndexOf('.');
        if (separator == -1) {
            return className;
        } else {
            return className.substring(separator + 1);
        }
    }
}
