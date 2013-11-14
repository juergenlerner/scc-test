/**
 * 
 */
package net.egosmart.scc.gui.dialog;


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
import android.widget.TextView;

/**
 * 
 * Allows to chose from several alter events: alter memos, ego-alter contact events, 
 * 
 * @author juergen
 *
 */
public class AddAlterEventDialog extends DialogFragment {

	private static SCCMainActivity activity;

	public static AddAlterEventDialog getInstance(SCCMainActivity act){
		activity = act;
		return new AddAlterEventDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		final String selectedAlter = network.getSelectedAlter();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if(selectedAlter != null){
			View contentView = LayoutInflater.from(activity).inflate(R.layout.add_alter_event_dialog_view, null);
			TextView alterNameView = (TextView) contentView.findViewById(R.id.add_alter_event_dialog_altername);
			alterNameView.setText(selectedAlter);
			builder.setView(contentView);
			builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// nothing to do
				}
			});
		} else {//selected alter is null
			dismiss();
		}
		return builder.create();
	}

}
