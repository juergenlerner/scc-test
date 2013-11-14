/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Ego;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.gui.util.AttributeDescriptionListBaseAdapter;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Dialog that shows all ego attributes for which ego has a defined value.
 * 
 * The values of these attributes can be changed. The user might also select from the declared 
 * attributes for which ego does not have a defined value and the user might create a
 * totally new ego attribute.
 * 
 * @author juergen
 *
 */
public class EditEgoAttributesDialog extends DialogFragment {

	private static SCCMainActivity activity;

	public static EditEgoAttributesDialog getInstance(SCCMainActivity act){
		activity = act;
		return new EditEgoAttributesDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		//create and populate the view
		ViewGroup contentViewGroup = (ViewGroup) LayoutInflater.from(getActivity()).
				inflate(R.layout.edit_attributes_dialog_view, null);
		String title = getActivity().getString(R.string.edit_ego_attrib_dialog_title);
		// title is not set as title but as headline
		TextView headline = (TextView) contentViewGroup.findViewById(R.id.edit_attributes_dialog_headline);
		headline.setText(title);
		HashSet<String> allAttrNames = network.getAttributeNames(PersonalNetwork.DOMAIN_EGO);
		//mapping attribute names to value fields (which are either edittexts or spinners)
		final HashMap<String,EditText> attr2text = new HashMap<String, EditText>();
		final HashMap<String,Spinner> attr2spinner = new HashMap<String, Spinner>();
		//holds the rows of those attributes for which ego has a value
		final TableLayout attribsTable = (TableLayout) contentViewGroup.
				findViewById(R.id.edit_attributes_dialog_table);
		//mapping from attribute names for which ego has no value to table rows (can be added in the GUI) 
		final LinkedHashMap<String, TableRow> otherAttributes = new LinkedHashMap<String, TableRow>();
		//iterate over all existing ego attributes; for each of them: if the value is set for  
		//ego, create a row where the value can be changed; if not, create a row that is stored in otherAttributes
		for(final String attrName : allAttrNames){
			String value = network.getAttributeValueAt(System.currentTimeMillis(), attrName, Ego.getInstance());
			final TableRow row = (TableRow) LayoutInflater.from(activity).
					inflate(R.layout.edit_alter_attributes_table_attr_row, null);//TODO: rename (but layout is usable)
			TextView name = (TextView) row.findViewById(R.id.edit_alter_attributes_dialog_attr_name);
			name.setText(attrName);
			final String empty = "";
			FrameLayout valueContainer = (FrameLayout) row.
					findViewById(R.id.edit_alter_attributes_dialog_value_container);
			final ImageButton removeValueButton = (ImageButton) row.
					findViewById(R.id.remove_attribute_value_button);
			int attrType = network.getAttributeValueType(PersonalNetwork.DOMAIN_EGO, attrName);
			//if the type is not finite choice, the values can be given in text fields
			if(attrType != PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
				final EditText valueEditText = new EditText(activity);
				if(attrType == PersonalNetwork.ATTRIB_TYPE_NUMBER){
					valueEditText.setInputType(InputType.TYPE_CLASS_NUMBER); //shows a keyboard for numbers
				}
				valueEditText.setHint(R.string.add_attribute_value_hint);
				if(!value.equals(PersonalNetwork.VALUE_NOT_ASSIGNED))
					valueEditText.setText(value);
				else
					valueEditText.setText(empty);
				valueEditText.setSingleLine();
				attr2text.put(attrName, valueEditText);
				valueContainer.addView(valueEditText);
				//when this button is clicked, the value of the textfield is set to the empty string
				// (results in the deletion of the attribute value) and the row is moved to other attributes
				removeValueButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						attribsTable.removeView(row);
						valueEditText.setText("");
						otherAttributes.put(attrName, row);
					}
				});
			} else { // attribute type is finite choice: values can be choosen from a spinner
				final Spinner spinner = new Spinner(activity);
				HashSet<String> choices = network.getAttributeChoices(
						PersonalNetwork.DOMAIN_EGO, attrName);
				final ArrayList<String> items = new ArrayList<String>(choices);
				if(!value.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, 
							android.R.layout.simple_spinner_item, items);
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					spinner.setAdapter(adapter);
					spinner.setSelection(items.indexOf(value));
				}
				else {
					items.add(empty);
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, 
							android.R.layout.simple_spinner_item, items);
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					spinner.setAdapter(adapter);
					spinner.setSelection(items.indexOf(empty));
				}
				attr2spinner.put(attrName, spinner);
				valueContainer.addView(spinner);
				//when this button is clicked, set the selection to the empty string (results in the deletion
				//of the attribute value) and move it to the other attributes
				removeValueButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						attribsTable.removeView(row);
						items.add(empty);
						ArrayAdapter<String> newAdapter = new ArrayAdapter<String>(activity, 
								android.R.layout.simple_spinner_item, items);
						spinner.setAdapter(newAdapter);
						spinner.setSelection(items.indexOf(empty));
						otherAttributes.put(attrName, row);
					}
				});
			}
			//if a value is set, add it to the table; otherwise just remember the row
			if(!value.equals(PersonalNetwork.VALUE_NOT_ASSIGNED))
				attribsTable.addView(row);
			else
				otherAttributes.put(attrName, row);
		}
		//clicking this button shows the list of other attributes in a new child dialog; clicking on a list item
		// adds that attribute to the attributes table where a value can be set
		final Button addOtherAttribs = (Button) contentViewGroup.
				findViewById(R.id.add_other_attrib_button);
		addOtherAttribs.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				new DialogFragment(){
					public Dialog onCreateDialog(Bundle savedInstanceState) {
						super.onCreateDialog(savedInstanceState);
						AlertDialog.Builder newAttrDialogBuilder = new AlertDialog.Builder(activity);
						//the dialog content view shows a headline, a list of ego attributes that are 
						//declared in the network but have no value for ego, and a button to 
						//create a new ego attribute
						LinearLayout contentView = (LinearLayout) LayoutInflater.from(activity).
								inflate(R.layout.add_new_attribute_4alter_view, null);//TODO: rename (but layout is usable)
						//TextView headline = (TextView) contentView.findViewById(R.id.add_new_attribute_4alter_headline);
						//headline.setText(selectedAlter); //TODO: set a headline?
						ListView attrList = (ListView) contentView.findViewById(R.id.add_new_attribute_4alter_list);
						ArrayList<Pair<String, String>> attrNameDescList = new ArrayList<Pair<String,String>>();
						for(String aName : otherAttributes.keySet()){
							attrNameDescList.add(new Pair<String, String>(aName, 
									network.getAttributeDescription(PersonalNetwork.DOMAIN_EGO, aName)));
						}
						AttributeDescriptionListBaseAdapter attrNameDescListAdapter = 
								new AttributeDescriptionListBaseAdapter(activity, attrNameDescList);
						attrList.setAdapter(attrNameDescListAdapter);
						attrList.setOnItemClickListener(new OnItemClickListener() {

							public void onItemClick(AdapterView<?> parent, View v, int position,
									long id) {
								String a = ((Pair<String,String>) parent.getItemAtPosition(position)).first;
								attribsTable.addView(otherAttributes.get(a));
								otherAttributes.remove(a);
								dismiss();
							}
						});
						//clicking this button opens yet another dialog where a totally new
						//ego attribute can be declared for the network 
						//and then a value can be selected for this alter
						Button createNewAttr = (Button) contentView.
								findViewById(R.id.create_new_attribute_from_edit_alter_dialog);
						createNewAttr.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View arg0) {
								new DialogFragment(){
									public Dialog onCreateDialog(Bundle savedInstanceState) {
										super.onCreateDialog(savedInstanceState);
										AlertDialog.Builder createAttrDialogBuilder = new AlertDialog.Builder(activity);
										createAttrDialogBuilder.setTitle(R.string.create_new_ego_label_title);
										//TODO: rename (but layout is usable)
										ViewGroup createAttrContentView = (ViewGroup) LayoutInflater.from(activity).
												inflate(R.layout.create_new_alter_attribute_from_dialog_view, null);
										final EditText attrNameEditText = (EditText) createAttrContentView.
												findViewById(R.id.new_attribute_name_edit_text);
										final EditText attrDescEditText = (EditText) createAttrContentView.
												findViewById(R.id.new_attribute_desc_edit_text);
										//stores the list of attribute choices
										final ArrayList<String> attributeChoices = new ArrayList<String>();
										//this row and the list below will be only visible if the 
										//attribute type is choice
										final LinearLayout newAttributeChoiceRow = (LinearLayout) createAttrContentView.
												findViewById(R.id.new_attribute_choice_row);
										final ListView newAttributeChoicesList = (ListView) createAttrContentView.
												findViewById(R.id.new_attribute_choices_list);
										final EditText newAttributeChoiceEditText = (EditText) createAttrContentView.
												findViewById(R.id.new_attribute_choice_edit_text);
										newAttributeChoicesList.setAdapter(new ArrayAdapter<String>(activity, 
												android.R.layout.simple_list_item_1, attributeChoices.toArray(new String[0])));
										Button addAttributeChoiceButton = (Button) createAttrContentView.
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
										final Spinner attrTypeSpinner = (Spinner) createAttrContentView
												.findViewById(R.id.new_alter_attribute_type_spinner);
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
													newAttributeChoiceRow.setVisibility(View.VISIBLE);
													newAttributeChoicesList.setVisibility(View.VISIBLE);
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
										createAttrDialogBuilder.setView(createAttrContentView);
										createAttrDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

											@Override
											public void onClick(DialogInterface dialog, int which) {
												final String attrName = attrNameEditText.getText().toString().trim();
												String attrDesc = attrDescEditText.getText().toString().trim();
												if(attrName.length() > 0 && !attrName.startsWith(PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART)){
													network.addAttribute(PersonalNetwork.DOMAIN_EGO, 
															attrName, attrDesc, 
															attrTypeSpinner.getSelectedItemPosition(),
															PersonalNetwork.DYAD_DIRECTION_SYMMETRIC,
															PersonalNetwork.ATTRIBUTE_DYNAMIC_TYPE_STATE);
													if(attrTypeSpinner.getSelectedItemPosition() == PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
														LinkedHashSet<String> choicesSet = new LinkedHashSet<String>(attributeChoices);
														network.setAttributeChoices(PersonalNetwork.DOMAIN_EGO, attrName, choicesSet);
													}
													final TableRow newlyCreatedRow = (TableRow) LayoutInflater.from(activity).
															inflate(R.layout.edit_alter_attributes_table_attr_row, null);
													TextView name = (TextView) newlyCreatedRow.findViewById(R.id.edit_alter_attributes_dialog_attr_name);
													name.setText(attrName);
													final String empty = "";
													FrameLayout valueContainer = (FrameLayout) newlyCreatedRow.
															findViewById(R.id.edit_alter_attributes_dialog_value_container);
													final ImageButton removeValueButton = (ImageButton) newlyCreatedRow.
															findViewById(R.id.remove_attribute_value_button);
													int attrType = network.getAttributeValueType(PersonalNetwork.DOMAIN_EGO, attrName);
													if(attrType != PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
														final EditText valueEditText = new EditText(activity);
														if(attrType == PersonalNetwork.ATTRIB_TYPE_NUMBER){
															valueEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
														}
														valueEditText.setHint(R.string.add_attribute_value_hint);
														valueEditText.setText(empty);
														valueEditText.setSingleLine();
														attr2text.put(attrName, valueEditText);
														valueContainer.addView(valueEditText);
														removeValueButton.setOnClickListener(new OnClickListener() {

															@Override
															public void onClick(View v) {
																attribsTable.removeView(newlyCreatedRow);
																valueEditText.setText("");
																otherAttributes.put(attrName, newlyCreatedRow);
															}
														});
													} else { // attribute type is finite choice
														final Spinner spinner = new Spinner(activity);
														HashSet<String> choices = network.getAttributeChoices(PersonalNetwork.DOMAIN_EGO, attrName);
														final ArrayList<String> items = new ArrayList<String>(choices);
														items.add(empty);
														ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, 
																android.R.layout.simple_spinner_item, items);
														adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
														spinner.setAdapter(adapter);
														spinner.setSelection(items.indexOf(empty));
														attr2spinner.put(attrName, spinner);
														valueContainer.addView(spinner);
														removeValueButton.setOnClickListener(new OnClickListener() {

															@Override
															public void onClick(View v) {
																attribsTable.removeView(newlyCreatedRow);
																items.add(empty);
																ArrayAdapter<String> newAdapter = new ArrayAdapter<String>(activity, 
																		android.R.layout.simple_spinner_item, items);
																spinner.setAdapter(newAdapter);
																spinner.setSelection(items.indexOf(empty));
																otherAttributes.put(attrName, newlyCreatedRow);
															}
														});
													}
													attribsTable.addView(newlyCreatedRow);
												} else { //it's not a valid name for a user-defined attribute
													// TODO: inform the user
												}
											}
										});
										createAttrDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

											@Override
											public void onClick(DialogInterface dialog, int which) {
												// nothing to do
											}
										});
										return createAttrDialogBuilder.create();
									}
								}.show(getFragmentManager(), null);
								dismiss(); //this closes the add new alter dialog but not the create new alter dialog
							}
						});
						newAttrDialogBuilder.setView(contentView);
						return newAttrDialogBuilder.create();
					}
				}.show(getChildFragmentManager(), null);
			}
		});
		builder.setView(contentViewGroup);
		// Add action buttons
		builder.setPositiveButton(R.string.edit_attrib_ok_button_text, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				for(String attrName : attr2text.keySet()){
					String value = attr2text.get(attrName).getText().toString().trim();
					network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(), 
							attrName, Ego.getInstance(), value);
				}
				for(String attrName : attr2spinner.keySet()){
					Spinner spinner = attr2spinner.get(attrName);
					Object selected = spinner.getSelectedItem();
					if(selected != null){
						String value = selected.toString().trim();
						network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(), 
								attrName, Ego.getInstance(), value);							
					}
				}
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
}
