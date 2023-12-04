package com.lr_soft.windows98simulator.System;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

public class Button extends Element {
    public String text;
    private int textColor;
    private final static int disabledTextColor = Color.parseColor("#87888F");
    public OnClickRunnable action;
    private boolean pressed;
    private boolean cool = false;  // при нажатии сдвигаем текст на 1 пиксель по x и по y
    public boolean coolActive = false;  // более жирная обводка, если cool
    boolean ignoreOtherTouch = false;  // надо, чтобы при нажатии на TopButtons кнопка не переставала быть активной
    public boolean disabled = false;

    public Button(String text, int width, int height, int x, int y, int textColor, OnClickRunnable action){
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.text = text;
        this.textColor = textColor;
        this.action = action;
    }
    public Button(String text, int width, int height, int x, int y, int textColor, boolean cool, OnClickRunnable action){
        this(text, width, height, x, y, textColor, action);
        this.cool = cool;
    }

    public Button(String text, Rect rect, OnClickRunnable action){  // более удобный конструктор
        this(text, rect.width(), rect.height(), rect.left, rect.top, Color.BLACK, true, action);
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        if(disabled)
            coolActive = false;
        if(cool){
            p.setColor(Color.BLACK);
            canvas.drawRect(x, y, x + width, y + height, p);
        }
        if(!cool || !coolActive) {
            if (!pressed)
                drawFrameRect(canvas, x, y, x + width, y + height, true, cool);
            else
                drawFrameRectActive(canvas, x, y, x + width, y + height);
        }
        else{
            if (!pressed)
                drawFrameRect(canvas, x + 1, y + 1, x + width - 1, y + height - 1, true, cool);
            else{
                p.setColor(Color.parseColor("#87888F"));  // тёмно-серый
                canvas.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, p);
                p.setColor(Color.parseColor("#C0C7C8"));  // серый
                canvas.drawRect(x + 2, y + 2, x + width - 2, y + height - 2, p);
            }
        }
        // если cool, то там ещё есть черная тень, которая не относится к форматированию текста
        int text_x = Math.round((cool? width : width - 1) / 2.0f - p.measureText(text) / 2);
        /*if(cool)
            text_x++;*/
        int text_y;
        if(!cool)
            text_y = (int)((height / 2 - ((p.descent() + p.ascent()) / 2))) + 2;  // google :(
        else
            text_y = (int)(((height - 1) / 2 - ((p.descent() + p.ascent()) / 2))) + 1;
        if(pressed && cool) {
            text_x++;
            text_y++;
        }
        p.setColor(disabled? disabledTextColor : textColor);
        canvas.drawText(text, text_x + x, text_y + y, p);
        p.setColor(Color.BLACK);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(disabled)
            return false;  // чтобы другие кнопки не получили onOtherTouch... сори.
        if(!(0 <= x && x < width && 0 <= y && y < height))
            return false;
        if(touch) {
            pressed = true;
            coolActive = true;
        }
        return true;
    }

    @Override
    public void onClick(int x, int y) {
        if(disabled)
            return;
        pressed = false;
        if(action != null)
            action.run(this);
    }

    @Override
    public void onMouseLeave() {
        pressed = false;
    }

    @Override
    public void onOtherTouch() {
        pressed = false;
        if(ignoreOtherTouch){
            coolActive = true;
            ignoreOtherTouch = false;
        }
        else
            coolActive = false;
    }
}
