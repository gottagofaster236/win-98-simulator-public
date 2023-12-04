package com.lr_soft.windows98simulator.System;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.List;

public class ElementContainer extends Element {
    public List<Element> elements = new ArrayList<>();
    public Element startTouch = null;
    Element lastClick = null;  // для того, чтобы делать onDoubleClick. Равен null, если это selfClick
    private int[][] lastTouches = {{-100, -100}, {-100, -100}};  // координаты последних 2 нажатий (по порядку)
    // нам нужны координат позапрошлого касания, чтобы удостовериться, что мы не подвинули мышку слишком далеко
    public Element inputFocus = null;  // Последний элемент, на котором был onMouseOver(touch = true). null - мы сами. Нужен для onKeyPress.
    boolean notChangeInputFocus = false;  // в следующий раз, когда мы захотим изменить inputFocus, не делать этого. (см. использования)

    protected boolean bringClickedElementToFront = false;  // устанавливает topElement на кликнутый элемент
    public Element topElement = null;  // на самом деле, чтобы не менять порядок элементов в elements (но типа для производительности, ибо remove за O(n))
    // при удалении элемента надо не забывать обнулять topElement

    public void drawElements(Canvas canvas, int x, int y){
        for(int i = 0; i < elements.size(); i++){
            Element el = elements.get(i);
            if(!el.visible)
                continue;
            if(el == topElement)
                continue;
            el.parent = this;
            el.onDraw(canvas, x + el.x, y + el.y);
        }

        if(topElement != null && topElement.visible) {
            topElement.parent = this;
            topElement.onDraw(canvas, x + topElement.x, y + topElement.y);
        }
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        drawElements(canvas, x, y);
    }

    public boolean onMouseOver(int x, int y, boolean touch){
        boolean foundElement = false;
        for(int i = elements.size(); i >= 0; i--){
            Element el;
            if(i == elements.size()){  // чтобы первым рассматривался topElement
                if(topElement == null)
                    continue;
                else
                    el = topElement;
            }
            else {
                el = elements.get(i);
                if(el == topElement)
                    continue;
            }
            el.parent = this;
            if(!el.visible) {
                el.lastClickTime = -1;
                continue;
            }
            if(!foundElement && el.onMouseOver(x - el.x, y - el.y, touch)) {
                foundElement = true;
                if (touch) {
                    if(topElement != el && el instanceof Window && ((Window) el).closed)  // внезапно. Иногда элемент может внезапно удалить себя после onMouseOver
                        continue;
                    startTouch = el;
                    if (!(el instanceof TopButtons)) {
                        if (notChangeInputFocus)
                            notChangeInputFocus = false;
                        else {
                            if (inputFocus != el) {
                                inputFocus = el;
                            }
                        }
                    }
                    if (bringClickedElementToFront) {
                        if(i < elements.size() && elements.get(i) == el)  // элемент не удалил себя
                            topElement = el;
                    }
                }
            }
            else if(foundElement && el.captureMouse && el == startTouch){
                el.onMouseOver(x - el.x, y - el.y, touch);
            }
            else{
                el.lastClickTime = -1;
                if (touch)
                    el.onOtherTouch();
                else
                    el.onMouseLeave();
                if(el == startTouch)
                    startTouch = null;
                if(touch && el == topElement && bringClickedElementToFront) {
                    topElement = null;
                }
                // не убирать здесь inputFocus! (см. Link и DisplayProperties)
            }
        }
        boolean return_value;
        if(foundElement)
            return_value = true;
        else {
            /*if(touch)
                inputFocus = null;  // на себя*/
            return_value = onSelfMouseOver(x, y, touch);
        }
        if(return_value && touch){  // записываем последнее нажатие
            int[] temp = lastTouches[0];
            lastTouches[0] = lastTouches[1];
            lastTouches[1] = temp;  // no memory allocation!
            lastTouches[1][0] = getCursorX();
            lastTouches[1][1] = getCursorY();
        }
        return return_value;
    }

    @Override
    public void onClick(int x, int y) {
        onClick(x, y, false);
    }

    public void onClick(int x, int y, boolean callSelfDoubleClick) {
        int cursor_x = getCursorX(), cursor_y = getCursorY();
        boolean cancelDoubleClick = false;
        if((lastTouches[0][0] - cursor_x) * (lastTouches[0][0] - cursor_x) + (lastTouches[0][1] - cursor_y) * (lastTouches[0][1] - cursor_y) > 4 * 4){  // подвинули курсор на более, чем 4 пикселя
            cancelDoubleClick = true;
        }
        if(startTouch != null) {
            Element el = startTouch;
            startTouch = null;
            el.parent = this;
            if (el.visible && (el.onMouseOver(x - el.x, y - el.y, false) || el.captureMouse)) {
                if (System.currentTimeMillis() - el.lastClickTime <= 500 && !cancelDoubleClick) {
                    el.lastClickTime = -1;
                    el.onDoubleClick(x - el.x, y - el.y);
                } else {
                    el.onClick(x - el.x, y - el.y);  // не менять порядок этих 2 строчек! Он используется в Link (а именно в rename)
                    el.lastClickTime = System.currentTimeMillis();
                    lastClick = el;
                }
                return;
            }
        }
        if(callSelfDoubleClick && lastClick == null && !cancelDoubleClick) {
            onSelfDoubleClick(x, y);
        }
        else {
            lastClick = null;
            onSelfClick(x, y);
        }
    }

    @Override
    public void onDoubleClick(int x, int y) {
        onClick(x, y, true);
    }

    public boolean onSelfMouseOver(int x, int y, boolean touch){
        return false;
    }

    @Override
    public void onOtherTouch() {
        onSelfOtherTouch();
        startTouch = null;
        if(bringClickedElementToFront)
            topElement = null;
        for(int i = 0; i < elements.size(); i++){  // чтобы избежать concurrentModification
            Element el = elements.get(i);
            if(!el.visible)
                continue;
            el.lastClickTime = -1;
            el.parent = this;
            el.onOtherTouch();
        }
    }

    @Override
    public void onMouseLeave() {
        onSelfMouseLeave();
        startTouch = null;
        for(int i = 0; i < elements.size(); i++){
            Element el = elements.get(i);
            if(!el.visible)
                continue;
            el.lastClickTime = -1;
            el.parent = this;
            el.onMouseLeave();
        }
    }

    @Override
    public void onRightClick(int x, int y) {
        boolean foundElement = false;
        for(int i=elements.size()-1; i>=0; i--){
            Element e = elements.get(i);
            if(!e.visible)
                continue;
            if(foundElement)
                e.onOtherTouch();
            else{
                if(e.onMouseOver(x - e.x, y - e.y, false)) {
                    e.onRightClick(x - e.x, y - e.y);
                    foundElement = true;
                    if(i < elements.size() && elements.get(i) == e) {  // элемент не удалил себя
                        if (bringClickedElementToFront)
                            topElement = e;
                        inputFocus = e;
                    }
                }
                else
                    e.onOtherTouch();
            }
        }
        if(!foundElement)
            onSelfRightClick(x, y);
    }

    @Override
    public void onKeyPress(String key) {
        if(inputFocus != null && inputFocus.visible) {
            inputFocus.parent = this;
            inputFocus.onKeyPress(key);
        }
    }

    /*@Override
    public int getTextLength() {
        if(inputFocus != null)
            return inputFocus.getTextLength();
        else
            return -1;
    }*/

    public void onSelfMouseLeave(){}
    public void onSelfOtherTouch(){ onSelfMouseLeave(); }
    public void onSelfClick(int x, int y){}
    public void onSelfDoubleClick(int x, int y){
        onSelfClick(x, y);
    }
    public void onSelfRightClick(int x, int y){
        super.onRightClick(x, y);
    }

    @Override
    public void prepareForDelete() {
        //for(Element element : elements){ - создаётся итератор :)
        for(int i = 0; i < elements.size(); i++){
            elements.get(i).prepareForDelete();
        }
    }
}
