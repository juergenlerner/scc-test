/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.util.ArrayList;
import java.util.LinkedHashSet;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * @author juergen
 *
 */
public class AddAttributeDialog extends DialogFragment {

	private static SCCMainActivity activity;

	public static AddAttributeDialog getInstance(SCCMainActivity act){
		activity = act;
		return new AddAttributeDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		ViewGroup contentView = (ViewGroup) LayoutInflater.from(activity).
				inflate(R.layout.create_new_attribute_dialog_view, null);
		final EditText attrNameEditText = (EditText) contentView.
				findViewById(R.id.new_general_attribute_name_edit_text);
		final EditText attrDescEditText = (EditText) contentView.
				findViewById(R.id.new_general_attribute_desc_edit_text);
		//stores the list of attribute choices
		final ArrayList<String> attributeChoices = new ArrayList<String>();
		//this row and the list below will be only visible if the 
		//attribute type is choice
		final LinearLayout newAttributeChoiceContainer = (LinearLayout) contentView.
				findViewById(R.id.new_general_attribute_choice_container);
		final ListView newAttributeChoicesList = (ListView) contentView.
				findViewById(R.id.new_general_attribute_choices_list);
		final EditText newAttributeChoiceEditText = (EditText) contentView.
				findViewById(R.id.new_general_attribute_choice_edit_text);
		newAttributeChoicesList.setAdapter(new ArrayAdapter<String>(activity, 
				android.R.layout.simple_list_item_1, attributeChoices.toArray(new String[0])));
		Button addAttributeChoiceButton = (Button) contentView.
				findViewById(R.id.add_new_general_attribute_choice_button);
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
		// this view will only be visible if the domain is ego-alter or alter-alter (and the selection possibilities)
		//will adapt accordingly)
		final LinearLayout attrDirectionContainer = (LinearLayout) contentView.
				findViewById(R.id.new_general_attribute_direction_container);
		final Spinner attrDirectionSpinner = (Spinner) contentView
				.findViewById(R.id.new_general_attribute_direction_spinner);
		final Spinner attrTypeSpinner = (Spinner) contentView
				.findViewById(R.id.new_general_attribute_type_spinner);
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getActivity(), 
				android.R.layout.simple_spinner_item, 
				PersonalNetwork.ATTRIB_TYPE_NAMES);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		attrTypeSpinner.setAdapter(spinnerAdapter);
		attrTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				//String selectedType = parent.getItemAtPosition(pos).toString();
				if(pos == PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
					newAttributeChoiceContainer.setVisibility(View.VISIBLE);
					newAttributeChoicesList.setVisibility(View.VISIBLE);
				} else {
					newAttributeChoiceContainer.setVisibility(View.INVISIBLE);
					newAttributeChoicesList.setVisibility(View.INVISIBLE);
				}
			}

			@Override
			public void onNothingSelected(
					AdapterView<?> parent) {
				// nothing to do
			}
		});
		final Spinner attrDomainSpinner = (Spinner) contentView
				.findViewById(R.id.new_general_attribute_domain_spinner);
		ArrayAdapter<String> attrDomainAdapter = new ArrayAdapter<String>(activity, 
				android.R.layout.simple_spinner_item, 
				new String[]{PersonalNetwork.DOMAIN_EGO,
				PersonalNetwork.DOMAIN_ALTER,
				PersonalNetwork.DOMAIN_EGO_ALTER,
				PersonalNetwork.DOMAIN_ALTER_ALTER});
		attrDomainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		attrDomainSpinner.setAdapter(attrDomainAdapter);
		attrDomainSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				String selectedDomain = parent.getItemAtPosition(pos).toString();
				if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(selectedDomain)){
					String[] dirTypes = {PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC,
							PersonalNetwork.DYAD_DIRECTION_SYMMETRIC,
							PersonalNetwork.DYAD_DIRECTION_OUT,
							PersonalNetwork.DYAD_DIRECTION_IN};
					ArrayAdapter<String> dirSpinnerAdapter = new ArrayAdapter<String>(getActivity(), 
							android.R.layout.simple_spinner_item, dirTypes);
					dirSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					attrDirectionSpinner.setAdapter(dirSpinnerAdapter);
					attrDirectionContainer.setVisibility(View.VISIBLE);
				} else if(PersonalNetwork.DOMAIN_ALTER_ALTER.equals(selectedDomain)){
					String[] dirTypes = {PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC,
							PersonalNetwork.DYAD_DIRECTION_SYMMETRIC};
					ArrayAdapter<String> dirSpinnerAdapter = new ArrayAdapter<String>(getActivity(), 
							android.R.layout.simple_spinner_item, dirTypes);
					dirSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					attrDirectionSpinner.setAdapter(dirSpinnerAdapter);
					attrDirectionContainer.setVisibility(View.VISIBLE);
				} else {
					attrDirectionContainer.setVisibility(View.INVISIBLE);
				}
			}

			@Override
			public void onNothingSelected(
					AdapterView<?> parent) {
				// nothing to do
			}

		});
		if(network.getSelectedAttributeDomain() != null)
			attrDomainSpinner.setSelection(attrDomainAdapter.getPosition(network.getSelectedAttributeDomain()));
		builder.setView(contentView);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Object attrDomainObj = attrDomainSpinner.getSelectedItem();
				String attrDomain = null;
				if(attrDomainObj != null)
					attrDomain = attrDomainObj.toString();
				Object attrDirObj = attrDirectionSpinner.getSelectedItem();
				String attrDir = null;
				if(attrDirObj != null)
					attrDir = attrDirObj.toString();
				String attrName = attrNameEditText.getText().toString().trim();
				String attrDesc = attrDescEditText.getText().toString().trim();
				LinkedHashSet<String> choicesSet = new LinkedHashSet<String>(attributeChoices);
				if(attrName.length() > 0 && !attrName.startsWith(PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART)){
					network.addAttribute(attrDomain, attrName, attrDesc, 
							attrTypeSpinner.getSelectedItemPosition(),
							attrDir,
							PersonalNetwork.ATTRIBUTE_DYNAMIC_TYPE_STATE);
					if(attrTypeSpinner.getSelectedItemPosition() == PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
						network.setAttributeChoices(attrDomain, attrName, choicesSet);
					}
					network.setSelectedAttribute(attrDomain, attrName);
					if(attrDomain != null)
						network.setSelectedAttributeDomain(attrDomain);
					activity.updatePersonalNetworkViews();
				} else{//it's not a valid user-defined attribute name 
					//TODO: ==> inform the user
				}
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// nothing to do
			}
		});
		return builder.create();
	}

}
