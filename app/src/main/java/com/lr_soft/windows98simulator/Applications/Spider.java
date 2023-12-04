package com.lr_soft.windows98simulator.Applications;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.media.MediaPlayer;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.DialogWindow;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.HelpTopics;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.System.RadioButton;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Windows98;
import com.lr_soft.windows98simulator.WindowsView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Spider extends BaseSolitaire {
    private Bitmap[] cardBitmaps;
    private int difficulty;
    private final static int EASY = 0, MEDIUM = 1, DIFFICULT = 2;
    private int score = 0, moves = 0;
    private boolean win = false;
    private List<Hint> hints;
    private int currentHint;
    private Bitmap winBmp;
    private Paint fillPaint;
    private Rect scoreWindow = new Rect();
    private ButtonInList dealNextRow, openLastSavedGame;
    private TopMenuButton dealButton;
    private MediaPlayer takeSound, releaseSound, hintSound, noMovesSound, dealStockSound, winSound;
    private int[] tmp = new int[71 * 96];
    private Random random = new Random();
    private int currentSeed;  // seed для текущей игры

    public Spider(){
        super("Spider", getBmp(R.drawable.spider_icon_small), Windows98.SCREEN_WIDTH, Windows98.TASKBAR_Y, true, true, false);
        if(!Windows98.WIDESCREEN){
            // просим переключиться в широкоэкранный режим
            forceClose();
            new MessageBox("Spider", "This game requires widescreen resolution. Restart now?\n(You can return to the previous resolution in Control Panel).",
                    MessageBox.YESNO, MessageBox.WARNING, new MessageBox.MsgResultListener() {
                @Override
                public void onMsgResult(int buttonNumber) {
                    if(buttonNumber == YES){
                        SharedPreferences sharedPreferences = getSharedPreferences();
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("widescreen", true);
                        editor.apply();
                        Windows98.windows98.restart();
                    }
                }
            }, null);
            return;
        }

        drawElements = false;  // см. Window. Мы сами рисуем элементы, так как хотим рисовать зелёное поле под картами
        fillPaint = new Paint();
        fillPaint.setShader(new BitmapShader(getBmp(R.drawable.spider_fill), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));

        cardBitmaps = new Bitmap[53];
        for(int i = 0; i < 52; i++)
            cardBitmaps[i] = getBmp(cardBitmapsIds[i]);
        cardBitmaps[52] = getBmp(R.drawable.spider_cardback);

        takeSound = MediaPlayer.create(context, R.raw.spider_take);
        releaseSound = MediaPlayer.create(context, R.raw.spider_release);
        hintSound = MediaPlayer.create(context, R.raw.spider_hint);
        noMovesSound = MediaPlayer.create(context, R.raw.spider_no_moves);
        dealStockSound = MediaPlayer.create(context, R.raw.spider_deal_stock);
        winSound = MediaPlayer.create(context, R.raw.spider_win);

        for(int i = 0; i < 10; i++)
            addElement(new RowStack());
        addElement(new Stock());
        addElement(new Foundation());

        // top menu
        ButtonList game = new ButtonList();
        game.elements.add(new ButtonInList("New Game", "F2", new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new MessageBox("Spider", "Are you sure you want to start a new game?",
                        MessageBox.YESNO, MessageBox.QUESTION, new MessageBox.MsgResultListener() {
                    @Override
                    public void onMsgResult(int buttonNumber) {
                        if(buttonNumber == YES)
                            newGame();
                    }
                }, Spider.this);
            }
        }));
        game.elements.add(new ButtonInList("Restart This Game", new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new MessageBox("Spider", "Are you sure you want to restart this game from the beginning?",
                        MessageBox.YESNO, MessageBox.QUESTION, new MessageBox.MsgResultListener() {
                    @Override
                    public void onMsgResult(int buttonNumber) {
                        if(buttonNumber == YES)
                            newGame(currentSeed);
                    }
                }, Spider.this);
            }
        }));
        game.elements.add(new Separator());
        ButtonInList undo = new ButtonInList("Undo", "Ctrl+Z");
        undo.disabled = true;
        game.elements.add(undo);
        dealNextRow = new ButtonInList("Deal Next Row", "D", parent -> dealStock());
        game.elements.add(dealNextRow);
        game.elements.add(new ButtonInList("Show An Available Move", "M", parent -> showHint()));
        game.elements.add(new Separator());
        game.elements.add(new ButtonInList("Difficulty...", "F3", parent -> new DifficultyDialog()));
        game.elements.add(new ButtonInList("Statistics...", "F4"));
        game.elements.add(new ButtonInList("Options...", "F5"));
        game.elements.add(new Separator());
        game.elements.add(new ButtonInList("Save This Game", "Ctrl+S", parent -> saveGameWithCheck()));
        openLastSavedGame = new ButtonInList("Open Last Saved Game", "Ctrl+O", new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new MessageBox("Spider", "Are you sure you want to discard the game you are currently playing, and load your previously saved game?",
                        MessageBox.YESNO, MessageBox.QUESTION, new MessageBox.MsgResultListener() {
                    @Override
                    public void onMsgResult(int buttonNumber) {
                        if(buttonNumber == YES)
                            loadGame();
                    }
                }, Spider.this);
            }
        });
        openLastSavedGame.disabled = !getSharedPreferences().contains(saveKey);
        game.elements.add(openLastSavedGame);
        game.elements.add(new Separator());
        game.elements.add(new ButtonInList("Exit", parent -> close()));
        ButtonList help = new ButtonList();
        help.elements.add(new ButtonInList("Contents", "F1", parent ->
                new HelpTopics("Spider Solitaire Help", false,
                new int[]{R.drawable.spider_help_1, R.drawable.spider_help_2, R.drawable.spider_help_3})));
        help.elements.add(new Separator());
        help.elements.add(new ButtonInList("About Spider...", parent -> {
            new AboutSpider();  // inner class, поэтому нельзя сделать конструктором ButtonInList
        }));
        topMenu.elements.add(new TopMenuButton("Game", game));
        dealButton = new TopMenuButton("Deal!", parent -> dealStock());
        topMenu.elements.add(dealButton);
        //dealButton.disabled = true;
        topMenu.elements.add(new TopMenuButton("Help", help));
        maximize();
        new DifficultyDialog();
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        limitsRect.set(fieldRect);
        limitsRect.offset(x, y);
        canvas.save();
        canvas.translate(x, y);
        canvas.drawRect(fieldRect, fillPaint);
        canvas.restore();
        drawCards(canvas, x, y, false);
        // рисуем окошко со счетом
        scoreWindow.set(0, 0, 200, 96);
        scoreWindow.offset(limitsRect.centerX() - scoreWindow.centerX(), limitsRect.bottom - 106);
        p.setColor(Color.BLACK);
        canvas.drawRect(scoreWindow, p);
        p.setColor(Color.rgb(0, 127, 0));
        canvas.drawRect(scoreWindow.left + 1, scoreWindow.top + 1,
                scoreWindow.right - 1, scoreWindow.bottom - 1, p);
        p_system.setColor(Color.WHITE);
        canvas.drawText("Score:", scoreWindow.left + 60, scoreWindow.top + 43, p_system);
        canvas.drawText(String.valueOf(score), scoreWindow.left + 111, scoreWindow.top + 43, p_system);
        canvas.drawText("Moves:", scoreWindow.left + 53, scoreWindow.top + 63, p_system);
        canvas.drawText(String.valueOf(moves), scoreWindow.left + 111, scoreWindow.top + 63, p_system);

        drawMovingCards(canvas, x, y);
        if(win){  // рисуем надпись "You Won!"
            int drawX = limitsRect.centerX() - winBmp.getWidth() / 2;
            int drawY = limitsRect.centerY() - winBmp.getHeight() / 2;
            canvas.drawBitmap(winBmp, drawX, drawY, null);
        }
    }

    @Override
    public void repositionElements() {
        fieldRect.set(4, 42, width - 4, height - 4);
        int cardDx = getCardDx(10);
        int curX = fieldRect.left + cardDx - cardWidth;
        for(int i = 0; i < 10; i++){
            RowStack rowStack = (RowStack) elements.get(i);
            rowStack.x = curX;
            curX += cardDx;
            rowStack.y = fieldRect.top + 10;
            rowStack.updateCardDy();
        }
        Element stock = elements.get(10);
        stock.x = elements.get(9).x;
        stock.y = fieldRect.bottom - 106;
        Element foundation = elements.get(11);
        foundation.x = elements.get(0).x;
        foundation.y = fieldRect.bottom - 106;
    }

    @Override
    void onCardMoved(CardStack src, CardStack dst) {
        score--;
        if(score < 0)
            score = 0;
        moves++;
        hints = null;
        if(!src.elements.isEmpty())
            src.top().closed = false;
        if(dst.elements.size() >= 13) {
            // если выстроить последовательность от туза до короля одной масти, она уходит в Foundation
            boolean fullStack = true;
            for(int i = 0; i < 13; i++){
                Card card = dst.cardFromTop(i);
                if(card.closed || card.number != i || card.suit != dst.top().suit){
                    fullStack = false;
                    break;
                }
            }
            if(fullStack){
                score += 100;
                Foundation foundation = (Foundation) elements.get(11);
                for(int i = 0; i < 13; i++)
                    foundation.elements.add(dst.pop());
                if(!dst.elements.isEmpty())
                    dst.top().closed = false;
                dealStockSound.start();
                // проверяем выигрыш
                if(foundation.elements.size() == 52 * 2){
                    win = true;
                    if(winBmp == null)
                        winBmp = getBmp(R.drawable.you_won);
                    winSound.start();
                    new GameOverDialog();
                }
            }
        }
        ((RowStack) src).updateCardDy();
        ((RowStack) dst).updateCardDy();
    }

    private void dealStock(){
        Stock stock = (Stock) elements.get(10);
        if(stock.elements.isEmpty())
            return;

        for(int i = 0; i < 10; i++){
            RowStack rowStack = (RowStack) elements.get(i);
            if(rowStack.elements.isEmpty()){
                new MessageBox("Spider", "You are not allowed to deal a new row while there are any empty slots.",
                        MessageBox.OK, MessageBox.INFO, null, this);
                return;
            }
        }

        dealStockSound.start();
        for(int i = 0; i < 10; i++){
            Card card = stock.pop();
            card.closed = false;
            RowStack rowStack = (RowStack) elements.get(i);
            rowStack.elements.add(card);
            rowStack.updateCardDy();
        }
        if(stock.elements.isEmpty())
            dealButton.disabled = dealNextRow.disabled = true;
        hints = null;
    }

    private void newGame(){
        currentSeed = random.nextInt();
        newGame(currentSeed);
    }

    private void newGame(long seed){  // разложить карты для новой игры
        score = 500;
        moves = 0;
        dealButton.disabled = dealNextRow.disabled = false;
        win = false;
        hints = null;
        for(int i = 0; i < 12; i++){  // удаляем старые карты
            ((CardStack) elements.get(i)).elements.clear();
        }

        List<Card> cards = new ArrayList<>();
        switch (difficulty){
            case EASY:
                // 8 раз пики
                for(int i = 0; i < 8; i++){
                    for(int j = 39; j < 52; j++)
                        cards.add(new Card(j, cardBitmaps, tmp));
                }
                break;
            case MEDIUM:
                // 4 раза червы, 4 раза пики
                for(int i = 0; i < 4; i++){
                    for(int j = 26; j < 52; j++)
                        cards.add(new Card(j, cardBitmaps, tmp));
                }
                break;
            case DIFFICULT:
                // по 2 раза все карты
                for(int i = 0; i < 2; i++){
                    for(int j = 0; j < 52; j++)
                        cards.add(new Card(j, cardBitmaps, tmp));
                }
                break;
        }
        random.setSeed(seed);
        Collections.shuffle(cards, random);
        for(int i = 0; i < 54; i++) {
            RowStack rowStack = (RowStack) elements.get(i % 10);
            rowStack.elements.add(cards.get(i));
        }
        Stock stock = (Stock) elements.get(10);
        for(int i = 54; i < cards.size(); i++)
            stock.elements.add(cards.get(i));

        for(int i = 0; i < 10; i++){
            RowStack rowStack = (RowStack) elements.get(i);
            rowStack.top().closed = false;
            rowStack.updateCardDy();
        }
    }

    private boolean gameStarted(){
        return score != 0 || moves != 0;
    }

    private void setupHints(){
        hints = new ArrayList<>();
        currentHint = -1;
        // сколько карт сверху каждой стопки образуют возрастающую последовательность
        int[] ascendingAmounts = new int[10];
        for(int i = 0; i < 10; i++){
            RowStack rowStack = (RowStack) elements.get(i);
            if(rowStack.elements.isEmpty()){
                ascendingAmounts[i] = 0;
                continue;
            }
            ascendingAmounts[i] = 1;
            for(int j = 1; j < rowStack.elements.size(); j++){
                Card card = rowStack.cardFromTop(j);
                Card previous = rowStack.cardFromTop(j - 1);
                if(!card.closed && card.number == previous.number + 1 && card.suit == previous.suit)
                    ascendingAmounts[i]++;
                else
                    break;
            }
        }
        // перебираем возможные ходы
        for(int i = 0; i < 10; i++){
            if(ascendingAmounts[i] == 0)
                continue;
            for(int j = 0; j < 10; j++){
                if(i == j)
                    continue;
                RowStack stack1 = (RowStack) elements.get(i);
                RowStack stack2 = (RowStack) elements.get(j);
                if(stack2.elements.isEmpty() ||
                        stack2.top().number == stack1.cardFromTop(ascendingAmounts[i] - 1).number + 1)
                    hints.add(new Hint(stack1, ascendingAmounts[i], stack2));
            }
        }
    }

    private Runnable showHintRunnable = new Runnable() {
        @Override
        public void run() {
            Hint hint = hints.get(currentHint);
            hint.showNext();
            updateWindow();
            if(hint.show != Hint.NOTHING)
                WindowsView.handler.postDelayed(this, Hint.DELAY);
        }
    };

    private void showHint(){
        if(hints == null)
            setupHints();
        if(hints.isEmpty())
            noMovesSound.start();
        else {
            currentHint = (currentHint + 1) % hints.size();
            hints.get(currentHint).showNext();
            WindowsView.handler.postDelayed(showHintRunnable, Hint.DELAY);
            hintSound.start();
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        boolean showingHint = hints != null && !hints.isEmpty() &&
                hints.get(currentHint).show != Hint.NOTHING;
        if(showingHint && touch && 0 <= x && x < width && 0 <= y && y < height)
            return true;
        return super.onMouseOver(x, y, touch);
    }

    // ===============================================
    // =============== сохранение ====================
    // ===============================================
    private final static String saveKey = "spiderSave";
    private boolean closeOnSave = false;

    private void saveGame(){
        List<Integer> stream = new ArrayList<>();
        stream.add(currentSeed);
        stream.add(difficulty);
        stream.add(score);
        stream.add(moves);
        for(int i = 0; i < 12; i++){
            ((CardStack) elements.get(i)).save(stream);
        }
        StringBuilder builder = new StringBuilder();
        for(int x : stream){
            builder.append(x);
            builder.append(' ');
        }

        SharedPreferences sharedPreferences = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(saveKey, builder.toString());
        editor.apply();
        openLastSavedGame.disabled = false;
        if(closeOnSave)
            super.close(true);
    }

    private void saveGameWithCheck(){
        if(win || !gameStarted()) {
            return;
        }
        if(openLastSavedGame.disabled){  // нет сохранения
            saveGame();
            return;
        }
        new MessageBox("Spider", "A saved game already exists. Are you sure you want to replace your previously saved game with your current game?",
                MessageBox.YESNO, MessageBox.QUESTION, new MessageBox.MsgResultListener() {
            @Override
            public void onMsgResult(int buttonNumber) {
                if(buttonNumber == YES)
                    saveGame();
                else
                    closeOnSave = false;
            }
        }, Spider.this);
    }

    private void loadGame(){
        SharedPreferences sharedPreferences = getSharedPreferences();
        String save = sharedPreferences.getString(saveKey, null);
        if(save == null)
            return;
        Scanner scanner = new Scanner(save);
        currentSeed = scanner.nextInt();
        difficulty = scanner.nextInt();
        score = scanner.nextInt();
        moves = scanner.nextInt();
        for(int i = 0; i < 12; i++){
            CardStack cardStack = (CardStack) elements.get(i);
            cardStack.load(scanner, cardBitmaps, tmp);
        }
        Stock stock = (Stock) elements.get(10);
        dealButton.disabled = dealNextRow.disabled = stock.elements.isEmpty();
        win = false;
        hints = null;
    }

    @Override
    public void close(final boolean activateNextWindow) {
        if(win || !gameStarted()){  // выигрыш или игра не началась
            super.close(activateNextWindow);
            return;
        }
        new MessageBox("Spider", "Do you want to save this game before closing it?",
                MessageBox.YESNOCANCEL, MessageBox.QUESTION, new MessageBox.MsgResultListener() {
            @Override
            public void onMsgResult(int buttonNumber) {
                if(buttonNumber == YES) {
                    closeOnSave = true;
                    saveGameWithCheck();
                }
                else if(buttonNumber == NO)
                    Spider.super.close(activateNextWindow);
            }
        }, this);
    }

    // =================================================
    // ================ Классы для карт ================
    // =================================================

    private class RowStack extends CardStack {
        Bitmap backgroundBmp = getBmp(R.drawable.spider_background);
        int cardDy;
        boolean inverted = false;  // для подсказок
        Bitmap invertedBmp = getBmp(R.drawable.spider_bg_inverted);

        RowStack() {}

        // dy после свернутых карт - 7
        // для развернутых max dy = 28, min dy = 15
        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            canvas.drawBitmap(inverted? invertedBmp : backgroundBmp, x, y, null);
            int cur_x = 0, cur_y = 0;
            for(Element el : elements){
                Card card = (Card) el;
                card.x = cur_x;
                card.y = cur_y;
                if(card.closed)
                    cur_y += 7;
                else
                    cur_y += cardDy;
            }
            drawElements(canvas, x, y);
        }

        @Override
        boolean acceptCards(List<Card> cards) {
            if(elements.isEmpty())
                return true;
            return top().number == cards.get(0).number + 1;
        }

        // можно взять стопку только если она образует возрастающую последовательность
        @Override
        boolean mayTakeCards(Card start) {
            if(start.closed)
                return false;
            int index = elements.indexOf(start);
            for(int i = index; i < elements.size() - 1; i++){
                Card bottom = (Card) elements.get(i);
                Card top = (Card) elements.get(i + 1);
                if(bottom.number != top.number + 1 || bottom.suit != top.suit)
                    return false;
            }
            return true;
        }

        void updateCardDy(){  // карты не должны вылезать ниже окошка со счетом
            int spaceLeft = fieldRect.bottom - y - 106;
            int openCards = 0;
            for(Element element : elements){
                Card card = (Card) element;
                if(card.closed)
                    spaceLeft -= 7;
                else
                    openCards++;
            }
            if(openCards <= 1)
                return;
            // spaceTaken = cardDy * (openCards - 1) + cardHeight
            cardDy = (spaceLeft - cardHeight) / (openCards - 1);
            if(cardDy > 28)
                cardDy = 28;
            else if(cardDy < 15)
                cardDy = 15;
        }

        @Override
        public int getCardDy() {
            return cardDy;
        }

        @Override
        void load(Scanner scanner, Bitmap[] cardBitmaps, int[] pixels) {
            super.load(scanner, cardBitmaps, pixels);
            updateCardDy();
        }
    }

    private class Stock extends CardStack {
        // x, y - левый верхний угол правой карты
        final static int cardDx = 12;

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            int cur_x = x;
            for(int i = 0; i < cardAmount(); i++){
                // рисуем закрытые карты
                canvas.drawBitmap(cardBitmaps[52], cur_x, y, null);
                cur_x -= cardDx;
            }
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(cardAmount() == 0)
                return false;
            boolean result = -(cardAmount() - 1) * cardDx <= x && x < cardWidth &&
                    0 <= y && y < cardHeight;
            if(!result)
                return false;
            if(touch)
                dealStock();
            return true;
        }

        int cardAmount(){
            return elements.size() / 10;
        }

        @Override
        boolean acceptCards(List<Card> cards) {
            return false;
        }
    }

    private static class Foundation extends CardStack {
        final static int cardDx = 12;

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            int cur_x = x;
            for(int i = 12; i < elements.size(); i += 13){
                elements.get(i).onDraw(canvas, cur_x, y);
                cur_x += cardDx;
            }
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            return false;
        }

        @Override
        boolean acceptCards(List<Card> cards) {
            return false;
        }
    }

    private class Hint {
        RowStack src, dst;
        int amount;
        final static int NOTHING = 2;
        int show = NOTHING;  // 0 - первый ряд, 1 - второй ряд, 2 - ничего
        final static int DELAY = 500;  // в оригинале 250

        Hint(RowStack src, int amount, RowStack dst) {
            this.src = src;
            this.dst = dst;
            this.amount = amount;
        }

        void showNext(){
            show = (show + 1) % 3;
            for(int i = 0; i < amount; i++) {
                Card card = src.cardFromTop(i);
                card.inverted = (show == 0);
            }
            if(!dst.elements.isEmpty())
                dst.top().inverted = (show == 1);
            dst.inverted = (show == 1 && dst.elements.isEmpty());
        }
    }

    @Override
    void playCardTakenSound() {
        // т. к. если делать двойной тап, то будет два раза воспроизведение
        if(!WindowsView.windowsView.artificialClickOngoing)
            takeSound.start();
    }

    @Override
    void playCardReleasedSound() {
        if(!WindowsView.windowsView.artificialClickOngoing)
            releaseSound.start();
    }

    // ==================================
    // ============ Диалоги =============
    // ==================================

    private class AboutSpider extends DialogWindow {
        Bitmap spiderLogo = getBmp(R.drawable.spider_logo);

        AboutSpider(){
            super("About Spider", 471, 288, Spider.this);
            Button ok = addCloseButton(new Rect(383, 33, 458, 56), "OK");
            ok.coolActive = true;
            defaultButton = ok;
            centerWindowOnScreen();
        }

        @Override
        public void onNewDraw(Canvas canvas, int x, int y) {
            super.onNewDraw(canvas, x, y);
            canvas.drawBitmap(spiderLogo, x + 13, y + 32, null);
        }
    }

    private class GameOverDialog extends DialogWindow {
        GameOverDialog(){
            super("Game Over", 255, 137, Spider.this);
            Button yes = new Button("Yes", new Rect(41, 100, 116, 123), parent -> {
                close();
                newGame();
            });
            yes.coolActive = true;
            defaultButton = yes;
            addElement(yes);
            addCloseButton(new Rect(140, 100, 215, 123), "No");
            //centerWindowOnScreen();
        }

        @Override
        public void onNewDraw(Canvas canvas, int x, int y) {
            super.onNewDraw(canvas, x, y);
            p.setColor(Color.BLACK);
            canvas.drawText("Congratulations, you won!", x + 15, y + 44, p);
            canvas.drawText("Do you want to start another game?", x + 15, y + 70, p);
        }
    }

    private class DifficultyDialog extends DialogWindow {
        Bitmap diamond, club, heart, spade;
        final static String difficultyKey = "spiderDefaultDifficulty";

        DifficultyDialog(){
            super("Difficulty", 302, 202, Spider.this);
            diamond = getBmp(R.drawable.spider_diamond);
            club = getBmp(R.drawable.spider_club);
            heart = getBmp(R.drawable.spider_heart);
            spade = getBmp(R.drawable.spider_spade);

            final RadioButton easy = new RadioButton("Easy: One Suit");
            final RadioButton medium = new RadioButton("Medium: Two Suits");
            final RadioButton difficult = new RadioButton("Difficult: Four Suits");
            RadioButton.createGroup(easy, medium, difficult);
            addElement(easy, 82, 70);
            addElement(medium, 82, 96);
            addElement(difficult, 82, 122);

            int defaultDifficulty = gameStarted()?
                    difficulty : getSharedPreferences().getInt(difficultyKey, EASY);
            switch (defaultDifficulty){
                case EASY:
                    easy.active = true;
                    break;
                case MEDIUM:
                    medium.active = true;
                    break;
                case DIFFICULT:
                    difficult.active = true;
                    break;
            }

            Button ok = new Button("OK", new Rect(63, 165, 138, 188), parent -> {
                if(easy.active)
                    difficulty = EASY;
                else if(medium.active)
                    difficulty = MEDIUM;
                else
                    difficulty = DIFFICULT;
                close();
                SharedPreferences.Editor editor = getSharedPreferences().edit();
                editor.putInt(difficultyKey, difficulty);
                editor.apply();
                newGame();
            });
            defaultButton = ok;
            ok.coolActive = true;
            addElement(ok);
            addCloseButton(new Rect(162, 165, 237, 188), "Cancel");
            centerWindowOnScreen();
        }

        @Override
        public void onNewDraw(Canvas canvas, int x, int y) {
            super.onNewDraw(canvas, x, y);
            p.setColor(Color.BLACK);
            canvas.drawText("Select the game difficulty that you want:", x + 15, y + 44, p);
            canvas.drawBitmap(spade, x + 69, y + 71, null);
            canvas.drawBitmap(spade, x + 69, y + 97, null);
            canvas.drawBitmap(spade, x + 69, y + 123, null);
            canvas.drawBitmap(heart, x + 56, y + 97, null);
            canvas.drawBitmap(heart, x + 56, y + 123, null);
            canvas.drawBitmap(club, x + 42, y + 123, null);
            canvas.drawBitmap(diamond, x + 30, y + 123, null);
        }
    }
}
