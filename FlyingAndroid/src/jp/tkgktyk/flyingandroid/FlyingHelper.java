package jp.tkgktyk.flyingandroid;

import java.util.ArrayList;
import java.util.List;

import jp.tkgktyk.flyingandroid.VerticalDragDetectorView.OnDraggedListener;
import jp.tkgktyk.flyinglayout.FlyingLayoutF;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ToggleButton;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FlyingHelper {
	public static final String PACKAGE_NAME = FlyingAndroid.class.getPackage()
			.getName();

	private ViewGroup mTarget;

	private final FA.Settings mSettings;
	private FlyingLayoutF mFlyingLayout;
	private FrameLayout mContainerView;
	private View mOverlayView;
	private ToggleButton mPinButton;

	private Drawable mBoundaryDrawable;

	private boolean mForceSet = false;

	public FlyingHelper(FA.Settings settings) {
		mSettings = settings.clone();
	}

	/**
	 * Install FlyingLayout under following conditions.
	 * <ul>
	 * <li>enable vertical dragging on the screen edge</li>
	 * <li>disable takeoff position</li>
	 * <li>without pin</li>
	 * </ul>
	 * 
	 * @param context
	 * @throws Throwable
	 */
	public void installForFloatingWindow(ViewGroup target) throws Throwable {
		mSettings.initialXp = 0;
		mSettings.initialYp = 0;
		mSettings.usePin = false;
		// create FlyingLayout
		installFlyingLayout(target.getContext(), true);

		attachTo(target);
	}

	/**
	 * Install FlyingLayout following settings.
	 * 
	 * @param context
	 * @throws Throwable
	 */
	public void install(ViewGroup target) throws Throwable {
		// create FlyingLayout
		installFlyingLayout(target.getContext(), false);

		attachTo(target);
	}

	/**
	 * Install FlyingLayout with pin shown always.
	 * 
	 * @param context
	 * @throws Throwable
	 */
	public void installWithPinShownAlways(ViewGroup target) throws Throwable {
		if (!mSettings.usePin) {
			mSettings.usePin = true;
		}
		if (!mSettings.alwaysShowPin()) {
			mSettings.overwriteAlwaysShowPin(true);
		}
		// create FlyingLayout
		installFlyingLayout(target.getContext(), false);

		setOverlayShown(true);

		attachTo(target);
	}

	private static String FA_HELPER = "FA_helper";

	private void installFlyingLayout(Context context, boolean verticalDrag)
			throws Throwable {
		mForceSet = mSettings.forceSetBlackBackgroundSet.contains(context
				.getPackageName());

		// prepare boundary
		prepareBoundary(context);

		// prepare dragView as FlyingLayout's container
		prepareContainerView(context, verticalDrag);
		// prepare overlay
		prepareOverlayView(context);

		// setup view hierarchy
		mFlyingLayout = new FlyingLayoutF(context);
		mFlyingLayout.addView(mContainerView);
		mFlyingLayout.addView(mOverlayView);

		// setup FlyingLayout
		mFlyingLayout.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		mFlyingLayout.setSpeed(mSettings.speed);
		final Context flyContext = context.createPackageContext(PACKAGE_NAME,
				Context.CONTEXT_IGNORE_SECURITY);
		final int padding = flyContext.getResources().getDimensionPixelSize(
				R.dimen.flying_view_padding);
		mFlyingLayout.setHorizontalPadding(padding);
		mFlyingLayout.setVerticalPadding(padding);
		mFlyingLayout.setIgnoreTouchEvent(true);
		mFlyingLayout.setUseContainer(true);
		mFlyingLayout
				.setOnFlyingEventListener(new FlyingLayoutF.OnFlyingEventListener() {
					@Override
					public void onOutsideClick(FlyingLayoutF v, int x, int y) {
						// log("outside click");
						resetState();
					}

					@Override
					public void onMoveFinished(FlyingLayoutF v) {
						if (mSettings.autoPin(FA.AUTO_PIN_AFTER_MOVING)) {
							pin();
						}
					}
				});
	}

	public void prepareBoundary(Context context) {
		if (!mSettings.notifyFlying) {
			mBoundaryDrawable = null;
		} else {
			Context flyContext = null;
			try {
				flyContext = context.createPackageContext(PACKAGE_NAME,
						Context.CONTEXT_IGNORE_SECURITY);
			} catch (Throwable t) {
				XposedBridge.log(t);
			}
			if (flyContext != null) {
				mBoundaryDrawable = flyContext.getResources().getDrawable(
						R.drawable.notify_flying);
			} else {
				mBoundaryDrawable = null;
			}
		}
	}

	private void prepareOverlayView(Context context) throws Throwable {
		Context flyContext = context.createPackageContext(PACKAGE_NAME,
				Context.CONTEXT_IGNORE_SECURITY);
		mOverlayView = LayoutInflater.from(flyContext).inflate(
				R.layout.view_pin_button, null);
		mPinButton = (ToggleButton) mOverlayView.findViewById(R.id.pin);
		// set initial state to pinned
		mPinButton.setChecked(true);
		mPinButton
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							unfly();
						} else {
							fly();
						}
					}
				});
		if (mSettings.useNiwatoriButton) {
			mPinButton.setButtonDrawable(R.drawable.pin_niwatori);
		}

		mOverlayView.setVisibility(View.GONE);
		if (mSettings.usePin) {
			PinPosition pos = new PinPosition(context, mSettings.pinXp,
					mSettings.pinYp);
			pos.apply(mOverlayView);
		} else {
			mPinButton.setVisibility(View.GONE);
		}
	}

	private void prepareContainerView(Context context, boolean verticalDrag) {
		VerticalDragDetectorView dragView = new VerticalDragDetectorView(
				context);
		dragView.setOnDraggedListener(new OnDraggedListener() {
			@Override
			public void onDragged(VerticalDragDetectorView v) {
				// log("dragged");
				toggle();
			}
		});
		dragView.setIgnoreTouchEvent(!verticalDrag);

		mContainerView = dragView;
	}

	private void attachTo(ViewGroup target) {
		List<View> contents = new ArrayList<View>();
		for (int i = 0; i < target.getChildCount(); ++i) {
			contents.add(target.getChildAt(i));
		}
		FA.logD("children: " + target.getChildCount());
		target.removeAllViews();
		for (View v : contents) {
			mContainerView.addView(v, v.getLayoutParams());
		}
		target.addView(mFlyingLayout);
		XposedHelpers.setAdditionalInstanceField(target.getRootView(),
				FA_HELPER, this);

		mTarget = target;
	}

	/**
	 * Find a FlyingHelper attached to a DecorView by
	 * {@link FlyingAndroid#setFlyingHelper(ViewGroup, FlyingHelper)} and return
	 * it.
	 * 
	 * @param target
	 * @return
	 */
	public static FlyingHelper getFrom(View target) {
		return (FlyingHelper) XposedHelpers.getAdditionalInstanceField(
				target.getRootView(), FA_HELPER);
	}

	public FA.Settings getSettings() {
		return mSettings;
	}

	public FlyingLayoutF getFlyingLayout() {
		return mFlyingLayout;
	}

	private void setOverlayShown(boolean shown) {
		if (shown) {
			mOverlayView.setVisibility(View.VISIBLE);
		} else if (!mSettings.alwaysShowPin()) {
			mOverlayView.setVisibility(View.GONE);
		}
	}

	private void setBoundaryShown(boolean shown) {
		if (mSettings.notifyFlying) {
			if (shown) {
				mContainerView.setForeground(mBoundaryDrawable);
			} else {
				mContainerView.setForeground(null);
			}
		}
	}

	private void pin() {
		if (mPinButton.isChecked()) {
			mPinButton.setChecked(false);
		}
		mPinButton.setChecked(true);
	}

	private void unpin() {
		if (!mPinButton.isChecked()) {
			mPinButton.setChecked(true);
		}
		mPinButton.setChecked(false);
	}

	public void togglePin() {
		mPinButton.setChecked(!mPinButton.isChecked());
	}

	private void unfly() {
		mFlyingLayout.setIgnoreTouchEvent(true);
		setBoundaryShown(false);
	}

	private void fly() {
		forceSetBlackBackground();
		if (mFlyingLayout.staysHome()) {
			moveToInitialPosition();
		}
		mFlyingLayout.setIgnoreTouchEvent(false);
		setBoundaryShown(true);
	}

	private void forceSetBlackBackground() {
		// if (mForceSet) {
		// Activity activity = (Activity) mFlyingLayout.getContext();
		// // force set black background for clear background.
		// activity.getWindow().setBackgroundDrawableResource(
		// android.R.drawable.screen_background_dark);
		// }
	}

	public void toggle() {
		if (mFlyingLayout.getIgnoreTouchEvent() && mFlyingLayout.staysHome()) {
			// take off
			setOverlayShown(true);
			boolean moved = moveToInitialPosition();
			if (moved
					&& (mSettings.autoPin(FA.AUTO_PIN_WHEN_TAKEOFF) || mSettings
							.autoPin(FA.AUTO_PIN_AFTER_MOVING))) {
				pin();
			} else {
				unpin();
			}
		} else {
			resetState();
		}
	}

	private boolean moveToInitialPosition() {
		InitialPosition pos = new InitialPosition(mSettings.initialXp,
				mSettings.initialYp);
		int x = pos.getX(mFlyingLayout);
		int y = pos.getY(mFlyingLayout);
		boolean moved = false;
		if (x != 0 || y != 0) {
			moved = true;
			mFlyingLayout.moveWithoutSpeed(x, y, mSettings.animation);
		}
		return moved;
	}

	public void resetState() {
		// go home and unfly
		setOverlayShown(false);
		pin();
		// goHome must be placed after pin() for "Reset when collapsed" option.
		mFlyingLayout.goHome(mSettings.animation);
	}
}
