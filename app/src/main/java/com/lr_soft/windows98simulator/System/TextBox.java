package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.WindowsView;

public class TextBox extends Element implements Scrollable {
    private Paint fontPaint;
    public int leftMargin, topMargin, lineMargin;
    private Rect cursor;

    public String text = "";
    public String[] lines = null;

    private Rect[][] cursors = null;  // cursor[строка][номер символа]
    private int cursor_pos = 0;  // перед каким символом стоит курсор
    private int cursor_line = 0, cursor_symbol = 0;  // строчка и символ в строчке
    public int selectionStart = -1;  // если selectionStart не -1, то selectionStart - один конец выделения, cursor_pos - другой
    private int maxLines = 0;
    public int maxTextLength = -1;

    private boolean active = true;  // мигает ли курсор
    private boolean drawCursor = true;
    public boolean deleteLongText = false;  // если текст имеет слишком большую длину (так как мы сделали такой setText), то мы его удаляем при попытке редактирования
    public boolean selectOnActive = false;  // при нажатии выделять весь текст (как, например, в Internet Explorer)
    private boolean selectAllOnClick = false;  // (internal) в следующем onClick выделить всё, если не было перемещений
    public boolean isNumeric = false;  // можно вводить только цифры
    public boolean drawBorder = false;  // рисовать вокруг границу (за пределами bounds!)

    public Runnable enterRunnable = null;  // вызывается при нажатии на клавишу Enter
    boolean ignoreOtherTouch = false;
    public boolean isMsDosPrompt = false;  // нет выделения мышкой, свой bitmap
    public int backgroundColor = Color.WHITE, textColor = Color.BLACK, whiteCursor = Color.WHITE;
    public boolean centerText = false;  // нужно для переименования в Link
    private int[] lines_shiftX = null;  // для centerText
    public int minCursor = 0;  // чтобы курсор не доходил до программного текста (например, в MS-DOS)
    public ScrollBar verticalScrollBar = null;  // можно, но не обязательно. В случае, если его нет, будет ограничение по длине текста.
    public int scrolledLines = 0;  // номер строчки, которая отображается первой на экране (если scrollBar всё-таки присутствует)
    private Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            drawCursor = !drawCursor;
            if(parent instanceof Window)
                ((Window) parent).shouldRedraw = true;
            WindowsView.windowsView.invalidate();
            WindowsView.handler.postDelayed(this, 500);
        }
    };
    public Bitmap drawingBitmap = WindowsView.canvas_bmp;  // Bitmap, на который мы отрисовываемся, нужно в MS-DOS, где мы отрисовываемся не на WindowsView.canvas_bmp
    private Rect tmp = new Rect();
    private Point tmp_point = new Point();
    public Rect minimizedBounds, maximizedBounds;

    public TextBox(Rect bounds, int leftMargin, int topMargin, Paint fontPaint, Rect cursor){
        // leftMargin, topMargin - где начинается текст
        // Rect cursor - отсчёт от правой нижней точки буквы
        this.leftMargin = leftMargin;
        this.topMargin = topMargin;
        this.lineMargin = cursor.height();  // внезапно
        this.fontPaint = fontPaint;
        this.cursor = cursor;
        this.minimizedBounds = bounds;
        setBounds(bounds);
        WindowsView.handler.postDelayed(blinkRunnable, 500);
    }

    public TextBox(Window.RelativeBounds bounds, int leftMargin, int topMargin, Paint fontPaint, Rect cursor){
        this(bounds.getMinimizedRect(), bounds.getMaximizedRect(), leftMargin, topMargin, fontPaint, cursor);
    }

    private TextBox(Rect minimizedBounds, Rect maximizedBounds, int leftMargin, int topMargin, Paint fontPaint, Rect cursor){
        this(minimizedBounds, leftMargin, topMargin, fontPaint, cursor);
        this.minimizedBounds = minimizedBounds;
        this.maximizedBounds = maximizedBounds;
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        if(drawBorder)
            drawFrameRectActive(canvas, x - 2, y - 2, x + width + 2, y + height + 2);
        // рисуем фон
        p.setColor(backgroundColor);
        if(!centerText)
            canvas.drawRect(x, y, x + width, y + height, p);
        /*else {
            p.setColor(Color.RED);
            canvas.drawRect(x, y, x + width, y + height, p);
        }*/
        if(lines == null) {
            updateLinesAndCursors();
        }

        // рисуем текст
        int cur_y = y + realTopMargin();
        fontPaint.setColor(textColor);

        for(int i = 0; i < lines.length; i++){
            String line = lines[i];
            int len = line.length();
            if((isMsDosPrompt || fontPaint == Element.p_fixedsys) && line.endsWith("\n"))  // так как в этом шрифте зачем-то есть символ для \n !!!
                len--;
            if(isCursorVisible(cursors[i][0]) == 0)  // курсор - не за экраном, значит, строчка - не за экраном
                canvas.drawText(line, 0, len, x + leftMargin + (centerText? lines_shiftX[i] : 0), cur_y, fontPaint);
            cur_y += lineMargin;
        }

        if(!isMsDosPrompt && !centerText){
            Window parentWindow = (Window) parent;
            if(parentWindow.active || parentWindow.backupBitmap == null)
                drawingBitmap = WindowsView.canvas_bmp;
            else
                drawingBitmap = parentWindow.backupBitmap;
        }

        // рисуем выделение
        int BLUE = Color.parseColor("#0000A8");  // синий
        if(selectionStart != -1) {
            int start = Math.min(selectionStart, cursor_pos), end = Math.max(selectionStart, cursor_pos);
            getCursorIndices(start);
            int start_cursor_line = tmp_point.x, start_cursor_symbol = tmp_point.y;
            getCursorIndices(end);
            int end_cursor_line = tmp_point.x, end_cursor_symbol = tmp_point.y;
            for (int i = start_cursor_line; i <= end_cursor_line; i++) {
                if(isCursorVisible(cursors[i][0]) != 0)  // строчка за экраном
                    continue;
                p.setColor(BLUE);
                for(int j = (i == start_cursor_line)? start_cursor_symbol : 0;
                    j < (i == end_cursor_line? end_cursor_symbol : lines[i].length()); j++) {
                    // рисуем синий прямоугольник между двумя последовательными курсорами
                    Rect first = cursors[i][j], second = cursors[i][j + 1];
                    canvas.drawRect(x + first.left, y + realTopMargin() + first.top,
                            x + second.left, y + realTopMargin() + second.bottom, p);
                }
                fontPaint.setColor(Color.WHITE);
                for(int j = (i == start_cursor_line)? start_cursor_symbol : 0;
                    j < (i == end_cursor_line? end_cursor_symbol : lines[i].length()); j++) {
                    Rect first = cursors[i][j];  //, second = cursors[i][j + 1];
                    // "меняем" цвет текста с черного на белый
                    canvas.drawText(String.valueOf(lines[i].charAt(j)),
                            x + first.left - cursor.left, y + realTopMargin() + first.top - cursor.top, fontPaint);
                }
            }
        }

        // рисуем курсор
        if(drawCursor && active) {
            Rect cursor = cursors[cursor_line][cursor_symbol];
            if(isCursorVisible(cursor) == 0) {  // курсор - не за экраном
                for (int n = cursor.left; n < cursor.right; n++) {
                    for (int m = cursor.top; m < cursor.bottom; m++) {
                        if (!(0 <= x + n && x + n < drawingBitmap.getWidth() && 0 <= y + realTopMargin() + m && y + realTopMargin() + m < drawingBitmap.getHeight()))
                            continue;
                        int pixelColor = drawingBitmap.getPixel(x + n, y + realTopMargin() + m);
                        if (pixelColor == Color.BLACK)
                            p.setColor(whiteCursor);
                        else if (pixelColor == BLUE)  // синий
                            p.setColor(Color.YELLOW);
                        else  // белый
                            p.setColor(Color.BLACK);
                        canvas.drawPoint(x + n, y + realTopMargin() + m, p);
                    }
                }

            }
        }
    }
    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if (!(0 <= x && x < width && 0 <= y && y < height) && !centerText)
            return false;
        if(isMsDosPrompt)
            return true;
        if(lines == null)
            updateLinesAndCursors();
        // определяем строку
        int minDist = (int) 1e9;
        int minLine = -1, minLineStart = -1;
        int symbolsCount = 0;
        for (int i = 0; i < lines.length; i++) {
            if(isCursorVisible(cursors[i][0]) != 0) {  // эта строчка не отрисовывается
                symbolsCount += lines[i].length();
                continue;
            }
            int dist = Math.abs(cursors[i][0].centerY() + realTopMargin() - y);
            if (dist < minDist) {
                minDist = dist;
                minLine = i;
                minLineStart = symbolsCount;
            }
            symbolsCount += lines[i].length();
        }
        // определяем символ в строке
        minDist = (int) 1e9;
        int minSymbol = -1;  // курсор с минимальным расстоянием от клика
        for (int i = 0; i <= lines[minLine].length(); i++) {
            int dist = Math.abs(cursors[minLine][i].centerX() - x);
            if (dist < minDist) {  // чтобы если есть 2 совпадающих курсора, будет выбран первый
                minDist = dist;
                minSymbol = i;
            }
        }
        if (touch) {
            ////// КУРСОР РАВЕН minCursor
            selectionStart = cursor_pos = minLineStart + minSymbol;
            cursor_line = minLine;
            cursor_symbol = minSymbol;
            setActive(true);
            resetCursorAnimation();
        }
        else if(isPressed()) {
            if (cursor_pos != minSymbol)
                selectAllOnClick = false;
            cursor_pos = minLineStart + minSymbol;
            cursor_line = minLine;
            cursor_symbol = minSymbol;
        }
        return true;
    }

    private int isCursorVisible(Rect cursor){  // 0 - виден, -1 - слишком высоко, 1 - слишком низко
        if(centerText)
            return 0;
        tmp.set(cursor);
        tmp.offset(0, realTopMargin());
        if(0 <= tmp.left && 0 <= tmp.top && width > tmp.right && height > tmp.bottom)
            return 0;
        else if(tmp.bottom >= height)
            return 1;
        else
            return -1;
    }

    private int realTopMargin(){
        return topMargin - scrolledLines * lineMargin;
    }

    @Override
    public void onKeyPress(String key) {
        if(deleteLongText && lines.length > maxLines)
            setText("");
        int start = Math.min(selectionStart, cursor_pos), end = Math.max(selectionStart, cursor_pos);
        String oldText = text;
        int old_cursor = cursor_pos;
        if(key.equals("DEL")){
            if(selectionStart != -1){  // выделение не пустое
                text = text.substring(0, start) + text.substring(end);
                cursor_pos = start;
                selectionStart = -1;
            }
            else if(cursor_pos != 0 && cursor_pos != minCursor) {
                text = text.substring(0, cursor_pos - 1) + text.substring(cursor_pos);
                cursor_pos--;
            }
        }
        else {
            String new_text = text;
            if(selectionStart != -1){
                new_text = text.substring(0, start) + text.substring(end);
                cursor_pos = start;
                selectionStart = -1;
            }
            new_text = new_text.substring(0, cursor_pos) + key + new_text.substring(cursor_pos);
            if((maxTextLength == -1 || new_text.length() <= maxTextLength)  // различные проверки
                    && (!isNumeric || Character.isDigit(key.charAt(0)))) {
                text = new_text;
                cursor_pos++;
            }
            else if(maxTextLength != -1 && text.length() > maxTextLength){  // текст изначально был больше, чем лимит (например, из-за setText(имя файла))
                text = text.substring(0, maxTextLength);
                if(cursor_pos > maxTextLength)
                    cursor_pos = maxTextLength;
            }
        }
        updateLinesAndCursors();
        setCursorPos(cursor_pos);
        if(lines.length > maxLines && maxLines != -1 && verticalScrollBar == null){  // возвращаем всё назад
            text = oldText;
            updateLinesAndCursors();
            setCursorPos(old_cursor);
        }
        int cursorState;
        if(verticalScrollBar != null && (cursorState = isCursorVisible(cursors[cursor_line][cursor_symbol])) != 0){  // курсор не виден, это надо исправить
            if(cursorState == -1) {  // курсор слишком высоко - ставим его в начало
                scrolledLines = cursor_line;
            }
            else {  // курсор слишком низко - ставим его в конец экрана
                scrolledLines = cursor_line - maxLines + 1;
            }
        }
        checkScrollBounds();
        updateScrollbarPosition();
        resetCursorAnimation();
        if(key.equals("\n")) {
            if(enterRunnable != null)
                enterRunnable.run();
        }
    }

    private void resetCursorAnimation(){
        resetCursorAnimation(false);
    }
    private void resetCursorAnimation(boolean checkActive){  // checkActive - если курсор не мигает, не включаем его
        if(checkActive){
            if(!active)
                return;
        }
        WindowsView.handler.removeCallbacks(blinkRunnable);
        // `i(TAG, "caller of resetCursorAnim: " + Thread.currentThread().getStackTrace()[5].toString());
        drawCursor = true;
        WindowsView.handler.postDelayed(blinkRunnable, 500);
    }

    @Override
    public void onClick(int x, int y) {
        if(selectionStart == cursor_pos)
            selectionStart = -1;
        if(selectAllOnClick){
            selectionStart = 0;
            moveCursorToEnd();
            selectAllOnClick = false;
        }
    }

    @Override
    public void onOtherTouch() {
        if(selectOnActive)
            selectionStart = -1;
        if(ignoreOtherTouch){
            ignoreOtherTouch = false;
            return;
        }
        setActive(false);
    }
    public boolean isActive(){
        return active;
    }

    public void setActive(boolean active){
        if(active == this.active)
            return;
        this.active = active;
        if(active) {
            resetCursorAnimation();
            parent.inputFocus = this;
            if(selectOnActive){
                selectAllOnClick = true;
            }
        }
        else {
            drawCursor = false;
            WindowsView.handler.removeCallbacks(blinkRunnable);
        }
    }

    public void updateLinesAndCursors(){
        lines = splitTextIntoLines(text, width - cursor.right - leftMargin - 1, -1, fontPaint, true, true);
        cursors = new Rect[lines.length][];//new Rect[text.length() + 1];
        int cur_y = 0;  // не topMargin, так как topMargin может меняться
        if(centerText)
            lines_shiftX = new int[lines.length];
        for(int i=0; i<lines.length; i++) {
            String line = lines[i];
            cursors[i] = new Rect[line.length() + 1];
            if(centerText){
                int width = (int) p.measureText(line);
                lines_shiftX[i] = -(width / 2);
            }
            for (int cursor_sym = 0; cursor_sym <= lines[i].length(); cursor_sym++) {
                int cursor_x = leftMargin +
                        (int) fontPaint.measureText(line, 0, cursor_sym) +
                        (centerText? lines_shiftX[i] : 0);
                cursors[i][cursor_sym] = new Rect(cursor);
                cursors[i][cursor_sym].offset(cursor_x, cur_y);
            }
            if(!line.isEmpty() && line.charAt(line.length() - 1) == '\n')  // если последний символ в строке - \n, не надо для него создавать курсор.
                cursors[i][line.length()] = cursors[i][line.length() - 1];
            cur_y += lineMargin;
        }
        /*if(deleteTextIfManyLines && lines.length > maxLines && verticalScrollBar == null){
            text = "";  // sorry ;(
            selectionStart = -1;
            cursor_pos = 0;
            updateLinesAndCursors(false);
        }*/
    }

    @Override
    public void setBounds(Rect bounds){
        this.x = bounds.left;
        this.y = bounds.top;
        this.width = bounds.width();
        this.height = bounds.height();
        maxLines = (height - cursor.bottom - topMargin) / lineMargin + 1;  // чтобы помещался курсор
        if(verticalScrollBar != null){
            if(lines == null)
                updateLinesAndCursors();
            checkScrollBounds();
            updateScrollbarPosition();
        }
    }
    // scroll
    public void updateScrollbarPosition(){
        if(verticalScrollBar == null)
            return;
        if(lines == null)
            updateLinesAndCursors();
        if(maxLines >= lines.length)
            verticalScrollBar.setDisabled(true);
        else {
            verticalScrollBar.setDisabled(false);
            verticalScrollBar.startPosition = (double) scrolledLines / lines.length;
            verticalScrollBar.endPosition = (double) (scrolledLines + maxLines) / lines.length;
        }
    }

    @Override
    public void onScrollbarMoved() {
        if(lines == null)
            updateLinesAndCursors();
        scrolledLines = (int) Math.round(verticalScrollBar.startPosition * lines.length);
    }

    @Override
    public void scrollUpDown(boolean up, boolean vertical) {
        if(up)
            scrolledLines--;
        else
            scrolledLines++;
        checkScrollBounds();
        updateScrollbarPosition();
    }
    @Override
    public void pageUpDown(boolean up, boolean vertical) {
        if(up)
            scrolledLines -= maxLines - 1;
        else
            scrolledLines += maxLines - 1;
        checkScrollBounds();
        updateScrollbarPosition();
    }
    public void checkScrollBounds(){
        if(lines == null)
            updateLinesAndCursors();
        if(scrolledLines + maxLines >= lines.length)
            scrolledLines = Math.max(lines.length - maxLines, 0);
        else if(scrolledLines < 0)
            scrolledLines = 0;
    }

    @Override
    public void handleScrollbarClick() {
        setActive(true);
        if(parent.inputFocus == this)
            parent.notChangeInputFocus = true;  // так как это вызывается из ScrollBar, он вернёт true в onMouseOver и его сделают новым inputFocus.
        parent.inputFocus = this;
    }

    @Override
    public void prepareForDelete() {
        WindowsView.handler.removeCallbacks(blinkRunnable);
    }
    public void setText(String text){
        this.text = text;
        updateLinesAndCursors();
        setCursorPos(0);
        selectionStart = -1;
        scrolledLines = 0;
        updateScrollbarPosition();
        resetCursorAnimation(true);
    }

    @Override
    public void onWindowResize(boolean maximized) {
        if(maximizedBounds == null)
            return;
        setBounds(maximized? maximizedBounds : minimizedBounds);
        updateLinesAndCursors();
        setCursorPos(cursor_pos);
    }

    /*@Override
    public int getTextLength() {
        return text.length();
    }*/

    public void setCursorPos(int cursor_pos){
        this.cursor_pos = cursor_pos;
        Point cursor = getCursorIndices(cursor_pos);
        cursor_line = cursor.x;
        cursor_symbol = cursor.y;
    }

    public void moveCursorToEnd(){
        setCursorPos(text.length());
    }

    private Point getCursorIndices(int cursor_pos){  // возвращает пару {cursor_line, cursor_symbol}. Если есть 2 варианта, выбирает верхний
        int symbolsCount = 0;
        for(int i = 0; i < lines.length; i++){
            if(symbolsCount + lines[i].length() == cursor_pos && lines[i].endsWith("\n")) {
                symbolsCount += lines[i].length();
                continue;  // если строка заканчивается на \n, всё-таки выбираем нижнюю строку
            }
            if(symbolsCount + lines[i].length() >= cursor_pos){
                int cursor_line = i;
                int cursor_symbol = cursor_pos - symbolsCount;
                tmp_point.set(cursor_line, cursor_symbol);
                return tmp_point;
            }
            symbolsCount += lines[i].length();
        }
        return null;
    }

    public int getCursorPos() {
        return cursor_pos;
    }

    public void selectAll(){
        selectionStart = 0;
        moveCursorToEnd();
    }
    /*private Rect getCursorRect(int cursor_pos){
        Point p = getCursorIndices(cursor_pos);
        return cursors[p.x][p.y];
    }*/
}
