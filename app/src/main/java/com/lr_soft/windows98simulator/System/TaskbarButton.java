package com.lr_soft.windows98simulator.System;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class TaskbarButton extends ElementContainer {
    Window window;
    public TaskbarButton(Window window, ContextMenu contextMenu){
        this.window = window;
        height = 22;
        width = 160;
        this.contextMenu = contextMenu;
        elements.add(contextMenu);
    }

    @Override
    public void onDraw(Canvas canvas_big, int x, int y) {
        Canvas canvas = ((ProgramsInTaskBar) parent).textCanvas;
        if(window.visible && (window.active || (window.childMessagebox != null && window.childMessagebox.active))){
            canvas.drawColor(Color.parseColor("#C0C7C8"));
            p.setColor(Color.WHITE);
            for(int i = 2; i < canvas.getWidth(); i++){
                for(int j = 2; j < canvas.getHeight() - 2; j++){
                    if((i + j) % 2 == 0)
                        canvas.drawPoint(i, j, p);  // рисуем шахматный dither белого и серого
                }
            }
            canvas.drawRect(2, 2, width - 2, 3, p);  // белая линия сверху дизера
            if(window.icon != null)
                canvas.drawBitmap(window.icon, 4, 4, null);
            p_bold.setColor(Color.BLACK);
            drawTextInLimitedSpace(canvas, window.getTitle(), window.icon != null? 23 : 5, 16, p_bold);
            canvas_big.drawBitmap(((ProgramsInTaskBar) parent).textSpace, x, y, null);
            drawSimpleFrameRectActive(canvas_big, x, y, x + width, y + height);
        }
        else {
            canvas.drawColor(Color.parseColor("#C0C7C8"));
            if(window.icon != null)
                canvas.drawBitmap(window.icon, 4, 3, null);
            p.setColor(Color.BLACK);
            drawTextInLimitedSpace(canvas, window.getTitle(), window.icon != null? 23 : 5, 15, p);
            canvas_big.drawBitmap(((ProgramsInTaskBar) parent).textSpace, x, y, null);
            if(!isPressed())
                drawSimpleFrameRect(canvas_big, x, y, x + width, y + height);
            else
                drawSimpleFrameRectActive(canvas_big, x, y, x + width, y + height);
        }
        contextMenu.onDraw(canvas_big, x + contextMenu.x, y + contextMenu.y);
    }

    private static void drawTextInLimitedSpace(Canvas canvas, String text, int x, int y, Paint p){
        if(text.isEmpty())
            return;
        int freeSpace = canvas.getWidth() - x;
        canvas.drawText(shortenTextToThreeDots(text, freeSpace, p), x, y, p);
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        return 0 <= x && x < width && 0 <= y && y < height;
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(!super.onMouseOver(x, y, touch))
            return false;
        if(touch) {
            if(window.visible)
                window.ignoreOtherTouch = true;  // см. класс Window
        }
        return true;
    }

    @Override
    public void onSelfClick(int x, int y) {
        if(!window.active)
            window.makeActive();
        else
            window.minimize();
    }

    @Override
    public void onSelfRightClick(int x, int y) {
        if(window.childMessagebox != null)  // чтобы нельзя было закрыть через меню
            return;

        super.onSelfRightClick(x, y);

        if(window.visible) {
            window.ignoreOtherTouch = true;  // см. класс Window
            window.makeActive();
        }
    }
}
