package jp.tkgktyk.flyingandroid;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import jp.tkgktyk.flyingandroid.FlyingView2.OnUnhandledClickListener;
import jp.tkgktyk.flyingandroid.VerticalDragDetectorView.OnDraggedListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class FlyingAndroid implements IXposedHookLoadPackage,
		IXposedHookZygoteInit {
	public static final String PACKAGE_NAME = FlyingAndroid.class.getPackage()
			.getName();
	private static float sSpeed;
	private static boolean sShowDragArea;

	@Override
	public void initZygote(StartupParam startupParam) {
		XSharedPreferences pref = new XSharedPreferences(PACKAGE_NAME);

		sSpeed = Float.parseFloat(pref.getString("pref_key_speed", "1.5"));
		sShowDragArea = pref.getBoolean("pref_key_show_drag_area", true);
	}

	private ViewGroup newFlyingView(Context context) throws Throwable {
		FlyingView2 fly = new FlyingView2(context);
		fly.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		fly.setSpeed(sSpeed);
		final Context flyContext = context.createPackageContext(PACKAGE_NAME,
				Context.CONTEXT_IGNORE_SECURITY);
		final int padding = flyContext.getResources().getDimensionPixelSize(
				R.dimen.flying_view_padding);
		fly.setHorizontalPadding(padding);
		fly.setVerticalPadding(padding);
		fly.setIgnoreTouchEvent(true);
		fly.setOnUnhandledClickListener(new OnUnhandledClickListener() {
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
					v.returnToHome();
					v.setIgnoreTouchEvent(true);
					((VerticalDragDetectorView) v.getParent())
							.setIgnoreTouchEvent(false);
					Toast.makeText(v.getContext(), "Rest", Toast.LENGTH_SHORT)
							.show();
				}
			}
		});
		return fly;
	}

	private VerticalDragDetectorView newVerticalDragDetectorView(Context context) {
		VerticalDragDetectorView drag = new VerticalDragDetectorView(context);
		drag.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		drag.setOnDraggedListener(new OnDraggedListener() {
			@Override
			public void onDragged(VerticalDragDetectorView v) {
				FlyingView2 fly = ((FlyingView2) v.getChildAt(0));
				XposedBridge.log("toggle");
				boolean next = !fly.getIgnoreTouchEvent();
				fly.setIgnoreTouchEvent(next);
				String text = null;
				if (next) {
					XposedBridge.log("toggle: never reached");
					text = "Rest";
					fly.returnToHome();
					v.setIgnoreTouchEvent(false);
				} else {
					text = "Fly";
					v.setIgnoreTouchEvent(true);
				}
				Toast.makeText(v.getContext(), text, Toast.LENGTH_SHORT).show();
			}
		});
		return drag;
	}

	private View newDragAreaView(final Context context, int width) {
		final RelativeLayout container = new RelativeLayout(context);
		container.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		container.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@SuppressLint("NewApi")
					@SuppressWarnings("deprecation")
					@Override
					public void onGlobalLayout() {
						container.startAnimation(AnimationUtils.loadAnimation(
								context, android.R.anim.fade_out));
						container.setVisibility(View.GONE);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							container.getViewTreeObserver()
									.removeOnGlobalLayoutListener(this);
						} else {
							container.getViewTreeObserver()
									.removeGlobalOnLayoutListener(this);
						}
					}
				});
		// add left
		final View left = new View(context);
		left.setBackgroundColor(0xAEFFCBDB);
		final RelativeLayout.LayoutParams lpLeft = new RelativeLayout.LayoutParams(
				width, ViewGroup.LayoutParams.WRAP_CONTENT);
		lpLeft.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lpLeft.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		left.setLayoutParams(lpLeft);
		container.addView(left);
		// add right
		final View right = new View(context);
		right.setBackgroundColor(0xAEFFCBDB);
		final RelativeLayout.LayoutParams lpRight = new RelativeLayout.LayoutParams(
				width, ViewGroup.LayoutParams.WRAP_CONTENT);
		lpRight.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lpRight.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		right.setLayoutParams(lpRight);
		container.addView(right);

		return container;
	}

	private View newChildView(FrameLayout decor, View child,
			ViewGroup.LayoutParams layoutParams) throws Throwable {
		VerticalDragDetectorView drag = null;
		for (int i = 0; i < decor.getChildCount(); ++i) {
			View v = decor.getChildAt(i);
			if (v instanceof VerticalDragDetectorView) {
				drag = (VerticalDragDetectorView) v;
				break;
			}
		}
		if (drag != null) {
			((FlyingView2) drag.getChildAt(0)).addView(child, layoutParams);
			return null;
		} else {
			final Context context = decor.getContext();
			ViewGroup fly = newFlyingView(context);
			fly.addView(child, layoutParams);
			drag = newVerticalDragDetectorView(context);
			drag.addView(fly);
			if (sShowDragArea) {
				View dragArea = newDragAreaView(context,
						drag.getDetectionWidth());
				drag.addView(dragArea);
			}
			return drag;
		}
	}

	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		try {
			// if (!lpparam.packageName.equals("jp.tkgktyk.talktotimer"))
			// return;

			// XposedBridge.log("Loaded app: " + lpparam.packageName);
			// XposedBridge.log("is first app: " + lpparam.isFirstApplication);
			findAndHookMethod("android.view.ViewGroup", lpparam.classLoader,
					"addView", View.class, ViewGroup.LayoutParams.class,
					new XC_MethodReplacement() {
						@Override
						protected Object replaceHookedMethod(
								MethodHookParam param) {
							try {
								View child = (View) param.args[0];
								ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams) param.args[1];
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
									FrameLayout decor = (FrameLayout) param.thisObject;
									View newChild = newChildView(decor, child,
											layoutParams);
									if (newChild != null) {
										// to avoid stack overflow (recursive),
										// call addView(View, int, LayoutParams)
										callMethod(param.thisObject, "addView",
												newChild, -1,
												newChild.getLayoutParams());
									} else {
										// already exists newChild and child
										// have
										// been
										// added to newChild.
									}
								}
							} catch (Throwable t) {
								XposedBridge.log(t);
							}
							return null;
						}
					});
		} catch (Throwable t) {
			XposedBridge.log(t);
		}
	}
}
