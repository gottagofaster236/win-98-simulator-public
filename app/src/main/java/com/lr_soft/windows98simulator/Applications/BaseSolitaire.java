package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.Window;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSolitaire extends Window {
    Rect fieldRect = new Rect(), limitsRect = new Rect();  // чтобы рисовать карту без вылезаний
    int cardShiftX, cardShiftY;  // сдвиг левого верхнего угла перемещаемой карты относительно курсора
    List<Card> movingCards = new ArrayList<>();  // карты, которые мы сейчас перемещаем. Если пустой, то не перемещаем
    int movingCardsDy;
    CardStack returnCardsTo;  // откуда взяли карты
    static final int cardWidth = 71, cardHeight = 96;

    final static int[] cardBitmapsIds = {R.drawable.c1, R.drawable.c2, R.drawable.c3, R.drawable.c4, R.drawable.c5, R.drawable.c6, R.drawable.c7, R.drawable.c8, R.drawable.c9, R.drawable.c10,
            R.drawable.c11, R.drawable.c12, R.drawable.c13, R.drawable.c14, R.drawable.c15, R.drawable.c16, R.drawable.c17, R.drawable.c18, R.drawable.c19, R.drawable.c20,
            R.drawable.c21, R.drawable.c22, R.drawable.c23, R.drawable.c24, R.drawable.c25, R.drawable.c26, R.drawable.c27, R.drawable.c28, R.drawable.c29, R.drawable.c30,
            R.drawable.c31, R.drawable.c32, R.drawable.c33, R.drawable.c34, R.drawable.c35, R.drawable.c36, R.drawable.c37, R.drawable.c38, R.drawable.c39, R.drawable.c40,
            R.drawable.c41, R.drawable.c42, R.drawable.c43, R.drawable.c44, R.drawable.c45, R.drawable.c46, R.drawable.c47, R.drawable.c48, R.drawable.c49, R.drawable.c50,
            R.drawable.c51, R.drawable.c52, R.drawable.c53, R.drawable.c54, R.drawable.c55};

    BaseSolitaire(String title, Bitmap icon, int minimized_width, int minimized_height, boolean borderThick, boolean createTopMenu, boolean isMessageBox) {
        super(title, icon, minimized_width, minimized_height, borderThick, createTopMenu, isMessageBox);
    }

    void drawCards(Canvas canvas, int x, int y, boolean drawMovingCards){
        // рисуем стеки
        for (Element el : elements) {
            if (!el.visible)
                continue;
            if (el == topMenu)  // т. к. рисуется в super()
                continue;
            el.parent = this;
            el.onDraw(canvas, x + el.x, y + el.y);
        }
        if(drawMovingCards)
            drawMovingCards(canvas, x, y);
    }

    void drawMovingCards(Canvas canvas, int x, int y){
        if(!movingCards.isEmpty()){  // если двигаем карту
            int cur_x = getCursorX() + cardShiftX;
            int cur_y = getCursorY() + cardShiftY;
            for(Card card : movingCards){
                card.onDraw(canvas, cur_x, cur_y, limitsRect);  // не меняем x и y, это нужно
                cur_y += movingCardsDy;
            }
        }
    }

    void onSolitaireClick(int x, int y, boolean callSuperOnClick) {  // не onSelfClick, потому что CardStack может забрать себе onMouseOver (и клика не будет)
        if(!movingCards.isEmpty()){
            int card_x = x + cardShiftX, card_y = y + cardShiftY;
            int card_cx = card_x + 36, card_cy = card_y + 48;  // центр движущейся карты (или нескольких карт)
            int min_dist = (int) 1e9;
            CardStack bestCardStack = null;
            for(int i = 0; i < elements.size(); i++){
                if(!(elements.get(i) instanceof CardStack))
                    continue;
                CardStack cardStack = (CardStack) elements.get(i);
                if(cardStack.acceptCards(movingCards)){
                    if(!cardStack.intersectsWithCard(card_x - cardStack.x, card_y - cardStack.y))
                        continue;
                    int dist_x = cardStack.getCenterX() - card_cx;
                    int dist_y = cardStack.getCenterY() - card_cy;
                    int dist = dist_x * dist_x + dist_y * dist_y;
                    if(dist < min_dist){
                        min_dist = dist;
                        bestCardStack = cardStack;
                    }
                }
            }
            if(bestCardStack == null)
                returnCardsTo.elements.addAll(movingCards);
            else
                bestCardStack.elements.addAll(movingCards);
            movingCards.get(0).onCardClick();
            movingCards.clear();

            if(bestCardStack != null){
                CardStack from = returnCardsTo, to = bestCardStack;
                if(from != to)
                    onCardMoved(from, to);
            }
            playCardReleasedSound();
        }
        if(callSuperOnClick)
            super.onClick(x, y);
    }

    @Override
    public void onClick(int x, int y) {
        onSolitaireClick(x, y, true);
    }

    @Override
    public void onDoubleClick(int x, int y) {
        //Log.d(TAG, "on solitaire double click");
        onSolitaireClick(x, y, false);
        super.onDoubleClick(x, y);
    }

    @Override
    public void onSelfMouseLeave() {
        super.onSelfMouseLeave();
        if(!movingCards.isEmpty()){
            returnCardsTo.elements.addAll(movingCards);
            movingCards.clear();
        }
    }

    @Override
    public void onSelfOtherTouch() {
        super.onSelfOtherTouch();
        if(!movingCards.isEmpty()){
            returnCardsTo.elements.addAll(movingCards);
            movingCards.clear();
        }
    }

    abstract void onCardMoved(CardStack from, CardStack to);

    void playCardTakenSound() {}
    void playCardReleasedSound() {}

    int getCardDx(int cardsAmount){
        return (fieldRect.width() - cardsAmount * cardWidth) / (cardsAmount + 1) + cardWidth;
    }
}
