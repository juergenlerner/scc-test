/**
 * 
 */
package net.egosmart.scc.gui.util;

import java.util.Iterator;
import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Lifetime;
import net.egosmart.scc.data.TimeInterval;
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
public class SimpleLifetimeView extends View {
	
	private SCCMainActivity activity;
	private static Paint tiePaint = new Paint();
	private Lifetime lifetime;
	private long from;
	private long to;
	private float width;
	
	public SimpleLifetimeView(SCCMainActivity activity, Lifetime lifetime, long minTime, long maxTime) {
		super(activity);
		this.activity = activity;
		tiePaint.setColor(Color.GRAY);
		this.lifetime = lifetime;
		from = minTime;
		to = maxTime;
	}

	public void onDraw(Canvas canvas){
		width = canvas.getWidth();
		int height = canvas.getHeight();
		tiePaint.setStrokeWidth(0.9f * height);
		float y = 0.5f * height;
		Iterator<TimeInterval> it = lifetime.getIterator();
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
			canvas.drawLine(transform(start), y, transform(end), y, tiePaint);
		}
	}

	private float transform(long time){
		return ((float) (time - from))/((float) (to - from))*width; 
	}
	
}
