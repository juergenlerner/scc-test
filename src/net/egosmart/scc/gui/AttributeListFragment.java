/**
 * 
 */
package net.egosmart.scc.gui;

import java.util.HashSet;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.SCCProperties;
import net.egosmart.scc.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author juergen
 *
 */
public class AttributeListFragment extends Fragment {

	private SCCMainActivity activity;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.attribute_list_view, container, false);
	}

	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		final SCCProperties properties = SCCProperties.getInstance(activity);
		final PersonalNetwork network = PersonalNetwork.getInstance(activity);
		LinearLayout egoAttributesList = (LinearLayout) activity.findViewById(R.id.attribute_list_ego_attributes);
		if(egoAttributesList == null)
			return;
		LinearLayout alterAttributesList = (LinearLayout) activity.findViewById(R.id.attribute_list_alter_attributes);
		LinearLayout egoAlterAttributesList = (LinearLayout) activity.findViewById(R.id.attribute_list_ego_alter_attributes);
		LinearLayout alterAlterAttributesList = (LinearLayout) activity.findViewById(R.id.attribute_list_alter_alter_attributes);
		// clear the lists in any case
		egoAttributesList.removeAllViews();
		alterAttributesList.removeAllViews();
		egoAlterAttributesList.removeAllViews();
		alterAlterAttributesList.removeAllViews();
		//EGO ATTRIBUTES
		//configure expand ego attributes button
		ImageButton expandEgoAttributesButton = (ImageButton) activity.
				findViewById(R.id.attribute_list_expand_ego_attrib_button);
		if(properties.getPropertyAttributeListExpandEgoAttributes()){
			expandEgoAttributesButton.setImageResource(R.drawable.ic_button_shrink);
			expandEgoAttributesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAttributeListExpandEgoAttributes(false);
					activity.updatePersonalNetworkViews();
				}
			});
			//create and fill the list
			HashSet<String> attrNames = network.getAttributeNames(PersonalNetwork.DOMAIN_EGO);
			for(final String attrName : attrNames){
				View row = getAttributeDescriptionListItemView(attrName, 
						network.getAttributeDescription(PersonalNetwork.DOMAIN_EGO, attrName));
				egoAttributesList.addView(row);
				row.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						network.setSelectedAttribute(PersonalNetwork.DOMAIN_EGO, attrName);
						network.setSelectedAttributeDomain(PersonalNetwork.DOMAIN_EGO);
						SCCProperties.getInstance(activity).setPropertyShowDetailInSinglePaneView(true);
						activity.switchToAttributeView();
					}
				});
			}
		} else { // do not expand ego attributes
			expandEgoAttributesButton.setImageResource(R.drawable.ic_button_expand);
			expandEgoAttributesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAttributeListExpandEgoAttributes(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not show ego attributes
		}
		//ALTER ATTRIBUTES
		//configure expand alter attributes button
		ImageButton expandAlterAttributesButton = (ImageButton) activity.
				findViewById(R.id.attribute_list_expand_alter_attrib_button);
		if(properties.getPropertyAttributeListExpandAlterAttributes()){
			expandAlterAttributesButton.setImageResource(R.drawable.ic_button_shrink);
			expandAlterAttributesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAttributeListExpandAlterAttributes(false);
					activity.updatePersonalNetworkViews();
				}
			});
			//create and fill the list
			HashSet<String> attrNames = network.getAttributeNames(PersonalNetwork.DOMAIN_ALTER);
			for(final String attrName : attrNames){
				View row = getAttributeDescriptionListItemView(attrName, 
						network.getAttributeDescription(PersonalNetwork.DOMAIN_ALTER, attrName));
				alterAttributesList.addView(row);
				row.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						network.setSelectedAttribute(PersonalNetwork.DOMAIN_ALTER, attrName);
						network.setSelectedAttributeDomain(PersonalNetwork.DOMAIN_ALTER);
						SCCProperties.getInstance(activity).setPropertyShowDetailInSinglePaneView(true);
						activity.switchToAttributeView();
					}
				});
			}
		} else { // do not expand alter attributes
			expandAlterAttributesButton.setImageResource(R.drawable.ic_button_expand);
			expandAlterAttributesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAttributeListExpandAlterAttributes(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not show alter attributes
		}
		//EGO_ALTER ATTRIBUTES
		//configure expand ego-alter attributes button
		ImageButton expandEgoAlterAttributesButton = (ImageButton) activity.
				findViewById(R.id.attribute_list_expand_ego_alter_attrib_button);
		if(properties.getPropertyAttributeListExpandEgoAlterAttributes()){
			expandEgoAlterAttributesButton.setImageResource(R.drawable.ic_button_shrink);
			expandEgoAlterAttributesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAttributeListExpandEgoAlterAttributes(false);
					activity.updatePersonalNetworkViews();
				}
			});
			//create and fill the list
			HashSet<String> attrNames = network.getAttributeNames(PersonalNetwork.DOMAIN_EGO_ALTER);
			for(final String attrName : attrNames){
				View row = getAttributeDirectionDescriptionListItemView(attrName,
						network.getAttributeDirectionType(PersonalNetwork.DOMAIN_EGO_ALTER, attrName),
						network.getAttributeDescription(PersonalNetwork.DOMAIN_EGO_ALTER, attrName));
				egoAlterAttributesList.addView(row);
				row.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						network.setSelectedAttribute(PersonalNetwork.DOMAIN_EGO_ALTER, attrName);
						network.setSelectedAttributeDomain(PersonalNetwork.DOMAIN_EGO_ALTER);
						SCCProperties.getInstance(activity).setPropertyShowDetailInSinglePaneView(true);
						activity.switchToAttributeView();
					}
				});
			}
		} else { // do not expand ego-alter attributes
			expandEgoAlterAttributesButton.setImageResource(R.drawable.ic_button_expand);
			expandEgoAlterAttributesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAttributeListExpandEgoAlterAttributes(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not show ego-alter attributes
		}
		//ALTER_ALTER ATTRIBUTES
		//configure expand alter-alter attributes button
		ImageButton expandAlterAlterAttributesButton = (ImageButton) activity.
				findViewById(R.id.attribute_list_expand_alter_alter_attrib_button);
		if(properties.getPropertyAttributeListExpandAlterAlterAttributes()){
			expandAlterAlterAttributesButton.setImageResource(R.drawable.ic_button_shrink);
			expandAlterAlterAttributesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAttributeListExpandAlterAlterAttributes(false);
					activity.updatePersonalNetworkViews();
				}
			});
			//create and fill the list
			HashSet<String> attrNames = network.getAttributeNames(PersonalNetwork.DOMAIN_ALTER_ALTER);
			for(final String attrName : attrNames){
				View row = getAttributeDirectionDescriptionListItemView(attrName,
						network.getAttributeDirectionType(PersonalNetwork.DOMAIN_ALTER_ALTER, attrName),
						network.getAttributeDescription(PersonalNetwork.DOMAIN_ALTER_ALTER, attrName));
				alterAlterAttributesList.addView(row);
				row.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						network.setSelectedAttribute(PersonalNetwork.DOMAIN_ALTER_ALTER, attrName);
						network.setSelectedAttributeDomain(PersonalNetwork.DOMAIN_ALTER_ALTER);
						SCCProperties.getInstance(activity).setPropertyShowDetailInSinglePaneView(true);
						activity.switchToAttributeView();
					}
				});
			}
		} else { // do not expand alter-alter attributes
			expandAlterAlterAttributesButton.setImageResource(R.drawable.ic_button_expand);
			expandAlterAlterAttributesButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					properties.setPropertyAttributeListExpandAlterAlterAttributes(true);
					activity.updatePersonalNetworkViews();
				}
			});
			// do not show alter-alter attributes
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity is the right class. If not, it throws an exception
		try {
			this.activity = (SCCMainActivity) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must be instance of SCCMainActivity");
		}
	}

	private View getAttributeDescriptionListItemView(String attributeName, String description) {
		View item = LayoutInflater.from(activity).inflate(R.layout.clickable_attribute_description_list_item_view, null);
		TextView nameView = (TextView) item.findViewById(R.id.clickable_attribute_description_list_item_field_attrname);
		TextView descView = (TextView) item.findViewById(R.id.clickable_attribute_description_list_item_field_desc);
		nameView.setText(attributeName);
		//shorten the description if too long
		String desc = description;
		if(desc.length() > 60){
			desc = desc.substring(0, 60);
			desc = desc + " ...";
		}
		descView.setText(desc);
		return item;
	}

	private View getAttributeDirectionDescriptionListItemView(String attributeName, String direction, String description) {
		View item = LayoutInflater.from(activity).inflate(R.layout.clickable_attribute_direction_description_list_item_view, null);
		TextView nameView = (TextView) item.findViewById(R.id.clickable_attribute_direction_description_list_item_field_attrname);
		TextView dirView = (TextView) item.findViewById(R.id.clickable_attribute_direction_description_list_item_field_direction);
		TextView descView = (TextView) item.findViewById(R.id.clickable_attribute_direction_description_list_item_field_desc);
		nameView.setText(attributeName);
		//get the direction text
        if(direction.equals(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC)){
        	dirView.setText("(" + activity.getString(R.string.direction_text_symmetric) + ")");
        }		
        if(direction.equals(PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC)){
        	dirView.setText("(" + activity.getString(R.string.direction_text_asymmetric) + ")");
        }		
        if(direction.equals(PersonalNetwork.DYAD_DIRECTION_OUT)){
        	dirView.setText("(" + activity.getString(R.string.direction_text_ego_to_alter) + ")");
        }		
        if(direction.equals(PersonalNetwork.DYAD_DIRECTION_IN)){
        	dirView.setText("(" + activity.getString(R.string.direction_text_alter_to_ego) + ")");
        }		
        //shorten the description if too long
		String desc = description;
		if(desc.length() > 60){
			desc = desc.substring(0, 60);
			desc = desc + " ...";
		}
		descView.setText(desc);
		return item;
	}

}
