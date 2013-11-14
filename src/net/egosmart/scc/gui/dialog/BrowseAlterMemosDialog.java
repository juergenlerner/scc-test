/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.text.DateFormat;
import java.util.Date;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * @author juergen
 *
 */
public class BrowseAlterMemosDialog extends DialogFragment {

	private static SCCMainActivity activity;
	private static long memoTime;
	
	public static BrowseAlterMemosDialog getInstance(SCCMainActivity act, long timeOfSelected){
		activity = act;
		memoTime = timeOfSelected;
		return new BrowseAlterMemosDialog();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    PersonalNetwork history = PersonalNetwork.getInstance(activity);
	    String selectedAlter = PersonalNetwork.getInstance(activity).getSelectedAlter();
	    String datumID = history.getAttributeDatumIDAt(memoTime, 
	    		PersonalNetwork.getAlterMemosAttributeName(), 
	    		Alter.getInstance(selectedAlter));
	    String memoText = history.getSecondaryAttributeValue(datumID, 
	    		PersonalNetwork.getSecondaryAttributeNameText());
//	    if(memoText == null || memoText.equals(PersonalNetwork.VALUE_NOT_ASSIGNED))
//	    	memoText = history.getRecentAlterAttributeValues(
//	    			PersonalNetworkHistory.getAlterMemosAttributeName(), selectedAlter, 1).getNewestValue();
	    Date date = new Date(memoTime);
	    DateFormat dateFormat =  android.text.format.DateFormat.getLongDateFormat(activity);
		DateFormat timeFormat =  android.text.format.DateFormat.getTimeFormat(activity);
	    //create and populate the view
	    final View view = LayoutInflater.from(activity).inflate(R.layout.browse_alter_memos_dialog_view, null, false);
	    //set date and time
	    TextView dateView = (TextView) view.findViewById(R.id.browse_alter_memos_dialog_date_view);
	    dateView.setText(dateFormat.format(date));
	    TextView timeView = (TextView) view.findViewById(R.id.browse_alter_memos_dialog_time_view);
	    timeView.setText(timeFormat.format(date));
	    TextView memoTextView = (TextView) view.findViewById(R.id.browse_alter_memos_text);
	    memoTextView.setText(memoText);
	    builder.setView(view);
	    // Add action buttons
	     builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
	        	   
	               public void onClick(DialogInterface dialog, int id) {
	            	   //nothing to do (dialog closes)
	               }
	           });      
	    return builder.create();
	}
		
}
