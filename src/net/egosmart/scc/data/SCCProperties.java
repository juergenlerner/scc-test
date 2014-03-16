package net.egosmart.scc.data;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.gui.EgoDetailViewFragment;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SCCProperties {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME_PREFIX = "egosmart_properties_db.";
    
	/*
	 * Key for the property whether ego attributes should be expanded in the 
	 * ego detail view.
	 */
	private static final String KEY_EGO_DETAIL_EXPAND_EGO_MEMOS = "key_ego_detail_expand_ego_memos";
	/*
	 * Key for the property whether ego attributes should be expanded in the 
	 * ego detail view.
	 */
	private static final String KEY_ALTER_DETAIL_EXPAND_ALTER_EVENTS = "key_alter_detail_expand_alter_memos";
	/*
	 * Key for the property whether ego attributes should be expanded in the 
	 * ego detail view.
	 */
	private static final String KEY_EGO_DETAIL_EXPAND_ATTRIBUTES = "key_ego_detail_expand_attributes";
	/*
	 * Key for the property whether ego-alter ties should be expanded in the 
	 * ego detail view.
	 */
	private static final String KEY_EGO_DETAIL_EXPAND_EGO_ALTER_TIES = "key_ego_detail_expand_ego_alter_ties";
	/*
	 * Key for the property whether all ego-alter ties should be shown in the 
	 * ego detail view (if true) or only the attributes of one selected ego alter tie.
	 */
	private static final String KEY_EGO_DETAIL_SHOW_ALL_ALTERS = "key_ego_detail_show_all_alters";
	/*
	 * Key for the property whether alter attributes should be expanded in the 
	 * alter detail view.
	 */
	private static final String KEY_ALTER_DETAIL_EXPAND_ATTRIBUTES = "key_alter_detail_expand_attributes";
	/*
	 * Key for the property whether ego-alter ties should be expanded in the 
	 * alter detail view.
	 */
	private static final String KEY_ALTER_DETAIL_EXPAND_EGO_ALTER_TIES = "key_alter_detail_expand_ego_alter_ties";
	/*
	 * Key for the property whether alter-alter ties should be expanded in the 
	 * alter detail view.
	 */
	private static final String KEY_ALTER_DETAIL_EXPAND_ALTER_ALTER_TIES = "key_alter_detail_expand_alter_alter_ties";
	/*
	 * Key for the property whether all alter-alter ties of the selected alter should be shown in the 
	 * alter detail view (if true) or rather the details of a selected second alter (if false)
	 */
	private static final String KEY_ALTER_DETAIL_SHOW_ALL_NEIGHBORS = "key_alter_detail_show_all_neighbors";
	/*
	 * Key for the property whether ego attributes should be expanded in the 
	 * attribute list view.
	 */
	private static final String KEY_ATTRIBUTE_LIST_EXPAND_EGO_ATTRIBUTES = "key_attribute_list_expand_ego_attributes";
	/*
	 * Key for the property whether alter attributes should be expanded in the 
	 * attribute list view.
	 */
	private static final String KEY_ATTRIBUTE_LIST_EXPAND_ALTER_ATTRIBUTES = "key_attribute_list_expand_alter_attributes";
	/*
	 * Key for the property whether ego-alter attributes should be expanded in the 
	 * attribute list view.
	 */
	private static final String KEY_ATTRIBUTE_LIST_EXPAND_EGO_ALTER_ATTRIBUTES = "key_attribute_list_expand_ego_alter_attributes";
	/*
	 * Key for the property whether alter-alter attributes should be expanded in the 
	 * attribute list view.
	 */
	private static final String KEY_ATTRIBUTE_LIST_EXPAND_ALTER_ALTER_ATTRIBUTES = "key_attribute_list_expand_alter_alter_attributes";

	/*
	 * Key for the boolean property whether the lifetimes of all alters should be shown
	 * in the history view or rather the detailed history of a selected alter.
	 */
	private static final String KEY_HISTORY_VIEW_SHOW_ALL_ALTERS = "key_history_view_show_all_alters";
	/*
	 * Key for the boolean property whether the detail view should be shown in a single pane view (portrait orientation);
	 *  otherwise (if the property is false) the master view / list view is to be shown. 
	 */
	private static final String KEY_SHOW_DETAIL_IN_SINGLE_PANE_VIEW = "key_show_detail_in_single_pane_view";	
	/*
	 * Key for the boolean property whether interview settings are "egonet-style interview"
	 * (if true) or "randomized interview" (if false). 
	 */
	private static final String KEY_INTERVIEW_SETTINGS_EGONET = "key_interview_settings_egonet";	
	/*
	 * Key to address the current layout settings - allowing to 
	 * retrieve these after restart. 
	 */
	private static final String KEY_LAST_VIEW_LABEL = "key_last_view_label";	
	/*
	 * Key to address the current layout settings. The top level view is only the ego, alter, or attribute view.
	 */
	private static final String KEY_LAST_TOP_LEVEL_VIEW_LABEL = "key_last_top_level_view_label";	
	/*
	 * Key to address the current layout settings - allowing to 
	 * remember which statistic view was visited.
	 */
	private static final String KEY_LAST_STATISTIC_VIEW = "key_last_statistics_view";
	/*
	 * Key to address the ideal percentage value for male statistics.
	 */
	private static final String KEY_STATISTIC_IDEAL_MALE = "key_statistic_ideal_male";
	/*
	 * Key to address the ideal percentage value for female statistics.
	 */
	private static final String KEY_STATISTIC_IDEAL_FEMALE ="key_statistic_ideal_female";
	/*
	 * Key to address the ideal graph density statistic.
	 */
	private static final String KEY_STATISTIC_IDEAL_GRAPH_DENSITY = "key_statistic_ideal_graph_density";
	/*
	 * Key to address the ideal components statistic.
	 */
	private static final String KEY_STATISTIC_IDEAL_COMPONENTS = "key_statistic_ideal_components";
	/*
	 * Values for boolean properties.
	 */
	private static final String VALUE_TRUE = "true";
	private static final String VALUE_FALSE = "false";
	
	/*
	 * Table holding statistics ideal values.
	 */
	private static final String STATISTICS_PROPERTIES_TABLE_NAME = "statistics_properties";
	private static final String STATISTICS_PROPERTIES_COL_KEY = "statistics_properties_key";
	private static final String STATISTICS_PROPERTIES_COL_VALUE="statistics_properties_value";
	private static final String STATISTICS_PROPERTIES_TABLE_CREATE_CMD = 
			"CREATE TABLE " + STATISTICS_PROPERTIES_TABLE_NAME + " (" +
			STATISTICS_PROPERTIES_COL_KEY + " TEXT, " +
			STATISTICS_PROPERTIES_COL_VALUE + " TEXT NOT NULL, " +
			"PRIMARY KEY (" + STATISTICS_PROPERTIES_COL_KEY + ") );";
	/*
	 * Table holding changeable properties of the app
	 */
	private static final String PROPERTIES_TABLE_NAME = "properties";
	private static final String PROPERTIES_COL_KEY = "properties_key";
	private static final String PROPERTIES_COL_VALUE = "properties_value";
    private static final String PROPERTIES_TABLE_CREATE_CMD =
            "CREATE TABLE " + PROPERTIES_TABLE_NAME + " (" +
            PROPERTIES_COL_KEY + " TEXT, " +
            PROPERTIES_COL_VALUE + " TEXT NOT NULL, " +
            "PRIMARY KEY (" + PROPERTIES_COL_KEY + ")  );";
	
    //Reference to the activity that uses these properties
	private SCCMainActivity activity;
	
	//The instance SCCProperties
    private static SCCProperties instance;
    
    //Reference to the database.
	private SQLiteDatabase database;
	
	/*
	 * Gets a reference to the database (if necessary creates it). This may trigger a call
	 * to onUpgrade which might take long.
	 */
	private SCCProperties(SCCMainActivity activity){
		this.activity = activity;
		SCCPropertiesDBOpenHelper helper = new SCCPropertiesDBOpenHelper(activity);
		database = helper.getWritableDatabase();
	}
	
	/**
	 * Returns the instance of PersonalNetwork.
	 */
	public static SCCProperties getInstance(SCCMainActivity activity){
		if(instance == null)
			instance = new SCCProperties(activity);
		return instance;
	}
	
	/**
	 * Sets the boolean property whether the detail view should be shown in a single pane view (portrait orientation);
	 *  otherwise (if the property is false) the master view / list view is to be shown.
	 */
	public void setPropertyShowDetailInSinglePaneView(boolean showDetail){
		setBooleanProperty(KEY_SHOW_DETAIL_IN_SINGLE_PANE_VIEW, showDetail);
	}
	
	/**
	 * Sets the boolean property whether the detail view should be shown in a single pane view (portrait orientation);
	 *  otherwise (if the property is false) the master view / list view is to be shown.
	 */
	public boolean getPropertyShowDetailInSinglePaneView(){
		return getBooleanProperty(KEY_SHOW_DETAIL_IN_SINGLE_PANE_VIEW);
	}
	
	/**
	 * Sets the last view label property to the given label.
	 */
	public void setPropertyLastViewLabel(String viewLabel){
		setStringProperty(KEY_LAST_VIEW_LABEL, viewLabel);
	}
	
	/**
	 * Returns the last view label.
	 */
	public String getPropertyLastViewLabel(){
		return getStringProperty(KEY_LAST_VIEW_LABEL);
	}
	
	/**
	 * Sets the last top level view label property to the given label.
	 */
	public void setPropertyLastTopLevelViewLabel(String viewLabel){
		setStringProperty(KEY_LAST_TOP_LEVEL_VIEW_LABEL, viewLabel);
	}
	
	/**
	 * Returns the last top level view label.
	 */
	public String getPropertyLastTopLevelViewLabel(){
		return getStringProperty(KEY_LAST_TOP_LEVEL_VIEW_LABEL);
	}
	
	/**
	 * Sets the last statistic view visited by the user.
	 */
	public void setPropertyLastStatisticView(String statisticView){
		setStringProperty(KEY_LAST_STATISTIC_VIEW, statisticView);
	}
	
	/**
	 * Returns the last statistic view visited.
	 */
	public String getPropertyLastStatisticsView() {
		return getStringProperty(KEY_LAST_STATISTIC_VIEW);
	}
	
	/**
	 * Sets the property whether interview settings point to "Egonet-style"
	 * interview (if true) or randomized interview (if false).
	 */
	public void setInterviewSettingsEgonet(boolean doEgonetInterview){
		setBooleanProperty(KEY_INTERVIEW_SETTINGS_EGONET, doEgonetInterview);
	}
	
	/**
	 * Gets the property whether interview settings point to "Egonet-style"
	 * interview (if true) or randomized interview (if false).
	 */
	public boolean getInterviewSettingsEgonet(){
		return getBooleanProperty(KEY_INTERVIEW_SETTINGS_EGONET);
	}
	
	/**
	 * Sets the property whether ego attributes should be expanded in the 
	 * attribute list view.
	 */
	public void setPropertyAttributeListExpandEgoAttributes(boolean expand){
		setBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_EGO_ATTRIBUTES, expand);
	}
	
	/**
	 * Returns the property whether ego attributes should be expanded in the 
	 * attribute list view.
	 */
	public boolean getPropertyAttributeListExpandEgoAttributes(){
		return getBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_EGO_ATTRIBUTES);
	}
	
	/**
	 * Sets the property whether alter attributes should be expanded in the 
	 * attribute list view.
	 */
	public void setPropertyAttributeListExpandAlterAttributes(boolean expand){
		setBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_ALTER_ATTRIBUTES, expand);
	}
	
	/**
	 * Returns the property whether alter attributes should be expanded in the 
	 * attribute list view.
	 */
	public boolean getPropertyAttributeListExpandAlterAttributes(){
		return getBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_ALTER_ATTRIBUTES);
	}
	
	/**
	 * Sets the property whether ego-alter attributes should be expanded in the 
	 * attribute list view.
	 */
	public void setPropertyAttributeListExpandEgoAlterAttributes(boolean expand){
		setBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_EGO_ALTER_ATTRIBUTES, expand);
	}
	
	/**
	 * Returns the property whether ego-alter attributes should be expanded in the 
	 * attribute list view.
	 */
	public boolean getPropertyAttributeListExpandEgoAlterAttributes(){
		return getBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_EGO_ALTER_ATTRIBUTES);
	}
	
	/**
	 * Sets the property whether alter-alter attributes should be expanded in the 
	 * attribute list view.
	 */
	public void setPropertyAttributeListExpandAlterAlterAttributes(boolean expand){
		setBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_ALTER_ALTER_ATTRIBUTES, expand);
	}
	
	/**
	 * Returns the property whether alter-alter attributes should be expanded in the 
	 * attribute list view.
	 */
	public boolean getPropertyAttributeListExpandAlterAlterAttributes(){
		return getBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_ALTER_ALTER_ATTRIBUTES);
	}
	
	/**
	 * Sets the property whether the lifetimes of all alters should be shown in the history view
	 * (or rather the detailed history of one selected alter).
	 */
	public void setPropertyHistoryViewShowAllAlters(boolean showAllAlters){
		setBooleanProperty(KEY_HISTORY_VIEW_SHOW_ALL_ALTERS, showAllAlters);
	}
	
	/**
	 * Returns the property whether the lifetimes of all alters should be shown in the history view
	 * (or rather the detailed history of one selected alter).
	 */
	public boolean getPropertyHistoryViewShowAllAlters(){
		return getBooleanProperty(KEY_HISTORY_VIEW_SHOW_ALL_ALTERS);
	}
	
	/**
	 * Sets the property whether ego memos should be expanded in the 
	 * ego detail view.
	 */
	public void setPropertyEgoDetailExpandEgoMemos(boolean expand){
		setBooleanProperty(KEY_EGO_DETAIL_EXPAND_EGO_MEMOS, expand);
	}
	
	/**
	 * Returns the property whether ego memos should be expanded in the 
	 * ego detail view.
	 */
	public boolean getPropertyEgoDetailExpandEgoMemos(){
		return getBooleanProperty(KEY_EGO_DETAIL_EXPAND_EGO_MEMOS);
	}
	
	/**
	 * Sets the property whether alter events should be expanded in the 
	 * alter detail view.
	 */
	public void setPropertyAlterDetailExpandAlterEvents(boolean expand){
		setBooleanProperty(KEY_ALTER_DETAIL_EXPAND_ALTER_EVENTS, expand);
	}
	
	/**
	 * Returns the property whether alter events should be expanded in the 
	 * alter detail view.
	 */
	public boolean getPropertyAlterDetailExpandAlterEvents(){
		return getBooleanProperty(KEY_ALTER_DETAIL_EXPAND_ALTER_EVENTS);
	}
	
	/**
	 * Sets the property whether ego attributes should be expanded in the 
	 * ego detail view.
	 */
	public void setPropertyEgoDetailExpandAttributes(boolean expand){
		setBooleanProperty(KEY_EGO_DETAIL_EXPAND_ATTRIBUTES, expand);
	}
	
	/**
	 * Returns the property whether ego attributes should be expanded in the 
	 * ego detail view.
	 */
	public boolean getPropertyEgoDetailExpandAttributes(){
		return getBooleanProperty(KEY_EGO_DETAIL_EXPAND_ATTRIBUTES);
	}
	
	/**
	 * Sets the property whether ego-alter ties should be expanded in the 
	 * ego detail view.
	 */
	public void setPropertyEgoDetailExpandEgoAlterTies(boolean expand){
		setBooleanProperty(KEY_EGO_DETAIL_EXPAND_EGO_ALTER_TIES, expand);
	}
	
	/**
	 * Returns the property whether ego-alter ties should be expanded in the 
	 * ego detail view.
	 */
	public boolean getPropertyEgoDetailExpandEgoAlterTies(){
		return getBooleanProperty(KEY_EGO_DETAIL_EXPAND_EGO_ALTER_TIES);
	}
	
	/**
	 * Sets the property whether all ego-alter ties should be shown in the 
	 * ego detail view (if true) or rather the attributes of a selected ego alter tie.
	 */
	public void setPropertyEgoDetailShowAllAlters(boolean show){
		setBooleanProperty(KEY_EGO_DETAIL_SHOW_ALL_ALTERS, show);
	}
	
	/**
	 * Returns the property whether all ego-alter ties should be shown in the 
	 * ego detail view (if true) or rather the attributes of a selected ego alter tie.
	 */
	public boolean getPropertyEgoDetailShowAllAlters(){
		return getBooleanProperty(KEY_EGO_DETAIL_SHOW_ALL_ALTERS);
	}
	
	/**
	 * Sets the property whether alter attributes should be expanded in the 
	 * alter detail view.
	 */
	public void setPropertyAlterDetailExpandAttributes(boolean expand){
		setBooleanProperty(KEY_ALTER_DETAIL_EXPAND_ATTRIBUTES, expand);
	}
	
	/**
	 * Returns the property whether alter attributes should be expanded in the 
	 * alter detail view.
	 */
	public boolean getPropertyAlterDetailExpandAttributes(){
		return getBooleanProperty(KEY_ALTER_DETAIL_EXPAND_ATTRIBUTES);
	}
	
	/**
	 * Sets the property whether ego-alter ties should be expanded in the 
	 * alter detail view.
	 */
	public void setPropertyAlterDetailExpandEgoAlterTies(boolean expand){
		setBooleanProperty(KEY_ALTER_DETAIL_EXPAND_EGO_ALTER_TIES, expand);
	}
	
	/**
	 * Returns the property whether ego-alter ties should be expanded in the 
	 * alter detail view.
	 */
	public boolean getPropertyAlterDetailExpandEgoAlterTies(){
		return getBooleanProperty(KEY_ALTER_DETAIL_EXPAND_EGO_ALTER_TIES);
	}
	
	/**
	 * Sets the property whether alter-alter ties should be expanded in the 
	 * alter detail view.
	 */
	public void setPropertyAlterDetailExpandAlterAlterTies(boolean expand){
		setBooleanProperty(KEY_ALTER_DETAIL_EXPAND_ALTER_ALTER_TIES, expand);
	}
	
	/**
	 * Returns the property whether alter-alter ties should be expanded in the 
	 * alter detail view.
	 */
	public boolean getPropertyAlterDetailExpandAlterAlterTies(){
		return getBooleanProperty(KEY_ALTER_DETAIL_EXPAND_ALTER_ALTER_TIES);
	}
	
	/**
	 * Sets the property whether all alter-alter ties of the selected alter should be shown in the 
	 * alter detail view (if true) or rather the details of a selected second alter (if false)
	 * 
	 * @param show
	 */
	public void setPropertyAlterDetailShowAllNeighbors(boolean show){
		setBooleanProperty(KEY_ALTER_DETAIL_SHOW_ALL_NEIGHBORS, show);
	}
	
	/**
	 * Returns the property whether all alter-alter ties of the selected alter should be shown in the 
	 * alter detail view (if true) or rather the details of a selected second alter (if false)
	 * 
	 */
	public boolean getPropertyAlterDetailShowAllNeighbors(){
		return getBooleanProperty(KEY_ALTER_DETAIL_SHOW_ALL_NEIGHBORS);
	}
	
	/**
	 * Sets the ideal value to male percentage in gender statistics
	 * 
	 * @param value
	 */
	public void setIdealValueMaleStatistics (float value) {
		setFloatStatisticProperty(KEY_STATISTIC_IDEAL_MALE, value);
	}
	
	/**
	 * Returns the current ideal male percentage in database.
	 * 
	 */
	public float getIdealValueMaleStatistics() {
		return getFloatStatisticProperty(KEY_STATISTIC_IDEAL_MALE);
	}
	
	/**
	 * Sets the ideal value to female percentage in gender statistics
	 * 
	 * @param value
	 */
	public void setIdealValueFemaleStatistics (float value) {
		setFloatStatisticProperty(KEY_STATISTIC_IDEAL_FEMALE, value);
	}
	
	/**
	 * Returns the current ideal female percentage in database.
	 * 
	 */
	public float getIdealValueFemaleStatistics() {
		return getFloatStatisticProperty(KEY_STATISTIC_IDEAL_FEMALE);
	}
	
	/**
	 * Sets the ideal value to graph density percentage in density statistics
	 * 
	 * @param value
	 */
	public void setIdealValueDensityStatistics (float value) {
		setFloatStatisticProperty(KEY_STATISTIC_IDEAL_GRAPH_DENSITY, value);
	}
	
	/**
	 * Returns the current ideal graph density in database.
	 * 
	 */
	public float getIdealValueDensityStatistics() {
		return getFloatStatisticProperty(KEY_STATISTIC_IDEAL_GRAPH_DENSITY);
	}
	
	/**
	 * Returns the current ideal graph components in database.
	 * 
	 */
	public int getIdealValueComponentsStatistics() {
		return getIntStatisticProperty(KEY_STATISTIC_IDEAL_COMPONENTS);
	}
	
	/**
	 * Sets the ideal value to number of components
	 * 
	 * @param value
	 */
	public void setIdealValueComponentsStatistics (int value) {
		setIntStatisticProperty(KEY_STATISTIC_IDEAL_COMPONENTS, value);
	}

	/*
	 * Sets the given int property to the given label in statistic properties
	 */
	private void setIntStatisticProperty(String propertyKey, int value){
        ContentValues values = new ContentValues();
        values.put(STATISTICS_PROPERTIES_COL_KEY, propertyKey);
        values.put(STATISTICS_PROPERTIES_COL_VALUE, Integer.toString(value));
        String where = STATISTICS_PROPERTIES_COL_KEY + " = ?";
        String[] whereArgs = {propertyKey};
        Cursor c = database.query(STATISTICS_PROPERTIES_TABLE_NAME, new String[]{STATISTICS_PROPERTIES_COL_VALUE}, 
        		where, whereArgs, null, null, null);
		if(c.moveToFirst()) //value is already set --> update
			database.update(STATISTICS_PROPERTIES_TABLE_NAME, values, where, whereArgs);
		else
			database.insert(STATISTICS_PROPERTIES_TABLE_NAME, null, values);
		c.close();
	}
	
	/*
	 * Returns the value of the given int property in statistics properties.
	 */
	private int getIntStatisticProperty(String propertyKey){
        String where = STATISTICS_PROPERTIES_COL_KEY + " = ?";
        String[] whereArgs = {propertyKey};
        Cursor c = database.query(STATISTICS_PROPERTIES_TABLE_NAME, new String[]{STATISTICS_PROPERTIES_COL_VALUE}, 
        		where, whereArgs, null, null, null);
		if(!c.moveToFirst()){
			c.close();
			//TODO: That makes sense?
			return 0;
		}
		int ret = c.getInt(c.getColumnIndexOrThrow(STATISTICS_PROPERTIES_COL_VALUE));
		c.close();
		return ret;
	}
	
	/*
	 * Sets the given float property to the given label in statistic properties
	 */
	private void setFloatStatisticProperty(String propertyKey, float value){
        ContentValues values = new ContentValues();
        values.put(STATISTICS_PROPERTIES_COL_KEY, propertyKey);
        values.put(STATISTICS_PROPERTIES_COL_VALUE, Float.toString(value));
        String where = STATISTICS_PROPERTIES_COL_KEY + " = ?";
        String[] whereArgs = {propertyKey};
        Cursor c = database.query(STATISTICS_PROPERTIES_TABLE_NAME, new String[]{STATISTICS_PROPERTIES_COL_VALUE}, 
        		where, whereArgs, null, null, null);
		if(c.moveToFirst()) //value is already set --> update
			database.update(STATISTICS_PROPERTIES_TABLE_NAME, values, where, whereArgs);
		else
			database.insert(STATISTICS_PROPERTIES_TABLE_NAME, null, values);
		c.close();
	}
	
	/*
	 * Returns the value of the given float property in statistics properties.
	 */
	private float getFloatStatisticProperty(String propertyKey){
        String where = STATISTICS_PROPERTIES_COL_KEY + " = ?";
        String[] whereArgs = {propertyKey};
        Cursor c = database.query(STATISTICS_PROPERTIES_TABLE_NAME, new String[]{STATISTICS_PROPERTIES_COL_VALUE}, 
        		where, whereArgs, null, null, null);
		if(!c.moveToFirst()){
			c.close();
			//TODO: That makes sense?
			return (Float) null;
		}
		float ret = c.getFloat(c.getColumnIndexOrThrow(STATISTICS_PROPERTIES_COL_VALUE));
		c.close();
		return ret;
	}
	
	/*
	 * Sets the given string property to the given label.
	 */
	private void setStringProperty(String propertyKey, String label){
        ContentValues values = new ContentValues();
        values.put(PROPERTIES_COL_KEY, propertyKey);
        values.put(PROPERTIES_COL_VALUE, label);
        String where = PROPERTIES_COL_KEY + " = ?";
        String[] whereArgs = {propertyKey};
        Cursor c = database.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
        		where, whereArgs, null, null, null);
		if(c.moveToFirst()) //value is already set --> update
			database.update(PROPERTIES_TABLE_NAME, values, where, whereArgs);
		else
			database.insert(PROPERTIES_TABLE_NAME, null, values);
		c.close();
	}
	
	/*
	 * Returns the value of the given string property.
	 */
	private String getStringProperty(String propertyKey){
        String where = PROPERTIES_COL_KEY + " = ?";
        String[] whereArgs = {propertyKey};
        Cursor c = database.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
        		where, whereArgs, null, null, null);
		if(!c.moveToFirst()){
			c.close();
			return null;
		}
		String ret = c.getString(c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE));
		c.close();
		return ret;
	}
	
	/*
	 * Sets the given boolean property to the given value.
	 */
	private void setBooleanProperty(String propertyKey, boolean value){
        ContentValues values = new ContentValues();
        values.put(PROPERTIES_COL_KEY, propertyKey);
        if(value)
        	values.put(PROPERTIES_COL_VALUE, VALUE_TRUE);
        else 
        	values.put(PROPERTIES_COL_VALUE, VALUE_FALSE);
        String where = PROPERTIES_COL_KEY + " = ?";
        String[] whereArgs = {propertyKey};
        Cursor c = database.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
        		where, whereArgs, null, null, null);
        if(c.moveToFirst()) //value is already set --> update
        	database.update(PROPERTIES_TABLE_NAME, values, where, whereArgs);
        else //insert for the first time
        	database.insert(PROPERTIES_TABLE_NAME, null, values);
        c.close();
	}
	
	/*
	 * Returns the value of the given boolean property.
	 */
	private boolean getBooleanProperty(String propertyKey){
        String where = PROPERTIES_COL_KEY + " = ?";
        String[] whereArgs = {propertyKey};
        Cursor c = database.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
        		where, whereArgs, null, null, null);
		if(!c.moveToFirst()){
			c.close();
			return false;
		}
		String value = c.getString(c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE));
		c.close();
		if(VALUE_TRUE.equals(value))
			return true;
		else
			return false;
	}
	
	private class SCCPropertiesDBOpenHelper extends SQLiteOpenHelper {

	    SCCPropertiesDBOpenHelper(Context context) {
	        super(context, DATABASE_NAME_PREFIX, null, DATABASE_VERSION);
	    }

	    @Override
	    public void onCreate(SQLiteDatabase db) {
	    	//create tables
	        db.execSQL(PROPERTIES_TABLE_CREATE_CMD);
	        db.execSQL(STATISTICS_PROPERTIES_TABLE_CREATE_CMD);
	        //SET INITIAL VALUES
	        //ego attributes in attribute list view
	        setInitialBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_EGO_ATTRIBUTES, true, db);
	        //alter attributes in attribute list view
	        setInitialBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_ALTER_ATTRIBUTES, true, db);
	        //ego-alter attributes in attribute list view
	        setInitialBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_EGO_ALTER_ATTRIBUTES, true, db);
	        //alter-alter attributes in attribute list view
	        setInitialBooleanProperty(KEY_ATTRIBUTE_LIST_EXPAND_ALTER_ALTER_ATTRIBUTES, true, db);
	        //ego memos in ego detail view
	        setInitialBooleanProperty(KEY_EGO_DETAIL_EXPAND_EGO_MEMOS, true, db);
	        //alter memos in alter detail view
	        setInitialBooleanProperty(KEY_ALTER_DETAIL_EXPAND_ALTER_EVENTS, true, db);
	        //ego attributes in ego detail view
	        setInitialBooleanProperty(KEY_EGO_DETAIL_EXPAND_ATTRIBUTES, false, db);
	        //ego-alter ties from ego view
	        setInitialBooleanProperty(KEY_EGO_DETAIL_EXPAND_EGO_ALTER_TIES, false, db);
	        //show all ego-alter ties from ego view
	        setInitialBooleanProperty(KEY_EGO_DETAIL_SHOW_ALL_ALTERS, true, db);
	        //alter attributes in alter detail view
	        setInitialBooleanProperty(KEY_ALTER_DETAIL_EXPAND_ATTRIBUTES, true, db);
	        //ego-alter ties in alter detail view
	        setInitialBooleanProperty(KEY_ALTER_DETAIL_EXPAND_EGO_ALTER_TIES, false, db);
	        //alter-alter ties in alter detail view
	        setInitialBooleanProperty(KEY_ALTER_DETAIL_EXPAND_ALTER_ALTER_TIES, false, db);
	        //all alters in history view
	        setInitialBooleanProperty(KEY_HISTORY_VIEW_SHOW_ALL_ALTERS, true, db);
	        //all neighbors of the selected alter in alter detail view
	        setInitialBooleanProperty(KEY_ALTER_DETAIL_SHOW_ALL_NEIGHBORS, true, db);
	        //master view / list view is shown in a single pane view
	        setInitialBooleanProperty(KEY_SHOW_DETAIL_IN_SINGLE_PANE_VIEW, true, db);
	        //initial interview setting is "randomized interview"
	        setInitialBooleanProperty(KEY_INTERVIEW_SETTINGS_EGONET, false, db);
	        //initial view label
	        setInitialStringProperty(KEY_LAST_VIEW_LABEL, SCCMainActivity.LAST_VIEW_LABEL_ALTER, db);
	        //initial view label
	        setInitialStringProperty(KEY_LAST_TOP_LEVEL_VIEW_LABEL, SCCMainActivity.LAST_VIEW_LABEL_ALTER, db);
	        //initial statistic view
	        setInitialStringProperty(KEY_LAST_STATISTIC_VIEW, SCCMainActivity.STATISTICS_GENDER,db);
	        //INITIAL VALUES FOR STATISTICS PROPERTIES
	        //initial ideal male percentage
	        //TODO: put the default values in a better place (R.strings maybe?)  
	        setInitialStatisticProperty(KEY_STATISTIC_IDEAL_MALE, "0.5" ,db);
	        //initial ideal female percentage
	        setInitialStatisticProperty(KEY_STATISTIC_IDEAL_FEMALE, "0.5", db);
	        //initial ideal graph density
	        setInitialStatisticProperty(KEY_STATISTIC_IDEAL_GRAPH_DENSITY, "0.5", db);
	        //initial ideal components
	        setInitialStatisticProperty(KEY_STATISTIC_IDEAL_COMPONENTS, "2", db);
	    }

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			
		}
		private void setInitialBooleanProperty(String key, boolean value, SQLiteDatabase db){
	        ContentValues values = new ContentValues();
	        values.put(PROPERTIES_COL_KEY, key);
	        String valueStr = VALUE_FALSE;
	        if(value)
	        	valueStr = VALUE_TRUE;
	        values.put(PROPERTIES_COL_VALUE, valueStr);
	        db.insert(PROPERTIES_TABLE_NAME, null, values);		
		}
		
		private void setInitialStringProperty(String key, String value, SQLiteDatabase db){
	        ContentValues values = new ContentValues();
	        values.put(PROPERTIES_COL_KEY, key);
	        values.put(PROPERTIES_COL_VALUE, value);
	        db.insert(PROPERTIES_TABLE_NAME, null, values);		
		}
		
		private void setInitialStatisticProperty(String key, String value, SQLiteDatabase db) {
			ContentValues values = new ContentValues();
			values.put(STATISTICS_PROPERTIES_COL_KEY,  key);
			values.put(STATISTICS_PROPERTIES_COL_VALUE, value);
			db.insert(STATISTICS_PROPERTIES_TABLE_NAME, null, values);
		}
		
	}
}
