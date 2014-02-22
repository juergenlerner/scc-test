package net.egosmart.scc.gui;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;

import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.androidplot.xy.BarRenderer.BarRenderStyle;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.Statistics;
import net.egosmart.scc.R;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author Josep
 *
 */

public class StatisticsViewDensityFragment extends Fragment {

	private SCCMainActivity activity;
	private Statistics stats;
	private XYPlot plot;
	private BarFormatter formatter1;
	private BarFormatter formatter2;
	private BarRenderer barRenderer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.statistics_view, container, false);
	}

	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		activity = (SCCMainActivity) getActivity();
		//TODO: there is no need to recalculate any stat if the network has not changed.
		stats = new Statistics(PersonalNetwork.getInstance(activity),activity);
		stats.calculateAllStatisticsAt(System.currentTimeMillis());
		
		formatter1 = new BarFormatter(Color.argb(200, 100, 150, 100), Color.LTGRAY);
		formatter1.setPointLabelFormatter(new PointLabelFormatter(Color.DKGRAY));
		formatter2 = new BarFormatter(Color.argb(200, 100, 100, 150), Color.LTGRAY);
		formatter2.setPointLabelFormatter(new PointLabelFormatter(Color.DKGRAY));
		
		//Just rounding to 4 decimals.
		float graphDensity = (float)Math.round(stats.getGraphDensity() * 10000) / 10000;
		float idealDensity = (float)Math.round(stats.getIdealGraphDensity() * 10000) / 10000;
		Number[] successCaseArray = {graphDensity};
		Number[] idealCaseArray = {idealDensity};
		Number[] xValuesSuccess = {0};
		Number[] xValuesIdeal = {1};
		
		plot = (XYPlot) activity.findViewById(R.id.plotView);
		
		SimpleXYSeries successCaseSeries = new SimpleXYSeries(Arrays.asList(xValuesSuccess),
				Arrays.asList(successCaseArray), activity.getString(R.string.statistics_success_case_text));
		SimpleXYSeries idealCaseSeries = new SimpleXYSeries(Arrays.asList(xValuesIdeal),
				Arrays.asList(idealCaseArray), activity.getString(R.string.statistics_ideal_case_text));
		
		plot.setTitle(activity.getString(R.string.statistics_control_show_density));
		
		//We don't want to show anything at domain or range label.
		plot.setDomainLabel("");
		plot.setRangeLabel("");
		
		plot.addSeries(successCaseSeries, formatter1);
		plot.addSeries(idealCaseSeries, formatter2);
		
		//There is no need to show any value at domain axis.
		plot.getGraphWidget().getDomainLabelPaint().setColor(Color.TRANSPARENT);
		plot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.TRANSPARENT);
		
		plot.getGraphWidget().setRangeValueFormat(new DensityYFormatter());
		plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1);
		plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.1);
        plot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
	    plot.setDomainLowerBoundary(-1, BoundaryMode.FIXED);
	    plot.setDomainUpperBoundary(2, BoundaryMode.FIXED);
	    //1 + 0.1 to allow 1.0 value at the top of bar.
	    plot.setRangeTopMin(1.05);
		
		barRenderer = (BarRenderer) plot.getRenderer(BarRenderer.class);
		barRenderer.setBarRenderStyle(BarRenderStyle.SIDE_BY_SIDE);
		barRenderer.setBarWidth(200);  
	}	
	
	private class DensityYFormatter extends Format {

		@Override
		public StringBuffer format(Object object, StringBuffer buffer,
				FieldPosition field) {
			float parsedFloat = Float.parseFloat(object.toString());
			String labelString;
			
			if(parsedFloat > 1)
				labelString = "";
			else 
				labelString = Float.toString(parsedFloat);
			buffer.append(labelString);
			return buffer;
		}

		@Override
		public Object parseObject(String arg0, ParsePosition arg1) {
			return null;
		}
		
	}

}
