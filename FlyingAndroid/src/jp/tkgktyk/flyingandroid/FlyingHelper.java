package jp.tkgktyk.flyingandroid;

import jp.tkgktyk.flyingandroid.FlyingAndroid.Settings;
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

	private final Settings mSettings;
	private ToggleHelper mToggleHelper;
	private FlyingView mFlyingView;
	private View mOverlay;
	private ToggleButton mPinButton;

	private void installFlyingView(Context context, boolean verticalDrag)
			throws Throwable {
		// prepare ToggleHelper
		mToggleHelper = new ToggleHelper(context);

		// prepare dragView as FlyingView's container
		ViewGroup container = createDragView(context, verticalDrag);
		// prepare overlay
		installOverlayView(context);
		mOverlay.setVisibility(View.GONE);

		// setup view hierarchy
		mFlyingView = new FlyingView1(context);
		mFlyingView.addView(container);
		mFlyingView.addView(mOverlay);

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
					mToggleHelper.toggle();
				}
			}

			@Override
			public boolean onMove(FlyingView v, int deltaX, int deltaY) {
				// execute original function by returning false.
				return false;
			}

			@Override
			public void onMoveFinished(FlyingView v) {
				if (mSettings.autoPin) {
					if (mPinButton.isChecked()) {
						mPinButton.setChecked(false);
					}
					mPinButton.setChecked(true);
				}
			}
		});
	}

	private void installOverlayView(Context context) {
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
					mToggleHelper.setWindowPinned(isChecked);
				}
			});
			LinearLayout container = (LinearLayout) overlay
					.findViewById(R.id.container);
			switch (mSettings.pinPosition) {
			case Settings.PIN_POSITION_NONE:
				button.setVisibility(View.GONE);
				break;
			case Settings.PIN_POSITION_CENTER_LEFT:
				container.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
				break;
			case Settings.PIN_POSITION_CENTER_RIGHT:
				container.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
				break;
			case Settings.PIN_POSITION_LOWER_LEFT:
				container.setGravity(Gravity.BOTTOM | Gravity.LEFT);
				break;
			case Settings.PIN_POSITION_LOWER_RIGHT:
				container.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
				break;
			}
		} catch (Throwable t) {
			XposedBridge.log(t);
			overlay = new View(context);
			button = new ToggleButton(context);
		}

		mOverlay = overlay;
		mPinButton = button;
	}

	private VerticalDragDetectorView createDragView(Context context,
			boolean verticalDrag) {
		VerticalDragDetectorView dragView = new VerticalDragDetectorView(
				context);
		dragView.setOnDraggedListener(new OnDraggedListener() {
			@Override
			public void onDragged(VerticalDragDetectorView v) {
				// log("dragged");
				mToggleHelper.toggle();
			}
		});
		dragView.setIgnoreTouchEvent(!verticalDrag);

		return dragView;
	}

	private final boolean mUseOverlay;
	private final boolean mAlwaysShowPin;

	public FlyingHelper(Settings settings, Context context, boolean isFloating,
			boolean usePin, boolean alwaysShowPin) throws Throwable {
		mSettings = settings.clone();
		if (isFloating) {
			mSettings.takeoffPosition = Settings.TAKEOFF_POSITION_CENTER;
			mSettings.autoPin = false;
			usePin = false;
		}
		if (!usePin) {
			mSettings.pinPosition = Settings.PIN_POSITION_NONE;
			mSettings.autoPin = false;
			alwaysShowPin = false;
		}
		if (mSettings.pinPosition == Settings.PIN_POSITION_NONE) {
			mSettings.autoPin = false;
			alwaysShowPin = false;
		}
		if (alwaysShowPin) {
			mSettings.takeoffPosition = Settings.TAKEOFF_POSITION_CENTER;
		}
		mUseOverlay = usePin;
		mAlwaysShowPin = alwaysShowPin;
		// create FlyingView
		installFlyingView(context, isFloating);

		if (mAlwaysShowPin) {
			if (mPinButton.isChecked()) {
				mPinButton.setChecked(false);
			}
			mPinButton.setChecked(true);
			mToggleHelper.setOverlayShown(true);
		}
	}

	public Settings getSettings() {
		return mSettings;
	}

	public FlyingView getFlyingView() {
		return mFlyingView;
	}

	public ViewGroup getFlyingRootView() {
		return mFlyingView;
	}

	public ToggleHelper getToggleHelper() {
		return mToggleHelper;
	}

	private FrameLayout getContainer() {
		return (FrameLayout) mFlyingView.getChildAt(0);
	}

	public void addViewToFlyingView(View child,
			ViewGroup.LayoutParams layoutParams) {
		getContainer().addView(child, layoutParams);
	}

	private View getOverlay() {
		return mFlyingView.getChildAt(mFlyingView.getChildCount() - 1);
	}

	public class ToggleHelper {
		private final Drawable mNotifyFlyingDrawable;

		private boolean mReceiverRegistered = false;

		private boolean mFlying = false;

		public ToggleHelper(Context context) throws Throwable {
			if (!mSettings.notifyFlying) {
				mNotifyFlyingDrawable = null;
			} else {
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

		private void setWindowPinned(boolean pinned) {
			// String text;
			if (pinned) {
				// text = "pin";
				mFlyingView.setIgnoreTouchEvent(true);
				setBoundaryShown(false);
				// mFlying = false;
			} else {
				// text = "Unpin";
				mFlyingView.setIgnoreTouchEvent(false);
				setBoundaryShown(true);
				mFlying = true;
			}
		}

		private void setOverlayShown(boolean shown) {
			if (mUseOverlay) {
				if (shown) {
					getOverlay().setVisibility(View.VISIBLE);
				} else if (!mAlwaysShowPin) {
					getOverlay().setVisibility(View.GONE);
				}
			}
		}

		private void setBoundaryShown(boolean shown) {
			if (mSettings.notifyFlying) {
				FrameLayout container = getContainer();
				if (shown) {
					container.setForeground(mNotifyFlyingDrawable);
				} else {
					container.setForeground(null);
				}
			}
		}

		public void toggle() {
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
				boolean moved = false;
				switch (mSettings.takeoffPosition) {
				case Settings.TAKEOFF_POSITION_CENTER:
					// do noting
					break;
				case Settings.TAKEOFF_POSITION_BOTTOM:
					mFlyingView.move(0,
							Math.round(mFlyingView.getHeight() / 2.0f));
					moved = true;
					break;
				case Settings.TAKEOFF_POSITION_LOWER_LEFT:
					mFlyingView.move(
							Math.round(-mFlyingView.getWidth() / 2.0f),
							Math.round(mFlyingView.getHeight() / 2.0f));
					moved = true;
					break;
				case Settings.TAKEOFF_POSITION_LOWER_RIGHT:
					mFlyingView.move(Math.round(mFlyingView.getWidth() / 2.0f),
							Math.round(mFlyingView.getHeight() / 2.0f));
					moved = true;
					break;
				}
				if (mSettings.autoPin && moved) {
					if (mPinButton.isChecked()) {
						mPinButton.setChecked(false);
					}
					mPinButton.setChecked(true);
				} else {
					if (!mPinButton.isChecked()) {
						mPinButton.setChecked(true);
					}
					mPinButton.setChecked(false);
				}
				setOverlayShown(true);
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
}
