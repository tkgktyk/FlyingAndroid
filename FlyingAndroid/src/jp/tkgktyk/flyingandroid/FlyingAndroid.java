package jp.tkgktyk.flyingandroid;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;

import java.util.Collections;
import java.util.Set;

import jp.tkgktyk.flyingandroid.FlyingView.OnUnhandledClickListener;
import jp.tkgktyk.flyingandroid.VerticalDragDetectorView.OnDraggedListener;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class FlyingAndroid implements IXposedHookLoadPackage,
		IXposedHookZygoteInit {
	public static final String PACKAGE_NAME = FlyingAndroid.class.getPackage()
			.getName();
	public static final String ACTION_TOGGLE = PACKAGE_NAME + ".ACTION_TOGGLE";

	private static XSharedPreferences sPref;
	private static float sSpeed;
	private static int sTakeoffPosition;
	private static boolean sShowToast;
	private static boolean sNotifyFlying;
	private static Set<String> sBlackSet;

	private static Drawable sEraseDrawable;
	private static Drawable sNotifyFlyingDrawable;

	@Override
	public void initZygote(StartupParam startupParam) {
		sPref = new XSharedPreferences(PACKAGE_NAME);
		reloadPreferences();
	}

	/**
	 * cannot call static functions from other process such as MainActivity.
	 */
	public void reloadPreferences() {
		sPref.reload();
		sSpeed = Float.parseFloat(sPref.getString("pref_key_speed", "1.5"));
		sTakeoffPosition = Integer.parseInt(sPref.getString(
				"pref_key_takeoff_position", "0"));
		sShowToast = sPref.getBoolean("pref_key_show_toast", true);
		sNotifyFlying = sPref.getBoolean("pref_key_notify_flying", true);
		sBlackSet = sPref.getStringSet("pref_key_black_list",
				Collections.<String> emptySet());
	}

	private float getSpeed() {
		return sSpeed;
	}

	private int getTakeoffPosition() {
		return sTakeoffPosition;
	}

	private boolean getShowToast() {
		return sShowToast;
	}

	private boolean getNotifyFlying() {
		return sNotifyFlying;
	}

	private Set<String> getBlackSet() {
		return sBlackSet;
	}

	private void log(String text) {
		if (BuildConfig.DEBUG) {
			XposedBridge.log("FA: " + text);
		}
	}

	private FlyingView newFlyingView(Context context) throws Throwable {
		final FlyingView flyingView = new FlyingView2(context);
		flyingView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		flyingView.setSpeed(getSpeed());
		final Context flyContext = context.createPackageContext(PACKAGE_NAME,
				Context.CONTEXT_IGNORE_SECURITY);
		final int padding = flyContext.getResources().getDimensionPixelSize(
				R.dimen.flying_view_padding);
		flyingView.setHorizontalPadding(padding);
		flyingView.setVerticalPadding(padding);
		flyingView.setIgnoreTouchEvent(true);
		flyingView.setOnUnhandledClickListener(new OnUnhandledClickListener() {
			@Override
			public void onUnhandledClick(FlyingView v, int x, int y) {
				boolean inside = false;
				for (int i = 0; i < v.getChildCount(); ++i) {
					boolean in = false;
					View child = v.getChildAt(i);
					if (x >= child.getLeft() && x <= child.getRight()) {
						if (y >= child.getTop() && y <= child.getBottom()) {
							in = true;
						}
					}
					inside = (inside || in);
				}
				if (!inside) {
					// log("unhandled outside click");
					final BroadcastReceiver receiver = new ToggleReceiver(v);
					receiver.onReceive(v.getContext(), null);
				}
			}
		});
		return flyingView;
	}

	private FlyingView findFlyingView(Activity activity) {
		return findFlyingView((ViewGroup) activity.getWindow().getDecorView());
	}

	private FlyingView findFlyingView(ViewGroup decor) {
		FlyingView flyingView = null;
		for (int i = 0; i < decor.getChildCount(); ++i) {
			final View v = decor.getChildAt(i);
			if (v instanceof VerticalDragDetectorView) {
				flyingView = (FlyingView) ((ViewGroup) v).getChildAt(0);
				break;
			}
		}
		return flyingView;
	}

	private ViewGroup getContainer(FlyingView flyingView) {
		return (ViewGroup) flyingView.getChildAt(0);
	}

	private View newChildView(ViewGroup decor, View child,
			ViewGroup.LayoutParams layoutParams, boolean verticalDrag)
			throws Throwable {
		FlyingView flyingView = findFlyingView(decor);
		if (flyingView != null) {
			// add child at index excluded notifyFlyingView
			ViewGroup container = getContainer(flyingView);
			container.addView(child, container.getChildCount() - 1,
					layoutParams);
			return null;
		} else {
			final Context context = decor.getContext();
			flyingView = newFlyingView(context);
			ViewGroup container = new FrameLayout(context);
			flyingView.addView(container);
			container.addView(child, layoutParams);
			// notify flying and vertical drag view
			View notifyFlyingView = new FrameLayout(context);
			container.addView(notifyFlyingView);
			VerticalDragDetectorView dragView = new VerticalDragDetectorView(
					context);
			dragView.setOnDraggedListener(new OnDraggedListener() {
				@Override
				public void onDragged(VerticalDragDetectorView v) {
					// log("dragged");
					final BroadcastReceiver receiver = new ToggleReceiver(
							(FlyingView) v.getChildAt(0));
					receiver.onReceive(v.getContext(), null);
				}
			});
			dragView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
			dragView.addView(flyingView);
			dragView.setIgnoreTouchEvent(verticalDrag);
			return dragView;
		}
	}

	private class ToggleReceiver extends BroadcastReceiver {
		private final FlyingView mFlyingView;

		public ToggleReceiver(FlyingView flyingView) {
			super();
			mFlyingView = flyingView;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			// log("toggle");
			final boolean next = !mFlyingView.getIgnoreTouchEvent();
			mFlyingView.setIgnoreTouchEvent(next);
			String text = null;
			if (next) {
				text = "Rest";
				mFlyingView.returnToHome();
				if (sEraseDrawable == null) {
					try {
						final Context flyContext = context
								.createPackageContext(PACKAGE_NAME,
										Context.CONTEXT_IGNORE_SECURITY);
						sEraseDrawable = flyContext.getResources().getDrawable(
								R.drawable.erase_flying_notification);
					} catch (Throwable t) {
						XposedBridge.log(t);
					}
				}
				ViewGroup container = getContainer(mFlyingView);
				container.getChildAt(container.getChildCount() - 1)
						.setBackgroundDrawable(sEraseDrawable);
			} else {
				text = "Fly";
				switch (getTakeoffPosition()) {
				case 0: // center
					// do noting
					break;
				case 1: // bottom
					mFlyingView.move(0,
							Math.round(mFlyingView.getHeight() / 2.0f));
					break;
				case 2: // lower left
					mFlyingView.move(
							Math.round(-mFlyingView.getWidth() / 2.0f),
							Math.round(mFlyingView.getHeight() / 2.0f));
					break;
				case 3: // lower right
					mFlyingView.move(Math.round(mFlyingView.getWidth() / 2.0f),
							Math.round(mFlyingView.getHeight() / 2.0f));
					break;
				}
				if (getNotifyFlying()) {
					if (sNotifyFlyingDrawable == null) {
						try {
							final Context flyContext = context
									.createPackageContext(PACKAGE_NAME,
											Context.CONTEXT_IGNORE_SECURITY);
							sNotifyFlyingDrawable = flyContext.getResources()
									.getDrawable(R.drawable.notify_flying);
						} catch (Throwable t) {
							XposedBridge.log(t);
						}
					}
					ViewGroup container = getContainer(mFlyingView);
					container.getChildAt(container.getChildCount() - 1)
							.setBackgroundDrawable(sNotifyFlyingDrawable);
				}
			}
			if (getShowToast())
				Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
		}
	};

	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		try {
			// reloadPreferences();

			if (getBlackSet().contains(lpparam.packageName)) {
				return;
			}

			findAndHookMethod(ViewGroup.class, "addView", View.class,
					ViewGroup.LayoutParams.class, new XC_MethodReplacement() {
						@Override
						protected Object replaceHookedMethod(
								MethodHookParam param) {
							try {
								final View child = (View) param.args[0];
								final ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams) param.args[1];
								// class.getName() equals
								// "com.android.internal.policy.impl.PhoneWindow$DecorView"
								// class.getCanonicalName() equals
								// "com.android.internal.policy.impl.PhoneWindow.DecorView"
								if (!param.thisObject
										.getClass()
										.getCanonicalName()
										.equals("com.android.internal.policy.impl.PhoneWindow.DecorView")) {
									// don't touch original method
									// to avoid stack overflow (recursive),
									// call addView(View, int, LayoutParams)
									callMethod(param.thisObject, "addView",
											child, -1, layoutParams);
								} else {
									final ViewGroup decor = (ViewGroup) param.thisObject;
									// vertical drag interface is enabled on
									// floating window only.
									TypedArray a = decor
											.getContext()
											.getTheme()
											.obtainStyledAttributes(
													new int[] { android.R.attr.windowIsFloating });
									final View newChild = newChildView(decor,
											child, layoutParams,
											!a.getBoolean(0, false));
									a.recycle();
									if (newChild != null) {
										// to avoid stack overflow (recursive),
										// call addView(View, int, LayoutParams)
										callMethod(param.thisObject, "addView",
												newChild, -1,
												newChild.getLayoutParams());
									} else {
										// already exists newChild and child has
										// been added to newChild.
									}
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
							return null;
						}
					});
			findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					try {
						final Activity activity = (Activity) param.thisObject;
						final Object r = getAdditionalInstanceField(activity,
								"flyingAndroidReceiver");
						if (r == null) {
							FlyingView flyingView = findFlyingView(activity);
							if (flyingView != null) {
								final BroadcastReceiver receiver = new ToggleReceiver(
										flyingView);
								activity.registerReceiver(receiver,
										new IntentFilter(ACTION_TOGGLE));
								setAdditionalInstanceField(activity,
										"flyingAndroidReceiver", receiver);
								// log("register");
							} else {
								log("FlyingView is not found.");
							}
						}
					} catch (Throwable t) {
						XposedBridge.log(t);
					}
				}
			});
			findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					try {
						final Activity activity = (Activity) param.thisObject;
						final Object r = getAdditionalInstanceField(activity,
								"flyingAndroidReceiver");
						if (r != null) {
							activity.unregisterReceiver((BroadcastReceiver) r);
							setAdditionalInstanceField(activity,
									"flyingAndroidReceiver", null);
							// log("unregister");
						}
					} catch (Throwable t) {
						XposedBridge.log(t);
					}
				}
			});
			findAndHookMethod(Activity.class, "onConfigurationChanged",
					Configuration.class, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								Activity activity = (Activity) param.thisObject;
								FlyingView flyingView = findFlyingView(activity);
								if (flyingView != null) {
									Configuration newConfig = (Configuration) param.args[0];
									if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
										flyingView.rotate();
									} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
										flyingView.rotate();
									}
								} else {
									log("FlyingView is not found.");
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
}
