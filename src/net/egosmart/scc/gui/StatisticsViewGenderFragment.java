package net.egosmart.scc.gui;

import java.util.Arrays;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class StatisticsViewGenderFragment extends Fragment {

	private SCCMainActivity activity;
	//private Statistics stats;
	private XYPlot plot;

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
		/*stats = new Statistics(PersonalNetwork.getInstance(activity),activity);
		stats.calculateAllStatisticsAt(System.currentTimeMillis());*/
		
		plot = (XYPlot) activity.findViewById(R.id.mySimpleXYPlot);
		Number[] series1Numbers = {1, 8, 5, 2, 7, 4};
	    
	    XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(series1Numbers),          
                SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Series1");        
	   
	    LineAndPointFormatter series1Format = new LineAndPointFormatter();
        series1Format.setPointLabelFormatter(new PointLabelFormatter());
        series1Format.configure(activity,R.xml.line_point_formatter_with_plf1);	    
        plot.addSeries(series1, series1Format);

	}

}
