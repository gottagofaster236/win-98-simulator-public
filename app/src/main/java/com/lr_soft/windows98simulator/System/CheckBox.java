package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;

public class CheckBox extends Element {
    private String text;
    public boolean checked = false;
    private Rect bounds;
    private Bitmap checkBmp;
    public Runnable onCheckChange;

    public CheckBox(String text){
        this.text = text;
        bounds = new Rect();
        checkBmp = getBmp(R.drawable.check_alpha);
        bounds.left = 0;
        bounds.top = -1;
        bounds.bottom = 13 + 2;
        bounds.right = 17 + (int) p.measureText(text) + 3;
    }
    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        drawFrameRectActive(canvas, x, y, x + 13, y + 13);
        if(!isPressed()) {
            p.setColor(Color.WHITE);
            canvas.drawRect(x + 2, y + 2, x + 13 - 2, y + 13 - 2, p);
        }
        if(checked)
            canvas.drawBitmap(checkBmp, x + 2, y + 2, null);
        p.setColor(Color.BLACK);
        canvas.drawText(text, x + 19, y + 11, p);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        return bounds.contains(x, y);
    }

    @Override
    public void onClick(int x, int y) {
        checked = !checked;
        if(onCheckChange != null)
            onCheckChange.run();
    }
}
