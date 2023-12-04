package com.lr_soft.windows98simulator.Applications;

import android.graphics.Rect;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.AboutWindow;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.HelpTopics;
import com.lr_soft.windows98simulator.System.OnClickRunnable;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TopMenu;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Windows98;

public class WordPad extends BaseNotepad {
    public WordPad(){
        super("Document - WordPad",
                R.drawable.write_wordpad_0,  R.drawable.wordpad1, Windows98.WIDESCREEN? R.drawable.wordpad2w : R.drawable.wordpad2);
        initTextAndScrollBar(new RelativeBounds(6, 131, -22, -24), new RelativeBounds(-22, 131, -6, -24),
                15, 15, p, new Rect(0, -12, 1, 3));
        appTitle = "WordPad";
        // top menu
        ButtonList file = new ButtonList();
        ButtonInList new_ = new ButtonInList("New...", "Ctrl+N");
        new_.action = parent -> {
            performActionWithSaveCheck(() -> {
                textBox.setText("");
                textChanged = false;
                openedFile = null;
                setTitle("Document - WordPad");
                textBox.parent.inputFocus = textBox;
                textBox.setActive(true);
            });
        };
        file.elements.add(new_);
        ButtonInList open = new ButtonInList("Open...", "Ctrl+O");
        open.action = parent -> open();
        file.elements.add(open);
        OnClickRunnable saveRunnable = parent -> save();
        ButtonInList save = new ButtonInList("Save", "Ctrl+S");
        save.action = saveRunnable;
        file.elements.add(save);
        ButtonInList saveAs = new ButtonInList("Save As...");
        saveAs.action = saveRunnable;
        file.elements.add(saveAs);
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("Print...", "Ctrl+P"));
        file.elements.add(new ButtonInList("Print Preview"));
        file.elements.add(new ButtonInList("Page Setup..."));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("Send..."));
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
        edit.elements.add(new ButtonInList("Paste Special..."));
        edit.elements.add(new ButtonInList("Clear", "Del"));
        edit.elements.add(new ButtonInList("Select All", "Ctrl+A"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("Find...", "Ctrl+F"));
        edit.elements.add(new ButtonInList("Find Next", "F3"));
        edit.elements.add(new ButtonInList("Replace...", "Ctrl+H"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("Links..."));
        edit.elements.add(new ButtonInList("Object Properties", "Alt+Enter"));
        edit.elements.add(new ButtonInList("Object"));
        ButtonList view = new ButtonList();
        view.elements.add(new ButtonInList("Toolbar"));
        view.elements.add(new ButtonInList("Format Bar"));
        view.elements.add(new ButtonInList("Ruler"));
        view.elements.add(new ButtonInList("Status Bar"));
        for(Element element : view.elements){
            ButtonInList button = (ButtonInList) element;
            button.check = button.checkActive = true;
        }
        view.elements.add(new Separator());
        view.elements.add(new ButtonInList("Options..."));
        ButtonList insert = new ButtonList();
        insert.elements.add(new ButtonInList("Date and Time..."));
        insert.elements.add(new ButtonInList("Object..."));
        ButtonList format = new ButtonList();
        format.elements.add(new ButtonInList("Font..."));
        format.elements.add(new ButtonInList("Bullet Style"));
        format.elements.add(new ButtonInList("Paragraph..."));
        format.elements.add(new ButtonInList("Tabs..."));
        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("Help Topics");
        helpTopics.action = parent -> new HelpTopics("WordPad Help", true,
                new int[]{R.drawable.wordpad_help1, R.drawable.wordpad_help2, R.drawable.wordpad_help3});
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("About WordPad");
        about.action = parent -> {
            ((ButtonInList) parent).closeMenu();
            new AboutWindow(WordPad.this, "WordPad", getBmp(R.drawable.write_wordpad_1));
        };
        help.elements.add(about);

        TopMenu topMenu = new TopMenu();
        topMenu.elements.add(new TopMenuButton("File", file));
        topMenu.elements.add(new TopMenuButton("Edit", edit));
        topMenu.elements.add(new TopMenuButton("View", view));
        topMenu.elements.add(new TopMenuButton("Insert", insert));
        topMenu.elements.add(new TopMenuButton("Format", format));
        topMenu.elements.add(new TopMenuButton("Help", help));
        setTopMenu(topMenu);
        restore();
    }

    /* Задел на будущее
    private static class RichString {
        private static final int ALIGN_LEFT = -1, ALIGN_CENTER = 0, ALIGN_RIGHT = 1;

        private static class RichChar {
            char c;
            boolean bold, italic;

            public RichChar(char c, boolean bold, boolean italic) {
                this.c = c;
                this.bold = bold;
                this.italic = italic;
            }
        }

        private static class Part {
            String str;
            boolean bold, italic;
            int align;

            public Part(String str, boolean bold, boolean italic, int align) {
                this.str = str;
                this.bold = bold;
                this.italic = italic;
                this.align = align;
            }

            public RichChar charAt(int index){
                return new RichChar(str.charAt(index), bold, italic);
            }

            public int length(){
                return str.length();
            }
        }

        List<Part> parts = new ArrayList<>();

        public RichChar charAt(int index) {
            for(Part part : parts){
                if(part.length() < index)
                    return part.charAt(index);
                else
                    index -= part.length();
            }
            return null;
        }

        
    }
    */
}
