package com.lr_soft.windows98simulator.Applications;

import android.graphics.Canvas;
import android.graphics.Color;

import com.lr_soft.windows98simulator.System.Element;

public class ColorButton extends Element {  // для PaintBrush
    boolean twoColors;
    int color, color2;
    int colorMix;  // если у нас 2 цвета - смешаем их в один
    DitherPainter ditherPainter = null;

    public ColorButton(int color, int color2){
        width = 16;
        height = 16;
        this.color = color;
        this.color2 = color2;
        twoColors = true;
        colorMix = Color.rgb((Color.red(color) + Color.red(color2)) / 2,
                (Color.green(color) + Color.green(color2)) / 2,
                (Color.blue(color) + Color.blue(color2)) / 2);
        ditherPainter = new DitherPainter(color, color2);
    }

    public ColorButton(int color){
        width = 16;
        height = 16;
        this.color = colorMix = color;
        twoColors = false;
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        p.setColor(Color.parseColor("#87888F"));
        canvas.drawRect(x, y, x + 1, y + 15, p);
        canvas.drawRect(x, y, x + 15, y + 1, p);
        p.setColor(Color.BLACK);
        canvas.drawRect(x + 1, y + 1, x + 2, y + 14, p);
        canvas.drawRect(x + 1, y + 1, x + 14, y + 2, p);
        p.setColor(Color.parseColor("#C0C7C8"));
        canvas.drawRect(x + 1, y + 14, x + 15, y + 15, p);
        canvas.drawRect(x + 14, y + 1, x + 15, y + 15, p);
        p.setColor(Color.WHITE);
        canvas.drawRect(x, y + 15, x + 16, y + 16, p);
        canvas.drawRect(x + 15, y, x + 16, y + 16, p);
        p.setColor(color);
        if(!twoColors)
            canvas.drawRect(x + 2, y + 2, x + 14, y + 14, p);
        else{
            drawDitherRect(canvas, x + 2, y + 2, x + 14, y + 14, ditherPainter);
            /*p.setColor(color2);
            for(int i = x + 2; i <= x + 13; i++){
                for(int j = y + 2; j <= y + 13; j++){
                    if((i + j - x - y) % 2 == 0){
                        canvas.drawPoint(i, j, p);
                    }
                }
            }*/
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(!(0 <= x && x < width && 0 <= y && y < height))
            return false;
        if(touch)
            ((PaintBrush) parent).color = colorMix;
        return true;
    }
}
