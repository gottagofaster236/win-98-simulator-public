package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.WindowsView;

public class Window extends ElementContainer {
    private String title;
    private String shortTitle;  // так как title может не помещаться
    protected Bitmap icon;  // иконка, 16x16

    protected int normal_width, normal_height;  // размеры в неразвернутом состоянии
    private int maximized_width, maximized_height;  // размеры в развернутом состоянии
    protected int border_width;  // у калькулятора - 3; у эксплорера - 4

    public boolean maximized = false;
    public boolean active = true;
    public boolean closed = false;  // если окно закрылось с помощью close()
    public boolean shouldRedraw = true;  // если окно активно, то всегда true. Если неактивно, то надо устанавливать самостоятельно

    protected TopMenu topMenu;
    protected TopButtons topButtons;  // свернуть, развернуть, закрыть
    public Window childMessagebox = null;
    public Button defaultButton = null;  // кнопка, для которой мы делаем жирную обводку по умолчанию. Чаще всего это "OK".

    protected int x_old, y_old;  // x и y до развертывания на экран
    private boolean moving = false;
    private boolean startedMovement = false;  // возможно, мы кликнули по заголовку, но не подвинули курсор
    private int move_shiftX, move_shiftY;  // сдвиг левого верхнего угла относительно курсора
    protected boolean unableToMaximize = false;
    private boolean isMessageBox;
    Bitmap backupBitmap;  // битмап, на который записывается состояние окна в неактивном состоянии
    private Canvas backupCanvas;
    private Rect src = new Rect(), dst = new Rect();
    protected int pos = 0;  // позиция, куда вставляются новые элементы (elements.size() - pos). Нужно, чтобы элементы вроде TopMenu и TopButtons были сверху
    protected boolean drawElements = true;  // если потомок хочет рисовать элементы сам
    protected boolean fillWindow = true;  // если не надо заполнять окно серым цветом
    protected boolean repositionTopMenu = true;  // если не надо заново размещать topMenu. Надо в Explorer'е
    boolean ignoreOtherTouch = false;  // если мы проходимся циклом onMouseOver и на нашу кнопку нажали, то у нас вызывается onSelfOtherTouch (так как таскбар принял нажатие), который стоит проигнорировать

    public Window(String title, Bitmap icon, int normal_width, int normal_height, boolean borderThick, boolean createTopMenu, boolean isMessageBox) {
        // isMessageBox - значит, нет кнопки в панели задач, а также из верхних кнопок есть только "закрыть" (нет "развернуть" и "свернуть")
        //this.shortTitle = shortTitle;
        this.title = title;
        this.icon = icon;  // 16 x 16
        this.border_width = borderThick? 4 : 3;
        this.normal_width = normal_width;
        this.normal_height = normal_height;
        this.isMessageBox = isMessageBox;
        maximized_width = Windows98.SCREEN_WIDTH + border_width * 2;
        maximized_height = Windows98.SCREEN_HEIGHT - 28 + border_width * 2;
        backupBitmap = createBitmap(maximized_width, maximized_height, Bitmap.Config.ARGB_8888);
        if(backupBitmap == null)  // crash в Play Console
            throw new OutOfMemoryError();
        backupCanvas = new Canvas(backupBitmap);
        int max_random = 89;  // самые большие x и y при рандоме
        max_random = Math.min(max_random, Windows98.SCREEN_WIDTH - normal_width);
        max_random = Math.min(max_random, Windows98.TASKBAR_Y - normal_height);
		if(max_random < 0)
			max_random = 0;
        x = (int)(Math.random() * max_random);
        y = x;
        x_old = x;
        y_old = y;
        topButtons = new TopButtons();
        topButtons.showOnlyClose = isMessageBox;
        unableToMaximize = isMessageBox;
        elements.add(topButtons);
        pos++;  // topButtons

        if(createTopMenu){
            topMenu = new TopMenu();
            elements.add(topMenu); // чтобы обрабатывать нажатия
            pos++;  // то есть никто не перекроет topMenu из элементов
        }
        if(!isMessageBox) {
            TaskbarButton taskbarButton = new TaskbarButton(this, getContextMenu(false));
            ProgramsInTaskBar.programsInTaskBar.elements.add(taskbarButton);
            ProgramsInTaskBar.programsInTaskBar.updateButtonsSize();
        }
        makeActive();  // добавляет в Windows98.windows98.elements, делает onOtherTouch дргуим окнам
        //Windows98.windows98.elements.add(Windows98.windows98.elements.size() - 1, this);  // это равносильно top()
        contextMenu = getContextMenu(isMessageBox);
        contextMenu.y = -1;  // нужно для обработки клика на иконку
        elements.add(contextMenu);
        pos++;  // contextMenu
        Windows98.windows98.inputFocus = this;
        restore(false);
    }

    @Override
    public final void onDraw(Canvas canvas, int x, int y) {
        if(active || backupBitmap == null){  // если у нас было плохо с памятью, могло не получиться создать backupBitmap
            onNewDraw(canvas, x, y);
            drawTopLayer(canvas, x, y);
        }
        else{
            if(shouldRedraw){  // первый неактивный кадр, либо окно неактивно но обновилось
                onNewDraw(backupCanvas, 0, 0);
                drawTopLayer(backupCanvas, 0, 0);
            }
            src.right = width;
            src.bottom = height;
            dst.set(src);
            dst.offset(x, y);
            canvas.drawBitmap(backupBitmap, src, dst, replacePaint);
        }
        shouldRedraw = active;
    }

    public void onNewDraw(Canvas canvas, int x, int y) {  // если не active, то не надо перерисовываться, и этот метод не вызывается
        Element.drawFrameRect(canvas, x, y, x + width, y + height, fillWindow);
        if(active)
            p.setColor(Color.parseColor("#0000A8"));  // синий
        else
            p.setColor(Color.parseColor("#87888F"));  // серый
        canvas.drawRect(x + border_width, y + border_width, x + width - border_width, y + border_width + 18, p);
        if(icon != null)
            canvas.drawBitmap(icon, x + border_width + 2, y + border_width + 1, null);
        if (active)
            p_bold.setColor(Color.WHITE);
        else
            p_bold.setColor(Color.parseColor("#C0C7C8"));
        canvas.drawText(shortTitle, x + border_width + (icon == null ? 2 : 21), y + border_width + 13, p_bold);  // если нет иконки, то заголовок окна сдвинут влево
        // рисуем элементы. Не вызовом ElementContainer, т. к. TopMenu иногда приходится рисовать позже
        if(drawElements) {
            for (Element el : elements) {
                if (!el.visible)
                    continue;
                if(el == topElement)
                    continue;
                if (el == topMenu)
                    continue;
                el.parent = this;
                el.onDraw(canvas, x + el.x, y + el.y);
            }
        }
        /*if(drawTopLayer)
            drawTopLayer(canvas, shift_x, shift_y);*/
    }
    private void drawTopLayer(Canvas canvas, int x, int y){  // рисуем в верхнем слое topMenu, contextMenu, а также пунктирную линию при перемещении окна
        if(topMenu != null && topMenu.visible) {
            topMenu.parent = this;
            topMenu.onDraw(canvas, x + topMenu.x, y + topMenu.y);
        }

        if(topElement != null && topElement.visible) {
            topElement.parent = this;
            topElement.onDraw(canvas, x + topElement.x, y + topElement.y);
        }

        if(contextMenu != null)
            contextMenu.onDraw(canvas, x + contextMenu.x, y + contextMenu.y);

        if(moving && startedMovement) {
            int real_x = getCursorX(), real_y = getCursorY();
            drawNegatedRect(WindowsView.canvas, real_x - move_shiftX, real_y - move_shiftY, real_x - move_shiftX + width, real_y - move_shiftY + height,
                    border_width == 3 ? 1 : 4);
        }
    }
    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(moving) {
            startedMovement = true;
            return true;
        }
        boolean return_value = super.onMouseOver(x, y, touch);
        if(return_value && touch) {  // если в нас нажали
            parent.startTouch = this;  // иногда надо
            makeActive();
        }
        return return_value;
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        if(!(0 <= x && x < width && 0 <= y && y < height))
            return false;
        if(!touch)
            return true;
        if(defaultButton != null)
            defaultButton.coolActive = true;
        if(inputFocus instanceof TextBox)
            ((TextBox) inputFocus).setActive(true);

        int x1 = border_width, y1 = border_width, x2 = width - border_width, y2 = border_width + 18;  // 18 - высота синей панели
        if(x1 <= x && x < x2 && y1 <= y && y < y2){  // если кликнули в заголовок
            // клик на иконку - открываем контекстное меню.
            if(x < border_width + 2 + 16){
                if(contextMenu.y != -1 && (contextMenu.y < y1 || contextMenu.y == y2)){  // мы его уже открыли - закрываем
                    contextMenu.active = false;
                    contextMenu.y = -1;
                    return true;
                }
                if(contextMenu.child.width == -1)
                    contextMenu.child.onDraw(new Canvas(), 0, 0);
                contextMenu.width = contextMenu.child.width;
                contextMenu.height = contextMenu.child.height;
                TopMenuButton.positionDropdownMenu(x1 + getAbsoluteX(), y1 + getAbsoluteY(), x2 - x1, y2 - y1, contextMenu);
                contextMenu.x += x1;
                contextMenu.y += y1;
                contextMenu.active = true;
                return true;
            }
            if(!maximized) {
                moving = true;
                Windows98.movingWindow = true;
                startedMovement = false;
                move_shiftX = x;
                move_shiftY = y;
            }
        }
        return true;
    }

    private class WindowContextMenu extends ContextMenu {
        public WindowContextMenu(ButtonList child) {
            super(child);
        }

        @Override
        public void onOtherTouch() {
            super.onOtherTouch();
            int x1 = border_width, y1 = border_width, x2 = border_width + 2 + 16, y2 = border_width + 18;  // 18 - высота синей панели
            int clickX = getCursorX() - Window.this.x;
            int clickY = getCursorY() - Window.this.y;
            // мы кликнули на иконку
            if(!(x1 <= clickX && clickX < x2 && y1 <= clickY && clickY < y2))
                y = -1;  // для того, чтобы при нажатии на иконку меню открывалось правильно
        }
    }

    @Override
    public void onSelfRightClick(int x, int y) {
        int x1 = border_width, y1 = border_width, x2 = width - border_width, y2 = border_width + 18;  // 18 - высота синей панели
        if(x1 <= x && x < x2 && y1 <= y && y < y2){  // если кликнули в заголовок
            super.onSelfRightClick(x, y);
        }
    }

    @Override
    public void onRightClick(int x, int y) {
        super.onRightClick(x, y);
        makeActive();
    }

    @Override
    public void onSelfDoubleClick(int x, int y) {
        moving = false;
        startedMovement = false;
        Windows98.movingWindow = false;
        int x1 = border_width, y1 = border_width, x2 = width - border_width, y2 = border_width + 18;
        if(x1 <= x && x < x2 && y1 <= y && y < y2){
            if(maximized)
                restore();
            else
                maximize();
        }
        onSelfClick(x, y);
    }

    @Override
    public void onSelfOtherTouch() {
        active = false;
        if(defaultButton != null)
            defaultButton.coolActive = true;
    }

    @Override
    public void onOtherTouch() {
        if(ignoreOtherTouch){
            ignoreOtherTouch = false;
            return;
        }
        super.onOtherTouch();
    }

    @Override
    public void onClick(int x, int y) {
        if(moving) {
            moving = false;
            Windows98.movingWindow = false;
            this.x = this.x + x - move_shiftX;
            this.y = this.y + y - move_shiftY;
            x_old = this.x;
            y_old = this.y;
            lastClick = null; // клик на себя
        }
        else
            super.onClick(x, y);
    }

    private void restore(boolean repositionElements){
        maximized = false;
        if(!visible)
            makeActive();
        width = normal_width;
        height = normal_height;
        x = x_old;
        y = y_old;
        contextMenu.active = false;
        repositionEverything(repositionElements);
    }

    public void restore(){
        restore(true);
    }

    public void maximize(){
        if(unableToMaximize)
            return;
        maximized = true;
        if(!visible)
            makeActive();
        width = maximized_width;
        height = maximized_height;
        x_old = x;
        y_old = y;
        x = -border_width;
        y = -border_width;
        contextMenu.active = false;
        repositionEverything(true);
    }

    public void minimize(){  // свернуть
        if(childMessagebox != null || topButtons.showOnlyClose)
            return;
        active = false;
        shouldRedraw = true;
        visible = false;
        activateNextWindow();
    }

    protected void repositionEverything(boolean repositionElements){
        topButtons.y = border_width + 2;
        topButtons.x = width - border_width - topButtons.width - 2;
        if(repositionTopMenu && topMenu != null){
            topMenu.y = border_width + 19;
            topMenu.x = border_width;
        }
        int maxWidth = topButtons.x + (topButtons.showOnlyClose? 34 : 0) - border_width - (icon == null ? 2 : 21) - 3;
        shortTitle = shortenTextToThreeDots(title, maxWidth, p_bold);
        if(repositionElements) {
            repositionElements();
            for(int i = elements.size() - 1; i >= 0; i--)  // IE зависит от этого
                elements.get(i).onWindowResize(maximized);
            updateMouseOver();  // так как положение элементов изменилось
        }
    }

    public void repositionElements(){}

    public final void close(){  // закрыть окно
        // переопределять close(boolean activateNextWindow)
        close(true);
    }

    public void close(boolean activateNextWindow) {
        forceClose();
        if(activateNextWindow) {
            activateNextWindow();
        }
    }

    public void forceClose(){  // закрыть без, например, создания MessageBox при несохраненном тексте
        if(closed)
            return;
        if(backupBitmap != null) {
            backupBitmap.recycle();
            backupBitmap = null;
        }
        TaskbarButton taskbarButton = ProgramsInTaskBar.programsInTaskBar.getButtonByWindow(this);
        if(taskbarButton != null) {
            taskbarButton.prepareForDelete();
            taskbarButton.parent = ProgramsInTaskBar.programsInTaskBar;
            taskbarButton.removeFromParent();
            ProgramsInTaskBar.programsInTaskBar.updateButtonsSize();
        }
        parent = Windows98.windows98;
        removeFromParent();
        closed = true;
    }

    private void activateNextWindow(){
        for (int i = Windows98.windows98.elements.size() - 1; i >= 0; i--) {
            Element element = Windows98.windows98.elements.get(i);
            if (element instanceof Window) {
                Window window = (Window) element;
                if(window.visible) {  // не свёрнуто
                    ((Window) element).makeActive();
                    break;
                }
            }
        }
    }

    static void drawNegatedRect(Canvas canvas, int left, int top, int right, int bottom, int lineWidth){
        //p.setStyle(Paint.Style.FILL);
        if(lineWidth == 1){
            for(int y = top; y < bottom; y++){  // вертикальные линии
                if((left + y) % 2 == 0)
                    negatePixel(canvas, left, y);
                if((right - 1 + y) % 2 == 0)
                    negatePixel(canvas, right - 1, y);
            }
            // чтобы линии не пересекались, там другие значения в цикле
            for(int x = left + 1; x < right - 1; x++){
                if((top + x) % 2 == 0)
                    negatePixel(canvas, x, top);
                if((bottom - 1 + x) % 2 == 0)
                    negatePixel(canvas, x, bottom - 1);
            }
        }
        else{
            for(int i = 0; i < lineWidth; i++){
                drawNegatedRect(canvas, left, top, right, bottom, 1);
                left++;
                top++;
                right--;
                bottom--;
            }
        }
    }

    static void drawNegatedRect(Canvas canvas, Rect rect, int lineWidth){
        drawNegatedRect(canvas, rect.left, rect.top, rect.right, rect.bottom, lineWidth);
    }

    private static void negatePixel(Canvas canvas, int x, int y){
        if(!(0 <= x && x < Windows98.SCREEN_WIDTH && 0 <= y && y < Windows98.SCREEN_HEIGHT))
            return;
        int color = WindowsView.canvas_bmp.getPixel(x, y);
        if(color == Color.TRANSPARENT)  // internet explorer
            color = Color.WHITE;
        //int new_color = Color.rgb(255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color));
        color ^= 0x00ffffff;  // ARGB
        p.setColor(color);
        canvas.drawPoint(x, y, p);
    }

    public void makeActive(){  // сделать окно активным, вызывается при нажатии на окно.
        // Также используется, чтобы восстановиться из свернутого состояния.
        if(closed)
            return;
        if(defaultButton != null && !active){
            defaultButton.coolActive = true;
        }
        if(inputFocus instanceof TextBox) {
            inputFocus.parent = this;  // иногда нужно
            ((TextBox) inputFocus).setActive(true);
        }
        for(Element element : Windows98.windows98.elements) {
            if(element != this && element instanceof Window)
                element.onOtherTouch();
        }
        top();
        active = true;
        visible = true;
        Windows98.windows98.inputFocus = this;
        // вызывается сразу после нажатия на кнопку таксбара, но до onSelfOtherTouch
        if(childMessagebox != null) {
            active = false;
            shouldRedraw = true;
            childMessagebox.makeActive();
            if(ignoreOtherTouch){  // см. ignoreOtherTouch
                ignoreOtherTouch = false;
                childMessagebox.ignoreOtherTouch = true;
            }
        }
    }

    void top() {
        Windows98.windows98.elements.remove(this);
        Windows98.windows98.elements.add(Windows98.windows98.elements.size() - 1, this);  // перед taskbar'ом
    }

    private ContextMenu getContextMenu(boolean showOnlyClose){
        ButtonList rightMenu = new ButtonList();
        OnClickRunnable closeRunnable = parent -> {
            if(childMessagebox != null)
                return;
            Window window = Window.this;
            // контектное меню открыто в кнопке окна в таскбаре
            boolean activateNextWindow = ((ContextMenu) (((ButtonList)((ButtonInList) parent).parent).parentButton)).parent instanceof TaskbarButton;
            window.close(activateNextWindow);
            ((ButtonInList) parent).closeMenu();
        };
        OnClickRunnable moveRunnable = parent -> {
            // располагаем курсор по середине синего прямоугольника
            int cursor_x = x + width / 2;
            int cursor_y = y + 10;
            if(cursor_x < 0)
                cursor_x = 0;
            else if(cursor_x > Windows98.SCREEN_WIDTH - 1)
                cursor_x = Windows98.SCREEN_WIDTH - 1;
            if(cursor_y < 0)
                cursor_y = 0;
            else if(cursor_y > Windows98.SCREEN_HEIGHT - 1)
                cursor_y = Windows98.SCREEN_HEIGHT - 1;
            WindowsView.cursor_x = cursor_x;
            WindowsView.cursor_y = cursor_y;
            ((ButtonInList) parent).closeMenu();
        };
        if(!showOnlyClose) {
            ButtonInList restore = new ButtonInList("Restore", getBmp(R.drawable.restore_image_inactive),
                    getBmp(R.drawable.restore_image_active));
            restore.closeMenuAfterClick = false;
            restore.action = parent -> {
                Window window = Window.this;
                if(!window.maximized && window.visible)
                    return;
                window.restore();
                ((ButtonInList) parent).closeMenu();
            };
            rightMenu.elements.add(restore);
            ButtonInList move = new ButtonInList("Move");
            move.closeMenuAfterClick = false;
            move.action = moveRunnable;
            rightMenu.elements.add(move);
            ButtonInList size = new ButtonInList("Size");
            size.closeMenuAfterClick = false;
            size.disabled = true;
            rightMenu.elements.add(size);
            ButtonInList minimize = new ButtonInList("Minimize", getBmp(R.drawable.minimize_image_inactive),
                    getBmp(R.drawable.minimize_image_active));
            minimize.closeMenuAfterClick = false;
            minimize.action = parent -> {
                Window window = Window.this;
                window.minimize();
                ((ButtonInList) parent).closeMenu();
            };
            rightMenu.elements.add(minimize);
            ButtonInList maximize = new ButtonInList("Maximize", getBmp(R.drawable.maximize_image_inactive),
                    getBmp(R.drawable.maximize_image_active));
            maximize.closeMenuAfterClick = false;
            maximize.action = parent -> {
                Window window = Window.this;
                if(window.maximized && window.visible)
                    return;
                window.maximize();
                ((ButtonInList) parent).closeMenu();
            };
            rightMenu.elements.add(maximize);
            rightMenu.elements.add(new Separator());
            ButtonInList close = new ButtonInList("Close", "Alt+F4", getBmp(R.drawable.close_image_inactive),
                    getBmp(R.drawable.close_image_active));
            close.closeMenuAfterClick = false;
            close.action = closeRunnable;
            rightMenu.elements.add(close);
        }
        else{
            ButtonInList move = new ButtonInList("Move");
            move.closeMenuAfterClick = false;
            move.action = moveRunnable;
            rightMenu.elements.add(move);
            if(!topButtons.closeDisabled) {
                ButtonInList close = new ButtonInList("Close", "Alt+F4", getBmp(R.drawable.close_image_inactive),
                        getBmp(R.drawable.close_image_active));
                close.closeMenuAfterClick = false;
                close.action = closeRunnable;
                rightMenu.elements.add(close);
            }
        }
        return new WindowContextMenu(rightMenu);
    }

    public void addElement(Element element){
        elements.add(elements.size() - pos, element);
        element.parent = this;  // иногда надо
    }

    public void addElement(Element element, int x, int y){
        addElement(element);
        element.x = x;
        element.y = y;
    }

    public void alignWith(Window window){  // когда мы в Explorer'е переходим из одной папки в другую, открывается новое окно, которое должно располагаться так же, как старое
        if(window.maximized)
            maximize();
		x = window.x;
		x_old = window.x_old;
        y = window.y;
        y_old = window.y_old;

        if(!window.isMessageBox) {
            TaskbarButton taskbarButton = ProgramsInTaskBar.programsInTaskBar.getButtonByWindow(this);  // чтобы кнопка окна в панели задач была в том же месте
            ProgramsInTaskBar.programsInTaskBar.elements.remove(taskbarButton);
            int position = ProgramsInTaskBar.programsInTaskBar.elements.indexOf(ProgramsInTaskBar.programsInTaskBar.getButtonByWindow(window));
            ProgramsInTaskBar.programsInTaskBar.elements.add(position, taskbarButton);
        }
        if(!window.closed)
            window.close();
        updateMouseOver();
    }

    public void updateMouseOver(){
        onMouseOver(getCursorX() - x, getCursorY() - y, false);
    }

    protected Button addCloseButton(Rect closeButtonRect, String text){  // добавляет в окно кнопку, которая его закрывает. Требуется в различных DummyWindow, чтобы работали кнопки вроде "OK" или "Cancel"
        Button closeButton = new Button(text, closeButtonRect, parent -> close());
        addElement(closeButton);
        return closeButton;
    }

    public void setTopMenu(TopMenu topMenu){
        if(this.topMenu != null)
            return;
        this.topMenu = topMenu;
        elements.add(topMenu);
        pos++;
    }

    protected void setTitle(String title){
        this.title = title;
        shouldRedraw = true;
        repositionEverything(false);
    }

    public String getTitle(){
        return title;
    }

    public void centerWindowOnScreen(){
        centerWindowHorizontally();
        y = Windows98.SCREEN_HEIGHT / 2 - height / 2;
        if(y + height > Windows98.TASKBAR_Y)
            y = Windows98.SCREEN_HEIGHT - height;
        if(y < 0)
            y = 0;
        y_old = y;
    }

    public void centerWindowHorizontally(){
        x = Windows98.SCREEN_WIDTH / 2 - width / 2;
        if(x + width > Windows98.SCREEN_WIDTH)
            x = Windows98.SCREEN_WIDTH - width;
        if(x < 0)
            x = 0;
        x_old = x;
    }

    protected void disableCloseButton(){
        topButtons.closeDisabled = true;
        // убираем close из контекстного меню
        contextMenu.child.elements.remove(contextMenu.child.elements.get(1));
        // вызывается только для DialogWindow, у которых нет кнопки в таксбаре
    }

    protected void deleteCloseButton(){
        topButtons.visible = false;
        // убираем close из контекстного меню
        contextMenu.child.elements.remove(contextMenu.child.elements.get(1));
    }

    public void updateWindow(){
        shouldRedraw = true;
        WindowsView.windowsView.invalidate();
    }

    public class RelativeBounds {
        // класс, задающий координаты чего-либо относительно размеров окна. Отрицательное значение - относительно правой/нижней стороны
        private int left, top, right, bottom;
        private Rect minimizedRect, maximizedRect;  // чтобы не выделять лишний раз память

        public RelativeBounds(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public Rect getMinimizedRect(){
            if(minimizedRect != null)
                return minimizedRect;
            return minimizedRect = getRect(normal_width, normal_height);
        }

        public Rect getMaximizedRect(){
            if(maximizedRect != null)
                return maximizedRect;
            return maximizedRect = getRect(maximized_width, maximized_height);
        }

        public Rect getRect(boolean maximized){
            return maximized? getMaximizedRect() : getMinimizedRect();
        }

        private Rect getRect(int width, int height){
            return new Rect(getCoord(left, width), getCoord(top, height), getCoord(right, width), getCoord(bottom, height));
        }

        private int getCoord(int coord, int size){
            if(coord < 0)
                coord += size;
            return coord;
        }
    }
}
