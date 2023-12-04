package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;

import com.lr_soft.windows98simulator.R;

public class WebFolders extends Explorer {
    public WebFolders(){
        super("Web Folders", "Web Folders", R.drawable.network_drive_world_1, R.drawable.network_drive_world_0, false);
        addLink(new Link("Add Web Folder",
                "The Add Web Folder wizard walks you through the steps to create a Web Folder. Just follow the instructions on each screen.",
                getBmp(R.drawable.template_directory_net_web_0), parent ->
                new Wizard("Add Web Folder", false, new Bitmap[]{getBmp(R.drawable.web_folder_wiz)})));
        helpText = defaultHelpText = "";
        linkContainer.updateLinkPositions();
    }
}
