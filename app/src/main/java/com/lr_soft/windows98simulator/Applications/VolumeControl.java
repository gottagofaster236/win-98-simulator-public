package com.lr_soft.windows98simulator.Applications;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.CheckBox;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.ElementContainer;
import com.lr_soft.windows98simulator.System.Taskbar;

public class VolumeControl extends ElementContainer {  // громкость в системном трее
    private VolumeSlider volumeSlider;
    private CheckBox mute;
    private Bitmap background;
    private Bitmap volumeSnd, volumeSndMuted;

    private AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

    public VolumeControl() {
        background = getBmp(R.drawable.volume_background);
        width = background.getWidth();
        height = background.getHeight();
        volumeSnd = getBmp(R.drawable.tray_volume);
        volumeSndMuted = getBmp(R.drawable.volume_mute);

        volumeSlider = new VolumeSlider();
        volumeSlider.x = 29; volumeSlider.y = 35;
        elements.add(volumeSlider);

        mute = new CheckBox("Mute");
        Taskbar.trayVolume.bmp = mute.checked? volumeSndMuted : volumeSnd;
        mute.onCheckChange = new Runnable() {
            @Override
            public void run() {
                //Windows98.updateAllMplayers();
                // чтобы в трее показался перечёркнутый динамик
                Taskbar.trayVolume.bmp = mute.checked? volumeSndMuted : volumeSnd;
                updateSystemVolume();
            }
        };
        mute.x = 9; mute.y = 117;
        elements.add(mute);

        Taskbar.volumeControl = this;

        onSystemVolumeUpdate();
        //Windows98.setDesiredVolume(volumeSlider.volumeTestSound, 1);
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        canvas.drawBitmap(background, x, y, null);
        drawElements(canvas, x, y);
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        return 0 <= x && x < width && 0 <= y && y < height;
    }

    @Override
    public void onOtherTouch() {
        visible = false;  // скрываем "окно"
    }

    private float getWindowsVolume(){
        return mute.checked? 0 : volumeSlider.getRealPos();
    }

    public float getSystemVolume(){  // андроидовская громкость
        return (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public void onSystemVolumeUpdate(){
        volumeSlider.thumbPos = Math.round((1 - getSystemVolume()) * (volumeSlider.height - VolumeSlider.thumbHeight));  // т. к. thumbPos перевёрнут
        if(mute.checked) {
            mute.checked = false;
            mute.onCheckChange.run();
        }
    }

    private void updateSystemVolume(){
        int streamVolume = mute.checked? 0 : (int) Math.ceil(getWindowsVolume() * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVolume, 0);
    }

    private class VolumeSlider extends Element {
        static final int thumbHeight = 11;
        int thumbPos;  // от 0 до height - thumbHeight
        int thumbPress;  // где кликнули на ползунок
        DitherPainter ditherPainter = new DitherPainter(Color.WHITE, Color.parseColor("#C0C7C8"));
        MediaPlayer volumeTestSound = MediaPlayer.create(context, R.raw.ding);

        VolumeSlider(){
            width = 21;
            height = 65;
            captureMouse = true;
            volumeSlider = this;
        }



        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            p.setColor(Color.parseColor("#C0C7C8"));
            canvas.drawRect(x, y + thumbPos, x + width, y + thumbPos + thumbHeight, p);
            drawSimpleFrameRect(canvas, x, y + thumbPos, x + width, y + thumbPos + thumbHeight);
            if(isPressed())
                drawDitherRect(canvas, x + 2, y + thumbPos + 2, x + width - 2, y + thumbPos + thumbHeight - 2, ditherPainter);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(!(isPressed() || (0 <= x && x < width && 0 <= y && y < height)))
                return false;

            if(touch) {
                if(thumbPos <= y && y < thumbPos + thumbHeight)
                    thumbPress = y - thumbPos;
                else
                    thumbPress = thumbHeight / 2;
            }

            if(isPressed() || touch){
                float oldVolume = getWindowsVolume();
                thumbPos = y - thumbPress;
                checkThumbPos();
                if(oldVolume != getWindowsVolume()){
                    updateSystemVolume();
                    //Windows98.updateAllMplayers();
                }
            }

            return true;
        }

        @Override
        public void onClick(int x, int y) {
            volumeTestSound.start();
        }

        float getRealPos(){
            return 1 - ((float) thumbPos) / (height - thumbHeight);
        }

        void checkThumbPos(){
            if(thumbPos < 0)
                thumbPos = 0;
            else if(thumbPos > height - thumbHeight)
                thumbPos = height - thumbHeight;
        }
    }
}
