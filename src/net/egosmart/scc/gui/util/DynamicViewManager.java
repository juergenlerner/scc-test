package net.egosmart.scc.gui.util;

import net.egosmart.scc.SCCMainActivity;
import android.widget.Button;
import android.widget.EditText;


/**
 * Creates and gives access to view objects that are dynamically generated (rather than defined in XML files).
 * 
 * @author juergen
 *
 */
public class DynamicViewManager {

	private static EditText surveyAnswerEditText;
	private static Button pickAlterButton;
	
	public static EditText createSurveyAnswerEditText(SCCMainActivity activity){
		surveyAnswerEditText = new EditText(activity);
		return surveyAnswerEditText;
	}
	
	public static EditText getSurveyAnswerEditText(){
		return surveyAnswerEditText;
	}
	
	public static Button createPickAlterButton(SCCMainActivity activity){
		pickAlterButton = new Button(activity);
		return pickAlterButton;
	}
	
	public static Button getPickAlterButton(){
		return pickAlterButton;
	}
	
	
}
