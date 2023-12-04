package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class ProgramsInTaskBar extends ElementContainer {
    Bitmap textSpace;
    Canvas textCanvas = new Canvas();  // нужен для отрисовки TaskbarButton
    static ProgramsInTaskBar programsInTaskBar;

    public ProgramsInTaskBar(){
        width = Windows98.SCREEN_WIDTH - 261;
        height = 22;
        x = 153;
        y = Windows98.TASKBAR_Y + 4;
        bringClickedElementToFront = true;
        programsInTaskBar = this;
    }
    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        // рисуем кнопки, пока не станут вылезать
        boolean notDrawingAnymore = false;  // закончилось место, и мы просто делаем все элементы невидимыми
        int cur_x = 0;
        for(Element el : elements){
            el.parent = this;
            if(notDrawingAnymore) {
                el.visible = false;
                continue;
            }
            el.visible = true;
            el.x = cur_x;
            if(el.x + el.width > this.width) {
                notDrawingAnymore = true;
                el.visible = false;
                continue;
            }
            if(el != topElement)
                el.onDraw(canvas, x + el.x, y + el.y);
            cur_x += el.width + 3;
        }
        if(topElement != null && topElement.visible)
            topElement.onDraw(canvas, x + topElement.x, y + topElement.y);
    }
    void updateButtonsSize(){
        if(elements.isEmpty())
            return;
        // обновляет также и bitmap с canvas'ом
        int freeSpace = width - (elements.size() - 1) * 3;  // минус пропуск
        int buttonWidth = freeSpace / elements.size();
        if(buttonWidth < 22)
            buttonWidth = 22;
        else if(buttonWidth > 160)
            buttonWidth = 160;
        textSpace = createBitmap(buttonWidth - 2, 22, Bitmap.Config.ARGB_8888);
        textCanvas.setBitmap(textSpace);
        for(Element button : elements)
            button.width = buttonWidth;
    }
    TaskbarButton getButtonByWindow(Window window){
        for(Element el : elements){
            if(((TaskbarButton) el).window == window) {
                return (TaskbarButton) el;
            }
        }
        return null;
    }
}
