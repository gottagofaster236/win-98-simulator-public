package com.lr_soft.windows98simulator.Applications;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.System.Windows98;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MsDos extends BaseNotepad {
    Bitmap textBitmap;
    Canvas textCanvas;  // так как текст в TextBox может вылезать за верхнюю границу
    int maxNoScrollLines;
    boolean startedScrolling = false;  // последняя строчка находится внизу экрана
    int initialTopMargin;  // до скроллинга
    Explorer explorer = new DriveC(true);
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm:ss:SSSa", Locale.US);
    {
        DateFormatSymbols symbols = timeFormat.getDateFormatSymbols();
        symbols.setAmPmStrings(new String[] {"a", "p"});  // вместо am и pm использовать одну букву
        timeFormat.setDateFormatSymbols(symbols);  // так как getDateFormatSymbols() возвращает копию
    }
    private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MM-dd-yyyy", Locale.US);

    private static final String initialText = "\n\nMicrosoft(R) Windows 98\n   (C)Copyright Microsoft Corp 1998-1999.";
    private static final String driveLabel = "\n Volume in drive C has no label\n Volume Serial Number is 123E-07FC";

    private MsDos(Rect textInput, int leftMargin, int topMargin, Paint p, Rect cursor){
        super("MS-DOS Prompt",
                textInput, leftMargin, topMargin, p, cursor,
                R.drawable.ms_dos, !isMsDosMode()? R.drawable.ms_dos_prompt : 0);
        drawElements = false;
        textBitmap = createBitmap(textBox.width, textBox.height, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);
        centerWindowHorizontally();
        y = y_old = 0;

        maxNoScrollLines = !isMsDosMode()? 17 : 25;
        textBox.isMsDosPrompt = true;
        textBox.backgroundColor = Color.BLACK;
        textBox.textColor = !isMsDosMode()? Color.rgb(190, 197, 198) : Color.rgb(168, 168, 168);
        textBox.lineMargin = !isMsDosMode()? 12 : 16;
        if(isMsDosMode())
            textBox.whiteCursor = textBox.textColor;
        initialTopMargin = topMargin;

        print(initialText);
        printWorkingDirectory();
    }

    public MsDos(){
        this(new Rect(5, 24, 598 - 6, 244 - 6),
                1, 14, p_dos, new Rect(0, 1, 8, 2));
    }

    public static void createMsDosMode(){
        MsDos msDos = new MsDos(new Rect(0, 0, 720, 400),
                0, 12, p_dos_mode, new Rect(0, 1, 9, 3));
        msDos.x = msDos.y = 0;
        msDos.topButtons.visible = false;
    }

    @Override
    public void onNewDraw(Canvas canvas, int x, int y) {
        super.onNewDraw(canvas, x, y);
        textBox.parent = this;
        textBox.drawingBitmap = textBitmap;
        textBox.onDraw(textCanvas, 0, 0);
        for (Element el : elements) {
            if (!el.visible)
                continue;
            if (el == topMenu)
                continue;
            if(el == textBox){
                canvas.drawBitmap(textBitmap, x + textBox.x, y + textBox.y, null);
                continue;
            }
            el.parent = this;
            el.onDraw(canvas, x + el.x, y + el.y);
        }
    }

    private void print(String str){
        textBox.text += str + "\n";
    }

    private void print(){
        textBox.text += "\n";
    }

    private void updateTextScrolling(){
        if(!startedScrolling){

            if(textBox.lines.length > maxNoScrollLines)
                startedScrolling = true;
        }
        if(startedScrolling){  // иначе ничего делать не надо
            // Если у нас очень много строк (строк, в смысле разделённых \n), то верхние строки можно удалить
            List<String> lines = new ArrayList<>();  // линии с концом \n
            StringBuilder curLine = new StringBuilder();
            for(int i = 0; i < textBox.text.length(); i++){
                char c = textBox.text.charAt(i);
                if(c != '\n')
                    curLine.append(c);
                else{
                    curLine.append(c);
                    lines.add(curLine.toString());
                    curLine.setLength(0);
                }
            }
            if(curLine.length() > 0)
                lines.add(curLine.toString());
            if(lines.size() > maxNoScrollLines + 2){
                String oldText = textBox.text;
                StringBuilder newText = new StringBuilder();
                for(int i = lines.size() - (maxNoScrollLines + 2); i < lines.size(); i++)
                    newText.append(lines.get(i));
                textBox.text = newText.toString();
                int deletedChars = (oldText.length() - textBox.text.length());
                textBox.updateLinesAndCursors();
                textBox.setCursorPos(textBox.getCursorPos() - deletedChars);
                textBox.minCursor -= deletedChars;
            }
            int textHeight = (textBox.lines.length - 1) * textBox.lineMargin;  // y, с которым будет отрисовываться последняя строчка относительно topMargin
            textBox.topMargin = textBox.height - textHeight - (isMsDosMode()? 4 : 3);
            //textBox.updateLinesAndCursors();  // так как изменили topMargin
        }
    }

    @Override
    public void onKeyPress(String key) {
        if(key.codePointAt(0) >= 127) {  // в шрифте нет unicode-символов
            makeSnackbar(R.string.only_eng_keyboard_is_supported, Snackbar.LENGTH_SHORT);
            return;
        }
        super.onKeyPress(key);
        if(key.equals("\n")){  // Enter - обрабатываем последнюю строчку
            String lastCommand = textBox.text.substring(textBox.minCursor, textBox.text.length() - 1);  // без самого \n
            lastCommand = lastCommand.trim();
            if(lastCommand.equalsIgnoreCase("cd.."))
                lastCommand = "cd ..";
            String[] words = lastCommand.split(" ");

            boolean badCommand = false;

            if(lastCommand.isEmpty()){
                // убираем \n, чтобы правильно напечатать приглашение MS-DOS
                textBox.text = textBox.text.substring(0, textBox.text.length() - 1);
            }
            else if(lastCommand.length() >= 4 && lastCommand.substring(0, 4).equalsIgnoreCase("echo")){
                if(lastCommand.length() >= 5)
                    print(lastCommand.substring(5));
                else
                    print("ECHO is on");
            }
            else if(words.length == 1){
                String command = words[0];
                command = command.toLowerCase();
                if(command.equals("ver")){
                    print("\nWindows 98 [Version 4.10.2222]\n");
                }
                else if(command.equals("exit")){
                    if(isMsDosMode())
                        returnToWindows();
                    else
                        close();
                }
                else if(command.equals("cd")){  // cd без параметров
                    print(explorer.fullPath);
                }
                else if(command.equals("dir")){
                    print(driveLabel);
                    print(" Directory of " + explorer.fullPath + "\n");
                    for(Element element : explorer.linkContainer.elements){
                        if(!(element instanceof Link))
                            continue;
                        Link link = (Link) element;
                        String newFilename = link.fullFilename.toUpperCase();
                        String extension = "";
                        if(newFilename.contains(".")) {
                            int dotIndex = newFilename.lastIndexOf(".");
                            extension = newFilename.substring(dotIndex + 1);
                            newFilename = newFilename.substring(0, dotIndex);
                        }

                        if(newFilename.length() <= 8)
                            newFilename = String.format("%-8.8s", newFilename);  // дополняем пробелами справа
                        else
                            newFilename = newFilename.substring(0, 6) + "~1";

                        print(String.format("%s %-3.3s   %s    04-23-99 10:22p %s",
                                newFilename, extension, link.isFolder? "<DIR>" : "     ", link.fullFilename));
                    }
                    print();
                }
                else if(command.equals("time")){
                    print("Current time is " + timeFormat.format(new Date()));
                }
                else if(command.equals("date")){
                    print("Current date is " + dateFormat.format(new Date()));
                }
                else if(command.equals("cls")){  // возвращаем всё в исходное состояние
                    startedScrolling = false;
                    textBox.topMargin = initialTopMargin;
                    textBox.text = "";
                }
                else if(command.equals("help")){
                    print("<CD  > Displays/changes the current directory.");
                    print("<CLS > Clear screen.");
                    print("<DATE> Displays or changes the internal date.");
                    print("<DIR > Directory View.");
                    print("<ECHO> Display messages and enable/disable command echoing.");
                    print("<EXIT> Exit from the shell.");
                    print("<HELP> Show help.");
                    print("<MEM > Displays the amount of used and free memory.");
                    print("<TIME> Displays the internal time.");
                    print("<VER > View and set the reported DOS version.");
                    print("<VOL > Displays the disk volume label and serial number.");
                }
                else if(command.equals("mem")){
                    print();
                    print("Memory Type        Total       Used       Free  ");
                    print("----------------  --------   --------   --------");
                    print("Conventional          640K        39K       601K");
                    print("Upper                   0K         0K         0K");
                    print("Reserved              384K       384K         0K");
                    print("Extended (XMS)     64,512K       164K    64,348K");
                    print("----------------  --------   --------   --------");
                    print("Total memory       65,536K       587K    64,949K\n");
                    print("Total under 1 MB      640K        39K       601K\n");
                    print("Total Expanded (EMS)                   64M (66,600,960 bytes)");
                    print("Free Expanded (EMS)                    16M (16,777,216 bytes)\n");
                    print("Largest executable program size       601K (614,912 bytes)");
                    print("Largest free upper memory block         0K       (0 bytes)");
                    print("MS-DOS is resident in the high memory area.");
                }
                else if(command.equals("vol")){
                    print(driveLabel);
                }
                else if(command.equals("win")){
                    if(isMsDosMode())
                        returnToWindows();
                    else{
                        print("You are already running Windows.\n");
                        print("- Press ALT+ENTER to switch this MS-DOS prompt between");
                        print("  windowed and full-screen display.");
                        print("- Type Exit and press Enter to quit this MS-DOS prompt and");
                        print("  return to Windows.");
                        print("- Press ALT+TAB to switch to Windows or another application.");
                    }
                }
                else if(command.length() == 2 && command.endsWith(":")){
                    // смена диска
                    char driveLetter = Character.toUpperCase(command.charAt(0));
                    if(driveLetter >= 'A' && driveLetter <= 'Z') {
                        if(driveLetter != 'C')
                            print("\n\nNot ready reading drive " + driveLetter);
                    }
                    else
                        print("Invalid drive specification");
                }
                else{
                    badCommand = true;
                }
            }
            else if(words.length >= 2){
                String command = words[0];
                command = command.toLowerCase();
                if(command.equals("cd")){
                    String oldPath = explorer.fullPath;
                    if(!words[1].startsWith("\"") && words.length > 2){
                        print("Too many parameters");
                    }
                    else try {
                        changeDirectory(lastCommand.substring(lastCommand.indexOf(' ') + 1));
                    }
                    catch (BadFilenameException e){
                        print("Invalid directory");
                        try {
                            changeDirectory(oldPath);
                        }
                        catch (BadFilenameException ignored) {}
                    }
                }
                else
                    badCommand = true;
            }
            else
                badCommand = true;

            if(badCommand){  // возможно, мы открываем файл
                badCommand = false;
                if(lastCommand.startsWith("\"") && lastCommand.endsWith("\"") && lastCommand.length() >= 2)
                    lastCommand = lastCommand.substring(1, lastCommand.length() - 1);
                if(lastCommand.startsWith("\""))  // больше чем одна пара кавычек
                    badCommand = true;
                else{
                    String oldPath = explorer.fullPath;
                    try{
                        if(!lastCommand.contains("\\"))
                            openFile(lastCommand);
                        else{
                            int index = lastCommand.lastIndexOf('\\');
                            String newPath = lastCommand.substring(0, index);
                            String filename = lastCommand.substring(index + 1);
                            changeDirectory(newPath);
                            openFile(filename);
                            changeDirectory(oldPath);
                        }
                    }
                    catch (BadFilenameException e){
                        badCommand = true;
                        if(!explorer.fullPath.equals(oldPath)) {
                            try {
                                changeDirectory(oldPath);
                            }
                            catch (BadFilenameException ignore) {}
                        }
                    }
                }
            }

            if(badCommand){  // пытаемся запустить программу
                Class<? extends Window> windowClass = Run.getWindowClass(lastCommand);
                if(windowClass != null && windowClass != MsDos.class) {
                    if(Windows98.state != Windows98.MS_DOS_MODE) {
                        try {
                            windowClass.newInstance();
                        }
                        catch (IllegalAccessException ignored) {}
                        catch (InstantiationException ignored) {}
                    }
                    else{
                        print("This program requires Microsoft Windows.");
                    }
                    badCommand = false;
                }
            }

            if(badCommand)
                print("Bad command or file name");

            printWorkingDirectory();
        }
        updateTextScrolling();
    }

    public static String getMsDosFilename(String filename){
        filename = filename.toUpperCase();
        String extension = "";
        if(filename.contains(".")) {
            int dotIndex = filename.lastIndexOf(".");
            extension = filename.substring(dotIndex + 1);
            filename = filename.substring(0, dotIndex);
        }
        if(filename.length() > 8)
            filename = filename.substring(0, 6) + "~1";
        return filename + (extension.isEmpty()? "" : "." + extension);
    }

    private void printWorkingDirectory(){
        // напечатать приглашение MS-DOS
        explorer.parent = this;
        textBox.text += "\n" + explorer.fullPath + ">";
        //textBox.minCursor = textBox.cursor_pos = textBox.text.length();
        textBox.updateLinesAndCursors();
        textBox.moveCursorToEnd();
        textBox.minCursor = textBox.getCursorPos();
    }

    private void changeDirectory(String command) throws BadFilenameException {
        if(command.startsWith("\"") && command.endsWith("\"") && command.length() >= 2)
            command = command.substring(1, command.length() - 1);
        if(!command.contains("\\")){  // один файл
            if(command.equals("..")){  // наверх
                if(explorer.parentFolder == -1)
                    throw new BadFilenameException();
                explorer = new Explorer(explorer.parentFolder, true);
            }
            else{
                for(Element element : explorer.linkContainer.elements){
                    Link link = (Link) element;
                    if(link.isFolder && link.fullFilename.equalsIgnoreCase(command)){
                        link.action.run(link);
                        explorer.parent = this;
                        return;
                    }
                }
                //Log.d(TAG, "could not find folder: " + command + ", cur folder: " + explorer.fullPath);
                throw new BadFilenameException();
            }
        }
        else{
            if(command.startsWith("\\") || command.contains("\\\\"))  // бэкслеш должен использоваться правильно
                throw new BadFilenameException();
            if(command.startsWith("C:\\") || command.startsWith("c:\\")){
                explorer = new DriveC(true);
                explorer.parent = this;
                command = command.substring(3);
            }
            for(String folder : command.split("\\\\")) {
                if(!folder.isEmpty())
                    changeDirectory(folder);
            }
        }
    }

    private static class BadFilenameException extends Exception {}

    private void openFile(String filename) throws BadFilenameException{  // filename - имя файла в папке
        if(!filename.contains(".")){  // ищем в порядке: .com, .exe, .bat
            try {
                openFile(filename + ".com");
                return;
            }
            catch (BadFilenameException ignore){}

            try {
                openFile(filename + ".exe");
                return;
            }
            catch (BadFilenameException ignore){}

            openFile(filename + ".bat");
        }
        else{
            for(Element element : explorer.linkContainer.elements){
                if(!(element instanceof Link))
                    continue;
                Link link = (Link) element;
                if(!link.isFolder && link.fullFilename.equalsIgnoreCase(filename)){
                    if(filename.equalsIgnoreCase("command.com")){
                        print(initialText);
                    }
                    else if(!filename.equalsIgnoreCase("autoexec.bat") && link.action != null)
                        link.action.run(link);
                    explorer.parent = this;
                    return;
                }
            }
            //Log.d(TAG, "could not find filename: " + filename + ", cur folder: " + explorer.fullPath);
            throw new BadFilenameException();
        }
    }

    private void returnToWindows(){
        close();
        Windows98.windows98.elements.clear();
        Windows98.windows98.startLoadingWindows();
        Windows98.windows98.startup();
    }

    @Override
    public boolean onMouseOver(int x, int y, boolean touch) {
        if(isMsDosMode())
            return true;
        return super.onMouseOver(x, y, touch);
    }

    private static boolean isMsDosMode(){
        return Windows98.state == Windows98.MS_DOS_MODE;
    }
}
