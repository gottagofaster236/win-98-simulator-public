package com.lr_soft.windows98simulator.System;

import android.graphics.Canvas;
import android.graphics.Color;

public class TopMenuButton extends Element implements CloseableMenu {  // кнопка вроде "File", "Edit", "Help"
    String text;
    ButtonList child;
    boolean active = false;  // показываем потомка
    public boolean disabled = false;
    public OnClickRunnable action = null;

    private boolean lastActive = false;  // последний active, когда был onDraw
    boolean groupActive = false;  // если мы нажмем на хотя бы одну кнопку в верхнем меню, то можно будет двигая мышкой открывать остальные
    private boolean mouseOver = false;  // группа неактивна, но мы наводимся на кнопку
    boolean gray = false;  // если окно неактивно, то текст в нопках серый
    int buttonsSize;  // см. TopMenu
    private boolean lastMouseOverSelf = false;

    public TopMenuButton(String text, ButtonList child){
        this.text = text;
        this.child = child;
    }

    public TopMenuButton(String text, OnClickRunnable action){
        this.text = text;
        this.action = action;
        this.child = null;
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        if(child != null)
            child.parentThick = (buttonsSize >= 1);
        if(width == -1){
            if(buttonsSize == 2){
                width = 9 + (int) (p.measureText(text)) + 10;
                height = 21;
            }
            else if(buttonsSize == 1) {
                width = 9 + (int) (p.measureText(text)) + 10;
                height = 19;
            }
            else{
                width = 6 + (int) (p.measureText(text)) + 7;
                height = 18;
            }
        }
        p.setColor(Color.parseColor("#C0C7C8"));
        canvas.drawRect(x, y, x + width, y + height, p);  // задний фон
        if(mouseOver || active) {  // иначе вообще никаких линий нет
            p.setColor(active ? Color.parseColor("#87888F") : Color.WHITE);  // линии имеют разные цвета в зависимости от active
            canvas.drawRect(x, y, x + 1, y + height - 1, p);
            canvas.drawRect(x, y, x + width - 1, y + 1, p);
            p.setColor(active ? Color.WHITE : Color.parseColor("#87888F"));
            canvas.drawRect(x, y + height - 1, x + width, y + height, p);
            canvas.drawRect(x + width - 1, y, x + width, y + height, p);
        }
        if(!gray && !disabled)
            p.setColor(Color.BLACK);
        else
            p.setColor(Color.parseColor("#87888F"));
        int text_x, text_y;
        if(buttonsSize == 2){
            text_x = x + 10;
            text_y = y + height - 7;
        }
        else if(buttonsSize == 1){
            text_x = x + 10;
            text_y = y + height - 5;
        }
        else{
            text_x = x + 7;
            text_y = y + height - 5;
        }
        if(active) {
            text_x++;
            text_y++;
        }
        canvas.drawText(text, text_x, text_y, p);
        if(active && !lastActive && child != null){  // первый кадр, когда показываем предка
            child.reset();
        }
        lastActive = active;
    }

    void drawChild(Canvas canvas, int x, int y) {  // так как контекстное меню может накрывать кнопки, оно должно рисоваться позже
        if (child == null)
            return;
        if (!active)
            return;
        child.parentButton = this;
        if (child.height == -1)
            child.onDraw(new Canvas(), 0, 0);
        positionDropdownMenu(x, y, width, height, child);
        child.onDraw(canvas, x + child.x, y + child.y);
    }

    static void positionDropdownMenu(int x, int y, int width, int height, Element child){
        // x, y - АБСОЛЮТНЫЕ координаты родителя (они же параметры в onDraw); width, height - ширина и высота родителя
        int draw_x = 0, draw_y = 0;  // АБСОЛЮТНЫЕ координаты
        boolean foundPosition = false;
        if (!foundPosition) {  // снизу от кнопки (со смещением по x, если кнопка за экраном)
            draw_x = x;
            draw_y = y + height;
            if (draw_x < 0)
                draw_x = 0;
            else if (draw_x + child.width > Windows98.SCREEN_WIDTH)
                draw_x = Windows98.SCREEN_WIDTH - child.width;
            foundPosition = fitsInScreen(child, draw_x, draw_y);
        }
        if (!foundPosition) {  // сверху от кнопки со смещением по x
            draw_x = x;
            draw_y = y - child.height;
            if (draw_x < 0)
                draw_x = 0;
            else if (draw_x + child.width > Windows98.SCREEN_WIDTH)
                draw_x = Windows98.SCREEN_WIDTH - child.width;
            foundPosition = fitsInScreen(child, draw_x, draw_y);
        }
        if (!foundPosition) {  // по нижней границе экрана справа от кнопки
            draw_x = x + width;
            draw_y = Windows98.TASKBAR_Y - child.height;
            foundPosition = fitsInScreen(child, draw_x, draw_y);
        }
        if (!foundPosition) {  // по нижней границе экрана слева от кнопки
            draw_x = x - child.width;
            draw_y = Windows98.TASKBAR_Y - child.height;
            //foundPosition = fitsInScreen(child, draw_x, draw_y);
        }
        draw_x -= x;  // переводим в координаты относительно своего верхнего левого угла (x, y - параметры onDraw!)
        draw_y -= y;
        child.x = draw_x;
        child.y = draw_y;
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(child != null)
            child.parentButton = this;
        if(0 <= x && x < width && 0 <= y && y < height){  // нажатие на кнопку
            lastMouseOverSelf = true;
            if(touch){
                if(active){  // если кликнули на уже открытую менюшку, её надо закрыть
                    for(Element e : parent.elements){
                        ((TopMenuButton) e).groupActive = false;
                        ((TopMenuButton) e).active = false;
                    }
                    return true;
                }
                if(child != null) {
                    for (Element e : parent.elements) {
                        ((TopMenuButton) e).groupActive = true;
                    }
                }
                else
                    groupActive = true;
            }
            else if(!groupActive)
                mouseOver = true;
            if(groupActive){
                for(Element e : parent.elements){
                    ((TopMenuButton) e).active = false;
                }
                active = true;
            }
            return true;
        }
        else{
            lastMouseOverSelf = false;
            if(active && child != null)
                return child.onMouseOver(x - child.x, y - child.y, touch);
            else
                return false;
        }
    }

    @Override
    public void onOtherTouch() {
        lastMouseOverSelf = true;
        active = false;
        mouseOver = false;
    }

    @Override
    public void onMouseLeave() {
        lastMouseOverSelf = true;
        mouseOver = false;
        if(child != null)
            child.onMouseLeave();
        else {
            active = false;
            for(Element e : parent.elements){  // если мы нажали на кнопку с action, то у нее неверный groupActive
                if(e != this){
                    groupActive = ((TopMenuButton) e).groupActive;
                    break;
                }
            }
        }
    }

    @Override
    public void closeMenu() {
        for(Element e : parent.elements){
            TopMenuButton topMenuButton = (TopMenuButton) e;
            topMenuButton.active = false;
            topMenuButton.mouseOver = false;
            topMenuButton.groupActive = false;
        }
    }

    @Override
    public void onClick(int x, int y) {
        if(active && !lastMouseOverSelf && child != null && child.onMouseOver(x - child.x, y - child.y, false))
            child.onClick(x - child.x, y - child.y);
        if(action != null){
            if(!disabled)
                action.run(this);
            active = groupActive = false;
        }
    }
}
