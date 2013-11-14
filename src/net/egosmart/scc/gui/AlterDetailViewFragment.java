package net.egosmart.scc.gui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.AlterAlterDyad;
import net.egosmart.scc.data.EgoAlterDyad;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.SCCProperties;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.data.TimeVaryingAttributeValues;
import net.egosmart.scc.gui.dialog.AddAlterAlterContactEventDialog;
import net.egosmart.scc.gui.dialog.AddAlterAlterMemoDialog;
import net.egosmart.scc.gui.dialog.AddAlterMemoDialog;
import net.egosmart.scc.gui.dialog.AddEgoAlterContactEventDialog;
import net.egosmart.scc.gui.dialog.BrowseAlterContactEventsDialog;
import net.egosmart.scc.gui.dialog.BrowseAlterMemosDialog;
import net.egosmart.scc.gui.dialog.BrowseEgoMemosDialog;
import net.egosmart.scc.gui.dialog.EditAlterAlterAttributesDialog;
import net.egosmart.scc.gui.dialog.EditAlterAlterTiesDialog;
import net.egosmart.scc.R;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class AlterDetailViewFragment extends Fragment {

	private SCCMainActivity activity;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.alter_detail_view, container, false);
		return view;
	}

	public void onStart(){
		super.onStart();
		updateView();
	}

	//should be called whenever the personal network changed and this fragment is visible
	public void updateView(){
		activity = (SCCMainActivity) getActivity();
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		final String selectedAlter = network.getSelectedAlter();
		// display the selected alter
		TextView textView = (TextView) activity.findViewById(R.id.detail_view_alter_name);
		if(textView == null)
			return;
		textView.setText(selectedAlter);
		// clear the tables in any case
		TableLayout alterAttribTable = (TableLayout) activity.findViewById(R.id.detail_view_alter_attributes);
		alterAttribTable.removeAllViews();
		TableLayout egoAlterTiesTable = (TableLayout) activity.findViewById(R.id.detail_view_ego_alter_ties);
		egoAlterTiesTable.removeAllViews();
		FrameLayout alterAlterTiesContainer = (FrameLayout) activity.findViewById(R.id.detail_view_alter_alter_ties_container);
		alterAlterTiesContainer.removeAllViews();
		LinearLayout alterEventsList = (LinearLayout) activity.findViewById(R.id.detail_view_alter_events_list);
		alterEventsList.removeAllViews();
		if(selectedAlter != null){
			final SCCProperties properties = SCCProperties.getInstance(activity);
			// events of alter
			displayAlterEvents(selectedAlter, alterEventsList, properties);
			// configure expand alter attributes button and potentially display alter attributes
			displayAlterAttributes(network, selectedAlter, alterAttribTable, properties);
			// configure expand ego alter ties button and potentially display ego alter attributes
			displayEgoAlterTies(network, selectedAlter, egoAlterTiesTable, properties);
			// configure expand alter alter ties button and potentially display neighbors of the selected alter
			displayAlterAlterTies(network, selectedAlter, alterAlterTiesContainer, properties);
		} else { // selected alter is null

		}
	}

	private void displayAlterAlterTies(final PersonalNetwork network,
			final String selectedAlter, FrameLayout alterAlterTiesContainer,
			final SCCProperties properties) {
		ImageButton expandAlterAlterTiesButton = (ImageButton) activity.findViewById(R.id.expand_alter_alter_ties_button);
		if(properties.getPropertyAlterDetailExpandAlterAlterTies()){
			expandAlterAlterTiesButton.setImageResource(R.drawable.ic_button_shrink);
			expandAlterAlterTiesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAlterDetailExpandAlterAlterTies(false);
					activity.updatePersonalNetworkViews();
				}
			});
			boolean showAllNeigbors = properties.getPropertyAlterDetailShowAllNeighbors();
			if(showAllNeigbors){
				displayAlterAlterTiesOfAllNeighbors(network, selectedAlter,
						alterAlterTiesContainer, properties);
			} else { //show details of a selected second alter
				displayDetailsOfAlterAlterTieWithSelectedSecondAlter(network,
						selectedAlter, alterAlterTiesContainer, properties);
			}
		} else { //do not expand alter alter ties
			expandAlterAlterTiesButton.setImageResource(R.drawable.ic_button_expand);
			expandAlterAlterTiesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAlterDetailExpandAlterAlterTies(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not display alter-alter ties
		}
		//in any case set on click listener for the edit alter ties button
		ImageButton editAlterTiesButton = (ImageButton) activity.findViewById(R.id.edit_alter_ties_button);
		editAlterTiesButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				editAlterAlterTies();
			}
		});
	}

	private void displayDetailsOfAlterAlterTieWithSelectedSecondAlter(
			final PersonalNetwork network, final String selectedAlter,
			FrameLayout alterAlterTiesContainer, final SCCProperties properties) {
		final String selectedSecondAlter = network.getSelectedSecondAlter();
		if(network.areAdjacentAt(TimeInterval.getCurrentTimePoint(), selectedAlter, selectedSecondAlter)){
			View alterAlterDetailView = LayoutInflater.from(activity).
					inflate(R.layout.alter_alter_tie_detail_view, null);
			ImageButton upButton = (ImageButton) alterAlterDetailView.
					findViewById(R.id.up_to_all_alter_ties_button);
			upButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					properties.setPropertyAlterDetailShowAllNeighbors(true);								
					activity.updatePersonalNetworkViews();
				}
			});
			if(selectedSecondAlter != null && !selectedSecondAlter.equals(selectedAlter)){
				TextView secondAlterName = (TextView) alterAlterDetailView.
						findViewById(R.id.detail_view_second_alter_name);
				secondAlterName.setText(selectedSecondAlter);
				//display alter-alter tie events
				LinearLayout alterAlterTieEventsContentView = (LinearLayout) alterAlterDetailView.
						findViewById(R.id.alter_alter_tie_detail_view_events_list);
				PersonalNetwork history = PersonalNetwork.getInstance(activity);
				DateFormat dateFormat =  android.text.format.DateFormat.getLongDateFormat(activity);
				DateFormat timeFormat =  android.text.format.DateFormat.getTimeFormat(activity);
				TimeVaryingAttributeValues memos = history.
						getRecentAttributeValues(
								PersonalNetwork.getAlterAlterMemosAttributeName(), 
								AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter), 5);
				Iterator<TimeInterval> memoIt = memos.getSupport().getDescendingIterator();
				TimeInterval memoTime = null;
				if(memoIt.hasNext())
					memoTime = memoIt.next();
				TimeVaryingAttributeValues contactEvents = history.
						getRecentAttributeValues(
								PersonalNetwork.getAlterAlterContactEventAttributeName(), 
								AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter),
								5);
				Iterator<TimeInterval> contactIt = contactEvents.getSupport().getDescendingIterator();
				TimeInterval contactTime = null;
				if(contactIt.hasNext())
					contactTime = contactIt.next();
				while(memoTime != null || contactTime != null){
					if(memoTime != null && (contactTime == null || memoTime.getStartTime() >= contactTime.getStartTime())){
						String datumID = history.
								getAttributeDatumIDAt(memoTime.getStartTime(),
										PersonalNetwork.getAlterAlterMemosAttributeName(), 
										AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter));
						String text = history.getSecondaryAttributeValue(datumID, 
								PersonalNetwork.getSecondaryAttributeNameText());
						Date date = new Date(memoTime.getStartTime());
						String timeText = dateFormat.format(date) + "  " + timeFormat.format(date);
						View memoView = LayoutInflater.from(activity).inflate(R.layout.memo_text_date_view, null);
						TextView dateView = (TextView) memoView.findViewById(R.id.memo_text_date_view_field_date);
						dateView.setText(timeText);
						TextView memoTextView = (TextView) memoView.findViewById(R.id.memo_text_date_view_field_text);
						if(text.length() > 100)
							text = text.substring(0, 100) + "...";
						memoTextView.setText(text);
						final long memoStartTime = memoTime.getStartTime();
						memoView.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								DialogFragment dialog = AddAlterAlterMemoDialog.
										getInstance(activity, memoStartTime);
								dialog.show(activity.getSupportFragmentManager(), 
										SCCMainActivity.ADD_ALTER_ALTER_MEMO_DIALOG_TAG);
							}
						});
						alterAlterTieEventsContentView.addView(memoView);
						if(memoIt.hasNext())
							memoTime = memoIt.next();
						else
							memoTime = null;
					} else if(contactTime != null){
						String datumID = history.
								getAttributeDatumIDAt(contactTime.getStartTime(),
										PersonalNetwork.getAlterAlterContactEventAttributeName(), 
										AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter));
						//show date
						Date date = new Date(contactTime.getStartTime());
						String timeText = dateFormat.format(date) + "  " + timeFormat.format(date);
						View contactView = LayoutInflater.from(activity).inflate(R.layout.contact_event_view, null);
						TextView dateView = (TextView) contactView.findViewById(R.id.contact_event_view_field_date);
						dateView.setText(timeText);
						//show contact form
						String contactForm = history.getSecondaryAttributeValue(datumID, 
								PersonalNetwork.getSecondaryAttributeNameContactForm());
						TextView formView = (TextView) contactView.findViewById(R.id.contact_event_view_field_form_value);
						formView.setText(contactForm);
						//show contact content
						String contactContent = history.getSecondaryAttributeValue(datumID, 
								PersonalNetwork.getSecondaryAttributeNameContactContent());
						TextView contentView = (TextView) contactView.findViewById(R.id.contact_event_view_field_content_value);
						contentView.setText(contactContent);
						//show contact atmosphere
						String contactAtmos = history.getSecondaryAttributeValue(datumID, 
								PersonalNetwork.getSecondaryAttributeNameContactAtmosphere());
						TextView atmosView = (TextView) contactView.findViewById(R.id.contact_event_view_field_atmosphere_value);
						atmosView.setText(contactAtmos);
						//show text
						String text = history.getSecondaryAttributeValue(datumID, 
								PersonalNetwork.getSecondaryAttributeNameText());
						if(text != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(text)){
							TextView contactTextView = (TextView) contactView.findViewById(R.id.contact_event_view_field_text);
							if(text.length() > 100)
								text = text.substring(0, 100) + "...";
							contactTextView.setText(text);
						}
						final long contactStartTime = contactTime.getStartTime();
						contactView.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								DialogFragment dialog = AddAlterAlterContactEventDialog.
										getInstance(activity, contactStartTime);
								dialog.show(activity.getSupportFragmentManager(), 
										SCCMainActivity.ADD_ALTER_ALTER_CONTACT_EVENT_DIALOG_TAG);
							}
						});
						alterAlterTieEventsContentView.addView(contactView);
						if(contactIt.hasNext())
							contactTime = contactIt.next();
						else
							contactTime = null;						
					}
				}
				//display alter alter state type attributes
				TableLayout alterAlterTieTable = (TableLayout) alterAlterDetailView.
						findViewById(R.id.detail_view_alter_alter_ties_table);
				ArrayList<String> attrNames = new ArrayList<String>(network.getAttributeNames(
						PersonalNetwork.DOMAIN_ALTER_ALTER));
				for(String attrName : attrNames){
					String directionType = network.getAttributeDirectionType(
							PersonalNetwork.DOMAIN_ALTER_ALTER, attrName);
					//display value for direction from first to second alter or for symmetric attributes
					if(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC.equals(directionType) || 
							PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC.equals(directionType)){
						String textValue = network.getAttributeValueAt(System.currentTimeMillis(), attrName, 
								AlterAlterDyad.getInstance(selectedAlter, selectedSecondAlter));
						if(textValue != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(textValue)){
							TableRow row = (TableRow) LayoutInflater.from(activity).
									inflate(R.layout.ego_alter_attributes_table_attr_row, null);
							TextView name = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_name);
							name.setText(attrName + " ");
							ImageView img = (ImageView) row.findViewById(R.id.ego_alter_attributes_table_image_direction);
							if(!directionType.equals(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC)){
								img.setImageResource(R.drawable.ic_info_attr_ego2alter);
							}
							TextView value = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_value);
							value.setText(textValue);
							alterAlterTieTable.addView(row);
						}
					}
					//display value for direction from second to first alter
					if(PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC.equals(directionType)){
						String textValue = network.getAttributeValueAt(System.currentTimeMillis(), attrName, 
								AlterAlterDyad.getInstance(selectedSecondAlter, selectedAlter));
						if(textValue != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(textValue)){
							TableRow row = (TableRow) LayoutInflater.from(activity).
									inflate(R.layout.ego_alter_attributes_table_attr_row, null);
							TextView name = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_name);
							name.setText(attrName + " ");
							ImageView img = (ImageView) row.findViewById(R.id.ego_alter_attributes_table_image_direction);
							img.setImageResource(R.drawable.ic_info_attr_alter2ego);
							TextView value = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_value);
							value.setText(textValue);
							alterAlterTieTable.addView(row);
						}
					}
				}
			}
			alterAlterTiesContainer.addView(alterAlterDetailView);
		} else {
			properties.setPropertyAlterDetailShowAllNeighbors(true);
			activity.updatePersonalNetworkViews();
		}
	}

	private void displayAlterAlterTiesOfAllNeighbors(
			final PersonalNetwork network, final String selectedAlter,
			FrameLayout alterAlterTiesContainer, final SCCProperties properties) {
		// display alter-alter ties of the selected alter
		HashSet<String> neighborsSet = network.getNeighborsAt(TimeInterval.getCurrentTimePoint(), selectedAlter);
		//ArrayList<Pair<String,String>> alterNamesAttrInfo = new ArrayList<Pair<String,String>>();
		LinearLayout neighborsList = new LinearLayout(activity);
		neighborsList.setOrientation(LinearLayout.VERTICAL);
		if(neighborsSet != null){
			for(final String neighbor : neighborsSet){
				//TODO: change this: for asymmetric attributes this returns only the value for one direction
				HashMap<String, String> attrName2Value = network.
						getValuesOfAllAttributesForElementAt(System.currentTimeMillis(),
								AlterAlterDyad.getInstance(selectedAlter, neighbor));
				StringBuffer valueStr = new StringBuffer("| ");
				for(String attrName : attrName2Value.keySet()){
					String value = attrName2Value.get(attrName);
					if(!PersonalNetwork.VALUE_NOT_ASSIGNED.equals(value)){
						valueStr.append(attrName).append(':').append(value).append(" | ");
					}
				}
				View row = LayoutInflater.from(activity).inflate(R.layout.neighbor_list_single_row_view, null);
				TextView nameView = (TextView) row.findViewById(R.id.neighbor_list_single_row_field_name);
				nameView.setText(neighbor);
				TextView attrInfoView = (TextView) row.findViewById(R.id.neighbor_list_single_row_field_attribute);
				attrInfoView.setText(valueStr.toString());
				row.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						properties.setPropertyAlterDetailShowAllNeighbors(false);
						network.setSelectedSecondAlter(neighbor);
						activity.updatePersonalNetworkViews();
					}
				});
				neighborsList.addView(row);							
			}
		}
		alterAlterTiesContainer.addView(neighborsList);
	}

	private void displayEgoAlterTies(final PersonalNetwork network,
			final String selectedAlter, TableLayout egoAlterTiesTable,
			final SCCProperties properties) {
		ImageButton expandEgoAlterTiesButton = (ImageButton) activity.findViewById(R.id.expand_ego_alter_ties_button);
		if(properties.getPropertyAlterDetailExpandEgoAlterTies()){
			expandEgoAlterTiesButton.setImageResource(R.drawable.ic_button_shrink);
			expandEgoAlterTiesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAlterDetailExpandEgoAlterTies(false);
					activity.updatePersonalNetworkViews();
				}
			});
			// display ego-alter ties of the selected alter
			ArrayList<String> attrNames = new ArrayList<String>(network.getAttributeNames(
					PersonalNetwork.DOMAIN_EGO_ALTER));
			for(String attrName : attrNames){
				String directionType = network.getAttributeDirectionType(
						PersonalNetwork.DOMAIN_EGO_ALTER, attrName);
				//display value for direction from ego to alter
				if(PersonalNetwork.DYAD_DIRECTION_OUT.equals(directionType) || 
						PersonalNetwork.DYAD_DIRECTION_SYMMETRIC.equals(directionType) || 
						PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC.equals(directionType)){
					String textValue = network.getAttributeValueAt(System.currentTimeMillis(),
							attrName, 
							EgoAlterDyad.getInstance(selectedAlter, PersonalNetwork.DYAD_DIRECTION_OUT));
					if(textValue != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(textValue)){
						TableRow row = (TableRow) LayoutInflater.from(activity).
								inflate(R.layout.ego_alter_attributes_table_attr_row, null);
						TextView name = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_name);
						name.setText(attrName + " ");
						ImageView img = (ImageView) row.findViewById(R.id.ego_alter_attributes_table_image_direction);
						if(!directionType.equals(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC)){
							img.setImageResource(R.drawable.ic_info_attr_ego2alter);
						}
						TextView value = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_value);
						value.setText(textValue);
						egoAlterTiesTable.addView(row);
					}
				}
				//display value for direction from alter to ego
				if(PersonalNetwork.DYAD_DIRECTION_IN.equals(directionType) || 
						PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC.equals(directionType)){
					String textValue = network.getAttributeValueAt(System.currentTimeMillis(),
							attrName, 
							EgoAlterDyad.getInstance(selectedAlter, PersonalNetwork.DYAD_DIRECTION_IN));
					if(textValue != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(textValue)){
						TableRow row = (TableRow) LayoutInflater.from(activity).
								inflate(R.layout.ego_alter_attributes_table_attr_row, null);
						TextView name = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_name);
						name.setText(attrName + " ");
						ImageView img = (ImageView) row.findViewById(R.id.ego_alter_attributes_table_image_direction);
						img.setImageResource(R.drawable.ic_info_attr_alter2ego);
						TextView value = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_value);
						value.setText(textValue);
						egoAlterTiesTable.addView(row);
					}
				}
			}
		} else {
			expandEgoAlterTiesButton.setImageResource(R.drawable.ic_button_expand);
			expandEgoAlterTiesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAlterDetailExpandEgoAlterTies(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not display ego-alter ties
		}
	}

	private void displayAlterAttributes(final PersonalNetwork network,
			final String selectedAlter, TableLayout alterAttribTable,
			final SCCProperties properties) {
		ImageButton expandAlterAttribtesButton = (ImageButton) activity.findViewById(R.id.expand_alter_attrib_button);
		if(properties.getPropertyAlterDetailExpandAttributes()){
			expandAlterAttribtesButton.setImageResource(R.drawable.ic_button_shrink);
			expandAlterAttribtesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAlterDetailExpandAttributes(false);
					activity.updatePersonalNetworkViews();
				}
			});
			// display some system attributes of the selected alter
			// email addresses
			String emails = network.getAttributeValueAt(System.currentTimeMillis(),
					PersonalNetwork.ALTER_ATTRIBUTE_NAME_ALTER_EMAIL_ADDRESSES, 
					Alter.getInstance(selectedAlter));
			if(emails != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(emails)){
				String attrDisplayName = activity.getString(R.string.alter_attribute_alter_email_display_name);
				String[] emailArray = emails.split(", ");
				for(int i = 0; i < emailArray.length; ++i){
					TableRow row = (TableRow) LayoutInflater.from(activity).
							inflate(R.layout.alter_attributes_table_attr_row, null);
					TextView name = (TextView) row.findViewById(R.id.alter_attributes_table_row_attr_name);
					if(emailArray.length > 1)
						name.setText(attrDisplayName + " " + (i+1));
					else
						name.setText(attrDisplayName);						
					TextView value = (TextView) row.findViewById(R.id.alter_attributes_table_row_attr_value);
					value.setText(emailArray[i]);
					alterAttribTable.addView(row);					
				}
			}
			// phone numbers
			String phoneNumbers = network.getAttributeValueAt(System.currentTimeMillis(),
					PersonalNetwork.ALTER_ATTRIBUTE_NAME_ALTER_PHONE_NUMBERS, 
					Alter.getInstance(selectedAlter));
			if(phoneNumbers != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(phoneNumbers)){
				String attrDisplayName = activity.getString(R.string.alter_attribute_alter_phone_numbers_display_name);
				String[] phoneArray = phoneNumbers.split(", ");
				for(int i = 0; i < phoneArray.length; ++i){
					TableRow row = (TableRow) LayoutInflater.from(activity).
							inflate(R.layout.alter_attributes_table_attr_row, null);
					TextView name = (TextView) row.findViewById(R.id.alter_attributes_table_row_attr_name);
					if(phoneArray.length > 1)
						name.setText(attrDisplayName + " " + (i+1));
					else
						name.setText(attrDisplayName);						
					TextView value = (TextView) row.findViewById(R.id.alter_attributes_table_row_attr_value);
					value.setText(phoneArray[i]);
					alterAttribTable.addView(row);					
				}
			}
			// display user defined attributes of the selected alter
			HashMap<String,String> nameValuePairs = network.getValuesOfAllAttributesForElementAt(
					System.currentTimeMillis(), Alter.getInstance(selectedAlter));
			for(String attrName : nameValuePairs.keySet()){
				String textValue = nameValuePairs.get(attrName);
				if(textValue != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(textValue)){
					TableRow row = (TableRow) LayoutInflater.from(activity).
							inflate(R.layout.alter_attributes_table_attr_row, null);
					TextView name = (TextView) row.findViewById(R.id.alter_attributes_table_row_attr_name);
					name.setText(attrName + " ");
					TextView value = (TextView) row.findViewById(R.id.alter_attributes_table_row_attr_value);
					value.setText(textValue);
					alterAttribTable.addView(row);
				}
			}
		} else {
			expandAlterAttribtesButton.setImageResource(R.drawable.ic_button_expand);
			expandAlterAttribtesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAlterDetailExpandAttributes(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not display attributes of the selected alter
		}
	}

	private void displayAlterEvents(final String selectedAlter,
			LinearLayout alterEventsList, final SCCProperties properties) {
		// configure expand events button and potentially display these
		ImageButton expandAlterEventsButton = (ImageButton) activity.findViewById(R.id.expand_alter_events_button);
		if(properties.getPropertyAlterDetailExpandAlterEvents()){
			expandAlterEventsButton.setImageResource(R.drawable.ic_button_shrink);
			expandAlterEventsButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAlterDetailExpandAlterEvents(false);
					activity.updatePersonalNetworkViews();
				}
			});
			// display events
			PersonalNetwork history = PersonalNetwork.getInstance(activity);
			DateFormat dateFormat =  android.text.format.DateFormat.getLongDateFormat(activity);
			DateFormat timeFormat =  android.text.format.DateFormat.getTimeFormat(activity);
			TimeVaryingAttributeValues memos = history.
					getRecentAttributeValues(PersonalNetwork.getAlterMemosAttributeName(), 
							Alter.getInstance(selectedAlter), 5);
			Iterator<TimeInterval> memoIt = memos.getSupport().getDescendingIterator();
			TimeInterval memoTime = null;
			if(memoIt.hasNext())
				memoTime = memoIt.next();
			TimeVaryingAttributeValues contactEvents = history.
					getRecentAttributeValues(
							PersonalNetwork.getEgoAlterContactEventAttributeName(), 
							EgoAlterDyad.getOutwardInstance(selectedAlter),
							5);
			Iterator<TimeInterval> contactIt = contactEvents.getSupport().getDescendingIterator();
			TimeInterval contactTime = null;
			if(contactIt.hasNext())
				contactTime = contactIt.next();
			while(memoTime != null || contactTime != null){
				if(memoTime != null && (contactTime == null || memoTime.getStartTime() >= contactTime.getStartTime())){
					String datumID = history.getAttributeDatumIDAt(memoTime.getStartTime(), 
							PersonalNetwork.getAlterMemosAttributeName(), 
							Alter.getInstance(selectedAlter));
					String text = history.getSecondaryAttributeValue(datumID, 
							PersonalNetwork.getSecondaryAttributeNameText());
					Date date = new Date(memoTime.getStartTime());
					String timeText = dateFormat.format(date) + "  " + timeFormat.format(date);
					View memoView = LayoutInflater.from(activity).inflate(R.layout.memo_text_date_view, null);
					TextView dateView = (TextView) memoView.findViewById(R.id.memo_text_date_view_field_date);
					dateView.setText(timeText);
					TextView memoTextView = (TextView) memoView.findViewById(R.id.memo_text_date_view_field_text);
					if(text.length() > 100)
						text = text.substring(0, 100) + "...";
					memoTextView.setText(text);
					final long memoStartTime = memoTime.getStartTime();
					memoView.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							DialogFragment dialog = AddAlterMemoDialog.getInstance(activity, memoStartTime);
							dialog.show(activity.getSupportFragmentManager(), 
									SCCMainActivity.ADD_ALTER_MEMO_DIALOG_TAG);
						}
					});
					alterEventsList.addView(memoView);
					if(memoIt.hasNext())
						memoTime = memoIt.next();
					else
						memoTime = null;
				} else if(contactTime != null){
					String datumID = history.
							getAttributeDatumIDAt(contactTime.getStartTime(),
									PersonalNetwork.getEgoAlterContactEventAttributeName(), 
									EgoAlterDyad.getOutwardInstance(selectedAlter));
					//show date
					Date date = new Date(contactTime.getStartTime());
					String timeText = dateFormat.format(date) + "  " + timeFormat.format(date);
					View contactView = LayoutInflater.from(activity).inflate(R.layout.contact_event_view, null);
					TextView dateView = (TextView) contactView.findViewById(R.id.contact_event_view_field_date);
					dateView.setText(timeText);
					//show contact form
					String contactForm = history.getSecondaryAttributeValue(datumID, 
							PersonalNetwork.getSecondaryAttributeNameContactForm());
					TextView formView = (TextView) contactView.findViewById(R.id.contact_event_view_field_form_value);
					formView.setText(contactForm);
					//show contact content
					String contactContent = history.getSecondaryAttributeValue(datumID, 
							PersonalNetwork.getSecondaryAttributeNameContactContent());
					TextView contentView = (TextView) contactView.findViewById(R.id.contact_event_view_field_content_value);
					contentView.setText(contactContent);
					//show contact atmosphere
					String contactAtmos = history.getSecondaryAttributeValue(datumID, 
							PersonalNetwork.getSecondaryAttributeNameContactAtmosphere());
					TextView atmosView = (TextView) contactView.findViewById(R.id.contact_event_view_field_atmosphere_value);
					atmosView.setText(contactAtmos);
					//show text
					String text = history.getSecondaryAttributeValue(datumID, 
							PersonalNetwork.getSecondaryAttributeNameText());
					if(text != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(text)){
						TextView contactTextView = (TextView) contactView.findViewById(R.id.contact_event_view_field_text);
						if(text.length() > 100)
							text = text.substring(0, 100) + "...";
						contactTextView.setText(text);
					}
					final long contactStartTime = contactTime.getStartTime();
					contactView.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							DialogFragment dialog = AddEgoAlterContactEventDialog.
									getInstance(activity,contactStartTime);
							dialog.show(activity.getSupportFragmentManager(), 
									SCCMainActivity.ADD_ALTER_CONTACT_EVENT_DIALOG_TAG);
//							DialogFragment dialog = BrowseAlterContactEventsDialog.getInstance(activity, contactStartTime);
//							dialog.show(activity.getSupportFragmentManager(), 
//									SCCMainActivity.BROWSE_ALTER_CONTACT_EVENTS_DIALOG_TAG);
						}
					});
					alterEventsList.addView(contactView);
					if(contactIt.hasNext())
						contactTime = contactIt.next();
					else
						contactTime = null;						
				}
			}
		} else { //do not show events
			expandAlterEventsButton.setImageResource(R.drawable.ic_button_expand);
			expandAlterEventsButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAlterDetailExpandAlterEvents(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not display events
		}
	}

	/*
	 * Opens the dialog to edit alter-alter ties.
	 * @param view
	 */
	private void editAlterAlterTies(){
		SCCProperties properties = SCCProperties.getInstance(activity);
		properties.setPropertyAlterDetailExpandAlterAlterTies(true);
		boolean showAllNeigbors = properties.getPropertyAlterDetailShowAllNeighbors();
		if(showAllNeigbors){
			DialogFragment editAlterTiesDialog = EditAlterAlterTiesDialog.getInstance(activity);
			editAlterTiesDialog.show(activity.getSupportFragmentManager(), SCCMainActivity.EDIT_ALTER_ALTER_TIES_DIALOG_TAG);
		} else {
			PersonalNetwork network = PersonalNetwork.getInstance(activity);
			String selectedAlter = network.getSelectedAlter();
			String selectedSecondAlter = network.getSelectedSecondAlter();
			if(selectedAlter != null && !selectedAlter.equals(selectedSecondAlter)){
				DialogFragment editAlterAlterAttributesDialog = EditAlterAlterAttributesDialog.getInstance(activity);
				editAlterAlterAttributesDialog.show(activity.getSupportFragmentManager(), 
						SCCMainActivity.EDIT_ALTER_ALTER_ATTRIBS_DIALOG_TAG);
			}
		}
	}



}