package com.lr_soft.windows98simulator.System;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.lr_soft.windows98simulator.MainActivity;
import com.lr_soft.windows98simulator.WindowsView;

public class ViewContainer extends Element {
    private View view;
    protected float scale;  // во сколько раз разрешение телефона больше, чем разрешение винды

    public ViewContainer(View view){
        this.view = view;
        scale = (float) WindowsView.windowsView.position.width() / Windows98.SCREEN_WIDTH;

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(1, 1);
        params.leftMargin = 0;
        params.topMargin = 0;
        MainActivity.windowsViewGroup.addView(this.view, params);
        WindowsView.windowsView.bringToFront();
        updateLayoutIfNeeded();
    }

    // ============= "Callback" =================
    // вызывать в makeActive, если viewContainer != null (super конструктор)
    public void onMakeActive(){
        if(view.getVisibility() != View.VISIBLE)
            view.setVisibility(View.VISIBLE);
        view.bringToFront();
        WindowsView.windowsView.bringToFront();
        updateLayoutIfNeeded();
    }

    public void onMinimize(){
        // так как другое окно может не получить makeActive
        view.setVisibility(View.INVISIBLE);
    }
    // также в onClick надо проверять перемещение окна и вызывать updateViewPosition, см. IE.


    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        drawHole(canvas, x, y, x + width, y + height);
    }

    public void updateViewPosition(){
        Rect position = WindowsView.windowsView.position;  // положение WindowsView на экране
        float ourX = getAbsoluteX(), ourY = getAbsoluteY();
        float x = (Windows98.SCREEN_WIDTH - ourX) / Windows98.SCREEN_WIDTH * position.left + ourX / Windows98.SCREEN_WIDTH * position.right;
        float y = (Windows98.SCREEN_HEIGHT - ourY) / Windows98.SCREEN_HEIGHT * position.top + ourY / Windows98.SCREEN_HEIGHT * position.bottom;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        params.leftMargin = Math.round(x);
        params.topMargin = Math.round(y);
        view.setLayoutParams(params);
        //Log.d(TAG, "update view position: " + params.leftMargin + " " + params.topMargin, new Exception());
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        updateViewPosition();
        setSize(view, (int) (width * scale + 1), (int) (height * scale + 1));  // чтобы не было зазоров
    }

    public static void setSize(View view, int targetWidth, int targetHeight){
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if(params == null) {
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        if(params.width == targetWidth && params.height == targetHeight)
            return;
        params.width = targetWidth;
        params.height = targetHeight;
        view.setLayoutParams(params);  // из под себя вызывает requestLayout()
        WindowsView.handler.postDelayed(ViewContainer::updateLayout, 500);
    }

    private static void updateLayoutIfNeeded(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            updateLayout();
        }
    }

    private static void updateLayout(){
        MainActivity.windowsViewGroup.requestLayout();
        MainActivity.windowsViewGroup.invalidate();
    }

    @Override
    public void prepareForDelete() {
        MainActivity.windowsViewGroup.removeView(view);
    }

    public View getView() {
        return view;
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        return false;
    }
}
