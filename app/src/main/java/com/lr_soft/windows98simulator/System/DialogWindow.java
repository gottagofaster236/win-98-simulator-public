package com.lr_soft.windows98simulator.System;

public class DialogWindow extends Window {  // только кнопка "закрыть", также не даёт кликать на родительское окно
    protected Window parentWindow;

    public DialogWindow(String windowTitle, int width, int height, Window parentWindow){
        this(windowTitle, width, height, false, parentWindow);
    }

    public DialogWindow(String windowTitle, int width, int height, boolean borderThick, Window parentWindow){
        super(windowTitle, null, width, height, borderThick, false, true);
        this.parentWindow = parentWindow;
        unableToMaximize = true;

        if(parentWindow == null)
            return;
        if(parentWindow.childMessagebox != null) {
            super.close(true);
            return;
        }
        parentWindow.childMessagebox = this;
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        boolean selfMouseOver = super.onMouseOver(x, y, touch);
        if(!selfMouseOver){
            int real_x = x + this.x, real_y = y + this.y;
            if(touch && parentWindow != null && parentWindow.onMouseOver(real_x - parentWindow.x, real_y - parentWindow.y, false)){  // если кликнули в потомка
                makeActive();
                if(defaultButton != null){
                    defaultButton.coolActive = true;
                }
                return true;
            }
        }
        return selfMouseOver;
    }

    @Override
    public void makeActive() {
        if(closed)
            return;
        if(parentWindow != null && !parentWindow.closed)
            parentWindow.top();  // если нажали на нас, то мы поднимаем наверх и родителя
        super.makeActive();
    }

    @Override
    public void forceClose() {
        super.forceClose();
        if(parentWindow != null) {
            parentWindow.childMessagebox = null;  // именно в таком порядке, иначе parent.makeActive()->this.makeActive()->this.top()->и мы снова появляемся на экране
            parentWindow.makeActive();  // надо, так как makeActive() в super.close() активировал нас, а не parent (так как DialogWindow)
        }
    }

    protected void centerInParent(){
        x = parentWindow.x + parentWindow.width / 2 - width / 2;  // центруем окно относительно предка
        y = parentWindow.y + parentWindow.height / 2 - height / 2;
        if(x + width > Windows98.SCREEN_WIDTH)
            x = Windows98.SCREEN_WIDTH - width;
        if(y + height > Windows98.TASKBAR_Y)
            y = Windows98.TASKBAR_Y - height;
        x_old = x;
        y_old = y;
    }

    protected void alignWithParent(int shiftX, int shiftY){  // расположиться на (shiftX, shiftY) относительно верхнего левого угла родителя
        x = parentWindow.x + shiftX;
        y = parentWindow.y + shiftY;
        if(x + width > Windows98.SCREEN_WIDTH)
            x = Windows98.SCREEN_WIDTH - width;
        if(y + height > Windows98.SCREEN_HEIGHT - 32)  // weird stuff
            y = Windows98.SCREEN_HEIGHT - 32 - height;
    }
}
