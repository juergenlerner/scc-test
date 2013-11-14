/**
 * 
 */
package net.egosmart.scc.gui.dialog;

import java.util.ArrayList;
import java.util.Random;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.SCCProperties;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author juergen
 *
 */
public class InterviewMeDialog extends DialogFragment {

	private static SCCMainActivity activity;
	private static Random random;

	//the current question asks to add more alters (if true)
	//otherwise data on an existing element is updated
	private boolean addAlters; 

	//the current question asks to update one alter (if true)
	//otherwise to update one attribute
	private boolean updateOneAlter;
	
	private String selectedAlter;
	private String selectedAttribute;
	
	public static InterviewMeDialog getInstance(SCCMainActivity act){
		random = new Random();
		activity = act;
		return new InterviewMeDialog();
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		super.onCreateDialog(savedInstanceState);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		//create and populate the view
		View view = LayoutInflater.from(activity).inflate(R.layout.interview_me_dialog_view, null, false);
		final LinearLayout questionContainer = (LinearLayout) view.findViewById(R.id.interview_me_question_container);
		fillNextQuestion(questionContainer);
		View nextQuestionRow = view.findViewById(R.id.interview_me_skip_to_next_question_row);
		nextQuestionRow.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				fillNextQuestion(questionContainer);
			}
		});
		View nextQuestionButton = view.findViewById(R.id.interview_me_skip_to_next_question_button);
		nextQuestionButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				fillNextQuestion(questionContainer);
			}
		});
		builder.setView(view);
		// Add action buttons
		builder.setPositiveButton(R.string.let_me_do_it, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) {
				if(addAlters){
					activity.addAlterItemClicked();
				} else {
					if(updateOneAlter){
						PersonalNetwork.getInstance(activity).setSelectedAlter(selectedAlter);
						SCCProperties.getInstance(activity).setPropertyShowDetailInSinglePaneView(true);
						activity.switchToAlterView();
					} else { //update one attribute
						PersonalNetwork network = PersonalNetwork.getInstance(activity);
						network.setSelectedAttributeDomain(PersonalNetwork.DOMAIN_ALTER);
						network.setSelectedAttribute(PersonalNetwork.DOMAIN_ALTER, selectedAttribute);
						SCCProperties.getInstance(activity).setPropertyShowDetailInSinglePaneView(true);
						activity.switchToAttributeView();						
					}
				}
			}
		});      
		// Add action buttons
		builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) {
				//nothing to do
			}
		});      
		return builder.create();
	}

	private void fillNextQuestion(LinearLayout questionContainer) {
		questionContainer.removeAllViews();
		PersonalNetwork network = PersonalNetwork.getInstance(activity);
		int n = network.getNumberOfAltersAt(TimeInterval.getCurrentTimePoint());
		// decide about adding more alters
		if(n == 0 || random.nextDouble() <= 1.0/n + 0.05){
			addAlters = true;
			TextView contentView = (TextView) LayoutInflater.from(activity).
					inflate(R.layout.question_content_view_simple_question, null);
			contentView.setText(R.string.question_text_add_more_alters);
			questionContainer.addView(contentView);
		} else {
			addAlters = false;
			//decide about update alters vs. labels (vs. ego?)
			if(random.nextDouble() <= 0.5){ //update one alter
				updateOneAlter = true;
				ArrayList<String> alters = new ArrayList<String>(network.getAltersAt(TimeInterval.getCurrentTimePoint()));
				int i = random.nextInt(alters.size());
				selectedAlter = alters.get(i);
				View contentView = LayoutInflater.from(activity).
						inflate(R.layout.question_content_view_about_alter, null);
				TextView view = (TextView) contentView.
						findViewById(R.id.question_content_view_about_alter_text_before);
				view.setText(R.string.question_text_about_alter_before);
				view = (TextView) contentView.
						findViewById(R.id.question_content_view_about_alter_text_after);
				view.setText(R.string.question_text_about_alter_after);
				view = (TextView) contentView.
						findViewById(R.id.question_content_view_about_alter_altername);
				view.setText(selectedAlter);
				questionContainer.addView(contentView);
				
			} else { //update one attribute
				updateOneAlter = false;
				//TODO also allow the selection of attributes for other domains
				ArrayList<String> attrNames = new ArrayList<String>(network.getAttributeNames(PersonalNetwork.DOMAIN_ALTER));
				int i = random.nextInt(attrNames.size());
				selectedAttribute = attrNames.get(i);
				View contentView = LayoutInflater.from(activity).
						inflate(R.layout.question_content_view_about_attribute, null);
				TextView view = (TextView) contentView.
						findViewById(R.id.question_content_view_about_attribute_text_before);
				view.setText(R.string.question_text_about_alter_attribute_before);
				view = (TextView) contentView.
						findViewById(R.id.question_content_view_about_attribute_text_after);
				view.setText(R.string.question_text_about_alter_attribute_after);
				view = (TextView) contentView.
						findViewById(R.id.question_content_view_about_attribute_attrname);
				view.setText(selectedAttribute);
				questionContainer.addView(contentView);				
			}
		}
	}

}
