package com.lr_soft.windows98simulator.System;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.Applications.InternetExplorer;
import com.lr_soft.windows98simulator.Applications.SchedTasks;
import com.lr_soft.windows98simulator.Applications.StartMenu;
import com.lr_soft.windows98simulator.Applications.VolumeControl;
import com.lr_soft.windows98simulator.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Taskbar extends ElementContainer {
    public static Taskbar taskbar;
    public static TrayIcon trayVolume;
    public static VolumeControl volumeControl;

    private DateFormat df = new SimpleDateFormat("h:mm a", Locale.US);
    public Taskbar(){
        taskbar = this;
        elements.add(new ProgramsInTaskBar());
        elements.add(new StartMenu());
        // quick launch
        elements.add(new QuickLaunchIcon(getBmp(R.drawable.desktop_3), this::minimizeAll,
                69, Windows98.SCREEN_HEIGHT - 24));
        elements.add(new QuickLaunchIcon(getBmp(R.drawable.iexplore_32528_3), InternetExplorer::new,
                92, Windows98.SCREEN_HEIGHT - 24));
        elements.add(new QuickLaunchIcon(getBmp(R.drawable.outlook_express_2), StartMenu::createOutlookExpress,
                115, Windows98.SCREEN_HEIGHT - 24));

        // tray icons
        ButtonList schedMenu = new ButtonList();
        ButtonInList schedOpen = new ButtonInList("Open", parent -> {
            SchedTasks schedTasks = new SchedTasks();
            schedTasks.maximize();
        });
        schedOpen.textBold = true;
        schedMenu.elements.add(schedOpen);
        schedMenu.elements.add(new ButtonInList("Pause Task Scheduler"));
        TrayIcon schedTasks = new TrayIcon(getBmp(R.drawable.task_sched), new ContextMenu(schedMenu), Windows98.SCREEN_WIDTH - 97, Windows98.SCREEN_HEIGHT - 21);
        schedTasks.onDoubleClick = () -> {
            SchedTasks schedTasks1 = new SchedTasks();
            schedTasks1.maximize();
        };
        elements.add(schedTasks);

        final ButtonList volumeMenu = new ButtonList();
        ButtonInList openVolume = new ButtonInList("Open Volume Controls", parent -> StartMenu.createVolumeControl());
        openVolume.textBold = true;
        volumeMenu.elements.add(openVolume);
        volumeMenu.elements.add(new ButtonInList("Adjust Audio Properties",
                parent -> new DummyWindow("Audio Properties", null, false, getBmp(R.drawable.audio_props),
                new Rect(121, 396, 196, 419), "OK",
                new Rect(202, 396, 277, 419), "Cancel")));

        trayVolume = new TrayIcon(getBmp(R.drawable.tray_volume), new ContextMenu(volumeMenu), Windows98.SCREEN_WIDTH - 80, Windows98.SCREEN_HEIGHT - 21);
        trayVolume.onClick = () -> {
            volumeControl.visible = true;
            volumeControl.y = Windows98.SCREEN_HEIGHT - volumeControl.height;
            volumeControl.x = getCursorX() - volumeControl.width;
        };
        elements.add(trayVolume);

        volumeControl = new VolumeControl();
        volumeControl.visible = false;
        elements.add(volumeControl);
        // context menu
        ButtonList toolbars = new ButtonList();
        toolbars.elements.add(new ButtonInList("Address"));
        toolbars.elements.add(new ButtonInList("Links"));
        toolbars.elements.add(new ButtonInList("Desktop"));
        toolbars.elements.add(new ButtonInList("Quick Launch"));
        for(Element e : toolbars.elements)
            ((ButtonInList) e).check = true;
        ((ButtonInList) toolbars.elements.get(3)).checkActive = true;  // Quick Launch
        toolbars.elements.add(new Separator());
        toolbars.elements.add(new ButtonInList("New Toolbar..."));
        ButtonList rightMenu = new ButtonList();
        rightMenu.elements.add(new ButtonInList("Toolbars", toolbars));
        rightMenu.elements.add(new Separator());
        rightMenu.elements.add(new ButtonInList("Cascade Windows"));
        rightMenu.elements.add(new ButtonInList("Tile Windows Horizontally"));
        rightMenu.elements.add(new ButtonInList("Tile Windows Vertically"));
        for(int i=2; i<=4; i++){
            ((ButtonInList) rightMenu.elements.get(i)).disabled = true;
        }
        rightMenu.elements.add(new Separator());
        ButtonInList minimizeAll = new ButtonInList("Minimize All Windows");
        minimizeAll.action = parent -> minimizeAll();
        rightMenu.elements.add(minimizeAll);
        rightMenu.elements.add(new Separator());
        ButtonInList properties = new ButtonInList("Properties");
        properties.action = parent -> StartMenu.createTaskbarProps();
        rightMenu.elements.add(properties);
        contextMenu = new ContextMenu(rightMenu);
        elements.add(contextMenu);
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        p.setColor(Color.rgb(192, 199, 200));  // серый
        canvas.drawRect(0, Windows98.TASKBAR_Y, Windows98.SCREEN_WIDTH, Windows98.SCREEN_HEIGHT, p);
        p.setColor(Color.WHITE);
        canvas.drawRect(0, Windows98.TASKBAR_Y + 1, Windows98.SCREEN_WIDTH, Windows98.TASKBAR_Y + 2, p);
        p.setColor(Color.rgb(135, 136, 143));  // тёмно-серый
        canvas.drawRect(58, Windows98.TASKBAR_Y + 4, 59, Windows98.TASKBAR_Y + 26, p);
        canvas.drawRect(142, Windows98.TASKBAR_Y + 4, 143, Windows98.TASKBAR_Y + 26, p);
        canvas.drawRect(Windows98.SCREEN_WIDTH - 104, Windows98.TASKBAR_Y + 4, Windows98.SCREEN_WIDTH - 103, Windows98.TASKBAR_Y + 26, p);
        p.setColor(Color.WHITE);
        canvas.drawRect(59, Windows98.TASKBAR_Y + 4, 60, Windows98.TASKBAR_Y + 26, p);
        canvas.drawRect(143, Windows98.TASKBAR_Y + 4, 144, Windows98.TASKBAR_Y + 26, p);
        canvas.drawRect(Windows98.SCREEN_WIDTH - 103, Windows98.TASKBAR_Y + 4, Windows98.SCREEN_WIDTH - 102, Windows98.TASKBAR_Y + 26, p);

        drawVerySimpleFrameRect(canvas, 62, Windows98.TASKBAR_Y + 6, 65, Windows98.TASKBAR_Y + 24, false);
        drawVerySimpleFrameRect(canvas, 146, Windows98.TASKBAR_Y + 6, 149, Windows98.TASKBAR_Y + 24, false);
        drawVerySimpleFrameRect(canvas, Windows98.SCREEN_WIDTH - 100, Windows98.TASKBAR_Y + 4, Windows98.SCREEN_WIDTH - 2, Windows98.TASKBAR_Y + 26, true);

        String date = df.format(Calendar.getInstance().getTime());  // рисуем часы
        Element.p.setColor(Color.BLACK);
        canvas.drawText(date, Windows98.SCREEN_WIDTH - 52, Windows98.SCREEN_HEIGHT - 8, Element.p);
        drawElements(canvas, x, y);
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {
        return y >= Windows98.TASKBAR_Y;
    }

    @Override
    public void onSelfRightClick(int x, int y) {
        if(x > 539){  // нажали на часы
            onOtherTouch();
            return;
        }
        super.onSelfRightClick(x, y);
    }

    private void minimizeAll(){
        List<Element> elements = Windows98.windows98.elements;
        // чтобы избежать concurrentModificationException
        for(int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if(element instanceof Window)
                ((Window) element).minimize();
        }
    }

    private static class QuickLaunchIcon extends Element {
        private Bitmap icon;
        private Runnable action;
        private boolean mouseOver = false;

        QuickLaunchIcon(Bitmap icon, Runnable action, int x, int y){
            this.icon = icon;
            this.action = action;
            width = height = 23;
            this.x = x;
            this.y = y;
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            if(mouseOver)
                drawVerySimpleFrameRect(canvas, x, y, x + 23, y + 23, isPressed());
            int coord = isPressed()? 4 : 3;
            canvas.drawBitmap(icon, x + coord, y + coord, null);
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            if(0 <= x && x < width && 0 <= y && y < height){
                mouseOver = true;
                return true;
            }
            else
                return false;
        }

        @Override
        public void onMouseLeave() {
            mouseOver = false;
        }

        @Override
        public void onClick(int x, int y) {
            action.run();
        }
    }

    public static class TrayIcon extends ElementContainer {
        public Bitmap bmp;
        Runnable onClick, onDoubleClick;

        public TrayIcon(Bitmap bmp, ContextMenu contextMenu, int x, int y) {
            this.bmp = bmp;
            this.contextMenu = contextMenu;
            this.x = x;
            this.y = y;
            elements.add(contextMenu);
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            canvas.drawBitmap(bmp, x, y, null);
            drawElements(canvas, x, y);
        }

        @Override
        public boolean onSelfMouseOver(int x, int y, boolean touch) {
            return 0 <= x && x < 16 && 0 <= y && y < 16;
        }

        @Override
        public void onSelfClick(int x, int y) {
            if(onClick != null)
                onClick.run();
        }

        @Override
        public void onSelfDoubleClick(int x, int y) {
            if(onDoubleClick != null)
                onDoubleClick.run();
        }
    }


}
