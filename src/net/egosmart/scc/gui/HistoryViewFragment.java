package net.egosmart.scc.gui;

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.Lifetime;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.SCCProperties;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.data.TimeVaryingAttributeValues;
import net.egosmart.scc.gui.util.SimpleLifetimeView;
import net.egosmart.scc.gui.util.SimpleTimeVaryingAttributeView;
import net.egosmart.scc.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HistoryViewFragment extends Fragment {

	private SCCMainActivity activity;

	private DateFormat dateFormat;
	private DateFormat timeFormat;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.history_view, container, false);
	}

	public void onStart(){
		super.onStart();
		updateView();
	}

	public void updateView() {
		activity = (SCCMainActivity) getActivity();
		dateFormat =  android.text.format.DateFormat.getLongDateFormat(activity);
		timeFormat =  android.text.format.DateFormat.getTimeFormat(activity);
		FrameLayout container = (FrameLayout) activity.findViewById(R.id.history_view_container); 
		container.removeAllViews();
		final SCCProperties properties = SCCProperties.getInstance(activity);
		String lastTopLevelViewLabel = properties.getPropertyLastTopLevelViewLabel();
		if(SCCMainActivity.LAST_VIEW_LABEL_ALTER.equals(lastTopLevelViewLabel)){
			if(properties.getPropertyHistoryViewShowAllAlters()){
				LinearLayout rootView = (LinearLayout) LayoutInflater.from(activity).
						inflate(R.layout.history_view_show_all_alters, null);
				container.addView(rootView);
				LinearLayout alterList = (LinearLayout) rootView.findViewById(R.id.history_view_all_alters_alter_list);
				if(alterList == null)
					return;
				alterList.removeAllViews();
				final PersonalNetwork history = PersonalNetwork.getInstance(activity);
				LinkedHashSet<String> alters = history.getAltersAt(TimeInterval.getMaxInterval());
				LinkedHashMap<String, Lifetime> alter2lifetime = new LinkedHashMap<String, Lifetime>();
				long firstStartTime = Long.MAX_VALUE;
				long lastEndTime = Long.MIN_VALUE;
				long currentTime = System.currentTimeMillis();

				for(String alter: alters){
					Lifetime lifetime = history.getLifetimeOfAlter(alter);
					if(!lifetime.isEmpty()){
						alter2lifetime.put(alter, lifetime);
						TimeInterval first = lifetime.getFirstTimeInterval();
						TimeInterval last = lifetime.getLastTimeInterval();
						if(first.getStartTime() < firstStartTime)
							firstStartTime = first.getStartTime();
						long realEndTime = Math.min(currentTime, last.getEndTime());//don't use MAX_VALUE as an endtime
						if(realEndTime > lastEndTime)
							lastEndTime = realEndTime;
					}
				}
				if(lastEndTime <= firstStartTime)//either there is no alter or all have empty lifetimes or lifetimes that are points
					return;

				Date firstStartDate = new Date(firstStartTime);
				Date lastEndDate = new Date(lastEndTime);
				String startText = dateFormat.format(firstStartDate);// + " " + timeFormat.format(firstStartdate); 
				TextView startView = (TextView) rootView.findViewById(R.id.history_view_all_alters_field_start_time);
				startView.setText(startText);
				String endText = dateFormat.format(lastEndDate);// + " " + timeFormat.format(lastEnddate); 
				TextView endView = (TextView) rootView.findViewById(R.id.history_view_all_alters_field_end_time);
				endView.setText(endText);

				for(final String alter : alter2lifetime.keySet()){
					Lifetime lifetime = alter2lifetime.get(alter);
					View alterLifetimeView = LayoutInflater.from(activity).inflate(R.layout.altername_lifetime_view, null);
					TextView alterNameView = (TextView) alterLifetimeView.findViewById(R.id.altername_lifetime_field_altername);
					alterNameView.setText(alter);
					FrameLayout lifetimeContainer = (FrameLayout) alterLifetimeView.
							findViewById(R.id.altername_lifetime_field_lifetime_container);
					View lifetimeView = new SimpleLifetimeView(activity, lifetime, firstStartTime, lastEndTime);
					lifetimeContainer.addView(lifetimeView);
					alterLifetimeView.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							properties.setPropertyHistoryViewShowAllAlters(false);
							history.setSelectedAlterInHistory(alter);
							PersonalNetwork network = PersonalNetwork.getInstance(activity);
							// select this alter also in the personal network (if it is still there)
							if(network.hasAlterAt(TimeInterval.getCurrentTimePoint(), alter))
								network.setSelectedAlter(alter);
							updateView();
						}
					});
					alterList.addView(alterLifetimeView);
				}
			} else { //show detailed history of a selected alter
				PersonalNetwork history = PersonalNetwork.getInstance(activity);
				String selectedAlter = history.getSelectedAlterInHistory();
				LinearLayout rootView = (LinearLayout) LayoutInflater.from(activity).
						inflate(R.layout.history_view_show_selected_alter, null);
				container.addView(rootView);
				ImageButton upButton = (ImageButton) rootView.
						findViewById(R.id.history_view_show_selected_alter_up_to_all_alters_button);
				upButton.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						properties.setPropertyHistoryViewShowAllAlters(true);
						updateView();
					}
				});
				if(selectedAlter == null)
					return;
				long currentTime = System.currentTimeMillis();
				long firstStartTime = Long.MAX_VALUE;
				long lastEndTime = Long.MIN_VALUE;
				Lifetime lifetime = history.getLifetimeOfAlter(selectedAlter);
				if(!lifetime.isEmpty()){
					TimeInterval first = lifetime.getFirstTimeInterval();
					firstStartTime = first.getStartTime();
					TimeInterval last = lifetime.getLastTimeInterval();
					lastEndTime = Math.min(currentTime, last.getEndTime());//don't use MAX_VALUE as an endtime
				} 
				if(lastEndTime <= firstStartTime)//either lifetime is empty or a time point 
					return;
				//show start and end date
				Date firstStartDate = new Date(firstStartTime);
				Date lastEndDate = new Date(lastEndTime);
				String startText = dateFormat.format(firstStartDate);// + " " + timeFormat.format(firstStartdate); 
				TextView startView = (TextView) rootView.findViewById(R.id.history_view_selected_alter_field_start_time);
				startView.setText(startText);
				String endText = dateFormat.format(lastEndDate);// + " " + timeFormat.format(lastEnddate); 
				TextView endView = (TextView) rootView.findViewById(R.id.history_view_selected_alter_field_end_time);
				endView.setText(endText);
				TextView alterNameView = (TextView) rootView.
						findViewById(R.id.history_view_selected_alter_field_altername);
				alterNameView.setText(selectedAlter);
				//show lifetime
				FrameLayout lifetimeContainer = (FrameLayout) rootView.
						findViewById(R.id.history_view_selected_alter_field_lifetime_container);
				View lifetimeView = new SimpleLifetimeView(activity, lifetime, firstStartTime, lastEndTime);
				lifetimeContainer.addView(lifetimeView);
				//show memo times
				FrameLayout memoTimesContainer = (FrameLayout) rootView.
						findViewById(R.id.history_view_selected_alter_field_memotimes_container);
				View memoTimesView = new SimpleTimeVaryingAttributeView(activity, 
						history.getAttributeValues(PersonalNetwork.getAlterMemosAttributeName(), 
								Alter.getInstance(selectedAlter)), 
								firstStartTime, 
								lastEndTime);
				memoTimesContainer.addView(memoTimesView);
				//show attribute times 
			    LinearLayout attributeTimesContainer = (LinearLayout) rootView.
						findViewById(R.id.history_view_selected_alter_field_attribute_times_container);
				LinkedHashMap<String, TimeVaryingAttributeValues> allValues = 
						history.getValuesOfAllAttributesForElement(Alter.getInstance(selectedAlter));
				for(String attrName : allValues.keySet()){
					TimeVaryingAttributeValues values = allValues.get(attrName);
					if(!values.isEmpty()){
						View attrLifetimeView = LayoutInflater.from(activity).
								inflate(R.layout.attributename_time_varying_values_view, null);
						TextView attrNameView = (TextView) attrLifetimeView.
								findViewById(R.id.attributename_time_varying_field_attrname);
						attrNameView.setText(attrName);
						FrameLayout valuesContainer = (FrameLayout) attrLifetimeView.
								findViewById(R.id.attributename_time_varying_field_values_container);
						View valuesView = new SimpleTimeVaryingAttributeView(activity, values, 
								firstStartTime, lastEndTime);
						valuesContainer.addView(valuesView);
						attributeTimesContainer.addView(attrLifetimeView);
					}
				}
				//TODO: further information ...
			}
		}
		if(SCCMainActivity.LAST_VIEW_LABEL_ATTRIBUTE.equals(lastTopLevelViewLabel)){
			//TODO
		}
		if(SCCMainActivity.LAST_VIEW_LABEL_EGO.equals(lastTopLevelViewLabel)){
			// TODO
		}

	}

}
