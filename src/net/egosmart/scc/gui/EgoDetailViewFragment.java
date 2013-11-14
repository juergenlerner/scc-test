package net.egosmart.scc.gui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Ego;
import net.egosmart.scc.data.EgoAlterDyad;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.SCCProperties;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.data.TimeVaryingAttributeValues;
import net.egosmart.scc.gui.dialog.AddEgoMemoDialog;
import net.egosmart.scc.gui.dialog.EditEgoAlterAttributesDialog;
import net.egosmart.scc.R;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class EgoDetailViewFragment extends Fragment {

	private SCCMainActivity activity;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.ego_detail_view, container, false);
		return view;
	}

	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView(){
		activity = (SCCMainActivity) getActivity();
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		// clear the lists, tables, and containers in any case
		TableLayout egoAttribTable = (TableLayout) activity.findViewById(R.id.detail_view_ego_attributes);
		if(egoAttribTable == null)
			return;
		egoAttribTable.removeAllViews();
		FrameLayout egoAlterTiesContainer = (FrameLayout) activity.findViewById(R.id.ego_detail_view_ego_alter_ties_container);
		egoAlterTiesContainer.removeAllViews();
		LinearLayout egoMemosList = (LinearLayout) activity.findViewById(R.id.detail_view_ego_memos_list);
		egoMemosList.removeAllViews();
		final SCCProperties properties = SCCProperties.getInstance(activity);
		// display memos of ego
		// configure expand memos button and potentially display these
		ImageButton expandEgoMemosButton = (ImageButton) activity.findViewById(R.id.expand_ego_memos_button);
		if(properties.getPropertyEgoDetailExpandEgoMemos()){
			expandEgoMemosButton.setImageResource(R.drawable.ic_button_shrink);
			expandEgoMemosButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyEgoDetailExpandEgoMemos(false);
					activity.updatePersonalNetworkViews();
				}
			});
			// display memos
			PersonalNetwork history = PersonalNetwork.getInstance(activity);
			TimeVaryingAttributeValues memos = history.
					getRecentAttributeValues(PersonalNetwork.getEgoMemosAttributeName(), 
							Ego.getInstance(), 5);
			DateFormat dateFormat =  android.text.format.DateFormat.getLongDateFormat(activity);
			DateFormat timeFormat =  android.text.format.DateFormat.getTimeFormat(activity);
			Iterator<TimeInterval> it = memos.getSupport().getDescendingIterator();
			while(it.hasNext()){
				final TimeInterval time = it.next();
				String datumID = history.getAttributeDatumIDAt(time.getStartTime(), 
						PersonalNetwork.getEgoMemosAttributeName(),
						Ego.getInstance());
				String text = history.getSecondaryAttributeValue(datumID, 
						PersonalNetwork.getSecondaryAttributeNameText());
				Date date = new Date(time.getStartTime());
				String timeText = dateFormat.format(date) + "  " + timeFormat.format(date);
				View memoView = LayoutInflater.from(activity).inflate(R.layout.memo_text_date_view, null);
				TextView dateView = (TextView) memoView.findViewById(R.id.memo_text_date_view_field_date);
				dateView.setText(timeText);
				TextView textView = (TextView) memoView.findViewById(R.id.memo_text_date_view_field_text);
				if(text.length() > 100)
					text = text.substring(0, 100) + "...";
				textView.setText(text);
				memoView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						DialogFragment dialog = AddEgoMemoDialog.getInstance(activity, time.getStartTime());
						dialog.show(activity.getSupportFragmentManager(), SCCMainActivity.ADD_EGO_MEMO_DIALOG_TAG);
					}
				});
				egoMemosList.addView(memoView);
			}
		} else { //do not show memos
			expandEgoMemosButton.setImageResource(R.drawable.ic_button_expand);
			expandEgoMemosButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyEgoDetailExpandEgoMemos(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not display memos
		}
		// display attributes of ego
		// configure expand ego attributes button and potentially display ego attributes
		ImageButton expandEgoAttribtesButton = (ImageButton) activity.findViewById(R.id.expand_ego_attrib_button);
		if(properties.getPropertyEgoDetailExpandAttributes()){
			expandEgoAttribtesButton.setImageResource(R.drawable.ic_button_shrink);
			expandEgoAttribtesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyEgoDetailExpandAttributes(false);
					activity.updatePersonalNetworkViews();
				}
			});
			// display attributes of ego
			HashMap<String,String> nameValuePairs = network.getValuesOfAllAttributesForElementAt(
					System.currentTimeMillis(), Ego.getInstance());
			for(String attrName : nameValuePairs.keySet()){
				String textValue = nameValuePairs.get(attrName);
				if(textValue != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(textValue)){
					TableRow row = (TableRow) LayoutInflater.from(activity).
							inflate(R.layout.alter_attributes_table_attr_row, null);
					TextView name = (TextView) row.findViewById(R.id.alter_attributes_table_row_attr_name);
					name.setText(attrName + " ");
					TextView value = (TextView) row.findViewById(R.id.alter_attributes_table_row_attr_value);
					value.setText(textValue);
					egoAttribTable.addView(row);
				}
			}
		} else { //do not show ego attributes
			expandEgoAttribtesButton.setImageResource(R.drawable.ic_button_expand);
			expandEgoAttribtesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyEgoDetailExpandAttributes(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not display attributes of ego
		}
		// display attributes of ego-alter ties
		// configure expand ego alter ties button and display all alters or the attributes of a selected alter
		ImageButton expandEgoAlterTiesButton = (ImageButton) activity.
				findViewById(R.id.expand_ego_alter_ties_from_ego_view_button);
		if(properties.getPropertyEgoDetailExpandEgoAlterTies()){
			expandEgoAlterTiesButton.setImageResource(R.drawable.ic_button_shrink);
			expandEgoAlterTiesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyEgoDetailExpandEgoAlterTies(false);
					activity.updatePersonalNetworkViews();
				}
			});
			boolean showAllAlters = properties.getPropertyEgoDetailShowAllAlters();
			if(showAllAlters){
				// display alter-alter ties of the selected alter
				HashSet<String> altersSet = network.getAltersAt(TimeInterval.getCurrentTimePoint());
				LinearLayout alterList = new LinearLayout(activity);
				alterList.setOrientation(LinearLayout.VERTICAL);
				if(altersSet != null){
					for(final String alter : altersSet){
						//TODO: change this: for asymmetric attributes this returns only the value for one direction
						HashMap<String, String> attrName2Value = network.
								getValuesOfAllAttributesForElementAt(System.currentTimeMillis(), 
										EgoAlterDyad.getInstance(alter, PersonalNetwork.DYAD_DIRECTION_OUT));
						StringBuffer valueStr = new StringBuffer("| ");
						for(String attrName : attrName2Value.keySet()){
							String value = attrName2Value.get(attrName);
							if(!PersonalNetwork.VALUE_NOT_ASSIGNED.equals(value)){
								valueStr.append(value).append(" | ");
							}
						}
						//TODO: change name (but layout is usable)
						View row = LayoutInflater.from(activity).inflate(R.layout.neighbor_list_single_row_view, null);
						TextView nameView = (TextView) row.findViewById(R.id.neighbor_list_single_row_field_name);
						nameView.setText(alter);
						TextView attrInfoView = (TextView) row.findViewById(R.id.neighbor_list_single_row_field_attribute);
						attrInfoView.setText(valueStr.toString());
						row.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View v) {
								properties.setPropertyEgoDetailShowAllAlters(false);
								network.setSelectedAlter(alter);
								activity.updatePersonalNetworkViews();
							}
						});
						alterList.addView(row);							
					}
				}
				egoAlterTiesContainer.addView(alterList);
			} else { //show details of the selected alter
				String selectedAlterNameToBeChanged = network.getSelectedAlter();
				View egoAlterDetailView = LayoutInflater.from(activity).
						inflate(R.layout.ego_alter_tie_detail_view, null);
				ImageButton upButton = (ImageButton) egoAlterDetailView.
						findViewById(R.id.up_to_all_alter_ties_button);
				upButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						properties.setPropertyEgoDetailShowAllAlters(true);								
						activity.updatePersonalNetworkViews();
					}
				});
				if(selectedAlterNameToBeChanged != null){
					TextView alterName = (TextView) egoAlterDetailView.
							findViewById(R.id.ego_alter_detail_view_alter_name);
					alterName.setText(selectedAlterNameToBeChanged);
					TableLayout egoAlterTieTable = (TableLayout) egoAlterDetailView.
							findViewById(R.id.detail_view_ego_alter_ties_table);
					ArrayList<String> attrNames = new ArrayList<String>(network.
							getAttributeNames(PersonalNetwork.DOMAIN_EGO_ALTER));
					for(String attrName : attrNames){
						String directionType = network.getAttributeDirectionType(
								PersonalNetwork.DOMAIN_EGO_ALTER, attrName);
						//display value for direction from ego to alter (OUT) or for symmetric attributes
						if(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC.equals(directionType) || 
								PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC.equals(directionType) || 
								PersonalNetwork.DYAD_DIRECTION_OUT.equals(directionType)){
							String textValue = network.getAttributeValueAt(System.currentTimeMillis(), attrName, 
									EgoAlterDyad.getInstance(selectedAlterNameToBeChanged, PersonalNetwork.DYAD_DIRECTION_OUT));
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
								egoAlterTieTable.addView(row);
							}
						}
						//display value for direction from alter to ego (IN)
						if(PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC.equals(directionType) ||
								PersonalNetwork.DYAD_DIRECTION_IN.equals(directionType)){
							String textValue = network.getAttributeValueAt(System.currentTimeMillis(), attrName, 
									EgoAlterDyad.getInstance(selectedAlterNameToBeChanged, PersonalNetwork.DYAD_DIRECTION_IN));
							if(textValue != null && !PersonalNetwork.VALUE_NOT_ASSIGNED.equals(textValue)){
								TableRow row = (TableRow) LayoutInflater.from(activity).
										inflate(R.layout.ego_alter_attributes_table_attr_row, null);
								TextView name = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_name);
								name.setText(attrName + " ");
								ImageView img = (ImageView) row.findViewById(R.id.ego_alter_attributes_table_image_direction);
								img.setImageResource(R.drawable.ic_info_attr_alter2ego);
								TextView value = (TextView) row.findViewById(R.id.ego_alter_attributes_table_row_attr_value);
								value.setText(textValue);
								egoAlterTieTable.addView(row);
							}
						}
					}
				}
				egoAlterTiesContainer.addView(egoAlterDetailView);
			}
		} else { //do not expand ego alter ties
			expandEgoAlterTiesButton.setImageResource(R.drawable.ic_button_expand);
			expandEgoAlterTiesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyEgoDetailExpandEgoAlterTies(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not display ego-alter ties
		}
		//in any case set on click listener for the edit ego-alter ties button
		//enable this button only if not all alters are shown i.e., if one particular is selected
		ImageButton editEgoAlterTiesButton = (ImageButton) activity.
				findViewById(R.id.edit_ego_alter_ties_from_ego_view_button);
		editEgoAlterTiesButton.setEnabled(!properties.getPropertyEgoDetailShowAllAlters());
		editEgoAlterTiesButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				properties.setPropertyEgoDetailExpandEgoAlterTies(true); 
				DialogFragment editEgoAlterAttributesDialog = EditEgoAlterAttributesDialog.getInstance(activity);
				editEgoAlterAttributesDialog.show(activity.getSupportFragmentManager(), 
						SCCMainActivity.EDIT_EGO_ALTER_ATTRIBS_DIALOG_TAG);
			}
		});
	}

}