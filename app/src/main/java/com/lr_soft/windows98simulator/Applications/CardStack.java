package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;

import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.ElementContainer;

import java.util.List;
import java.util.Scanner;

public abstract class CardStack extends ElementContainer {
    private boolean mayTakeTop;  // можно взять только верхнюю карту
    private boolean mayTakeSeveral;  // можно взять несколько карт сверху

    public CardStack(boolean mayTakeTop, boolean mayTakeSeveral) {
        this.mayTakeTop = mayTakeTop;
        this.mayTakeSeveral = mayTakeSeveral;
    }

    public CardStack(){
        mayTakeTop = mayTakeSeveral = false;
    }

    abstract boolean acceptCards(List<Card> cards);

    Card top(){
        return (Card) elements.get(elements.size() - 1);
    }

    Card cardFromTop(int index){
        return (Card) elements.get(elements.size() - 1 - index);
    }

    Card pop(){
        return (Card) elements.remove(elements.size() - 1);
    }

    boolean mayTakeCards(Card start){
        if(start.closed)
            return false;
        return mayTakeSeveral || (start == top() && mayTakeTop);
    }

    int getCardDy(){
        return 15;
    }

    int getCenterX(){  // центр стопки - чтобы определять, какая ближе к перемещаемой карте
        return x + 36;   // относительно окна!!! (чтобы можно было сравнивать)
    }

    int getCenterY(){
        int ret = y + 48;
        if(!elements.isEmpty())
            ret += top().y;
        return ret;
    }

    boolean intersectsWithCard(int left, int top){  // относительно нашего верхнего угла
        if(elements.isEmpty())
            return left >= -71 && left < 71 && top >= -96 && top < 96;
        else
            return top().intersectsWithCard(left - top().x, top - top().y);
    }

    // сохранение
    void save(List<Integer> stream){
        stream.add(elements.size());
        for(Element card : elements)
            ((Card) card).save(stream);
    }

    void load(Scanner scanner, Bitmap[] cardBitmaps, int[] pixels){
        int size = scanner.nextInt();
        elements.clear();
        for(int i = 0; i < size; i++)
            elements.add(new Card(scanner, cardBitmaps, pixels));
    }
}
