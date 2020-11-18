package com.example.randomdatalogger;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;


public class AccessibilityServiceToggleAirplaneMode extends AccessibilityService {

    private static final String TAG = "AccessibilityServiceToggleAirplaneMode";
    private static final int TOGGLE_DELAY_SECONDS = 1;  // Toggle delay.

    // Are we on the wireless settings screen?
    private static boolean isWirelessSettingsScreen;

    // Has the toggle been completed?
    private static boolean isToggleComplete;

    // Is this the airplane mode toggle?
    private boolean isAirplaneModeToggle;

    // Is this the button to go to mobile hotspots and tethering?
    private boolean isMobileHotspotButton;

    // Is the the mobile hotspot toggle?
    private boolean isMobileHotspotToggle;

    private int countOff;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "onAccessibilityEvent: " + event.getEventType());

        // Respond to window state change events (i.e., go to wireless settings screen).
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            isWirelessSettingsScreen = false;
            isToggleComplete = false;

            isAirplaneModeToggle = false;
            isMobileHotspotButton = false;
            isMobileHotspotToggle = false;

            countOff = 0;

            // Recursive search to: (1) find and toggle airplane mode switch and then go to
            // mobile hotspots and tethering; or (2) enable mobile hotspot after (1) is complete.
            airplaneModeRecursiveSearch(getRootInActiveWindow(), 0);
        }
    }

    @Override
    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected");

        // Return to main logging activity after enabling this service.
        PackageManager packageManager = this.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage("com.example.randomdatalogger");
        if (intent != null) this.startActivity(intent);
    }

    @Override
    public void onInterrupt() {
    }

    // In wireless settings screen, recursively search for airplane mode switch and toggle on/off.
    public void airplaneModeRecursiveSearch(AccessibilityNodeInfo nodeInfo, int depth) {

        if (nodeInfo == null) {
            return;
        }

        // if (isToggleComplete) return;


        String nodeText;
        if (nodeInfo.getText() == null) nodeText = "(null)";
        else nodeText = nodeInfo.getText().toString();

        Log.d(TAG, "Text: " + nodeText);

        if (nodeText.equals("Bluetooth tethering") || nodeText.equals("USB tethering")) {
            isMobileHotspotButton = false;
            isMobileHotspotToggle = false;
        }

        // Upon finding airplane mode toggle, obtain node coordinates and automate gesture to
        // toggle the switch on and off. Text may be device-dependent.

        // Check if airplane mode is on this screen?
        if (nodeText.equals("Airplane mode")) {
            isAirplaneModeToggle = true;
            isWirelessSettingsScreen = true;
        }

        // If wireless settings screen, toggle airplane mode.
        if (isWirelessSettingsScreen && (nodeText.equals("OFF") || nodeText.equals("Off"))) {
            Log.d(TAG, "Found airplane mode switch!");

            Rect rect = new Rect();
            nodeInfo.getBoundsInScreen(rect);

            int bottom = rect.bottom;
            int top = rect.top;
            int left = rect.left;
            int right = rect.right;

            int airplaneModeSwitchX = (left + right) / 2;
            int airplaneModeSwitchY = (top + bottom) / 2;

            Log.d(TAG, "Airplane mode switch: (" + airplaneModeSwitchX + ", " + airplaneModeSwitchY + ").");

            // Perform gesture.
            GestureDescription.Builder gestureBuilder1 = new GestureDescription.Builder();
            GestureDescription.Builder gestureBuilder2 = new GestureDescription.Builder();

            int duration = 1;

            Path clickPath1 = new Path();
            Path clickPath2 = new Path();

            clickPath1.moveTo(airplaneModeSwitchX, airplaneModeSwitchY);
            GestureDescription.StrokeDescription clickStroke1 =
                    new GestureDescription.StrokeDescription(clickPath1, 0, duration);
            gestureBuilder1.addStroke(clickStroke1);
            dispatchGesture(gestureBuilder1.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.w(TAG, "First click completed.");
                    super.onCompleted(gestureDescription);
                }
            }, null);

            // Delay before second click.
            try {
                Thread.sleep(TOGGLE_DELAY_SECONDS * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            clickPath2.moveTo(airplaneModeSwitchX, airplaneModeSwitchY);
            GestureDescription.StrokeDescription clickStroke2 =
                    new GestureDescription.StrokeDescription(clickPath2, 0, duration);
            gestureBuilder2.addStroke(clickStroke2);
            dispatchGesture(gestureBuilder2.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.w(TAG, "Second click completed.");
                    super.onCompleted(gestureDescription);
                }
            }, null);

            // Delay after second click.
            try {
                Thread.sleep(TOGGLE_DELAY_SECONDS * 200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            /*
            // After toggling airplane mode, return to main logging activity.
            PackageManager packageManager = this.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage("com.example.randomdatalogger");
            if (intent != null) this.startActivity(intent);
            */

            isToggleComplete = true;

            // return;

        // If airplane mode is already on, toggle it off.
        } else if (isWirelessSettingsScreen && (nodeText.equals("ON") || nodeText.equals("On"))) {
            Log.d(TAG, "Airplane mode already enabled; disabling airplane mode...");

            Rect rect = new Rect();
            nodeInfo.getBoundsInScreen(rect);

            int bottom = rect.bottom;
            int top = rect.top;
            int left = rect.left;
            int right = rect.right;

            Log.d(TAG, "Switch location: " + bottom + " (bottom), " + top + " (top), "
                    + left + " (left), " + right + " (right).");

            // Rectangle coordinates may need to be swapped.
            if (top > bottom) {
                int temp = top;
                top = bottom;
                bottom = temp;
            }

            if (left > right) {
                int temp = left;
                left = right;
                right = temp;
            }

            int middleXValue = (left + right) / 2;
            int middleYValue = (top + bottom) / 2;

            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();

            int duration = 1;

            Path clickPath = new Path();

            clickPath.moveTo(middleXValue, middleYValue);
            GestureDescription.StrokeDescription clickStroke =
                    new GestureDescription.StrokeDescription(clickPath, 0, duration);
            gestureBuilder.addStroke(clickStroke);
            dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.w(TAG, "Click completed.");
                    super.onCompleted(gestureDescription);
                }
            }, null);

            // Delay after click.
            try {
                Thread.sleep(TOGGLE_DELAY_SECONDS * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // After toggling airplane mode, return to main logging activity.
            PackageManager packageManager = this.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage("com.example.randomdatalogger");
            if (intent != null) this.startActivity(intent);

            isToggleComplete = true;
            // return;
        }

        if (nodeText.equals("Mobile Hotspot and Tethering") && depth != 1) {
            Log.d(TAG, "Mobile Hotspot and Tethering node found!");
            isMobileHotspotButton = true;

            Rect rect = new Rect();
            nodeInfo.getBoundsInScreen(rect);

            int bottom = rect.bottom;
            int top = rect.top;
            int left = rect.left;
            int right = rect.right;

            Log.d(TAG, "Location: " + bottom + " (bottom), " + top + " (top), " + left + " (left), " + right + " (right).");

            int buttonX = (left + right) / 2;
            int buttonY = (top + bottom) / 2;

            Log.d(TAG, "Mobile hotspot settings button: (" + buttonX + ", " + buttonY + ").");

            // Delay before click.
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // If airplane mode has been toggled, go to mobile hotspot and tethering.
            if (isToggleComplete) {

                // Perform first click.
                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
                int duration = 1;
                Path clickPath = new Path();
                clickPath.moveTo(buttonX, buttonY);
                GestureDescription.StrokeDescription clickStroke =
                        new GestureDescription.StrokeDescription(clickPath, 0, duration);
                gestureBuilder.addStroke(clickStroke);
                dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        Log.w(TAG, "Clicked on mobile hotspot link.");
                        super.onCompleted(gestureDescription);
                    }
                }, null);

                return;
            }
        }

        if (nodeText.equals("Mobile Hotspot")) {
            Log.d(TAG, "Mobile hotspot node found!");
            isMobileHotspotToggle = true;
        }

        if (isMobileHotspotToggle && (nodeText.equals("Off") || nodeText.equals("OFF"))) {
            // isMobileHotspotToggle = true;
            countOff++;
        }

        if (isMobileHotspotToggle && countOff > 1 && (nodeText.equals("Off") || nodeText.equals("OFF"))) {
            Rect rect = new Rect();
            nodeInfo.getBoundsInScreen(rect);

            int bottom = rect.bottom;
            int top = rect.top;
            int left = rect.left;
            int right = rect.right;

            Log.d(TAG, "Location: " + bottom + " (bottom), " + top + " (top), " + left + " (left), " + right + " (right).");

            int hotspotSwitchX = (left + right) / 2;
            int hotspotSwitchY = (top + bottom) / 2;

            Log.d(TAG, "Mobile hotspot switch: (" + hotspotSwitchX + ", " + hotspotSwitchY + ").");

            // Perform first click.
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            int duration = 1;
            Path clickPath = new Path();
            clickPath.moveTo(hotspotSwitchX, hotspotSwitchY);
            GestureDescription.StrokeDescription clickStroke =
                    new GestureDescription.StrokeDescription(clickPath, 0, duration);
            gestureBuilder.addStroke(clickStroke);
            dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.w(TAG, "Clicked to enable mobile hotspot.");
                    super.onCompleted(gestureDescription);
                }
            }, null);

            try {
                Thread.sleep(TOGGLE_DELAY_SECONDS * 3 * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // After enabling hotspot, return to main logging activity.
            PackageManager packageManager = this.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage("com.example.randomdatalogger");
            if (intent != null) this.startActivity(intent);

            return;
        }

        // Recursively search through view nodes in wireless settings.
        for (int i = 0; i < nodeInfo.getChildCount(); ++i) {
            airplaneModeRecursiveSearch(nodeInfo.getChild(i), depth + 1);
        }
    }
}