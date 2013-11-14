/**
 * 
 */
package net.egosmart.scc.gui.dialog;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * @author juergen
 *
 */
public class AddAlterDialog extends DialogFragment {

	private static SCCMainActivity activity;
	
	public static AddAlterDialog getInstance(SCCMainActivity act){
		activity = act;
		return new AddAlterDialog();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    //create and populate the table
	    View view = LayoutInflater.from(activity).inflate(R.layout.add_alter_dialog_view, null, false);
		final EditText alterNameEditText = (EditText) view.findViewById(R.id.alter_name_editText);
	    Button addAlterButton = (Button) view.findViewById(R.id.add_alter_button);
	    addAlterButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String alterName = alterNameEditText.getText().toString().trim();
				if(alterName.length() > 0){
					PersonalNetwork network = PersonalNetwork.getInstance(activity);
					network.addToLifetimeOfAlter(TimeInterval.getRightUnboundedFromNow(), alterName);
					activity.onAlterSelected(alterName);
					dismiss();
				}
			}
		});
	    builder.setView(view);
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(alterNameEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	    // Add action buttons
	     builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	        	   
	               public void onClick(DialogInterface dialog, int id) {
	            	   //nothing to do
	               }
	           });      
	    return builder.create();
	}
		
}
