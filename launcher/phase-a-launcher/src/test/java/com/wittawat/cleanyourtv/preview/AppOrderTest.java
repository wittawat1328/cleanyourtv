package com.wittawat.cleanyourtv.preview;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class AppOrderTest {
    @Test public void customPinsAndRemovalSurviveReload() {
        ArrayList<String> order=AppOrder.fromSaved("settings,pkg:org.example.tv,youtube");
        assertTrue(AppOrder.moveOne(order,"pkg:org.example.tv",-1));
        assertEquals(Arrays.asList("pkg:org.example.tv","settings","youtube"),AppOrder.fromSaved(AppOrder.serialize(order)));
        assertFalse(order.contains("netflix"));
    }
    @Test public void removedAppsAndCorruptPinsCannotLeaveEmptyHome() {
        assertEquals(Arrays.asList("settings"),AppOrder.reconcile(AppOrder.fromSaved("pkg:org.removed.tv"),Collections.emptySet()));
        assertEquals(Arrays.asList("settings"),AppOrder.fromSaved("bad,pkg:../../foo,pkg:bad;cmd,"));
        assertEquals(Arrays.asList("settings","youtube"),AppOrder.fromSaved("settings,settings,youtube"));
    }
    @Test public void movingAtEdgesDoesNotWrapOrLoseApps() {
        ArrayList<String> order=AppOrder.fromSaved(null);
        assertFalse(AppOrder.moveOne(order,"youtube",-1));
        assertFalse(AppOrder.moveOne(order,"settings",1));
        assertFalse(AppOrder.moveOne(order,"unknown",1));
        assertEquals(AppOrder.DEFAULT,order);
    }
}
