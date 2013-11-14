/**
 * 
 */
package net.egosmart.scc.gui.dialog;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.collect.ImportContentProviderData;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;

/**
 * @author juergen
 *
 */
public class SuggestAltersDialog extends DialogFragment {

	private static SCCMainActivity activity;
	
	public static SuggestAltersDialog getInstance(SCCMainActivity act){
		activity = act;
		return new SuggestAltersDialog();
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
	    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    //create and populate the table
	    View view = LayoutInflater.from(activity).inflate(R.layout.suggest_alters_dialog_view, null, false);
	    ListView altersList = (ListView) view.findViewById(R.id.suggest_alters_dialog_alters_list);
	    ImportContentProviderData.fillAlterSuggestions(activity, altersList, 30);
	    builder.setView(view);
	    // Add action buttons
	    builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
	        	   
	               public void onClick(DialogInterface dialog, int id) {
	            	   //nothing to do
	               }
	           });      
	    return builder.create();
	}
		
}
