package jp.tkgktyk.flyingandroid;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TabHost;
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

	private static final String FA_HANDLED = "FA_handled";
	private static final String FA_REGISTERED = "FA_registered";

	private static XSharedPreferences sPref;

	private void installToActivity(Activity activity) {
		ViewGroup decor = (ViewGroup) activity.getWindow().peekDecorView();
		if (decor == null) {
			FA.logD("decorView is null.");
			return;
		}
		if (isTabContent(decor)) {
			FA.logD("tab content activity is ignored. @onResume");
			XposedHelpers.setAdditionalInstanceField(activity, FA_HANDLED,
					false);
			return;
		}
		String packageName = activity.getPackageName();
		FA.Settings settings = new FA.Settings(sPref);
		FA.logD("reload settings at " + activity.getLocalClassName() + "@"
				+ packageName);
		FlyingHelper helper = null;
		if (!settings.blackSet.contains(packageName)) {
			// save / restore current focus
			View v = activity.getCurrentFocus();
			settings.overwriteUsePinByWhiteList(packageName);
			helper = new FlyingHelper(settings);
			try {
				if (settings.alwaysShowPin()) {
					helper.installWithPinShownAlways(decor);
				} else {
					helper.install(decor);
				}
			} catch (Throwable t) {
				FA.logE(t);
				helper = null;
			}
			if (v != null) {
				v.requestFocus();
			}
		} else {
			FA.logD(packageName + " is contained blacklist.");
		}
		if (helper != null) {
			resetBackground(activity);
		}
		XposedHelpers.setAdditionalInstanceField(activity, FA_HANDLED, true);
	}

	private boolean isTabContent(ViewParent v) {
		while (v != null) {
			if (v instanceof TabHost) {
				return true;
			}
			v = v.getParent();
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private void resetBackground(Activity activity) {
		// force set window background for clear background.
		// ColorDrawable and a certain View
		View decor = activity.getWindow().peekDecorView();
		Drawable drawable = decor.getBackground();
		if (drawable == null || drawable instanceof ColorDrawable) {
			TypedArray a = activity.getTheme().obtainStyledAttributes(
					new int[] { android.R.attr.windowBackground });
			int background = a.getResourceId(0, 0);
			a.recycle();
			if (background != 0) {
				activity.getWindow().setBackgroundDrawableResource(background);
				drawable = decor.getBackground();
				if (drawable instanceof ColorDrawable
						&& ((ColorDrawable) drawable).getAlpha() == 0xFF) {
					FlyingHelper helper = FlyingHelper.getFrom(decor);
					helper.getFlyingLayout().setBackgroundDrawable(drawable);
				}
			} else {
				FA.logD("window background is 0.");
				if (drawable == null) {
					drawable = new ColorDrawable(activity.getResources()
							.getColor(android.R.color.background_dark));
					FlyingHelper helper = FlyingHelper.getFrom(decor);
					helper.getFlyingLayout().setBackgroundDrawable(drawable);
				} else if (((ColorDrawable) drawable).getAlpha() == 0xFF) {
					FlyingHelper helper = FlyingHelper.getFrom(decor);
					helper.getFlyingLayout().setBackgroundDrawable(drawable);
				}
			}
		}
	}

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
								installToActivity(activity);
							} catch (Throwable t) {
								FA.logE(t);
							}
						}
					});

			{
				XC_MethodHook afterSetContentView = new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						try {
							Activity activity = (Activity) param.thisObject;
							Boolean registered = (Boolean) XposedHelpers
									.getAdditionalInstanceField(activity,
											FA_REGISTERED);
							if (registered != null) {
								Boolean handled = (Boolean) XposedHelpers
										.getAdditionalInstanceField(activity,
												FA_HANDLED);
								if (handled == null) {
									FA.logD("install after set content view.");
									installToActivity(activity);
								}
							}
						} catch (Throwable t) {
							FA.logE(t);
						}
					}
				};
				XposedHelpers.findAndHookMethod(Activity.class,
						"setContentView", int.class, afterSetContentView);
				XposedHelpers.findAndHookMethod(Activity.class,
						"setContentView", View.class, afterSetContentView);
				XposedHelpers.findAndHookMethod(Activity.class,
						"setContentView", View.class,
						ViewGroup.LayoutParams.class, afterSetContentView);
			}

			final BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Activity activity = (Activity) context;
					FlyingHelper helper = FlyingHelper.getFrom(activity
							.getWindow().peekDecorView());
					if (helper != null) {
						helper.toggle();
					} else {
						FA.logD("FlyingHelper is not found.");
					}
				}
			};

			XposedHelpers.findAndHookMethod(Activity.class, "onResume",
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								Activity activity = (Activity) param.thisObject;
								Boolean registered = (Boolean) XposedHelpers
										.getAdditionalInstanceField(activity,
												FA_REGISTERED);
								if (registered == null) {
									activity.registerReceiver(toggleReceiver,
											new IntentFilter(ACTION_TOGGLE));
									XposedHelpers.setAdditionalInstanceField(
											activity, FA_REGISTERED, true);
								} else {
									FA.logD("already registered.");
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
								Boolean registered = (Boolean) XposedHelpers
										.getAdditionalInstanceField(activity,
												FA_REGISTERED);
								if (registered != null) {
									activity.unregisterReceiver(toggleReceiver);
									XposedHelpers
											.removeAdditionalInstanceField(
													activity, FA_REGISTERED);
								} else {
									FA.logD("not registered.");
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
										.getFrom(activity.getWindow()
												.peekDecorView());
								if (helper != null) {
									FlyingLayoutF flyingView = helper
											.getFlyingLayout();
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
										ViewGroup decor = (ViewGroup) dialog
												.getWindow().peekDecorView();
										FlyingHelper helper = FlyingHelper
												.getFrom(decor);
										if (helper == null) {
											// save / restore current focus
											View v = dialog.getCurrentFocus();
											helper = new FlyingHelper(settings);
											helper.installForFloatingWindow(decor);
											if (v != null) {
												v.requestFocus();
											}
										}
									} else {
										FA.logD(packageName
												+ " is contained blacklist.");
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
