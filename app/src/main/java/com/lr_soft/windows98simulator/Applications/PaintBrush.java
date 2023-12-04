package com.lr_soft.windows98simulator.Applications;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.AboutWindow;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.Cursor;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.HelpTopics;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.ScrollBar;
import com.lr_soft.windows98simulator.System.Scrollable;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;
import com.lr_soft.windows98simulator.WindowsView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

public class PaintBrush extends Window implements FileDialog.ActionOnSave, Scrollable {
    int activeInstrument = PENCIL;
    final static int SELECT_FREE_FORM = 0, SELECT = 1, ERASER = 2, FILL = 3, PICK_COLOR = 4,
            MAGNIFIER = 5, PENCIL = 6, BRUSH = 7, SPRAY = 8, TEXT = 9, LINE = 10, CURVE = 11, RECT = 12,
            POLYGON = 13, ELLIPSE = 14, ROUND_RECT = 15;
    int color = Color.BLACK;
    Bitmap drawingBitmap;
    Canvas drawingCanvas;
    Rect src = new Rect(), dst = new Rect();
    boolean drawing = false;  // если сейчас мы рисуем с зажатой ЛКМ
    int start_x, start_y;  // где мы в первый раз нажали
    int last_x, last_y;
    int[] pixels;  // массивы для заливки (fillWithColor)
    private final int[] moves = {-1, 1};
    RectF tmpRectF = new RectF();  // чтобы рисовать овал в API до 21, надо использовать RectF
	private boolean calledSelfMouseOver = false;  // см. использования. Чтобы если курсор перешёл с области рисования на элемент, мы об этом узнали

    private File openedFile = null;
    private boolean imageChanged = false;
    private Runnable actionOnSave = null;  // см. BaseNotepad

    private int scrollX = 0, scrollY = 0;
    private ScrollBar verticalScrollBar, horizontalScrollBar;
    private Bitmap[] previousVersions;  // Для Undo. Предыдущие версии (в начале самые новые).
    private int amountOfPreviousVersions = 0;

    public final static String[] supportedFormats = {"png", "jpg", "jpeg", "gif", "bmp"};

    private Cursor defaultCursor, brush, eraser, fill, magnifier, pencil, pickColor, spray;
    private List<Cursor> allCursors;

    public PaintBrush(String filename, File path) {
        super(filename + " - Paint", getBmp(R.drawable.paint_0), Windows98.WIDESCREEN? 350 : 291, 416,
                true, true, false);
        // цвета, всего 28
        final String[][] colors_table = {{"#000000"}, {"#87888F"}, {"#A80057"}, {"#A8A857"}, {"#00A857"}, {"#57A8A8"}, {"#0000A8"}, {"#A857A8"},
                {"#A8A857", "#87888F"}, {"#57A8A8", "#000000"}, {"#00FFFF", "#0000FF"}, {"#57A8A8", "#0000A8"}, {"#FF00FF", "#0000FF"}, {"#A8A857", "#A80057"},
                {"#FFFFFF"}, {"#C0C7C8"}, {"#FF0000"}, {"#FFFF00"}, {"#00FF00"}, {"#00FFFF"}, {"#0000FF"}, {"#FF00FF"},
                {"#FFFF00", "#FFFFFF"}, {"#00FFFF", "#00FF00"}, {"#00FFFF", "#FFFFFF"}, {"#0000FF", "#FFFFFF"}, {"#FF00FF", "#FF0000"}, {"#FFFF00", "#FF0000"}
        };
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < colors_table.length / 2; j++) {
                int index = i * colors_table.length / 2 + j;
                if (colors_table[index].length == 1)
                    addElement(new ColorButton(Color.parseColor(colors_table[index][0])));
                else
                    addElement(new ColorButton(Color.parseColor(colors_table[index][0]), Color.parseColor(colors_table[index][1])));
            }
        }
        // инструменты
        Bitmap buttonsBmp = getBmp(R.drawable.paint_buttons);
        List<ImageSelectButton> instruments = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 2; j++) {
                int x = 8 + j * 25;
                int y = 43 + i * 25;
                instruments.add(new ImageSelectButton(x, y, buttonsBmp, instruments));
                if (i * 2 + j == 6)  // изначально активен карандаш
                    instruments.get(instruments.size() - 1).active = true;
            }
        }
        for (Element e : instruments)
            addElement(e);
        verticalScrollBar = new ScrollBar(this, new Rect(), true);
        horizontalScrollBar = new ScrollBar(this, new Rect(), false);
        addElement(verticalScrollBar);
        addElement(horizontalScrollBar);
        if (path == null) {
            setDrawingBitmap(createBitmap(Windows98.SCREEN_WIDTH - 61, Windows98.SCREEN_HEIGHT - 142, Bitmap.Config.ARGB_8888));
            drawingCanvas.drawColor(Color.WHITE);
        } else
            getBmpWriter().openFile(path);
        repositionElements();  // расставляет ColorButtons и ScrollBar
        // cursors
        defaultCursor = new Cursor(getBmp(R.drawable.paint_all), 16, 16);
        brush = new Cursor(getBmp(R.drawable.paint_brush), 9, 9);
        eraser = new Cursor(getBmp(R.drawable.paint_eraser), 4, 4);
        fill = new Cursor(getBmp(R.drawable.paint_fill), 2, 14);
        magnifier = new Cursor(getBmp(R.drawable.paint_magnifier), 10, 9);
        pencil = new Cursor(getBmp(R.drawable.paint_pencil), 13, 23);
        pickColor = new Cursor(getBmp(R.drawable.paint_pick_color), 9, 22);
        spray = new Cursor(getBmp(R.drawable.paint_spray), 0, 12);

        allCursors = Arrays.asList(defaultCursor, brush, eraser,
                fill, magnifier, pencil, pickColor, spray);

        setupTopMenu();
        onMouseOver(getCursorX() - getAbsoluteX(), getCursorY() - getAbsoluteY(), false);
		
        SharedPreferences sharedPreferences = getSharedPreferences();
        final String key = "paintTutorialShowedTimes";
        int showedTimes = sharedPreferences.getInt(key, 0);
        if (showedTimes < 6) {  // 6 раз показываем
            if(!Windows98.TAUON)
                makeSnackbar(R.string.paint_tutorial, 4000);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(key, showedTimes + 1);
            editor.apply();
        }
    }

    public PaintBrush(){
        this("untitled");
    }
    public PaintBrush(File path){
        this(path.getName(), path);
    }
    public PaintBrush(String filename){
        this(filename, null);
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        // серые линии
        p.setColor(Color.parseColor("#87888F"));
        canvas.drawRect(x + 4, y + 42, x + width - 5, y + 43, p);
        canvas.drawRect(x + 61, y + 43, x + 62, y + height - 77, p);
        canvas.drawRect(x + 4, y + height - 78, x + 62, y + height - 77, p);
        canvas.drawRect(x + 4, y + height - 29, x + width - 4, y + height - 28, p);
        canvas.drawRect(x + 4, y + height - 25, x + 169, y + height - 24, p);
        canvas.drawRect(x + 4, y + height - 25, x + 5, y + height - 5, p);
        canvas.drawRect(x + 172, y + height - 25, x + width - 5, y + height - 24, p);
        canvas.drawRect(x + 172, y + height - 25, x + 173, y + height - 5, p);
        canvas.drawRect(x + 12, y + 245, x + 13, y + 310, p);
        canvas.drawRect(x + 12, y + 245, x + 52, y + 246, p);
        canvas.drawRect(x + 5, y + height - 69, x + 34, y + height - 68, p);
        canvas.drawRect(x + 5, y + height - 69, x + 6, y + height - 38, p);
        // белые линии
        p.setColor(Color.WHITE);
        canvas.drawRect(x + 4, y + 43, x + 61, y + 44, p);
        canvas.drawRect(x + 4, y + height - 28, x + width - 4, y + height - 27, p);
        canvas.drawRect(x + 4, y + height - 77, x + width - 4, y + height - 76, p);
        canvas.drawRect(x + width - 5, y + 42, x + width - 4, y + height - 76, p);
        canvas.drawRect(x + 4, y + height - 5, x + 169, y + height - 4, p);
        canvas.drawRect(x + 169, y + height - 25, x + 170, y + height - 4, p);
        canvas.drawRect(x + 172, y + height - 5, x + width - 4, y + height - 4, p);
        canvas.drawRect(x + width - 5, y + height - 25, x + width - 4, y + height - 4, p);
        canvas.drawRect(x + 52, y + 246, x + 53, y + 311, p);
        canvas.drawRect(x + 13, y + 310, x + 52, y + 311, p);
        canvas.drawRect(x + 5, y + height - 38, x + 35, y + height - 37, p);
        canvas.drawRect(x + 35, y + height - 69, x + 36, y + height - 37, p);
        // черные линии
        p.setColor(Color.BLACK);
        canvas.drawRect(x + 62, y + 43, x + width - 6, y + 44, p);
        canvas.drawRect(x + 62, y + 43, x + 63, y + height - 78, p);
        canvas.drawRect(x + 6, y + height - 68, x + 34, y + height - 67, p);
        canvas.drawRect(x + 6, y + height - 68, x + 7, y + height - 39, p);
        canvas.drawText("For Help, click Help Topics on...", x + 7, y + height - 10, p);
        // рисуем выбранный цвет
        p.setColor(Color.WHITE);
        canvas.drawRect(x + 7, y + height - 67, x + 34, y + height - 39, p);
        p.setColor(Color.parseColor("#C0C7C8"));
        for(int i = x + 7; i < x + 34; i++){
            for(int j = y + height - 67; j < y + height - 39; j++){
                if((i + j - x - y) % 2 == 0)
                    canvas.drawPoint(i, j, p);
            }
        }
        drawColorSquare(canvas, x + 16, y + height - 57, Color.WHITE);
        drawColorSquare(canvas, x + 9, y + height - 64, color);
        // рисуем само изображение (drawingBitmap)
        dst.left = x + 63; dst.top = y + 44;
        dst.right = dst.left + getVisibleImageWidth();
        dst.bottom = dst.top + getVisibleImageHeight();
        src.left = scrollX;
        src.top = scrollY;
        src.right = src.left + dst.width();
        src.bottom = src.top + dst.height();
        p.setColor(Color.parseColor("#87888F"));  // серый фон если bitmap слишком маленький
        canvas.drawRect(dst, p);
        canvas.drawBitmap(drawingBitmap, src, dst, null);

        // рисуем промежуточные результаты, ещё не наложенные на canvas - например, линию
        if(drawing) {
            int cur_x = getCursorX();  // на какую координату мы сейчас навелись
            int cur_y = getCursorY();
            dst.left -= scrollX;
            dst.top -= scrollY;
            drawInstrumentPreview(canvas, dst.left + start_x, dst.top + start_y, cur_x, cur_y);
        }
    }

    @Override
    public void repositionElements() {
        for(int i = 0; i < 2; i++){
            for(int j = 0; j < 14; j++){
                int ind = i * 14 + j;
                Element colorButton = elements.get(ind);
                colorButton.x = 36 + j * 16;
                colorButton.y = height - 69 + i * 16;
            }
        }
        updateScrollbarsVisibility();

        horizontalScrollBar.x = 63;
        horizontalScrollBar.y = height - 78 - 16;
        horizontalScrollBar.width = getVisibleImageWidth();
        horizontalScrollBar.height = 16;

        verticalScrollBar.x = width - 6 - 16;
        verticalScrollBar.y = 44;
        verticalScrollBar.width = 16;
        verticalScrollBar.height = getVisibleImageHeight();
        updateScrollBars();
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        calledSelfMouseOver = false;
        if(!super.onMouseOver(x, y, touch))
            return false;
        if(!calledSelfMouseOver) {  // т. е. кто-то другой забрал onMouseOver (например, scrollBar), т. е. мы вышли за область рисования
            stopDrawingDrag();
            removeCustomCursor();
        }
        return true;
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        if(!super.onSelfMouseOver(x, y, touch))
            return false;

        calledSelfMouseOver = true;
        int startX = 63 - scrollX, startY = 44 - scrollY;
        if(63 <= x && x < Math.min(width - 6, startX + drawingBitmap.getWidth())
                && 44 <= y && y < Math.min(height - 78, startY + drawingBitmap.getHeight())){  // point на изображение
            x -= startX;
            y -= startY;
            if(activeInstrument == SELECT_FREE_FORM || activeInstrument == SELECT || activeInstrument == MAGNIFIER) {
                updateCursor();
                return true;  // остальные нереализованные инструменты заменяются на кисть
            }
            if(touch){
                if(activeInstrument != PICK_COLOR) {
                    imageChanged = true;
                    saveVersion();
                }
                if(activeInstrument == FILL){  // заливка
                    fillWithColor(x, y);
                }
                else if(activeInstrument == PICK_COLOR){  // пипетка
                    color = drawingBitmap.getPixel(x, y);
                    activeInstrument = PENCIL;
                    ((ImageSelectButton) elements.get(28 + 4)).active = false;
                    ((ImageSelectButton) elements.get(28 + 6)).active = true;
                }
                else if(activeInstrument == SPRAY){
                    // при нажатии
                    WindowsView.handler.postDelayed(sprayRunnable, SPRAY_DELAY);
                }
                drawing = true;
                start_x = last_x = x;
                start_y = last_y = y;
            }

            if(drawing){
                if(activeInstrument == ERASER){
                    p.setColor(Color.WHITE);
                    drawingCanvas.drawRect(x - 4, y - 4, x + 4, y + 4, p);
                }
                else if(activeInstrument == BRUSH || activeInstrument == TEXT){  // кисть (или спрей, или текст)
                    p.setColor(color);
                    p.setStrokeWidth(4);
                    drawingCanvas.drawLine(last_x, last_y, x, y, p);
                    p.setStrokeWidth(0);
                    drawingCanvas.drawRect(x - 1, y - 2, x + 1, y + 2, p);  // рисуем несглаженный круг сами
                    drawingCanvas.drawRect(x - 2, y - 1, x + 2, y + 1, p);
                }
                else if(activeInstrument == PENCIL){  // карандаш
                    p.setColor(color);
                    p.setStrokeWidth(1);
                    drawingCanvas.drawLine(last_x, last_y, x, y, p);
                    p.setStrokeWidth(0);
                    drawingCanvas.drawPoint(x, y, p);
                }
                else if(activeInstrument == SPRAY){
                    drawSpray(x, y);
                }
            }
            last_x = x;
            last_y = y;
            updateCursor();
        }
        else {
            stopDrawingDrag();
            removeCustomCursor();
        }
        return true;
    }

    private void drawSpray(int x, int y){
        // выбираем в круге радиусом 4 десять рандомных точек
        p.setColor(color);
        for(int i = 0; i < 10; i++){
            // https://stackoverflow.com/a/50746409/6120487
            final int radius = 4;
            double abs = radius * Math.sqrt(Math.random());
            double phi = Math.random() * 2 * Math.PI;
            int sprayX = (int) Math.round(abs * Math.cos(phi));
            int sprayY = (int) Math.round(abs * Math.sin(phi));
            drawingCanvas.drawPoint(x + sprayX, y + sprayY, p);
        }
    }

    private static final int SPRAY_DELAY = 100;

    private Runnable sprayRunnable = new Runnable() {
        @Override
        public void run() {
            drawSpray(last_x, last_y);
            updateWindow();
            WindowsView.handler.postDelayed(this, SPRAY_DELAY);
        }
    };

    @Override
    public void onClick(int x, int y) {
        super.onClick(x, y);
        if(drawing && 63 <= x && x < Math.min(width - 6, 63 + drawingBitmap.getWidth())
                && 44 <= y && y < Math.min(height - 78, 44 + drawingBitmap.getHeight())){  // клик на изображение
            x -= 63 - scrollX;
            y -= 44 - scrollY;
            drawInstrumentPreview(drawingCanvas, start_x, start_y, x, y);
        }
        stopDrawingDrag();
    }

    private void drawInstrumentPreview(Canvas canvas, int start_x, int start_y, int x, int y){
        if(activeInstrument == LINE || activeInstrument == CURVE || activeInstrument == POLYGON){  // отрезок
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            canvas.drawLine(start_x, start_y, x, y, p);
        }
        else if(activeInstrument == RECT){
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            canvas.drawRect(start_x, start_y, x, y, p);
            p.setStyle(Paint.Style.FILL);
        }
        else if(activeInstrument == ELLIPSE){
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.1f);
            tmpRectF.set(start_x, start_y, x, y);
            canvas.drawOval(tmpRectF, p);
            p.setStrokeWidth(0);
            p.setStyle(Paint.Style.FILL);
        }
        else if(activeInstrument == ROUND_RECT){
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            tmpRectF.set(start_x, start_y, x, y);
            fixRectF();
            canvas.drawRoundRect(tmpRectF, 9, 9, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    private void updateCursor(){
        if(activeInstrument == ERASER)
            Windows98.setCursor(eraser);
        else if(activeInstrument == FILL)
            Windows98.setCursor(fill);
        else if(activeInstrument == PICK_COLOR)
            Windows98.setCursor(pickColor);
        else if(activeInstrument == MAGNIFIER)
            Windows98.setCursor(magnifier);
        else if(activeInstrument == PENCIL)
            Windows98.setCursor(pencil);
        else if(activeInstrument == BRUSH)
            Windows98.setCursor(brush);
        else if(activeInstrument == SPRAY)
            Windows98.setCursor(spray);
        else
            Windows98.setCursor(defaultCursor);
    }

    @Override
    public void onDoubleClick(int x, int y) {
        super.onDoubleClick(x, y);
        stopDrawingDrag();
    }

    @Override
    public void onSelfMouseLeave() {
        super.onSelfMouseLeave();
        stopDrawingDrag();
        removeCustomCursor();
    }

    private void stopDrawingDrag(){
        drawing = false;
        WindowsView.handler.removeCallbacks(sprayRunnable);
    }

    private void setDrawingBitmap(Bitmap bmp) {
        if (bmp == drawingBitmap) {  // размер загружаемого битмапа совпал с размером существующего, и мы использовали inBitmap
            drawingCanvas = new Canvas(drawingBitmap);  // на всякий случай (??)
            amountOfPreviousVersions = 0;
            return;
        }
        drawingBitmap = bmp;
        pixels = new int[drawingBitmap.getWidth() * drawingBitmap.getHeight()];
        drawingCanvas = new Canvas(drawingBitmap);
        previousVersions = new Bitmap[10];
        amountOfPreviousVersions = 0;
        // по умолчанию пытаемся сделать 10 картинок Undo. При стандартном размере картинки это уже 7.5 МБ (OMG!)
        int i = 0;
        try {
            for (i = 0; i < 10; i++)
                previousVersions[i] = createBitmap(drawingBitmap.getWidth(), drawingBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            // изменяем размер previousVersions до нормального
            Bitmap[] oldPreviousVersions = previousVersions;
            previousVersions = new Bitmap[i];
            System.arraycopy(oldPreviousVersions, 0, previousVersions, 0, i);
        }
    }

    private void saveVersion(){  // для undo. сохраняет текущую версию
        // сдвигаем все по циклу
        int len = previousVersions.length;
        if(len == 0)
            return;
        Bitmap last = previousVersions[len - 1];
        for(int i = len - 1; i > 0; i--)
            previousVersions[i] = previousVersions[i - 1];
        previousVersions[0] = last;
        // копируем текущий битмап
        Canvas canvas = new Canvas(previousVersions[0]);
        canvas.drawBitmap(drawingBitmap, 0, 0, null);
        if(amountOfPreviousVersions != previousVersions.length)
            amountOfPreviousVersions++;
    }

    private void undo(){
        if (amountOfPreviousVersions > 0) {
            Bitmap first = previousVersions[0];
            for (int i = 0; i < previousVersions.length - 1; i++)
                previousVersions[i] = previousVersions[i + 1];
            previousVersions[previousVersions.length - 1] = drawingBitmap;
            drawingBitmap = first;
            drawingCanvas = new Canvas(drawingBitmap);
            amountOfPreviousVersions--;
        }
    }

    private void fillWithColor(int x, int y){
        // https://en.wikipedia.org/wiki/Flood_fill
        int w = drawingBitmap.getWidth(), h = drawingBitmap.getHeight();
        drawingBitmap.getPixels(pixels, 0, w, 0, 0, w, h);
        Queue<Point> q = new ArrayDeque<>();
        int target_color = pixels[x + y * w];  // перекрашиваемый цвет
        if(target_color == color)
            return;
        q.add(new Point(x, y));
        while(!q.isEmpty()){
            Point p = q.remove();
            int west = p.x;
            int east = p.x;
            while(0 <= west && pixels[west + w * p.y] == target_color)
                west--;
            while(east < w && pixels[east + w * p.y] == target_color)
                east++;
            for(int new_x = west + 1; new_x < east; new_x++){
                pixels[new_x + w * p.y] = color;
                for(int move : moves){
                    int new_y = p.y + move;
                    if(0 <= new_y && new_y < h && pixels[new_x + w * new_y] == target_color){
                        q.add(new Point(new_x, new_y));
                    }
                }
            }
        }
        drawingBitmap.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    private void fixRectF(){
        if(tmpRectF.left > tmpRectF.right){
            float tmp = tmpRectF.left;
            tmpRectF.left = tmpRectF.right;
            tmpRectF.right = tmp;
        }
        if(tmpRectF.top > tmpRectF.bottom){
            float tmp = tmpRectF.top;
                tmpRectF.top = tmpRectF.bottom;
            tmpRectF.bottom = tmp;
        }
    }

    private void drawColorSquare(Canvas canvas, int x, int y, int color){  // вспомогательный метод для onNewDraw, рисуем квадратик с цветом (основной и цвет ластика выглядят одинаково)
        p.setColor(Color.WHITE);
        canvas.drawRect(x, y, x + 15, y + 1, p);
        canvas.drawRect(x, y, x + 1, y + 15, p);
        p.setColor(Color.parseColor("#C0C7C8"));
        canvas.drawRect(x + 1, y + 1, x + 14, y + 2, p);
        canvas.drawRect(x + 1, y + 1, x + 2, y + 14, p);
        canvas.drawRect(x + 13, y + 2, x + 14, y + 14, p);
        canvas.drawRect(x + 2, y + 13, x + 14, y + 14, p);
        p.setColor(Color.parseColor("#87888F"));
        canvas.drawRect(x + 1, y + 14, x + 15, y + 15, p);
        canvas.drawRect(x + 14, y + 1, x + 15, y + 15, p);
        p.setColor(color);
        canvas.drawRect(x + 2, y + 2, x + 13, y + 13, p);
    }

    // ============== Сохранение файлов ================
    private FileDialog.OnResultListener getBmpWriter(){
        return new FileDialog.OnResultListener() {
            @Override
            public void writeToFile(File file) {
                FileOutputStream out = null;
                boolean success = false;
                try {  // так как try-with-resources поддерживается только начиная с API 19
                    file.createNewFile();
                    out = new FileOutputStream(file);
                    drawingBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    success = true;
                }
                catch (IOException e) {
                    new MessageBox(getTitle(), "Failed to save file.", MessageBox.OK, MessageBox.ERROR, null, PaintBrush.this);
                }
                finally {
                    try {
                        if(out != null)
                            out.close();
                    }
                    catch (IOException ignored){}
                }
                if(success) {
                    imageChanged = false;
                    openedFile = file;
                    setTitle(file.getName() + " - Paint");
                }
            }

            @Override
            public void openFile(File file) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;  // проверяем размер. Если он такой же, как у нашего drawingBitmap, ничего не надо делать
                BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                options.inJustDecodeBounds = false;
                if(drawingBitmap != null &&
                        drawingBitmap.getWidth() == options.outWidth && drawingBitmap.getHeight() == options.outHeight){
                    options.inBitmap = drawingBitmap;
                }
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap loaded = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                if(loaded == null){
                    setDrawingBitmap(createBitmap(Windows98.SCREEN_WIDTH - 61, Windows98.SCREEN_HEIGHT - 142, Bitmap.Config.ARGB_8888));  // fallback
                    drawingCanvas.drawColor(Color.WHITE);
                    new MessageBox(getTitle(), "Failed to open file.", MessageBox.OK, MessageBox.ERROR, null, PaintBrush.this);
                    return;
                }
                setDrawingBitmap(loaded.copy(Bitmap.Config.ARGB_8888, true));
                scrollX = scrollY = 0;
                repositionElements();
                updateScrollBars();
                openedFile = file;
                imageChanged = false;
                setTitle(file.getName() + " - Paint");
            }
        };
    }
    private void saveAs(){
        new FileDialog(false, "png", "Bitmap Files (*.png)",
                this, getBmpWriter(), FileDialog.getDir(openedFile));
    }
    private void open() {
        performActionWithSaveCheck(
                new Runnable() {
                    @Override
                    public void run() {
                        new FileDialog(true, supportedFormats, "Bitmap Files",
                                PaintBrush.this, getBmpWriter(), FileDialog.getDir(openedFile));
                    }
                }
        );
    }
    private void save(){
        if(openedFile != null && openedFile.exists() && Link.getExtension(openedFile.getName()).equals("png")) {
            getBmpWriter().writeToFile(openedFile);
            runActionOnSave();
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
                        PaintBrush.super.close(activateNextWindow);
                    }
                }
        );
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

    private void performActionWithSaveCheck(final Runnable action){  // если мы открываем файл, закрываем окно или создаём новый файл
        if(imageChanged) {
            // спрашиваем пользователя, хочет ли он сохранить файл
            new MessageBox("Paint", "Save changes to " + (openedFile == null ? "untitled" : openedFile.getName()) + "?",
                    MessageBox.YESNOCANCEL, MessageBox.WARNING, new MessageBox.MsgResultListener() {
                @Override
                public void onMsgResult(int buttonNumber) {
                    if(buttonNumber == YES){
                        setActionOnSave(action);
                        save();
                    }
                    else if(buttonNumber == NO){
                        action.run();
                    }
                }
            }, this);
        }
        else
            action.run();
    }

    // =============== scroll bar =================
    private void updateScrollBars(){
        //updateScrollbarsVisibility();
        //Log.d(TAG, "show scroll bars: " + showScrollBars);
        if(!horizontalScrollBar.visible)
            scrollX = 0;
        else {
            if (scrollX < 0)
                scrollX = 0;
            else if (scrollX + getVisibleImageWidth() > drawingBitmap.getWidth())
                scrollX = Math.max(0, drawingBitmap.getWidth() - getVisibleImageWidth());
        }

        if(!verticalScrollBar.visible)
            scrollY = 0;
        else {
            if (scrollY < 0)
                scrollY = 0;
            else if (scrollY + getVisibleImageHeight() > drawingBitmap.getHeight())
                scrollY = Math.max(0, drawingBitmap.getHeight() - getVisibleImageHeight());
        }
        horizontalScrollBar.startPosition = (double) scrollX / drawingBitmap.getWidth();
        horizontalScrollBar.endPosition = (double) (scrollX + getVisibleImageWidth()) / drawingBitmap.getWidth();
        verticalScrollBar.startPosition = (double) scrollY / drawingBitmap.getHeight();
        verticalScrollBar.endPosition = (double) (scrollY + getVisibleImageHeight()) / drawingBitmap.getHeight();
    }

    private void updateScrollbarsVisibility(){
        horizontalScrollBar.visible = false;
        verticalScrollBar.visible = false;
        horizontalScrollBar.visible = getVisibleImageWidth() < drawingBitmap.getWidth();
        verticalScrollBar.visible = getVisibleImageHeight() < drawingBitmap.getHeight();
        horizontalScrollBar.visible = getVisibleImageWidth() < drawingBitmap.getWidth();
    }

    private int getVisibleImageWidth(){
        int w = width - 6 - 63;
        if(verticalScrollBar.visible)
            w -= 16;
        return Math.min(w, drawingBitmap.getWidth());
    }

    private int getVisibleImageHeight(){
        int h = height - 78 - 44;
        if(horizontalScrollBar.visible)
            h -= 16;
        return Math.min(h, drawingBitmap.getHeight());
    }

    @Override
    public void onScrollbarMoved() {
        scrollX = (int) Math.round(horizontalScrollBar.startPosition * drawingBitmap.getWidth());
        scrollY = (int) Math.round(verticalScrollBar.startPosition * drawingBitmap.getHeight());
    }

    @Override
    public void pageUpDown(boolean up, boolean vertical) {
        int coeff = up? -1 : 1;
        if(vertical)
            scrollY += getVisibleImageHeight() * coeff;
        else
            scrollX += getVisibleImageWidth() * coeff;
        updateScrollBars();
    }

    @Override
    public void scrollUpDown(boolean up, boolean vertical) {
        int coeff = up? -1 : 1;
        if(vertical)
            scrollY += 65 * coeff;
        else
            scrollX += 65 * coeff;
        updateScrollBars();
    }

    @Override
    public void handleScrollbarClick() {}

    private void removeCustomCursor(){  // убираем кастомный курсор (если курсор не над изображением)
        if(allCursors.contains(Windows98.windows98.getCursor()))
            Windows98.setDefaultCursor();
    }

    // ImageSelectButton
    public static class ImageSelectButton extends Element {
        Bitmap bmp;
        Rect src;
        Rect dst = new Rect();
        List<ImageSelectButton> group;
        boolean active = false;
        boolean pressed = false;
        public ImageSelectButton(int x, int y, Bitmap bmp, List<ImageSelectButton> group){  // задаёт Bitmap, а так же координаты начала картинки. Картинка 16x16
            this.x = x;
            this.y = y;
            this.bmp = bmp;
            this.group = group;
            width = height = 25;
            int x_coord = group.size() * 16;  // откуда брать картинку
            src = new Rect(x_coord, 0, x_coord + 16, 16);
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            p.setColor(Color.parseColor("#C0C7C8"));
            canvas.drawRect(x, y, x + width, y + height, p);  // Серый фон
            if(pressed || active) {
                drawSimpleFrameRectActive(canvas, x, y, x + width, y + height);
            }
            else
                drawSimpleFrameRect(canvas, x, y, x + width, y + height);
            if(active){
                p.setColor(Color.WHITE);
                for(int i=x+2; i<x+width-2; i++){
                    for(int j=y+2; j<y+height-2; j++){
                        if((i + j - x - y) % 2 == 0){
                            canvas.drawPoint(i, j, p);
                        }
                    }
                }
            }
            if(active) {
                x++;
                y++;
            }
            else if(pressed) {
                x += 2;  // сдвигаем вниз, если мы зажимаем кнопку
                y += 2;
            }
            dst.left = x + 4; dst.top = y + 4; dst.right = x + 4 + 16; dst.bottom = y + 4 + 16;
            canvas.drawBitmap(bmp, src, dst, null);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(!(0 <= x && x < width && 0 <= y && y < height))
                return false;
            if(touch){
                pressed = true;
            }
            return true;
        }

        @Override
        public void onClick(int x, int y) {
            for(Element e : group){
                ((ImageSelectButton) e).active = false;
            }
            active = true;
            pressed = false;
            ((PaintBrush) parent).activeInstrument = group.indexOf(this);
        }

        @Override
        public void onMouseLeave() {
            pressed = false;
        }
    }

    @Override
    public void prepareForDelete() {
        super.prepareForDelete();
        stopDrawingDrag();  // sprayRunnable
    }

    private void setupTopMenu(){
        // top menu
        ButtonList file = new ButtonList();
        ButtonInList add_new = new ButtonInList("New", "Ctrl+N");
        add_new.action = parent -> performActionWithSaveCheck(() -> {
            drawingCanvas.drawColor(Color.WHITE);
            imageChanged = false;
            openedFile = null;
            setTitle("untitled - Paint");
        });
        file.elements.add(add_new);
        ButtonInList open = new ButtonInList("Open...", "Ctrl+O");
        open.action = parent -> open();
        file.elements.add(open);
        ButtonInList save = new ButtonInList("Save", "Ctrl+S");
        save.action = parent -> save();
        file.elements.add(save);
        ButtonInList saveAs = new ButtonInList("Save as...");
        saveAs.action = parent -> saveAs();
        file.elements.add(saveAs);
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("Print Preview"));
        file.elements.add(new ButtonInList("Page Setup..."));
        file.elements.add(new ButtonInList("Print", "Ctrl+P"));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("Send..."));
        file.elements.add(new Separator());
        ButtonInList setAsWallpaperTiled = new ButtonInList("Set As Wallpaper (Tiled)");
        setAsWallpaperTiled.disabled = true;
        file.elements.add(setAsWallpaperTiled);
        ButtonInList setAsWallpaperCentered = new ButtonInList("Set As Wallpaper (Centered)");
        setAsWallpaperCentered.disabled = true;
        file.elements.add(setAsWallpaperCentered);
        file.elements.add(new Separator());
        ButtonInList recentFile = new ButtonInList("Recent File");
        recentFile.disabled = true;
        file.elements.add(recentFile);
        ButtonInList exit = new ButtonInList("Exit", "Alt+F4");
        exit.action = parent -> close();
        file.elements.add(exit);
        ButtonList edit = new ButtonList();
        ButtonInList undo = new ButtonInList("Undo", "Ctrl+Z");
        undo.action = parent -> undo();
        edit.elements.add(undo);
        edit.elements.add(new ButtonInList("Repeat", "F4"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("Cut", "Ctrl+X"));
        edit.elements.add(new ButtonInList("Copy", "Ctrl+C"));
        edit.elements.add(new ButtonInList("Paste", "Ctrl+V"));
        edit.elements.add(new ButtonInList("Clear Selection", "Del"));
        edit.elements.add(new ButtonInList("Select All", "Ctrl+A"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("Copy To..."));
        edit.elements.add(new ButtonInList("Paste From..."));
        for (Element el : edit.elements) {  // делаем все кнопки неактивными, кроме некоторых
            if (el instanceof Separator)
                continue;
            String text = ((ButtonInList) el).text;
            if (text.equals("Undo"))
                continue;
            ((ButtonInList) el).disabled = true;
        }
        ButtonList view = new ButtonList();
        ButtonInList toolBox = new ButtonInList("Tool Box", "Ctrl+T");
        toolBox.check = true;
        toolBox.checkActive = true;
        view.elements.add(toolBox);
        ButtonInList colorBox = new ButtonInList("Color Box", "Ctrl+L");
        colorBox.check = true;
        colorBox.checkActive = true;
        view.elements.add(colorBox);
        ButtonInList statusBar = new ButtonInList("Status Bar");
        statusBar.check = true;
        statusBar.checkActive = true;
        view.elements.add(statusBar);
        ButtonInList textToolbar = new ButtonInList("Text Toolbar");
        textToolbar.check = true;
        textToolbar.checkActive = true;
        view.elements.add(textToolbar);
        view.elements.add(new Separator());
        ButtonList zoom = new ButtonList();
        zoom.elements.add(new ButtonInList("Normal Size", "Ctrl+PgUp"));
        zoom.elements.add(new ButtonInList("Large Size", "Ctrl+PgDn"));
        zoom.elements.add(new ButtonInList("Custom..."));
        zoom.elements.add(new Separator());
        zoom.elements.add(new ButtonInList("Show Grid", "Ctrl+G"));
        zoom.elements.add(new ButtonInList("Show Thumbnail"));
        view.elements.add(new ButtonInList("Zoom", zoom));
        view.elements.add(new ButtonInList("View Bitmap", "Ctrl+F"));
        ButtonList image = new ButtonList();
        image.elements.add(new ButtonInList("Flip/Rotate...", "Ctrl+R"));
        image.elements.add(new ButtonInList("Stretch/Skew...", "Ctrl+W"));
        image.elements.add(new ButtonInList("Invert Colors", "Ctrl+I"));
        image.elements.add(new ButtonInList("Attributes...", "Ctrl+E"));
        image.elements.add(new ButtonInList("Clear Image", "Ctrl+Shft+N"));
        ButtonInList drawOpaque = new ButtonInList("Draw Opaque");
        drawOpaque.check = true;
        drawOpaque.checkActive = true;
        image.elements.add(drawOpaque);
        ButtonList colors = new ButtonList();
        colors.elements.add(new ButtonInList("Edit Colors..."));
        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("Help Topics");
        helpTopics.action = parent -> new HelpTopics("Paint Help", true,
                new int[]{R.drawable.paint1, R.drawable.paint2, R.drawable.paint3, R.drawable.paint4, R.drawable.paint5, R.drawable.paint6});
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("About Paint");
        about.action = parent -> new AboutWindow(PaintBrush.this, "Paint", getBmp(R.drawable.paint_2));
        help.elements.add(about);
        topMenu.elements.add(new TopMenuButton("File", file));
        topMenu.elements.add(new TopMenuButton("Edit", edit));
        topMenu.elements.add(new TopMenuButton("View", view));
        topMenu.elements.add(new TopMenuButton("Image", image));
        topMenu.elements.add(new TopMenuButton("Colors", colors));
        topMenu.elements.add(new TopMenuButton("Help", help));
    }
}
