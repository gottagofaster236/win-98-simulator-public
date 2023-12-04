package com.lr_soft.windows98simulator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.VelocityTracker;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import com.lr_soft.windows98simulator.System.Cursor;
import com.lr_soft.windows98simulator.System.Windows98;

import static com.lr_soft.windows98simulator.System.Element.context;
import static com.lr_soft.windows98simulator.System.Element.getBmp;
import static com.lr_soft.windows98simulator.System.Windows98.windows98;

public class WindowsView extends View {
    public static Handler handler = new Handler();
    public static Bitmap canvas_bmp;
    public static Canvas canvas;  // SCREEN_WIDTH x SCREEN_HEIGHT
    public Rect position = new Rect();  // положение windows 98 на экране
    private Rect src = new Rect();
    RectF dstF = new RectF();  // положение кнопки клавиатуры
    public static float cursor_x, cursor_y;
    float prev_x, prev_y;  // координаты предыдущего touch event
    boolean pressed = false;
    float[][] lastCursors = new float[2][2];  // координаты начала нажатия (на тачскрин!)
    public boolean artificialClickOngoing = false;
    boolean afterArtificialClick = false;  // после искуственного (не громкость, а короткое нажатие на экран) нажатия
    boolean isArtificialHold = false;  // при поднятии пальца с экрана будет произведен клик
    long[] pressStartTimes = new long[2];  // два последних нажатия на экран
    static float screen_width, screen_height;
    Paint p = new Paint();
    public static WindowsView windowsView;
    boolean leftDown = false;
    Bitmap keyboardBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.keyboard_button);
    public Runnable update = new Runnable() {  // для того, чтобы обновлялись часы
        @Override
        public void run() {
            WindowsView.windowsView.invalidate();
            handler.postDelayed(this, 5000);
            if(Windows98.TAUON) {
                if(Windows98.state == Windows98.WORKING){
                    Windows98.windows98.getCursor().recreatePointerIcon();
                }
            }
        }
    };
    public static int sensitivity;  // Windows98.SCREEN_HEIGHT - движение курсором как обычно
    static float accelerationCoeff = 100f / 77;  // коэффициент для ускорения курсора (повышенная точность указателя)
    public static float accelerationPower;  // коэффициент для ускорения курсора
    VelocityTracker velocityTracker = VelocityTracker.obtain();
    public static PointerIcon windowsCursor;  // TYPE_NULL в обычном случае, либо картинка курсора в случае TAUON
    static{
        if(Build.VERSION.SDK_INT >= 24)
            windowsCursor = PointerIcon.getSystemIcon(context, PointerIcon.TYPE_NULL);
    }

    boolean isBSOD = false;
    private static Bitmap bsodBitmap;

    public WindowsView(final Context context, AttributeSet attrs){
        super(context, attrs);
        if(Build.VERSION.SDK_INT >= 26)
            setDefaultFocusHighlightEnabled(false);
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        p.setDither(true);
        windowsView = this;
        handler.removeCallbacks(update);
        handler.post(update);
        if(Build.VERSION.SDK_INT >= 24)
            setPointerIcon(windowsCursor);
        if(bsodBitmap == null)
            bsodBitmap = getBmp(R.drawable.bsod);
    }

    @Override
    protected void onDraw(Canvas canvas_big) {
        if(canvas_big == null) {
            return;
        }
        if(windows98 == null && !isBSOD)
            return;
        screen_width = getWidth();
        screen_height = getHeight();
        try {
            if (windows98 != null && Windows98.state != Windows98.WAIT_FOR_STARTUP) {
                //long start = System.currentTimeMillis();
                windows98.onDraw(canvas, 0, 0);
                //long end = System.currentTimeMillis();
                //Log.d("Debuggy!", "frame took " + (end - start) + " ms");
            }
        }
        catch (OutOfMemoryError e){
            showBSOD();
        }
        // Переносим с маленького Canvas'а на большой
        float aspectRatio;
        if(isBSOD)
            aspectRatio = (float) bsodBitmap.getWidth() / bsodBitmap.getHeight();
        else {
            Windows98.windows98.getSrc(src);
            aspectRatio = (float) src.width() / src.height();
        }
        if(screen_width / screen_height > aspectRatio){  // width / height > 4 : 3. Если у нас широкоэкранный телефон, рисуем черные полосы слева и справа
            position.left = (int)(screen_width / 2 - screen_height * aspectRatio / 2);
            position.top = 0;
            position.right = (int)(screen_width / 2 + screen_height * aspectRatio / 2);
            position.bottom = (int) screen_height;
            p.setColor(Color.BLACK);
            canvas_big.drawRect(0, 0, position.left, screen_height, p);
            canvas_big.drawRect(position.right, 0, screen_width, screen_height, p);
        }
        else{  // квадратное разрешение - рисуем полосы сверху и снизу
            position.left = 0;
            position.top = (int)(screen_height / 2 - screen_width / aspectRatio / 2);
            position.right = (int) screen_width;
            position.bottom = (int)(screen_height / 2 + screen_width / aspectRatio / 2);
            p.setColor(Color.BLACK);
            canvas_big.drawRect(0, 0, screen_width, position.top, p);
            canvas_big.drawRect(0, position.bottom, screen_width, screen_height, p);
        }
        if(false){  // рястягиваем экран
            position.left = 0; position.top = 0;
            position.right = (int) screen_width;
            position.bottom = (int) screen_height;
        }

        if(!isBSOD)
            canvas_big.drawBitmap(canvas_bmp, src, position, p);
        else
            canvas_big.drawBitmap(bsodBitmap, null, position, p);

        getKeyboardButtonCoords();
        if(!dstF.isEmpty()) {  // рисуем кнопку клавиатуры
            p.setColor(0x92DDDDDD);
            canvas_big.drawOval(dstF, p);
            float imageScale = 0.72f;
            float cx = dstF.centerX(), cy = dstF.centerY();
            float sizeCoeff = dstF.right - dstF.left;
            dstF.top -= sizeCoeff * 0.06f;  // центруем картинку
            dstF.bottom -= sizeCoeff * 0.06f;
            dstF.left = (dstF.left - cx) * imageScale + cx;
            dstF.top = (dstF.top - cy) * imageScale + cy;
            dstF.right = (dstF.right - cx) * imageScale + cx;
            dstF.bottom = (dstF.bottom - cy) * imageScale + cy;
            canvas_big.drawBitmap(keyboardBitmap, null, dstF, p);
            p.setColor(Color.WHITE);
        }
    }

    @SuppressLint({"WrongCall", "MissingSuperCall"})
    @Override
    public void draw(Canvas canvas) {  // чтобы не было оранжевой рамки фокуса
        onDraw(canvas);
    }

    private void getKeyboardButtonCoords(){
        if(Windows98.TAUON){
            dstF.left = dstF.top = dstF.right = dstF.bottom = 0;
            return;
        }
        // есть минимальный и максимальный размеры кнопки, если возможно, делаем как можно больше
        float minRectWidth = Math.min(screen_height, screen_width) * 0.16f;
        float maxRectWidth = Math.min(screen_height, screen_width) * 0.22f;
        float winAlighRectWidth = (screen_width - screen_height * Windows98.SCREEN_WIDTH / (float) Windows98.SCREEN_HEIGHT) / 2.3f;
        float rectWidth = Math.min(Math.max(winAlighRectWidth, minRectWidth), maxRectWidth);
        dstF.right = screen_width; dstF.top = screen_height * 0.05f;
        dstF.left = dstF.right - rectWidth; dstF.bottom = dstF.top + rectWidth;
    }

    boolean lastRightPressed = false;  // нажата ли правая кнопка мыши на (usb) мышке

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(windows98 == null)
            return true;

        try {
            if((event.getSource() & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE){  // физическая мышь
                // правая кнопка мыши
                boolean rightPressed = (event.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0;
                if(!lastRightPressed && rightPressed){
                    onRightDown();
                    onRightUp();
                }
                lastRightPressed = rightPressed;
                int x = (int) ((event.getX() - position.left) / (position.right - position.left) * Windows98.SCREEN_WIDTH);
                int y = (int) ((event.getY() - position.top) / (position.bottom - position.top) * Windows98.SCREEN_HEIGHT);
                if(Build.VERSION.SDK_INT >= 24) {
                    // снаружи курсор - стрелочка; внутри - ничего (так как мы сами отрисовываем курсор)
                    boolean outside = x < 0 || x >= Windows98.SCREEN_WIDTH || y < 0 || y >= Windows98.SCREEN_HEIGHT;
                    if(Windows98.TAUON)
                        outside = false;
                    PointerIcon pointerIcon = outside ? PointerIcon.getSystemIcon(context, PointerIcon.TYPE_DEFAULT) : windowsCursor;
                    if(getPointerIcon() != pointerIcon) {
                        setPointerIcon(pointerIcon);
                    }
                }
                if(checkKeyboardButtonClick(event))
                    return true;

                if (x < 0)
                    x = 0;
                else if (x > Windows98.SCREEN_WIDTH - 1)
                    x = Windows98.SCREEN_WIDTH - 1;
                if (y < 0)
                    y = 0;
                else if (y > Windows98.SCREEN_HEIGHT - 1)
                    y = Windows98.SCREEN_HEIGHT - 1;
                if (Windows98.movingWindow && y > Windows98.TASKBAR_Y - 1)
                    y = Windows98.TASKBAR_Y - 1;
                if(event.getActionMasked() == MotionEvent.ACTION_DOWN && !rightPressed) {
                    onCursorDown();
                }
                else if(event.getActionMasked() == MotionEvent.ACTION_UP)
                    onCursorUp();
                else if(event.getActionMasked() == MotionEvent.ACTION_MOVE || event.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE){
                    if(x != cursor_x || y != cursor_y){
                        cursor_x = x;
                        cursor_y = y;
                        onMove();
                    }
                }
                return true;
            }

            velocityTracker.addMovement(event);

            switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                // делаем что-то похожее на "повышенную точность установки указателя мыши" в Windows
                velocityTracker.computeCurrentVelocity(1);  // скорость в пикселях за миллисекунду
                double velocity = sensitivity / screen_height * accelerationCoeff *
                        Math.sqrt(velocityTracker.getXVelocity() * velocityTracker.getXVelocity() + velocityTracker.getYVelocity() * velocityTracker.getYVelocity());
                if (velocity > 20)
                    velocity = 1;
                float coeff = (float) Math.pow(velocity, accelerationPower);  // коэффициент ускорения курсора
                if (coeff < 1)
                    coeff = 1;
                //else if(coeff > 1)
                //    Log.d(TAG, "COEFF: " + coeff);

                float dx = event.getX() - prev_x, dy = event.getY() - prev_y;
                int last_cursor_x = (int) cursor_x, last_cursor_y = (int) cursor_y;
                cursor_x += dx * sensitivity / screen_height * coeff;
                cursor_y += dy * sensitivity / screen_height * coeff;
                prev_x = event.getX();
                prev_y = event.getY();
                if (cursor_x < 0)
                    cursor_x = 0;
                else if (cursor_x > Windows98.SCREEN_WIDTH - 1)
                    cursor_x = Windows98.SCREEN_WIDTH - 1;
                if (cursor_y < 0)
                    cursor_y = 0;
                else if (cursor_y > Windows98.SCREEN_HEIGHT - 1)
                    cursor_y = Windows98.SCREEN_HEIGHT - 1;
                if (Windows98.movingWindow && cursor_y > Windows98.TASKBAR_Y - 1)
                    cursor_y = Windows98.TASKBAR_Y - 1;
                if ((int) cursor_x != last_cursor_x || (int) cursor_y != last_cursor_y)  // если курсор действительно передвинулся
                    onMove();
                break;
            case MotionEvent.ACTION_DOWN:
                velocityTracker.clear();
                velocityTracker.addMovement(event);
                prev_x = event.getX();
                prev_y = event.getY();
                if (checkKeyboardButtonClick(event))
                    return true;
                float[] tmp = lastCursors[0];
                lastCursors[0] = lastCursors[1];
                lastCursors[1] = tmp;
                lastCursors[1][0] = cursor_x;
                lastCursors[1][1] = cursor_y;
                pressStartTimes[0] = pressStartTimes[1];
                pressStartTimes[1] = System.currentTimeMillis();
                if (afterArtificialClick) {
                    float dist = (cursor_x - lastCursors[0][0]) * (cursor_x - lastCursors[0][0]) + (cursor_y - lastCursors[0][1]) * (cursor_y - lastCursors[0][1]);
                    float maxDist = (screen_width * 0.005f) * (screen_width * 0.005f);
                    if (dist <= maxDist && System.currentTimeMillis() - pressStartTimes[0] <= 200 && !leftDown) {  // двойной тап
                        afterArtificialClick = false;
                        isArtificialHold = true;
                        leftDown = true;
                        windows98.onMouseOver((int) cursor_x, (int) cursor_y, true);
                        //Log.d(TAG, "double tap");
                        invalidate();
                    }
                }
                pressed = true;
                break;
            case MotionEvent.ACTION_UP:
                pressed = false;
                float dist = (cursor_x - lastCursors[1][0]) * (cursor_x - lastCursors[1][0]) + (cursor_y - lastCursors[1][1]) * (cursor_y - lastCursors[1][1]);
                float maxDist = (screen_width * 0.005f) * (screen_width * 0.005f);
                // Log.d(TAG, "dist, maxDist: " + dist + " " + maxDist);
                if (isArtificialHold) {
                    isArtificialHold = false;
                    //Log.d(TAG, "on win98 click!");
                    leftDown = false;
                    windows98.onClick((int) cursor_x, (int) cursor_y);
                    invalidate();
                    return true;
                }
                if (dist <= maxDist && System.currentTimeMillis() - pressStartTimes[1] <= 300 && !leftDown) {  // click
                    artificialClickOngoing = true;
                    windows98.onMouseOver((int) cursor_x, (int) cursor_y, true);
                    if (windows98 != null)  // bsod
                        windows98.onClick((int) cursor_x, (int) cursor_y);
                    artificialClickOngoing = false;
                    afterArtificialClick = true;
                    invalidate();
                }
                else
                    afterArtificialClick = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                windows98.onRightClick((int) cursor_x, (int) cursor_y);
                break;
            }
        }
        catch (OutOfMemoryError e){
            showBSOD();
        }
        return true;
    }

    private boolean checkKeyboardButtonClick(MotionEvent event){
        if(event.getActionMasked() != MotionEvent.ACTION_DOWN)
            return false;
        getKeyboardButtonCoords();
        // проверка на нажатие по кнопке "клавиатура"
        float cx = dstF.centerX(), cy = dstF.centerY(), radius = (dstF.right - dstF.left) / 2;
        float expandCoeff = 1; //1.2f;  // нажать можно не только в саму кнопку, но и рядом
        if ((event.getX() - cx) * (event.getX() - cx) + (event.getY() - cy) * (event.getY() - cy) <= radius * radius * expandCoeff * expandCoeff) {

            // показываем клавиатуру
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            context.delayedHide(300);
            return true;
        }
        return false;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {  // т. к. action_hover не доставляется в touch_event
        onTouchEvent(event);
        return true;
    }

    public void onMove(){
        if(windows98 == null)
            return;
        try {
            windows98.onMouseOver((int) cursor_x, (int) cursor_y, false);
            invalidate();
        }
        catch (OutOfMemoryError e){
            showBSOD();
        }
    }

    public void onCursorDown(){
        if(windows98 == null)
            return;
		if(leftDown)
			return;
        try {
            windows98.onMouseOver((int) cursor_x, (int) cursor_y, true);
        }
        catch (OutOfMemoryError e){
            showBSOD();
            return;
        }
        afterArtificialClick = false;
        isArtificialHold = false;
        leftDown = true;
        invalidate();
    }

    public void onCursorUp(){
        if(windows98 == null)
            return;
		if(!leftDown)
			return;
        try {
            windows98.onClick((int) cursor_x, (int) cursor_y);
        }
        catch (OutOfMemoryError e){
            showBSOD();
            return;
        }
		afterArtificialClick = false;
        isArtificialHold = false;
        leftDown = false;
        invalidate();
    }

    public void onRightDown(){
        if(windows98 == null)
            return;
        if(leftDown)
            return;
        try {
            windows98.onRightClick((int) cursor_x, (int) cursor_y);
        }
        catch (OutOfMemoryError e){
            showBSOD();
        }
        invalidate();
    }

    public void onRightUp(){
    }

    public void showBSOD(){
        if(isBSOD)
            return;
        // удаляем всё, так как BSOD показывается в случае OutOfMemoryError (what?)
        isBSOD = true;
        Windows98.state = Windows98.WAIT_FOR_STARTUP;
        Windows98.deleteWindows();
        windows98 = null;
        System.gc();
        hideCustomCursor();
        // скрываем клавиатуру
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindowToken(), 0);

        handler.postDelayed(() -> {
            WindowsView.windowsView.isBSOD = false;
            Windows98.windows98 = new Windows98();
            Windows98.windows98.startup();
        }, 4500);
        invalidate();
    }

    public void slowBSOD(){
        Windows98.setCursor(new Cursor(getBmp(R.drawable.sandglass)));
        handler.postDelayed(this::showBSOD, 1000);
    }

    public void hideCustomCursor(){
        if(Windows98.TAUON && Build.VERSION.SDK_INT >= 24) {
            windowsCursor = PointerIcon.getSystemIcon(context, PointerIcon.TYPE_NULL);
            setPointerIcon(windowsCursor);
        }
    }

    /*public void exit(){  // закрывает приложение
        if(Build.VERSION.SDK_INT >= 16) {
            try {
                ((MainActivity) getContext()).finishAffinity();
            }
            catch (Exception e){  // crash в play console
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
        else
            android.os.Process.killProcess(android.os.Process.myPid());
    }*/
    // ============================================ ВВОД ТЕКСТА ============================================

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(Windows98.windows98 == null)
            return true;
        /*if(event.getKeyCode() == KeyEvent.KEYCODE_BACK){
            onRightUp();
            onRightDown();
            return true;
        }*/
        if(event.getAction() != KeyEvent.ACTION_DOWN)
            return true;
        String key;
        if(keyCode == KeyEvent.KEYCODE_DEL) {
            key = "DEL";
        }
        else {
            char c = (char) event.getUnicodeChar();
            if(c == EditableAccomodatingLatinIMETypeNullIssues.ONE_UNPROCESSED_CHARACTER.charAt(0))
                return true;
            if(c == 0)
                return true;
            key = String.valueOf(c);
        }
        Windows98.windows98.onKeyPress(key);
        invalidate();
        return true;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        if(Windows98.windows98 == null)
            return true;
        if(event.getCharacters() != null && !event.getCharacters().contentEquals(EditableAccomodatingLatinIMETypeNullIssues.ONE_UNPROCESSED_CHARACTER)) {
            if(keyCode == KeyEvent.KEYCODE_DEL){
                Windows98.windows98.onKeyPress("DEL");
                invalidate();
                return true;
            }
            int c = event.getCharacters().codePointAt(0);
            if(c == 0)
                return true;
            Windows98.windows98.onKeyPress(String.valueOf((char) c));
            invalidate();
            return true;
        }
        else
            return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    // (с) https://stackoverflow.com/questions/18581636/android-cannot-capture-backspace-delete-press-in-soft-keyboard/19980975#19980975
    // @author Carl Gunther
    private static class InputConnectionAccomodatingLatinIMETypeNullIssues extends BaseInputConnection {
        Editable myEditable = null;
        public InputConnectionAccomodatingLatinIMETypeNullIssues(View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
        }

        @Override
        public Editable getEditable() {
            if(Build.VERSION.SDK_INT >= 14) {
                if(myEditable == null) {
                    myEditable = new EditableAccomodatingLatinIMETypeNullIssues(
                            EditableAccomodatingLatinIMETypeNullIssues.ONE_UNPROCESSED_CHARACTER);
                    Selection.setSelection(myEditable, 1);
                }
                else {
                    int myEditableLength = myEditable.length();
                    if(myEditableLength == 0) {
                        myEditable.append(
                                EditableAccomodatingLatinIMETypeNullIssues.ONE_UNPROCESSED_CHARACTER);
                        Selection.setSelection(myEditable, 1);
                    }
                }
                return myEditable;
            }
            else {
                return super.getEditable();
            }
        }
        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if((Build.VERSION.SDK_INT >= 14) // && (Build.VERSION.SDK_INT < 19)
                    && (beforeLength == 1 && afterLength == 0)) {
                //Send Backspace key down and up events to replace the ones omitted
                // by the LatinIME keyboard.
                return super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && super.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
            else {
                return super.deleteSurroundingText(beforeLength, afterLength);
            }
        }
    }

    private static class EditableAccomodatingLatinIMETypeNullIssues extends SpannableStringBuilder {
        EditableAccomodatingLatinIMETypeNullIssues(CharSequence source) {
            super(source);
        }

        //This character must be ignored by your onKey() code.
        public static CharSequence ONE_UNPROCESSED_CHARACTER = "\uffff";

        @Override
        public SpannableStringBuilder replace(final int spannableStringStart, final int spannableStringEnd, CharSequence replacementSequence,
                                              int replacementStart, int replacementEnd) {
            if (replacementEnd > replacementStart) {
                super.replace(0, length(), "", 0, 0);
                return super.replace(0, 0, replacementSequence, replacementStart, replacementEnd);
            }
            else if (spannableStringEnd > spannableStringStart) {
                super.replace(0, length(), "", 0, 0);
                return super.replace(0, 0, ONE_UNPROCESSED_CHARACTER, 0, 1);
            }
            return super.replace(spannableStringStart, spannableStringEnd,
                    replacementSequence, replacementStart, replacementEnd);
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnectionAccomodatingLatinIMETypeNullIssues baseInputConnection =
                new InputConnectionAccomodatingLatinIMETypeNullIssues(this, false);

        outAttrs.actionLabel = null;
        outAttrs.inputType = InputType.TYPE_NULL;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;

        return baseInputConnection;
    }
}
