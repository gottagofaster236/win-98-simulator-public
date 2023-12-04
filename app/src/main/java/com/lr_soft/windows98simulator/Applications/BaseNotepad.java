package com.lr_soft.windows98simulator.Applications;

import android.graphics.Paint;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.System.DummyWindow;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.ScrollBar;
import com.lr_soft.windows98simulator.System.TextBox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;


public class BaseNotepad extends DummyWindow implements FileDialog.ActionOnSave {  // DummyWindow + TextBox
    TextBox textBox;
    private ScrollBar scrollBar = null;
    String appTitle = null;
    File openedFile = null;  // открытый в Notepad файл (реальный)
    boolean textChanged = false;
    private Runnable actionOnSave = null;  // см. FileDialog.ActionOnSave

    /*public BaseNotepad(String windowTitle, Rect textInput, Rect textInputMaximized, int leftMargin, int topMargin, Paint fontPaint, Rect cursor, int icon, int bmp1, int bmp2){
        this(windowTitle, textInput, textInputMaximized, null, null, leftMargin, topMargin, fontPaint, cursor, icon, bmp1, bmp2);
    }*/
    public BaseNotepad(String windowTitle, int icon, int bmp1, int bmp2){
        super(windowTitle, getBmp(icon), true, getBmp(bmp1), getBmp(bmp2));
    }

    public BaseNotepad(String windowTitle, Rect textInput, int leftMargin, int topMargin, Paint fontPaint, Rect cursor, int icon, int bmp1){
        super(windowTitle, getBmp(icon), true, getBmp(bmp1));
        addTextBox(textInput, leftMargin, topMargin, fontPaint, cursor);
    }
    public BaseNotepad(String windowTitle, boolean borderBold, Rect textInput, int leftMargin, int topMargin, Paint fontPaint, Rect cursor, int bmp1, Rect closeButton, String closeButtonText){  // messagewindow
        super(windowTitle, null, borderBold, getBmp(bmp1), null, closeButton, closeButtonText);
        addTextBox(textInput, leftMargin, topMargin, fontPaint, cursor);
    }

    private void addTextBox(Rect bounds, int leftMargin, int topMargin, Paint fontPaint, Rect cursor){
        textBox = new TextBox(bounds, leftMargin, topMargin, fontPaint, cursor);
        addElement(textBox);
        inputFocus = textBox;
    }

    void initTextAndScrollBar(RelativeBounds textInput, RelativeBounds scrollBar, int leftMargin, int topMargin, Paint fontPaint, Rect cursor){
        addTextBox(textInput.getMinimizedRect(), leftMargin, topMargin, fontPaint, cursor);
        textBox.maximizedBounds = textInput.getMaximizedRect();
        this.scrollBar = new ScrollBar(textBox, scrollBar, true);
        textBox.verticalScrollBar = this.scrollBar;
        elements.add(0, this.scrollBar);  // scrollBar должен быть ниже, чем TextBox
        textBox.updateScrollbarPosition();
    }

    @Override
    public void repositionElements() {
        if(scrollBar != null) {
            textBox.updateScrollbarPosition();
        }
    }

    FileDialog.OnResultListener getTxtWriter(){
        return new FileDialog.OnResultListener() {
            @Override
            public void writeToFile(File file) {
                boolean success = false;
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"))) {  // так как try-with-resources поддерживается только начиная с API 19
                    file.createNewFile();
                    writer.write(textBox.text);
                    success = true;
                }
                catch (IOException e) {
                    new MessageBox(getTitle(), "Failed to save file.", MessageBox.OK, MessageBox.ERROR, null, BaseNotepad.this);
                }
                if(success) {
                    textChanged = false;
                    openedFile = file;
                    if(appTitle != null)
                        setTitle(file.getName() + " - " + appTitle);
                    inputFocus = textBox;
                    textBox.setActive(true);
                }
            }

            @Override
            public void openFile(File file) {
                boolean success = false;
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))){
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    while((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append('\n');
                    }
                    textBox.setText(stringBuilder.toString());
                    if(appTitle != null)
                        setTitle(file.getName() + " - " + appTitle);
                    success = true;
                }
                catch (IOException e){
                    new MessageBox(getTitle(), "Failed to open file.", MessageBox.OK, MessageBox.ERROR, null, BaseNotepad.this);
                }
                if(success) {
                    inputFocus = textBox;
                    textBox.setActive(true);
                    textChanged = false;
                    BaseNotepad.this.openedFile = file;
                }
            }
        };
    }

    @Override
    public void onKeyPress(String key) {
        if(inputFocus == textBox){
            textChanged = true;
        }
        super.onKeyPress(key);
    }

    void saveAs(){
        new FileDialog(false, "txt", "Text Documents",
                this, getTxtWriter(), FileDialog.getDir(openedFile));
    }

    void open(){
        performActionWithSaveCheck(
                new Runnable() {
                    @Override
                    public void run() {
                        new FileDialog(true, "txt", "Text Documents",
                                BaseNotepad.this, getTxtWriter(), FileDialog.getDir(openedFile));
                    }
                }
        );
    }

    void save(){
        if(openedFile != null && openedFile.exists()) {
            getTxtWriter().writeToFile(openedFile);
            runActionOnSave();
            /*if(closingWindow){
                forceClose();
            }*/
        }
        else
            saveAs();
    }

    @Override
    public void close(final boolean activateNextWindow) {
        performActionWithSaveCheck(
                new Runnable() {
                    @Override
                    public void run() {
                        BaseNotepad.super.close(activateNextWindow);
                    }
                }
        );
    }

    void performActionWithSaveCheck(final Runnable action){  // если мы открываем файл, закрываем окно или создаём новый файл
        if(textChanged && appTitle != null) {  // appTitle = "Notepad" или "WordPad"
            // спрашиваем пользователя, хочет ли он сохранить файл
            new MessageBox(appTitle, "The text in the " + (openedFile == null ? "Untitled" : MyDocuments.getFullPath(openedFile)) + " file has changed.\n\nDo you want to save the changes?",
                    MessageBox.YESNOCANCEL, MessageBox.WARNING, new MessageBox.MsgResultListener() {
                @Override
                public void onMsgResult(int buttonNumber) {
                    if(buttonNumber == YES){  // YES
                        setActionOnSave(action);
                        save();
                    }
                    else if(buttonNumber == NO){  // NO
                        action.run();
                    }
                }
            }, this);
        }
        else
            action.run();
    }

    @Override
    public void setActionOnSave(Runnable actionOnSave) {
        this.actionOnSave = actionOnSave;
    }

    @Override
    public void runActionOnSave() {
        if(actionOnSave != null) {
            actionOnSave.run();
            actionOnSave = null;
        }
    }
}
