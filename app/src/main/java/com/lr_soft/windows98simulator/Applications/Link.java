package com.lr_soft.windows98simulator.Applications;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.ContextMenu;
import com.lr_soft.windows98simulator.System.DropdownList;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.ElementContainer;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.System.ScrollElementContainer;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.Taskbar;
import com.lr_soft.windows98simulator.System.TextBox;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;
import com.lr_soft.windows98simulator.WindowsView;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Link extends ElementContainer implements DropdownList.Item {
    String text;
    private String[] lines, lines_full;  // в первом случае ограничение в 2 строки, в втором ограничений нет
    Bitmap icon, activeIcon;  // icon - обычная иконка; activeIcon - синяя иконка
    String helpText;
    public String fullFilename;  // с расширением файла
    String extension;
    String fullPath; // включая сам файл
    public OnClickRunnable action = null;
    public boolean active = false;
    boolean isFolder;
    public File path = null;  // если не null, то это файл в диалоге сохранения / открытия файла, или в My Documents

    private boolean small = false;  // по умолчанию вид ярлыка "обычный", small - "мелкий"
    private boolean isDesktopLink = false;
    public Rect smallBounds = new Rect(), fullBounds = new Rect();  // в первом случае текст ограничен до 2 строк, во втором - нет
    private Rect textTmp = new Rect();
    private Rect textRect = new Rect(), offsetRect = new Rect();
    private static final int widthLimit = 69;
    private int textDrawX = 16, textDrawY = 47;
    private Window parentWindow = null;
    private TextBox renameTextBox = null;
    private Rect renameTextBoxBounds = null;
    private Runnable renameRunnable = new Runnable() {
        @Override
        public void run() {
            rename();
            renameRunnableRunning = false;
            WindowsView.windowsView.invalidate();
        }
    };
    private boolean renameRunnableRunning = false;
    private static final List<String> knownTypes = Arrays.asList("bat", "icon", "ico", "gif", "bmp", "png", "jpg", "jpeg", "exe", "com", "ttf", "dll", "fon", "inf", "txt", "url", "ani",
            "aac", "flac", "mp3", "ogg", "wav", "wma", "mp4", "3gp", "wmv", "webm", "avi", "mkv", "flv", "mov", "apk");
    // При ошибке переименования создаётся MessageBox, у него в super конструторе вызывается makeActive(),
    // который вызывает onOtherTouch на Explorer, а значит и на Link, Link вызывает applyRename, и возвращаемся к началу...
    private boolean isMessageBoxPresent = false;
    public boolean ignoreOtherTouch = false;
    private Bitmap movingIcon; // иконка для перемещения link
    private boolean isMoving = false;
    private int lastTouchX, lastTouchY;  // для перемещения Link (оно начинается после смещения курсора на 4 пикселя)


    public Link(String text, String helpText, Bitmap icon, OnClickRunnable action){
        this.text = text;
        this.helpText = helpText;
        fullFilename = text;
        this.icon = icon;
        lines = splitTextIntoLines(text, widthLimit, 2, p);
        lines_full = splitTextIntoLines(text, widthLimit, -1, p, true, false);
        this.action = action;
        contextMenu = Link.getContextMenu(action, this, false);  // в таком ярлыке не должно быть properties
        elements.add(contextMenu);
        updateBounds(false);
    }

    public Link(int folder, int file, String fullPath, Window parentWindow){
        this.fullPath = fullPath;
        this.parentWindow = parentWindow;
        String info = Explorer.filesystem[folder][file];
        initialize(info);
        contextMenu = Link.getContextMenu(action, this);
        elements.add(contextMenu);
        updateBounds(false);
    }

    public Link(final File path, final Window parentWindow){
        this.path = path;
        this.parentWindow = parentWindow;
        isFolder = path.isDirectory();
        initialize(path.getName());
        fullPath = MyDocuments.getFullPath(path.isDirectory()? path : path.getParentFile());

        if(path.isDirectory()){
            action = p -> ((Explorer.LinkContainer) parent).changeDirectory(path);
        }
        else if(parentWindow instanceof FileDialog) {
            final FileDialog fileDialog = (FileDialog) parentWindow;
            action = p -> {
                if (fileDialog.open)
                    fileDialog.onOpenPress();
                else
                    fileDialog.onSavePress();
            };
        }
        else {  // MyDocs, FileDownloadWindow, null (Windows 98)
            action = p -> {
                String extension = getExtension(fullFilename);
                if ("txt".equals(extension))
                    new Notepad(path);
                else if (Arrays.asList(PaintBrush.supportedFormats).contains(extension))
                    new PaintBrush(path);
                else if (Arrays.asList(MPlayer.supportedFormats).contains(extension))
                    new MPlayer(path);
                else if ("exe".equals(extension))
                    new MessageBox(fullPath + "\\" + fullFilename, "Win32 applications are not supported. This is a simulator, not an emulator.",
                            MessageBox.OK, MessageBox.ERROR, null, parentWindow);
                else if ("apk".equals(extension)) {
                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Uri contentUri = FileProvider.getUriForFile(context,
                                "com.lr_soft.windows98simulator.fileProvider", path);
                        installIntent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                    }
                    else {
                        installIntent.setDataAndType(Uri.fromFile(path), "application/vnd.android.package-archive");
                    }
                    context.startActivity(installIntent);
                }
            };
        }
        contextMenu = Link.getContextMenu(action, this, true);
        elements.add(contextMenu);
        // x, y = 16, 47; widthLimit = 69 (34 + 35)
        renameTextBox = new TextBox(new Rect(16, 47, 16 + 69, Windows98.SCREEN_HEIGHT), 0, 0, p, new Rect(-1, -11, 0, 2));
        renameTextBox.centerText = true;
        renameTextBox.maxTextLength = 127;
        renameTextBoxBounds = new Rect();
        renameTextBox.parent = this;
        stopRenaming();  // чтобы не мигал курсор
        elements.add(renameTextBox);
        updateBounds(false);
    }

    public static Link createSmallLink(String text, Bitmap icon, OnClickRunnable action){
        return new Link(text, icon, action, 19, 13);
    }


    public static Link createExplorerDropdownLink(String text, Bitmap icon){
        return new Link(text, icon, null, 22, 12);
    }

    private Link(String text, Bitmap icon, OnClickRunnable action, int textDrawX, int textDrawY){  // конструктор для маленького ярлыка
        small = true;
        this.text = text;
        this.icon = icon;
        createActiveIcon();
        this.action = action;
        this.textDrawX = textDrawX;
        this.textDrawY = textDrawY;
        updateBounds(false);
    }

    public Link(String text, String helpText, Bitmap icon, final Class<? extends Window> targetWindow, final Window parentAlignWindow){
        this(text, helpText, icon, parent -> {
            try {
                Window window = targetWindow.newInstance();
                if(parentAlignWindow != null){
                    window.alignWith(parentAlignWindow);
                }
            }
            catch(IllegalAccessException ignore){} catch(InstantiationException ignore){}
        });
    }
    public Link(String text, String helpText, Bitmap icon, Class<? extends Window> targetWindow){
        this(text, helpText, icon, targetWindow, null);
    }
    public Link(int default_x, int default_y, String text, Bitmap icon, boolean addShortcutOverlay, final Class<? extends Window> targetWindow){  // конструктор для ярлыков рабочего стола
        this(default_x, default_y, text, icon, addShortcutOverlay, parent -> {
            try {
                targetWindow.newInstance();
            }
            catch(IllegalAccessException ignored){} catch(InstantiationException ignored){}
        });
    }
    public Link(int default_x, int default_y, String text, Bitmap icon, boolean addShortcutOverlay, OnClickRunnable action) {  // конструктор для ярлыков рабочего стола
        this(text, "", icon, action);
        fullFilename = text + ".lnk";
        convertToDesktopLink(addShortcutOverlay);
        Windows98.windows98.links.positionLinkInGrid(this, default_x, default_y, true);
    }
    public Link(String text, String helpText, Bitmap icon) {
        this(text, helpText, icon, (OnClickRunnable) null);
    }
    public Link(int folder, int file, String fullPath){
        this(folder, file, fullPath, null);
    }

    private void initialize(String info){  // Инициализация ярлыка как файла в Explorer (info - строка из файловой системы в Explorer)
        if(info.contains("/") || isFolder){  // папка
            String[] splitArr = info.split("/");
            text = fullFilename = splitArr[0];
            icon = getBmp(R.drawable.folder);
            helpText = "File Folder";
            isFolder = true;
            if(path == null) {
                final int destinationFolder = Integer.parseInt(splitArr[1]);
                action = parent -> {
                    Explorer parentExplorer = (Explorer) Link.this.parent.parent;
                    Explorer openingFolder;
                    if(destinationFolder != Explorer.DESKTOP_INDEX || parentExplorer.isMsDos)
                        openingFolder = new Explorer(destinationFolder, parentExplorer.isMsDos);
                    else
                        openingFolder = new MyDocuments(MyDocuments.getDesktopDirectory());
                    if (!parentExplorer.isMsDos) {
                        openingFolder.alignWith(parentExplorer);
                    }
                    else{
                        ((MsDos) parentExplorer.parent).explorer = openingFolder;
                    }
                };
            }
        }
        else{  // файл
            fullFilename = info;
            extension = "";  // расширение (extension)
            if(!info.contains(".")){
                text = info;
            }
            else{
                //String[] splitArr = info.split("\\.");
                int dotIndex = info.lastIndexOf('.');
                extension = info.substring(dotIndex + 1).toLowerCase();
                text = getSimpleFilename();
            }
            switch(extension) {
                case "bat":
                    setIcon(R.drawable.bat);
                    helpText = "MS-DOS Batch File";
                    action = parent -> new MsDos();
                    break;
                case "icon": case "ico": case "gif": case "bmp": case "png": case "jpg": case "jpeg":
                    setIcon(R.drawable.paint_2);
                    helpText = "Bitmap Image";
                    action = parent -> new PaintBrush(text);
                    break;
                case "exe": case "com":
                    setIcon(R.drawable.com_exe);
                    helpText = extension.equals("exe") ? "Application" : "MS-DOS Application";
                    if (text.equals("Mspaint"))
                        setIcon(R.drawable.paint_2);
                    else if (text.equals("Wordpad"))
                        setIcon(R.drawable.write_wordpad_1);
                    else if (text.equals("Iexplore"))
                        setIcon(R.drawable.iexplore_32528_0);
                    else if (text.equals("Mplayer2"))
                        setIcon(R.drawable.mplayer2_110_0);
                    else if (text.equals("Msimn"))
                        setIcon(R.drawable.outlook_express_0);

                    if (extension.equals("exe")) {
                        action = parent -> {
                            if (text.equals("Mspaint"))
                                new PaintBrush();
                            else if (text.equals("Wordpad"))
                                new WordPad();
                            else if (text.equals("Iexplore"))
                                new InternetExplorer();
                            else if (text.equals("Mplayer2"))
                                new MPlayer();
                            else if (text.equals("Msimn"))
                                StartMenu.createOutlookExpress();
                        };
                    } else {// if (ext.equals("com")) {
                        action = parent -> new MsDos();
                    }
                    break;
                case "apk":
                    setIcon(R.drawable.android);
                    helpText = "Android Package";
                    break;
                case "ttf":
                    setIcon(R.drawable.ttf);
                    helpText = "TrueType Font file";
                    break;
                case "dll":
                    setIcon(R.drawable.file_gears_0);
                    helpText = "Application Extension";
                    break;
                case "fon":
                    setIcon(R.drawable.fon);
                    helpText = "Font file";
                    break;
                case "inf": case "txt":
                    if (extension.equals("txt")) {
                        setIcon(R.drawable.txt);
                        helpText = "Text Document";
                    } else {
                        setIcon(R.drawable.inf);
                        helpText = "Setup Information";
                    }
                    action = parent -> new Notepad(text);
                    break;
                case "url":
                    setIcon(R.drawable.url1_0);
                    helpText = "Internet Shortcut";
                    action = parent -> new InternetExplorer();
                    break;
                case "ani":
                    setIcon(R.drawable.other_file);
                    helpText = "Animated Cursor";
                    break;
                case "wav":
                    setIcon(R.drawable.sound_yel_2);
                    helpText = "Wave Sound";
                    action = parent -> new MPlayer();
                    break;
                case "aac": case "flac": case "mp3": case "ogg": case "wma":
                    setIcon(R.drawable.video_tl_1);
                    helpText = extension.toUpperCase() + " File";
                    action = parent -> new MPlayer();
                    break;
                case "mp4": case "3gp": case "wmv": case "webm": case "avi": case "mkv": case "flv": case "mov":
                    setIcon(R.drawable.video_1);
                    helpText = "Video Clip";
                    action = parent -> new MPlayer();
                    break;
                default:
                    setIcon(R.drawable.other_file);
                    text = fullFilename;
                    if (!extension.isEmpty())
                        helpText = extension.toUpperCase() + " File";
                    else
                        helpText = "File";
                    break;
            }
        }
        lines = splitTextIntoLines(text, widthLimit, 2, p);
        lines_full = splitTextIntoLines(text, widthLimit, -1, p, true, false);
    }

    private void setIcon(int resourceId){
        if(!(parentWindow instanceof Explorer && ((Explorer) parentWindow).isMsDos))
            icon = getBmp(resourceId);
    }

    private static Bitmap addShortcutOverlay(Bitmap bmp){
        Bitmap result = bmp.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(getBmp(R.drawable.overlay_shortcut_1), 0, 0, null);
        return result;
    }

    private void createActiveIcon(){
        //long start = System.currentTimeMillis();
        activeIcon = createIcon(Color.parseColor("#0000A8"));
        //long end = System.currentTimeMillis();
        //Log.d(TAG, "active icon creation took " + (end - start) + " ms");
    }

    private void createSemiTransparentIcon(){
        movingIcon = createIcon(Color.TRANSPARENT);
    }

    private Bitmap createIcon(int color){  // сделать dither с цветом color
        if(icon == null)  // для MS-DOS
            return null;
        Bitmap newIcon = icon.copy(Bitmap.Config.ARGB_8888, true);
        for(int i = 0; i < icon.getWidth(); i++){
            for(int j = 0; j < icon.getHeight(); j++){
                if((i + j) % 2 == 0) {
                    if(icon.getPixel(i, j) != Color.TRANSPARENT)
                        newIcon.setPixel(i, j, color);
                }
            }
        }
        return newIcon;
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        if(active && activeIcon == null)  // ленивая инициализация
            createActiveIcon();
        canvas.drawBitmap(active? activeIcon : icon, x, y, null);
        if(!isRenaming()) {  // если не переименовываем - рисуем текст
            drawText(active ? this.lines_full : this.lines, canvas,
                    x + textDrawX, y + textDrawY);
        }
        if(contextMenu != null && parent.parent != Windows98.windows98)
            contextMenu.onDraw(canvas, x + contextMenu.x, y + contextMenu.y);
        if(isRenaming()){
            if(parent instanceof Explorer.LinkContainer)
                renameTextBox.drawingBitmap = ((Explorer.LinkContainer) parent).drawingBitmap;
            offsetRect.set(renameTextBoxBounds);
            offsetRect.offset(x, y);
            p.setColor(Color.BLACK);
            canvas.drawRect(offsetRect, p);
            p.setColor(Color.WHITE);  // WHITE
            offsetRect.left += 1; offsetRect.top += 1;
            offsetRect.right -= 1; offsetRect.bottom -= 1;
            canvas.drawRect(offsetRect, p);
            renameTextBox.onDraw(canvas, x + renameTextBox.x, y + renameTextBox.y);
        }
        if(isMoving){
            x += WindowsView.cursor_x - (lastTouchX + getAbsoluteX());
            y += WindowsView.cursor_y - (lastTouchY + getAbsoluteY());
            canvas.drawBitmap(movingIcon, x, y, null);
            p.setColor(Color.BLACK);
            drawMultilineText(canvas, lines_full, x + 16, y + 47, 13, p, true);
        }
    }

    // так как drawText - медленная операция, кешируем результат в битмапе
    private String[] lastTextDrawn;
    private boolean lastActive;
    private Bitmap drawTextCacheBmp;
    private Canvas drawTextCacheCanvas;
    private int cacheTextX, cacheTextY;

    private void drawText(String[] lines, Canvas canvas, int x, int y){
        // рисует текст и, если надо, синюю рамку вокруг него
        if(lines != lastTextDrawn || active != lastActive || drawTextCacheBmp == null) {  // надо обновить кеш
            lastTextDrawn = lines;
            lastActive = active;
            computeTextBounds(lines, textRect);
            cacheTextX = -textRect.left;
            cacheTextY = -textRect.top;
            updateDrawTextCacheSize(textRect.width(), textRect.height());
            textRect.offsetTo(0, 0);
            if (active) {
                drawBlueRectWithYellowBorder(drawTextCacheCanvas, textRect, false);
            }
            else if (parent.parent == Windows98.windows98) {
                p.setColor(Color.parseColor("#57A8A8"));  // голубой (зелёный) фон рабочего стола, на случай если мы поменяли обои
                drawTextCacheCanvas.drawRect(textRect, p);
            }

            if (active || isDesktopLink)
                p.setColor(Color.WHITE);
            else
                p.setColor(Color.BLACK);

            if (small)
                drawTextCacheCanvas.drawText(text, cacheTextX, cacheTextY, p);
            else
                drawMultilineText(drawTextCacheCanvas, lines, cacheTextX, cacheTextY, 13, p, true);
        }
        // используем кешированный результат
        canvas.drawBitmap(drawTextCacheBmp, x - cacheTextX, y - cacheTextY, null);
    }

    private void updateDrawTextCacheSize(int width, int height){
        if(drawTextCacheBmp != null && drawTextCacheBmp.getWidth() >= width && drawTextCacheBmp.getHeight() >= height) {
            drawTextCacheCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            return;
        }
        drawTextCacheBmp = createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawTextCacheCanvas = new Canvas(drawTextCacheBmp);
    }

    private void computeTextBounds(String[] lines, Rect bounds){
        if(small){
            bounds.left = -3;
            bounds.top = -textDrawY;
            bounds.right = (int) p.measureText(text) + 3;
            bounds.bottom = 4;
            return;
        }
        bounds.set(0, 0, 0, 0);
        int cur_y = 0;
        for(String line : lines) {
            int width = (int) p.measureText(line);
            p.getTextBounds(line, 0, line.length(), textTmp);
            textTmp.left = 0; textTmp.right = width;  // ужос, оба метода (measureText, getTextBounds) дают неточные результаты
            //int width = textTmp.width();
            textTmp.offset(-(width / 2), cur_y);
            if(textTmp.top == textTmp.bottom)  // т. к. union делает проверку (left < right) && (top < bottom)
                textTmp.top--;
            bounds.union(textTmp);
            cur_y += 13;
        }
        bounds.left -= 3;
        bounds.top -= 3;
        bounds.right += 2;
        bounds.bottom += 3;
        bounds.top = Math.min(bounds.top, bounds != renameTextBoxBounds? -10 : -13);
        bounds.bottom = Math.max(bounds.bottom, 13 * (lines.length - 1) + 4);  // чтобы помещался курсор
        bounds.left = Math.min(-4, bounds.left);
        bounds.right = Math.max(3, bounds.right);
    }

    private void updateBounds(){ updateBounds(true); }

    private void updateBounds(boolean callUpdateParent){
        if(small){
            fullBounds.left = 0;
            fullBounds.top = 0;
            fullBounds.right = textDrawX + (int) p.measureText(text) + 3;
            fullBounds.bottom = 17;
            smallBounds = fullBounds;
            return;
        }
        updateBounds(smallBounds, lines);
        updateBounds(fullBounds, lines_full);
        if(callUpdateParent){
            if(parent instanceof Explorer.LinkContainer) {
                ((Explorer.LinkContainer) parent).updateLinkPositions();
            }
        }
    }

    private void updateBounds(Rect bounds, String[] lines){
        computeTextBounds(active? lines_full : lines, bounds);
        bounds.offset(16, 47);
        if(isRenaming())
            bounds.union(renameTextBoxBounds);
        bounds.union(0, 0, 32, 32);  // вычисление bounds
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(isRenaming()){
             // т. к. x, y, width, height у renameTextBox неправильные
            renameTextBox.visible = renameTextBoxBounds.contains(x, y);
            boolean returnValue = super.onMouseOver(x, y, touch);
            renameTextBox.visible = true;
            if(touch && !renameTextBoxBounds.contains(x, y) && renameTextBox.visible) {
                applyRename();
                if(isMessageBoxPresent)
                    return true;
            }
            return returnValue;
        }
        else if(isDesktopLink){  // перемещение ярлыка
            if(isMoving)
                return true;
            else if(!super.onMouseOver(x, y, touch))
                return false;
            if(movingIcon == null)
                return true;

            if(touch){
                lastTouchX = x;
                lastTouchY = y;
            }
            else if(isPressed() && !isMoving){
                if((x - lastTouchX) * (x - lastTouchX) + (y - lastTouchY) * (y - lastTouchY) > 16) {
                    isMoving = true;
                    Windows98.movingWindow = true;
                    stopRenameRunnable();
                }
            }
            return true;
        }
        return super.onMouseOver(x, y, touch);
    }

    @Override
    public void onOtherTouch() {
        if(parent.parent == Windows98.windows98 && contextMenu != null && Taskbar.taskbar.startTouch == contextMenu)  // на самом деле мы нажали на contextMenu
            return;
        super.onOtherTouch();
        if(isRenaming()) {
            applyRename();
        }
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        Rect bounds = active? fullBounds : smallBounds;
        if(!bounds.contains(x, y))
            return false;
        if(touch) {
            makeActive();
            if(isRenaming()) {
                applyRename();
            }
        }
        return true;
    }

    @Override
    public void onSelfOtherTouch() {
        if(ignoreOtherTouch)
            return;
        active = false;
        stopRenameRunnable();
    }

    @Override
    public void onSelfMouseLeave() {
        if(isMoving) {
            isMoving = false;
            Windows98.movingWindow = false;
        }
    }

    @Override
    public void onSelfDoubleClick(int x, int y) {
        if(action != null) {
            active = false;
            action.run(this);
            if(parent.topElement == this)
                parent.topElement = null;
            stopRenameRunnable();
        }
    }

    @Override
    public void onSelfRightClick(int x, int y) {
        makeActive();  // чтобы selected = true и появилась надпись в Explorer'е
        if(contextMenu == null)
            return;
        if(parent.parent != null && parent.parent == Windows98.windows98) {  // наше контекстное меню - в таскбаре
            contextMenu.reset();
            contextMenu.x = x + this.x;
            contextMenu.y = y + this.y;
            contextMenu.active = true;
        }
        else
            super.onSelfRightClick(x, y);
    }

    @Override
    public void onClick(int x, int y) {
        super.onClick(x, y);
        if(isMoving){
            isMoving = false;
            Windows98.movingWindow = false;
            // делаем "snap to grid"
            int new_x = this.x + x - lastTouchX, new_y = this.y + y - lastTouchY;
            int xpos = Math.round((new_x - 21) / 75f);  // координаты ярлыка "в сетке"
            int ypos = Math.round((new_y - 2) / 75f);
            xpos = Math.max(xpos, 0);
            ypos = Math.min(Math.max(ypos, 0), 5);
            new_x = 21 + 75 * xpos;
            new_y = 2 + 75 * ypos;
            for(Element link : parent.elements){
                if(link.x == new_x && link.y == new_y){
                    return;  // уже есть ярлык на этом месте
                }
            }
            this.x = new_x;
            this.y = new_y;
            saveDesktopPosition();
        }
        if(lastClickTime != -1){  // то есть это не первый клик
            if(isRenaming())
                return;
            computeTextBounds(active? lines_full : lines, textRect);
            textRect.offset(16, 47);
            if(textRect.contains(x, y)){
                startRenameRunnable();
            }
        }
    }

    public void makeActive(){
        if(parent instanceof Explorer.LinkContainer){
            for(Element lnk : parent.elements)
                ((Link) lnk).active = false;
            parent.topElement = this;
        }
        active = true;
        if(parent != null)
            parent.inputFocus = this;
        scrollToSelf();
        if(parent.parent instanceof Explorer){
            Explorer explorer = (Explorer) parent.parent;
            explorer.curFilename = fullFilename;
            explorer.curFilenameLines = null;
            explorer.helpText = helpText;
            explorer.helpTextLines = null;
        }
        else if(parentWindow != null && parentWindow instanceof FileDialog){
            FileDialog fileDialog = (FileDialog) parentWindow;
            fileDialog.textBox.setText(fullFilename);
            fileDialog.textBox.moveCursorToEnd();
        }
    }

    private void scrollToSelf(){
        if(parent instanceof ScrollElementContainer && !(parent instanceof Windows98.DesktopLinks)){  // скроллим его так, чтобы нас было видно
            Rect bounds = active? fullBounds : smallBounds;
            ScrollElementContainer scrollArea = (ScrollElementContainer) parent;
            offsetRect.set(bounds);
            offsetRect.offset(this.x, this.y - scrollArea.scrollY);
            if(offsetRect.bottom > scrollArea.height){  // в первую очередь - нижняя граница, т. к. набор текста
                scrollArea.scrollY = this.y + bounds.bottom - scrollArea.height;
                scrollArea.updateScrollBar();
            }
            else if(offsetRect.top < 0){
                if(bounds.height() <= scrollArea.height)
                    scrollArea.scrollY = this.y;
                else  // приоритет - нижняя граница
                    scrollArea.scrollY = this.y + bounds.bottom - scrollArea.height;
                scrollArea.updateScrollBar();
            }
        }
    }

    void rename(){  // начать переименовывание
        if(renameTextBox != null){
            if(renameTextBox.visible)
                return;
            renameTextBox.visible = true;
            if(isKnownFileType())  // не даём пользователю менять расширение файла
                renameTextBox.setText(getSimpleFilename());
            else
                renameTextBox.setText(fullFilename);
            renameTextBox.selectAll();
            renameTextBox.setActive(true);
            updateRenameTextBox();
            renameTextBox.requestInputFocus();
        }
        else if(isInFolder())
            makeSnackbar(R.string.unable_to_rename, Snackbar.LENGTH_LONG);
    }

    public boolean isRenaming(){
        return renameTextBox != null && renameTextBox.visible;
    }

    public void stopRenaming(){
        renameTextBox.visible = false;
        renameTextBox.setActive(false);
    }

    private void updateRenameTextBox(){  // вызывается, когда текст в renameTextBox обновился
        computeTextBounds(renameTextBox.lines, renameTextBoxBounds);
        renameTextBoxBounds.offset(16, 47);
        updateBounds();
        scrollToSelf();
    }

    private boolean applyRename(){  // возвращает создался ли новый ярлык (было ли переименование успешным)
        //if(parentWindow.childMessagebox != null)
        //    return;
        if(isMessageBoxPresent)
            return false;
        String newFilename = renameTextBox.text.trim();
        if(isKnownFileType())
            newFilename += "." + extension;
        if(newFilename.equals(fullFilename)) {
            stopRenaming();
            return false;
        }
        MessageBox.MsgResultListener msgListener = new MessageBox.MsgResultListener() {
            @Override
            public void onMsgResult(int buttonNumber) {
                stopRenaming();
                isMessageBoxPresent = false;
            }
        };
        if(newFilename.isEmpty() || newFilename.startsWith(".")){
            isMessageBoxPresent = true;
            new MessageBox("Rename", "You must type a filename.", MessageBox.OK, MessageBox.ERROR, msgListener, parentWindow);
            return false;
        }
        else if(!FileDialog.checkFilename(newFilename)){
            // Здесь MessageBox отображает "центрованный текст"
            // "A filename cannot contain any of the following characters:\n\t \\ / :  * ? "" < > | "
            isMessageBoxPresent = true;
            new MessageBox("Rename", "A filename cannot contain any of the following characters:\n              \\ / :  * ? \" < > |",
                    MessageBox.OK, MessageBox.ERROR, msgListener, parentWindow);
            return false;
        }
        File newPath = new File(path.getParent() + File.separator + newFilename);
        if(newPath.exists() && !(newFilename.equalsIgnoreCase(fullFilename))){
            isMessageBoxPresent = true;
            new MessageBox("Error Renaming File",
                    "Cannot rename " + getSimpleFilename() + ": A file with the name you specified already exists. Specify a different filename.",
                    MessageBox.OK, MessageBox.ERROR, msgListener, parentWindow);
            return false;
        }
        stopRenaming();
        int index = parent.elements.indexOf(this);  // чтобы новый ярлык был в том же месте
        removeFromParent();
        path.renameTo(newPath);
        Link newLink = new Link(newPath, parentWindow);
        newLink.x = this.x; newLink.y = this.y;
        newLink.parent = this.parent;
        newLink.makeActive();
        parent.topElement = newLink;  // надо, т. к. если один элемент вернул true в onMouseOver, то все остальные получат onOtherTouch (включая новый Link)
        parent.elements.add(index, newLink);
        if(isDesktopLink) {
            newLink.convertToDesktopLink(false);
            newLink.saveDesktopPosition();
        }
        return true;
    }

    private void startRenameRunnable(){
        if(renameRunnableRunning)
            return;
        renameRunnableRunning = true;
        WindowsView.handler.postDelayed(renameRunnable, 500);
    }

    private void stopRenameRunnable(){
        if(!renameRunnableRunning)
            return;
        renameRunnableRunning = false;
        WindowsView.handler.removeCallbacks(renameRunnable);
    }

    @Override
    public void onKeyPress(String key) {
        if(key.equals("\n")){
            if(isRenaming())
                applyRename();
            return;
        }
        super.onKeyPress(key);
        if(isRenaming()) {
            updateRenameTextBox();
        }
    }

    private static ContextMenu getContextMenu(OnClickRunnable openAction, Link lnk){ return getContextMenu(openAction, lnk, true); }
    private static ContextMenu getContextMenu(final OnClickRunnable openAction, final Link lnk, boolean addPropertiesMenu){
        ButtonList rightMenu = new ButtonList();
        ButtonInList open = new ButtonInList("Open");
        open.textBold = true;
        open.action = parent -> {
            if(openAction != null) {
                openAction.run(parent);
                lnk.active = false;
                if(lnk.parent.topElement == lnk)
                    lnk.parent.topElement = null;
                lnk.stopRenameRunnable();
            }
        };
        rightMenu.elements.add(open);
        rightMenu.elements.add(new Separator());
        rightMenu.elements.add(new ButtonInList("Send To..."));
        rightMenu.elements.add(new Separator());
        rightMenu.elements.add(new ButtonInList("Cut"));
        rightMenu.elements.add(new ButtonInList("Copy", parent -> lnk.copy()));
        rightMenu.elements.add(new Separator());
        rightMenu.elements.add(new ButtonInList("Create Shortcut"));
        ButtonInList delete = new ButtonInList("Delete");
        delete.action = parent -> lnk.delete();
        rightMenu.elements.add(delete);
        final ButtonInList rename = new ButtonInList("Rename");
        rename.action = parent -> lnk.rename();
        rightMenu.elements.add(rename);
        rightMenu.elements.add(new Separator());
        ButtonInList properties = new ButtonInList("Properties");
        if(addPropertiesMenu){
            properties.action = parent -> new FileProperties(lnk);
        }
        rightMenu.elements.add(properties);
        return new ContextMenu(rightMenu);
    }

    public void delete(){
        if(path == null) {
            if(isInFolder()) {
                if(text.equals("Windows") || text.equals("Program Files")){
                    WindowsView.windowsView.slowBSOD();
                }
                else
                    makeSnackbar(R.string.unable_to_delete, Snackbar.LENGTH_LONG);
            }
            else if(parent.parent instanceof AndroidApps){
                ((AndroidApps) parent.parent).deleteApp(this);
            }
            return;
        }
        else if(!path.exists()){  // у нас старая версия файлов, очень жаль
            removeFromParent();
            ((Explorer.LinkContainer) parent).updateLinkPositions();
            return;
        }
        new MessageBox("Confirm File Delete", "Are you sure you want to delete '" + getSimpleFilename() + "'?",
                MessageBox.YESNO, getBmp(R.drawable.delete_file), new MessageBox.MsgResultListener() {
            @Override
            public void onMsgResult(int buttonNumber) {
                if(buttonNumber == YES) {
                    deleteFile(path);
                    removeFromParent();
                    ((Explorer.LinkContainer) parent).updateLinkPositions();
                }
            }
        }, parentWindow);
    }

    private static void deleteFile(File file){
        if(file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles != null) {
                for (File sub : subFiles)
                    deleteFile(sub);
            }
        }
        file.delete();
    }

    void copy(){
        if(path != null && path.exists())
            Explorer.clipboard = path;
    }

    public void saveDesktopPosition(){
        SharedPreferences sharedPreferences = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("Desktop" + fullFilename + "x", this.x);
        editor.putInt("Desktop" + fullFilename + "y", this.y);
        editor.apply();
    }

    private void deleteDesktopPosition(){
        SharedPreferences sharedPreferences = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("Desktop" + fullFilename + "x");
        editor.remove("Desktop" + fullFilename + "y");
        editor.apply();
    }

    private boolean isInFolder(){  // надо ли показывать toast при попытке удаления файла
        return (parent.parent instanceof Explorer) && ((Explorer) parent.parent).isFolder;
    }

    public static String getExtension(String filename){
        int index = filename.lastIndexOf('.');
        if(index == -1)
            return "";
        return filename.substring(index + 1).toLowerCase();
    }

    private boolean isKnownFileType(){
        return isKnownFileType(extension);
    }

    private static boolean isKnownFileType(String extension){
        return knownTypes.contains(extension);
    }

    private String getSimpleFilename(){
        return getSimpleFilename(fullFilename);
    }

    public static String getSimpleFilename(String filename){  // убираем расширение файла, если оно известно
        String extension = getExtension(filename);
        if(isKnownFileType(extension))
            return filename.substring(0, filename.length() - extension.length() - 1);
        else
            return filename;
    }

    public void convertToDesktopLink(boolean addShortcutOverlay){
        if(isDesktopLink)
            return;
        isDesktopLink = true;
        if(fullFilename == null)
            fullFilename = text + ".lnk";
        elements.remove(contextMenu);  // контекстное меню будет в taskbar, так как иначе оно будет перекрываться окнами
        Taskbar.taskbar.elements.add(contextMenu);
        createActiveIcon();  // для addShortcutOverlay
        createSemiTransparentIcon();
        if(addShortcutOverlay){
            this.icon = addShortcutOverlay(this.icon);
            activeIcon = addShortcutOverlay(activeIcon);
            movingIcon = addShortcutOverlay(movingIcon);
        }
    }

    @Override
    public void removeFromParent() {
        super.removeFromParent();
        if(isDesktopLink)
            deleteDesktopPosition();
    }

    @Override
    public void prepareForDelete() {
        if(parent != null && parent.parent == Windows98.windows98) {
            Taskbar.taskbar.elements.remove(contextMenu);
            contextMenu.prepareForDelete();
        }
        stopRenameRunnable();
        super.prepareForDelete();
    }

    // Реализация интерфейса DropdownList.Item
    @Override
    public void onDraw(Canvas canvas, int x, int y, int width, boolean active, boolean isTitle, boolean disabled) {
        assertSmallLink();
        this.active = active;
        onDraw(canvas, x + 3, y);
    }

    @Override
    public int getHeight(boolean isTitle) {
        assertSmallLink();
        return 16;
    }

    private void assertSmallLink(){
        if(!small)
            throw new IllegalStateException("Link should be small");
    }
}
