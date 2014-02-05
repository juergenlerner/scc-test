package net.egosmart.scc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;

import net.egosmart.scc.collect.EgonetQuestionnaireFile;
import net.egosmart.scc.collect.ImportContentProviderData;
import net.egosmart.scc.collect.Questionnaire;
import net.egosmart.scc.data.LayoutSQLite;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.SCCProperties;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.gui.AlterDetailViewFragment;
import net.egosmart.scc.gui.AlterListViewFragment;
import net.egosmart.scc.gui.AttributeDetailViewFragment;
import net.egosmart.scc.gui.AttributeListFragment;
import net.egosmart.scc.gui.EgoControlViewFragment;
import net.egosmart.scc.gui.EgoDetailViewFragment;
import net.egosmart.scc.gui.HistoryControlFragment;
import net.egosmart.scc.gui.HistoryViewFragment;
import net.egosmart.scc.gui.NetworkControlFragment;
import net.egosmart.scc.gui.NetworkViewFragment;
import net.egosmart.scc.gui.SearchControlFragment;
import net.egosmart.scc.gui.SearchViewFragment;
import net.egosmart.scc.gui.StatisticsControlFragment;
import net.egosmart.scc.gui.StatisticsViewDensityFragment;
import net.egosmart.scc.gui.StatisticsViewGenderFragment;
import net.egosmart.scc.gui.SurveyControlFragment;
import net.egosmart.scc.gui.SurveyFragment;
import net.egosmart.scc.gui.dialog.AddAlterAlterContactEventDialog;
import net.egosmart.scc.gui.dialog.AddAlterAlterEventDialog;
import net.egosmart.scc.gui.dialog.AddAlterAlterMemoDialog;
import net.egosmart.scc.gui.dialog.AddAlterDialog;
import net.egosmart.scc.gui.dialog.AddAlterEventDialog;
import net.egosmart.scc.gui.dialog.AddAlterMemoDialog;
import net.egosmart.scc.gui.dialog.AddAttributeDialog;
import net.egosmart.scc.gui.dialog.AddEgoAlterContactEventDialog;
import net.egosmart.scc.gui.dialog.AddEgoMemoDialog;
import net.egosmart.scc.gui.dialog.ChangeSettingsDialog;
import net.egosmart.scc.gui.dialog.ChoseAlterOperationDialog;
import net.egosmart.scc.gui.dialog.ConfirmRemoveAlterDialog;
import net.egosmart.scc.gui.dialog.ConfirmRemoveAttributeDialog;
import net.egosmart.scc.gui.dialog.EditAlterAlterAttributesDialog;
import net.egosmart.scc.gui.dialog.EditAlterAlterTiesDialog;
import net.egosmart.scc.gui.dialog.EditAlterAttributesDialog;
import net.egosmart.scc.gui.dialog.EditAttributeStructureDialog;
import net.egosmart.scc.gui.dialog.EditEgoAlterAttributesDialog;
import net.egosmart.scc.gui.dialog.EditEgoAttributesDialog;
import net.egosmart.scc.gui.dialog.InterviewMeDialog;
import net.egosmart.scc.gui.dialog.RenameAlterDialog;
import net.egosmart.scc.gui.dialog.SelectHistoryOutputGraphMLFileDialog;
import net.egosmart.scc.gui.dialog.SelectInputEgoFileDialog;
import net.egosmart.scc.gui.dialog.SelectInputGraphMLFileDialog;
import net.egosmart.scc.gui.dialog.SelectInputIntFileDialog;
import net.egosmart.scc.gui.dialog.SelectOutputGraphMLFileDialog;
import net.egosmart.scc.gui.dialog.SuggestAltersDialog;
import net.egosmart.scc.gui.util.DynamicViewManager;
import net.egosmart.scc.gui.util.OnAlterSelectedListener;
import net.egosmart.scc.gui.util.OnNeighborSelectedListener;


import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.SearchManager;
import android.net.Uri;
import android.os.Bundle;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SearchView;

/**
 * Main activity handling the top-level hierarchy of the user interface and several
 * call-back methods.
 * 
 * The layout of the GUI differs for large screens and smaller screens. For large screens there
 * are generally two major areas: a smaller one (the list view) for displaying a list of elements 
 * and/or some control possibilities and a larger one (the detail view) for displaying details of 
 * the selected element or a network map. 
 * 
 * For smaller screens: show just either the list view or the detail view (yet to be implemented).
 *  
 *  A spinner in the action bar allows to switch between the various types of objects: 
 *  contacts, labels, ego. 
 *  
 *  
 * 
 * @author juergen
 *
 */
public class SCCMainActivity extends FragmentActivity implements
ActionBar.OnNavigationListener, OnAlterSelectedListener, OnNeighborSelectedListener, 
android.view.View.OnClickListener {

	/**
	 * String constants to encode the current layout settings - allowing to 
	 * retrieve these after restart. 
	 */
	public static final String LAST_VIEW_LABEL_NETWORK = "network";
	public static final String LAST_VIEW_LABEL_SEARCH = "search";
	public static final String LAST_VIEW_LABEL_SURVEY = "survey";
	public static final String LAST_VIEW_LABEL_HISTORY = "history";
	public static final String LAST_VIEW_LABEL_STATISTICS="statistics";
	public static final String LAST_VIEW_LABEL_EGO = "ego";
	public static final String LAST_VIEW_LABEL_ALTER = "alter";
	public static final String LAST_VIEW_LABEL_ATTRIBUTE = "attribute";

	/*
	 * String constants serving as identifying tags for the various fragments.
	 */
	private static final String EGO_CONTROL_VIEW_FRAGMENT_TAG = "ego_control_view_fragment_tag";
	private static final String ALTER_LIST_VIEW_FRAGMENT_TAG = "alter_list_view_fragment_tag";
	private static final String EGO_DETAIL_VIEW_FRAGMENT_TAG = "ego_detail_view_fragment_tag";
	private static final String ALTER_DETAIL_VIEW_FRAGMENT_TAG = "alter_detail_view_fragment_tag";
	private static final String ATTRIBUTE_LIST_FRAGMENT_TAG = "attribute_list_fragment_tag";
	private static final String ATTRIBUTE_DETAIL_VIEW_FRAGMENT_TAG = "attribute_detail_view_fragment_tag";
	private static final String NETWORK_VIEW_FRAGMENT_TAG = "network_view_fragment_tag";
	private static final String NETWORK_CONTROL_FRAGMENT_TAG = "network_control_fragment_tag";
	private static final String SEARCH_VIEW_FRAGMENT_TAG = "search_view_fragment_tag";
	private static final String SEARCH_CONTROL_FRAGMENT_TAG = "search_control_fragment_tag";
	private static final String HISTORY_VIEW_FRAGMENT_TAG = "history_view_fragment_tag";
	private static final String HISTORY_CONTROL_FRAGMENT_TAG = "history_control_fragment_tag";
	private static final String STATISTICS_CONTROL_FRAGMENT_TAG = "statistics_control_fragment_tag";
	private static final String STATISTICS_VIEW_GENDER_FRAGMENT_TAG = "statistics_view_gender_fragment_tag";
	private static final String STATISTICS_VIEW_DENSITY_FRAGMENT_TAG = "statistics_view_density_fragment_tag";
	private static final String SURVEY_CONTROL_FRAGMENT_TAG = "survey_control_fragment_tag";
	private static final String SURVEY_FRAGMENT_TAG = "survey_fragment_tag";

	/*
	 * String constants defining the statistics view to show.
	 */
	private static final String STATISTICS_CONTROL = "statistics_control";
	private static final String STATISTICS_GENDER = "statistics_gender";
	private static final String STATISTICS_DENSITY = "statistics_density";
		
	/*
	 * Integer constants defining the ordering of elements in the view selection spinner in the action bar.
	 */
	private static final int viewSelectionItemPosOfEgo = 0;
	private static final int viewSelectionItemPosOfAlter = 1;
	private static final int viewSelectionItemPosOfAttribute = 2;
	//number of elements in the view selection spinner
	private static final int numberOfViewSelectionItems = 3;

	//menu item ids for items that are only in the menu if API level < HONEYCOMB
	private static final int menuItemIdEgo = -3383482;
	private static final int menuItemIdAlter = -3383483;
	private static final int menuItemIdAttribute = -3383484;

	/**
	 * String constants serving as identifying tags for various dialogs.
	 */
	public static final String SELECT_INPUT_EGO_FILE_DIALOG_TAG = "select_input_ego_file_dialog_tag";
	public static final String SELECT_INPUT_INT_FILE_DIALOG_TAG = "select_input_int_file_dialog_tag";
	public static final String SELECT_OUTPUT_GRAPHML_FILE_DIALOG_TAG = "select_output_graphml_file_dialog_tag";
	public static final String SELECT_HISTORY_OUTPUT_GRAPHML_FILE_DIALOG_TAG = "select_history_output_graphml_file_dialog_tag";
	public static final String SELECT_INPUT_GRAPHML_FILE_DIALOG_TAG = "select_input_graphml_file_dialog_tag";
	public static final String EDIT_EGO_ATTRIBS_DIALOG_TAG = "edit_ego_attribs_dialog_tag";
	public static final String ADD_ALTER_MEMO_DIALOG_TAG = "add_alter_memo_dialog_tag";
	public static final String SUGGEST_ALTERS_DIALOG_TAG = "suggest_alters_dialog_tag";
	public static final String ADD_EGO_MEMO_DIALOG_TAG = "add_ego_memo_dialog_tag";
	public static final String BROWSE_ALTER_CONTACT_EVENTS_DIALOG_TAG = "browse_alter_contact_events_dialog_tag";
	public static final String BROWSE_ALTER_MEMOS_DIALOG_TAG = "browse_alter_memos_dialog_tag";
	public static final String BROWSE_EGO_MEMOS_DIALOG_TAG = "browse_ego_memos_dialog_tag";
	public static final String EDIT_ALTER_ATTRIBS_DIALOG_TAG = "edit_alter_attribs_dialog_tag";
	public static final String EDIT_ALTER_ALTER_TIES_DIALOG_TAG = "edit_alter_alter_ties_dialog_tag";
	public static final String EDIT_EGO_ALTER_ATTRIBS_DIALOG_TAG = "edit_ego_alter_attribs_dialog_tag";
	public static final String EDIT_ALTER_ALTER_ATTRIBS_DIALOG_TAG = "edit_alter_alter_attribs_dialog_tag";
	private static final String ADD_ATTRIBUTE_DIALOG_TAG = "add_attribute_dialog_tag";
	private static final String ADD_ALTER_DIALOG_TAG = "add_alter_dialog_tag";
	private static final String CHANGE_SETTINGS_DIALOG_TAG = "change_settings_dialog_tag";
	private static final String INTERVIEW_ME_DIALOG_TAG = "interview_me_dialog_tag";
	private static final String EDIT_ATTRIBUTE_STRUCTURE_DIALOG_TAG = "edit_attrib_structure_dialog_tag";
	public static final String EDIT_ATTRIBUTE_VALUES_DIALOG_TAG = "edit_attrib_values_dialog_tag";
	private static final String CONFIRM_REMOVE_ATTRIBUTE_DIALOG_TAG = "confirm_remove_attribute_dialog_tag";
	private static final String CONFIRM_REMOVE_ALTER_DIALOG_TAG = "confirm_remove_alter_dialog_tag";
	private static final String RENAME_ALTER_DIALOG_TAG = "rename_alter_dialog_tag";
	private static final String CHOSE_ALTER_OPERATION_DIALOG_TAG = "chose_alter_operation_dialog_tag";
	private static final String ADD_ALTER_EVENT_DIALOG_TAG = "add_alter_event_dialog_tag";
	private static final String ADD_ALTER_ALTER_EVENT_DIALOG_TAG = "add_alter_alter_event_dialog_tag";
	public static final String ADD_ALTER_ALTER_CONTACT_EVENT_DIALOG_TAG = "add_alter_alter_contact_event_dialog_tag";
	public static final String ADD_ALTER_ALTER_MEMO_DIALOG_TAG = "add_alter_alter_memo_dialog_tag";
	public static final String ADD_ALTER_CONTACT_EVENT_DIALOG_TAG = "add_alter_contact_event_dialog_tag";

	/**
	 * Integer constants identifying the request to pick a contact. 
	 */
	public static final int PICK_CONTACT_DURING_SURVEY_REQUEST_CODE = 909;
	public static final int PICK_CONTACT_FROM_ADD_ALTER_DIALOG_REQUEST_CODE = 876;

	@Override
	/*
	 * 
	 * (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onStart()
	 */
	protected void onStart(){
		super.onStart();

	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onStop()
	 */
	protected void onStop(){
		super.onStop();
	}

	/*
	 * Sets the action bar for top level views.
	 */
	private void setDisplayShowTopLevelView(boolean dualPane){
		boolean showDetail = SCCProperties.getInstance(this).getPropertyShowDetailInSinglePaneView();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Set up the action bar to show a dropdown list.
			final ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowTitleEnabled(false);
			if(dualPane || !showDetail)
				actionBar.setDisplayHomeAsUpEnabled(false);
			else
				actionBar.setDisplayHomeAsUpEnabled(true);
			if(actionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_LIST){
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
				// Set up the dropdown list navigation in the action bar.
				String[] viewSelectionItems = new String[numberOfViewSelectionItems];
				viewSelectionItems[viewSelectionItemPosOfEgo] = getString(R.string.view_label_ego); 
				viewSelectionItems[viewSelectionItemPosOfAlter] = getString(R.string.view_label_alter); 
				viewSelectionItems[viewSelectionItemPosOfAttribute] = getString(R.string.view_label_attribute); 
				actionBar.setListNavigationCallbacks(
						new ArrayAdapter<String>(getThemedContextPrevIceCreamSandwichCompatible(),
								android.R.layout.simple_list_item_1,
								android.R.id.text1, viewSelectionItems), this);
			}
			//Restore the current view selection if necessary
			String lastViewLabel = SCCProperties.getInstance(this).getPropertyLastViewLabel();
			int position = -1;
			if(LAST_VIEW_LABEL_EGO.equals(lastViewLabel)){
				position = viewSelectionItemPosOfEgo;
			}
			if(LAST_VIEW_LABEL_ALTER.equals(lastViewLabel)){
				position = viewSelectionItemPosOfAlter;
			}
			if(LAST_VIEW_LABEL_ATTRIBUTE.equals(lastViewLabel)){
				position = viewSelectionItemPosOfAttribute;
			}
			if(position >= 0){
				if(getActionBar().getSelectedNavigationIndex() != position)
					actionBar.setSelectedNavigationItem(position);
			}
		}
	}

	/*
	 * Sets the action bar for second level views.
	 */
	private void setDisplayShowSecondLevelView(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			SCCProperties properties = SCCProperties.getInstance(this);
			// Set up the action bar to not show a dropdown list but a title and the up button
			final ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowTitleEnabled(true);
			if(actionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_STANDARD)
				actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			String lastTopLabel = properties.getPropertyLastTopLevelViewLabel();
			String lastTopDisplayLabel = null;
			if(LAST_VIEW_LABEL_ALTER.equals(lastTopLabel))
				lastTopDisplayLabel = getString(R.string.view_label_alter);
			if(LAST_VIEW_LABEL_ATTRIBUTE.equals(lastTopLabel))
				lastTopDisplayLabel = getString(R.string.view_label_attribute);
			if(LAST_VIEW_LABEL_EGO.equals(lastTopLabel))
				lastTopDisplayLabel = getString(R.string.view_label_ego);
			String lastViewLabel = properties.getPropertyLastViewLabel();
			String lastDisplayViewLabel = null;
			if(LAST_VIEW_LABEL_HISTORY.equals(lastViewLabel))
				lastDisplayViewLabel = getString(R.string.view_label_history);
			if(LAST_VIEW_LABEL_NETWORK.equals(lastViewLabel))
				lastDisplayViewLabel = getString(R.string.view_label_network);
			if(LAST_VIEW_LABEL_SURVEY.equals(lastViewLabel))
				lastDisplayViewLabel = getString(R.string.view_label_survey);
			if(LAST_VIEW_LABEL_SEARCH.equals(lastViewLabel))
				lastDisplayViewLabel = getString(R.string.view_label_search);
			if(LAST_VIEW_LABEL_STATISTICS.equals(lastViewLabel))
				lastDisplayViewLabel = getString(R.string.view_label_statistics);
			actionBar.setTitle(lastDisplayViewLabel);
			actionBar.setSubtitle("(" + lastTopDisplayLabel + ")");
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	/*
	 * Sets the content view and fills it with the views last seen before the activity had been stopped.
	 * 
	 * (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onCreate(android.os.Bundle)
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_sccmain);

		String lastViewLabel = SCCProperties.getInstance(this).getPropertyLastViewLabel();

		boolean dualPane = false;
		
		// Create the view only if not restored from a previous state
		//if (savedInstanceState == null) {

		// Check that the activity is using the layout version with
		// the list_container FrameLayout; if so restore the last list view
		if (findViewById(R.id.list_container) != null) {

			dualPane = true;

			Fragment fragment = null; //the fragment to be put into the list container 
			String listFragmentTag = null;

			if(LAST_VIEW_LABEL_EGO.equals(lastViewLabel)){
				fragment = new EgoControlViewFragment();
				listFragmentTag = EGO_CONTROL_VIEW_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_ALTER.equals(lastViewLabel)){
				fragment = new AlterListViewFragment();
				listFragmentTag = ALTER_LIST_VIEW_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_ATTRIBUTE.equals(lastViewLabel)){
				fragment = new AttributeListFragment();
				listFragmentTag = ATTRIBUTE_LIST_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_SURVEY.equals(lastViewLabel)){
				fragment = new SurveyControlFragment();
				listFragmentTag = SURVEY_CONTROL_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_HISTORY.equals(lastViewLabel)){
				fragment = new HistoryControlFragment();
				listFragmentTag = HISTORY_CONTROL_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_STATISTICS.equals(lastViewLabel)){
				fragment = new StatisticsControlFragment();
				listFragmentTag = STATISTICS_CONTROL_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_NETWORK.equals(lastViewLabel)){
				fragment = new NetworkControlFragment();
				listFragmentTag = NETWORK_CONTROL_FRAGMENT_TAG;
			}
			if(fragment != null){
				// Add the fragment to the 'list_container' FrameLayout
				getSupportFragmentManager().beginTransaction()
				.replace(R.id.list_container, fragment, listFragmentTag).commit();
			}

		}
		// Check that the activity is using the layout version with
		// the detail_container FrameLayout; if so restore the last detail view
		if (findViewById(R.id.detail_container) != null) {

			dualPane = true;

			Fragment fragment = null;
			String detailFragmentTag = null;
			if(LAST_VIEW_LABEL_EGO.equals(lastViewLabel)){
				fragment = new EgoDetailViewFragment();
				detailFragmentTag = EGO_DETAIL_VIEW_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_NETWORK.equals(lastViewLabel)){
				fragment = new NetworkViewFragment();
				detailFragmentTag = NETWORK_VIEW_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_ALTER.equals(lastViewLabel)){
				fragment = new AlterDetailViewFragment();
				detailFragmentTag = ALTER_DETAIL_VIEW_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_ATTRIBUTE.equals(lastViewLabel)){
				fragment = new AttributeDetailViewFragment();
				detailFragmentTag = ATTRIBUTE_DETAIL_VIEW_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_SURVEY.equals(lastViewLabel)){
				fragment = new SurveyFragment();
				detailFragmentTag = SURVEY_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_STATISTICS.equals(lastViewLabel)){
				fragment = new StatisticsViewGenderFragment();
				detailFragmentTag = STATISTICS_VIEW_GENDER_FRAGMENT_TAG;
			}
			if(LAST_VIEW_LABEL_HISTORY.equals(lastViewLabel)){
				fragment = new HistoryViewFragment();
				detailFragmentTag = HISTORY_VIEW_FRAGMENT_TAG;
			}
			if(fragment != null){
				// Add the fragment to the 'detail_container' FrameLayout
				getSupportFragmentManager().beginTransaction()
				.replace(R.id.detail_container, fragment, detailFragmentTag).commit();
			}
		}
		// Check that the activity is using the layout version with
		// the single_pane_container FrameLayout (in portrait orientation); 
		// if so restore the last list or detail view
		if (findViewById(R.id.single_pane_container) != null) {

			dualPane = false;

			boolean showDetails = SCCProperties.getInstance(this).getPropertyShowDetailInSinglePaneView();

			Fragment fragment = null; //the fragment to be put into the single pane container 
			String fragmentTag = null;

			if(showDetails){
				if(LAST_VIEW_LABEL_EGO.equals(lastViewLabel)){
					fragment = new EgoDetailViewFragment();
					fragmentTag = EGO_DETAIL_VIEW_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_NETWORK.equals(lastViewLabel)){
					fragment = new NetworkViewFragment();
					fragmentTag = NETWORK_VIEW_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_ALTER.equals(lastViewLabel)){
					fragment = new AlterDetailViewFragment();
					fragmentTag = ALTER_DETAIL_VIEW_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_ATTRIBUTE.equals(lastViewLabel)){
					fragment = new AttributeDetailViewFragment();
					fragmentTag = ATTRIBUTE_DETAIL_VIEW_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_SURVEY.equals(lastViewLabel)){
					fragment = new SurveyFragment();
					fragmentTag = SURVEY_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_STATISTICS.equals(lastViewLabel)){
					fragment = new StatisticsViewGenderFragment();
					fragmentTag = STATISTICS_VIEW_GENDER_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_HISTORY.equals(lastViewLabel)){
					fragment = new HistoryViewFragment();
					fragmentTag = HISTORY_VIEW_FRAGMENT_TAG;
				}
			} else {
				if(LAST_VIEW_LABEL_EGO.equals(lastViewLabel)){
					fragment = new EgoControlViewFragment();
					fragmentTag = EGO_CONTROL_VIEW_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_ALTER.equals(lastViewLabel)){
					fragment = new AlterListViewFragment();
					fragmentTag = ALTER_LIST_VIEW_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_ATTRIBUTE.equals(lastViewLabel)){
					fragment = new AttributeListFragment();
					fragmentTag = ATTRIBUTE_LIST_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_SURVEY.equals(lastViewLabel)){
					fragment = new SurveyControlFragment();
					fragmentTag = SURVEY_CONTROL_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_HISTORY.equals(lastViewLabel)){
					fragment = new HistoryControlFragment();
					fragmentTag = HISTORY_CONTROL_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_STATISTICS.equals(lastViewLabel)){
					fragment = new StatisticsControlFragment();
					fragmentTag = STATISTICS_CONTROL_FRAGMENT_TAG;
				}
				if(LAST_VIEW_LABEL_NETWORK.equals(lastViewLabel)){
					fragment = new NetworkControlFragment();
					fragmentTag = NETWORK_CONTROL_FRAGMENT_TAG;
				}
			}
			if(fragment != null){
				// Add the fragment to the 'list_container' FrameLayout
				getSupportFragmentManager().beginTransaction()
				.replace(R.id.single_pane_container, fragment, fragmentTag).commit();
			}

		}
		//} //end if saved instance state ...
		// in any case, set up the action bar dependent on the view level
		if(LAST_VIEW_LABEL_EGO.equals(lastViewLabel) || 
				LAST_VIEW_LABEL_ALTER.equals(lastViewLabel) || 
				LAST_VIEW_LABEL_ATTRIBUTE.equals(lastViewLabel)){
			setDisplayShowTopLevelView(dualPane);
		} else {
			setDisplayShowSecondLevelView();
		}
		// this will only do anything if the activity has been started with a search intent
        handleSearchIntent(getIntent());
        //if the network is currently empty, immediately show the add alter dialog
        if(PersonalNetwork.getInstance(this).getNumberOfAltersAt(TimeInterval.getCurrentTimePoint()) == 0){
        	DialogFragment dialog = AddAlterDialog.getInstance(this);
        	dialog.show(getSupportFragmentManager(), ADD_ALTER_DIALOG_TAG);
        }
	}

    @Override
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
        handleSearchIntent(intent);
    }
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getThemedContextPrevIceCreamSandwichCompatible() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_sccmain, menu);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			//add menu items that are normally (for newer versions) displayed in the navigation item
			menu.add(Menu.NONE, menuItemIdEgo, 30, R.string.view_label_ego);
			menu.add(Menu.NONE, menuItemIdAlter, 31, R.string.view_label_alter);
			menu.add(Menu.NONE, menuItemIdAttribute, 32, R.string.view_label_attribute);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Associate searchable configuration with the SearchView
			SearchManager searchManager =
					(SearchManager) getSystemService(Context.SEARCH_SERVICE);
			SearchView searchView =
					(SearchView) menu.findItem(R.id.menu_search).getActionView();
			searchView.setSearchableInfo(
					searchManager.getSearchableInfo(getComponentName()));
		}
		return true;
	}

	/**
	 * Handles selection events from the view selection spinner in the action bar.
	 * 
	 * Switches to the selected view dimension.
	 */
	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		switch(position){
		case viewSelectionItemPosOfEgo:
			switchToEgoView();
			return true;
		case viewSelectionItemPosOfAlter:
			switchToAlterView();
			return true;
		case viewSelectionItemPosOfAttribute:
			switchToAttributeView();
			return true;
		default:
			return false;//TODO: makes sense?
		}
	}

	@Override
	/**
	 * Handles selection events from the action bar (apart from the view selection spinner).
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		LayoutSQLite layout = LayoutSQLite.getInstance(this);
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_network_view:
			SCCProperties.getInstance(this).setPropertyShowDetailInSinglePaneView(true);
			layout.resetLayout();
			switchToNetworkView();
			return true;
		case R.id.menu_interview:
			if(SCCProperties.getInstance(this).getInterviewSettingsEgonet())
				switchToSurveyView();
			else
				openInterviewMeDialog();
			return true;		
		case R.id.menu_history:
			SCCProperties.getInstance(this).setPropertyShowDetailInSinglePaneView(true);
			switchToHistoryView();
			return true;		
		case R.id.menu_add_alter:
			addAlterItemClicked();
			return true;
		case R.id.menu_export_history_graphml:
			exportHistory2GraphML();
			return true;
		case R.id.menu_import_history_graphml:
			importHistoryFromGraphML();
			return true;
		case R.id.menu_import_int_file:
			importEgonetInterview();
			return true;
		case R.id.menu_statistics_view:
			showStatisticsControl();
			return true;
		case R.id.menu_settings:
			openSettings();
			return true;
		case android.R.id.home: //up-button clicked
			doUpNavigation();
			return true;
			//handle options that are in the menu for API level that have no action bar
		case menuItemIdEgo:
			switchToEgoView();
			return true;
		case menuItemIdAlter:
			switchToAlterView();
			return true;
		case menuItemIdAttribute:
			switchToAttributeView();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void handleSearchIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            PersonalNetwork network = PersonalNetwork.getInstance(this);
            LinkedHashSet<String> alters = network.search(query);
            switchToSearchView(alters);
        }		
	}

	public void addAlterItemClicked() {
		DialogFragment dialog = AddAlterDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), ADD_ALTER_DIALOG_TAG);
	}

	/*
	 * Opens a dialog to change settings.
	 */
	private void openSettings() {
		DialogFragment dialog = ChangeSettingsDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), CHANGE_SETTINGS_DIALOG_TAG);
	}

	/*
	 * Opens a dialog that presents the next question in a randomized way.
	 */
	private void openInterviewMeDialog() {
		DialogFragment dialog = InterviewMeDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), INTERVIEW_ME_DIALOG_TAG);
	}

	/**
	 * Ensures that the displayed views show the current data. Should be called whenever
	 * the database changed.
	 */
	public void updatePersonalNetworkViews(){
		//ALTER
		AlterDetailViewFragment alterDetailViewFragment = (AlterDetailViewFragment) getSupportFragmentManager().
				findFragmentByTag(ALTER_DETAIL_VIEW_FRAGMENT_TAG);
		if(alterDetailViewFragment != null && alterDetailViewFragment.isAdded())
			alterDetailViewFragment.updateView();
		AlterListViewFragment alterListViewFragment = (AlterListViewFragment) getSupportFragmentManager().
				findFragmentByTag(ALTER_LIST_VIEW_FRAGMENT_TAG);
		if(alterListViewFragment != null && alterListViewFragment.isAdded())
			alterListViewFragment.updateView();
		//ATTRIBUTE
		AttributeListFragment attributeListFragment = (AttributeListFragment) getSupportFragmentManager().
				findFragmentByTag(ATTRIBUTE_LIST_FRAGMENT_TAG);
		if(attributeListFragment != null && attributeListFragment.isAdded())
			attributeListFragment.updateView();
		AttributeDetailViewFragment attribDetailViewFragment = (AttributeDetailViewFragment) getSupportFragmentManager()
				.findFragmentByTag(ATTRIBUTE_DETAIL_VIEW_FRAGMENT_TAG);
		if(attribDetailViewFragment != null && attribDetailViewFragment.isAdded())
			attribDetailViewFragment.updateView();
		//NETWORK
		NetworkViewFragment networkViewFragment = (NetworkViewFragment) getSupportFragmentManager().
				findFragmentByTag(NETWORK_VIEW_FRAGMENT_TAG);
		if(networkViewFragment != null && networkViewFragment.isAdded())
			networkViewFragment.updateView();
		NetworkControlFragment networkControlFragment = (NetworkControlFragment) getSupportFragmentManager().
				findFragmentByTag(NETWORK_CONTROL_FRAGMENT_TAG);
		if(networkControlFragment != null && networkControlFragment.isAdded())
			networkControlFragment.updateView();
		//SURVEY
		SurveyControlFragment surveyControlFragment = (SurveyControlFragment) getSupportFragmentManager().
				findFragmentByTag(SURVEY_CONTROL_FRAGMENT_TAG);
		if(surveyControlFragment != null && surveyControlFragment.isAdded())
			surveyControlFragment.updateView();
		SurveyFragment surveyFragment = (SurveyFragment) getSupportFragmentManager().
				findFragmentByTag(SURVEY_FRAGMENT_TAG);
		if(surveyFragment != null && surveyFragment.isAdded())
			surveyFragment.updateView();
		//HISTORY
		HistoryControlFragment historyControlFragment = (HistoryControlFragment) getSupportFragmentManager().
				findFragmentByTag(HISTORY_CONTROL_FRAGMENT_TAG);
		if(historyControlFragment != null && historyControlFragment.isAdded())
			historyControlFragment.updateView();
		HistoryViewFragment historyFragment = (HistoryViewFragment) getSupportFragmentManager().
				findFragmentByTag(HISTORY_VIEW_FRAGMENT_TAG);
		if(historyFragment != null && historyFragment.isAdded())
			historyFragment.updateView();
		//EGO
		EgoControlViewFragment egoControlFragment = (EgoControlViewFragment) getSupportFragmentManager().
				findFragmentByTag(EGO_CONTROL_VIEW_FRAGMENT_TAG);
		if(egoControlFragment != null && egoControlFragment.isAdded())
			egoControlFragment.updateView();
		EgoDetailViewFragment egoDetailFragment = (EgoDetailViewFragment) getSupportFragmentManager().
				findFragmentByTag(EGO_DETAIL_VIEW_FRAGMENT_TAG);
		if(egoDetailFragment != null && egoDetailFragment.isAdded())
			egoDetailFragment.updateView();
	}

	private void doUpNavigation(){
		SCCProperties properties = SCCProperties.getInstance(this);
		String viewLabel = properties.getPropertyLastViewLabel();
		String topLevelViewLabel = properties.getPropertyLastTopLevelViewLabel();
		if(viewLabel.equals(topLevelViewLabel))//we must be in single pane, detail view --> move to the master view
			properties.setPropertyShowDetailInSinglePaneView(false);
		if(LAST_VIEW_LABEL_STATISTICS.equals(viewLabel) &&  //statistics view and
				findViewById(R.id.single_pane_container) != null && // single pane showing 
				properties.getPropertyShowDetailInSinglePaneView()){ //the details view --> move to the statistics control view
			properties.setPropertyShowDetailInSinglePaneView(false);
			switchToStatisticsView(STATISTICS_CONTROL); //TODO: it should be possible to implement switchToStatisticsView without any parameter (storing that information in the properties)
		} else { //in top-level views and all other second level views (besides statistics) and in two-pane view, we move to the last top level view
			if(LAST_VIEW_LABEL_ALTER.equals(topLevelViewLabel))
				switchToAlterView();
			if(LAST_VIEW_LABEL_ATTRIBUTE.equals(topLevelViewLabel))
				switchToAttributeView();
			if(LAST_VIEW_LABEL_EGO.equals(topLevelViewLabel))
				switchToEgoView();
		}
	}
	
	/**
	 * Shows the statistics view.
	 */
	private void switchToStatisticsView(String statisticsFragment) {

		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		// Check that the activity is using the layout version with
		// the list_container FrameLayout
		if (findViewById(R.id.list_container) != null) {
			StatisticsControlFragment fragment = new StatisticsControlFragment();
			// Replace the statistics control fragment in the 'list_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.list_container, fragment, STATISTICS_CONTROL_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.detail_container) != null) {
			//Check which statistics fragment must be showed in the right fragment.
			if(statisticsFragment.equals(STATISTICS_GENDER)) {
				StatisticsViewGenderFragment fragment = new StatisticsViewGenderFragment();
				// Replace the statistics fragment in the 'view_container' FrameLayout
				if(!fragment.isVisible())
					trans.replace(R.id.detail_container, fragment, STATISTICS_VIEW_GENDER_FRAGMENT_TAG);
			}
			else if(statisticsFragment.equals(STATISTICS_DENSITY)){
				StatisticsViewDensityFragment fragment = new StatisticsViewDensityFragment();
				// Replace the statistics fragment in the 'view_container' FrameLayout
				if(!fragment.isVisible())
					trans.replace(R.id.detail_container, fragment, STATISTICS_VIEW_DENSITY_FRAGMENT_TAG);
			} else {
				//If method was called with STATISTICS_CONTROL or something else (unknown statistics view),
				//right panel will show gender chart.
				StatisticsViewGenderFragment fragment = new StatisticsViewGenderFragment();
				// Replace the statistics fragment in the 'view_container' FrameLayout
				if(!fragment.isVisible())
					trans.replace(R.id.detail_container, fragment, STATISTICS_VIEW_GENDER_FRAGMENT_TAG);
			}
		}
		// Check that the activity is using the layout version with
		// the single_pane_container
		if (findViewById(R.id.single_pane_container) != null) {
			if(SCCProperties.getInstance(this).getPropertyShowDetailInSinglePaneView()){
				if (statisticsFragment.equals(STATISTICS_GENDER)) {
					Fragment fragment = new StatisticsViewGenderFragment();
					if(!fragment.isVisible())
						trans.replace(R.id.single_pane_container, fragment, STATISTICS_VIEW_GENDER_FRAGMENT_TAG);
				}
				else if (statisticsFragment.equals(STATISTICS_DENSITY)) {
					Fragment fragment = new StatisticsViewDensityFragment();
					if(!fragment.isVisible())
						trans.replace(R.id.single_pane_container, fragment, STATISTICS_VIEW_DENSITY_FRAGMENT_TAG);
				} 
				else {
					//If method was called with STATISTICS_CONTROL or something else (unknown statistic view),
					//show statistics control fragment.
					Fragment fragment = new StatisticsControlFragment();
					if(!fragment.isVisible())
						trans.replace(R.id.single_pane_container, fragment, STATISTICS_CONTROL_FRAGMENT_TAG);
				}
			} else {
				Fragment fragment = new StatisticsControlFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, STATISTICS_CONTROL_FRAGMENT_TAG);
			}
		}
		if(!trans.isEmpty()){
			//trans.addToBackStack(null);
			trans.commitAllowingStateLoss();
		}
		SCCProperties.getInstance(this).setPropertyLastViewLabel(LAST_VIEW_LABEL_STATISTICS);
		setDisplayShowSecondLevelView();
	}
	
	/**
	 * Shows the search view.
	 */
	private void switchToSearchView(LinkedHashSet<String> alters){
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		// Check that the activity is using the layout version with
		// the list_container FrameLayout
		if (findViewById(R.id.list_container) != null) {

			SearchControlFragment fragment = new SearchControlFragment();

			// Replace the network fragment in the 'list_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.list_container, fragment, SEARCH_CONTROL_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.detail_container) != null) {

			SearchViewFragment fragment = new SearchViewFragment();
			fragment.setAlterList(alters);
			// Replace the network fragment in the 'view_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.detail_container, fragment, SEARCH_VIEW_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the single_pane_container
		if (findViewById(R.id.single_pane_container) != null) {
			SCCProperties.getInstance(this).setPropertyShowDetailInSinglePaneView(true);
			SearchViewFragment fragment = new SearchViewFragment();
			fragment.setAlterList(alters);
			if(!fragment.isVisible())
				trans.replace(R.id.single_pane_container, fragment, SEARCH_VIEW_FRAGMENT_TAG);
		}
		if(!trans.isEmpty()){
			//trans.addToBackStack(null);
			trans.commitAllowingStateLoss();
		}
		SCCProperties.getInstance(this).setPropertyLastViewLabel(LAST_VIEW_LABEL_SEARCH);
		setDisplayShowSecondLevelView();
	}

	/**
	 * Shows the network view.
	 */
	private void switchToNetworkView(){
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		// Check that the activity is using the layout version with
		// the list_container FrameLayout
		if (findViewById(R.id.list_container) != null) {

			NetworkControlFragment fragment = new NetworkControlFragment();

			// Replace the network fragment in the 'list_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.list_container, fragment, NETWORK_CONTROL_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.detail_container) != null) {

			NetworkViewFragment fragment = new NetworkViewFragment();

			// Replace the network fragment in the 'view_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.detail_container, fragment, NETWORK_VIEW_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the single_pane_container
		if (findViewById(R.id.single_pane_container) != null) {
			if(SCCProperties.getInstance(this).getPropertyShowDetailInSinglePaneView()){
				Fragment fragment = new NetworkViewFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, NETWORK_VIEW_FRAGMENT_TAG);
			} else {
				Fragment fragment = new NetworkControlFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, NETWORK_CONTROL_FRAGMENT_TAG);				
			}
		}
		if(!trans.isEmpty()){
			//trans.addToBackStack(null);
			trans.commitAllowingStateLoss();
		}
		SCCProperties.getInstance(this).setPropertyLastViewLabel(LAST_VIEW_LABEL_NETWORK);
		setDisplayShowSecondLevelView();
	}

	/**
	 */
	private void switchToHistoryView(){
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		// Check that the activity is using the layout version with
		// the list_container FrameLayout
		if (findViewById(R.id.list_container) != null) {

			HistoryControlFragment fragment = new HistoryControlFragment();

			// Replace the fragment in the 'list_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.list_container, fragment, HISTORY_CONTROL_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.detail_container) != null) {

			HistoryViewFragment fragment = new HistoryViewFragment();

			// Replace the fragment in the 'view_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.detail_container, fragment, HISTORY_VIEW_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the single_pane_container
		if (findViewById(R.id.single_pane_container) != null) {
			if(SCCProperties.getInstance(this).getPropertyShowDetailInSinglePaneView()){
				Fragment fragment = new HistoryViewFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, HISTORY_VIEW_FRAGMENT_TAG);
			} else {
				Fragment fragment = new HistoryControlFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, HISTORY_CONTROL_FRAGMENT_TAG);				
			}
		}
		if(!trans.isEmpty()){
			//trans.addToBackStack(null);
			trans.commitAllowingStateLoss();
		}
		SCCProperties.getInstance(this).setPropertyLastViewLabel(LAST_VIEW_LABEL_HISTORY);
		setDisplayShowSecondLevelView();
	}

	/**
	 * Shows the current survey question in the detail view area.
	 */
	public void switchToSurveyView(){
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		// Check that the activity is using the layout version with
		// the list_container FrameLayout
		if (findViewById(R.id.list_container) != null) {

			SurveyControlFragment fragment = new SurveyControlFragment();

			// Replace the network fragment in the 'list_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.list_container, fragment, SURVEY_CONTROL_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.detail_container) != null) {

			SurveyFragment fragment = new SurveyFragment();

			// Replace the network fragment in the 'view_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.detail_container, fragment, SURVEY_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the single_pane_container
		if (findViewById(R.id.single_pane_container) != null) {
			if(SCCProperties.getInstance(this).getPropertyShowDetailInSinglePaneView()){
				Fragment fragment = new SurveyFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, SURVEY_FRAGMENT_TAG);
			} else {
				Fragment fragment = new SurveyControlFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, SURVEY_CONTROL_FRAGMENT_TAG);				
			}
		}
		//put the transaction to the backstack, excute and remember survey as the last view
		if(!trans.isEmpty()){
			//trans.addToBackStack(null);
			trans.commitAllowingStateLoss();
		}
		SCCProperties properties = SCCProperties.getInstance(this);
		properties.setPropertyLastViewLabel(LAST_VIEW_LABEL_SURVEY);
		setDisplayShowSecondLevelView();
	}

	/**
	 * Shows the ego details (attributes, etc) in the detail view area.
	 */
	private void switchToEgoView(){
		boolean dualPane = false;
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		// Check that the activity is using the layout version with
		// the list_container FrameLayout
		if (findViewById(R.id.list_container) != null) {

			dualPane = true;

			EgoControlViewFragment fragment = new EgoControlViewFragment();

			// Replace the network fragment in the 'view_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.list_container, fragment, EGO_CONTROL_VIEW_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.detail_container) != null) {

			dualPane = true;

			EgoDetailViewFragment fragment = new EgoDetailViewFragment();

			// Replace the network fragment in the 'view_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.detail_container, fragment, EGO_DETAIL_VIEW_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the single_pane_container
		if (findViewById(R.id.single_pane_container) != null) {
			if(SCCProperties.getInstance(this).getPropertyShowDetailInSinglePaneView()){
				Fragment fragment = new EgoDetailViewFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, EGO_DETAIL_VIEW_FRAGMENT_TAG);
			} else {
				Fragment fragment = new EgoControlViewFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, EGO_CONTROL_VIEW_FRAGMENT_TAG);				
			}
		}
		if(!trans.isEmpty()){
			//trans.addToBackStack(null);
			trans.commitAllowingStateLoss();
		}
		SCCProperties properties = SCCProperties.getInstance(this);
		properties.setPropertyLastViewLabel(LAST_VIEW_LABEL_EGO);
		properties.setPropertyLastTopLevelViewLabel(LAST_VIEW_LABEL_EGO);
		setDisplayShowTopLevelView(dualPane);
	}

	/**
	 * Shows the details (attributes, ties) of the selected alter in the detail view area.
	 */
	public void switchToAlterView(){

		boolean dualPane = false;

		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.list_container) != null) {

			dualPane = true;

			AlterListViewFragment fragment = new AlterListViewFragment();

			if(!fragment.isVisible())
				trans.replace(R.id.list_container, fragment, ALTER_LIST_VIEW_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.detail_container) != null) {

			dualPane = true;

			AlterDetailViewFragment fragment = new AlterDetailViewFragment();

			// Replace the network fragment in the 'view_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.detail_container, fragment, ALTER_DETAIL_VIEW_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the single_pane_container
		if (findViewById(R.id.single_pane_container) != null) {
			if(SCCProperties.getInstance(this).getPropertyShowDetailInSinglePaneView()){
				Fragment fragment = new AlterDetailViewFragment();
				if(!fragment.isVisible()){
					trans.replace(R.id.single_pane_container, fragment, ALTER_DETAIL_VIEW_FRAGMENT_TAG);
				}
			} else {
				Fragment fragment = new AlterListViewFragment();
				if(!fragment.isVisible()){
					trans.replace(R.id.single_pane_container, fragment, ALTER_LIST_VIEW_FRAGMENT_TAG);
				}
			}
		}
		if(!trans.isEmpty()){
			//trans.addToBackStack(null);
			trans.commitAllowingStateLoss();
		}
		SCCProperties properties = SCCProperties.getInstance(this);
		properties.setPropertyLastViewLabel(LAST_VIEW_LABEL_ALTER);
		properties.setPropertyLastTopLevelViewLabel(LAST_VIEW_LABEL_ALTER);
		setDisplayShowTopLevelView(dualPane);
	}

	/**
	 * Shows the details (type, values) of the selected attribute in the detail view area.
	 */
	public void switchToAttributeView(){

		boolean dualPane = false;

		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.list_container) != null) {

			dualPane = true;

			/* that's how it should work but replacing fragments to another container does not work :-(
			 * So we always have to create a new fragment
			AttributeListFragment fragment = (AttributeListFragment) getSupportFragmentManager().
					findFragmentByTag(ATTRIBUTE_LIST_FRAGMENT_TAG);
			if(fragment == null)
				fragment = new AttributeListFragment();
			 */
			AttributeListFragment fragment = new AttributeListFragment();

			// Replace the network fragment in the 'view_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.list_container, fragment, ATTRIBUTE_LIST_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the view_container FrameLayout
		if (findViewById(R.id.detail_container) != null) {

			dualPane = true;

			AttributeDetailViewFragment fragment = new AttributeDetailViewFragment();

			// Replace the network fragment in the 'view_container' FrameLayout
			if(!fragment.isVisible())
				trans.replace(R.id.detail_container, fragment, ATTRIBUTE_DETAIL_VIEW_FRAGMENT_TAG);
		}
		// Check that the activity is using the layout version with
		// the single_pane_container
		if (findViewById(R.id.single_pane_container) != null) {
			if(SCCProperties.getInstance(this).getPropertyShowDetailInSinglePaneView()){
				Fragment fragment = new AttributeDetailViewFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, ATTRIBUTE_DETAIL_VIEW_FRAGMENT_TAG);
			} else {
				Fragment fragment = new AttributeListFragment();
				if(!fragment.isVisible())
					trans.replace(R.id.single_pane_container, fragment, ATTRIBUTE_LIST_FRAGMENT_TAG);				
			}
		}
		if(!trans.isEmpty()){
			//trans.addToBackStack(null);
			trans.commitAllowingStateLoss();
		}
		SCCProperties properties = SCCProperties.getInstance(this);
		properties.setPropertyLastViewLabel(LAST_VIEW_LABEL_ATTRIBUTE);
		properties.setPropertyLastTopLevelViewLabel(LAST_VIEW_LABEL_ATTRIBUTE);
		setDisplayShowTopLevelView(dualPane);
	}	

	/**
	 * Shows the ego details in the single pane.
	 * 
	 * Called from show ego details button in the ego control view (only present in single pane view).
	 * 
	 * @param view 
	 */
	public void showEgoDetailsFromEgoControl(View view){
		SCCProperties.getInstance(this).setPropertyShowDetailInSinglePaneView(true);
		switchToEgoView();
	}

	/**
	 * Shows history details in the single pane.
	 * 
	 * Called from show history details button in the history control view (only present in single pane view).
	 * 
	 * @param view 
	 */
	public void showHistoryDetailsFromHistoryControl(View view){
		SCCProperties.getInstance(this).setPropertyShowDetailInSinglePaneView(true);
		switchToHistoryView();
	}
	
	/**
	 * Shows gender statistics in the single pane.
	 * 
	 * Called from show gender statistics button in the statistics control view.
	 * 
	 * @param view 
	 */
	public void showGenderStatisticsFromStatisticsControl(View view){
		SCCProperties.getInstance(this).setPropertyShowDetailInSinglePaneView(true);
		switchToStatisticsView(STATISTICS_GENDER);
	}
	
	/**
	 * Shows density statistics in the single pane.
	 * 
	 * Called from show density statistics button in the statistics control view.
	 * 
	 * @param view 
	 */
	public void showDensityStatisticsFromStatisticsControl(View view){
		SCCProperties.getInstance(this).setPropertyShowDetailInSinglePaneView(true);
		switchToStatisticsView(STATISTICS_DENSITY);
	}
	
	public void showStatisticsControl() {
		SCCProperties.getInstance(this).setPropertyShowDetailInSinglePaneView(false);
		switchToStatisticsView(STATISTICS_CONTROL);
	}

	/**
	 * Opens the dialog to compose and add a new ego memo.
	 * 
	 * Called from add_ego_memo button in the ego detail view.
	 * 
	 * @param view 
	 */
	public void addEgoMemo(View view){
		DialogFragment dialog = AddEgoMemoDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), ADD_EGO_MEMO_DIALOG_TAG);
	}

	/**
	 * Opens the chose alter operation dialog. 
	 * 
	 *  Called from the respective button in the alter detail view. 
	 *  
	 * @param view
	 */
	public void addAlterEvent(View view){
		if(PersonalNetwork.getInstance(this).getSelectedAlter() != null){
			DialogFragment dialog = AddAlterEventDialog.getInstance(this);
			dialog.show(getSupportFragmentManager(), ADD_ALTER_EVENT_DIALOG_TAG);
		}
	}

	/**
	 * Opens a dialog to add an alter alter event or to reverse the tie. 
	 * 
	 *  Called from the respective button in the alter detail view. 
	 *  
	 * @param view
	 */
	public void addAlterAlterEvent(View view){
		String selectedAlter = PersonalNetwork.getInstance(this).getSelectedAlter(); 
		String selectedSecondAlter = PersonalNetwork.getInstance(this).getSelectedSecondAlter(); 
		if(selectedAlter != null && selectedSecondAlter != null && !selectedAlter.equals(selectedSecondAlter)){
			DialogFragment dialog = AddAlterAlterEventDialog.getInstance(this);
			dialog.show(getSupportFragmentManager(), ADD_ALTER_ALTER_EVENT_DIALOG_TAG);
		}
	}

	/**
	 * Opens a dialog to add an alter alter contact event. 
	 * 
	 *  Called from the respective button in the alter detail view. 
	 *  
	 * @param view
	 */
	public void addAlterAlterContactEvent(View view){
		DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
				findFragmentByTag(ADD_ALTER_ALTER_EVENT_DIALOG_TAG);
		if(callingDialog != null)
			callingDialog.dismiss();
		String selectedAlter = PersonalNetwork.getInstance(this).getSelectedAlter(); 
		String selectedSecondAlter = PersonalNetwork.getInstance(this).getSelectedSecondAlter(); 
		if(selectedAlter != null && selectedSecondAlter != null && !selectedAlter.equals(selectedSecondAlter)){
			DialogFragment dialog = AddAlterAlterContactEventDialog.getInstance(this);
			dialog.show(getSupportFragmentManager(), ADD_ALTER_ALTER_CONTACT_EVENT_DIALOG_TAG);
		}
	}

	/**
	 * Opens the dialog to compose and add a new alter memo.
	 * 
	 * Called from add_alter_memo button in the alter detail view.
	 * 
	 * @param view 
	 */
	public void addAlterMemo(View view){
		DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
				findFragmentByTag(ADD_ALTER_EVENT_DIALOG_TAG);
		if(callingDialog != null)
			callingDialog.dismiss();
		DialogFragment dialog = AddAlterMemoDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), ADD_ALTER_MEMO_DIALOG_TAG);
	}

	/**
	 * Opens the dialog to compose and add a new alter alter memo.
	 * 
	 * Called from add_alter_alter_memo button in the alter detail view.
	 * 
	 * @param view 
	 */
	public void addAlterAlterMemo(View view){
		DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
				findFragmentByTag(ADD_ALTER_ALTER_EVENT_DIALOG_TAG);
		if(callingDialog != null)
			callingDialog.dismiss();
		String selectedAlter = PersonalNetwork.getInstance(this).getSelectedAlter(); 
		String selectedSecondAlter = PersonalNetwork.getInstance(this).getSelectedSecondAlter(); 
		if(selectedAlter != null && selectedSecondAlter != null && !selectedAlter.equals(selectedSecondAlter)){
			DialogFragment dialog = AddAlterAlterMemoDialog.getInstance(this);
			dialog.show(getSupportFragmentManager(), ADD_ALTER_ALTER_MEMO_DIALOG_TAG);
		}
	}

	/**
	 * @param view 
	 */
	public void exchangeSelectedAlters(View view){
		DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
				findFragmentByTag(ADD_ALTER_ALTER_EVENT_DIALOG_TAG);
		if(callingDialog != null)
			callingDialog.dismiss();
		PersonalNetwork network = PersonalNetwork.getInstance(this);
		String selectedSecondAlter = network.getSelectedSecondAlter();
		String selectedAlter = network.getSelectedAlter();
		if(selectedAlter != null && selectedSecondAlter != null){
			network.setSelectedAlter(selectedSecondAlter);
			network.setSelectedSecondAlter(selectedAlter);
			updatePersonalNetworkViews();
		}
	}

	/**
	 * Opens the dialog that shows potentially relevant alters and allows to select from these.
	 * 
	 * Called from Suggest alters button in the add alter dialog.
	 * 
	 * @param view 
	 */
	public void suggestAlters(View view){
		DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
				findFragmentByTag(ADD_ALTER_DIALOG_TAG);
		if(callingDialog != null)
			callingDialog.dismiss();
		DialogFragment dialog = SuggestAltersDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), SUGGEST_ALTERS_DIALOG_TAG);
	}

	/**
	 * Opens the chose alter operation dialog. 
	 * 
	 *  Called from the respective button in the alter detail view. 
	 *  
	 * @param view
	 */
	public void addAlterContactEvent(View view){
		if(PersonalNetwork.getInstance(this).getSelectedAlter() != null){
			DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
					findFragmentByTag(ADD_ALTER_EVENT_DIALOG_TAG);
			if(callingDialog != null)
				callingDialog.dismiss();
			DialogFragment dialog = AddEgoAlterContactEventDialog.getInstance(this);
			dialog.show(getSupportFragmentManager(), ADD_ALTER_CONTACT_EVENT_DIALOG_TAG);
		}
	}

	/**
	 * Opens the dialog to edit the ego attributes.
	 * 
	 * Called from the edit ego attributes button in the ego detail view.
	 * 
	 * @param view 
	 */
	public void editEgoAttributes(View view){
		SCCProperties.getInstance(this).setPropertyEgoDetailExpandAttributes(true);
		DialogFragment dialog = EditEgoAttributesDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), EDIT_EGO_ATTRIBS_DIALOG_TAG);
	}

	/**
	 * Opens the edit alter attributes dialog. (From alter detail view --> alter attributes row.)
	 * @param view 
	 */
	public void editAlterAttributes(View view){
		SCCProperties.getInstance(this).setPropertyAlterDetailExpandAttributes(true);
		DialogFragment editAlterAttributesDialog = EditAlterAttributesDialog.getInstance(this);
		editAlterAttributesDialog.show(getSupportFragmentManager(), EDIT_ALTER_ATTRIBS_DIALOG_TAG);
	}

	/**
	 * Opens the dialog to edit the structure of the currently selected attribute.
	 * (Called from the attribute detail view --> edit button in the top row)
	 * @param view
	 */
	public void editAttributeStructure(View view){
		if(PersonalNetwork.getInstance(this).getSelectedAttributeDomain() != null){
			DialogFragment editAttributeStructureDialog = EditAttributeStructureDialog.getInstance(this);
			editAttributeStructureDialog.show(getSupportFragmentManager(), EDIT_ATTRIBUTE_STRUCTURE_DIALOG_TAG);
		}
	}

	/**
	 * Opens the edit ego alter attributes dialog.
	 * @param view 
	 */
	public void editEgoAlterTies(View view){
		SCCProperties.getInstance(this).setPropertyAlterDetailExpandEgoAlterTies(true); 
		DialogFragment editEgoAlterAttributesDialog = EditEgoAlterAttributesDialog.getInstance(this);
		editEgoAlterAttributesDialog.show(getSupportFragmentManager(), EDIT_EGO_ALTER_ATTRIBS_DIALOG_TAG);
	}

	/*
	 * Opens the dialog to edit alter-alter ties.
	 * @param view
	 */
	public void editAlterAlterTies(View view){
		DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
				findFragmentByTag(ADD_ALTER_ALTER_EVENT_DIALOG_TAG);
		if(callingDialog != null)
			callingDialog.dismiss();
		SCCProperties properties = SCCProperties.getInstance(this);
		properties.setPropertyAlterDetailExpandAlterAlterTies(true);
		boolean showAllNeigbors = properties.getPropertyAlterDetailShowAllNeighbors();
		if(showAllNeigbors){
			DialogFragment editAlterTiesDialog = EditAlterAlterTiesDialog.getInstance(this);
			editAlterTiesDialog.show(getSupportFragmentManager(), EDIT_ALTER_ALTER_TIES_DIALOG_TAG);
		} else {
			PersonalNetwork network = PersonalNetwork.getInstance(this);
			String selectedAlter = network.getSelectedAlter();
			String selectedSecondAlter = network.getSelectedSecondAlter();
			if(selectedAlter != null && !selectedAlter.equals(selectedSecondAlter)){
				DialogFragment editAlterAlterAttributesDialog = 
						EditAlterAlterAttributesDialog.getInstance(this);
				editAlterAlterAttributesDialog.show(getSupportFragmentManager(), 
						EDIT_ALTER_ALTER_ATTRIBS_DIALOG_TAG);
			}
		}
	}

	/**
	 * Opens the ego input file selection dialog.
	 * @param view
	 */
	public void loadSurvey(View view){
		DialogFragment dialog = SelectInputEgoFileDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), SELECT_INPUT_EGO_FILE_DIALOG_TAG);
	}

	/**
	 * Reads a questionnaire from an ego file.
	 * @param egoFile
	 */
	public void loadSurveyFromFile(File egoFile){
		EgonetQuestionnaireFile questionnairefile = new EgonetQuestionnaireFile(egoFile);
		Questionnaire questionnaire = Questionnaire.getInstance(this);
		questionnaire.initFromFile(questionnairefile);

		updatePersonalNetworkViews();
	}

	/**
	 * Creates a new attribute for ego, alter, ego-alter, or alter-alter.
	 * On click method activated by the create attribute button in the attribute list view.
	 * @param view
	 */
	public void addAttribute(View view){
		DialogFragment dialog = AddAttributeDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), ADD_ATTRIBUTE_DIALOG_TAG);
	}

	/**
	 * Opens the chose alter operation dialog. 
	 * 
	 *  Called from the respective button in the alter detail view. 
	 *  
	 * @param view
	 */
	public void choseAlterOperation(View view){
		if(PersonalNetwork.getInstance(this).getSelectedAlter() != null){
			DialogFragment dialog = ChoseAlterOperationDialog.getInstance(this);
			dialog.show(getSupportFragmentManager(), CHOSE_ALTER_OPERATION_DIALOG_TAG);
		}
	}

	/**
	 * Removes the currently selected alter. (Does nothing if there is no selected alter.)
	 * Called from the chose alter operations dialog.
	 * @param view
	 */
	public void removeAlter(View view){
		if(PersonalNetwork.getInstance(this).getSelectedAlter() != null){
			DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
					findFragmentByTag(CHOSE_ALTER_OPERATION_DIALOG_TAG);
			if(callingDialog != null)
				callingDialog.dismiss();
			DialogFragment dialog = ConfirmRemoveAlterDialog.getInstance(this);
			dialog.show(getSupportFragmentManager(), CONFIRM_REMOVE_ALTER_DIALOG_TAG);
		}
	}

	/**
	 * Renames the currently selected alter. (Does nothing if there is no selected alter.)
	 * 
	 * Called from the chose alter operations dialog.
	 * @param view
	 */
	public void renameAlter(View view){
		if(PersonalNetwork.getInstance(this).getSelectedAlter() != null){
			DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
					findFragmentByTag(CHOSE_ALTER_OPERATION_DIALOG_TAG);
			if(callingDialog != null)
				callingDialog.dismiss();
			DialogFragment dialog = RenameAlterDialog.getInstance(this);
			dialog.show(getSupportFragmentManager(), RENAME_ALTER_DIALOG_TAG);
		}
	}

	/**
	 * Updates local data (phone, calls, calendar events, etc)
	 * of the currently selected alter. (Does nothing if there is no selected alter.)
	 * 
	 * Called from the chose alter operations dialog.
	 * @param view
	 */
	public void updateLocalAlterData(View view){
		String alter = PersonalNetwork.getInstance(this).getSelectedAlter();
		if(alter != null){
			DialogFragment callingDialog = (DialogFragment) getSupportFragmentManager().
					findFragmentByTag(CHOSE_ALTER_OPERATION_DIALOG_TAG);
			if(callingDialog != null)
				callingDialog.dismiss();
			ImportContentProviderData.importDataForAlterByName(this, alter);
		}
		updatePersonalNetworkViews();
	}

	/**
	 * Removes the currently selected attribute.
	 * @param view
	 */
	public void removeAttribute(View view){
		if(PersonalNetwork.getInstance(this).getSelectedAttributeDomain() != null){
			DialogFragment dialog = ConfirmRemoveAttributeDialog.getInstance(this);
			dialog.show(getSupportFragmentManager(), CONFIRM_REMOVE_ATTRIBUTE_DIALOG_TAG);
		}
	}

	/*
	 * Opens the select output graphml file dialog.
	 */
	private void exportGraphML() {
		DialogFragment dialog = SelectOutputGraphMLFileDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), SELECT_OUTPUT_GRAPHML_FILE_DIALOG_TAG);
	}

	/*
	 * Opens the select output graphml file dialog.
	 */
	private void exportHistory2GraphML() {
		DialogFragment dialog = SelectHistoryOutputGraphMLFileDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), SELECT_HISTORY_OUTPUT_GRAPHML_FILE_DIALOG_TAG);
	}

	/*
	 * Opens the select output graphml file dialog.
	 */
	private void importHistoryFromGraphML() {
		DialogFragment dialog = SelectInputGraphMLFileDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), SELECT_INPUT_GRAPHML_FILE_DIALOG_TAG);
	}

	/*
	 * Opens the select output int file dialog.
	 */
	private void importEgonetInterview() {
		DialogFragment dialog = SelectInputIntFileDialog.getInstance(this);
		dialog.show(getSupportFragmentManager(), SELECT_INPUT_INT_FILE_DIALOG_TAG);
	}
	
	/**
	 * Reads an interview from an int file.
	 * @param intFile
	 */
	public void loadInterviewFromFile(File intFile){
		PersonalNetwork.getInstance(this).importEgonetInterview(intFile);
	}
	
	/**
	 * Writes the PersonalNetwork to the specified file in GraphML format.
	 * @param file
	 */
	public void exportToGraphMLFile(File file){
		FileWriter writer;
		try {
			writer = new FileWriter(file);
			PersonalNetwork.getInstance(this).writeNetworkHistory2GraphML(writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			//reportError(e.getMessage());
		}
	}

	/**
	 * Writes the PersonalNetworkHistory to the specified file in GraphML format.
	 * @param file
	 */
	//TODO: remove this method
	public void exportHistoryToGraphMLFile(File file){
		FileWriter writer;
		try {
			writer = new FileWriter(file);
			PersonalNetwork.getInstance(this).writeNetworkHistory2GraphML(writer);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			reportError(e.getMessage());
		}
	}

	/**
	 * Writes the PersonalNetworkHistory to the specified file in GraphML format.
	 * @param file
	 */
	public void importFromGraphMLFile(File file){
		PersonalNetwork.getInstance(this).importHistoryFromGraphML(file);
	}

	/**
	 * Called when an alter is selected by a click event in the alter list.
	 */
	public void onAlterSelected(String alterName) {
		PersonalNetwork.getInstance(this).setSelectedAlter(alterName);
		SCCProperties.getInstance(this).setPropertyShowDetailInSinglePaneView(true);
		switchToAlterView();
		updatePersonalNetworkViews();
	}

	/**
	 * Creates (if addAsNeighbor == true) or removes (else) a tie from focalActor to neighbor. 
	 */
	public void onNeighborSelected(String focalActor, String neighbor, boolean addAsNeighbor) {
		if(addAsNeighbor){
			PersonalNetwork.getInstance(this).addToLifetimeOfTie(TimeInterval.getRightUnboundedFromNow(),
					focalActor, neighbor);
		} else{
			PersonalNetwork.getInstance(this).removeTieAt(TimeInterval.getRightUnboundedFromNow(), 
					focalActor, neighbor);
		}
		updatePersonalNetworkViews();
	}

	/**
	 * Called when encountering a serious error. 
	 * @param message
	 */
	public void reportError(String message) {
		// TODO do something more intelligent
		//throw new RuntimeException(message);
	}

	/**
	 * Called when the user should be informed about something that did not work as expected.
	 * @param message
	 */
	public void reportWarning(String message) {
		// TODO do something more intelligent
	}

	/**
	 * Called when the user should be informed about something.
	 * @param message
	 */
	public void reportInfo(String message) {
		// TODO do something more intelligent
	}

	/**
	 * Called when a developer (but not the user) should be informed about something.
	 * @param message
	 */
	public void reportDebugMessage(String message) {
		// TODO do something more intelligent
	}


	public void pickAlterFromAddAlterDialog(View view){
		Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
		//pickContactIntent.setType(Phone.CONTENT_TYPE); // Show phone data
		pickContactIntent.setType(Contacts.CONTENT_TYPE); // show contact data
		//pickContactIntent.setType(Email.CONTENT_TYPE); // show email data
		try {
			startActivityForResult(pickContactIntent, PICK_CONTACT_FROM_ADD_ALTER_DIALOG_REQUEST_CODE);
			//result will be read in onActivityResult
		} catch (ActivityNotFoundException e) {
			reportInfo(e.getMessage());
		}		
	}

	@Override
	/*
	 * 
	 * (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		//this is the pick alter button in the survey view 
		//TODO will be changed somehow
		Button pickAlterButton = DynamicViewManager.getPickAlterButton();
		if(pickAlterButton != null && pickAlterButton == v){
			Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
			pickContactIntent.setType(Contacts.CONTENT_TYPE); // show contact data
			try {
				startActivityForResult(pickContactIntent, PICK_CONTACT_DURING_SURVEY_REQUEST_CODE);
				//result will be read in onActivityResult
			} catch (ActivityNotFoundException e) {
				//TODO: do something more intelligent
				reportError(e.getMessage());
			}
		}
	}

	@Override
	/*
	 * Processes the results of other activities started by startActivityForResult.
	 * (non-Javadoc)
	 * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int, android.content.Intent)
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check which request we're responding to
		if (requestCode == PICK_CONTACT_DURING_SURVEY_REQUEST_CODE) {
			// Make sure the request was successful
			if (resultCode == RESULT_OK) {
				Uri contactUri = data.getData();
				String[] projection = {Contacts._ID, Contacts.DISPLAY_NAME};
				//alternative: use Contacts.DISPLAY_NAME_PRIMARY;
				// CAUTION: The query() method should be called from a separate thread to avoid blocking
				// your app's UI thread. Consider using CursorLoader to perform the query.
				Cursor cursor = getContentResolver()
						.query(contactUri, projection, null, null, null);
				if(cursor == null || !cursor.moveToFirst())
					return;
				int nameColumn = cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME);
				String alterName = cursor.getString(nameColumn);
				cursor.close();
				PersonalNetwork network = PersonalNetwork.getInstance(this);
				network.addToLifetimeOfAlter(TimeInterval.getRightUnboundedFromNow(), alterName);
				Questionnaire.getInstance(this).addAlter(alterName);
				//do something with the _ID (link it to this alter etc) or with the email ...
				updatePersonalNetworkViews();
			}
		}
		if (requestCode == PICK_CONTACT_FROM_ADD_ALTER_DIALOG_REQUEST_CODE) {
			// Make sure the request was successful
			if (resultCode == RESULT_OK) {
				Uri contactUri = data.getData();
				String[] projection = {Contacts.DISPLAY_NAME};//use the photo id or uri
				Cursor cursor = getContentResolver()
						.query(contactUri, projection, null, null, null);
				if(cursor != null && cursor.moveToFirst()){//the only one
					int displayNameColumn = cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME);
					String alterName = cursor.getString(displayNameColumn);
					cursor.close();
					if(alterName != null){
						alterName = alterName.trim();
						ImportContentProviderData.importDataForAlterByName(this, alterName);
						onAlterSelected(alterName);
					}
				}
			}
		}
	}
	
}

