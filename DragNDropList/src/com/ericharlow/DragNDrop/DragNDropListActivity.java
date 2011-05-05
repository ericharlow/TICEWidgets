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

import java.util.ArrayList;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DragNDropListActivity extends ListActivity {
	
	boolean changed; //To store list changes - currently the first change event changes the variable's value from false to true  
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dragndroplistview);
        
        ArrayList<String> content = new ArrayList<String>(mListContent.length);
        for (int i=0; i < mListContent.length; i++) {
        	content.add("item " + i);
        }
        
        setListAdapter(new DragNDropAdapter(this, new int[]{R.layout.dragitem}, new int[]{R.id.TextView01}, content));//new DragNDropAdapter(this,content)
        
        /*
         * Added changelistener object to handle change events from DragNDropAdapter
         */
        
        ListAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof DragNDropAdapter) {
        	((DragNDropAdapter) listAdapter).setChangeListener(mChangeListener);
        }
        
        ListView listView = getListView();
        
        if (listView instanceof DragNDropListView) {
        	((DragNDropListView) listView).setDropListener(mDropListener);
        	((DragNDropListView) listView).setRemoveListener(mRemoveListener);
        	((DragNDropListView) listView).setDragListener(mDragListener);
        }
        
        changed = false; //Setting the changed variable to false after the activity has been initialized
    }
    
    @Override
    public void onDestroy(){
    	// bsz - Dialog can be presented here if list change event must be handled before exiting
    	super.onDestroy();
    }
    
    /*
     * bsz - Creating a changelistener object to handle events from DragNDropActivity
     */
    private ChangeListener mChangeListener = 
    	new ChangeListener() {
			
			@Override
			public void onChange() {
				Toast.makeText(getApplicationContext(), "Changed!", Toast.LENGTH_SHORT).show(); //bsz - Demonstrating the event with a toast
				changed = true;
			}
		}; 

	private DropListener mDropListener = 
		new DropListener() {
        public void onDrop(int from, int to) {
        	ListAdapter adapter = getListAdapter();
        	if (adapter instanceof DragNDropAdapter) {
        		((DragNDropAdapter)adapter).onDrop(from, to);
        		getListView().invalidateViews();
        	}
        }
    };
    
    private RemoveListener mRemoveListener =
        new RemoveListener() {
        public void onRemove(int which) {
        	ListAdapter adapter = getListAdapter();
        	if (adapter instanceof DragNDropAdapter) {
        		((DragNDropAdapter)adapter).onRemove(which);
        		getListView().invalidateViews();
        	}
        }
    };
    
    private DragListener mDragListener =
    	new DragListener() {

    	int backgroundColor = 0xe0444444;			// bsz - gray background
    	int defaultBackgroundColor = 0xe0444444;	// bsz - gray background
    	int deleteBackgroundColor = 0xe0441010;		// bsz - red when deleting range reached
    	
    	boolean inDeleteRange = false;				// bsz - for monitoring the deleting range
    	
    	int itemViewWidth;
    	int itemViewHeight;
    	View dragView;
    	
    	float origTextSize;		// bsz - original text size before dragging
    	int origTextWidth;		// bsz - original width of TextView before dragging
    	int origX;				// bsz - start X position when onStartDrag occured
    	int origY;				// bsz - start Y position when onStartDrag occured
    	
    	/*
    	 * bsz - this is completely rewritten by me. I think it is clear, I played with 
    	 * the original object in the list, stored the original values after dragging
    	 * started, restored when stopped.
    	 * Also extended the onDrag event, x and y parameters are compared to historical
    	 * x and y parameters to determining the deleting range.
    	 */
    	
    	public void onDrag(int x, int y, ListView listView) {
			boolean prevInDeleteRange = inDeleteRange;
			
			int deltaX = (int) Math.abs(origX - x);
			int deltaY = (int) Math.abs(origY - y);

			if ((deltaX > 5*itemViewWidth/8) && (deltaY < itemViewHeight)) {
				inDeleteRange = true;
				if (!prevInDeleteRange) {
					dragView.setBackgroundColor(deleteBackgroundColor);
					TextView tv = (TextView)dragView.findViewById(R.id.TextView01);
					tv.setText("Release here to delete");
				}
			} else {
				inDeleteRange = false;
				if (prevInDeleteRange) {
					dragView.setBackgroundColor(backgroundColor);
					TextView tv = (TextView)dragView.findViewById(R.id.TextView01);
					tv.setText("Drag vertical to new position or right to delete");
				}
			}
		}

		public void onStartDrag(View itemView, int x, int y) {
			//itemView.setVisibility(View.INVISIBLE);
			defaultBackgroundColor = itemView.getDrawingCacheBackgroundColor();
			itemView.setBackgroundColor(backgroundColor);
			itemViewHeight = itemView.getHeight();
			itemViewWidth = itemView.getWidth();
			origX = x;
			origY = y;
			dragView = itemView;
			ImageView iv = (ImageView)itemView.findViewById(R.id.ImageView01);
			if (iv != null) iv.setVisibility(View.INVISIBLE);
		}

		public void onStopDrag(View itemView) {
			//itemView.setVisibility(View.VISIBLE);
			itemView.setBackgroundColor(defaultBackgroundColor);
			ImageView iv = (ImageView)itemView.findViewById(R.id.ImageView01);
			if (iv != null) iv.setVisibility(View.VISIBLE);
			TextView tv = (TextView)itemView.findViewById(R.id.TextView01);
			tv.setWidth(origTextWidth);
			tv.setTextSize(origTextSize);
		}
    	
		public void afterStartDrag(View itemView) {
			TextView tv = (TextView)itemView.findViewById(R.id.TextView01);
			origTextWidth = tv.getWidth();
			origTextSize = tv.getTextSize();
			tv.setWidth(itemViewWidth / 2);
			tv.setSingleLine(false);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
			tv.setText("Drag vertical to new position or right to delete");
		}
    };

    private static String[] mListContent = new String[10];
    //private static String[] mListContent={"Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6", "Item 7"};
	
}