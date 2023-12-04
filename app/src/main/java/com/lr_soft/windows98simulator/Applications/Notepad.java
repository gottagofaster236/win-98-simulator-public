package com.lr_soft.windows98simulator.Applications;

import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.AboutWindow;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.HelpTopics;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TopMenu;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Windows98;

import java.io.File;

public class Notepad extends BaseNotepad {
    public Notepad(){
        this("Untitled");
    }
    public Notepad(String openFilename){
        this(openFilename, null);
    }
    public Notepad(File path){
        this(path.getName(), path);
    }
    public Notepad(final String openFilename, File path){
        super(openFilename + " - Notepad",
                R.drawable.notepad_0, R.drawable.notepad1, Windows98.WIDESCREEN? R.drawable.notepad2w : R.drawable.notepad2);
        initTextAndScrollBar(new RelativeBounds(6, 44, -23, -22), new RelativeBounds(-22, 44, -6, -22),
                1, 13, p_fixedsys, new Rect(0, -12, 2, 3));
        appTitle = "Notepad";
        if(path != null){
            getTxtWriter().openFile(path);
        }
        // top menu
        ButtonList file = new ButtonList();
        ButtonInList new_ = new ButtonInList("New");
        new_.action = parent -> performActionWithSaveCheck(
                () -> {
                    textBox.setText("");
                    textChanged = false;
                    openedFile = null;
                    setTitle("Untitled - Notepad");
                    textBox.parent.inputFocus = textBox;
                    textBox.setActive(true);
                });
        file.elements.add(new_);
        ButtonInList open = new ButtonInList("Open...");
        open.action = parent -> open();
        file.elements.add(open);
        ButtonInList save = new ButtonInList("Save");
        save.action = parent -> save();
        file.elements.add(save);
        ButtonInList saveAs = new ButtonInList("Save As...");
        saveAs.action = parent -> saveAs();
        file.elements.add(saveAs);
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("Page Setup..."));
        ButtonInList print = new ButtonInList("Print");
        print.disabled = true;
        file.elements.add(print);
        file.elements.add(new Separator());
        ButtonInList exit = new ButtonInList("Exit");
        exit.action = parent -> close();
        file.elements.add(exit);

        ButtonList edit = new ButtonList();
        edit.elements.add(new ButtonInList("Undo", "Ctrl+Z"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("Cut", "Ctrl+X"));
        edit.elements.add(new ButtonInList("Copy", "Ctrl+C"));
        edit.elements.add(new ButtonInList("Paste", "Ctrl+V"));
        edit.elements.add(new ButtonInList("Delete", "Del"));
        edit.elements.add(new Separator());
        for(Element el : edit.elements)
            ((ButtonInList) el).disabled = true;
        edit.elements.add(new ButtonInList("Select All"));
        edit.elements.add(new ButtonInList("Time/Date", "F5"));
        edit.elements.add(new Separator());
        ButtonInList wordWrap = new ButtonInList("Word Wrap");
        wordWrap.check = wordWrap.checkActive = true;
        edit.elements.add(wordWrap);
        edit.elements.add(new ButtonInList("Set Font..."));

        ButtonList search = new ButtonList();
        search.elements.add(new ButtonInList("Find"));
        search.elements.add(new ButtonInList("Find Next", "F3"));

        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("Help Topics");
        helpTopics.action = parent -> new HelpTopics("Notepad Help", true,
                new int[]{R.drawable.notepad_help1, R.drawable.notepad_help2, R.drawable.notepad_help3});
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("About Notepad");
        about.action = parent -> new AboutWindow(Notepad.this, "Notepad", getBmp(R.drawable.notepad_1));
        help.elements.add(about);

        TopMenu topMenu = new TopMenu();
        topMenu.elements.add(new TopMenuButton("File", file));
        topMenu.elements.add(new TopMenuButton("Edit", edit));
        topMenu.elements.add(new TopMenuButton("Search", search));
        topMenu.elements.add(new TopMenuButton("Help", help));
        setTopMenu(topMenu);
        restore();
        makeActive();
    }
}
