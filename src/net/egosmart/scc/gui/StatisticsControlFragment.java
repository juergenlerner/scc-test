/**
 * 
 */
package net.egosmart.scc.gui;

import net.egosmart.scc.R;
import net.egosmart.scc.SCCMainActivity;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * @author juergen
 *
 */
public class StatisticsControlFragment extends Fragment {
	
	private SCCMainActivity activity;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.statistics_control_view, container, false);
    }
    
	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		activity = (SCCMainActivity) getActivity();
		TextView headline = (TextView) activity.findViewById(R.id.statistics_control_headline_textview);
		if(headline == null)
			return;
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
