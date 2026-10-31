package com.wittawat.cleanyourtv.preview;
import android.app.Instrumentation;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.widget.TextView;

public class LauncherUiTest extends Instrumentation {
    private NativePreviewActivity activity;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).setClassName(getTargetContext().getPackageName(), NativePreviewActivity.class.getName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = (NativePreviewActivity) startActivitySync(intent);
            waitForIdleSync();
            testReorderSaveCancelAndPause();
            result.putString("stream", "\nOK (1 test)\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable ex) {
            result.putString("stream", "\nFAILURES: " + android.util.Log.getStackTraceString(ex));
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private Instrumentation getInstrumentation() { return this; }
    private NativePreviewActivity getActivity() { return activity; }
    private void assertNotNull(Object value) { if(value == null) throw new AssertionError("Expected a visible view"); }
    private void assertEquals(Object expected,Object actual) {
        if(!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but was " + actual);
    }
    private void runTestOnUiThread(Runnable action) throws Throwable {
        final Throwable[] error = new Throwable[1];
        runOnMainSync(() -> { try { action.run(); } catch(Throwable ex) { error[0] = ex; } });
        if(error[0] != null) throw error[0];
    }
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
