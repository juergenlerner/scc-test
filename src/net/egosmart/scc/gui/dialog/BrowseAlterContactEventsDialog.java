/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.text.DateFormat;
import java.util.Date;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.EgoAlterDyad;
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
public class BrowseAlterContactEventsDialog extends DialogFragment {

	private static SCCMainActivity activity;
	private static long eventTime;
	
	public static BrowseAlterContactEventsDialog getInstance(SCCMainActivity act, long timeOfSelected){
		activity = act;
		eventTime = timeOfSelected;
		return new BrowseAlterContactEventsDialog();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
	    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	    PersonalNetwork history = PersonalNetwork.getInstance(activity);
	    String selectedAlter = PersonalNetwork.getInstance(activity).getSelectedAlter();
	    String datumID = history.getAttributeDatumIDAt(eventTime, 
	    		PersonalNetwork.getEgoAlterContactEventAttributeName(), 
	    		EgoAlterDyad.getOutwardInstance(selectedAlter));
	    Date date = new Date(eventTime);
	    DateFormat dateFormat =  android.text.format.DateFormat.getLongDateFormat(activity);
		DateFormat timeFormat =  android.text.format.DateFormat.getTimeFormat(activity);
	    //create and populate the view
	    final View view = LayoutInflater.from(activity).inflate(R.layout.browse_alter_contact_events_dialog_view, null, false);
	    //set date and time
	    TextView dateView = (TextView) view.findViewById(R.id.browse_alter_contact_events_dialog_date_view);
	    dateView.setText(dateFormat.format(date));
	    TextView timeView = (TextView) view.findViewById(R.id.browse_alter_contact_events_dialog_time_view);
	    timeView.setText(timeFormat.format(date));
		//show contact form
		String contactForm = history.getSecondaryAttributeValue(datumID, 
				PersonalNetwork.getSecondaryAttributeNameContactForm());
		TextView formView = (TextView) view.findViewById(R.id.contact_event_view_field_form_value);
		formView.setText(contactForm);
		//show contact content
		String contactContent = history.getSecondaryAttributeValue(datumID, 
				PersonalNetwork.getSecondaryAttributeNameContactContent());
		TextView contentView = (TextView) view.findViewById(R.id.contact_event_view_field_content_value);
		contentView.setText(contactContent);
		//show contact atmosphere
		String contactAtmos = history.getSecondaryAttributeValue(datumID, 
				PersonalNetwork.getSecondaryAttributeNameContactAtmosphere());
		TextView atmosView = (TextView) view.findViewById(R.id.contact_event_view_field_atmosphere_value);
		atmosView.setText(contactAtmos);
		//show text
		String text = history.getSecondaryAttributeValue(datumID, 
				PersonalNetwork.getSecondaryAttributeNameText());
		if(text != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(text)){
			TextView contactTextView = (TextView) view.findViewById(R.id.contact_event_view_field_text);
			contactTextView.setText(text);
		}
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
