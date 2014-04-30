package jp.tkgktyk.flyingandroid;

import java.util.ArrayList;
import java.util.List;

import jp.tkgktyk.flyingandroid.VerticalDragDetectorView.OnDraggedListener;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
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

	private final FA.Settings mSettings;
	private FlyingLayoutF mFlyingLayout;
	private FrameLayout mContainerView;
	private View mOverlayView;
	private ToggleButton mPinButton;

	private PinPosition mPinPosition;

	private void installFlyingLayout(ViewGroup target, boolean verticalDrag)
			throws Throwable {
		Context context = target.getContext();
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
						toggle();
					}

					@Override
					public void onMove(FlyingLayoutF v, int deltaX, int deltaY) {
						// do nothing
					}

					@Override
					public void onMoveFinished(FlyingLayoutF v) {
						if (mSettings.autoPin(FA.AUTO_PIN_AFTER_MOVING)) {
							pin();
						}
					}
				});
	}

	private void prepareOverlayView(Context context) {
		Context flyContext;
		try {
			flyContext = context.createPackageContext(PACKAGE_NAME,
					Context.CONTEXT_IGNORE_SECURITY);
			mOverlayView = LayoutInflater.from(flyContext).inflate(
					R.layout.view_pin_button, null);
			mPinButton = (ToggleButton) mOverlayView.findViewById(R.id.pin);
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

			mOverlayView.setVisibility(View.GONE);
			if (mSettings.usePin) {
				mPinPosition = new PinPosition(context, mSettings.pinXp,
						mSettings.pinYp);
				mPinPosition.apply(mOverlayView);
			} else {
				mPinButton.setVisibility(View.GONE);
			}
		} catch (NameNotFoundException e) {
			FA.logE(e);
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

	private boolean mAlwaysShowPin;

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
		mSettings.takeoffPosition = FA.TAKEOFF_POSITION_CENTER;
		mSettings.usePin = false;
		// create FlyingLayout
		installFlyingLayout(target, true);

		installToViewGroup(target);
	}

	/**
	 * Install FlyingLayout following settings.
	 * 
	 * @param context
	 * @throws Throwable
	 */
	public void install(ViewGroup target) throws Throwable {
		mAlwaysShowPin = false;
		// create FlyingLayout
		installFlyingLayout(target, false);

		installToViewGroup(target);
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
		mSettings.takeoffPosition = FA.TAKEOFF_POSITION_CENTER;
		mAlwaysShowPin = true;
		// create FlyingLayout
		installFlyingLayout(target, false);

		pin();
		setOverlayShown(true);

		installToViewGroup(target);
	}

	private static String FA_HELPER = "FA_helper";

	private void installToViewGroup(ViewGroup target) {
		List<View> contents = new ArrayList<View>();
		for (int i = 0; i < target.getChildCount(); ++i) {
			contents.add(target.getChildAt(i));
		}
		FA.logD("children: " + target.getChildCount());
		target.removeAllViews();
		for (View v : contents) {
			addViewToFlyingLayout(v, v.getLayoutParams());
		}
		target.addView(mFlyingLayout);
		XposedHelpers.setAdditionalInstanceField(target.getRootView(),
				FA_HELPER, this);
	}

	/**
	 * Find a FlyingHelper attached to a DecorView by
	 * {@link FlyingAndroid#setFlyingHelper(ViewGroup, FlyingHelper)} and return
	 * it.
	 * 
	 * @param target
	 * @return
	 */
	public static FlyingHelper getFrom(Activity target) {
		return getFrom(target.getWindow().peekDecorView());
	}

	/**
	 * Find a FlyingHelper attached to a DecorView by
	 * {@link FlyingAndroid#setFlyingHelper(ViewGroup, FlyingHelper)} and return
	 * it.
	 * 
	 * @param target
	 * @return
	 */
	public static FlyingHelper getFrom(Dialog target) {
		return getFrom(target.getWindow().peekDecorView());
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

	public void addViewToFlyingLayout(View child,
			ViewGroup.LayoutParams layoutParams) {
		mContainerView.addView(child, layoutParams);
	}

	private Drawable mBoundaryDrawable;

	private boolean mReceiverRegistered = false;

	private boolean mFlying = false;

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

	private void setOverlayShown(boolean shown) {
		if (shown) {
			mOverlayView.setVisibility(View.VISIBLE);
		} else if (!mAlwaysShowPin) {
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

	private void unfly() {
		mFlyingLayout.setIgnoreTouchEvent(true);
		setBoundaryShown(false);
		mFlying = false;
	}

	private void fly() {
		mFlyingLayout.setIgnoreTouchEvent(false);
		setBoundaryShown(true);
		mFlying = true;
	}

	public void toggle() {
		if (mFlyingLayout.staysHome() && !mFlying) {
			// take off
			setOverlayShown(true);
			boolean moved = false;
			switch (mSettings.takeoffPosition) {
			case FA.TAKEOFF_POSITION_CENTER:
				// do noting
				break;
			case FA.TAKEOFF_POSITION_BOTTOM:
				mFlyingLayout.moveWithoutSpeed(0,
						Math.round(mFlyingLayout.getHeight() / 2.0f));
				moved = true;
				break;
			case FA.TAKEOFF_POSITION_LOWER_LEFT:
				mFlyingLayout.moveWithoutSpeed(
						Math.round(-mFlyingLayout.getWidth() / 2.0f),
						Math.round(mFlyingLayout.getHeight() / 2.0f));
				moved = true;
				break;
			case FA.TAKEOFF_POSITION_LOWER_RIGHT:
				mFlyingLayout.moveWithoutSpeed(
						Math.round(mFlyingLayout.getWidth() / 2.0f),
						Math.round(mFlyingLayout.getHeight() / 2.0f));
				moved = true;
				break;
			}
			if (moved
					&& (mSettings.autoPin(FA.AUTO_PIN_WHEN_TAKEOFF) || mSettings
							.autoPin(FA.AUTO_PIN_AFTER_MOVING))) {
				pin();
			} else {
				unpin();
			}
		} else {
			// go home and unfly
			setOverlayShown(false);
			mFlyingLayout.goHome();
			pin();
		}
	}

	public void resetState() {
		// go home and unfly
		setOverlayShown(false);
		mFlyingLayout.goHome();
		pin();
	}

	private final BroadcastReceiver mToggleReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// log("toggle");
			toggle();
		}
	};

	public BroadcastReceiver getToggleReceiver() {
		return mToggleReceiver;
	}

	public boolean receiverRegistered() {
		return mReceiverRegistered;
	}

	public void onReceiverRegistered() {
		mReceiverRegistered = true;
	}

	public void onReceiverUnregistered() {
		mReceiverRegistered = false;
	}
}
