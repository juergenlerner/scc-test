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

/**
 * @author juergen
 *
 */
//TODO: can attributes be removed from the history?
public class ConfirmRemoveAttributeDialog extends DialogFragment {

	private static SCCMainActivity activity;

	public static ConfirmRemoveAttributeDialog getInstance(SCCMainActivity act){
		activity = act;
		return new ConfirmRemoveAttributeDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		final String selectedAttributeDomain = network.getSelectedAttributeDomain();
		final String selectedAttribute = network.getSelectedAttribute(selectedAttributeDomain);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.confirm_remove_attribute_dialog_title);
		String message = activity.getString(R.string.confirm_remove_attribute_message_part_1) +
				"\n" + selectedAttribute + "\n" +
				activity.getString(R.string.confirm_remove_attribute_message_part_2);
		builder.setMessage(message);
		if(selectedAttributeDomain != null){
			//create and populate the dialog view
			builder.setPositiveButton(R.string.delete_button_text, 
					new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
//					if(PersonalNetwork.DOMAIN_EGO.equals(selectedAttributeDomain))
//						network.removeEgoAttribute(selectedAttribute);
//					else if(PersonalNetwork.DOMAIN_ALTER.equals(selectedAttributeDomain))
//						network.removeAlterAttribute(selectedAttribute);
//					else if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(selectedAttributeDomain))
//						network.removeEgoAlterDyadAttribute(selectedAttribute);
//					else 
//						network.removeAlterAlterDyadAttribute(selectedAttribute);
//					activity.updatePersonalNetworkViews();
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

}
