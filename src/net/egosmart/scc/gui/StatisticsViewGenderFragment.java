package net.egosmart.scc.gui;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;

import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BarRenderer.BarRenderStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.PointLabelFormatter;
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
		formatter1.setPointLabelFormatter(new PointLabelFormatter(Color.DKGRAY));
		formatter2 = new BarFormatter(Color.argb(200, 100, 100, 150), Color.LTGRAY);
		formatter2.setPointLabelFormatter(new PointLabelFormatter(Color.DKGRAY));

		//Just rounding to 4 decimals.
		float malePercentage = (float)Math.round(stats.getMalePercentage() * 10000) / 10000;
		float femalePercentage = (float)Math.round(stats.getFemalePercentage() * 10000) / 10000;
		float idealMalePercentage = (float)Math.round(stats.getIdealMalePercentage() * 10000) / 10000;
		float idealFemalePercentage = (float)Math.round(stats.getIdealFemalePercentage() * 10000) / 10000;
		
		Number[] successCaseArray = {malePercentage,femalePercentage};
		Number[] idealCaseArray = {idealMalePercentage,idealFemalePercentage};
		Number[] xValuesSuccess = {0,3};
		Number[] xValuesIdeal = {1, 4};
		
		plot = (XYPlot) activity.findViewById(R.id.plotView);
		
		SimpleXYSeries successCaseSeries = new SimpleXYSeries(Arrays.asList(xValuesSuccess),
				Arrays.asList(successCaseArray), activity.getString(R.string.statistics_success_case_text));
		SimpleXYSeries idealCaseSeries = new SimpleXYSeries(Arrays.asList(xValuesIdeal),
				Arrays.asList(idealCaseArray), activity.getString(R.string.statistics_ideal_case_text));
		
		plot.setTitle(activity.getString(R.string.statistics_control_show_gender));

		//We don't want to show anything at domain or range label.
		plot.setDomainLabel("");
		plot.setRangeLabel("");
		
		plot.addSeries(successCaseSeries, formatter1);
		plot.addSeries(idealCaseSeries, formatter2);

		plot.getGraphWidget().setDomainValueFormat(new GenderXFormatter());
		plot.getGraphWidget().setRangeValueFormat(new GenderYFormatter());
		
		plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 0.5);
		plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 0.1);
        plot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
	    plot.setDomainLowerBoundary(-1, BoundaryMode.FIXED);
	    plot.setDomainUpperBoundary(5, BoundaryMode.FIXED);
	    //1 + 0.05 to make room for value at the top of bar in case to be 1.0
	    plot.setRangeTopMin(1.05);
	    
		barRenderer = (BarRenderer) plot.getRenderer(BarRenderer.class);
		barRenderer.setBarRenderStyle(BarRenderStyle.SIDE_BY_SIDE);
		barRenderer.setBarWidth(100);  
	}	
	
	private class GenderXFormatter extends Format {
		public StringBuffer format(Object object, StringBuffer buffer,
				FieldPosition field) {
			float parsedFloat = Float.parseFloat(object.toString());
	        String labelString;
	        
	        if(parsedFloat == 0.5)
	        	labelString = activity.getString(R.string.alter_attribute_gender_male);
	        else if(parsedFloat == 3.5)
	        	labelString = activity.getString(R.string.alter_attribute_gender_female);
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
	
	private class GenderYFormatter extends Format {

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
