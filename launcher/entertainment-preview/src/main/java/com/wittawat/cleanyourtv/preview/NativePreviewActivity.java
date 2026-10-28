package com.wittawat.cleanyourtv.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
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
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Native UI proof. Independent APK, never declares HOME, never writes device settings or disables packages.
 * No streaming API is used; artwork and episodes are fictional placeholders.
 */
public final class NativePreviewActivity extends Activity {
    private static final int BLACK = 0xFF07090F;
    private static final int WHITE = 0xFFF6F8FC;
    private static final int MUTED = 0xFFB6BDCC;
    private static final int HOLD_MS = 2000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, TextView> appCards = new LinkedHashMap<>();
    private LinkedHashMap<String, InstalledApps.Item> availableApps = new LinkedHashMap<>();
    private TextView allAppsButton;
    private AlertDialog allAppsDialog;
    private final BroadcastReceiver packageChanges = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            handler.removeCallbacks(updateApps);
            handler.postDelayed(updateApps, 350);
        }
    };
    private final Runnable updateApps = () -> refreshApps();


        private ArrayList<String> appOrder;
    private LinearLayout appStrip;
    private HorizontalScrollView appScroll;
    private TextView instructions, heroTitle, heroGenre, heroSummary;
    private HeroArt heroArt;
    private boolean moving, held;
    private String selectedId;
    private ArrayList<String> previousOrder;
    private Runnable holdTask;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        availableApps = InstalledApps.discover(this);
        appOrder = initialFavorites();
        render();
        IntentFilter changes = new IntentFilter();
        changes.addAction(Intent.ACTION_PACKAGE_ADDED);
        changes.addAction(Intent.ACTION_PACKAGE_REMOVED);
        changes.addAction(Intent.ACTION_PACKAGE_CHANGED);
        changes.addAction(Intent.ACTION_PACKAGE_REPLACED);
        changes.addDataScheme("package");
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(packageChanges, changes, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(packageChanges, changes);
        }
    }

    private int dp(float value) { return Math.round(getResources().getDisplayMetrics().density * value); }
    private ArrayList<String> initialFavorites() {
        String saved = getPreferences(MODE_PRIVATE).getString("favorite_packages_v1", null);
        if (saved != null) return FavoritesPolicy.visibleFromSaved(saved, availableApps.keySet());
        LinkedHashMap<String, String> labels = new LinkedHashMap<>();
        for (InstalledApps.Item item : availableApps.values()) labels.put(item.packageName, item.label);
        ArrayList<String> seeded = FavoritesPolicy.firstRun(labels);
        getPreferences(MODE_PRIVATE).edit().putString("favorite_packages_v1", FavoritesPolicy.serialize(seeded)).apply();
        return seeded;
    }

    private void saveFavorites() {
        getPreferences(MODE_PRIVATE).edit()
                .putString("favorite_packages_v1", FavoritesPolicy.serialize(appOrder)).apply();
    }

    private void refreshApps() {
        if (moving) finishMove(false);
        String currentFocus = null;
        for (Map.Entry<String, TextView> entry : appCards.entrySet()) {
            if (entry.getValue().isFocused()) { currentFocus = entry.getKey(); break; }
        }
        availableApps = InstalledApps.discover(this);
        appOrder = initialFavorites();
        if (appStrip != null) buildAppCards(currentFocus);
        if (allAppsDialog != null && allAppsDialog.isShowing()) {
            allAppsDialog.dismiss();
            showAllApps();
        }
    }

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
        body.setPadding(dp(38), dp(22), dp(38), dp(35));
        scroll.addView(body);
        background.addView(scroll);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("◈  CLEAN YOUR TV     /     HOME", 18, WHITE, true);
        toolbar.addView(brand, new LinearLayout.LayoutParams(0, dp(50), 1));
        toolbar.addView(toolbarButton("▣", "HDMI / Inputs", () -> new AlertDialog.Builder(this)
                .setTitle("HDMI / Inputs")
                .setMessage("Native Preview: ยังไม่สั่งสลับพอร์ตบนเครื่องจริง เมื่อทดสอบพอร์ต Hisense แล้วจึงจะเปิดใช้งาน")
                .setPositiveButton("ปิด", null).show()));
        allAppsButton = toolbarButton("▦", "All Apps", this::showAllApps);
        toolbar.addView(allAppsButton);
        toolbar.addView(toolbarButton("⌕", "Search", () -> information("ค้นหา", "กำลังเตรียมระบบค้นหาในเวอร์ชัน Native ถัดไป")));
        toolbar.addView(toolbarButton("⚙", "Settings", () -> {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (ActivityNotFoundException | SecurityException e) { information("Settings", "ไม่พบเมนูตั้งค่าบน Emulator นี้"); }
        }));
        TextView preview = text("ANDROID 11 · SAFE PREVIEW", 11, MUTED, false);
        preview.setPadding(dp(15), 0, 0, 0);
        toolbar.addView(preview);
        body.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(50)));

        FrameLayout hero = new FrameLayout(this);
        heroArt = new HeroArt();
        hero.addView(heroArt, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout heroLabels = new LinearLayout(this);
        heroLabels.setPadding(dp(32), dp(26), dp(32), dp(26));
        heroLabels.setOrientation(LinearLayout.VERTICAL);
        heroLabels.setGravity(Gravity.BOTTOM);
        TextView eyebrow = text("EXCLUSIVE     ·     CONTINUE WATCHING", 12, 0xFFDDE4F0, true);
        heroLabels.addView(eyebrow);
        heroTitle = text("ข้ามฟ้าสู่ดาว", 44, WHITE, true);
        heroLabels.addView(heroTitle);
        heroGenre = text("ซีรีส์ไซไฟ    ·    2026    ·    4K UHD    ·    EP 03", 14, 0xFFD8DCE6, false);
        heroLabels.addView(heroGenre);
        heroSummary = text("ภาพยนตร์และรายการสมมติสำหรับทดสอบระบบเลื่อนด้วยรีโมตบน Android TV", 13, MUTED, false);
        heroLabels.addView(heroSummary);
        hero.addView(heroLabels, new FrameLayout.LayoutParams(-1, -1));
        body.addView(hero, new LinearLayout.LayoutParams(-1, dp(320)));

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

        addContentRow(body, "ดูต่อจากที่ค้างไว้", new String[]{"ข้ามฟ้าสู่ดาว", "บทเพลงของฤดูร้อน", "คืนที่ดาวตก", "ทะเลแห่งความลับ"}, "CONTINUE", 0);
        addContentRow(body, "ภาพยนตร์และซีรีส์แนะนำ", new String[]{"คืนที่ดาวตก", "ทะเลแห่งความลับ", "บทเพลงของฤดูร้อน"}, "FEATURED", 1);
        addContentRow(body, "ตอนต่าง ๆ · ข้ามฟ้าสู่ดาว", new String[]{"EP 01 · จุดเริ่มต้น", "EP 02 · ประตูแห่งเวลา", "EP 03 · สัญญาณสุดท้าย", "EP 04 · แสงขอบฟ้า"}, "EPISODE", 2);

        TextView legal = text("DEMO CONTENT · ชื่อเรื่องและภาพเป็นข้อมูลสมมติ · ไม่มีการดึงรายการจาก Netflix/Prime/Disney+", 11, 0xFF8994A9, false);
        legal.setGravity(Gravity.CENTER);
        legal.setPadding(0, dp(15), 0, dp(15));
        body.addView(legal);
        setContentView(background);
        handler.post(() -> {
            if (!appOrder.isEmpty()) {
                TextView first = appCards.get(appOrder.get(0));
                if (first != null) { first.requestFocus(); return; }
            }
            if (allAppsButton != null) allAppsButton.requestFocus();
        });
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
        if (appOrder.isEmpty()) {
            TextView empty = text("＋ เลือกแอปโปรดจาก All Apps", 17, WHITE, true);
            empty.setGravity(Gravity.CENTER);
            empty.setFocusable(true);
            empty.setBackground(rounded(0xFF283346, 0, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(320), dp(92));
            lp.setMargins(dp(6), dp(3), dp(6), dp(3));
            appStrip.addView(empty, lp);
            empty.setOnClickListener(v -> showAllApps());
            empty.setOnFocusChangeListener((v, focused) -> styleFocus(empty, 0xFF283346, focused));
            return;
        }
        for (String pkg : appOrder) {
            InstalledApps.Item item = availableApps.get(pkg);
            if (item == null) continue;
            final int color = 0xFF213349 | (pkg.hashCode() & 0x001C1C1C);
            TextView card = text(item.label, 14, WHITE, true);
            card.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            card.setGravity(Gravity.CENTER);
            card.setFocusable(true);
            card.setClickable(true);
            card.setTag(pkg);
            card.setContentDescription(item.label);
            card.setBackground(rounded(color, 0, 12));
            Drawable icon = item.icon(getPackageManager());
            if (icon != null) {
                icon.setBounds(0, 0, dp(39), dp(39));
                card.setCompoundDrawables(null, icon, null, null);
                card.setCompoundDrawablePadding(dp(4));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(160), dp(91));
            lp.setMargins(dp(6), dp(3), dp(6), dp(3));
            appStrip.addView(card, lp);
            appCards.put(pkg, card);
            card.setOnFocusChangeListener((v, focused) -> {
                if (!focused) cancelHold();
                styleFocus(card, color, focused);
            });
            card.setOnClickListener(v -> {
                if (moving) finishMove(true);
                else launchApp(pkg);
            });
            card.setOnKeyListener((v, key, event) -> handleAppKey(pkg, key, event));
        }
        if (focusId != null) {
            TextView desired = appCards.get(focusId);
            if (desired != null) handler.post(desired::requestFocus);
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
                if(event.getAction()==KeyEvent.ACTION_UP) finishMove(ok);
                return true;
            }
            return true;
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
        instructions.setText("◀ ▶ ย้ายแอป    ·    OK บันทึก    ·    Back ยกเลิก");
        TextView card=appCards.get(id);
        if(card!=null)card.setBackground(rounded(0xFF253950,0xFF78D4FF,12));
    }
    private void finishMove(boolean save){
        if(!moving)return;
        String id=selectedId;
        moving=false;selectedId=null;
        if(save){
            saveFavorites();
        } else {
            appOrder=previousOrder;
        }
        instructions.setText("กดค้าง OK 2 วินาทีเพื่อจัดลำดับ");
        buildAppCards(id);
    }

    private void styleFocus(View view,int color,boolean focus) {
        view.setBackground(rounded(color, focus ? WHITE : 0, 12));
        view.animate().scaleX(focus?1.045f:1f).scaleY(focus?1.045f:1f)
                .setDuration(focus?165:120).start();
        view.setTranslationZ(focus?dp(10):0);
    }

    private void addContentRow(LinearLayout body,String heading,String[] names,String tag,int rowIndex){
        body.addView(sectionTitle(heading,"ทั้งหมด   ›"));
        HorizontalScrollView horizontal=new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        LinearLayout cards=new LinearLayout(this);
        cards.setPadding(dp(12),dp(10),dp(12),dp(15));
        cards.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=0;i<names.length;i++){
            final String title=names[i];
            final int palette=(rowIndex*2+i)%4;
            int[] colors={0xFF263D56,0xFF684435,0xFF333264,0xFF17565D};
            TextView cover=text(tag+"    ·    DEMO\n\n"+title+"\n"+(rowIndex==2?"45 นาที":"2026  ·  4K"),16,WHITE,true);
            cover.setGravity(Gravity.BOTTOM|Gravity.START);
            cover.setPadding(dp(16),dp(12),dp(16),dp(16));
            cover.setFocusable(true);cover.setClickable(true);
            cover.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{colors[palette],0xFF141922}));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(230),dp(137));
            lp.setMargins(dp(6),dp(4),dp(6),dp(4));
            cards.addView(cover,lp);
            cover.setOnFocusChangeListener((v,focused)->{
                styleFocus(cover,colors[palette],focused);
                if(focused){
                    heroTitle.setText(title);
                    heroGenre.setText(rowIndex==2?"ตอนจากซีรีส์ · 45 นาที":"DEMO · ภาพยนตร์และซีรีส์");
                    heroArt.setPalette(palette);
                }
            });
            cover.setOnClickListener(v->information(title,"รายการตัวอย่างเพื่อทดสอบ Navigation และ Episode เท่านั้น ไม่ได้เชื่อมต่อแอปสตรีมมิง"));
        }
        horizontal.addView(cards);
        body.addView(horizontal,new LinearLayout.LayoutParams(-1,dp(168)));
    }

    private void launchApp(String pkg) {
        InstalledApps.Item app = availableApps.get(pkg);
        if (app == null) {
            information("แอปไม่พร้อมใช้งาน", "ไม่พบแอปนี้ในรายการที่เปิดได้บนทีวี");
            refreshApps();
            return;
        }
        try {
            startActivity(app.launchIntent());
        } catch (ActivityNotFoundException | SecurityException ex) {
            information(app.label, "ไม่สามารถเปิดแอปนี้ได้ ตรวจสอบว่าแอปยังติดตั้งและเปิดใช้งานอยู่");
            refreshApps();
        }
    }

    private void pin(String pkg, boolean add) {
        appOrder = add ? FavoritesPolicy.add(appOrder, pkg, availableApps.keySet())
                : FavoritesPolicy.remove(appOrder, pkg);
        saveFavorites();
        buildAppCards(null);
    }

    private void showAllApps() {
        LinkedHashMap<String, InstalledApps.Item> latest = InstalledApps.discover(this);
        availableApps = latest;
        appOrder = initialFavorites();
        if (appStrip != null) buildAppCards(null);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(dp(14), dp(12), dp(14), dp(12));
        for (InstalledApps.Item item : latest.values()) {
            boolean pinned = appOrder.contains(item.packageName);
            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setFocusable(true);
            tile.setClickable(true);
            tile.setContentDescription(item.label + (pinned ? " แอปโปรด" : ""));
            tile.setBackground(rounded(0xFF243347,0,10));
            ImageView image = new ImageView(this);
            image.setContentDescription(null);
            Drawable icon = item.icon(getPackageManager());
            if (icon != null) image.setImageDrawable(icon);
            tile.addView(image, new LinearLayout.LayoutParams(dp(42), dp(42)));
            TextView label = text(item.label + (pinned ? "  ★" : ""), 13, WHITE, true);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(2);
            tile.addView(label);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(180);
            lp.height = dp(108);
            lp.setMargins(dp(6), dp(6), dp(6), dp(6));
            grid.addView(tile, lp);
            tile.setOnFocusChangeListener((v, focused) -> styleFocus(tile, 0xFF243347, focused));
            tile.setOnClickListener(v -> {
                boolean isPinned = appOrder.contains(item.packageName);
                String choice = isPinned ? "เอาออกจากแอปโปรด" : "เพิ่มในแอปโปรด";
                new AlertDialog.Builder(this).setTitle(item.label)
                        .setItems(new String[]{"เปิดแอป", choice}, (dialog, action) -> {
                            if (action == 0) {
                                if (allAppsDialog != null) allAppsDialog.dismiss();
                                launchApp(item.packageName);
                            } else {
                                pin(item.packageName, !isPinned);
                                if (allAppsDialog != null) allAppsDialog.dismiss();
                                showAllApps();
                            }
                        }).show();
            });
        }
        if (latest.isEmpty()) {
            TextView empty = text("ไม่พบแอปที่เปิดได้จาก Launcher ของ Android", 16, MUTED, false);
            empty.setPadding(dp(20), dp(20), dp(20), dp(20));
            grid.addView(empty);
        }
        scroll.addView(grid);
        allAppsDialog = new AlertDialog.Builder(this)
                .setTitle("All Apps  ·  " + latest.size() + " แอป")
                .setView(scroll)
                .setNegativeButton("ปิด", (dialog, which) -> { })
                .create();
        allAppsDialog.setOnDismissListener(dialog -> allAppsDialog = null);
        allAppsDialog.show();
        allAppsDialog.getWindow().setLayout(dp(860), dp(650));
    }

        private void information(String title,String message){
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("ตกลง",null).show();
    }
    @Override public void onBackPressed(){
        if(moving){finishMove(false);return;}
        super.onBackPressed(); // Return to original stock HOME. Never intercept HOME globally.
    }
    @Override protected void onResume() {
        super.onResume();
        if (appStrip != null) refreshApps();
    }
    @Override protected void onDestroy() {
        cancelHold();
        handler.removeCallbacksAndMessages(null);
        try { unregisterReceiver(packageChanges); } catch (IllegalArgumentException ignored) { }
        super.onDestroy();
    }

    private final class HeroArt extends View {
        private int palette;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        HeroArt(){super(NativePreviewActivity.this);}
        void setPalette(int index){palette=index;invalidate();}
        @Override protected void onDraw(Canvas canvas){
            int w=getWidth(),h=getHeight();
            int[][] colors={{0xFF0F182D,0xFF334A62},{0xFF1B151D,0xFF774D39},
                    {0xFF0C1533,0xFF54406B},{0xFF092F38,0xFF237481}};
            int[] cs=colors[palette%4];
            paint.setShader(new LinearGradient(0,0,w,h,cs[0],cs[1],Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,paint);paint.setShader(null);
            paint.setShader(new RadialGradient(w*.75f,h*.36f,h*.45f,
                    new int[]{0x99D2DCEA,0x444B789B,0x00202030},null,Shader.TileMode.CLAMP));
            canvas.drawCircle(w*.75f,h*.36f,h*.45f,paint);paint.setShader(null);
            Path mountain=new Path();mountain.moveTo(w*.40f,h);
            mountain.lineTo(w*.52f,h*.61f);mountain.lineTo(w*.59f,h*.80f);
            mountain.lineTo(w*.68f,h*.42f);mountain.lineTo(w*.81f,h*.85f);
            mountain.lineTo(w*.92f,h*.57f);mountain.lineTo(w,h*.76f);mountain.lineTo(w,h);mountain.close();
            paint.setColor(0xDD0A1421);canvas.drawPath(mountain,paint);
            paint.setShader(new LinearGradient(0,0,w,0,
                    new int[]{0xFF07090F,0xE707090F,0x0007090F},
                    new float[]{0f,.28f,.82f},Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,w,h,paint);paint.setShader(null);
        }
    }
}
