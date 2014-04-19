package jp.tkgktyk.flyingandroid;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
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

	/**
	 * I want to FlyingHelper attaches to a DecorView, but some Window has
	 * multiple DecorViews (e.g. when including TagHost). If attach to a
	 * DecorView directly, FlyingHelper is created a number of DecorViews
	 * (TabHosts). This is undesirable behavior. Here attach to a RootView of
	 * DecorView as workaround.
	 * 
	 * @param decor
	 * @param helper
	 */
	// private void setFlyingHelper(ViewGroup decor, FlyingHelper helper) {
	// // I want to a FlyingHelper attaches to a DecorView, but some Window has
	// // multiple DecorViews (e.g. TagHost). To avoid it, attach to RootView
	// // of DecorView.
	// XposedHelpers.setAdditionalInstanceField(decor.getRootView(),
	// FA_HELPER, helper);
	// }

	/**
	 * Find a FlyingHelper attached to a DecorView by
	 * {@link FlyingAndroid#setFlyingHelper(ViewGroup, FlyingHelper)} and return
	 * it.
	 * 
	 * @param activity
	 * @return
	 */
	// private FlyingHelper getFlyingHelper(Activity activity) {
	// return getFlyingHelper((ViewGroup) activity.getWindow().peekDecorView());
	// }

	@Override
	public void initZygote(StartupParam startupParam) {
		sPref = new XSharedPreferences(PACKAGE_NAME);
		sPref.makeWorldReadable();

		try {
			// XposedHelpers.findAndHookMethod(ViewGroup.class, "addView",
			// View.class, ViewGroup.LayoutParams.class,
			// new XC_MethodReplacement() {
			// @Override
			// protected Object replaceHookedMethod(
			// MethodHookParam param) {
			// try {
			// boolean handled = false;
			// View child = (View) param.args[0];
			// ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams)
			// param.args[1];
			// // class.getName() equals
			// // "com.android.internal.policy.impl.PhoneWindow$DecorView"
			// // class.getCanonicalName() equals
			// // "com.android.internal.policy.impl.PhoneWindow.DecorView"
			// // NOTICE: getCanonicalName() returns null
			// // in
			// // Galaxy's InCall.
			// if (param.thisObject
			// .getClass()
			// .getName()
			// .equals("com.android.internal.policy.impl.PhoneWindow$DecorView"))
			// {
			// final ViewGroup decor = (ViewGroup) param.thisObject;
			// String packageName = decor.getContext()
			// .getPackageName();
			// if (!sIgnoreSet.contains(packageName)) {
			// FlyingAndroidSettings settings = new FlyingAndroidSettings(
			// sPref);
			// FA.logD("reload settings at " + packageName);
			// if (!settings.blackSet
			// .contains(packageName)) {
			// FlyingHelper helper = getFlyingHelper(decor);
			// if (helper == null) {
			// helper = new FlyingHelper(
			// settings);
			// // vertical drag interface
			// // is
			// // enabled on
			// // floating window only.
			// Context context = decor
			// .getContext();
			// TypedArray a = context
			// .getTheme()
			// .obtainStyledAttributes(
			// new int[] { android.R.attr.windowIsFloating });
			// boolean floating = a
			// .getBoolean(0, false);
			// a.recycle();
			// if (floating) {
			// helper.installForFloatingWindow(context);
			// } else {
			// settings.overwriteUsePinByWhiteList(packageName);
			// helper.install(context);
			// }
			// View newChild = helper
			// .getFlyingView();
			// // to avoid stack overflow
			// // (recursive),
			// // call addView(View, int,
			// // LayoutParams)
			// XposedHelpers.callMethod(
			// param.thisObject,
			// "addView",
			// newChild,
			// -1,
			// newChild.getLayoutParams());
			// //
			// setFlyingHelper(decor, helper);
			// }
			// FA.logD("FA_attached = "
			// + (Boolean) XposedHelpers
			// .getAdditionalInstanceField(
			// decor,
			// "FA_attached"));
			// helper.addViewToFlyingView(child,
			// layoutParams);
			// handled = true;
			// }
			// }
			// }
			// if (!handled) {
			// // don't touch original method
			// // to avoid stack overflow (recursive),
			// // call addView(View, int, LayoutParams)
			// XposedHelpers.callMethod(param.thisObject,
			// "addView", child, -1, layoutParams);
			// }
			// } catch (Throwable t) {
			// FA.logE(t);
			// }
			// return null;
			// }
			// });
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
										settings.overwriteUsePinByWhiteList(packageName);
										helper = new FlyingHelper(settings);
										helper.install((ViewGroup) activity
												.getWindow().peekDecorView());
									}
								}
								if (helper != null) {
									// force set window background for clear
									// background.
									Drawable d = activity.getWindow()
											.peekDecorView().getBackground();
									if (d instanceof ColorDrawable) {
										FA.logD("force set window background.");
										TypedArray a = activity
												.getTheme()
												.obtainStyledAttributes(
														new int[] { android.R.attr.windowBackground });
										int background = a.getResourceId(0, 0);
										a.recycle();
										if (background != 0) {
											activity.getWindow()
													.setBackgroundDrawableResource(
															background);
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
			findAndHookMethod(Dialog.class, "onAttachedToWindow",
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
											helper = new FlyingHelper(settings);
											helper.installForFloatingWindow((ViewGroup) dialog
													.getWindow()
													.peekDecorView());
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
				findAndHookMethod(
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
				findAndHookMethod(
						"com.android.systemui.statusbar.phone.PhoneStatusBarView",
						lpparam.classLoader, "onAllPanelsCollapsed",
						new XC_MethodHook() {
							@Override
							protected void afterHookedMethod(
									MethodHookParam param) throws Throwable {
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
