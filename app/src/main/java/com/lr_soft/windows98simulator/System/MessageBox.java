package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.lr_soft.windows98simulator.R;

public class MessageBox extends DialogWindow {
    public static final int OK = 0, OKCANCEL = 1, RETRYCANCEL = 2, YESNO = 3, YESNOCANCEL = 4;  // кнопки
    public static final int WARNING = 0, ERROR = 1, INFO = 2, QUESTION = 3;  // иконки

    private static String[] linesTemp = null;  // потому что fukken Java обязывает первой строчкой вызывать super()
    private String[] lines;
    private Bitmap icon;

    public MessageBox(String title, String text, int buttons, int icon, final MsgResultListener msgResultListener, Window parentWindow){
        this(title, text, buttons, getIcon(icon), msgResultListener, parentWindow);
    }

    public MessageBox(String title, String text, int buttons, Bitmap icon, final MsgResultListener msgResultListener, Window parentWindow){
        super(title, getWidth(text, buttons), getHeight(), parentWindow);
        Windows98.chord.start();
        lines = linesTemp;
        linesTemp = null;
        this.icon = icon;
        String[] buttonTexts;
        switch (buttons){
            case OK:
                buttonTexts = new String[]{"OK"};
                disableCloseButton();  // крестик нельзя нажать, если нет кнопки Cancel (крестик и Cancel равносильны)
                break;
            case OKCANCEL:
                buttonTexts = new String[]{"OK", "Cancel"};
                break;
            case RETRYCANCEL:
                buttonTexts = new String[]{"Retry", "Cancel"};
                break;
            case YESNO:
                buttonTexts = new String[]{"Yes", "No"};
                disableCloseButton();
                break;
            case YESNOCANCEL:
                buttonTexts = new String[]{"Yes", "No", "Cancel"};
                break;
            default:
                 buttonTexts = new String[0];
                 break;
        }
        int buttonGroupWidth = buttonTexts.length * 75 + (buttonTexts.length - 1) * 6;
        int cur_x = (width - buttonGroupWidth) / 2;
        for(int i = 0; i < buttonTexts.length; i++){
            final int i_copy = i;  // aw yeah
            Button button = new Button(buttonTexts[i], 75, 23, cur_x, height - 14 - 23, Color.BLACK, true, parent -> {
                close();
                if(msgResultListener != null)
                    msgResultListener.onMsgResult(i_copy);
            });
            if(i == 0){  // первая кнопка должна быть выделена
                defaultButton = button;
                button.coolActive = true;
            }
            addElement(button);
            cur_x += 75 + 6;
        }

        centerWindowOnScreen();
    }

    private static Bitmap getIcon(int icon){
        switch (icon){
            case WARNING:
                return getBmp(R.drawable.msg_warning_0);
            case ERROR:
                return getBmp(R.drawable.msg_error_0);
            case INFO:
                return getBmp(R.drawable.msg_information_0);
            case QUESTION:
                return getBmp(R.drawable.msg_question_0);
            default:
                return null;
        }
    }

    private static int getWidth(String text, int buttons){  // для того, чтобы запихнуть в вызов super()
        linesTemp = splitTextIntoLines(text, 325, -1, p, true, false);
        float maxLength = 0;
        for(String line : linesTemp){
            maxLength = Math.max(maxLength, p.measureText(line));
        }
        int result = 14 + 32 + 17 + (int) maxLength + 13;
        int buttonsAmount;
        if(buttons == OK)
            buttonsAmount = 1;
        else if(buttons == OKCANCEL || buttons == RETRYCANCEL || buttons == YESNO)
            buttonsAmount = 2;
        else
            buttonsAmount = 3;
        result = Math.max(result, 2 * 14 + buttonsAmount * 75 + (buttonsAmount - 1) * 6);
        return result;
    }

    private static int getHeight(){
        int text_y_margin;  // y-координата отрисовки текста относительно иконки
        if(linesTemp.length <= 1)
            text_y_margin = 20;
        else if(linesTemp.length == 2)
            text_y_margin = 15;
        else
            text_y_margin = 11;
        return 33 + Math.max(text_y_margin + 13 * (linesTemp.length - 1), 32) + 18 + 23 + 14;
    }
    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        canvas.drawBitmap(icon, x + 14, y + 33, null);
        p.setColor(Color.BLACK);
        int text_y_margin;  // y-координата отрисовки текста относительно иконки
        if(lines.length <= 1)
            text_y_margin = 20;
        else if(lines.length == 2)
            text_y_margin = 15;
        else
            text_y_margin = 11;
        drawMultilineText(canvas, lines, x + 14 + 32 + 17, y + 33 + text_y_margin, 13, p);
    }

    public interface MsgResultListener {
        int YES = 0, NO = 1, RETRY = 0;
        void onMsgResult(int buttonNumber);
    }
}
