package jp.tkgktyk.flyingandroid;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;
import de.robv.android.xposed.XposedBridge;

public class VerticalDragDetectorView extends FrameLayout {
	private static final String TAG = "VerticalDragDetectorView";

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
	private int mTouchSlopX;
	private int mDragSlop;
	/**
	 * True if the user is currently dragging this ScrollView around. This is
	 * not the same as 'is being flinged', which can be checked by
	 * mScroller.isFinished() (flinging begins when the user lifts his finger).
	 */
	private boolean mIsBeginTouchedX;
	private boolean mIsBeginDragged;
	/**
	 * Position of the last motion event.
	 */
	private int mLastMotionX;
	private int mLastMotionY;

	private boolean mIgnoreTouchEvent;

	private int mDetectionWidth;

	private void resetPrivateVariable() {
		mIsBeginTouchedX = false;
		mIsBeginDragged = false;
		mTouchSlopX = ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2;
		mDragSlop = mTouchSlopX * 2;
		mIgnoreTouchEvent = false;
		mDetectionWidth = Math.round(ViewConfiguration.get(getContext())
				.getScaledEdgeSlop() * 1.5f);
	}

	public VerticalDragDetectorView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		resetPrivateVariable();
	}

	public VerticalDragDetectorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		resetPrivateVariable();
	}

	public VerticalDragDetectorView(Context context) {
		super(context);
		resetPrivateVariable();
	}

	public void setIgnoreTouchEvent(boolean ignore) {
		mIgnoreTouchEvent = ignore;
	}

	public boolean getIgnoreTouchEvent() {
		return mIgnoreTouchEvent;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (!isBeginAnything() && mIgnoreTouchEvent) {
			return false;
		}

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
		if (action == MotionEvent.ACTION_MOVE) {
			if (mIsBeginTouchedX)
				return false;
			if (mIsBeginDragged)
				return true;
		}

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_MOVE: {
			/*
			 * mIsBeginTouchedY == false, otherwise the shortcut would have
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

			final int x = (int) ev.getX(pointerIndex);
			final int xDiff = Math.abs(x - mLastMotionX);
			if (xDiff > mTouchSlopX) {
				// XposedBridge.log("last x: " + mLastMotionX);
				// XposedBridge.log("x: " + x);
				// XposedBridge.log("touch slop: " + mTouchSlopX);
				mIsBeginTouchedX = true;
			}
			final int y = (int) ev.getY(pointerIndex);
			final int yDiff = Math.abs(y - mLastMotionY);
			if (yDiff > mDragSlop) {
				final ViewParent parent = getParent();
				if (parent != null) {
					parent.requestDisallowInterceptTouchEvent(true);
				}
				// XposedBridge.log("last y: " + mLastMotionY);
				// XposedBridge.log("y: " + y);
				// XposedBridge.log("touch slop: " + mDragSlop);
				mIsBeginDragged = true;
				onDragged();
			}
			break;
		}

		case MotionEvent.ACTION_DOWN: {
			final int x = (int) ev.getX();
			// XposedBridge.log("left: " + getLeft());
			// XposedBridge.log("right: " + getRight());
			// XposedBridge.log("DetectionWidth: " + mDetectionWidth);
			// XposedBridge.log("down at x: " + x);
			// XposedBridge.log("down at y: " + ev.getY());
			if ((x >= getLeft() && x <= getLeft() + mDetectionWidth)
					|| (x >= getRight() - mDetectionWidth && x <= getRight())) {
				/*
				 * Remember location of down touch. ACTION_DOWN always refers to
				 * pointer index 0.
				 */
				mLastMotionX = (int) ev.getX();
				mLastMotionY = (int) ev.getY();
				mActivePointerId = ev.getPointerId(0);
			} else {
				if (BuildConfig.DEBUG)
					XposedBridge.log("this touch event is ignored.");
				// ignore this touch event
				mIsBeginTouchedX = true;
			}
			break;
		}

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			// always does not intercept

			/* Release the drag */
			mIsBeginTouchedX = false;
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
		if (mIsBeginTouchedX)
			return false;

		return mIsBeginDragged;
	};

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (!isBeginAnything() && mIgnoreTouchEvent) {
			return false;
		}

		final int action = ev.getAction();
		if (action == MotionEvent.ACTION_MOVE) {
			if (mIsBeginTouchedX)
				return false;
		}

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			if (getChildCount() == 0) {
				return false;
			}

			final int x = (int) ev.getX();
			// XposedBridge.log("left: " + getLeft());
			// XposedBridge.log("right: " + getRight());
			// XposedBridge.log("DetectionWidth: " + mDetectionWidth);
			// XposedBridge.log("down at x: " + x);
			// XposedBridge.log("down at y: " + ev.getY());
			if ((x >= getLeft() && x <= getLeft() + mDetectionWidth)
					|| (x >= getRight() - mDetectionWidth && x <= getRight())) {
				// Remember where the motion event started
				mLastMotionX = (int) ev.getX();
				mLastMotionY = (int) ev.getY();
				mActivePointerId = ev.getPointerId(0);
			} else {
				if (BuildConfig.DEBUG)
					XposedBridge.log("this touch event is ignored.");
				// ignore this touch event
				mIsBeginTouchedX = true;
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

			final int x = (int) ev.getX(activePointerIndex);
			int deltaX = mLastMotionX - x;
			if (!isBeginAnything() && Math.abs(deltaX) > mTouchSlopX) {
				// XposedBridge.log("last x: " + mLastMotionX);
				// XposedBridge.log("x: " + x);
				// XposedBridge.log("touch slop: " + mTouchSlopX);
				mIsBeginTouchedX = true;
			}
			final int y = (int) ev.getY(activePointerIndex);
			int deltaY = mLastMotionY - y;
			if (!mIsBeginDragged && Math.abs(deltaY) > mDragSlop) {
				// XposedBridge.log("last y: " + mLastMotionY);
				// XposedBridge.log("y: " + y);
				// XposedBridge.log("touch slop: " + mDragSlop);
				mIsBeginDragged = true;
				final ViewParent parent = getParent();
				if (parent != null) {
					parent.requestDisallowInterceptTouchEvent(true);
				}
				onDragged();
			}

			break;
		}
		case MotionEvent.ACTION_UP: {
			if (isBeginAnything()) {
				mActivePointerId = INVALID_POINTER;
				mIsBeginTouchedX = false;
				mIsBeginDragged = false;
			}
			break;
		}
		case MotionEvent.ACTION_CANCEL:
			if (isBeginAnything() && getChildCount() > 0) {
				mActivePointerId = INVALID_POINTER;
				mIsBeginTouchedX = false;
				mIsBeginDragged = false;
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN: {
			final int index = ev.getActionIndex();
			mLastMotionX = (int) ev.getX(index);
			mLastMotionY = (int) ev.getY(index);
			mActivePointerId = ev.getPointerId(index);
			break;
		}
		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			mLastMotionX = (int) ev.getX(ev.findPointerIndex(mActivePointerId));
			mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
			break;
		}
		return true;
	}

	private boolean isBeginAnything() {
		return mIsBeginTouchedX || mIsBeginDragged;
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		final int pointerId = ev.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			// TODO: Make this decision more intelligent.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastMotionX = (int) ev.getX(newPointerIndex);
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