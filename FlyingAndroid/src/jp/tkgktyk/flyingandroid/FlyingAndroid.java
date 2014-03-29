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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;
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
	private static int sPinPosition;

	private static final int TAKE_OFF_POSITION_CENTER = 0;
	private static final int TAKE_OFF_POSITION_BOTTOM = 1;
	private static final int TAKE_OFF_POSITION_LOWER_LEFT = 2;
	private static final int TAKE_OFF_POSITION_LOWER_RIGHT = 3;

	@Override
	public void initZygote(StartupParam startupParam) {
		sPref = new XSharedPreferences(PACKAGE_NAME);
		reloadPreferences();

		//
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
		sPinPosition = Integer.parseInt(sPref.getString(
				"pref_key_pin_position", "0"));
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

	private int getPinPosition() {
		return sPinPosition;
	}

	private void log(String text) {
		if (BuildConfig.DEBUG) {
			XposedBridge.log("FA: " + text);
		}
	}

	private ToggleHelper getToggleHelper(FlyingView flyingView) {
		return (ToggleHelper) flyingView.getTag();
	}

	private FlyingView createFlyingView(Context context, boolean useOverlay)
			throws Throwable {
		final FlyingView flyingView = new FlyingView1(context);
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
		final ToggleHelper helper = new ToggleHelper(flyingView, useOverlay);
		flyingView.setTag(helper);
		flyingView.setOnUnhandledClickListener(new OnUnhandledClickListener() {
			@Override
			public void onUnhandledClick(FlyingView v, int x, int y) {
				boolean inside = false;
				// for (int i = 0; i < v.getChildCount(); ++i) {
				boolean in = false;
				// View child = v.getChildAt(i);
				View child = v.getChildAt(0);
				if (x >= child.getLeft() && x <= child.getRight()) {
					if (y >= child.getTop() && y <= child.getBottom()) {
						in = true;
					}
				}
				inside = (inside || in);
				// }
				if (!inside) {
					// log("unhandled outside click");
					helper.toggle();
				}
			}
		});
		return flyingView;
	}

	private FlyingView findFlyingView(Activity activity) {
		return findFlyingView((ViewGroup) activity.getWindow().getDecorView());
	}

	private FlyingView findFlyingView(ViewGroup decor) {
		for (int i = 0; i < decor.getChildCount(); ++i) {
			View v = decor.getChildAt(i);
			if (v instanceof FlyingView) {
				return (FlyingView) v;
			}
			if (v instanceof ViewGroup) {
				return findFlyingView((ViewGroup) v);
			}
		}
		return null;
	}

	private FrameLayout getContainer(FlyingView flyingView) {
		return (FrameLayout) flyingView.getChildAt(0);
	}

	private VerticalDragDetectorView createDragView(Context context,
			final ToggleHelper helper, boolean verticalDrag) {
		VerticalDragDetectorView dragView = new VerticalDragDetectorView(
				context);
		dragView.setOnDraggedListener(new OnDraggedListener() {
			@Override
			public void onDragged(VerticalDragDetectorView v) {
				// log("dragged");
				helper.toggle();
			}
		});
		dragView.setIgnoreTouchEvent(!verticalDrag);

		return dragView;
	}

	private View createNewChildView(Context context, boolean verticalDrag,
			boolean useOverlay) throws Throwable {
		// create FlyingView
		FlyingView flyingView = createFlyingView(context, useOverlay);
		ToggleHelper helper = getToggleHelper(flyingView);
		// add dragView as FlyingView's container
		ViewGroup container = createDragView(context, helper, verticalDrag);
		flyingView.addView(container);

		return flyingView;
	}

	private void addViewToFlyingView(FlyingView flyingView, View child,
			ViewGroup.LayoutParams layoutParams) {
		getContainer(flyingView).addView(child, layoutParams);
	}

	private class ToggleHelper {
		private final FlyingView mFlyingView;
		private final Drawable mNotifyFlyingDrawable;
		private final boolean mUseOverlay;

		private boolean mFlying = false;
		private boolean mWindowPinned = false;
		private boolean mOverlayShown = false;
		private boolean mBoundaryShown = false;

		public ToggleHelper(FlyingView flyingView, boolean useOverlay)
				throws Throwable {
			mFlyingView = flyingView;
			mUseOverlay = useOverlay;

			if (!getNotifyFlying()) {
				mNotifyFlyingDrawable = null;
			} else {
				final Context context = mFlyingView.getContext();
				Context flyContext = null;
				try {
					flyContext = context.createPackageContext(PACKAGE_NAME,
							Context.CONTEXT_IGNORE_SECURITY);
				} catch (Throwable t) {
					XposedBridge.log(t);
				}
				if (flyContext != null) {
					mNotifyFlyingDrawable = flyContext.getResources()
							.getDrawable(R.drawable.notify_flying);
				} else {
					mNotifyFlyingDrawable = null;
				}
			}
		}

		private View createOverlayView(int pinPosition) {
			final Context context = mFlyingView.getContext();
			if (pinPosition == 0) {
				return new View(context);
			}

			View overlay;
			try {
				final Context flyContext = context.createPackageContext(
						PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
				overlay = LayoutInflater.from(flyContext).inflate(
						R.layout.view_overlay, null);
				ToggleButton button = (ToggleButton) overlay
						.findViewById(R.id.button);
				button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						setWindowPinned(isChecked);
					}
				});
				LinearLayout container = (LinearLayout) overlay
						.findViewById(R.id.container);
				switch (pinPosition) {
				case 1:
					container
							.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
					break;
				case 2:
					container.setGravity(Gravity.CENTER_VERTICAL
							| Gravity.RIGHT);
					break;
				case 3:
					container.setGravity(Gravity.BOTTOM | Gravity.LEFT);
					break;
				case 4:
					container.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
					break;
				}
			} catch (Throwable t) {
				XposedBridge.log(t);
				overlay = new View(context);
			}

			return overlay;
		}

		private void setWindowPinned(boolean pinned) {
			if (mWindowPinned == pinned) {
				return;
			}
			mWindowPinned = pinned;

			String text;
			if (pinned) {
				text = "pin";
				mFlyingView.setIgnoreTouchEvent(true);
				setBoundaryShown(false);
			} else {
				text = "Unpin";
				mFlyingView.setIgnoreTouchEvent(false);
				setBoundaryShown(true);
			}
			if (getShowToast()) {
				Toast.makeText(mFlyingView.getContext(), text,
						Toast.LENGTH_SHORT).show();
			}
		}

		private void setOverlayShown(boolean shown) {
			if (mOverlayShown == shown) {
				return;
			}
			mOverlayShown = shown;

			if (mUseOverlay && getPinPosition() != 0) {
				if (shown) {
					mFlyingView.addView(createOverlayView(getPinPosition()));
				} else {
					mFlyingView.removeViewAt(mFlyingView.getChildCount() - 1);
				}
			}
		}

		private void setBoundaryShown(boolean shown) {
			if (mBoundaryShown == shown) {
				return;
			}
			mBoundaryShown = shown;

			if (getNotifyFlying()) {
				FrameLayout container = getContainer(mFlyingView);
				if (shown) {
					container.setForeground(mNotifyFlyingDrawable);
				} else {
					container.setForeground(null);
				}
			}
		}

		private void toggle() {
			// log("toggle");
			mFlying = !mFlying;
			mFlyingView.setIgnoreTouchEvent(!mFlying);
			String text = null;
			if (!mFlying) {
				text = "Rest";
				mFlyingView.returnToHome();
				setBoundaryShown(false);
				setOverlayShown(false);
			} else {
				text = "Fly";
				switch (getTakeoffPosition()) {
				case TAKE_OFF_POSITION_CENTER:
					// do noting
					break;
				case TAKE_OFF_POSITION_BOTTOM:
					mFlyingView.move(0,
							Math.round(mFlyingView.getHeight() / 2.0f));
					break;
				case TAKE_OFF_POSITION_LOWER_LEFT:
					mFlyingView.move(
							Math.round(-mFlyingView.getWidth() / 2.0f),
							Math.round(mFlyingView.getHeight() / 2.0f));
					break;
				case TAKE_OFF_POSITION_LOWER_RIGHT:
					mFlyingView.move(Math.round(mFlyingView.getWidth() / 2.0f),
							Math.round(mFlyingView.getHeight() / 2.0f));
					break;
				}
				mWindowPinned = false;
				setBoundaryShown(true);
				setOverlayShown(true);
			}
			if (getShowToast()) {
				Toast.makeText(mFlyingView.getContext(), text,
						Toast.LENGTH_SHORT).show();
			}
		}

		private final BroadcastReceiver mToggleReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// log("toggle");
				toggle();
			}
		};

		private BroadcastReceiver getToggleReceiver() {
			return mToggleReceiver;
		}
	}

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
									FlyingView flyingView = findFlyingView(decor);
									if (flyingView == null) {
										// vertical drag interface is enabled on
										// floating window only.
										TypedArray a = decor
												.getContext()
												.getTheme()
												.obtainStyledAttributes(
														new int[] { android.R.attr.windowIsFloating });
										boolean floating = a.getBoolean(0,
												false);
										final View newChild = createNewChildView(
												decor.getContext(), floating,
												!floating);
										a.recycle();
										// to avoid stack overflow (recursive),
										// call addView(View, int, LayoutParams)
										callMethod(param.thisObject, "addView",
												newChild, -1,
												newChild.getLayoutParams());
										flyingView = findFlyingView(decor);
									}
									addViewToFlyingView(flyingView, child,
											layoutParams);
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
							return null;
						}
					});
			findAndHookMethod(Activity.class, "onPostCreate", Bundle.class,
					new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param)
								throws Throwable {
							// for clear background
							Activity activity = (Activity) param.thisObject;
							activity.getWindow().setBackgroundDrawable(
									new ColorDrawable(0));
							TypedArray a = activity
									.getTheme()
									.obtainStyledAttributes(
											new int[] { android.R.attr.windowBackground });
							int background = a.getResourceId(0, 0);
							a.recycle();
							FlyingView flyingView = findFlyingView(activity);
							if (flyingView != null) {
								getContainer(flyingView).setBackgroundResource(
										background);
							} else {
								log("FlyingView is not found.");
							}
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
								final ToggleHelper helper = getToggleHelper(flyingView);
								final BroadcastReceiver receiver = helper
										.getToggleReceiver();
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
