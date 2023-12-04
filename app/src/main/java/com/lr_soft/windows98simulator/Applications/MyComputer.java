package com.lr_soft.windows98simulator.Applications;

import com.lr_soft.windows98simulator.MainActivity;
import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.WindowsView;

public class MyComputer extends Explorer {
    public MyComputer(){
        super("My Computer", "My Computer", R.drawable.computer_explorer_0, R.drawable.computer_explorer_2, false);
        helpText = defaultHelpText = "Select an item to view its description.";
        addLink(new Link("3½ Floppy (A:)", "3½ Inch Floppy Disk", getBmp(R.drawable.floppy_drive_3_5), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new MessageBox("My Computer", "The disk in drive A is not formatted.\n\nDo you want to format it now?",
                        MessageBox.YESNO, MessageBox.WARNING, new MessageBox.MsgResultListener() {
                    @Override
                    public void onMsgResult(int buttonNumber) {
                        if(buttonNumber == YES){  // если пользователь хочет отформатировать дискету в (A:), говорим, что это невозможно
                            WindowsView.handler.postDelayed(() -> {
                                if(MyComputer.this.closed)
                                    return;
                                new MessageBox("My Computer", "The disk in drive A cannot be formatted.",
                                        MessageBox.OK, MessageBox.WARNING, null, MyComputer.this);
                                WindowsView.windowsView.invalidate();
                            }, 500);
                        }
                    }
                }, MyComputer.this);
            }
        }));
        addLink(new Link("(C:)", "Local Disk", getBmp(R.drawable.hard_disk_drive_0), DriveC.class, this));
        addLink(new Link("Android (D:)", "CD-ROM Disk", getBmp(R.drawable.cd_drive_5), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                context.checkWriteExternalPermission(new MainActivity.PermissionResultListener() {
                    @Override
                    public void onPermissionGranted() {
                        MyDocuments driveD = MyDocuments.createDriveD();
                        driveD.alignWith(MyComputer.this);
                    }

                    @Override
                    public void onPermissionDenied() {
                        MyDocuments.diskNotAccessible(MyComputer.this);
                    }
                });
            }
        }));
        addLink(new Link("Printers", "System Folder", getBmp(R.drawable.directory_printer_shortcut_0), Printers.class, this));
        addLink(new Link("Control Panel", "System Folder", getBmp(R.drawable.directory_control_panel_4), ControlPanel.class, this));
        addLink(new Link("Dial-Up Networking", "System Folder", getBmp(R.drawable.directory_dial_up_networking_0), parent -> StartMenu.createDialupNetworking()));
        addLink(new Link("Scheduled Tasks", "System Folder", getBmp(R.drawable.directory_sched_tasks_2), SchedTasks.class, this));
        addLink(new Link("Web Folders", "Web Folders", getBmp(R.drawable.network_drive_world_0), WebFolders.class, this));
        linkContainer.updateLinkPositions();
    }
}
