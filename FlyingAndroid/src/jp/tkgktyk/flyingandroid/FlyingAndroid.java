package jp.tkgktyk.flyingandroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class FlyingAndroid implements IXposedHookZygoteInit,
		IXposedHookLoadPackage {
	public static final String PACKAGE_NAME = FlyingAndroid.class.getPackage()
			.getName();
	public static final String ACTION_TOGGLE = PACKAGE_NAME + ".ACTION_TOGGLE";

	private static XSharedPreferences sPref;

	@Override
	public void initZygote(StartupParam startupParam) {
		sPref = new XSharedPreferences(PACKAGE_NAME);
		sPref.makeWorldReadable();

		try {
			XposedHelpers.findAndHookMethod(Activity.class, "onPostCreate",
					Bundle.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								Activity activity = (Activity) param.thisObject;
								FlyingHelper helper = FlyingHelper
										.getFrom(activity.getWindow());
								if (helper == null) {
									String packageName = activity
											.getPackageName();
									FA.Settings settings = new FA.Settings(
											sPref);
									FA.logD("reload settings at " + packageName);
									if (!settings.blackSet
											.contains(packageName)) {
										// save / restore current focus
										View v = activity.getCurrentFocus();
										settings.overwriteUsePinByWhiteList(packageName);
										helper = new FlyingHelper(settings);
										helper.install((ViewGroup) activity
												.getWindow().peekDecorView());
										if (v != null) {
											v.requestFocus();
										}
									}
								}
								if (helper != null) {
									// force set window background for clear
									// background.
									// ColorDrawable and a certain View
									Window window = activity.getWindow();
									View decor = window.peekDecorView();
									Drawable drawable = decor.getBackground();
									if (drawable == null
											|| drawable instanceof ColorDrawable) {
										FA.logD("force set window background.");
										TypedArray a = activity
												.getTheme()
												.obtainStyledAttributes(
														new int[] { android.R.attr.windowBackground });
										int background = a.getResourceId(0, 0);
										a.recycle();
										if (background != 0) {
											window.setBackgroundDrawableResource(background);
											FA.logD(decor.getBackground()
													.toString());
										} else {
											FA.logD("window background is 0.");
										}
									}
								} else {
									FA.logD("FlyingHelper is not found.");
								}
							} catch (Throwable t) {
								FA.logE(t);
							}
						}
					});
			XposedHelpers.findAndHookMethod(Activity.class, "onResume",
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								Activity activity = (Activity) param.thisObject;
								FlyingHelper helper = FlyingHelper
										.getFrom(activity.getWindow());
								if (helper != null) {
									if (!helper.receiverRegistered()) {
										BroadcastReceiver receiver = helper
												.getToggleReceiver();
										activity.registerReceiver(receiver,
												new IntentFilter(ACTION_TOGGLE));
										helper.onReceiverRegistered();
										FA.logD("register");
									}
								} else {
									FA.logD("FlyingHelper is not found.");
								}
							} catch (Throwable t) {
								FA.logE(t);
							}
						}
					});
			XposedHelpers.findAndHookMethod(Activity.class, "onPause",
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								Activity activity = (Activity) param.thisObject;
								FlyingHelper helper = FlyingHelper
										.getFrom(activity.getWindow());
								if (helper != null) {
									if (helper.receiverRegistered()) {
										BroadcastReceiver receiver = helper
												.getToggleReceiver();
										activity.unregisterReceiver(receiver);
										helper.onReceiverUnregistered();
										FA.logD("unregister");
									}
								} else {
									FA.logD("FlyingHelper is not found.");
								}
							} catch (Throwable t) {
								FA.logE(t);
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
								FlyingHelper helper = FlyingHelper
										.getFrom(activity.getWindow());
								if (helper != null) {
									FlyingView flyingView = helper
											.getFlyingView();
									Configuration newConfig = (Configuration) param.args[0];
									if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
										flyingView.rotate();
									} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
										flyingView.rotate();
									}
								} else {
									FA.logD("FlyingHelper is not found.");
								}
							} catch (Throwable t) {
								FA.logE(t);
							}
						}
					});
			XposedHelpers.findAndHookMethod(Dialog.class, "onAttachedToWindow",
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								FA.Settings settings = new FA.Settings(sPref);
								if (settings.flyingDialog) {
									Dialog dialog = (Dialog) param.thisObject;
									String packageName = dialog.getContext()
											.getPackageName();
									if (!settings.blackSet
											.contains(packageName)) {
										FlyingHelper helper = FlyingHelper
												.getFrom(dialog.getWindow());
										if (helper == null) {
											// save / restore current focus
											View v = dialog.getCurrentFocus();
											helper = new FlyingHelper(settings);
											helper.installForFloatingWindow((ViewGroup) dialog
													.getWindow()
													.peekDecorView());
											if (v != null) {
												v.requestFocus();
											}
										}
									}
								}
							} catch (Throwable t) {
								FA.logE(t);
							}
						}
					});
		} catch (Throwable t) {
			FA.logE(t);
		}
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		try {
			if (!lpparam.packageName.equals("com.android.systemui")) {
				return;
			}
			try {
				XposedHelpers.findAndHookMethod(
						"com.android.systemui.statusbar.phone.PhoneStatusBar",
						lpparam.classLoader, "makeStatusBarView",
						new XC_MethodHook() {
							@Override
							protected void afterHookedMethod(
									MethodHookParam param) throws Throwable {
								try {
									FA.Settings settings = new FA.Settings(
											sPref);
									if (settings.useFlyingStatusBar()) {
										ViewGroup panel = (ViewGroup) XposedHelpers
												.getObjectField(
														param.thisObject,
														"mNotificationPanel");
										FlyingHelper helper = FlyingHelper
												.getFrom(panel);
										if (helper == null) {
											helper = new FlyingHelper(settings);
											helper.installWithPinShownAlways(panel);
										}
									}
								} catch (Throwable t) {
									FA.logE(t);
								}
							}
						});
			} catch (NoSuchMethodError e) {
				FA.logE("PhoneStatusBar#makeStatusBarView is not found. \"Flying status bar\" is not available.");
			}
			try {
				XposedHelpers
						.findAndHookMethod(
								"com.android.systemui.statusbar.phone.PhoneStatusBarView",
								lpparam.classLoader, "onAllPanelsCollapsed",
								new XC_MethodHook() {
									@Override
									protected void afterHookedMethod(
											MethodHookParam param)
											throws Throwable {
										try {
											FlyingHelper helper = FlyingHelper
													.getFrom((View) param.thisObject);
											if (helper != null
													&& helper.getSettings().resetWhenCollapsed) {
												helper.resetState();
											}
										} catch (Throwable t) {
											FA.logE(t);
										}
									}
								});
			} catch (NoSuchMethodError e) {
				FA.logE("PhoneStatusBarView#onAllPanelsCollapsed is not found. \"Reset when collapsed\" is not available.");
			}
		} catch (Throwable t) {
			FA.logE(t);
		}
	}
}
