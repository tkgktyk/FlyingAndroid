package jp.tkgktyk.flyingandroid;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;

import java.util.HashSet;
import java.util.Set;

import jp.tkgktyk.flyingandroid.FlyingView2.OnUnhandledClickListener;
import jp.tkgktyk.flyingandroid.VerticalDragDetectorView.OnDraggedListener;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
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

	private static float sSpeed;
	private static boolean sNotifyFlying;
	private static int sTakeoffPosition;
	private static Set<String> sBlackSet;

	private static Drawable sEraseDrawable;
	private static Drawable sNotifyFlyingDrawable;

	@Override
	public void initZygote(StartupParam startupParam) {
		reloadPreferences();
	}

	/**
	 * cannot call static functions from other process such as MainActivity.
	 */
	public static void reloadPreferences() {
		final XSharedPreferences pref = new XSharedPreferences(PACKAGE_NAME);

		sSpeed = Float.parseFloat(pref.getString("pref_key_speed", "1.5"));
		sNotifyFlying = pref.getBoolean("pref_key_notify_flying", true);
		sTakeoffPosition = Integer.parseInt(pref.getString(
				"pref_key_takeoff_position", "0"));
		sBlackSet = pref.getStringSet("pref_key_black_list",
				new HashSet<String>());
	}

	private ViewGroup newFlyingView(Context context) throws Throwable {
		final FlyingView2 flyingView = new FlyingView2(context);
		flyingView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		flyingView.setSpeed(sSpeed);
		final Context flyContext = context.createPackageContext(PACKAGE_NAME,
				Context.CONTEXT_IGNORE_SECURITY);
		final int padding = flyContext.getResources().getDimensionPixelSize(
				R.dimen.flying_view_padding);
		flyingView.setHorizontalPadding(padding);
		flyingView.setVerticalPadding(padding);
		flyingView.setIgnoreTouchEvent(true);
		flyingView.setOnUnhandledClickListener(new OnUnhandledClickListener() {
			@Override
			public void onUnhandledClick(FlyingView2 v, int x, int y) {
				boolean inside = false;
				for (int i = 0; i < v.getChildCount(); ++i) {
					boolean in = false;
					View child = v.getChildAt(i);
					if (x >= child.getLeft() && x <= child.getRight()) {
						if (y >= child.getTop() && y <= child.getBottom()) {
							in = true;
						}
					}
					inside = (inside && in);
				}
				if (!inside) {
					XposedBridge.log("outside unhandled click");
					v.getContext().sendBroadcast(new Intent(ACTION_TOGGLE));
				}
			}
		});
		return flyingView;
	}

	private View newChildView(ViewGroup decor, View child,
			ViewGroup.LayoutParams layoutParams) throws Throwable {
		ViewGroup flyingView = null;
		for (int i = 0; i < decor.getChildCount(); ++i) {
			final View v = decor.getChildAt(i);
			if (v instanceof VerticalDragDetectorView) {
				flyingView = (FlyingView2) ((ViewGroup) v).getChildAt(0);
				break;
			}
		}
		if (flyingView != null) {
			// add child at index excluded notifyFlyingView
			flyingView.addView(child, flyingView.getChildCount() - 1,
					layoutParams);
			return null;
		} else {
			final Context context = decor.getContext();
			flyingView = newFlyingView(context);
			flyingView.addView(child, layoutParams);
			// notify flying view
			View notifyFlyingView = new View(context);
			flyingView.addView(notifyFlyingView);
			// drag
			VerticalDragDetectorView dragView = new VerticalDragDetectorView(
					context);
			dragView.setOnDraggedListener(new OnDraggedListener() {
				@Override
				public void onDragged(VerticalDragDetectorView v) {
					XposedBridge.log("dragged");
					final BroadcastReceiver receiver = new ToggleReceiver(
							(FlyingView2) v.getChildAt(0));
					receiver.onReceive(v.getContext(), null);
				}
			});
			dragView.setLayoutParams(new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
			dragView.addView(flyingView);
			return dragView;
		}
	}

	private class ToggleReceiver extends BroadcastReceiver {
		private final FlyingView2 mFlyingView;

		public ToggleReceiver(FlyingView2 flyingView) {
			super();
			mFlyingView = flyingView;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			XposedBridge.log("toggle");
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
				mFlyingView.getChildAt(mFlyingView.getChildCount() - 1)
						.setBackgroundDrawable(sEraseDrawable);
			} else {
				text = "Fly";
				switch (sTakeoffPosition) {
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
				if (sNotifyFlying) {
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
					mFlyingView.getChildAt(mFlyingView.getChildCount() - 1)
							.setBackgroundDrawable(sNotifyFlyingDrawable);
				}
			}
			Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
		}
	};

	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		try {
			if (sBlackSet.contains(lpparam.packageName))
				return;

			// XposedBridge.log("Loaded app: " + lpparam.packageName);
			// XposedBridge.log("is first app: " + lpparam.isFirstApplication);
			findAndHookMethod("android.view.ViewGroup", lpparam.classLoader,
					"addView", View.class, ViewGroup.LayoutParams.class,
					new XC_MethodReplacement() {
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
									final View newChild = newChildView(decor,
											child, layoutParams);
									if (newChild != null) {
										// to avoid stack overflow (recursive),
										// call addView(View, int, LayoutParams)
										callMethod(param.thisObject, "addView",
												newChild, -1,
												newChild.getLayoutParams());
									} else {
										// already exists newChild and child
										// have been added to newChild.
									}
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
							return null;
						}
					});
			findAndHookMethod("android.app.Activity", lpparam.classLoader,
					"onResume", new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								final Activity activity = (Activity) param.thisObject;
								final Object r = getAdditionalInstanceField(
										activity, "flyingAndroidReceiver");
								if (r == null) {
									final ViewGroup decor = (ViewGroup) activity
											.getWindow().getDecorView();
									FlyingView2 flyingView = null;
									for (int i = 0; i < decor.getChildCount(); ++i) {
										final View v = decor.getChildAt(i);
										if (v instanceof VerticalDragDetectorView) {
											flyingView = (FlyingView2) ((ViewGroup) v)
													.getChildAt(0);
											break;
										}
									}
									if (flyingView != null) {
										final BroadcastReceiver receiver = new ToggleReceiver(
												flyingView);
										activity.registerReceiver(receiver,
												new IntentFilter(ACTION_TOGGLE));
										setAdditionalInstanceField(activity,
												"flyingAndroidReceiver",
												receiver);
										XposedBridge.log("register");
									} else {
										XposedBridge
												.log("FlyingView is not found.");
									}
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
						}
					});
			findAndHookMethod("android.app.Activity", lpparam.classLoader,
					"onPause", new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							try {
								final Activity activity = (Activity) param.thisObject;
								final Object r = getAdditionalInstanceField(
										activity, "flyingAndroidReceiver");
								if (r != null) {
									activity.unregisterReceiver((BroadcastReceiver) r);
									setAdditionalInstanceField(activity,
											"flyingAndroidReceiver", null);
									XposedBridge.log("unregister");
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
