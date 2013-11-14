/**
 * 
 */
package net.egosmart.scc.gui.dialog;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.SCCProperties;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;

/**
 * @author juergen
 *
 */
public class ChangeSettingsDialog extends DialogFragment {

	private static SCCMainActivity activity;
	
	public static ChangeSettingsDialog getInstance(SCCMainActivity act){
		activity = act;
		return new ChangeSettingsDialog();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    //create and populate the view
	    View view = LayoutInflater.from(activity).inflate(R.layout.change_settings_dialog_view, null, false);
	    RadioButton interviewSettingsButtonRandom = (RadioButton) view.
	    		findViewById(R.id.interview_type_settings_random);
	    RadioButton interviewSettingsButtonEgonet = (RadioButton) view.
	    		findViewById(R.id.interview_type_settings_egonet);
	    final SCCProperties properties = SCCProperties.getInstance(activity);
	    boolean doEgonetInterview = properties.getInterviewSettingsEgonet();
	    if(doEgonetInterview)
	    	interviewSettingsButtonEgonet.setChecked(true);
	    else
	    	interviewSettingsButtonRandom.setChecked(true);
	    interviewSettingsButtonEgonet.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				RadioButton clickedButton = (RadioButton) v;
				if(clickedButton.isChecked()){
					properties.setInterviewSettingsEgonet(true);
				}
			}
		});
	    interviewSettingsButtonRandom.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				RadioButton clickedButton = (RadioButton) v;
				if(clickedButton.isChecked()){
					properties.setInterviewSettingsEgonet(false);
				}
			}
		});
	    builder.setView(view);
	    // Add action buttons
	     builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
	        	   
	               public void onClick(DialogInterface dialog, int id) {
	            	   //nothing to do
	               }
	           });      
	    return builder.create();
	}
		
}
