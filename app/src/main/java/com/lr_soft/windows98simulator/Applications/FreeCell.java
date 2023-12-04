package com.lr_soft.windows98simulator.Applications;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.AboutWindow;
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.CheckBox;
import com.lr_soft.windows98simulator.System.Cursor;
import com.lr_soft.windows98simulator.System.DialogWindow;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.ElementContainer;
import com.lr_soft.windows98simulator.System.HelpTopics;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TextBox;
import com.lr_soft.windows98simulator.System.TopMenu;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FreeCell extends Window {
    // testing: https://www.youtube.com/watch?v=f0_z0yJ5__o, https://freecellgamesolutions.com/play/?g=25904&v=All
    private CardStack selectedStack = null;
    private Cursor acceptCursor, acceptEmptyCursor;  // курсор, если можно положить карту на другую карту, либо на пустое место

    private int gameNumber;
    private Random random = new Random();
    private int holdrand = 1;

    private int rand(){
        return ((holdrand = holdrand * 214013 + 2531011) >> 16) & 0x7fff;
    }

    private void srand(int seed){
        holdrand = seed;
    }

    private Card[] allCards = new Card[52];
    private Bitmap headLeft, headRight, winBmp;
    private boolean headIsLeft = false;
    private static final int EMPTY = 0, PLAYING = 1, WIN = 2, LOST = 3;
    private int state = EMPTY;
    private String cardsLeftString = null;
    private Rect src = new Rect(), dst = new Rect(), field = new Rect();

    public FreeCell(){
        super("FreeCell", getBmp(R.drawable.freecell), 640, 452, true, false, false);
        acceptCursor = new Cursor(getBmp(R.drawable.freecell_accept_cursor), 7, 25);
        acceptEmptyCursor = new Cursor(getBmp(R.drawable.freecell_accept_empty), 4, 0);
        headLeft = getBmp(R.drawable.freecell_head_left);
        headRight = getBmp(R.drawable.freecell_head_right);
        winBmp = getBmp(R.drawable.freecell_win);
        winBmp = Bitmap.createScaledBitmap(winBmp, winBmp.getWidth() * 10, winBmp.getHeight() * 10, false);

        Bitmap[] cardBitmaps = new Bitmap[52];
        for(int i = 0; i < 52; i++)
            cardBitmaps[i] = getBmp(BaseSolitaire.cardBitmapsIds[i]);
        int[] tmp = new int[71 * 96];
        for(int i = 0; i < 13; i++){
            for(int j = 0; j < 4; j++){
                allCards[i * 4 + j] = new Card(i, j, cardBitmaps, tmp);
            }
        }
        // элементы от 0 до 7 - cardStack
        // от 8 до 11 - freeCell
        // от 12 до 15 - homeCell
        for(int i = 0; i < 8; i++)
            addElement(new ColumnStack(), 11 + i * 78, 148);
        for(int i = 0; i < 4; i++)
            addElement(new FreeCellStack(), 4 + i * 71, 42);
        for(int i = 0; i < 4; i++)
            addElement(new HomeCell(), 352 + i * 71, 42);
        drawElements = false;  // т. к. green background

        // TOP MENU

        ButtonList game = new ButtonList();
        game.elements.add(new ButtonInList("New Game", "F2", parent -> confirmGameQuit(() -> dealCards(true))));
        game.elements.add(new ButtonInList("Select Game...", "F3", parent -> confirmGameQuit(SelectGameDialog::new)));
        game.elements.add(new ButtonInList("Restart Game", parent -> confirmGameQuit(() -> dealCards(false))));
        game.elements.add(new Separator());
        game.elements.add(new ButtonInList("Statistics...", "F4"));
        game.elements.add(new ButtonInList("Options...", "F5"));
        game.elements.add(new Separator());
        ButtonInList undo = new ButtonInList("Undo", "F10");
        undo.disabled = true;
        game.elements.add(new ButtonInList("Exit", parent -> close()));

        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("Help Topics");
        helpTopics.action = parent -> new HelpTopics("Free Cell Help", false,
                new int[]{R.drawable.freecell1, R.drawable.freecell2, R.drawable.freecell3});
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("About FreeCell");
        about.action = parent -> new AboutWindow(FreeCell.this, "FreeCell", getBmp(R.drawable.game_freecell_2), "by Jim Horne");
        help.elements.add(about);
        TopMenu topMenu = new MyTopMenu();
        topMenu.elements.add(new TopMenuButton("Game", game));
        topMenu.elements.add(new TopMenuButton("Help", help));
        setTopMenu(topMenu);
        repositionEverything(false);
        centerWindowHorizontally();
        // Tutorial
        SharedPreferences sharedPreferences = getSharedPreferences();
        final String key = "freecellTutorialShowedTimes";
        int showedTimes = sharedPreferences.getInt(key, 0);
        if (showedTimes < 6) {  // 6 раз показываем
            makeSnackbar(R.string.freecell_tutorial, Snackbar.LENGTH_LONG);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(key, showedTimes + 1);
            editor.apply();
        }
    }

    // (c) http://www.solitairelaboratory.com/mshuffle.txt
    private void dealCards(boolean updateGameNumber){
        if(updateGameNumber)
            setGameNumber(randomGameNumber());
        for (int i = 0; i < 16; i++)  // clear deck
            ((CardStack) elements.get(i)).elements.clear();
        selectedStack = null;

        // shuffle cards
        Card[] deck = Arrays.copyOf(allCards, allCards.length);

        srand(gameNumber);  // gamenumber is seed for rand()
        int wLeft = 52;  // cards left to be chosen in shuffle
        for (int i = 0; i < 52; i++) {
            int j = rand() % wLeft;
            ((CardStack) elements.get(i % 8)).elements.add(deck[j]);
            deck[j] = deck[--wLeft];
        }

        state = PLAYING;
        updateState();
    }

    private int randomGameNumber(){
        return random.nextInt(1000000) + 1;
    }

    private void setGameNumber(int gameNumber){
        this.gameNumber = gameNumber;
        setTitle("FreeCell Game #" + gameNumber);
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        p.setColor(Color.rgb(0, 168, 87));  // зеленый background
        field.set(x + 4, y + 42, x + width - 4, y + height - 4);
        canvas.drawRect(field, p);
        if(state == WIN){
            com.lr_soft.windows98simulator.Applications.Card
                    .drawBitmap(canvas, winBmp, x + 14, y + 148, src, dst, field);
        }
        for (Element el : elements) {  // рисуем элементы, т. к. drawElements = false
            if (!el.visible)
                continue;
            if (el == topMenu)  // т. к. рисуется в super()
                continue;
            el.parent = this;
            el.onDraw(canvas, x + el.x, y + el.y);
        }
        canvas.drawBitmap(headIsLeft? headLeft : headRight, x + 301, y + 60, p);
        if(cardsLeftString != null) {
            p.setColor(Color.BLACK);
            canvas.drawText(cardsLeftString, x + 567, y + 37, p);
        }
    }

    private void makeAutoMoves(){  // переместить тузы (и т. д.) в HomeCell
        while(true){
            boolean updated = false;
            for(int i = 0; i < 8 + 4; i++){  // ColumnStack и FreeCell
                CardStack cardStack = (CardStack) elements.get(i);
                for(int j = 12; j < 16; j++){  // HomeCell
                    if(cardStack.elements.isEmpty())
                        break;
                    HomeCell homeCell = (HomeCell) elements.get(j);
                    if(homeCell.acceptCard(cardStack.top()) && isUnneeded(cardStack.top())){
                        homeCell.elements.add(cardStack.pop());
                        updated = true;
                    }
                }
            }
            if(!updated)
                break;
        }
        updateState();
    }

    private boolean isUnneeded(Card card){
        // карта не нужна, когда в игровой зоне не осталось карт меньшего ранга противоположного цвета (см. Help Topics)
        for(int i = 0; i < 8 + 4; i++){
            CardStack cardStack = (CardStack) elements.get(i);
            for(Element e : cardStack.elements){
                Card otherCard = (Card) e;
                if(!otherCard.sameColorSuit(card) && otherCard.number != Card.A && otherCard.number < card.number)
                    return false;
            }
        }
        return true;
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        if(!super.onSelfMouseOver(x, y, touch))
            return false;
        Windows98.setDefaultCursor();
        if(touch){
            selectedStack = null;
            makeAutoMoves();
        }
        return true;
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        boolean result = super.onMouseOver(x, y, touch);
        if(childMessagebox != null)
            Windows98.setDefaultCursor();
        return result;
    }

    private void updateState(){
        int cardsLeft = 52;
        for(int i = 12; i < 16; i++){
            cardsLeft -= ((HomeCell) elements.get(i)).elements.size();
        }
        if(state == PLAYING)
            cardsLeftString = "Cards Left: " + cardsLeft;
        else
            cardsLeftString = null;

        if(state != PLAYING)
            return;
        // проверяем на проигрыш и выигрыш
        if(cardsLeft == 0){
            state = WIN;
            cardsLeftString = null;
            new GameOverDialog(true);
        }
        else{
            // смотрим, есть ли возможные ходы
            CardStack oldSelectedStack = selectedStack;
            boolean existsMove = false;
            for(int i = 0; i < 12; i++){  // карту можно брать только из ColumnStack и FreeCell
                selectedStack = (CardStack) elements.get(i);
                if(selectedStack.elements.isEmpty())
                    continue;
                for(int j = 0; j < 16; j++){
                    if(((CardStack) elements.get(j)).acceptSelectedStack()){
                        existsMove = true;
                        break;
                    }
                }
            }
            selectedStack = oldSelectedStack;
            if(!existsMove){
                state = LOST;
                cardsLeftString = null;
                new GameOverDialog(false);
            }
        }
    }

    /*@Override
    public void makeActive() {  // по нам кликнули
        super.makeActive();
        if(elements.size() < 16)  // так как makeActive() вызывается из конструктора Window
            return;
        updateState();
    }*/

    @Override
    public void close(final boolean activateNextWindow) {
        confirmGameQuit(new Runnable() {
            @Override
            public void run() {
                FreeCell.super.close(activateNextWindow);
            }
        });
    }

    private void confirmGameQuit(final Runnable action){
        if(state == PLAYING)
            new MessageBox("FreeCell", "Are you sure you want to quit this game?", MessageBox.YESNO, MessageBox.QUESTION, new MessageBox.MsgResultListener() {
                @Override
                public void onMsgResult(int buttonNumber) {
                    if(buttonNumber == YES)
                        action.run();
                }
            }, this);
        else
            action.run();
    }

    @Override
    public void maximize() {
        super.maximize();
        width = 640 + border_width * 2;
        repositionEverything(false);
    }

    // =================================================
    // ================ Классы для карт ================
    // =================================================

    private class Card extends Element {  // copy-paste из Solitaire, но не совсем
        int number, suit;
        // 0, 1, 2, ..., 8, 9, 10, 11, 12
        // Туз, 2, 3, 4, ..., 10, Валет, Дама, Король
        private static final int A = 0, J = 10;

        private Bitmap bmp, inverted;
        private int[] pixels;

        public Card(int number, int suit, Bitmap[] cardBitmaps, int[] pixels){  // pixels - preallocated array
            this.number = number;
            this.suit = suit;
            int index = suit * 13 + number;
            bmp = cardBitmaps[index];
            width = bmp.getWidth();
            height = bmp.getHeight();
            this.pixels = pixels;
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            //Log.d(TAG, "sel stack, its top, we: " + selectedStack + " " + (selectedStack != null? selectedStack.top() : "npe") + " " + this);
            canvas.drawBitmap((parent == selectedStack && selectedStack.top() == this)?
                    getInvertedBmp() : bmp, x, y, null);
        }


        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {  // не используется
            return false;
            //return 0 <= x && x < width && 0 <= y && y < height;
        }

        private Bitmap getInvertedBmp(){  // lazy инициализация
            if(inverted == null)
                inverted = createInvertedBitmap(bmp, number, red(), pixels);
            return inverted;
        }

        boolean sameColorSuit(Card otherCard){
            return red() == otherCard.red();
        }

        boolean red(){
            return suit == 1 || suit == 2;
        }
    }

    static Bitmap createInvertedBitmap(Bitmap bmp, int number, boolean red, int[] pixels){
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                switch (pixels[i + width * j]){
                    case Color.BLACK:
                        pixels[i + width * j] = Color.WHITE;
                        break;
                    case Color.WHITE:
                        pixels[i + width * j] = (red|| number >= Card.J)? Color.BLACK : Color.rgb(87, 168, 168);
                        break;
                    case Color.RED:
                        pixels[i + width * j] = Color.rgb(87, 168, 168);
                        break;
                    case Color.YELLOW:
                        pixels[i + width * j] = Color.rgb(0, 0, 168);
                        break;
                    case Color.MAGENTA:
                        pixels[i + width * j] = red? Color.rgb(0, 168, 87) : Color.rgb(168, 0, 87); ;
                        break;
                    case Color.BLUE:
                        pixels[i + width * j] = Color.YELLOW;
                        break;
                    case Color.CYAN:
                        pixels[i + width * j] = Color.RED;
                        break;
                    case Color.GREEN:
                        pixels[i + width * j] = Color.rgb(168, 87, 168);
                        break;
                        /*case Color.TRANSPARENT:
                            break;
                        default:
                            throw new RuntimeException("r, g, b: " + Color.red(pixels[i + width * j]) + " " + Color.green(pixels[i + width * j]) + " " + Color.blue(pixels[i + width * j]));
                         */
                }
            }
        }
        Bitmap result = Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        result.setDensity(Bitmap.DENSITY_NONE);
        return result;
    }

    private abstract class CardStack extends ElementContainer {
        boolean canBeSelected = true;
        int drawShiftY;
        long lastTouchTime = -1;  // здесь вместо doubleClick используется doubleTouch
        boolean drawBackground = false;  // см использование

        CardStack(int drawShiftY, int height){
            this.drawShiftY = drawShiftY;
            this.width = 71;  // ширина карта
            this.height = height;
        }

        CardStack(){
            this(0, 96);  // высота карты
            drawBackground = true;
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            if(drawBackground){
                p.setColor(Color.BLACK);
                canvas.drawRect(x, y, x + width - 1, y + 1, p);
                canvas.drawRect(x, y, x + 1, y + height - 1, p);
                p.setColor(Color.GREEN);
                canvas.drawRect(x + width - 1, y + 1, x + width, y + height, p);
                canvas.drawRect(x + 1, y + height - 1, x + width, y + height, p);
            }
            int cur_y = 0;
            for(Element element : elements){
                element.y = cur_y;
                cur_y += drawShiftY;
            }
            super.onDraw(canvas, x, y);
        }

        Card top(){
            return (Card) elements.get(elements.size() - 1);
        }

        Card pop(){
            return (Card) elements.remove(elements.size() - 1);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(state != PLAYING && touch)
                return false;
            if(!onSelfMouseOver(x, y))
                return false;
            if(touch){
                // проверяем на doubleClick
                if(System.currentTimeMillis() - lastTouchTime <= 500){
                    if(onSelfDoubleClick())
                        return true;
                    lastTouchTime = -1;
                }
                else
                    lastTouchTime = System.currentTimeMillis();

                if(selectedStack == null){
                    if(elements.size() > 0 && canBeSelected)
                        selectedStack = this;
                }
                else {
                    if(acceptSelectedStack()){
                        moveCardsFromSelectedStack();
                    }
                    else if(selectedStack != this){
                        new MessageBox("FreeCell", "That move is not allowed.", MessageBox.OK, MessageBox.INFO, new MessageBox.MsgResultListener() {
                            @Override
                            public void onMsgResult(int buttonNumber) {
                                makeAutoMoves();
                                selectedStack = null;
                            }
                        }, FreeCell.this);
                    }
                    else {  // selectedStack = this
                        makeAutoMoves();
                        selectedStack = null;
                    }
                    Windows98.setDefaultCursor();
                }
            }
            else if(selectedStack != null){
                if(acceptSelectedStack())
                    Windows98.setCursor(elements.isEmpty()? acceptEmptyCursor : acceptCursor);
                else
                    Windows98.setDefaultCursor();
            }
            return true;
        }

        @Override
        public void onMouseLeave() {
            lastTouchTime = -1;
        }

        boolean onSelfMouseOver(int x, int y){
            return 0 <= x && x < width && 0 <= y && y < height;
        }

        boolean acceptCard(Card card){
            return false;
        }

        boolean acceptSelectedStack(){  // принимаем ли мы карты из выбранной стопки
            return selectedStack != this && acceptCard(selectedStack.top());
        }

        void moveCardsFromSelectedStack(){  // переместить карты из выбранной стопки в нашу
            elements.add(selectedStack.pop());
            selectedStack = null;
            makeAutoMoves();
        }

        boolean onSelfDoubleClick() {  // возвращает обработал ли onSelfDoubleClick нажатие
            return false;
        }
    }

    private class ColumnStack extends CardStack {  // 8 стопок карт лицом вверх
        ColumnStack(){
            super(18, 0);
        }

        @Override
        boolean acceptCard(Card card) {  // not used
            if(elements.isEmpty())
                return true;
            return isCorrect(top(), card);
        }

        boolean isCorrect(Card bottom, Card top){
            return bottom.number == top.number + 1 && !bottom.sameColorSuit(top);
        }

        private int getNumberOfCardsFromSelected(){  // мы можем переместить не одну карту, а несколько
            int maxAmount = 1;  // для этого нужны свободные ячейки
            for(int i = 8; i < 8 + 4; i++){
                if(((FreeCellStack) FreeCell.this.elements.get(i)).elements.isEmpty())
                    maxAmount++;
            }
            //noinspection unchecked
            List<Card> selectedCards = (List) selectedStack.elements;
            loop:
            for(int take = Math.min(maxAmount, selectedCards.size()); take >= 1; take--){  // take - сколько карт берём
                // проверяем, что карты чередуются
                for(int i = selectedCards.size() - 1; i > selectedCards.size() - take; i--){
                    if(!isCorrect(selectedCards.get(i - 1), selectedCards.get(i)))
                        continue loop;
                }
                // проверяем, что последняя карта подходит на наш top()
                if(acceptCard(selectedCards.get(selectedCards.size() - take))){
                    return take;
                }
            }
            return -1;
        }

        @Override
        boolean acceptSelectedStack() {
            return selectedStack != this && getNumberOfCardsFromSelected() != -1;
        }

        @Override
        void moveCardsFromSelectedStack() {
            int numberOfMoving = getNumberOfCardsFromSelected();
            if(elements.isEmpty() && numberOfMoving > 1)
                new MoveToEmptyColumnDialog(this);
            else
                moveCardsFromSelectedStack(numberOfMoving);
        }

        void moveCardsFromSelectedStack(int numberOfMoving){
            for(int i = selectedStack.elements.size() - numberOfMoving; i < selectedStack.elements.size(); i++)
                elements.add(selectedStack.elements.get(i));
            for(int i = 0; i < numberOfMoving; i++)
                selectedStack.elements.remove(selectedStack.elements.size() - 1);
            selectedStack = null;
            makeAutoMoves();
        }

        @Override
        public boolean onSelfMouseOver(int x, int y) {
            if(selectedStack != null && selectedStack != this)  // на нас перемещают карту - можно нажать и ниже
                height = FreeCell.this.height - 4 - this.y;
            else
                height = getRealHeight();
            return super.onSelfMouseOver(x, y);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(!super.onMouseOver(x, y, touch))
                return false;
            if(y >= getRealHeight() && !elements.isEmpty())
                Windows98.setDefaultCursor();
            return true;
        }

        private int getRealHeight(){  // т. е. нижний пиксель нижней карты, или 0, если карт нет
            if(elements.isEmpty())
                return 0;
            else
                return top().y + top().height;
        }

        @Override
        boolean onSelfDoubleClick() {
            Windows98.setDefaultCursor();
            selectedStack = null;
            if(!elements.isEmpty()){
                // от 8 до 11 - freeCell
                for(int i = 8; i < 12; i++){
                    FreeCellStack freeCell = (FreeCellStack) FreeCell.this.elements.get(i);
                    if(freeCell.elements.isEmpty()){
                        freeCell.elements.add(pop());
                        makeAutoMoves();
                        return true;
                    }
                }
            }
            return true;
        }
    }

    private class FreeCellStack extends CardStack {
        @Override
        boolean acceptCard(Card card) {
            return elements.isEmpty();
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(super.onMouseOver(x, y, touch)) {
                headIsLeft = true;
                return true;
            }
            else
                return false;
        }
    }

    private class HomeCell extends CardStack {
        HomeCell(){
            //super();
            canBeSelected = false;
        }
        @Override
        boolean acceptCard(Card card) {
            if(elements.isEmpty())
                return card.number == Card.A;
            return top().suit == card.suit && card.number == top().number + 1;
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(super.onMouseOver(x, y, touch)) {
                headIsLeft = false;
                if(Windows98.windows98.getCursor() == acceptCursor)  // почему-то так
                    Windows98.setCursor(acceptEmptyCursor);
                return true;
            }
            else
                return false;
        }
    }

    private class MoveToEmptyColumnDialog extends DialogWindow {
        MoveToEmptyColumnDialog(final ColumnStack dest){
            super("Move to Empty Column...", 231, 155, FreeCell.this);
            Button moveColumn = new Button("Move column", new Rect(48, 46, 183, 69), parent -> {
                close();
                dest.moveCardsFromSelectedStack(dest.getNumberOfCardsFromSelected());
            });
            defaultButton = moveColumn;
            moveColumn.coolActive = true;
            addElement(moveColumn);
            addElement(new Button("Move single card", new Rect(48, 79, 183, 102), parent -> {
                close();
                dest.moveCardsFromSelectedStack(1);
            }));
            addElement(new Button("Cancel", new Rect(86, 115, 146, 138), parent -> {
                close();
                selectedStack = null;
                makeAutoMoves();
            }));
            deleteCloseButton();
            centerInParent();
        }
    }

    private class SelectGameDialog extends DialogWindow {
        TextBox gameNumberEdit;

        SelectGameDialog(){
            super("Game Number", 186, 147, FreeCell.this);
            gameNumberEdit = new TextBox(new Rect(70, 76, 117, 92), 2, 12, p, new Rect(-1, -11, 0, 2));
            gameNumberEdit.drawBorder = true;
            gameNumberEdit.isNumeric = true;
            gameNumberEdit.enterRunnable = new Runnable() {
                @Override
                public void run() {
                    apply();
                }
            };
            gameNumberEdit.setText(String.valueOf(randomGameNumber()));
            gameNumberEdit.selectAll();
            inputFocus = gameNumberEdit;
            addElement(gameNumberEdit);

            Button ok = new Button("OK", new Rect(56, 110, 131, 133), parent -> apply());
            defaultButton = ok;
            ok.coolActive = true;
            addElement(ok);
            deleteCloseButton();
            centerInParent();
        }

        @Override
        public void onNewDraw(Canvas canvas, int x, int y) {
            super.onNewDraw(canvas, x, y);
            p.setColor(Color.BLACK);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Select a game number", x + width / 2, y + 42, p);
            canvas.drawText("from 1 to 1000000", x + width / 2, y + 56, p);
            p.setTextAlign(Paint.Align.LEFT);
        }

        private void apply(){
            int number = 0;
            if(!gameNumberEdit.text.isEmpty() && gameNumberEdit.text.length() < 9)
                number = Integer.parseInt(gameNumberEdit.text);
            if(1 <= number && number <= 1000000){
                close();
                setGameNumber(number);
                dealCards(false);
            }
            else{
                gameNumberEdit.setText("0");
                gameNumberEdit.selectAll();
                gameNumberEdit.setActive(true);
            }
        }
    }

    private class GameOverDialog extends DialogWindow {
        private boolean win;
        private CheckBox checkBox;
        GameOverDialog(final boolean win){
            super("Game Over", 209, win? 155 : 171, FreeCell.this);
            this.win = win;
            checkBox = new CheckBox(win? "Select game" : "Same game");
            checkBox.checked = true;
            addElement(checkBox, 26, win? 95 : 114);

            Button yes = new Button("Yes", new Rect(26, height - 35, 101, height - 12), parent -> {
                close();
                if(win) {
                    if(checkBox.checked)  // Select game
                        new SelectGameDialog();
                    else
                        dealCards(true);
                }
                else
                    dealCards(!checkBox.checked);  // Same game
            });
            defaultButton = yes;
            yes.coolActive = true;
            addElement(yes);

            Button no = new Button("No", new Rect(108, height - 35, 183, height - 12), parent -> close());
            addElement(no);
            centerInParent();
        }

        @Override
        public void onNewDraw(Canvas canvas, int x, int y) {
            super.onNewDraw(canvas, x, y);
            p.setColor(Color.BLACK);
            if(win){
                canvas.drawText("Congratulations, you win!", x + 27, y + 46, p);
                canvas.drawText("Do you want to play again?", x + 27, y + 72, p);
            }
            else{
                canvas.drawText("Sorry, you lose.", x + 27, y + 43, p);
                canvas.drawText("There are no more legal moves.", x + 27, y + 69, p);
                canvas.drawText("Do you want to play again?", x + 27, y + 82, p);
            }
        }
    }

    private class MyTopMenu extends TopMenu {
        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(super.onMouseOver(x, y, touch)){
                Windows98.setDefaultCursor();
                return true;
            }
            else
                return false;
        }
    }
}
