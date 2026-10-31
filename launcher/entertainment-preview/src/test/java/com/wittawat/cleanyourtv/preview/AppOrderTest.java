package com.wittawat.cleanyourtv.preview;

import org.junit.Test;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class AppOrderTest {
    @Test public void browserStartsThird() {
        assertEquals("browser", AppOrder.fromSaved(null).get(2));
    }
    @Test public void invalidSavedOrderFailsClosed() {
        assertEquals(new ArrayList<>(AppOrder.DEFAULT), AppOrder.fromSaved("youtube,youtube,foreign"));
        assertEquals(new ArrayList<>(AppOrder.DEFAULT), AppOrder.fromSaved("netflix,browser,youtube,prime,settings"));
    }
    @Test public void movePersistsOrderWithoutDuplicates() {
        ArrayList<String> ordered = AppOrder.fromSaved(null);
        assertTrue(AppOrder.moveOne(ordered, "browser", -1));
        assertEquals("browser", ordered.get(1));
        assertEquals(ordered, AppOrder.fromSaved(AppOrder.serialize(ordered)));
        assertFalse(AppOrder.moveOne(ordered, "youtube", -1));
    }
}
