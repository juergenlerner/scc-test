/**
 * 
 */
package net.egosmart.scc.gui.util;

import java.util.Iterator;
import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.data.TimeVaryingAttributeValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Draws the network with random coordinates.
 * 
 * @author juergen
 *
 */
public class SimpleTimeVaryingAttributeView extends View {
	
	private SCCMainActivity activity;
	private static Paint timelinePaint = new Paint();
	private static Paint supportPaint = new Paint();
	private static Paint startTimePaint = new Paint();
	private TimeVaryingAttributeValues values;
	private long from;
	private long to;
	private float width;
	
	public SimpleTimeVaryingAttributeView(SCCMainActivity activity, TimeVaryingAttributeValues values, 
			long minTime, long maxTime) {
		super(activity);
		this.activity = activity;
		timelinePaint.setColor(Color.LTGRAY);
		supportPaint.setColor(Color.DKGRAY);
		startTimePaint.setColor(Color.BLUE);
		this.values = values;
		from = minTime;
		to = maxTime;
	}

	public void onDraw(Canvas canvas){
		width = canvas.getWidth();
		int height = canvas.getHeight();
		timelinePaint.setStrokeWidth(0.2f * height);
		supportPaint.setStrokeWidth(0.6f * height);
		startTimePaint.setStrokeWidth(0.9f * height);
		float y = 0.5f * height;
		canvas.drawLine(transform(from), y, transform(to), y, timelinePaint);
		Iterator<TimeInterval> it = values.getSupport().getIterator();
		while(it.hasNext()){
			TimeInterval interval = it.next();
			long start = interval.getStartTime();
			if(!interval.isLeftBounded()){
				start = from;
			}
			long end = interval.getEndTime();
			if(!interval.isRightBounded()){
				end = to;
			}
			canvas.drawLine(transform(start), y, transform(end), y, supportPaint);
			canvas.drawLine(transform(start), y, transform(start + (to-from)/80), y, startTimePaint);
		}
	}

	private float transform(long time){
		return ((float) (time - from))/((float) (to - from))*width; 
	}
	
}
