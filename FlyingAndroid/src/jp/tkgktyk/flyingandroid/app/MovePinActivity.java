package jp.tkgktyk.flyingandroid.app;

import jp.tkgktyk.flyingandroid.PinPosition;
import jp.tkgktyk.flyingandroid.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnLongClickListener;
import android.widget.ToggleButton;

public class MovePinActivity extends Activity {

	private ToggleButton mPin;
	private TargetingLayout mContainer;

	private PinPosition mPinPosition;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_move_pin);

		mPinPosition = new PinPosition(this);

		mContainer = (TargetingLayout) findViewById(R.id.container);
		mContainer.setTarget(R.id.pin);
		mPin = (ToggleButton) findViewById(R.id.pin);

		mPin.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				v.startDrag(null, new View.DragShadowBuilder(v), v, 0);
				return true;
			}
		});
		mContainer.setOnDragListener(new OnDragListener() {
			private void updatePosition(DragEvent event) {
				float x = event.getX();
				float y = event.getY();
				mPinPosition.setXp(mContainer, x);
				mPinPosition.setYp(mContainer, y);
				move();
			}

			@Override
			public boolean onDrag(View v, DragEvent event) {
				switch (event.getAction()) {
				case DragEvent.ACTION_DRAG_STARTED: {
					updatePosition(event);
					break;
				}
				case DragEvent.ACTION_DRAG_LOCATION: {
					updatePosition(event);
					break;
				}
				case DragEvent.ACTION_DROP: {
					updatePosition(event);
					mPinPosition.save(v.getContext());
					break;
				}
				default:
					break;
				}
				// must return true for dragging progress
				return true;
			}
		});

		move();
	}

	private void move() {
		mPinPosition.apply(mContainer);
		mContainer.invalidate();
	}
}
