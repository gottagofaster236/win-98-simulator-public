package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.lr_soft.windows98simulator.R;

public class TopButtons extends Element {  // кнопки свернуть, развернуть, закрыть в правом верхнем углу окна
    public boolean showOnlyClose = false;
    public boolean closeDisabled = false;
    private Bitmap minimize, minimizePressed, maximize, maximizePressed, maximizeDisabled, close, closePressed, closeDisabledBmp, restore, restorePressed;
    private int pressed = NOTHING;
    private static final int MINIMIZE = 0, MAXIMIZE = 1, CLOSE = 2, NOTHING = -1;

    public TopButtons(){
        width = 50;
        height = 14;
        minimize = getBmp(R.drawable.minimize);
        minimizePressed = getBmp(R.drawable.minimize_pressed);
        maximize = getBmp(R.drawable.maximize);
        maximizePressed = getBmp(R.drawable.maximize_pressed);
        maximizeDisabled = getBmp(R.drawable.maximize_inactive);
        close = getBmp(R.drawable.close);
        closePressed = getBmp(R.drawable.close_pressed);
        closeDisabledBmp = getBmp(R.drawable.close_disabled);
        restore = getBmp(R.drawable.restore);
        restorePressed = getBmp(R.drawable.restore_pressed);
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        Bitmap min = pressed == MINIMIZE? minimizePressed : minimize;
        Bitmap max;
        if(((Window) parent).unableToMaximize)
            max = maximizeDisabled;
        else {
            if(!((Window) parent).maximized)
                max = pressed == MAXIMIZE ? maximizePressed : maximize;
            else
                max = pressed == MAXIMIZE ? restorePressed : restore;
        }
        Bitmap close = pressed == CLOSE? closePressed : this.close;
        if(closeDisabled)
            close = closeDisabledBmp;
        if(!showOnlyClose) {
            canvas.drawBitmap(min, x, y, null);
            canvas.drawBitmap(max, x + 16, y, null);
        }
        canvas.drawBitmap(close, x + 34, y, null);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(!((showOnlyClose? 34 : 0) <= x && x < width + 2 && -2 <= y && y < height + 2))
            return false;
        if(!touch)
            return true;
        Window parentWindow = (Window) parent;
        if(parentWindow.defaultButton != null)
            parentWindow.defaultButton.ignoreOtherTouch = true;
        if(parentWindow.inputFocus instanceof TextBox)
            ((TextBox) parentWindow.inputFocus).ignoreOtherTouch = true;
        if(x < 16)
            pressed = MINIMIZE;
        else if(x < 34)
            pressed = MAXIMIZE;
        else
            pressed = CLOSE;
        return true;
    }

    @Override
    public void onMouseLeave() {
        pressed = NOTHING;
    }

    @Override
    public void onClick(int x, int y) {
        Window parent_win = (Window) parent;
        switch (pressed){
            case MINIMIZE:
                parent_win.minimize();
                break;
            case MAXIMIZE:
                if(parent_win.unableToMaximize)
                    break;
                if(parent_win.maximized)
                    parent_win.restore();
                else
                    parent_win.maximize();
                break;
            case CLOSE:
                if(closeDisabled)
                    break;
                parent_win.close();
                break;
        }
        pressed = NOTHING;
    }
}
