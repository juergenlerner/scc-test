package net.egosmart.scc.data;

import java.util.HashMap;

import net.egosmart.scc.SCCMainActivity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

public class LayoutSQLite {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME_PREFIX = "layout_db.";

	// Reference to the activity that uses this personal network.
	private SCCMainActivity activity;

	// The instance of PersonalNetworkSQLite.
	private static LayoutSQLite instance;

	// Reference to the database.
	private SQLiteDatabase db;


	/*
	 * Table holding the coordinates.
	 */

	private static final String LAYOUT_TABLE_NAME = "layout";
	private static final String LAYOUT_COL_ALTER = "alter_name";
	private static final String LAYOUT_COL_X = "x_coordinate";
	private static final String LAYOUT_COL_Y = "y_coordinate";
	private static final String LAYOUT_TABLE_CREATE_CMD = "CREATE TABLE "
			+ LAYOUT_TABLE_NAME + " (" + LAYOUT_COL_ALTER + " TEXT,  "
			+ LAYOUT_COL_X + " REAL, " + LAYOUT_COL_Y + " REAL, "
			+ "PRIMARY KEY (" + LAYOUT_COL_ALTER + ") );";

	private LayoutSQLite(SCCMainActivity activity) {
		this.activity = activity;
		LayoutDBOpenHelper helper = new LayoutDBOpenHelper(activity);
		db = helper.getWritableDatabase();
	}

	private static final String PROPERTIES_TABLE_NAME = "properties";
	private static final String PROPERTIES_COL_KEY = "property_name";
	private static final String PROPERTIES_COL_VALUE = "property_value";
	private static final String PROPERTIES_TABLE_CREATE_CMD = "CREATE TABLE "
			+ PROPERTIES_TABLE_NAME + " (" + PROPERTIES_COL_KEY + " TEXT DEFAULT NAN, " 
			+ PROPERTIES_COL_VALUE + " REAL, "
			+ "PRIMARY KEY (" + PROPERTIES_COL_KEY + ") );";
	/**
	 * Returns the instance of PersonalNetwork.
	 */
	public static LayoutSQLite getInstance(SCCMainActivity activity) {
		if (instance == null)
			instance = new LayoutSQLite(activity);
		return instance;
	}

	public void setCoordinates(HashMap<String, Float[]> coordinates) {
		if (coordinates == null || coordinates.isEmpty()) {
			return;
		}
		db.beginTransaction();
		try {
			HashMap<String, Float[]> dbCoordinates = getCoordinates();

			for (String node : coordinates.keySet()) {
				Float[] c = coordinates.get(node);
				ContentValues values = new ContentValues();
				values.put(LAYOUT_COL_ALTER, node);
				values.put(LAYOUT_COL_X, c[0]);
				values.put(LAYOUT_COL_Y, c[1]);
				String selection = LAYOUT_COL_ALTER + " = ?";
				String[] selectionArgs = { node };
				if (dbCoordinates.containsKey(node)) {
					db.update(LAYOUT_TABLE_NAME, values, selection,
							selectionArgs);
				} else {
					db.insert(LAYOUT_TABLE_NAME, null, values);
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		setTime(System.currentTimeMillis());
	}

	public void setTime(long time) {
		//TODO: why not put the time into the table?
		Float t = new Float(time);
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, "last_layout");
		values.put(PROPERTIES_COL_VALUE, t);
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = { "last_layout"};
		if(getTime()>0){
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		} else {
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		}
	}
	
	
	/**
	 * writes the scale variable of the layout into the database
	 * @param scale
	 */
	public void setScale(float scale){
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, "layout_scale");
		values.put(PROPERTIES_COL_VALUE, scale);
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = { "layout_scale"};
		if(getScale()>0){
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		} else {
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		}
	}
	
	
	/**
	 * writes the translation variables into the database
	 * @param dx
	 * @param dy
	 */
	public void setTranslate(float dx, float dy){
		ContentValues valuesX = new ContentValues();
		ContentValues valuesY = new ContentValues();
		valuesX.put(PROPERTIES_COL_KEY, "layout_xTrans");
		valuesY.put(PROPERTIES_COL_KEY, "layout_yTrans");
		valuesX.put(PROPERTIES_COL_VALUE, dx);
		valuesY.put(PROPERTIES_COL_VALUE, dy);
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgsX = { "layout_xTrans"};
		String[] selectionArgsY = { "layout_yTrans"};
		if(getTranslate() != null){
			db.update(PROPERTIES_TABLE_NAME, valuesX, selection, selectionArgsX);
			db.update(PROPERTIES_TABLE_NAME, valuesY, selection, selectionArgsY);
		} else {
			db.insert(PROPERTIES_TABLE_NAME, null, valuesX);
			db.insert(PROPERTIES_TABLE_NAME, null, valuesY);
		}
	}

	
	/**
	 * Returns the map of all coordinates of the layout.
	 */
	public HashMap<String, Float[]> getCoordinates() {
		Cursor result = db.query(LAYOUT_TABLE_NAME, new String[] {
				LAYOUT_COL_ALTER, LAYOUT_COL_X, LAYOUT_COL_Y }, null, null,
				null, null, null);
		HashMap<String, Float[]> coordinates = new HashMap<String, Float[]>();
		int col_of_name = result.getColumnIndex(LAYOUT_COL_ALTER);
		int col_of_X = result.getColumnIndex(LAYOUT_COL_X);
		int col_of_Y = result.getColumnIndex(LAYOUT_COL_Y);
		if (result.moveToFirst() && col_of_name >= 0 && col_of_X >= 0
				&& col_of_Y >= 0) {
			while (!result.isAfterLast()) {
				Float x = result.getFloat(col_of_X);
				Float y = result.getFloat(col_of_Y);
				coordinates.put(result.getString(col_of_name), new Float[] { x,
						y });
				result.moveToNext();
			}
		}
		result.close();
		return coordinates;
	}

	/**
	 * returns the time of the last layout calculation or 0 if there was none
	 * @return
	 */
	public long getTime() {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {"last_layout"};
		Cursor result = db.query(PROPERTIES_TABLE_NAME, new String[] {PROPERTIES_COL_KEY, PROPERTIES_COL_VALUE}, selection, selectionArgs,
				null, null, null);
		int col_of_value = result.getColumnIndex(PROPERTIES_COL_VALUE);
		long time = 0;
		if(result.moveToFirst() && col_of_value >=0){
			time = result.getLong(col_of_value);
		}
		result.close();
		return time;
	}

	public float getScale() {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {"layout_scale"};
		Cursor result = db.query(PROPERTIES_TABLE_NAME, new String[] {PROPERTIES_COL_KEY, PROPERTIES_COL_VALUE}, selection, selectionArgs, null, null, null);
		int col_of_value = result.getColumnIndex(PROPERTIES_COL_VALUE);
		float scale = 1f;
		if(result.moveToFirst() && col_of_value >= 0){
			scale = result.getFloat(col_of_value);
		} else {
			return 0;
		}
		result.close();
		return scale;
	}
	
	public float[] getTranslate() {
		float[] translate = new float[2];
		String selection = PROPERTIES_COL_KEY + " = ? OR " + PROPERTIES_COL_KEY + " = ?";
		String layoutx = "layout_xTrans";
		String layouty = "layout_yTrans";
		String[] selectionArgs = { layoutx, layouty};
		Cursor result = db.query(PROPERTIES_TABLE_NAME, new String[] {PROPERTIES_COL_KEY, PROPERTIES_COL_VALUE}, selection, selectionArgs, null, null, null);
		int col_of_key = result.getColumnIndex(PROPERTIES_COL_KEY);
		int col_of_value = result.getColumnIndex(PROPERTIES_COL_VALUE);
		float xTrans = 0f;
		float yTrans = 0f;
		if(result.moveToFirst() && col_of_value >=0 ){
			if(result.getString(col_of_key).equals("layout_xTrans")){
				xTrans = result.getFloat(col_of_value);
			} else if(result.getString(col_of_key).equals("layout_yTrans")){
				yTrans = result.getFloat(col_of_value);
			} 
			if(result.moveToNext()){
				if(result.getString(col_of_key).equals("layout_xTrans")){
					xTrans = result.getFloat(col_of_value);
				} else if(result.getString(col_of_key).equals("layout_yTrans")){
					yTrans = result.getFloat(col_of_value);
				}
			}
		} else { 
			return null;
		}
		translate[0] = xTrans;
		translate[1] = yTrans;
		return translate;
	}
	
	public void resetLayout(){
		setScale(1f);
		setTranslate(0f, 0f);
	}
	private class LayoutDBOpenHelper extends SQLiteOpenHelper {

		LayoutDBOpenHelper(Context context) {
			super(context, DATABASE_NAME_PREFIX, null, DATABASE_VERSION);
		}

		/**
		 * Sets the coordinates for the layout.
		 */

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(LAYOUT_TABLE_CREATE_CMD);
			db.execSQL(PROPERTIES_TABLE_CREATE_CMD);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}
	}
}
