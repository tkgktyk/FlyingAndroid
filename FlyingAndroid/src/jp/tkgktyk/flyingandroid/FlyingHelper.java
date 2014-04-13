package jp.tkgktyk.flyingandroid;

import jp.tkgktyk.flyingandroid.FlyingView.OnFlyingEventListener;
import jp.tkgktyk.flyingandroid.VerticalDragDetectorView.OnDraggedListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ToggleButton;
import de.robv.android.xposed.XposedBridge;

public class FlyingHelper {
	public static final String PACKAGE_NAME = FlyingAndroid.class.getPackage()
			.getName();

	private final FlyingAndroidSettings mSettings;
	private FlyingView mFlyingView;
	private FrameLayout mContainerView;
	private View mOverlayView;
	private ToggleButton mPinButton;

	private void installFlyingView(Context context, boolean verticalDrag)
			throws Throwable {
		// prepare boundary
		prepareBoundary(context);

		// prepare dragView as FlyingView's container
		prepareContainerView(context, verticalDrag);
		// prepare overlay
		prepareOverlayView(context);

		// setup view hierarchy
		mFlyingView = new FlyingView1(context);
		mFlyingView.addView(mContainerView);
		mFlyingView.addView(mOverlayView);

		// setup FlyingView
		mFlyingView.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		mFlyingView.setSpeed(mSettings.speed);
		final Context flyContext = context.createPackageContext(PACKAGE_NAME,
				Context.CONTEXT_IGNORE_SECURITY);
		final int padding = flyContext.getResources().getDimensionPixelSize(
				R.dimen.flying_view_padding);
		mFlyingView.setHorizontalPadding(padding);
		mFlyingView.setVerticalPadding(padding);
		mFlyingView.setIgnoreTouchEvent(true);
		mFlyingView.setOnFlyingEventListener(new OnFlyingEventListener() {
			@Override
			public void onClickUnhandled(FlyingView v, int x, int y) {
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
					toggle();
				}
			}

			@Override
			public boolean onMove(FlyingView v, int deltaX, int deltaY) {
				// execute original function by returning false.
				return false;
			}

			@Override
			public void onMoveFinished(FlyingView v) {
				if (mSettings.autoPin()) {
					pin();
				}
			}
		});
	}

	private void prepareOverlayView(Context context) {
		View overlay;
		ToggleButton button;
		try {
			final Context flyContext = context.createPackageContext(
					PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
			overlay = LayoutInflater.from(flyContext).inflate(
					R.layout.view_overlay, null);
			button = (ToggleButton) overlay.findViewById(R.id.button);
			button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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
			LinearLayout container = (LinearLayout) overlay
					.findViewById(R.id.container);
			switch (mSettings.pinPosition) {
			case FlyingAndroidSettings.PIN_POSITION_CENTER_LEFT:
				container.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
				break;
			case FlyingAndroidSettings.PIN_POSITION_CENTER_RIGHT:
				container.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
				break;
			case FlyingAndroidSettings.PIN_POSITION_LOWER_LEFT:
				container.setGravity(Gravity.BOTTOM | Gravity.LEFT);
				break;
			case FlyingAndroidSettings.PIN_POSITION_LOWER_RIGHT:
				container.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
				break;
			default:
				container.setGravity(Gravity.BOTTOM | Gravity.LEFT);
				break;
			}
		} catch (Throwable t) {
			XposedBridge.log(t);
			overlay = new View(context);
			button = new ToggleButton(context);
		}

		mOverlayView = overlay;
		mPinButton = button;

		mOverlayView.setVisibility(View.GONE);
		if (!mSettings.usePin) {
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

	private boolean mAlwaysShowPin;

	public FlyingHelper(FlyingAndroidSettings settings) {
		mSettings = settings.clone();
	}

	/**
	 * Install FlyingView under following conditions.
	 * <ul>
	 * <li>enable vertical dragging on the screen edge</li>
	 * <li>disable takeoff position</li>
	 * <li>without pin</li>
	 * </ul>
	 * 
	 * @param context
	 * @throws Throwable
	 */
	public void installForFloatingWindow(Context context) throws Throwable {
		mSettings.takeoffPosition = FlyingAndroidSettings.TAKEOFF_POSITION_CENTER;
		mSettings.usePin = false;
		// create FlyingView
		installFlyingView(context, true);
	}

	/**
	 * Install FlyingView following settings.
	 * 
	 * @param context
	 * @throws Throwable
	 */
	public void install(Context context) throws Throwable {
		mAlwaysShowPin = false;
		// create FlyingView
		installFlyingView(context, false);
	}

	/**
	 * Install FlyingView with pin shown always.
	 * 
	 * @param context
	 * @throws Throwable
	 */
	public void installWithPinShownAlways(Context context) throws Throwable {
		if (!mSettings.usePin) {
			throw new IllegalStateException("usePin should be true.");
		}
		mSettings.takeoffPosition = FlyingAndroidSettings.TAKEOFF_POSITION_CENTER;
		mAlwaysShowPin = true;
		// create FlyingView
		installFlyingView(context, false);

		pin();
		setOverlayShown(true);
	}

	public FlyingAndroidSettings getSettings() {
		return mSettings;
	}

	public FlyingView getFlyingView() {
		return mFlyingView;
	}

	public void addViewToFlyingView(View child,
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
		mFlyingView.setIgnoreTouchEvent(true);
		setBoundaryShown(false);
		mFlying = false;
	}

	private void fly() {
		mFlyingView.setIgnoreTouchEvent(false);
		setBoundaryShown(true);
		mFlying = true;
	}

	public void toggle() {
		if (mFlyingView.staysHome() && !mFlying) {
			// take off
			setOverlayShown(true);
			boolean moved = false;
			switch (mSettings.takeoffPosition) {
			case FlyingAndroidSettings.TAKEOFF_POSITION_CENTER:
				// do noting
				break;
			case FlyingAndroidSettings.TAKEOFF_POSITION_BOTTOM:
				mFlyingView.move(0, Math.round(mFlyingView.getHeight() / 2.0f));
				moved = true;
				break;
			case FlyingAndroidSettings.TAKEOFF_POSITION_LOWER_LEFT:
				mFlyingView.move(Math.round(-mFlyingView.getWidth() / 2.0f),
						Math.round(mFlyingView.getHeight() / 2.0f));
				moved = true;
				break;
			case FlyingAndroidSettings.TAKEOFF_POSITION_LOWER_RIGHT:
				mFlyingView.move(Math.round(mFlyingView.getWidth() / 2.0f),
						Math.round(mFlyingView.getHeight() / 2.0f));
				moved = true;
				break;
			}
			if (mSettings.autoPin() && moved) {
				pin();
			} else {
				unpin();
			}
		} else {
			// go home and unfly
			setOverlayShown(false);
			mFlyingView.goHome();
			pin();
		}
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
