package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.System.Window;

public class ControlPanel extends Explorer {
    public ControlPanel(){
        super("Control Panel", "Control Panel", R.drawable.directory_control_panel_5, R.drawable.directory_control_panel_4, false);
        helpText = defaultHelpText = "Use the settings in Control Panel to personalize your computer. Select an item to view its description.";
        addLink(new Link("Accessibility Options",
                "Changes accessibility options for your system.",
                getBmp(R.drawable.accessibility_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Accessibility Properties", new Bitmap[]{getBmp(R.drawable.accessibility_props1), getBmp(R.drawable.accessibility_props2), getBmp(R.drawable.accessibility_props3),
                        getBmp(R.drawable.accessibility_props4), getBmp(R.drawable.accessibility_props5)}, new int[]{11, 68, 111, 157, 201, 250},
                        new Rect(121, 410, 196, 433), new Rect(202, 410, 277, 433));
            }
        }));
        addLink(new Link("Add New hardware",
                "Adds new hardware to your system.",
                getBmp(R.drawable.hardware_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new Wizard("Add New Hardware Wizard", true, new Bitmap[]{getBmp(R.drawable.hardw_wiz1), getBmp(R.drawable.hardw_wiz2), getBmp(R.drawable.hardw_wiz3), getBmp(R.drawable.hardw_wiz4)});
            }
        }));
        addLink(new Link("Add/Remove Programs",
                "Sets up programs and creates shortcuts.",
                getBmp(R.drawable.appwizard), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Add/Remove Programs Properties", new Bitmap[]{getBmp(R.drawable.add_remove_progs1), getBmp(R.drawable.add_remove_progs2), getBmp(R.drawable.add_remove_progs3)},
                        new int[]{11, 95, 182, 252},
                        new Rect(121, 427, 196, 450), new Rect(202, 427, 277, 450));
            }
        }));
        addLink(new Link("Date/Time",
                "Changes date, time and time zone information.",
                getBmp(R.drawable.time_and_date_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Date/Time Properties", new Bitmap[]{getBmp(R.drawable.date_time_props)},
                        new int[]{1, 1},
                        new Rect(176, 376, 251, 399), new Rect(257, 376, 332, 399));
            }
        }));
        addLink(new Link("Display",
                "Changes display settings.",
                getBmp(R.drawable.display_properties_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new DisplayProperties();
            }
        }));
        addLink(new Link("Fonts",
                "Views, adds and removes fonts on your computer.",
                getBmp(R.drawable.directory_fonts_shortcut_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new Explorer(Explorer.FONTS_INDEX);  // C:\Windows\Fonts
            }
        }));
        addLink(new Link("Game Controllers",
                "Adds, removes, or changes settings for game controllers",
                getBmp(R.drawable.joystick_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Game Controllers", new Bitmap[]{getBmp(R.drawable.game_controllers1), getBmp(R.drawable.game_controllers2)},
                        new int[]{11, 60, 121},
                        new Rect(239, 428, 314, 451), new Rect(320, 428, 395, 451));
            }
        }));
        addLink(new Link("Internet Options",
                "Changes your Internet settings.",
                getBmp(R.drawable.internet_options_4), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Internet Properties", new Bitmap[]{getBmp(R.drawable.internet_props1), getBmp(R.drawable.internet_props2),
                        getBmp(R.drawable.internet_props3), getBmp(R.drawable.internet_props4), getBmp(R.drawable.internet_props5), getBmp(R.drawable.internet_props6)},
                        new int[]{11, 60, 110, 159, 230, 286, 347},
                        new Rect(160, 419, 235, 442), new Rect(241, 419, 316, 442));
            }
        }));
        addLink(new Link("Keyboard",
                "Changes settings for your keyboard.",
                getBmp(R.drawable.keyboard_1), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Keyboard Properties", new Bitmap[]{getBmp(R.drawable.kbd_props1), getBmp(R.drawable.kbd_props2)},
                        new int[]{11, 54, 114},
                        new Rect(158, 415, 233, 438), new Rect(239, 415, 314, 438));
            }
        }));
        addLink(new Link("Modems",
                "Installs a new modem and changes modem settings.",
                getBmp(R.drawable.modem_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                StartMenu.createDialupNetworking();
            }
        }));
        addLink(new Link("Mouse",
                "Changes settings for your mouse.",
                getBmp(R.drawable.mouse_ms_1), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Mouse Properties", new Bitmap[]{getBmp(R.drawable.mouse_props1), getBmp(R.drawable.mouse_props2), getBmp(R.drawable.mouse_props3)},
                        new int[]{11, 59, 109, 153},
                        new Rect(158, 415, 233, 438), new Rect(239, 415, 314, 438));
            }
        }));
        addLink(new Link("Multimedia",
                "Changes settings for multimedia devices.",
                getBmp(R.drawable.multimedia_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Multimedia Properties", new Bitmap[]{getBmp(R.drawable.multimedia_props1), getBmp(R.drawable.multimedia_props2), getBmp(R.drawable.multimedia_props3),
                        getBmp(R.drawable.multimedia_props4), getBmp(R.drawable.multimedia_props5)},
                        new int[]{11, 74, 137, 196, 266, 335},
                        new Rect(121, 415, 196, 438), new Rect(202, 415, 277, 438));
            }
        }));
        addLink(new Link("Network",
                "Confugures network hardware and software.",
                getBmp(R.drawable.network), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Network", new Bitmap[]{getBmp(R.drawable.network1), getBmp(R.drawable.network2), getBmp(R.drawable.network3)},
                        new int[]{11, 85, 157, 240},
                        new Rect(202, 427, 277, 450), new Rect(283, 427, 358, 450));
            }
        }));
        addLink(new Link("ODBC Data Sources (32bit)",
                "Maintains 32 bit ODBC data sources and drivers.",
                getBmp(R.drawable.odbccp32_1439), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("ODBC Data Source Administrator", new Bitmap[]{getBmp(R.drawable.odbc1), getBmp(R.drawable.odbc2), getBmp(R.drawable.odbc3),
                        getBmp(R.drawable.odbc4), getBmp(R.drawable.odbc5), getBmp(R.drawable.odbc6), getBmp(R.drawable.odbc7)},
                        new int[]{11, 71, 143, 197, 242, 290, 394, 436},
                        new Rect(134, 344, 209, 367), new Rect(215, 344, 290, 367));
            }
        }));
        addLink(new Link("Password",
                "Changes passwords and sets security options.",
                getBmp(R.drawable.keys_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Passwords Properties", new Bitmap[]{getBmp(R.drawable.passwords1), getBmp(R.drawable.passwords2)},
                        new int[]{11, 114, 185},
                        new Rect(179, 367, 254, 390), new Rect(260, 367, 335, 390));
            }
        }));
        addLink(new Link("Power Management",
                "Changes Power Management settings.",
                getBmp(R.drawable.power_management_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Power Managements Properties", new Bitmap[]{getBmp(R.drawable.power_mng1), getBmp(R.drawable.power_mng2)},
                        new int[]{11, 100, 161},
                        new Rect(158, 415, 233, 438), new Rect(239, 415, 314, 438));
            }
        }));
        addLink(new Link("Printers",
                "Adds, removes and changes settings for printers.",
                getBmp(R.drawable.directory_printer_shortcut_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                Window window = new Printers();
                window.alignWith(ControlPanel.this);
            }
        }));
        addLink(new Link("Regional Settings",
                "Changes how numbers, currencies, date and times are displayed.",
                getBmp(R.drawable.world_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Regional Settings Properties", new Bitmap[]{getBmp(R.drawable.regional1), getBmp(R.drawable.regional2),
                        getBmp(R.drawable.regional3), getBmp(R.drawable.regional4), getBmp(R.drawable.regional5)},
                        new int[]{11, 106, 155, 209, 251, 293},
                        new Rect(158, 415, 233, 438), new Rect(239, 415, 314, 438));
            }
        }));
        addLink(new Link("Sounds",
                "Changes system and program sounds.",
                getBmp(R.drawable.computer_sound_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Sounds Properties", new Bitmap[]{getBmp(R.drawable.sounds_props)},
                        new int[]{1, 1},
                        new Rect(121, 415, 196, 438), new Rect(202, 415, 277, 438));
            }
        }));
        addLink(new Link("System",
                "Provides system information and changes advanced settings.",
                getBmp(R.drawable.computer_2), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                StartMenu.createSystemProperties();
            }
        }));
        addLink(new Link("Telephony",
                "Configure Telephony Drivers and Dialing Properties",
                getBmp(R.drawable.telephony_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new TabControlWindow("Dialing Properties", new Bitmap[]{getBmp(R.drawable.dialing_props_1), getBmp(R.drawable.dialing_props_2)},
                        new int[]{11, 86, 184},
                        new Rect(166, 435, 241, 458), new Rect(247, 435, 322, 458));
            }
        }));
        addLink(new Link("Users",
                "Sets up and manages multiple users on your computer.",
                getBmp(R.drawable.users_0), new OnClickRunnable() {
            @Override
            public void run(Element parent) {
                new Wizard("Enable Multi-user Settings", true,
                        new Bitmap[]{getBmp(R.drawable.multiuser_1), getBmp(R.drawable.multiuser_2), getBmp(R.drawable.multiuser_3), getBmp(R.drawable.multiuser_4)});
            }
        }));
        linkContainer.updateLinkPositions();
    }
}
