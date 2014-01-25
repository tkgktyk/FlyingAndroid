package jp.tkgktyk.flyingandroid;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import jp.tkgktyk.flyingandroid.FlyingView2.OnUnhandledClickListener;
import jp.tkgktyk.flyingandroid.VerticalDragDetectorView.OnDraggedListener;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class FlyingAndroid implements IXposedHookLoadPackage {
	public static final String PACKAGE_NAME = FlyingAndroid.class.getPackage()
			.getName();

	public void handleLoadPackage(final LoadPackageParam lpparam)
			throws Throwable {
		// if (!lpparam.packageName.equals("jp.tkgktyk.talktotimer"))
		// return;

		// XposedBridge.log("Loaded app: " + lpparam.packageName);
		// XposedBridge.log("is first app: " + lpparam.isFirstApplication);
		findAndHookMethod("android.view.ViewGroup", lpparam.classLoader,
				"addView", View.class, ViewGroup.LayoutParams.class,
				new XC_MethodReplacement() {
					@Override
					protected Object replaceHookedMethod(MethodHookParam param)
							throws Throwable {
						View child = (View) param.args[0];
						ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams) param.args[1];
						if (param.thisObject.getClass().getSimpleName()
								.equals("DecorView")) {
							FrameLayout decor = (FrameLayout) param.thisObject;
							VerticalDragDetectorView drag = null;
							for (int i = 0; i < decor.getChildCount(); ++i) {
								View v = decor.getChildAt(i);
								if (v instanceof VerticalDragDetectorView) {
									drag = (VerticalDragDetectorView) v;
									break;
								}
							}
							if (drag != null) {
								((FlyingView2) drag.getChildAt(0)).addView(
										child, layoutParams);
								return null;
							} else {
								final Context context = (Context) callMethod(
										param.thisObject, "getContext");
								final Context flyContext = context
										.createPackageContext(PACKAGE_NAME,
												Context.CONTEXT_IGNORE_SECURITY);
								FlyingView2 fly = new FlyingView2(context);
								fly.setLayoutParams(new ViewGroup.LayoutParams(
										ViewGroup.LayoutParams.MATCH_PARENT,
										ViewGroup.LayoutParams.MATCH_PARENT));
								fly.addView(child, layoutParams);
								XSharedPreferences pref = new XSharedPreferences(
										PACKAGE_NAME);
								fly.setSpeed(Float.parseFloat(pref.getString(
										flyContext
												.getString(R.string.pref_key_speed),
										"1.5")));
								final int padding = flyContext.getResources()
										.getDimensionPixelSize(
												R.dimen.flying_view_padding);
								fly.setHorizontalPadding(padding);
								fly.setVerticalPadding(padding);
								fly.setIgnoreTouchEvent(true);
								fly.setOnUnhandledClickListener(new OnUnhandledClickListener() {
									@Override
									public void onUnhandledClick(FlyingView2 v,
											int x, int y) {
										boolean inside = false;
										for (int i = 0; i < v.getChildCount(); ++i) {
											boolean in = false;
											View child = v.getChildAt(i);
											if (x >= child.getLeft()
													&& x <= child.getRight()) {
												if (y >= child.getTop()
														&& y <= child
																.getBottom()) {
													in = true;
												}
											}
											inside = (inside && in);
										}
										if (!inside) {
											XposedBridge
													.log("outside unhandled click");
											v.returnToHome();
											v.setIgnoreTouchEvent(true);
											Toast.makeText(context, "Rest",
													Toast.LENGTH_SHORT).show();
										}
									}
								});
								drag = new VerticalDragDetectorView(context);
								drag.setLayoutParams(new ViewGroup.LayoutParams(
										ViewGroup.LayoutParams.MATCH_PARENT,
										ViewGroup.LayoutParams.MATCH_PARENT));
								drag.setOnDraggedListener(new OnDraggedListener() {
									@Override
									public void onDragged(
											VerticalDragDetectorView v) {
										FlyingView2 fly = ((FlyingView2) v
												.getChildAt(0));
										XposedBridge.log("toggle");
										boolean next = !fly
												.getIgnoreTouchEvent();
										fly.setIgnoreTouchEvent(next);
										String text = null;
										if (next) {
											text = "Rest";
											fly.returnToHome();
										} else {
											text = "Fly";
										}
										Toast.makeText(v.getContext(), text,
												Toast.LENGTH_SHORT).show();
									}
								});
								drag.addView(fly);
							}
							child = drag;
							layoutParams = drag.getLayoutParams();
						}

						// to avoid stack overflow (recursive),
						// call addView(View, int, LayoutParams)
						callMethod(param.thisObject, "addView", child, -1,
								layoutParams);
						return null;
					}
				});
		// findAndHookMethod("android.app.Activity", lpparam.classLoader,
		// "onKeyLongPress", int.class, KeyEvent.class,
		// new XC_MethodReplacement() {
		// @Override
		// protected Object replaceHookedMethod(MethodHookParam param)
		// throws Throwable {
		// final int keyCode = (Integer) param.args[0];
		// if (keyCode == KeyEvent.KEYCODE_BACK) {
		// XposedBridge.log("longpress back key");
		// final Activity activity = (Activity) param.thisObject;
		// boolean toggle = false;
		// boolean next = false;
		// ViewGroup decor = (ViewGroup) activity.getWindow()
		// .getDecorView();
		// for (int i = 0; i < decor.getChildCount(); ++i) {
		// View child = decor.getChildAt(i);
		// if (child instanceof FlyingView) {
		// XposedBridge.log("toggle");
		// toggle = true;
		// FlyingView fly = (FlyingView) child;
		// next = !fly.getIgnoreTouchEvent();
		// fly.setIgnoreTouchEvent(next);
		// if (next) {
		// // rest
		// fly.returnToHome();
		// }
		// }
		// }
		// if (toggle) {
		// String text = null;
		// if (next) {
		// text = "Rest";
		// } else {
		// text = "Fly";
		// }
		// Toast.makeText(activity, text,
		// Toast.LENGTH_SHORT).show();
		// }
		// return true;
		// }
		// return false;
		// }
		// });
		// cannot hook Activity#onBackPressed because it is overridden when
		// I would like to hook.
		// findAndHookMethod("android.app.Activity", lpparam.classLoader,
		// "onBackPressed", new XC_MethodHook() {
		// @Override
		// protected void beforeHookedMethod(MethodHookParam param)
		// throws Throwable {
		// XposedBridge.log("before onBackPressed");
		// Activity activity = (Activity) param.thisObject;
		// ViewGroup decor = (ViewGroup) activity.getWindow()
		// .getDecorView();
		// boolean rest = false;
		// for (int i = 0; i < decor.getChildCount(); ++i) {
		// View child = decor.getChildAt(i);
		// if (child instanceof FlyingView) {
		// FlyingView fly = (FlyingView) child;
		// if (!fly.getIgnoreTouchEvent()) {
		// XposedBridge.log("rest by back key");
		// rest = true;
		// fly.setIgnoreTouchEvent(true);
		// fly.returnToHome();
		// }
		// }
		// }
		// if (rest) {
		// Toast.makeText(activity, "Rest", Toast.LENGTH_SHORT)
		// .show();
		// }
		// }
		// });
	}
}
