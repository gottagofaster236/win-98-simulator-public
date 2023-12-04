package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

public class AboutWindow extends DialogWindow {
    Bitmap bigIcon;
    String appTitle;
    String additionalText;  // после Copyright (C) 1981-1998 Microsoft
    private int systemResources = (int)(Math.random() * (92 - 86 + 1)) + 86;  // 86 - 92%
    public AboutWindow(Window parentWindow, String appTitle, Bitmap bigIcon){
        this(parentWindow, appTitle, bigIcon, "");
    }
    public AboutWindow(Window parentWindow, String appTitle, Bitmap bigIcon, String additionalText){
        super("About " + appTitle, 348, 283, parentWindow);
        this.appTitle = appTitle;
        this.bigIcon = bigIcon;
        this.additionalText = additionalText;
        Button ok = new Button("OK", 75, 23, 258, 246, Color.BLACK, true, parent -> close());
        ok.coolActive = true;
        defaultButton = ok;
        elements.add(ok);
        alignWithParent(35, 74);
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        canvas.drawBitmap(bigIcon, x + 14, y + 33, null);
        p.setColor(Color.BLACK);
        canvas.drawText("Microsoft (R) " + appTitle, x + 82, y + 51, p);
        canvas.drawText("Windows 98", x + 81, y + 67, p);
        canvas.drawText("Copyright (C) 1981-1998 Microsoft Corp.", x + 82, y + 83, p);
        canvas.drawText(additionalText, x + 82, y + 100, p);
        canvas.drawText("This product is licensed to:", x + 82, y + 132, p);
        canvas.drawText("User", x + 82, y + 148, p);
        // рисуем separator
        p.setColor(Color.WHITE);
        canvas.drawRect(x + 81, y + 180, x + 331, y + 182, p);
        p.setColor(Color.parseColor("#87888F"));
        canvas.drawRect(x + 81, y + 180, x + 330, y + 181, p);
        // продолжение текста
        p.setColor(Color.BLACK);
        canvas.drawText("Physical memory available to Windows:", x + 82, y + 202, p);
        canvas.drawText("65,052 KB", x + 280, y + 202, p);
        canvas.drawText("System resources:", x + 82, y + 223, p);
        canvas.drawText( systemResources + "% Free", x + 280, y + 223, p);
        //drawTopLayer(canvas, shift_x, shift_y);
    }
}
