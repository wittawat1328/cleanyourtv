package com.wittawat.cleanyourtv.preview;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class FavoritesPolicyTest {
 @Test public void browserThirdOnlyIfInstalled() {
  Map<String,String> apps=new LinkedHashMap<>();
  apps.put("com.amazon.amazonvideo.livingroom","Prime Video");
  apps.put("com.google.android.youtube.tv","YouTube");
  apps.put("org.test.tvbrowser","TV Browser");
  apps.put("com.netflix.ninja","Netflix");
  List<String> initial=FavoritesPolicy.firstRun(apps);
  assertEquals("com.google.android.youtube.tv",initial.get(0));
  assertEquals("com.netflix.ninja",initial.get(1));
  assertEquals("org.test.tvbrowser",initial.get(2));
 }
 @Test public void missingPackagesNeverBecomeFavorites(){
  Map<String,String> apps=new LinkedHashMap<>();
  apps.put("com.google.android.youtube.tv","YouTube");
  assertEquals(Arrays.asList("com.google.android.youtube.tv"),FavoritesPolicy.firstRun(apps));
 }
 @Test public void removedAndDuplicatePackageIdsAreHidden(){
  Set<String> available=new LinkedHashSet<>(Arrays.asList("app.a","app.c"));
  assertEquals(Arrays.asList("app.a","app.c"),
   FavoritesPolicy.visibleFromSaved("app.a,missing,app.a,app.c",available));
 }
 @Test public void pinUnpinDoesNotUninstallOrAffectUnknownApps(){
  Set<String> available=new LinkedHashSet<>(Arrays.asList("app.a","app.b"));
  List<String> pinned=FavoritesPolicy.add(Arrays.asList("app.a"),"app.b",available);
  assertEquals(Arrays.asList("app.a","app.b"),pinned);
  assertEquals(Arrays.asList("app.a"),FavoritesPolicy.remove(pinned,"app.b"));
  assertEquals(Arrays.asList("app.a"),FavoritesPolicy.add(Arrays.asList("app.a"),"missing",available));
 }
 @Test public void deliberatelyEmptyFavoritesRemainEmpty(){
  assertTrue(FavoritesPolicy.visibleFromSaved("",new HashSet<>(Arrays.asList("app.a"))).isEmpty());
 }
}
