package net.egosmart.scc.gui;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;

import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BarRenderer.BarRenderStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.R;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.Statistics;
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

public class StatisticsViewGenderFragment extends Fragment {

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
		stats = new Statistics(PersonalNetwork.getInstance(activity),activity);
		stats.calculateAllStatisticsAt(System.currentTimeMillis());
		
		formatter1 = new BarFormatter(Color.argb(200, 100, 150, 100), Color.LTGRAY);
		formatter2 = new BarFormatter(Color.argb(200, 100, 100, 150), Color.LTGRAY);
		
		Number[] successCaseArray = {stats.getManPercentage(),null ,stats.getWomanPercentage()};
		Number[] idealCaseArray = {stats.getIdealManPercentage(),null ,stats.getIdealGraphDensity()};
		
		plot = (XYPlot) activity.findViewById(R.id.plotView);
		
		SimpleXYSeries successCaseSeries = new SimpleXYSeries(Arrays.asList(successCaseArray), 
				SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, activity.getString(R.string.statistics_success_case_text));
		SimpleXYSeries idealCaseSeries = new SimpleXYSeries(Arrays.asList(idealCaseArray), 
				SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, activity.getString(R.string.statistics_ideal_case_text));
		
		plot.setTitle(activity.getString(R.string.statistics_control_show_gender));
		
		//We don't want to show anything at domain or range label.
		plot.setDomainLabel("");
		plot.setRangeLabel("");
		
		plot.addSeries(successCaseSeries, formatter1);
		plot.addSeries(idealCaseSeries, formatter2);
		
		plot.getGraphWidget().setDomainValueFormat(new GenderFormatter());
		plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1);
        plot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
	    plot.setDomainLowerBoundary(-1, BoundaryMode.FIXED);
	    plot.setDomainUpperBoundary(3, BoundaryMode.FIXED);
	    plot.setRangeTopMin(1);
		
		barRenderer = (BarRenderer) plot.getRenderer(BarRenderer.class);
		barRenderer.setBarRenderStyle(BarRenderStyle.SIDE_BY_SIDE);
		barRenderer.setBarWidth(150);  
	}	
	
	private class GenderFormatter extends Format {
		public StringBuffer format(Object object, StringBuffer buffer,
				FieldPosition field) {
			int parsedInt = Math.round(Float.parseFloat(object.toString()));
	        String labelString;
	        
	        if(parsedInt == 0)
	        	labelString = activity.getString(R.string.alter_attribute_gender_man);
	        else if(parsedInt == 2)
	        	labelString = activity.getString(R.string.alter_attribute_gender_woman);
	        else
	        	labelString = "";

	        buffer.append(labelString);
	        return buffer;
		}

		@Override
		public Object parseObject(String string, ParsePosition position) {
			return null;
		}
	}
}
