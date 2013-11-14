/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.gui.util.NeighborListBaseAdapter;
import net.egosmart.scc.gui.util.NonNeighborListBaseAdapter;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author juergen
 *
 */
public class EditAlterAlterTiesDialog extends DialogFragment {

	private static SCCMainActivity activity;
	
	public static EditAlterAlterTiesDialog getInstance(SCCMainActivity act){
		activity = act;
		return new EditAlterAlterTiesDialog();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
	    final String selectedAlter = network.getSelectedAlter();
	    if(selectedAlter == null){
	    	dismiss();
	    }
	    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    String title = activity.getString(R.string.edit_alter_ties_dialog_title);
	    //title = title + " " + selectedAlter;
	    builder.setTitle(title); //TODO: we don't want a title
	    View contentView = LayoutInflater.from(activity).inflate(R.layout.edit_alter_alter_ties_dialog_view, null);
	    TextView headline = (TextView) contentView.findViewById(R.id.edit_alter_alter_ties_dialog_headline);
	    headline.setText(selectedAlter);
	    // build up the lists
		// display neighbors of the selected alter
		HashSet<String> neighbors = network.getNeighborsAt(TimeInterval.getCurrentTimePoint(), selectedAlter);
		final ListView neighborList = (ListView) contentView.findViewById(R.id.edit_alter_alter_ties_neighbor_list);
		final ListView nonNeighborList = (ListView) contentView.findViewById(R.id.edit_alter_alter_ties_non_neighbor_list);
		final ArrayList<Pair<String, String>> neigborValuePairs = new ArrayList<Pair<String,String>>();
		for(String neigh : neighbors){
			HashMap<String, String> attrValues = network.getValuesOfAllAttributesForElementAt(
					System.currentTimeMillis(), Alter.getInstance(neigh));
			StringBuffer valueStr = new StringBuffer("| ");
			for(String attrName : attrValues.keySet()){
				String value = attrValues.get(attrName);
				if(!PersonalNetwork.VALUE_NOT_ASSIGNED.equals(value)){
					valueStr
					//.append(attrName).append(':')
					.append(value).append(" | ");
				}
			}
			neigborValuePairs.add(new Pair<String, String>(neigh, valueStr.toString()));
		}
		final NeighborListBaseAdapter neighborAdapter = new NeighborListBaseAdapter(activity, neigborValuePairs);
		neighborList.setAdapter(neighborAdapter);
		HashSet<String> nonNeighbors = network.getAltersAt(TimeInterval.getCurrentTimePoint());
		nonNeighbors.
				removeAll(network.getNeighborsAt(TimeInterval.getCurrentTimePoint(), selectedAlter));
		nonNeighbors.remove(selectedAlter);
		if(selectedAlter == null)
			nonNeighbors = new HashSet<String>();
		final ArrayList<Pair<String, String>> nonNeigborValuePairs = new ArrayList<Pair<String,String>>();
		for(String nonNeigh : nonNeighbors){
			HashMap<String, String> attrValues = network.getValuesOfAllAttributesForElementAt(
					System.currentTimeMillis(), Alter.getInstance(nonNeigh));
			StringBuffer valueStr = new StringBuffer("| ");
			for(String attrName : attrValues.keySet()){
				String value = attrValues.get(attrName);
				if(!PersonalNetwork.VALUE_NOT_ASSIGNED.equals(value)){
					valueStr
					//.append(attrName).append(':')
					.append(value).append(" | ");
				}
			}
			nonNeigborValuePairs.add(new Pair<String, String>(nonNeigh, valueStr.toString()));
		}
		final NonNeighborListBaseAdapter nonNeighborAdapter = 
				new NonNeighborListBaseAdapter(activity, nonNeigborValuePairs);
		nonNeighborList.setAdapter(nonNeighborAdapter);
		//set the listeners
		neighborList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				Pair<String, String> selectedPair = (Pair<String, String>) parent.getItemAtPosition(position);
				String neighborToRemove = selectedPair.first; 
				activity.onNeighborSelected(selectedAlter, neighborToRemove, false);
				neigborValuePairs.remove(position);
				neighborAdapter.notifyDataSetChanged();
				nonNeigborValuePairs.add(selectedPair);
				nonNeighborAdapter.notifyDataSetChanged();
			}
		});
		// display non-neighbors of the selected alter
		nonNeighborList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				Pair<String, String> selectedPair = (Pair<String, String>) parent.getItemAtPosition(position);
				String neighborToAdd = selectedPair.first;
				activity.onNeighborSelected(selectedAlter, neighborToAdd, true);
				nonNeigborValuePairs.remove(position);
				nonNeighborAdapter.notifyDataSetChanged();
				neigborValuePairs.add(selectedPair);
				neighborAdapter.notifyDataSetChanged();
			}
		});
	    builder.setView(contentView);
	    // Add action buttons
	    builder.setPositiveButton(R.string.edit_alter_ties_ok_button_text, new DialogInterface.OnClickListener() {
	               	               
				public void onClick(DialogInterface dialog, int which) {

				}
	           });
	    return builder.create();
	}

		
}
