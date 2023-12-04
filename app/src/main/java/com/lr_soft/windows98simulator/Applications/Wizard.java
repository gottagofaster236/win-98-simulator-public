package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.Window;

public class Wizard extends Window {  // мастер по установке
    Bitmap[] pages;
    int curPage = 0;
    Button back, next, finish;
    boolean ableToFinish;
    public Wizard(String windowTitle, boolean ableToFinish, Bitmap[] pages){
        super(windowTitle, null, pages[0].getWidth(), pages[0].getHeight(), false, false, true);
        fillWindow = false;
        this.pages = pages;
        this.ableToFinish = ableToFinish;

        back = new Button("< Back", 75, 23, 193, height - 36, Color.BLACK, true, parent -> {
            curPage--;
            updateButtons();
        });
        addElement(back);

        next = new Button("Next >", 75, 23, 268, height - 36, Color.BLACK, true, parent -> {
            curPage++;
            updateButtons();
        });
        addElement(next);

        addElement(new Button("Cancel", 75, 23, 353, height - 36, Color.BLACK, true, parent -> close()));

        finish = new Button("Finish", 75, 23, 268, height - 36, Color.BLACK, true, parent -> close());
        addElement(finish);
		
		updateButtons();
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        canvas.drawBitmap(pages[curPage], x, y, null);
        super.onNewDraw(canvas, x, y);
    }

    private void updateButtons(){  // мы перешли на другую страницу, какие-то кнопки могут поменять своё состояние
        back.disabled = (curPage == 0);
        next.disabled = (curPage == pages.length - 1);
        finish.visible = (curPage == pages.length - 1 && ableToFinish);  // finish будет перекрывать next
        if(curPage == pages.length - 1){  // последняя страница
            if(ableToFinish) {
                finish.coolActive = true;
                defaultButton = finish;
            }
            else{
                back.coolActive = true;
                defaultButton = back;
            }
        }
        else{
            next.coolActive = true;
            back.coolActive = false;
            defaultButton = next;
        }
    }
}
