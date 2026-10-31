package com.wittawat.cleanyourtv.preview;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class LauncherUiTest extends ActivityInstrumentationTestCase2<NativePreviewActivity> {
    public LauncherUiTest() { super(NativePreviewActivity.class); }
    private TextView find(View v, String label) {
        if (v instanceof TextView && label.contentEquals(((TextView)v).getText())) return (TextView)v;
        if (v instanceof ViewGroup) for (int i=0;i<((ViewGroup)v).getChildCount();i++) {
            TextView found=find(((ViewGroup)v).getChildAt(i),label);
            if(found!=null)return found;
        }
        return null;
    }
    private void key(int code) { getInstrumentation().sendKeyDownUpSync(code); getInstrumentation().waitForIdleSync(); }
    private void focusFirst() throws Throwable {
        runTestOnUiThread(() -> {
            TextView card=find(getActivity().getWindow().getDecorView(),"▶\nYouTube");
            assertNotNull(card); card.requestFocus();
        });
        getInstrumentation().waitForIdleSync();
    }
    private void hold() throws Exception {
        long now=android.os.SystemClock.uptimeMillis();
        getInstrumentation().sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0));
        Thread.sleep(2200);
        getInstrumentation().sendKeySync(new KeyEvent(now,android.os.SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0));
        getInstrumentation().waitForIdleSync();
    }
    public void testReorderSaveCancelAndPause() throws Throwable {
        getActivity();
        focusFirst(); hold();
        runTestOnUiThread(() -> assertNotNull(find(getActivity().getWindow().getDecorView(),"◀ ▶ ย้ายแอป    ·    OK บันทึก    ·    Back ยกเลิก")));
        key(KeyEvent.KEYCODE_DPAD_RIGHT); key(KeyEvent.KEYCODE_DPAD_CENTER);
        assertEquals("netflix,youtube,browser,prime,disney,settings", getActivity().getPreferences(0).getString("app_order",null));
        focusFirst(); hold(); key(KeyEvent.KEYCODE_DPAD_RIGHT); key(KeyEvent.KEYCODE_BACK);
        assertEquals("netflix,youtube,browser,prime,disney,settings", getActivity().getPreferences(0).getString("app_order",null));
        focusFirst(); hold(); key(KeyEvent.KEYCODE_DPAD_LEFT);
        runTestOnUiThread(() -> {
            getInstrumentation().callActivityOnPause(getActivity());
            getInstrumentation().callActivityOnResume(getActivity());
        });
        getInstrumentation().waitForIdleSync();
        assertEquals("netflix,youtube,browser,prime,disney,settings", getActivity().getPreferences(0).getString("app_order",null));
    }
}
