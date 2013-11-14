package net.egosmart.scc.gui;

import net.egosmart.scc.SCCMainActivity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NetworkViewFragment extends Fragment {

	private static NetworkView networkView;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		networkView =  new NetworkView((SCCMainActivity) getActivity());
		return networkView;
    }

	public void updateView() {
		
	}

}
