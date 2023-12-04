package com.lr_soft.windows98simulator.System;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.lr_soft.windows98simulator.Applications.AndroidApps;
import com.lr_soft.windows98simulator.Applications.Calculator;
import com.lr_soft.windows98simulator.Applications.DisplayProperties;
import com.lr_soft.windows98simulator.Applications.Explorer;
import com.lr_soft.windows98simulator.Applications.FileProperties;
import com.lr_soft.windows98simulator.Applications.FreeCell;
import com.lr_soft.windows98simulator.Applications.InternetExplorer;
import com.lr_soft.windows98simulator.Applications.Link;
import com.lr_soft.windows98simulator.Applications.MPlayer;
import com.lr_soft.windows98simulator.Applications.Minesweeper;
import com.lr_soft.windows98simulator.Applications.MsDos;
import com.lr_soft.windows98simulator.Applications.MyComputer;
import com.lr_soft.windows98simulator.Applications.MyDocuments;
import com.lr_soft.windows98simulator.Applications.Notepad;
import com.lr_soft.windows98simulator.Applications.PaintBrush;
import com.lr_soft.windows98simulator.Applications.RecycleBin;
import com.lr_soft.windows98simulator.Applications.Solitaire;
import com.lr_soft.windows98simulator.Applications.Spider;
import com.lr_soft.windows98simulator.Applications.StartMenu;
import com.lr_soft.windows98simulator.Applications.WordPad;
import com.lr_soft.windows98simulator.MainActivity;
import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.WindowsView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
TODO

     Отображать первой папку, где есть файлы. МБ рефактор int'ов?
     Правильно сделать выбор расширения файла? По типу private final static String[] allExtensions = {"*"}
     Придумать что-нибудь с IE
     Make some buttons in WordPad work (such as New File, Open, Save, and Print -> Installing a printer) сделать ImageButton (принимает полупрозрачное ищобр
	 оптимизировать блокнот, т. к. моноширинный шрифт (например с помощью custom paint)
     исправить курсор в переименовании на жирный
     корзина (копировать в корзину нельзя; что если восстанавливаем файл из удаленной папки?)
     2) Mouse speed in control panel
     3) Users
     6) 7zip - это free software, его уж точно можно использовать!
     wddsa
     4) разобраться с rtf; не забыть про специальные символы вроде &

     локализация (нет, т. к. плохой user experience для знающих английский)
	 1) getString, который будет кешировать ресурсы (куда-нибудь, no idea). Можно сбрасывать в onConfigurationChanged(lang)
	 2) проверить все картинки, что в них не используется текст (или они не важны для перевода)
 */


public class Windows98 extends ElementContainer {
    public static int SCREEN_WIDTH = 640, SCREEN_HEIGHT = 480, TASKBAR_Y = 452;
    public static boolean WIDESCREEN = false;
    public static final boolean TAUON = false;
    private static Cursor cursor;
    private static Cursor defaultCursor;
    public static boolean movingWindow = false;  // сейчас переносим окно. В таком случае, курсор не может быть >= Windows98.DESKTOP_Y (не заходит за панель задач)
    public static Windows98 windows98;
    public static final int WAIT_FOR_STARTUP = -1, STARTUP = 0, WORKING = 1, SHUTDOWN = 2, MS_DOS_MODE = 3;
    public static int state = WAIT_FOR_STARTUP;

    private Bitmap wallpaper = null;
    private int wallpaperMode = 0;
    public final static int CENTER = 0, TILE = 1, STRETCH = 2;
    private Paint wallpaperPaint = new Paint();
    public static View targetFocus = WindowsView.windowsView;
    public DesktopLinks links;

    private int frameNumber = -1;  // для startup - номер кадра
    // 0 - 4 курсор
    // 5 - 23 windows 98 с ползунком
    // 24 - 33 курсор
    // 34 голубой экран с курсором
    private MediaPlayer startupSound,
            shutdownSound;
    static MediaPlayer chord;
    private Bitmap winShutdownBmp;
    private Bitmap windowsStartupBmp, startupLoadingBmp;
    private boolean restarting, restartingToMsDos;
    private Rect dst = new Rect();

    public Runnable startupRunnable = new Runnable() {
        @Override
        public void run() {
            frameNumber++;
            //Log.d(TAG, "frame: " + frameNumber);
            int delayTime;
            if(frameNumber <= 4 || (35 <= frameNumber && frameNumber <= 44)){  // курсор
                delayTime = 200;
            }
            else if(5 <= frameNumber && frameNumber <= 34){  // windows 98
                delayTime = 3700 / 30;
            }
            else if(frameNumber == 45){  // голубой экран с курсором
                // к этому моменту винда должна загрузиться
                try {
                    windowsLoadingThread.join();
                }
                catch (InterruptedException ignored) {}

                WindowsView.cursor_x = Windows98.SCREEN_WIDTH / 2;
                WindowsView.cursor_y = Windows98.SCREEN_HEIGHT / 2;
                cursor = new Cursor(getBmp(R.drawable.cursor_wait));
                setCursor(cursor);  // чтобы отобразить курсор в tauon
                delayTime = 2500;
            }
            else{
                try {
                    windowsLoadingThread.join();
                }
                catch (InterruptedException ignored) {}

                state = WORKING;
                Windows98.setDefaultCursor();
                windowsStartupBmp = startupLoadingBmp = null;
                WindowsView.windowsView.invalidate();
                startupSound.start();
                return;
            }
            WindowsView.windowsView.invalidate();
            WindowsView.handler.postDelayed(this, delayTime);
        }
    };

    public Runnable shutdown = new Runnable() {
        @Override
        public void run() {
            frameNumber++;
            int delayTime;
            if(frameNumber == 0){  // голубой экран с курсором
                shutdownSound.start();
                delayTime = 3000;
            }
            else if(frameNumber == 1){  // windows 98
                WindowsView.windowsView.hideCustomCursor();
                delayTime = 4000;
            }
            else{
                if(restarting){
                    restarting = false;
                    WindowsView.handler.post(WindowsView.windowsView.update);
                    startLoadingWindows();
                    startup();
                }
                else if(restartingToMsDos){
                    restartingToMsDos = false;
                    startupMsDos();
                }
                else {  // закрываем приложение
                    state = WAIT_FOR_STARTUP;
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
                return;
            }
            WindowsView.windowsView.invalidate();
            WindowsView.handler.postDelayed(this, delayTime);
        }
    };

    private Thread windowsLoadingThread;

    public Windows98(){
        windows98 = this;
        updateScreenSize();
        startLoadingWindows();
    }

    public void startLoadingWindows(){
        // делаем загрузку винды async
        windowsLoadingThread = new Thread(this::loadWindows);
        windowsLoadingThread.start();
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        // 640 x 400
        if(state == WAIT_FOR_STARTUP){
            canvas.drawColor(Color.BLACK);
        }
        else if(state == STARTUP){
            if(frameNumber <= 4 || (35 <= frameNumber && frameNumber <= 44)){  // курсор консоли
                canvas.drawColor(Color.BLACK);
                p.setColor(Color.rgb(168, 168, 168));
                if(frameNumber % 2 == 0)
                    canvas.drawRect(0, 14, 9, 16, p);
            }
            else if(5 <= frameNumber && frameNumber <= 34){  // windows 98
                canvas.drawColor(Color.BLACK);
                //canvas.drawBitmap(windowsStartupBmp, 0, 40, null);  наш битмап сжат по горизонтали в 2 раза
                dst.set(0, 0, 640, 393);
                canvas.drawBitmap(windowsStartupBmp, null, dst, null);
                int loadingDrawCoord = (32 * (frameNumber - 5)) % 640;
                canvas.drawBitmap(startupLoadingBmp, loadingDrawCoord - 640, 393, null);
                canvas.drawBitmap(startupLoadingBmp, loadingDrawCoord, 393, null);
            }
            else {  // голубой экран с курсором
                drawWallpaper(canvas);
                cursor.draw(canvas, getCursorX(), getCursorY());  // draw cursor
            }
        }
        else if(state == SHUTDOWN){
            if(frameNumber == 0){  // голубой экран с курсором
                drawWallpaper(canvas);
                cursor.draw(canvas, getCursorX(), getCursorY());  // draw cursor
            }
            else if(frameNumber == 1){  // windows 98
                canvas.drawColor(Color.BLACK);
                //canvas.drawBitmap(winShutdownBmp, 0, 40, null);
                dst.set(0, 0, 640, 400);
                canvas.drawBitmap(winShutdownBmp, null, dst, null);
            }
        }
        else {  // WORKING или MS-DOS
            drawWallpaper(canvas);
            drawElements(canvas, 0, 0);
            if(state == WORKING)
                cursor.draw(canvas, getCursorX(), getCursorY());  // draw cursor
        }
    }

    private void drawWallpaper(Canvas canvas){
        //if(wallpaper == null || wallpaperMode == CENTER) - бывают полупрозрачные обои :)
        canvas.drawColor(Color.parseColor("#57A8A8"));  // рисуем голубой (зелёный) фон рабочего стола
        if(wallpaper != null){
            if(wallpaperMode == CENTER)
                canvas.drawBitmap(wallpaper, SCREEN_WIDTH / 2 - wallpaper.getWidth() / 2, SCREEN_HEIGHT / 2 - wallpaper.getHeight() / 2, null);
            else if(wallpaperMode == STRETCH)
                canvas.drawBitmap(wallpaper, 0, 0, null);
            else
                canvas.drawRect(0, 0, SCREEN_WIDTH, state == WORKING? TASKBAR_Y : SCREEN_HEIGHT, wallpaperPaint);
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(state != WORKING)
            return false;
        boolean result = super.onMouseOver(x, y, touch);
        if(targetFocus != null && !targetFocus.hasFocus())
            targetFocus.requestFocusFromTouch();
        return result;
    }

    @Override
    public void onClick(int x, int y) {
        if(state != WORKING)
            return;
        //for(Runnable r : onClickListeners)
        //    r.run();
        //Log.d(TAG, "on win 98 click! startTouch = " + (startTouch instanceof Window? ((Window) startTouch).getTitle() : (startTouch != null? startTouch.getClass().getSimpleName() : "null")));
        super.onClick(x, y);
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        if(touch)
            inputFocus = null;  // чтобы неактивные окна не получали нажатия клавиш
        return true;
    }

    @Override
	public void onRightClick(int x, int y){
		if(state == WORKING)
            super.onRightClick(x, y);
	}

    @Override
    public void onKeyPress(String key) {
        if(state == WORKING || state == MS_DOS_MODE)
            super.onKeyPress(key);
    }

    public void startup(){
        frameNumber = -1;
        state = STARTUP;
        /*final int[] startupBmpIds = {R.drawable.w98_startup, R.drawable.win1, R.drawable.win2, R.drawable.win3, R.drawable.win4, R.drawable.win5, R.drawable.win6,
            R.drawable.win7, R.drawable.win8, R.drawable.win9, R.drawable.win10, R.drawable.win11, R.drawable.win12, R.drawable.win13, R.drawable.win14, R.drawable.win15,
            R.drawable.win16, R.drawable.win17, R.drawable.win18, R.drawable.win19};
        for(int i=0; i<20; i++)
            startupBmps[i] = getBmp(startupBmpIds[i]);*/
        windowsStartupBmp = getBmp(R.drawable.w98_startup);
        startupLoadingBmp = getBmp(R.drawable.win_loading);
        //WindowsView.handler.postDelayed(startupRunnable, 250);
        startupRunnable.run();
    }

    public void shutdown(){
        if(AndroidApps.weAreLauncher() && !restartingToMsDos)  // лаунчер не должен закрываться
            restarting = true;
        state = SHUTDOWN;
        deleteWindows();
        setCursor(new Cursor(getBmp(R.drawable.sandglass)));
        winShutdownBmp = getBmp(R.drawable.w98_shutdown);
        frameNumber = -1;
        shutdown.run();
    }

    public void restart(){
        restarting = true;
        shutdown();
    }


    public void restartInMsDosMode(){
        restartingToMsDos = true;
        shutdown();
    }

    private void startupMsDos(){
        state = MS_DOS_MODE;
        elements.add(Taskbar.taskbar);
        MsDos.createMsDosMode();
    }

    private void loadWindows(){
        updateScreenSize();
        // loading fonts
        Typeface tf = Typeface.createFromAsset(context.getAssets(),"M 8pt.ttf");
        synchronized(Element.p) {
            Element.p.setTypeface(tf);
            //Element.p.setTextSize(8);
            Element.p.setTextSize(11);
            Element.p.setAntiAlias(false);
            Element.p.setDither(false);
            Element.p.setFilterBitmap(false);
            Element.p.setStyle(Paint.Style.FILL);
        }
        AssetManager assets = context.getAssets();
        Typeface tf_bold = Typeface.createFromAsset(assets,"M 8pt bold.ttf");
        Element.p_bold = new Paint();
        Element.p_bold.setTypeface(tf_bold);
        Element.p_bold.setTextSize(11);
        Typeface tf_very_bold = Typeface.createFromAsset(assets,"T 16pt bold.ttf");
        Element.p_very_bold = new Paint();
        Element.p_very_bold.setTypeface(tf_very_bold);
        Element.p_very_bold.setTextSize(21);
        Typeface tf_fixedsys = Typeface.createFromAsset(assets,"Fixedsys 8pt.ttf");
        Element.p_fixedsys = new Paint();
        Element.p_fixedsys.setTypeface(tf_fixedsys);
        Element.p_fixedsys.setTextSize(16);
        Typeface tf_dos = Typeface.createFromAsset(assets,"the_one_true_font_system.ttf");  // 8x12
        Element.p_dos = new Paint();
        Element.p_dos.setTypeface(tf_dos);
        Element.p_dos.setTextSize(16);
        Typeface tf_dos_mode = Typeface.createFromAsset(assets,"Px437_IBM_VGA9.ttf");  // 9x16
        Element.p_dos_mode = new Paint();
        Element.p_dos_mode.setTypeface(tf_dos_mode);
        Element.p_dos_mode.setTextSize(16);
        Typeface tf_system = Typeface.createFromAsset(assets,"System 10pt.ttf");
        Element.p_system = new Paint();
        Element.p_system.setTypeface(tf_system);
        Element.p_system.setTextSize(8);
        // loading some static bitmaps
        ButtonInList.arrowActive = getBmp(R.drawable.arrow_active);
        ButtonInList.arrowInactive = getBmp(R.drawable.arrow_inactive);
        ButtonInList.checkActiveBmp = getBmp(R.drawable.check_active);
        ButtonInList.checkInactive = getBmp(R.drawable.check_inactive);
        ButtonInList.circleActive = getBmp(R.drawable.circle_active);
        ButtonInList.circleInactive = getBmp(R.drawable.circle_inactive);
        defaultCursor = new Cursor(getBmp(R.drawable.cursor));
        // taskbar
        Taskbar taskbar = new Taskbar();
        elements.add(taskbar);
        // loading some sounds
        startupSound = MediaPlayer.create(context, R.raw.the_microsoft_sound);
        startupSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {  // для экономии памяти
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (startupSound.isPlaying())
                    startupSound.stop();
                //deleteMplayer(startupSound);
                startupSound.reset();
                startupSound.release();
                startupSound = null;
            }
        });
        shutdownSound = MediaPlayer.create(context, R.raw.logoff);
        chord = MediaPlayer.create(context, R.raw.chord);

        // Добавляем ярлыки рабочего стола
        links = new DesktopLinks();
        links.bringClickedElementToFront = true;

        final Link myComputer = new Link(0, 0, "My Computer", getBmp(R.drawable.computer_explorer_2), false, MyComputer.class);
        final List<Element> contextMenuButtons = myComputer.contextMenu.child.elements;  // Properties в контекстном меню
        ((ButtonInList) contextMenuButtons.get(contextMenuButtons.size() - 1)).action = parent -> {
            ((ButtonInList) parent).closeMenu();
            StartMenu.createSystemProperties();
        };
        links.elements.add(myComputer);
        boolean narrow = !WIDESCREEN;
        links.elements.add(new Link(1, 0, "Recycle Bin", getBmp(R.drawable.recycle_bin_empty_0), false, RecycleBin.class));
        links.elements.add(new Link(0, 1, "My Documents", getBmp(R.drawable.directory_open_file_mydocs_0), false, MyDocuments.class));
        links.elements.add(new Link(1, 1, "Android (D:)", getBmp(R.drawable.cd_drive_5), false, p -> {
            context.checkWriteExternalPermission(new MainActivity.PermissionResultListener() {
                @Override
                public void onPermissionGranted() {
                    MyDocuments.createDriveD();
                }

                @Override
                public void onPermissionDenied() {
                    MyDocuments.diskNotAccessible(null);
                }
            });
        }));
        links.elements.add(new Link(1, 2, "Windows Media Player", getBmp(R.drawable.media_player_0), true, MPlayer.class));
        links.elements.add(new Link(1, 3, "Paint", getBmp(R.drawable.paint_2), true, PaintBrush.class));
        links.elements.add(new Link(0, 2, "Internet Explorer", getBmp(R.drawable.iexplore_32528_0), false, InternetExplorer.class));
        links.elements.add(new Link(0, 3, "Android Apps", getBmp(R.drawable.android), true, AndroidApps.class));
        links.elements.add(new Link(narrow? 3 : 4, 2, "Minesweeper", getBmp(R.drawable.winmine_bigicon), true, Minesweeper.class));
        links.elements.add(new Link(narrow? 4 : 5, 2, "Spider Solitaire", getBmp(R.drawable.spider_icon), true, Spider.class));
        links.elements.add(new Link(narrow? 3 : 4, 3, "Solitaire", getBmp(R.drawable.game_solitaire_0), true, Solitaire.class));
        links.elements.add(new Link(narrow? 4 : 5, 3, "FreeCell", getBmp(R.drawable.game_freecell_2), true, FreeCell.class));
        links.elements.add(new Link(narrow? 6 : 8, 2, "Notepad", getBmp(R.drawable.notepad_1), true, Notepad.class));
        links.elements.add(new Link(narrow? 7 : 9, 2, "MS-DOS Prompt", getBmp(R.drawable.ms_dos_1), true, MsDos.class));
        links.elements.add(new Link(narrow? 6 : 8, 3, "Calculator", getBmp(R.drawable.calculator_0), true, Calculator.class));
        links.elements.add(new Link(narrow? 7 : 9, 3, "WordPad", getBmp(R.drawable.write_wordpad_1), true, WordPad.class));
        AndroidApps.startInstallMonitoring();
        links.initFromDirectory(MyDocuments.getDesktopDirectory(), null, null);
        elements.add(elements.size() - 1, links);
        links.elements.remove(links.contextMenu);
        inputFocus = links;
        taskbar.elements.add(links.contextMenu);  // чтобы контекстное меню было выше всего остального

        if(context.firstLaunch) {  // первый запуск. Проверяем, есть ли файлы в старой директории файлов
            //Log.d(TAG, "moving files!");
            File oldDir = context.getFilesDir();
            File[] files = oldDir.listFiles();
            File extDir = context.getExternalFilesDir(null);
            if(extDir != null && files != null) {
                String newFolder = extDir.getAbsolutePath() + File.separator;
                for (File src : files) {
                    switch (src.getName()) {  // т. к. быстрее, чем сравнение через equals
                        case "hw_cached_resid.list":
                        case "aware_learning_data":  // появляются на устройствах Huawei
                            break;
                        default:
                            // Internal and external memory are two different file systems. Therefore renameTo() fails
                            // копируем файл, затем удаляем изначальный файл
                            File dst = new File(newFolder + src.getName());
                            try {
                                copyFile(src, dst);
                            }
                            catch (IOException ignored){}
                            src.delete();
                    }
                }
            }
        }

        SharedPreferences sharedPreferences = getSharedPreferences();
        // загружаем обои
        String wallpaperString = sharedPreferences.getString("wallpaper_bmp", null);
        wallpaperMode = sharedPreferences.getInt("wallpaper_mode", TILE);
        wallpaper = getWallpaperBmp(wallpaperString, wallpaperMode, Windows98.SCREEN_WIDTH, Windows98.SCREEN_HEIGHT, wallpaperPaint);
        updateAndroidWallpaper();
        // testing
        //new Calculator();
        //new PaintBrush();
        //new Minesweeper();
        //new Solitaire();
        //ScrollBar scrollBar = new ScrollBar(new Scrollable() {
        //context.runOnUiThread(new Runnable(){public void run(){new InternetExplorer();}});
        //new MessageBox("My Computer", "D:\\ is not accessible.\n\nThe device is not ready.", MessageBox.RETRYCANCEL, MessageBox.ERROR, null, calculator);
        //Link test = new Link("Hello", getBmp(R.drawable.paint_file_0), new OnClickRunnable() {
        //DropdownList dropdownList = new DropdownList(new String[]{"Center", "Tile", "Stretch"}, 1, 75);
        //new InternetExplorer.FileDownloadWindow();
        //new MPlayer();
        //new FreeCell();
        //new Spider();
        WindowsView.windowsView.postInvalidate();  // на всякий случай (если загрузка заняла долгое время???)
    }

    public void updateWallpaper(String wallpaperString, int mode){
        SharedPreferences sharedPreferences = context.settings;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("wallpaper_bmp", wallpaperString);
        editor.putInt("wallpaper_mode", mode);
        editor.apply();
        wallpaperMode = mode;
        wallpaper = getWallpaperBmp(wallpaperString, mode, Windows98.SCREEN_WIDTH, Windows98.SCREEN_HEIGHT, wallpaperPaint);
        updateAndroidWallpaper();
    }

    public static Bitmap getWallpaperBmp(String wallpaperString, int mode, int w, int h, Paint wallpaperPaint) {
        Bitmap wallpaper = getWallpaperBmp(wallpaperString);
        if(wallpaper == null)
            return null;
        if(mode == STRETCH){
            wallpaper = Bitmap.createScaledBitmap(wallpaper, w, h, false);
        }
        else if(w != Windows98.SCREEN_WIDTH || h != Windows98.SCREEN_HEIGHT)  // чтобы в preview был соответствующий масштаб
            wallpaper = Bitmap.createScaledBitmap(wallpaper, wallpaper.getWidth() * w / Windows98.SCREEN_WIDTH, wallpaper.getHeight() * h / Windows98.SCREEN_HEIGHT, false);
        if(mode == TILE)
            wallpaperPaint.setShader(new BitmapShader(wallpaper, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        else
            wallpaperPaint.setShader(null);
        if(mode == CENTER && (wallpaper.getWidth() > w || wallpaper.getHeight() > h)){  // обои слишком больших размеров
            Bitmap newWallpaper = createBitmap(Math.min(wallpaper.getWidth(), w),
                    Math.min(wallpaper.getHeight(), h), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newWallpaper);
            int drawX = newWallpaper.getWidth() / 2 - wallpaper.getWidth() / 2;
            int drawY = newWallpaper.getHeight() / 2 - wallpaper.getHeight() / 2;
            canvas.drawBitmap(wallpaper, drawX, drawY, null);
            wallpaper = newWallpaper;
        }
        return wallpaper;
    }

    private static Bitmap getWallpaperBmp(String wallpaperString){  // "декодировать" строку из SharedPrefernces
        if (wallpaperString == null) {
            return null;
        }
        if (wallpaperString.startsWith("file:")
                || wallpaperString.startsWith("absolute:")) {  // файл в My Documents
            String filename;
            if(wallpaperString.startsWith("file:")) {
                wallpaperString = wallpaperString.substring(5);
                filename = MyDocuments.getFilesDir().getAbsolutePath() + File.separator + wallpaperString;
            }
            else{
                filename = wallpaperString.substring(9);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try {
                return BitmapFactory.decodeFile(filename, options);  // может быть null в случае ошибки
            }
            catch (Exception e){
                return null;
            }
        }
        else {  // картинка из встроенных в винду
            int resId = DisplayProperties.ids[DisplayProperties.imagesList.indexOf(wallpaperString)];
            return getBmp(resId);
        }
    }

    public static void deleteWindows(){
        while(!windows98.elements.isEmpty()){
            Element element = windows98.elements.get(windows98.elements.size() - 1);
            if(element instanceof Window) {
                try {
                    ((Window) element).forceClose();
                }
                catch (Exception ignored) {}
            }
            else {
                element.removeFromParent();
            }
            // нужно, чтобы, например, остановить таймеры в Minesweeper и Solitaire
        }
        WindowsView.handler.removeCallbacksAndMessages(null);
    }

    @SuppressLint("MissingPermission")  // лаунчер отключен из-за нестабильности
    private void updateAndroidWallpaper(){
        if(AndroidApps.weAreLauncher()){
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            Bitmap androidWallpaper = wallpaper;
            if(wallpaper == null) {  // голубой экран
                // создаем bitmap 1x1
                androidWallpaper = Bitmap.createBitmap(new int[]{Color.parseColor("#57A8A8")},
                        0, 1, 1, 1, Bitmap.Config.ARGB_8888);
                androidWallpaper.setDensity(Bitmap.DENSITY_NONE);
            }
            try {
                wallpaperManager.setBitmap(androidWallpaper);
            }
            catch (IOException ignored) {}
        }
    }

    public static ButtonList newMenu(final Explorer.LinkContainer linkContainer){  // Меню New > (Folder, Shortcut...). Используется ещё в Explorer'е
        ButtonList addNew = new ButtonList();
        ButtonInList folder = new ButtonInList("Folder", StartMenu.directoryClosed, false);
        folder.action = parent -> {
            if(linkContainer.getDirectory() == null)
                new MyDocuments();
            else
                linkContainer.createFile("New Folder", "");
        };
        addNew.elements.add(folder);
        addNew.elements.add(new ButtonInList("Shortcut",
                getBmp(R.drawable.overlay_shortcut_0), false));
        addNew.elements.add(new Separator());
        ButtonInList textDocument = new ButtonInList("Text Document", getBmp(R.drawable.notepad_file_1), false);
        textDocument.action = parent -> {
            if(linkContainer.getDirectory() == null)
                new Notepad();
            else
                linkContainer.createFile("New Text Document", ".txt");
        };
        addNew.elements.add(textDocument);
        ButtonInList wordpadDocument = new ButtonInList("WordPad Document",
                getBmp(R.drawable.document_1), false);
        wordpadDocument.action = parent -> {
            if(linkContainer.getDirectory() == null)
                new WordPad();
            else
                linkContainer.createFile("New WordPad Document", ".txt");
        };
        addNew.elements.add(wordpadDocument);
        addNew.elements.add(new ButtonInList("Bitmap Image",
                StartMenu.paint, false, PaintBrush.class));
        addNew.elements.add(new ButtonInList("Wave Sound",
                StartMenu.soundYel, false));
        addNew.elements.add(new ButtonInList("Microsoft Data Link",
                getBmp(R.drawable.msdatalink), false));
        return addNew;
    }

    public static ContextMenu win98contextMenu(final Explorer.LinkContainer linkContainer){
        // контекстное меню рабочего стола
        ButtonList rightMenu = new ButtonList();

        if(linkContainer instanceof DesktopLinks) {
            ButtonList activeDesktop = new ButtonList();
            activeDesktop.elements.add(new ButtonInList("View As Web Page"));
            activeDesktop.elements.add(new ButtonInList("Customize my Desktop..."));
            activeDesktop.elements.add(new ButtonInList("Update Now"));
            rightMenu.elements.add(new ButtonInList("Active Desktop", activeDesktop));
        }
        else{
            ButtonList view = new ButtonList();
            ButtonInList asWebPage = new ButtonInList("as Web Page");
            asWebPage.check = true;
            asWebPage.checkActive = true;
            view.elements.add(asWebPage);
            view.elements.add(new Separator());
            List<ButtonInList> buttonGroup = new ArrayList<>();
            buttonGroup.add(new ButtonInList("Large Icons"));
            buttonGroup.add(new ButtonInList("Small Icons"));
            buttonGroup.add(new ButtonInList("List"));
            buttonGroup.add(new ButtonInList("Details"));
            buttonGroup.get(0).checkActive = true;
            for(ButtonInList button : buttonGroup){
                button.radioButtonGroup = buttonGroup;
                view.elements.add(button);
            }
            rightMenu.elements.add(new ButtonInList("View", view));
            rightMenu.elements.add(new Separator());
            rightMenu.elements.add(new ButtonInList("Customize this Folder..."));
        }
        rightMenu.elements.add(new Separator());

        ButtonList arrangeIcons = new ButtonList();
        arrangeIcons.elements.add(new ButtonInList("by Name"));
        arrangeIcons.elements.add(new ButtonInList("by Type"));
        arrangeIcons.elements.add(new ButtonInList("by Size"));
        arrangeIcons.elements.add(new ButtonInList("by Date"));
        arrangeIcons.elements.add(new Separator());
        arrangeIcons.elements.add(new ButtonInList("Auto Arrange"));
        rightMenu.elements.add(new ButtonInList("Arrange Icons", arrangeIcons));
        rightMenu.elements.add(new ButtonInList("Line Up Icons"));
        rightMenu.elements.add(new Separator());
        ButtonInList refresh = new ButtonInList("Refresh");
        refresh.disabled = true;
        rightMenu.elements.add(refresh);
        ButtonInList paste = new ButtonInList("Paste", parent -> linkContainer.paste());
        rightMenu.elements.add(paste);
        ButtonInList pasteShortcut = new ButtonInList("Paste Shortcut");
        pasteShortcut.disabled = true;
        rightMenu.elements.add(pasteShortcut);
        rightMenu.elements.add(new Separator());
        rightMenu.elements.add(new ButtonInList("New", newMenu(linkContainer)));
        if(!(linkContainer instanceof DesktopLinks))
            rightMenu.elements.add(new Separator());
        ButtonInList properties = new ButtonInList("Properties", parent -> {
            if(linkContainer instanceof DesktopLinks)
                new DisplayProperties();
            else if(linkContainer.getDirectory() != null){
                File directory = linkContainer.getDirectory();
                String name = directory.equals(MyDocuments.getFilesDir())? "My Documents" : directory.getName();
                new FileProperties(name, "File Folder", MyDocuments.getFullPath(directory),
                        InternetExplorer.FileDownloadWindow.bytesToString(directory.length()),
                        getBmp(R.drawable.folder));
            }
        });
        rightMenu.elements.add(properties);
        return new ContextMenu(rightMenu);
    }

    public static class DesktopLinks extends Explorer.LinkContainer {
        private List<Link> programLinks = new ArrayList<>();
        private boolean firstRun;  // при первом создании ярлыков мы берем координаты из sharedPreferences
        private boolean ignoreLinkRepositions = false;
        private Rect selectionArea = new Rect();  // голубая область выделения
        private Rect selectionAreaSorted = new Rect();

        public DesktopLinks(){
            super(new ArrayList<>(), new Rect(0, 0, Windows98.SCREEN_WIDTH, Windows98.TASKBAR_Y), null, WindowsView.canvas_bmp);
        }

        @Override
        public void onWindowResize(boolean maximized) {
            if(ignoreLinkRepositions)
                return;
            for(Element element : elements){
                Link link = (Link) element;
                link.convertToDesktopLink(false);
                if(link.x == 0 && link.y == 0){  // координаты не заданы
                    positionLinkInGrid(link, firstRun);
                }
            }
        }

        @Override
        protected void updateFiles() {
            ignoreLinkRepositions = true;
            elements.removeAll(programLinks);
            super.updateFiles();
            elements.addAll(programLinks);
            ignoreLinkRepositions = false;
            updateLinkPositions();
        }

        @Override
        public void initFromDirectory(File directory, FileProvider fileProvider, Window parentWindow) {
            firstRun = true;
            super.initFromDirectory(directory, fileProvider, parentWindow);
            firstRun = false;
        }

        Point getFirstFreePosition(){
            for(int grid_x = 0;; grid_x++){
                for(int grid_y = 0; grid_y < 6; grid_y++){
                    int x = 21 + 75 * grid_x;
                    int y = 2 + 75 * grid_y;
                    if(checkPosition(x, y))
                        return new Point(x, y);
                }
            }
        }

        boolean checkPosition(int x, int y){  // возвращает true, если на том же месте нет другого ярлыка
            for(Element otherLink : elements){
                if(otherLink.x == x && otherLink.y == y){
                    return false;
                }
            }
            return true;
        }

        public void positionLinkInGrid(Link link, int defaultX, int defaultY, boolean loadFromPreferences){
            int fallbackX = 0, fallbackY = 0;
            if(defaultX != -1) {
                programLinks.add(link);  // положение задается только для ярлыков программ
                fallbackX = 21 + 75 * defaultX;
                fallbackY = 2 + 75 * defaultY;
            }

            link.x = link.y = 0;

            SharedPreferences sharedPreferences = getSharedPreferences();
            if(loadFromPreferences) {
                int newX = sharedPreferences.getInt("Desktop" + link.fullFilename + "x", fallbackX);
                int newY = sharedPreferences.getInt("Desktop" + link.fullFilename + "y", fallbackY);
                if(checkPosition(newX, newY) && newX < Windows98.SCREEN_WIDTH){
                    link.x = newX;
                    link.y = newY;
                }
            }

            if(link.x == 0){
                Point pos = getFirstFreePosition();
                link.x = pos.x;
                link.y = pos.y;
                link.saveDesktopPosition();
            }
        }

        public void positionLinkInGrid(Link link, boolean loadFromPreferences){
            positionLinkInGrid(link, -1, -1, loadFromPreferences);
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            super.onDraw(canvas, x, y);
            if(shouldDrawSelectionArea()) {
                Window.drawNegatedRect(canvas, selectionAreaSorted, 1);
            }
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(!super.onMouseOver(x, y, touch))
                return false;

            if(touch){
                selectionArea.set(x, y, x, y);
                sortSelectionArea();
            }
            else if(shouldDrawSelectionArea()){
                selectionArea.right = x;
                selectionArea.bottom = y;
                sortSelectionArea();

                for(Element element : elements){
                    Link link = (Link) element;
                    // если область выделения пересекается с иконкой, активируем ярлык
                    link.active = selectionAreaSorted.intersects(link.x, link.y, link.x + 32, link.y + 32);
                }
            }
            return true;
        }

        private boolean shouldDrawSelectionArea(){  // если мы нажаты, но не какой-то ярлык
            return isPressed() && this.startTouch == null;
        }

        private void sortSelectionArea(){
            selectionAreaSorted.set(selectionArea);
            selectionAreaSorted.sort();
        }
    }

    public void getSrc(Rect src){  // во время включения/выключения используется только часть (640x400) canvas'а
        // голубой экран с курсором
        if(state == WORKING || (state == STARTUP && frameNumber == 45) || (state == SHUTDOWN && frameNumber == 0))
            src.set(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        // ms-dos
        else if(state == MS_DOS_MODE || (state == STARTUP && (frameNumber <= 4 || (35 <= frameNumber && frameNumber <= 44))))
            src.set(0, 0, 720, 400);
        else  // картинка с windows (включение или выключение)
            src.set(0, 0, 640, 400);
    }

    public Cursor getCursor() {
        return cursor;
    }

    public static void setCursor(Cursor cursor) {
        Windows98.cursor = cursor;
        if(Windows98.TAUON && Build.VERSION.SDK_INT >= 24){
            WindowsView.windowsCursor = cursor.getPointerIcon();
            WindowsView.windowsView.setPointerIcon(WindowsView.windowsCursor);
        }
    }

    public static void setDefaultCursor(){
        setCursor(defaultCursor);
    }

    private static void updateScreenSize(){
        // определяем, широкоэкранный ли экран
        SharedPreferences sharedPreferences = getSharedPreferences();
        final String widescreenKey = "widescreen";

        if(sharedPreferences.contains(widescreenKey))
            WIDESCREEN = sharedPreferences.getBoolean(widescreenKey, true);
        else {
            Point size = new Point();
            MainActivity.getScreenSize(size);
            int width = size.x;
            int height = size.y;
            WIDESCREEN = (float) Math.max(width, height) / Math.min(width, height) >= 1.54;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(widescreenKey, WIDESCREEN);
            editor.apply();
        }

        if(WIDESCREEN){
            SCREEN_WIDTH = 854;  // FWVGA
            SCREEN_HEIGHT = 480;
            TASKBAR_Y = SCREEN_HEIGHT - 28;
            WindowsView.sensitivity = 370;
            WindowsView.accelerationPower = 0.6f;
        }
        else{
            SCREEN_WIDTH = 640;  // VGA
            SCREEN_HEIGHT = 480;
            TASKBAR_Y = SCREEN_HEIGHT - 28;
            WindowsView.sensitivity = 350;
            WindowsView.accelerationPower = 0.5f;
        }
        int targetWidth = Math.max(720, SCREEN_WIDTH);
        if(WindowsView.canvas_bmp == null || WindowsView.canvas_bmp.getWidth() != targetWidth || WindowsView.canvas_bmp.getHeight() != SCREEN_HEIGHT){
            WindowsView.canvas_bmp = createBitmap(targetWidth, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888);
            WindowsView.canvas = new Canvas(WindowsView.canvas_bmp);
        }
    }
}
