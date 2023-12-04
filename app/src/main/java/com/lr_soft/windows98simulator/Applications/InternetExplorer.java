package com.lr_soft.windows98simulator.Applications;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Movie;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedImageDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;

import com.lr_soft.windows98simulator.MainActivity;
import com.lr_soft.windows98simulator.MyWebView;
import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.BigTopButtons;
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.CheckBox;
import com.lr_soft.windows98simulator.System.DummyWindow;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.System.ScrollBar;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TextBox;
import com.lr_soft.windows98simulator.System.TopMenu;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;
import com.lr_soft.windows98simulator.WindowsView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InternetExplorer extends DummyWindow {
    public WebViewContainer webViewContainer;
    public TextBox urlAddressEdit;
    private boolean goActive = false;  // кнопка Go
    private Bitmap goBmp = getBmp(R.drawable.ie_go);

    public InternetExplorer(){
        this(true);
    }

    public InternetExplorer(boolean loadHomePage){
        super("Google - Microsoft Internet Explorer", getBmp(R.drawable.html_0), true,
                getBmp(R.drawable.ie1), getBmp(Windows98.WIDESCREEN? R.drawable.ie2w : R.drawable.ie2));
        final int[] topButtonsIds = {R.drawable.back_ie, R.drawable.stop, R.drawable.refresh, R.drawable.home, R.drawable.search, R.drawable.favorites, R.drawable.history, R.drawable.mail, R.drawable.print};
        Bitmap[] topButtons = new Bitmap[9];
        for(int i = 0; i < 9; i++)
            topButtons[i] = getBmp(topButtonsIds[i]);
        BigTopButtons bigTopButtons = new BigTopButtons(topButtons,
                new int[][]{{15, 78}, {141, 190}, {191, 240}, {241, 290}, {297, 346}, {347, 396}, {397, 446}, {453, 502}, {503, 552}}, 7, new BigTopButtons.OnButtonPressListener() {
            @Override
            public void onButtonPress(int buttonNumber) {
                MyWebView webView = webViewContainer.webView;
                if(buttonNumber == 0) {
                    if(webView.canGoBack()) {
                        WebBackForwardList backForwardList = webView.copyBackForwardList();
                        WebHistoryItem previousPage = backForwardList.getItemAtIndex(backForwardList.getCurrentIndex() - 1);
                        webView.updateUrl(previousPage.getUrl());
                        setWebPageTitle(previousPage.getTitle(), previousPage.getUrl());
                        webView.goBack();
                    }
                }
                else if(buttonNumber == 1){
                    webView.stopLoading();
                }
                else if(buttonNumber == 2){
                    webView.reload();
                }
                else if(buttonNumber == 3){
                    webView.loadUrl("http://www.google.com");
                }
            }
        });
        bigTopButtons.y = 49;
        addElement(bigTopButtons);

        // поле ввода url
        urlAddressEdit = new TextBox(new RelativeBounds(75, 93, -125, 111),
                6, 14, p, new Rect(-1, -11, 0, 2));
        urlAddressEdit.parent = this;
        urlAddressEdit.setActive(false);
        urlAddressEdit.deleteLongText = true;
        urlAddressEdit.selectOnActive = true;
        urlAddressEdit.enterRunnable = this::go;
        addElement(urlAddressEdit);
        // собственно, интернет
        webViewContainer = new WebViewContainer(new RelativeBounds(6, 120, -22, -42), this);
        addElement(webViewContainer);
        webViewContainer.verticalScrollBar = new ScrollBar(webViewContainer, new RelativeBounds(-22, 120, -6, -42), true);
        webViewContainer.horizontalScrollBar = new ScrollBar(webViewContainer, new RelativeBounds(6, -42, -22, -26), false);
        addElement(webViewContainer.verticalScrollBar);
        addElement(webViewContainer.horizontalScrollBar);
        if(loadHomePage) {
            //webViewContainer.webView.loadUrl("chrome://crash");
            //webViewContainer.webView.loadUrl("https://yadi.sk/d/_uWMbu594SsYow");
            //webViewContainer.webView.loadUrl("https://www.microsoft.com/en-us/download/confirmation.aspx?id=35");
            //webViewContainer.webView.loadUrl("https://gvyoutube.com/watch?v=Zv7UUKRyCis");
            webViewContainer.webView.loadUrl("http://www.google.com");  // указываем страницу загрузки
        }
        setupTopMenu();
    }

    private RelativeBounds progressBarBounds = new RelativeBounds(-314, -22, -216, -4);
    private RelativeBounds goBounds = new RelativeBounds(-105, 91, -62, 113);

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        Rect bounds = progressBarBounds.getRect(maximized);
        bounds.offset(x, y);
        drawProgressBar(canvas, bounds);
        bounds.offset(-x, -y);  // т. к. всё по ссылке

        if(goActive) {
            Rect goBounds = this.goBounds.getRect(maximized);
            canvas.drawBitmap(goBmp, x + goBounds.left, y + goBounds.top, null);
        }
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(!super.onMouseOver(x, y, touch))
            return false;
        goActive = goBounds.getRect(maximized).contains(x, y);
        if(touch && goActive){
            go();
        }
        return true;
    }

    @Override
    public void onOtherTouch() {
        super.onOtherTouch();
        goActive = false;
    }

    private void go(){  // перейти по ссылке, набранной в textBox
        String url = urlAddressEdit.text.trim();
        if(url.contains(" ") || !url.contains(".")){  // делаем из этого поисковый запрос
            try {
                String query = URLEncoder.encode(url, "utf-8");
                url = "http://www.google.com/search?q=" + query;
            }
            catch (UnsupportedEncodingException ignored){}
        }
        else if(!url.startsWith("http://") && !url.startsWith("https://"))
            url = "http://" + url; //"http://www." + url;
        urlAddressEdit.setText(url);
        webViewContainer.webView.loadUrl(url);
    }

    private void drawProgressBar(Canvas canvas, Rect rect){
        int x1 = rect.left, y1 = rect.top, x2 = rect.right, y2 = rect.bottom;
        int progress = webViewContainer.webView.getProgress();
        if(progress == 100)
            return;
        drawVerySimpleFrameRect(canvas, x1, y1, x2, y2);
        // надо ещё дорисовать, т. к. на самом деле мы рисуем поверх уже такого же simpleFrameRect
        p.setColor(Color.parseColor("#C0C7C8"));
        canvas.drawRect(x1 - 2, y1, x1, y2, p);
        p.setColor(Color.WHITE);
        canvas.drawRect(x1 - 3, y1, x1 - 2, y2, p);
        int progressWidth = Math.round((x2 - x1 - 6) * progress * 0.01f);
        p.setColor(Color.parseColor("#0000A8"));
        canvas.drawRect(x1 + 3, y1 + 3, x1 + 3 + progressWidth, y2 - 3, p);
    }

    public void setWebPageTitle(String webPageTitle, String url){
        if(webPageTitle == null || url == null){  // в Play Console есть такой crash. На всякий случай.
            setTitle("Microsoft Internet Explorer");
            return;
        }
        //if(!isAsciiString(webPageTitle))
        //    webPageTitle = url;
        if(!webPageTitle.isEmpty()) {  // && isAsciiString(webPageTitle)) {
            setTitle(webPageTitle + " - Microsoft Internet Explorer");
        }
        else
            setTitle("Microsoft Internet Explorer");
    }

    // работа с положением webView
    @Override
    public void makeActive() {
        super.makeActive();
        if(webViewContainer != null) {  // не вызываемся из super конструктора
            webViewContainer.onMakeActive();
            if(WebViewContainer.customView != null)
                WebViewContainer.customView.bringToFront();
        }
    }

    @Override
    public void minimize() {
        super.minimize();
        webViewContainer.onMinimize();
    }

    @Override
    public void onClick(int x, int y) {
        // проверяем, переместилось ли окно, если да - перемещаем webView
        int oldX = this.x, oldY = this.y;
        super.onClick(x, y);
        if(oldX != this.x || oldY != this.y){
            webViewContainer.updateViewPosition();
        }
    }

    // ================ FILE DOWNLOAD WINDOW =======================================

    public static class FileDownloadWindow extends Window {
        // для GIF анимации загрузки используется Movie для API < 28, AnimatedImageDrawable для API >= 28
        Movie oldMovie;
        AnimatedImageDrawable newMovie;
        Bitmap movieBitmap = createBitmap(272, 60, Bitmap.Config.ARGB_8888);
        Canvas movieCanvas = new Canvas(movieBitmap);  // потому что Movie глючный (???)
        long downloadStartTime = -1;
        Button open, openFolder;
        CheckBox closeOnDownloadComplete;
        float progress = 0;
        boolean downloadComplete = false, downloadFailed = false;
        long downloadId;
        long lastTextUpdate = -1;
        long updateRate = 1000;  // обновляем текст раз в 1000 миллисекунд
        File file;
        String url;
        String savingFrom = "", timeLeft = "", downloadTo = "", transferRate = "";
        BroadcastReceiver onDownloadComplete;
        ContentObserver contentObserver;
        boolean isContentObserverWorking = false;  // может быть, contentObserver ничего не получит - это недокументированная функция
        Bitmap downloadCompleteBmp = getBmp(R.drawable.download_complete);

        @Override
        public void onNewDraw(Canvas canvas, int x, int y) {
            super.onNewDraw(canvas, x, y);
            if(!downloadComplete) {
                movieCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if (android.os.Build.VERSION.SDK_INT < 28) {
                    if (downloadStartTime != -1)
                        oldMovie.setTime((int) (System.currentTimeMillis() - downloadStartTime) % oldMovie.duration());
                    oldMovie.draw(movieCanvas, 0, 0); //, x + 14, y + 22);
                }
                else
                    newMovie.draw(movieCanvas);
                canvas.drawBitmap(movieBitmap, x + 14, y + 22, null);
            }
            else
                canvas.drawBitmap(downloadCompleteBmp, x + 14, y + 38, null);
            updateStrings();
            p.setColor(Color.BLACK);
            canvas.drawText(downloadComplete? "Saved:" : "Saving:", x + 15, y + 95, p);
            canvas.drawText(savingFrom, x + 15, y + 111, p);
            canvas.drawText(downloadComplete? "Downloaded:" : "Estimated time left:", x + 15, y + 144, p);
            canvas.drawText(timeLeft, x + 109, y + 144, p);
            canvas.drawText("Download to:", x + 15, y + 160, p);
            canvas.drawText(downloadTo, x + 109, y + 160, p);
            canvas.drawText("Transfer rate:", x + 15, y + 176, p);
            canvas.drawText(transferRate, x + 109, y + 176, p);
            // progress bar
            p.setColor(Color.parseColor("#87888F"));  // серый
            canvas.drawRect(x + 14, y + 116, x + 360, y + 117, p);
            canvas.drawRect(x + 14, y + 116, x + 15, y + 128, p);
            p.setColor(Color.WHITE);
            canvas.drawRect(x + 14, y + 128, x + 361, y + 129, p);
            canvas.drawRect(x + 360, y + 116, x + 361, y + 129, p);
            int progressSquares = Math.round(progress * 43);
            p.setColor(Color.rgb(0, 0, 168));  // синий
            for(int i = 0; i < progressSquares; i++){
                canvas.drawRect(x + 16 + 8 * i, y + 118, x + 22 + 8 * i, y + 127, p);
            }
        }

        private void updateStrings(){
            updateStrings(false);
        }

        private void updateStrings(boolean isCalledByContentObserver){  // обновляет строки, которые выводятся на экран, и вообще состояние всего окна
            if(downloadComplete || downloadFailed)
                return;
            if(isContentObserverWorking && !isCalledByContentObserver)
                return;
            if(!isContentObserverWorking && System.currentTimeMillis() - lastTextUpdate < updateRate)
                return;
            checkDownloadStatus();
            if(downloadFailed)
                return;
            //if(file != null) {
            //    if (file.exists())
            //        fileObserver.startWatching();
            //}

            savingFrom = shortenString(getFilename() + " from " + url, 270);
            downloadTo = shortenString(MyDocuments.getFullPath(file), 250);

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor c = ((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE)).query(query);
            if(!c.moveToFirst()) {  // база данных пустая (?)
                c.close();
                return;
            }
            long size = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            long downloadedBytes = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            c.close();
            //if(size == -1)
            //    return;
            progress = (float) downloadedBytes / size;
            if(progress < 0)
                progress = 0;
            int progressPercent = Math.round(progress * 100);
            setTitle(progressPercent + "% of " + getFilename() + " Completed");
            //float speed = (downloadedBytes - lastDownloadedBytes) / (System.currentTimeMillis() - lastTextUpdate);
            float downloadTime = (System.currentTimeMillis() - downloadStartTime) / 1000f;
            float speed = downloadedBytes / downloadTime;
            int timeToFinish = Math.round((size - downloadedBytes) / speed);  // в секундах
            String timeString = secondsToString(timeToFinish);
            if(size == -1)
                timeString = "unknown ";
            timeLeft = timeString + "(" + bytesToString(downloadedBytes) + " of " + bytesToString(size) + " copied)";
            timeLeft = shortenTextToThreeDots(timeLeft, 250, p);
            transferRate = bytesToString(speed) + "/Sec";

            lastTextUpdate = System.currentTimeMillis();
        }

        private String getFilename(){
            return file != null? file.getName() : "";
        }

        private void checkDownloadStatus(){
            if(downloadComplete || downloadFailed)
                return;
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor c = ((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE)).query(query);
            if(!c.moveToFirst()) {  // база данных пустая (?)
                c.close();
                return;
            }
            int downloadStatus = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            /*int downloadedBytes = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            int size = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            if(downloadStatus == DownloadManager.STATUS_SUCCESSFUL || downloadedBytes == size){
                Log.d(TAG, "STATUS SUCCESS!!!!!!");
            }
            else */
            if(downloadStatus == DownloadManager.STATUS_FAILED){
                int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                /*if(reason == DownloadManager.ERROR_CANNOT_RESUME){
                    File file = new File(MyDocuments.getFilesDir().getAbsolutePath() + File.separator + filename);
                    if(file.exists()){
                        Log.d(TAG, "this file exists, and size is " + file.length());
                    }
                    else
                        Log.d(TAG, "sadly, it no more exists");
                }
                Log.d(TAG, "shit ass mother fuckers!", new Exception());*/
                setDownloadFailed(reason);
            }
            c.close();
            //Log.d(TAG, "downloaded vs size: " + downloadedBytes + " " + size);
        }

        private void setDownloadFailed(int reason){
            downloadFailed = true;
            stopAnimation();
            String text = "Download Failed.";
            if(reason == DownloadManager.ERROR_INSUFFICIENT_SPACE)
                text += " Not enough free disk space. Free some space on this disk and then try again.";
            else if(reason == DownloadManager.ERROR_CANNOT_RESUME)
                text += " Cannot resume download.";
            else if(reason == DownloadManager.ERROR_DEVICE_NOT_FOUND)
                text += " Disk not accessible.";
            else if(reason == DownloadManager.ERROR_FILE_ERROR)
                text += " Disk write error.";
            //Log.d(TAG, "reason: " + reason);
            new MessageBox("File Download", text, MessageBox.OK, MessageBox.ERROR, new MessageBox.MsgResultListener() {
                @Override
                public void onMsgResult(int buttonNumber) {
                    close();
                }
            }, this);
        }

        private String secondsToString(int time){  // строка с пробелом на конце!
            int seconds = time % 60;
            int minutes = (time / 60) % 60;
            int hours = (time / (60 * 60)) % 24;
            int days = (time / (60 * 60 * 24));
            String timeString = "";
            if(days != 0)
                timeString += days + (days == 1? " day " : " days ");
            if(hours != 0)
                timeString += hours + (hours == 1? " hour " : " hours ");
            if(minutes != 0)
                timeString += minutes + (minutes == 1? " minute " : " minutes ");
            timeString += seconds + (seconds == 1? " second " : " seconds ");
            return timeString;
        }
        private String shortenString(String text, int maxWidth){
            if (measureText(text, p) > maxWidth) {
                int countDrawingSymbols = 0;
                // copypaste из shortenTextToThreeDots
                while (measureText("..." + text.substring(text.length() - (countDrawingSymbols + 1)), p) <= maxWidth)
                    countDrawingSymbols++;
                return "..." + text.substring(text.length() - countDrawingSymbols);
            } else
                return text;
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            updateStrings();
            if(!closed)
                return super.onMouseOver(x, y, touch);
            else
                return false;
        }

        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                WindowsView.handler.postDelayed(this, 70);
                FileDownloadWindow.this.updateWindow();
            }
        };

        @SuppressWarnings("ResourceType")
        public FileDownloadWindow(final String url, final String userAgent, final String contentDisposition, final String mimeType){
            super("File Download", null, 374, 261, false, false, false);
            // инициализируем элементы
            unableToMaximize = true;
            if (android.os.Build.VERSION.SDK_INT < 28) {
                oldMovie = Movie.decodeStream(resources.openRawResource(R.drawable.tshell32_170));
            }
            else{
                try {
                    ImageDecoder.Source source = ImageDecoder.createSource(resources, R.drawable.tshell32_170);
                    newMovie = (AnimatedImageDrawable) ImageDecoder.decodeDrawable(source);
                }
                catch (Exception e){
                    throw new RuntimeException("AnimatedImageDrawable loading failed");
                }
                newMovie.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
            }

            open = new Button("Open", new Rect(123, 224, 198, 247), parent -> {
                close();
                OnClickRunnable r = new Link(file, FileDownloadWindow.this).action;
                if(r != null)
                    r.run(null);
            });

            open.disabled = true;
            addElement(open);
            openFolder = new Button("Open Folder", new Rect(204, 224, 279, 247), parent -> {
                close();
                new MyDocuments(file.getParentFile());
            });
            openFolder.disabled = true;
            addElement(openFolder);
            defaultButton = new Button("Cancel", new Rect(285, 224, 360, 247), parent -> close());
            defaultButton.coolActive = true;
            addElement(defaultButton);
            closeOnDownloadComplete = new CheckBox("Close this dialog box when download completes");
            closeOnDownloadComplete.x = 14;
            closeOnDownloadComplete.y = 186;
            addElement(closeOnDownloadComplete);
            centerWindowOnScreen();
            this.url = url.substring(url.indexOf('/') + 2);  // http(s)://
            if(this.url.contains("/"))
                this.url = this.url.substring(0, this.url.indexOf('/'));
            // создаём FileDialog
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
            String name, extension;
            if(filename.contains(".")) {
                int dotIndex = filename.lastIndexOf('.');
                name = filename.substring(0, dotIndex);
                extension = filename.substring(dotIndex + 1);
            }
            else {
                name = filename;
                extension = "bin";
            }
            //if(!isAsciiString(name))
            //    name = "downloadfile";
            //filename = name + "." + extension;
            String extensionDescription = extension.toUpperCase() + " Files";
            extensionDescription = shortenTextToThreeDots(extensionDescription, 205, p);
            new FileDialog(false, extension, extensionDescription, this, new FileDialog.OnResultListener() {
                @Override
                public void writeToFile(final File file) {
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        context.checkWriteExternalPermission(new MainActivity.PermissionResultListener() {
                             @Override
                             public void onPermissionGranted() {
                                 FileDownloadWindow.this.onPermissionGranted(file, url, mimeType, userAgent);
                             }

                             @Override
                             public void onPermissionDenied() {
                                 setDownloadFailed(DownloadManager.ERROR_DEVICE_NOT_FOUND);
                                 updateWindow();
                             }
                        });
                    }
                    else
                        onPermissionGranted(file, url, mimeType, userAgent);
                }
                @Override
                void onCancel() {
                    close();
                }
            }, name, false);
        }

        private void onPermissionGranted(File file, String url, String mimeType, String userAgent) {
            if (file.exists())  // DownloadManager не умеет заменять файлы
                file.delete();
            FileDownloadWindow.this.file = file;
            String filename = file.getName();
            startAnimation();
            // загружаем файл
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("Cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Win 98 Download");
            request.setTitle(filename);
            if(Build.VERSION.SDK_INT < 29) {
                request.allowScanningByMediaScanner();
                request.setVisibleInDownloadsUi(false);
            }
            //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setDestinationInExternalFilesDir(context, null, MyDocuments.getRelativePath(file));
            DownloadManager dm = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
            downloadId = dm.enqueue(request);
            // конец загрузки
            onDownloadComplete = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //Fetching the download id received with the broadcast
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    //Checking if the received broadcast is for our enqueued download by matching download id
                    if (id == downloadId) {
                        checkDownloadStatus();
                        if (downloadFailed) {
                            return;
                        }
                        downloadComplete = true;
                        progress = 1;

                        if (closeOnDownloadComplete.checked) {
                            close();
                            return;
                        }
                        setTitle("Download complete");
                        closeOnDownloadComplete.visible = false;
                        open.disabled = false;
                        openFolder.disabled = false;
                        defaultButton.text = "Close";  // Cancel -> Close
                        float downloadTime = (System.currentTimeMillis() - downloadStartTime) / 1000f;

                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterById(downloadId);
                        Cursor c = ((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE)).query(query);
                        long size = 0;
                        if(c.moveToFirst())
                            size = c.getLong(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        c.close();
                        // переиспользуем те же строки
                        timeLeft = bytesToString(size) + " in " + secondsToString(Math.round(downloadTime));
                        transferRate = bytesToString(size / downloadTime) + "/Sec";
                        stopAnimation();
                        updateWindow();
                    }
                }
            };
            context.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            Uri myDownloads = Uri.parse("content://downloads/my_downloads");
            contentObserver = new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    String uriStr = uri.toString();
                    if(uriStr.substring(uriStr.lastIndexOf('/') + 1).equals(String.valueOf(downloadId))) {
                        //Log.d(TAG, "content observer update");
                        isContentObserverWorking = true;
                        updateStrings(true);
                    }
                    updateWindow();
                }
            };
            context.getContentResolver().registerContentObserver(myDownloads, true, contentObserver);
            updateWindow();
        }

        private void startAnimation(){
            WindowsView.handler.postDelayed(updateRunnable, 70);
            if(android.os.Build.VERSION.SDK_INT >= 28)
                newMovie.start();
            downloadStartTime = System.currentTimeMillis();
        }

        private void stopAnimation(){
            WindowsView.handler.removeCallbacks(updateRunnable);
            if(android.os.Build.VERSION.SDK_INT >= 28)
                newMovie.stop();
        }

        @Override
        public void close(boolean activateNextWindow) {
            if(!downloadComplete)
                ((DownloadManager) context.getSystemService(DOWNLOAD_SERVICE)).remove(downloadId);  // удаляем нашу загрузку
            super.close(activateNextWindow);
        }

        @Override
        public void prepareForDelete() {
            super.prepareForDelete();
            stopAnimation();
            try{
                context.unregisterReceiver(onDownloadComplete);
            }
            catch (Exception ignored){}
            try{
                context.getContentResolver().unregisterContentObserver(contentObserver);
            }
            catch (Exception ignored){}
        }

        private final static String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB"};
        public static String bytesToString(double bytes){
            if(bytes < 0)
                return "(unknown)";
            int curIndex = 0;  // индекс в массиве units
            while(bytes > 1024 && curIndex < units.length - 1){
                bytes /= 1024;
                curIndex++;
            }
            // округляем до 3 значащих цифр
            String numberFormatted;
            if(bytes < 10)  // 1.23
                numberFormatted = String.format(Locale.US, "%.2f", bytes);
            else if(bytes < 100)  // 12.3
                numberFormatted = String.format(Locale.US, "%.1f", bytes);
            else // 123
                numberFormatted = String.format(Locale.US, "%.0f", bytes);
            return numberFormatted + " " + units[curIndex];
        }
    }

    private void setupTopMenu(){
        ButtonList file = new ButtonList();
        ButtonList new_ = new ButtonList();
        new_.elements.add(new ButtonInList("Window", "Ctrl+N"));
        new_.elements.add(new Separator());
        new_.elements.add(new ButtonInList("Message"));
        new_.elements.add(new ButtonInList("Post"));
        new_.elements.add(new ButtonInList("Contact"));
        new_.elements.add(new ButtonInList("Internet Call"));
        file.elements.add(new ButtonInList("New", new_));
        file.elements.add(new ButtonInList("Open...", "Ctrl+O"));
        ButtonInList editButton = new ButtonInList("Edit");
        editButton.disabled = true;
        file.elements.add(editButton);
        ButtonInList save = new ButtonInList("Save", "Ctrl+S");
        save.disabled = true;
        file.elements.add(save);
        file.elements.add(new ButtonInList("Save as..."));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("Page Setup..."));
        file.elements.add(new ButtonInList("Print...", "Ctrl+P"));
        file.elements.add(new Separator());
        ButtonList send = new ButtonList();
        send.elements.add(new ButtonInList("Page by E-mail..."));
        send.elements.add(new ButtonInList("Link by E-mail..."));
        send.elements.add(new ButtonInList("Shortcut to Desktop"));
        file.elements.add(new ButtonInList("Send", send));
        file.elements.add(new ButtonInList("Import and Export..."));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("Properties"));
        file.elements.add(new ButtonInList("Work Offline"));
        ButtonInList close = new ButtonInList("Close");
        close.action = parent -> close();
        file.elements.add(close);

        ButtonList edit = new ButtonList();
        ButtonInList cut = new ButtonInList("Cut", "Ctrl+X");
        cut.disabled = true;
        edit.elements.add(cut);
        ButtonInList copy = new ButtonInList("Copy", "Ctrl+C");
        copy.disabled = true;
        edit.elements.add(copy);
        ButtonInList paste = new ButtonInList("Paste", "Ctrl+V");
        paste.disabled = true;
        edit.elements.add(paste);
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("Select All", "Ctrl+A"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("Find (on This Page)...", "Ctrl+F"));

        ButtonList view = new ButtonList();
        ButtonList toolbars = new ButtonList();
        ButtonInList standartButtons = new ButtonInList("Standart Buttons");
        standartButtons.check = true;
        standartButtons.checkActive = true;
        toolbars.elements.add(standartButtons);
        ButtonInList addressBar = new ButtonInList("Address Bar");
        addressBar.check = true;
        addressBar.checkActive = true;
        toolbars.elements.add(addressBar);
        ButtonInList links = new ButtonInList("Links");
        links.check = true;
        links.checkActive = true;
        toolbars.elements.add(links);
        ButtonInList radio = new ButtonInList("Radio");
        radio.check = true;
        radio.checkActive = false;
        toolbars.elements.add(radio);
        toolbars.elements.add(new Separator());
        toolbars.elements.add(new ButtonInList("Customize..."));
        view.elements.add(new ButtonInList("Toolbars", toolbars));
        ButtonInList statusBar = new ButtonInList("Status Bar");
        statusBar.check = true;
        statusBar.checkActive = true;
        view.elements.add(statusBar);
        ButtonList explorerBar = new ButtonList();
        explorerBar.elements.add(new ButtonInList("Search", "Ctrl+E"));
        explorerBar.elements.add(new ButtonInList("Favorites", "Ctrl+I"));
        explorerBar.elements.add(new ButtonInList("History", "Ctrl+H"));
        explorerBar.elements.add(new ButtonInList("Folders"));
        explorerBar.elements.add(new Separator());
        explorerBar.elements.add(new ButtonInList("Tip of the Day"));
        view.elements.add(new ButtonInList("Explorer Bar", explorerBar));
        view.elements.add(new Separator());
        ButtonList goTo = new ButtonList();
        goTo.elements.add(new ButtonInList("Back", "Alt+Left Arrow"));
        goTo.elements.add(new ButtonInList("Forward", "Alt+Right Arrow"));
        goTo.elements.add(new Separator());
        goTo.elements.add(new ButtonInList("Home Page", "Alt+Home"));
        goTo.elements.add(new Separator());
        goTo.elements.add(new ButtonInList("Cannot find server"));
        view.elements.add(new ButtonInList("Go To", goTo));
        view.elements.add(new ButtonInList("Stop", "Esc"));
        view.elements.add(new ButtonInList("Refresh", "F5"));
        view.elements.add(new Separator());
        ButtonList textSize = new ButtonList();
        List<ButtonInList> buttonGroup = new ArrayList<>();
        buttonGroup.add(new ButtonInList("Largest"));
        buttonGroup.add(new ButtonInList("Larger"));
        buttonGroup.add(new ButtonInList("Medium"));
        buttonGroup.add(new ButtonInList("Smaller"));
        buttonGroup.add(new ButtonInList("Smallest"));
        buttonGroup.get(2).checkActive = true;  // Medium
        for(ButtonInList button : buttonGroup){
            button.radioButtonGroup = buttonGroup;
            textSize.elements.add(button);
        }
        view.elements.add(new ButtonInList("Text Size", textSize));
        ButtonList encoding = new ButtonList();
        ButtonInList autoSelect = new ButtonInList("Auto Select");
        autoSelect.check = true;
        encoding.elements.add(autoSelect);
        encoding.elements.add(new Separator());
        ButtonInList westernEuropean = new ButtonInList("Western European (Windows)");
        List<ButtonInList> buttonGroup2 = new ArrayList<>();
        buttonGroup2.add(westernEuropean);
        westernEuropean.radioButtonGroup = buttonGroup2;
        westernEuropean.checkActive = true;
        encoding.elements.add(westernEuropean);
        encoding.elements.add(new ButtonInList("More..."));
        view.elements.add(new ButtonInList("Encoding", encoding));
        view.elements.add(new Separator());
        view.elements.add(new ButtonInList("Source"));
        view.elements.add(new ButtonInList("Full Screen", "F11"));

        ButtonList favorites = StartMenu.getFavoritesMenu();
        favorites.elements.add(0, new ButtonInList("Add to Favorites..."));
        favorites.elements.add(1, new ButtonInList("Organize Favorites..."));
        favorites.elements.add(2, new Separator());

        ButtonList tools = new ButtonList();
        ButtonList mailAndNews = new ButtonList();
        mailAndNews.elements.add(new ButtonInList("Read Mail"));
        mailAndNews.elements.add(new ButtonInList("New Message..."));
        mailAndNews.elements.add(new ButtonInList("Send a Link..."));
        mailAndNews.elements.add(new ButtonInList("Send Page..."));
        mailAndNews.elements.add(new Separator());
        mailAndNews.elements.add(new ButtonInList("Read News"));
        tools.elements.add(new ButtonInList("Mail and News", mailAndNews));
        tools.elements.add(new ButtonInList("Synchronize"));
        tools.elements.add(new ButtonInList("Windows Update"));
        tools.elements.add(new Separator());
        tools.elements.add(new ButtonInList("Show Related Links"));
        tools.elements.add(new Separator());
        tools.elements.add(new ButtonInList("Internet Options..."));

        ButtonList help = new ButtonList();
        help.elements.add(new ButtonInList("Contents and Index"));
        help.elements.add(new ButtonInList("Tip of the Day"));
        help.elements.add(new ButtonInList("For Netscape Users"));
        help.elements.add(new ButtonInList("Tour"));
        help.elements.add(new ButtonInList("Online Support"));
        help.elements.add(new ButtonInList("Send Feedback"));
        help.elements.add(new Separator());
        ButtonInList aboutIE = new ButtonInList("About Internet Explorer");
        aboutIE.action = parent -> {
            new DummyWindow("About Internet Explorer", null, false, getBmp(R.drawable.about_ie), new Rect(318, 297, 393, 320), "OK");
            onOtherTouch();
        };
        help.elements.add(aboutIE);

        TopMenu topMenu = new TopMenu();
        topMenu.buttonsSize = 2;
        topMenu.elements.add(new TopMenuButton("File", file));
        topMenu.elements.add(new TopMenuButton("Edit", edit));
        topMenu.elements.add(new TopMenuButton("View", view));
        topMenu.elements.add(new TopMenuButton("Favorites", favorites));
        topMenu.elements.add(new TopMenuButton("Tools", tools));
        topMenu.elements.add(new TopMenuButton("Help", help));
        topMenu.x = 15;
        topMenu.y = 25;
        repositionTopMenu = false;
        setTopMenu(topMenu);
    }
}
