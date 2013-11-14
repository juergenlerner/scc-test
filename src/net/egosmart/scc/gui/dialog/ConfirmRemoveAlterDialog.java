/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * @author juergen
 *
 */
public class ConfirmRemoveAlterDialog extends DialogFragment {

	private static SCCMainActivity activity;

	public static ConfirmRemoveAlterDialog getInstance(SCCMainActivity act){
		activity = act;
		return new ConfirmRemoveAlterDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		final String selectedAlter = network.getSelectedAlter();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if(selectedAlter != null){
			//create and populate the dialog view
			builder.setTitle(R.string.confirm_remove_alter_dialog_title);
			String message = activity.getString(R.string.confirm_remove_alter_message_part_1) +
					"\n" + selectedAlter + "\n" +
					activity.getString(R.string.confirm_remove_alter_message_part_2);
			builder.setMessage(message);
			builder.setPositiveButton(R.string.delete_button_text, 
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					network.removeAlterAt(TimeInterval.getRightUnboundedFromNow(), selectedAlter);
					activity.updatePersonalNetworkViews();
				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

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
