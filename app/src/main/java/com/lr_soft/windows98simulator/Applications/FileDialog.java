package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.lr_soft.windows98simulator.MainActivity;
import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.Button;
import com.lr_soft.windows98simulator.System.DialogWindow;
import com.lr_soft.windows98simulator.System.DropdownList;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.ScrollBar;
import com.lr_soft.windows98simulator.System.TextBox;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileDialog extends DialogWindow {
    private String[] extensions;
    private String fileDescription;
    TextBox textBox;
    private OnResultListener onResultListener;
    public boolean open;
    private File directory;
    private String directoryString;
    private boolean allowExternalStorageDirectory;  // может ли пользователь переключиться на Android (D:)
    public Explorer.LinkContainer linkContainer;
    private Bitmap directoryBmp;
    private Bitmap backgroundBmp;  // см. DummyWindow

    public FileDialog(boolean open, String[] extensions, String fileDescription, Window parentWindow, OnResultListener onResultListener) {
        this(open, extensions, fileDescription, parentWindow, onResultListener, MyDocuments.getFilesDir());
    }

    public FileDialog(boolean open, String extension, String fileDescription, Window parentWindow, OnResultListener onResultListener, File directory){
        this(open, new String[]{extension}, fileDescription, parentWindow, onResultListener, directory);
    }

    public FileDialog(boolean open, String[] extensions, String fileDescription, Window parentWindow, OnResultListener onResultListener, final File directory){
        this(open, extensions, fileDescription, parentWindow, onResultListener, directory, open? "" : "Untitled", true);
    }

    public FileDialog(boolean open, String extension, String fileDescription, Window parentWindow, OnResultListener onResultListener,
                      String defaultFilename, boolean allowExternalStorageDirectory){
        this(open, new String[]{extension}, fileDescription, parentWindow, onResultListener, MyDocuments.getFilesDir(), defaultFilename, allowExternalStorageDirectory);
    }

    public FileDialog(boolean open, String[] extensions, String fileDescription, Window parentWindow, OnResultListener onResultListener,
                      final File directory, String defaultFilename, boolean allowExternalStorageDirectory){
        //super(open? "Open" : "Save as", null, true, getBmp(open? R.drawable.open_file_dialog : R.drawable.save_file_dialog),
        //        new Rect(337, 349, 412, 372), open? "Open" : "Save", new Rect(337, 378, 412, 401), "Close");/
        super(open? "Open" : "Save as", 428, 413, true, parentWindow);
        backgroundBmp = getBmp(open? R.drawable.open_file_dialog : R.drawable.save_file_dialog);
        defaultButton = new Button(open? "Open" : "Save", new Rect(337, 349, 412, 372), parent -> {
            if(FileDialog.this.open)
                onOpenPress();
            else
                onSavePress();
        });
        defaultButton.coolActive = true;
        addElement(defaultButton);
        addCloseButton(new Rect(337, 378, 412, 401), "Cancel");
        fillWindow = false;

        this.extensions = extensions;
        this.fileDescription = fileDescription;
        this.onResultListener = onResultListener;
        this.open = open;
        //directory = MyDocuments.getFilesDir();
        this.directory = directory;
        this.allowExternalStorageDirectory = allowExternalStorageDirectory;
        directoryString = shortenTextToThreeDots(MyDocuments.getDirectoryName(directory), 161, p);
        directoryBmp = getBmp(MyDocuments.getSmallIcon(directory));

        textBox = new TextBox(new Rect(87, 352, 316, 368), 2, 12, p, new Rect(-1, -11, 0, 2));
        textBox.setText(defaultFilename);
        textBox.selectOnActive = true;
        textBox.deleteLongText = true;
        textBox.selectAll();
        textBox.enterRunnable = () -> {
            if(FileDialog.this.open)
                onOpenPress();
            else
                onSavePress();
        };
        inputFocus = textBox;
        addElement(textBox);

        DropdownList fileExtensionsList = new DropdownList(new String[]{fileDescription, "All Files (*.*)"}, 0, 233);
        addElement(fileExtensionsList, 85, 380);

        // кнопки сверху
        addElement(new ImageButton(getBmp(R.drawable.folder_up), () -> {
            File parentDirectory;
            if(directory.equals(MyDocuments.getDesktopDirectory()))
                return;
            if(directory.equals(MyDocuments.getExternalStorageDirectory()))
                return;
            else if(directory.equals(MyDocuments.getFilesDir()))
                parentDirectory = MyDocuments.getDesktopDirectory();
            else
                parentDirectory = directory.getParentFile();
            new FileDialog(parentDirectory, FileDialog.this);
        }), 270, 28);

        addElement(new ImageButton(getBmp(R.drawable.desktop_3), () -> {
            if(directory.equals(MyDocuments.getDesktopDirectory()))
                return;
            new FileDialog(MyDocuments.getDesktopDirectory(), FileDialog.this);
        }), 301, 28);

        addElement(new ImageButton(getBmp(R.drawable.new_folder), () -> {
            linkContainer.createFile("New Folder", "");
        }), 332, 28);

        Explorer.LinkContainer.FileProvider fileProvider = new Explorer.LinkContainer.FileProvider() {
            @Override
            public boolean acceptFile(File file) {
                return file.isDirectory() || checkExtension(file.getName());
            }
        };

        linkContainer = new Explorer.LinkContainer(new ArrayList<Link>(), new Rect(12, 58, 400, 339), null);
        addElement(linkContainer);
        ScrollBar scrollBar = new ScrollBar(linkContainer, new Rect(400, 58, 416, 339), true);
        linkContainer.scrollBar = scrollBar;
        elements.add(0, scrollBar);
        linkContainer.initFromDirectory(directory, fileProvider, this);

        FolderDropdownList folderDropdownList = new FolderDropdownList(this);
        addElement(folderDropdownList, 58, 28);

        bringClickedElementToFront = true;
    }

    FileDialog(File directory, FileDialog old){  // для перехода по папкам
        // т. к. первый вызов должен быть super, а нам надо закрыть old
        this(tmpOpen(old), old.extensions, old.fileDescription, old.parentWindow, old.onResultListener,
                directory, old.textBox.text, old.allowExternalStorageDirectory);
        alignWith(old);
        parentWindow.makeActive();
    }

    private static boolean tmpOpen(FileDialog fileDialog){
        fileDialog.forceClose();
        return fileDialog.open;
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        canvas.drawBitmap(backgroundBmp, x, y, null);
        /*p.setColor(Color.BLACK);
        canvas.drawBitmap(directoryBmp, x + 64, y + 31, null);
        canvas.drawText(directoryString, x + 85, y + 43, p);*/
        super.onNewDraw(canvas, x, y);
    }

    void onSavePress(){
        String filename = textBox.text.trim();
        if(filename.isEmpty())
            return;
        if(openFolder(filename))
            return;
        if((filename = addExtension(filename)) == null)
            return;
        if(!checkFilename(filename)) {
            new MessageBox(getTitle(), MyDocuments.getFullPath(directory) + "\\" + filename + "\nThe above file name is invalid.", MessageBox.OK, MessageBox.WARNING, null, this);
            return;
        }
        final File path = new File(directory, filename);
        if(path.exists()) {
            if (path.isDirectory())
                new FileDialog(path, this);
            else
                new MessageBox("Save As", MyDocuments.getFullPath(directory) + "\\" + filename + " already exists.\nDo you want to replace it?",
                        MessageBox.YESNO, MessageBox.WARNING, new MessageBox.MsgResultListener() {
                    @Override
                    public void onMsgResult(int buttonNumber) {
                        if (buttonNumber == YES) {
                            FileDialog.super.close(true);  // т. к. close() в FileDialog обнуляет actionOnSave
                            onResultListener.writeToFile(path);
                            if (parentWindow instanceof ActionOnSave)
                                ((ActionOnSave) parentWindow).runActionOnSave();
                        }
                    }
                }, this);
        }
        else{
            FileDialog.super.close(true);  // т. к. close() в FileDialog обнуляет actionOnSave
            onResultListener.writeToFile(path);
            if(parentWindow instanceof ActionOnSave)
                ((ActionOnSave) parentWindow).runActionOnSave();
        }
    }

    void onOpenPress(){
        String filename = textBox.text.trim();
        if(filename.isEmpty())
            return;
        if(openFolder(filename))
            return;
        if((filename = addExtension(filename)) == null)
            return;
        if(!checkFilename(filename)) {
            new MessageBox(getTitle(), MyDocuments.getFullPath(directory) + "\\" + filename + "\nThe above file name is invalid.", MessageBox.OK, MessageBox.WARNING, null, this);
            return;
        }
        final File path = new File(directory, filename);
        if(path.exists()) {
            if(path.isDirectory())
                new FileDialog(path, this);
            else {
                close();
                onResultListener.openFile(path);
            }
        }
        else
            new MessageBox(getTitle(), filename + "\nFile not found.\nPlease verify the correct file name was given.",
                    MessageBox.OK, MessageBox.WARNING, null, this);
    }

    private boolean openFolder(String filename){
        // пытаемся открыть filename как папку
        if(!checkFilename(filename))
            return false;
        File folder = new File(directory, filename);
        if(folder.exists() && folder.isDirectory()){
            new FileDialog(folder, this);
            return true;
        }
        else
            return false;
    }

    static boolean checkFilename(String filename){
        return !filename.startsWith(".") && !filename.contains("<") && !filename.contains(">") && !filename.contains(":") && !filename.contains("\"") &&
                !filename.contains("/") && !filename.contains("\\") && !filename.contains("|") && !filename.contains("?") && !filename.contains("*");
    }

    private String addExtension(String filename){
        if(checkExtension(filename))
            return filename;
        else{
            String result = null;
            for(Element link : linkContainer.elements){
                String linkName = ((Link) link).fullFilename;
                if(checkExtension(linkName)){
                    String name = linkName.substring(0, linkName.lastIndexOf('.'));
                    if(!name.equalsIgnoreCase(filename))
                        continue;
                    if(result == null)
                        result = linkName;
                    else{  // нашлось 2 файла с таким названием (и с разными расширениями)
                        new MessageBox(getTitle(), "More than one file has the name '" + filename +
                                "'.\n\nChoose one from the list of files.", MessageBox.OK, MessageBox.WARNING, null, this);
                        return null;
                    }
                }
            }
            if(result != null)
                return result;
            else
                return filename + "." + extensions[0];
        }
    }

    private boolean checkExtension(String filename){
        String ourExt = Link.getExtension(filename);
        for(String ext : extensions){
            if(ext.equals(ourExt))
                return true;
        }
        return false;
    }

    @Override
    public void close(boolean activateNextWindow) {
        super.close(activateNextWindow);
        onResultListener.onCancel();
        if(parentWindow instanceof ActionOnSave)
            ((ActionOnSave) parentWindow).setActionOnSave(null);
    }

    private static class ImageButton extends Element{
        Bitmap bmp;  // 16x16
        Runnable action;

        public ImageButton(Bitmap bmp, Runnable action) {
            this.bmp = bmp;
            this.action = action;
            width = 23;
            height = 22;
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            if(!isPressed()) {
                drawFrameRect(canvas, x, y, x + width, y + height, true, true);
                canvas.drawBitmap(bmp, x + 3, y + 3, null);
            }
            else {
                drawFrameRectActive(canvas, x, y, x + width, y + height);
                canvas.drawBitmap(bmp, x + 4, y + 4, null);
            }
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
            return 0 <= x && x < width && 0 <= y && y < height;
        }

        @Override
        public void onClick(int x, int y) {
            action.run();
        }
    }

    public static class FolderDropdownList extends DropdownList {
        private Explorer.LinkContainer linkContainer;
        private static int tmpSelectedItem;  // т. к. первый вызов к super
        //private Map<Link, Class<? extends Explorer>> targetWindows;

        private FolderDropdownList(FileDialog fileDialog){
            this(fileDialog.linkContainer, 207,
                    fileDialog.directory, fileDialog.directoryBmp, fileDialog.directoryString,
                    fileDialog.allowExternalStorageDirectory, true);
        }

        public FolderDropdownList(Explorer.LinkContainer linkContainer, int width,
                                  File directory, Bitmap directoryBmp, String directoryString,
                           boolean allowExternalStorageDirectory, boolean drawTitle){
            super(getItems(directory, directoryBmp, directoryString,
                    allowExternalStorageDirectory),
                    tmpSelectedItem, width, false);
            this.linkContainer = linkContainer;
            this.drawTitle = drawTitle;
        }

        static Link[] getItems(File directory, Bitmap directoryBmp, String directoryString, boolean allowExternalStorageDirectory){
            List<Link> links = new ArrayList<>();

            Link desktop = Link.createExplorerDropdownLink("Desktop", getBmp(R.drawable.desktop_3));
            desktop.path = MyDocuments.getDesktopDirectory();
            links.add(desktop);

            Link myDocs = Link.createExplorerDropdownLink("My Documents", getBmp(R.drawable.directory_open_file_mydocs_2));
            myDocs.path = MyDocuments.getFilesDir();
            links.add(myDocs);

            if(allowExternalStorageDirectory) {
                File externalStorageDirectory = MyDocuments.getExternalStorageDirectory();
                if (externalStorageDirectory != null) {
                    Link driveD = Link.createExplorerDropdownLink("Android (D:)", getBmp(R.drawable.cd_drive_4));
                    driveD.path = externalStorageDirectory;
                    links.add(driveD);
                }
            }

            int selectedItem = -1;
            for(int i = 0; i < links.size(); i++){
                if(links.get(i).path.equals(directory)){
                    selectedItem = i;
                    break;
                }
            }

            if(selectedItem == -1){
                Link newLink = Link.createExplorerDropdownLink(directoryString, directoryBmp);
                newLink.path = directory;
                int newIndex;
                if(newLink.path == null)
                    newIndex = 0;
                else if(MyDocuments.isInFilesDir(newLink.path))
                    newIndex = links.indexOf(myDocs) + 1;
                else
                    newIndex = links.size();
                links.add(newIndex, newLink);
                selectedItem = newIndex;
            }

            // костыль, т. к. Link ожидает, что parent - это Explorer.LinkContainer
            for(Link link : links)
                link.parent = Windows98.windows98;

            tmpSelectedItem = selectedItem;
            return links.toArray(new Link[0]);
        }

        @Override
        protected void onSelectionChanged(int oldSelection, int newSelection){
            Link link = (Link) getSelectedItem();
            if (link.path.equals(MyDocuments.getExternalStorageDirectory())) {
                context.checkWriteExternalPermission(new MainActivity.PermissionResultListener() {
                    @Override
                    public void onPermissionGranted() {
                        linkContainer.changeDirectory(link.path);
                    }

                    @Override
                    public void onPermissionDenied() {
                        MyDocuments.diskNotAccessible((Window) linkContainer.parent);
                        selectedItem = oldSelection;
                    }
                });
            }
            else
                linkContainer.changeDirectory(link.path);
        }
    }

    public static File getDir(File openedFile){  // папка, в которой находится файл
        if(openedFile != null && openedFile.exists())
            return openedFile.getParentFile();
        else
            return MyDocuments.getFilesDir();
    }

    public static abstract class OnResultListener{  // Listener, который вызывается при нажатии на кнопку Open/Save
        void writeToFile(File file) {}  // file - какой файл мы в итоге выбрали
        void openFile(File file) {}
        void onCancel() {}
    }

    public interface ActionOnSave {  // Notepad или Paint при закрытии/new/open будут спрашивать, хотите ли вы сохранить файл
        void setActionOnSave(Runnable actionOnSave);  // actionOnSave - принудительное закрытие окна (без вопросов), New или Open.
        void runActionOnSave();
    }
}
