package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class BigTopButtons extends Element {  // Верхние большие кнопки в Explorer'е и в Internet Explorer'е
    Bitmap[] topButtons;
    int[][] topButtonsCoords;  // координаты левой и правой границы кнопок
    int minimizedButtonsAmount;  // когда окно свёрнуто, некоторые кнопки могут не помещаться
    OnButtonPressListener listener = null;
    private int curButton = -1;  // кнопка, на которую сейчас наведён курсор
    public BigTopButtons(Bitmap[] topButtons, int[][] topButtonsCoords, int minimizedButtonsAmount, OnButtonPressListener listener) {
        this.topButtons = topButtons;
        this.topButtonsCoords = topButtonsCoords;
        this.minimizedButtonsAmount = minimizedButtonsAmount;
        this.listener = listener;
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        if(curButton != -1)
            canvas.drawBitmap(topButtons[curButton], x + topButtonsCoords[curButton][0], y, null);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        Window parentWindow = ((Window) parent);
        if(curButton != -1) {
            parentWindow.shouldRedraw = true;  // обновим окно, чтобы показать кнопочки
        }
        boolean foundElement = false;
        if(0 <= y && y < 40){
            for(int i = 0; i < topButtonsCoords.length; i++){
                if(i == minimizedButtonsAmount && !parentWindow.maximized)  // не все кнопки видны
                    break;
                if(topButtonsCoords[i][0] <= x && x <= topButtonsCoords[i][1]){
                    curButton = i;
                    foundElement = true;
                    break;
                }
            }
            parentWindow.shouldRedraw = true;
            if(touch && listener != null)
                listener.onButtonPress(curButton);
        }
        return foundElement;
    }

    @Override
    public void onMouseLeave() {
        if(curButton != -1) {
            curButton = -1;
            ((Window) parent).shouldRedraw = true;
        }
    }

    public interface OnButtonPressListener {
        void onButtonPress(int buttonNumber);
    }
}
