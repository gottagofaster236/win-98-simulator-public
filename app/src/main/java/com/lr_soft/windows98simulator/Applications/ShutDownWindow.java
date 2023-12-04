package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.RadioButton;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;

public class ShutDownWindow extends Window {
    private Bitmap iconBmp;
    private DitherPainter ditherPainter;
    private boolean logoff;

    public ShutDownWindow(boolean logoff){
        super(logoff? "Log Off Windows" : "Shut Down Windows", null,
                logoff? 288 : 323, logoff? 123 : 173, false, false, true);
        this.logoff = logoff;
        ditherPainter = new DitherPainter(Color.TRANSPARENT, Color.BLACK);
        ditherPainter.ignorePosition = true;
        Windows98.windows98.topElement = this;

        if(!logoff) {
            centerWindowHorizontally();
            y = 100;
            iconBmp = getBmp(R.drawable.shut_down_with_computer_0);

            final RadioButton shutDown = new RadioButton("Shut down");
            final RadioButton restart = new RadioButton("Restart");
            RadioButton restartDos = new RadioButton("Restart in MS-DOS mode");
            shutDown.active = true;
            RadioButton.createGroup(shutDown, restart, restartDos);
            addElement(shutDown, 61, 67);
            addElement(restart, 61, 87);
            addElement(restartDos, 61, 106);

            Button ok = new Button("OK", new Rect(62, 139, 140, 162), parent -> {
                if(shutDown.active)
                    Windows98.windows98.shutdown();
                else if(restart.active)
                    Windows98.windows98.restart();
                else
                    Windows98.windows98.restartInMsDosMode();
            });
            ok.coolActive = true;
            defaultButton = ok;
            addElement(ok);

            addCloseButton(new Rect(146, 139, 224, 162), "Cancel");
            addElement(new Button("Help", new Rect(230, 139, 308, 162), null));
        }
        else{
            iconBmp = getBmp(R.drawable.logoff_key_big);
            Button yes = new Button("Yes", new Rect(77, 84, 142, 107),
                    parent -> Windows98.windows98.shutdown());
            yes.coolActive = true;
            defaultButton = yes;
            addElement(yes);
            addCloseButton(new Rect(152, 84, 217, 107), "No");
            centerWindowOnScreen();
        }
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        drawDitherRect(canvas, 0, 0, Windows98.SCREEN_WIDTH, Windows98.SCREEN_HEIGHT, ditherPainter);
        super.onNewDraw(canvas, x, y);
        canvas.drawBitmap(iconBmp, x + (logoff? 24 : 14), y + 40, null);
        p.setColor(Color.BLACK);
        if(!logoff)
            canvas.drawText("What do you want the computer to do?", x + 60, y + 51, p);
        else
            canvas.drawText("Are you sure you want to log off?", x + 77, y + 57, p);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        super.onMouseOver(x, y, touch);
        return true;
    }

    @Override
    public void onSelfOtherTouch() {
        topElement = this;  // на случай, если будет создан MessageBox или типа того
    }
}
