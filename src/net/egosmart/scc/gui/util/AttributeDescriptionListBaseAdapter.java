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
 * List adapter for list items that show an attribute name (big, bold, blue) and an attribute description in the 
 * line below (dark-gray and smaller).
 *  
 * @author juergen
 *
 */
public class AttributeDescriptionListBaseAdapter extends BaseAdapter {

	private static ArrayList<Pair<String, String>> list;
	
	private SCCMainActivity activity;
	
	public AttributeDescriptionListBaseAdapter(SCCMainActivity activity, ArrayList<Pair<String, String>> attrNameDescList){
		this.activity = activity;
		list = attrNameDescList;
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
            convertView = LayoutInflater.from(activity).inflate(R.layout.attribute_description_list_item_view, null);
            viewPair = new Pair<TextView, TextView>((TextView) convertView.
            		findViewById(R.id.attribute_description_list_item_field_attrname), (TextView) convertView.
            		findViewById(R.id.attribute_description_list_item_field_desc));
            convertView.setTag(viewPair);
        } else {
            viewPair = (Pair<TextView, TextView>) convertView.getTag();
        }
        viewPair.first.setText(list.get(position).first);
        //shorten the description if too long
        String desc = list.get(position).second;
        if(desc.length() > 160){
        	desc = desc.substring(0, 160);
        	desc = desc + " ...";
        }
        viewPair.second.setText(desc);
        return convertView;
	}

}
