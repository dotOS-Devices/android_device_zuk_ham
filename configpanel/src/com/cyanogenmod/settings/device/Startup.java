/*
 * Copyright (C) 2016 The CyanogenMod Project
 *           (C) 2017 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import org.cyanogenmod.internal.util.FileUtils;

public class Startup extends BroadcastReceiver {

    private static final String TAG = Startup.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_PRE_BOOT_COMPLETED.equals(action)) {
            // Disable touchscreen gesture settings if needed
            if (!hasTouchscreenGestures()) {
                disableComponent(context, TouchscreenGestureSettings.class.getName());
            } else {
                enableComponent(context, TouchscreenGestureSettings.class.getName());
                // Restore nodes to saved preference values
                for (String pref : Constants.sGesturePrefKeys) {
                    boolean value = Constants.isPreferenceEnabled(context, pref);
                    String node = Constants.sBooleanNodePreferenceMap.get(pref);
                    // If music gestures are toggled, update values of all music gesture proc files
                    if (pref.equals(Constants.TOUCHSCREEN_MUSIC_GESTURE_KEY)) {
                        for (String music_nodes: Constants.TOUCHSCREEN_MUSIC_GESTURES_ARRAY) {
                            if (!FileUtils.writeLine(music_nodes, value ? "1" : "0")) {
                                Log.w(TAG, "Write to node " + music_nodes +
                                    " failed while restoring saved preference values");
                            }
                        }
                    }
                    else if (!FileUtils.writeLine(node, value ? "1" : "0")) {
                        Log.w(TAG, "Write to node " + node +
                            " failed while restoring saved preference values");
                    }
                }
            }

            // Disable backtouch settings if needed
            if (hasGestureService(context)) {
                disableComponent(context, GesturePadSettings.class.getName());
            } else {
                IBinder b = ServiceManager.getService("gesture");
                IGestureService sInstance = IGestureService.Stub.asInterface(b);

                boolean value = Constants.isPreferenceEnabled(context,
                        Constants.TOUCHPAD_STATE_KEY);
                String node = Constants.sBooleanNodePreferenceMap.get(
                        Constants.TOUCHPAD_STATE_KEY);
                if (!FileUtils.writeLine(node, value ? "1" : "0")) {
                    Log.w(TAG, "Write to node " + node +
                            " failed while restoring touchpad enable state");
                }

                // Set longPress event
                toggleLongPress(context, sInstance, Constants.isPreferenceEnabled(
                        context, Constants.TOUCHPAD_LONGPRESS_KEY));

                // Set doubleTap event
                toggleDoubleTap(context, sInstance, Constants.isPreferenceEnabled(
                        context, Constants.TOUCHPAD_DOUBLETAP_KEY));
            }

            // Disable button settings if needed
            if (!hasButtonProcs()) {
                disableComponent(context, ButtonSettingsActivity.class.getName());
            } else {
                enableComponent(context, ButtonSettingsActivity.class.getName());

                // Restore nodes to saved preference values
                for (String pref : Constants.sButtonPrefKeys) {
                    String node, value;
                    if (Constants.sStringNodePreferenceMap.containsKey(pref)) {
                        node = Constants.sStringNodePreferenceMap.get(pref);
                        value = Utils.getPreferenceString(context, pref);
                    } else {
                        node = Constants.sBooleanNodePreferenceMap.get(pref);
                        value = Utils.isPreferenceEnabled(context, pref) ? "1" : "0";
                    }
                    if (!FileUtils.writeLine(node, value)) {
                        Log.w(TAG, "Write to node " + node +
                            " failed while restoring saved preference values");
                    }
                }
            }
        }
    }

    public static void toggleDoubleTap(Context context, IGestureService gestureService,
            boolean enable) {
        PendingIntent pendingIntent = null;
        if (enable) {
            Intent doubleTapIntent = new Intent("cyanogenmod.intent.action.GESTURE_CAMERA", null);
            pendingIntent = PendingIntent.getBroadcastAsUser(
                    context, 0, doubleTapIntent, 0, UserHandle.CURRENT);
        }
        try {
            System.out.println("toggleDoubleTap : " + pendingIntent);
            gestureService.setOnDoubleClickPendingIntent(pendingIntent);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void toggleLongPress(Context context, IGestureService gestureService,
            boolean enable) {
        PendingIntent pendingIntent = null;
        if (enable) {
            Intent longPressIntent = new Intent(Intent.ACTION_CAMERA_BUTTON, null);
            pendingIntent = PendingIntent.getBroadcastAsUser(
                    context, 0, longPressIntent, 0, UserHandle.CURRENT);
        }
        try {
            System.out.println("toggleLongPress : " + pendingIntent);
            gestureService.setOnLongPressPendingIntent(pendingIntent);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendInputEvent(InputEvent event) {
        InputManager inputManager = InputManager.getInstance();
        inputManager.injectInputEvent(event,
                InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }

    static boolean hasGestureService(Context context) {
        return !context.getResources().getBoolean(
                com.android.internal.R.bool.config_enableGestureService);
    }

    static  boolean hasTouchscreenGestures() {
        return new File(Constants.TOUCHSCREEN_CAMERA_NODE).exists() &&
            new File(Constants.TOUCHSCREEN_DOUBLE_SWIPE_NODE).exists() &&
            new File(Constants.TOUCHSCREEN_FLASHLIGHT_NODE).exists();
    }

    static boolean hasButtonProcs() {
        return (FileUtils.fileExists(Constants.BUTTON_SWAP_NODE));
    }

    private void disableComponent(Context context, String component) {
        ComponentName name = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(name,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void enableComponent(Context context, String component) {
        ComponentName name = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        if (pm.getComponentEnabledSetting(name)
                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            pm.setComponentEnabledSetting(name,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
