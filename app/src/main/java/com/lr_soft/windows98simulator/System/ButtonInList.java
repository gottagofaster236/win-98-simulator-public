package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.Applications.InternetExplorer;
import com.lr_soft.windows98simulator.Applications.StartMenu;
import com.lr_soft.windows98simulator.WindowsView;

import java.util.List;

public class ButtonInList extends Element implements CloseableMenu {
    public String text;
    ButtonList child;
    private Bitmap image;  // иконка
    public OnClickRunnable action = null;
    boolean twoRows = false;
    private String[] rows;

    public List<ButtonInList> radioButtonGroup = null; // группа переключателей с кружочком
    public boolean check = false;
    public boolean checkActive = false;  // является ли галочка/кружочек выбранным на этой кнопке
    // ЕСЛИ radioButtonGroup != null - слева кружочек, если check - галочка

    public boolean disabled = false;  // это недоступная серая кнопка
    public boolean textBold = false;  // жирный текст
    boolean closeMenuAfterClick = true;

    private Bitmap image2;  // image2 - если на нас навели мышку (для контекстного меню Window)
    boolean blue = false, showChild = false;
    boolean lastShow = false;  // показывали ли мы предка в предыдущем onDraw
    boolean hasChild = false;  // есть ли у нас подменю
    private boolean runnableWaits;  // сейчас запущен Runnable, который через 500 мс покажет подменю
    private boolean topMenu = false;  // является кнопкой верхнего серого меню в окне
    private boolean lastMouseOverSelf = false;
    // В таких меню есть 2 ряда: текст и клавиатурное сокращение. Их ширина вычисляется отдельно
    int width1 = -1, width2 = -1;
    boolean parentThick = false;  // есть ли у окна толстая граница (Explorer) или тонкая (Calculator)
    public boolean isWindowsUpdate = false;
    static Bitmap arrowActive, arrowInactive, circleActive, circleInactive, checkActiveBmp, checkInactive;

    private Runnable showChildRunnable = new Runnable() {
        @Override
        public void run() {
            for(Element b : parent.elements){
                ((ButtonInList) b).showChild = false;
                ((ButtonInList) b).blue = false;
            }
            ButtonInList.this.showChild = true;
            ButtonInList.this.blue = true;
            WindowsView.windowsView.invalidate();
        }
    };

    public ButtonInList(String text) {
        this(text, "");
    }

    public ButtonInList(String text, OnClickRunnable action) {
        this(text, "", action);
    }

    public ButtonInList(String text, ButtonList child) {
        this(text, "");
        this.child = child;
        hasChild = true;
    }

    public ButtonInList(String text1, String text2) {
        this(text1, text2, null, null);
    }

    public ButtonInList(String text1, String text2, OnClickRunnable action) {
        this(text1, text2, null, null);
        this.action = action;
    }

    public ButtonInList(String text, final Class targetWindow) {
        this(text, "", parent -> {
            try {
                targetWindow.newInstance();
            }
            catch(IllegalAccessException ignored){} catch(InstantiationException ignored){}
        });
    }

    public void onDraw(Canvas canvas, int x, int y) {
        if(isWindowsUpdate){
            if(!blue)
                canvas.drawBitmap(StartMenu.windowsUpdateButton, x, y, null);
            else
                canvas.drawBitmap(StartMenu.windowsUpdateButtonPressed, x, y, null);
            return;
        }
        if(blue) {
            p.setColor(Color.parseColor("#0000A8"));
            canvas.drawRect(x, y, x + width, y + height, p);
        }
        else{
            p.setColor(Color.parseColor("#C0C7C8"));
            canvas.drawRect(x, y, x + width, y + height, p);
        }
        if(image != null && !topMenu) {
            if (height == 20)  // маленькая кнопка
                canvas.drawBitmap(image, x + 2, y + 2, null);
            else  // большая
                canvas.drawBitmap(image, x + 2, y, null);
        }

        Paint textPaint = textBold? p_bold : p;
        if(disabled)
            textPaint.setColor(Color.parseColor("#87888F"));
        else if(blue)
            textPaint.setColor(Color.WHITE);
        else
            textPaint.setColor(Color.BLACK);
        if(topMenu){
            if(checkActive){
                Bitmap pointImage;
                if(check) // галочка
                    pointImage = blue? checkActiveBmp : checkInactive;
                else  // кружочек
                    pointImage = blue? circleActive : circleInactive;
                canvas.drawBitmap(pointImage, x + 6, y + 5, null);
            }
            else if(image != null){
                canvas.drawBitmap(blue? image2 : image, x + 4, y + 3, null);
            }
            int gap = parentThick ? 13 : 9;  // пропуск между рядами
            canvas.drawText(rows[0], x + 22, y + height - 5, textPaint);
            canvas.drawText(rows[1], x + 22 + width1 + gap, y + height - 5, textPaint);
            // если кнопка topMenu, но не 2 ряда, 1-й ряд все равно рисуется правильно!!!
        }
        else {
            if(height == 20)
                canvas.drawText(text, x + 28, y + height - 5, textPaint);
            else
                canvas.drawText(text, x + 43, y + height - 11, textPaint);
        }
        if(hasChild) {
            Bitmap arrow_bmp = blue ? arrowActive : arrowInactive;
            if (height == 17)
                canvas.drawBitmap(arrow_bmp, x + width - 10, y + 4, null);
            else if (height == 20)
                canvas.drawBitmap(arrow_bmp, x + width - 9, y + 6, null);
            else
                canvas.drawBitmap(arrow_bmp, x + width - 9, y + 12, null);
        }
    }

    void showChild(Canvas canvas, int x, int y) {
        if (!hasChild || !showChild)
            return;
        child.parentButton = this;
        child.parentThick = this.parentThick;
        if(child.width == -1)
            child.onDraw(new Canvas(), 0, 0);
        if (!lastShow) {  // если это первый кадр, когда мы показываем предка, надо обнулить все состояния
            child.reset();
        }
        boolean foundPosition = false;
        int draw_x = 0, draw_y = 0;

        if (!foundPosition) {  // справа от кнопки со смещением по y
            draw_x = x + width - 3;
            draw_y = y - 3;
            if (draw_y < 0)
                draw_y = 0;
            else if (draw_y + child.height > Windows98.TASKBAR_Y)
                draw_y = Windows98.TASKBAR_Y - child.height;
            foundPosition = fitsInScreen(child, draw_x, draw_y);
        }
        if (!foundPosition) {  // слева от кнопки со смещением по y
            draw_x = x - child.width - 1;
            draw_y = y - 3;
            if (draw_y < 0)
                draw_y = 0;
            else if (draw_y + child.height > Windows98.TASKBAR_Y)
                draw_y = Windows98.TASKBAR_Y - child.height;
            // foundPosition = fitsInScreen(child, draw_x, draw_y);
        }

        draw_x -= x;  // переводим абсолютные координаты в координаты относительно своего верхнего левого угла
        draw_y -= y;
        child.x = draw_x;
        child.y = draw_y;
        child.onDraw(canvas, x + child.x, y + child.y);
    }

    public int desiredWidth(boolean existsSubmenu){  // на topMenu вызывается только если все кнопки из 1 ряда
        if(isWindowsUpdate)
            return StartMenu.windowsUpdateButton.getWidth();
        int text_width = (int) (textBold? p_bold : p).measureText(text);
        if(topMenu)
            return 22 + text_width + 21;  // вне зависимости от стрелки (да!!)
        if(!existsSubmenu){
            return 28 + text_width + 15;  // слева - картинка; справа - пропуск
        }
        else{
            if(height == 20)
                return 28 + text_width + 21;
            else
                return 43 + text_width + 25;
        }
    }

    int desiredWidthForRow(int rowNumber){  // если есть клав. сокращение, то стрелки нет, так что ок
        return (int) (textBold? p_bold : p).measureText(rows[rowNumber]);
    }

    static int desiredWidth(int row1, int row2, boolean parentThick){
        if(parentThick)
            return 22 + row1 + 13 + row2 + 19;
        else
            return 22 + row1 + 9 + row2 + 19;
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        //Log.d(TAG, "On button point");
        if(child != null) {
            child.parentButton = this;
            child.parentThick = parentThick;
            if(child.width == -1)
                child.onDraw(new Canvas(), 0, 0);
        }
        if(showChild && hasChild){
            child.parentThick = parentThick;
            if(child.onMouseOver(x - child.x, y - child.y, touch)) {
                blue = true;
                lastMouseOverSelf = false;
                return true;
            }
        }
        lastMouseOverSelf = true;
        if(0 <= x && x < width && 0 <= y && y < height){  // на кнопку
            blue = true;
            if(disabled)
                return true;
            if(touch) {  // все действия выполняются при клике, а не нажатии, КРОМЕ показывания child
                if(hasChild)
                    showChild = true;
            }
            else if(!showChild)
                startRunnable();
            if(hasChild)
                child.onMouseLeave();
            return true;
        }
        else
            return false;
    }

    @Override
    public void onMouseLeave() {
        blue = false;
        lastMouseOverSelf = false;
        if(hasChild && showChild)
            child.onMouseLeave();
        discardRunnable();
    }

    @Override
    public void onOtherTouch() {
        blue = false;
        lastMouseOverSelf = false;
        showChild = false;
        discardRunnable();
    }

    @Override
    public void onClick(int x, int y){
        if(disabled)
            return;
        if(hasChild){
            if(showChild && !lastMouseOverSelf && child.onMouseOver(x - child.x, y - child.y, false))
                child.onClick(x - child.x, y - child.y);
        }
        else if(action != null) {
            action.run(this);
            if(closeMenuAfterClick)
                closeMenu();
        }
        else if(radioButtonGroup != null){  // кружочек
            for(ButtonInList b : radioButtonGroup)
                b.checkActive = false;
            checkActive = true;
            closeMenu();
        }
        else if(check){  // галочка
            checkActive = !checkActive;
            closeMenu();
        }
        else  // кнопка без действия
            closeMenu();
    }

    public void closeMenu(){
        ((ButtonList) parent).closeMenu();
    }

    private void startRunnable(){
        if(runnableWaits)
            return;
        WindowsView.handler.postDelayed(showChildRunnable, 500);
        runnableWaits = true;
    }

    private void discardRunnable(){
        WindowsView.handler.removeCallbacks(showChildRunnable);
        runnableWaits = false;
    }

    // ================= конструкторы с ограниченным применением =======================
    public ButtonInList(String text, Bitmap image, boolean big) {  // конструктор для кнопок Start Menu
        this.image = image;
        this.text = text;
        if(big)
            height = 32;
        else
            height = 20;
        if(image == StartMenu.html){
            action = parent -> new InternetExplorer();
        }
    }
    public ButtonInList(String text, Bitmap image, boolean big, final Class targetWindow) {  // конструктор для кнопок Start Menu
        this(text, image, big);
        action = parent -> {
            try {
                targetWindow.newInstance();
            }
            catch(IllegalAccessException ignored){} catch(InstantiationException ignored){}
        };
    }
    public ButtonInList(String text, Bitmap image, boolean big, ButtonList child) {  // конструктор для кнопок Start Menu
        this.image = image;
        this.text = text;
        if(big)
            height = 32;
        else
            height = 20;
        hasChild = true;
        this.child = child;
    }
    // конструкторы, создающие окна
    public ButtonInList(String text, boolean big, int image_id, final String windowTitle, final boolean borderThick, final int bmp_id1, final int bmp_id2){  // сразу создаёт DummyWindow
        this(text, getBmp(image_id), big);
        action = parent -> new DummyWindow(windowTitle, image, borderThick, getBmp(bmp_id1), getBmp(bmp_id2));
    }
    public ButtonInList(String text, boolean big, int image_id, final String windowTitle, final boolean borderThick, final int bmp_id1){
        this(text, getBmp(image_id), big);
        action = parent -> new DummyWindow(windowTitle, image, borderThick, getBmp(bmp_id1));
    }
    public ButtonInList(String text, boolean big, int image_id, final String windowTitle, final int bmp_id1, final Rect closeButton, final String closeButtonText){  // создаёт почти messagewindow
        this(text, getBmp(image_id), big);
        action = parent -> {
            Window window = new DummyWindow(windowTitle, null, false, getBmp(bmp_id1), closeButton, closeButtonText);
            window.topButtons.showOnlyClose = true;
        };
    }
    public ButtonInList(String text, boolean big, int image_id, final String windowTitle, final int bmp_id1, final Rect closeButton, final String closeButtonText,
                        final Rect alternativeCloseButton, final String alternativeCloseButtonText){  // создаёт почти messagewindow
        this(text, getBmp(image_id), big);
        action = parent -> {
            Window window = new DummyWindow(windowTitle, null, false, getBmp(bmp_id1), closeButton, closeButtonText, alternativeCloseButton, alternativeCloseButtonText);
            window.topButtons.showOnlyClose = true;
        };
    }
    public ButtonInList(String text1, String text2, Bitmap image, Bitmap image2) {  // конструктор для контекстного меню Window
        rows = new String[]{text1, text2};
        text = text1;
        height = 17;
        topMenu = true;
        if(!text2.isEmpty())
            twoRows = true;
        this.image = image;
        this.image2 = image2;
    }
    public ButtonInList(String text, Bitmap image, Bitmap image2){  // конструктор для контекстного меню Window
        this(text, "", image, image2);
    }
}
