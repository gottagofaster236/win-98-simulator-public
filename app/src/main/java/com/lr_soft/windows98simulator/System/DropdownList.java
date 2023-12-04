package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

import com.lr_soft.windows98simulator.R;

public class DropdownList extends Element {
    protected Item[] items;
    private boolean active = false;  // рисуем себя синим после закрытия меню
    public boolean disabled = false;
    private boolean itemsVisible = false;
    public int selectedItem;  // selected - выбранная, active - та, на которую наведен курсор
    private int activeItem;
    private boolean downButtonPressed = false;  // кнопка справа от текста
    private Bitmap downButtonBmp, downButtonPressedBmp, downButtonDisabledBmp;
    protected boolean drawBackground;
    protected boolean drawTitle = true;

    public DropdownList(String[] items, int defaultButton, int width){
        this(stringsToItems(items), defaultButton, width, true);
    }

    public DropdownList(Item[] items, int defaultButton, int width, boolean drawBackground){
        this.items = items;
        selectedItem = defaultButton;
        this.width = width;
        height = items[0].getHeight(true) + 6;
        this.drawBackground = drawBackground;
        if(drawBackground) {
            downButtonBmp = getBmp(R.drawable.dropdown_button);
            downButtonPressedBmp = getBmp(R.drawable.dropdown_button_pressed);
            downButtonDisabledBmp = getBmp(R.drawable.dropdown_button_disabled);
            downButtonDisabledBmp = getBmp(R.drawable.dropdown_button_disabled);
        }
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        if(drawBackground)
            drawFrameRectActive(canvas, x, y, x + width, y + height);
        if(drawTitle)
            drawTitle(canvas, x, y);
        if(drawBackground)
            canvas.drawBitmap(downButtonPressed ? downButtonPressedBmp : (!disabled ? downButtonBmp : downButtonDisabledBmp),
                    x + width - 18, y + 2, null);

        if(itemsVisible){
            // рисуем фон
            int items_y = getItemsY(y);
            int items_height = getItemsHeight();
            p.setColor(Color.BLACK);
            canvas.drawRect(x, items_y, x + width, items_y + items_height, p);
            p.setColor(Color.WHITE);
            canvas.drawRect(x + 1, items_y + 1, x + width - 1, items_y + items_height - 1, p);

            drawButtons(canvas, x, y);
        }
    }

    private void drawTitle(Canvas canvas, int x, int y){  // нарисовать итоговый выбор
        if(drawBackground) {
            int backgroundColor = !disabled ? Color.WHITE : Color.rgb(192, 199, 200);
            p.setColor(backgroundColor);
            canvas.drawRect(x + 2, y + 2, x + width - 2, y + height - 2, p);
        }
        items[selectedItem].onDraw(canvas, x + 3, y + 3, width - 22, active, true, disabled);
    }

    private void drawButtons(Canvas canvas, int x, int y){
        int cur_y = getItemsY(y) + 1;
        for(int i = 0; i < items.length; i++, cur_y += getItemHeight()){
            boolean active = (i == activeItem);
            items[i].onDraw(canvas, x + 1, cur_y, width - 2, active, false, false);
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(disabled)
            return false;
        if(!(2 <= x && x < width - 2))
            return false;
        int absolute_y = getAbsoluteY();
        int items_y = getItemsY(absolute_y) - absolute_y;
        int items_height = getItemsHeight();
        if(0 <= y && y < height){  // нажатие на заголовок
            if(touch){
                toggleDropdownVisibility();
            }
            if(width - 18 <= x && x < width - 2){  // нажали на кнопку
                if(touch)
                    downButtonPressed = true;
            }
            else
                downButtonPressed = false;
            return true;
        }
        else if(itemsVisible && items_y <= y && y < items_y + items_height){
            int cur_y = items_y + 1;
            int item_height = getItemHeight();

            for(int i = 0; i < items.length; i++, cur_y += item_height){
                if(cur_y <= y && y < cur_y + item_height){
                    activeItem = i;
                    if(touch) {
                        if(selectedItem != i) {
                            int oldSelection = selectedItem;
                            selectedItem = i;
                            onSelectionChanged(oldSelection, selectedItem);
                        }
                        toggleDropdownVisibility();
                    }
                    break;
                }
            }
            return true;
        }
        else
            return false;
    }

    private void toggleDropdownVisibility(){
        if(itemsVisible){
            itemsVisible = false;
            active = true;
        }
        else{
            itemsVisible = true;
            activeItem = selectedItem;
            active = false;
        }
    }

    private int getItemsY(int drawY){
        int items_y = drawY + height;
        int items_height = getItemsHeight();
        if(items_y + items_height > Windows98.TASKBAR_Y)
            items_y = drawY - items_height;
        return items_y;
    }

    private int getItemsHeight(){
        return 2 + items.length * getItemHeight();
    }

    private int getItemHeight(){
        return items[0].getHeight(false);
    }

    @Override
    public void onOtherTouch() {
        if(itemsVisible)
            toggleDropdownVisibility();
        else
            active = false;
    }

    @Override
    public void onMouseLeave() {
        downButtonPressed = false;
    }

    @Override
    public void onClick(int x, int y) {
        downButtonPressed = false;
    }

    public Item getSelectedItem() {
        return items[selectedItem];
    }

    protected void onSelectionChanged(int oldSelection, int newSelection){}

    public interface Item {
        void onDraw(Canvas canvas, int x, int y, int width, boolean active, boolean isTitle, boolean disabled);
        int getHeight(boolean isTitle);
    }


    private static class TextItem implements Item {
        private String text;

        public TextItem(String text) {
            this.text = text;
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y, int width, boolean active, boolean isTitle, boolean disabled) {
            int textColor;
            if(active && !disabled){
                drawBlueRectWithYellowBorder(canvas, x, y, x + width, y + getHeight(isTitle), isTitle);
                textColor = Color.WHITE;
            }
            else if(disabled)
                textColor = Color.rgb(135, 136, 143);
            else
                textColor = Color.BLACK;
            p.setColor(textColor);
            canvas.drawText(text, x + 3, y + (isTitle? 12 : 11), p);
        }

        @Override
        public int getHeight(boolean isTitle) {
            return isTitle? 15 : 13;
        }
    }

    private static Item[] stringsToItems(String[] strings){
        Item[] items = new Item[strings.length];
        for(int i = 0; i < strings.length; i++)
            items[i] = new TextItem(strings[i]);
        return items;
    }
}
