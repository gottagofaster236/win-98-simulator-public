package com.lr_soft.windows98simulator.Applications;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;

import com.lr_soft.windows98simulator.BuildConfig;
import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.CloseableMenu;
import com.lr_soft.windows98simulator.System.DummyWindow;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TextBox;
import com.lr_soft.windows98simulator.System.Windows98;

public class StartMenu extends Element implements CloseableMenu {
    private ButtonList child;
    private boolean showChild = false;
    public static Bitmap startButtonPressed, startButton, windowsSlider, emptyButton, emptyButtonPressed, windowsUpdateButton, windowsUpdateButtonPressed;
    public static Bitmap directoryClosed, soundYel, paint, solitaire, winmine, html, mPlayer;
    private boolean lastMouseOverSelf = false;

    public StartMenu() {
        startButton = getBmp(R.drawable.start_button);
        startButtonPressed = getBmp(R.drawable.start_button_pressed);
        windowsSlider = getBmp(R.drawable.windows_slider);
        emptyButton = getBmp(R.drawable.empty_button);
        emptyButtonPressed = getBmp(R.drawable.empty_button_pressed);
        windowsUpdateButton = getBmp(R.drawable.windows_update_button);
        windowsUpdateButtonPressed = getBmp(R.drawable.windows_update_button_pressed);
        Bitmap programGroup = getBmp(R.drawable.directory_program_group_small_1);
        directoryClosed = getBmp(R.drawable.directory_closed_2);
        html = getBmp(R.drawable.html_0);
        soundYel = getBmp(R.drawable.sound_yel_0);
        paint = getBmp(R.drawable.paint_0);
        winmine = getBmp(R.drawable.game_mine);
        solitaire = getBmp(R.drawable.game_solitaire_1);
        mPlayer = Element.getBmp(R.drawable.mplayer2_110_1);
        // 4-й уровень вложенности
        ButtonList communications = new ButtonList();
        ButtonInList dialUpNetw = new ButtonInList("Dial-Up Networking", getBmp(R.drawable.directory_dial_up_networking_2), false);
        dialUpNetw.action = parent -> createDialupNetworking();
        communications.elements.add(dialUpNetw);
        communications.elements.add(new ButtonInList("Phone Dialer",
                getBmp(R.drawable.dialer_1_1), false));
        ButtonList entertainment = new ButtonList();
        entertainment.elements.add(new ButtonInList("CD Player", false,
                R.drawable.cdplayer_107_1, "CD Player", false, R.drawable.cd_player));
        entertainment.elements.add(new ButtonInList("Interactive CD Sampler", false,
                R.drawable.imgstart_103, "Microsoft Interactive CD Sampler", R.drawable.interactive_cd_sampler, new Rect(65, 115, 140, 138), "OK",
                new Rect(147, 115, 222, 138), "Cancel"));
        entertainment.elements.add(new ButtonInList("Sound Recorder", false,
                R.drawable.sndrec32_10_1, "Sound - Sound Recorder", false, R.drawable.sndrec));
        ButtonInList volumeControl = new ButtonInList("Volume Control", getBmp(R.drawable.sndvol32_300_1), false);
        volumeControl.action = parent -> createVolumeControl();
        entertainment.elements.add(volumeControl);
        entertainment.elements.add(new ButtonInList("Windows Media Player", getBmp(R.drawable.mplayer2_110_1), false, MPlayer.class));
        ButtonList games = new ButtonList();
        games.elements.add(new ButtonInList("FreeCell", getBmp(R.drawable.freecell), false, FreeCell.class));
        games.elements.add(new ButtonInList("Hearts", false,
                R.drawable.game_hearts, "The Microsoft Hearts Network", false, R.drawable.hearts));
        games.elements.add(new ButtonInList("Minesweeper", winmine, false, Minesweeper.class));
        games.elements.add(new ButtonInList("Solitaire", solitaire, false, Solitaire.class));
        games.elements.add(new ButtonInList("Spider Solitaire", getBmp(R.drawable.spider_icon_small), false, Spider.class));
        ButtonList internetTools = new ButtonList();
        internetTools.elements.add(new ButtonInList("Internet Connection Wizard", false,
                R.drawable.internet_connection_wiz_1, "Internet Connection Wizard", R.drawable.internet_conn_wiz, new Rect(454, 405, 529, 428), "Cancel"));
        internetTools.elements.add(new ButtonInList("NetMeeting", false,
                R.drawable.netmeeting_1, "NetMeeting", R.drawable.net_meeting, new Rect(359, 291, 434, 314), "Cancel"));
        internetTools.elements.add(new ButtonInList("Personal Web Server",
                html, false));
        ButtonList systemTools = new ButtonList();
        systemTools.elements.add(new ButtonInList("Disk Cleanup",
                getBmp(R.drawable.clean_drive_2), false));
        systemTools.elements.add(new ButtonInList("Disk Defragmenter",
                getBmp(R.drawable.defragment_1), false));
        systemTools.elements.add(new ButtonInList("Drive Converter (FAT32)",
                getBmp(R.drawable.cvt1_128_1), false));
        systemTools.elements.add(new ButtonInList("Maintenance Wizard",
                getBmp(R.drawable.tune_up_1), false));
        systemTools.elements.add(new ButtonInList("ScanDisk",
                getBmp(R.drawable.scandisk_1), false));
        systemTools.elements.add(new ButtonInList("Scheduled Tasks",
                getBmp(R.drawable.directory_sched_tasks_0), false, SchedTasks.class));
        systemTools.elements.add(new ButtonInList("System Information",
                getBmp(R.drawable.msinfo32_0), false));
        systemTools.elements.add(new ButtonInList("Welcome To Windows",
                getBmp(R.drawable.welcome_102_1), false));
        // 3-й уровень вложенности
        ButtonList accessories = new ButtonList();
        accessories.elements.add(new ButtonInList("Communications", programGroup, false, communications));
        accessories.elements.add(new ButtonInList("Entertainment", programGroup, false, entertainment));
        accessories.elements.add(new ButtonInList("Games", programGroup, false, games));
        accessories.elements.add(new ButtonInList("Internet Tools", programGroup, false, internetTools));
        accessories.elements.add(new ButtonInList("System Tools", programGroup, false, systemTools));
        accessories.elements.add(new ButtonInList("Address Book", false,
                R.drawable.address_book_1, "Address Book - Main Identity", true, R.drawable.address_book));
        accessories.elements.add(new ButtonInList("Calculator",
                getBmp(R.drawable.calculator_1), false, Calculator.class));
        accessories.elements.add(new ButtonInList("Imaging", false,
                R.drawable.kodak_imaging_1, "Imaging", true, R.drawable.imaging1));
        accessories.elements.add(new ButtonInList("Notepad", getBmp(R.drawable.notepad_0), false, Notepad.class));
        accessories.elements.add(new ButtonInList("Paint", paint, false, PaintBrush.class));
        ButtonInList wordpad = new ButtonInList("WordPad",
                getBmp(R.drawable.write_wordpad_0), false, WordPad.class);
        accessories.elements.add(wordpad);
        ButtonList onlineServices = new ButtonList();
        onlineServices.elements.add(new ButtonInList("America Online",
                getBmp(R.drawable.aolsetup_300_2), false));
        onlineServices.elements.add(new ButtonInList("AT&T WorldNet Service",
                getBmp(R.drawable.at_and_t), false));
        onlineServices.elements.add(new ButtonInList("CompuServe",
                getBmp(R.drawable.cssetup_0_1), false));
        onlineServices.elements.add(new ButtonInList("Prodigy Internet",
                getBmp(R.drawable.pro_orb_1), false));
        ButtonList startUp = new ButtonList();
        ButtonList activeDesktop = new ButtonList();
        activeDesktop.elements.add(new ButtonInList("View as Web Page", null, false));
        activeDesktop.elements.add(new ButtonInList("Customize my Desktop", null, false, DisplayProperties.class));
        activeDesktop.elements.add(new ButtonInList("Update Now", null, false));
        // 2-й уровень вложенности
        ButtonList programs = new ButtonList();
        programs.elements.add(new ButtonInList("Accessories", programGroup, false, accessories));
        programs.elements.add(new ButtonInList("Online Services", programGroup, false, onlineServices));
        programs.elements.add(new ButtonInList("StartUp", programGroup, false, startUp));
        programs.elements.add(new ButtonInList("Internet Explorer",
                getBmp(R.drawable.msie1_3), false, InternetExplorer.class));
        ButtonInList msDos = new ButtonInList("MS-DOS Prompt", getBmp(R.drawable.ms_dos), false, MsDos.class);
        programs.elements.add(msDos);
        ButtonInList outlookExpress = new ButtonInList("Outlook Express", getBmp(R.drawable.outlook_express_2), false);
        outlookExpress.action = parent -> createOutlookExpress();
        programs.elements.add(outlookExpress);
        programs.elements.add(new ButtonInList("Windows Explorer", getBmp(R.drawable.directory_explorer_1), false, DriveC.class));
        ButtonList favorites = getFavoritesMenu();
        ButtonList documents = new ButtonList();
        documents.elements.add(new ButtonInList("My Documents", getBmp(R.drawable.directory_open_file_mydocs_small_1), false, MyDocuments.class));
        /*documents.elements.add(new Separator());
        ButtonInList chimes = new ButtonInList("Chimes", soundYel, false);
        chimes.r = mplayerRunnable;
        documents.elements.add(chimes);
        ButtonInList microsoftSound = new ButtonInList("The Microsoft Sound", soundYel, false);
        microsoftSound.r = mplayerRunnable;
        documents.elements.add(microsoftSound);*/
        ButtonList settings = new ButtonList();
        settings.elements.add(new ButtonInList("Control Panel", getBmp(R.drawable.directory_control_panel_5), false, ControlPanel.class));
        settings.elements.add(new ButtonInList("Printers", getBmp(R.drawable.directory_printer_4), false, Printers.class));
        ButtonInList taskbarAndStartmenu = new ButtonInList("Taskbar & Start Menu", getBmp(R.drawable.windows_button_1), false);
        taskbarAndStartmenu.action = parent -> StartMenu.createTaskbarProps();
        settings.elements.add(taskbarAndStartmenu);
        settings.elements.add(new ButtonInList("Folder Options",
                getBmp(R.drawable.directory_explorer_1), false));  // в оригинале - вылезает за экран
        settings.elements.add(new ButtonInList("Active Desktop",
                getBmp(R.drawable.desktop_3), false, activeDesktop));
        settings.elements.add(new Separator());
        OnClickRunnable windowsUpdateRunnable = new OnClickRunnable() {
            @Override
            public void run(Element parent) {  // открываем страницу с нашим приложением
                final String appPackageName = BuildConfig.APPLICATION_ID;
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException e) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
            }
        };
        ButtonInList windowsUpdate = new ButtonInList("Windows Update...",
                getBmp(R.drawable.windows_update_small_0), false);
        windowsUpdate.action = windowsUpdateRunnable;
        settings.elements.add(windowsUpdate);
        ButtonList find = new ButtonList();
        ButtonInList findAllFiles = new ButtonInList("Files or Folders", getBmp(R.drawable.search_file_2), false);
        findAllFiles.action = parent -> {
            BaseNotepad findAllFiles1 = new BaseNotepad("Find: All Files",
                    new Rect(114, 95, 305, 112), 2, 12, p, new Rect(-1, -11, 0, 3),
                    R.drawable.search_file_2, R.drawable.find_all_files);
            TextBox containingText = new TextBox(new Rect(114, 123, 321, 142), 2, 12, p, new Rect(-1, -11, 0, 2));
            containingText.setActive(false);  // так как уже есть один TextBox
            findAllFiles1.addElement(containingText);
        };
        find.elements.add(findAllFiles);
        ButtonInList findComputer = new ButtonInList("Computer...", getBmp(R.drawable.search_computer_1), false);
        findComputer.action = parent ->
                new BaseNotepad("Find: Computer",
                new Rect(78, 88, 307, 105), 2, 12, p, new Rect(-1, -11, 0, 3),
                R.drawable.search_computer_1, R.drawable.find_computer);
        find.elements.add(findComputer);
        find.elements.add(new ButtonInList("On the Internet",
                getBmp(R.drawable.search_web_1), false, InternetExplorer.class));
        ButtonInList findPeople = new ButtonInList("People", getBmp(R.drawable.search_people), false);
        findPeople.action = parent -> {
            BaseNotepad findPeople1 = new BaseNotepad("Find: People", true,
                    new Rect(84, 100, 316, 119), 2, 12, p, new Rect(-1, -11, 0, 2),
                    R.drawable.find_people, new Rect(340, 223, 468, 246), "Close");
            TextBox email = new TextBox(new Rect(84, 127, 316, 146), 2, 12, p, new Rect(-1, -11, 0, 2));
            TextBox address = new TextBox(new Rect(84, 155, 316, 174), 2, 12, p, new Rect(-1, -11, 0, 2));
            TextBox phone = new TextBox(new Rect(84, 183, 316, 202), 2, 12, p, new Rect(-1, -11, 0, 2));
            TextBox other = new TextBox(new Rect(84, 210, 316, 229), 2, 12, p, new Rect(-1, -11, 0, 2));
            email.setActive(false);  // так как уже есть один TextBox
            address.setActive(false);
            phone.setActive(false);
            other.setActive(false);
            findPeople1.addElement(email);
            findPeople1.addElement(address);
            findPeople1.addElement(phone);
            findPeople1.addElement(other);
        };
        find.elements.add(findPeople);
        // 1-й уровень вложенности
        child = new ButtonList();
        child.isStartMenu = true;
        ButtonInList wUpdate = new ButtonInList("Windows Update", null, true);

        wUpdate.height = windowsUpdateButton.getHeight();
        wUpdate.isWindowsUpdate = true;
        wUpdate.action = windowsUpdateRunnable;
        child.elements.add(wUpdate);
        child.elements.add(new Separator());
        child.elements.add(new ButtonInList("Programs",
                getBmp(R.drawable.directory_program_group_small_0), true, programs));
        child.elements.add(new ButtonInList("Favorites",
                getBmp(R.drawable.directory_favorites_small_0), true, favorites));
        child.elements.add(new ButtonInList("Documents",
                getBmp(R.drawable.directory_open_file_mydocs_small_0), true, documents));
        child.elements.add(new ButtonInList("Settings",
                getBmp(R.drawable.settings_gear_0), true, settings));
        child.elements.add(new ButtonInList("Find",
                getBmp(R.drawable.search_file_1), true, find));
        ButtonInList help = new ButtonInList("Help", getBmp(R.drawable.help_book), true);
        help.action = parent -> createWindowsHelp();
        child.elements.add(help);
        ButtonInList run = new ButtonInList("Run...", getBmp(R.drawable.run), true);
        run.action = parent -> new Run();
        child.elements.add(run);
        child.elements.add(new Separator());
        final ButtonInList logoff = new ButtonInList("Log Off User...", getBmp(R.drawable.key_win_0), true, ShutDownWindow.class);
        ButtonInList shutdown = new ButtonInList("Shut Down...", getBmp(R.drawable.shut_down_normal_0), true);
        OnClickRunnable shutdownRunnable = parent -> new ShutDownWindow(parent == logoff);
        shutdown.action = logoff.action = shutdownRunnable;
        child.elements.add(logoff);
        child.elements.add(shutdown);
    }

    public static ButtonList getFavoritesMenu() {  // так как ещё используется в Explorer'е
        ButtonList channels = new ButtonList();
        ButtonList links = new ButtonList();
        links.elements.add(new ButtonInList("Best of the Web", html, false));
        links.elements.add(new ButtonInList("Chanel Guide", html, false));
        links.elements.add(new ButtonInList("Customize links", html, false));
        links.elements.add(new ButtonInList("Free HotMail", html, false));
        links.elements.add(new ButtonInList("Internet Start", html, false));
        links.elements.add(new ButtonInList("Microsoft", html, false));
        links.elements.add(new ButtonInList("Windows Update", html, false));
        links.elements.add(new ButtonInList("Windows", html, false));
        ButtonList media = new ButtonList();
        media.elements.add(new ButtonInList("ABC News and Entertainment", html, false));
        media.elements.add(new ButtonInList("Bloomberg", html, false));
        media.elements.add(new ButtonInList("Capitol Records", html, false));
        media.elements.add(new ButtonInList("CBS", html, false));
        media.elements.add(new ButtonInList("CNBC Dow Jones Business Video", html, false));
        media.elements.add(new ButtonInList("CNET Today - Technology News", html, false));
        media.elements.add(new ButtonInList("CNN Videoselect", html, false));
        media.elements.add(new ButtonInList("Disney", html, false));
        media.elements.add(new ButtonInList("ESPN Sports", html, false));
        media.elements.add(new ButtonInList("Fox News", html, false));
        media.elements.add(new ButtonInList("Fox Sports", html, false));
        media.elements.add(new ButtonInList("Hollywood Online", html, false));
        media.elements.add(new ButtonInList("Internet Radio Guide", html, false));
        media.elements.add(new ButtonInList("MSNBC", html, false));
        media.elements.add(new ButtonInList("MUSICVIDEOS.COM", html, false));
        media.elements.add(new ButtonInList("NBC VideoSeeker", html, false));
        media.elements.add(new ButtonInList("TV Guide Entertainment Network", html, false));
        media.elements.add(new ButtonInList("Universal Studios Online", html, false));
        media.elements.add(new ButtonInList("Warner Bros. Hip Clips", html, false));
        media.elements.add(new ButtonInList("What's On Now", html, false));
        media.elements.add(new ButtonInList("Windows Media Showcase", html, false));
        ButtonList favorites = new ButtonList();
        favorites.elements.add(new ButtonInList("Channels", directoryClosed, false, channels));
        favorites.elements.add(new ButtonInList("Links", directoryClosed, false, links));
        favorites.elements.add(new ButtonInList("Media", directoryClosed, false, media));
        favorites.elements.add(new ButtonInList("MSN", html, false));
        favorites.elements.add(new ButtonInList("Radio Station Guide", html, false));
        favorites.elements.add(new ButtonInList("Web Events", html, false));
        return favorites;
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        child.parentButton = this;
        if (2 <= x && x <= 55 && Windows98.TASKBAR_Y + 4 <= y && y <= Windows98.TASKBAR_Y + 25) {  // нажатие на саму кнопку Start
            lastMouseOverSelf = true;
            if (!touch)
                return true;
            showChild = !showChild;

            if (showChild)
                child.reset();
            return true;
        } else {
            lastMouseOverSelf = false;
            if (showChild)
                return child.onMouseOver(x - child.x, y - child.y, touch);
            else
                return false;
        }
    }

    @Override
    public void onOtherTouch() {
        showChild = false;
        lastMouseOverSelf = false;
    }

    @Override
    public void onMouseLeave() {
        lastMouseOverSelf = false;
        if (showChild)
            child.onMouseLeave();
    }

    @Override
    public void onClick(int x, int y) {
        if (showChild && !lastMouseOverSelf && child.onMouseOver(x - child.x, y - child.y, false))
            child.onClick(x - child.x, y - child.y);
    }

    @Override
    public void onDraw(Canvas canvas, int x, int y) {
        // отрисовка кнопки
        canvas.drawBitmap(showChild? startButtonPressed : startButton, 2, Windows98.TASKBAR_Y + 4, null);
        if (showChild) {
            child.parentButton = this;
            if (child.height == -1)
                child.onDraw(new Canvas(), 0, 0);
            child.x = 2;
            child.y = Windows98.TASKBAR_Y + 4 - child.height;
            child.onDraw(canvas, child.x, child.y);
        }
    }

    @Override
    public void closeMenu() {
        showChild = false;
    }

    public static void createTaskbarProps() {
        new TabControlWindow("Taskbar Properties", new Bitmap[]{getBmp(R.drawable.taskbar_props_1), getBmp(R.drawable.taskbar_props_2)}, new int[]{11, 101, 212},
                new Rect(98, 367, 173, 390), new Rect(179, 367, 254, 390));
    }

    public static void createDialupNetworking() {
        new Wizard("Install New Modem", false, new Bitmap[]{getBmp(R.drawable.dial_up_netw_1),
                getBmp(R.drawable.dial_up_netw_2), getBmp(R.drawable.dial_up_netw_3), getBmp(R.drawable.dial_up_netw_4)});
    }

    public static void createSystemProperties() {
        new TabControlWindow("System Properties", new Bitmap[]{getBmp(R.drawable.system_props1), getBmp(R.drawable.system_props2),
                getBmp(R.drawable.system_props3), getBmp(R.drawable.system_props4)},
                new int[]{11, 60, 151, 246, 318},
                new Rect(245, 415, 320, 438), new Rect(326, 415, 401, 438));
    }

    public static void createOutlookExpress(){
        new DummyWindow("Inbox - Outlook Express", getBmp(R.drawable.outlook_express_2),
                true, getBmp(R.drawable.outlook1), getBmp(Windows98.WIDESCREEN? R.drawable.outlook2w : R.drawable.outlook2));
    }

    public static void createVolumeControl(){
        new DummyWindow("Volume Control", getBmp(R.drawable.sndvol32_300_1), false, getBmp(R.drawable.volume_control));
    }

    public static void createWindowsHelp(){
        new DummyWindow("Windows Help", Element.getBmp(R.drawable.help_topics), true,
                Element.getBmp(R.drawable.win_help1), Element.getBmp(R.drawable.win_help2));
    }
}
