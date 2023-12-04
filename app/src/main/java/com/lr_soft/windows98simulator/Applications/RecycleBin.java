package com.lr_soft.windows98simulator.Applications;

import com.lr_soft.windows98simulator.R;

public class RecycleBin extends Explorer {
    public RecycleBin(){
        super("Recycle Bin", "Recycle Bin", R.drawable.recycle_bin_empty_1, R.drawable.recycle_bin_empty_0, true);
        helpText = defaultHelpText = "This folder contains files and folders that you have deleted from your computer.";
        linkContainer.updateLinkPositions();
    }
}
