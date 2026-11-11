/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mapdemo;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ApplicationProvider;
import com.example.mapdemo.EdgeToEdgeUtil.EdgeToEdgeMarginConfig;
import com.example.mapdemo.testing.NavDelegateReflection;
import com.example.mapdemo.testing.NavMapsRobolectricTest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

/**
 * Java unit tests for {@code map-sample} utility and infrastructure classes using <b>Google
 * Truth</b> ({@link com.google.common.truth.Truth#assertThat}).
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>{@link EdgeToEdgeUtil} and {@link EdgeToEdgeMarginConfig} AutoValue builder defaults and
 *       system-bar window inset margin application.
 *   <li>{@link FeatureView} custom widget inflation and title/description binding.
 *   <li>{@link AnimationManager} frame scheduling, start/stop idempotency, and FPS updates.
 *   <li>{@link OnMapAndViewReadyListener} synchronization between layout completion and {@code
 *       getMapAsync} for both {@link SupportMapFragment} and {@link SupportNavigationFragment}.
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
public class MapUtilitiesJavaTest extends NavMapsRobolectricTest {

  /** Test double for {@link SupportMapFragment} with a configurable view and deferred map ready. */
  private static class FakeSupportMapFragment extends SupportMapFragment {
    private final View fragmentView;

    FakeSupportMapFragment(View fragmentView) {
      this.fragmentView = fragmentView;
    }

    @Override
    public View getView() {
      return fragmentView;
    }

    @Override
    public void getMapAsync(OnMapReadyCallback callback) {
      // Deferred until test explicitly invokes listener.onMapReady(map)
    }
  }

  /**
   * Test double for {@link SupportNavigationFragment} with a configurable view and deferred map
   * ready.
   */
  private static class FakeSupportNavigationFragment extends SupportNavigationFragment {
    private final View fragmentView;

    FakeSupportNavigationFragment(View fragmentView) {
      this.fragmentView = fragmentView;
    }

    @Override
    public View getView() {
      return fragmentView;
    }

    @Override
    public void getMapAsync(OnMapReadyCallback callback) {
      // Deferred until test explicitly invokes listener.onMapReady(map)
    }
  }

  @Test
  public void edgeToEdgeMarginConfig_defaultsAllMarginsToTrueAndAppliesSelectedInsets() {
    Context context = ApplicationProvider.getApplicationContext();
    View targetView = new View(context);
    MarginLayoutParams initialParams = new MarginLayoutParams(200, 200);
    targetView.setLayoutParams(initialParams);

    EdgeToEdgeMarginConfig defaultConfig =
        EdgeToEdgeMarginConfig.builder().setView(targetView).build();
    assertThat(defaultConfig.view()).isSameInstanceAs(targetView);
    assertThat(defaultConfig.shouldSetTopMargin()).isTrue();
    assertThat(defaultConfig.shouldSetBottomMargin()).isTrue();
    assertThat(defaultConfig.shouldSetLeftMargin()).isTrue();
    assertThat(defaultConfig.shouldSetRightMargin()).isTrue();

    EdgeToEdgeMarginConfig customConfig =
        EdgeToEdgeMarginConfig.builder()
            .setView(targetView)
            .setShouldSetTopMargin(true)
            .setShouldSetBottomMargin(false)
            .setShouldSetLeftMargin(true)
            .setShouldSetRightMargin(false)
            .build();
    EdgeToEdgeUtil.setMarginForEdgeToEdgeSupport(customConfig);

    WindowInsetsCompat windowInsets =
        new WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(12, 24, 36, 48))
            .build();
    ViewCompat.onApplyWindowInsets(targetView, windowInsets);

    MarginLayoutParams updatedParams = (MarginLayoutParams) targetView.getLayoutParams();
    assertThat(updatedParams.leftMargin).isEqualTo(12);
    assertThat(updatedParams.topMargin).isEqualTo(24);
    assertThat(updatedParams.rightMargin).isEqualTo(0);
    assertThat(updatedParams.bottomMargin).isEqualTo(0);
  }

  @Test
  public void featureView_inflatesLayoutAndUpdatesTitleAndDescriptionText() {
    Context context = ApplicationProvider.getApplicationContext();
    FeatureView featureView = new FeatureView(context);

    featureView.setTitleId(R.string.basic_map_demo_label);
    featureView.setDescriptionId(R.string.basic_map_demo_description);

    TextView titleView = featureView.findViewById(R.id.title);
    TextView descriptionView = featureView.findViewById(R.id.description);

    assertThat(titleView).isNotNull();
    assertThat(descriptionView).isNotNull();
    assertThat(titleView.getText().toString())
        .isEqualTo(context.getString(R.string.basic_map_demo_label));
    assertThat(descriptionView.getText().toString())
        .isEqualTo(context.getString(R.string.basic_map_demo_description));
  }

  @Test
  public void animationManager_schedulesFramesOnMainLooperAndStopsCleanly() {
    AtomicInteger frameCount = new AtomicInteger(0);
    AnimationManager manager = new AnimationManager(frameCount::incrementAndGet);

    assertThat(manager.getFrameRateFps()).isEqualTo(60.0);
    manager.setFrameRateFps(20.0);
    assertThat(manager.getFrameRateFps()).isEqualTo(20.0);

    manager.startAnimation();
    // Starting an already running animation is a no-op.
    manager.startAnimation();

    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(160));
    int countAfterRun = frameCount.get();
    assertThat(countAfterRun).isAtLeast(2);

    manager.stopAnimation();
    Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));
    assertThat(frameCount.get()).isEqualTo(countAfterRun);
  }

  @Test
  public void onMapAndViewReadyListener_waitsForBothLayoutAndMapReadyBeforeFiringCallback() {
    Context context = ApplicationProvider.getApplicationContext();
    // An un-laid-out view has width=0 and height=0
    FrameLayout unlaidOutView = new FrameLayout(context);
    FakeSupportMapFragment mapFragment = new FakeSupportMapFragment(unlaidOutView);

    AtomicReference<GoogleMap> receivedMap = new AtomicReference<>();
    OnMapAndViewReadyListener listener =
        new OnMapAndViewReadyListener(
            mapFragment,
            (OnMapAndViewReadyListener.OnGlobalLayoutAndMapReadyListener) receivedMap::set);

    GoogleMap expectedMap = NavDelegateReflection.createGoogleMapInstance();
    // Map arrives first while view dimensions are still 0x0 -> callback should not fire yet.
    listener.onMapReady(expectedMap);
    assertThat(receivedMap.get()).isNull();

    // Global layout completes -> callback fires with the ready GoogleMap.
    listener.onGlobalLayout();
    assertThat(receivedMap.get()).isSameInstanceAs(expectedMap);
  }

  @Test
  public void onMapAndViewReadyListener_firesImmediatelyWhenNavigationFragmentViewAlreadyLaidOut() {
    Context context = ApplicationProvider.getApplicationContext();
    FrameLayout laidOutView = new FrameLayout(context);
    laidOutView.layout(0, 0, 400, 400);
    FakeSupportNavigationFragment navFragment = new FakeSupportNavigationFragment(laidOutView);

    AtomicReference<GoogleMap> receivedMap = new AtomicReference<>();
    OnMapAndViewReadyListener listener =
        new OnMapAndViewReadyListener(navFragment, (OnMapReadyCallback) receivedMap::set);

    GoogleMap expectedMap = NavDelegateReflection.createGoogleMapInstance();
    listener.onMapReady(expectedMap);
    assertThat(receivedMap.get()).isSameInstanceAs(expectedMap);
  }

  @Test
  public void permissionUtils_evaluatesGrantResultsAndCreatesDialogs() {
    String fineLocation = Manifest.permission.ACCESS_FINE_LOCATION;
    String coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION;

    assertThat(
            PermissionUtils.isPermissionGranted(
                new String[] {coarseLocation, fineLocation},
                new int[] {PackageManager.PERMISSION_DENIED, PackageManager.PERMISSION_GRANTED},
                fineLocation))
        .isTrue();

    assertThat(
            PermissionUtils.isPermissionGranted(
                new String[] {fineLocation},
                new int[] {PackageManager.PERMISSION_DENIED},
                fineLocation))
        .isFalse();

    assertThat(
            PermissionUtils.isPermissionGranted(
                new String[] {coarseLocation},
                new int[] {PackageManager.PERMISSION_GRANTED},
                fineLocation))
        .isFalse();

    PermissionUtils.PermissionDeniedDialog deniedDialog =
        PermissionUtils.PermissionDeniedDialog.newInstance(true);
    assertThat(deniedDialog.getArguments().getBoolean("finish")).isTrue();

    PermissionUtils.RationaleDialog rationaleDialog =
        PermissionUtils.RationaleDialog.newInstance(42, true);
    assertThat(rationaleDialog.getArguments().getInt("requestCode")).isEqualTo(42);
    assertThat(rationaleDialog.getArguments().getBoolean("finish")).isTrue();
  }

  @Test
  public void polygonAndPolylinePagerAdapters_instantiateExpectedControlFragmentsAndTitles() {
    AppCompatActivity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();

    PolylineControlFragmentPagerAdapter polylineAdapter =
        new PolylineControlFragmentPagerAdapter(activity.getSupportFragmentManager());
    assertThat(polylineAdapter.getCount()).isEqualTo(8);
    assertThat(polylineAdapter.getPageTitle(0).toString()).isEqualTo("Color");
    assertThat(polylineAdapter.getPageTitle(1).toString()).isEqualTo("Width");
    assertThat(polylineAdapter.getPageTitle(2).toString()).isEqualTo("Cap");
    assertThat(polylineAdapter.getPageTitle(3).toString()).isEqualTo("Joint");
    assertThat(polylineAdapter.getPageTitle(4).toString()).isEqualTo("Pattern");
    assertThat(polylineAdapter.getPageTitle(5).toString()).isEqualTo("Points");
    assertThat(polylineAdapter.getPageTitle(6).toString()).isEqualTo("Spans");
    assertThat(polylineAdapter.getPageTitle(7).toString()).isEqualTo("Other Options");
    assertThat(polylineAdapter.getItem(0)).isInstanceOf(PolylineColorControlFragment.class);
    assertThat(polylineAdapter.getItem(7)).isInstanceOf(PolylineOtherOptionsControlFragment.class);

    PolygonControlFragmentPagerAdapter polygonAdapter =
        new PolygonControlFragmentPagerAdapter(activity.getSupportFragmentManager());
    assertThat(polygonAdapter.getCount()).isEqualTo(6);
    assertThat(polygonAdapter.getPageTitle(0).toString()).isEqualTo("Stroke/fill color");
    assertThat(polygonAdapter.getPageTitle(1).toString()).isEqualTo("Stroke width");
    assertThat(polygonAdapter.getPageTitle(2).toString()).isEqualTo("Stroke joint");
    assertThat(polygonAdapter.getPageTitle(3).toString()).isEqualTo("Stroke pattern");
    assertThat(polygonAdapter.getPageTitle(4).toString()).isEqualTo("Points");
    assertThat(polygonAdapter.getPageTitle(5).toString()).isEqualTo("Other Options");
    assertThat(polygonAdapter.getItem(0)).isInstanceOf(PolygonColorControlFragment.class);
    assertThat(polygonAdapter.getItem(5)).isInstanceOf(PolygonOtherOptionsControlFragment.class);
  }
}
