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
public class AddAlterAlterEventDialog extends DialogFragment {

	private static SCCMainActivity activity;

	public static AddAlterAlterEventDialog getInstance(SCCMainActivity act){
		activity = act;
		return new AddAlterAlterEventDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		final String selectedAlter = network.getSelectedAlter();
		final String selectedSecondAlter = network.getSelectedSecondAlter();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if(selectedAlter != null && selectedSecondAlter != null){
			View contentView = LayoutInflater.from(activity).inflate(R.layout.add_alter_alter_event_dialog_view, null);
			TextView tieNameView = (TextView) contentView.findViewById(R.id.add_alter_alter_event_dialog_tiename);
			tieNameView.setText(selectedAlter + "\n" + selectedSecondAlter);
			builder.setView(contentView);
			builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// nothing to do
				}
			});
		} else {//selected alter pair is null
			dismiss();
		}
		return builder.create();
	}

}
