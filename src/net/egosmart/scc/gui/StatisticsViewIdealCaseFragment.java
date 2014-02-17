package net.egosmart.scc.gui;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.R;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.Statistics;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class StatisticsViewIdealCaseFragment extends Fragment {

	private SCCMainActivity activity;
	private Statistics stats;
	private Button updateButton;
	private EditText idealMale;
	private EditText idealFemale;
	private EditText idealDensity;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.statistics_ideal_case_view, container, false);
	}

	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		stats = new Statistics(PersonalNetwork.getInstance(activity),activity);
		activity = (SCCMainActivity) getActivity();
		//Get references
		idealMale = (EditText) activity.findViewById(R.id.ideal_man_percentatge_edit_text);
		idealFemale = (EditText) activity.findViewById(R.id.ideal_woman_percentatge_edit_text);
		idealDensity = (EditText) activity.findViewById(R.id.ideal_density_percentatge_edit_text);
		//Sets every edit text with the current ideal values, and only allows numeric input.
		idealMale.setText(Float.toString(stats.getIdealMalePercentage()));
		idealMale.setRawInputType(Configuration.KEYBOARD_12KEY);
		idealFemale.setText(Float.toString(stats.getIdealFemalePercentage()));
		idealFemale.setRawInputType(Configuration.KEYBOARD_12KEY);
		idealDensity.setText(Float.toString(stats.getIdealGraphDensity()));
		idealDensity.setRawInputType(Configuration.KEYBOARD_12KEY);
		updateButton = (Button) activity.findViewById(R.id.update_ideal_case_button);
		updateButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				updateIdealCase();
			}			
		});
	}
	
	/**
	 * Updates database with the modified values. 
	 */
	private void updateIdealCase() {
		Toast toast;
		try {
			stats.setIdealFemalePercentage(Float.parseFloat(idealFemale.getText().toString()));
			stats.setIdealMalePercentage(Float.parseFloat(idealMale.getText().toString()));
			stats.setIdealGraphDensity(Float.parseFloat(idealDensity.getText().toString()));
			toast = Toast.makeText(activity, R.string.correctly_updated_ideal_case_toast, Toast.LENGTH_SHORT);
			toast.show();
		}catch(Exception e){
			toast = Toast.makeText(activity, R.string.error_update_ideal_case_toast, Toast.LENGTH_SHORT);
			toast.show();
		}
		
	}

}