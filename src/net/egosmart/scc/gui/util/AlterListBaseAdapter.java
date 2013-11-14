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
public class AlterListBaseAdapter extends BaseAdapter {

	private ArrayList<Pair<String, String>> list;
	
	private SCCMainActivity activity;
	
	public AlterListBaseAdapter(SCCMainActivity activity, ArrayList<Pair<String, String>> alterNameAttrInfoList){
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
            convertView = LayoutInflater.from(activity).inflate(R.layout.alter_list_single_row_view, null);
            viewPair = new Pair<TextView, TextView>(
            		(TextView) convertView.findViewById(R.id.alter_list_single_row_field_name),
            		(TextView) convertView.findViewById(R.id.alter_list_single_row_field_attribute));
            convertView.setTag(viewPair);
        } else {
            viewPair = (Pair<TextView, TextView>) convertView.getTag();
        }
        viewPair.first.setText(list.get(position).first);
        String attrDesc = list.get(position).second;
        if(attrDesc.length() > 80)
        	attrDesc = attrDesc.substring(0, 80) + "...";
        viewPair.second.setText(attrDesc);
        return convertView;
	}
}
