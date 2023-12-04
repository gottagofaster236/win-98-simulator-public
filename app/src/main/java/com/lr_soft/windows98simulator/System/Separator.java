package com.lr_soft.windows98simulator.System;

import android.graphics.Canvas;
import android.graphics.Color;

public class Separator extends ButtonInList {  // разделитель в списке
    public Separator(){
        super(null, null, false);
        height = -1;  // так как пока неизвестен parentThick
    }
    @Override
    public int desiredWidth(boolean existsSubmenu){
        return 0;
    }

    @Override
    public int desiredWidthForRow(int rowNumber) {
        return 0;
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y){
        height = parentThick ? 10 : 8;
        if(parentThick)
            y++;  // надо всё нарисовать на 1 пиксель ниже (what?)
        // серый фон уже нарисован ButtonList-ом
        p.setColor(Color.parseColor("#87888F"));  // тёмно-серый
        canvas.drawRect(x + 2, y + 3, x + width - 2, y + 4, p);
        p.setColor(Color.WHITE);
        canvas.drawRect(x + 2, y + 4, x + width - 2, y + 5, p);
    }

    @Override
    public void onClick(int x, int y) {}
}
