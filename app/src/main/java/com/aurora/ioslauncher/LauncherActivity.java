package com.aurora.ioslauncher;

import android.app.Activity;
import android.app.WallpaperManager;
import android.media.AudioManager;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LauncherActivity extends Activity {
    private WebView webView;
    private CameraManager cameraManager;
    private String flashCameraId;
    private boolean flashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF101324);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep the launcher self-contained. Web content cannot navigate to external URLs.
                return !"file".equalsIgnoreCase(request.getUrl().getScheme());
            }
        });
        webView.addJavascriptInterface(new NativeBridge(), "Native");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        try {
            cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (cameraManager != null) {
                for (String id : cameraManager.getCameraIdList()) {
                    android.hardware.camera2.CameraCharacteristics c = cameraManager.getCameraCharacteristics(id);
                    Boolean hasFlash = c.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer facing = c.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                    if (Boolean.TRUE.equals(hasFlash) && facing != null && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                        flashCameraId = id;
                        break;
                    }
                }
            }
        } catch (Exception ignored) { }
    }

    private void configureSystemBars() {
        Window w = getWindow();
        w.setStatusBarColor(0xFF101324);
        w.setNavigationBarColor(0xFF101324);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attrs = w.getAttributes();
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            w.setAttributes(attrs);
        }
        int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        w.getDecorView().setSystemUiVisibility(flags);
    }

    private class NativeBridge {
        @JavascriptInterface
        public String getApps() {
            JSONArray list = new JSONArray();
            try {
                Intent query = new Intent(Intent.ACTION_MAIN, null);
                query.addCategory(Intent.CATEGORY_LAUNCHER);
                PackageManager pm = getPackageManager();
                List<ResolveInfo> apps = pm.queryIntentActivities(query, PackageManager.MATCH_ALL);
                Collections.sort(apps, new Comparator<ResolveInfo>() {
                    @Override public int compare(ResolveInfo a, ResolveInfo b) {
                        return String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm)));
                    }
                });
                for (ResolveInfo info : apps) {
                    if (info.activityInfo == null || getPackageName().equals(info.activityInfo.packageName)) continue;
                    JSONObject item = new JSONObject();
                    String name = String.valueOf(info.loadLabel(pm));
                    item.put("name", name);
                    item.put("packageName", info.activityInfo.packageName);
                    item.put("icon", drawableToDataUri(info.loadIcon(pm)));
                    list.put(item);
                }
            } catch (Exception ignored) { }
            return list.toString();
        }

        @JavascriptInterface
        public void launchApp(String packageName) {
            if (packageName == null || packageName.trim().isEmpty()) return;
            runOnUiThread(() -> {
                try {
                    Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launch == null) {
                        Toast.makeText(LauncherActivity.this, "This app has no launch screen", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launch);
                } catch (Exception e) {
                    Toast.makeText(LauncherActivity.this, "Could not open app", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public String setWallpaper(String base64Image, String target) {
            try {
                byte[] decoded = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                if (bitmap == null) return "ERROR";
                WallpaperManager manager = WallpaperManager.getInstance(LauncherActivity.this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                    if ("both".equalsIgnoreCase(target)) {
                        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                    }
                } else {
                    manager.setBitmap(bitmap);
                }
                bitmap.recycle();
                return "OK";
            } catch (Exception e) {
                return "ERROR";
            }
        }

        @JavascriptInterface
        public int getBatteryLevel() {
            try {
                Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (battery == null) return -1;
                int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level < 0 || scale <= 0) return -1;
                return Math.round(level * 100f / scale);
            } catch (Exception e) { return -1; }
        }

        @JavascriptInterface
        public void openSettings(String panel) {
            runOnUiThread(() -> {
                Intent i;
                try {
                    if ("wifi".equalsIgnoreCase(panel) && Build.VERSION.SDK_INT >= 29) {
                        i = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
                    } else if ("bluetooth".equalsIgnoreCase(panel) && Build.VERSION.SDK_INT >= 29) {
                        i = new Intent(Settings.Panel.ACTION_BLUETOOTH);
                    } else if ("airplane".equalsIgnoreCase(panel)) {
                        i = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                    } else if ("display".equalsIgnoreCase(panel)) {
                        i = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                    } else {
                        i = new Intent(Settings.ACTION_SETTINGS);
                    }
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception ignored) { }
                } catch (Exception ignored) { }
            });
        }

        @JavascriptInterface
        public void requestHomeRole() {
            runOnUiThread(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        RoleManager roleManager = getSystemService(RoleManager.class);
                        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                            if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                                Toast.makeText(LauncherActivity.this, "Aurora is already the default Home app", Toast.LENGTH_SHORT).show();
                            } else {
                                startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), 9001);
                            }
                        } else {
                            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                        }
                    } else {
                        startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
                    }
                } catch (Exception e) {
                    try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception ignored) { }
                }
            });
        }

        @JavascriptInterface
        public void setBrightness(int percent) {
            int bounded = Math.max(5, Math.min(100, percent));
            runOnUiThread(() -> {
                try {
                    WindowManager.LayoutParams params = getWindow().getAttributes();
                    params.screenBrightness = bounded / 100f;
                    getWindow().setAttributes(params);
                } catch (Exception ignored) { }
            });
        }

        @JavascriptInterface
        public void setMediaVolume(int percent) {
            int bounded = Math.max(0, Math.min(100, percent));
            try {
                AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
                if (audio != null) {
                    int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int value = Math.round(max * bounded / 100f);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0);
                }
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public String toggleFlashlight(boolean enabled) {
            if (cameraManager == null || flashCameraId == null) return "ERROR";
            try {
                cameraManager.setTorchMode(flashCameraId, enabled);
                flashOn = enabled;
                return "OK";
            } catch (Exception e) {
                flashOn = false;
                return "ERROR";
            }
        }

        private String drawableToDataUri(Drawable drawable) {
            try {
                int width = 96, height = 96;
                Bitmap bitmap;
                if (drawable instanceof BitmapDrawable && ((BitmapDrawable) drawable).getBitmap() != null) {
                    Bitmap original = ((BitmapDrawable) drawable).getBitmap();
                    bitmap = Bitmap.createScaledBitmap(original, width, height, true);
                } else {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    drawable.setBounds(0, 0, width, height);
                    drawable.draw(canvas);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                bitmap.recycle();
                return "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            } catch (Exception e) { return ""; }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript("(()=>{const cc=document.getElementById('controlCenter');const dr=document.getElementById('appDrawer');if(cc&&cc.classList.contains('active')){cc.classList.remove('active');return 'handled'}if(dr&&dr.classList.contains('active')){dr.classList.remove('active');return 'handled'}const st=document.getElementById('studioScreen');const se=document.getElementById('settingsScreen');if(st&&st.classList.contains('active')){document.getElementById('studioBack').click();return 'handled'}if(se&&se.classList.contains('active')){document.getElementById('settingsBack').click();return 'handled'}return 'home'})()", null);
        }
    }
}
