package com.lr_soft.windows98simulator.System;

import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import static com.lr_soft.windows98simulator.System.Element.TAG;

public class WakeLock {
    private static int videoPlayers = 0;
    private boolean screenOn = false;

    public WakeLock() {}

    public void setScreenOn(boolean screenOn) {
        if(this.screenOn == screenOn)
            return;
        this.screenOn = screenOn;
        if(screenOn) {
            videoPlayers++;
            if(videoPlayers == 1){
                Element.context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
        else {
            videoPlayers--;
            if(videoPlayers == 0){
                Element.context.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }

    public boolean get(){
        return screenOn;
    }
}
