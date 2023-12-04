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
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.DropdownList;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.System.ScrollBar;
import com.lr_soft.windows98simulator.System.ScrollElementContainer;
import com.lr_soft.windows98simulator.System.Windows98;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DisplayProperties extends TabControlWindow {
    public static final int[] ids = {R.drawable.firstboot, R.drawable.blackthatch, R.drawable.bluerivets,
            R.drawable.bubbles, R.drawable.carvedstone, R.drawable.circles, R.drawable.houndstooth,
            R.drawable.pinstripe, R.drawable.setup, R.drawable.strawmat, R.drawable.tiles, R.drawable.triangles, R.drawable.waves};
    // resource ids могут меняться от билда к билду! поэтому мы для идентификации стандартной картинки используем её название
    private static final String[] images = {"1stboot", "Black Thatch", "Blue Rivets", "Bubbles", "Carved Stone",
            "Circles", "Houndstooth", "Pinstripe", "Setup", "Straw Mat", "Tiles", "Triangles", "Waves"};
    public static List<String> imagesList = Arrays.asList(images);
    private ScrollElementContainer links;
    private DropdownList wallpaperMode;
    private Button apply, cancel, advanced;
    private Bitmap curWallpaper = null;  // тот, что в preview
    private Paint wallpaperPaint = new Paint();
    private boolean widescreen = Windows98.WIDESCREEN;
    private boolean screenAreaThumbPressed = false;
    private Bitmap thumb = getBmp(R.drawable.screen_area_thumb), thumbPressed = getBmp(R.drawable.screen_area_thumb_pressed);

    public DisplayProperties(){
        super("Display Properties", new Bitmap[]{getBmp(R.drawable.display_props_1), getBmp(R.drawable.display_props_2),
                        getBmp(R.drawable.display_props_3), getBmp(R.drawable.display_props_4), getBmp(R.drawable.display_props_5), getBmp(R.drawable.display_props_6)},
                new int[]{11, 81, 158, 228, 273, 315, 368},
                new Rect(158, 415, 233, 438), new Rect(239, 415, 314, 438));

        apply = new Button("Apply", new Rect(320, 415, 395, 438), parent -> {
            apply();
            apply.disabled = true;
        });
        apply.disabled = true;
        addElement(apply);
        // defaultButton = OK
        defaultButton.action = parent -> {
            if(!apply.disabled)
                apply();
            close();
        };
        for(Element element : elements){
            if(element instanceof Button && ((Button) element).text.equals("Cancel")) {
                cancel = (Button) element;
                break;
            }
        }

        SharedPreferences sharedPreferences = getSharedPreferences();
        wallpaperMode = new DropdownList(new String[]{"Center", "Tile", "Stretch"}, sharedPreferences.getInt("wallpaper_mode", 1), 75);
        wallpaperMode.x = 294; wallpaperMode.y = 361;
        addElement(wallpaperMode);
        String wallpaperString = sharedPreferences.getString("wallpaper_bmp", null);
        boolean isStandartImage = wallpaperString != null && !wallpaperString.startsWith("file:");
        if(isStandartImage) {
            curWallpaper = Windows98.getWallpaperBmp(wallpaperString, wallpaperMode.selectedItem, 152, 112, wallpaperPaint);
        }

        final Bitmap paintFile = getBmp(R.drawable.paint_file_0);
        links = new ScrollElementContainer(new Rect(36, 287, 265, 379), 17);
        ScrollBar scrollBar = new ScrollBar(links, new Rect(265, 287, 281, 379), true);
        links.scrollBar = scrollBar;
        addElement(scrollBar);
        final OnClickRunnable action = parent -> ((Link) parent).active = true;  // чтобы после двойного клика ярлык не становился серым
        Link noneLink = Link.createSmallLink("(None)", getBmp(R.drawable.none), action);
        noneLink.ignoreOtherTouch = true;
        links.elements.add(noneLink);
        if(wallpaperString == null) {  // пустые обои
            noneLink.parent = links;
            noneLink.makeActive();
            wallpaperMode.disabled = true;
        }

        for (String image : images) {
            Link lnk = Link.createSmallLink(image, paintFile, action);
            lnk.ignoreOtherTouch = true;
            links.elements.add(lnk);
            lnk.parent = links;
            if (isStandartImage && image.equals(wallpaperString)) {
                lnk.makeActive();
            }
        }

        if(wallpaperString != null && (wallpaperString.startsWith("file:") || wallpaperString.startsWith("absolute:"))){
            // это обои из файла - добавляем соответствующий ярлык
            String fullFilename = wallpaperString;
            if(fullFilename.contains(File.separator))
                fullFilename = fullFilename.substring(fullFilename.lastIndexOf(File.separator) + 1);
            String name = fullFilename.substring(0, fullFilename.lastIndexOf('.'));
            Link lnk = Link.createSmallLink(name, paintFile, action);
            lnk.fullFilename = fullFilename;
            lnk.fullPath = wallpaperString;
            lnk.parent = links;
            lnk.ignoreOtherTouch = true;
            links.elements.add(lnk);
            for(Element el : links.elements)
                ((Link) el).active = false;
            lnk.makeActive();
            updatePreview();
            wallpaperMode.disabled = false;
        }

        positionLinks();
        ((Link) links.inputFocus).makeActive();
        addElement(links);

        Button browse = new Button("Browse", new Rect(294, 285, 369, 308), parent -> {
            FileDialog fileDialog = new FileDialog(true, PaintBrush.supportedFormats,
                    "Background Files", DisplayProperties.this, new FileDialog.OnResultListener() {
                @Override
                public void openFile(File file) {  // добавляем новый ярлык в список
                    String fullFilename = file.getName();
                    String name = fullFilename.substring(0, fullFilename.lastIndexOf('.'));
                    Link lnk = Link.createSmallLink(name, paintFile, action);
                    // используем fullPath для того, чтобы записать путь до файла
                    if(MyDocuments.isInFilesDir(file))
                        lnk.fullPath = "file:" + MyDocuments.getRelativePath(file);
                    else
                        lnk.fullPath = "absolute:" + file.getAbsolutePath();
                    lnk.fullFilename = fullFilename;
                    lnk.parent = links;
                    lnk.ignoreOtherTouch = true;
                    links.elements.add(lnk);
                    for(Element el : links.elements)
                        ((Link) el).active = false;
                    positionLinks();
                    lnk.makeActive();
                    updatePreview();
                    wallpaperMode.disabled = false;
                    apply.disabled = false;
                }
            });
            if(fileDialog.linkContainer.elements.isEmpty()) {
                makeSnackbar(R.string.wallpaper_no_files, 5000);
            }
        });
        addElement(browse);

        advanced = new Button("Advanced", new Rect(301, 376, 379, 399), null);  // для другой страницы
        advanced.visible = false;
        addElement(advanced);
    }

    private void checkElementsVisibility(){
        for(int i = 0; i < elements.size() - pos - 1; i++) {
            Element element = elements.get(i);
            if(element == defaultButton || element == cancel || element == apply)
                continue;
            elements.get(i).visible = (curPage == pages[0]);
        }
        advanced.visible = (curPage == pages[5]);
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        checkElementsVisibility();
        super.onNewDraw(canvas, x, y);
        if(curPage == pages[0]){  // изменение обоев
            // рисуем preview обоев
            // 126, 77; 278, 189
            if(curWallpaper == null)  // в картинке изначально синий монитор
                return;
            if(wallpaperMode.selectedItem == Windows98.CENTER)
                canvas.drawBitmap(curWallpaper, x + (126 + 278) / 2 - curWallpaper.getWidth() / 2, y + (77 + 189) / 2 - curWallpaper.getHeight() / 2, null);
            else if(wallpaperMode.selectedItem == Windows98.STRETCH)
                canvas.drawBitmap(curWallpaper, x + 126, y + 77, null);
            else {
                // так как BitmapShader будет начинать делать tile не от (x + 126, y + 77), а от (0, 0)!
                canvas.save();
                canvas.translate(x + 126, y + 77);
                //canvas.drawRect(x + 126, y + 77, x + 278, y + 189, wallpaperPaint);
                canvas.drawRect(0, 0, 278 - 126, 189 - 77, wallpaperPaint);
                canvas.restore();
            }
        }
        else if(curPage == pages[5]){  // изменение разрешения экрана
            p.setColor(Color.BLACK);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(widescreen? "854 by 480 pixels" : "640 by 480 pixels", x + 290, y + 358, p);
            p.setTextAlign(Paint.Align.LEFT);
            Bitmap thumbBmp = screenAreaThumbPressed ? thumbPressed : thumb;
            int drawX = widescreen? 326 - thumbBmp.getWidth() : 255;
            canvas.drawBitmap(thumbBmp, x + drawX, y + 320, null);
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        checkElementsVisibility();
        if(curPage == pages[0]) {  // изменение обоев
            Element lastSelected = links.inputFocus;
            int lastMode = wallpaperMode.selectedItem;
            boolean returnValue = super.onMouseOver(x, y, touch);
            if (lastSelected != links.inputFocus || wallpaperMode.selectedItem != lastMode) {
                if (lastSelected != links.inputFocus)
                    ((Link) lastSelected).active = false;
                apply.disabled = false;
                updatePreview();
                wallpaperMode.disabled = links.inputFocus == links.elements.get(0);
            }
            return returnValue;
        }
        else if(curPage == pages[5]){  // изменение разрешения экрана
            if(touch){
                screenAreaThumbPressed = 255 <= x && 320 <= y && x < 326 && y < 341;
            }
            if(screenAreaThumbPressed){  // мы перемещаем ползунок
                boolean newWidescreen = x >= 291;
                if(widescreen != newWidescreen){
                    widescreen = newWidescreen;
                    apply.disabled = false;
                }
            }
        }
        return super.onMouseOver(x, y, touch);
    }

    @Override
    public void onClick(int x, int y) {
        super.onClick(x, y);
        screenAreaThumbPressed = false;
    }

    @Override
    public void onDoubleClick(int x, int y) {
        super.onDoubleClick(x, y);
        screenAreaThumbPressed = false;
    }

    @Override
    public void onSelfMouseLeave() {
        screenAreaThumbPressed = false;
    }

    private void apply(){
        Windows98.windows98.updateWallpaper(getWallpaperString(), wallpaperMode.selectedItem);
        SharedPreferences sharedPreferences = getSharedPreferences();
        boolean oldWidescreen = sharedPreferences.getBoolean("widescreen", true);
        if(oldWidescreen != widescreen) {  // мы изменили разрешение экрана
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("widescreen", widescreen);
            editor.apply();
            if(Windows98.WIDESCREEN != widescreen) {
                close();
                new MessageBox("System Settings Change",
                        "You must restart your computer before the new settings will take effect.\n\nDo you want to restart your computer now?",
                        MessageBox.YESNO, MessageBox.QUESTION, new MessageBox.MsgResultListener() {
                    @Override
                    public void onMsgResult(int buttonNumber) {
                        if (buttonNumber == YES)
                            Windows98.windows98.restart();
                    }
                }, this);
            }
        }
    }

    private String getWallpaperString(){  // кодируем путь до картинки в строку (чтобы можно было сохранить в SharedPreferences)
        int index = links.elements.indexOf(links.inputFocus);
        if(index == 0)
            return null;  // None
        else if(index <= ids.length)  // это картинка из стандартных
            return images[index - 1];
        else
            return ((Link) links.inputFocus).fullPath;
    }

    private void updatePreview(){
        curWallpaper = Windows98.getWallpaperBmp(getWallpaperString(), wallpaperMode.selectedItem, 152, 112, wallpaperPaint);
    }

    private void positionLinks(){
        int cur_y = 0;

        for(Element lnk : links.elements) {
            lnk.x = 2;
            lnk.y = cur_y;
            cur_y += 17;
            links.scrollRange = lnk.y + ((Link) lnk).fullBounds.bottom + 7;
        }

        links.updateScrollBar();
    }
}
