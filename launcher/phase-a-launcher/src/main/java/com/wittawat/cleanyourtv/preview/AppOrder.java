package com.wittawat.cleanyourtv.preview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class AppOrder {
    public static final List<String> DEFAULT = java.util.Collections.unmodifiableList(Arrays.asList(
            "youtube", "netflix", "browser", "prime", "disney", "settings"));
    private AppOrder() {}
    public static ArrayList<String> fromSaved(String saved) {
        if (saved == null) return new ArrayList<>(DEFAULT);
        ArrayList<String> result = new ArrayList<>();
        for (String item : saved.split(",")) {
            if ((DEFAULT.contains(item) || item.matches("pkg:[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) && !result.contains(item)) result.add(item);
        }
        if (result.isEmpty()) result.add("settings");
        return result;
    }
    public static ArrayList<String> reconcile(List<String> order, Collection<String> installed) {
        ArrayList<String> result = new ArrayList<>();
        for (String id : order) {
            if ((DEFAULT.contains(id) || installed.contains(id)) && !result.contains(id)) result.add(id);
        }
        if (result.isEmpty()) result.add("settings");
        return result;
    }
    public static boolean moveOne(List<String> order, String id, int direction) {
        int from = order.indexOf(id), to = from + direction;
        if (from < 0 || (direction != -1 && direction != 1) || to < 0 || to >= order.size()) return false;
        order.remove(from); order.add(to, id); return true;
    }
    public static String serialize(List<String> order) { return String.join(",", order); }
}
