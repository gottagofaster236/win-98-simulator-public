package com.lr_soft.windows98simulator.Applications;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.AboutWindow;
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.CheckBox;
import com.lr_soft.windows98simulator.System.DialogWindow;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.HelpTopics;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.RadioButton;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Windows98;
import com.lr_soft.windows98simulator.WindowsView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Solitaire extends BaseSolitaire {
    private Bitmap[] cardBitmaps;

    // первый - относительно верхнего левого угла, другой - абсолютный
    volatile int score = 0, time = 0;
    private int bonus;  // в конце игры
    private final static int WAIT_FOR_CLICK = 0, PLAYING = 1, WIN = 2, DEAL_AGAIN = 3;
    int state = WAIT_FOR_CLICK;
    int drawAmount = getSharedPreferences().getBoolean(OptionsWindow.drawThreeString, true)? 3 : 1;  // по умолчанию drawThree
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);  // для (точного!) таймера
    private ScheduledFuture<?> scheduledFuture;
    private boolean timedGame = getSharedPreferences().getBoolean(OptionsWindow.timedGameString, true);
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (Solitaire.this) {
                time++;
                if (time % 10 == 0) {  // снимаем очки каждые 10 секунд
                    score -= 2;
                    if (score < 0)
                        score = 0;
                }
            }
            Solitaire.this.shouldRedraw = true;
            WindowsView.windowsView.postInvalidate();
        }
    };
    // переменные для win
    Bitmap winBmp = createBitmap(Windows98.SCREEN_WIDTH - 4, Windows98.SCREEN_HEIGHT - 87, Bitmap.Config.ARGB_8888);
    Canvas winCanvas = new Canvas(winBmp);
    // чтобы рисовать win для случая minimizedBounds
    private Rect src = new Rect(0, 0, 585, 367), dst = new Rect(src.left + 6, src.top + 44, src.right + 6, src.bottom + 44);
    private Rect tmp = new Rect();  // хранит абсолютные, а не относительные координаты в dst
    int curStack = 0;  // откуда берем следующую карту
    Card curCard = null;  // карта, которая сейчас находится в движении
    float cur_x, cur_y, cur_vx, cur_vy;
    float g = 0.3f;  // ускорение свободного падения для карт

    Runnable winRunnable = new Runnable() {
        @Override
        public void run() {
            if(curCard == null){
                CardStack cardStack = ((CardStack) elements.get(8 + curStack));
                if(cardStack.elements.isEmpty()){
                    askDealAgain();
                    WindowsView.windowsView.invalidate();
                    return;
                }
                curCard = cardStack.pop();
                curCard.x = curCard.y = 0;
                curStack = (curStack + 1) % 4;
                cur_x = cardStack.x - dst.left;
                cur_y = cardStack.y - dst.top;
                // значения из слитых исходников windows 2000 ;)
                cur_vx = ((int) randomInRange(-65, 45)) / 10;
                if(Math.abs(cur_vx) < 2){  // скорость по иксу слишком маленькая - карта будет двигаться очень долго
                    cur_vx = -2;
                }
                cur_vy = randomInRange(-7.5f, 3.5f);
            }
            else{
                cur_x += cur_vx;
                cur_y += cur_vy;
                cur_vy += g;
                int field_height = maximized? winBmp.getHeight() : src.height();
                if(cur_y + curCard.height > field_height) {
                    cur_y = field_height - curCard.height;
                    cur_vy = -cur_vy * 0.8f;
                }
            }
            curCard.x = (int) cur_x;
            curCard.y = (int) cur_y;
            curCard.onDraw(winCanvas, curCard.x, curCard.y);
            if(cur_x >= (maximized? winBmp.getWidth() : src.width()) || cur_x + curCard.width <= 0){  // вылезли за поле
                curCard = null;
            }
            WindowsView.handler.postDelayed(winRunnable, 16);
            updateWindow();
        }
    };
    private float randomInRange(float min, float max){
        return min + ((float) Math.random()) * (max - min);
    }

    public Solitaire() {
        super("Solitaire", StartMenu.solitaire, 597, 434, true, true, false);
        drawElements = false;  // см. Window. Мы сами рисуем элементы, так как хотим рисовать зелёное поле ПОД картами, то есть заранее
        cardBitmaps = new Bitmap[cardBitmapsIds.length];

        for (int i = 0; i < cardBitmaps.length; i++)
            cardBitmaps[i] = getBmp(cardBitmapsIds[i]);
        // добавляем стеки
        for(int i=0; i<7; i++)
            addElement(new RowStack());
        updateFieldRect();
        addElement(new Deck(cardBitmaps));
        for(int i=0; i<4; i++)
            addElement(new SuitStack(cardBitmaps[54]));
        setupCards();  // заполняем стеки картами
        // контекстное меню
        ButtonList game = new ButtonList();
        ButtonInList deal = new ButtonInList("Deal", parent -> reset());
        game.elements.add(deal);
        game.elements.add(new Separator());
        ButtonInList undo = new ButtonInList("Undo");
        undo.disabled = true;
        game.elements.add(undo);
        game.elements.add(new ButtonInList("Deck..."));
        game.elements.add(new ButtonInList("Options...", parent -> new OptionsWindow()));
        game.elements.add(new Separator());
        ButtonInList exit = new ButtonInList("Exit", parent -> close());
        game.elements.add(exit);
        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("Help Topics", parent ->
                new HelpTopics("Solitaire Help", false,
                new int[]{R.drawable.sol1, R.drawable.sol2, R.drawable.sol3}));
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("About Solitaire", parent ->
                new AboutWindow(Solitaire.this, "Solitaire",
                        getBmp(R.drawable.game_solitaire_0), "Developed for Microsoft by Wes Cherry"));
        help.elements.add(about);
        topMenu.elements.add(new TopMenuButton("Game", game));
        topMenu.elements.add(new TopMenuButton("Help", help));
        repositionElements();
    }

    private void setupCards(){
        for(int i=0; i<12; i++)
            ((CardStack) elements.get(i)).elements.clear();  // убираем все карты от предыдущей игры
        Deck deck = (Deck) elements.get(7);
        deck.deckCards.clear();
        deck.timesGoThrough = 0;
        /*
        // код, чтобы сделать красивый скриншот (прыгающие карты)
        for(int i=8; i<12; i++){
            CardStack cardStack = (CardStack) elements.get(i);
            cardStack.elements.add(new Card(Card.A, i-8, cardBitmaps));
            for(int j=1; j<12; j++)
                cardStack.elements.add(new Card(j, i-8, cardBitmaps));
            for(Element el : cardStack.elements)
                ((Card) el).closed = false;
        }
        for(int i=0; i<4; i++)
            ((CardStack) elements.get(i)).elements.add(new Card(Card.K, i, cardBitmaps));
        if(2 == 2)
            return;
        */
        List<Card> allCards = new ArrayList<>();
        for(int i = 0; i < 13; i++){
            for(int j = 0; j < 4; j++){
                allCards.add(new Card(i, j, cardBitmaps));
            }
        }
        Collections.shuffle(allCards);
        int index = 0;  // текущая карта
        // 24 in deck!!!
        for(int i = 0; i < 7; i++){
            RowStack rowStack = (RowStack) elements.get(i);
            for(int j = 0; j < i + 1; j++){
                rowStack.elements.add(allCards.get(index));
                index++;
                if(j == i){  // последняя карта должна быть открытой
                    rowStack.top().closed = false;
                }
            }
        }

        for(int i = 0; i < 24; i++) {
            Card adding = allCards.get(index);
            adding.closed = false;
            deck.deckCards.add(adding);
            index++;
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(!super.onMouseOver(x, y, state != WIN && touch))  // если WIN, то touch = false. (Так как, например, при нажатии кнопки "развернуть" не надо разворачивать, а надо показать messagebox)
            return false;                                       // если не WIN, то всё как обычно

        if(state == WAIT_FOR_CLICK && touch && !topMenu.onMouseOver(x - topMenu.x, y - topMenu.y, false) && fieldRect.contains(x, y)){  // кликнули в поле, не в меню
            state = PLAYING;
            startTimer();
        }

        if(!movingCards.isEmpty()){
            return true;
        }

        if(touch && state == WIN){  // куда не нажать - будет messagebox
            onOtherTouch();
            askDealAgain();
            return true;
        }
        return true;
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        // рисуем линии и прямоугольники
        drawFrameRectActive(canvas, x + 4, y + 42, x + width - 4, y + height - 4);
        p.setColor(Color.parseColor("#00A857"));  // зелёный
        limitsRect.set(fieldRect);
        limitsRect.offset(x, y);
        canvas.drawRect(limitsRect, p);
        p.setColor(Color.BLACK);
        canvas.drawRect(x + 6, y + height - 23, x + width - 6, y + height - 22, p);
        p.setColor(Color.WHITE);
        canvas.drawRect(x + 6, y + height - 22, x + width - 6, y + height - 6, p);
        // рисуем текст
        p_system.setTextAlign(Paint.Align.RIGHT);
        p_system.setColor(Color.BLACK);
        canvas.drawText("Score: " + score + (timedGame? " Time: " + time : ""), x + width - 12, y + height - 9, p_system);
        p_system.setTextAlign(Paint.Align.LEFT);
        if(state == WIN){
            tmp.left = dst.left + x;
            tmp.top = dst.top + y;
            tmp.right = dst.right + x;
            tmp.bottom = dst.bottom + y;
            if(maximized)
                canvas.drawBitmap(winBmp, tmp.left, tmp.top, null);
            else
                canvas.drawBitmap(winBmp, src, tmp, null);
            String text = "Press Esc or a mouse button to stop...";
            if(timedGame)
                text = "Bonus: " + bonus + "  " + text;
            canvas.drawText(text, x + 11, y + height - 9, p_system);
            topButtons.onDraw(canvas, x + topButtons.x, y + topButtons.y);
            return;
        }
        if(state == DEAL_AGAIN) {  // рисуем зелёное пустое поле
            topButtons.onDraw(canvas, x + topButtons.x, y + topButtons.y);
            return;
        }
        drawCards(canvas, x, y, true);
    }

    @Override
    public void repositionElements() {
        updateFieldRect();
        // располагаем стеки
        int gap = getCardDx();
        int startX = gap - cardWidth + 6;

        for(int i = 0; i < 7; i++){
            RowStack rowStack = (RowStack) elements.get(i);
            rowStack.x = startX + gap * i;
            rowStack.y = 151;
        }
        Deck deck = (Deck) elements.get(7);
        deck.x = startX;
        deck.y = 49;
        for(int i = 8; i < 12; i++){
            SuitStack suitStack = (SuitStack) elements.get(i);
            suitStack.x = startX + gap * (i - 5);
            suitStack.y = 49;
        }
        if(state == WIN){
            askDealAgain();
        }
    }

    private void updateFieldRect(){
        fieldRect.left = 6;
        fieldRect.top = 44;
        fieldRect.right = width - 6;
        fieldRect.bottom = height - 23;
    }

    private int getCardDx(){
        return getCardDx(7);
    }

    @Override
    void onSolitaireClick(int x, int y, boolean callSuperOnClick) {
        if(state == WIN)
            return;
        super.onSolitaireClick(x, y, callSuperOnClick);
    }

    @Override
    void onCardMoved(CardStack from, CardStack to){
        // подсчитываем очки
        synchronized (this) {
            if (!(from instanceof SuitStack) && to instanceof SuitStack)  // move card to suit stack
                score += 10;
            else if (from instanceof Deck && to instanceof RowStack)  // from deck to row stack
                score += 5;
            else if (from instanceof SuitStack && to instanceof RowStack)  // from suit stack back to row stack
                score -= 15;
            if (score < 0)
                score = 0;
        }
        checkForWin();
    }

    void checkForWin(){
        for(int i = 0; i < 8; i++) {  // 7 стопок и Deck
            if(!((CardStack) elements.get(i)).elements.isEmpty())
                return;
        }
        if(!((Deck) elements.get(7)).deckCards.isEmpty())
            return;
        // win
        if(time >= 30)  // прошло не менее 30 секунд
            bonus = (20000 / time) * 35;  // скопировано со слитых исходников Windows 2000, никому не говорите, пожалуйста
        else
            bonus = 0;
        synchronized (this) {
            score += bonus;
        }
        stopTimer();
        state = WIN;
        winCanvas.drawColor(Color.parseColor("#00A857"));  // зелёный
        for(int i = 7; i < 12; i++){  // рисуем Deck и 4 SuitStack'а
            elements.get(i).onDraw(winCanvas, -dst.left + elements.get(i).x, -dst.top + elements.get(i).y);  // рисуем учитывая положение winBitmap'а в окне
        }
        curStack = 0;
        WindowsView.handler.post(winRunnable);
    }

    private void startTimer(){
        time = 0;
        if(timedGame)
            scheduledFuture = scheduler.scheduleAtFixedRate(timerRunnable, 1, 1, TimeUnit.SECONDS);
        //WindowsView.handler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer(){
        if(scheduledFuture != null)
            scheduledFuture.cancel(false);
        //WindowsView.handler.removeCallbacks(timerRunnable);
    }

    private void askDealAgain(){  // создаёт messagebox с вопросом
        state = DEAL_AGAIN;
        WindowsView.handler.removeCallbacks(winRunnable);
        for(int i=0; i<12; i++)
            elements.get(i).visible = false;
        new MessageBox("Solitaire", "Deal Again?", MessageBox.YESNO, MessageBox.WARNING, new MessageBox.MsgResultListener() {
            @Override
            public void onMsgResult(int buttonNumber) {
                if(buttonNumber == YES)
                    reset();
            }
        }, this);
    }

    private void reset(){
        WindowsView.handler.removeCallbacks(winRunnable);
        stopTimer();
        for(int i=0; i<12; i++)
            elements.get(i).visible = true;
        time = 0;
        score = 0;
        curStack = 0;
        curCard = null;
        setupCards();
        state = WAIT_FOR_CLICK;
    }

    @Override
    public void prepareForDelete() {
        super.prepareForDelete();
        stopTimer();
        WindowsView.handler.removeCallbacks(winRunnable);
    }

    // =================================================
    // ================ Классы для карт ================
    // =================================================

    public static class RowStack extends CardStack {
        RowStack(){
            super(true, true);
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            int cur_x = 0, cur_y = 0;
            for(Element el : elements){
                Card card = (Card) el;
                card.x = cur_x;
                card.y = cur_y;
                if(card.closed)
                    cur_y += 3;
                else
                    cur_y += 15;
            }
            drawElements(canvas, x, y);
        }

        @Override
        boolean acceptCards(List<Card> cards) {
            if(elements.isEmpty())
                return cards.get(0).number == Card.K;
            if(top().closed)
                return false;
            return top().number == cards.get(0).number + 1 && !top().sameColorSuit(cards.get(0));
        }
    }

    public static class SuitStack extends CardStack {
        private Bitmap bmp;
        public SuitStack(Bitmap emptySuitStack){
            super(true, false);
            bmp = emptySuitStack;
        }
        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            for(Element el : elements)
                el.x = el.y = 0;  // один на другом, без смещения
            canvas.drawBitmap(bmp, x, y, null);
            drawElements(canvas, x, y);
        }

        @Override
        boolean acceptCards(List<Card> cards) {
            if(cards.size() != 1)
                return false;
            Card firstCard = cards.get(0);
            if(elements.isEmpty())
                return firstCard.number == Card.A;
            if(top().suit != firstCard.suit)  // должны быть карты одинаковой масти
                return false;
            return firstCard.number == top().number + 1;
        }
    }

    public class Deck extends CardStack {
        Bitmap closed, greenCircle;
        List<Card> deckCards = new ArrayList<>();  // карты, которые лежат в доп. колоде, но ещё не выложены на стол
        int timesGoThrough = 0; // сколько раз мы нажимали на зелёный кружочек
        int cardX = getCardDx();  // координата 3 карт, которые мы показываем

        public Deck(Bitmap[] bmpArray){
            super(true, false);
            closed = bmpArray[52];
            greenCircle = bmpArray[53];
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            Bitmap deckBmp;
            if(deckCards.isEmpty())
                deckBmp = greenCircle;
            else
                deckBmp = closed;
            if(deckBmp == closed) {  // может рисоваться от одной до 3 закрытых карт, символизирующих размер Deck'а
                int countClosedCards = Math.round(deckCards.size() / 8.0f);  // всего карт 24, поэтому делим на 8
                if(countClosedCards == 0)
                    countClosedCards = 1;
                int cur_x = x, cur_y = y;
                for(int i = 0; i < countClosedCards; i++){
                    canvas.drawBitmap(deckBmp, cur_x, cur_y, null);
                    cur_x += 2;
                    cur_y++;
                }
            }
            else
                canvas.drawBitmap(deckBmp, x, y, null);
            drawElements(canvas, x, y);
        }

        @Override
        public boolean onSelfMouseOver(int x, int y, boolean touch) {
            if(!(0 <= x && x < 71 && 0 <= y && y < 96))  // не на левую карту
                return false;
            if(!touch)
                return true;
            if(deckCards.isEmpty()){  // карты кончились - собираем обратно
                if(elements.size() <= drawAmount)  // но если их 3 или меньше - не собираем
                    return true;
                for(int i=elements.size()-1; i>=0; i--){  // в обратном порядке, так как мы брали карты сверху
                    deckCards.add((Card) elements.get(i));
                }
                elements.clear();
                timesGoThrough++;
                if(timesGoThrough > 3) {
                    synchronized (Solitaire.this) {
                        score -= 20;
                        if (score < 0)
                            score = 0;
                    }
                }
            }
            else
                drawThree();
            return true;
        }

        private void drawThree(){
            for(Element e : elements){  // сдвигаем уже лежащие карты влево
                e.x = cardX;
                e.y = 0;
            }
            int cur_x = cardX, cur_y = 0;
            for(int i = 0; i < ((Solitaire) parent).drawAmount; i++){
                if(deckCards.isEmpty())
                    break;
                Card topCard = deckCards.remove(deckCards.size() - 1);
                topCard.closed = false;
                topCard.x = cur_x;
                topCard.y = cur_y;
                elements.add(topCard);
                cur_x += 14;
                cur_y++;
            }
        }

        @Override
        public void onWindowResize(boolean maximized) {
            super.onWindowResize(maximized);
            int newCardMargin = getCardDx();
            if(cardX != newCardMargin){
                for(Element card : elements){
                    card.x += newCardMargin - cardX;
                }
                cardX = newCardMargin;
            }
        }

        @Override
        boolean acceptCards(List<Card> cards) {  // карты класть нельзя. но обратно вернуть можно
            return false;
        }
    }

    private class OptionsWindow extends DialogWindow {
        private static final String drawThreeString = "solitaireDrawThree",
                timedGameString = "solitaireTimedGame";
        RadioButton drawOne, drawThree;
        CheckBox timedGame;

        public OptionsWindow(){
            super("Options", 342, 217, Solitaire.this);
            SharedPreferences sharedPreferences = getSharedPreferences();
            boolean drawThreeBoolean = sharedPreferences.getBoolean(drawThreeString, true);

            drawOne = new RadioButton("Draw one");
            addElement(drawOne, 34, 62);
            drawThree = new RadioButton("Draw three");
            addElement(drawThree, 34, 91);
            RadioButton.createGroup(drawOne, drawThree);
            drawOne.active = !drawThreeBoolean;
            drawThree.active = drawThreeBoolean;

            RadioButton standard = new RadioButton("Standard");
            addElement(standard, 196, 56);
            RadioButton vegas = new RadioButton("Vegas");
            addElement(vegas, 196, 77);
            RadioButton none = new RadioButton("None");
            addElement(none, 196, 98);
            RadioButton.createGroup(standard, vegas, none);
            standard.active = true;

            timedGame = new CheckBox("Timed game");
            timedGame.checked = sharedPreferences.getBoolean(timedGameString, true);
            addElement(timedGame, 15, 130);
            CheckBox statusBar = new CheckBox("Status bar");
            statusBar.checked = true;
            addElement(statusBar, 15, 151);
            addElement(new CheckBox("Outline dragging"), 177, 130);
            addElement(new CheckBox("Keep score"), 177, 151);

            Button ok = new Button("OK", new Rect(171, 178, 246, 201), parent -> apply());
            ok.coolActive = true;
            defaultButton = ok;
            addElement(ok);
            addCloseButton(new Rect(252, 178, 327, 201), "Cancel");
            centerInParent();
        }

        private void apply(){
            // если настройки изменились, то перезапускаем игру
            close();
            SharedPreferences sharedPreferences = getSharedPreferences();
            boolean changed = (drawThree.active != sharedPreferences.getBoolean(drawThreeString, true))
                    || (timedGame.checked != sharedPreferences.getBoolean(timedGameString, true));
            if(!changed)
                return;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(drawThreeString, drawThree.active);
            editor.putBoolean(timedGameString, timedGame.checked);
            editor.apply();
            drawAmount = drawThree.active? 3 : 1;
            Solitaire.this.timedGame = timedGame.checked;
            reset();
        }

        @Override
        public void onNewDraw(Canvas canvas, int x, int y) {
            super.onNewDraw(canvas, x, y);
            drawGroupFrame(canvas, x + 15, y + 41, x + 165, y + 120, "Draw");
            drawGroupFrame(canvas, x + 177, y + 41, x + 327, y + 120, "Scoring");
        }

        private void drawGroupFrame(Canvas canvas, int x1, int y1, int x2, int y2, String text){
            p.setColor(Color.WHITE);
            canvas.drawRect(x1 + 1, y1 + 1, x2 - 2, y1 + 2, p);
            canvas.drawRect(x1 + 1, y1 + 1, x1 + 2, y2 - 2, p);
            canvas.drawRect(x1, y2 - 1, x2, y2, p);
            canvas.drawRect(x2 - 1, y1, x2, y2, p);
            p.setColor(Color.parseColor("#87888F"));  // тёмно-серый
            canvas.drawRect(x1, y1, x1 + 1, y2 - 1, p);
            canvas.drawRect(x1, y1, x2 - 1, y1 + 1, p);
            canvas.drawRect(x1, y2 - 2, x2 - 1, y2 - 1, p);
            canvas.drawRect(x2 - 2, y1, x2 - 1, y2 - 1, p);
            // текст
            int width = (int) p.measureText(text);
            p.setColor(Color.parseColor("#C0C7C8"));  // серый
            canvas.drawRect(x1 + 7, y1, x1 + width + 11, y1 + 2, p);
            p.setColor(Color.BLACK);
            canvas.drawText(text, x1 + 10, y1 + 5, p);
        }
    }
}
