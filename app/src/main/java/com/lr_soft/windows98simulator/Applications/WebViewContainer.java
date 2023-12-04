package com.lr_soft.windows98simulator.Applications;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.lr_soft.windows98simulator.MainActivity;
import com.lr_soft.windows98simulator.MyWebView;
import com.lr_soft.windows98simulator.System.ScrollBar;
import com.lr_soft.windows98simulator.System.Scrollable;
import com.lr_soft.windows98simulator.System.ViewContainer;
import com.lr_soft.windows98simulator.System.WakeLock;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;
import com.lr_soft.windows98simulator.WindowsView;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class WebViewContainer extends ViewContainer implements Scrollable {
    public MyWebView webView;
    public ScrollBar verticalScrollBar, horizontalScrollBar;
    private List<WeakReference<InternetExplorer.FileDownloadWindow>> downloads = new ArrayList<>();
    private DownloadListener downloadListener;
    private String lastNoRedirectUrl = null, lastLoadedUrl = null;  // lastNoRedirectUrl - последняя страница без редиректов.
    private WakeLock wakeLock = new WakeLock();
    public MyWebChromeClient webChromeClient;
    private Rect minimizedBounds, maximizedBounds;

    public WebViewContainer(Window.RelativeBounds bounds, final InternetExplorer parent){
        super(new MyWebView(context));
        this.minimizedBounds = bounds.getMinimizedRect();
        this.maximizedBounds = bounds.getMaximizedRect();
        this.parent = parent;
        setBounds(minimizedBounds);

        webView = (MyWebView) getView();
        webView.parent = this;

        if (android.os.Build.VERSION.SDK_INT >= 16)
            webView.setBackground(null);
        else
            webView.setBackgroundDrawable(null);

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalFadingEdgeEnabled(false);
        webView.setHorizontalFadingEdgeEnabled(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setSupportZoom(false);  // disables the ability to zoom
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.setInitialScale((int)(scale * 100));

        if(Build.VERSION.SDK_INT >= 21)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView,true);


        webView.setWebViewClient(new MyWebViewClient());
        //webView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");  // pc version
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setDomStorageEnabled(true);
        if(android.os.Build.VERSION.SDK_INT >= 17)
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        webChromeClient = new MyWebChromeClient();
        webView.setWebChromeClient(webChromeClient);

        downloadListener = new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                if(!MyDocuments.externalStorageAvailable())
                    return;
                Uri uri = Uri.parse(url);
                String scheme = uri.getScheme();
                if (scheme == null || (!scheme.equals("http") && !scheme.equals("https")))  // DownloadManager не понимает другого
                    return;
                // если одна и та же загрузка загружается 2 раза - это плохо
                Iterator<WeakReference<InternetExplorer.FileDownloadWindow>> iterator = downloads.iterator();
                while(iterator.hasNext()){
                    WeakReference<InternetExplorer.FileDownloadWindow> ref = iterator.next();
                    if(ref.get() == null || ref.get().closed)
                        iterator.remove();
                    else if(ref.get().childMessagebox != null){
                        return;
                    }
                }
                InternetExplorer.FileDownloadWindow fileDownloadWindow =
                        new InternetExplorer.FileDownloadWindow(url, userAgent, contentDisposition, mimetype);
                downloads.add(new WeakReference<>(fileDownloadWindow));

                // если это прямая ссылка на файл, то надо вернуться на последнюю страницу без редиректов
                if(lastNoRedirectUrl != null && !lastNoRedirectUrl.equals(lastLoadedUrl)){
                    webView.loadUrl(lastNoRedirectUrl);
                    //Log.d(TAG, "going back to lastNoRedirectUrl: " + lastNoRedirectUrl);
                }

                //if(webView.canGoBack())
                //    webView.goBack();
            }
        };
        webView.setDownloadListener(downloadListener);

        webView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                updateScrollbarPosition();
            }
        });

        // https://eurobeat-prime.com/lyrics.php?lyrics=1546
        // http://www.google.com
        // https://www.youidraw.com/apps/painter/
        // http://youtube.com

        //webView.loadUrl("https://getvideo.org/en#youtube/IvyGL03F5tk");
        //webView.loadUrl("https://yadi.sk/d/3B0_D8BYmyQILw");
        //webView.loadUrl("file:///android_asset/DNSERROR.HTM");
        Windows98.targetFocus = webView;
        //context.unregisterForContextMenu(webView);
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(!(0 <= x && x < width && 0 <= y && y < height)) {
            if(isPressed()) {
                long downTime = SystemClock.uptimeMillis();
                long eventTime = SystemClock.uptimeMillis();
                MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_OUTSIDE, x * scale, y * scale, 0);
                postEvent(event);
                //Log.d(TAG, "action outside");
            }
            return false;
        }
        final long downTime = SystemClock.uptimeMillis();
        final long eventTime = SystemClock.uptimeMillis();
        final int metaState = 0;
        if(touch){  // передаём нажатие WebView
            //Log.d(TAG, "action down!");
            Windows98.targetFocus = webView;
            webView.requestFocusFromTouch();
            final MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x * scale, y * scale, metaState);
            postEvent(event);
        }
        else if(isPressed()){
            //Log.d(TAG, "action move!");
            MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x * scale, y * scale, metaState);
            postEvent(event);
        }
        return true;
    }

    @Override
    public void onClick(int x, int y) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        if(wakeLock.get()){
            downTime += 100;
            eventTime += 100;
        }
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x * scale, y * scale, 0);
        postEvent(event, 100);
        //Log.d(TAG, "action up!");
    }

    private void postEvent(final MotionEvent event){
        postEvent(event, 0);
    }

    private void postEvent(final MotionEvent event, int delay){
        if(wakeLock.get())
            delay = 0;
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(webView != null && customView == null)
                    webView.dispatchTouchEvent(event);
                event.recycle();
            }
        }, delay);
    }


    public class MyWebViewClient extends WebViewClient {
        private boolean isRedirected = false;  // Для определения страницы без редиректов. (c) https://stackoverflow.com/a/25547544/

        @TargetApi(24)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            shouldOverrideUrlLoading(view, request.getUrl().toString());
            return true;
        }

        // Для старых устройств
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            lastLoadedUrl = url;
            //Log.d(TAG, "should override url loading: " + url);
            if (url.startsWith("intent://")) {  // (c) https://stackoverflow.com/a/34022490/6120487
                try {
                    Context context = view.getContext();
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        view.stopLoading();
                        PackageManager packageManager = context.getPackageManager();
                        ResolveInfo info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                        if (info != null)
                            context.startActivity(intent);
                        else {
                            String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                            view.loadUrl(fallbackUrl);
                        }
                        return true;
                    }
                } catch (URISyntaxException e) {
                    onReceivedError();
                }
            }
            else
                view.loadUrl(url);
            isRedirected = true;
            wakeLock.setScreenOn(url.contains(".youtube.com"));
            return true;
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public void onPageFinished(WebView view, final String url) {
            if(parent == null)
                return;
            ((InternetExplorer) parent).setWebPageTitle(view.getTitle(), url);
            webView.scrollTo(0, 0, true);
            if(!url.startsWith("file:///android_asset/")) {
                ((InternetExplorer) parent).urlAddressEdit.setText(url);
                ((InternetExplorer) parent).urlAddressEdit.moveCursorToEnd();

                if(!isRedirected) {
                    lastNoRedirectUrl = url;
                    //Log.d(TAG, "lastNoRedirectUrl is now " + lastNoRedirectUrl);
                }

                if(isRedirected) {
                    final String returnUrl = lastNoRedirectUrl;
                    // проверяем, если это прямая ссылка на видео. Тогда предлагаем его скачать
                    AsyncTask<Void, Integer, String> checkForVideo = new AsyncTask<Void, Integer, String>() {
                        @Override
                        protected String doInBackground(Void... voids) {
                            String contentType;
                            try {
                                URLConnection c = new URL(url).openConnection();
                                contentType = c.getContentType();
                            } catch (Exception e) {
                                return null;
                            }
                            return contentType;

                        }

                        @Override
                        protected void onPostExecute(String contentType) {
                            if (contentType != null && contentType.startsWith("video/")) {
                                lastNoRedirectUrl = returnUrl;
                                downloadListener.onDownloadStart(url, webView.getSettings().getUserAgentString(),
                                        null, contentType, -1);
                            }
                        }
                    };
                    checkForVideo.execute();
                }
            }
            updateScrollbarPosition();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            isRedirected = false;
        }

        void onReceivedError(){
            webView.loadUrl("file:///android_asset/DNSERROR.HTM");
        }
        @TargetApi(23)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if(request.isForMainFrame())  // OMG, finally found this!
                onReceivedError();
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            //Log.d(TAG, "error code, description: " + errorCode + ", " + description);
            onReceivedError();
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            Log.d(TAG, "process gone!");
            if(parent != null)
                ((InternetExplorer) parent).close();
            else
                WindowsView.windowsView.showBSOD();
            return true;
        }
    }

    @Override
    public void prepareForDelete() {
        super.prepareForDelete();
        wakeLock.setScreenOn(false);
        webView.destroy();
        webView = null;
    }

    @Override
    public void scrollUpDown(boolean up, boolean vertical) {
        int coeff = up? -1 : 1;
        if(vertical)
            webView.scrollBy(0, 26 * coeff, true);
        else
            webView.scrollBy(26 * coeff, 0, true);
    }

    @Override
    public void pageUpDown(boolean up, boolean vertical) {
        int coeff = up? -1 : 1;
        if(vertical)
            webView.scrollBy(0, webView.computeVerticalScrollExtent() * coeff, true);
        else
            webView.scrollBy(webView.computeHorizontalScrollExtent() * coeff, 0, true);
    }

    @Override
    public void handleScrollbarClick() {
        /*if(!webView.hasFocus()) {
            webView.requestFocusFromTouch();  // так как это вызывает прокрутку к началу страницы
            // скрываем клавиатуру
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(webView.getWindowToken(), 0);
        }*/
    }

    @Override
    public void onScrollbarMoved() {
        final int new_x = (int) Math.round(horizontalScrollBar.startPosition * webView.computeHorizontalScrollRange()),
                new_y = (int) Math.round(verticalScrollBar.startPosition * webView.computeVerticalScrollRange());
        webView.scrollTo(new_x, new_y, true);
        /*webView.post(new Runnable() {
            @Override
            public void run() {
                webView.scrollTo(new_x, new_y);
            }
        });*/
    }

    @Override
    public void onOtherTouch() {
        if(((InternetExplorer) parent).active) {
            if (verticalScrollBar.onMouseOver(getCursorX() - verticalScrollBar.x - verticalScrollBar.parent.x, getCursorY() - verticalScrollBar.y - verticalScrollBar.parent.y, false))
                return;
            if (horizontalScrollBar.onMouseOver(getCursorX() - horizontalScrollBar.x - horizontalScrollBar.parent.x, getCursorY() - horizontalScrollBar.y - horizontalScrollBar.parent.y, false))
                return;
        }
        //Log.d(TAG, "on other touch");
        // если не нажали на WebViewContainer в другом Internet Explorer'е
        if(!(Windows98.windows98.startTouch instanceof InternetExplorer && ((InternetExplorer) Windows98.windows98.startTouch).startTouch instanceof WebViewContainer)) {
            //Log.d(TAG, "okay, edit text selection and text: " + MainActivity.editText.getSelectionStart() + ", " + MainActivity.editText.getText());
            Windows98.targetFocus = WindowsView.windowsView;
        }
    }

    public void updateScrollbarPosition(){
        // если горизонтальный скроллбар не нужен, то убираем его, и устанавливаем нужный размер
        updateScrollbarPosition(false);
        // затем делаем вертикальный скроллбар
        updateScrollbarPosition(true);
    }

    public void updateScrollbarPosition(boolean vertical){
        ScrollBar updating = vertical? verticalScrollBar : horizontalScrollBar;
        if(updating == null || webView == null)
            return;
        int range = vertical? webView.computeVerticalScrollRange() : webView.computeHorizontalScrollRange();  // размер всей страницы
        int start = vertical? webView.getScrollY() : webView.getScrollX();
        int extent = vertical? webView.computeVerticalScrollExtent() : webView.computeHorizontalScrollExtent();  // размер видимой части

        boolean disabled;  // нужен ли скроллбар
        if(extent < range) {
            disabled = false;
            updating.startPosition = (double) start / range;
            updating.endPosition = (double) (start + extent) / range;
        }
        else
            disabled = true;

        if(vertical)  // вертикальный скроллбар делаем серым
            updating.setDisabled(disabled);
        else{  // горизонтальный скроллбар скрываем с изменением размеров webView и вертикального скроллбара
            if(updating.visible == !disabled)
                return;
            updating.visible = !disabled;
            int dy = updating.visible? -16 : 16;
            setBounds(x, y, x + width, y + height + dy);
            verticalScrollBar.setBounds(verticalScrollBar.x, verticalScrollBar.y,
                    verticalScrollBar.x + verticalScrollBar.width, verticalScrollBar.y + verticalScrollBar.height + dy);
        }
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        updateScrollbarPosition();
    }

    @Override
    public void onWindowResize(boolean maximized) {
        horizontalScrollBar.visible = true;
        setBounds(maximized? maximizedBounds : minimizedBounds);
        updateScrollbarPosition();
    }

    // для fullscreen video
    public static View customView;
    private static WebChromeClient.CustomViewCallback customViewCallback;

    public class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            ((InternetExplorer) parent).updateWindow();
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            InternetExplorer internetExplorer = new InternetExplorer(false);

            // так как onCreateWindow в некоторых случаях вызывает крэш, пытаемся использовать loadUrl
            WebView.HitTestResult result = view.getHitTestResult();
            String url = result.getExtra();

            if(url == null) {
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(internetExplorer.webViewContainer.webView);
                resultMsg.sendToTarget();
            }
            else
                internetExplorer.webViewContainer.webView.loadUrl(url);
            ((InternetExplorer) parent).updateWindow();
            return true;
        }

        /*public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }*/

        public void onHideCustomView() {
            //Log.d(TAG, "hide custom view");
            MainActivity.windowsViewGroup.removeView(customView);
            customView = null;
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }

        public void onShowCustomView(View customView, WebChromeClient.CustomViewCallback customViewCallback) {
            //Log.d(TAG, customView.getClass().getSimpleName());
            if (WebViewContainer.customView != null) {
                //Log.d(TAG, "you are stupid");
                onHideCustomView();
            }
            WebViewContainer.customView = customView;
            WebViewContainer.customViewCallback = customViewCallback;

            /*Point size = new Point();
            MainActivity.getScreenSize(size);
            int screenWidth = size.x;
            int screenHeight = size.y;
            Log.d("Debuggy!", "w, h: " + screenWidth + " " + screenHeight);*/

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.leftMargin = params.topMargin = 0;
            customView.setLayoutParams(params);
            //if(customView instanceof FrameLayout){
            //    ((FrameLayout) customView).getFocusedChild().setLayoutParams(new FrameLayout.LayoutParams(screenWidth, screenHeight));
           // }
            MainActivity.windowsViewGroup.addView(customView, params);
        }
    }
}
