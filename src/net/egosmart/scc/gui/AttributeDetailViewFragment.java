package net.egosmart.scc.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.AlterAlterDyad;
import net.egosmart.scc.data.Ego;
import net.egosmart.scc.data.EgoAlterDyad;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.gui.dialog.EditAttributeValuesDialog;
import net.egosmart.scc.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class AttributeDetailViewFragment extends Fragment {

	private SCCMainActivity activity;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.attribute_detail_view, container, false);
		return view;
	}

	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView(){
		activity = (SCCMainActivity) getActivity();
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		String selectedAttributeDomain = network.getSelectedAttributeDomain();
		if(selectedAttributeDomain != null){
			View rootView = getView();
			if(rootView != null)
				rootView.setVisibility(View.VISIBLE);
			String selectedAttributeName = network.
					getSelectedAttribute(selectedAttributeDomain);
			if(selectedAttributeName == null)
				return;
			//set the attribute name
			TextView attrNameTextView = (TextView) activity.findViewById(R.id.detail_view_attrib_name);
			if(attrNameTextView == null)
				return;
			attrNameTextView.setText(selectedAttributeName);
			//set the attribute description
			TextView attrDescTextView = (TextView) activity.findViewById(R.id.attribute_description_text_view);
			attrDescTextView.setText(network.getAttributeDescription(
					selectedAttributeDomain, selectedAttributeName));
			//set the attribute domain
			TextView attrDomainTextView = (TextView) activity.findViewById(R.id.attribute_domain_text_view);
			attrDomainTextView.setText(selectedAttributeDomain);
			if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(selectedAttributeDomain) ||
					PersonalNetwork.DOMAIN_ALTER_ALTER.equals(selectedAttributeDomain)){
				String direction = network.getAttributeDirectionType(selectedAttributeDomain,
						selectedAttributeName);
				String dirToken = "";
				if(direction.equals(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC)){
					dirToken = activity.getString(R.string.direction_text_symmetric);
				}		
				if(direction.equals(PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC)){
					dirToken = activity.getString(R.string.direction_text_asymmetric);
				}		
				if(direction.equals(PersonalNetwork.DYAD_DIRECTION_OUT)){
					dirToken = activity.getString(R.string.direction_text_ego_to_alter);
				}		
				if(direction.equals(PersonalNetwork.DYAD_DIRECTION_IN)){
					dirToken = activity.getString(R.string.direction_text_alter_to_ego);
				}		
				attrDomainTextView.setText(selectedAttributeDomain + " (" + dirToken + ")");			
			}
			//set the attribute type
			int attrType = -1;
			attrType = network.getAttributeValueType(selectedAttributeDomain, 
					selectedAttributeName);
			if(attrType >= 0){
				TextView attrTypeTextView = (TextView) activity.findViewById(R.id.attribute_type_text_view);
				attrTypeTextView.setText(PersonalNetwork.ATTRIB_TYPE_NAMES[attrType]);
			}
			//set attribute choices
			TextView choicesView = (TextView) getActivity().findViewById(R.id.attribute_choices_text_view);
			if(attrType	== PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
				HashSet<String> choicesSet = network.
						getAttributeChoices(selectedAttributeDomain, selectedAttributeName);
				StringBuffer choicesText = new StringBuffer("| ");
				for(String choice : choicesSet){
					choicesText.append(choice).append(" | ");
				}
				choicesView.setText(choicesText.toString());
			} else {
				choicesView.setText("");
			}
			//configure the button to edit attributes
			ImageButton editAttributeValuesButton = (ImageButton) activity.
					findViewById(R.id.edit_attribute_values_button);
			editAttributeValuesButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					DialogFragment dialog = EditAttributeValuesDialog.getInstance(activity);
					dialog.show(activity.getSupportFragmentManager(), 
							SCCMainActivity.EDIT_ATTRIBUTE_VALUES_DIALOG_TAG);					
				}
			});
			LinearLayout valuesContainer = (LinearLayout) activity.
					findViewById(R.id.attribute_detail_view_values_container);
			valuesContainer.removeAllViews();
			if(PersonalNetwork.DOMAIN_EGO.equals(selectedAttributeDomain)){
				fillEgoAttributeSummary(network, selectedAttributeName,	valuesContainer);
			}
			if(PersonalNetwork.DOMAIN_ALTER.equals(selectedAttributeDomain)){
				fillAlterAttributeSummary(network, selectedAttributeName, valuesContainer);
			}
			if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(selectedAttributeDomain)){
				fillEgoAlterAttributeSummary(network, selectedAttributeName, valuesContainer);
			}
			if(PersonalNetwork.DOMAIN_ALTER_ALTER.equals(selectedAttributeDomain)){
				fillAlterAlterAttributeSummary(network, selectedAttributeName, valuesContainer);
			}
		} else { //selected attribute domain is null --> hide the view
			//TODO: change this! display an info message
			View rootView = getView();
			if(rootView != null)
				rootView.setVisibility(View.INVISIBLE);
		}

	}

	private void addValue2AltersRow(TableLayout table,
			HashMap<String, String> alternameValuePairs,
			HashSet<String> uniqueAttributeValues) {
		HashMap<String, StringBuffer> value2alters = new HashMap<String, StringBuffer>();
		for(String value : uniqueAttributeValues){
			value2alters.put(value, new StringBuffer());
		}
		for(String alterName : alternameValuePairs.keySet()){
			String value = alternameValuePairs.get(alterName);
			value2alters.get(value).append(alterName + " | ");
		}
		for(String value : uniqueAttributeValues){
			StringBuffer sb = value2alters.get(value);
			if(sb.length() >= 3){ //only if there is at least one alter 
				sb.replace(sb.length()-3, sb.length(), "");
				String alterSet = sb.toString();
				TableRow row = (TableRow) LayoutInflater.from(activity).
						inflate(R.layout.attribute_value_with_alters_view, null);
				TextView valueView = (TextView) row.findViewById(R.id.attribute_value_with_alters_value_field);
				valueView.setText(value);
				TextView altersView = (TextView) row.findViewById(R.id.attribute_value_with_alters_alters_field);
				altersView.setText(alterSet);
				table.addView(row);
			}
		}
	}

	private void addValue2DyadRow(TableLayout table, 
			LinkedHashMap<AlterAlterDyad, String> dirDyad2value,
			HashSet<String> uniqueAttributeValues) {
		HashMap<String, LinkedList<AlterAlterDyad>> value2dyads = 
				new HashMap<String, LinkedList<AlterAlterDyad>>();
		for(String value : uniqueAttributeValues){
			value2dyads.put(value, new LinkedList<AlterAlterDyad>());
		}
		for(AlterAlterDyad dyad : dirDyad2value.keySet()){
			String value = dirDyad2value.get(dyad);
			value2dyads.get(value).add(dyad);
		}
		for(String value : uniqueAttributeValues){
			LinkedList<AlterAlterDyad> list = value2dyads.get(value);
			if(list.size() >= 1){ //only if there is at least one dyad 
				boolean isFirst = true;
				for(AlterAlterDyad dyad : list){
					TableRow row = (TableRow) LayoutInflater.from(activity).
							inflate(R.layout.attribute_value_with_dyad_view, null);
					if(isFirst){
						TextView valueView = (TextView) row.
								findViewById(R.id.attribute_value_with_dyad_value_field);
						valueView.setText(value);
					}
					TextView sourceView = (TextView) row.
							findViewById(R.id.attribute_value_with_dyad_source_field);
					sourceView.setText(dyad.getSourceName());
					TextView targetView = (TextView) row.
							findViewById(R.id.attribute_value_with_dyad_target_field);
					targetView.setText(dyad.getTargetName());
					table.addView(row);
					isFirst = false;
				}
			}
		}
	}

	private void fillAlterAlterAttributeSummary(final PersonalNetwork network,
			final String selectedAttributeName, LinearLayout valuesContainer) {
		if(network.hasAttribute(PersonalNetwork.DOMAIN_ALTER_ALTER, selectedAttributeName)){
			TableLayout table = new TableLayout(activity);
			HashSet<String> uniqueAttributeValues = network.
					getUniqueValuesForAttributeAt(TimeInterval.getCurrentTimePoint(),
							PersonalNetwork.DOMAIN_ALTER_ALTER, selectedAttributeName);
			LinkedHashMap<AlterAlterDyad, String> undirDyad2value = network.
					getValuesOfAttributeForAllElementsAt(System.currentTimeMillis(),
							AlterAlterDyad.getInstance("s", "t"), selectedAttributeName);
			addValue2DyadRow(table, undirDyad2value, uniqueAttributeValues);
			valuesContainer.addView(table);
		}
	}

	private void fillEgoAlterAttributeSummary(final PersonalNetwork network,
			final String selectedAttributeName, LinearLayout valuesContainer) {
		if(network.hasAttribute(PersonalNetwork.DOMAIN_EGO_ALTER, selectedAttributeName)){
			TableLayout table = new TableLayout(activity);
			HashSet<String> uniqueAttributeValues = network.
					getUniqueValuesForAttributeAt(TimeInterval.getCurrentTimePoint(), 
							PersonalNetwork.DOMAIN_EGO_ALTER, selectedAttributeName);
			HashMap<EgoAlterDyad,String> egoAlterDyad2Value = network.
						getValuesOfAttributeForAllElementsAt(System.currentTimeMillis(), 
								EgoAlterDyad.getOutwardInstance("a"), selectedAttributeName);
			HashMap<String, String> alterName2Value = new HashMap<String, String>();
			for(EgoAlterDyad dyad : egoAlterDyad2Value.keySet()){
				alterName2Value.put(dyad.getName(), egoAlterDyad2Value.get(dyad));
			}
			addValue2AltersRow(table, alterName2Value, uniqueAttributeValues);
			valuesContainer.addView(table);
		}
	}

	private void fillAlterAttributeSummary(final PersonalNetwork network,
			final String selectedAttributeName, LinearLayout valuesContainer) {
		if(network.hasAttribute(PersonalNetwork.DOMAIN_ALTER, selectedAttributeName)){
			HashMap<Alter,String> alternameValuePairs = network.
					getValuesOfAttributeForAllElementsAt(System.currentTimeMillis(), 
							Alter.getInstance("a"), selectedAttributeName);
			HashSet<String> uniqueAttributeValues = network.getUniqueValuesForAttributeAt(
					TimeInterval.getCurrentTimePoint(), PersonalNetwork.DOMAIN_ALTER, selectedAttributeName);
			HashMap<String, String> alterName2Value = new HashMap<String, String>();
			for(Alter alter : alternameValuePairs.keySet()){
				alterName2Value.put(alter.getName(), alternameValuePairs.get(alter));
			}
			TableLayout table = new TableLayout(activity);
			addValue2AltersRow(table, alterName2Value, uniqueAttributeValues);
			valuesContainer.addView(table);
		}
	}

	private void fillEgoAttributeSummary(final PersonalNetwork network,
			final String selectedAttributeName, LinearLayout valuesContainer) {
		String value = network.getAttributeValueAt(System.currentTimeMillis(), selectedAttributeName, 
				Ego.getInstance());
		if(!PersonalNetwork.VALUE_NOT_ASSIGNED.equals(value)){
			TextView valueView = (TextView) LayoutInflater.from(activity).
					inflate(R.layout.attribute_value_text_view, null);
			valueView.setText(value);
			valuesContainer.addView(valueView);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}


}