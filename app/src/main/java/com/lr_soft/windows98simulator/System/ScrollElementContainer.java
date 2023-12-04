package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.WindowsView;

public class ScrollElementContainer extends ElementContainer implements Scrollable {  // ElementContainer со скроллингом по оси Y
    public int scrollRange, scrollExtent; // см. View
    public int scrollY = 0;
    private int lineHeight;  // на сколько скроллим при нажатии кнопок вверх/вниз
    public Bitmap drawingBitmap;
    private Canvas drawingCanvas;  // так как ярлыки файлов могут вылезать за границы
    public ScrollBar scrollBar;
    protected Rect minimizedBounds, maximizedBounds;
    private Rect src = new Rect(), dst = new Rect();

    public ScrollElementContainer(Rect minimizedBounds, Rect maximizedBounds, int lineHeight, Bitmap drawingBitmap){
        this.minimizedBounds = minimizedBounds;
        this.maximizedBounds = maximizedBounds;
        setBounds(minimizedBounds);
        this.lineHeight = lineHeight;
        if(drawingBitmap == null) {
            if (maximizedBounds != null)
                this.drawingBitmap = createBitmap(maximizedBounds.width(), maximizedBounds.height(), Bitmap.Config.ARGB_8888);
            else
                this.drawingBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        else
            this.drawingBitmap = drawingBitmap;
        drawingCanvas = new Canvas(this.drawingBitmap);

        /*if(maximizedBounds == null)
            scrollBar = new ScrollBar(this, new Rect(minimizedBounds.right, minimizedBounds.top, minimizedBounds.right + 16, minimizedBounds.bottom), true);
        else
            scrollBar = new ScrollBar(this, new Rect(minimizedBounds.right, minimizedBounds.top, minimizedBounds.right + 16, minimizedBounds.bottom),
                    new Rect(maximizedBounds.right, maximizedBounds.top, maximizedBounds.right + 16, maximizedBounds.bottom), true);
        elements.add(scrollBar);  // должен быть последним среди элементов  */
        for(Element link : elements)
            link.parent = this;  // так как в случае MS-DOS не вызываются ни onDraw, ни onMouseOver
        //bringClickedElementToFront = true;
    }

    public ScrollElementContainer(Rect bounds, int lineHeight){
        this(bounds, null, lineHeight, null);
    }
    /*private void checkScrollbarPosition(){
        if(elements.get(elements.size() - 1) != scrollBar){
            elements.remove(scrollBar);
            elements.add(scrollBar);
        }
    }*/

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        if(drawingBitmap != WindowsView.canvas_bmp)
            drawHole(drawingCanvas, 0, 0, width, height);
        drawElements(drawingCanvas, 0, -scrollY);
        src.right = width;
        src.bottom = height;
        dst.left = x;
        dst.top = y;
        dst.right = dst.left + width;
        dst.bottom = dst.top + height;
        if(drawingBitmap != WindowsView.canvas_bmp)
            canvas.drawBitmap(drawingBitmap, src, dst, null);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        return super.onMouseOver(x, y + scrollY, touch);
    }

    @Override
    public void onClick(int x, int y) {
        super.onClick(x, y + scrollY);
    }

    @Override
    public void onDoubleClick(int x, int y) {
        super.onDoubleClick(x, y + scrollY);
    }

    @Override
    public void onRightClick(int x, int y) {
        super.onRightClick(x, y + scrollY);
    }

    @Override
    public void onOtherTouch() {
        super.onOtherTouch();
    }

    @Override
    public void scrollUpDown(boolean up, boolean vertical) {
        if(up)
            scrollY -= lineHeight;
        else
            scrollY += lineHeight;
        updateScrollBar();
    }
    @Override
    public void pageUpDown(boolean up, boolean vertical) {
        if(up)
            scrollY -= scrollExtent;
        else
            scrollY += scrollExtent;
        updateScrollBar();
    }

    @Override
    public void onScrollbarMoved() {
        scrollY = (int) Math.round(scrollBar.startPosition * scrollRange);
        if(scrollBar.endPosition == 1){
            scrollY = scrollRange - scrollExtent;
        }
    }

    @Override
    public void handleScrollbarClick() {}

    @Override
    public void onWindowResize(boolean maximized) {  // вызывать либо с параметром true (false ломает FileDialog), либо с настоящим параметром (maximized)
        setBounds(maximized? maximizedBounds : minimizedBounds);
        updateScrollBar();
    }

    public void updateScrollBar(){
        if(scrollY < 0)
            scrollY = 0;
        else if(scrollY + scrollExtent > scrollRange)
            scrollY = Math.max(0, scrollRange - scrollExtent);
        if(scrollExtent < scrollRange) {
            scrollBar.setDisabled(false);
            scrollBar.visible = true;
            scrollBar.startPosition = (double) scrollY / scrollRange;
            scrollBar.endPosition = (double) (scrollY + scrollExtent) / scrollRange;
        }
        else {
            scrollBar.visible = false;
            scrollBar.setDisabled(true);
        }
    }

    @Override
    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        scrollExtent = height;
    }
}
