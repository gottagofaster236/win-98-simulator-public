package com.lr_soft.windows98simulator.Applications;

import android.os.Environment;

import com.lr_soft.windows98simulator.R;
import com.lr_soft.windows98simulator.System.MessageBox;
import com.lr_soft.windows98simulator.System.Window;
import com.lr_soft.windows98simulator.WindowsView;

import java.io.File;
import java.io.IOException;

public class MyDocuments extends Explorer {  // Папка, в которой содержатся созданные пользователем файлы
    private File directory;

    public MyDocuments(){
        this(getFilesDir());
    }

    public MyDocuments(File directory){
        super(getDirectoryName(directory),
                directory.equals(getFilesDir())? "My Documents" : getFullPath(directory),
                getSmallIcon(directory),
                getBigIcon(directory), true, directory);
        this.directory = directory;

        linkContainer.initFromDirectory(directory, null, this);

        /*if(directory.equals(getFilesDir())) {
            SharedPreferences sharedPreferences = getSharedPreferences();
            final String key = "myDocsTutorialShowedTimes";
            int showedTimes = sharedPreferences.getInt(key, 0);
            if (showedTimes < 5 || linkContainer.elements.isEmpty()) {  // 5 раз показываем
                Toast.makeText(context, R.string.mydocs_tutorial, Toast.LENGTH_LONG).show();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(key, showedTimes + 1);
                editor.apply();
            }
        }*/
    }



    public static String getFullPath(File directory){
        if(directory == null){
            //Log.w(TAG, "Warning! directory is null at ", new Exception());
            return "";
        }
        try{
            if(isInFilesDir(directory)) {
                String relativePath = directory.getCanonicalPath()
                        .substring(getFilesDir().getCanonicalPath().length())
                        .replace("/", "\\");
                if (relativePath.startsWith("\\Desktop"))
                    return "C:\\Windows\\Desktop" + relativePath.substring(8);
                else
                    return "C:\\My Documents" + relativePath;
            }
            else{
                String relativePath = directory.getCanonicalPath()
                        .substring(getExternalStorageDirectory().getCanonicalPath().length())
                        .replace("/", "\\");
                if(!relativePath.isEmpty())
                    return "D:" + relativePath;
                else
                    return "D:\\";
            }
        }
        catch (IOException e){
            return "My Documents";
        }
    }

    static String getRelativePath(File file){  // только для файлов в getFilesDir
        String result;
        try {
            result = file.getCanonicalPath().substring(getFilesDir().getCanonicalPath().length());
        }
        catch (IOException e){
            result = file.getAbsolutePath().substring(getFilesDir().getAbsolutePath().length());
        }

        if(result.startsWith(File.separator))  // если это My Documents, то result будет пустым
            result = result.substring(1);
        return result;
    }

    public static boolean isInFilesDir(File file){
        return isInSubfolder(getFilesDir(), file);
    }

    private static boolean isInSubfolder(File folder, File file){
        try{
            String folderPath = folder.getCanonicalPath();
            String filePath = file.getCanonicalPath();
            return filePath.startsWith(folderPath + File.separator) || filePath.equals(folderPath);
        }
        catch (IOException e){
            return false;
        }
    }

    private static File getFilesDirCached = null;

    public static File getFilesDir(){
        if(getFilesDirCached != null)
            return getFilesDirCached;
        File file = context.getExternalFilesDir(null);
        if(file == null)  // нет карты памяти
            file = context.getFilesDir();
        if(!file.exists())
            file.mkdirs();
        File desktopRoot = new File(file, "Desktop");
        if(!desktopRoot.exists())
            desktopRoot.mkdir();
        getFilesDirCached = file;
        return file;
    }

    private static File getDesktopDirCached = null;

    public static boolean externalStorageAvailable(){
        return getFilesDir().equals(context.getExternalFilesDir(null));
    }

    public static File getDesktopDirectory(){
        if(getDesktopDirCached != null)
            return getDesktopDirCached;
        return (getDesktopDirCached = new File(getFilesDir(), "Desktop"));
    }

    public static File getExternalStorageDirectory(){
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return Environment.getExternalStorageDirectory();
        else
            return null;
    }

    static String getDirectoryName(File directory){
        if(directory.equals(getFilesDir()))
            return "My Documents";
        else if(directory.equals(getDesktopDirectory()))
            return "Desktop";
        else if(directory.equals(getExternalStorageDirectory()))
            return "(D:)";
        else
            return directory.getName();
    }

    static int getSmallIcon(File directory){
        if(directory.equals(getFilesDir()))
            return R.drawable.directory_open_file_mydocs_2;
        else if(directory.equals(getDesktopDirectory()))
            return R.drawable.desktop_3;
        else if(directory.equals(getExternalStorageDirectory()))
            return R.drawable.cd_drive_4;
        else
            return R.drawable.directory_open_2;
    }

    private static int getBigIcon(File directory){
        if(directory.equals(getFilesDir()))
            return R.drawable.directory_open_file_mydocs_0;
        else if(directory.equals(getDesktopDirectory()))
            return R.drawable.desktop_0;
        else if(directory.equals(getExternalStorageDirectory()))
            return R.drawable.cd_drive_5;
        else
            return R.drawable.folder;
    }

    @Override
    protected void upOneLevel() {
        Explorer openingFolder;
        if(directory.equals(getFilesDir())){  // My Documents -> Desktop
            openingFolder = new MyDocuments(MyDocuments.getDesktopDirectory());
        }
        else if(directory.equals(getDesktopDirectory())){  // это Desktop, он идёт в C:\Windows
            openingFolder = new Explorer(Explorer.WINDOWS_INDEX);
        }
        else if(directory.equals(getExternalStorageDirectory())){
            openingFolder = new MyComputer();
        }
        else{  // Подпапка в My Documents
            File parentDirectory = directory.getParentFile();
            openingFolder = new MyDocuments(parentDirectory);
        }
        openingFolder.alignWith(this);
    }

    public static MyDocuments createDriveD(){
        return new MyDocuments(MyDocuments.getExternalStorageDirectory());
    }

    public static void diskNotAccessible(Window parentWindow){
        new MessageBox(parentWindow != null? parentWindow.getTitle() : "Disk error",
                "D:\\ is not accessible.\n\nThe device is not ready.",
                MessageBox.OK, MessageBox.ERROR, null, parentWindow);
        WindowsView.windowsView.invalidate();
    }
}
