package net.egosmart.scc.gui;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EgoControlViewFragment extends Fragment {
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.ego_control_view, container, false);
		return view;
    }

	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		SCCMainActivity activity = (SCCMainActivity) getActivity();
		TextView egoNameTextView = (TextView) activity.findViewById(R.id.ego_name_text);
		if(egoNameTextView == null)
			return;
	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
    }

	
}
