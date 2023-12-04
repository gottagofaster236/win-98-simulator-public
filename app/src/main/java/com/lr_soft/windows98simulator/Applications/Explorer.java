package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.FileObserver;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.AboutWindow;
import com.lr_soft.windows98simulator.System.BigTopButtons;
import com.lr_soft.windows98simulator.System.ButtonInList;
import com.lr_soft.windows98simulator.System.ButtonList;
import com.lr_soft.windows98simulator.System.DummyWindow;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.ScrollBar;
import com.lr_soft.windows98simulator.System.ScrollElementContainer;
import com.lr_soft.windows98simulator.System.Scrollable;
import com.lr_soft.windows98simulator.System.Separator;
import com.lr_soft.windows98simulator.System.TopMenu;
import com.lr_soft.windows98simulator.System.TopMenuButton;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;
import com.lr_soft.windows98simulator.WindowsView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Explorer extends DummyWindow {
    String folderName;
    String fullPath;  // путь, в котором лежит папка. Не путь до самой папки.
    int parentFolder = -1;  // My Computer
    public LinkContainer linkContainer;
    private FileDialog.FolderDropdownList folderDropdownList;

    String curFilename = "";  // Имя выбранного файла
    String defaultHelpText = "Select an item to view its description.";
    String helpText = defaultHelpText;
    // Информация про выбранный файл. Рисуются на свой canvas с переносами строк, его размеры меняются если свернуть/развернуть окно
    String[] folderNameLines = null, curFilenameLines = null, helpTextLines = null;  // эти строки могут быть слишком длинные, поэтому мы разбиваем их на строки
    private String minimizedFullPath, maximizedFullPath;  // так как путь может не помещаться в окно
    private Bitmap bigIcon;  // рисуется слева, радом с текстом
    private Bitmap rainbowSmall = getBmp(R.drawable.explorer_rainbow_1),
            rainbowBig = getBmp(R.drawable.explorer_rainbow_2);
    private Bitmap textBmp = createBitmap(175, 314, Bitmap.Config.ARGB_8888);
    private Canvas textCanvas = new Canvas(textBmp);  // сюда рисуется текст такой, как имя папки и тип файла
    private Rect src = new Rect(0, 0, 105, 207), dst = new Rect();
    private Bitmap folderBmp;  // у обычного Explorer'а картинка слева от пути к папке отличается от иконки
    boolean isFolder = true;
    boolean isMsDos = false;  // это Explorer для того, чтобы использовать его внутри MS-DOS. Не грузим никакие картинки, нельзя подниматься выше, чем диск C:\
    public static File clipboard = null;  // буфер обмена

    public Explorer(String windowTitle, String fullPath, int iconSmall, int iconBig, boolean isFolder){
        this(windowTitle, fullPath, iconSmall, iconBig, isFolder, null);
    }

    public Explorer(String windowTitle, String fullPath, int iconSmall, int iconBig, boolean isFolder, File directory){
        // is Folder - это значит, что в MyDocuments можно создавать файлы, а в Printers - нельзя (нет меню New)
        super(windowTitle, getBmp(iconSmall), true, getBmp(R.drawable.explorer_1), getBmp(Windows98.WIDESCREEN? R.drawable.explorer_2w : R.drawable.explorer_2));
        this.folderName = windowTitle;
        this.fullPath = fullPath;
        bigIcon = getBmp(iconBig);
        if(iconSmall == R.drawable.directory_open_2)
            folderBmp = getBmp(R.drawable.directory_closed_2);
        initLinkContainer(new ArrayList<Link>());
        setupButtons();
        initFolderDropdown(directory);
        createTopMenu(isFolder);
        repositionEverything(true);
    }

    public Explorer(int folder){
        this(folder, false);
    }

    public Explorer(int folder, boolean isMsDos) {
        this(folder, R.drawable.directory_open_2, R.drawable.folder, isMsDos);
    }

    public Explorer(int folder, int iconSmall, int iconBig, boolean isMsDos){
        super(getFolderName(folder), getBmp(iconSmall), true, getBmp(R.drawable.explorer_1), getBmp(Windows98.WIDESCREEN? R.drawable.explorer_2w : R.drawable.explorer_2));
        //Log.d(TAG, "folder: " + folder + ", folderName: " + folderName);
        this.folderName = getTitle();
        this.isMsDos = isMsDos;
        if(folderName.equals("(C:)")){  // отдельный случай
            this.fullPath = "C:\\";
        }
        else {
            this.fullPath = filesystem[folder][0];
        }
        List<Link> links = new ArrayList<>();
        for(int file = 2; file < filesystem[folder].length; file++){
            Link link = new Link(folder, file, fullPath, this);
            links.add(link);
        }
        initLinkContainer(links);

        parentFolder = Integer.parseInt(filesystem[folder][1]);
        if(isMsDos) {
            close();  // чтобы не отрисовываться
            return;
        }
        bigIcon = getBmp(iconBig);
        if(iconSmall == R.drawable.directory_open_2)
            folderBmp = getBmp(R.drawable.directory_closed_2);
        setupButtons();
        initFolderDropdown(null);
        createTopMenu(true);
        repositionEverything(true);
    }

    private void initLinkContainer(List<Link> links){
        linkContainer = new LinkContainer(links, new Rect(127, 120, 394, 343),
                new Rect(197, 120, Windows98.SCREEN_WIDTH - 14, Windows98.SCREEN_HEIGHT - 46));
        // не используем RelativeBounds, так как здесь он неверен
        addElement(linkContainer);
        linkContainer.parent = this;  // для MS-DOS
        bringClickedElementToFront = true;  // так как у LinkContainer'а есть contextMenu, который будет перекрывать topMenu окна
        ScrollBar scrollBar = new ScrollBar(linkContainer, new Rect(394, 120, 410, 343),
                new Rect(Windows98.SCREEN_WIDTH - 14, 120, Windows98.SCREEN_WIDTH + 2, Windows98.SCREEN_HEIGHT - 46), true);
        linkContainer.scrollBar = scrollBar;
        elements.add(0, scrollBar);
        linkContainer.updateLinkPositions();
    }

    private void setupButtons(){
        final int[] topButtonsIds = {R.drawable.back, R.drawable.up, R.drawable.cut, R.drawable.copy,
                R.drawable.paste, R.drawable.undo, R.drawable.delete, R.drawable.properties, R.drawable.views};
        Bitmap[] topButtons = new Bitmap[9];
        for(int i=0; i<9; i++)
            topButtons[i] = getBmp(topButtonsIds[i]);
        BigTopButtons bigTopButtons = new BigTopButtons(topButtons,
                new int[][]{{15, 81}, {149, 202}, {209, 262}, {263, 316}, {317, 370}, {377, 430}, {437, 490}, {491, 544}, {551, 617}}, 5, new BigTopButtons.OnButtonPressListener() {
            @Override
            public void onButtonPress(int buttonNumber) {
                Link activeLink = null;
                if(linkContainer.topElement != null && linkContainer.topElement instanceof Link)  // && ((Link) linkContainer.topElement).selected){  // есть активный ярлык
                    activeLink = (Link) linkContainer.topElement;
                if(buttonNumber == 0 || buttonNumber == 1)  // Back / Up
                    upOneLevel();
                else if(buttonNumber == 6){  // Delete
                    if(activeLink != null)
                        activeLink.delete();
                }
                else if(buttonNumber == 3) {  // Copy
                    if(activeLink != null)
                        activeLink.copy();
                }
                else if(buttonNumber == 4){  // Paste
                    linkContainer.paste();
                }
            }
        });
        bigTopButtons.y = 49;
        addElement(bigTopButtons);
    }

    private void initFolderDropdown(File directory){
        minimizedFullPath = shortenTextToThreeDots(fullPath, 307, p);
        maximizedFullPath = shortenTextToThreeDots(fullPath, Windows98.WIDESCREEN? 753 : 585, p);
        folderDropdownList = new FileDialog.FolderDropdownList(linkContainer, 0, directory,
                folderBmp != null? folderBmp : icon, minimizedFullPath, true, false);
        addElement(folderDropdownList, 57, 91);
    }

    static private String getFolderName(int folder){  // для того, чтобы запихнуть в вызов super()
        String[] splitArr = filesystem[folder][0].split("\\\\");
        return splitArr[splitArr.length - 1];
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        canvas.drawBitmap(folderBmp != null? folderBmp : icon, x + 60, y + 94, null);  // см. folderBmp
        p.setColor(Color.BLACK);
        canvas.drawText(maximized? maximizedFullPath : minimizedFullPath, x + 81, y + 107, p);

        textCanvas.drawColor(Color.WHITE);
        textCanvas.drawBitmap(bigIcon, 15, 15, null);
        p.setColor(Color.BLACK);
        p_bold.setColor(Color.BLACK);
        p_very_bold.setColor(Color.BLACK);
        if(folderNameLines == null)
            folderNameLines = splitTextIntoLines(folderName, maximized? 159 : 89, p_very_bold);
        int cur_y = drawMultilineText(textCanvas, folderNameLines, 16, 73, 25, p_very_bold);
        //textCanvas.drawText(folderName, 16, 73, p_very_bold);  116
        textCanvas.drawBitmap(maximized? rainbowBig : rainbowSmall, 0, cur_y + 9, null);
        cur_y += 43;
        if(!curFilename.isEmpty()) {
            if(curFilenameLines == null)
                curFilenameLines = splitTextIntoLines(curFilename, maximized? 159 : 89, p_bold);
            cur_y = drawMultilineText(textCanvas, curFilenameLines, 16, cur_y, 13, p_bold);
            //textCanvas.drawText(curFilename, 16, cur_y, p_bold);
            cur_y += 26;
        }
        if(helpTextLines == null)
            helpTextLines = splitTextIntoLines(helpText, maximized ? 160 : 90, p);
        drawMultilineText(textCanvas, helpTextLines, 15, cur_y, 13, p);
        //textCanvas.drawText(helpText, 15, 142, p);
        if(maximized){
            canvas.drawBitmap(textBmp, x + 6, y + 120, null);
        }
        else{
            dst.set(src);
            dst.offset(x + 6, y + 120);
            canvas.drawBitmap(textBmp, src, dst, null);
        }
    }

    @Override
    public void repositionElements() {
        // Переделываем строки
        folderNameLines = splitTextIntoLines(folderName, maximized? 159 : 89, p_very_bold);
        curFilenameLines = splitTextIntoLines(curFilename, maximized? 159 : 89, p_bold);
        helpTextLines = splitTextIntoLines(helpText, maximized? 160 : 90, p);

        if(maximized)
            folderDropdownList.width = Windows98.WIDESCREEN? 795 : 581;
        else
            folderDropdownList.width = 349;
    }

    @Override
    public boolean onSelfMouseOver(int x, int y, boolean touch) {  // нажали на себя - значит, не на файл
        if(touch)
            resetHelpText();
        return super.onSelfMouseOver(x, y, touch);
    }

    private void resetHelpText(){
        curFilename = "";
        helpText = defaultHelpText;
        helpTextLines = null;
    }

    protected void upOneLevel(){  // подняться на один уровень вверх в файловой системе
        if(getTitle().equals("My Computer"))
            return;
        Window openingFolder;
        if(parentFolder == -1)
            openingFolder = new MyComputer();
        else if(parentFolder == 0)
            openingFolder = new DriveC();
        else
            openingFolder = new Explorer(parentFolder, isMsDos);
        openingFolder.alignWith(this);
    }

    public static class LinkContainer extends ScrollElementContainer implements Scrollable {
        private File directory;
        private FixedFileObserver fileObserver;  // иначе будет garbage collection
        private FileProvider fileProvider;
        private Window parentWindow;

        public LinkContainer(List<Link> links, RelativeBounds bounds){
            this(links, bounds.getMinimizedRect(), bounds.getMaximizedRect());
        }

        public LinkContainer(List<Link> links, Rect minimizedBounds, Rect maximizedBounds){
            this(links, minimizedBounds, maximizedBounds, null);
        }

        public LinkContainer(List<Link> links, Rect minimizedBounds, Rect maximizedBounds, Bitmap drawingBitmap){
            super(minimizedBounds, maximizedBounds, 37, drawingBitmap);
            // noinspection unchecked
            elements = (List) links;
            for(Element link : elements)
                link.parent = this;  // так как в случае MS-DOS не вызываются ни onDraw, ни onMouseOver
            bringClickedElementToFront = true;
            contextMenu = Windows98.win98contextMenu(this);  // показывается с помощью topElement
        }

        @Override
        public void onDraw(Canvas canvas, int x, int y) {
            if(parent == Windows98.windows98){
                super.onDraw(canvas, x, y);
                return;
            }
            boolean fixContextMenu = topElement != null && topElement.contextMenu != null;  // так как иначе контекстное меню обрежется (из-за drawingBitmap)
            boolean oldActive = false, oldLastActive = false;
            if(fixContextMenu) {
                oldActive = topElement.contextMenu.active;
                oldLastActive = topElement.contextMenu.lastActive;
                topElement.contextMenu.active = false;  // чтобы не рисовал самостоятельно. Из-за смещения scrollY контекстное меню может неправильно расположиться на экране.
            }
            Element oldTopElement = topElement;
            if(topElement == contextMenu)  // чтобы context menu не смещался
                topElement = null;
            super.onDraw(canvas, x, y);
            if(oldTopElement == contextMenu){
                topElement = oldTopElement;
                topElement.onDraw(canvas, x + topElement.x, y + topElement.y);
            }

            if(fixContextMenu){
                topElement.contextMenu.active = oldActive;
                topElement.contextMenu.lastActive = oldLastActive;
                topElement.contextMenu.onDraw(canvas, x + topElement.x + topElement.contextMenu.x, y + topElement.y + topElement.contextMenu.y - scrollY);
            }
        }

        @Override
        public boolean onMouseOver(int x, int y, boolean touch) {
             // высовывающееся за границы LinkContainer'а
             //  контекстное меню не получает onMouseOver из-за проверки принадлежности к прямоугольнику (которая нужна из-за scroll)
            boolean insideRect = 0 <= x && x < width && 0 <= y && y < height;
            boolean linkContextMenu = topElement != null && topElement.contextMenu != null && topElement.contextMenu.active
                    && topElement.contextMenu.onMouseOver(x - topElement.x - topElement.contextMenu.x, y + scrollY - topElement.y - topElement.contextMenu.y, false);
            boolean ourContextMenu = topElement == contextMenu && contextMenu.onMouseOver(x - contextMenu.x, y - contextMenu.y, false);
            if(!insideRect && !linkContextMenu && !ourContextMenu)
                return false;
            // если (1) курсор в прямоугольнике (2) курсор над контекстным меню ярлыка (3) курсор над нашим контекстным меню продолжаем
            if(touch && topElement == contextMenu && !ourContextMenu){
                contextMenu.onOtherTouch();
                topElement = null;
            }
            contextMenu.y += scrollY;
            super.onMouseOver(x, y, touch);
            contextMenu.y -= scrollY;
            return true;  // возвращаем true если в квадрате
        }

        @Override
        public void onClick(int x, int y) {
            contextMenu.y += scrollY;
            super.onClick(x, y);
            contextMenu.y -= scrollY;
        }

        @Override
        public void onOtherTouch() {
            // если кликнули не в нас, но в эксплорер, то активный ярлык не должен пропасть
            if(parent instanceof Windows98){
                super.onOtherTouch();
                return;
            }
            Window parentWindow = (Window) parent;
            contextMenu.active = false;
            if(parentWindow.active && parentWindow.onSelfMouseOver(getCursorX() - parentWindow.x, getCursorY() - parentWindow.y, false)){
                boolean ignoreOtherTouch = true;
                for(Element element : elements){
                    Link lnk = (Link) element;
                    if(lnk.isRenaming() || lnk.contextMenu.active){
                        ignoreOtherTouch = false;
                        break;
                    }
                }
                if(ignoreOtherTouch)
                    return;
            }
            super.onOtherTouch();
        }

        @Override
        public boolean onSelfMouseOver(int x, int y, boolean touch) {
            if(touch && parent instanceof Explorer)
                ((Explorer) parent).resetHelpText();
            return super.onSelfMouseOver(x, y, touch);
        }

        @Override
        public void onSelfRightClick(int x, int y) {
            contextMenu.active = true;
            contextMenu.x = x;
            contextMenu.y = y - scrollY;
            contextMenu.reset();
            if(!(this instanceof Windows98.DesktopLinks)) {  // контекстное меню не в таксбаре (как в DesktopLinks)
                topElement = contextMenu;
            }
        }

        @Override
        public void onWindowResize(boolean maximized) {  // вызывать либо с параметром true (false ломает FileDialog), либо с настоящим параметром (maximized)
            if(maximizedBounds != null){  // так как есть этот if
                if(parent != null)
                    maximized = ((Window) parent).maximized;
                setBounds(maximized? maximizedBounds : minimizedBounds);
            }
            int linksInRow;
            if(parentWindow instanceof Explorer || (parent != null && parent instanceof Explorer)) {
                if (!maximized)
                    linksInRow = 3;
                else
                    linksInRow = Windows98.WIDESCREEN ? 8 : 5;
            }
            else
                linksInRow = 5;
            int start_x = 21, start_y = 2;
            int cur_x = 0, cur_y = 0;  // номер текущего стобца и строки
            scrollRange = 0;

            for (int i = 0; i < elements.size(); i++) {
                if(!(elements.get(i) instanceof Link))
                    continue;
                Link lnk = (Link) elements.get(i);
                lnk.x = start_x + 75 * cur_x;
                if(cur_x == linksInRow){
                    lnk.x = start_x;
                    cur_x = 0;
                    cur_y++;
                }
                lnk.y = start_y + 75 * cur_y;
                //if(lnk.bounds.left == -1000)
                //    lnk.onDraw(new Canvas(), 0, 0);
                scrollRange = Math.max(scrollRange, lnk.y + lnk.fullBounds.bottom + 2);
                cur_x++;
            }
            updateScrollBar();
        }

        public void updateLinkPositions(){  // вызывать после добавления/удаления ярлыков
            onWindowResize(true);
        }

        public void createFile(String defaultName, String extension){
            // расширение должно включать в себя точку!
            // если это папка, надо передать пустое расширение ("")
            int number = 1;  // имя_файла (номер повтора).расширение
            String resultingName;
            File newFile;
            while(true){
                if(number == 1)
                    resultingName = defaultName + extension;
                else
                    resultingName = defaultName + " (" + number + ")" + extension;
                newFile = new File(directory, resultingName);
                if(newFile.exists()) {
                    number++;
                    continue;
                }
                break;
            }

            boolean success;
            if(extension.isEmpty())
                success = newFile.mkdir();
            else {
                try {
                    success = newFile.createNewFile();
                }
                catch (IOException e){
                    success = false;
                }
            }
            if(!success) {
                new MessageBox("Error Creating File", "Cannot create file: File system error.", MessageBox.OK, MessageBox.ERROR, null, parentWindow);
                return;
            }
            Link newLink = new Link(newFile, parentWindow);
            addLink(newLink);
            newLink.rename();
        }

        public void paste(){  // вставить файл из буфера обмена
            if(directory == null)
                return;
            if(clipboard == null)
                return;
            if(!clipboard.exists()){
                new MessageBox("Error Copying File", "Cannot copy file: File system error (1026).",
                        MessageBox.OK, MessageBox.ERROR, null, parentWindow);
                return;
            }
            if(clipboard.isDirectory()){
                // нельзя скопировать папку в саму себя
                String src, dst;
                try {
                    src = clipboard.getCanonicalPath();
                    dst = directory.getCanonicalPath();
                }
                catch (IOException e){
                    copyFileError();
                    return;
                }
                if(src.equals(dst)){
                    new MessageBox("Error Copying File", "Cannot copy " + clipboard.getName() + ": The destination folder is the same as the source folder.",
                            MessageBox.OK, MessageBox.ERROR, null, parentWindow);
                    return;
                }
                if(dst.startsWith(src + File.separator)){
                    new MessageBox("Error Copying File", "Cannot copy " + clipboard.getName() + ": The destination folder is a subfolder of the source folder.",
                            MessageBox.OK, MessageBox.ERROR, null, parentWindow);
                    return;
                }
            }
            File newFile = new File(directory, clipboard.getName());
            if(newFile.exists()){  // уже есть файл с таким именем
                String oldName = clipboard.getName();
                int number = 1;
                while(true){
                    String resultingName;
                    if(number == 1)
                        resultingName = "Copy of " + oldName;
                    else
                        resultingName = "Copy (" + number + ") of " + oldName;
                    newFile = new File(directory, resultingName);
                    if(newFile.exists()) {
                        number++;
                        continue;
                    }
                    break;
                }
            }

            try{
                copy(clipboard, newFile);
            }
            catch (IOException e){
                copyFileError();
                return;
            }

            Link newLink = new Link(newFile, parentWindow);
            addLink(newLink);
        }

        private void copyFileError(){
            new MessageBox("Error Copying File", "Cannot copy file: File system error.",
                    MessageBox.OK, MessageBox.ERROR, null, parentWindow);
        }

        // addLink из LinkContainer
        public void addLink(Link newLink){
            // добавляем ярлык, делаем его активным
            elements.add(newLink);
            newLink.parent = this;
            updateLinkPositions();
            newLink.makeActive();
        }

        public File getDirectory() {
            return directory;
        }

        public void initFromDirectory(final File directory, FileProvider fileProvider, final Window parentWindow) {
            this.directory = directory;
            this.fileProvider = fileProvider;
            this.parentWindow = parentWindow;
            updateFiles();
            //  сортируем по имени
            Collections.sort(elements, (o1, o2) -> {
                Link link1 = (Link) o1, link2 = (Link) o2;
                if(link1.isFolder == link2.isFolder)
                    return link1.fullFilename.compareTo(link2.fullFilename);
                else{
                    if(link1.isFolder)
                        return -1;
                    else
                        return 1;
                }
            });

            // на рабочем столе есть симлинк в My Documents
            if(parentWindow != null && directory.equals(MyDocuments.getDesktopDirectory())){
                Link myDocs;
                if(parentWindow instanceof FileDialog)
                    myDocs = new Link("My Documents", "My Documents", getBmp(R.drawable.directory_open_file_mydocs_0),
                            parent -> new FileDialog(MyDocuments.getFilesDir(), (FileDialog) parentWindow));
                else  // MyDocuments
                    myDocs = new Link("My Documents", "My Documents", getBmp(R.drawable.directory_open_file_mydocs_0), MyDocuments.class, parentWindow);
                myDocs.fullFilename = ".My Documents";
                elements.add(0, myDocs);
            }
            updateLinkPositions();

            fileObserver = new FixedFileObserver(directory) {
                @Override
                public void onEvent(final int event, final String path) {
                    WindowsView.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(event == FileObserver.DELETE_SELF || event == FileObserver.MOVE_SELF){
                                if(parentWindow != null)
                                    parentWindow.close();
                            }
                            else {
                                if(parentWindow != null && parentWindow.closed)
                                    return;
                                if(!directory.exists())
                                    return;
                                updateFiles();
                                if (parentWindow != null)
                                    parentWindow.updateWindow();
                            }
                        }
                    });
                }
            };
            fileObserver.startWatching();
        }

        /*private Link findLinkByFilename(String filename){  // case-sensitive
            for(Element element : elements){
                Link link = (Link) element;
                if(link.fullFilename.equals(filename))
                    return link;
            }
            return null;
        }*/

        public interface FileProvider {
            boolean acceptFile(File file);
        }

        protected void updateFiles(){  // положить в linkContainer файлы из directory
            // проверяем, изменились ли файлы
            String[] directoryList = directory.list();
            if(directoryList == null){  // crash из play console
                directoryList = new String[0];
            }
            Set<String> newFiles = new HashSet<>(Arrays.asList(directoryList));
            Set<String> oldFiles = new HashSet<>();
            for (Element element : elements)
                oldFiles.add(((Link) element).fullFilename);
            if(parentWindow != null && directory.equals(MyDocuments.getDesktopDirectory()))
                newFiles.add(".My Documents");
            if(newFiles.equals(oldFiles))
                return;
            // удаляем файлы, которых больше нет
            for(int i = elements.size() - 1; i >= 0; i--){
                Link link = (Link) elements.get(i);
                if(!newFiles.contains(link.fullFilename))
                    link.removeFromParent();
            }
            // добавляем новые
            for(String filename : newFiles){
                if(!oldFiles.contains(filename)) {
                    File file = new File(directory, filename);
                    boolean ok = !file.isHidden() && !filename.startsWith(".")&&
                            !(directory.equals(MyDocuments.getFilesDir()) && filename.equals("Desktop"));
                    if (ok && (fileProvider == null || fileProvider.acceptFile(file))) {
                        Link newLink = new Link(file, parentWindow);
                        newLink.parent = this;
                        elements.add(newLink);
                    }
                }
            }

            updateLinkPositions();
        }

        public void changeDirectory(File newDirectory){
            if(parent instanceof FileDialog){
                new FileDialog(newDirectory, (FileDialog) parent);
            }
            else {
                MyDocuments myDocuments = new MyDocuments(newDirectory);
                if(parent instanceof Window)
                    myDocuments.alignWith((Window) parent);
            }
        }

        @Override
        public void prepareForDelete() {
            super.prepareForDelete();
            if(fileObserver != null)
                fileObserver.stopWatching();
        }
    }

    // addLink из Explorer: после этого надо вызвать updateLinkPositions
    protected void addLink(Link link){
        linkContainer.elements.add(link);
    }

    // (c) https://stackoverflow.com/a/5368745/6120487
    public static void copy(File sourceLocation, File targetLocation) throws IOException {
        if(sourceLocation.isDirectory())
            copyDirectory(sourceLocation, targetLocation);
        else
            copyFile(sourceLocation, targetLocation);
    }

    private static void copyDirectory(File source, File target) throws IOException {
        if(!target.exists())
            target.mkdir();
        for(String f : source.list())
            copy(new File(source, f), new File(target, f));
    }
    //

    /*public void changeDirectory(String directory){
        for(Element element : linkContainer.elements){
            if(((Link) element).fullFilename.equals(directory)){
                ((Link) element).action.run(element);
                break;
            }
        }
    }*/

    private static abstract class FixedFileObserver {
        private final static Map<File, FileObserver> fileObservers = new HashMap<>();
        private final static Map<File, Set<FixedFileObserver>> fixedObservers = new HashMap<>();
        private static final int mask = FileObserver.MOVED_FROM | FileObserver.MOVED_TO |
                FileObserver.CREATE | FileObserver.DELETE | FileObserver.DELETE_SELF | FileObserver.MOVE_SELF;

        private File file;

        public FixedFileObserver(File file) {
            this.file = file;
        }

        public abstract void onEvent(int event, String path);

        public void startWatching() {
            synchronized (fixedObservers) {
                if(!fixedObservers.containsKey(file)){  // никто не смотрит за этим файлом
                    FileObserver fileObserver = new FileObserver(file.getAbsolutePath(), mask) {
                        @Override
                        public void onEvent(int event, String path) {
                            event &= mask;
                            if(event == 0)
                                return;
                            Set<FixedFileObserver> observers = fixedObservers.get(file);
                            if(observers != null) {
                                for (FixedFileObserver observer : observers)
                                    observer.onEvent(event, path);
                            }
                        }
                    };
                    fileObserver.startWatching();
                    fileObservers.put(file, fileObserver);
                    fixedObservers.put(file, new HashSet<FixedFileObserver>());
                }

                fixedObservers.get(file).add(this);
            }
        }

        public void stopWatching() {
            synchronized (fixedObservers) {
                fixedObservers.get(file).remove(this);
                if(fixedObservers.get(file).isEmpty()){  // мы были последними, кто смотрел за файлом
                    fixedObservers.remove(file);
                    fileObservers.get(file).stopWatching();
                    fileObservers.remove(file);
                }
            }
        }
    }

    private void createTopMenu(boolean isFolder){
        this.isFolder = isFolder;
        ButtonList file = new ButtonList();
        if(isFolder){
            file.elements.add(new ButtonInList("New", Windows98.newMenu(linkContainer)));
            file.elements.add(new Separator());
        }
        file.elements.add(new ButtonInList("Create Shortcut"));
        file.elements.add(new ButtonInList("Delete"));
        file.elements.add(new ButtonInList("Rename"));
        file.elements.add(new ButtonInList("Properties"));
        file.elements.add(new Separator());
        file.elements.add(new ButtonInList("Work Offline"));
        ButtonInList close = new ButtonInList("Close");
        close.action = parent -> Explorer.this.close();
        file.elements.add(close);
        ButtonList edit = new ButtonList();
        edit.elements.add(new ButtonInList("Undo", "Ctrl+Z"));
        edit.elements.add(new Separator());
        edit.elements.add(new ButtonInList("Cut", "Ctrl+X"));
        edit.elements.add(new ButtonInList("Copy", "Ctrl+C"));
        edit.elements.add(new ButtonInList("Paste", "Ctrl+V"));
        edit.elements.add(new ButtonInList("Paste Shortcut"));
        edit.elements.add(new Separator());
        for(Element el : edit.elements){
            if(el instanceof Separator)
                continue;
            ((ButtonInList) el).disabled = true;
        }
        edit.elements.add(new ButtonInList("Select All", "Ctrl+A"));
        edit.elements.add(new ButtonInList("Invert Selection"));
        ButtonList view = new ButtonList();
        ButtonList toolbars = new ButtonList();
        ButtonInList standartButtons = new ButtonInList("Standart Buttons");
        standartButtons.check = true;
        standartButtons.checkActive = true;
        toolbars.elements.add(standartButtons);
        ButtonInList addressBar = new ButtonInList("Address Bar");
        addressBar.check = true;
        addressBar.checkActive = true;
        toolbars.elements.add(addressBar);
        ButtonInList links = new ButtonInList("Links");
        links.check = true;
        links.checkActive = false;
        toolbars.elements.add(links);
        ButtonInList radio = new ButtonInList("Radio");
        radio.check = true;
        radio.checkActive = false;
        toolbars.elements.add(radio);
        toolbars.elements.add(new Separator());
        ButtonInList textLabels = new ButtonInList("Text Labels");
        textLabels.check = true;
        textLabels.checkActive = true;
        toolbars.elements.add(textLabels);
        view.elements.add(new ButtonInList("Toolbars", toolbars));
        ButtonInList statusBar = new ButtonInList("Status Bar");
        statusBar.check = true;
        statusBar.checkActive = true;
        view.elements.add(statusBar);
        ButtonList explorerBar = new ButtonList();
        explorerBar.elements.add(new ButtonInList("Search", "Ctrl+E"));
        explorerBar.elements.add(new ButtonInList("Favorites", "Ctrl+I"));
        explorerBar.elements.add(new ButtonInList("History", "Ctrl+H"));
        explorerBar.elements.add(new ButtonInList("Folders"));
        explorerBar.elements.add(new Separator());
        explorerBar.elements.add(new ButtonInList("Tip of the Day"));
        view.elements.add(new ButtonInList("Explorer Bar", explorerBar));
        view.elements.add(new Separator());
        ButtonInList asWebPage = new ButtonInList("as Web Page");
        asWebPage.check = true;
        asWebPage.checkActive = true;
        view.elements.add(asWebPage);
        view.elements.add(new Separator());
        List<ButtonInList> buttonGroup = new ArrayList<>();
        buttonGroup.add(new ButtonInList("Large Icons"));
        buttonGroup.add(new ButtonInList("Small Icons"));
        buttonGroup.add(new ButtonInList("List"));
        buttonGroup.add(new ButtonInList("Details"));
        buttonGroup.get(0).checkActive = true;
        for(ButtonInList button : buttonGroup){
            button.radioButtonGroup = buttonGroup;
            view.elements.add(button);
        }
        view.elements.add(new Separator());
        view.elements.add(new ButtonInList("Customize this Folder"));
        view.elements.add(new Separator());
        ButtonList arrangeIcons = new ButtonList();
        arrangeIcons.elements.add(new ButtonInList("by Name"));
        arrangeIcons.elements.add(new ButtonInList("by Type"));
        arrangeIcons.elements.add(new ButtonInList("by Size"));
        arrangeIcons.elements.add(new ButtonInList("by Date"));
        arrangeIcons.elements.add(new Separator());
        arrangeIcons.elements.add(new ButtonInList("Auto Arrange"));
        view.elements.add(new ButtonInList("Arrange Icons", arrangeIcons));
        view.elements.add(new ButtonInList("Line Up Icons"));
        view.elements.add(new Separator());
        view.elements.add(new ButtonInList("Refresh"));
        view.elements.add(new ButtonInList("Folder Options"));
        ButtonList go = new ButtonList();
        go.elements.add(new ButtonInList("Back", "Alt+Left Arrow"));
        go.elements.add(new ButtonInList("Forward", "Alt+Right Arrow"));
        for(Element el : go.elements)
            ((ButtonInList) el).disabled = true;
        go.elements.add(new ButtonInList("Up One Level", parent -> upOneLevel()));
        go.elements.add(new Separator());
        go.elements.add(new ButtonInList("Home Page", "Alt+Home"));
        go.elements.add(new ButtonInList("Channel Guide"));
        go.elements.add(new ButtonInList("Search the Web"));
        go.elements.add(new Separator());
        go.elements.add(new ButtonInList("Mail"));
        go.elements.add(new ButtonInList("News"));
        go.elements.add(new ButtonInList("My Computer"));
        go.elements.add(new ButtonInList("Address Book"));
        go.elements.add(new ButtonInList("Internet Call"));
        ButtonList favorites = StartMenu.getFavoritesMenu();
        favorites.elements.add(0, new ButtonInList("Add to Favorites..."));
        favorites.elements.add(1, new ButtonInList("Organize Favorites..."));
        favorites.elements.add(2, new Separator());
        ButtonList help = new ButtonList();
        ButtonInList helpTopics = new ButtonInList("Help Topics");
        helpTopics.action = parent -> StartMenu.createWindowsHelp();
        help.elements.add(helpTopics);
        help.elements.add(new Separator());
        ButtonInList about = new ButtonInList("About Windows 98");
        about.action = parent -> new AboutWindow(Explorer.this, "Windows", getBmp(R.drawable.mswindows));
        help.elements.add(about);
        TopMenu topMenu = new TopMenu();
        topMenu.buttonsSize = 1;
        topMenu.elements.add(new TopMenuButton("File", file));
        topMenu.elements.add(new TopMenuButton("Edit", edit));
        topMenu.elements.add(new TopMenuButton("View", view));
        topMenu.elements.add(new TopMenuButton("Go", go));
        topMenu.elements.add(new TopMenuButton("Favorites", favorites));
        topMenu.elements.add(new TopMenuButton("Help", help));
        topMenu.x = 15;
        topMenu.y = 26;
        repositionTopMenu = false;
        setTopMenu(topMenu);
    }

    // сгенерированное скриптом на питоне представление файловой системы windows 98

    // первый элемент массива - путь к папке, включая саму папку; второй - номер каталога на уровень выше
    // синтаксис для папки: <имя папки>/<индекс папки в массиве>
    public static final int WINDOWS_INDEX = 60;  // C:\Windows
    public static final int FONTS_INDEX = 88;  // C:\Windows\Fonts
    public static final int DESKTOP_INDEX = 80;  // C:\Windows
    public static final String[][] filesystem = {{"(C:)", "-1", "Program Files/1", "Windows/60", "Autoexec.bat", "Command.com", "Frunlog.txt", "Netlog.txt", "Scandisk.log"}, {"C:\\Program Files", "0", "Accessories/2", "Chat/4", "Common Files/5", "DirectX/23", "Internet Explorer/25", "NetMeeting/29", "Online Services/30", "Outlook Express/37", "Plus!/38", "Uninstall Information/40", "Windows Media Player/59", "Desktop.ini", "Folder.htt"}, {"C:\\Program Files\\Accessories", "1", "HyperTerminal/3", "Cis.scp", "Mspaint.exe", "Mspcx32.dll", "Pcximp32.flt", "Pppmenu.scp", "Slip.scp", "Slipmenu.scp", "Wordpad.exe"}, {"C:\\Program Files\\Accessories\\HyperTerminal", "2"}, {"C:\\Program Files\\Chat", "1"}, {"C:\\Program Files\\Common Files", "1", "Microsoft Shared/6", "Services/18", "System/19"}, {"C:\\Program Files\\Common Files\\Microsoft Shared", "5", "Msinfo/7", "Proof/8", "Stationery/9", "Textconv/10", "Triedit/11", "Vgx/12", "Web Folders/13", "Web Server Extensions/14"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Msinfo", "6", "Msiav.ocx", "Msicdrom.ocx", "Msinfo32.exe", "Msiolerg.ocx", "Msiprint.ocx", "Msisys.ocx", "Msupdate.ocx", "Txtview.ocx"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Proof", "6", "Csapi3t1.dll"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Stationery", "6", "Baby News Bkgrd.gif", "Balloon Party Invitation Bkgrd.jpg", "Chess.gif", "Chess.htm", "Chicken Soup Bkgrd.gif", "Chicken Soup.htm", "Christmas Trees.gif", "For Sale Bkgrd.gif", "For Sale.htm", "Formal Announcement Bkgrd.gif", "Formal Announcement.htm", "Fun Bus.htm", "FunBus.gif", "Holiday Letter Bkgrd.gif", "Holiday Letter.htm", "Ivy.gif", "Ivy.htm", "Mabel.htm", "MabelB.gif", "MabelT.gif"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Textconv", "6", "Html32.cnv", "Msconv97.dll", "Mswrd632.wpc", "Mswrd832.cnv", "Write32.wpc"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Triedit", "6", "Dhtmled.ocx", "Triedit.dll"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Vgx", "6", "Vgx.dll"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Web Folders", "6", "Msonsext.dll", "Ragent.dll"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Web Server Extensions", "6", "40/15"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Web Server Extensions\\40", "14", "Bin/16"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Web Server Extensions\\40\\Bin", "15", "1033/17", "Fp4anwi.dll", "Fp4autl.dll", "Fp4awec.dll"}, {"C:\\Program Files\\Common Files\\Microsoft Shared\\Web Server Extensions\\40\\Bin\\1033", "16", "Fpext.msg"}, {"C:\\Program Files\\Common Files\\Services", "5", "Bigfoot.icon", "Infospbz.icon", "Infospce.icon", "Swtchbrd.icon", "Verisign.icon", "Whowhere.icon", "Yahoo.icon"}, {"C:\\Program Files\\Common Files\\System", "5", "Ado/20", "Msadc/21", "Ole db/22", "Directdb.dll", "Wab32.dll", "Wab32res.dll"}, {"C:\\Program Files\\Common Files\\System\\Ado", "19", "Adoapt15.reg", "Adofre15.reg", "Adojavas.inc", "ADOMDReadme.txt", "ADOReadme.txt", "Adovbs.inc", "ADOXReadme.txt", "JROreadme.txt", "Makapt15.bat", "Makfre15.bat", "Msader15.dll", "Msado15.dll", "Msado20.tlb", "Msadomd.dll", "Msador15.dll", "Msadox.dll", "Msadrh15.dll", "Msjro.dll"}, {"C:\\Program Files\\Common Files\\System\\Msadc", "19", "Adcjavas.inc", "Adcvbs.inc", "Handler.reg", "Handsafe.reg", "Handunsf.reg", "Msadce.dll", "Msadcer.dll", "Msadcf.dll", "Msadcfr.dll", "Msadco.dll", "Msadcor.dll", "Msadcs.dll", "Msadds.dll", "Msaddsr.dll", "Msdaprsr.dll", "Msdaprst.dll", "Msdarem.dll", "Msdaremr.dll", "Msdfmap.dll"}, {"C:\\Program Files\\Common Files\\System\\Ole db", "19", "Msdadc.dll", "Msdaenum.dll", "Msdaer.dll", "Msdaipp.dll", "Msdaora.dll", "Msdaosp.dll", "Msdapml.dll", "Msdaps.dll", "Msdasc.cnt", "Msdasc.dll", "Msdasc.hlp", "Msdasc.txt", "Msdasql.dll", "Msdasqlr.dll", "MSDASQLreadme.txt", "Msdatl2.dll", "Msdatt.dll", "Msdaurl.dll", "MSOrclOLEDBreadme.txt", "Msxactps.dll"}, {"C:\\Program Files\\DirectX", "1", "Setup/24"}, {"C:\\Program Files\\DirectX\\Setup", "23", "DxDiag.lnk"}, {"C:\\Program Files\\Internet Explorer", "1", "Connection Wizard/26", "Plugins/27", "Signup/28", "Hmmapi.dll", "Ie4.dll", "Iedetect.dll", "Iesetup.cif", "Iexplore.exe"}, {"C:\\Program Files\\Internet Explorer\\Connection Wizard", "25", "Icwconn.dll", "Icwconn1.exe", "Icwconn2.exe", "Icwdl.dll", "Icwhelp.dll", "Icwip.dun", "Icwoobe.exe", "Icwres.dll", "Icwrmind.exe", "Icwtutor.exe", "Icwutil.dll", "Icwx25a.dun", "Icwx25b.dun", "Icwx25c.dun", "Inetwiz.exe", "Isignup.exe", "Msicw.isp", "Msn.isp", "Phone.icw", "Phone.ver"}, {"C:\\Program Files\\Internet Explorer\\Plugins", "25"}, {"C:\\Program Files\\Internet Explorer\\Signup", "25", "Install.ins"}, {"C:\\Program Files\\NetMeeting", "1", "Blip.wav", "Callcont.dll", "Cb32.exe", "Conf.exe", "Confmrsl.dll", "Dcap16.dll", "Dcap32.dll", "H323cc.dll", "Mst120.dll", "Mst123.dll", "Nac.dll", "Netmeet.htm", "Nmas.dll", "Nmaswin.dll", "Nmchat.dll", "Nmcom.dll", "Nmft.dll", "Nmoldwb.dll", "Nmwb.dll", "Ringin.wav"}, {"C:\\Program Files\\Online Services", "1", "Aol/31", "At&t/32", "CompuServe/33", "Msn50/34", "Prodigy/36"}, {"C:\\Program Files\\Online Services\\Aol", "30", "Aolsetup.exe"}, {"C:\\Program Files\\Online Services\\At&t", "30", "Attsetup.exe"}, {"C:\\Program Files\\Online Services\\CompuServe", "30", "Cssetup.exe"}, {"C:\\Program Files\\Online Services\\Msn50", "30", "Ocx/35", "Msn50.cab", "Msnboot.exe", "Msnlogo.ico"}, {"C:\\Program Files\\Online Services\\Msn50\\Ocx", "34", "Msnsetup.dll"}, {"C:\\Program Files\\Online Services\\Prodigy", "30", "Pro_acce.gif", "Pro_acce.htm", "Pro_can.ins", "Pro_canc.gif", "Pro_cont.htm", "Pro_indx.htm", "Pro_logo.gif", "Pro_mail.gif", "Pro_memb.gif", "Pro_navi.htm", "Pro_ols.isp", "Pro_orb.ico", "Pro_orga.gif", "Pro_orga.htm", "Pro_pcma.gif", "Pro_pers.gif", "Pro_pers.htm", "Pro_reso.gif", "Pro_reso.htm", "Pro_sign.gif"}, {"C:\\Program Files\\Outlook Express", "1", "Junkmail.lko", "Msimn.exe", "Msoe.dll", "Msoe.txt", "Msoeres.dll", "Oeimport.dll", "Oejunk.dll", "Oemig50.exe", "Oemiglib.dll", "Setup50.exe", "Wab.exe", "Wabfind.dll", "Wabimp.dll", "Wabmig.exe"}, {"C:\\Program Files\\Plus!", "1", "Themes/39", "Sysagent.exe"}, {"C:\\Program Files\\Plus!\\Themes", "38"}, {"C:\\Program Files\\Uninstall Information", "1", "ConnectionConfiguration/41", "IE UserData/42", "IE.HKCUZoneInfo/43", "IE.HKLMZoneInfo/44", "IE40.Assoc/45", "IE40.Browser/46", "IE40.BrowseUI/47", "IE40.Comctl32/48", "IE40.Controls/49", "IE40.Shdoc401/50", "IE40.Shell/51", "IE40.Shell32/52", "IE40.UserAgent/53", "IE4Shell95UserData/54", "IEContentAdvisor.Assoc/55", "Mshtml.DllReg/56", "Mshtml.Install/57", "Msieftp/58"}, {"C:\\Program Files\\Uninstall Information\\ConnectionConfiguration", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE UserData", "40", "Ainf0000", "IE UserData.DAT", "IE UserData.INI"}, {"C:\\Program Files\\Uninstall Information\\IE.HKCUZoneInfo", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE.HKLMZoneInfo", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE40.Assoc", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE40.Browser", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE40.BrowseUI", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE40.Comctl32", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE40.Controls", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE40.Shdoc401", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE40.Shell", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE40.Shell32", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE40.UserAgent", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\IE4Shell95UserData", "40", "Ainf0000", "IE4Shell95UserData.DAT", "IE4Shell95UserData.INI"}, {"C:\\Program Files\\Uninstall Information\\IEContentAdvisor.Assoc", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\Mshtml.DllReg", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\Mshtml.Install", "40", "Ainf0000"}, {"C:\\Program Files\\Uninstall Information\\Msieftp", "40", "Ainf0000"}, {"C:\\Program Files\\Windows Media Player", "1", "Laprxy.dll", "Logagent.exe", "Mplayer2.exe"}, {"C:\\Windows", "0", "All Users/61", "Application Data/66", "Applog/74", "Catroot/75", "Command/76", "Config/77", "Cookies/78", "Cursors/79", "Desktop/80", "Downloaded Program Files/82", "Drwatson/83", "Favorites/84", "Fonts/88", "Help/89", "History/90", "Inf/94", "Java/96", "Media/100", "NetHood/101", "Offline Web Pages/102"}, {"C:\\Windows\\All Users", "60", "Desktop/62", "Start Menu/63"}, {"C:\\Windows\\All Users\\Desktop", "61", "Connect to the Internet.LNK"}, {"C:\\Windows\\All Users\\Start Menu", "61", "Programs/64"}, {"C:\\Windows\\All Users\\Start Menu\\Programs", "63", "StartUp/65"}, {"C:\\Windows\\All Users\\Start Menu\\Programs\\StartUp", "64"}, {"C:\\Windows\\Application Data", "60", "Identities/67", "Microsoft/69"}, {"C:\\Windows\\Application Data\\Identities", "66", "{8FA78040-0DE2-11E9-996C-CDBA93926046}/68"}, {"C:\\Windows\\Application Data\\Identities\\{8FA78040-0DE2-11E9-996C-CDBA93926046}", "67"}, {"C:\\Windows\\Application Data\\Microsoft", "66", "Internet Explorer/70", "Office/72", "Welcome/73"}, {"C:\\Windows\\Application Data\\Microsoft\\Internet Explorer", "69", "Quick Launch/71"}, {"C:\\Windows\\Application Data\\Microsoft\\Internet Explorer\\Quick Launch", "70", "Launch Internet Explorer Browser.lnk", "Launch Outlook Express.lnk", "Show Desktop.scf"}, {"C:\\Windows\\Application Data\\Microsoft\\Office", "69", "Webfdr16.inf"}, {"C:\\Windows\\Application Data\\Microsoft\\Welcome", "69", "Default.wbm", "Icw.wbm", "Logo.wbm", "Ms.wbm", "Regwiz.wbm", "Tour.wbm", "Tuneup.wbm", "Welcom98.wav", "Welcome.dat", "Weldata.exe", "Win.wbm"}, {"C:\\Windows\\Applog", "60", "APPLog.dnd", "Icwconn1.lgc", "Mplayer2.lgc", "Rundll32.lgc", "Rundll32.~~c", "Wupdmgr.lgc"}, {"C:\\Windows\\Catroot", "60", "Catalog3.cab"}, {"C:\\Windows\\Command", "60", "Ansi.sys", "Attrib.exe", "Bootdisk.bat", "Chkdsk.exe", "Choice.com", "Country.sys", "Cscript.exe", "Cvt.exe", "Debug.exe", "Deltree.exe", "Diskcopy.com", "Display.sys", "Doskey.com", "Drvspace.bin", "Edit.com", "Edit.hlp", "Ega.cpi", "Extract.exe", "Fc.exe", "Fdisk.exe"}, {"C:\\Windows\\Config", "60", "General.idf"}, {"C:\\Windows\\Cookies", "60", "Index.dat"}, {"C:\\Windows\\Cursors", "60", "Appstart.ani", "Globe.ani", "Hourglas.ani"}, {"C:\\Windows\\Desktop", "60", "Online Services/81", "Outlook Express.lnk"}, {"C:\\Windows\\Desktop\\Online Services", "80", "About The Online Services.lnk", "America Online.lnk", "AT&T WorldNet Service.lnk", "CompuServe.lnk", "Prodigy Internet.lnk"}, {"C:\\Windows\\Downloaded Program Files", "60", "Desktop.ini", "DirectAnimation Java Classes.osd", "Internet Explorer Classes for Java.osd", "Microsoft XML Parser for Java.osd"}, {"C:\\Windows\\Drwatson", "60", "Frame.htm"}, {"C:\\Windows\\Favorites", "60", "Channels/85", "Links/86", "Media/87", "Desktop.ini", "MSN.url", "Radio Station Guide.url", "Web Events.url"}, {"C:\\Windows\\Favorites\\Channels", "84"}, {"C:\\Windows\\Favorites\\Links", "84", "Best of the Web.url", "Channel Guide.url", "Customize Links.url", "Free HotMail.url", "Internet Start.url", "Microsoft.url", "Windows Update.url", "Windows.url"}, {"C:\\Windows\\Favorites\\Media", "84", "ABC News and Entertainment.url", "Bloomberg.url", "Capitol Records.url", "CBS.url", "CNBC Dow Jones Business Video.url", "CNET Today - Technology News.url", "CNN Videoselect.url", "Disney.url", "ESPN Sports.url", "Fox News.url", "Fox Sports.url", "Hollywood Online.url", "Internet Radio Guide.url", "MSNBC.url", "MUSICVIDEOS.COM.url", "NBC VideoSeeker.url", "TV Guide Entertainment Network.url", "Universal Studios Online.url", "Warner Bros. Hip Clips.url", "What's On Now.url"}, {"C:\\Windows\\Fonts", "60", "8514fix.fon", "8514oem.fon", "8514sys.fon", "Arial.ttf", "Arialbd.ttf", "Arialbi.ttf", "Ariali.ttf", "Ariblk.ttf", "Comic.ttf", "Comicbd.ttf", "Cour.ttf", "Courbd.ttf", "Courbi.ttf", "Coure.fon", "Courf.fon", "Couri.ttf", "Desktop.ini", "Dosapp.fon", "Impact.ttf", "Lucon.ttf"}, {"C:\\Windows\\Help", "60", "31users.chm", "98update.chm", "Access.chm", "Access.cnt", "Access.hlp", "Accessib.chm", "Amovie.chm", "Amovie.hlp", "Apps.hlp", "Audiocdc.hlp", "Batch98.chm", "Bnts.dll", "Brep.chm", "Brep.cnt", "Brep.hlp", "Calc.chm", "Calc.cnt", "Calc.hlp", "Camera.chm", "Cdplayer.chm"}, {"C:\\Windows\\History", "60", "History.IE5/91", "Desktop.ini"}, {"C:\\Windows\\History\\History.IE5", "90", "MSHist012019010120190102/92", "MSHist012019010420190105/93", "Desktop.ini", "Index.dat"}, {"C:\\Windows\\History\\History.IE5\\MSHist012019010120190102", "91", "Index.dat"}, {"C:\\Windows\\History\\History.IE5\\MSHist012019010420190105", "91", "Index.dat"}, {"C:\\Windows\\Inf", "60", "Catalog/95", "1394.inf", "Adapter.inf", "Amovie.inf", "Appletpp.inf", "Applets.inf", "Applets1.inf", "Apps.inf", "Atitunep.inf", "Atixbar.inf", "Atmuni.inf", "Awupd.inf", "Axa.inf", "Biosinfo.inf", "Bt829.inf", "Cabpayie.inf", "Cchat25.inf", "Cemmf.inf", "Clip.inf", "Cwbaud98.inf"}, {"C:\\Windows\\Inf\\Catalog", "94"}, {"C:\\Windows\\Java", "60", "Classes/97", "Packages/98"}, {"C:\\Windows\\Java\\Classes", "96"}, {"C:\\Windows\\Java\\Packages", "96", "Data/99", "0tntzhbp.zip", "1bzrtvjr.zip", "577dbhzr.zip", "5r7vlfv7.zip", "Brn7tnd3.zip", "H7jhzzf3.zip", "Hnv3lfdv.zip", "Q5b9z7h3.zip", "Vxnjxv79.zip"}, {"C:\\Windows\\Java\\Packages\\Data", "98", "53z9zdzj.dat", "577dbhzr.dat", "6fr5bzjx.dat", "9n39rflz.dat", "Eg6mpz7z.dat", "Fp3zd757.dat", "Gdbpzr9r.dat", "Jpn3rhvj.dat", "Pfbb3lbb.dat", "Rpnpfpzd.dat", "Uch3x7hv.dat"}, {"C:\\Windows\\Media", "60", "Chimes.wav", "Chord.wav", "Ding.wav", "Logoff.wav", "Notify.wav", "Recycle.wav", "Start.wav", "Tada.wav", "The Microsoft Sound.wav"}, {"C:\\Windows\\NetHood", "60"}, {"C:\\Windows\\Offline Web Pages", "60", "Desktop.ini"}};
}
