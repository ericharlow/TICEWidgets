/*
 * Copyright (C) 2010 Eric Harlow
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ericharlow.DragNDrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;

public class DragNDropListView extends ListView {

	boolean mDragMode;

	int mStartPosition;
	int mEndPosition;
	int mDragPointOffset;		//Used to adjust drag view location
	
	int hx;						//bsz - Used to store starting X coordinate right after onStartDrag event
	int hy;						//bsz - Used to store starting Y coordinate right after onStartDrag event
	
	ImageView mDragView;
	
	DropListener mDropListener;
	RemoveListener mRemoveListener;
	DragListener mDragListener;
	
	public DragNDropListView(Context context, AttributeSet attrs) {
		super(context, attrs);		
	}
	
	public void setDropListener(DropListener l) {
		mDropListener = l;
	}

	public void setRemoveListener(RemoveListener l) {
		mRemoveListener = l;
	}
	
	public void setDragListener(DragListener l) {
		mDragListener = l;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();
		
		if (action == MotionEvent.ACTION_DOWN && x < this.getWidth()/4) {
			mDragMode = true;
		}

		if (!mDragMode) 
			return super.onTouchEvent(ev);

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mStartPosition = pointToPosition(x,y);
				if (mStartPosition != INVALID_POSITION) {
					int mItemPosition = mStartPosition - getFirstVisiblePosition();
                    mDragPointOffset = y - getChildAt(mItemPosition).getTop();
                    mDragPointOffset -= ((int)ev.getRawY()) - y;
					startDrag(mItemPosition, x, y);
					drag(x, y);// replace 0 with x if desired
				}	
				break;
			case MotionEvent.ACTION_MOVE:
				drag(x, y);// replace 0 with x if desired
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP://bsz - Monitoring the release event, if width is 5/8th of the screen and height is almost the same, it is a drop event
				if (mDragView != null) {              	
		        	int deltaX = (int)Math.abs(hx-x);
		        	int deltaY = (int)Math.abs(hy - y);
		        	if ((deltaX > 5*mDragView.getWidth()/8) && (deltaY < mDragView.getHeight())) {
		        		mRemoveListener.onRemove(mStartPosition);
		            	stopDrag(mStartPosition - getFirstVisiblePosition());
		        	}
		        }
			default:
				mDragMode = false;
				mEndPosition = pointToPosition(x,y);
				stopDrag(mStartPosition - getFirstVisiblePosition());
				if (mDropListener != null && mStartPosition != INVALID_POSITION && mEndPosition != INVALID_POSITION) 
	        		 mDropListener.onDrop(mStartPosition, mEndPosition);
				break;
		}
		return true;
	}	
	
	// move the drag view
	private void drag(int x, int y) {
		
    	if (mDragView != null) {
			WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) mDragView.getLayoutParams();
			layoutParams.x = x;
			layoutParams.y = y - mDragPointOffset;
			WindowManager mWindowManager = (WindowManager) getContext()
					.getSystemService(Context.WINDOW_SERVICE);
			mWindowManager.updateViewLayout(mDragView, layoutParams);

			if (mDragListener != null)
				mDragListener.onDrag(x, y, this);// change null to "this" when ready to use
		}
	}

	// enable the drag view for dragging
	private void startDrag(int itemIndex, int x, int y) {
		stopDrag(itemIndex);
		
		hx = x;	//bsz - passing the X value to historical X
		hy = y;	//bsz - passing the Y value to historical Y

		View item = getChildAt(itemIndex);
		if (item == null) return;
		item.setDrawingCacheEnabled(true);
		if (mDragListener != null)
			mDragListener.onStartDrag(item, hx, hy);	//bsz - hx, hy must be passed for implementing object
		
        // Create a copy of the drawing cache so that it does not get recycled
        // by the framework when the list tries to clean up memory
        Bitmap bitmap = Bitmap.createBitmap(item.getDrawingCache());
        
        WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP;
        mWindowParams.x = 0;
        mWindowParams.y = y - mDragPointOffset;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = 0;
        
        mWindowParams.alpha=0.85f;		//bsz - added some alpha and dim setting for my fun, remove if it is ugly for you 
        mWindowParams.dimAmount=0.2f;	//
        
        Context context = getContext();
        ImageView v = new ImageView(context);
        v.setImageBitmap(bitmap);      

        WindowManager mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mWindowManager.addView(v, mWindowParams);
        mDragView = v;
        
        /*
         * bsz - Added a new afterStartDrag event to enable changes on the original location
         */
        
        if (mDragListener != null)
			mDragListener.afterStartDrag(item);
	}

	// destroy drag view
	private void stopDrag(int itemIndex) {
		if (mDragView != null) {
			if (mDragListener != null)
				mDragListener.onStopDrag(getChildAt(itemIndex));
            mDragView.setVisibility(GONE);
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mDragView);
            mDragView.setImageDrawable(null);
            mDragView = null;
        }
	}
}
