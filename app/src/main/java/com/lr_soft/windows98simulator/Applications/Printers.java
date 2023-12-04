package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;

import com.lr_soft.windows98simulator.R;

public class Printers extends Explorer {
    public Printers(){
        super("Printers", "Printers", R.drawable.directory_printer_4, R.drawable.directory_printer_5, false);
        helpText = defaultHelpText = "This folder contains information about your current printers and a wizard to help you install the new ones.";
        addLink(new Link("Add Printer",
                "The Add Printer wizard walks you step-by-step through installing a printer. Just follow the instructions on each screen.",
                getBmp(R.drawable.template_printer_0), parent -> {
                    onOtherTouch();
                    new Wizard("Add Printer Wizard", true, new Bitmap[]{getBmp(R.drawable.add_printer_1), getBmp(R.drawable.add_printer_2),
                            getBmp(R.drawable.add_printer_3), getBmp(R.drawable.add_printer_4), getBmp(R.drawable.add_printer_5), getBmp(R.drawable.add_printer_6)});
                }));
        linkContainer.updateLinkPositions();
    }
}
