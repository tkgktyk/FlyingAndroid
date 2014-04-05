package jp.tkgktyk.flyingandroid;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FlyingAndroid implements IXposedHookZygoteInit {
	public static final String PACKAGE_NAME = FlyingAndroid.class.getPackage()
			.getName();
	public static final String ACTION_TOGGLE = PACKAGE_NAME + ".ACTION_TOGGLE";

	public class Settings implements Cloneable {
		public static final int TAKEOFF_POSITION_CENTER = 0;
		public static final int TAKEOFF_POSITION_BOTTOM = 1;
		public static final int TAKEOFF_POSITION_LOWER_LEFT = 2;
		public static final int TAKEOFF_POSITION_LOWER_RIGHT = 3;

		public static final int PIN_POSITION_NONE = 0;
		public static final int PIN_POSITION_CENTER_LEFT = 1;
		public static final int PIN_POSITION_CENTER_RIGHT = 2;
		public static final int PIN_POSITION_LOWER_LEFT = 3;
		public static final int PIN_POSITION_LOWER_RIGHT = 4;

		public float speed;
		public int takeoffPosition;
		public boolean notifyFlying;
		public Set<String> blackSet;
		public int pinPosition;
		public boolean forceSetWindowBackground;
		public boolean autoPin;

		public Settings(XSharedPreferences pref) {
			pref.reload();
			speed = Float.parseFloat(pref.getString("pref_key_speed", "1.5"));
			takeoffPosition = Integer.parseInt(pref.getString(
					"pref_key_takeoff_position", "0"));
			notifyFlying = pref.getBoolean("pref_key_notify_flying", true);
			blackSet = pref.getStringSet("pref_key_black_list",
					Collections.<String> emptySet());
			pinPosition = Integer.parseInt(pref.getString(
					"pref_key_pin_position", "0"));
			forceSetWindowBackground = pref.getBoolean(
					"pref_key_force_set_window_background", false);
			autoPin = pref.getBoolean("pref_key_auto_pin", false);
		}

		@Override
		public Settings clone() {
			Settings ret = null;
			try {
				ret = (Settings) super.clone();
			} catch (CloneNotSupportedException e) {
				XposedBridge.log(e);
			}
			return ret;
		}

	}

	private static XSharedPreferences sPref;
	private static Set<String> sIgnoreSet;

	private static String FA_HELPER = "FA_helper";

	private FlyingHelper getFlyingHelper(Activity activity) {
		return getFlyingHelper((ViewGroup) activity.getWindow().peekDecorView());
	}

	private FlyingHelper getFlyingHelper(ViewGroup decor) {
		return (FlyingHelper) XposedHelpers.getAdditionalInstanceField(decor,
				FA_HELPER);
	}

	@Override
	public void initZygote(StartupParam startupParam) {
		sPref = new XSharedPreferences(PACKAGE_NAME);
		sPref.makeWorldReadable();
		sIgnoreSet = new HashSet<String>();
		sIgnoreSet.add(PACKAGE_NAME);

		try {
			XposedHelpers.findAndHookMethod(ViewGroup.class, "addView",
					View.class, ViewGroup.LayoutParams.class,
					new XC_MethodReplacement() {
						@Override
						protected Object replaceHookedMethod(
								MethodHookParam param) {
							try {
								boolean handled = false;
								View child = (View) param.args[0];
								ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams) param.args[1];
								// class.getName() equals
								// "com.android.internal.policy.impl.PhoneWindow$DecorView"
								// class.getCanonicalName() equals
								// "com.android.internal.policy.impl.PhoneWindow.DecorView"
								// NOTICE: getCanonicalName() returns null in
								// Galaxy's InCall.
								if (param.thisObject
										.getClass()
										.getName()
										.equals("com.android.internal.policy.impl.PhoneWindow$DecorView")) {
									final ViewGroup decor = (ViewGroup) param.thisObject;
									String packageName = decor.getContext()
											.getPackageName();
									if (!sIgnoreSet.contains(packageName)) {
										Settings settings = new Settings(sPref);
										log("reload settings at " + packageName);
										if (!settings.blackSet
												.contains(packageName)) {
											FlyingHelper helper = getFlyingHelper(decor);
											if (helper == null) {
												// vertical drag interface is
												// enabled on
												// floating window only.
												TypedArray a = decor
														.getContext()
														.getTheme()
														.obtainStyledAttributes(
																new int[] { android.R.attr.windowIsFloating });
												boolean floating = a
														.getBoolean(0, false);
												a.recycle();
												helper = new FlyingHelper(
														settings, decor
																.getContext(),
														floating);
												View newChild = helper
														.getFlyingRootView();
												// to avoid stack overflow
												// (recursive),
												// call addView(View, int,
												// LayoutParams)
												XposedHelpers.callMethod(
														param.thisObject,
														"addView",
														newChild,
														-1,
														newChild.getLayoutParams());
												//
												helper.addViewToFlyingView(
														child, layoutParams);
												//
												XposedHelpers
														.setAdditionalInstanceField(
																decor,
																FA_HELPER,
																helper);
											} else {
												helper.addViewToFlyingView(
														child, layoutParams);
											}
											handled = true;
										}
									}
								}
								if (!handled) {
									// don't touch original method
									// to avoid stack overflow (recursive),
									// call addView(View, int, LayoutParams)
									XposedHelpers.callMethod(param.thisObject,
											"addView", child, -1, layoutParams);
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
							return null;
						}
					});
			XposedHelpers.findAndHookMethod(Activity.class, "onPostCreate",
					Bundle.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								final Activity activity = (Activity) param.thisObject;
								final FlyingHelper helper = getFlyingHelper(activity);
								if (helper != null) {
									if (helper.getSettings().forceSetWindowBackground) {
										// force set window background for clear
										// background.
										TypedArray a = activity
												.getTheme()
												.obtainStyledAttributes(
														new int[] { android.R.attr.windowBackground });
										int background = a.getResourceId(0, 0);
										a.recycle();
										activity.getWindow()
												.setBackgroundDrawableResource(
														background);
									}
								} else {
									log("FlyingHelper is not found.");
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
						}
					});
			XposedHelpers.findAndHookMethod(Activity.class, "onResume",
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								final Activity activity = (Activity) param.thisObject;
								final FlyingHelper helper = getFlyingHelper(activity);
								if (helper != null) {
									final FlyingHelper.ToggleHelper toggleHelper = helper
											.getToggleHelper();
									if (!toggleHelper.receiverRegistered()) {
										final BroadcastReceiver receiver = toggleHelper
												.getToggleReceiver();
										activity.registerReceiver(receiver,
												new IntentFilter(ACTION_TOGGLE));
										toggleHelper.onReceiverRegistered();
										log("register");
									}
								} else {
									log("FlyingHelper is not found.");
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
						}
					});
			XposedHelpers.findAndHookMethod(Activity.class, "onPause",
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								final Activity activity = (Activity) param.thisObject;
								final FlyingHelper helper = getFlyingHelper(activity);
								if (helper != null) {
									final FlyingHelper.ToggleHelper toggleHelper = helper
											.getToggleHelper();
									if (toggleHelper.receiverRegistered()) {
										final BroadcastReceiver receiver = toggleHelper
												.getToggleReceiver();
										activity.unregisterReceiver(receiver);
										toggleHelper.onReceiverUnregistered();
										log("unregister");
									}
								} else {
									log("FlyingHelper is not found.");
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
						}
					});
			XposedHelpers.findAndHookMethod(Activity.class,
					"onConfigurationChanged", Configuration.class,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								Activity activity = (Activity) param.thisObject;
								final FlyingHelper helper = getFlyingHelper(activity);
								if (helper != null) {
									final FlyingView flyingView = helper
											.getFlyingView();
									Configuration newConfig = (Configuration) param.args[0];
									if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
										flyingView.rotate();
									} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
										flyingView.rotate();
									}
								} else {
									log("FlyingHelper is not found.");
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
						}
					});
		} catch (Throwable t) {
			XposedBridge.log(t);
		}
	}

	private void log(String text) {
		if (BuildConfig.DEBUG) {
			XposedBridge.log("FA: " + text);
		}
	}
}
