package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.lr_soft.windows98simulator.Applications.StartMenu;

public class ButtonList extends ElementContainer implements CloseableMenu {
    public Element parentButton;  // так как кнопка - это не ElementContainer
    boolean parentThick = false;

    public boolean isStartMenu = false;  // показывает слева полосу с логотипом Windows
    private boolean active = false;  // надо для пустого списка

    public void onDraw(Canvas canvas, int x, int y) {  // не drawElements, потому что вычисляем координаты
        for(Element b : elements) {  // b на самом деле ButtonInList
            b.parent = this;
            ((ButtonInList) b).parentThick = this.parentThick;
        }
        if(elements.isEmpty()){
            Bitmap empty_button = active? StartMenu.emptyButtonPressed : StartMenu.emptyButton;
            height = empty_button.getHeight();
            width = empty_button.getWidth();
            canvas.drawBitmap(empty_button, x, y, null);
            return;
        }
        boolean existsSubmenu = false;
        for(Element b : elements) {
            if (((ButtonInList) b).hasChild) {
                existsSubmenu = true;
                break;
            }
        }
        boolean containsTwoRows = false;
        for(Element b : elements){
            if(((ButtonInList) b).twoRows){
                containsTwoRows = true;
                break;
            }
        }
        int width = 0;
        height = 6;
        for (Element b : elements) {
            if(b.height == -1)
                b.onDraw(new Canvas(), 0, 0);
            height += b.height;
        }
        if(!containsTwoRows) {
            for (Element b : elements) {
                width = Math.max(width, ((ButtonInList) b).desiredWidth(existsSubmenu));
            }
            // со всех сторон рамка шириной 3
        }
        else{
            int width1 = 0, width2 = 0;  // ширина первого и второго ряда
            for (Element b : elements) {
                width1 = Math.max(width1, ((ButtonInList) b).desiredWidthForRow(0));
                width2 = Math.max(width2, ((ButtonInList) b).desiredWidthForRow(1));
            }
            width = ButtonInList.desiredWidth(width1, width2, parentThick);
            for(Element b_el : elements){
                ButtonInList b = (ButtonInList) b_el;
                b.width = width;
                b.width1 = width1;
                b.width2 = width2;
            }
        }

        this.width = width + 6;
        if (isStartMenu)
            this.width += 21;  // на показывание полосы слева

        Element.drawFrameRect(canvas, x, y, x + this.width, y + height);
        int cur_x = 3, cur_y = 3;
        if(isStartMenu)
            cur_x += 21;
        boolean contains_blue = false;
        for(Element b : elements)
            if(((ButtonInList) b).blue)
                contains_blue = true;
        if(!contains_blue)
            setActiveChildBlue();
        for(Element b : elements){
            b.x = cur_x;
            b.y = cur_y;
            b.width = width;
            b.onDraw(canvas, x + b.x, y + b.y);
            cur_y += b.height;
        }
        cur_y = 3;
        for(Element b_el : elements){
            ButtonInList b = (ButtonInList) b_el;
            b.showChild(canvas, x + b.x, y + b.y);
            b.lastShow = b.showChild;
            cur_y += b.height;
        }
        if(isStartMenu) {
            p.setColor(Color.parseColor("#0000A8"));
            canvas.drawRect(x + 3, y + 3, x + 3 + 21, y + height - 3, p);
            canvas.drawBitmap(StartMenu.windowsSlider, x + 3, y + height - 3 - 105, null);
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {  // сначала мы делаем onMouseOver детей, и лишь потом самих кнопок
        if(width == -1)
            onDraw(new Canvas(), 0, 0);
        ButtonInList childWithMouseOver = null;  // кнопка, ребёнок которой подсвечен
        for(Element el : elements){
            ButtonInList b = (ButtonInList) el;
            if(b.showChild && b.hasChild && b.child.onMouseOver(x - b.x - b.child.x, y - b.y - b.child.y, false)){
                childWithMouseOver = b;
                break;
            }
        }
        if(childWithMouseOver != null){
            startTouch = childWithMouseOver;
            for(Element el : elements){
                if(el != childWithMouseOver)
                    el.onMouseLeave();
                else
                    el.onMouseOver(x - el.x, y - el.y, touch);
            }
            return true;
        }
        else{
            return super.onMouseOver(x, y, touch);
        }
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        if(elements.isEmpty())
            active = 3 <= x && x < width - 3 && 0 <= y && y < height - 3;
        return 0 <= x && x < width && 0 <= y && y < height;
    }

    @Override
    public void onSelfMouseLeave() {
        if(elements.isEmpty())
            active = false;
    }

    private void setActiveChildBlue(){
        for(Element b_el : elements){
            ButtonInList b = (ButtonInList) b_el;
            b.blue = (b.showChild && b.hasChild);
        }
    }

    public void closeMenu(){
        ((CloseableMenu) parentButton).closeMenu();
    }

    public void reset(){
        for(Element e : elements){
            ButtonInList b = (ButtonInList) e;
            b.showChild = false;
            b.blue = false;
            b.lastShow = false;
        }
        active = false;
    }
}
