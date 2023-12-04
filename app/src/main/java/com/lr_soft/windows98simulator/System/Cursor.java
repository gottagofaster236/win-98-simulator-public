package com.lr_soft.windows98simulator.System;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.PointerIcon;

import androidx.annotation.RequiresApi;

import com.lr_soft.windows98simulator.MainActivity;
import com.lr_soft.windows98simulator.WindowsView;

import static com.lr_soft.windows98simulator.System.Element.TAG;

public class Cursor {
    private Bitmap bitmap;
    private int x, y;
    // tauon
    private Bitmap scaledBmp;
    private PointerIcon pointerIcon;
    private float coefficient;

    public Cursor(Bitmap bitmap, int x, int y) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        if(Windows98.TAUON && Build.VERSION.SDK_INT >= 24){
            Point size = new Point();
            MainActivity.getScreenSize(size);
            coefficient = size.y / 480f;
            int newWidth = Math.round(bitmap.getWidth() * coefficient);
            int newHeight = Math.round(bitmap.getHeight() * coefficient);
            scaledBmp = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            pointerIcon = PointerIcon.create(scaledBmp, x * coefficient, y * coefficient);
        }
    }

    public Cursor(Bitmap bitmap){
        this(bitmap, 0, 0);
    }

    public void draw(Canvas canvas, int x, int y){
        if(!Windows98.TAUON)
            canvas.drawBitmap(bitmap, x - this.x, y - this.y, null);
    }

    public PointerIcon getPointerIcon() {
        return pointerIcon;
    }

    @TargetApi(24)
    public void recreatePointerIcon(){
        scaledBmp = scaledBmp.copy(Build.VERSION.SDK_INT >= 26? Bitmap.Config.HARDWARE : Bitmap.Config.ARGB_8888, false);
        pointerIcon = PointerIcon.create(scaledBmp, x * coefficient, y * coefficient);
        Windows98.setCursor(this);
    }
}
