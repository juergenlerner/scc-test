package net.egosmart.scc.gui;

import java.util.ArrayList;
import java.util.HashMap;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.gui.util.AlterListBaseAdapter;
import net.egosmart.scc.gui.util.OnAlterSelectedListener;
import net.egosmart.scc.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

public class AlterListViewFragment extends Fragment {
	
	private OnAlterSelectedListener onAlterSelectedListener;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.alter_list_view, container, false);
		return view;
    }

	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		SCCMainActivity activity = (SCCMainActivity) getActivity();
		PersonalNetwork network = PersonalNetwork.getInstance(activity);
		String[] alterNames = network.getAltersAt(TimeInterval.getCurrentTimePoint()).
				toArray(new String[]{});
		ArrayList<Pair<String, String>> nameAttrPairs = new ArrayList<Pair<String,String>>();
		for(int i = 0; i < alterNames.length; ++i){
			HashMap<String, String> attrValues = network.getValuesOfAllAttributesForElementAt(
					System.currentTimeMillis(), Alter.getInstance(alterNames[i]));
			StringBuffer valueStr = new StringBuffer("| ");
			for(String attrName : attrValues.keySet()){
				String value = attrValues.get(attrName);
				if(!PersonalNetwork.VALUE_NOT_ASSIGNED.equals(value)){
					valueStr
					//.append(attrName).append(':')
					.append(value).append(" | ");
				}
			}
			nameAttrPairs.add(new Pair<String, String>(alterNames[i], valueStr.toString()));
		}
		ListAdapter adapter = new AlterListBaseAdapter(activity, nameAttrPairs);
		//ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, alterNames);
	
		ListView listview = (ListView) getActivity().findViewById(R.id.list_view_alter_names);
		if(listview == null)
			return;
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				Pair<String, String> pair = (Pair<String, String>) parent.getItemAtPosition(position);
				onAlterSelectedListener.onAlterSelected(pair.first);
			}
		});
	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
        	onAlterSelectedListener = (OnAlterSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnAlterSelectedListener");
        }
    }

	
}
