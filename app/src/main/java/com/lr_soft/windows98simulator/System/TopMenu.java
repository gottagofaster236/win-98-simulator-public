package com.lr_soft.windows98simulator.System;

import android.graphics.Canvas;

public class TopMenu extends ElementContainer {  // строка меню
    public int buttonsSize = 0;  // 0 - у калькулятора/сапёра/солитёра, 1 - Explorer, 2 - IE
    // 0 - 18, 1 - 19, 2 - 21
    private boolean lastMouseOver = false;  // последний результат, который вернул onMouseOver
    // конструктор стандартный, без аргументов
    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        int cur_x = 0, cur_y = 0;
        boolean gray = !((Window) parent).active;  // если наше окно неактивно, то текст в кноках меню серый
        for(Element e : elements){
            ((TopMenuButton) e).buttonsSize = buttonsSize;
            ((TopMenuButton) e).gray = gray;
            e.parent = this;
            e.x = cur_x;
            e.y = cur_y;
            e.onDraw(canvas, x + e.x, y + e.y);
            cur_x += e.width;
        }
        for(Element e : elements)
            ((TopMenuButton) e).drawChild(canvas, x + e.x, y + e.y);
    }

    @Override
    public void onSelfOtherTouch() {
        for(Element e : elements){
            ((TopMenuButton) e).groupActive = false;
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        for(Element el : elements){
            ((TopMenuButton) el).buttonsSize = buttonsSize;
        }
        boolean return_value = super.onMouseOver(x, y, touch);
        if(return_value || lastMouseOver){  // если на нас сейчас наведен курсор, или был наведен в прошлом кадре (а теперь, может быть, и нет)
            ((Window) parent).shouldRedraw = true;  // обновимся, чтобы показать себя
        }
        lastMouseOver = return_value;
        return return_value;
    }

    @Override
    public void onSelfMouseLeave() {
        if(lastMouseOver){
            ((Window) parent).shouldRedraw = true;
        }
        lastMouseOver = false;
    }
}
