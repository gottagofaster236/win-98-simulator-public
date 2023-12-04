package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.AboutWindow;
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.DialogWindow;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.HelpTopics;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TextBox;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;
import com.lr_soft.windows98simulator.WindowsView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Minesweeper extends Window {
    int field_w, field_h;
    int minesCount;  // количество мин в начале игры
    private static final int WAIT_FOR_CLICK = 0, PLAYING = 1, LOST = 2, WIN = 3;
    int state = WAIT_FOR_CLICK;
    // 0 - ждём первого нажатия на поле
    // 1 - играем, таймер идёт
    // 2 - мертвый смайлик
    // 3 - смайлик в солнечных очках (победа)
    boolean field_pressed = false;
    Bitmap digitsBmp, fieldBmp;
    int minesLeft;  // счетчики сверху
    volatile int timer = 0;
    int openedFields = 0;
    boolean[][] mines;
    int lost_x, lost_y;  // мина, на которой мы подорвались (показывается красным)
    int active_x, active_y;  // мина, на которой мы зажали ЛКМ
    int[][] cur_field;
    boolean[][] used;  // массив для BFS
    private final static int NOT_OPENED = -1, FLAG = -2, QUESTION = -3;
    // 1 - 8 - открытое число
    // 0 - пустая открытая клетка
    // -1 неоткрытая
    // -2 флажок
    // -3 вопросик
    private static final int[][] moves = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
    Rect src = new Rect(), dst = new Rect();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);  // для (точного!) таймера
    private ScheduledFuture<?> scheduledFuture;
    Runnable timerRunnable = new Runnable() {
        @Override
        public synchronized void run() {
            timer++;
            Minesweeper.this.shouldRedraw = true;
            WindowsView.windowsView.postInvalidate();
        }
    };
    public Minesweeper(int field_w, int field_h, int minesCount, boolean custom){
        super("Minesweeper", getBmp(R.drawable.game_mine), 30 + field_w * 16, 111 + field_h * 16, false, true, false);
        this.field_w = field_w;
        this.field_h = field_h;
        this.minesCount = minesCount;
        cur_field = new int[field_w][field_h];
        mines = new boolean[field_w][field_h];
        used = new boolean[field_w][field_h];
        /*switch(field_w){
            case 8:
                minesCount = 10;
                break;
            case 16:
                minesCount = 40;
                break;
            case 30:
                minesCount = 99;
                break;
        }*/
        unableToMaximize = true;
        digitsBmp = getBmp(R.drawable.winmine_digits);
        fieldBmp = getBmp(R.drawable.winmine_field);
        addElement(new SmileyButton(width / 2 - 13, 56));
        // top menu
        ButtonList game = new ButtonList();
        ButtonInList add_new = new ButtonInList("New", "F2");
        add_new.action = parent -> reset();
        game.elements.add(add_new);
        game.elements.add(new Separator());
        final OnClickRunnable changeSizeRunnable = parent -> {
            ButtonInList b = (ButtonInList) parent;
            switch (b.text){
                case "Beginner":
                    changeSizeTo(8, 8, 10, false);
                    break;
                case "Intermediate":
                    changeSizeTo(16, 16, 40, false);
                    break;
                case "Expert":
                    changeSizeTo(30, 16, 99, false);
                    break;
            }
        };
        ButtonInList beginner = new ButtonInList("Beginner", changeSizeRunnable);
        if(field_w == 8 && !custom){
            beginner.check = true;
            beginner.checkActive = true;
        }
        game.elements.add(beginner);
        ButtonInList intermediate = new ButtonInList("Intermediate", changeSizeRunnable);
        if(field_w == 16 && !custom){
            intermediate.check = true;
            intermediate.checkActive = true;
        }
        game.elements.add(intermediate);
        ButtonInList expert = new ButtonInList("Expert", changeSizeRunnable);
        if(field_w == 30 && !custom){
            expert.check = true;
            expert.checkActive = true;
        }
        game.elements.add(expert);
        ButtonInList customButton = new ButtonInList("Custom...", parent -> new CustomField());
        if(custom){
            customButton.check = true;
            customButton.checkActive = true;
        }
        game.elements.add(customButton);
        game.elements.add(new Separator());
        ButtonInList marks = new ButtonInList("Marks (?)");
        marks.check = true;
        marks.checkActive = true;
        game.elements.add(marks);
        ButtonInList color = new ButtonInList("Color");
        color.check = true;
        color.checkActive = true;
        game.elements.add(color);
        game.elements.add(new Separator());
        game.elements.add(new ButtonInList("Best Times..."));
        game.elements.add(new Separator());
        ButtonInList exit = new ButtonInList("Exit");
        exit.action = parent -> close();
        game.elements.add(exit);
        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("Help Topics");
        helpTopics.action = parent -> new HelpTopics("Minesweeper Help", false,
                new int[]{R.drawable.mine1, R.drawable.mine2, R.drawable.mine3});
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("About Minesweeper");
        about.action = parent -> new AboutWindow(Minesweeper.this, "Minesweeper",
                getBmp(R.drawable.winmine_bigicon), "by Robert Donner and Curt Johnson");
        help.elements.add(about);
        topMenu.elements.add(new TopMenuButton("Game", game));
        topMenu.elements.add(new TopMenuButton("Help", help));
        reset();
    }

    @SuppressWarnings("unused")  // used via reflection
    public Minesweeper(){
        this(8, 8, 10, false);
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        drawNumber(canvas, minesLeft, x + 19, y + 56);
        drawNumber(canvas, timer, x + width - 62, y + 56);
        // рисуем само поле
        for(int i=0; i<field_w; i++){
            for(int j=0; j<field_h; j++){
                int bmpNumber;
                if(cur_field[i][j] >= 0) {  // число или просто открытая клетка
                    bmpNumber = 15 - cur_field[i][j];
                }
                else if(cur_field[i][j] == FLAG){  // флажок
                    bmpNumber = 1;
                    if(state == LOST && !mines[i][j])  // ошибочный флажок
                        bmpNumber = 4;
                }
                else if(state == LOST && mines[i][j]){
                    bmpNumber = (i == lost_x && j == lost_y)? 3 : 5;  // красная мина или просто мина
                }
                else if(cur_field[i][j] == QUESTION){  // вопросик
                    bmpNumber = 2;
                    if(state == WIN){  // win - показываем флажок
                        bmpNumber = 1;
                    }
                }
                else {  // неоткрытая клетка
                    bmpNumber = 0;
                    if(state != LOST && state != WIN && field_pressed && i == active_x && j == active_y){  // клетка с зажатым ЛКМ
                        bmpNumber = 15;
                    }
                    else if(state == WIN){  // win - показываем флажок
                        bmpNumber = 1;
                    }
                }
                src.left = 0;
                src.right = 16;
                src.top = bmpNumber * 16;
                src.bottom = (bmpNumber + 1) * 16;
                dst.left = x + 15 + i * 16;
                dst.top = y + 96 + j * 16;
                dst.right = dst.left + 16;
                dst.bottom = dst.top + 16;
                canvas.drawBitmap(fieldBmp, src, dst, null);
            }
        }
        // рисуем линии
        // белые линии
        p.setColor(Color.WHITE);
        canvas.drawRect(x + 3, y + 41, x + width - 6, y + 44, p);
        canvas.drawRect(x + 3, y + 41, x + 6, y + height - 6, p);
        canvas.drawRect(x + 14, y + 85, x + width - 12, y + 87, p);
        canvas.drawRect(x + width - 14, y + 52, x + width - 12, y + 87, p);
        canvas.drawRect(x + 15, y + height - 15, x + width - 12, y + height - 12, p);
        canvas.drawRect(x + width - 15, y + 96, x + width - 12, y + height - 12, p);
        // серые линии
        p.setColor(Color.parseColor("#87888F"));
        canvas.drawRect(x + 6, y + height - 6, x + width - 3, y + height - 3, p);
        canvas.drawRect(x + width - 6, y + 44, x + width - 3, y + height - 3, p);
        canvas.drawRect(x + 12, y + 93, x + 15, y + height - 15, p);
        canvas.drawRect(x + 12, y + 93, x + width - 15, y + 96, p);
        canvas.drawRect(x + 12, y + 50, x + width - 14, y + 52, p);
        canvas.drawRect(x + 12, y + 50, x + 14, y + 85, p);
        // затенённые квадраты
        drawShadowedSquare(canvas, x + 3, y + height - 6, 3, true);
        drawShadowedSquare(canvas, x + width - 6, y + 41, 3, true);
        drawShadowedSquare(canvas, x + 12, y + height - 15, 3, false);
        drawShadowedSquare(canvas, x + width - 15, y + 93, 3, false);
        drawShadowedSquare(canvas, x + 12, y + 85, 2, false);
        drawShadowedSquare(canvas, x + width - 14, y + 50, 2, false);
    }
    private void drawShadowedSquare(Canvas canvas, int x, int y, int width, boolean whiteFirst){
        if(whiteFirst)
            p.setColor(Color.parseColor("#87888F"));
        else
            p.setColor(Color.WHITE);
        canvas.drawRect(x, y, x + width, y + width, p);
        if(whiteFirst)
            p.setColor(Color.WHITE);
        else
            p.setColor(Color.parseColor("#87888F"));
        for(int i=0; i<width; i++){
            for(int j=0; j<width-1-i; j++){  // j + i < width - 1 (например, пара 0, width-2). Первая половина квадрата.
                canvas.drawPoint(x + i, y + j, p);
            }
        }
        p.setColor(Color.parseColor("#C0C7C8"));
        for(int i=0; i<width; i++)
            canvas.drawPoint(x + i, y + width - 1 - i, p);  // серая диагональ
    }

    private void drawNumber(Canvas canvas, int number, int x, int y){
        if(number > 999)
            number = 999;
        else if(number < -99)
            number = -99;
        // рисуем линии вокруг цифр
        p.setColor(Color.parseColor("#87888F"));
        canvas.drawRect(x, y, x + 40, y + 1, p);
        canvas.drawRect(x, y, x + 1, y + 24, p);
        p.setColor(Color.WHITE);
        canvas.drawRect(x + 40, y + 1, x + 41, y + 25, p);
        canvas.drawRect(x + 1, y + 24, x + 41, y + 25, p);
        String formatted = String.format(Locale.US, "%03d", number);  // allocation on draw... sorry
        int cur_x = x + 1, cur_y = y + 1;
        for(char digit : formatted.toCharArray()){
            int bmpNumber;
            if(digit == '-')
                bmpNumber = 0;
            else  // 0 - 9
                bmpNumber = 11 - (digit - '0');
            src.left = 0;
            src.top = bmpNumber * 23;
            src.right = 13;
            src.bottom = (bmpNumber + 1) * 23;
            dst.left = cur_x;
            dst.top = cur_y;
            dst.right = cur_x + 13;
            dst.bottom = cur_y + 23;
            canvas.drawBitmap(digitsBmp, src, dst, null);
            cur_x += 13;
        }
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        if(!super.onSelfMouseOver(x, y, touch))
            return false;
        if(15 <= x && x < 15 + 16 * field_w && 96 <= y && y < 96 + 16 * field_h){  // на поле
            if(touch){
                field_pressed = true;
            }
            x -= 15;
            y -= 96;
            active_x = x / 16;
            active_y = y / 16;
        }
        else
            field_pressed = false;
        return true;
    }

    @Override
    public void onSelfClick(int x, int y) {  // перед onClick сначала вызывается onMouseOver, так что field_pressed, active_x, active_y будут правильные
        super.onSelfClick(x, y);
        if(state == LOST || state == WIN)
            return;
        if(field_pressed){
            if(state == WAIT_FOR_CLICK){
                placeMines(active_x, active_y);
                state = PLAYING;
                startTimer();
            }
            if(cur_field[active_x][active_y] == NOT_OPENED || cur_field[active_x][active_y] == QUESTION){  // неоткрытая либо вопросик
                if(mines[active_x][active_y]){  // напоролись на мину
                    lost_x = active_x;
                    lost_y = active_y;
                    state = LOST;
                    stopTimer();
                }
                else {
                    openBFS(active_x, active_y);
                    if(openedFields == field_w * field_h - minesCount) {
                        state = WIN;  // win
                        stopTimer();
                    }
                }
            }
        }
        field_pressed = false;
    }

    @Override
    public void onSelfRightClick(int x, int y) {
        super.onSelfRightClick(x, y);
        if(state == LOST || state == WIN)
            return;
        if(15 <= x && x < 15 + 16 * field_w && 96 <= y && y <= 96 + 16 * field_h) {  // на поле
            if(cur_field[active_x][active_y] < 0){  // цикл: неоткрытая (-1), флажок (-2), вопрос (-3)
                cur_field[active_x][active_y]--;
                if(cur_field[active_x][active_y] == -4)
                    cur_field[active_x][active_y] = -1;
                if(cur_field[active_x][active_y] == -2)
                    minesLeft--;
                else if(cur_field[active_x][active_y] == -3)
                    minesLeft++;
            }
        }
    }

    @Override
    public void onSelfMouseLeave() {
        super.onSelfMouseLeave();
        field_pressed = false;
    }

    @Override
    public void onSelfOtherTouch() {
        super.onSelfOtherTouch();
        field_pressed = false;
    }

    private void reset(){
        state = WAIT_FOR_CLICK;
        stopTimer();
        timer = 0;
        openedFields = 0;
        field_pressed = false;
        minesLeft = minesCount;
        for(int i=0; i<field_w; i++){
            for(int j=0; j<field_h; j++){
                cur_field[i][j] = NOT_OPENED;
                mines[i][j] = false;
            }
        }
    }

    private void placeMines(int forbid_x, int forbid_y){
        List<Integer> list = new ArrayList<>();
        int forbid_number = forbid_x + forbid_y * field_w;
        for(int i=0; i<field_w*field_h; i++)
            if(i != forbid_number)
                list.add(i);
        Collections.shuffle(list);
        for(int i=0; i<minesCount; i++){
            int number = list.get(i);
            mines[number % field_w][number / field_w] = true;
        }
    }

    /*private void openDFS(int x, int y){  // открыть клетку. если в ней 0, открываем также все соседние, и т. д.
        вызывает stack overflow на тупых устройствах
        if(cur_field[x][y] == FLAG)  // ошибочный флажок
            minesLeft++;
        cur_field[x][y] = 0;
        openedFields++;
        for(int[] move : moves){
            int new_x = x + move[0], new_y = y + move[1];
            if(!(0 <= new_x && new_x < field_w && 0 <= new_y && new_y < field_h))
                continue;
            if(mines[new_x][new_y])
                cur_field[x][y]++;
        }
        if(cur_field[x][y] == 0){
            for(int[] move : moves){
                int new_x = x + move[0], new_y = y + move[1];
                if(!(0 <= new_x && new_x < field_w && 0 <= new_y && new_y < field_h))
                    continue;
                if(cur_field[new_x][new_y] < 0)  // не открыто
                    openDFS(new_x, new_y);
            }
        }
    }*/

    private void openBFS(int startX, int startY){  // открыть клетку. если в ней 0, открываем также все соседние, и т. д.
        Queue<Point> shouldOpen = new ArrayDeque<>();
        shouldOpen.add(new Point(startX, startY));

        for(int i = 0; i < field_w; i++){
            for(int j = 0; j < field_h; j++)
                used[i][j] = false;
        }

        while(!shouldOpen.isEmpty()){
            Point cur = shouldOpen.poll();
            int x = cur.x, y = cur.y;
            if(cur_field[x][y] == FLAG)  // ошибочный флажок
                minesLeft++;
            cur_field[x][y] = 0;
            openedFields++;
            for(int[] move : moves){
                int new_x = x + move[0], new_y = y + move[1];
                if(!(0 <= new_x && new_x < field_w && 0 <= new_y && new_y < field_h))
                    continue;
                if(mines[new_x][new_y])
                    cur_field[x][y]++;
            }
            if(cur_field[x][y] == 0){
                for(int[] move : moves){
                    int new_x = x + move[0], new_y = y + move[1];
                    if(!(0 <= new_x && new_x < field_w && 0 <= new_y && new_y < field_h))
                        continue;
                    if(cur_field[new_x][new_y] < 0 && !used[new_x][new_y]) {  // не открыто
                        shouldOpen.add(new Point(new_x, new_y));
                        used[new_x][new_y] = true;
                    }
                }
            }
        }
    }

    private void startTimer(){
        timer = 1;
        scheduledFuture = scheduler.scheduleAtFixedRate(timerRunnable, 1, 1, TimeUnit.SECONDS);
        //WindowsView.handler.postDelayed(timerRunnable, 1000);
    }
    private void stopTimer(){
        if(scheduledFuture != null)
            scheduledFuture.cancel(false);
        //WindowsView.handler.removeCallbacks(timerRunnable);
    }
    private void changeSizeTo(int x, int y, int minesCount, boolean custom){  // поле x на y клеток
        if(x == field_w && y == field_h && this.minesCount == minesCount)
            return;
        Minesweeper mineNew = new Minesweeper(x, y, minesCount, custom);
        mineNew.alignWith(this);
        // если новое окно вылезает за пределы экрана, его надо подвинуть
        if(mineNew.x_old + mineNew.width > Windows98.SCREEN_WIDTH)
            mineNew.x_old = Windows98.SCREEN_WIDTH - mineNew.width;
        if(mineNew.y_old + mineNew.height > Windows98.TASKBAR_Y)
            mineNew.y_old = Windows98.TASKBAR_Y - mineNew.height;
        //mineNew.restore();
        mineNew.x = mineNew.x_old; mineNew.y = mineNew.y_old;
    }

    @Override
    public void prepareForDelete() {
        super.prepareForDelete();
        stopTimer();
    }

    private class SmileyButton extends Element {
        Bitmap smileBmp;
        Rect src = new Rect(), dst = new Rect();
        SmileyButton(int x, int y){
            this.x = x;
            this.y = y;
            this.width = this.height = 26;
            smileBmp = getBmp(R.drawable.winmine_miner);
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            int bmpNumber;
            if(isPressed())
                bmpNumber = 0;
            else if(state == WIN)
                bmpNumber = 1;
            else if(state == LOST)
                bmpNumber = 2;
            else if(field_pressed) // && winmine.state != LOST){
                bmpNumber = 3;  // wow
            else
                bmpNumber = 4;
            src.left = 0;
            src.top = bmpNumber * 26;
            src.right = 26;
            src.bottom = (bmpNumber + 1) * 26;
            dst.left = x;
            dst.top = y;
            dst.right = x + 26;
            dst.bottom = y + 26;
            canvas.drawBitmap(smileBmp, src, dst, null);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            return 0 <= x && x < width && 0 <= y && y < height;
        }

        @Override
        public void onClick(int x, int y) {
            reset();
        }
    }

    private class CustomField extends DialogWindow {  // выбор произвольного размера поля
        TextBox height, width, mines;

        CustomField(){
            super("Custom Field", 237, 136, Minesweeper.this);
            height = new TextBox(new Rect(80, 35, 136, 51), 2, 12, p, new Rect(-1, -11, 0, 2));
            height.setText(String.valueOf(field_h));
            inputFocus = height;
            addElement(height);
            width = new TextBox(new Rect(80, 70, 136, 86), 2, 12, p, new Rect(-1, -11, 0, 2));
            width.setText(String.valueOf(field_w));
            addElement(width);
            mines = new TextBox(new Rect(80, 104, 136, 120), 2, 12, p, new Rect(-1, -11, 0, 2));
            mines.setText(String.valueOf(minesCount));
            addElement(mines);
            height.isNumeric = width.isNumeric = mines.isNumeric = true;
            height.drawBorder = width.drawBorder = mines.drawBorder = true;
            width.setActive(false);  // так как активируются сразу все 3 TextBox'а
            mines.setActive(false);
            height.moveCursorToEnd();

            Button ok = new Button("OK", new Rect(149, 33, 224, 56), parent -> apply());
            ok.coolActive = true;
            defaultButton = ok;
            addElement(ok);
            addCloseButton(new Rect(149, 61, 224, 84), "Close");
            alignWithParent(28, 67);
        }

        @Override
        public void onNewDraw(Canvas canvas, int x, int y) {
            super.onNewDraw(canvas, x, y);
            canvas.drawText("Height:", x + 15, y + 48, p);
            canvas.drawText("Width:", x + 14, y + 82, p);
            canvas.drawText("Mines:", x + 15, y + 116, p);
        }

        private void apply(){
            int w = parseInt(width.text);
            int h = parseInt(height.text);
            int mines = parseInt(this.mines.text);
            if(w < 8)
                w = 8;
            else if(w > 38)
                w = 38;
            if(h < 1)
                h = 1;
            else if(h > 21)
                h = 21;
            mines = Math.min(mines, w * h - 1);  // нельзя проиграть первым ходом!
            close();
            changeSizeTo(w, h, mines, true);
        }

        private int parseInt(String s){
            if(s.isEmpty() || s.length() >= 9)  // чтобы не получить ошибку в следующей строчке
                return 0;
            return Integer.parseInt(s);
        }
    }
}
