package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.WindowsView;

public class ScrollBar extends Element {
    Scrollable scrollable;
    boolean isVertical;
    public double startPosition, endPosition;  // числа от 0 до 1, определяющие положение ползунка
    private boolean disabled = false;  // скроллинг не нужен

    private DitherPainter ditherPainter, ditherPainterActive;
    private int realButtonStart, realButtonEnd;  // первый и последний пиксели кнопки
    private int oldButtonStart;  // до начала скроллинга
    private Bitmap scrollLeft, scrollRight, scrollUp, scrollDown;
    private Bitmap scrollLeftDisabled, scrollRightDisabled, scrollUpDisabled, scrollDownDisabled;
    private Bitmap scrollLeftPressed, scrollRightPressed, scrollUpPressed, scrollDownPressed;

    public final static int OUTSIDE = -1, UP_BUTTON = 0, PAGE_UP = 1, THUMB = 2, PAGE_DOWN = 3, DOWN_BUTTON = 4;
    public int mouseOverPart = OUTSIDE;  // -1 - ничего, 0 - первая кнопка, 1 - первый зазор, 2 - ползунок, 3 - второй зазор, 4 - вторая кнопка
    private int lastMouseOverPart;
    private int thumbPress = -1;  // место, где мы нажали на ползунок
    private Runnable curAction = null;  // pageUp, pageDown, scrollUp или scrollDown
    private Runnable pageUp = new Runnable() { @Override public void run() { scrollable.pageUpDown(true, isVertical); }};
    private Runnable pageDown = new Runnable() { @Override public void run() { scrollable.pageUpDown(false, isVertical); }};
    private Runnable scrollUpR = new Runnable() { @Override public void run() { scrollable.scrollUpDown(true, isVertical); }};
    private Runnable scrollDownR = new Runnable() { @Override public void run() { scrollable.scrollUpDown(false, isVertical); }};
    private Runnable actionRunnable = new Runnable(){
        @Override
        public void run() {
            if(disabled)
                return;
            curAction.run();
            WindowsView.handler.postDelayed(this, 100);
            onMouseOver(getCursorX() - x - parent.x, getCursorY() - y - parent.y, false);
            WindowsView.windowsView.invalidate();
        }
    };

    private Rect minimizedBounds, maximizedBounds;

    public ScrollBar(Scrollable scrollable, Rect bounds, boolean isVertical){
        this.scrollable = scrollable;
        this.isVertical = isVertical;
        this.minimizedBounds = bounds;
        setBounds(bounds);
        ditherPainter = new DitherPainter(Color.WHITE, Color.parseColor("#C0C7C8"));
        ditherPainterActive = new DitherPainter(Color.BLACK, Color.parseColor("#C0C7C8"));

        scrollLeft = getBmp(R.drawable.scroll_left);
        scrollRight = getBmp(R.drawable.scroll_right);
        scrollUp = getBmp(R.drawable.scroll_up);
        scrollDown = getBmp(R.drawable.scroll_down);
        scrollLeftDisabled = getBmp(R.drawable.scroll_left_disabled);
        scrollRightDisabled = getBmp(R.drawable.scroll_right_disabled);
        scrollUpDisabled = getBmp(R.drawable.scroll_up_disabled);
        scrollDownDisabled = getBmp(R.drawable.scroll_down_disabled);
        scrollLeftPressed = getBmp(R.drawable.scroll_left_pressed);
        scrollRightPressed = getBmp(R.drawable.scroll_right_pressed);
        scrollUpPressed = getBmp(R.drawable.scroll_up_pressed);
        scrollDownPressed = getBmp(R.drawable.scroll_down_pressed);
        captureMouse = true;
    }

    public ScrollBar(Scrollable scrollable, Rect minimizedBounds, Rect maximizedBounds, boolean isVertical){
        this(scrollable, minimizedBounds, isVertical);
        this.minimizedBounds = minimizedBounds;
        this.maximizedBounds = maximizedBounds;
    }

    public ScrollBar(Scrollable scrollable, Window.RelativeBounds bounds, boolean isVertical){
        this(scrollable, bounds.getMinimizedRect(), bounds.getMaximizedRect(), isVertical);
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        // рисуем dither
        if(disabled) {
            drawDitherRect(canvas, x, y, x + width, y + height, ditherPainter);
        }
        else{
            calculateThumbCoords();
            if(isVertical) {
                drawDitherRect(canvas, x, y, x + width, y + realButtonStart,
                        isPressed() && mouseOverPart == PAGE_UP? ditherPainterActive : ditherPainter);
                drawDitherRect(canvas, x, y + realButtonEnd + 1, x + width, y + height,
                        isPressed() && mouseOverPart == PAGE_DOWN? ditherPainterActive : ditherPainter);
            }
            else{
                drawDitherRect(canvas, x, y, x + realButtonStart, y + height,
                        isPressed() && mouseOverPart == PAGE_UP? ditherPainterActive : ditherPainter);
                drawDitherRect(canvas, x + realButtonEnd + 1, y, x + width, y + height,
                        isPressed() && mouseOverPart == PAGE_DOWN? ditherPainterActive : ditherPainter);
            }
        }
        // рисуем кнопки вверх/вниз/влево/вправо
        if(isVertical){
            canvas.drawBitmap(disabled? scrollUpDisabled : (isPressed() && mouseOverPart == UP_BUTTON)? scrollUpPressed : scrollUp,
                    x, y, null);
            canvas.drawBitmap(disabled? scrollDownDisabled : (isPressed() && mouseOverPart == DOWN_BUTTON)? scrollDownPressed : scrollDown,
                    x + width - 16, y + height - 16, null);
        }
        else{
            canvas.drawBitmap(disabled? scrollLeftDisabled : (isPressed() && mouseOverPart == UP_BUTTON)? scrollLeftPressed : scrollLeft,
                    x, y, null);
            canvas.drawBitmap(disabled? scrollRightDisabled : (isPressed() && mouseOverPart == DOWN_BUTTON)? scrollRightPressed : scrollRight,
                    x + width - 16, y + height - 16, null);
        }
        // рисуем ползунок
        if(!disabled) {
            if (isVertical)
                drawFrameRect(canvas, x, y + realButtonStart, x + width, y + realButtonEnd + 1);
            else
                drawFrameRect(canvas, x + realButtonStart, y, x + realButtonEnd + 1, y + height);
        }
    }

    private void calculateThumbCoords(){
        /*startPosition = Math.max(0, startPosition);  // на всякий случай
        endPosition = Math.min(1, endPosition);*/
        int length = isVertical? height : width;
        length -= 16 * 2;  // 2 кнопки вверх / вниз
        int start = (int) Math.round(length * startPosition), end = (int) Math.round((length - 1) * endPosition);
        if(end - start + 1 < 7){  // слишком маленькая кнопка
            int center = (int) Math.round(length * (startPosition + endPosition) / 2);
            start = center - 3;
            end = center + 3;
        }
        if(start < 0){
            end -= start;
            start = 0;
        }
        else if(end >= length){
            start -= (end - (length - 1));
            end = length - 1;
        }
        realButtonStart = start + 16;
        realButtonEnd = end + 16;
    }

    public boolean onMouseOver(int x, int y, boolean touch) {
        if(isPressed() && !cursorIsNearby()) {
            resetThumbPosition();
            return true;  // см. captureMouse
        }
        if(!isPressed() && !(0 <= x && x < width && 0 <= y && y < height))
            return false;
        if(touch && (0 <= x && x < width && 0 <= y && y < height)) {
            parent.startTouch = this;  // isPressed = true
            scrollable.handleScrollbarClick();
        }
        int length = isVertical? y : x;
        calculateThumbCoords();
        if(!disabled && thumbPress != -1 || (realButtonStart <= length && length <= realButtonEnd)) {  // ползунок
            // сделано так, т. к. курсор может вылезти за пределелы ползунка (особенно если ползунок маленький)
            mouseOverPart = THUMB;
            if(touch){
                thumbPress = length - realButtonStart;
                oldButtonStart = realButtonStart;
            }
            else if(isPressed() && thumbPress != -1){
                int oldWidth = realButtonEnd - realButtonStart;  // не совсем width, ну и ладно
                realButtonStart = length - thumbPress;
                realButtonEnd = realButtonStart + oldWidth;
                if(realButtonStart < 16) {
                    realButtonStart = 16;
                    realButtonEnd = realButtonStart + oldWidth;
                }
                else if(realButtonEnd >= (isVertical? height : width) - 16){
                    realButtonEnd = (isVertical? height : width) - 16 - 1;
                    realButtonStart = realButtonEnd - oldWidth;
                }
                startPosition = (double) (realButtonStart - 16) / ((isVertical? height : width) - 32);
                endPosition = (double) (realButtonEnd - 16) / ((isVertical? height : width) - 33);
                scrollable.onScrollbarMoved();
            }
        }
        else if(0 <= length && length < 16) {  // кнопка наверх/влево
            mouseOverPart = UP_BUTTON;
            if(touch)
                startAction(scrollUpR);
        }
        else if(length < realButtonStart && !disabled) {  // зазор между кнопкой и ползунком
            mouseOverPart = PAGE_UP;
            if(touch)
                startAction(pageUp);
        }
        else if(length < (isVertical? height : width) - 16 && !disabled) {  // 2-й зазор между кнопкой и ползунком
            mouseOverPart = PAGE_DOWN;
            if(touch)
                startAction(pageDown);
        }
        else if(length >= (isVertical? height : width) - 16 && length < (isVertical? height : width)){  // кнопка вниз/вправо
            mouseOverPart = DOWN_BUTTON;
            if(touch)
                startAction(scrollDownR);
        }
        else{
            mouseOverPart = OUTSIDE;
        }
        if(!touch && lastMouseOverPart != mouseOverPart){
            mouseOverPart = OUTSIDE;
            stopAction();
        }
        lastMouseOverPart = mouseOverPart;
        return (0 <= x && x < width && 0 <= y && y < height) || isPressed();  // см. captureMouse
    }

    @Override
    public void onClick(int x, int y) {
        //isPressed = false;
        thumbPress = -1;
        stopAction();
    }

    private void resetThumbPosition() {  // если курсор слишком далеко
        if(thumbPress == -1)
            return;
        int oldWidth = realButtonEnd - realButtonStart;  // не совсем width, ну и ладно
        realButtonStart = oldButtonStart;
        realButtonEnd = realButtonStart + oldWidth;
        startPosition = (double) (realButtonStart - 16) / ((isVertical ? height : width) - 32);
        endPosition = (double) (realButtonEnd - 16) / ((isVertical ? height : width) - 33);
        scrollable.onScrollbarMoved();
    }

    private boolean cursorIsNearby() {  // курсор отстоит от нас менее, чем на 32 пикселя
        int x = getCursorX() - getAbsoluteX(), y = getCursorY() - getAbsoluteY();
        return -32 <= x && x < width + 32 && -32 <= y && y < height + 32;
    }

    @Override
    public void onMouseLeave() {
        if(thumbPress == -1){  // нажимали на кнопку вверх/вниз
            stopAction();
        }
    }

    private void startAction(Runnable action){
        // Выполнять какое-то действие, например скроллинг вниз.
        // Когда мы зажимаем кнопку, происходит действие;
        // если мы продолжаем держать кнопку, то через 500 мс действие начинает выполняться раз в 100 мс
        curAction = action;
        action.run();
        WindowsView.handler.postDelayed(actionRunnable, 500);
    }
    private void stopAction(){
        WindowsView.handler.removeCallbacks(actionRunnable);
    }

    public void setDisabled(boolean disabled){
        this.disabled = disabled;
        if(disabled) {
            stopAction();
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void prepareForDelete(){
        stopAction();
    }

    @Override
    public void onWindowResize(boolean maximized) {
        if(maximizedBounds == null)
            return;
        setBounds(maximized? maximizedBounds : minimizedBounds);
    }
}
