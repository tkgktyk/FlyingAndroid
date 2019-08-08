package jp.tkgktyk.flyingandroid;

import jp.tkgktyk.flyinglayout.FlyingLayout;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;
import android.widget.TabHost;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class FlyingAndroid implements IXposedHookZygoteInit,
		IXposedHookLoadPackage {
	private static final String FA_HANDLED = "FA_handled";
	private static final String FA_REGISTERED = "FA_registered";

	private static XSharedPreferences sPref;

	@Override
	public void initZygote(StartupParam startupParam) {
		sPref = new XSharedPreferences(FA.PACKAGE_NAME);
		sPref.makeWorldReadable();

		try {
			hooksForActivity();
			hooksForDialog();
			hooksForInputMethod();
			hooksForPopupWindow();
		} catch (Throwable t) {
			FA.logE(t);
		}
	}

	private void hooksForPopupWindow(){

		XC_MethodHook createReplace = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				ViewGroup decor = (ViewGroup) XposedBridge.invokeOriginalMethod(param.method,
						param.thisObject, param.args);

				FA.Settings settings = new FA.Settings(sPref);
				String packageName = decor.getContext().getPackageName();
				FlyingHelper helper = null;
				if (!settings.blackSet.contains(packageName)) {
					// save / restore current focus
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
				} else {
					FA.logD(packageName + " is contained blacklist.");
				}

				return decor;
			}
		};

		XposedBridge.hookAllMethods(PopupWindow.class, "createDecorView", createReplace);
	}
	private void hooksForActivity() {

		XC_MethodHook replaceAddView = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				if (param.thisObject
						.getClass()
						.getName()
						.equals("com.android.internal.policy.DecorView")) {
					ViewGroup decor = (ViewGroup) param.thisObject;
					FlyingLayout flyingLayout = null;
					ViewGroup viewToAdd = null;
					try {
						 viewToAdd = (ViewGroup) param.args[0]; // possible to crash and reset
						 View v = viewToAdd.getChildAt(0);
						 if(v.getClass()
								 .getName()
								 .equals("jp.tkgktyk.flyinglayout.FlyingLayout")) {
							flyingLayout = (FlyingLayout) v;
						 }
					}
					catch (Throwable t) {
						// Do nothing
					}

					if (flyingLayout != null) {
						viewToAdd.removeViewAt(0);
						decor.addView(flyingLayout);
						ViewGroup container = (ViewGroup) flyingLayout.getChildAt(0);
						List<View> containerChildren = new ArrayList<View>();
						List<View> viewToAddChildren = new ArrayList<View>();
						for (int i = 0; i < container.getChildCount(); ++i) {
							containerChildren.add(container.getChildAt(i));
						}
						for (int i = 0; i < viewToAdd.getChildCount(); ++i) {
							viewToAddChildren.add(viewToAdd.getChildAt(i));
						}
						viewToAdd.removeAllViews();
						container.removeAllViews();
						for (View v : containerChildren) {
							viewToAdd.addView(v, v.getLayoutParams());
						}
						for (View v : viewToAddChildren) {
							viewToAdd.addView(v, v.getLayoutParams());
						}
						container.addView(viewToAdd);
						return null;
					}
				}
				return XposedBridge.invokeOriginalMethod(param.method,
						param.thisObject, param.args);
			}
		};
		XposedBridge.hookAllMethods(ViewGroup.class, "addView", replaceAddView);

		XposedHelpers.findAndHookMethod(View.class, "setBackgroundDrawable",
				Drawable.class, new XC_MethodReplacement() {
					@Override
					protected Object replaceHookedMethod(MethodHookParam param)
							throws Throwable {
						Object[] args = null;
						if (param.thisObject
								.getClass()
								.getName()
								.equals("com.android.internal.policy.impl.PhoneWindow$DecorView")) {
							View v = (View) param.thisObject;
							FlyingHelper helper = FlyingHelper.getFrom(v);
							if (helper != null) {
								Drawable d = (Drawable) param.args[0];
								Context context = v.getContext();
								Drawable dark = context
										.getResources()
										.getDrawable(
												android.R.drawable.screen_background_dark);
								if (d == null) {
									helper.getFlyingLayout()
											.setBackgroundDrawable(dark);
								} else if (d.getOpacity() == PixelFormat.OPAQUE) {
									helper.getFlyingLayout()
											.setBackgroundDrawable(d);
								}
								args = new Object[] { dark };
							}
						}
						if (args == null) {
							args = param.args;
						}
						XposedBridge.invokeOriginalMethod(param.method,
								param.thisObject, param.args);
						return null;
					}
				});
		XposedHelpers.findAndHookMethod(Activity.class, "onPostCreate",
				Bundle.class, new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						try {
							installToActivity((Activity) param.thisObject);
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
			XposedHelpers.findAndHookMethod(Activity.class, "setContentView",
					int.class, afterSetContentView);
			XposedHelpers.findAndHookMethod(Activity.class, "setContentView",
					View.class, afterSetContentView);
			XposedHelpers.findAndHookMethod(Activity.class, "setContentView",
					View.class, ViewGroup.LayoutParams.class,
					afterSetContentView);
		}

		final BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Activity activity = (Activity) context;
				if (!isTabContent(activity)) {
					View decor = activity.getWindow().peekDecorView();
					FlyingHelper helper = FlyingHelper.getFrom(decor);
					if (helper != null) {
						FA.Settings settings = helper.getSettings();
						String action = intent.getAction();
						if (action.equals(FA.ACTION_TOGGLE)) {
							InitialPosition pos = new InitialPosition(
									settings.initialXp, settings.initialYp);
							try {
								XposedHelpers.callMethod(activity,
										"onToggleFlyingMode", pos.getX(decor),
										pos.getY(decor));
								FA.logD(FA.ACTION_TOGGLE + " is overridden");
							} catch (NoSuchMethodError e) {
								helper.toggle();
							}
						} else if (action.equals(FA.ACTION_RESET)) {
							try {
								XposedHelpers.callMethod(activity,
										"onResetFlyingMode");
								FA.logD(FA.ACTION_RESET + " is overridden");
							} catch (NoSuchMethodError e) {
								helper.resetState();
							}
						} else if (action.equals(FA.ACTION_TOGGLE_PIN)) {
							InitialPosition pos = new InitialPosition(
									settings.initialXp, settings.initialYp);
							try {
								XposedHelpers.callMethod(activity,
										"onToggleFlyingPin", pos.getX(decor),
										pos.getY(decor));
								FA.logD(FA.ACTION_TOGGLE_PIN + " is overridden");
							} catch (NoSuchMethodError e) {
								helper.togglePin();
							}
						}
					} else {
						FA.logD("FlyingHelper is not found.");
					}
				} else {
					FA.logD(activity.getLocalClassName() + " is a tab content.");
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
								IntentFilter filter = new IntentFilter();
								filter.addAction(FA.ACTION_TOGGLE);
								filter.addAction(FA.ACTION_RESET);
								filter.addAction(FA.ACTION_TOGGLE_PIN);
								activity.registerReceiver(toggleReceiver,
										filter);
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
								XposedHelpers.removeAdditionalInstanceField(
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
							FlyingHelper helper = FlyingHelper.getFrom(activity
									.getWindow().peekDecorView());
							if (helper != null) {
								FlyingLayout flyingLayout = helper
										.getFlyingLayout();
								Configuration newConfig = (Configuration) param.args[0];
								if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
									flyingLayout.rotate();
								} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
									flyingLayout.rotate();
								}
							} else {
								FA.logD("FlyingHelper is not found.");
							}
						} catch (Throwable t) {
							FA.logE(t);
						}
					}
				});
	}

	private void installToActivity(Activity activity) {
		ViewGroup decor = (ViewGroup) activity.getWindow().peekDecorView();
		if (decor == null) {
			FA.logD("decorView is null.");
			return;
		}
		String packageName = activity.getPackageName();
		if (packageName.equals(FA.PACKAGE_NAME)) {
			FA.logD("ignore own activity.");
			return;
		}
		FA.Settings settings = new FA.Settings(sPref);
		FA.logD("reload settings at " + activity.getLocalClassName() + "@"
				+ packageName);
		FlyingHelper helper = null;
		if (!settings.blackSet.contains(packageName)) {
			// save / restore current focus
			View focus = activity.getCurrentFocus();
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
			if (focus != null) {
				focus.requestFocus();
			}
		} else {
			FA.logD(packageName + " is contained blacklist.");
		}
		// set false because window background isn't reset yet.
		XposedHelpers.setAdditionalInstanceField(activity, FA_HANDLED,
				Boolean.FALSE);
	}

	private boolean isTabContent(Activity activity) {
		View decor = activity.getWindow().peekDecorView();
		if (decor != null) {
			for (ViewParent v = decor.getParent(); v != null; v = v.getParent()) {
				FA.logD(v.getClass().getName());
				if (v instanceof TabHost) {
					return true;
				}
			}
		}
		return false;
	}

	private void hooksForDialog() {
		XposedHelpers.findAndHookMethod(Dialog.class, "onAttachedToWindow",
				new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param)
							throws Throwable {
						try {
							installToDialog((Dialog) param.thisObject);
						} catch (Throwable t) {
							FA.logE(t);
						}
					}

					private void installToDialog(Dialog dialog) {
						FA.Settings settings = new FA.Settings(sPref);
						if (settings.flyingDialog) {
							String packageName = dialog.getContext()
									.getPackageName();
							if (!settings.blackSet.contains(packageName)) {
								ViewGroup decor = (ViewGroup) dialog
										.getWindow().peekDecorView();
								FlyingHelper helper = FlyingHelper
										.getFrom(decor);
								if (helper == null) {
									// save / restore current focus
									View v = dialog.getCurrentFocus();
									helper = new FlyingHelper(settings);
									try {
										helper.installForFloatingWindow(decor);
									} catch (Throwable t) {
										FA.logE(t);
										helper = null;
									}
									if (v != null) {
										v.requestFocus();
									}
								}
							} else {
								FA.logD(packageName
										+ " is contained blacklist.");
							}
						}
					}
				});
	}

	private void hooksForInputMethod() {
		XC_MethodHook onSoftInputShown = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
					throws Throwable {
				try {
					FA.logD(param.method.getName());
					Context context = ((View) XposedHelpers.getObjectField(
							param.thisObject, "mCurRootView")).getContext();
					context.sendBroadcast(new Intent(FA.ACTION_RESET));
				} catch (Throwable t) {
					FA.logE(t);
				}
			}
		};
		// for general EditText
		XposedHelpers.findAndHookMethod(InputMethodManager.class,
				"showSoftInput", View.class, int.class, ResultReceiver.class,
				onSoftInputShown);
		// for SearchView (on ActionBar)
		XposedHelpers.findAndHookMethod(InputMethodManager.class,
				"showSoftInputUnchecked", int.class, ResultReceiver.class,
				onSoftInputShown);
		// for another case??? just to be sure
		XposedHelpers.findAndHookMethod(InputMethodManager.class,
				"showSoftInputFromInputMethod", IBinder.class, int.class,
				onSoftInputShown);
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		try {
			if (!lpparam.packageName.equals("com.android.systemui")) {
				return;
			}
			hookForFlyingLayout(lpparam);
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

	private void hookForFlyingLayout(LoadPackageParam lpparam) {
		try {
			XposedHelpers.findAndHookMethod(FlyingLayout.class.getName(),
					lpparam.classLoader, "overwriteSpeedByXposed",
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							if (param.thisObject.getClass().getName()
									.equals(FlyingLayout.class.getName())) {
								FA.Settings settings = new FA.Settings(
										sPref);
								XposedHelpers.callMethod(param.thisObject,
										"setSpeed", settings.speed);
							}
						}
					});
		} catch (ClassNotFoundError e) {
			// this package doesn't have FlyingLayout
		}
	}
}
