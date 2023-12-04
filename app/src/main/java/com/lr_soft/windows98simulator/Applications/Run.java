package com.lr_soft.windows98simulator.Applications;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.WindowsView;

import java.util.StringTokenizer;

public class Run extends BaseNotepad {
    public Run(){
        super("Run", false,
                new Rect(59, 84, 314, 101), 2, 12, p, new Rect(-1, -11, 0, 3),
                R.drawable.run_window, new Rect(177, 124, 252, 147), "Cancel");
        ((Button) elements.get(0)).coolActive = false;  // так как уже есть другая кнопка, неактивный OK
        Button ok = new Button("OK", 75, 23, 96, 124, Color.BLACK, true, parent -> {
            if(runAction())
                close();
        });
        ok.coolActive = true;
        ok.disabled = true;  // потому, что textBox пустой
        defaultButton = ok;
        addElement(ok);
        addElement(new Button("Browse...", 75, 23, 258, 124, Color.BLACK, true, null));
    }

    private boolean runAction(){  // возвращает успех выполнения
        String text = textBox.text;

        Class<? extends Window> windowClass = getWindowClass(text);
        if(windowClass != null) {
            try {
                windowClass.newInstance();
            }
            catch (IllegalAccessException ignored) {}
            catch (InstantiationException ignored) {}

            return true;
        }

        if((text.contains("/") || text.contains("\\"))
                && (text.contains("con") || text.contains("prn") || text.contains("nul") || text.contains("aux") || text.contains("lpt"))){
            WindowsView.windowsView.slowBSOD();
            return false;
        }

        new MessageBox(text, "Cannot find the file '" + text + "' (or one of its components). Make sure the path and filename are correct and that all required libraries are available.",
                MessageBox.OK, MessageBox.ERROR, null, this);
        return false;
    }

    public static Class<? extends Window> getWindowClass(String text){
        if(text.isEmpty())
            return null;
        StringTokenizer stringTokenizer = new StringTokenizer(text, " ");
        if(!stringTokenizer.hasMoreElements())
            return null;
        text = stringTokenizer.nextToken();  // если есть пробелы - берем первое слово
        text = text.toLowerCase();

        if(text.equals("command.com") || text.equals("command")){
            return MsDos.class;
        }
        if(text.endsWith(".exe"))
            text = text.substring(0, text.length() - 4);

        switch (text){
            case "mspaint": case "paint":
                return PaintBrush.class;
            case "explorer":
                return DriveC.class;
            case "calculator": case "calc":
                return Calculator.class;
            case "solitaire": case "sol":
                return Solitaire.class;
            case "minesweeper": case "winmine":
                return Minesweeper.class;
            case "iexplore":
                return InternetExplorer.class;
            case "wordpad":
                return WordPad.class;
            case "notepad":
                return Notepad.class;
            case "freecell":
                return FreeCell.class;
            case "mplayer2":
                return MPlayer.class;
            case "control":
                return ControlPanel.class;
            case "cmd":
                return MsDos.class;
        }
        return null;
    }

    @Override
    public void onKeyPress(String key) {
        if(key.equals("\n")) {
            if(runAction())
                close();
        }
        super.onKeyPress(key);
        defaultButton.disabled = textBox.text.isEmpty(); // если текст пустой, то кнопка OK серая
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        if(active) {
            boolean containsActiveButton = false;  // костыль, чтобы кнопка ОК не теряла обводку при нажатии на TextBox
            for (Element element : elements) {
                if (element instanceof Button && ((Button) element).coolActive) {
                    containsActiveButton = true;
                    break;
                }
            }
            if (!containsActiveButton)
                defaultButton.coolActive = true;
        }
        super.onNewDraw(canvas, x, y);
    }
}
