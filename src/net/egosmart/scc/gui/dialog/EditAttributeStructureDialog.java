/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;


import net.egosmart.scc.SCCMainActivity;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author juergen
 *
 */
public class EditAttributeStructureDialog extends DialogFragment {

	private static SCCMainActivity activity;

	public static EditAttributeStructureDialog getInstance(SCCMainActivity act){
		activity = act;
		return new EditAttributeStructureDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		final String selectedAttributeDomain = network.getSelectedAttributeDomain();
		final String selectedAttribute = network.getSelectedAttribute(selectedAttributeDomain);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if(selectedAttributeDomain != null){
			//create and populate the dialog view
			LinearLayout contentView = (LinearLayout) LayoutInflater.from(activity).
					inflate(R.layout.edit_attribute_structure_dialog_view, null);
			final EditText attributeNameEditText = (EditText) contentView.
					findViewById(R.id.attribute_name_edit_text);
			attributeNameEditText.setText(selectedAttribute);
			final EditText attributeDescEditText = (EditText) contentView.
					findViewById(R.id.attribute_description_edit_text);
			String desc = network.getAttributeDescription(selectedAttributeDomain, selectedAttribute);
			attributeDescEditText.setText(desc);
			// current value type of the selected attribute
			final int currentAttributeType = network.getAttributeValueType(
					selectedAttributeDomain, selectedAttribute);
			//initializes list of current attribute choices
			final ArrayList<String> attributeChoices;
			if(PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE == currentAttributeType){
				attributeChoices = new ArrayList<String>(
						network.getAttributeChoices(selectedAttributeDomain, selectedAttribute));
			} else
				attributeChoices = new ArrayList<String>();
			//store the current values
			final HashSet<String> currentValues = network.getUniqueValuesForAttributeAt(
					TimeInterval.getCurrentTimePoint(), selectedAttributeDomain, selectedAttribute);
			//this row and the list below will be only visible if the 
			//attribute type is choice
			final LinearLayout newAttributeChoiceRow = (LinearLayout) contentView.
					findViewById(R.id.new_attribute_choice_row);
			final ListView newAttributeChoicesList = (ListView) contentView.
					findViewById(R.id.attribute_choices_list);
			final EditText newAttributeChoiceEditText = (EditText) contentView.
					findViewById(R.id.new_attribute_choice_edit_text);
			newAttributeChoicesList.setAdapter(new ArrayAdapter<String>(activity, 
					android.R.layout.simple_list_item_1, attributeChoices.toArray(new String[0])));
			//remove a choice if the user clicks on it
			newAttributeChoicesList.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> parent, View v, int position,
						long id) {
					String valueToRemove = attributeChoices.get(position);
					if(!currentValues.contains(valueToRemove)){
						attributeChoices.remove(position);
						newAttributeChoicesList.setAdapter(new ArrayAdapter<String>(activity, 
								android.R.layout.simple_list_item_1, attributeChoices.toArray(new String[0])));
					}
				}
			});
			Button addAttributeChoiceButton = (Button) contentView.
					findViewById(R.id.add_new_attribute_choice_button);
			addAttributeChoiceButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					String choiceStr = newAttributeChoiceEditText.getText().toString().trim();
					if(choiceStr.length() > 0){
						attributeChoices.add(choiceStr);
						newAttributeChoicesList.setAdapter(new ArrayAdapter<String>(activity, 
								android.R.layout.simple_list_item_1, attributeChoices.toArray(new String[0])));
						newAttributeChoiceEditText.setText("");
					}
				}
			});
			final Spinner attrTypeSpinner = (Spinner) contentView
					.findViewById(R.id.alter_attribute_type_spinner);
			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getActivity(), 
					android.R.layout.simple_spinner_item, 
					PersonalNetwork.ATTRIB_TYPE_NAMES);
			spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			attrTypeSpinner.setAdapter(spinnerAdapter);
			attrTypeSpinner.setSelection(currentAttributeType);
			attrTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int pos, long id) {
					//String selectedType = parent.getItemAtPosition(pos).toString();
					if(pos == PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
						newAttributeChoiceRow.setVisibility(View.VISIBLE);
						newAttributeChoicesList.setVisibility(View.VISIBLE);
						//if the current type is not choice but the user selects this, init the choices
						if(currentAttributeType != PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
							attributeChoices.clear();
							attributeChoices.addAll(currentValues);
							newAttributeChoicesList.setAdapter(new ArrayAdapter<String>(activity, 
									android.R.layout.simple_list_item_1, attributeChoices.toArray(new String[0])));
						}
					} else {
						newAttributeChoiceRow.setVisibility(View.INVISIBLE);
						newAttributeChoicesList.setVisibility(View.INVISIBLE);
					}
				}

				@Override
				public void onNothingSelected(
						AdapterView<?> parent) {
					// nothing to do
				}
			});
			builder.setView(contentView);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String newAttributeName = attributeNameEditText.getText().toString().trim();
					if(newAttributeName.length() == 0){
						return; //TODO: give the user the possibility to correct this?
					}
					if(!selectedAttribute.equals(newAttributeName)){
						//TODO: rename (check if there is already an attribute with that name)
					}
					String attrDesc = attributeDescEditText.getText().toString().trim();
					network.setAttributeDescription(selectedAttributeDomain, newAttributeName, attrDesc);
					//check whether the chosen attribute type is compatible with the current values
					//if so update
					//if type is finite choice: update the choices 
					int newAttributeType = attrTypeSpinner.getSelectedItemPosition();
					HashSet<String> values = network.
							getUniqueValuesForAttributeAt(
									TimeInterval.getCurrentTimePoint(), selectedAttributeDomain, newAttributeName);
					if(newAttributeType != currentAttributeType){
						if(newAttributeType == PersonalNetwork.ATTRIB_TYPE_NUMBER && 
								!allValuesAreNumeric(values))
							return;
						network.setAttributeValueType(selectedAttributeDomain, newAttributeName, newAttributeType);
					}
					if(newAttributeType == PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
						LinkedHashSet<String> choicesSet = new LinkedHashSet<String>(attributeChoices);
						if(choicesSet.containsAll(values))
							network.setAttributeChoices(selectedAttributeDomain, newAttributeName, choicesSet);
					}					
					activity.updatePersonalNetworkViews();
				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// nothing to do
				}
			});
		} else {//selected attribute domain is null
			dismiss();
		}
		return builder.create();
	}

	private boolean allValuesAreNumeric(HashSet<String> values){
		for(String value: values){
			try {
				Double.parseDouble(value);
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

}
