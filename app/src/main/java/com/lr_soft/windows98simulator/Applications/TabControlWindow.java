package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.System.Window;

public class TabControlWindow extends Window {  // окошко с несколькими вкладками. См. TabControl (Google)
    Bitmap[] pages;
    int[] coords;  // координаты - левый пиксель каждой кнопки, а также пиксель правее последней кнопки
    Bitmap curPage;
    public TabControlWindow(String windowTitle, Bitmap[] pages, int[] coords, Rect ok, Rect cancel){
        super(windowTitle, null, pages[0].getWidth(), pages[0].getHeight(), false, false, true);
        this.pages = pages;
        this.coords = coords;
        defaultButton = addCloseButton(ok, "OK");
        defaultButton.coolActive = true;
        addCloseButton(cancel, "Cancel");
        curPage = pages[0];
        fillWindow = false;
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(!super.onMouseOver(x, y, touch))
            return false;
        if(touch && 31 <= y && y <= 48){  // кликнули в панель с кнопочками
            for(int i=0; i<coords.length-1; i++){
                if(coords[i] <= x && x < coords[i + 1]){
                    curPage = pages[i];
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        canvas.drawBitmap(curPage, x, y, null);
        super.onNewDraw(canvas, x, y);
    }
}
