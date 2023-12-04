package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.CheckBox;
import com.lr_soft.windows98simulator.System.DummyWindow;

public class FileProperties extends DummyWindow {
    String filename, type, location, msDosName, size;
    Bitmap fileBmp;
    public FileProperties(Link lnk){
        this(lnk.fullFilename, lnk.helpText, lnk.fullPath,
                lnk.path != null? InternetExplorer.FileDownloadWindow.bytesToString(lnk.path.length()) : "0 bytes (0 bytes), 0 bytes used",
                lnk.icon);
    }

    public FileProperties(String filename, String type, String location, String size, Bitmap fileBmp) {
        super(Link.getSimpleFilename(filename) + " Properties", null, false, getBmp(R.drawable.properties_window),
                new Rect(121, 402, 196, 425), "OK", new Rect(202, 402, 277, 425), "Cancel");
        this.filename = shortenTextToThreeDots(Link.getSimpleFilename(filename), 256, p);
        this.type = type;
        this.location = location;
        this.msDosName = MsDos.getMsDosFilename(filename);
        this.size = size;
        this.fileBmp = fileBmp;
        addElement(new CheckBox("Read-only"), 115, 286);
        addElement(new CheckBox("Archive"), 115, 307);
        addElement(new CheckBox("Hidden"), 207, 286);
        addElement(new CheckBox("System"), 207, 307);
        addElement(new CheckBox("Enable thumbnail view"), 115, 336);
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        p.setColor(Color.BLACK);

        canvas.drawBitmap(fileBmp, x + 24, y + 62, null);
        canvas.drawText(filename, x + 86, y + 80, p);
        canvas.drawText(type, x + 86, y + 127, p);
        canvas.drawText(location, x + 86, y + 148, p);
        canvas.drawText(size, x + 86, y + 169, p);
        canvas.drawText(msDosName, x + 116, y + 233, p);
    }
}
