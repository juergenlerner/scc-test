/**
 * 
 */
package net.egosmart.scc.gui.util;

import java.util.ArrayList;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.R;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * List adapter for list items that show an attribute name (big, bold, blue), an attribute
 * direction (black, bold, big),
 * and an attribute description in the 
 * line below (dark-gray and smaller).
 *  
 * @author juergen
 *
 */
public class EgoAlterAttributeDescriptionListBaseAdapter extends BaseAdapter {

	private static ArrayList<Pair<Pair<String, String>, String>> list;
	
	private SCCMainActivity activity;
	
	public EgoAlterAttributeDescriptionListBaseAdapter(SCCMainActivity activity, 
			ArrayList<Pair<Pair<String, String>,String>> attrNameDirDescList){
		this.activity = activity;
		list = attrNameDirDescList;
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
		NameDirectionDescriptionViews viewTriplet;
        if (convertView == null) {
            convertView = LayoutInflater.from(activity).
            		inflate(R.layout.attribute_direction_description_list_item_view, null);
            viewTriplet = new NameDirectionDescriptionViews();
            viewTriplet.name = (TextView) convertView.
            		findViewById(R.id.attribute_direction_description_list_item_field_attrname);
            viewTriplet.direction = (TextView) convertView.
            		findViewById(R.id.attribute_direction_description_list_item_field_direction);
            viewTriplet.description = (TextView) convertView.
            		findViewById(R.id.attribute_direction_description_list_item_field_desc);
            convertView.setTag(viewTriplet);
        } else {
            viewTriplet = (NameDirectionDescriptionViews) convertView.getTag();
        }
        String attrName = list.get(position).first.first;
        viewTriplet.name.setText(attrName);
        String directionText;
        String dirToken = list.get(position).first.second;
        if(PersonalNetwork.getInstance(activity).getAttributeDirectionType(PersonalNetwork.DOMAIN_EGO_ALTER, attrName)
        		.equals(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC)){
        	directionText = "(" + activity.getString(R.string.direction_text_symmetric) + ")";
        } else {
        	if(PersonalNetwork.DYAD_DIRECTION_OUT.equals(dirToken)){
        		directionText = "(" + activity.getString(R.string.direction_text_ego_to_alter) + ")";
        	} else {
        		directionText = "(" + activity.getString(R.string.direction_text_alter_to_ego) + ")";
        	}
        }
        viewTriplet.direction.setText(directionText);
        //shorten the description if too long
        String desc = list.get(position).second;
        if(desc.length() > 160){
        	desc = desc.substring(0, 160);
        	desc = desc + " ...";
        }
        viewTriplet.description.setText(desc);
        return convertView;
	}

	static class NameDirectionDescriptionViews{
		TextView name;
		TextView direction;
		TextView description;
	}
	
}
