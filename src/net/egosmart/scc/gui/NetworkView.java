/**
 * 
 */
package net.egosmart.scc.gui;

import java.util.HashMap;
import java.util.HashSet;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.algo.NetworkLayout;
import net.egosmart.scc.data.LayoutSQLite;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.data.UnorderedDyad;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Draws the network with random coordinates.
 * 
 * @author juergen
 * 
 */
public class NetworkView extends View {

	private SCCMainActivity activity;
	private static Paint tiePaint = new Paint();
	private static Paint alterPaint = new Paint();
	private static Paint selectedAlterPaint = new Paint();
	private static Paint textPaint = new Paint();
	private static HashMap<String, Float> alterName2X = new HashMap<String, Float>();
	private static HashMap<String, Float> alterName2Y = new HashMap<String, Float>();
	private HashMap<String, Float> alterNameX = new HashMap<String, Float>();
	private HashMap<String, Float> alterNameY = new HashMap<String, Float>();
	private boolean isChanged = false;
	private long timeLastCheck;
	private float xTrans = 0f;
	private float yTrans = 0f;
	private float overallScale = 1f;
	private LayoutSQLite layout;
	

	public NetworkView(SCCMainActivity activity) {
		super(activity);
		layout = LayoutSQLite.getInstance(activity);
		timeLastCheck = System.currentTimeMillis();
		// scale and pan the layout as it used to be
		float[] trans = layout.getTranslate();
		xTrans = trans[0];
		yTrans = trans[1];
		overallScale = layout.getScale();
		
		setOnTouchListener(new OnTouchListener() {
			/**
			 * variables for multi-touch gestures
			 */
			private static final String TAG = "Touch";
			

			// We can be in one of these 3 states
			static final int NONE = 0;
			static final int DRAG = 1;
			static final int ZOOM = 2;
			int mode = NONE;
			PointF start0 = new PointF();
			PointF start1 = new PointF();
			PointF mid = new PointF();
			float oldDist = 1f;

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				
				// Handle touch events here...
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
					start0.set(event.getX(), event.getY());
					Log.d(TAG, "mode=DRAG");
					mode = DRAG;
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
					mode = NONE;
					Log.d(TAG, "mode=NONE");
					break;
				case MotionEvent.ACTION_POINTER_DOWN:
					start1.set(event.getX(1), event.getY(1));
					oldDist = spacing(event);
					Log.d(TAG, "oldDist=" + oldDist);
					if (oldDist > 10f) {
						midPoint(mid, event);
						mode = ZOOM;
						Log.d(TAG, "mode=ZOOM");
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (System.currentTimeMillis() - timeLastCheck > 10) {
						if (mode == DRAG) {
							Float dx = event.getX() - start0.x;
							Float dy = event.getY() - start0.y;
							xTrans += dx;
							yTrans += dy;
							translate(dx, dy);
							start0.set(event.getX(), event.getY());
						} else if (mode == ZOOM) {
							float newDist = spacing(event);
							Log.d(TAG, "newDist=" + newDist);
							if (Math.abs(newDist - oldDist) > 10f) {
								float scale = newDist / oldDist;
								oldDist = newDist;
								overallScale *= scale;
								scale(scale);
								float dx = mid.x * (1 - scale);
								float dy = mid.y * (1 - scale);
								translate(dx, dy);
							}
						}
					}

					break;
				}

				v.invalidate();

				return true; // indicate event was handled
			}

			private float spacing(MotionEvent event) {
				float x = event.getX(0) - event.getX(1);
				float y = event.getY(0) - event.getY(1);
				return FloatMath.sqrt(x * x + y * y);
			}

			private void midPoint(PointF point, MotionEvent event) {
				float x = event.getX(0) + event.getX(1);
				float y = event.getY(0) + event.getY(1);
				point.set(x / 2, y / 2);
			}

			
		});
		this.activity = activity;
		tiePaint.setColor(Color.LTGRAY);
		tiePaint.setStrokeWidth(5f);
		alterPaint.setColor(Color.GRAY);
		selectedAlterPaint.setARGB(255, 150, 30, 30);
		textPaint.setColor(Color.DKGRAY);
		textPaint.setTextSize(14f);
	}

	

	/**
	 * adjusts the coordinates according to the recent zoom and pan events
	 */
	public void preserveLayout() {
		boolean temp = isChanged;
		translate(xTrans, yTrans);
		scale(overallScale);
		isChanged = temp;
	}

	/**
	 * moves the coordinates of the graph in x and y direction
	 * 
	 * @param dx
	 * @param dy
	 */
	public void translate(Float dx, Float dy) {
		for (String node : alterName2X.keySet()) {
			Float x = alterName2X.get(node);
			Float y = alterName2Y.get(node);
			alterName2X.put(node, x + dx);
			alterName2Y.put(node, y + dy);
		}
		isChanged = true;
		layout.setTranslate(xTrans, yTrans);
	}

	/**
	 * scales the coordinates by a certain factor
	 * 
	 * @param scale
	 */
	public void scale(Float scale) {
		for (String node : alterName2X.keySet()) {
			Float x = alterName2X.get(node);
			Float y = alterName2Y.get(node);
			alterName2X.put(node, x * scale);
			alterName2Y.put(node, y * scale);
		}
		isChanged = true;
		layout.setScale(overallScale);
	}

	public void onDraw(Canvas canvas) {
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		if (!isChanged) {
			alterName2X.clear();
			alterName2Y.clear();

			fillCoordinates(width, height);
			isChanged = true;
		}
		drawNetwork(canvas, width, height);
	}

	/*
	 * Fills stored or newly computed coordinates into alterName2X and
	 * alterName2Y
	 */
	private void fillCoordinates(int width, int height) {
		PersonalNetwork network = PersonalNetwork.getInstance(activity);
		Float ratio = (float) width / height;
		NetworkLayout layout = new NetworkLayout(activity, ratio);
		alterNameX = layout.getXCoordinates();
		alterNameY = layout.getYCoordinates();
		for (String alterName : network.getAltersAt(TimeInterval
				.getCurrentTimePoint())) {
			float x = (alterNameX.get(alterName) * 0.9f + 0.05f) * width;
			float y = (alterNameY.get(alterName) * 0.9f + 0.05f) * height;
			alterName2X.put(alterName, x);
			alterName2Y.put(alterName, y);
		}
		preserveLayout();
	}

	private void drawNetwork(Canvas canvas, int width, int height) {
		PersonalNetwork network = PersonalNetwork.getInstance(activity);
		HashSet<UnorderedDyad> ties = network.getUndirectedTiesAt(TimeInterval
				.getCurrentTimePoint());
		for (UnorderedDyad tie : ties) {
			String sourceName = tie.source();
			String targetName = tie.target();
			float x_src = alterName2X.get(sourceName);
			float x_trg = alterName2X.get(targetName);
			float y_src = alterName2Y.get(sourceName);
			float y_trg = alterName2Y.get(targetName);
			canvas.drawLine(x_src, y_src, x_trg, y_trg, tiePaint);
		}
		String selectedAlter = network.getSelectedAlter();
		for (String alterName : alterName2X.keySet()) {
			float x = alterName2X.get(alterName);
			float y = alterName2Y.get(alterName);
			float radius = Math.min(width, height) / 30;
			if (selectedAlter != null && selectedAlter.equals(alterName)) {
				canvas.drawCircle(x, y, radius, selectedAlterPaint);
			} else {
				canvas.drawCircle(x, y, radius, alterPaint);
			}
			canvas.drawText(alterName, x, y, textPaint);
		}
	}

}
