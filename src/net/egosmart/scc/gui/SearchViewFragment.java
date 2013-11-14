package net.egosmart.scc.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.gui.util.AlterListBaseAdapter;
import net.egosmart.scc.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class SearchViewFragment extends Fragment {

	private SCCMainActivity activity;
	private LinkedHashSet<String> alters;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.search_view, container, false);
	}
	
	public void setAlterList(LinkedHashSet<String> alters){
		this.alters = alters;
		//updateView();
	}

	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		activity = (SCCMainActivity) getActivity();
		ListView alterList = (ListView) activity.findViewById(R.id.search_view_alter_list);
		if(alterList == null)
			return;
		if(alters != null){
			PersonalNetwork network = PersonalNetwork.getInstance(activity);
			String[] alterNames = alters.toArray(new String[]{});
			ArrayList<Pair<String, String>> nameAttrPairs = new ArrayList<Pair<String,String>>();
			for(int i = 0; i < alterNames.length; ++i){
				HashMap<String, String> attrValues = network.getValuesOfAllAttributesForElementAt(
						System.currentTimeMillis(),
						Alter.getInstance(alterNames[i]));
				StringBuffer valueStr = new StringBuffer("| ");
				for(String attrName : attrValues.keySet()){
					String value = attrValues.get(attrName);
					if(!PersonalNetwork.VALUE_NOT_ASSIGNED.equals(value)){
						valueStr.append(value).append(" | ");
					}
				}
				nameAttrPairs.add(new Pair<String, String>(alterNames[i], valueStr.toString()));
			}
			ListAdapter adapter = new AlterListBaseAdapter(activity, nameAttrPairs);
			alterList.setAdapter(adapter);
			alterList.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> parent, View v, int position,
						long id) {
					Pair<String, String> pair = (Pair<String, String>) parent.getItemAtPosition(position);
					activity.onAlterSelected(pair.first);
				}
			});
		}
	}

}
