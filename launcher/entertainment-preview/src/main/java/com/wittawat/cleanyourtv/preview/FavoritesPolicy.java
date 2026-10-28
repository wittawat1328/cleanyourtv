package com.wittawat.cleanyourtv.preview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure, testable favorite rules. Only discovered launchable apps become tiles. */
final class FavoritesPolicy {
    private static final List<String> PREFERRED=Arrays.asList(
        "com.google.android.youtube.tv","com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom","com.disney.disneyplus");
    private FavoritesPolicy(){}
    static ArrayList<String> firstRun(Map<String,String> labels) {
        LinkedHashSet<String> chosen=new LinkedHashSet<>();
        for(String pkg:PREFERRED.subList(0,2))if(labels.containsKey(pkg))chosen.add(pkg);
        for(Map.Entry<String,String> app:labels.entrySet()) {
            if(isBrowser(app.getKey(),app.getValue())){chosen.add(app.getKey());break;}
        }
        for(String pkg:PREFERRED.subList(2,4))if(labels.containsKey(pkg))chosen.add(pkg);
        for(String pkg:labels.keySet()){if(chosen.size()>=6)break;chosen.add(pkg);}
        return new ArrayList<>(chosen);
    }
    static boolean isBrowser(String pkg,String label) {
        String name=(label==null?"":label).toLowerCase(Locale.ROOT);
        String id=(pkg==null?"":pkg).toLowerCase(Locale.ROOT);
        return name.contains("browser")||name.equals("tv bro")||name.contains("puffin")
            ||name.equals("chrome")||name.equals("firefox")||name.equals("opera")
            ||name.equals("brave")||id.contains(".browser")||id.contains(".tvbro");
    }
    static ArrayList<String> visibleFromSaved(String saved,Set<String> available) {
        ArrayList<String> result=new ArrayList<>();
        if(saved==null||saved.isEmpty())return result;
        for(String pkg:saved.split(",",-1))
            if(!pkg.isEmpty()&&available.contains(pkg)&&!result.contains(pkg))result.add(pkg);
        return result;
    }
    static ArrayList<String> add(List<String> favorites,String pkg,Set<String> available) {
        ArrayList<String> next=new ArrayList<>(favorites);
        if(available.contains(pkg)&&!next.contains(pkg))next.add(pkg);
        return next;
    }
    static ArrayList<String> remove(List<String> favorites,String pkg) {
        ArrayList<String> next=new ArrayList<>(favorites);
        next.remove(pkg);return next;
    }
    static String serialize(List<String> favorites) {return String.join(",",favorites);}
}
