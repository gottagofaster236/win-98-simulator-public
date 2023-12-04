package com.lr_soft.windows98simulator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.ActionMode;
import android.webkit.WebView;

import com.lr_soft.windows98simulator.Applications.InternetExplorer;
import com.lr_soft.windows98simulator.Applications.WebViewContainer;
import com.lr_soft.windows98simulator.System.ScrollBar;

public class MyWebView extends WebView {
    public int lastScrollX, lastScrollY;
    int lastPageWidth = -1, lastPageHeight = -1;
    public WebViewContainer parent;

    /*public MyWebView(Context context, WebViewContainer parent){
        super(context);
        this.parent = parent;
    }*/

    public MyWebView(Context context){
        super(context);
    }

    @SuppressLint({"WrongCall", "MissingSuperCall"})
    @Override
    public void draw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        onDraw(canvas);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //canvas.drawColor(Color.WHITE);
        //Log.d(TAG, "onDraw");
        if((lastScrollX != getScrollX() || lastScrollY != getScrollY()))
            scrollTo(lastScrollX, lastScrollY, true);

        int pageWidth = computeHorizontalScrollRange(), pageHeight = computeVerticalScrollRange();
        // vk.com/feed нет прокрутки - очень жаль
        if(lastPageWidth != pageWidth || lastPageHeight != pageHeight) {
            //Log.d(TAG, "update scrollbar");
            if(parent != null) {
                parent.updateScrollbarPosition();
                ((InternetExplorer) parent.parent).updateWindow();
            }
        }
        lastPageWidth = pageWidth;
        lastPageHeight = pageHeight;
        super.onDraw(canvas);
    }

    @Override
    public int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    @Override
    public int computeVerticalScrollRange() {
        return super.computeVerticalScrollRange();
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    @Override
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {  // так как фокусировка webview вызывает скроллинг в начало страницы
        if((!parent.verticalScrollBar.isPressed() || parent.verticalScrollBar.mouseOverPart != ScrollBar.THUMB)
                && (!parent.horizontalScrollBar.isPressed() || parent.horizontalScrollBar.mouseOverPart != ScrollBar.THUMB)) {  // если пользователь в данный момент не скроллит ползунком, то обновляем позицию скроллбаров
            if(lastScrollX != getScrollX() || lastScrollY != getScrollY()) {
                scrollTo(lastScrollX, lastScrollY, true);
            }
            parent.updateScrollbarPosition();
            WindowsView.windowsView.invalidate();
        }
    }

    @Override
    public void scrollTo(int x, int y) { scrollTo(x, y, false); }
    @Override
    public void scrollBy(int x, int y) { scrollBy(x, y, false); }

    public void scrollTo(int x, int y, boolean isCalledByWindows) {
        if(isCalledByWindows)
            safeScrollTo(x, y);
    }
    public void scrollBy(int x, int y, boolean isCalledByWindows) {
        if(isCalledByWindows)
            scrollTo(getScrollX() + x, getScrollY() + y, true);
    }

    private void safeScrollTo(int x, int y){  // проверяет, что мы не заскроллили слишком далеко
        int max;
        if(x < 0)
            x = 0;
        else if(x > (max = computeHorizontalScrollRange() - computeHorizontalScrollExtent()))
            x = max;
        if(y < 0)
            y = 0;
        else if(y > (max = computeVerticalScrollRange() - computeVerticalScrollExtent()))
            y = max;
        lastScrollX = x;
        lastScrollY = y;
        super.scrollTo(x, y);
    }

    @Override
    public void loadUrl(String url) {
        updateUrl(url);
        super.loadUrl(url);
    }

    public void updateUrl(String url){  // обновить URL в TextBox
        if(parent == null || parent.parent == null || url == null)
            return;
        if(!url.startsWith("file:///android_asset/")) {
            InternetExplorer internetExplorer = (InternetExplorer) parent.parent;
            internetExplorer.urlAddressEdit.setText(url);
            internetExplorer.urlAddressEdit.moveCursorToEnd();
        }
    }

    @Override
    public void reload() {
        if(!getUrl().startsWith("file:///android_asset/"))
            super.reload();
        else if(canGoBack())
            goBack();
    }

    // чтобы не появлялась менюшка cut copy paste (иногда работает)
    @Override
    public ActionMode startActionMode(ActionMode.Callback callback, int type) {
        return null;
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        return null;
    }
}
