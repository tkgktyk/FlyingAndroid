package jp.tkgktyk.flyingandroid;

import de.robv.android.xposed.XposedBridge;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

public class VerticalDragDetectorView extends FrameLayout {
	private static final String TAG = "VerticalDragDetectorView";

	private static final float DEFAULT_TOUCH_SLOP_SCALE_FACTOR = 5.0f;
	private static final int DEFAULT_DETECTION_WIDTH = 40;

	/**
	 * Sentinel value for no current active pointer. Used by
	 * {@link #mActivePointerId}.
	 */
	private static final int INVALID_POINTER = -1;
	/**
	 * ID of the active pointer. This is used to retain consistency during
	 * drags/flings if multiple pointers are used.
	 */
	private int mActivePointerId = INVALID_POINTER;
	private int mTouchSlop;
	/**
	 * True if the user is currently dragging this ScrollView around. This is
	 * not the same as 'is being flinged', which can be checked by
	 * mScroller.isFinished() (flinging begins when the user lifts his finger).
	 */
	private boolean mIsBeginTouched = false;
	private boolean mIsBeginDragged = false;
	/**
	 * Position of the last motion event.
	 */
	private int mLastMotionY;

	private float mTouchSlopScaleFactor;
	private int mDetectionWidth;

	private void fetchAttribute(Context context, AttributeSet attrs) {
		// get attributes specified in XML
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.FlyingView, 0, 0);
		try {
			setTouchSlopScaleFactor(a.getFloat(
					R.styleable.VerticalDragDectorView_touchSlopScaleFactor,
					DEFAULT_TOUCH_SLOP_SCALE_FACTOR));
			setDetectionWidth(a.getDimensionPixelSize(
					R.styleable.VerticalDragDectorView_detectionWidth,
					DEFAULT_DETECTION_WIDTH));
		} finally {
			a.recycle();
		}
	}

	public VerticalDragDetectorView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		fetchAttribute(context, attrs);
	}

	public VerticalDragDetectorView(Context context, AttributeSet attrs) {
		super(context, attrs);

		fetchAttribute(context, attrs);
	}

	public VerticalDragDetectorView(Context context) {
		super(context);

		setTouchSlopScaleFactor(DEFAULT_TOUCH_SLOP_SCALE_FACTOR);
		setDetectionWidth(DEFAULT_DETECTION_WIDTH);
	}

	public void setTouchSlopScaleFactor(float scaleFactor) {
		mTouchSlopScaleFactor = scaleFactor;
		mTouchSlop = (int) Math.round(ViewConfiguration.get(getContext())
				.getScaledTouchSlop() * scaleFactor);
	}

	public float getTouchSlopScaleFactor() {
		return mTouchSlopScaleFactor;
	}

	public void setDetectionWidth(int width) {
		mDetectionWidth = width;
	}

	public int getDetectionWidth() {
		return mDetectionWidth;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		/*
		 * This method JUST determines whether we want to intercept the motion.
		 * If we return true, onMotionEvent will be called and we do the actual
		 * scrolling there.
		 */

		/*
		 * Shortcut the most recurring case: the user is in the dragging state
		 * and he is moving his finger. We want to intercept this motion.
		 */
		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE) && (mIsBeginTouched)) {
			return true;
		}

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_MOVE: {
			/*
			 * mIsBeginTouched == false, otherwise the shortcut would have
			 * caught it. Check whether the user has moved far enough from his
			 * original down touch.
			 */

			/*
			 * Locally do absolute value. mLastMotionYY is set to the y value of
			 * the down event.
			 */
			final int activePointerId = mActivePointerId;
			if (activePointerId == INVALID_POINTER) {
				// If we don't have a valid id, the touch down wasn't on
				// content.
				break;
			}

			final int pointerIndex = ev.findPointerIndex(activePointerId);
			if (pointerIndex == -1) {
				Log.e(TAG, "Invalid pointerId=" + activePointerId
						+ " in onInterceptTouchEvent");
				break;
			}

			final int y = (int) ev.getY(pointerIndex);
			final int yDiff = Math.abs(y - mLastMotionY);
			if (yDiff > mTouchSlop) {
				final ViewParent parent = getParent();
				if (parent != null) {
					parent.requestDisallowInterceptTouchEvent(true);
				}
				XposedBridge.log("last y: " + mLastMotionY);
				XposedBridge.log("y: " + y);
				XposedBridge.log("touch slop: " + mTouchSlop);
				mIsBeginDragged = true;
				mLastMotionY = y;
				onDragged();
			}
			break;
		}

		case MotionEvent.ACTION_DOWN: {
			final int x = (int) ev.getX();
			if ((x >= getLeft() && x <= getLeft() + mDetectionWidth)
					|| (x >= getRight() - mDetectionWidth && x <= getRight())) {
				/*
				 * Remember location of down touch. ACTION_DOWN always refers to
				 * pointer index 0.
				 */
				mLastMotionY = (int) ev.getY();
				mActivePointerId = ev.getPointerId(0);
				mIsBeginTouched = true;
			}
			break;
		}

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			// always does not intercept

			/* Release the drag */
			mIsBeginTouched = false;
			mIsBeginDragged = false;
			mActivePointerId = INVALID_POINTER;
			break;
		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			break;
		}
		/*
		 * The only time we want to intercept motion events is if we are in the
		 * drag mode.
		 */
		return mIsBeginTouched;
	};

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			if (getChildCount() == 0) {
				return false;
			}

			final int x = (int) ev.getX();
			if ((x >= getLeft() && x <= getLeft() + mDetectionWidth)
					|| (x >= getRight() - mDetectionWidth && x <= getRight())) {
				// Remember where the motion event started
				mLastMotionY = (int) ev.getY();
				mActivePointerId = ev.getPointerId(0);
				mIsBeginTouched = true;
			}
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			final int activePointerIndex = ev
					.findPointerIndex(mActivePointerId);
			if (activePointerIndex == -1) {
				Log.e(TAG, "Invalid pointerId=" + mActivePointerId
						+ " in onTouchEvent");
				break;
			}

			final int y = (int) ev.getY(activePointerIndex);
			int deltaY = mLastMotionY - y;
			if (!mIsBeginDragged && Math.abs(deltaY) > mTouchSlop) {
				XposedBridge.log("last y: " + mLastMotionY);
				XposedBridge.log("y: " + y);
				XposedBridge.log("touch slop: " + mTouchSlop);
				mIsBeginDragged = true;
				mLastMotionY = y;
				final ViewParent parent = getParent();
				if (parent != null) {
					parent.requestDisallowInterceptTouchEvent(true);
				}
				onDragged();
			}
			break;
		}
		case MotionEvent.ACTION_UP: {
			if (mIsBeginTouched) {
				mActivePointerId = INVALID_POINTER;
				mIsBeginTouched = false;
				mIsBeginDragged = false;
			}
			break;
		}
		case MotionEvent.ACTION_CANCEL:
			if (mIsBeginTouched && getChildCount() > 0) {
				mActivePointerId = INVALID_POINTER;
				mIsBeginTouched = false;
				mIsBeginDragged = false;
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			final int index = ev.getActionIndex();
			mLastMotionY = (int) ev.getY(index);
			mActivePointerId = ev.getPointerId(index);
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
			break;
		}
		return true;
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		final int pointerId = ev.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			// TODO: Make this decision more intelligent.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastMotionY = (int) ev.getY(newPointerIndex);
			mActivePointerId = ev.getPointerId(newPointerIndex);
		}
	}

	public void onDragged() {
		if (mOnDraggedListener != null) {
			mOnDraggedListener.onDragged(this);
		}
	}

	public interface OnDraggedListener {
		/**
		 * callback when onDragged event.
		 */
		public void onDragged(VerticalDragDetectorView v);
	}

	private OnDraggedListener mOnDraggedListener = null;

	public void setOnDraggedListener(OnDraggedListener listener) {
		mOnDraggedListener = listener;
	}

}