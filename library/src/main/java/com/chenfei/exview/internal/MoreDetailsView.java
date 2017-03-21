package com.chenfei.exview.internal;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public final class MoreDetailsView extends View {

    private static final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        iconPaint.setColor(ExViewUi.ROOT_COLOR);
    }

    public MoreDetailsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        float strokeSize = ExViewUi.dpToPixel(2f, getResources());
        iconPaint.setStrokeWidth(strokeSize);
    }

    private boolean opened;

    @Override protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        int halfHeight = height / 2;
        int halfWidth = width / 2;

        if (opened) {
            canvas.drawLine(0, halfHeight, width, halfHeight, iconPaint);
        } else {
            canvas.drawLine(0, halfHeight, width, halfHeight, iconPaint);
            canvas.drawLine(halfWidth, 0, halfWidth, height, iconPaint);
        }
    }

    public void setOpened(boolean opened) {
        if (opened != this.opened) {
            this.opened = opened;
            invalidate();
        }
    }
}
