package com.chenfei.exview.internal;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.chenfei.exview.ExcludeRules;
import com.chenfei.exview.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class ExViewAdapter extends BaseAdapter {
    private static final int TOP_ROW = 0;
    private static final int NORMAL_ROW = 1;

    private boolean[] opened = new boolean[0];

    private List<Object> elements = Collections.emptyList();

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        if (getItemViewType(position) == TOP_ROW) {
            if (convertView == null) {
                convertView =
                        LayoutInflater.from(context).inflate(R.layout.exview_top_row, parent, false);
            }
            TextView textView = findById(convertView, R.id.exview_row_text);
            textView.setText(getItem(position).toString());
        } else {
            if (convertView == null) {
                convertView =
                        LayoutInflater.from(context).inflate(R.layout.exview_ref_row, parent, false);
            }
            TextView textView = findById(convertView, R.id.exview_row_text);

            boolean isRoot = position == 1;
            boolean isLeakingInstance = position == getCount() - 1;
            StackTraceElement element = (StackTraceElement) getItem(position);
            String htmlString = elementToHtmlString(element, isRoot, opened[position]);
            textView.setText(Html.fromHtml(htmlString));

            DisplayLeakConnectorView connector = findById(convertView, R.id.exview_row_connector);
            if (isRoot) {
                connector.setType(DisplayLeakConnectorView.Type.START);
            } else {
                if (isLeakingInstance) {
                    connector.setType(DisplayLeakConnectorView.Type.END);
                } else {
                    connector.setType(DisplayLeakConnectorView.Type.NODE);
                }
            }
            MoreDetailsView moreDetailsView = findById(convertView, R.id.exview_row_more);
            moreDetailsView.setOpened(opened[position]);
        }

        return convertView;
    }

    private String elementToHtmlString(StackTraceElement element, boolean root, boolean opened) {
        String htmlString = "";

        if (root) {
            htmlString += "threw ";
        }

        if (element.isNativeMethod()) {
            htmlString += "<font color='#c48a47'>native</font> ";
        }

        int separator = element.getClassName().lastIndexOf('.');
        String qualifier;
        String simpleName;
        if (separator == -1) {
            qualifier = "";
            simpleName = element.getClassName();
        } else {
            qualifier = element.getClassName().substring(0, separator + 1);
            simpleName = element.getClassName().substring(separator + 1);
        }

        if (opened) {
            htmlString += "<font color='#919191'>" + qualifier + "</font>";
        }

        String styledClassName = "<font color='#ffffff'>" + simpleName + "</font>";

        htmlString += styledClassName;

        htmlString += ".<font color='#998bb5'>" + element.getMethodName().replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;") + "</font>";

        if (opened) {
            String fileInfo = (element.isNativeMethod() ? "(Native Method)" :
                    (element.getFileName() != null && element.getLineNumber() >= 0 ?
                            "(" + element.getFileName() + ":" + element.getLineNumber() + ")" :
                            (element.getFileName() != null ? "(" + element.getFileName() + ")" : "(Unknown Source)")));
            htmlString += ":<font color='#f3cf83'>" + fileInfo + "</font>";
            htmlString += "<br/>";
        }

        Exclusion exclusion = ExcludeRules.isExclude(element);
        if (exclusion == null) {
            htmlString += " <font color='#ff0000'>important</font>";
        } else if (opened) {
            htmlString += "<br/><br/>Excluded by rule";
            if (exclusion.name != null) {
                htmlString += " <font color='#ffffff'>" + exclusion.name + "</font>";
            }
            htmlString += " matching <font color='#f3cf83'>" + exclusion.matching + "</font>";
            if (exclusion.reason != null) {
                htmlString += " because <font color='#f3cf83'>" + exclusion.reason + "</font>";
            }
        }

        return htmlString;
    }

    void update(Throwable leakTrace) {
        int count = 0;
        for (Throwable t = leakTrace; t != null; t = t.getCause()) {
            count++;
            count += t.getStackTrace().length;
        }
        this.elements = new ArrayList<>(count);
        for (Throwable t = leakTrace; t != null; t = t.getCause()) {
            elements.add(t);
            elements.addAll(Arrays.asList(t.getStackTrace()));
        }
        opened = new boolean[count];
        notifyDataSetChanged();
    }

    void toggleRow(int position) {
        opened[position] = !opened[position];
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return elements.size();
    }

    @Override
    public Object getItem(int position) {
        return elements.get(position);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position) instanceof Throwable) {
            return TOP_ROW;
        }
        return NORMAL_ROW;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressWarnings("unchecked")
    private static <T extends View> T findById(View view, int id) {
        return (T) view.findViewById(id);
    }

    public static void append(SpannableStringBuilder ssb, CharSequence text, Object... what) {
        append(ssb, text, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, what);
    }

    public static void append(SpannableStringBuilder ssb, CharSequence text, int flag, Object... what) {
        if (what == null || what.length == 0) {
            ssb.append(text);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && what.length == 1) {
            ssb.append(text, what[0], flag);
        } else {
            int start = ssb.length();
            ssb.append(text);
            int end = ssb.length();
            for (Object o : what) {
                ssb.setSpan(o, start, end, flag);
            }
        }
    }
}
