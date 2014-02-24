package jp.tkgktyk.flyingandroid;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

public abstract class FlyingView extends FrameLayout {
	private static final String TAG = "FlyingView";

	private static final float DEFAULT_SPEED = 1.0f;
	private static final int DEFAULT_HORIZONTAL_PADDING = 0;
	private static final int DEFAULT_VERTICAL_PADDING = 0;
	private static final boolean DEFAULT_IGNORE_TOUCH_EVENT = false;

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
	private boolean mIsBeingDragged = false;
	/**
	 * Position of the last motion event.
	 */
	private int mLastMotionX;
	private int mLastMotionY;

	private float mSpeed;
	private int mHorizontalPadding;
	private int mVerticalPadding;
	private boolean mIgnoreTouchEvent;

	private void fetchAttribute(Context context, AttributeSet attrs) {
		// get attributes specified in XML
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.FlyingView, 0, 0);
		try {
			setSpeed(a.getFloat(R.styleable.FlyingView_speed, DEFAULT_SPEED));
			setHorizontalPadding(a.getDimensionPixelSize(
					R.styleable.FlyingView_horizontalPadding,
					DEFAULT_HORIZONTAL_PADDING));
			setVerticalPadding(a.getDimensionPixelSize(
					R.styleable.FlyingView_verticalPadding,
					DEFAULT_VERTICAL_PADDING));
			setIgnoreTouchEvent(a.getBoolean(
					R.styleable.FlyingView_ignoreTouchEvent,
					DEFAULT_IGNORE_TOUCH_EVENT));
		} finally {
			a.recycle();
		}
	}

	public FlyingView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

		fetchAttribute(context, attrs);
	}

	public FlyingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

		fetchAttribute(context, attrs);
	}

	public FlyingView(Context context) {
		super(context);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

		setSpeed(DEFAULT_SPEED);
		setHorizontalPadding(DEFAULT_HORIZONTAL_PADDING);
		setVerticalPadding(DEFAULT_VERTICAL_PADDING);
		setIgnoreTouchEvent(DEFAULT_IGNORE_TOUCH_EVENT);
	}

	public void setSpeed(float speed) {
		mSpeed = speed;
	}

	public float getSpeed() {
		return mSpeed;
	}

	public void setHorizontalPadding(int padding) {
		mHorizontalPadding = padding;
	}

	public int getHorizontalPadding() {
		return mHorizontalPadding;
	}

	public void setVerticalPadding(int padding) {
		mVerticalPadding = padding;
	}

	public int getVerticalPadding() {
		return mVerticalPadding;
	}

	public void setIgnoreTouchEvent(boolean ignore) {
		mIgnoreTouchEvent = ignore;
	}

	public boolean getIgnoreTouchEvent() {
		return mIgnoreTouchEvent;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (!mIsBeingDragged && mIgnoreTouchEvent) {
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
		if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
			return true;
		}

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_MOVE: {
			/*
			 * mIsBeingDragged == false, otherwise the shortcut would have
			 * caught it. Check whether the user has moved far enough from his
			 * original down touch.
			 */

			/*
			 * Locally do absolute value. mLastMotionY is set to the y value of
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

			boolean isBeingDraggedX = false;
			boolean isBeingDraggedY = false;
			final int x = (int) ev.getX(pointerIndex);
			final int xDiff = Math.abs(x - mLastMotionX);
			final int y = (int) ev.getY(pointerIndex);
			final int yDiff = Math.abs(y - mLastMotionY);
			if (xDiff > mTouchSlop) {
				isBeingDraggedX = true;
				mLastMotionX = x;
			}
			if (yDiff > mTouchSlop) {
				isBeingDraggedY = true;
				mLastMotionY = y;
			}
			if (isBeingDraggedX || isBeingDraggedY) {
				final ViewParent parent = getParent();
				if (parent != null) {
					parent.requestDisallowInterceptTouchEvent(true);
				}
				mIsBeingDragged = true;
			}
			break;
		}

		case MotionEvent.ACTION_DOWN: {
			final int x = (int) ev.getX();
			final int y = (int) ev.getY();
			/*
			 * Remember location of down touch. ACTION_DOWN always refers to
			 * pointer index 0.
			 */
			mLastMotionX = x;
			mLastMotionY = y;
			mActivePointerId = ev.getPointerId(0);
			break;
		}

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			// always does not intercept

			/* Release the drag */
			mIsBeingDragged = false;
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
		return mIsBeingDragged;
	};

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (!mIsBeingDragged && mIgnoreTouchEvent) {
			return false;
		}

		final int action = ev.getAction();

		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			if (getChildCount() == 0) {
				return false;
			}

			// Remember where the motion event started
			mLastMotionX = (int) ev.getX();
			mLastMotionY = (int) ev.getY();
			mActivePointerId = ev.getPointerId(0);
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

			boolean isBeingDraggedX = false;
			boolean isBeingDraggedY = false;
			final int x = (int) ev.getX(activePointerIndex);
			int deltaX = mLastMotionX - x;
			if (!mIsBeingDragged && Math.abs(deltaX) > mTouchSlop) {
				mIsBeingDragged = true;
				if (deltaX > 0) {
					deltaX -= mTouchSlop;
				} else {
					deltaX += mTouchSlop;
				}
			}
			final int y = (int) ev.getY(activePointerIndex);
			int deltaY = mLastMotionY - y;
			if (!mIsBeingDragged && Math.abs(deltaY) > mTouchSlop) {
				mIsBeingDragged = true;
				if (deltaY > 0) {
					deltaY -= mTouchSlop;
				} else {
					deltaY += mTouchSlop;
				}
			}
			if (isBeingDraggedX || isBeingDraggedY) {
				final ViewParent parent = getParent();
				if (parent != null) {
					parent.requestDisallowInterceptTouchEvent(true);
				}
				mIsBeingDragged = true;
			}
			if (mIsBeingDragged) {
				// Scroll to follow the motion event

				onMove(-deltaX, -deltaY);
				mLastMotionX = x;
				mLastMotionY = y;
			}
			break;
		}
		case MotionEvent.ACTION_UP: {
			if (mIsBeingDragged) {
				mActivePointerId = INVALID_POINTER;
				mIsBeingDragged = false;
			} else {
				onUnhandledClick(ev);
			}
			break;
		}
		case MotionEvent.ACTION_CANCEL:
			if (mIsBeingDragged && getChildCount() > 0) {
				mActivePointerId = INVALID_POINTER;
				mIsBeingDragged = false;
			} else if (!mIsBeingDragged) {
				onUnhandledClick(ev);
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

	protected int clamp(int src, int limit) {
		if (src > limit) {
			return limit;
		} else if (src < -limit) {
			return -limit;
		}
		return src;
	}

	public void onMove(int deltaX, int deltaY) {
		deltaX = (int) Math.round(deltaX * mSpeed);
		deltaY = (int) Math.round(deltaY * mSpeed);
		if (mOnMoveListener != null
				&& mOnMoveListener.onMove(this, deltaX, deltaY))
			return;

		move(deltaX, deltaY);
	}

	public abstract void move(int deltaX, int deltaY);

	public abstract void returnToHome();

	public abstract void rotate();

	public void onUnhandledClick(MotionEvent ev) {
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();

		if (mOnUnhandledClickListener != null)
			mOnUnhandledClickListener.onUnhandledClick(this, x, y);
	}

	public interface OnMoveListener {
		/**
		 * callback when onMove event.
		 * 
		 * @param deltaX
		 * @param deltaY
		 * @return if your listener handled this event, you should return true.
		 *         return false if did not handle or would like to execute
		 *         original behavior.
		 */
		public boolean onMove(FlyingView v, int deltaX, int deltaY);
	}

	private OnMoveListener mOnMoveListener = null;

	public void setOnMoveListener(OnMoveListener listener) {
		mOnMoveListener = listener;
	}

	public interface OnUnhandledClickListener {
		/**
		 * callback when happen unhandled click event.
		 * 
		 * @param v
		 * @param x
		 * @param y
		 */
		public void onUnhandledClick(FlyingView v, int x, int y);
	}

	private OnUnhandledClickListener mOnUnhandledClickListener = null;

	public void setOnUnhandledClickListener(OnUnhandledClickListener listener) {
		mOnUnhandledClickListener = listener;
	}
}