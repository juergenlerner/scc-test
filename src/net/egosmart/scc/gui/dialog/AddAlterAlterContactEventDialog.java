/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.AlterAlterDyad;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

/**
 * @author juergen
 *
 */
public class AddAlterAlterContactEventDialog extends DialogFragment {

	private static SCCMainActivity activity;
	private static long eventTime;
	private static boolean isExistingEvent = true;
	private static long existingEventTime;
	private static boolean existingEventHasToBeRemoved = false;

	public AddAlterAlterContactEventDialog(){
		super();
		isExistingEvent = true;
		existingEventHasToBeRemoved = false;
	}

	public static AddAlterAlterContactEventDialog getInstance(SCCMainActivity act){
		AddAlterAlterContactEventDialog instance = new AddAlterAlterContactEventDialog();
		isExistingEvent = false;
		eventTime = System.currentTimeMillis();
		existingEventTime = eventTime;
		activity = act;
		return instance;
	}

	public static AddAlterAlterContactEventDialog getInstance(SCCMainActivity act, long eventTime){
		AddAlterAlterContactEventDialog.eventTime = eventTime;
		existingEventTime = eventTime;
		activity = act;
		return new AddAlterAlterContactEventDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		Date date = new Date(eventTime);
		final DateFormat dateFormat =  android.text.format.DateFormat.getLongDateFormat(activity);
		final DateFormat timeFormat =  android.text.format.DateFormat.getTimeFormat(activity);
		//create and populate the view
		//TODO: wrong name - but will do at the moment
		final View view = LayoutInflater.from(activity).inflate(R.layout.add_alter_contact_event_dialog_view, null, false);
		//set date and time
		final TextView dateView = (TextView) view.findViewById(R.id.add_alter_contact_event_dialog_date_view);
		dateView.setText(dateFormat.format(date));
		final TextView timeView = (TextView) view.findViewById(R.id.add_alter_contact_event_dialog_time_view);
		timeView.setText(timeFormat.format(date));
		initDataIfEventExists(view);
		//access modify time button
		Button modifyTimeButton = (Button) view.findViewById(R.id.modify_alter_contact_event_time_button);
		modifyTimeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new DialogFragment(){
					public Dialog onCreateDialog(Bundle savedInstanceState) {
						super.onCreateDialog(savedInstanceState);
						AlertDialog.Builder builder = new AlertDialog.Builder(activity);
						LinearLayout contentView = new LinearLayout(activity);
						contentView.setOrientation(LinearLayout.VERTICAL);//TODO: set to horizontal if in landscape orientation
						GregorianCalendar initCalendar = new GregorianCalendar();
						initCalendar.setTimeInMillis(eventTime);
						final DatePicker datePicker = new DatePicker(activity);
						datePicker.setCalendarViewShown(false);//TODO: set to true if there is enough space
						datePicker.init(initCalendar.get(Calendar.YEAR), 
								initCalendar.get(Calendar.MONTH), 
								initCalendar.get(Calendar.DAY_OF_MONTH), 
								null);
						contentView.addView(datePicker);
						final TimePicker timePicker = new TimePicker(activity);
						timePicker.setCurrentHour(initCalendar.get(Calendar.HOUR_OF_DAY));
						timePicker.setCurrentMinute(initCalendar.get(Calendar.MINUTE));
						contentView.addView(timePicker);
						builder.setView(contentView);
						builder.setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								int year = datePicker.getYear();
								int month = datePicker.getMonth();
								int day = datePicker.getDayOfMonth();
								int hour = timePicker.getCurrentHour();
								int minute = timePicker.getCurrentMinute();
								GregorianCalendar newCal = new GregorianCalendar(year, month, day, hour, minute);
								long newEventTime = newCal.getTimeInMillis();
								if(eventTime != newEventTime){
									eventTime = newEventTime;
									Date newDate = newCal.getTime();
									dateView.setText(dateFormat.format(newDate));
									timeView.setText(timeFormat.format(newDate));
									if(isExistingEvent)
										existingEventHasToBeRemoved = true;
								}
							}
						});
						builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								//nothing to do
							}
						});
						return builder.create();
					}
				}.show(activity.getSupportFragmentManager(), null);
			}
		});
		builder.setView(view);
		// Add action buttons
		builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) {
				String selectedAlter = PersonalNetwork.getInstance(activity).getSelectedAlter();
				String selectedSecondAlter = PersonalNetwork.getInstance(activity).getSelectedSecondAlter();
				PersonalNetwork history = PersonalNetwork.getInstance(activity);
				if(existingEventHasToBeRemoved){//do it
					String datumId = history.getAttributeDatumIDAt(existingEventTime, 
							PersonalNetwork.getAlterAlterContactEventAttributeName(), 
							AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter));
					if(datumId != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(datumId)){
						history.setSecondaryAttributeValue(datumId, 
								PersonalNetwork.getSecondaryAttributeNameContactForm(), 
								PersonalNetwork.VALUE_NOT_ASSIGNED);						
						history.setSecondaryAttributeValue(datumId, 
								PersonalNetwork.getSecondaryAttributeNameContactContent(), 
								PersonalNetwork.VALUE_NOT_ASSIGNED);						
						history.setSecondaryAttributeValue(datumId, 
								PersonalNetwork.getSecondaryAttributeNameContactAtmosphere(), 
								PersonalNetwork.VALUE_NOT_ASSIGNED);						
						history.setSecondaryAttributeValue(datumId, 
								PersonalNetwork.getSecondaryAttributeNameText(), 
								PersonalNetwork.VALUE_NOT_ASSIGNED);					
						history.setAttributeValueAt(TimeInterval.getTimePoint(existingEventTime), 
								PersonalNetwork.getAlterAlterContactEventAttributeName(), 
								AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter),
								PersonalNetwork.VALUE_NOT_ASSIGNED);
					}
				}
				EditText editText = (EditText) view.findViewById(R.id.add_alter_contact_event_text);
				String text = editText.getText().toString().trim();
				Spinner spinner = (Spinner) view.findViewById(R.id.add_alter_contact_event_form_spinner);
				String contactForm = spinner.getSelectedItem().toString();
				spinner = (Spinner) view.findViewById(R.id.add_alter_contact_event_content_spinner);
				String contactContent = spinner.getSelectedItem().toString();
				spinner = (Spinner) view.findViewById(R.id.add_alter_contact_event_atmosphere_spinner);
				String contactAtmos = spinner.getSelectedItem().toString();
				history.setAttributeValueAt(TimeInterval.getTimePoint(eventTime), 
						PersonalNetwork.getAlterAlterContactEventAttributeName(), 
						AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter),
						"value");//TODO: could use the value for something like an event title
				String datumID = history.getAttributeDatumIDAt(eventTime, 
						PersonalNetwork.getAlterAlterContactEventAttributeName(), 
						AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter));
				if(contactForm.length() > 0)
					history.setSecondaryAttributeValue(datumID, 
							PersonalNetwork.getSecondaryAttributeNameContactForm(), 
							contactForm);
				if(contactContent.length() > 0)
					history.setSecondaryAttributeValue(datumID, 
							PersonalNetwork.getSecondaryAttributeNameContactContent(), 
							contactContent);
				if(contactAtmos.length() > 0)
					history.setSecondaryAttributeValue(datumID, 
							PersonalNetwork.getSecondaryAttributeNameContactAtmosphere(), 
							contactAtmos);
				if(text.length() > 0)
					history.setSecondaryAttributeValue(datumID, 
							PersonalNetwork.getSecondaryAttributeNameText(), 
							text);
				activity.updatePersonalNetworkViews();
			}
		});      
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) {
				//nothing to do
			}
		});      
		return builder.create();
	}

	private void initDataIfEventExists(View view) {
		String selectedAlter = PersonalNetwork.getInstance(activity).getSelectedAlter();
		String selectedSecondAlter = PersonalNetwork.getInstance(activity).getSelectedSecondAlter();
		PersonalNetwork history = PersonalNetwork.getInstance(activity);
		String datumID = history.getAttributeDatumIDAt(eventTime, 
				PersonalNetwork.getAlterAlterContactEventAttributeName(), 
				AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter));
		if(datumID != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(datumID)){
			EditText editText = (EditText) view.findViewById(R.id.add_alter_contact_event_text);
			String text = history.getSecondaryAttributeValue(datumID, 
					PersonalNetwork.getSecondaryAttributeNameText());
			if(text != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(text))
				editText.setText(text);
			Spinner spinner = (Spinner) view.findViewById(R.id.add_alter_contact_event_form_spinner);
			String contactForm = history.getSecondaryAttributeValue(datumID, 
					PersonalNetwork.getSecondaryAttributeNameContactForm());
			if(contactForm != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(contactForm)){
				String[] choices = activity.getResources().getStringArray(R.array.secondary_attribute_contact_form_choices);
				for(int i = 0; i < choices.length; ++i){
					if(contactForm.equals(choices[i])){
						spinner.setSelection(i);
					}
				}
			}
			spinner = (Spinner) view.findViewById(R.id.add_alter_contact_event_content_spinner);
			String contactContent = history.getSecondaryAttributeValue(datumID, 
					PersonalNetwork.getSecondaryAttributeNameContactContent());
			if(contactContent != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(contactContent)){
				String[] choices = activity.getResources().getStringArray(R.array.secondary_attribute_contact_content_choices);
				for(int i = 0; i < choices.length; ++i){
					if(contactContent.equals(choices[i])){
						spinner.setSelection(i);
					}
				}
			}
			spinner = (Spinner) view.findViewById(R.id.add_alter_contact_event_atmosphere_spinner);
			String contactAtmos = history.getSecondaryAttributeValue(datumID, 
					PersonalNetwork.getSecondaryAttributeNameContactAtmosphere());
			if(contactAtmos != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(contactAtmos)){
				String[] choices = activity.getResources().getStringArray(R.array.secondary_attribute_contact_atmosphere_choices);
				for(int i = 0; i < choices.length; ++i){
					if(contactAtmos.equals(choices[i])){
						spinner.setSelection(i);
					}
				}
			}
		}
	}

}
