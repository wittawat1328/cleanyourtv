package com.wittawat.cleanyourtv.preview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public final class AppOrder {
    public static final List<String> DEFAULT = Arrays.asList(
            "youtube", "netflix", "browser", "prime", "disney", "settings");

    private AppOrder() {}

    /** Discard stale/duplicate/foreign IDs instead of showing hidden or phantom apps. */
    public static ArrayList<String> fromSaved(String saved) {
        ArrayList<String> result = new ArrayList<>(DEFAULT);
        if (saved == null) return result;
        String[] items = saved.split(",", -1);
        if (items.length != DEFAULT.size()) return result;
        List<String> candidate = Arrays.asList(items);
        if (new HashSet<>(candidate).size() != DEFAULT.size() || !candidate.containsAll(DEFAULT)) return result;
        return new ArrayList<>(candidate);
    }

    public static boolean moveOne(List<String> order, String id, int direction) {
        int from = order.indexOf(id);
        int to = from + direction;
        if (from < 0 || (direction != -1 && direction != 1) || to < 0 || to >= order.size()) return false;
        order.remove(from);
        order.add(to, id);
        return true;
    }

    public static String serialize(List<String> order) {
        return String.join(",", order);
    }
}
