/**
 * 
 */
package net.egosmart.scc.gui.util;

import java.util.ArrayList;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.R;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * @author juergen
 *
 */
public class NonNeighborListBaseAdapter extends BaseAdapter {

	private ArrayList<Pair<String, String>> list;
	
	private SCCMainActivity activity;
	
	public NonNeighborListBaseAdapter(SCCMainActivity activity, ArrayList<Pair<String, String>> alterNameAttrInfoList){
		this.activity = activity;
		list = alterNameAttrInfoList;
	}
	
	/* (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() {
		return list.size();
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) {
		return position;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        Pair<TextView, TextView> viewPair;
        if (convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(R.layout.movable_non_neighbor_list_single_row_view, null);
            viewPair = new Pair<TextView, TextView>(
            		(TextView) convertView.findViewById(R.id.movable_non_neighbor_list_single_row_field_name),
            		(TextView) convertView.findViewById(R.id.movable_non_neighbor_list_single_row_field_attribute));
            convertView.setTag(viewPair);
        } else {
            viewPair = (Pair<TextView, TextView>) convertView.getTag();
        }
        viewPair.first.setText(list.get(position).first);
        String attrDesc = list.get(position).second;
        if(attrDesc.length() > 70)
        	attrDesc = attrDesc.substring(0, 70) + " ...";
        viewPair.second.setText(attrDesc);
        return convertView;
	}
}
