package com.wittawat.cleanyourtv.preview;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/** Scope discovery to visible, enabled, exported TV/phone launcher activities only. */
final class InstalledApps {
    static final class Item {
        final String packageName;
        final String label;
        final ComponentName component;
        final boolean leanback;
        final ResolveInfo resolved;
        Item(String pkg,String text,ComponentName activity,boolean tv,ResolveInfo info) {
            packageName=pkg; label=text; component=activity; leanback=tv; resolved=info;
        }
        Intent launchIntent() {
            return new Intent(Intent.ACTION_MAIN)
                .addCategory(leanback?Intent.CATEGORY_LEANBACK_LAUNCHER:Intent.CATEGORY_LAUNCHER)
                .setComponent(component);
        }
        Drawable icon(PackageManager pm) {
            try{return resolved.loadIcon(pm);}catch(RuntimeException ex){return null;}
        }
    }
    private InstalledApps(){}
    static LinkedHashMap<String,Item> discover(Context context) {
        PackageManager pm=context.getPackageManager();
        LinkedHashMap<String,Item> apps=new LinkedHashMap<>();
        read(pm,context.getPackageName(),Intent.CATEGORY_LEANBACK_LAUNCHER,true,apps);
        read(pm,context.getPackageName(),Intent.CATEGORY_LAUNCHER,false,apps);
        ArrayList<Item> sorted=new ArrayList<>(apps.values());
        Collections.sort(sorted,Comparator.comparing((Item item)->item.label,String.CASE_INSENSITIVE_ORDER)
            .thenComparing(item->item.packageName));
        LinkedHashMap<String,Item> result=new LinkedHashMap<>();
        for(Item item:sorted)result.put(item.packageName,item);
        return result;
    }
    private static void read(PackageManager pm,String own,String category,boolean tv,LinkedHashMap<String,Item> apps){
        List<ResolveInfo> found;
        try{found=pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0);}
        catch(RuntimeException ex){return;}
        if(found==null)return;
        for(ResolveInfo resolved:found){
            if(resolved==null)continue;
            ActivityInfo info=resolved.activityInfo;
            if(info==null||info.applicationInfo==null||!info.enabled
                ||!info.applicationInfo.enabled||!info.exported)continue;
            String pkg=info.packageName, cls=info.name;
            if(pkg==null||pkg.isEmpty()||pkg.equals(own)||cls==null||cls.isEmpty()
                ||apps.containsKey(pkg))continue;
            CharSequence name;
            try{name=resolved.loadLabel(pm);}catch(RuntimeException ex){name=null;}
            String label=name==null?pkg:name.toString().trim();
            if(label.isEmpty())label=pkg;
            apps.put(pkg,new Item(pkg,label,new ComponentName(pkg,cls),tv,resolved));
        }
    }
}
