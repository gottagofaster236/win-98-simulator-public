package com.lr_soft.windows98simulator;

import static com.lr_soft.windows98simulator.System.Element.context;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.lr_soft.windows98simulator.Applications.InternetExplorer;
import com.lr_soft.windows98simulator.Applications.MPlayer;
import com.lr_soft.windows98simulator.Applications.MyDocuments;
import com.lr_soft.windows98simulator.Applications.WebViewContainer;
import com.lr_soft.windows98simulator.System.Element;
import com.lr_soft.windows98simulator.System.Taskbar;
import com.lr_soft.windows98simulator.System.Windows98;

import java.lang.reflect.Method;

public class MainActivity extends AppCompatActivity {
    boolean leftKeyPressed = false, rightKeyPressed = false;
    public SharedPreferences settings;
    ScrollView tutorial;
    CheckBox checkBox;
    public static RelativeLayout windowsViewGroup;
    public static LinearLayout contentRoot;

    private PermissionResultListener permissionResultListener;
    public boolean firstLaunch = false;
    public boolean afterUpdate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Element.context = this;
        Element.resources = getResources();
        setContentView(R.layout.activity_main);
        hideSystemUI();
        /*if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }*/
        settings = getSharedPreferences("settings", Context.MODE_PRIVATE);
        // установлены ли предыдущие версии?
        String installedVersions = settings.getString("installedVersions", "");
        firstLaunch = installedVersions.isEmpty();
        if(!installedVersions.contains(BuildConfig.VERSION_NAME)) {
            afterUpdate = true;
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("installedVersions", installedVersions + ";" + BuildConfig.VERSION_NAME);
            editor.apply();
        }
        WindowsView.windowsView = findViewById(R.id.windowsView);
        contentRoot = findViewById(R.id.root_view);
        RelativeLayout oldWindowsViewGroup = windowsViewGroup;
        windowsViewGroup = findViewById(R.id.windowsViewGroup);
        boolean recreatedViews = oldWindowsViewGroup != windowsViewGroup;
        //Log.d(TAG, "onCreate! recreatedviews: " + recreatedViews);
        if(Windows98.windows98 != null && Windows98.state != Windows98.WAIT_FOR_STARTUP) {  // мы уже работаем
            if(recreatedViews) {
                for (Element element : Windows98.windows98.elements) {
                    if (element instanceof InternetExplorer) {  // InternetExplorer использует WebView... Так как при onCreate весь layout создаётся заново, надо туда обратно добавить все WebView
                        InternetExplorer ie = (InternetExplorer) element;
                        WebView webView = ie.webViewContainer.webView;
                        ViewGroup.LayoutParams params = webView.getLayoutParams();
                        if(webView.getParent() != null)
                            ((ViewGroup) webView.getParent()).removeView(webView);
                        windowsViewGroup.addView(webView, params);
                    }
                    else if(element instanceof MPlayer){
                        TextureView textureView = ((MPlayer) element).textureView;
                        if(textureView.getParent() != null)
                            ((ViewGroup) textureView.getParent()).removeView(textureView);
                        windowsViewGroup.addView(textureView, new RelativeLayout.LayoutParams(textureView.getWidth(), textureView.getHeight()));
                    }
                    WindowsView.windowsView.bringToFront();
                }
            }
            return;
        }
        if(Windows98.windows98 == null)
            Windows98.windows98 = new Windows98();
        // туториал
        boolean showTutorialOnStartup = settings.getBoolean("showOnStartup", true);
        checkBox = findViewById(R.id.checkBox);
        checkBox.setChecked(showTutorialOnStartup);
        if(showTutorialOnStartup && !Windows98.TAUON) {
            tutorial = findViewById(R.id.tutorial);
            Typeface coolTf = Typeface.createFromAsset(getAssets(), "HKGrotesk-Medium.otf");
            TextView t1 = findViewById(R.id.tView1), t2 = findViewById(R.id.tView2), t3 = findViewById(R.id.tView3);
            Button b = findViewById(R.id.button);
            t1.setTypeface(coolTf);
            t2.setTypeface(coolTf);
            t3.setTypeface(coolTf);
            b.setTypeface(coolTf);
            tutorial.setVisibility(View.VISIBLE);
            windowsViewGroup.setVisibility(View.GONE);
        }
        else {
            hideTutorial();
            Windows98.windows98.startup();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(Windows98.state == Windows98.WORKING && WebViewContainer.customView != null)
            return super.dispatchKeyEvent(event);
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:   // ЛКМ
                if(action == KeyEvent.ACTION_DOWN) {
                    if(leftKeyPressed)
                        return true;
                    leftKeyPressed = true;
                    WindowsView.windowsView.onCursorDown();
                }
                else if(action == KeyEvent.ACTION_UP){
                    if(!leftKeyPressed)
                        return true;
                    leftKeyPressed = false;
                    WindowsView.windowsView.onCursorUp();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:   // ПКМ
                if (action == KeyEvent.ACTION_DOWN) {
                    if(rightKeyPressed)
                        return true;
                    rightKeyPressed = true;
                    WindowsView.windowsView.onRightDown();
                }
                else if(action == KeyEvent.ACTION_UP){
                    if(!rightKeyPressed)
                        return true;
                    rightKeyPressed = false;
                    WindowsView.windowsView.onRightUp();
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public void onTutorialEndClick(View view){
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean("showOnStartup", checkBox.isChecked());
        edit.apply();
        hideTutorial();
        windowsViewGroup.setVisibility(View.VISIBLE);
        WindowsView.windowsView.requestFocus();
        Windows98.windows98.startup();
    }

    private void hideTutorial(){
        //Log.d(TAG, "tutorial hidden");
        contentRoot.removeView(tutorial);
    }

    public void onCheckboxClick(View view){
        checkBox.toggle();
    }

    public void checkWriteExternalPermission(PermissionResultListener listener){
        if(!MyDocuments.externalStorageAvailable())
            listener.onPermissionDenied();
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            listener.onPermissionGranted();
        else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                listener.onPermissionGranted();
            }
            else {  // просим разрешение
                this.permissionResultListener = listener;
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
        else{
            if(Environment.isExternalStorageManager()){
                listener.onPermissionGranted();
            }
            else{  // просим разрешение
                this.permissionResultListener = listener;
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                startActivityForResult(intent, 1);
            }
        }
    }

    public interface PermissionResultListener {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode != 1)
            return;
        if(permissionResultListener == null)  // crash в Play Console
            return;
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            permissionResultListener.onPermissionGranted();
        else
            permissionResultListener.onPermissionDenied();
        permissionResultListener = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 1 || permissionResultListener == null || Build.VERSION.SDK_INT < 30)
            return;
        if (Environment.isExternalStorageManager())
            permissionResultListener.onPermissionGranted();
        else
            permissionResultListener.onPermissionDenied();
    }

    /*@Override
    public void onBackPressed() {
        WindowsView.windowsView.onRightDown();
        WindowsView.windowsView.onRightUp();
    }*/

    private float oldSystemVolume;

    @Override
    protected void onPause() {
        if(Windows98.state == Windows98.WORKING) {
            oldSystemVolume = Taskbar.volumeControl.getSystemVolume();
            for(Element element : Windows98.windows98.elements){
                if(element instanceof InternetExplorer){
                    WebViewContainer webViewContainer = ((InternetExplorer) element).webViewContainer;
                    if(WebViewContainer.customView != null)
                        webViewContainer.webChromeClient.onHideCustomView();
                }
            }
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT >= 24) {
            WindowsView.windowsView.setPointerIcon(WindowsView.windowsCursor);
        }
        if(Windows98.state == Windows98.WORKING) {
            if(Taskbar.volumeControl.getSystemVolume() != oldSystemVolume)
                Taskbar.volumeControl.onSystemVolumeUpdate();
        }
    }

    // (c) https://stackoverflow.com/a/23861333/6120487
    public static void getScreenSize(Point point){
        Display display = context.getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 17) {
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            point.x = realMetrics.widthPixels;
            point.y = realMetrics.heightPixels;
        }
        else{
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                point.x = (Integer) mGetRawW.invoke(display);
                point.y = (Integer) mGetRawH.invoke(display);
            }
            catch (Exception e) {
                //this may not be 100% accurate, but it's all we've got
                point.x = display.getWidth();
                point.y = display.getHeight();
            }
        }
    }

    // (c) https://stackoverflow.com/a/25129542/6120487
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // When the window loses focus (e.g. the action overflow is shown),
        // cancel any pending hide action. When the window gains focus,
        // hide the system UI.
        if (hasFocus) {
            delayedHide(300);
        } else {
            mHideHandler.removeMessages(0);
        }
    }

    private void hideSystemUI() {
        if(Build.VERSION.SDK_INT < 19)
            return;
        if(Build.VERSION.SDK_INT < 30) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        else {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if(controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    private final Handler mHideHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            hideSystemUI();
        }
    };

    public void delayedHide(int delayMillis) {
        if(Build.VERSION.SDK_INT >= 30)
            return;
        mHideHandler.removeMessages(0);
        mHideHandler.sendEmptyMessageDelayed(0, delayMillis);
    }
}
