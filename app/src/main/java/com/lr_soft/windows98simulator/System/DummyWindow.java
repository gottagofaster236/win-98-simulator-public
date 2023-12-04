package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class DummyWindow extends Window {  // окно, которое основывается на одной или двух (для minimized и maximized) картинках
    Bitmap bmp1, bmp2;
    public DummyWindow(String windowTitle, Bitmap icon, boolean borderBold, Bitmap bmp1, Bitmap bmp2, Rect closeButton, String closeButtonText) {
        super(windowTitle, icon, bmp1.getWidth(), bmp1.getHeight(), borderBold, false, icon == null);
        this.bmp1 = bmp1;
        this.bmp2 = bmp2;
        if(closeButton != null){
            defaultButton = addCloseButton(closeButton, closeButtonText);
            defaultButton.coolActive = true;
        }
        fillWindow = false;
        if(bmp2 == null) {
            unableToMaximize = true;
        }
    }
    public DummyWindow(String windowTitle, Bitmap icon, boolean borderBold, Bitmap bmp1, Bitmap bmp2){
        this(windowTitle, icon, borderBold, bmp1, bmp2, null, null);
    }
    public DummyWindow(String windowTitle, Bitmap icon, boolean borderBold, Bitmap bmp1){
        this(windowTitle, icon, borderBold, bmp1, null, null, null);
    }
    public DummyWindow(String windowTitle, Bitmap icon, boolean borderBold, Bitmap bmp1, Rect closeButton, String closeButtonText, Rect alternativeCloseButton, String alternativeCloseButtonText){
        this(windowTitle, icon, borderBold, bmp1, null, closeButton, closeButtonText);
        addCloseButton(alternativeCloseButton, alternativeCloseButtonText);
    }
    public DummyWindow(String windowTitle, Bitmap icon, boolean borderBold, Bitmap bmp1, Rect closeButton, String closeButtonText){
        this(windowTitle, icon, borderBold, bmp1, null, closeButton, closeButtonText);
    }
    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        if(!maximized)
            canvas.drawBitmap(bmp1, x, y, null);
        else{
            canvas.drawBitmap(bmp2, x + border_width, y + border_width, null);
        }
        super.onNewDraw(canvas, x, y);
    }
}
