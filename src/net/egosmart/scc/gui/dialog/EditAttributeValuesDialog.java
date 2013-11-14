/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.Ego;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * @author juergen
 *
 */
public class EditAttributeValuesDialog extends DialogFragment {

	private static SCCMainActivity activity;
	private static PersonalNetwork network;
	private static String selectedAttributeDomain;
	private static String selectedAttribute;
	
	//informs the user which element is selected so that it's value can be (re)assigned
	private static TextView displaySelectedElementTextView;
	

	//value currently set for ego (N/A if the value should be removed)
	private static String egoValue;
	//text view displaying the ego name (me)
	private static TextView egoTextView;
	//maps attribute values to the view group that contains the text view showing me
	private static HashMap<String, LinearLayout> value2egoViewGroup;
	//the view group for ego without values
	private static LinearLayout viewOfEgoWithoutValues;
	
	//variables for ALTER attributes
	//maps alters to the values that should be assigned (if the user clicks the positive button); 
	//alters map to N/A if the value should be removed but was set previously
	private static HashMap<String, String> alter2value;
	//maps attribute values to the view group that contains the text views showing the alters
	private static HashMap<String, LinearLayout> value2altersView;
	//the view group for alters without values
	private static LinearLayout viewOfAltersWithoutValues;
	//maps alter names to the text views displaying them
	private static HashMap<String, TextView> alters2textView;
	//name of alter currently selected for value assignment
	private static String locallySelectedAlter = null;
	
	public static EditAttributeValuesDialog getInstance(SCCMainActivity act){
		activity = act;
		return new EditAttributeValuesDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		network = PersonalNetwork.getInstance(activity);
		selectedAttributeDomain = network.getSelectedAttributeDomain();
		selectedAttribute = network.getSelectedAttribute(selectedAttributeDomain);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if(selectedAttributeDomain != null && //TODO: remove the AND condition when implementation is complete 
				(PersonalNetwork.DOMAIN_EGO.equals(selectedAttributeDomain) 
						|| PersonalNetwork.DOMAIN_ALTER.equals(selectedAttributeDomain))){
			//create and populate the dialog view
			LinearLayout contentView = (LinearLayout) LayoutInflater.from(activity).
					inflate(R.layout.edit_attribute_values_dialog_view, null);
			//display the attribute name
			TextView attrNameView = (TextView) contentView.findViewById(R.id.edit_attribute_values_attribute_name);
			attrNameView.setText(selectedAttribute);
			displaySelectedElementTextView = (TextView) contentView.findViewById(R.id.edit_attribute_values_name_of_selected_element);
			// get and fill the values container
			LinearLayout valuesContainer = (LinearLayout) contentView.findViewById(R.id.edit_attribute_values_values_container);
			if(PersonalNetwork.DOMAIN_EGO.equals(selectedAttributeDomain))
				fillViewsForEgoAttributes(valuesContainer, contentView);
			else if(PersonalNetwork.DOMAIN_ALTER.equals(selectedAttributeDomain))
				fillViewsForAlterAttributes(valuesContainer, contentView);
			else if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(selectedAttributeDomain))
				fillViewsForEgoAlterAttributes(valuesContainer, contentView);
			else 
				fillViewsForAlterAlterAttributes(valuesContainer, contentView);
			builder.setView(contentView);
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(PersonalNetwork.DOMAIN_EGO.equals(selectedAttributeDomain)){
						network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
								selectedAttribute, Ego.getInstance(), egoValue);
					} else if(PersonalNetwork.DOMAIN_ALTER.equals(selectedAttributeDomain)){
						for(String alterName: alter2value.keySet()){
							network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
									selectedAttribute, Alter.getInstance(alterName), alter2value.get(alterName));
						}	
					} else if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(selectedAttributeDomain)){
						//TODO
					} else {//alter-alter attribute
						//TODO
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

	private void fillViewsForEgoAttributes(LinearLayout valuesContainer, LinearLayout contentView) {
		displaySelectedElementTextView.setText(R.string.me);
		final TableLayout table = new TableLayout(activity);
		addValue2EgoRow(table, network.getAttributeValueAt(System.currentTimeMillis(), 
				selectedAttribute, Ego.getInstance()));
		valuesContainer.addView(table);
		//configure add value button
		Button addValueButton = (Button) contentView.findViewById(R.id.edit_attribute_values_add_value_button);
		addValueButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new DialogFragment(){
					public Dialog onCreateDialog(Bundle savedInstanceState) {
						super.onCreateDialog(savedInstanceState);
						AlertDialog.Builder addValueDialogBuilder = new AlertDialog.Builder(activity);
						if(network.getAttributeValueType(PersonalNetwork.DOMAIN_EGO, selectedAttribute) == 
								PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
							HashSet<String> choices = network.getAttributeChoices(
									PersonalNetwork.DOMAIN_EGO, selectedAttribute);
							choices.removeAll(value2egoViewGroup.keySet());
							if(choices.size() > 0){
								final Spinner attrValueSpinner = (Spinner) new Spinner(activity);
								ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(activity, 
										android.R.layout.simple_spinner_item, 
										choices.toArray(new String[0]));
								spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
								attrValueSpinner.setAdapter(spinnerAdapter);
								addValueDialogBuilder.setView(attrValueSpinner);
								addValueDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Object item = attrValueSpinner.getSelectedItem();
										if(item != null){
											final String newValue = item.toString(); 
											if(!value2egoViewGroup.containsKey(newValue))
												table.addView(createNewValueRowForEgoAttribute(newValue));
										}
									}

								});
								addValueDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										//nothing to do
									}
								});
							} else { //there are no choices which are not yet assigned to some alter
								TextView message = new TextView(activity);
								message.setText("no more choices available \n " +
										"consider editing the attribute structure \n" +
										"(click the edit button on top)");
								addValueDialogBuilder.setView(message);
							}
						} else { //attribute type is text or number
							final EditText valueEditText = new EditText(activity);
							valueEditText.setHint(R.string.edit_attribute_values_new_value_choice_hint);
							if(network.getAttributeValueType(PersonalNetwork.DOMAIN_EGO, selectedAttribute) == 
									PersonalNetwork.ATTRIB_TYPE_NUMBER)
								valueEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
							addValueDialogBuilder.setView(valueEditText);
							addValueDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									String newValue = valueEditText.getText().toString();
									if(newValue != null && !value2egoViewGroup.containsKey(newValue)){
											table.addView(createNewValueRowForEgoAttribute(newValue));
									}
								}
								
							});
							addValueDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									//nothing to do
								}
							});								
						}
						return addValueDialogBuilder.create();
					}
				}.show(getFragmentManager(), null);
			}
		});
		//configure row showing ego with no values assigned
		TextView noValueView = (TextView) contentView.findViewById(R.id.edit_attribute_values_no_value_assigned);
		noValueView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String previousValue = egoValue;
				if(previousValue != null && !previousValue.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
					LinearLayout previousRow = value2egoViewGroup.get(previousValue);
					previousRow.removeView(egoTextView);
					viewOfEgoWithoutValues.addView(egoTextView);
					egoValue = PersonalNetwork.VALUE_NOT_ASSIGNED;
				}
			}
		});
		viewOfEgoWithoutValues = (LinearLayout) contentView.
				findViewById(R.id.edit_attribute_values_row_without_values);
		String value = network.getAttributeValueAt(System.currentTimeMillis(), selectedAttribute, Ego.getInstance());
		if(value == null || 
				value.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
			viewOfEgoWithoutValues.addView(egoTextView);
		}
	}		

	private void fillViewsForAlterAttributes(LinearLayout valuesContainer, LinearLayout contentView) {
		if(network.hasAttribute(PersonalNetwork.DOMAIN_ALTER, selectedAttribute)){
			LinkedHashMap<Alter,String> alternameValuePairs = 
					network.getValuesOfAttributeForAllElementsAt(System.currentTimeMillis(),
							Alter.getInstance("a"), selectedAttribute);
			final HashSet<String> uniqueAttributeValues = network.getUniqueValuesForAttributeAt(
					TimeInterval.getCurrentTimePoint(), PersonalNetwork.DOMAIN_ALTER, selectedAttribute);
			final TableLayout table = new TableLayout(activity);
			HashMap<String, String> alter2ValueLocal = new HashMap<String, String>();
			for(Alter alter : alternameValuePairs.keySet()){
				alter2ValueLocal.put(alter.getName(), alternameValuePairs.get(alter));
			}
			addValue2AltersRows(table, alter2ValueLocal, uniqueAttributeValues);
			valuesContainer.addView(table);
			//configure add value button
			Button addValueButton = (Button) contentView.findViewById(R.id.edit_attribute_values_add_value_button);
			addValueButton.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					new DialogFragment(){
						public Dialog onCreateDialog(Bundle savedInstanceState) {
							super.onCreateDialog(savedInstanceState);
							AlertDialog.Builder addValueDialogBuilder = new AlertDialog.Builder(activity);
							if(network.getAttributeValueType(
									PersonalNetwork.DOMAIN_ALTER, selectedAttribute) == 
									PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
								HashSet<String> choices = network.getAttributeChoices(
										PersonalNetwork.DOMAIN_ALTER, selectedAttribute);
								choices.removeAll(value2altersView.keySet());
								if(choices.size() > 0){
									final Spinner attrValueSpinner = (Spinner) new Spinner(activity);
									ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(activity, 
											android.R.layout.simple_spinner_item, 
											choices.toArray(new String[0]));
									spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
									attrValueSpinner.setAdapter(spinnerAdapter);
									addValueDialogBuilder.setView(attrValueSpinner);
									addValueDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											Object item = attrValueSpinner.getSelectedItem();
											if(item != null){
												final String newValue = item.toString(); 
												if(!value2altersView.containsKey(newValue))
													table.addView(createNewValueRowForAlterAttribute(newValue));
											}
										}

									});
									addValueDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											//nothing to do
										}
									});
								} else { //there are no choices which are not yet assigned to some alter
									TextView message = new TextView(activity);
									message.setText("no more choices available \n " +
											"consider editing the attribute structure \n" +
											"(click the edit button on top)");
									addValueDialogBuilder.setView(message);
								}
							} else { //attribute type is text or number
								final EditText valueEditText = new EditText(activity);
								valueEditText.setHint(R.string.edit_attribute_values_new_value_choice_hint);
								if(network.getAttributeValueType(
										PersonalNetwork.DOMAIN_ALTER, selectedAttribute) == 
										PersonalNetwork.ATTRIB_TYPE_NUMBER)
									valueEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
								addValueDialogBuilder.setView(valueEditText);
								addValueDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										String newValue = valueEditText.getText().toString();
										if(newValue != null &&!value2altersView.containsKey(newValue)){
												table.addView(createNewValueRowForAlterAttribute(newValue));
										}
									}

								});
								addValueDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										//nothing to do
									}
								});								
							}
							return addValueDialogBuilder.create();
						}
					}.show(getFragmentManager(), null);
				}
			});
			//configure row showing alters with no values assigned
			TextView noValueView = (TextView) contentView.findViewById(R.id.edit_attribute_values_no_value_assigned);
			noValueView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(locallySelectedAlter != null){
						String previousValue = alter2value.get(locallySelectedAlter);
						TextView viewOfSelectedAlter = null;
						if(previousValue != null && !previousValue.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
							LinearLayout previousRow = value2altersView.get(previousValue);
							viewOfSelectedAlter = alters2textView.get(locallySelectedAlter);
							previousRow.removeView(viewOfSelectedAlter);
							viewOfAltersWithoutValues.addView(viewOfSelectedAlter);
							alter2value.put(locallySelectedAlter, PersonalNetwork.VALUE_NOT_ASSIGNED);
							locallySelectedAlter = null;
							displaySelectedElementTextView.setText("");
						}
					}
				}
			});
			viewOfAltersWithoutValues = (LinearLayout) contentView.
					findViewById(R.id.edit_attribute_values_row_without_values);
			HashSet<String> altersWithoutValues = network.getAltersAt(TimeInterval.getCurrentTimePoint());
			altersWithoutValues.removeAll(alternameValuePairs.keySet());
			for(final String alterName : altersWithoutValues){
				TextView alterView = (TextView) LayoutInflater.from(activity).
						inflate(R.layout.clickable_edit_attribute_values_for_element_view, null);
				alterView.setText(alterName + " |");
				alters2textView.put(alterName, alterView);
				alterView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						locallySelectedAlter = alterName;
						displaySelectedElementTextView.setText(alterName);
					}
				});
				viewOfAltersWithoutValues.addView(alterView);
			}
		}		
	}

	private void fillViewsForEgoAlterAttributes(LinearLayout valuesContainer, LinearLayout contentView) {
		// TODO Auto-generated method stub
		
	}

	private void fillViewsForAlterAlterAttributes(LinearLayout valuesContainer, LinearLayout contentView) {
		//TODO
	}

	private TableRow createNewValueRowForEgoAttribute(
			final String newValue) {
		TableRow row = (TableRow) LayoutInflater.from(activity).
				inflate(R.layout.edit_attribute_value_row, null);
		TextView valueView = (TextView) row.findViewById(R.id.edit_attribute_value_row_value_field);
		valueView.setText(newValue);
		valueView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String previousValue = egoValue;
				if(previousValue != null && !previousValue.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
					LinearLayout previousRow = value2egoViewGroup.get(previousValue);
					previousRow.removeView(egoTextView);
				} else {
					viewOfEgoWithoutValues.removeView(egoTextView);
				}
				value2egoViewGroup.get(newValue).addView(egoTextView);
				egoValue = newValue;
			}
		});
		LinearLayout egoViewGroup = (LinearLayout) row.
				findViewById(R.id.edit_attribute_value_row_elements_view);
		value2egoViewGroup.put(newValue, egoViewGroup);
		return row;
	}

	private TableRow createNewValueRowForAlterAttribute(
			final String newValue) {
		TableRow row = (TableRow) LayoutInflater.from(activity).
				inflate(R.layout.edit_attribute_value_row, null);
		TextView valueView = (TextView) row.findViewById(R.id.edit_attribute_value_row_value_field);
		valueView.setText(newValue);
		valueView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(locallySelectedAlter != null){
					String previousValue = alter2value.get(locallySelectedAlter);
					TextView viewOfSelectedAlter = null;
					if(previousValue != null && !previousValue.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
						LinearLayout previousRow = value2altersView.get(previousValue);
						viewOfSelectedAlter = alters2textView.get(locallySelectedAlter);
						previousRow.removeView(viewOfSelectedAlter);
					} else {
						viewOfSelectedAlter = alters2textView.get(locallySelectedAlter);
						viewOfAltersWithoutValues.removeView(viewOfSelectedAlter);
					}
					value2altersView.get(newValue).addView(viewOfSelectedAlter);
					alter2value.put(locallySelectedAlter, newValue);
					locallySelectedAlter = null;
					displaySelectedElementTextView.setText("");
				}
			}
		});
		LinearLayout altersView = (LinearLayout) row.
				findViewById(R.id.edit_attribute_value_row_elements_view);
		value2altersView.put(newValue, altersView);
		return row;
	}

	private void addValue2EgoRow(TableLayout table, String value) {
		value2egoViewGroup = new HashMap<String, LinearLayout>();
		egoTextView =  (TextView) LayoutInflater.from(activity).
				inflate(R.layout.clickable_edit_attribute_values_for_element_view, null);
		egoTextView.setText(R.string.me);
		final String thisValue;
		if(value != null && !value.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
			thisValue = value;
			egoValue = value;
		}
		else{
			egoValue = null;
			thisValue = null;
		}
		if(thisValue != null){
			TableRow row = (TableRow) LayoutInflater.from(activity).
					inflate(R.layout.edit_attribute_value_row, null);
			TextView valueView = (TextView) row.findViewById(R.id.edit_attribute_value_row_value_field);
			valueView.setText(thisValue);
			valueView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					String previousValue = egoValue;
					if(previousValue != null && !previousValue.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
						LinearLayout previousRow = value2egoViewGroup.get(previousValue);
						previousRow.removeView(egoTextView);
					} else {
						viewOfEgoWithoutValues.removeView(egoTextView);
					}
					value2egoViewGroup.get(thisValue).addView(egoTextView);
					egoValue = thisValue;
				}
			});
			LinearLayout egoViewGroup = (LinearLayout) row.
					findViewById(R.id.edit_attribute_value_row_elements_view);
			value2egoViewGroup.put(thisValue, egoViewGroup);
			egoViewGroup.addView(egoTextView);
			table.addView(row);
		}
	}

	private void addValue2AltersRows(TableLayout table,
			HashMap<String, String> alternameValuePairs,
			HashSet<String> uniqueAttributeValues) {
		HashMap<String, LinkedHashSet<String>> value2alters = new HashMap<String, LinkedHashSet<String>>();
		alter2value = alternameValuePairs;
		value2altersView = new HashMap<String, LinearLayout>();
		alters2textView = new HashMap<String, TextView>();
		for(String value : uniqueAttributeValues){
			value2alters.put(value, new LinkedHashSet<String>());
		}
		for(String alterName : alternameValuePairs.keySet()){
			String value = alternameValuePairs.get(alterName);
			value2alters.get(value).add(alterName);
		}
		for(final String value : uniqueAttributeValues){
			LinkedHashSet<String> alters = value2alters.get(value);
			TableRow row = (TableRow) LayoutInflater.from(activity).
					inflate(R.layout.edit_attribute_value_row, null);
			TextView valueView = (TextView) row.findViewById(R.id.edit_attribute_value_row_value_field);
			valueView.setText(value);
			valueView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(locallySelectedAlter != null){
						String previousValue = alter2value.get(locallySelectedAlter);
						TextView viewOfSelectedAlter = null;
						if(previousValue != null && !previousValue.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
							LinearLayout previousRow = value2altersView.get(previousValue);
							viewOfSelectedAlter = alters2textView.get(locallySelectedAlter);
							previousRow.removeView(viewOfSelectedAlter);
						} else {
							viewOfSelectedAlter = alters2textView.get(locallySelectedAlter);
							viewOfAltersWithoutValues.removeView(viewOfSelectedAlter);
						}
						value2altersView.get(value).addView(viewOfSelectedAlter);
						alter2value.put(locallySelectedAlter, value);
						locallySelectedAlter = null;
						displaySelectedElementTextView.setText("");
					}
				}
			});
			LinearLayout altersView = (LinearLayout) row.
					findViewById(R.id.edit_attribute_value_row_elements_view);
			value2altersView.put(value, altersView);
			for(final String alterName : alters){
				TextView alterView = (TextView) LayoutInflater.from(activity).
						inflate(R.layout.clickable_edit_attribute_values_for_element_view, null);
				alterView.setText(alterName + " |");
				alters2textView.put(alterName, alterView);
				alterView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						locallySelectedAlter = alterName;
						displaySelectedElementTextView.setText(alterName);
					}
				});
				altersView.addView(alterView);
			}
			table.addView(row);
		}
	}

}
