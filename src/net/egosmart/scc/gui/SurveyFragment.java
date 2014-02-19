/**
 * 
 */
package net.egosmart.scc.gui;

import java.util.Set;


import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.collect.Question;
import net.egosmart.scc.collect.InterviewManager;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.AlterAlterDyad;
import net.egosmart.scc.data.Ego;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.gui.util.DynamicViewManager;
import net.egosmart.scc.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * @author juergen
 *
 */
public class SurveyFragment extends Fragment {
	
	private SCCMainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.survey_view, container, false);
    }
    
	public void onStart(){
		super.onStart();
		updateView();
	}
	
	public void updateView() {
		TextView surveyHeadline = (TextView) activity.findViewById(R.id.survey_view_headline_textview);
		if(surveyHeadline == null)
			return;
		final InterviewManager questionnaire = InterviewManager.getInstance(activity);
		surveyHeadline.setText(activity.getString(R.string.survey_view_headline) +
				" " + questionnaire.getName());
		// show question and potentially answer choices dependent on type
		LinearLayout questionContainer = (LinearLayout) activity.findViewById(R.id.survey_view_question_container);
		questionContainer.removeAllViews();
		if(questionnaire.hasCurrentQuestion()){
			// fill question formulation and edit area dependent on current question and answer type
			fillAnswerEditArea(questionContainer);
		} else if(questionnaire.isAfterLastQuestion()){
			//Thank the user!
			TextView thankYou = new TextView(activity);
			thankYou.setText(R.string.survey_thank_you_message);
			thankYou.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f);
			questionContainer.addView(thankYou);
		}
		//enable / disable prev and next question buttons
		Button prevQButton = (Button) activity.findViewById(R.id.previous_question_button);
		prevQButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
		    	questionnaire.gotoPreviousQuestion();
		    	activity.updatePersonalNetworkViews();
			}
		});
		prevQButton.setEnabled(questionnaire.hasPreviousQuestion());
		Button nextQButton = (Button) activity.findViewById(R.id.next_question_button);
		nextQButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
		   		questionnaire.gotoNextQuestion();
		   		activity.updatePersonalNetworkViews();
			}
		});
		nextQButton.setEnabled(questionnaire.shouldEnableNextQuestionButton());
	}

	private void fillAnswerEditArea(LinearLayout questionContainer) {
		final InterviewManager questionnaire = InterviewManager.getInstance(activity);
		final Question question = questionnaire.getCurrentQuestion();
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		TextView title = new TextView(activity);
		title.setText(question.title());
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
		questionContainer.addView(title); 
		TextView text = new TextView(activity);
		String questionText = question.text();
		if(question.type() == InterviewManager.Q_ABOUT_ALTERS){
			String altername = questionnaire.getCurrentFirstAlterName();
			questionText = questionText.replace("$$", altername);
		}
		if(question.type() == InterviewManager.Q_ALTER_ALTER_TIES){
			String first = questionnaire.getCurrentFirstAlterName();
			String second = questionnaire.getCurrentSecondAlterName();
			questionText = questionText + "\n" + "Alter-alter pair: (" +
			first + ", " + second + ")";
		}		
		text.setText(questionText);
		text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
		questionContainer.addView(text);
		if(question.type() != InterviewManager.Q_NAME_GENERATOR){
			int answerType = question.answerType();
			if(answerType == InterviewManager.ANSWER_TYPE_TEXT || 
					answerType == InterviewManager.ANSWER_TYPE_NUMBER){
				EditText textAnswer = DynamicViewManager.createSurveyAnswerEditText(activity);
				if(answerType == InterviewManager.ANSWER_TYPE_TEXT){
					//TODO: choose alternatives
					//textAnswer.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
					textAnswer.setInputType(InputType.TYPE_CLASS_TEXT);
				}
				if(answerType == InterviewManager.ANSWER_TYPE_NUMBER){
					textAnswer.setInputType(InputType.TYPE_CLASS_NUMBER); 
				}
				String currentValue = null;
				if(question.type() == InterviewManager.Q_ABOUT_EGO)
					currentValue = network.getAttributeValueAt(System.currentTimeMillis(), question.title(),
							Ego.getInstance());
				if(question.type() == InterviewManager.Q_ABOUT_ALTERS)
					currentValue = network.getAttributeValueAt(System.currentTimeMillis(), question.title(),
							Alter.getInstance(questionnaire.getCurrentFirstAlterName()));;
				if(question.type() == InterviewManager.Q_ALTER_ALTER_TIES)
					currentValue = network.getAttributeValueAt(System.currentTimeMillis(), question.title(), 
							AlterAlterDyad.getInstance(questionnaire.getCurrentFirstAlterName(), 
							questionnaire.getCurrentSecondAlterName()));
				if(currentValue != null && !currentValue.equals(PersonalNetwork.VALUE_NOT_ASSIGNED))
					textAnswer.setText(currentValue);
				questionContainer.addView(textAnswer);
				if (textAnswer.requestFocus()) {
			        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			        inputManager.showSoftInput(textAnswer, InputMethodManager.SHOW_IMPLICIT);
			    }
			}
			if(answerType == InterviewManager.ANSWER_TYPE_CHOICE){
				String[] choices = question.answerChoiceTextValues();
				RadioGroup choicesGroup = new RadioGroup(activity);
				String currentValue = null;
				if(question.type() == InterviewManager.Q_ABOUT_EGO)
					currentValue = network.getAttributeValueAt(System.currentTimeMillis(), question.title(),
							Ego.getInstance());
				if(question.type() == InterviewManager.Q_ABOUT_ALTERS)
					currentValue = network.getAttributeValueAt(System.currentTimeMillis(), question.title(), 
							Alter.getInstance(questionnaire.getCurrentFirstAlterName()));
				if(question.type() == InterviewManager.Q_ALTER_ALTER_TIES)
					currentValue = network.getAttributeValueAt(System.currentTimeMillis(), question.title(), 
							AlterAlterDyad.getInstance(questionnaire.getCurrentFirstAlterName(), 
							questionnaire.getCurrentSecondAlterName()));
				for(int i = 0; i < choices.length; ++i){
					RadioButton button = new RadioButton(activity);
					button.setText(choices[i]);
					button.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							RadioButton clickedButton = (RadioButton) v;
							if(clickedButton.isChecked()){
								String value = clickedButton.getText().toString();
								if(question.type() == InterviewManager.Q_ABOUT_EGO){
									network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
											question.title(), Ego.getInstance(), value);
								}
								if(question.type() == InterviewManager.Q_ABOUT_ALTERS){
									network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
											question.title(), 
											Alter.getInstance(questionnaire.getCurrentFirstAlterName()), 
											value);
								}
								if(question.type() == InterviewManager.Q_ALTER_ALTER_TIES){
									//find out whether the value means adjacent or not
									boolean adjacent = false;
									Set<Integer> indicees = question.answerChoiceIndicees();
									for(Integer index : indicees){
										if(question.answerChoiceTextValue(index).equals(value)){
											adjacent = question.answerChoiceAdjacent(index);
										}
									}
									String a = questionnaire.getCurrentFirstAlterName();
									String b = questionnaire.getCurrentSecondAlterName();
									//in any case add the dyad attribute value (even if it means not adjacent)
									network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
											question.title(), 
											AlterAlterDyad.getInstance(a, b), value);
									if(!adjacent){
										network.removeTieAt(TimeInterval.getRightUnboundedFromNow(), a, b);
									} else {
										network.addToLifetimeOfTie(TimeInterval.getRightUnboundedFromNow(), a, b);
									}
								}
							}
						}
					});
					choicesGroup.addView(button);
					if(currentValue != null && currentValue.equals(choices[i])){
						button.setChecked(true);
					}
				}
				ScrollView choicesScrollView = new ScrollView(activity);
				choicesScrollView.addView(choicesGroup);
				questionContainer.addView(choicesScrollView);
			}
		} else { // name generator question
			LinearLayout addAlterLayout = new LinearLayout(activity);
			addAlterLayout.setOrientation(LinearLayout.HORIZONTAL);
			final EditText newAlterName = new EditText(activity);
			newAlterName.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); //don't make suggestions, since names are not necessarily in the dictionary
			newAlterName.setHint(R.string.new_alter_name_edit_text_hint);
			addAlterLayout.addView(newAlterName);
			Button addAlterButton = new Button(activity);
			addAlterButton.setText(R.string.add_alter_button_text);
			addAlterButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String alterName = newAlterName.getText().toString();
					if(alterName != null && alterName.length() > 0){
						network.addToLifetimeOfAlter(TimeInterval.getRightUnboundedFromNow(), alterName);
						questionnaire.addAlter(alterName);
						newAlterName.setText("");
						activity.updatePersonalNetworkViews();
					}
				}
			});
			addAlterLayout.addView(addAlterButton);
			questionContainer.addView(addAlterLayout);
			Button pickAlterButton = DynamicViewManager.createPickAlterButton(activity);
			pickAlterButton.setText(R.string.pick_alter_button_text);
			pickAlterButton.setOnClickListener(activity);
			questionContainer.addView(pickAlterButton);
			
			TextView altersView = new TextView(activity);
			ScrollView altersScrollView = new ScrollView(activity);
			String[] alterNames = questionnaire.getAlterNames();
			StringBuffer altersViewContent = new StringBuffer();
			if(alterNames != null){
				for(int i = 0; i < alterNames.length; ++i){
					altersViewContent.append(alterNames[i] + "\n");
				}
			}
			altersView.setText(altersViewContent.toString());
			altersScrollView.addView(altersView);
			questionContainer.addView(altersScrollView);
			if (newAlterName.requestFocus()) {
		        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		        inputManager.showSoftInput(newAlterName, InputMethodManager.SHOW_IMPLICIT);
		    }
		}
	}
	

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        try {
        	activity = (SCCMainActivity) act;
        } catch (ClassCastException e) {
            throw new ClassCastException(act.toString()
                    + " must implement PersonalNetworkMainActivity");
        }
    }

}
