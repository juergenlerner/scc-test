/**
 * 
 */
package net.egosmart.scc.gui;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.collect.Questionnaire;
import net.egosmart.scc.data.SCCProperties;
import net.egosmart.scc.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author juergen
 *
 */
public class SurveyControlFragment extends Fragment {
	
	private SCCMainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.survey_control_view, container, false);
    }
    
	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		TextView surveyControlHeadline = (TextView) activity.
				findViewById(R.id.survey_control_headline_textview);
		if(surveyControlHeadline == null)
			return;
		surveyControlHeadline.setText(activity.getString(R.string.survey_control_headline) + 
				" " + Questionnaire.getInstance(activity).getName());
		
		Button startSurveyButton = (Button) activity.findViewById(R.id.goto_survey_button);
		startSurveyButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SCCProperties.getInstance(activity).setPropertyShowDetailInSinglePaneView(true);
				activity.switchToSurveyView();
			}
		});
	}

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        
        try {
        	activity = (SCCMainActivity) act;
        } catch (ClassCastException e) {
            throw new ClassCastException(act.toString()
                    + " must implement SCCMainActivity");
        }
    }


}
