package com.lr_soft.windows98simulator.System;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.lr_soft.windows98simulator.MainActivity;
import com.lr_soft.windows98simulator.WindowsView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public abstract class Element {
    public ElementContainer parent;
    public ContextMenu contextMenu = null;
    public int x = 0, y = 0;  // относительно предка
    public int width = -1, height = -1;
    public boolean visible = true;
    protected long lastClickTime = -1;  // для double click
    protected boolean captureMouse = false;  // костыль, чтобы можно было перемещать мышью за границами элемента.
    // правильная реализация onMouseOver в таком случае - возвращать true если мышь над элементом или он нажат
    public static Paint p = new Paint(), p_bold, p_very_bold, p_fixedsys, p_dos, p_dos_mode, p_system;  // инициализируются в Windows98
    public static Resources resources;
    public static MainActivity context;

    public abstract void onDraw(Canvas canvas, int x, int y);  // отрисовка. x, y - абсолютные координаты, где надо отрисовываться

    public abstract boolean onMouseOver(int x, int y, boolean touch); // Проверить, наведена ли на нас мышь. Touch - при событии "нажатие на мышь" (но не клике)

    public void onClick(int x, int y) {} // событие поднятия левой кнопки мыши (при условии, что она была нажата на нас)

    public void onDoubleClick(int x, int y) {
        onClick(x, y);
    }  // двойной клик на нас

    public void onMouseLeave() {}  // на нас больше не наведена мышь

    public void onOtherTouch() {  // на кого-то другого нажали
        onMouseLeave();
    }

    public void onRightClick(int x, int y) {  // на нас кликнули правой кнопкой мыши
        if (contextMenu != null) {
            contextMenu.reset();
            contextMenu.x = x;
            contextMenu.y = y;
            contextMenu.active = true;
        }
    }

    public void onKeyPress(String key){}

    /*public int getTextLength(){
        return -1;
    }*/

    public void prepareForDelete(){}  // Убрать за собой. Чаще всего нужно, чтобы убрать Runnable из handler'а (иначе сборка мусора невозможна)

    public void onWindowResize(boolean maximized){}  // вызывается, когда меняются размеры окна

    public void setBounds(Rect bounds){
        setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public void setBounds(int left, int top, int right, int bottom){
        x = left;
        y = top;
        width = right - left;
        height = bottom - top;
    }

    public int getAbsoluteX(){
        int ret = 0;
        Element cur = this;
        while(cur != null){
            ret += cur.x;
            cur = cur.parent;
        }
        return ret;
    }

    public int getAbsoluteY(){
        int res = 0;
        Element cur = this;
        while(cur != null){
            res += cur.y;
            cur = cur.parent;
        }
        return res;
    }

    public void removeFromParent(){
        parent.elements.remove(this);
        if(parent.topElement == this)
            parent.topElement = null;
        if(parent.inputFocus == this)
            parent.inputFocus = null;
        if(parent.startTouch == this)
            parent.startTouch = null;
        prepareForDelete();
    }

    public boolean isPressed(){
        if(parent == null)
            return false;
        return this == parent.startTouch;
    }

    public void requestInputFocus(){  // рекурсивно установить inputFocus
        Element cur = this;
        while(cur.parent != null){
            cur.parent.inputFocus = cur;
            cur = cur.parent;
        }
    }

    // далее идёт "библиотека полезных функций"
    public static void drawFrameRect(Canvas c, int x1, int y1, int x2, int y2) {
        drawFrameRect(c, x1, y1, x2, y2, true);
    }

    public static void drawFrameRect(Canvas c, int x1, int y1, int x2, int y2, boolean fillRect){
        drawFrameRect(c, x1, y1, x2, y2, fillRect, false);
    }

    public static void drawFrameRect(Canvas c, int x1, int y1, int x2, int y2, boolean fillRect, boolean shiftWhiteLines) {
        p.setColor(Color.parseColor("#C0C7C8"));
        if (fillRect) {
            c.drawRect(x1, y1, x2, y2, p);
        } else {
            c.drawRect(x1, y1, x1 + 1, y2, p);
            c.drawRect(x1, y1, x2, y1 + 1, p);
        }
        // Gray shades
        p.setColor(Color.parseColor("#87888F"));
        c.drawRect(x1 + 1, y2 - 2, x2 - 1, y2 - 1, p);
        c.drawRect(x2 - 2, y1 + 1, x2 - 1, y2, p);
        // White lines
        p.setColor(Color.WHITE);
        if(shiftWhiteLines){
            x1--; y1--; x2++; y2++;
        }
        c.drawRect(x1 + 1, y1 + 1, x1 + 2, y2 - 2, p);
        c.drawRect(x1 + 1, y1 + 1, x2 - 2, y1 + 2, p);
        if(shiftWhiteLines){
            x1++; y1++; x2--; y2--;
        }
        // Black lines
        p.setColor(Color.BLACK);
        c.drawRect(x1, y2 - 1, x2, y2, p);
        c.drawRect(x2 - 1, y1, x2, y2, p);
    }

    public static void drawFrameRectActive(Canvas c, int x1, int y1, int x2, int y2) {
        p.setColor(Color.parseColor("#C0C7C8"));
        p.setStyle(Paint.Style.FILL);
        c.drawRect(x1, y1, x2, y2, p);
        // Black lines
        p.setColor(Color.BLACK);
        c.drawRect(x1 + 1, y1 + 1, x1 + 2, y2 - 2, p);
        c.drawRect(x1 + 1, y1 + 1, x2 - 2, y1 + 2, p);
        // Gray shades
        p.setColor(Color.parseColor("#87888F"));
        c.drawRect(x1, y1, x1 + 1, y2 - 1, p);
        c.drawRect(x1, y1, x2 - 1, y1 + 1, p);
        // White lines
        p.setColor(Color.WHITE);
        c.drawRect(x1, y2 - 1, x2, y2, p);
        c.drawRect(x2 - 1, y1, x2, y2, p);
    }

    public static void drawSimpleFrameRect(Canvas c, int x1, int y1, int x2, int y2) {  // нарисовать рамку шириной 2, а не 3. используется, например, в кнопках таксбара
        p.setColor(Color.parseColor("#C0C7C8"));
        p.setStyle(Paint.Style.FILL);
        //c.drawRect(x1, y1, x2, y2, p);  - не заполняет рамку. Осторожно
        // Gray shades
        p.setColor(Color.parseColor("#87888F"));
        c.drawRect(x1 + 1, y2 - 2, x2 - 1, y2 - 1, p);
        c.drawRect(x2 - 2, y1 + 1, x2 - 1, y2, p);
        // White lines
        p.setColor(Color.WHITE);
        c.drawRect(x1, y1, x1 + 1, y2 - 1, p);
        c.drawRect(x1, y1, x2 - 1, y1 + 1, p);
        // Black lines
        p.setColor(Color.BLACK);
        c.drawRect(x1, y2 - 1, x2, y2, p);
        c.drawRect(x2 - 1, y1, x2, y2, p);
    }

    public static void drawSimpleFrameRectActive(Canvas c, int x1, int y1, int x2, int y2) {  // нарисовать рамку шириной 2, а не 3
        p.setColor(Color.parseColor("#C0C7C8"));
        p.setStyle(Paint.Style.FILL);
        //c.drawRect(x1, y1, x2, y2, p);
        // Gray shades
        p.setColor(Color.parseColor("#87888F"));
        c.drawRect(x1 + 1, y1 + 1, x1 + 2, y2 - 2, p);
        c.drawRect(x1 + 1, y1 + 1, x2 - 2, y1 + 2, p);
        // Black lines
        p.setColor(Color.BLACK);
        c.drawRect(x1, y1, x1 + 1, y2 - 1, p);
        c.drawRect(x1, y1, x2 - 1, y1 + 1, p);
        // White lines
        p.setColor(Color.WHITE);
        c.drawRect(x1, y2 - 1, x2, y2, p);
        c.drawRect(x2 - 1, y1, x2, y2, p);
    }

    public static void drawVerySimpleFrameRect(Canvas c, int x1, int y1, int x2, int y2){
        drawVerySimpleFrameRect(c, x1, y1, x2, y2, true);
    }

    public static void drawVerySimpleFrameRect(Canvas c, int x1, int y1, int x2, int y2, boolean active){  // рамка шириной 1 пиксель, без заполнения!
        // active - вогнутый
        int grey = Color.rgb(135, 136, 143);
        p.setColor(active? grey : Color.WHITE);
        c.drawRect(x1, y1, x1 + 1, y2, p);
        c.drawRect(x1, y1, x2, y1 + 1, p);
        p.setColor(active? Color.WHITE : grey);
        c.drawRect(x1, y2 - 1, x2, y2, p);
        c.drawRect(x2 - 1, y1, x2, y2, p);
    }

    public static void drawBlueRectWithYellowBorder(Canvas canvas, Rect rect, boolean drawBlackPixels) {
        drawBlueRectWithYellowBorder(canvas, rect.left, rect.top, rect.right, rect.bottom, drawBlackPixels);
    }
    public static void drawBlueRectWithYellowBorder(Canvas canvas, int left, int top, int right, int bottom, boolean drawBlackPixels){  // sorry for copypaste
        p.setColor(Color.parseColor("#0000A8"));  // синий
        canvas.drawRect(left, top, right, bottom, p);
        p.setColor(Color.YELLOW);  // жёлтый пунктир
        for(int i = left; i < right; i++){
            if ((i + top - left - right) % 2 == 0)
                canvas.drawPoint(i, top, p);
            else if(drawBlackPixels){
                p.setColor(Color.BLACK);
                canvas.drawPoint(i, top, p);
                p.setColor(Color.YELLOW);
            }
            if ((i + bottom - 1 - left - right) % 2 == 0)
                canvas.drawPoint(i, bottom - 1, p);
            else if(drawBlackPixels){
                p.setColor(Color.BLACK);
                canvas.drawPoint(i, bottom - 1, p);
                p.setColor(Color.YELLOW);
            }
        }
        for(int i = top; i < bottom; i++){
            if ((i + left - left - right) % 2 == 0)
                canvas.drawPoint(left, i, p);
            else if(drawBlackPixels){
                p.setColor(Color.BLACK);
                canvas.drawPoint(left, i, p);
                p.setColor(Color.YELLOW);
            }
            if ((i + right - 1 - left - right) % 2 == 0)
                canvas.drawPoint(right - 1, i, p);
            else if(drawBlackPixels){
                p.setColor(Color.BLACK);
                canvas.drawPoint(right - 1, i, p);
                p.setColor(Color.YELLOW);
            }
        }
    }

    public static boolean fitsInScreen(Element e, int draw_x, int draw_y) {
        return 0 <= draw_x && e.width + draw_x <= Windows98.SCREEN_WIDTH && 0 <= draw_y && e.height + draw_y <= Windows98.TASKBAR_Y;
    }

    private static SparseArray<WeakReference<Bitmap>> bitmaps = new SparseArray<>();

    public static Bitmap getBmp(int resId) {
        //return BitmapFactory.decodeResource(resources, resId);
        Bitmap bmp;
        if(bitmaps.get(resId) == null || (bmp = bitmaps.get(resId).get()) == null) {
            if(resId != 0) {
                bmp = BitmapFactory.decodeResource(resources, resId);
                bmp.setDensity(Bitmap.DENSITY_NONE);
            }
            else {  // dummy ресурс
                bmp = createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }
            bitmaps.put(resId, new WeakReference<>(bmp));
        }
        return bmp;
    }

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
        Bitmap result = Bitmap.createBitmap(width, height, config);
        result.setDensity(Bitmap.DENSITY_NONE);
        return result;
    }

    public static String[] splitTextIntoLines(String text, int widthLimit, Paint p) {  // слишком длинные слова просто вылезают за правую границу
        return splitTextIntoLines(text, widthLimit, -1, p, false, false);
    }

    public static String[] splitTextIntoLines(String text, int widthLimit, int linesLimit, Paint p) {
        return splitTextIntoLines(text, widthLimit, linesLimit, p, false, false);
    }

    public static String[] splitTextIntoLines(String text, int widthLimit, int linesLimit, Paint p, boolean splitWords, boolean saveDelimiters){  // разбивает текст на строчки
        // splitWords - разделять слишком длинное слово на 2 или более строчек (иначе оставлять его вылезать за край экрана)
        // saveDelimiters - в конце каждой строки есть её разделитель (либо пробел, либо \n, нужно чтобы сохранялась длина текста)

        // разделяем text на слова, пробелы и \n включаем в конец слова
        List<String> words = new ArrayList<>();
        StringBuilder curWord = new StringBuilder();
        boolean lastCharIsDelimiter = false;
        for(int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                lastCharIsDelimiter = true;
                curWord.append(c);
                if (c == '\n') {
                    words.add(curWord.toString());
                    lastCharIsDelimiter = false;
                    curWord.setLength(0);
                }
            }
            else{
                if(lastCharIsDelimiter){
                    words.add(curWord.toString());
                    lastCharIsDelimiter = false;
                    curWord.setLength(0);
                }
                curWord.append(c);
            }
        }
        if(curWord.length() > 0)
            words.add(curWord.toString());

        StringBuilder curLine = new StringBuilder();
        List<String> result = new ArrayList<>();
        for(int i = 0; i < words.size(); i++){
            if(curLine.length() > 0 && curLine.charAt(curLine.length() - 1) == '\n'){  // если предыдущая (!) строка заканчивалась на \n
                result.add(curLine.toString());
                curLine.setLength(0);
            }
            String word = words.get(i);
            if(result.size() == linesLimit - 1){  // это последняя строчка, её делаем по-другому
                String leftWords = curLine + word;
                i++;
                while(i < words.size()){
                    leftWords += words.get(i);
                    i++;
                }
                result.add(shortenTextToThreeDots(leftWords, widthLimit, p));
                curLine.setLength(0);
                break;
            }
            if((splitWords || linesLimit != -1) && measureText(word, p) > widthLimit){
                if(curLine.length() > 0)
                    result.add(curLine.toString());
                int countDrawingSymbols = 0;
                if(linesLimit != -1) {  // ставим на конце три точки
                    result.add(shortenTextToThreeDots(word, widthLimit, p));
                    curLine.setLength(0);
                    break;
                }
                else{  // разделяем слово
                    while (measureText(word.substring(0, countDrawingSymbols + 1), p) <= widthLimit)
                        countDrawingSymbols++;
                    String newWord = word.substring(0, countDrawingSymbols);
                    String otherPart = word.substring(countDrawingSymbols);
                    words.add(i + 1, otherPart);
                    result.add(newWord);
                    curLine.setLength(0);
                    continue;
                }
            }
            if(curLine.length() == 0 || measureText(curLine + word, p) <= widthLimit) {
                curLine.append(word);
            }
            else{
                result.add(curLine.toString());
                curLine.setLength(0);
                curLine.append(word);
            }
        }
        if(curLine.length() > 0 || (!result.isEmpty() && result.get(result.size() - 1).endsWith("\n")))
            result.add(curLine.toString());
        // плохо, если текст кончается на \n, но нет новой пустой строки
        if(curLine.length() > 0 && curLine.charAt(curLine.length() - 1) == '\n'){
            //result.set(result.size() - 1, curLine.substring(0, curLine.length() - 1));
            result.add("");
        }
        if(result.isEmpty())  // иначе в Notepad не будет курсоров
            result.add("");
        String[] lines = new String[result.size()];
        result.toArray(lines);
        if(!saveDelimiters) {
            for (int i = 0; i < lines.length; i++) {
                while (lines[i].length() > 0 && Character.isWhitespace(lines[i].charAt(lines[i].length() - 1)))
                    lines[i] = lines[i].substring(0, lines[i].length() - 1);
            }
        }
        return lines;
    }

    public static float measureText(String text, Paint p){  // чтобы не учитывались переводы строк в конце
        int newlineCount = 0;
        while(newlineCount < text.length() && text.charAt(text.length() - 1 - newlineCount) == '\n')
            newlineCount++;
        text = text.substring(0, text.length() - newlineCount);
        return p.measureText(text);
    }

    public static String shortenTextToThreeDots(String text, int widthLimit, Paint p) {  // если текст слишком длинный, просто ставит на конце три точки
        if (measureText(text, p) > widthLimit) {
            int countDrawingSymbols = 0;
            // если ширина текста больше, чем widthLimit, то ширина текста и ещё трех точек тем более больше, так что мы не обратимся к неправильному индексу
            while (measureText(text.substring(0, countDrawingSymbols + 1) + "...", p) <= widthLimit)
                countDrawingSymbols++;
            return text.substring(0, countDrawingSymbols) + "...";
        } else
            return text;
    }

    public static int drawMultilineText(Canvas canvas, String[] lines, int x, int y, int shift_y, Paint p){
        return drawMultilineText(canvas, lines, x, y, shift_y, p, false);
    }

    public static int drawMultilineText(Canvas canvas, String[] lines, int x, int y, int shift_y, Paint p, boolean centerText){  // возвращает y, с которым рисовалась последняя строка
        int cur_y = y;
        for(String line : lines){
            int shift_x = 0;
            if(centerText)
                shift_x = -(((int) measureText(line, p)) / 2);
            canvas.drawText(line, x + shift_x, cur_y, p);
            cur_y += shift_y;
        }
        return y + (lines.length - 1) * shift_y;
    }

    /*public static boolean isAsciiString(String string){
        boolean isAsciiString = true;
        for(int i=0; i<string.length(); i++) {
            if (string.charAt(i) >= 128) {
                isAsciiString = false;
                break;
            }
        }
        return isAsciiString;
    }*/

    public static SharedPreferences getSharedPreferences(){
        return context.settings;
    }

    public static class DitherPainter {  // класс для (эффективной) отрисовки dither'а из двух цветов
        private Paint ditherPaint1 = new Paint(), ditherPaint2 = new Paint();
        public boolean ignorePosition = false;

        public DitherPainter(int color1, int color2){
            Bitmap ditherPattern1 = createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            Bitmap ditherPattern2 = createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            for(int i = 0; i < 2; i++){
                for(int j = 0; j < 2; j++){
                    if((i + j) % 2 == 0){
                        ditherPattern1.setPixel(i, j, color1);
                        ditherPattern2.setPixel(i, j, color2);
                    }
                    else{
                        ditherPattern1.setPixel(i, j, color2);
                        ditherPattern2.setPixel(i, j, color1);
                    }
                }
            }
            ditherPaint1.setShader(new BitmapShader(ditherPattern1, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
            ditherPaint2.setShader(new BitmapShader(ditherPattern2, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        }
    }

    protected void drawDitherRect(Canvas canvas, int left, int top, int right, int bottom, DitherPainter ditherPainter){
        Paint ditherPaint;
        if(!ditherPainter.ignorePosition)
            ditherPaint = (getAbsoluteX() + getAbsoluteY()) % 2 == 0? ditherPainter.ditherPaint1 : ditherPainter.ditherPaint2;
        else
            ditherPaint = ditherPainter.ditherPaint1;
        canvas.drawRect(left, top, right, bottom, ditherPaint);
    }

    static Paint replacePaint = new Paint();  // paint для того, чтобы рисовать прозрачность
    static {
        replacePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
    }

    public static void drawHole(Canvas canvas, int left, int top, int right, int bottom){
        replacePaint.setColor(Color.TRANSPARENT);
        canvas.drawRect(left, top, right, bottom, replacePaint);
        replacePaint.setColor(Color.WHITE);
    }

    /*public static void drawHole(Canvas canvas, Rect rect){
        drawHole(canvas, rect.left, rect.top, rect.right, rect.bottom);
    }*/

    public static int getCursorX(){  // возвращает абсолютное положение курсора на экране
        return (int) WindowsView.cursor_x;
    }
    public static int getCursorY(){
        return (int) WindowsView.cursor_y;
    }

    public static void copyFile(File source, File target) throws IOException {
        FileChannel inChannel = null, outChannel = null;
        try{
            inChannel = new FileInputStream(source).getChannel();
            outChannel = new FileOutputStream(target).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally{
            try{
                if (inChannel != null)
                    inChannel.close();
            }
            catch (IOException ignored){}
            try{
                if (outChannel != null)
                    outChannel.close();
            }
            catch (IOException ignored){}
        }
    }

    public static void makeSnackbar(int resId, int duration) {
        View windowsView = WindowsView.windowsView;
        if (windowsView == null) {
            return;
        }
        Snackbar snackbar = Snackbar.make(windowsView, resId, duration);
        View snackbarView = snackbar.getView();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) snackbarView.getLayoutParams();
        layoutParams.width = windowsView.getWidth();
        TextView tv = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        } else {
            tv.setGravity(Gravity.CENTER_HORIZONTAL);
        }
        snackbar.show();
    }

    public static final String TAG = "Debuggy!";
}
