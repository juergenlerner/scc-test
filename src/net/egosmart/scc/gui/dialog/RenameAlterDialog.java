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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author juergen
 *
 */
public class RenameAlterDialog extends DialogFragment {

	private static SCCMainActivity activity;

	public static RenameAlterDialog getInstance(SCCMainActivity act){
		activity = act;
		return new RenameAlterDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		final String selectedAlter = network.getSelectedAlter();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		if(selectedAlter != null){
			View contentView = LayoutInflater.from(activity).inflate(R.layout.rename_alter_dialog_view, null);
			TextView alterNameView = (TextView) contentView.findViewById(R.id.rename_alter_dialog_altername);
			alterNameView.setText(selectedAlter);
			final EditText newAlterNameEditText = (EditText) contentView.
					findViewById(R.id.rename_alter_dialog_new_altername);
			builder.setView(contentView);
			builder.setPositiveButton(R.string.rename_alter_button_text, 
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					String newAlterName = newAlterNameEditText.getText().toString().trim();
					PersonalNetwork history = PersonalNetwork.getInstance(activity);
					if(newAlterName == null || newAlterName.length() == 0){
						Toast.makeText(activity, "alter name must not be empty", Toast.LENGTH_LONG).show();
						activity.renameAlter(getView());
					} else if(history.hasAlterAt(TimeInterval.getMaxInterval(), newAlterName)){
						Toast.makeText(activity, "alter " + newAlterName + 
								" is already in your network", Toast.LENGTH_LONG).show();
						activity.renameAlter(getView());
					} else {
						network.renameAlter(selectedAlter, newAlterName);
					}
				}
			});
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
