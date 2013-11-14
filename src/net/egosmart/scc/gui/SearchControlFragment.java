/**
 * 
 */
package net.egosmart.scc.gui;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author juergen
 *
 */
public class SearchControlFragment extends Fragment {
	
	private SCCMainActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.search_control_view, container, false);
    }
    
	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		/*
		TextView networkControlHeadline = (TextView) activity.
				findViewById(R.id.network_control_headline_textview);
		if(networkControlHeadline == null)
			return;
			*/
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
