package com.wittawat.cleanyourtv.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.ComponentName;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvContract;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase A HOME candidate. Default selection is performed by Android settings only.
 * Offline native UI; no streaming API, boot receiver or key interception service.
 */
public final class NativePreviewActivity extends Activity {
    private static final int BLACK = 0xFF07090F;
    private static final int WHITE = 0xFFF6F8FC;
    private static final int MUTED = 0xFFB6BDCC;
    private static final int HOLD_MS = 2000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, TextView> appCards = new LinkedHashMap<>();
    private final String[] appNames = {"YouTube", "Netflix", "Browser", "Prime Video", "Disney+", "Settings"};
    private final String[] appSymbols = {"▶", "N", "◎", "prime", "Disney+", "⚙"};
    private final String[] appPackages = {
            "com.google.android.youtube.tv", "com.netflix.ninja", "",
            "com.amazon.amazonvideo.livingroom", "com.disney.disneyplus", ""
    };
    private final int[] appColors = {0xFF9C1023, 0xFF14151B, 0xFF194A76, 0xFF135B91, 0xFF20356E, 0xFF3A4150};
    private ArrayList<String> appOrder;
    private LinearLayout appStrip;
    private HorizontalScrollView appScroll;
    private TextView instructions, heroTitle, heroGenre, heroSummary;
    private HeroArt heroArt;
    private boolean moving, held, ignoreHeldOkRelease;
    private String selectedId;
    private ArrayList<String> previousOrder;
    private Runnable holdTask;
    private String lastFocusId;
    private final Map<String, ResolveInfo> installedApps = new LinkedHashMap<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        appOrder = AppOrder.fromSaved(getPreferences(MODE_PRIVATE).getString("app_order", null));
        refreshApps();
        render();
    }

    private int dp(float value) { return Math.round(getResources().getDisplayMetrics().density * value); }
    private int indexOf(String id) { return AppOrder.DEFAULT.indexOf(id); }

    private GradientDrawable rounded(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(2), stroke);
        return d;
    }

    private TextView text(String label, float sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private void render() {
        FrameLayout background = new FrameLayout(this);
        background.setBackgroundColor(BLACK);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24), dp(14), dp(24), dp(14));
        scroll.addView(body);
        background.addView(scroll);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("◈  CLEAN YOUR TV     /     HOME", 18, WHITE, true);
        toolbar.addView(brand, new LinearLayout.LayoutParams(0, dp(50), 1));
        toolbar.addView(toolbarButton("▦", "All Apps", () -> showAppChooser(false)));
        toolbar.addView(toolbarButton("▣", "HDMI / Inputs", this::showInputs));
        toolbar.addView(toolbarButton("⌂", "Home / Recovery", this::showHomeMenu));
        toolbar.addView(toolbarButton("⚙", "Settings", () -> {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException | SecurityException e) { information("Settings", "ไม่พบเมนูตั้งค่าบน Emulator นี้"); }
        }));
        TextView preview = text("PHASE A · LAUNCHER", 11, MUTED, false);
        preview.setPadding(dp(15), 0, 0, 0);
        toolbar.addView(preview);
        body.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(50)));

        FrameLayout hero = new FrameLayout(this);
        heroArt = new HeroArt();
        hero.addView(heroArt, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout heroLabels = new LinearLayout(this);
        heroLabels.setPadding(dp(24), dp(16), dp(24), dp(16));
        heroLabels.setOrientation(LinearLayout.VERTICAL);
        heroLabels.setGravity(Gravity.BOTTOM);
        TextView eyebrow = text("YOUR APPS     ·     YOUR HOME", 12, 0xFFDDE4F0, true);
        heroLabels.addView(eyebrow);
        heroTitle = text("ยินดีต้อนรับกลับ", 32, WHITE, true);
        heroLabels.addView(heroTitle);
        heroGenre = text("แอปโปรด · HDMI · ตั้งค่าทีวี", 14, 0xFFD8DCE6, false);
        heroLabels.addView(heroGenre);
        heroSummary = text("เลือกแอปเพื่อเปิด · All Apps เพื่อปักหมุด · กดค้าง OK เพื่อจัดลำดับ", 13, MUTED, false);
        heroLabels.addView(heroSummary);
        hero.addView(heroLabels, new FrameLayout.LayoutParams(-1, -1));
        float screenDp = getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density;
        body.addView(hero, new LinearLayout.LayoutParams(-1, dp(Math.max(125, Math.min(240, screenDp - 400)))));

        LinearLayout appTitle = sectionTitle("แอปของคุณ", "กดค้าง OK 2 วินาทีเพื่อจัดลำดับ");
        instructions = (TextView) appTitle.getChildAt(1);
        body.addView(appTitle);
        appScroll = new HorizontalScrollView(this);
        appScroll.setHorizontalScrollBarEnabled(false);
        appScroll.setClipToPadding(false);
        appStrip = new LinearLayout(this);
        appStrip.setPadding(dp(12), dp(14), dp(12), dp(14));
        appStrip.setOrientation(LinearLayout.HORIZONTAL);
        appScroll.addView(appStrip);
        body.addView(appScroll, new LinearLayout.LayoutParams(-1, dp(127)));
        buildAppCards(null);

        TextView help = text("All Apps: ปักหมุดแอป     ·     ⌂: ตั้งค่า Home / ย้อนกลับ", 13, MUTED, false);
        help.setPadding(dp(8), dp(8), dp(8), dp(8));
        body.addView(help);
        setContentView(background);
        handler.post(() -> { TextView first = appCards.get(appOrder.get(0)); if (first != null) first.requestFocus(); });
    }

    private TextView toolbarButton(String symbol, String label, Runnable action) {
        TextView button = text(symbol, 24, WHITE, true);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(label);
        button.setFocusable(true);
        button.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
        lp.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(lp);
        button.setBackground(rounded(0xFF252A36, 0, 20));
        button.setOnClickListener(v -> action.run());
        button.setOnFocusChangeListener((v, focused) -> styleFocus(button, 0xFF252A36, focused));
        return button;
    }

    private LinearLayout sectionTitle(String title, String caption) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(4), dp(8), dp(4), dp(4));
        TextView heading = text(title, 20, WHITE, true);
        bar.addView(heading, new LinearLayout.LayoutParams(0, dp(38), 1));
        TextView hint = text(caption, 12, MUTED, false);
        bar.addView(hint);
        return bar;
    }

    private void buildAppCards(String focusId) {
        appStrip.removeAllViews();
        appCards.clear();
        for (String id : appOrder) {
            int idx=indexOf(id);
            final int color = idx >= 0 ? appColors[idx] : 0xFF253D58;
            ResolveInfo info = installedApps.get(id);
            final String label = idx >= 0 ? appNames[idx] : info == null ? id : info.loadLabel(getPackageManager()).toString();
            TextView card = text((idx >= 0 ? appSymbols[idx] : "▦") + "\n" + label, idx==3||idx==4?15:19, WHITE, true);
            card.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            card.setGravity(Gravity.CENTER);
            card.setFocusable(true);
            card.setClickable(true);
            card.setTag(id);
            card.setBackground(rounded(color, 0, 12));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(160), dp(91));
            p.setMargins(dp(6), dp(3), dp(6), dp(3));
            appStrip.addView(card, p);
            appCards.put(id,card);
            card.setOnFocusChangeListener((v,focused)->{
                if(!focused) cancelHold();
                styleFocus(card, color, focused);
                if (focused) lastFocusId = id;
            });
            card.setOnClickListener(v -> {
                if(moving) finishMove(true);
                else launchApp(id);
            });
            card.setOnKeyListener((v,key,event)-> handleAppKey(id,key,event));
        }
        if(focusId!=null) {
            TextView card=appCards.get(focusId);
            if(card!=null) handler.post(card::requestFocus);
        }
    }

    private boolean handleAppKey(String id,int key,KeyEvent event) {
        boolean ok=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER;
        boolean back=key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_ESCAPE;
        if(moving){
            if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){
                if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0) {
                    AppOrder.moveOne(appOrder,selectedId,key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);
                    buildAppCards(selectedId);
                }
                return true;
            }
            if(back||ok) {
                // The first OK-UP finishes the original two-second hold, not the move.
                // Otherwise releasing that held button would instantly save and exit move mode.
                if(ok && event.getAction()==KeyEvent.ACTION_UP && ignoreHeldOkRelease) {
                    ignoreHeldOkRelease=false;
                    return true;
                }
                if(event.getAction()==KeyEvent.ACTION_UP) finishMove(ok);
                return true;
            }
            // Never swallow Volume, Home, Power or vendor shortcuts. Only D-pad
            // navigation is reserved for moving a tile; other keys go to the OS.
            if(key==KeyEvent.KEYCODE_DPAD_UP || key==KeyEvent.KEYCODE_DPAD_DOWN) return true;
            return false;
        }
        if(!ok)return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN){
            if(event.getRepeatCount()==0){
                held=false;
                cancelHold();
                holdTask=()->{held=true;startMove(id);};
                handler.postDelayed(holdTask,HOLD_MS);
            }
            return true;
        }
        if(event.getAction()==KeyEvent.ACTION_UP){
            cancelHold();
            if(!held&&!event.isCanceled())launchApp(id);
            held=false;
            return true;
        }
        return true;
    }

    private void cancelHold(){if(holdTask!=null){handler.removeCallbacks(holdTask);holdTask=null;}}
    private void startMove(String id){
        moving=true;selectedId=id;previousOrder=new ArrayList<>(appOrder);
        ignoreHeldOkRelease=true;
        instructions.setText("◀ ▶ ย้ายแอป    ·    OK บันทึก    ·    Back ยกเลิก");
        TextView card=appCards.get(id);
        if(card!=null)card.setBackground(rounded(indexOf(id) >= 0 ? appColors[indexOf(id)] : 0xFF253D58,0xFF78D4FF,12));
    }
    private void finishMove(boolean save){
        if(!moving)return;
        String id=selectedId;
        moving=false;selectedId=null;ignoreHeldOkRelease=false;
        if(save){
            getPreferences(MODE_PRIVATE).edit().putString("app_order",AppOrder.serialize(appOrder)).apply();
        } else {
            appOrder=previousOrder;
        }
        instructions.setText("กดค้าง OK 2 วินาทีเพื่อจัดลำดับ");
        buildAppCards(id);
    }

    private void styleFocus(TextView view,int color,boolean focus) {
        view.setBackground(rounded(color, focus ? WHITE : 0, 12));
        view.animate().scaleX(focus?1.045f:1f).scaleY(focus?1.045f:1f)
                .setDuration(focus?165:120).start();
        view.setTranslationZ(focus?dp(10):0);
    }

    private void launchApp(String id){
        if (id.startsWith("pkg:")) {
            if (!launchPackage(id.substring(4), id)) information("แอป", "ไม่พบแอปนี้แล้ว");
            return;
        }
        if("browser".equals(id)){
            String saved=getPreferences(MODE_PRIVATE).getString("browser_package", "");
            if(saved!=null && !saved.isEmpty()) {
                if(launchPackage(saved, "Browser")) return;
                getPreferences(MODE_PRIVATE).edit().remove("browser_package").apply();
            }
            showAppChooser(true);
            return;
        }
        if("settings".equals(id)){
            try{startActivity(new Intent(Settings.ACTION_SETTINGS));}
            catch(ActivityNotFoundException|SecurityException e){information("Settings","ไม่พบหน้าตั้งค่าบนอุปกรณ์นี้");}
            return;
        }
        int idx=indexOf(id);
        if(idx>=0 && !appPackages[idx].isEmpty() && launchPackage(appPackages[idx],appNames[idx])) return;
        information(idx>=0?appNames[idx]:"แอป", "ไม่พบแอปที่ติดตั้งหรือไม่อนุญาตให้เปิดจากอุปกรณ์นี้");
    }

    // Open only explicitly selected installed apps; never change system package state.
    private boolean launchPackage(String pkg,String label){
        try{
            Intent launch=getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if(launch==null)launch=getPackageManager().getLaunchIntentForPackage(pkg);
            if(launch==null)return false;
            startActivity(launch);
            return true;
        }catch(ActivityNotFoundException | SecurityException ignored){
            return false;
        }
    }

    // Android 11 requires manifest package visibility for both LAUNCHER and LEANBACK_LAUNCHER.
    // The OS supplies only enabled, exported launch activities. System components are not modified.
    private List<ResolveInfo> discoverApps(){
        PackageManager pm=getPackageManager();
        LinkedHashMap<String,ResolveInfo> discovered=new LinkedHashMap<>();
        String[] categories={Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER};
        for(String category:categories){
            Intent query=new Intent(Intent.ACTION_MAIN).addCategory(category);
            for(ResolveInfo info:pm.queryIntentActivities(query,0)){
                if(info.activityInfo==null || !info.activityInfo.exported || !info.activityInfo.enabled)continue;
                String pkg=info.activityInfo.packageName;
                if(getPackageName().equals(pkg))continue;
                String lower = pkg.toLowerCase(java.util.Locale.ROOT);
                if (lower.contains("factory") || lower.contains("hkcfac") || lower.contains("engineering") || lower.contains("engmode")) continue;
                if(!discovered.containsKey(pkg))discovered.put(pkg,info);
            }
        }
        ArrayList<ResolveInfo> result=new ArrayList<>(discovered.values());
        Collections.sort(result, Comparator.comparing(
            item -> item.loadLabel(pm).toString(),String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private void showAppChooser(boolean selectBrowser){
        PackageManager pm=getPackageManager();
        List<ResolveInfo> apps=discoverApps();
        if(selectBrowser){
            // Candidate list is a suggestion only: do not assign an unknown browser package automatically.
            ArrayList<ResolveInfo> browsers=new ArrayList<>();
            for(ResolveInfo app:apps){
                String label=app.loadLabel(pm).toString().toLowerCase(java.util.Locale.ROOT);
                String pkg=app.activityInfo.packageName.toLowerCase(java.util.Locale.ROOT);
                if(label.contains("browser") || label.contains("internet") || label.contains("web")
                        || pkg.contains("browser") || pkg.contains("tvbro") || pkg.contains("chrome")
                        || pkg.contains("firefox") || pkg.contains("puffin"))browsers.add(app);
            }
            if(!browsers.isEmpty())apps=browsers;
        }
        if(apps.isEmpty()){
            information(selectBrowser?"Browser":"All Apps","ยังไม่พบแอปที่เปิดใช้งานได้บนอุปกรณ์นี้");
            return;
        }
        final List<ResolveInfo> choices=apps;
        String[] labels=new String[choices.size()];
        for(int i=0;i<choices.size();i++)labels[i]=choices.get(i).loadLabel(pm).toString();
        new AlertDialog.Builder(this)
          .setTitle(selectBrowser?"เลือก Browser ที่ติดตั้งไว้":"All Apps · แอปที่เปิดใช้งานได้")
          .setItems(labels,(dialog,position)->{
              ResolveInfo selected=choices.get(position);
              String pkg=selected.activityInfo.packageName;
              if (selectBrowser) {
                  getPreferences(MODE_PRIVATE).edit().putString("browser_package",pkg).apply();
                  if (!launchPackage(pkg, labels[position])) information(labels[position], "ไม่สามารถเปิดแอปนี้ได้");
              } else {
                  String id = favoriteId(pkg);
                  boolean pinned = appOrder.contains(id);
                  new AlertDialog.Builder(this).setTitle(labels[position])
                    .setItems(new String[]{"เปิดแอป", pinned ? "นำออกจากแอปโปรด" : "ปักหมุดในหน้า Home"}, (d, choice) -> {
                        if (choice == 0) {
                            if (!launchPackage(pkg, labels[position])) information(labels[position], "ไม่สามารถเปิดแอปนี้ได้");
                        } else {
                            if (pinned) appOrder.remove(id); else appOrder.add(id);
                            if (appOrder.isEmpty()) appOrder.add("settings");
                            getPreferences(MODE_PRIVATE).edit().putString("app_order", AppOrder.serialize(appOrder)).apply();
                            buildAppCards(appOrder.get(0));
                        }
                    }).setNegativeButton("ยกเลิก", null).show();
              }
          })
          .setNegativeButton("ยกเลิก",null).show();
    }

    private void information(String title,String message){
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("ตกลง",null).show();
    }
    private String favoriteId(String pkg) {
        for (int i = 0; i < appPackages.length; i++) {
            if (!appPackages[i].isEmpty() && appPackages[i].equals(pkg)) return AppOrder.DEFAULT.get(i);
        }
        return "pkg:" + pkg;
    }

    private void refreshApps() {
        installedApps.clear();
        for (ResolveInfo info : discoverApps()) installedApps.put("pkg:" + info.activityInfo.packageName, info);
        appOrder = AppOrder.reconcile(appOrder, installedApps.keySet());
    }

    private void showInputs() {
        TvInputManager manager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        ArrayList<TvInputInfo> inputs = new ArrayList<>();
        try {
            if (manager != null) for (TvInputInfo input : manager.getTvInputList()) {
                if (input.getType() == TvInputInfo.TYPE_HDMI && input.isPassthroughInput() && !input.isHidden(this)) inputs.add(input);
            }
        } catch (SecurityException ignored) { /* Firmware may restrict input discovery. */ }
        if (inputs.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("HDMI / Inputs")
                .setMessage("ไม่พบพอร์ต HDMI ที่ระบบอนุญาตให้เปิด ใช้ปุ่ม Input บนรีโมตหรือเมนูตั้งค่าทีวี")
                .setPositiveButton("ตั้งค่าทีวี", (d, w) -> openSettings(Settings.ACTION_SETTINGS))
                .setNegativeButton("ปิด", null).show();
            return;
        }
        String[] labels = new String[inputs.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = inputs.get(i).loadLabel(this).toString();
        new AlertDialog.Builder(this).setTitle("เลือก HDMI").setItems(labels, (d, which) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputs.get(which).getId())));
            } catch (ActivityNotFoundException | SecurityException ex) {
                information("HDMI", "เฟิร์มแวร์ไม่อนุญาตให้เปิดพอร์ตนี้ ใช้ปุ่ม Input บนรีโมต");
            }
        }).setNegativeButton("ยกเลิก", null).show();
    }

    private void openSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (ActivityNotFoundException | SecurityException ex) {
            if (!Settings.ACTION_SETTINGS.equals(action)) openSettings(Settings.ACTION_SETTINGS);
            else information("Settings", "โปรดใช้ปุ่มตั้งค่าบนรีโมต");
        }
    }

    private void showHomeMenu() {
        new AlertDialog.Builder(this).setTitle("Home / Recovery")
            .setItems(new String[]{"เลือกแอป Home เริ่มต้น", "เปิด Home เดิม", "วิธีย้อนกลับ", "ตรวจสถานะ Home"}, (d, which) -> {
                if (which == 0) openSettings(Settings.ACTION_HOME_SETTINGS);
                else if (which == 1) openOtherHome();
                else if (which == 3) showHomeStatus();
                else information("ย้อนกลับสู่ Home เดิม", "ไปที่ Settings > Apps > Default apps > Home app แล้วเลือก Google TV Home เดิม หากเมนูต่างกันให้ใช้คู่มือ Recovery ที่จัดเตรียมไว้ การเปิด Home เดิมชั่วคราวไม่ได้เปลี่ยน Default Home");
            }).setNegativeButton("ปิด", null).show();
    }

    private void showHomeStatus() {
        ResolveInfo resolved = getPackageManager().resolveActivity(
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY);
        if (resolved == null || resolved.activityInfo == null) {
            information("สถานะ Home", "ระบบยังไม่ระบุ Home ที่เปิดได้ โปรดตรวจจากเมนูตั้งค่าทีวี"); return;
        }
        boolean ours = getPackageName().equals(resolved.activityInfo.packageName);
        information("สถานะ Home", ours
            ? "ระบบชี้ Home มาที่ CleanYourTV Launcher แล้ว โปรดทดสอบปุ่ม Home จริงและการปลุกทีวีก่อนใช้งานต่อเนื่อง"
            : "ขณะนี้ระบบยังชี้ Home ไปที่ " + resolved.loadLabel(getPackageManager())
                + " หากเลือก CleanYourTV แล้วแต่ปุ่ม Home ยังกลับตัวเดิม แสดงว่าเฟิร์มแวร์ยังให้ Home เดิมทำงาน ให้กลับไปใช้ Home เดิมและตรวจสอบก่อน");
    }

    private void openOtherHome() {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ArrayList<ResolveInfo> homes = new ArrayList<>();
        for (ResolveInfo home : getPackageManager().queryIntentActivities(query, 0)) {
            if (home.activityInfo != null && home.activityInfo.exported && home.activityInfo.enabled
                    && !getPackageName().equals(home.activityInfo.packageName)
                    && !"com.android.internal.app.ResolverActivity".equals(home.activityInfo.name)) homes.add(home);
        }
        if (homes.isEmpty()) { information("Home เดิม", "ไม่พบ Home อื่น ให้เปิดตั้งค่าจากรีโมต"); return; }
        String[] labels = new String[homes.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = homes.get(i).loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this).setTitle("เปิด Home เดิม (ชั่วคราว)").setItems(labels, (d, which) -> {
            ResolveInfo home = homes.get(which);
            try {
                startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setComponent(new ComponentName(home.activityInfo.packageName, home.activityInfo.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (ActivityNotFoundException | SecurityException ex) { openSettings(Settings.ACTION_HOME_SETTINGS); }
        }).setNegativeButton("ยกเลิก", null).show();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshApps();
        if (appStrip != null) buildAppCards(lastFocusId != null && appOrder.contains(lastFocusId) ? lastFocusId : appOrder.get(0));
    }
    @Override protected void onPause() {
        cancelHold();
        held = false;
        if (moving) finishMove(false);
        super.onPause();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        cancelHold();
        held = false;
        if (moving) finishMove(false);
        TextView first = appCards.get(appOrder.get(0));
        if (first != null) first.requestFocus();
    }
    @Override public void onBackPressed() {
        if (moving) { finishMove(false); return; }
        TextView first = appCards.get(appOrder.get(0));
        if (first != null) first.requestFocus();
    }
    @Override protected void onDestroy(){cancelHold();handler.removeCallbacksAndMessages(null);super.onDestroy();}

    private final class HeroArt extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[][] palettes={
            {0xFF0F182D,0xFF334A62},{0xFF1B151D,0xFF774D39},
            {0xFF0C1533,0xFF54406B},{0xFF092F38,0xFF237481}
        };
        private final Path mountains=new Path();
        private final int[] glowColors={0x99D2DCEA,0x444B789B,0x00202030};
        private final int[] fadeColors={0xFF07090F,0xE707090F,0x0007090F};
        private final float[] fadePositions={0f,.28f,.82f};
        private Shader baseShader,glowShader,fadeShader;
        private int palette;
        HeroArt(){super(NativePreviewActivity.this);}
        void setPalette(int index){
            if(palette==index)return;
            palette=index;rebuildShaders();invalidate();
        }
        @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){
            super.onSizeChanged(w,h,oldW,oldH);
            rebuildShaders();
        }
        private void rebuildShaders(){
            int w=getWidth(),h=getHeight();
            if(w<=0||h<=0)return;
            int[] colors=palettes[palette%palettes.length];
            baseShader=new LinearGradient(0,0,w,h,colors[0],colors[1],Shader.TileMode.CLAMP);
            glowShader=new RadialGradient(w*.75f,h*.36f,h*.45f,glowColors,null,Shader.TileMode.CLAMP);
            fadeShader=new LinearGradient(0,0,w,0,fadeColors,fadePositions,Shader.TileMode.CLAMP);
            mountains.reset();
            mountains.moveTo(w*.40f,h);
            mountains.lineTo(w*.52f,h*.61f);
            mountains.lineTo(w*.59f,h*.80f);
            mountains.lineTo(w*.68f,h*.42f);
            mountains.lineTo(w*.81f,h*.85f);
            mountains.lineTo(w*.92f,h*.57f);
            mountains.lineTo(w,h*.76f);
            mountains.lineTo(w,h);
            mountains.close();
        }
        @Override protected void onDraw(Canvas canvas){
            int w=getWidth(),h=getHeight();
            if(baseShader==null)return;
            paint.setShader(baseShader);
            canvas.drawRect(0,0,w,h,paint);
            paint.setShader(glowShader);
            canvas.drawCircle(w*.75f,h*.36f,h*.45f,paint);
            paint.setShader(null);
            paint.setColor(0xDD0A1421);
            canvas.drawPath(mountains,paint);
            paint.setShader(fadeShader);
            canvas.drawRect(0,0,w,h,paint);
            paint.setShader(null);
        }
    }
}
