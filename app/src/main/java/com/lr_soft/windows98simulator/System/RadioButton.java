package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;

public class RadioButton extends Element {
    private String text;
    private RadioButton[] group;
    private Rect bounds = new Rect();
    private Bitmap background, backgroundActive;

    public boolean active = false;

    public RadioButton(String text){
        this.text = text;
        bounds.left = -1;
        bounds.top = -1;
        bounds.bottom = 13 + 2;
        bounds.right = 16 + (int) p.measureText(text) + 3;
        background = getBmp(R.drawable.radio_button);
        backgroundActive = getBmp(R.drawable.radio_pressed);
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        canvas.drawBitmap(isPressed()? backgroundActive : background, x, y, null);
        p.setColor(Color.BLACK);
        if(active){  // рисуем черную точку
            canvas.drawRect(x + 5, y + 4, x + 7, y + 8, p);
            canvas.drawRect(x + 4, y + 5, x + 8, y + 7, p);
        }
        canvas.drawText(text, x + 18, y + 11, p);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        return bounds.contains(x, y);
    }

    @Override
    public void onClick(int x, int y) {
        for(RadioButton radioButton : group)
            radioButton.active = false;
        active = true;
    }

    public static void createGroup(RadioButton... radioButtons){
        for(RadioButton radioButton : radioButtons)
            radioButton.group = radioButtons;
    }
}
