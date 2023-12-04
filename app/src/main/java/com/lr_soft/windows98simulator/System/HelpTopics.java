package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;


public class HelpTopics extends Window {
    Bitmap[] pages;
    int cur_page = 0;
    private Rect pageButton = new Rect();  // для onMouseOver, чтобы без allocation on draw
    boolean hasFirstFolder;  // первая кнопка - это папка, её надо пропустить
    private static Bitmap firstPage;  // так как Java обязывает вызывать super() первой строчкой

    public HelpTopics(String windowTitle, boolean hasFirstFolder, int[] pages_ids){
        super(windowTitle, getBmp(R.drawable.help_topics), getWidth(pages_ids[0]), firstPage.getHeight(), true, false, false);
        this.hasFirstFolder = hasFirstFolder;
        fillWindow = false;
        unableToMaximize = true;
        pages = new Bitmap[pages_ids.length];
        pages[0] = firstPage;
        firstPage = null;
        for(int i = 1; i < pages_ids.length; i++){
            pages[i] = getBmp(pages_ids[i]);
        }

        Bitmap[] topButtons = {getBmp(R.drawable.hide), getBmp(R.drawable.back_help), getBmp(R.drawable.options), getBmp(R.drawable.web_help)};
        BigTopButtons bigTopButtons = new BigTopButtons(topButtons,
                new int[][]{{4, 58}, {59, 113}, {169, 223}, {224, 278}}, 4, null);
        bigTopButtons.y = 23;
        addElement(bigTopButtons);

        centerWindowOnScreen();
        y = y_old = 0;
    }

    private static int getWidth(int firstPageId){
        firstPage = getBmp(firstPageId);
        return firstPage.getWidth();
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        canvas.drawBitmap(pages[cur_page], x, y, null);
        super.onNewDraw(canvas, x, y);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        boolean return_value = super.onMouseOver(x, y, touch);
        if(!touch)
            return return_value;
        pageButton.left = 15; pageButton.top = 101;
        pageButton.right = 208; pageButton.bottom = 116;
        if(hasFirstFolder){
            pageButton.top += 16;
            pageButton.bottom += 16;
        }
        for(int i=0; i<pages.length; i++){
            if(pageButton.contains(x, y)) {
                cur_page = i;
                break;
            }
            pageButton.top += 16;
            pageButton.bottom += 16;
        }
        return return_value;
    }
}
