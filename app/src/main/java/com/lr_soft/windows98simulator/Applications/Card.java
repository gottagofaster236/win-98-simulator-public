package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.System.Element;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Card extends Element {
    // 0, 1, 2, ..., 8, 9, 10, 11, 12
    // Туз, 2, 3, 4, ..., 10, Валет, Дама, Король
    int number, suit;
    static final int K = 12, A = 0;
    private Bitmap bmp, closedBmp, invertedBmp;
    private Rect src = new Rect(), dst = new Rect();
    boolean closed = true;
    boolean inverted = false;
    private int[] pixels;
    private long selfLastClickTime = -1;
    private int[][] last_touches = {{-100, -100}, {-100, -100}};  // координаты последних 2 нажатий (по порядку). Для double click

    Card(int number, int suit, Bitmap[] cardBitmaps){
        this.number = number;
        this.suit = suit;
        int index = suit * 13 + number;
        bmp = cardBitmaps[index];
        closedBmp = cardBitmaps[52];
        width = bmp.getWidth();
        height = bmp.getHeight();
    }

    Card(int index, Bitmap[] cardBitmaps, int[] pixels){  // для Spider
        this(index % 13, index / 13, cardBitmaps);
        this.pixels = pixels;
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        canvas.drawBitmap(getDrawingBmp(), x, y, null);
    }

    public void onDraw(Canvas canvas, int x, int y, Rect limits){  // координаты абсолютные
        drawBitmap(canvas, getDrawingBmp(), x, y, src, dst, limits);
    }

    private Bitmap getDrawingBmp(){
        if(closed)
            return closedBmp;
        else if(!inverted)
            return bmp;
        else
            return getInvertedBmp();
    }

    public static void drawBitmap(Canvas canvas, Bitmap bmp, int x, int y, Rect src, Rect dst, Rect limits){  // координаты абсолютные
        // limits - границы, за которые bitmap не должен вылезать. Используется, когда мы перемещаем карту
        src.left = 0;
        src.top = 0;
        src.right = bmp.getWidth();
        src.bottom = bmp.getHeight();
        dst.left = x;
        dst.top = y;
        dst.right = x + bmp.getWidth();
        dst.bottom = y + bmp.getHeight();
        if(dst.left < limits.left){
            src.left += limits.left - dst.left;
            dst.left = limits.left;
        }
        if(dst.top < limits.top){
            src.top += limits.top - dst.top;
            dst.top = limits.top;
        }
        if(dst.right > limits.right){
            src.right -= dst.right - limits.right;
            dst.right = limits.right;
        }
        if(dst.bottom > limits.bottom){
            src.bottom -= dst.bottom - limits.bottom;
            dst.bottom = limits.bottom;
        }
        if(!dst.isEmpty()){
            canvas.drawBitmap(bmp, src, dst, null);
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        Bitmap bmp = closed? closedBmp : this.bmp;
        if(!(0 <= x && x < bmp.getWidth() && 0 <= y && y < bmp.getHeight()))
            return false;
        if(touch){
            CardStack parentStack = (CardStack) parent;
            boolean top = (this == parentStack.top());
            if(closed && parent.parent instanceof Solitaire){
                if(top) {
                    closed = false;
                    synchronized (parent.parent) {
                        ((Solitaire) parent.parent).score += 5;
                    }
                }
            }
            else{  // открытая - пытаемся начать перемещать
                BaseSolitaire parentWindow = (BaseSolitaire) parent.parent;
                if(parentStack.mayTakeCards(this)){
                    int index = parentStack.elements.indexOf(this);
                    parentWindow.movingCards.clear();
                    parentWindow.returnCardsTo = parentStack;
                    parentWindow.movingCardsDy = parentStack.getCardDy();
                    parentWindow.cardShiftX = -x;
                    parentWindow.cardShiftY = -y;
                    int cardsCount = parentStack.elements.size() - index;
                    for(int i = 0; i < cardsCount; i++){
                        Card movingCard = (Card) parentStack.elements.remove(index);
                        parentWindow.movingCards.add(movingCard);
                    }
                    parentWindow.playCardTakenSound();
                }
            }
            int[] temp = last_touches[0];
            last_touches[0] = last_touches[1];
            last_touches[1] = temp;
            last_touches[1][0] = getCursorX();
            last_touches[1][1] = getCursorY();
        }
        return true;
    }

    public void onCardClick() {  // свой метод, потому что из-за удаления карты из ElementContainer мы не можем сделать onClick (из-за startTouch)
        int cursor_x = getCursorX(), cursor_y = getCursorY();
        if(selfLastClickTime != -1 && System.currentTimeMillis() - selfLastClickTime <= 500) {
            if((last_touches[0][0] - cursor_x) * (last_touches[0][0] - cursor_x) + (last_touches[0][1] - cursor_y) * (last_touches[0][1] - cursor_y) <= 4 * 4){  // подвинули курсор менее, чем на 4 пикселя
                doubleClickAction();
            }
        }
        selfLastClickTime = System.currentTimeMillis();
    }

    public void doubleClickAction() {  // по двойному клику карта пытается переместиться в одну из верхних стопок
        // так как карта уходит из CardStack после нажатия, она не получает onDoubleClick
        if(!(parent.parent instanceof Solitaire))
            return;
        CardStack parentStack = (CardStack) parent;
        Solitaire parentWindow = (Solitaire) parent.parent;
        boolean top = parentStack.top() == this;
        if(parentStack instanceof Solitaire.SuitStack)  // нет смысла, уже в верхней стопке
            return;
        if(top && parentStack.mayTakeCards(this)){
            parentStack.elements.remove(this);
            List<Card> movingCards = Collections.singletonList(this);

            for(int i = 8; i < 12; i++){
                Solitaire.SuitStack suitStack = (Solitaire.SuitStack) parentWindow.elements.get(i);
                if(suitStack.acceptCards(movingCards)) {
                    suitStack.elements.addAll(movingCards);
                    parentStack.startTouch = null; // чтобы верхняя карта не среагировала на предстоящий клик
                    synchronized (parentWindow) {
                        parentWindow.score += 10;
                    }
                    parentWindow.checkForWin();
                    return;
                }
            }
            // если дошли досюда, то не нашли подходяющую стопку
            parentStack.elements.addAll(movingCards);
        }
    }

    boolean intersectsWithCard(int left, int top){  // относительно нашего верхнего угла
        return left >= -71 && left < 71 && top >= -96 && top < 96;
    }

    boolean sameColorSuit(Card otherCard){
        boolean weBlack = (suit == 0 || suit == 3);
        boolean otherBlack = (otherCard.suit == 0 || otherCard.suit == 3);
        return weBlack == otherBlack;
    }

    private boolean red(){
        return suit == 1 || suit == 2;
    }

    private Bitmap getInvertedBmp(){  // lazy инициализация
        if(invertedBmp == null)
            invertedBmp = FreeCell.createInvertedBitmap(bmp, number, red(), pixels);
        return invertedBmp;
    }

    // сохранение
    void save(List<Integer> stream){
        int index = suit * 13 + number;
        stream.add(index);
        stream.add(closed? 1 : 0);
    }

    Card(Scanner scanner, Bitmap[] cardBitmaps, int[] pixels){
        this(scanner.nextInt(), cardBitmaps, pixels);
        closed = (scanner.nextInt() == 1);
    }
}
