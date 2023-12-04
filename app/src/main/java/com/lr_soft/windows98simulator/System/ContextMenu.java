package com.lr_soft.windows98simulator.System;

import android.graphics.Canvas;

public class ContextMenu extends Element implements CloseableMenu {  // меню по правой кнопке мыши
    // для использования рекомендуется унаследоваться от ElementContainer и добавить ContextMenu в elements
    public ButtonList child;
    public boolean active = false, lastActive = false;
    public ContextMenu(ButtonList child){
        this.child = child;
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        if(!active) {
            lastActive = active;
            return;
        }
        child.parentButton = this;
        if(!lastActive){  // 1-й кадр
            child.reset();
        }
        int draw_x = x, draw_y = y;
        if(child.width == -1)
            child.onDraw(new Canvas(), 0, 0);
        if(draw_x + child.width > Windows98.SCREEN_WIDTH)
            draw_x = x - child.width;
        if(draw_y + child.height > Windows98.TASKBAR_Y)
            draw_y = y - child.height;
        child.x = draw_x - x;
        child.y = draw_y - y;
        child.onDraw(canvas, x + child.x, y + child.y);
        lastActive = active;
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(!active) {
            return false;
        }
        child.parentButton = this;
        return child.onMouseOver(x - child.x, y - child.y, touch);
    }

    @Override
    public void onOtherTouch() {
        if(active)
            child.onOtherTouch();
        active = false;
    }

    @Override
    public void onMouseLeave() {
        if(active) {
            child.onMouseLeave();
        }
    }

    @Override
    public void onClick(int x, int y) {
        if(!active)
            return;
        child.onClick(x - child.x, y - child.y);
    }

    @Override
    public void closeMenu() {
        active = false;
    }

    public void reset(){
        child.reset();
    }
}
