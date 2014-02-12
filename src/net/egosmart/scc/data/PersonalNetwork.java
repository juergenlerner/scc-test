/**
 * 
 */
package net.egosmart.scc.data;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import net.egosmart.scc.R;
import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.collect.EgonetQuestionnaireFile;
import net.egosmart.scc.collect.Question;
import net.egosmart.scc.collect.Questionnaire;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;
import android.util.Xml;

/**
 * 
 * Implementation of PersonalNetworkHistory with an SQLite database. All changes are automatically permanent.
 *
 * Represents a time-dependent attributed graph consisting of a focal person (called "ego") and persons
 * known to ego (called "alters"). 
 * 
 * The alters are the nodes of the graph. Alters are referenced by unique Strings, the alter names. 
 * Alter names have no leading or trailing white spaces; null and empty Strings are not allowed as alter names.
 * 
 * The ties are ordered pairs of different alters. If (a,b) is a tie in the network then (b,a) is also a tie.
 * (Thus, the graph is symmetric directed and has no loops.) No multiple ties connecting the same 
 * ordered pair are allowed.
 * 
 * Dyads are ordered pairs of different alters. However, a dyad (a,b) can be in the network independent of
 * whether a and b are adjacent or not. Dyads allow to associate attributes with pairs of alters 
 * even if these are not connected. 
 * 
 * Attributes can be defined for ego, alters, ego-alter dyads and alter-alter dyads. 
 * They are partial functions mapping from these sets to 
 * a range of allowed values. The allowed values are determined by the type of the attribute which can be 
 * NUMBER (meaning double values), TEXT (arbitrary strings), or CHOICE 
 * (a specified finite set of strings). 
 * 
 * Attributes are referenced by their names (string values that are not null, have no leading or trailing
 * white spaces and are not empty strings). Attribute values are always given as strings that are not null, 
 * have no leading or trailing white spaces and are not empty strings. Attribute values may be undefined
 * for some elements in which case the getter methods return a special string "N/A". Setting a value that
 * is null or the empty string or equal to "N/A" removes the attribute value for the given element.
 * 
 * Alter-alter dyad attributes have a direction type which is SYMMETRIC or ASYMMETRIC. 
 * For SYMMETRIC attributes if holds that if (a,b) has an associated value, 
 * then (b,a) has the same associated value.
 * 
 * Ego-alter dyad attributes can also be SYMMETRIC or ASYMMETRIC with the same meaning as for alter-alter
 * dyads; in addition, they can have the direction type OUT (meaning values can only be attached to the 
 * dyad from ego to alter) or IN (meaning values can only be attached to the dyad from alter to ego).
 * 
 * Attribute types, allowed choices (if applies), direction types (if applies), and descriptions are always 
 * the same for all points in time. Description and choices might be updated later but these updates also affece
 * the past. Only values can truely change over time, i.e., the history saves potentially different values for
 * different subsets of the lifetime.
 * 
 * All ego, alter, ego-alter, or alter-alter attributes have a dynamic type which is either STATE
 * or EVENT. The values of event type attributes can only be set for time points.
 * 
 * @author juergen
 *
 */
public class PersonalNetwork{

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME_PREFIX = "egosmart_history_db.";

	/*
	 * String constants for column names used in history tables.
	 */
	//interval start time
	private static final String COL_TIME_START = "time_start";
	//interval end time
	private static final String COL_TIME_END = "time_end";

	//column name holding a unique id for the row in history or in value files
	private static final String COL_DATUM_ID = "datum_id";

	/*
	 * Table holding changeable properties of the network
	 */
	private static final String PROPERTIES_TABLE_NAME = "properties";
	private static final String PROPERTIES_COL_KEY = "properties_key";
	private static final String PROPERTIES_COL_VALUE = "properties_value";
	private static final String PROPERTIES_TABLE_CREATE_CMD =
			"CREATE TABLE " + PROPERTIES_TABLE_NAME + " (" +
					PROPERTIES_COL_KEY + " TEXT, " +
					PROPERTIES_COL_VALUE + " TEXT NOT NULL, " +
					"PRIMARY KEY (" + PROPERTIES_COL_KEY + ")  );";

	//////////////////////////////////////////////////////////////////////////
	//ALTERS
	//////////////////////////////////////////////////////////////////////////
	/*
	 * Table holding the unique names of alters. 
	 * Maps alter names to system ids if available, if not maps to NOID.
	 * An alter in this table might have an empty lifetime.
	 */
	private static final String ALTERS_TABLE_NAME = "alters";
	protected static final String ALTERS_COL_NAME = "alter_name";
	private static final String ALTERS_TABLE_CREATE_CMD =
			"CREATE TABLE " + ALTERS_TABLE_NAME + " (" +
					ALTERS_COL_NAME + " TEXT, " +
					"PRIMARY KEY (" + ALTERS_COL_NAME + ") " +
					" );";

	/*
	 * Table holding the lifetimes of alters. 
	 * An alter that does not appear in this table has an empty lifetime.
	 */
	private static final String ALTERS_HISTORY_TABLE_NAME = "alters_history";
	private static final String ALTERS_HISTORY_TABLE_CREATE_CMD =
			"CREATE TABLE " + ALTERS_HISTORY_TABLE_NAME + " (" +
					ALTERS_COL_NAME + " TEXT, " +
					COL_TIME_START + " NUMERIC, " +
					COL_TIME_END + " NUMERIC, " +
					COL_DATUM_ID + " NUMERIC, " +
					"FOREIGN KEY (" + ALTERS_COL_NAME + ") " +
					"REFERENCES " + ALTERS_TABLE_NAME + " (" + ALTERS_COL_NAME + ")"+
					" );";

	//////////////////////////////////////////////////////////////////////////
	//TIES
	//////////////////////////////////////////////////////////////////////////
	/*
	 * Table holding the directed ties which are pairs of different alters (source, target);
	 * both referencing alter names. The ordered pairs (source, target) are unique.
	 */
	private static final String TIES_TABLE_NAME = "ties";
	protected static final String DYADS_COL_SOURCE = "source";
	protected static final String DYADS_COL_TARGET = "target";
	private static final String TIES_TABLE_CREATE_CMD =
			"CREATE TABLE " + TIES_TABLE_NAME + " (" +
					DYADS_COL_SOURCE + " TEXT REFERENCES " + ALTERS_TABLE_NAME + " (" + ALTERS_COL_NAME + "), " +
					DYADS_COL_TARGET + " TEXT REFERENCES " + ALTERS_TABLE_NAME + " (" + ALTERS_COL_NAME + "), " +
					"PRIMARY KEY (" + DYADS_COL_SOURCE + ", " + DYADS_COL_TARGET + ")  );";

	/*
	 * Table holding the lifetimes of ties. 
	 * A tie that does not appear in this table has an empty lifetime.
	 */
	private static final String TIES_HISTORY_TABLE_NAME = "ties_history";
	private static final String TIES_HISTORY_TABLE_CREATE_CMD =
			"CREATE TABLE " + TIES_HISTORY_TABLE_NAME + " (" +
					DYADS_COL_SOURCE + " TEXT, " +
					DYADS_COL_TARGET + " TEXT, " +
					COL_TIME_START + " NUMERIC, " +
					COL_TIME_END + " NUMERIC, " +
					COL_DATUM_ID + " NUMERIC, " +
					"FOREIGN KEY (" + DYADS_COL_SOURCE + "," + DYADS_COL_TARGET + ") " +
					"REFERENCES " + TIES_TABLE_NAME + " (" + DYADS_COL_SOURCE + "," + DYADS_COL_TARGET + ") " +
					" );";

	//////////////////////////////////////////////////////////////////////////
	//ALTER-ALTER DYADS
	//////////////////////////////////////////////////////////////////////////
	/*
	 * Table holding the directed alter-alter dyads which are pairs of different alters (source, target);
	 * both referencing alter names. The ordered pairs (source, target) are unique.
	 */
	private static final String ALTER_ALTER_DYADS_TABLE_NAME = "alter_alter_dyads";
	private static final String ALTER_ALTER_DYADS_TABLE_CREATE_CMD =
			"CREATE TABLE " + ALTER_ALTER_DYADS_TABLE_NAME + " (" +
					DYADS_COL_SOURCE + " TEXT REFERENCES " + ALTERS_TABLE_NAME + " (" + ALTERS_COL_NAME + "), " +
					DYADS_COL_TARGET + " TEXT REFERENCES " + ALTERS_TABLE_NAME + " (" + ALTERS_COL_NAME + "), " +
					"PRIMARY KEY (" + DYADS_COL_SOURCE + ", " + DYADS_COL_TARGET + ")  );";

	/*
	 * Table holding the lifetimes of alter-alter dyads. 
	 * A dyad that does not appear in this table has an empty lifetime.
	 */
	private static final String ALTER_ALTER_DYADS_HISTORY_TABLE_NAME = "alter_alter_dyads_history";
	private static final String ALTER_ALTER_DYADS_HISTORY_TABLE_CREATE_CMD =
			"CREATE TABLE " + ALTER_ALTER_DYADS_HISTORY_TABLE_NAME + " (" +
					DYADS_COL_SOURCE + " TEXT, " +
					DYADS_COL_TARGET + " TEXT, " +
					COL_TIME_START + " NUMERIC, " +
					COL_TIME_END + " NUMERIC, " +
					COL_DATUM_ID + " NUMERIC, " +
					"FOREIGN KEY (" + DYADS_COL_SOURCE + "," + DYADS_COL_TARGET + ") " +
					"REFERENCES " + ALTER_ALTER_DYADS_TABLE_NAME + " (" + DYADS_COL_SOURCE + "," + DYADS_COL_TARGET + ") " +
					" );";

	//////////////////////////////////////////////////////////////////////////
	//COLMN NAMES FOR ATTRIBUTES
	//////////////////////////////////////////////////////////////////////////

	//Name of the columns for primary attributes 
	protected static final String ATTRIBUTES_COL_NAME = "attribute_name";
	private static final String ATTRIBUTES_COL_DESCRIPTION = "attribute_description";
	private static final String ATTRIBUTES_COL_VALUE_TYPE = "attribute_value_type";
	protected static final String ATTRIBUTES_COL_DIRECTION_TYPE = "attribute_direction_type";
	private static final String ATTRIBUTES_COL_DYNAMIC_TYPE = "attribute_dynamic_type";
	private static final String ATTRIBUTES_COL_VALUE = "attribute_value";
	private static final String ATTRIBUTES_COL_CHOICE = "attribute_choice";

	//column name holding the domain of a secondary attribute (which are all in one table)
	private static final String ATTRIBUTES_COL_DOMAIN = "attribute_domain";
	//Names for columns for secondary attributes
	private static final String SECONDARY_ATTRIBUTES_COL_NAME = "secondary_attribute_name";
	//take the column names for primary attributes for: value_type, description, value, choice
	
	///////////////////////////////////////////////////////////////////////////
	// SECONDARY ATTRIBUTES (ONE TABLE FOR ALL DOMAINS, SINCE INSTANCES ARE IDENTIFIED BY ID)
	///////////////////////////////////////////////////////////////////////////
	
	/*
	 * Table holding the names of secondary attributes, their types, and description (free text).
	 * Secondary attributes are available for specific primary attributes from specific domains
	 */
	private static final String SECONDARY_ATTRIBS_NAMES_TABLE_NAME = "secondary_attributes_names";
	private static final String SECONDARY_ATTRIBS_NAMES_TABLE_CREATE_CMD =
			"CREATE TABLE " + SECONDARY_ATTRIBS_NAMES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_DOMAIN + " TEXT, " + //this is the domain of the primary attribute
					ATTRIBUTES_COL_NAME + " TEXT, " + //this is the name of the primary attribute
					SECONDARY_ATTRIBUTES_COL_NAME + " TEXT, " + //this is the name of the secondary attribute
					ATTRIBUTES_COL_DESCRIPTION + " TEXT, " +
					ATTRIBUTES_COL_VALUE_TYPE + " INT, " +
					"PRIMARY KEY (" + ATTRIBUTES_COL_DOMAIN + "," + ATTRIBUTES_COL_NAME + "," + SECONDARY_ATTRIBUTES_COL_NAME + ")  );";

	/*
	 * Table holding the values of secondary attributes. Note that the object (e.g., the event)
	 * is identified by the datum id.
	 */
	private static final String SECONDARY_ATTRIBS_VALUES_TABLE_NAME = "secondary_attributes_values";
	private static final String SECONDARY_ATTRIBS_VALUES_TABLE_CREATE_CMD = 
			"CREATE TABLE " + SECONDARY_ATTRIBS_VALUES_TABLE_NAME + " (" +
					SECONDARY_ATTRIBUTES_COL_NAME + " TEXT, " +
					COL_DATUM_ID + " TEXT, " +
					ATTRIBUTES_COL_VALUE + " TEXT "  +
					" );";	
	
	/*
	 * Table holding the allowed values for the secondary attributes of type CHOICE. An arbitrary 
	 * number of allowed values can be associated with any of these attributes.
	 */
	private static final String SECONDARY_ATTRIBS_CHOICES_TABLE_NAME = "secondary_attributes_choices";
	private static final String SECONDARY_ATTRIBS_CHOICES_TABLE_CREATE_CMD =
			"CREATE TABLE " + SECONDARY_ATTRIBS_CHOICES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_DOMAIN + " TEXT, " + //this is the domain of the primary attribute
					ATTRIBUTES_COL_NAME + ", " +
					SECONDARY_ATTRIBUTES_COL_NAME + ", " +
					ATTRIBUTES_COL_CHOICE + " TEXT   );";

	
	//////////////////////////////////////////////////////////////////////////
	//EGO ATTRIBUTES
	//////////////////////////////////////////////////////////////////////////
	/*
	 * Table holding the names of ego attributes, their types, and description (free text).
	 */
	private static final String EGO_ATTRIBS_NAMES_TABLE_NAME = "ego_attributes_names";
	private static final String EGO_ATTRIBS_NAMES_TABLE_CREATE_CMD =
			"CREATE TABLE " + EGO_ATTRIBS_NAMES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT, " +
					ATTRIBUTES_COL_DESCRIPTION + " TEXT, " +
					ATTRIBUTES_COL_VALUE_TYPE + " INT, " +
					ATTRIBUTES_COL_DYNAMIC_TYPE + " TEXT, " +
					"PRIMARY KEY (" + ATTRIBUTES_COL_NAME + ")  );";

	/*
	 * Table holding the time-varying values of ego attributes. 
	 */
	private static final String EGO_ATTRIBS_VALUES_TABLE_NAME = "ego_attributes_values";
	private static final String EGO_ATTRIBS_VALUES_TABLE_CREATE_CMD = 
			"CREATE TABLE " + EGO_ATTRIBS_VALUES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT, " +
					ATTRIBUTES_COL_VALUE + " TEXT, "  +
					COL_TIME_START + " NUMERIC, " +
					COL_TIME_END + " NUMERIC, " +
					COL_DATUM_ID + " NUMERIC, " +
					"FOREIGN KEY (" + ATTRIBUTES_COL_NAME + ") " +
					"REFERENCES " + EGO_ATTRIBS_NAMES_TABLE_NAME + " (" + ATTRIBUTES_COL_NAME + ")"+
					" );";

	/*
	 * Table holding the allowed values for the ego attributes of type CHOICE. An arbitrary 
	 * number of allowed values can be associated with any of these attributes.
	 */
	private static final String EGO_ATTRIBS_CHOICES_TABLE_NAME = "ego_attributes_choices";
	private static final String EGO_ATTRIBS_CHOICES_TABLE_CREATE_CMD =
			"CREATE TABLE " + EGO_ATTRIBS_CHOICES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT REFERENCES " 
					+ EGO_ATTRIBS_NAMES_TABLE_NAME + " (" + ATTRIBUTES_COL_NAME + "), " +
					ATTRIBUTES_COL_CHOICE + " TEXT   );";

	
	//////////////////////////////////////////////////////////////////////////
	//ALTER ATTRIBUTES
	//////////////////////////////////////////////////////////////////////////
	/*
	 * Table holding the names of alter attributes, their types, and description (free text).
	 */
	private static final String ALTER_ATTRIBS_NAMES_TABLE_NAME = "alter_attributes_names";
	private static final String ALTER_ATTRIBS_NAMES_TABLE_CREATE_CMD =
			"CREATE TABLE " + ALTER_ATTRIBS_NAMES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT, " +
					ATTRIBUTES_COL_DESCRIPTION + " TEXT, " +
					ATTRIBUTES_COL_VALUE_TYPE + " INT, " +
					ATTRIBUTES_COL_DYNAMIC_TYPE + " TEXT, " +
					"PRIMARY KEY (" + ATTRIBUTES_COL_NAME + ")  );";

	/*
	 * Table holding the time-varying values of alter attributes. 
	 */
	private static final String ALTER_ATTRIBS_VALUES_TABLE_NAME = "alter_attributes_values";
	private static final String ALTER_ATTRIBS_VALUES_TABLE_CREATE_CMD = 
			"CREATE TABLE " + ALTER_ATTRIBS_VALUES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT REFERENCES " 
					+ ALTER_ATTRIBS_NAMES_TABLE_NAME + " (" + ATTRIBUTES_COL_NAME + "), " +
					ALTERS_COL_NAME + " TEXT REFERENCES " 
					+ ALTERS_TABLE_NAME + " (" + ALTERS_COL_NAME + "), " +		
					ATTRIBUTES_COL_VALUE + " TEXT, "  +
					COL_TIME_START + " NUMERIC, " +
					COL_TIME_END + " NUMERIC, " +
					COL_DATUM_ID + " NUMERIC " +
					" );";

	/*
	 * Table holding the allowed values for the alter attributes of type CHOICE. An arbitrary 
	 * number of allowed values can be associated with any of these attributes.
	 */
	private static final String ALTER_ATTRIBS_CHOICES_TABLE_NAME = "alter_attributes_choices";
	private static final String ALTER_ATTRIBS_CHOICES_TABLE_CREATE_CMD =
			"CREATE TABLE " + ALTER_ATTRIBS_CHOICES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT REFERENCES " 
					+ ALTER_ATTRIBS_NAMES_TABLE_NAME + " (" + ATTRIBUTES_COL_NAME + "), " +
					ATTRIBUTES_COL_CHOICE + " TEXT   );";

	//////////////////////////////////////////////////////////////////////////
	//EGO-ALTER ATTRIBUTES
	//////////////////////////////////////////////////////////////////////////
	/*
	 * Table holding the names of ego-alter dyad attributes, 
	 * their value types, direction types (can be symmetric, asymmetric, out or in), 
	 * dynamic types, and description (free text).
	 */
	private static final String EGO_ALTER_ATTRIBS_NAMES_TABLE_NAME = "ego_alter_attributes_names";
	private static final String EGO_ALTER_ATTRIBS_NAMES_TABLE_CREATE_CMD =
			"CREATE TABLE " + EGO_ALTER_ATTRIBS_NAMES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT, " +
					ATTRIBUTES_COL_DESCRIPTION + " TEXT, " +
					ATTRIBUTES_COL_VALUE_TYPE + " INT, " +
					ATTRIBUTES_COL_DYNAMIC_TYPE + " TEXT, " +
					ATTRIBUTES_COL_DIRECTION_TYPE + " TEXT, " +
					"PRIMARY KEY (" + ATTRIBUTES_COL_NAME + ")  );";

	/*
	 * Table holding the values of ego-alter dyad attributes. Each triple (attribute name, alter name, direction) 
	 * has at most one value associated with it for any point in time; 
	 * the special value "N/A" is not explicitly stored.
	 * Direction can be IN or OUT.
	 * Symmetric attributes are always stored in the direction OUT and IN (similar behavior to symmetric
	 * alter-alter dyad attributes.
	 */
	private static final String EGO_ALTER_ATTRIBS_VALUES_TABLE_NAME = "ego_alter_attributes_values";
	private static final String EGO_ALTER_ATTRIBS_VALUES_TABLE_CREATE_CMD = 
			"CREATE TABLE " + EGO_ALTER_ATTRIBS_VALUES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT REFERENCES " 
					+ ALTER_ATTRIBS_NAMES_TABLE_NAME + " (" + ATTRIBUTES_COL_NAME + "), " +
					ALTERS_COL_NAME + " TEXT REFERENCES " 
					+ ALTERS_TABLE_NAME + " (" + ALTERS_COL_NAME + "), " +		
					ATTRIBUTES_COL_DIRECTION_TYPE + " TEXT, "  +
					ATTRIBUTES_COL_VALUE + " TEXT, "  +
					COL_TIME_START + " NUMERIC, " +
					COL_TIME_END + " NUMERIC, " +
					COL_DATUM_ID + " NUMERIC " +
					" );";

	/*
	 * Table holding the allowed values for the ego-alter dyad attributes of type CHOICE. An arbitrary 
	 * number of allowed values can be associated with any of these attributes.
	 */
	private static final String EGO_ALTER_ATTRIBS_CHOICES_TABLE_NAME = "ego_alter_attributes_choices";
	private static final String EGO_ALTER_ATTRIBS_CHOICES_TABLE_CREATE_CMD =
			"CREATE TABLE " + EGO_ALTER_ATTRIBS_CHOICES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT REFERENCES " 
					+ EGO_ALTER_ATTRIBS_NAMES_TABLE_NAME + " (" + ATTRIBUTES_COL_NAME + "), " +
					ATTRIBUTES_COL_CHOICE + " TEXT   " +
							");";

	//////////////////////////////////////////////////////////////////////////
	//ALTER-ALTER ATTRIBUTES
	//////////////////////////////////////////////////////////////////////////
	/*
	 * Table holding the names of alter-alter dyad attributes, 
	 * their types, direction types (symmetric or asymmetric), and description (free text).
	 */
	private static final String ALTER_ALTER_ATTRIBS_NAMES_TABLE_NAME = "alter_alter_attribs_names";
	private static final String ALTER_ALTER_ATTRIBS_NAMES_TABLE_CREATE_CMD =
			"CREATE TABLE " + ALTER_ALTER_ATTRIBS_NAMES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT, " +
					ATTRIBUTES_COL_DESCRIPTION + " TEXT, " +
					ATTRIBUTES_COL_VALUE_TYPE + " INT, " +
					ATTRIBUTES_COL_DYNAMIC_TYPE + " TEXT, " +
					ATTRIBUTES_COL_DIRECTION_TYPE + " TEXT, " +
					"PRIMARY KEY (" + ATTRIBUTES_COL_NAME + ")  );";

	/*
	 * Table holding the values of alter-alter dyad attributes. Each 
	 * triplet (attribute name, source name, target name) has at most one value associated with it.
	 * For undirected attributes: the triplet (attribute name, source name, target name) has the same
	 * associated value as (attribute name, target name, source name), if any. 
	 * The special value "N/A" is not explicitly stored.
	 */
	private static final String ALTER_ALTER_ATTRIBS_VALUES_TABLE_NAME = "alter_alter_attribs_values";
	private static final String ALTER_ALTER_ATTRIBS_VALUES_TABLE_CREATE_CMD = 
			"CREATE TABLE " + ALTER_ALTER_ATTRIBS_VALUES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT REFERENCES " 
					+ ALTER_ALTER_ATTRIBS_NAMES_TABLE_NAME + " (" + ATTRIBUTES_COL_NAME + "), " +		
					DYADS_COL_SOURCE + " TEXT, " + 
					DYADS_COL_TARGET + " TEXT," +		
					ATTRIBUTES_COL_VALUE + " TEXT, "  +
					COL_TIME_START + " NUMERIC, " +
					COL_TIME_END + " NUMERIC, " +
					COL_DATUM_ID + " NUMERIC, " +
					"FOREIGN KEY (" + DYADS_COL_SOURCE + ", " + DYADS_COL_TARGET + ") " + 
					"REFERENCES " + ALTER_ALTER_DYADS_TABLE_NAME + 
					" (" + DYADS_COL_SOURCE + "," + DYADS_COL_TARGET + ") " +		
					" );";

	/*
	 * Table holding the allowed values for the tie attributes of type CHOICE. An arbitrary 
	 * number of allowed values can be associated with any of these attributes.
	 */
	private static final String ALTER_ALTER_ATTRIBS_CHOICES_TABLE_NAME = "alter_alter_attribs_choices";
	private static final String ALTER_ALTER_ATTRIBS_CHOICES_TABLE_CREATE_CMD =
			"CREATE TABLE " + ALTER_ALTER_ATTRIBS_CHOICES_TABLE_NAME + " (" +
					ATTRIBUTES_COL_NAME + " TEXT REFERENCES " 
					+ ALTER_ALTER_ATTRIBS_NAMES_TABLE_NAME + " (" + ATTRIBUTES_COL_NAME + "), " +
					ATTRIBUTES_COL_CHOICE + " TEXT   );";

	////////////////////////////////////////////////////////////////////////////////////////
	
	//Reference to the activity that uses this personal network.
	private SCCMainActivity activity;

	//The instance of PersonalNetworkHistorySQLite.
	private static PersonalNetwork instance;

	//Reference to the database.
	private SQLiteDatabase db;
	
	//Reference to the study file loaded.
	private EgonetQuestionnaireFile studyFile;

	//the interview has loaded correctly?
	private boolean interviewLoaded;
	
	/*
	 * Gets a reference to the database (if necessary creates it). This may trigger a call
	 * to onUpgrade which might take long.
	 */
	private PersonalNetwork(SCCMainActivity activity){
		this.activity = activity;
		PersonalNetworkHistoryDBOpenHelper helper = new PersonalNetworkHistoryDBOpenHelper(activity);
		db = helper.getWritableDatabase();
		boolean initialized = false;
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_DB_INITIALIZED};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, 
				null, null, null);
		if(c.moveToFirst()){
			String initstr = c.getString(c.getColumnIndex(PROPERTIES_COL_VALUE));
			if(initstr.equals("true"))
				initialized = true;
		}
		if(!initialized){
			initSystemAttributes();
			initBasicAttributes();
			ContentValues values = new ContentValues();
			values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_DB_INITIALIZED);
			values.put(PROPERTIES_COL_VALUE, "true");
			if(c.moveToFirst())
				db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
			else 
				db.insert(PROPERTIES_TABLE_NAME, null, values);
		}
	}

	/**
	 * Returns the instance of PersonalNetwork.
	 */
	public static PersonalNetwork getInstance(SCCMainActivity activity){
		if(instance == null)
			instance = new PersonalNetwork(activity);
		return instance;
	}

	/**
	 * Properties key identifying the selected alter property.
	 */
	private static final String PROPERTIES_KEY_DB_INITIALIZED = "db_initialized";

	/**
	 * Properties key identifying the selected alter property.
	 */
	public static final String PROPERTIES_KEY_SELECTED_ALTER = "selected_alter";

	/**
	 * Properties key identifying the selected second-alter property.
	 */
	public static final String PROPERTIES_KEY_SELECTED_SECOND_ALTER = "selected_second_alter";

	/**
	 * Properties key identifying the selected ego-attribute property.
	 */
	public static final String PROPERTIES_KEY_SELECTED_EGO_ATTRIBUTE = "selected_ego_attribute";

	/**
	 * Properties key identifying the selected alter attribute property.
	 */
	public static final String PROPERTIES_KEY_SELECTED_ALTER_ATTRIBUTE = "selected_alter_attribute";

	/**
	 * Properties key identifying the selected ego-alter attribute property.
	 */
	public static final String PROPERTIES_KEY_SELECTED_EGO_ALTER_ATTRIBUTE = "selected_ego_alter_attribute";

	/**
	 * Properties key identifying the selected alter-alter attribute property.
	 */
	public static final String PROPERTIES_KEY_SELECTED_ALTER_ALTER_ATTRIBUTE = "selected_alter_alter_attribute";

	/**
	 * Properties key identifying the selected attribute domain property.
	 */
	public static final String PROPERTIES_KEY_SELECTED_ATTRIBUTE_DOMAIN = "selected_attribute_domain";

	
	private static final String DOMAIN_EGOSMART = "EGOSMART";
	/**
	 * String constant used to prefix attributes that are declared by the software (rather than by the user).
	 * User-defined attribute names must not start with that prefix. 
	 */
	public static final String ATTRIBUTE_PREFIX_EGOSMART = DOMAIN_EGOSMART + ":";
	
	/**
	 * String constant for the ego attribute holding ego's display name 
	 * obtained from the Android user profile (assuming that ego is the
	 * user of the Android device)
	 */
	public static final String EGO_ATTRIBUTE_NAME_EGO_DISPLAY_NAME = ATTRIBUTE_PREFIX_EGOSMART + "ego_display_name";
	
	/**
	 * String constant for the alter attribute holding alters' email addresses (multiple
	 * addresses separated by comma).
	 */
	public static final String ALTER_ATTRIBUTE_NAME_ALTER_EMAIL_ADDRESSES = ATTRIBUTE_PREFIX_EGOSMART + "alter_email_addresses";
	
	/**
	 * String constant for the alter attribute holding alters' phone numbers (multiple
	 * numbers separated by comma).
	 */
	public static final String ALTER_ATTRIBUTE_NAME_ALTER_PHONE_NUMBERS = ATTRIBUTE_PREFIX_EGOSMART + "alter_phone_numbers";
	
	/**
	 * String constant for the alter attribute holding alters' system ids. 
	 */
	public static final String ALTER_ATTRIBUTE_NAME_ALTER_SYSTEM_ID = ATTRIBUTE_PREFIX_EGOSMART + "alter_system_id";
	
	/**
	 * String constant for the alter attribute holding alters' photo id 
	 */
	public static final String ALTER_ATTRIBUTE_NAME_ALTER_PHOTO_ID = ATTRIBUTE_PREFIX_EGOSMART + "alter_photo_id";
	
	/**
	 * String constant for the alter attribute holding alters' photo uri 
	 */
	public static final String ALTER_ATTRIBUTE_NAME_ALTER_PHOTO_URI = ATTRIBUTE_PREFIX_EGOSMART + "alter_photo_uri";
	
	/**
	 * String constant for the alter attribute holding alters' photo thumbnail uri 
	 */
	public static final String ALTER_ATTRIBUTE_NAME_ALTER_PHOTO_THUMBNAIL_URI = ATTRIBUTE_PREFIX_EGOSMART + "alter_photo_thumbnail_uri";
	
	/**
	 * String constants denoting the domain of elements of a personal network and 
	 * thus the domains of attributes.
	 */
	public static final String DOMAIN_EGO = "EGO";
	public static final String DOMAIN_ALTER = "ALTER";
	public static final String DOMAIN_EGO_ALTER = "EGO_ALTER";
	public static final String DOMAIN_ALTER_ALTER = "ALTER_ALTER";
	
	/**
	 * Integer id for the attribute type TEXT (arbitrary but non-null and non-empty strings without
	 * leading or trailing white spaces).
	 */
	public static final int ATTRIB_TYPE_TEXT = 0;
	/**
	 * Integer id for the attribute type NUMBER (strings that can be parsed to a double).
	 */
	public static final int ATTRIB_TYPE_NUMBER = 1;
	/**
	 * Integer id for the attribute type CHOICE (strings from a finite set of allowed
	 * values which can be modified).
	 */
	public static final int ATTRIB_TYPE_FINITE_CHOICE = 2;

	/**
	 * Names of attribute types
	 */
	public static final String[] ATTRIB_TYPE_NAMES = {
		"TEXT", 
		"NUMBER", 
	"CHOICE"};

	/**
	 * Special attribute value with the meaning that no value is associated for the given element. 
	 */
	public static final String VALUE_NOT_ASSIGNED = "N/A";

	/**
	 * Value for the system id of alters for which no system id is known.
	 */
	public static final String VALUE_NO_SYSTEM_ID = "NOID";

	/**
	 * Direction type for symmetric dyad attributes (that is a dyad and its reverse dyad necessarily have the same value).
	 */
	public static final String DYAD_DIRECTION_SYMMETRIC = "SYMMETRIC";
	/**
	 * Direction type for asymmetric dyad attributes (that is a dyad may have a different value then its reverse dyad). 
	 */
	public static final String DYAD_DIRECTION_ASYMMETRIC = "ASYMMETRIC";
	/**
	 * Direction type for ego-alter dyad attributes indicating the direction from ego to alter.
	 */
	public static final String DYAD_DIRECTION_OUT = "OUT";
	/**
	 * Direction type for ego-alter dyad attributes indicating the direction from alter to ego.
	 */
	public static final String DYAD_DIRECTION_IN = "IN";

	/**
	 * Properties key identifying the selected alter in history property.
	 * (This alter might be already removed from the personal network.)
	 */
	private static final String PROPERTIES_KEY_SELECTED_ALTER_IN_HISTORY = "selected_alter_in_history";

	/**
	 * Properties key identifying the last network change property.
	 */
	public static final String PROPERTIES_KEY_LAST_CHANGE = "last_change";

	/**
	 * Properties key identifying the event counter (serving as id).
	 */
	private static final String PROPERTIES_KEY_NEXT_DATUM_ID = "next_event_id";

	/**
	 * String describing that an attribute has state-type dynamics.
	 */
	public static final String ATTRIBUTE_DYNAMIC_TYPE_STATE = "STATE";

	/**
	 * String describing that an attribute has event-type dynamics.
	 */
	public static final String ATTRIBUTE_DYNAMIC_TYPE_EVENT = "EVENT";

	
	//marks the time when the start time has been set
	private static final String SECONDARY_ATTRIBUTE_NAME_SUFFIX_TIMESTAMP_START = "timestamp_start";
	/**
	 * Name for secondary attributes containing unconstrained text (notes, memos).
	 */
	public static String getSecondaryAttributeNameTimestampStart(){
		return ATTRIBUTE_PREFIX_EGOSMART + 
				SECONDARY_ATTRIBUTE_NAME_SUFFIX_TIMESTAMP_START;
	}
	
	//marks the time when the end time has been set
	private static final String SECONDARY_ATTRIBUTE_NAME_SUFFIX_TIMESTAMP_END = "timestamp_end";
	/**
	 * Name for secondary attributes containing unconstrained text (notes, memos).
	 */
	public static String getSecondaryAttributeNameTimestampEnd(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + 
				SECONDARY_ATTRIBUTE_NAME_SUFFIX_TIMESTAMP_END;
	}
	
	/*
	 * Name for secondary attributes containing unconstrained text (notes, memos).
	 */
	private static final String SECONDARY_ATTRIBUTE_NAME_SUFFIX_TEXT = "text";
	
	/**
	 * Name for secondary attributes containing unconstrained text (notes, memos).
	 */
	public static String getSecondaryAttributeNameText(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + SECONDARY_ATTRIBUTE_NAME_SUFFIX_TEXT;
	}
	
	/*
	 * Name for secondary attributes encoding contact form 
	 * (e.g., face-to-face, phone, Internet, postal mail, other).
	 */
	private static final String SECONDARY_ATTRIBUTE_NAME_SUFFIX_CONTACT_FORM = "contact_form";
	
	/**
	 * Name for secondary attributes encoding contact form 
	 * (e.g., face-to-face, phone, Internet, postal mail, other).
	 */
	public static String getSecondaryAttributeNameContactForm(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + SECONDARY_ATTRIBUTE_NAME_SUFFIX_CONTACT_FORM;
	}
	
	/*
	 * Name for secondary attributes encoding contact content 
	 * (what happened in the contact: information exchange, greeting, talking, making an agreement, etc).
	 */
	private static final String SECONDARY_ATTRIBUTE_NAME_SUFFIX_CONTACT_CONTENT = "contact_content";
	
	/**
	 * Name for secondary attributes encoding contact content 
	 * (what happened in the contact: information exchange, greeting, talking, making an agreement, etc).
	 */
	public static String getSecondaryAttributeNameContactContent(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + SECONDARY_ATTRIBUTE_NAME_SUFFIX_CONTACT_CONTENT;
	}
	
	/*
	 * Name for secondary attributes encoding contact atmosphere 
	 * (pleasant, unpleasant).
	 */
	private static final String SECONDARY_ATTRIBUTE_NAME_SUFFIX_CONTACT_ATMOSPHERE = "contact_atmosphere";
	
	/**
	 * Name for secondary attributes encoding contact atmosphere 
	 * (pleasant, unpleasant).
	 */
	public static String getSecondaryAttributeNameContactAtmosphere(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + SECONDARY_ATTRIBUTE_NAME_SUFFIX_CONTACT_ATMOSPHERE;
	}
	
	/*
	 * String for event-type attributes recording memos (notes, unconstrained text) related to ego.
	 */
	private static final String EGO_MEMOS_ATTRIBUTE_NAME_SUFFIX = "ego_memo";
	
	/**
	 * String for event-type attributes recording memos (notes, unconstrained text) related to ego.
	 */
	public static String getEgoMemosAttributeName(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + EGO_MEMOS_ATTRIBUTE_NAME_SUFFIX;
	}
	
	/*
	 * String for event-type attributes recording memos (notes, unconstrained text) related to an alter.
	 */
	private static final String ALTER_MEMOS_ATTRIBUTE_NAME_SUFFIX = "alter_memo";
	
	/**
	 * String for event-type attributes recording memos (notes, unconstrained text) related to an alter.
	 */
	public static String getAlterMemosAttributeName(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + ALTER_MEMOS_ATTRIBUTE_NAME_SUFFIX;
	}
	
	/*
	 * String for event-type attributes recording memos (notes, unconstrained text) related 
	 * to an alter-alter dyad.
	 */
	private static final String ALTER_ALTER_MEMOS_ATTRIBUTE_NAME_SUFFIX = "alter_alter_memo";
	
	/**
	 * String for event-type attributes recording memos (notes, unconstrained text) related 
	 * to an alter-alter dyad.
	 */
	public static String getAlterAlterMemosAttributeName(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + ALTER_ALTER_MEMOS_ATTRIBUTE_NAME_SUFFIX;
	}
	
	/*
	 * String for events recording contacts (telephone, email, face-to-face) between ego and alter.
	 */
	private static final String EGO_ALTER_CONTACT_EVENT_ATTRIBUTE_NAME_SUFFIX = "ego_alter_contact_event";
	
	/**
	 * String for events recording contacts (telephone, email, face-to-face) between ego and alter.
	 */
	public static String getEgoAlterContactEventAttributeName(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + EGO_ALTER_CONTACT_EVENT_ATTRIBUTE_NAME_SUFFIX;
	}
	
	/*
	 * String for events recording contacts (face-to-face, communication or other) between alter and alter.
	 */
	private static final String ALTER_ALTER_CONTACT_EVENT_ATTRIBUTE_NAME_SUFFIX = "alter_alter_contact_event";	
	/**
	 * String for events recording contacts (telephone, email, face-to-face) between alter and alter.
	 */
	public static String getAlterAlterContactEventAttributeName(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + ALTER_ALTER_CONTACT_EVENT_ATTRIBUTE_NAME_SUFFIX;
	}
	
	/*
	 * String for the attribute recording the lifetime of alters (only needed to attach secondary 
	 * attributes).
	 */
	private static final String ALTER_LIFETIME_ATTRIBUTE_NAME_SUFFIX = "alter_lifetime";	
	/**
	 * String for the attribute recording the lifetime of alters (only needed to attach secondary 
	 * attributes).
	 */
	public static String getAlterLifetimeAttributeName(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + ALTER_LIFETIME_ATTRIBUTE_NAME_SUFFIX;
	}
	
	/*
	 * String for the attribute recording the lifetime of ties (only needed to attach secondary 
	 * attributes).
	 */
	private static final String TIE_LIFETIME_ATTRIBUTE_NAME_SUFFIX = "tie_lifetime";	
	/**
	 * String for the attribute recording the lifetime of ties (only needed to attach secondary 
	 * attributes).
	 */
	public static String getTieLifetimeAttributeName(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + TIE_LIFETIME_ATTRIBUTE_NAME_SUFFIX;
	}
	
	/*
	 * String for the attribute recording the lifetime of alter-alter dyads 
	 * (only needed to attach secondary 
	 * attributes).
	 */
	private static final String ALTER_ALTER_DYAD_LIFETIME_ATTRIBUTE_NAME_SUFFIX = "alter_alter_dyad_lifetime";	
	/**
	 * String for the attribute recording the lifetime of alter-alter dyads 
	 * (only needed to attach secondary 
	 * attributes).
	 */
	public static String getAlterAlterDyadLifetimeAttributeName(){
		return PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART + ALTER_ALTER_DYAD_LIFETIME_ATTRIBUTE_NAME_SUFFIX;
	}
	
	public void setLastChange(long time){
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_LAST_CHANGE);
		String s = String.valueOf(time);
		values.put(PROPERTIES_COL_VALUE, s);
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_LAST_CHANGE};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(!c.moveToFirst()){ 
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		} else { 
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		}
		c.close();
	}
	
	public long getLastChange(){
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_LAST_CHANGE};
		Cursor result = db.query(PROPERTIES_TABLE_NAME, new String[] {PROPERTIES_COL_KEY, PROPERTIES_COL_VALUE}, selection, selectionArgs,
				null, null, null);
		//int col_of_key = result.getColumnIndex(PROPERTIES_COL_KEY);
		int col_of_value = result.getColumnIndex(PROPERTIES_COL_VALUE);
		long time = Long.MIN_VALUE;
		if(result.moveToFirst() && col_of_value >=0 ){
			String t = result.getString(col_of_value);
			time = Long.valueOf(t).longValue();
		}		
		result.close();
		return time;
	}

	/**
	 * Searches for the given query. 
	 * 
	 * In this implementation is searches only for alter names that are currently
	 * in the network and returns the list of those that
	 * match the query. 
	 * 
	 * This will be changed in the future to also search for attributes, values, 
	 * alters having attribute values, etc; and also for historic values.
	 * 
	 * @param query
	 * @return
	 */
	public LinkedHashSet<String> search(String query) {
		//TODO: change this!!! (but works for a demo)
		LinkedHashSet<String> result = new LinkedHashSet<String>();
		db.beginTransaction();
		try{
			db.execSQL("CREATE VIRTUAL TABLE ALTERS_FTS_VIRTUAL_TABLE"  +
					" USING fts3 (" +
					ALTERS_COL_NAME + ")");
			for(String alter: getAltersAt(TimeInterval.getCurrentTimePoint())){
				ContentValues values = new ContentValues();
				values.put(ALTERS_COL_NAME, alter);
				db.insert("ALTERS_FTS_VIRTUAL_TABLE", null, values);
			}
			String selection = ALTERS_COL_NAME + " MATCH ?";
			String[] selectionArgs = new String[] {query+"*"};
			//String selection = ALTERS_COL_NAME + " = ?";
			//String[] selectionArgs = new String[] {query};
			SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
			//builder.setTables(ALTERS_TABLE_NAME);
			builder.setTables("ALTERS_FTS_VIRTUAL_TABLE");
			Cursor c = builder.query(db, new String[]{ALTERS_COL_NAME}, 
					selection, selectionArgs, null, null, 
					ALTERS_COL_NAME + " ASC", Integer.toString(10));
			//Cursor c = db.query(ALTERS_TABLE_NAME, new String[]{ALTERS_COL_NAME}, 
			//	selection, selectionArgs, null, null, ALTERS_COL_NAME + " ASC", Integer.toString(10));
			int index = c.getColumnIndexOrThrow(ALTERS_COL_NAME);
			if(c.moveToFirst()){
				while(!c.isAfterLast()){
					result.add(c.getString(index));
					c.moveToNext();
				}
			}
			c.close();
			db.execSQL("DROP TABLE IF EXISTS ALTERS_FTS_VIRTUAL_TABLE");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return result;
	}

	/**
	 * Returns the name of the currently selected alter; an arbitrary alter if none is selected
	 * but if there is an alter in the network and null if there is no alter in the network.
	 * 
	 * @return
	 */
	public String getSelectedAlter() {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_ALTER};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		int col = c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE);
		if(!c.moveToFirst()){
			String alter = getAnyCurrentAlter();
			if(alter != null)
				setSelectedAlter(alter);
			c.close();
			return alter;
		}
		String ret = c.getString(col);
		c.close();
		return ret;
	}

	/**
	 * Sets the alter selection to the given alter. 
	 * 
	 * Does not change the current selection if alterName is currently not an alter in the network.
	 * 
	 * @param alterName
	 */
	public void setSelectedAlter(String alterName) {
		if(!hasAlterAt(TimeInterval.getCurrentTimePoint(), alterName))
			return;
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_SELECTED_ALTER);
		values.put(PROPERTIES_COL_VALUE, alterName);
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_ALTER};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(!c.moveToFirst()){ // no alter is currently selected
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		} else { 
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		}
		c.close();
	}

	/**
	 * Sets the alter selection to the given alter.
	 * 
	 * If alterName is not among the current alters (or if alterName is null) then the selected alter is 
	 * removed.
	 * 
	 * @param alterName
	 */
	private void setSelectedAlterForced(String alterName) {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_ALTER};
		if(!hasAlterAt(TimeInterval.getCurrentTimePoint(), alterName)){
			db.delete(PROPERTIES_TABLE_NAME, selection, selectionArgs);
			return;
		}
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_SELECTED_ALTER);
		values.put(PROPERTIES_COL_VALUE, alterName);
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(!c.moveToFirst()){ // no alter is currently selected
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		} else { 
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		}
		c.close();
	}

	/**
	 * Returns the name of the currently selected second alter; an arbitrary current alter if none is selected
	 * but if currently there is an alter in the network and null if currently
	 * there is no alter in the network.
	 * 
	 * It is not guaranteed that the selected second alter is different from the selected alter.
	 * 
	 * @return
	 */
	public String getSelectedSecondAlter() {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_SECOND_ALTER};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		int col = c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE);
		if(!c.moveToFirst()){
			String alter = getAnyCurrentAlter();
			if(alter != null)
				setSelectedSecondAlter(alter);
			c.close();
			return alter;
		}
		String ret = c.getString(col);
		c.close();
		return ret;
	}

	/**
	 * Sets the selected second alter. 
	 * 
	 * Does not change the current selection if alterName is currently not an alter in the network.
	 * 
	 * @param alterName
	 */
	public void setSelectedSecondAlter(String alterName) {
		if(!hasAlterAt(TimeInterval.getCurrentTimePoint(), alterName))
			return;
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_SELECTED_SECOND_ALTER);
		values.put(PROPERTIES_COL_VALUE, alterName);
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_SECOND_ALTER};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(!c.moveToFirst()){ // no alter is currently selected
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		} else { 
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		}
		c.close();
	}

	/**
	 * Sets the selected second alter.
	 * 
	 * If alterName is not among the current alters (or if alterName is null) then the selected second 
	 * alter is removed.
	 * 
	 * @param alterName
	 */
	private void setSelectedSecondAlterForced(String alterName) {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_SECOND_ALTER};
		if(!hasAlterAt(TimeInterval.getCurrentTimePoint(), alterName)){
			db.delete(PROPERTIES_TABLE_NAME, selection, selectionArgs);
			return;
		}
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_SELECTED_SECOND_ALTER);
		values.put(PROPERTIES_COL_VALUE, alterName);
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(!c.moveToFirst()){ // no alter is currently selected
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		} else { 
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		}
		c.close();
	}

	/**
	 * Returns the currently selected attribute domain.
	 * 
	 * @return EGO, ALTER, EGO_ALTER, or ALTER_ALTER
	 */
	public String getSelectedAttributeDomain() {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_ATTRIBUTE_DOMAIN};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		int col_of_value = c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE);
		if(!c.moveToFirst()){
			c.close();
			return DOMAIN_ALTER;
		}
		String ret = c.getString(col_of_value);
		c.close();
		return ret;
	}
	
	/**
	 * Sets the selected attribute domain. Does nothing if domain does not exist.
	 * 
	 * @param domain must be EGO, ALTER, EGO_ALTER, or ALTER_ALTER
	 */
	public void setSelectedAttributeDomain(String domain) {
		if(!attributeDomainExists(domain)){
			activity.reportInfo("domain: " + domain + " is not among the allowed values");
			return;
		}
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_SELECTED_ATTRIBUTE_DOMAIN);
		values.put(PROPERTIES_COL_VALUE, domain);
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_ATTRIBUTE_DOMAIN};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_KEY}, 
				selection, selectionArgs, null, null, null);
		if(c.getCount() == 0){ // no attribute domain is currently selected
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		} else { 
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		}
		c.close();
	}
	
	/**
	 * Returns the name of the currently selected attribute for the given domain; null if none is selected
	 * for that domain or if the domain does not exist.
	 * 
	 * @return
	 */
	public String getSelectedAttribute(String domain) {
		if(!attributeDomainExists(domain))
			return null;
		String selection = PROPERTIES_COL_KEY + " = ?";
		String propertyKey = null;
		if(DOMAIN_EGO.equals(domain))
			propertyKey = PROPERTIES_KEY_SELECTED_EGO_ATTRIBUTE;
		if(DOMAIN_ALTER.equals(domain))
			propertyKey = PROPERTIES_KEY_SELECTED_ALTER_ATTRIBUTE;
		if(DOMAIN_EGO_ALTER.equals(domain))
			propertyKey = PROPERTIES_KEY_SELECTED_EGO_ALTER_ATTRIBUTE;
		if(DOMAIN_ALTER_ALTER.equals(domain))
			propertyKey = PROPERTIES_KEY_SELECTED_ALTER_ALTER_ATTRIBUTE;
		String[] selectionArgs = {propertyKey};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		int col_of_value = c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE);
		String ret = null;
		if(c.moveToFirst())
			ret = c.getString(col_of_value);
		c.close();
		return ret;
	}

	/**
	 * Sets the attribute selection for the given domain to the given attribute. 
	 * 
	 * Does nothing if the domain or the attribute does not exist.
	 * 
	 * @param domain
	 * @param attributeName
	 */
	public void setSelectedAttribute(String domain, String attributeName) {
		if(!hasAttribute(domain, attributeName))
			return;
		String selection = PROPERTIES_COL_KEY + " = ?";
		String propertyKey = null;
		if(DOMAIN_EGO.equals(domain))
			propertyKey = PROPERTIES_KEY_SELECTED_EGO_ATTRIBUTE;
		if(DOMAIN_ALTER.equals(domain))
			propertyKey = PROPERTIES_KEY_SELECTED_ALTER_ATTRIBUTE;
		if(DOMAIN_EGO_ALTER.equals(domain))
			propertyKey = PROPERTIES_KEY_SELECTED_EGO_ALTER_ATTRIBUTE;
		if(DOMAIN_ALTER_ALTER.equals(domain))
			propertyKey = PROPERTIES_KEY_SELECTED_ALTER_ALTER_ATTRIBUTE;
		String[] selectionArgs = {propertyKey};
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, propertyKey);
		values.put(PROPERTIES_COL_VALUE, attributeName);
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(c.moveToFirst()){
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);			
		} else {
			db.insert(PROPERTIES_TABLE_NAME, null, values);			
		}
		c.close();
	}

	/**
	 * Adds the two anti-parallel ties (source, target) and (target, source) to the network history.
	 * 
	 * Does nothing if source and target are equal or if the tie is already in the network history.
	 * 
	 * The lifetime of the newly created tie is empty.
	 * 
	 * If necessary adds source and target to the set of alters.
	 * 
	 * @param sourceName
	 * @param targetName
	 */
	private void addTie(String sourceName, String targetName) {
		if(sourceName.equals(targetName))
			return;
		if(hasTie(sourceName, targetName))
			return;
		if(!hasAlter(sourceName)){
			addAlter(sourceName);
		}
		if(!hasAlter(targetName)){
			addAlter(targetName);
		}
		ContentValues values = new ContentValues();
		values.put(DYADS_COL_SOURCE, sourceName);
		values.put(DYADS_COL_TARGET, targetName);
		db.insert(TIES_TABLE_NAME, null, values);
		//add the reverse tie
		values.put(DYADS_COL_SOURCE, targetName);
		values.put(DYADS_COL_TARGET, sourceName);
		db.insert(TIES_TABLE_NAME, null, values);
	}

	/**
	 * Returns true if (source, target) is a tie in the network irrespective of its lifetime
	 * (which might even be empty).
	 * 
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	private boolean hasTie(String sourceName, String targetName) {
		if(sourceName == null || targetName == null)
			return false;
		String selection = DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ?";
		String[] selectionArgs = {sourceName, targetName};
		Cursor c = db.query(TIES_TABLE_NAME, new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET}, 
				selection, selectionArgs, null, null, null, "1");
		boolean ret = c.getCount() > 0;
		c.close();
		return ret;
	}

	/**
	 * Returns the set of all alter names 'other' for which (alterName, other), 
	 * and therefore (other, alterName),
	 * is a tie in the network, irrespective of its lifetime. 
	 * 
	 * Returns null if alterName is unknown.
	 * 
	 * @param alterName
	 * @return
	 */
	private LinkedHashSet<String> getNeighbors(String alterName){
		if(!hasAlter(alterName))
			return null;
		String selection = DYADS_COL_SOURCE + " = ?";
		String[] selectionArgs = {alterName};
		Cursor c = db.query(TIES_TABLE_NAME, new String[]{DYADS_COL_TARGET}, 
				selection, selectionArgs, null, null, DYADS_COL_TARGET + " ASC");
		int col_of_target = c.getColumnIndex(DYADS_COL_TARGET);
		LinkedHashSet<String> neighbors = new LinkedHashSet<String>();
		if(c.moveToFirst() && col_of_target >= 0){
			while(!c.isAfterLast()){
				String neighbor = c.getString(col_of_target);
				neighbors.add(neighbor);
				c.moveToNext();
			}
		}		
		c.close();
		return neighbors;
	}

	/**
	 * Returns the set of directed ties irrespective of their lifetimes.
	 * 
	 * @return
	 */
	private LinkedHashSet<OrderedDyad> getDirectedTies(){
		Cursor c = db.query(TIES_TABLE_NAME, new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET}, 
				null, null, null, null, null);
		LinkedHashSet<OrderedDyad> ties = new LinkedHashSet<OrderedDyad>();
		int col_of_source = c.getColumnIndex(DYADS_COL_SOURCE);
		int col_of_target = c.getColumnIndex(DYADS_COL_TARGET);
		if(c.moveToFirst() && col_of_source >= 0 && col_of_target >= 0){
			while(!c.isAfterLast()){
				ties.add(new OrderedDyad(c.getString(col_of_source), 
						c.getString(col_of_target)));
				c.moveToNext();
			}
		}
		c.close();
		return ties;
	}

	/**
	 * Returns the set of undirected ties irrespective of their lifetimes.
	 * 
	 * @return
	 */
	private LinkedHashSet<UnorderedDyad> getUndirectedTies(){
		Cursor c = db.query(TIES_TABLE_NAME, new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET}, 
				null, null, null, null, null);
		LinkedHashSet<UnorderedDyad> ties = new LinkedHashSet<UnorderedDyad>();
		int col_of_source = c.getColumnIndex(DYADS_COL_SOURCE);
		int col_of_target = c.getColumnIndex(DYADS_COL_TARGET);
		if(c.moveToFirst() && col_of_source >= 0 && col_of_target >= 0){
			while(!c.isAfterLast()){
				ties.add(new UnorderedDyad(c.getString(col_of_source), 
						c.getString(col_of_target)));
				c.moveToNext();
			}
		}
		c.close();
		return ties;
	}

	/**
	 * Sets the lifetime of the two anti-parallel ties (source, target) and (target, source) 
	 * equal to the union of their current lifetime with the specified interval.
	 * 
	 * If (source,target) is not yet in the set of ties then addTie(source, target) is called.
	 * 
	 * If necessary adds source and target to set of alters 
	 * or (if necessary) increases their lifetime to include interval.
	 * 
	 * Does nothing if sourceName equals targetName
	 * 
	 * @param sourceName
	 * @param targetName
	 * @param interval
	 */
	public void addToLifetimeOfTie(TimeInterval interval, String sourceName, String targetName) {
		if(sourceName.equals(targetName))
			return;
		if(!hasTie(sourceName, targetName))
			addTie(sourceName, targetName);
		addToLifetimeOfAlter(interval, sourceName);
		addToLifetimeOfAlter(interval, targetName);
		String historyTableName = TIES_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		ContentValues values = new ContentValues();
		values.put(DYADS_COL_SOURCE, sourceName);
		values.put(DYADS_COL_TARGET, targetName);
		unionWithLifetime(interval, historyTableName, elementSelection,
				elementSelectionArgs, values);
		//do the same for the reverse tie
		elementSelectionArgs = new String[]{targetName, sourceName};
		values = new ContentValues();
		values.put(DYADS_COL_SOURCE, targetName);
		values.put(DYADS_COL_TARGET, sourceName);
		unionWithLifetime(interval, historyTableName, elementSelection,
				elementSelectionArgs, values);
		setLastChange(System.currentTimeMillis());
	}

	/**
	 * Returns true if (source, target) is a tie in the network 
	 * at some point in time included in the given interval.
	 * 
	 * @param interval
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	public boolean areAdjacentAt(TimeInterval interval, String sourceName, String targetName) {
		if(!hasTie(sourceName, targetName))
			return false;
		String historyTableName = TIES_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		return overlapsLifetime(interval, historyTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns the lifetime datum id of the given tie if (source, target) is a tie in the network 
	 * at the given point in time.
	 * 
	 * Returns null if the given time point is outside of the lifetime of that tie.
	 * 
	 * @param interval
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	public String getTieDatumIDAt(long timePoint, String sourceName, String targetName) {
		if(!hasTie(sourceName, targetName))
			return null;
		String historyTableName = TIES_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		return getLifetimeDatumIDAt(timePoint, historyTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * 
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	public Lifetime getLifetimeOfTie(String sourceName, String targetName) {
		String historyTableName = TIES_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		return getLifetime(historyTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns the set of all alter names 'other' for which (alterName, other)
	 * - and therefore (other, alterName) - is a tie in the network at some point
	 * in time included in the given interval. 
	 * 
	 * Returns the empty set if alterName is unknown.
	 * 
	 * @param interval
	 * @param alterName
	 * @return
	 */
	public LinkedHashSet<String> getNeighborsAt(TimeInterval interval, String alterName) {
		LinkedHashSet<String> neighbors = new LinkedHashSet<String>();
		String selection = DYADS_COL_SOURCE + " = ? AND " +
				COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ?";
		String[] selectionArgs = {alterName, 
				Long.toString(interval.getEndTime()), 
				Long.toString(interval.getStartTime())};
		Cursor c = db.query(TIES_HISTORY_TABLE_NAME, 
				new String[]{DYADS_COL_TARGET, COL_TIME_START, COL_TIME_END}, 
				selection, selectionArgs, null, null, null);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				long start = c.getLong(c.getColumnIndex(COL_TIME_START));
				long end = c.getLong(c.getColumnIndex(COL_TIME_END));
				if(overlap(start, end, interval.getStartTime(), interval.getEndTime()))
					neighbors.add(c.getString(c.getColumnIndex(DYADS_COL_TARGET)));
				c.moveToNext();
			}
		}
		c.close();
		return neighbors;
	}

	/**
	 * Cuts the given interval from the lifetime of the ties (source, target) and (target, source).
	 * 
	 * Note the behavior of Lifetime.cutOut(interval) if interval is a time point.
	 * 
	 * @param interval 
	 * @param sourceName
	 * @param targetName
	 */
	public void removeTieAt(TimeInterval interval, String sourceName, String targetName) {
		String historyTableName = TIES_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		ContentValues values = new ContentValues();
		values.put(DYADS_COL_SOURCE, sourceName);
		values.put(DYADS_COL_TARGET, targetName);
		cutOutOfLifetime(interval, historyTableName, elementSelection, elementSelectionArgs, values);
		//do the same for the reverse tie
		elementSelectionArgs = new String[]{targetName, sourceName};
		values = new ContentValues();
		values.put(DYADS_COL_SOURCE, targetName);
		values.put(DYADS_COL_TARGET, sourceName);
		cutOutOfLifetime(interval, historyTableName, elementSelection, elementSelectionArgs, values);
		setLastChange(System.currentTimeMillis());
	}

	/**
	 * Returns the number of undirected pairs of alters that are adjacent at some point in the given interval.
	 * @return
	 */
	public int getNumberOfUndirectedTiesAt(TimeInterval interval) {
		return getUndirectedTiesAt(interval).size();//TODO: this should be possible in a more efficient way
	}

	/**
	 * Returns the set of OrderedDyads that are ties at some point in the given interval.
	 * 
	 * @return
	 */
	public LinkedHashSet<OrderedDyad> getDirectedTiesAt(TimeInterval interval) {
		LinkedHashSet<OrderedDyad> dyads = new LinkedHashSet<OrderedDyad>();
		String selection = COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ?";
		String[] selectionArgs = {Long.toString(interval.getEndTime()), 
				Long.toString(interval.getStartTime())};
		Cursor c = db.query(TIES_HISTORY_TABLE_NAME, 
				new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET, COL_TIME_START, COL_TIME_END}, 
				selection, selectionArgs, null, null, null);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				long start = c.getLong(c.getColumnIndex(COL_TIME_START));
				long end = c.getLong(c.getColumnIndex(COL_TIME_END));
				if(overlap(start, end, interval.getStartTime(), interval.getEndTime())){
					dyads.add(new OrderedDyad(c.getString(c.getColumnIndex(DYADS_COL_SOURCE)), 
							c.getString(c.getColumnIndex(DYADS_COL_TARGET))));
				}
				c.moveToNext();
			}
		}
		c.close();
		return dyads;
	}

	/**
	 * Returns the set of UnorderedDyads that are ties are some point in the given interval.
	 * 
	 * @return
	 */
	public LinkedHashSet<UnorderedDyad> getUndirectedTiesAt(TimeInterval interval) {
		LinkedHashSet<UnorderedDyad> dyads = new LinkedHashSet<UnorderedDyad>();
		String selection = COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ?";
		String[] selectionArgs = {Long.toString(interval.getEndTime()), 
				Long.toString(interval.getStartTime())};
		Cursor c = db.query(TIES_HISTORY_TABLE_NAME, 
				new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET, COL_TIME_START, COL_TIME_END}, 
				selection, selectionArgs, null, null, null);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				long start = c.getLong(c.getColumnIndex(COL_TIME_START));
				long end = c.getLong(c.getColumnIndex(COL_TIME_END));
				if(overlap(start, end, interval.getStartTime(), interval.getEndTime())){
					dyads.add(new UnorderedDyad(c.getString(c.getColumnIndex(DYADS_COL_SOURCE)), 
							c.getString(c.getColumnIndex(DYADS_COL_TARGET))));
				}
				c.moveToNext();
			}
		}
		c.close();
		return dyads;
	}

	/**
	 * Adds the two anti-parallel dyads (source, target) and (target, source) to the network.
	 * 
	 * If necessary adds sourceName and targetName to the set of alters.
	 * 
	 * @param sourceName
	 * @param targetName
	 */
	protected void addAlterAlterDyad(String sourceName, String targetName) {
		if(sourceName.equals(targetName))
			return;
		if(hasAlterAlterDyad(sourceName, targetName))
			return;
		if(!hasAlter(sourceName)){
			addAlter(sourceName);
		}
		if(!hasAlter(targetName)){
			addAlter(targetName);
		}
		ContentValues values = new ContentValues();
		values.put(DYADS_COL_SOURCE, sourceName);
		values.put(DYADS_COL_TARGET, targetName);
		db.insert(ALTER_ALTER_DYADS_TABLE_NAME, null, values);
		//add the reverse dyad
		values.put(DYADS_COL_SOURCE, targetName);
		values.put(DYADS_COL_TARGET, sourceName);
		db.insert(ALTER_ALTER_DYADS_TABLE_NAME, null, values);
	}

	/**
	 * Returns true if (source, target) is a dyad in the network irrespective of its lifetime
	 * (which might even be empty).
	 * 
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	protected boolean hasAlterAlterDyad(String sourceName, String targetName) {
		if(sourceName == null || targetName == null)
			return false;
		String selection = DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ?";
		String[] selectionArgs = {sourceName, targetName};
		Cursor c = db.query(ALTER_ALTER_DYADS_TABLE_NAME, new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET}, 
				selection, selectionArgs, null, null, null);
		boolean ret = c.getCount() > 0;
		c.close(); 
		return ret;
	}

	/**
	 * Returns the set of all dyads incident to alterName that are in the network 
	 * irrespective of their lifetime.
	 * 
	 * Returns null if alterName is unknown.
	 * 
	 * @param alterName
	 * @return
	 */
	protected LinkedHashSet<UnorderedDyad> getIncidentDyads(String alterName) {
		if(!hasAlter(alterName))
			return null;
		String selection = DYADS_COL_SOURCE + " = ?";
		String[] selectionArgs = {alterName};
		Cursor c = db.query(ALTER_ALTER_DYADS_TABLE_NAME, new String[]{DYADS_COL_TARGET}, 
				selection, selectionArgs, null, null, DYADS_COL_TARGET + " ASC");
		int col_of_target = c.getColumnIndex(DYADS_COL_TARGET);
		LinkedHashSet<UnorderedDyad> dyads = new LinkedHashSet<UnorderedDyad>();
		if(c.moveToFirst() && col_of_target >= 0){
			while(!c.isAfterLast()){
				String neighbor = c.getString(col_of_target);
				dyads.add(new UnorderedDyad(alterName, neighbor));
				c.moveToNext();
			}
		}		
		c.close();
		return dyads;
	}

	/**
	 * Returns the set of directed dyads irrespective of their lifetimes.
	 * 
	 * @return
	 */
	protected LinkedHashSet<OrderedDyad> getDirectedAlterAlterDyads() {
		Cursor c = db.query(ALTER_ALTER_DYADS_TABLE_NAME, new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET}, 
				null, null, null, null, null);
		LinkedHashSet<OrderedDyad> dyads = new LinkedHashSet<OrderedDyad>();
		int col_of_source = c.getColumnIndex(DYADS_COL_SOURCE);
		int col_of_target = c.getColumnIndex(DYADS_COL_TARGET);
		if(c.moveToFirst() && col_of_source >= 0 && col_of_target >= 0){
			while(!c.isAfterLast()){
				dyads.add(new OrderedDyad(c.getString(col_of_source), 
						c.getString(col_of_target)));
				c.moveToNext();
			}
		}
		c.close();
		return dyads;
	}

	/**
	 * Returns the set of undirected dyads irrespective of their lifetimes.
	 * 
	 * @return
	 */
	protected LinkedHashSet<UnorderedDyad> getUndirectedAlterAlterDyads() {
		Cursor c = db.query(ALTER_ALTER_DYADS_TABLE_NAME, new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET}, 
				null, null, null, null, null);
		LinkedHashSet<UnorderedDyad> dyads = new LinkedHashSet<UnorderedDyad>();
		int col_of_source = c.getColumnIndex(DYADS_COL_SOURCE);
		int col_of_target = c.getColumnIndex(DYADS_COL_TARGET);
		if(c.moveToFirst() && col_of_source >= 0 && col_of_target >= 0){
			while(!c.isAfterLast()){
				dyads.add(new UnorderedDyad(c.getString(col_of_source), 
						c.getString(col_of_target)));
				c.moveToNext();
			}
		}
		c.close();
		return dyads;
	}

	/**
	 * Sets the lifetime of the two anti-parallel dyads (source, target) and (target, source) 
	 * equal to the union of its current lifetime with the specified interval.
	 * 
	 * If source and target are not yet in the set of dyads then addAlterAlterDyad(source, target) is called.
	 * 
	 * If necessary adds source and target to set of alters 
	 * or (if necessary) increases their lifetime to include interval.
	 * 
	 * @param interval
	 * @param sourceName
	 * @param targetName
	 */
	public void addToLifetimeOfAlterAlterDyad(TimeInterval interval, String sourceName, String targetName) {
		if(!hasAlterAlterDyad(sourceName, targetName))
			addAlterAlterDyad(sourceName, targetName);
		addToLifetimeOfAlter(interval, sourceName);
		addToLifetimeOfAlter(interval, targetName);
		String historyTableName = ALTER_ALTER_DYADS_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		ContentValues values = new ContentValues();
		values.put(DYADS_COL_SOURCE, sourceName);
		values.put(DYADS_COL_TARGET, targetName);
		unionWithLifetime(interval, historyTableName, elementSelection,
				elementSelectionArgs, values);
		//do the same for the reverse dyad
		elementSelectionArgs = new String[]{targetName, sourceName};
		values = new ContentValues();
		values.put(DYADS_COL_SOURCE, targetName);
		values.put(DYADS_COL_TARGET, sourceName);
		unionWithLifetime(interval, historyTableName, elementSelection,
				elementSelectionArgs, values);
	}

	/**
	 * Returns true if (source, target) is a dyad in the network at some point in the given interval.
	 * 
	 * @param interval
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	public boolean isAlterAlterDyadAt(TimeInterval interval, String sourceName, String targetName) {
		if(!hasAlterAlterDyad(sourceName, targetName))
			return false;
		String historyTableName = ALTER_ALTER_DYADS_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		ContentValues values = new ContentValues();
		values.put(DYADS_COL_SOURCE, sourceName);
		values.put(DYADS_COL_TARGET, targetName);
		return overlapsLifetime(interval, historyTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns the datum id of the given dyad if the lifetime 
	 * contains the given point; null otherwise.
	 * 
	 * @param timePoint
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	public String getAlterAlterDyadDatumIDAt(long timePoint, String sourceName, String targetName) {
		if(!hasAlterAlterDyad(sourceName, targetName))
			return null;
		String historyTableName = ALTER_ALTER_DYADS_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		ContentValues values = new ContentValues();
		values.put(DYADS_COL_SOURCE, sourceName);
		values.put(DYADS_COL_TARGET, targetName);
		return getLifetimeDatumIDAt(timePoint, historyTableName, elementSelection, elementSelectionArgs);
	}


	/**
	 * 
	 * @param sourceName
	 * @param targetName
	 * @return
	 */
	public Lifetime getLifetimeOfAlterAlterDyad(String sourceName, String targetName) {
		String historyTableName = ALTER_ALTER_DYADS_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		return getLifetime(historyTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns the set of all dyads incident to alterName that are in the network at some point
	 * in time included in the given interval. 
	 * 
	 * Returns null if alterName is unknown.
	 * 
	 * @param interval
	 * @param alterName
	 * @return
	 */
	public LinkedHashSet<UnorderedDyad> getIncidentDyadsAt(TimeInterval interval, String alterName) {
		LinkedHashSet<UnorderedDyad> dyads = new LinkedHashSet<UnorderedDyad>();
		String selection = DYADS_COL_SOURCE + " = ? AND " +
				COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ?";
		String[] selectionArgs = {alterName, 
				Long.toString(interval.getEndTime()), 
				Long.toString(interval.getStartTime())};
		Cursor c = db.query(ALTER_ALTER_DYADS_HISTORY_TABLE_NAME, 
				new String[]{DYADS_COL_TARGET, COL_TIME_START, COL_TIME_END}, 
				selection, selectionArgs, null, null, null);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				long start = c.getLong(c.getColumnIndex(COL_TIME_START));
				long end = c.getLong(c.getColumnIndex(COL_TIME_END));
				if(overlap(start, end, interval.getStartTime(), interval.getEndTime()))
					dyads.add(new UnorderedDyad(c.getString(c.getColumnIndex(DYADS_COL_TARGET)),alterName));
				c.moveToNext();
			}
		}
		c.close();
		return dyads;
	}

	/**
	 * Cuts the given interval from the lifetime of the dyads (source, target) and (target, source).
	 * 
	 * First removes all associated dyad attribute values at the given interval.
	 * 
	 * Note the behavior of Lifetime.cutOut(interval) if interval is a time point.
	 * 
	 * @param interval
	 * @param sourceName
	 * @param targetName
	 */
	public void removeAlterAlterDyadAt(TimeInterval interval, String sourceName, String targetName) {
		//associated attributes
		LinkedHashSet<String> attrNames = getAttributeNames(PersonalNetwork.DOMAIN_ALTER_ALTER);
		if(attrNames != null){
			for(String attrName : attrNames){
				setAttributeValueAt(interval, attrName, 
						AlterAlterDyad.getInstance(sourceName, targetName), 
						PersonalNetwork.VALUE_NOT_ASSIGNED);
			}
		}
		String historyTableName = ALTER_ALTER_DYADS_HISTORY_TABLE_NAME;
		String elementSelection =  DYADS_COL_SOURCE + " = ? AND " + DYADS_COL_TARGET + " = ? ";
		String[] elementSelectionArgs = {sourceName, targetName};
		ContentValues values = new ContentValues();
		values.put(DYADS_COL_SOURCE, sourceName);
		values.put(DYADS_COL_TARGET, targetName);
		cutOutOfLifetime(interval, historyTableName, elementSelection, elementSelectionArgs, values);
		//do the same for the reverse dyad
		elementSelectionArgs = new String[]{targetName, sourceName};
		values = new ContentValues();
		values.put(DYADS_COL_SOURCE, targetName);
		values.put(DYADS_COL_TARGET, sourceName);
		cutOutOfLifetime(interval, historyTableName, elementSelection, elementSelectionArgs, values);
	}

	/**
	 * Returns the number of undirected pairs of alters that form a dyad at some point in the given interval.
	 * @return
	 */
	public int getNumberOfUndirectedDyadsAt(TimeInterval interval) {
		return getUndirectedAlterAlterDyadsAt(interval).size();//TODO: this should be possible in a more efficient way
	}

	/**
	 * Returns the set of directed dyads that are in the network at some point in the given interval.
	 * 
	 * @return
	 */
	public LinkedHashSet<OrderedDyad> getDirectedAlterAlterDyadsAt(TimeInterval interval) {
		LinkedHashSet<OrderedDyad> dyads = new LinkedHashSet<OrderedDyad>();
		String selection = COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ?";
		String[] selectionArgs = {Long.toString(interval.getEndTime()), 
				Long.toString(interval.getStartTime())};
		Cursor c = db.query(ALTER_ALTER_DYADS_HISTORY_TABLE_NAME, 
				new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET, COL_TIME_START, COL_TIME_END}, 
				selection, selectionArgs, null, null, null);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				long start = c.getLong(c.getColumnIndex(COL_TIME_START));
				long end = c.getLong(c.getColumnIndex(COL_TIME_END));
				if(overlap(start, end, interval.getStartTime(), interval.getEndTime())){
					dyads.add(new OrderedDyad(c.getString(c.getColumnIndex(DYADS_COL_SOURCE)), 
							c.getString(c.getColumnIndex(DYADS_COL_TARGET))));
				}
				c.moveToNext();
			}
		}
		c.close();
		return dyads;
	}

	/**
	 * Returns the set of undirected dyads that are in the network at some point in the given interval.
	 * 
	 * @return
	 */
	public LinkedHashSet<UnorderedDyad> getUndirectedAlterAlterDyadsAt(TimeInterval interval) {
		LinkedHashSet<UnorderedDyad> dyads = new LinkedHashSet<UnorderedDyad>();
		String selection = COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ?";
		String[] selectionArgs = {Long.toString(interval.getEndTime()), 
				Long.toString(interval.getStartTime())};
		Cursor c = db.query(ALTER_ALTER_DYADS_HISTORY_TABLE_NAME, 
				new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET, COL_TIME_START, COL_TIME_END}, 
				selection, selectionArgs, null, null, null);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				long start = c.getLong(c.getColumnIndex(COL_TIME_START));
				long end = c.getLong(c.getColumnIndex(COL_TIME_END));
				if(overlap(start, end, interval.getStartTime(), interval.getEndTime())){
					dyads.add(new UnorderedDyad(c.getString(c.getColumnIndex(DYADS_COL_SOURCE)), 
							c.getString(c.getColumnIndex(DYADS_COL_TARGET))));
				}
				c.moveToNext();
			}
		}
		c.close();
		return dyads;
	}

	/**
	 * Adds a new alter with an empty lifetime to the network.
	 * 
	 * Leading and trailing white spaces are removed from the alter name. The method does nothing
	 * if alterName is null or has length zero. 
	 * 
	 * @param alterName
	 */
	protected void addAlter(String alterName) {
		if(alterName == null || alterName.trim().length() == 0)
			return;
		alterName = alterName.trim();
		if(hasAlter(alterName)){
			activity.reportInfo("alter " + alterName + " is already in the network");
			return;
		}
		ContentValues values = new ContentValues();
		values.put(ALTERS_COL_NAME, alterName);
		db.insert(ALTERS_TABLE_NAME, null, values);
	}

	/**
	 * Returns true if altername is an alter (independent of whether its lifetime is empty or not).
	 * 
	 * @param alterName
	 */
	protected boolean hasAlter(String alterName) {
		if(alterName == null)
			return false;
		String selection = ALTERS_COL_NAME + " = ?";
		String[] selectionArgs = {alterName};
		Cursor c = db.query(ALTERS_TABLE_NAME, new String[]{ALTERS_COL_NAME}, 
				selection, selectionArgs, null, null, null);
		boolean ret = c.getCount() > 0;
		c.close();
		return ret;
	}

	/**
	 * Returns the list of all alters in the network
	 * (independent of whether their lifetimes are empty or not).
	 * 
	 */
	protected LinkedHashSet<String> getAlters(){
		String orderBy = ALTERS_COL_NAME + " ASC";
		Cursor c = db.query(ALTERS_TABLE_NAME, new String[]{ALTERS_COL_NAME}, 
				null, null, null, null, 
				orderBy);
		LinkedHashSet<String> names = new LinkedHashSet<String>();
		int col_of_name = c.getColumnIndex(ALTERS_COL_NAME);
		if(c.moveToFirst() && col_of_name >= 0){
			while(!c.isAfterLast()){
				names.add(c.getString(col_of_name));
				c.moveToNext();
			}
		}
		c.close();
		return names;
	}

	/**
	 * Renames the alter oldName into newName.
	 * 
	 * Does nothing if oldName is not an alter or if
	 * there is already an alter called newName in the network history. 
	 * 
	 * @param oldName
	 * @param newName
	 */
	public void renameAlter(String oldName, String newName) {
		if(newName == null)
			return;
		if(!hasAlter(oldName))
			return;
		newName = newName.trim();
		if(newName.length() == 0)
			return;
		if(hasAlter(newName))
			return;
		db.beginTransaction();
		int editRowCount = 0;
		try {
			//rename alter in all tables in which it might be in the column ALTERS_COL_NAME
			ContentValues values = new ContentValues();
			values.put(ALTERS_COL_NAME, newName);
			String selection = ALTERS_COL_NAME + " = ?";
			String[] selectionArgs = {oldName};
			editRowCount = editRowCount + db.update(ALTERS_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(ALTERS_HISTORY_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(ALTER_ATTRIBS_VALUES_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(EGO_ALTER_ATTRIBS_VALUES_TABLE_NAME, values, selection, selectionArgs);
			//rename alter in all tables in which it might be in the column DYADS_COL_SOURCE
			values = new ContentValues();
			values.put(DYADS_COL_SOURCE, newName);
			selection = DYADS_COL_SOURCE + " = ?";
			editRowCount = editRowCount + db.update(TIES_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(TIES_HISTORY_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(ALTER_ALTER_DYADS_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(ALTER_ALTER_DYADS_HISTORY_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(ALTER_ALTER_ATTRIBS_VALUES_TABLE_NAME, values, selection, selectionArgs);
			//rename alter in all tables in which it might be in the column DYADS_COL_TARGET
			values = new ContentValues();
			values.put(DYADS_COL_TARGET, newName);
			selection = DYADS_COL_TARGET + " = ?";
			editRowCount = editRowCount + db.update(TIES_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(TIES_HISTORY_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(ALTER_ALTER_DYADS_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(ALTER_ALTER_DYADS_HISTORY_TABLE_NAME, values, selection, selectionArgs);
			editRowCount = editRowCount + db.update(ALTER_ALTER_ATTRIBS_VALUES_TABLE_NAME, values, selection, selectionArgs);
			if(oldName.equals(getSelectedAlterInHistory())){
				setSelectedAlterInHistory(newName);
			}
			db.setTransactionSuccessful();
			setLastChange(System.currentTimeMillis());
		} finally {
			db.endTransaction();
		}
		//Toast.makeText(activity, "updated " + editRowCount + " rows in your network history", Toast.LENGTH_LONG).show();
	}

	/**
	 * Returns an arbitrary alter who is currently in the network; 
	 * null if currently there is no alter in the network
	 * 
	 * @return
	 */
	public String getAnyCurrentAlter(){
		String currentTime = Long.toString(System.currentTimeMillis());
		String selection = COL_TIME_START + " <= ? AND " + COL_TIME_END + " >= ?";
		String[] selectionArgs = {currentTime, currentTime};
		Cursor c = db.query(ALTERS_HISTORY_TABLE_NAME, 
				new String[]{ALTERS_COL_NAME}, 
				selection, selectionArgs, null, null, null, "1");
		int col_of_name = c.getColumnIndex(ALTERS_COL_NAME);
		if(c.moveToFirst() && col_of_name >= 0){
			String ret = c.getString(col_of_name);
			c.close();
			return ret;
		}
		c.close();
		return null;
	}
	/**
	 * Returns the name of the currently selected alter; null if none is selected.
	 * 
	 * @return
	 */
	public String getSelectedAlterInHistory() {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_ALTER_IN_HISTORY};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		int col_of_value = c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE);
		if(!c.moveToFirst()){
			c.close();
			return getAnyCurrentAlter();
		}
		String ret = c.getString(col_of_value);
		c.close();
		return ret;
	}

	/**
	 * Sets the alter selection to the given alter. 
	 * 
	 * Does not change the current selection if alterName is not an alter in the network history
	 * 
	 * @param alterName
	 */
	public void setSelectedAlterInHistory(String alterName) {
		if(!hasAlter(alterName))
			return;
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_SELECTED_ALTER_IN_HISTORY);
		values.put(PROPERTIES_COL_VALUE, alterName);
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_SELECTED_ALTER_IN_HISTORY};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(!c.moveToFirst()){
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		} else { 
			db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		}
		c.close();
	}

	/**
	 * Sets the lifetime of the given alter 
	 * equal to the union of its current lifetime with the specified interval.
	 * 
	 * If alterName is not yet in the network then addAlter(alterName) is called.
	 * 
	 * @param interval
	 * @param alterName
	 */
	public void addToLifetimeOfAlter(TimeInterval interval, String alterName) {
		if(!hasAlter(alterName))
			addAlter(alterName);
		String historyTableName = ALTERS_HISTORY_TABLE_NAME;
		String elementSelection =  ALTERS_COL_NAME + " = ?";
		String[] elementSelectionArgs = {alterName};
		ContentValues values = new ContentValues();
		values.put(ALTERS_COL_NAME, alterName);
		unionWithLifetime(interval, historyTableName, elementSelection,
				elementSelectionArgs, values);
		setLastChange(System.currentTimeMillis());
	}

	/**
	 * Returns true if alterName is an alter in the network at some point in time included in interval.
	 * 
	 * @param alterName
	 * @return
	 */
	public boolean hasAlterAt(TimeInterval interval, String alterName) {
		if(!hasAlter(alterName))
			return false;
		String historyTableName = ALTERS_HISTORY_TABLE_NAME;
		String elementSelection =  ALTERS_COL_NAME + " = ?";
		String[] elementSelectionArgs = {alterName};
		return overlapsLifetime(interval, historyTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns the datum id of the lifetime of the given alter
	 *  if alterName is an alter in the network at the given point in time.
	 * 
	 * @param alterName
	 * @return
	 */
	public String getAlterDatumIDAt(long timePoint, String alterName) {
		if(!hasAlter(alterName))
			return null;
		String historyTableName = ALTERS_HISTORY_TABLE_NAME;
		String elementSelection =  ALTERS_COL_NAME + " = ?";
		String[] elementSelectionArgs = {alterName};
		return getLifetimeDatumIDAt(timePoint, historyTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * 
	 * @param alterName
	 * @return null if alterName is unknown, his/her lifetime otherwise
	 */
	public Lifetime getLifetimeOfAlter(String alterName) {
		if(!hasAlter(alterName))
			return null;
		String historyTableName = ALTERS_HISTORY_TABLE_NAME;
		String elementSelection =  ALTERS_COL_NAME + " = ?";
		String[] elementSelectionArgs = {alterName};
		return getLifetime(historyTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Cuts the given interval from the lifetime of the specified alter.
	 * 
	 * First removes interval from the lifetime of all incident ties and dyads as well as
	 * all associated attribute values.
	 * 
	 * Note the behavior of Lifetime.cutOut(interval) if interval is a time point.
	 * 
	 * @param interval 
	 * @param alterName
	 */
	public void removeAlterAt(TimeInterval interval, String alterName) {
		if(!hasAlter(alterName))
			return;
		//incident ties
		LinkedHashSet<String> neighbors = getNeighborsAt(interval, alterName);
		if(neighbors != null){
			for(String neighbor : neighbors){
				removeTieAt(interval, alterName, neighbor);
			}
		}
		//incident dyads
		LinkedHashSet<UnorderedDyad> dyads = getIncidentDyadsAt(interval, alterName);
		if(dyads != null){
			for(UnorderedDyad dyad : dyads){
				removeAlterAlterDyadAt(interval, dyad.source(), dyad.target());
			}
		}
		//associated alter attributes
		LinkedHashSet<String> attrNames = getAttributeNames(PersonalNetwork.DOMAIN_ALTER);
		if(attrNames != null){
			for(String attrName : attrNames){
				setAttributeValueAt(interval, attrName, 
						Alter.getInstance(alterName), 
						PersonalNetwork.VALUE_NOT_ASSIGNED);
			}
		}
		//associated ego alter attributes
		attrNames = getAttributeNames(PersonalNetwork.DOMAIN_EGO_ALTER);
		if(attrNames != null){
			for(String attrName : attrNames){
				String dir = getAttributeDirectionType(PersonalNetwork.DOMAIN_EGO_ALTER, attrName);
				if(dir.equals(PersonalNetwork.DYAD_DIRECTION_OUT) 
						|| dir.equals(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC) 
						|| dir.equals(PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC))
					setAttributeValueAt(
							interval, attrName, 
							EgoAlterDyad.getOutwardInstance(alterName),
							PersonalNetwork.VALUE_NOT_ASSIGNED);
				if(dir.equals(PersonalNetwork.DYAD_DIRECTION_IN) 
						|| dir.equals(PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC))
					setAttributeValueAt(
							interval, attrName, 
							EgoAlterDyad.getInwardInstance(alterName), 
							PersonalNetwork.VALUE_NOT_ASSIGNED);
			}
		}
		String historyTableName = ALTERS_HISTORY_TABLE_NAME;
		String elementSelection =  ALTERS_COL_NAME + " = ?";
		String[] elementSelectionArgs = {alterName};
		ContentValues values = new ContentValues();
		values.put(ALTERS_COL_NAME, alterName);
		cutOutOfLifetime(interval, historyTableName, elementSelection, elementSelectionArgs, values);
		//if the cut interval includes the current time and alter name is the currently selected alter remove it
		if(interval.contains(System.currentTimeMillis())){
			if(alterName.equals(getSelectedAlter()))
				setSelectedAlterForced(null);
			if(alterName.equals(getSelectedSecondAlter()))
				setSelectedSecondAlterForced(null);
		}
		setLastChange(System.currentTimeMillis());
	}

	/**
	 * Returns the number of alters that are in the network at some point in time included in interval.
	 * 
	 * @return
	 */
	public int getNumberOfAltersAt(TimeInterval interval) {
		return getAltersAt(interval).size();//TODO: do this in a more efficient way
	}

	/**
	 * Returns the set of all alters that are in the network at some point in time in the given interval.
	 * 
	 * @return
	 */
	public LinkedHashSet<String> getAltersAt(TimeInterval interval) {
		LinkedHashSet<String> alters = new LinkedHashSet<String>();
		String selection = COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ?";
		String[] selectionArgs = {Long.toString(interval.getEndTime()), 
				Long.toString(interval.getStartTime())};
		Cursor c = db.query(ALTERS_HISTORY_TABLE_NAME, 
				new String[]{ALTERS_COL_NAME, COL_TIME_START, COL_TIME_END}, 
				selection, selectionArgs, null, null, null);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				long start = c.getLong(c.getColumnIndex(COL_TIME_START));
				long end = c.getLong(c.getColumnIndex(COL_TIME_END));
				if(overlap(start, end, interval.getStartTime(), interval.getEndTime())){
					alters.add(c.getString(c.getColumnIndex(ALTERS_COL_NAME)));
				}
				c.moveToNext();
			}
		}
		c.close();
		return alters;
	}

	/**
	 * Creates a new attribute for the given domain. 
	 * 
	 * Removes leading and trailing white spaces from
	 * attributeName. Silently does nothing if attributeName is null or attributeName.trim().length() == 0,
	 * or if an attribute with this name already exists for the given domain, or if any of the types is unknown.
	 *  
	 * The domain is either {@link PersonalNetwork#DOMAIN_EGO}, 
	 * {@link PersonalNetwork#DOMAIN_ALTER},
	 * {@link PersonalNetwork#DOMAIN_EGO_ALTER}, or 
	 * {@link PersonalNetwork#DOMAIN_ALTER_ALTER} <br/>
	 * 
	 * The direction type is ignored for ego or alter attributes.
	 * 
	 * @param domain of the newly created attribute
	 * @param attributeName string identifier for this attribute (unique within the given domain)
	 * @param attributeDescription free text describing the attribute.
	 * @param valueType 
	 * @param directionType of dyad attributes (ego-alter or alter-alter); is ignored otherwise
	 * @param dynamicType STATE or EVENT
	 */
	public void addAttribute(String domain, String attributeName, String attributeDescription, 
			int valueType, String directionType, String dynamicType) {
		if(attributeName == null || attributeName.trim().length() == 0)
			return;
		attributeName = attributeName.trim();
		if(!attributeDomainExists(domain))
			return;
		if(hasAttribute(domain, attributeName))
			return;
		if(!attributeValueTypeExists(valueType))
			return;
		if(!attributeDynamicTypeExists(dynamicType))
			return;
		if(domainHasAttributeDirectionType(domain) && 
				!attributeDirectionTypeExistsForDomain(domain, directionType))
			return;
		//now everything is ok
		ContentValues values = new ContentValues();
		values.put(ATTRIBUTES_COL_NAME, attributeName);
		values.put(ATTRIBUTES_COL_VALUE_TYPE, valueType);
		values.put(ATTRIBUTES_COL_DYNAMIC_TYPE, dynamicType);
		if(domainHasAttributeDirectionType(domain))
			values.put(ATTRIBUTES_COL_DIRECTION_TYPE, directionType);
		values.put(ATTRIBUTES_COL_DESCRIPTION, attributeDescription);
		String tableName = getAttributeNamesTableNameForDomain(domain);
		db.insert(tableName, null, values);
		addSecondaryAttribute(domain, attributeName, 
				getSecondaryAttributeNameTimestampStart(), 
				"time stamp when start time has been set", PersonalNetwork.ATTRIB_TYPE_TEXT);
		addSecondaryAttribute(domain, attributeName, 
				getSecondaryAttributeNameTimestampEnd(), 
				"time stamp when end time has been set", PersonalNetwork.ATTRIB_TYPE_TEXT);
	}

	/**
	 * Sets the set of allowed values for an attribute to the string values in choices
	 * union the set of values that are assigned to some element at some point in time. 
	 * 
	 * The new set of allowed choices apply to the whole time line.
	 * 
	 * Silently does nothing if no attribute with this
	 * name exists or if the type of this attribute is not CHOICE. 
	 *
	 * Removes leading and trailing white spaces of Strings in choices. Uses only values in
	 * choices that are not null and not empty strings.
	 * @param attributeName
	 * @param choices
	 */
	public void setAttributeChoices(String domain, String attributeName, LinkedHashSet<String> choices) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo("attribute " + attributeName + " is not an attribute for " +
					"the domain " + domain);
			return;			
		}
		if(getAttributeValueType(domain, attributeName) != PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
			activity.reportInfo("attribute " + attributeName + " is not of finite choice type");
			return;						
		}
		LinkedHashSet<String> currentValues = getUniqueValuesForAttributeAt(
				TimeInterval.getMaxInterval(), domain, attributeName);
		String tableName = getAttributeChoicesTableNameForDomain(domain);
		setAttributeChoices(attributeName, tableName, choices, currentValues);
	}

	/**
	 * Adds choice.trim() to the set of allowed choices for the given attribute.
	 * 
	 * Does nothing if there is no such attribute or if it has not the value type choice or
	 * if choice is null or its trimmed value has length zero or if it is already included
	 * in the set of choices.
	 * 
	 * @param domain
	 * @param attributeName
	 * @param choice
	 */
	public void addAttributeChoice(String domain, String attributeName, String choice) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo("attribute " + attributeName + " is not an attribute for " +
					"the domain " + domain);
			return;			
		}
		if(getAttributeValueType(domain, attributeName) != PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
			activity.reportInfo("attribute " + attributeName + " is not of finite choice type");
			return;						
		}
		if(choice == null || choice.trim().length() == 0)
			return;
		choice = choice.trim();
		if(hasAttributeChoice(domain, attributeName, choice))
			return;
		String tableName = getAttributeChoicesTableNameForDomain(domain);
		ContentValues values = new ContentValues();
		values.put(ATTRIBUTES_COL_NAME, attributeName);
		values.put(ATTRIBUTES_COL_CHOICE, choice);
		db.insert(tableName, null, values);
	}

	/**
	 * Sets (updates) the type (specified by its name) of the given attribute.
	 * 
	 * Silently does nothing if there is no attribute with the given name or if
	 * this already has that type or if the type is unknown, or if the 
	 * values (at any point in time) are not compatible with the new type. (Setting the type to choice will
	 * never cause this failure since the set of allowed values will be set to these 
	 * values.)  
	 * @param attributeName
	 * @param attributeValueTypeName
	 */
	public void setAttributeValueType(String domain, String attributeName, String attributeValueTypeName) {
		for(int i = 0; i < PersonalNetwork.ATTRIB_TYPE_NAMES.length; ++i){
			if(PersonalNetwork.ATTRIB_TYPE_NAMES[i].equals(attributeValueTypeName)){
				setAttributeValueType(domain, attributeName, i);
				return;
			}
		}
		// when we are here something is wrong
		activity.reportError("attribute type " + attributeValueTypeName + " is unknown");
	}

	/**
	 * Sets (updates) the type (specified by its integer id) of the given attribute.
	 * 
	 * Silently does nothing if there is no attribute with the given name or if
	 * this attribute already has that type or if the type is unknown, or if the 
	 * values (at any point in time) are not compatible with the new type. (Setting the type to CHOICE will
	 * never cause this failure since the set of allowed values will be set to these 
	 * values.)  
	 * @param attributeName
	 * @param attributeValueType
	 */
	public void setAttributeValueType(String domain, String attributeName, int attributeValueType) {
		if(!attributeValueTypeExists(attributeValueType)){
			activity.reportInfo("unknown attribute type: " + attributeValueType);
			return;
		}
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo("attribute: " + attributeName + " does not exist for" +
					" the domain " + domain);
			return;
		}
		if(attributeValueType == getAttributeValueType(domain, attributeName)){
			activity.reportInfo("attribute: " + attributeName 
					+ " has already type " + attributeValueType);
			return;
		}
		if(attributeValueType == PersonalNetwork.ATTRIB_TYPE_NUMBER){
			//check for compatibility of values
			LinkedHashSet<String> values = getUniqueValuesForAttributeAt(
					TimeInterval.getMaxInterval(), domain, attributeName);
			if(!allValuesAreNumbers(values)){
				activity.reportInfo(" not all current values are numbers");
				return;
			}
		}
		if(getAttributeValueType(domain, attributeName) == PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
			//the current type is finite choice; this will be changed: remove the choices
			String selection = ATTRIBUTES_COL_NAME + " = ?";
			String[] selectionArgs = {attributeName};
			// remove all choices set for this attribute
			db.delete(ALTER_ALTER_ATTRIBS_CHOICES_TABLE_NAME, selection, selectionArgs);
		}
		//change the type
		ContentValues values = new ContentValues();
		values.put(ATTRIBUTES_COL_NAME, attributeName);
		values.put(ATTRIBUTES_COL_VALUE_TYPE, attributeValueType);
		String selection = ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {attributeName};
		db.update(ALTER_ALTER_ATTRIBS_NAMES_TABLE_NAME, values, selection, selectionArgs);
		if(attributeValueType == PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
			// make current values the allowed choices
			setAttributeChoices(domain, attributeName, 
					getUniqueValuesForAttributeAt(TimeInterval.getMaxInterval(), 
							domain, attributeName));
		}
	}

	/**
	 * Updates the description of the given attribute.
	 *  
	 * Silently does nothing if there is no attribute with the given name.
	 * 
	 * @param attributeName
	 * @param description
	 */
	public void setAttributeDescription(String domain, String attributeName, String description) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo("attribute " + attributeName + " is not an attribute for the " +
					"domain " + domain);
			return;			
		}
		String selection = ATTRIBUTES_COL_NAME + " = ?";
		String[] selectArgs = {attributeName};
		ContentValues values = new ContentValues();
		values.put(ATTRIBUTES_COL_DESCRIPTION, description);
		db.update(ALTER_ALTER_ATTRIBS_NAMES_TABLE_NAME, values, selection, selectArgs);
	}

	/**
	 * Sets the value of the specified attribute for the specified element at the given interval.
	 * 
	 *  Leading and trailing white spaces are removed from textValue. 
	 *  
	 *  Silently does nothing if there is no such attribute or if the specified value is not compatible
	 *  with the attribute value type.
	 *  
	 *  Adds the specified element (if necessary) and increases its lifetime (if necessary)
	 * 
	 *  If the textValue is null, has length zero, or is equal to "N/A" then the value
	 *  of this attribute for the given element at the given interval is removed.
	 *    
	 * Does nothing if interval is a time point that is included in a previously set interval 
	 * which is not a point. Also see the behavior of Lifetime.cutOut for this case.
	 *    
	 *  If the direction type is SYMMETRIC, then the same value will be set for the reverse dyad.
	 *  
	 * @param interval   
	 * @param attributeName
	 * @param sourceAlterName
	 * @param targetAlterName
	 * @param textValue
	 */
	public void setAttributeValueAt(TimeInterval interval, String attributeName, 
			Element element, String textValue) {
		if(element == null)
			return;
		String domain = element.getDomain();
		if(!hasAttribute(domain, attributeName))
			return;
		int valueType = getAttributeValueType(domain, attributeName);
		if(PersonalNetwork.ATTRIB_TYPE_NUMBER == valueType){
			try{
				Double.parseDouble(textValue);
			} catch(Exception e){
				return;
			}
		}
		if(PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE == valueType){
			if(!hasAttributeChoice(domain, attributeName, textValue))
				addAttributeChoice(domain, attributeName, textValue);
		}
		//increase the lifetime of the element if necessary
		if(PersonalNetwork.DOMAIN_ALTER.equals(domain) || 
				PersonalNetwork.DOMAIN_EGO_ALTER.equals(domain)){
			String alterName;
			if(PersonalNetwork.DOMAIN_ALTER.equals(domain))
				alterName = ((Alter) element).getName();
			else
				alterName = ((EgoAlterDyad) element).getName();
			addToLifetimeOfAlter(interval, alterName);
		}
		if(PersonalNetwork.DOMAIN_ALTER_ALTER.equals(domain)){
			AlterAlterDyad dyad = (AlterAlterDyad) element;
			addToLifetimeOfAlterAlterDyad(interval, dyad.getSourceName(), dyad.getTargetName());
		}
		setAttributeValueAtAlreadyChecked(interval, attributeName, element, textValue);
		if(element.isDyadicElement() && PersonalNetwork.DYAD_DIRECTION_SYMMETRIC.
				equals(getAttributeDirectionType(domain, attributeName))){
			setAttributeValueAtAlreadyChecked(interval, attributeName, element.getReverseElement(), textValue);
		}
	}

	/**
	 * Sets the value of the given attribute for the given element in the given interval.
	 * 
	 * The feasibility is not checked and this method does not set the 
	 * value for the reverse dyad for symmetric dyadic attributes. 
	 * 
	 * @param interval
	 * @param attributeName
	 * @param element
	 * @param textValue
	 */
	private void setAttributeValueAtAlreadyChecked(TimeInterval interval,
			String attributeName, Element element, String textValue) {
		String valueHistoryTableName = getAttributeValuesTableNameForDomain(element.getDomain());
		String elementSelection =  element.getAttributeElementSelectionString();
		String[] elementSelectionArgs = element.getAttributeElementSelectionArgs(attributeName);
		ContentValues dbContentValues = element.getAttributeElementContentValues(attributeName);
		setAttributeValueAt(interval, textValue, 
				valueHistoryTableName, elementSelection, elementSelectionArgs, 
				dbContentValues);
	}

	/**
	 * Returns true if there is an attribute with the given name for the given domain.
	 * 
	 * @param domain
	 * @param attributeName
	 * @return 
	 */
	public boolean hasAttribute(String domain, String attributeName) {
		if(attributeName == null)
			return false;
		if(!attributeDomainExists(domain))
			return false;
		String selection = ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {attributeName};
		String tableName = getAttributeNamesTableNameForDomain(domain);
		Cursor c = db.query(tableName, 
				new String[]{ATTRIBUTES_COL_NAME}, 
				selection, selectionArgs, null, null, null);
		boolean ret = c.getCount() > 0;
		c.close();
		return ret;
	}

	/**
	 * Checks whether choice is among the set of allowed choices for the given attribute.
	 * 
	 * Returns false if there is no such attribute or if its value type is not choice.
	 * 
	 * @param domain
	 * @param attributeName
	 * @param choice
	 * @return
	 */
	public boolean hasAttributeChoice(String domain, String attributeName, String choice) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo("attribute " + attributeName + " is not an alter-alter attribute");
			return false;			
		}
		if(getAttributeValueType(domain, attributeName) != PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
			activity.reportInfo("attribute " + attributeName + " is not of finite choice type");
			return false;						
		}
		String tableName = getAttributeChoicesTableNameForDomain(domain);
		String selection = ATTRIBUTES_COL_NAME + " = ? AND " + ATTRIBUTES_COL_CHOICE + " = ? ";
		String[] selectionArgs = {attributeName, choice};
		Cursor c = db.query(tableName, 
				new String[]{ATTRIBUTES_COL_CHOICE}, selection, 
				selectionArgs, null, null, null);
		boolean ret = c.getCount() > 0;
		c.close();
		return ret;
	}

	/**
	 * Returns the set of allowed values currently set for this attribute.
	 * 
	 * Returns null if there is no such attribute or if
	 * the type of this attribute is not CHOICE.
	 * 
	 * @param domain
	 * @param attributeName
	 * @return
	 */
	public LinkedHashSet<String> getAttributeChoices(String domain, String attributeName) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo("attribute " + attributeName + " is not an alter-alter attribute");
			return null;			
		}
		if(getAttributeValueType(domain, attributeName) != PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
			activity.reportInfo("attribute " + attributeName + " is not of finite choice type");
			return null;						
		}
		String tableName = getAttributeChoicesTableNameForDomain(domain);
		LinkedHashSet<String> choices = new LinkedHashSet<String>();
		String selection = ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {attributeName};
		Cursor c = db.query(tableName, 
				new String[]{ATTRIBUTES_COL_CHOICE}, selection, 
				selectionArgs, null, null, ATTRIBUTES_COL_CHOICE + " ASC");
		int col = c.getColumnIndex(ATTRIBUTES_COL_CHOICE);
		if(c.moveToFirst() && col >= 0){
			while(!c.isAfterLast()){
				choices.add(c.getString(col));
				c.moveToNext();
			}
		}
		c.close();
		return choices;
	}

	/**
	 * Returns the value type (as an integer id) of the specified attribute.
	 * 
	 * Returns -1 (a non-existing attribute value type) if there is no attribute with the given name
	 * for the given domain.
	 * 
	 * @param domain
	 * @param attributeName
	 * @return
	 */
	public int getAttributeValueType(String domain, String attributeName) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo("attribute " + attributeName + " is not an attribute for " +
					"the domain " + domain);
			return -1;			
		}
		String type = getAttributeProperty(domain, attributeName, ATTRIBUTES_COL_VALUE_TYPE);
		if(type == null)
			return -1;
		return Integer.parseInt(type);
	}

	/**
	 * Returns the value type (as a string) of the specified attribute.
	 * 
	 * Returns null if there is no attribute with the given name
	 * for the given domain.
	 * 
	 * @param domain
	 * @param attributeName
	 * @return
	 */
	public String getAttributeValueTypeName(String domain, String attributeName) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo("attribute " + attributeName + " is not an attribute for " +
					"the domain " + domain);
			return null;			
		}
		String type = getAttributeProperty(domain, attributeName, ATTRIBUTES_COL_VALUE_TYPE);
		if(type == null)
			return null;
		return PersonalNetwork.ATTRIB_TYPE_NAMES[Integer.parseInt(type)];
	}

	/**
	 * Returns the description of the specified attribute.
	 * 
	 * Returns null if there is no such attribute.
	 * 
	 * @param attributeName
	 * @return
	 */
	public String getAttributeDescription(String domain, String attributeName) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo(attributeName + " is not an attribute for " +
					"the domain " + domain);
			return null;
		}
		return getAttributeProperty(domain, attributeName, ATTRIBUTES_COL_DESCRIPTION);
	}

	/**
	 * Returns the dynamic type of the specified attribute.
	 * 
	 * Returns null if there is no such attribute.
	 * 
	 * @param attributeName
	 * @return
	 */
	public String getAttributeDynamicType(String domain, String attributeName) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo(attributeName + " is not an attribute for " +
					"the domain " + domain);
			return null;
		}
		return getAttributeProperty(domain, attributeName, ATTRIBUTES_COL_DYNAMIC_TYPE);
	}

	/**
	 * Returns the direction type of the specified attribute.
	 * 
	 * Returns null if there is no attribute with the given name
	 * for the given domain or if the domain has no attribute direction type
	 * 
	 * @param domain
	 * @param attributeName
	 * @return
	 */
	public String getAttributeDirectionType(String domain, String attributeName) {
		if(!hasAttribute(domain, attributeName)){
			activity.reportInfo("attribute " + attributeName + " is not an attribute for " +
					"the domain " + domain);
			return null;			
		}
		if(!domainHasAttributeDirectionType(domain))
			return null;
		return getAttributeProperty(domain, attributeName, ATTRIBUTES_COL_DIRECTION_TYPE);
	}

	/**
	 * Returns the set of attribute names that are declared for the given domain.
	 * 
	 * Only returns the names of attributes that are not system attributes, i.e., that
	 * do not start with the prefix egosmart: 
	 * 
	 * @return
	 */
	public LinkedHashSet<String> getAttributeNames(String domain) {
		String tableName = getAttributeNamesTableNameForDomain(domain);
		Cursor c = db.query(tableName, 
				new String[]{ATTRIBUTES_COL_NAME}, 
				null, null, null, null, ATTRIBUTES_COL_NAME + " ASC");
		LinkedHashSet<String> names = new LinkedHashSet<String>();
		int col = c.getColumnIndex(ATTRIBUTES_COL_NAME);
		if(c.moveToFirst() && col >= 0){
			while(!c.isAfterLast()){
				String name = c.getString(col);
				if(!name.startsWith(PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART))
					names.add(name);
				c.moveToNext();
			}
		}
		c.close();
		return names;
	}

	/**
	 * Returns the set of attribute names that are declared for the given domain.
	 * 
	 * Returns all attributes including system attributes, i.e., those that
	 * start with the prefix egosmart: 
	 * 
	 * @return
	 */
	private LinkedHashSet<String> getAllAttributeNames(String domain) {
		String tableName = getAttributeNamesTableNameForDomain(domain);
		Cursor c = db.query(tableName, 
				new String[]{ATTRIBUTES_COL_NAME}, 
				null, null, null, null, ATTRIBUTES_COL_NAME + " ASC");
		LinkedHashSet<String> names = new LinkedHashSet<String>();
		int col = c.getColumnIndex(ATTRIBUTES_COL_NAME);
		if(c.moveToFirst() && col >= 0){
			while(!c.isAfterLast()){
				String name = c.getString(col);
				names.add(name);
				c.moveToNext();
			}
		}
		c.close();
		return names;
	}

	/**
	 * Returns the value of the specified attribute for the given element at the given point in time.
	 * 
	 * Returns VALUE_NOT_ASSIGNED if there is no such attribute or if there is no such element
	 * of if no attribute value is set for the given attribute and elemen at
	 * the given point in time.
	 * 
	 * @param timePoint 
	 * @param domain 
	 * @param attributeName
	 * @param element
	 * @return
	 */
	public String getAttributeValueAt(long timePoint, String attributeName, Element element) {
		String domain = element.getDomain();
		String valueHistoryTableName = getAttributeValuesTableNameForDomain(domain);
		String elementSelection =  element.getAttributeElementSelectionString();
		String[] elementSelectionArgs = element.getAttributeElementSelectionArgs(attributeName);
		return getAttributeValueAt(timePoint, valueHistoryTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns the datum id of the given attribute for the given element at the given point in time.
	 * 
	 * Returns VALUE_NOT_ASSIGNED if the element or the attribute is unknown or if
	 * no attribute value is set for the given attribute and element at
	 * the given point in time.
	 * 
	 * @param timePoint 
	 * @param attributeName
	 * @param element
	 * @return
	 */
	public String getAttributeDatumIDAt(long timePoint, String attributeName, Element element) {
		String valueHistoryTableName = getAttributeValuesTableNameForDomain(element.getDomain());
		String elementSelection =  element.getAttributeElementSelectionString();
		String[] elementSelectionArgs = element.getAttributeElementSelectionArgs(attributeName);
		return getAttributeDatumIDAt(timePoint, valueHistoryTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns the values of the specified attribute for the given element
	 * over the whole time line.
	 * 
	 * Returns values with an empty support if the attribute or the element are unknown. 
	 *  
	 * @param attributeName
	 * @param sourceAlterName
	 * @param targetAlterName
	 * @return
	 */
	public TimeVaryingAttributeValues getAttributeValues(String attributeName, Element element) {
		String valueHistoryTableName = getAttributeValuesTableNameForDomain(element.getDomain());
		String elementSelection =  element.getAttributeElementSelectionString();
		String[] elementSelectionArgs = element.getAttributeElementSelectionArgs(attributeName);
		return getAttributeValues(valueHistoryTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns the values of the given attribute for the given element
	 * restricted to the limit-many most recent
	 * time intervals.
	 * 
	 * @param attributeName
	 * @param element
	 * @param limit
	 * @return
	 */
	public TimeVaryingAttributeValues getRecentAttributeValues(
			String attributeName, 
			Element element,
			int limit) {
		String valueHistoryTableName = getAttributeValuesTableNameForDomain(element.getDomain());
		String elementSelection =  element.getAttributeElementSelectionString();
		String[] elementSelectionArgs = element.getAttributeElementSelectionArgs(attributeName);
		return getRecentAttributeValues(valueHistoryTableName, elementSelection, elementSelectionArgs, limit);
	}

	/**
	 * Returns a map from names of attributes in the element's domain to 
	 * their values for the given element at the given point in time.
	 * 
	 * Includes only attributes for which a value is set for the given element
	 * at the given point in time
	 *   
	 * @param timePoint
	 * @param element
	 * @return
	 */
	public LinkedHashMap<String, String> getValuesOfAllAttributesForElementAt(long timePoint, Element element) {
		String tableName = getAttributeValuesTableNameForDomain(element.getDomain());
		String elementSelection = element.getElementSelectionString();
		String[] elementSelectionArgs = element.getElementSelectionArgs();
		return getValuesOfAllAttributesAt(timePoint, tableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns a map from attribute names to 
	 * their values for the given element over the whole time line.
	 * 
	 * Only attributes for which a value is set at some point in time appear in the map.
	 *  
	 * @param element
	 * @return map from attribute names to time-varying attribute values
	 */
	public LinkedHashMap<String, TimeVaryingAttributeValues> getValuesOfAllAttributesForElement(Element element) {
		String valueHistoryTableName = getAttributeValuesTableNameForDomain(element.getDomain());
		String elementSelection =  element.getElementSelectionString();
		String[] elementSelectionArgs = element.getElementSelectionArgs();
		return getValuesOfAllAttributes(valueHistoryTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Returns a map from elements in the given domain to their values of the given attribute at the
	 * given point in time.
	 * 
	 *  Only elements for which the attribute value is set at that point 
	 *  in time are included in the map.
	 *  
	 * @param timePoint 
	 * @param element will only be used to infer the domain and the concrete type (otherwise gets ignored)
	 * @param attributeName
	 * @return map from elements to attribute values
	 */
	public <E extends Element> LinkedHashMap<E, String> getValuesOfAttributeForAllElementsAt(
			long timePoint, E element, String attributeName) {
		String domain = element.getDomain();
		LinkedHashMap<E, String> map = new LinkedHashMap<E, String>();
		if(!hasAttribute(domain, attributeName))
			return map;
		String whereClause = ATTRIBUTES_COL_NAME + " = ? AND " + 
			COL_TIME_START + " <= ? AND " + COL_TIME_END + " >= ? ";
		String[] whereArgs = {attributeName,Long.toString(timePoint),Long.toString(timePoint)};
		String tableName = getAttributeValuesTableNameForDomain(domain);
		String[] elementCols = element.getElementColumnNames();
		String[] projection = new String[elementCols.length + 3];
		projection[0] = ATTRIBUTES_COL_VALUE;
		projection[1] = COL_TIME_START;
		projection[2] = COL_TIME_END;
		for(int i = 0; i < elementCols.length; ++i){
			projection[3+i] = elementCols[i];
		}
		Cursor c = db.query(tableName, 
				projection, 
				whereClause, 
				whereArgs, null, null, null);
		int col_of_value = c.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
		int col_of_start = c.getColumnIndexOrThrow(COL_TIME_START);
		int col_of_end = c.getColumnIndexOrThrow(COL_TIME_END);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				long start = c.getLong(col_of_start);
				long end = c.getLong(col_of_end);
				if(contains(start, end, timePoint)){
					map.put((E) element.getInstanceFromCursor(c),
							c.getString(col_of_value));
				}
				c.moveToNext();
			}
		}
		c.close();
		return map;
	}

	/**
	 * Returns a map from elements in the given domain to their values of the given attribute over
	 * the whole lifetime.
	 * 
	 *  Only elements for which the attribute value is set at some point 
	 *  in time are included in the map.
	 *  
	 * @param element will only be used to infer the domain and the concrete type (otherwise gets ignored)
	 * @param attributeName
	 * @return map from elements to time varying attribute values
	 */
	public <E extends Element> LinkedHashMap<E, TimeVaryingAttributeValues> getValuesOfAttributeForAllElements(
			E element, String attributeName) {
		String domain = element.getDomain();
		LinkedHashMap<E, TimeVaryingAttributeValues> map = new LinkedHashMap<E, TimeVaryingAttributeValues>();
		if(!hasAttribute(domain, attributeName))
			return map;
		String whereClause = ATTRIBUTES_COL_NAME + " = ?";
		String[] whereArgs = {attributeName};
		String tableName = getAttributeValuesTableNameForDomain(domain);
		String[] elementCols = element.getElementColumnNames();
		String[] projection = new String[elementCols.length + 3];
		projection[0] = ATTRIBUTES_COL_VALUE;
		projection[1] = COL_TIME_START;
		projection[2] = COL_TIME_END;
		for(int i = 0; i < elementCols.length; ++i){
			projection[3+i] = elementCols[i];
		}
		Cursor c = db.query(tableName, 
				projection, 
				whereClause, 
				whereArgs, null, null, null);
		int col_of_value = c.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
		int col_of_start = c.getColumnIndexOrThrow(COL_TIME_START);
		int col_of_end = c.getColumnIndexOrThrow(COL_TIME_END);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				long start = c.getLong(col_of_start);
				long end = c.getLong(col_of_end);
				E currentElement = (E) element.getInstanceFromCursor(c);
				TimeVaryingAttributeValues values = map.get(currentElement);
				if(values == null){
					values = new TimeVaryingAttributeValues();
					map.put(currentElement, values);
				}
				values.setValueAt(new TimeInterval(start, end), 
						c.getString(col_of_value));
				c.moveToNext();
			}
		}
		c.close();
		return map;
	}

	/**
	 * Returns the set of unique values that are set for the given attribute at any point in the given interval.
	 * 
	 * Returns null if there is no such attribute.
	 * 
	 * @param domain
	 * @param interval
	 * @param attributeName
	 * @return
	 */
	public LinkedHashSet<String> getUniqueValuesForAttributeAt(TimeInterval interval, 
			String domain, String attributeName) {
		String valueHistoryTableName = getAttributeValuesTableNameForDomain(domain);
		String elementSelection =  ATTRIBUTES_COL_NAME + " = ?";
		String[] elementSelectionArgs = {attributeName};
		return getUniqueValuesForAttributeAt(interval, valueHistoryTableName, elementSelection, elementSelectionArgs);
	}

	/**
	 * Deletes this attribute from the list of available attributes, and deletes all associated values and
	 * choices.
	 * 
	 * @param domain
	 * @param attributeName
	 */
	public void eraseAttribute(String domain, String attributeName){
		if(!hasAttribute(domain, attributeName))
			return;
		String selection = ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {attributeName};
		db.beginTransaction();
		try{
			String tableName = getAttributeNamesTableNameForDomain(domain);
			db.delete(tableName, selection, selectionArgs);
			tableName = getAttributeValuesTableNameForDomain(domain);
			//TODO: delete associated secondary attributes and their values!!!
			db.delete(tableName, selection, selectionArgs);
			tableName = getAttributeChoicesTableNameForDomain(domain);
			db.delete(tableName, selection, selectionArgs);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
///////////////////////////////////////////////////////////////////////////////////////////////
// The code below gives separate implementations per domain for the methods getValuesOfAttributeForAllElements/At
// (still to be checked).
///////////////////////////////////////////////////////////////////////////////////////////////
//	/**
//	 * Returns a map from directed alter-alter dyads to their values of the given attribute at the
//	 * given point in time.
//	 * 
//	 *  Only alter-alter dyads for which the attribute value is set at that point 
//	 *  in time are included in the map.
//	 *  Returns null if there is no alter-alter dyad attribute with the given name. Fails if the direction type
//	 *  of this attribute is SYMMETRIC (call getValuesOfAllUndirectedAlterAlterDyadsForAttributeAt in this case).
//	 *  
//	 * @param timePoint 
//	 * @param attributeName
//	 * @return map from ordered dyads to attribute values
//	 */
//	public LinkedHashMap<OrderedDyad, String> getValuesOfAllDirectedAlterAlterDyadsForAttributeAt(
//			long timePoint, String attributeName) {
//		String domain = PersonalNetwork.DOMAIN_ALTER_ALTER;
//		if(!hasAttribute(domain, attributeName))
//			return null;
//		String whereClause = ATTRIBUTES_COL_NAME + " = ? AND " + 
//			COL_TIME_START + " <= ? AND " + COL_TIME_END + " >= ? ";
//		String[] whereArgs = {attributeName,Long.toString(timePoint),Long.toString(timePoint)};
//		String tableName = ALTER_ALTER_ATTRIBS_VALUES_TABLE_NAME;
//		Cursor values = db.query(tableName, 
//				new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET, ATTRIBUTES_COL_VALUE,
//				COL_TIME_START, COL_TIME_END}, 
//				whereClause, 
//				whereArgs, null, null, DYADS_COL_SOURCE + " ASC");
//		int col_of_source = values.getColumnIndexOrThrow(DYADS_COL_SOURCE);
//		int col_of_target = values.getColumnIndexOrThrow(DYADS_COL_TARGET);
//		int col_of_value = values.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
//		int col_of_start = values.getColumnIndexOrThrow(COL_TIME_START);
//		int col_of_end = values.getColumnIndexOrThrow(COL_TIME_END);
//		LinkedHashMap<OrderedDyad, String> map = new LinkedHashMap<OrderedDyad, String>();
//		if(values.moveToFirst()){
//			while(!values.isAfterLast()){
//				long start = values.getLong(col_of_start);
//				long end = values.getLong(col_of_end);
//				if(contains(start, end, timePoint))
//					map.put(new OrderedDyad(values.getString(col_of_source),values.getString(col_of_target)),
//							values.getString(col_of_value));
//				values.moveToNext();
//			}
//		}
//		return map;
//	}
//
//	/**
//	 * Returns a map from directed alter-alter dyads to their values of the given attribute over
//	 * the whole time line.
//	 * 
//	 *  Only alter-alter dyads for which the attribute value is set at some point 
//	 *  in time are included in the map.
//	 *  Returns null if there is no alter-alter dyad attribute with the given name. Fails if the direction type
//	 *  of this attribute is SYMMETRIC (call getValuesOfAllUndirectedAlterAlterDyadsForAttribute in this case).
//	 *  
//	 * @param attributeName
//	 * @return map from ordered dyads to time varying attribute values
//	 */
//	public LinkedHashMap<OrderedDyad, TimeVaryingAttributeValues> getValuesOfAllDirectedAlterAlterDyadsForAttribute(
//			String attributeName) {
//		String domain = PersonalNetwork.DOMAIN_ALTER_ALTER;
//		if(!hasAttribute(domain, attributeName))
//			return null;
//		String whereClause = ATTRIBUTES_COL_NAME + " = ?  ";
//		String[] whereArgs = {attributeName};
//		String tableName = ALTER_ALTER_ATTRIBS_VALUES_TABLE_NAME;
//		Cursor values = db.query(tableName, 
//				new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET, ATTRIBUTES_COL_VALUE,
//				COL_TIME_START, COL_TIME_END}, 
//				whereClause, 
//				whereArgs, null, null, DYADS_COL_SOURCE + " ASC");
//		int col_of_source = values.getColumnIndexOrThrow(DYADS_COL_SOURCE);
//		int col_of_target = values.getColumnIndexOrThrow(DYADS_COL_TARGET);
//		int col_of_value = values.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
//		int col_of_start = values.getColumnIndexOrThrow(COL_TIME_START);
//		int col_of_end = values.getColumnIndexOrThrow(COL_TIME_END);
//		LinkedHashMap<OrderedDyad, TimeVaryingAttributeValues> map = 
//				new LinkedHashMap<OrderedDyad, TimeVaryingAttributeValues>();
//		if(values.moveToFirst()){
//			while(!values.isAfterLast()){
//				long start = values.getLong(col_of_start);
//				long end = values.getLong(col_of_end);
//				String sourceName = values.getString(col_of_source);
//				String targetName = values.getString(col_of_target);
//				OrderedDyad dyad = new OrderedDyad(sourceName, targetName);
//				String value = values.getString(col_of_value);
//				TimeVaryingAttributeValues tmpValues = map.get(dyad);
//				if(tmpValues == null){
//					tmpValues = new TimeVaryingAttributeValues();
//					map.put(dyad,tmpValues);
//				}
//				tmpValues.setValueAt(new TimeInterval(start, end), value);
//				values.moveToNext();
//			}
//		}
//		return map;
//	}
//
//	/**
//	 * Returns a map from undirected alter-alter dyads to their values of the given attribute
//	 * at the given point in time.
//	 * 
//	 *  Only alter-alter dyads for which the attribute value is set at that point in time are included in the map.
//	 *  Returns null if there is no alter-alter attribute with the given name. Fails if the direction type
//	 *  of this attribute is ASYMMETRIC (call getValuesOfAllDirectedAlterAlterDyadsForAttributeAt in this case).
//	 *  
//	 * @param timePoint 
//	 * @param attributeName
//	 * @return map from undordered dyads to values
//	 */
//	public LinkedHashMap<UnorderedDyad, String> getValuesOfAllUndirectedAlterAlterDyadsForAttributeAt(
//			long timePoint, String attributeName) {
//		String domain = PersonalNetwork.DOMAIN_ALTER_ALTER;
//		if(!hasAttribute(domain, attributeName))
//			return null;
//		String whereClause = ATTRIBUTES_COL_NAME + " = ? AND " + 
//			COL_TIME_START + " <= ? AND " + COL_TIME_END + " >= ? ";
//		String[] whereArgs = {attributeName,Long.toString(timePoint),Long.toString(timePoint)};
//		String tableName = ALTER_ALTER_ATTRIBS_VALUES_TABLE_NAME;
//		Cursor values = db.query(tableName, 
//				new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET, ATTRIBUTES_COL_VALUE,
//				COL_TIME_START, COL_TIME_END}, 
//				whereClause, 
//				whereArgs, null, null, DYADS_COL_SOURCE + " ASC");
//		int col_of_source = values.getColumnIndexOrThrow(DYADS_COL_SOURCE);
//		int col_of_target = values.getColumnIndexOrThrow(DYADS_COL_TARGET);
//		int col_of_value = values.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
//		int col_of_start = values.getColumnIndexOrThrow(COL_TIME_START);
//		int col_of_end = values.getColumnIndexOrThrow(COL_TIME_END);
//		LinkedHashMap<UnorderedDyad, String> map = new LinkedHashMap<UnorderedDyad, String>();
//		if(values.moveToFirst()){
//			while(!values.isAfterLast()){
//				long start = values.getLong(col_of_start);
//				long end = values.getLong(col_of_end);
//				if(contains(start, end, timePoint))
//					map.put(new UnorderedDyad(values.getString(col_of_source),values.getString(col_of_target)),
//							values.getString(col_of_value));
//				values.moveToNext();
//			}
//		}
//		return map;
//	}
//
//	/**
//	 * Returns a map from undirected alter-alter dyads to their values of the given attribute
//	 * over the whole time line.
//	 * 
//	 *  Only alter-alter dyads for which the attribute value is set at any point in time are included in the map.
//	 *  Returns null if there is no alter-alter attribute with the given name. Fails if the direction type
//	 *  of this attribute is ASYMMETRIC (call getValuesOfAllDirectedAlterAlterDyadsForAttribute in this case).
//	 *  
//	 * @param attributeName
//	 * @return map from undordered dyads to time varying values
//	 */
//	public LinkedHashMap<UnorderedDyad, TimeVaryingAttributeValues> getValuesOfAllUndirectedAlterAlterDyadsForAttribute(
//			String attributeName) {
//		String domain = PersonalNetwork.DOMAIN_ALTER_ALTER;
//		if(!hasAttribute(domain, attributeName))
//			return null;
//		String whereClause = ATTRIBUTES_COL_NAME + " = ?  ";
//		String[] whereArgs = {attributeName};
//		String tableName = ALTER_ALTER_ATTRIBS_VALUES_TABLE_NAME;
//		Cursor values = db.query(tableName, 
//				new String[]{DYADS_COL_SOURCE, DYADS_COL_TARGET, ATTRIBUTES_COL_VALUE,
//				COL_TIME_START, COL_TIME_END}, 
//				whereClause, 
//				whereArgs, null, null, DYADS_COL_SOURCE + " ASC");
//		int col_of_source = values.getColumnIndexOrThrow(DYADS_COL_SOURCE);
//		int col_of_target = values.getColumnIndexOrThrow(DYADS_COL_TARGET);
//		int col_of_value = values.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
//		int col_of_start = values.getColumnIndexOrThrow(COL_TIME_START);
//		int col_of_end = values.getColumnIndexOrThrow(COL_TIME_END);
//		LinkedHashMap<UnorderedDyad, TimeVaryingAttributeValues> map = 
//				new LinkedHashMap<UnorderedDyad, TimeVaryingAttributeValues>();
//		if(values.moveToFirst()){
//			while(!values.isAfterLast()){
//				long start = values.getLong(col_of_start);
//				long end = values.getLong(col_of_end);
//				String sourceName = values.getString(col_of_source);
//				String targetName = values.getString(col_of_target);
//				UnorderedDyad dyad = new UnorderedDyad(sourceName, targetName);
//				String value = values.getString(col_of_value);
//				TimeVaryingAttributeValues tmpValues = map.get(dyad);
//				if(tmpValues == null){
//					tmpValues = new TimeVaryingAttributeValues();
//					map.put(dyad,tmpValues);
//				}
//				tmpValues.setValueAt(new TimeInterval(start, end), value);
//				values.moveToNext();
//			}
//		}
//		return map;
//	}
//
//	/**
//	 * Returns a map from alter names to their associated values 
//	 * for the given ego-alter attribute in the given direction at the given point in time.
//	 * 
//	 *  Only alters for which the attribute value is set in the given direction 
//	 *  at the given point in time are included in the map.
//	 *  Returns null if there is no ego-alter dyad attribute with the given name. Fails if the direction type
//	 *  of this attribute is not compatible with the specified direction. For symmetric attributes
//	 *  out or in are allowed and yield the same value; for asymmetric attributes out or in are allowed 
//	 *  and may yield different values.
//	 *  
//	 * @param timePoint 
//	 * @param attributeName
//	 * @param direction OUT or IN
//	 * @return map from alter names to attribute values.
//	 */
//	public LinkedHashMap<String, String> getValuesOfAllEgoAlterDyadsForAttributeAt(
//			long timePoint, String attributeName, String direction) {
//		String domain = PersonalNetwork.DOMAIN_EGO_ALTER;
//		if(!hasAttribute(domain, attributeName))
//			return null;
//		String directionToActOn = direction;
//		if(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC.equals(
//				getAttributeDirectionType(PersonalNetwork.DOMAIN_EGO_ALTER, attributeName)))
//			directionToActOn = PersonalNetwork.DYAD_DIRECTION_OUT;
//		String whereClause = ATTRIBUTES_COL_NAME + " = ? AND " + ATTRIBUTES_COL_DIRECTION_TYPE + " = ? AND " +
//			COL_TIME_START + " <= ? AND " + COL_TIME_END + " >= ? ";
//		String[] whereArgs = {attributeName,directionToActOn,Long.toString(timePoint),Long.toString(timePoint)};
//		String tableName = EGO_ALTER_ATTRIBS_VALUES_TABLE_NAME;
//		Cursor values = db.query(tableName, 
//				new String[]{ALTERS_COL_NAME, ATTRIBUTES_COL_VALUE,
//				COL_TIME_START, COL_TIME_END}, 
//				whereClause, 
//				whereArgs, null, null, ALTERS_COL_NAME + " ASC");
//		int col_of_name = values.getColumnIndexOrThrow(ALTERS_COL_NAME);
//		int col_of_value = values.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
//		int col_of_start = values.getColumnIndexOrThrow(COL_TIME_START);
//		int col_of_end = values.getColumnIndexOrThrow(COL_TIME_END);
//		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
//		if(values.moveToFirst()){
//			while(!values.isAfterLast()){
//				long start = values.getLong(col_of_start);
//				long end = values.getLong(col_of_end);
//				if(contains(start, end, timePoint))
//					map.put(values.getString(col_of_name),values.getString(col_of_value));
//				values.moveToNext();
//			}
//		}
//		return map;
//	}
//
//	/**
//	 * Returns a map from alter names to their associated values 
//	 * for the given ego-alter attribute in the given direction 
//	 * over the whole time line.
//	 * 
//	 *  Only alters for which the attribute value is set in the given direction 
//	 *  at some point in time are included in the map.
//	 *  Returns null if there is no ego-alter dyad attribute with the given name. Fails if the direction type
//	 *  of this attribute is not compatible with the specified direction. For symmetric attributes
//	 *  out or in are allowed and yield the same value; for asymmetric attributes out or in are allowed 
//	 *  and may yield different values.
//	 *  
//	 * @param timePoint 
//	 * @param attributeName
//	 * @param direction OUT or IN
//	 * @return map from alter names to time-varying attribute values.
//	 */
//	public LinkedHashMap<String, TimeVaryingAttributeValues> getValuesOfAllEgoAlterDyadsForAttribute(
//			String attributeName, String direction) {
//		String domain = PersonalNetwork.DOMAIN_EGO_ALTER;
//		if(!hasAttribute(domain, attributeName))
//			return null;
//		String directionToActOn = direction;
//		if(PersonalNetwork.DYAD_DIRECTION_SYMMETRIC.equals(
//				getAttributeDirectionType(PersonalNetwork.DOMAIN_EGO_ALTER, attributeName)))
//			directionToActOn = PersonalNetwork.DYAD_DIRECTION_OUT;
//		String whereClause = ATTRIBUTES_COL_NAME + " = ?  AND " + ATTRIBUTES_COL_DIRECTION_TYPE + " = ? ";
//		String[] whereArgs = {attributeName,directionToActOn};
//		String tableName = EGO_ALTER_ATTRIBS_VALUES_TABLE_NAME;
//		Cursor values = db.query(tableName, 
//				new String[]{ALTERS_COL_NAME, ATTRIBUTES_COL_VALUE,
//				COL_TIME_START, COL_TIME_END}, 
//				whereClause, 
//				whereArgs, null, null, ALTERS_COL_NAME + " ASC");
//		int col_of_name = values.getColumnIndexOrThrow(ALTERS_COL_NAME);
//		int col_of_value = values.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
//		int col_of_start = values.getColumnIndexOrThrow(COL_TIME_START);
//		int col_of_end = values.getColumnIndexOrThrow(COL_TIME_END);
//		LinkedHashMap<String, TimeVaryingAttributeValues> map = 
//				new LinkedHashMap<String, TimeVaryingAttributeValues>();
//		if(values.moveToFirst()){
//			while(!values.isAfterLast()){
//				long start = values.getLong(col_of_start);
//				long end = values.getLong(col_of_end);
//				String alterName = values.getString(col_of_name);
//				String value = values.getString(col_of_value);
//				TimeVaryingAttributeValues tmpValues = map.get(alterName);
//				if(tmpValues == null){
//					tmpValues = new TimeVaryingAttributeValues();
//					map.put(alterName,tmpValues);
//				}
//				tmpValues.setValueAt(new TimeInterval(start, end), value);
//				values.moveToNext();
//			}
//		}
//		return map;
//	}
//
//	/**
//	 * Returns a map from alters to their values of the given attribute at the given point in time.
//	 * 
//	 *  Only alters for which the attribute value is set are included in the map.
//	 *  Returns null if there is no alter attribute with the given name. 
//	 *  
//	 * @param timePoint 
//	 * @param attributeName
//	 * @return map from alternames to values
//	 */
//	public LinkedHashMap<String, String> getValuesOfAllAltersForAttributeAt(
//			long timePoint, String attributeName) {
//		String domain = PersonalNetwork.DOMAIN_ALTER;
//		if(!hasAttribute(domain, attributeName))
//			return null;
//		String whereClause = ATTRIBUTES_COL_NAME + " = ? AND " + 
//			COL_TIME_START + " <= ? AND " + COL_TIME_END + " >= ? ";
//		String[] whereArgs = {attributeName,Long.toString(timePoint),Long.toString(timePoint)};
//		String tableName = ALTER_ATTRIBS_VALUES_TABLE_NAME;
//		Cursor values = db.query(tableName, 
//				new String[]{ALTERS_COL_NAME, ATTRIBUTES_COL_VALUE,
//				COL_TIME_START, COL_TIME_END}, 
//				whereClause, 
//				whereArgs, null, null, ALTERS_COL_NAME + " ASC");
//		int col_of_name = values.getColumnIndexOrThrow(ALTERS_COL_NAME);
//		int col_of_value = values.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
//		int col_of_start = values.getColumnIndexOrThrow(COL_TIME_START);
//		int col_of_end = values.getColumnIndexOrThrow(COL_TIME_END);
//		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
//		if(values.moveToFirst()){
//			while(!values.isAfterLast()){
//				long start = values.getLong(col_of_start);
//				long end = values.getLong(col_of_end);
//				if(contains(start, end, timePoint))
//					map.put(values.getString(col_of_name),values.getString(col_of_value));
//				values.moveToNext();
//			}
//		}
//		return map;
//	}
//
//	/**
//	 * Returns a map from alters to their values of the given attribute over the whole time line.
//	 * 
//	 *  Only alters for which the attribute value is set at some point in time
//	 *  are included in the map.
//	 *  Returns null if there is no alter attribute with the given name. 
//	 *  
//	 * @param attributeName
//	 * @return map from alternames to time-varying values
//	 */
//	public LinkedHashMap<String, TimeVaryingAttributeValues> getValuesOfAllAltersForAttribute(
//			String attributeName) {
//		String domain = PersonalNetwork.DOMAIN_ALTER;
//		if(!hasAttribute(domain, attributeName))
//			return null;
//		String whereClause = ATTRIBUTES_COL_NAME + " = ? ";
//		String[] whereArgs = {attributeName};
//		String tableName = ALTER_ATTRIBS_VALUES_TABLE_NAME;
//		Cursor values = db.query(tableName, 
//				new String[]{ALTERS_COL_NAME, ATTRIBUTES_COL_VALUE,
//				COL_TIME_START, COL_TIME_END}, 
//				whereClause, 
//				whereArgs, null, null, ALTERS_COL_NAME + " ASC");
//		int col_of_name = values.getColumnIndexOrThrow(ALTERS_COL_NAME);
//		int col_of_value = values.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
//		int col_of_start = values.getColumnIndexOrThrow(COL_TIME_START);
//		int col_of_end = values.getColumnIndexOrThrow(COL_TIME_END);
//		LinkedHashMap<String, TimeVaryingAttributeValues> map = 
//				new LinkedHashMap<String, TimeVaryingAttributeValues>();
//		if(values.moveToFirst()){
//			while(!values.isAfterLast()){
//				long start = values.getLong(col_of_start);
//				long end = values.getLong(col_of_end);
//				String alterName = values.getString(col_of_name);
//				String value = values.getString(col_of_value);
//				TimeVaryingAttributeValues tmpValues = map.get(alterName);
//				if(tmpValues == null){
//					tmpValues = new TimeVaryingAttributeValues();
//					map.put(alterName,tmpValues);
//				}
//				tmpValues.setValueAt(new TimeInterval(start, end), value);
//				values.moveToNext();
//			}
//		}
//		return map;
//	}
///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * @param egoName
	 */
	public void setEgoName(String egoName) {
		//TODO
	}

	/**
	 * 
	 * @return ego name
	 */
	public String getEgoName() {
		//TODO
		return null;
	}

	/**
	 * Creates a new secondary attribute.
	 * 
	 * Removes leading and trailing white spaces from
	 * attributeName. Silently does nothing if attributeName is null or attributeName.trim().length() == 0,
	 * or if a secondary attribute with this name already exists or if no primary attribute with
	 * the given name exists in the given domain.
	 * Fails if valuetype is unknown.
	 *  
	 * @param primaryAttributeDomain ego, alter, ego-alter, or alter-alter
	 * @param primaryAttributeName unique string identifier for the primary attribute
	 * @param secondaryAttributeName unique string identifier for the secondary attribute to be created
	 * @param attributeDescription free text describing the attribute.
	 * @param valueType 
	 */
	public void addSecondaryAttribute(String primaryAttributeDomain, 
			String primaryAttributeName, String secondaryAttributeName, 
			String attributeDescription, int valueType) {
		if(secondaryAttributeName == null || secondaryAttributeName.trim().length() == 0 
				|| primaryAttributeName == null)
			return;
		primaryAttributeName = primaryAttributeName.trim();
		secondaryAttributeName = secondaryAttributeName.trim();
		if(!hasAttribute(primaryAttributeDomain, primaryAttributeName)){
			activity.reportInfo("primary attribute: " + primaryAttributeName + 
					" does not exist for the domain: " + primaryAttributeDomain);
			return;
		}
		if(hasSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName)){
			activity.reportInfo("secondary attribute: " + secondaryAttributeName + " " +
					"already exists for primary attribute " + primaryAttributeName);
			return;			
		}
		if(valueType < 0 || valueType >= PersonalNetwork.ATTRIB_TYPE_NAMES.length){
			activity.reportInfo("unknown attribute type: " + valueType);
			return;
		}
		//add attributeName into table
		ContentValues values = new ContentValues();
		values.put(ATTRIBUTES_COL_DOMAIN, primaryAttributeDomain);
		values.put(ATTRIBUTES_COL_NAME, primaryAttributeName);
		values.put(SECONDARY_ATTRIBUTES_COL_NAME, secondaryAttributeName);
		values.put(ATTRIBUTES_COL_VALUE_TYPE, valueType);
		values.put(ATTRIBUTES_COL_DESCRIPTION, attributeDescription);
		db.insert(SECONDARY_ATTRIBS_NAMES_TABLE_NAME, null, values);
	}

	/**
	 * Sets the allowed values for the secondary attribute to the string values in choices union the set of
	 * currently assigned choices.
	 * 
	 * Silently does nothing if no secondary attribute with this
	 * name exists or if the type of this attribute is not CHOICE. 
	 * 
	 * Removes leading and trailing white spaces of Strings in choices. Uses only values in
	 * choices that are not null and not empty strings.
	 * 
	 * @param attributeName
	 * @param choices
	 */
	public void setSecondaryAttributeChoices(String primaryAttributeDomain, 
			String primaryAttributeName, 
			String secondaryAttributeName, 
			LinkedHashSet<String> choices) {
		if(!hasSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName)){
			activity.reportInfo("attribute " + secondaryAttributeName + " is not a secondary Attribute" +
					" for primary attribute " + primaryAttributeName);
			return;			
		}
		if(getSecondaryAttributeValueType(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName) != 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
			activity.reportInfo("attribute " + secondaryAttributeName + " is not of finite choice type");
			return;						
		}
		LinkedHashSet<String> currentValues = getSecondaryAttributeChoices(
				primaryAttributeDomain, primaryAttributeName, secondaryAttributeName);
		String tableName = SECONDARY_ATTRIBS_CHOICES_TABLE_NAME;
		String selection = ATTRIBUTES_COL_DOMAIN + " = ? AND " + 
				ATTRIBUTES_COL_NAME + " = ? AND " + 
				SECONDARY_ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {primaryAttributeDomain, primaryAttributeName, secondaryAttributeName};
		// remove all choices set so far for this attribute
		db.delete(tableName, selection, selectionArgs);
		for(String choice : choices){
			if(choice == null || choice.trim().length() == 0)
				choices.remove(choice);
			else{
				String choice_trimmed = choice.trim();
				if(!choice.equals(choice_trimmed)){
					choices.remove(choice);
					choices.add(choice_trimmed);
				}
			}
		}
		choices.addAll(currentValues);
		for(String choice : choices){
			choice = choice.trim();
			ContentValues values = new ContentValues();
			values.put(ATTRIBUTES_COL_DOMAIN, primaryAttributeDomain);
			values.put(ATTRIBUTES_COL_NAME, primaryAttributeName);
			values.put(SECONDARY_ATTRIBUTES_COL_NAME, secondaryAttributeName);
			values.put(ATTRIBUTES_COL_CHOICE, choice);
			db.insert(tableName, null, values);
		}
	}
	
	/**
	 * Adds choice.trim() to the set of allowed choices for the given attribute.
	 * 
	 * Does nothing if there is no such attribute or if it has not the value type choice or
	 * if choice is null or its trimmed value has length zero or if it is already included
	 * in the set of choices.
	 * 
	 * @param primaryAttributeDomain
	 * @param primaryAttributeName
	 * @param choice
	 */
	public void addSecondaryAttributeChoice(String primaryAttributeDomain, String primaryAttributeName, 
			String secondaryAttributeName, String choice) {
		if(!hasSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName)){
			return;			
		}
		if(getSecondaryAttributeValueType(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName) 
				!= PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
			return;						
		}
		if(choice == null || choice.trim().length() == 0)
			return;
		choice = choice.trim();
		//find out whether this value is already in the choices table
		String tableName = SECONDARY_ATTRIBS_CHOICES_TABLE_NAME;
		String selection = ATTRIBUTES_COL_DOMAIN + " = ? AND " + 
				ATTRIBUTES_COL_NAME + " = ? AND " + 
				SECONDARY_ATTRIBUTES_COL_NAME + " = ? AND " + ATTRIBUTES_COL_CHOICE + " = ?";
		String[] selectionArgs = {primaryAttributeDomain, primaryAttributeName, secondaryAttributeName, choice};
		if(db.query(tableName, new String[]{ATTRIBUTES_COL_CHOICE}, selection, selectionArgs, 
				null, null, null).getCount() > 0)
			return;
		ContentValues values = new ContentValues();
		values.put(ATTRIBUTES_COL_DOMAIN, primaryAttributeDomain);
		values.put(ATTRIBUTES_COL_NAME, primaryAttributeName);
		values.put(SECONDARY_ATTRIBUTES_COL_NAME, secondaryAttributeName);
		values.put(ATTRIBUTES_COL_CHOICE, choice);
		db.insert(tableName, null, values);
	}



	/**
	 * Updates the description of the given secondary attribute.
	 *  
	 * Silently does nothing if there is no secondary attribute with the given name.
	 * 
	 * @param attributeName
	 * @param description
	 */
	public void setSecondaryAttributeDescription(String primaryAttributeDomain, 
			String primaryAttributeName, 
			String secondaryAttributeName, 
			String description) {
		if(!hasSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName)){
			activity.reportInfo("attribute " + secondaryAttributeName + " is not a secondary attribute");
			return;			
		}
		String selection = ATTRIBUTES_COL_DOMAIN + " = ? AND " + 
				ATTRIBUTES_COL_NAME + " = ? AND " + 
				SECONDARY_ATTRIBUTES_COL_NAME + " = ?";
		String[] selectArgs = {primaryAttributeDomain, primaryAttributeName, secondaryAttributeName};
		ContentValues values = new ContentValues();
		values.put(ATTRIBUTES_COL_DESCRIPTION, description);
		db.update(SECONDARY_ATTRIBS_NAMES_TABLE_NAME, values, selection, selectArgs);
	}

	/**
	 * Sets the value of the given secondary attribute for the given datum id. This method
	 * applies to all attribute domains (instances are identified by the datum id).
	 * 
	 * Leading and trailing whitespaces are removed from value. If value is null, or has length
	 * zero or is equal to N/A then the respective entry is removed.
	 * 
	 * It is not checked whether the given datum id and the given secondary attribute name exists.
	 * 
	 * Compatibility of the given value with the required value type is not checked.
	 * 
	 * @param datumID
	 * @param secondaryAttributeName
	 * @param value
	 */
	public void setSecondaryAttributeValue(String datumID, 
			String secondaryAttributeName, String value) {
		String selection =  COL_DATUM_ID + " = ? AND " +
				SECONDARY_ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {datumID, secondaryAttributeName};
		if(value == null){
			db.delete(SECONDARY_ATTRIBS_VALUES_TABLE_NAME, selection, selectionArgs);
			return;
		}
		value = value.trim();
		if(value.length() == 0 || PersonalNetwork.VALUE_NOT_ASSIGNED.equals(value)){
			db.delete(SECONDARY_ATTRIBS_VALUES_TABLE_NAME, selection, selectionArgs);
			return;			
		}
		ContentValues values = new ContentValues();
		values.put(COL_DATUM_ID, datumID);
		values.put(SECONDARY_ATTRIBUTES_COL_NAME, secondaryAttributeName);
		values.put(ATTRIBUTES_COL_VALUE, value);
		Cursor c = db.query(SECONDARY_ATTRIBS_VALUES_TABLE_NAME, new String[]{ATTRIBUTES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(c.moveToFirst()){// there is already a value --> update
			db.update(SECONDARY_ATTRIBS_VALUES_TABLE_NAME, values, selection, selectionArgs);
		} else {
			db.insert(SECONDARY_ATTRIBS_VALUES_TABLE_NAME, null, values);
		}
		c.close();
	}
	
	/**
	 * Returns the set of allowed values currently set for this attribute.
	 * 
	 * Returns null if there is no secondary attribute with the given name of if
	 * the type of this attribute is not CHOICE.
	 * 
	 * @param attributeName
	 * @return
	 */
	public LinkedHashSet<String> getSecondaryAttributeChoices(String primaryAttributeDomain, 
			String primaryAttributeName, String secondaryAttributeName) {
		if(!hasSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName)){
			activity.reportInfo("attribute " + secondaryAttributeName + " is not a secondary attribute " +
					"for primary attribute " + primaryAttributeName);
			return null;			
		}
		if(getSecondaryAttributeValueType(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName) != 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE){
			activity.reportInfo("attribute " + secondaryAttributeName + " is not of finite choice type");
			return null;						
		}
		LinkedHashSet<String> choices = new LinkedHashSet<String>();
		String selection = ATTRIBUTES_COL_DOMAIN + " = ? AND " + 
				ATTRIBUTES_COL_NAME + " = ? AND " + 
				SECONDARY_ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {primaryAttributeDomain, primaryAttributeName, secondaryAttributeName};
		Cursor c = db.query(SECONDARY_ATTRIBS_CHOICES_TABLE_NAME, 
				new String[]{ATTRIBUTES_COL_CHOICE}, selection, 
				selectionArgs, null, null, ATTRIBUTES_COL_CHOICE + " ASC");
		int col_of_choice = c.getColumnIndexOrThrow(ATTRIBUTES_COL_CHOICE);
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				choices.add(c.getString(col_of_choice));
				c.moveToNext();
			}
		}
		c.close();
		return choices;
	}

	/**
	 * Returns the type (as an integer id) of the specified secondary attribute.
	 * 
	 * Returns -1 (a non-existing attribute type) if there is no secondary attribute with the given name.
	 * 
	 * @param attributeName
	 * @return
	 */
	public int getSecondaryAttributeValueType(String primaryAttributeDomain, 
			String primaryAttributeName, String secondaryAttributeName) {
		if(!hasSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName)){
			activity.reportInfo("attribute " + secondaryAttributeName + " is not a secondary attribute " +
					"for the primary attribute " + primaryAttributeName);
			return -1;			
		}
		String selection = ATTRIBUTES_COL_DOMAIN + " = ? AND " + ATTRIBUTES_COL_NAME + " = ? AND " + SECONDARY_ATTRIBUTES_COL_NAME + " = ?";
		String[] selectArgs = {primaryAttributeDomain, primaryAttributeName, secondaryAttributeName};
		Cursor attrType = db.query(SECONDARY_ATTRIBS_NAMES_TABLE_NAME, 
				new String[]{ATTRIBUTES_COL_VALUE_TYPE}, 
				selection, 
				selectArgs, null, null, null);
		int col_of_type = attrType.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE_TYPE);
		if(!attrType.moveToFirst())
			throw new IllegalStateException("something's wrong");
		int ret = Integer.parseInt(attrType.getString(col_of_type));
		attrType.close();
		return ret;
	}

	/**
	 * Returns the type name of the specified secondary attribute.
	 * 
	 * Returns null if there is no secondary attribute with the given name.
	 * 
	 * @param attributeName
	 * @return
	 */
	public String getSecondaryAttributeValueTypeName(String primaryAttributeDomain, String primaryAttributeName, String secondaryAttributeName) {
		if(!hasSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName)){
			activity.reportInfo("attribute " + secondaryAttributeName + " is not a secondary attribute " +
					"for the primary attribute " + primaryAttributeName);
			return null;			
		}
		return PersonalNetwork.ATTRIB_TYPE_NAMES[getSecondaryAttributeValueType(
				primaryAttributeDomain, primaryAttributeName, secondaryAttributeName)];
	}

	/**
	 * Returns true if there is a secondary attribute with the given name associated
	 * with the primary attribute of the given name.
	 * 
	 * @param attributeName
	 * @return 
	 */
	public boolean hasSecondaryAttribute(String primaryAttributeDomain, 
			String primaryAttributeName, String secondaryAttributeName) {
		if(secondaryAttributeName == null)
			return false;
		String selection = ATTRIBUTES_COL_DOMAIN + " = ? AND " + ATTRIBUTES_COL_NAME + " = ? AND " + SECONDARY_ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {primaryAttributeDomain, primaryAttributeName, secondaryAttributeName};
		Cursor theAttribName = db.query(SECONDARY_ATTRIBS_NAMES_TABLE_NAME, 
				new String[]{SECONDARY_ATTRIBUTES_COL_NAME}, 
				selection, selectionArgs, null, null, null);
		boolean ret = theAttribName.getCount() > 0;
		theAttribName.close();
		return ret;
	}

	/**
	 * Returns an event id that has never been returned before.
	 * 
	 * The first call in the lifetime of the database will return the string "0".
	 * 
	 * Each subsequent call increments the value of the last returned string (seen as a long integer)
	 * by one and returns this.
	 * 
	 * @return
	 */
	private String getNextDatumIDAndIncrement() {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_NEXT_DATUM_ID};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(!c.moveToFirst()){
			ContentValues values = new ContentValues();
			values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_NEXT_DATUM_ID);
			values.put(PROPERTIES_COL_VALUE, "0");
			db.insert(PROPERTIES_TABLE_NAME, null, values);
			return getNextDatumIDAndIncrement(); // now there is a value
		}
		int col_of_value = c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE);
		String id = c.getString(col_of_value);
		c.close();
		//increment by one and update the database
		String nextID = Long.toString(Long.parseLong(id)+1);
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_NEXT_DATUM_ID);
		values.put(PROPERTIES_COL_VALUE, nextID);
		db.update(PROPERTIES_TABLE_NAME, values, selection, selectionArgs);
		return id;
	}

	/**
	 * Returns the last returned datum id.
	 * 
	 * Does not increment the datum id.
	 * 
	 * @return
	 */
	public String getCurrentDatumID() {
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] selectionArgs = {PROPERTIES_KEY_NEXT_DATUM_ID};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(!c.moveToFirst()){
			return "0";
		}
		int col_of_value = c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE);
		String id = c.getString(col_of_value);
		c.close();
		//increment by one and update the database
		return Long.toString(Long.parseLong(id)-1);
	}

	/**
	 * Returns the description of the specified secondary attribute.
	 * 
	 * Returns null if there is no secondary attribute with the given name.
	 * 
	 * @param attributeName
	 * @return
	 */
	public String getSecondaryAttributeDescription(String primaryAttributeDomain, 
			String primaryAttributeName, String secondaryAttributeName) {
		if(!hasSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName)){
			activity.reportInfo(secondaryAttributeName + " is not a secondary attribute");
			return null;
		}
		String selection = ATTRIBUTES_COL_DOMAIN + " = ? AND " + 
				ATTRIBUTES_COL_NAME + " = ? AND " + 
				SECONDARY_ATTRIBUTES_COL_NAME + " = ?";
		String[] selectArgs = {primaryAttributeDomain, primaryAttributeName, secondaryAttributeName};
		Cursor attrDesc = db.query(SECONDARY_ATTRIBS_NAMES_TABLE_NAME, 
				new String[]{ATTRIBUTES_COL_DESCRIPTION}, 
				selection, 
				selectArgs, null, null, null);
		int col_of_desc = attrDesc.getColumnIndexOrThrow(ATTRIBUTES_COL_DESCRIPTION);
		if(!attrDesc.moveToFirst())
			throw new IllegalStateException("something's wrong");
		String ret = attrDesc.getString(col_of_desc);
		attrDesc.close();
		return ret;
	}

	/**
	 * Returns the set of secondary attribute names associated with the given primary attribute"
	 * @return
	 */
	public LinkedHashSet<String> getSecondaryAttributeNames(String primaryAttributeDomain, 
			String primaryAttributeName) {
		String selection = ATTRIBUTES_COL_DOMAIN + " = ? AND " + ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {primaryAttributeDomain, primaryAttributeName};
		Cursor nc = db.query(SECONDARY_ATTRIBS_NAMES_TABLE_NAME, 
				new String[]{SECONDARY_ATTRIBUTES_COL_NAME}, selection, selectionArgs,
				null, null, SECONDARY_ATTRIBUTES_COL_NAME + " ASC");
		LinkedHashSet<String> names = new LinkedHashSet<String>();
		int col_of_name = nc.getColumnIndex(SECONDARY_ATTRIBUTES_COL_NAME);
		if(nc.moveToFirst() && col_of_name >= 0){
			while(!nc.isAfterLast()){
				String name = nc.getString(col_of_name);
				names.add(name);
				nc.moveToNext();
			}
		}
		nc.close();
		return names;
	}

	/**
	 * Returns the value of the given secondary attribute for the given datum id. This method
	 * applies to all attribute domains (instances are identified by the datum id).
	 * 
	 * Returns N/A if the given secondary attribute is not set for the given datum id.
	 * 
	 * @param datumID
	 * @param secondaryAttributeName
	 * @return
	 */
	public String getSecondaryAttributeValue(String datumID, 
			String secondaryAttributeName) {
		String selection = COL_DATUM_ID + " = ? AND " +
				SECONDARY_ATTRIBUTES_COL_NAME + " = ?";
		String[] selectionArgs = {datumID, secondaryAttributeName};
		Cursor c = db.query(SECONDARY_ATTRIBS_VALUES_TABLE_NAME, new String[]{ATTRIBUTES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		int col_index = c.getColumnIndex(ATTRIBUTES_COL_VALUE);
		if(c.moveToFirst() && col_index >= 0){
			String ret = c.getString(col_index);
			c.close();
			return ret;
		}
		c.close();
		return PersonalNetwork.VALUE_NOT_ASSIGNED;
	}
	
	/**
	 * Returns a map from secondary attribute names to values for the given datum id. This method
	 * applies to all attribute domains (instances are identified by the datum id).
	 * 
	 * Returns only names of secondary attributes for which a values is set for the given datum id.
	 * 
	 * @param datumID
	 * @return map from secondary attribute names to values
	 */
	public LinkedHashMap<String, String> getSecondaryAttributeValues(String datumID) {
		String selection = COL_DATUM_ID + " = ?";
		String[] selectionArgs = {datumID};
		Cursor c = db.query(SECONDARY_ATTRIBS_VALUES_TABLE_NAME, 
				new String[]{SECONDARY_ATTRIBUTES_COL_NAME, ATTRIBUTES_COL_VALUE}, 
				selection, selectionArgs, null, null, SECONDARY_ATTRIBUTES_COL_NAME + " ASC");
		int col_value = c.getColumnIndex(ATTRIBUTES_COL_VALUE);
		int col_name = c.getColumnIndex(SECONDARY_ATTRIBUTES_COL_NAME);
		LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
		if(c.moveToFirst() && col_value >= 0 && col_name >= 0){
			while(!c.isAfterLast()){
				values.put(c.getString(col_name), c.getString(col_value));
				c.moveToNext();
			}
		}
		c.close();
		return values;
	}
	
	private static final String ATTR_NAME_PREFIX_EGO = DOMAIN_EGO + ":";
	private static final String ATTR_NAME_PREFIX_ALTER = DOMAIN_ALTER + ":";
	private static final String ATTR_NAME_PREFIX_EGO_ALTER = DOMAIN_EGO_ALTER + ":";
	private static final String ATTR_NAME_PREFIX_ALTER_ALTER = DOMAIN_ALTER_ALTER + ":";
	private static final String DOMAIN_SECONDARY = "SECONDARY";
	private static final String ATTR_NAME_PREFIX_SECONDARY = DOMAIN_SECONDARY + ":";
	
	private static final String ATTR_NAME_IS_EGO = ATTRIBUTE_PREFIX_EGOSMART + "is ego";
	private static final String ATTR_NAME_EGO_NAME = ATTRIBUTE_PREFIX_EGOSMART + "ego name";
	private static final String ATTR_NAME_ALTER_NAME = ATTRIBUTE_PREFIX_EGOSMART + "alter name";
	/**
	 * Writes the complete network history in GraphML format to the specified writer.
	 * 
	 * The writer will not be closed in this method.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void writeNetworkHistory2GraphML(Writer writer) throws IOException {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		XmlSerializer serializer = Xml.newSerializer();
		serializer.setOutput(writer);
		//start document
		//serializer.startDocument("UTF-8", true);
		serializer.startDocument("UTF-8", null);
		serializer.startTag("", "graphml");
		serializer.attribute("", "xmlns", "http://graphml.graphdrawing.org/xmlns/graphml");
		//mapping from attribute names (with their prefixes "ego:", ...) to key ids (with the "d" prefix)
		HashMap<String,String> attrName2KeyId = new HashMap<String,String>();
		int keyId = 0;
		//write keys////////////////////////
		//special attributes
		//attribute for identifying ego among all the nodes
		String isEgoKeyId = "d" + keyId;
		writeOpenKeyElement("true iff this node is ego; default is false", ATTR_NAME_IS_EGO, 
				"boolean", "node", isEgoKeyId, 
				DYAD_DIRECTION_SYMMETRIC, ATTRIBUTE_DYNAMIC_TYPE_STATE, serializer);
		serializer.endTag("", "key");
		++keyId;
		// ego name
		String egoNameKeyId = "d" + keyId;
		writeOpenKeyElement("name of ego", ATTR_NAME_EGO_NAME, "string", "node", egoNameKeyId, 
				DYAD_DIRECTION_SYMMETRIC, ATTRIBUTE_DYNAMIC_TYPE_STATE, serializer);
		serializer.endTag("", "key");
		++keyId;
		// alter names
		String alterNameKeyId = "d" + keyId;
		writeOpenKeyElement("alter name chosen by ego", ATTR_NAME_ALTER_NAME, "string", "node", alterNameKeyId, 
				DYAD_DIRECTION_SYMMETRIC, ATTRIBUTE_DYNAMIC_TYPE_STATE, serializer);
		serializer.endTag("", "key");
		++keyId;
		//all attributes per domain
		//ego-level attributes (these are node attributes)
		keyId = writeKeysForDomain(serializer, attrName2KeyId, keyId, DOMAIN_EGO,
				ATTR_NAME_PREFIX_EGO, "node");
		//alter-level attributes
		keyId = writeKeysForDomain(serializer, attrName2KeyId, keyId, DOMAIN_ALTER,
				ATTR_NAME_PREFIX_ALTER, "node");
		//ego-alter level attributes
		keyId = writeKeysForDomain(serializer, attrName2KeyId, keyId, DOMAIN_EGO_ALTER,
				ATTR_NAME_PREFIX_EGO_ALTER, "dyad");
		//alter-alter level attributes
		keyId = writeKeysForDomain(serializer, attrName2KeyId, keyId, DOMAIN_ALTER_ALTER,
				ATTR_NAME_PREFIX_ALTER_ALTER, "dyad");
		//write graph////////////////////////
		serializer.startTag("", "graph");
		serializer.attribute("", "id", "G");
		serializer.attribute("", "edgedefault", "undirected");
		//write the single special node representing ego
		int nodeId = 0;
		serializer.startTag("", "node");
		String egoNodeId = "v" + nodeId;
		serializer.attribute("", "id", egoNodeId);
		++nodeId;
		serializer.attribute("", "egosmart.is.ego", "true");
		//write ego-level attributes (stored as node attributes)
		String egoName = getEgoName();
		if(egoName != null){
			serializer.startTag("", "data");
			serializer.attribute("", "key", egoNameKeyId);
			serializer.text(egoName);
			serializer.endTag("", "data");
		}
		//attribute indicating that this is ego
		serializer.startTag("", "data");
		serializer.attribute("", "key", isEgoKeyId);
		serializer.text("true");
		serializer.endTag("", "data");
		//all other ego attributes
		writeDataForElement(formatter, serializer, attrName2KeyId, Ego.getInstance());
		serializer.endTag("", "node");//close the ego node
		//write nodes representing alters
		LinkedHashSet<String> alterNames = getAltersAt(TimeInterval.getMaxInterval());
		HashMap<String,Integer> alterName2Id = new HashMap<String,Integer>();
		LinkedHashMap<String, TimeVaryingAttributeValues> attrName2values; //TODO: to be removed
		for(String alterName : alterNames){
			alterName2Id.put(alterName,nodeId);
			serializer.startTag("", "node");
			serializer.attribute("", "id", ("v" + nodeId)); 
			//write lifetime attributes of this node
			Iterator<TimeInterval> lifetimeIt = getLifetimeOfAlter(alterName).getIterator();
			StringBuffer startTimes = new StringBuffer();
			StringBuffer endTimes = new StringBuffer();
			while(lifetimeIt.hasNext()){
				TimeInterval time = lifetimeIt.next();
				startTimes.append(formatter.format(new Date(time.getStartTime())) + " ");
				endTimes.append(formatter.format(new Date(time.getEndTime())) + " ");
			}
			serializer.attribute("", "time.intervals.start", startTimes.toString().trim());
			serializer.attribute("", "time.intervals.end", endTimes.toString().trim());
			//write the alter name in a data element (is time independent)
			serializer.startTag("", "data");
			serializer.attribute("", "key", alterNameKeyId);
			serializer.text(alterName);
			serializer.endTag("", "data");
			//write all the other data elements
			writeDataForElement(formatter, serializer, attrName2KeyId, Alter.getInstance(alterName));
			serializer.endTag("", "node");
			++nodeId;
		}
		//write ego-alter ties (edge elements)
		for(String alterName : alterNames){
			serializer.startTag("", "edge");
			serializer.attribute("", "source", egoNodeId);
			serializer.attribute("", "target", "v" + alterName2Id.get(alterName));
			//write lifetime attributes of this edge
			Iterator<TimeInterval> lifetimeIt = 
					getLifetimeOfAlter(alterName).getIterator();
			StringBuffer startTimes = new StringBuffer();
			StringBuffer endTimes = new StringBuffer();
			while(lifetimeIt.hasNext()){
				TimeInterval time = lifetimeIt.next();
				startTimes.append(formatter.format(new Date(time.getStartTime())) + " ");
				endTimes.append(formatter.format(new Date(time.getEndTime())) + " ");
			}
			serializer.attribute("", "time.intervals.start", startTimes.toString().trim());
			serializer.attribute("", "time.intervals.end", endTimes.toString().trim());
			serializer.endTag("", "edge");			
		}
		//write alter-alter ties (edge elements)
		for(UnorderedDyad dyad : getUndirectedTiesAt(TimeInterval.getMaxInterval())){
			serializer.startTag("", "edge");
			serializer.attribute("", "source", "v" + alterName2Id.get(dyad.source()));
			serializer.attribute("", "target", "v" + alterName2Id.get(dyad.target()));
			//write lifetime attributes of this edge
			Iterator<TimeInterval> lifetimeIt = 
					getLifetimeOfTie(dyad.source(), dyad.target()).getIterator();
			StringBuffer startTimes = new StringBuffer();
			StringBuffer endTimes = new StringBuffer();
			while(lifetimeIt.hasNext()){
				TimeInterval time = lifetimeIt.next();
				startTimes.append(formatter.format(new Date(time.getStartTime())) + " ");
				endTimes.append(formatter.format(new Date(time.getEndTime())) + " ");
			}
			serializer.attribute("", "time.intervals.start", startTimes.toString().trim());
			serializer.attribute("", "time.intervals.end", endTimes.toString().trim());
			serializer.endTag("", "edge");
		}
		//write dyads with their attributes: ego-alter
		for(String alterName : getAltersAt(TimeInterval.getMaxInterval())){
			//OUT instances
			serializer.startTag("", "dyad");
			serializer.attribute("", "source", egoNodeId);
			serializer.attribute("", "target", "v" + alterName2Id.get(alterName));
			serializer.attribute("", "directed", "true");
			//write all data attached to this dyad
			writeDataForElement(formatter, serializer, attrName2KeyId, EgoAlterDyad.getOutwardInstance(alterName));
			serializer.endTag("", "dyad");
			//IN instances
			serializer.startTag("", "dyad");
			serializer.attribute("", "source", "v" + alterName2Id.get(alterName));
			serializer.attribute("", "target", egoNodeId);
			serializer.attribute("", "directed", "true");
			//write all data attached to this dyad
			writeDataForElement(formatter, serializer, attrName2KeyId, EgoAlterDyad.getInwardInstance(alterName));
			serializer.endTag("", "dyad");
		}
		//write dyads with their attributes: alter-alter
		for(OrderedDyad dyad : getDirectedAlterAlterDyadsAt(TimeInterval.getMaxInterval())){
			serializer.startTag("", "dyad");
			serializer.attribute("", "source", "v" + alterName2Id.get(dyad.source()));
			serializer.attribute("", "target", "v" + alterName2Id.get(dyad.target()));
			serializer.attribute("", "directed", "true");
			//write all data attached to this dyad
			writeDataForElement(formatter, serializer, attrName2KeyId, 
					AlterAlterDyad.getInstance(dyad.source(), dyad.target()));
			serializer.endTag("", "dyad");
		}
		//close graph and file
		serializer.endTag("", "graph");
		serializer.endTag("", "graphml");
		serializer.endDocument();
	}

	/**
	 * @param formatter to convert times to date-time strings
	 * @param serializer to write XML code
	 * @param attrName2KeyId maps attribute names (including all prefixes to key ids)
	 * @param element the element for which attributes have to be written 
	 * @throws IOException
	 */
	private void writeDataForElement(DateFormat formatter,
			XmlSerializer serializer, HashMap<String, String> attrName2KeyId,
			Element element) throws IOException {
		LinkedHashMap<String,TimeVaryingAttributeValues> attrName2values = 
				getValuesOfAllAttributesForElement(element);
		for(String attrName : attrName2values.keySet()){
			TimeVaryingAttributeValues values = attrName2values.get(attrName);
			Iterator<TimeInterval> timeIterator = values.getSupport().getIterator();
			while(timeIterator.hasNext()){
				TimeInterval time = timeIterator.next();
				String value = values.getValueAt(time.getStartTime());
				serializer.startTag("", "data");
				serializer.attribute("", "key", attrName2KeyId.get(element.getDomain() + ":" + attrName));
				if(time.isTimePoint()){
					serializer.attribute("", "time.point", 
							formatter.format(new Date(time.getStartTime())));
				} else {
					serializer.attribute("", "time.interval.start", 
							formatter.format(new Date(time.getStartTime())));					
					serializer.attribute("", "time.interval.end", 
							formatter.format(new Date(time.getEndTime())));					
				}
				serializer.text(value);
				String datumId = getAttributeDatumIDAt(time.getStartTime(), 
						attrName, element);
				LinkedHashMap<String, String> secAttrName2Value = getSecondaryAttributeValues(datumId);
				for(String secAttrName : secAttrName2Value.keySet()){
					String secAttrValue = secAttrName2Value.get(secAttrName);
					serializer.startTag("", "data");
					String secAttrNameWithPrefixes = ATTR_NAME_PREFIX_SECONDARY + element.getDomain() + ":" + 
							attrName + ":" + secAttrName;
					//Log.e("info", secAttrNameWithPrefixes);
					serializer.attribute("", "key", 
							attrName2KeyId.get(secAttrNameWithPrefixes));
					serializer.text(secAttrValue);
					serializer.endTag("", "data");
				}
				serializer.endTag("", "data");//close after secondary data has been written
			}
		}
	}

	/**
	 * @param serializer to write out any XML code
	 * @param attrName2KeyId mapping from attribute names (with prefixes) to key ids (will be filled by this method)
	 * @param keyId next available keyid number (will be incremented and returned by this method)
	 * @param domain attribute domain: EGO, ALTER, EGO_ALTER, or ALTER_ALTER
	 * @param attrPrefix dependent on the domain
	 * @param forwhat value of the for attribute of key (node for ego or alter and dyad for dyadic attribs
	 * @return next available key id
	 * @throws IOException
	 */
	private int writeKeysForDomain(XmlSerializer serializer,
			HashMap<String, String> attrName2KeyId, int keyId, String domain,
			String attrPrefix, String forwhat) throws IOException {
		for(String attrName : getAllAttributeNames(domain)){
			String desc = getAttributeDescription(domain, attrName);
			String type = "string";
			int type_int = getAttributeValueType(domain, attrName);
			if(type_int == ATTRIB_TYPE_NUMBER)
				type = "double";
			boolean hasChoices = false;
			if(type_int == ATTRIB_TYPE_FINITE_CHOICE)
				hasChoices = true;
			String key = "d" + keyId;
			String attrNameInGraphML = attrPrefix + attrName;
			writeOpenKeyElement(desc, attrNameInGraphML, type, forwhat, key, 
					getAttributeDirectionType(domain, attrName),
					getAttributeDynamicType(domain, attrName), hasChoices,
					serializer);
			attrName2KeyId.put(attrNameInGraphML, key);
			if(hasChoices){
				for(String choice : getAttributeChoices(domain, attrName)){
					serializer.startTag("", "choice");
					serializer.attribute("", "value", choice);
					serializer.endTag("", "choice");
				}
			}
			++keyId;
			for(String secondaryAttrName : getSecondaryAttributeNames(domain, attrName)){
				desc = getSecondaryAttributeDescription(domain, attrName, secondaryAttrName);
				type = "string";
				type_int = getSecondaryAttributeValueType(domain, attrName,
						secondaryAttrName);
				if(type_int == ATTRIB_TYPE_NUMBER)
					type = "double";
				hasChoices = false;
				if(type_int == ATTRIB_TYPE_FINITE_CHOICE)
					hasChoices = true;
				key = "d" + keyId;
				attrNameInGraphML = ATTR_NAME_PREFIX_SECONDARY + attrPrefix + attrName + ":" + secondaryAttrName;
				writeOpenKeyElement(desc, attrNameInGraphML, type, "data", key, hasChoices, serializer);
				if(hasChoices){
					for(String choice : getSecondaryAttributeChoices(domain, attrName, secondaryAttrName)){
						serializer.startTag("", "choice");
						serializer.attribute("", "value", choice);
						serializer.endTag("", "choice");
					}
				}
				serializer.endTag("", "key");
				attrName2KeyId.put(attrNameInGraphML, key);
				++keyId;				
			}
			serializer.endTag("", "key");
		}
		return keyId;
	}

	/**
	 * Writes the current network (that is, all alters ties and attributes as they 
	 * are set in the current time) 
	 * in GraphML format to the specified writer.
	 * 
	 * The writer will not be closed in this method.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void writeCurrentNetwork2GraphML(Writer writer) throws IOException {
		//used to query alters, ties, and attribute values
		long currentTime = System.currentTimeMillis();
		XmlSerializer serializer = Xml.newSerializer();
		serializer.setOutput(writer);
		//start document
		//serializer.startDocument("UTF-8", true);
		serializer.startDocument("UTF-8", null);
		serializer.startTag("", "graphml");
		serializer.attribute("", "xmlns", "http://graphml.graphdrawing.org/xmlns/graphml");
		//write keys////////////////////////
		//ego-level attributes (graph attributes)
		HashMap<String,String> egoAttrName2KeyId = new HashMap<String,String>();
		int keyId = 1;
		String egoNameKeyId = "d" + keyId;
		writeOpenKeyElement("ego name", "ego name", "string", "graph", egoNameKeyId, serializer);
		serializer.endTag("", "key");
		++keyId;
		HashSet<String> egoAttrNames = getAttributeNames(DOMAIN_EGO);
		for(String egoAttrName : egoAttrNames){
			String desc = getAttributeDescription(DOMAIN_EGO, egoAttrName);
			String type = "string";
			if(getAttributeValueType(DOMAIN_EGO, egoAttrName) == ATTRIB_TYPE_NUMBER)
				type = "double";
			String key = "d" + keyId;
			writeOpenKeyElement(desc, egoAttrName, type, "graph", key, serializer);
			serializer.endTag("", "key");
			egoAttrName2KeyId.put(egoAttrName, key);
			++keyId;
		}
		//node-level attributes
		HashMap<String,String> alterAttrName2KeyId = new HashMap<String,String>();
		String alterNameKeyId = "d" + keyId;
		writeOpenKeyElement("alter name chosen by ego", "alter name", "string", "node", alterNameKeyId, serializer);
		serializer.endTag("", "key");
		++keyId;
		HashSet<String> alterAttrNames = getAttributeNames(DOMAIN_ALTER);
		for(String alterAttrName : alterAttrNames){
			String desc = getAttributeDescription(DOMAIN_ALTER, alterAttrName);
			String type = "string";
			if(getAttributeValueType(DOMAIN_ALTER, alterAttrName) == ATTRIB_TYPE_NUMBER)
				type = "double";
			String key = "d" + keyId;
			writeOpenKeyElement(desc, alterAttrName, type, "node", key, serializer);
			serializer.endTag("", "key");
			alterAttrName2KeyId.put(alterAttrName, key);
			++keyId;
		}
		//edge-level attributes
		HashMap<String,String> tieAttrName2KeyId = new HashMap<String,String>();
		HashSet<String> tieAttrNames = getAttributeNames(DOMAIN_ALTER_ALTER);
		for(String tieAttrName : tieAttrNames){
			String desc = getAttributeDescription(DOMAIN_ALTER_ALTER, tieAttrName);
			String type = "string";
			if(getAttributeValueType(DOMAIN_ALTER_ALTER, tieAttrName) == ATTRIB_TYPE_NUMBER)
				type = "double";
			String key = "d" + keyId;
			writeOpenKeyElement(desc, tieAttrName, type, "edge", key, serializer);
			serializer.endTag("", "key");
			tieAttrName2KeyId.put(tieAttrName, key);
			++keyId;
		}
		//write graph////////////////////////
		serializer.startTag("", "graph");
		serializer.attribute("", "id", "G");
		serializer.attribute("", "edgedefault", "undirected");
		//write ego-level attributes (stored as graph attributes)
		serializer.startTag("", "data");
		serializer.attribute("", "key", egoNameKeyId);
		serializer.text(getEgoName());
		serializer.endTag("", "data");
		for(String egoAttrName : getAttributeNames(DOMAIN_EGO)){
			String value = getAttributeValueAt(currentTime, egoAttrName, Ego.getInstance());
			if(value != null && !VALUE_NOT_ASSIGNED.equals(value)){
				serializer.startTag("", "data");
				serializer.attribute("", "key", egoAttrName2KeyId.get(egoAttrName));
				serializer.text(value);
				serializer.endTag("", "data");
			}
		}
		//write nodes
		String[] alterId2Name = getAltersAt(TimeInterval.getTimePoint(currentTime)).toArray(new String[0]);
		HashMap<String,Integer> alterName2Id = new HashMap<String,Integer>();
		for(int i = 0; i < alterId2Name.length; ++i){
			alterName2Id.put(alterId2Name[i],i);
		}
		for(int i = 0; i < alterId2Name.length; ++i){
			serializer.startTag("", "node");
			serializer.attribute("", "id", ("v" + i)); 
			serializer.startTag("", "data");
			serializer.attribute("", "key", alterNameKeyId);
			serializer.text(alterId2Name[i]);
			serializer.endTag("", "data");
			for(String alterAttrName : getAttributeNames(DOMAIN_ALTER)){
				String value = getAttributeValueAt(currentTime, alterAttrName, 
						Alter.getInstance(alterId2Name[i]));
				if(value != null && !VALUE_NOT_ASSIGNED.equals(value)){
					serializer.startTag("", "data");
					serializer.attribute("", "key", alterAttrName2KeyId.get(alterAttrName));
					serializer.text(value);
					serializer.endTag("", "data");
				}
			}
			serializer.endTag("", "node");
		}
		//write edges
		for(UnorderedDyad dyad : getUndirectedTiesAt(TimeInterval.getTimePoint(currentTime))){
			serializer.startTag("", "edge");
			serializer.attribute("", "source", "v" + alterName2Id.get(dyad.source()));
			serializer.attribute("", "target", "v" + alterName2Id.get(dyad.target()));
			for(String tieAttrName : getAttributeNames(DOMAIN_ALTER_ALTER) ){
				String value = getAttributeValueAt(currentTime, tieAttrName, 
						AlterAlterDyad.getInstance(dyad.source(), dyad.target()));
				if(value != null && !VALUE_NOT_ASSIGNED.equals(value)){
					serializer.startTag("", "data");
					serializer.attribute("", "key", tieAttrName2KeyId.get(tieAttrName));
					serializer.text(value);
					serializer.endTag("", "data");
				}
			}
			serializer.endTag("", "edge");
		}
		//close graph and file
		serializer.endTag("", "graph");
		serializer.endTag("", "graphml");
		serializer.endDocument();
	}

	/*
	 * Writes one GraphML key element.
	 */
	private static void writeOpenKeyElement(String desc, String name, String type, String forwhat, String id, 
			String directionType, String dynamicType, boolean hasChoices, XmlSerializer serializer) throws IOException {
		serializer.startTag("", "key");
		serializer.attribute("", "for", forwhat);
		serializer.attribute("", "id", id);
		serializer.attribute("", "attr.description", desc);
		serializer.attribute("", "attr.name", name);
		serializer.attribute("", "attr.type", type);
		if(directionType != null)
			serializer.attribute("", "attr.direction.type", directionType);
		serializer.attribute("", "attr.dynamic.type", dynamicType);
		if(hasChoices)
			serializer.attribute("", "attr.has.choices", "true");
		//serializer.endTag("", "key");
	}

	/*
	 * Writes one GraphML key element.
	 */
	private static void writeOpenKeyElement(String desc, String name, String type, String forwhat, String id, 
			String directionType, String dynamicType, XmlSerializer serializer) throws IOException {
		serializer.startTag("", "key");
		serializer.attribute("", "for", forwhat);
		serializer.attribute("", "id", id);
		serializer.attribute("", "attr.description", desc);
		serializer.attribute("", "attr.name", name);
		serializer.attribute("", "attr.type", type);
		if(directionType != null)
			serializer.attribute("", "attr.direction.type", directionType);
		serializer.attribute("", "attr.dynamic.type", dynamicType);
		//serializer.endTag("", "key");
	}

	/*
	 * Writes one GraphML key element.
	 */
	private static void writeOpenKeyElement(String desc, String name, String type, String forwhat, String id, 
			boolean hasChoices, XmlSerializer serializer) throws IOException {
		serializer.startTag("", "key");
		serializer.attribute("", "for", forwhat);
		serializer.attribute("", "id", id);
		serializer.attribute("", "attr.description", desc);
		serializer.attribute("", "attr.name", name);
		serializer.attribute("", "attr.type", type);
		if(hasChoices)
			serializer.attribute("", "attr.has.choices", "true");
		//serializer.endTag("", "key");
	}

	/*
	 * Writes one GraphML key element.
	 */
	private static void writeOpenKeyElement(String desc, String name, String type, String forwhat, String id, 
			XmlSerializer serializer) throws IOException {
		serializer.startTag("", "key");
		serializer.attribute("", "for", forwhat);
		serializer.attribute("", "id", id);
		serializer.attribute("", "attr.description", desc);
		serializer.attribute("", "attr.name", name);
		serializer.attribute("", "attr.type", type);
		//serializer.endTag("", "key");
	}

	/**
	 * Reads the content of the int file and load its data into application.
	 * 
	 * @param file Int file with the Egonet interview.
	 * @param study Ego file with the definition of the egonet study.
	 */
	public void importEgonetInterview(File file){
		SAXParserFactory saxfactory = SAXParserFactory.newInstance();
		SAXParser saxparser;
		try {
			interviewLoaded = false;
			saxparser = saxfactory.newSAXParser();
			DefaultHandler handler = new ImportIntHandler(studyFile);
			db.beginTransaction();
			saxparser.parse(file, handler);
			db.setTransactionSuccessful();
			interviewLoaded = true;
			
		} catch (ParserConfigurationException e) {
			Log.e("Import int", e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			Log.e("Import int", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("Import int", e.getMessage());
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}
	
	/**
	 * Reads the content of the GraphML file and merges it into the current network history
	 * (which does not have to be empty before import).
	 * 
	 * @param file the GraphML file containing the network history
	 */
	public void importHistoryFromGraphML(File file){
		SAXParserFactory saxfactory = SAXParserFactory.newInstance();
		SAXParser saxparser;
		try {
			saxparser = saxfactory.newSAXParser();
			DefaultHandler handler = new ImportGraphMLHandler();
			db.beginTransaction();
			saxparser.parse(file, handler);
			db.setTransactionSuccessful();
		} catch (ParserConfigurationException e) {
			Log.e("Import GraphML", e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			Log.e("Import GraphML", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("Import GraphML", e.getMessage());
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}

	}
	private class ImportIntHandler extends DefaultHandler {
		//names of XML elements
		private static final String elem_interview = "Interview";
		private static final String elem_name = "Name";
		private static final String elem_question_id = "QuestionId";
		private static final String elem_string = "String";
		private static final String elem_index = "Index";
		private static final String elem_adjacent = "Adjacent";
		private static final String elem_alters = "Alters";
		
		//names of XML attributes		
		//attributes of Index
		private static final String index_name = "name";
		//attributes of Interview
		private static final String interview_study_id = "StudyId";
		
		String parsedValue;
		String alterResponse; //we can't add a reponse of an alter question until we know alter name. 
		String[] alterPair;
		boolean areAdjacent;
		TimeInterval interval;		
		
		Question currentQuestion;
		EgonetQuestionnaireFile study;
		
		ImportIntHandler(EgonetQuestionnaireFile egoFile) {
			study = egoFile;
		}

		@Override
		public void startDocument() throws SAXException {
			areAdjacent = false;
			interval = TimeInterval.getRightUnbounded(System.currentTimeMillis());
		}
				
		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException{
			if(elem_interview.equals(localName))
				startInterview(atts);
			if(elem_alters.equals(localName))
				startAlters(atts);
			if(elem_index.equals(localName))
				startIndex(atts);
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(elem_name.equals(localName))
				endName();
			if(elem_question_id.equals(localName))
				endQuestionId();
			if(elem_string.equals(localName))
				endString();
			if(elem_alters.equals(localName))
				endAlters();
			if(elem_adjacent.equals(localName))
				endAdjacent();
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			//TODO: could it happen that the parser calls characters several times for 'one chunk' of PCDATA?
			//TODO: maybe it is saver to call StringBuffer.apend(ch, start, length)
			parsedValue = new String(ch, start,length); 
		}
		
		private void startInterview (Attributes atts) throws SAXException {
			//Check if current study matches with the study of interview. 
			long studyIdFromInterview = Long.valueOf(atts.getValue("", interview_study_id));
			if(studyIdFromInterview != study.studyId())
				throw new SAXException("The study loaded and the study of the interview does not match");
				
		}
		
		private void startAlters(Attributes atts) {
			alterPair = new String[2];
		}
		
		private void startIndex(Attributes atts) {
			String indexNameAttribute = atts.getValue("", index_name);
			if(indexNameAttribute == null)
				return;
			if(alterPair[0] == null)
				alterPair[0] = indexNameAttribute;
			else if(alterPair[1] == null)
				alterPair[1] = indexNameAttribute;
			else 
				return;
		}
		
		private void endQuestionId() {
			long questionId = Long.valueOf(parsedValue.toString().trim());
			currentQuestion = study.getQuestion(questionId);
		}
		
		private void endString() {
			String value = parsedValue.toString().trim();
			if(currentQuestion.type() == Questionnaire.Q_ABOUT_EGO)
				setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
						currentQuestion.title(), Ego.getInstance(), value);
			if(currentQuestion.type() == Questionnaire.Q_ABOUT_ALTERS)
				alterResponse = value;
		}
		
		private void endName() {
			String alterName = parsedValue.toString().trim(); 
			if(!hasAlter(alterName)) {
				addToLifetimeOfAlter(interval, alterName);
			}
		}
				
		private void endAdjacent() {
			areAdjacent = Boolean.valueOf(parsedValue.toString().trim()); 
		}	
		
		private void endAlters() {
			//Now we know alter names. We can store them answers.
			if (currentQuestion.type() == Questionnaire.Q_ABOUT_ALTERS)
				setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
						currentQuestion.title(), Alter.getInstance(alterPair[0]), alterResponse);
			if (currentQuestion.type() == Questionnaire.Q_ALTER_ALTER_TIES) {
				if (areAdjacent)
					addToLifetimeOfTie(interval,alterPair[0], alterPair[1]);
				//Add dyad value.
				setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(), 
						currentQuestion.title(), AlterAlterDyad.getInstance(alterPair[0], alterPair[1]),
						alterResponse);
			}	
		}
	}
	private class ImportGraphMLHandler extends DefaultHandler {

		//names of XML elements
		private static final String elem_key = "key";
		private static final String elem_choice = "choice";
		private static final String elem_graph = "graph";
		private static final String elem_node = "node";
		private static final String elem_edge = "edge";
		private static final String elem_dyad = "dyad";
		private static final String elem_data = "data";

		//names of XML attributes
		//attributes of key
		private static final String key_attr_description = "attr.description";
		private static final String key_attr_name = "attr.name";
		private static final String key_attr_type = "attr.type";
		private static final String key_attr_direction_type = "attr.direction.type";
		private static final String key_attr_dynamic_type = "attr.dynamic.type";
		private static final String key_attr_has_choices = "attr.has.choices";
		private static final String key_for = "for";
		private static final String key_id = "id";
		//attributes of choice
		private static final String choice_value = "value";
		//attributes of node
		private static final String node_id = "id";
		private static final String node_is_ego = "egosmart.is.ego";
		//attributes of edge
		private static final String edge_source = "source";
		private static final String edge_target = "target";
		//attributes of dyad
		private static final String dyad_source = "source";
		private static final String dyad_target = "target";
		//attributes of data
		private static final String data_key = "key";
		//time attributes
		private static final String time_interval_start = "time.interval.start";
		private static final String time_interval_end = "time.interval.end";
		private static final String time_point = "time.point";
		private static final String time_intervals_start = "time.intervals.start";
		private static final String time_intervals_end = "time.intervals.end";
		private static final String time_points = "time.points";
		
		private DateFormat formatter;
		
		//data to remember
		private StringBuffer curr_pcd; //parsed character data starting from an element start tag
		private String egoID; //id of the node that represents ego
		private String keyIdOfAlterName;
		private String enclosingNodeID; //the id of a node element that is parent to the current position or null
		private HashMap<String, String> nodeID2alterName; //maps node id's to alternames once these are known
		private String enclosingSourceID; //value of the source attribute of an enclosing edge or dyad
		private String enclosingTargetID; //value of the target attribute of an enclosing edge or dyad
		private Lifetime enclosingDataValueLifetime;
		private int dataDepth;// is -1 outside of any data element, 0 if enclosed in one data element, ...
		private int keyDepth;// is -1 outside of any key element, 0 if enclosed in one key element, ...
		private HashMap<String, String> keyID2AttrName;//including all prefixes (also for nested keys, i.e., secondary attributes)
		private ArrayList<StringBuffer> textValueAtDataDepth;
		private ArrayList<String> keyAtDataDepth;
		private String primaryAttributeDomain; //domain of outermost key element; null if outside of any key element 
		private String primaryAttributeName; //name of outermost key element; null if outside of any key element 
		private String secondaryAttributeName;
		private HashMap<String, String> secondaryAttrName2Value;
		
		private ImportGraphMLHandler(){
		}

		@Override
		public void startDocument() throws SAXException{
			formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			curr_pcd = new StringBuffer();
			nodeID2alterName = new HashMap<String, String>();
			keyID2AttrName = new HashMap<String, String>();
			textValueAtDataDepth = new ArrayList<StringBuffer>();
			keyAtDataDepth = new ArrayList<String>();
			dataDepth = -1;
			keyDepth = -1;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException{
			if(elem_key.equals(localName))
				startKeyElement(atts);
			if(elem_choice.equals(localName))
				startChoiceElement(atts);
			if(elem_graph.equals(localName))
				startGraphElement(atts);
			if(elem_node.equals(localName))
				startNodeElement(atts);
			if(elem_edge.equals(localName))
				startEdgeElement(atts);
			if(elem_dyad.equals(localName))
				startDyadElement(atts);
			if(elem_data.equals(localName))
				startDataElement(atts);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException{
			if(elem_key.equals(localName))
				endKeyElement();
			if(elem_graph.equals(localName))
				endGraphElement();
			if(elem_node.equals(localName))
				endNodeElement();
			if(elem_edge.equals(localName))
				endEdgeElement();
			if(elem_dyad.equals(localName))
				endDyadElement();
			if(elem_data.equals(localName))
				endDataElement();
		}

		private void startKeyElement(Attributes atts){
			++keyDepth;
			String id = atts.getValue("", key_id);
			String name = atts.getValue("", key_attr_name);
			String valueType = atts.getValue("", key_attr_type);
			String directionType = atts.getValue("", key_attr_direction_type);
			String description = atts.getValue("", key_attr_description);
			String dynamicType = atts.getValue("", key_attr_dynamic_type);
			String hasChoices = atts.getValue("", key_attr_has_choices);
			if(name == null || name.trim().length() == 0)
				return;
			name = name.trim();
			keyID2AttrName.put(id, name);
			int valueType2Take = PersonalNetwork.ATTRIB_TYPE_TEXT;
			if(valueType != null && valueType.equals("double"))
				valueType2Take = PersonalNetwork.ATTRIB_TYPE_NUMBER;
			if(hasChoices != null && hasChoices.equals("true"))
				valueType2Take = PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE;
			if(directionType == null)
				directionType = DYAD_DIRECTION_SYMMETRIC;
			if(dynamicType == null)
				dynamicType = ATTRIBUTE_DYNAMIC_TYPE_STATE;
			if(keyDepth == 0){
				if(ATTR_NAME_ALTER_NAME.equals(name))
					keyIdOfAlterName = id;
				if(!ATTR_NAME_ALTER_NAME.equals(name) && 
						!ATTR_NAME_IS_EGO.equals(name) && 
						!ATTR_NAME_EGO_NAME.equals(name)){
					String[] tokens = name.trim().split(":");
					if(tokens.length <= 1)
						return;
					String domain = tokens[0];
					if(DOMAIN_EGO.equals(domain) || 
							DOMAIN_ALTER.equals(domain) || 
							DOMAIN_EGO_ALTER.equals(domain) || 
							DOMAIN_ALTER_ALTER.equals(domain)){
						StringBuffer attrName = new StringBuffer();
						for(int i = 1; i < tokens.length; ++i){
							attrName.append(tokens[i].trim());
							if(i < tokens.length -1)
								attrName.append(":");
						}
						addAttribute(domain, attrName.toString(), 
								description, valueType2Take, directionType, dynamicType);
						primaryAttributeDomain = domain;
						primaryAttributeName = attrName.toString();
					}
				}
			} 
			if (keyDepth > 0){
				if(primaryAttributeDomain != null && primaryAttributeName != null){
					int length_of_prefixes = ATTR_NAME_PREFIX_SECONDARY.length() + 
							primaryAttributeDomain.length() + 1 + 
							primaryAttributeName.length() + 1;
					String attrName = name.trim();
					//check if the name follows the egosmart naming conventions for secondary attributes
					if(name.startsWith(ATTR_NAME_PREFIX_SECONDARY) && name.length() > length_of_prefixes)
						attrName = name.substring(length_of_prefixes).trim();
					addSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, 
							attrName, description, valueType2Take);
					secondaryAttributeName = attrName;
				}
			}
		}

		private void endKeyElement(){
			--keyDepth;
			if(keyDepth < 0){
				primaryAttributeDomain = null;
				primaryAttributeName = null;
			}
		}

		private void startChoiceElement(Attributes atts){
			String value = atts.getValue("", choice_value);
			if(keyDepth == 0 && primaryAttributeDomain != null && primaryAttributeName != null){
				addAttributeChoice(primaryAttributeDomain, primaryAttributeName, value);
			}
			if(keyDepth > 0 
					&& primaryAttributeDomain != null && primaryAttributeName != null && secondaryAttributeName != null){
				addSecondaryAttributeChoice(primaryAttributeDomain, primaryAttributeName, secondaryAttributeName, value);
			}
		}

		private void startGraphElement(Attributes atts){
			//nothing to do
		}

		private void endGraphElement(){
			//nothing to do
		}

		private void startNodeElement(Attributes atts){
			String id = atts.getValue("", node_id);
			enclosingNodeID = id;
			String isEgo = atts.getValue("", node_is_ego);
			if(isEgo != null && isEgo.equals("true")){
				egoID = id;
				return;
			}
			nodeID2alterName.put(id, id);
			Lifetime lifetime = readLifetime(atts);
			Iterator<TimeInterval> it = lifetime.getIterator();
			while(it.hasNext()){
				addToLifetimeOfAlter(it.next(), id);
			}
		}
		
		private void endNodeElement(){
			enclosingNodeID = null;
		}

		private Lifetime readLifetime(Attributes atts){
			Lifetime lifetime = new Lifetime();
			//read the single time interval
			String timeIntervalStartStr = atts.getValue("", time_interval_start);
			String timeIntervalEndStr = atts.getValue("", time_interval_end);
			if(timeIntervalStartStr != null && timeIntervalEndStr != null){
				try {
					long startTime = formatter.parse(timeIntervalStartStr.trim()).getTime();
					long endTime = formatter.parse(timeIntervalEndStr.trim()).getTime();
					lifetime.union(new TimeInterval(startTime, endTime));
				} catch(Exception e){}
			} else if(timeIntervalStartStr != null) {
				try {
					long startTime = formatter.parse(timeIntervalStartStr.trim()).getTime();
					lifetime.union(TimeInterval.getRightUnbounded(startTime));
				} catch(Exception e){}				
			} else if(timeIntervalEndStr != null){
				try {
					long endTime = formatter.parse(timeIntervalEndStr.trim()).getTime();
					lifetime.union(TimeInterval.getLeftUnbounded(endTime));
				} catch(Exception e){}				
			}
			//read the single time point
			String timePointStr = atts.getValue("", time_point);
			if(timePointStr != null){
				try {
					long timePoint = formatter.parse(timePointStr.trim()).getTime();
					lifetime.union(TimeInterval.getTimePoint(timePoint));
				} catch (Exception e){}
			}
			//read a union of time intervals
			String timeIntervalsStartStr = atts.getValue("", time_intervals_start);
			String timeIntervalsEndStr = atts.getValue("", time_intervals_end);
			if(timeIntervalsStartStr != null && timeIntervalsEndStr != null){
				String[] startStr = timeIntervalsStartStr.trim().split("\\s+");
				String[] endStr = timeIntervalsEndStr.trim().split("\\s+");
				if(startStr.length == endStr.length){
					for(int i = 0; i < startStr.length; ++i){
						try {
							long startTime = formatter.parse(startStr[i]).getTime();
							long endTime = formatter.parse(endStr[i]).getTime();
							lifetime.union(new TimeInterval(startTime, endTime));							
						} catch(Exception e){}
					}
				}
			}			
			//read a union of time points
			String timePointsStr = atts.getValue("", time_points);
			if(timePointsStr != null){
				String[] pointStr = timePointsStr.trim().split("\\s+");
				for(int i = 0; i < pointStr.length; ++i){
					try {
						long timePoint = formatter.parse(pointStr[i]).getTime();
						lifetime.union(TimeInterval.getTimePoint(timePoint));							
					} catch(Exception e){}
				}
			}			
			if(lifetime.size() == 0)
				lifetime.union(TimeInterval.getMaxInterval());
			return lifetime;
		}

		private void startEdgeElement(Attributes atts){
			String sourceID = atts.getValue("", edge_source);
			String targetID = atts.getValue("", edge_target);
			if(sourceID == null || targetID == null)
				return;
			enclosingSourceID = sourceID;
			enclosingTargetID = targetID;
			if(sourceID.equals(egoID) || targetID.equals(egoID))
				return;
			String source = nodeID2alterName.get(sourceID);
			String target = nodeID2alterName.get(targetID);
			if(source == null || target == null)
				return;
			Lifetime lifetime = readLifetime(atts);
			Iterator<TimeInterval> it = lifetime.getIterator();
			while(it.hasNext()){
				addToLifetimeOfTie(it.next(), source, target);
			}
		}

		private void endEdgeElement(){
			enclosingSourceID = null;
			enclosingTargetID = null;
		}

		private void startDyadElement(Attributes atts){
			String sourceID = atts.getValue("", dyad_source);
			String targetID = atts.getValue("", dyad_target);
			if(sourceID == null || targetID == null)
				return;
			enclosingSourceID = sourceID;
			enclosingTargetID = targetID;
		}

		private void endDyadElement(){
			enclosingSourceID = null;
			enclosingTargetID = null;
		}

		private void startDataElement(Attributes atts){
			if(dataDepth >= 0){
				textValueAtDataDepth.get(dataDepth).append(curr_pcd.toString().trim());
			}
			++dataDepth;
			textValueAtDataDepth.add(dataDepth, new StringBuffer());
			curr_pcd = new StringBuffer();
			String keyID = atts.getValue("", data_key);
			keyAtDataDepth.add(dataDepth, keyID);
			if(dataDepth == 0){
				enclosingDataValueLifetime = readLifetime(atts);
				secondaryAttrName2Value = new HashMap<String, String>();
			}
		}

		private void endDataElement(){
			textValueAtDataDepth.get(dataDepth).append(curr_pcd.toString().trim());
			//if the data holds the altername put it into the map
			if(keyIdOfAlterName != null && keyIdOfAlterName.equals(keyAtDataDepth.get(dataDepth))){
				if(enclosingNodeID != null){
					String alterName = textValueAtDataDepth.get(dataDepth).toString().trim();
					nodeID2alterName.put(enclosingNodeID, alterName);
					renameAlter(enclosingNodeID, alterName);
				}
			}
			String value = textValueAtDataDepth.get(dataDepth).toString();
			String prefixedAttrName = keyID2AttrName.get(keyAtDataDepth.get(dataDepth));
			if(dataDepth > 0 && primaryAttributeDomain != null && primaryAttributeName != null){//it's a secondary attribute
				int length_of_prefixes = ATTR_NAME_PREFIX_SECONDARY.length() + 
						primaryAttributeDomain.length() + 1 + 
						primaryAttributeName.length() + 1;
				//check if the name follows the egosmart naming conventions for secondary attributes
				String secondaryAttrName = prefixedAttrName;
				if(prefixedAttrName != null && prefixedAttrName.startsWith(ATTR_NAME_PREFIX_SECONDARY) && 
						prefixedAttrName.length() > length_of_prefixes)
					secondaryAttrName = prefixedAttrName.substring(length_of_prefixes).trim();
				if(secondaryAttrName != null)
					secondaryAttrName2Value.put(secondaryAttrName, value);
			}
			if(dataDepth == 0){
				if(prefixedAttrName != null){
					String[] tokens = prefixedAttrName.split(":");
					if(tokens.length > 1){
						String domain = tokens[0];
						primaryAttributeDomain = domain;
						StringBuffer nameBuffer = new StringBuffer();
						for(int i = 1; i < tokens.length; ++i){
							nameBuffer.append(tokens[i].trim());
							if(i < tokens.length -1)
								nameBuffer.append(":");
						}
						String name = nameBuffer.toString();
						primaryAttributeName = name;
						if(!ATTR_NAME_ALTER_NAME.equals(name) && 
								!ATTR_NAME_IS_EGO.equals(name) && 
								!ATTR_NAME_EGO_NAME.equals(name)){
							Element element = null;
							if(DOMAIN_EGO.equals(domain)){
								element = Ego.getInstance();
							}
							if(DOMAIN_ALTER.equals(domain)){
								 String alterName = nodeID2alterName.get(enclosingNodeID);
								 if(alterName != null)
									 element = Alter.getInstance(alterName);
							}
							if(DOMAIN_EGO_ALTER.equals(domain)){
								 String alterName = null;
								 String direction = null;
								 if(enclosingSourceID != null && enclosingSourceID.equals(egoID)){
									 direction = DYAD_DIRECTION_OUT;
									 alterName = nodeID2alterName.get(enclosingTargetID);
								 }
								 if(enclosingTargetID != null && enclosingTargetID.equals(egoID)){
									 direction = DYAD_DIRECTION_IN;
									 alterName = nodeID2alterName.get(enclosingSourceID);
								 }
								 element = EgoAlterDyad.getInstance(alterName, direction);
							}
							if(DOMAIN_ALTER_ALTER.equals(domain)){
								String sourceName = nodeID2alterName.get(enclosingSourceID);
								String targetName = nodeID2alterName.get(enclosingTargetID);
								if(sourceName != null && targetName != null){
									element = AlterAlterDyad.getInstance(sourceName, targetName);
								}
							}
							if(element != null && enclosingDataValueLifetime != null){
								Iterator<TimeInterval> it = enclosingDataValueLifetime.getIterator();
								while(it.hasNext()){
									TimeInterval time = it.next();
									setAttributeValueAt(time, name, element, value);
									String datumID = getAttributeDatumIDAt(time.getStartTime(), name, element);
									for(String secondaryAttrName : secondaryAttrName2Value.keySet()){
										setSecondaryAttributeValue(datumID, secondaryAttrName, 
												secondaryAttrName2Value.get(secondaryAttrName));
									}
								}
							}
						}
					}
				}
				enclosingDataValueLifetime = null;
				primaryAttributeDomain = null;
				primaryAttributeName = null;
			}
			textValueAtDataDepth.remove(dataDepth);
			keyAtDataDepth.remove(dataDepth);
			--dataDepth;
			curr_pcd = new StringBuffer();
		}

		@Override
		//called whenever the parser meets parsed character data
		public void characters(char[] ch, int start, int length) throws SAXException{
			curr_pcd.append(ch, start, length);
		}

		@Override
		public void endDocument() throws SAXException{
			//nothing to do
		}

	}

	private class PersonalNetworkHistoryDBOpenHelper extends SQLiteOpenHelper {

		PersonalNetworkHistoryDBOpenHelper(Context context) {
			super(context, DATABASE_NAME_PREFIX, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase localDB) {
			createTables(localDB);
			ContentValues values = new ContentValues();
			values.put(PROPERTIES_COL_KEY, PROPERTIES_KEY_DB_INITIALIZED);
			values.put(PROPERTIES_COL_VALUE, "false");
			localDB.insert(PROPERTIES_TABLE_NAME, null, values);
		}

		private void createTables(SQLiteDatabase localDB) {
			localDB.execSQL(PROPERTIES_TABLE_CREATE_CMD);
			localDB.execSQL(ALTERS_TABLE_CREATE_CMD);
			localDB.execSQL(ALTERS_HISTORY_TABLE_CREATE_CMD);
			localDB.execSQL(TIES_TABLE_CREATE_CMD);
			localDB.execSQL(TIES_HISTORY_TABLE_CREATE_CMD);
			localDB.execSQL(ALTER_ALTER_DYADS_TABLE_CREATE_CMD);
			localDB.execSQL(ALTER_ALTER_DYADS_HISTORY_TABLE_CREATE_CMD);
			localDB.execSQL(SECONDARY_ATTRIBS_NAMES_TABLE_CREATE_CMD);
			localDB.execSQL(SECONDARY_ATTRIBS_VALUES_TABLE_CREATE_CMD);
			localDB.execSQL(SECONDARY_ATTRIBS_CHOICES_TABLE_CREATE_CMD);
			localDB.execSQL(EGO_ATTRIBS_NAMES_TABLE_CREATE_CMD);
			localDB.execSQL(EGO_ATTRIBS_VALUES_TABLE_CREATE_CMD);
			localDB.execSQL(EGO_ATTRIBS_CHOICES_TABLE_CREATE_CMD);
			localDB.execSQL(ALTER_ATTRIBS_NAMES_TABLE_CREATE_CMD);
			localDB.execSQL(ALTER_ATTRIBS_VALUES_TABLE_CREATE_CMD);
			localDB.execSQL(ALTER_ATTRIBS_CHOICES_TABLE_CREATE_CMD);
			localDB.execSQL(EGO_ALTER_ATTRIBS_NAMES_TABLE_CREATE_CMD);
			localDB.execSQL(EGO_ALTER_ATTRIBS_VALUES_TABLE_CREATE_CMD);
			localDB.execSQL(EGO_ALTER_ATTRIBS_CHOICES_TABLE_CREATE_CMD);
			localDB.execSQL(ALTER_ALTER_ATTRIBS_NAMES_TABLE_CREATE_CMD);
			localDB.execSQL(ALTER_ALTER_ATTRIBS_VALUES_TABLE_CREATE_CMD);
			localDB.execSQL(ALTER_ALTER_ATTRIBS_CHOICES_TABLE_CREATE_CMD);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}
	}
	private void initBasicAttributes(){
		///////////////////////////////////////////////////////
		// EGO
		///////////////////////////////////////////////////////
		//Gender
		addAttribute(DOMAIN_EGO, 
				activity.getString(R.string.ego_attribute_gender_name), 
				activity.getString(R.string.ego_attribute_gender_description), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE, 
				DYAD_DIRECTION_SYMMETRIC, 
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		Resources res = activity.getResources();
		String[] choices = res.getStringArray(R.array.ego_attribute_gender_choices);
		setAttributeChoices(DOMAIN_EGO, 
				activity.getString(R.string.ego_attribute_gender_name), 
				new LinkedHashSet<String>(Arrays.asList(choices)));
		//City
		addAttribute(DOMAIN_EGO, 
				activity.getString(R.string.ego_attribute_city_name), 
				activity.getString(R.string.ego_attribute_city_description), 
				PersonalNetwork.ATTRIB_TYPE_TEXT, 
				DYAD_DIRECTION_SYMMETRIC, 
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		///////////////////////////////////////////////////////
		// ALTER
		///////////////////////////////////////////////////////
		//Gender
		addAttribute(DOMAIN_ALTER, 
				activity.getString(R.string.alter_attribute_gender_name), 
				activity.getString(R.string.alter_attribute_gender_description), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE,
				DYAD_DIRECTION_SYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		choices = res.getStringArray(R.array.alter_attribute_gender_choices);
		setAttributeChoices(DOMAIN_ALTER, 
				activity.getString(R.string.alter_attribute_gender_name), 
				new LinkedHashSet<String>(Arrays.asList(choices)));
		//City
		addAttribute(DOMAIN_ALTER, 
				activity.getString(R.string.alter_attribute_city_name), 
				activity.getString(R.string.alter_attribute_city_description), 
				PersonalNetwork.ATTRIB_TYPE_TEXT,
				DYAD_DIRECTION_SYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		//occupation
		addAttribute(DOMAIN_ALTER, 
				activity.getString(R.string.alter_attribute_occupation_name), 
				activity.getString(R.string.alter_attribute_occupation_description), 
				PersonalNetwork.ATTRIB_TYPE_TEXT,
				DYAD_DIRECTION_SYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		//financial support
		addAttribute(DOMAIN_ALTER, 
				activity.getString(R.string.alter_attribute_would_help_financially_name), 
				activity.getString(R.string.alter_attribute_would_help_financially_description), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE,
				DYAD_DIRECTION_SYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		choices = res.getStringArray(R.array.alter_attribute_would_help_financially_choices);
		setAttributeChoices(DOMAIN_ALTER, 
				activity.getString(R.string.alter_attribute_would_help_financially_name), 
				new LinkedHashSet<String>(Arrays.asList(choices)));
		//active support
		addAttribute(DOMAIN_ALTER, 
				activity.getString(R.string.alter_attribute_would_help_actively_name), 
				activity.getString(R.string.alter_attribute_would_help_actively_description), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE,
				DYAD_DIRECTION_SYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		choices = res.getStringArray(R.array.alter_attribute_would_help_actively_choices);
		setAttributeChoices(DOMAIN_ALTER, 
				activity.getString(R.string.alter_attribute_would_help_actively_name), 
				new LinkedHashSet<String>(Arrays.asList(choices)));
		///////////////////////////////////////////////////////
		// EGO-ALTER
		///////////////////////////////////////////////////////
		//Type of relationship
		addAttribute(DOMAIN_EGO_ALTER, 
				activity.getString(R.string.ego_alter_attribute_type_of_relation_name), 
				activity.getString(R.string.ego_alter_attribute_type_of_relation_description), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE, 
				PersonalNetwork.DYAD_DIRECTION_SYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		choices = res.getStringArray(R.array.ego_alter_attribute_relation_choices);
		setAttributeChoices(DOMAIN_EGO_ALTER, 
				activity.getString(R.string.ego_alter_attribute_type_of_relation_name), 
				new LinkedHashSet<String>(Arrays.asList(choices)));
		//Importance
		addAttribute(DOMAIN_EGO_ALTER, 
				activity.getString(R.string.ego_alter_attribute_importance_name), 
				activity.getString(R.string.ego_alter_attribute_importance_description), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE, 
				PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		choices = res.getStringArray(R.array.ego_alter_attribute_importance_choices);
		setAttributeChoices(DOMAIN_EGO_ALTER, 
				activity.getString(R.string.ego_alter_attribute_importance_name), 
				new LinkedHashSet<String>(Arrays.asList(choices)));
		//Valuation
		addAttribute(DOMAIN_EGO_ALTER, 
				activity.getString(R.string.ego_alter_attribute_valuation_name), 
				activity.getString(R.string.ego_alter_attribute_valuation_description), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE, 
				PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		choices = res.getStringArray(R.array.ego_alter_attribute_valuation_choices);
		setAttributeChoices(DOMAIN_EGO_ALTER, 
				activity.getString(R.string.ego_alter_attribute_valuation_name), 
				new LinkedHashSet<String>(Arrays.asList(choices)));
		///////////////////////////////////////////////////////
		// ALTER-ALTER
		///////////////////////////////////////////////////////
		//Type of relationship
		addAttribute(DOMAIN_ALTER_ALTER, 
				activity.getString(R.string.alter_alter_attribute_type_of_relation_name), 
				activity.getString(R.string.alter_alter_attribute_type_of_relation_description), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE, 
				PersonalNetwork.DYAD_DIRECTION_SYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		choices = res.getStringArray(R.array.alter_alter_attribute_relation_choices);
		setAttributeChoices(DOMAIN_ALTER_ALTER, 
				activity.getString(R.string.alter_alter_attribute_type_of_relation_name), 
				new LinkedHashSet<String>(Arrays.asList(choices)));
		//Like/dislike
		addAttribute(DOMAIN_ALTER_ALTER, 
				activity.getString(R.string.alter_alter_attribute_valuation_name), 
				activity.getString(R.string.alter_alter_attribute_valuation_description), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE, 
				PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC,
				ATTRIBUTE_DYNAMIC_TYPE_STATE);
		choices = res.getStringArray(R.array.alter_alter_attribute_valuation_choices);
		setAttributeChoices(DOMAIN_ALTER_ALTER, 
				activity.getString(R.string.alter_alter_attribute_valuation_name), 
				new LinkedHashSet<String>(Arrays.asList(choices)));
	}

	private void initSystemAttributes() {
		//////////////////////////////////////////////////////////////////
		//System attributes (prefix egosmart:)
		//////////////////////////////////////////////////////////////////
		//Ego memos
		addAttribute(DOMAIN_EGO, getEgoMemosAttributeName(), 
				activity.getString(R.string.attribute_description_for_ego_memos), 
				PersonalNetwork.ATTRIB_TYPE_TEXT, 
				DYAD_DIRECTION_SYMMETRIC, ATTRIBUTE_DYNAMIC_TYPE_EVENT);
		//Alter memos
		addAttribute(DOMAIN_ALTER, getAlterMemosAttributeName(), 
				activity.getString(R.string.attribute_description_for_alter_memos), 
				PersonalNetwork.ATTRIB_TYPE_TEXT, 
				DYAD_DIRECTION_SYMMETRIC, ATTRIBUTE_DYNAMIC_TYPE_EVENT);
		//Alter-alter memos
		addAttribute(DOMAIN_ALTER_ALTER, getAlterAlterMemosAttributeName(), 
				activity.getString(R.string.attribute_description_for_alter_alter_memos), 
				PersonalNetwork.ATTRIB_TYPE_TEXT, 
				DYAD_DIRECTION_ASYMMETRIC, ATTRIBUTE_DYNAMIC_TYPE_EVENT);
		//Ego-alter contact events
		addAttribute(DOMAIN_EGO_ALTER, getEgoAlterContactEventAttributeName(), 
				activity.getString(R.string.attribute_description_for_ego_alter_contact), 
				PersonalNetwork.ATTRIB_TYPE_TEXT, 
				DYAD_DIRECTION_SYMMETRIC, ATTRIBUTE_DYNAMIC_TYPE_EVENT);
		//Alter-alter contact events
		addAttribute(DOMAIN_ALTER_ALTER, getAlterAlterContactEventAttributeName(), 
				activity.getString(R.string.attribute_description_for_alter_alter_contact), 
				PersonalNetwork.ATTRIB_TYPE_TEXT, 
				DYAD_DIRECTION_SYMMETRIC, ATTRIBUTE_DYNAMIC_TYPE_EVENT);
		//////////////////////////////////////////////////////////////////
		//Special system secondary attributes (prefix egosmart:)
		//TODO: change all this
		//////////////////////////////////////////////////////////////////
		//time stamps when the start/end times of the data entry have been set 
		initSecondaryAttribute(PersonalNetwork.DOMAIN_ALTER, 
				getAlterLifetimeAttributeName(), 
				getSecondaryAttributeNameTimestampStart(), 
				PersonalNetwork.ATTRIB_TYPE_TEXT,//TODO: is of type long 
				"timestamp when the start time of the data entry has been set", //TODO: outsource 
				db);			
		initSecondaryAttribute(PersonalNetwork.DOMAIN_ALTER, 
				getAlterLifetimeAttributeName(), 
				getSecondaryAttributeNameTimestampEnd(), 
				PersonalNetwork.ATTRIB_TYPE_TEXT,//TODO: is of type long 
				"timestamp when the end time of the data entry has been set", //TODO: outsource 
				db);			
		initSecondaryAttribute(PersonalNetwork.DOMAIN_ALTER_ALTER, 
				getTieLifetimeAttributeName(), 
				getSecondaryAttributeNameTimestampStart(), 
				PersonalNetwork.ATTRIB_TYPE_TEXT,//TODO: is of type long 
				"timestamp when the start time of the data entry has been set", //TODO: outsource 
				db);			
		initSecondaryAttribute(PersonalNetwork.DOMAIN_ALTER_ALTER, 
				getTieLifetimeAttributeName(), 
				getSecondaryAttributeNameTimestampEnd(), 
				PersonalNetwork.ATTRIB_TYPE_TEXT,//TODO: is of type long 
				"timestamp when the end time of the data entry has been set", //TODO: outsource 
				db);			
		initSecondaryAttribute(PersonalNetwork.DOMAIN_ALTER_ALTER, 
				getAlterAlterDyadLifetimeAttributeName(), 
				getSecondaryAttributeNameTimestampStart(), 
				PersonalNetwork.ATTRIB_TYPE_TEXT,//TODO: is of type long 
				"timestamp when the start time of the data entry has been set", //TODO: outsource 
				db);			
		initSecondaryAttribute(PersonalNetwork.DOMAIN_ALTER_ALTER, 
				getAlterAlterDyadLifetimeAttributeName(), 
				getSecondaryAttributeNameTimestampEnd(), 
				PersonalNetwork.ATTRIB_TYPE_TEXT,//TODO: is of type long 
				"timestamp when the end time of the data entry has been set", //TODO: outsource 
				db);			
		// Memo text (ego)
		initSecondaryAttributeText(PersonalNetwork.DOMAIN_EGO, 
				getEgoMemosAttributeName(), db);
		// Memo text (alter)
		initSecondaryAttributeText(PersonalNetwork.DOMAIN_ALTER, 
				getAlterMemosAttributeName(), db);
		// Memo text (alter-alter)
		initSecondaryAttributeText(PersonalNetwork.DOMAIN_ALTER_ALTER, 
				getAlterAlterMemosAttributeName(), db);
		//Contact form (ego-alter)
		initSecondaryAttributeContactForm(PersonalNetwork.DOMAIN_EGO_ALTER, 
				getEgoAlterContactEventAttributeName(), db);
		//Contact form (alter-alter)
		initSecondaryAttributeContactForm(PersonalNetwork.DOMAIN_ALTER_ALTER, 
				getAlterAlterContactEventAttributeName(), db);
		//Contact content (ego-alter)
		initSecondaryAttributeContactContent(PersonalNetwork.DOMAIN_EGO_ALTER, 
				getEgoAlterContactEventAttributeName(), db);
		//Contact content (alter-alter)
		initSecondaryAttributeContactContent(PersonalNetwork.DOMAIN_ALTER_ALTER, 
				getAlterAlterContactEventAttributeName(), db);
		//Contact atmosphere (ego-alter)
		initSecondaryAttributeContactAtmosphere(PersonalNetwork.DOMAIN_EGO_ALTER, 
				getEgoAlterContactEventAttributeName(), db);
		//Contact atmosphere (alter-alter)
		initSecondaryAttributeContactAtmosphere(PersonalNetwork.DOMAIN_ALTER_ALTER, 
				getAlterAlterContactEventAttributeName(), db);
		//Contact textual information (ego-alter)
		initSecondaryAttributeText(PersonalNetwork.DOMAIN_EGO_ALTER, 
				getEgoAlterContactEventAttributeName(), db);
		//Contact textual information (alter-alter)
		initSecondaryAttributeText(PersonalNetwork.DOMAIN_ALTER_ALTER, 
				getAlterAlterContactEventAttributeName(), db);
	}

	private void initSecondaryAttributeContactAtmosphere(String primaryAttributeDomain,
			String primaryAttributeName, SQLiteDatabase localDB){
		initSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, 
				getSecondaryAttributeNameContactAtmosphere(), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE, 
				activity.getString(R.string.secondary_attribute_description_contact_atmosphere), 
				localDB);			
		//add choices
		initSecondaryAttributeChoices(primaryAttributeDomain, primaryAttributeName, getSecondaryAttributeNameContactAtmosphere(), 
				R.array.secondary_attribute_contact_atmosphere_choices, localDB);
	}

	private void initSecondaryAttributeContactContent(String primaryAttributeDomain,
			String primaryAttributeName, SQLiteDatabase localDB){
		initSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, 
				getSecondaryAttributeNameContactContent(), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE, 
				activity.getString(R.string.secondary_attribute_description_contact_content), 
				localDB);			
		//add choices
		initSecondaryAttributeChoices(primaryAttributeDomain, primaryAttributeName, getSecondaryAttributeNameContactContent(), 
				R.array.secondary_attribute_contact_content_choices, localDB);
	}

	private void initSecondaryAttributeContactForm(String primaryAttributeDomain,
			String primaryAttributeName, SQLiteDatabase localDB){
		initSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, 
				getSecondaryAttributeNameContactForm(), 
				PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE, 
				activity.getString(R.string.secondary_attribute_description_contact_form), 
				localDB);			
		//add choices
		initSecondaryAttributeChoices(primaryAttributeDomain, primaryAttributeName, getSecondaryAttributeNameContactForm(), 
				R.array.secondary_attribute_contact_form_choices, localDB);
	}

	private void initSecondaryAttributeChoices(String primaryAttributeDomain,
			String primaryAttributeName, String secondaryAttributeName,
			int resourceID, SQLiteDatabase localDB){
		Resources res = activity.getResources();
		String[] choices = res.getStringArray(resourceID);
		for(int i = 0; i < choices.length; ++i){
			ContentValues values = new ContentValues();
			values.put(ATTRIBUTES_COL_DOMAIN, primaryAttributeName);
			values.put(ATTRIBUTES_COL_NAME, primaryAttributeName);
			values.put(SECONDARY_ATTRIBUTES_COL_NAME, secondaryAttributeName);
			values.put(ATTRIBUTES_COL_CHOICE, choices[i]);
			localDB.insert(SECONDARY_ATTRIBS_CHOICES_TABLE_NAME, null, values);
		}

	}

	private void initSecondaryAttributeText(String primaryAttributeDomain,
			String primaryAttributeName, SQLiteDatabase localDB){
		initSecondaryAttribute(primaryAttributeDomain, primaryAttributeName, 
				getSecondaryAttributeNameText(), 
				PersonalNetwork.ATTRIB_TYPE_TEXT, 
				activity.getString(R.string.secondary_attribute_description_text), 
				localDB);			
	}

	private void initSecondaryAttribute(String primaryAttributeDomain,
			String primaryAttributeName, 
			String secondaryAttributeName, 
			int valueType, String description, SQLiteDatabase localDB){
		ContentValues values = new ContentValues();
		values.put(ATTRIBUTES_COL_DOMAIN, primaryAttributeDomain);
		values.put(ATTRIBUTES_COL_NAME, primaryAttributeName);
		values.put(SECONDARY_ATTRIBUTES_COL_NAME, secondaryAttributeName);
		values.put(ATTRIBUTES_COL_DESCRIPTION, description);
		values.put(ATTRIBUTES_COL_VALUE_TYPE, valueType);
		localDB.insert(SECONDARY_ATTRIBS_NAMES_TABLE_NAME, null, values);			
	}

	/**
	 * Retrieves a property of the given attribute for the given domain.
	 * 
	 * The properties that can be retrieved this way are: valueType (the integer as a string), description,
	 * directionType (if applies for the given domain), or dynamicType.
	 * 
	 * Does not check the arguments for sanity.
	 *
	 * @param domain
	 * @param attributeName
	 * @param propertyColumnName
	 * @return
	 */
	private String getAttributeProperty(String domain, String attributeName, String propertyColumnName){
		String selection = ATTRIBUTES_COL_NAME + " = ?";
		String[] selectArgs = {attributeName};
		String tableName = getAttributeNamesTableNameForDomain(domain);
		Cursor c = db.query(tableName, 
				new String[]{propertyColumnName}, 
				selection, 
				selectArgs, null, null, null);
		int col_of_type = c.getColumnIndexOrThrow(propertyColumnName);
		if(!c.moveToFirst()){
			c.close();
			return null;
		}
		String ret = c.getString(col_of_type);
		c.close();
		return ret;
	}
	
	private boolean attributeDomainExists(String domain){
		return PersonalNetwork.DOMAIN_EGO.equals(domain) || 
				PersonalNetwork.DOMAIN_ALTER.equals(domain) || 
				PersonalNetwork.DOMAIN_EGO_ALTER.equals(domain) ||
				PersonalNetwork.DOMAIN_ALTER_ALTER.equals(domain);
	}
	
	private boolean attributeValueTypeExists(int valueType){
		return valueType >= 0 && valueType < PersonalNetwork.ATTRIB_TYPE_NAMES.length;
	}

	private boolean attributeDynamicTypeExists(String dynamicType){
		return ATTRIBUTE_DYNAMIC_TYPE_STATE.equals(dynamicType) || 
				ATTRIBUTE_DYNAMIC_TYPE_EVENT.equals(dynamicType);
	}
	
	private boolean domainHasAttributeDirectionType(String domain){
		return PersonalNetwork.DOMAIN_EGO_ALTER.equals(domain) ||
				PersonalNetwork.DOMAIN_ALTER_ALTER.equals(domain);
	}
	
	/**
	 * Returns whether the given attribute direction type existis for the given domain.
	 * 
	 * Always returns true if the given domain does not have an attribute direction type.
	 * 
	 * @param domain
	 * @param directionType
	 * @return
	 */
	private boolean attributeDirectionTypeExistsForDomain(String domain, String directionType){
		if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(domain))
			return PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC.equals(directionType) ||
					PersonalNetwork.DYAD_DIRECTION_SYMMETRIC.equals(directionType) ||
					PersonalNetwork.DYAD_DIRECTION_OUT.equals(directionType) ||
					PersonalNetwork.DYAD_DIRECTION_IN.equals(directionType);
		if(PersonalNetwork.DOMAIN_ALTER_ALTER.equals(domain))
			return PersonalNetwork.DYAD_DIRECTION_ASYMMETRIC.equals(directionType) ||
					PersonalNetwork.DYAD_DIRECTION_SYMMETRIC.equals(directionType);
		return true;
					
	}
	
	/**
	 * Returns the name of the table holding the attribute names for the given domain or null
	 * if the domain is unknown.
	 * 
	 * @param domain
	 * @return
	 */
	private String getAttributeNamesTableNameForDomain(String domain){
		String tableName = null;
		if(PersonalNetwork.DOMAIN_EGO.equals(domain))
			tableName = EGO_ATTRIBS_NAMES_TABLE_NAME;
		if(PersonalNetwork.DOMAIN_ALTER.equals(domain))
			tableName = ALTER_ATTRIBS_NAMES_TABLE_NAME;
		if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(domain))
			tableName = EGO_ALTER_ATTRIBS_NAMES_TABLE_NAME;
		if(PersonalNetwork.DOMAIN_ALTER_ALTER.equals(domain))
			tableName = ALTER_ALTER_ATTRIBS_NAMES_TABLE_NAME;
		return tableName;
	}
	
	/**
	 * Returns the name of the table holding the attribute values for the given domain or null
	 * if the domain is unknown.
	 * 
	 * @param domain
	 * @return
	 */
	private String getAttributeValuesTableNameForDomain(String domain){
		String tableName = null;
		if(PersonalNetwork.DOMAIN_EGO.equals(domain))
			tableName = EGO_ATTRIBS_VALUES_TABLE_NAME;
		if(PersonalNetwork.DOMAIN_ALTER.equals(domain))
			tableName = ALTER_ATTRIBS_VALUES_TABLE_NAME;
		if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(domain))
			tableName = EGO_ALTER_ATTRIBS_VALUES_TABLE_NAME;
		if(PersonalNetwork.DOMAIN_ALTER_ALTER.equals(domain))
			tableName = ALTER_ALTER_ATTRIBS_VALUES_TABLE_NAME;
		return tableName;
	}
	
	/**
	 * Returns the name of the table holding the attribute choices for the given domain or null
	 * if the domain is unknown.
	 * 
	 * @param domain
	 * @return
	 */
	private String getAttributeChoicesTableNameForDomain(String domain){
		String tableName = null;
		if(PersonalNetwork.DOMAIN_EGO.equals(domain))
			tableName = EGO_ATTRIBS_CHOICES_TABLE_NAME;
		if(PersonalNetwork.DOMAIN_ALTER.equals(domain))
			tableName = ALTER_ATTRIBS_CHOICES_TABLE_NAME;
		if(PersonalNetwork.DOMAIN_EGO_ALTER.equals(domain))
			tableName = EGO_ALTER_ATTRIBS_CHOICES_TABLE_NAME;
		if(PersonalNetwork.DOMAIN_ALTER_ALTER.equals(domain))
			tableName = ALTER_ALTER_ATTRIBS_CHOICES_TABLE_NAME;
		return tableName;
	}
	
	/**
	 * Returns the lifetime of the element specified by the selection strings.
	 */
	private Lifetime getLifetime(String historyTableName, String elementSelection,
			String[] elementSelectionArgs) {
		Cursor cursor = db.query(historyTableName, 
				new String[]{COL_TIME_START, COL_TIME_END}, 
				elementSelection, elementSelectionArgs, null, null, null);
		Lifetime lifetime = new Lifetime();
		if(cursor == null)
			return lifetime;
		int start = cursor.getColumnIndexOrThrow(COL_TIME_START);
		int end = cursor.getColumnIndexOrThrow(COL_TIME_END);
		if(cursor.moveToFirst()){
			while(!cursor.isAfterLast()){
				lifetime.union(new TimeInterval(cursor.getLong(start), cursor.getLong(end)));
				cursor.moveToNext();
			}
		}
		cursor.close();
		return lifetime;
	}

	/**
	 * Makes the lifetime to be equal to the old lifetime SETMINUS interval.
	 * 
	 * If interval is a time point then either
	 * (1) the lifetime contains a time-interval equal to this point which is then removed or
	 * (2) nothing is done (even if the lifetime contains an interval that is not a time point and contains
	 * the point to be removed). 
	 * 
	 * The content values must have set all columns that identify the element so that
	 * new rows can be inserted after the start and end times have been set appropriately.
	 *
	 */
	private void cutOutOfLifetime(TimeInterval interval,
			String historyTableName, String elementSelection,
			String[] elementSelectionArgs, ContentValues values) {
		long cutStartTime = interval.getStartTime();
		long cutEndTime = interval.getEndTime();
		String currentTimeStamp = Long.toString(System.currentTimeMillis()); //used as time stamps for newly created intervals
		//if interval is a time point, then remove it (if present) and don't do anything else
		if(cutStartTime == cutEndTime){
			String removeTimePointSelection = elementSelection + " AND " + COL_TIME_START +
					" = ? AND " + COL_TIME_END + " = ?";
			String[] removeTimePointSelectionArgs = new String[elementSelectionArgs.length+2];
			for(int i = 0; i < elementSelectionArgs.length; ++i){
				removeTimePointSelectionArgs[i] = elementSelectionArgs[i];
			}
			removeTimePointSelectionArgs[elementSelectionArgs.length] = Long.toString(cutStartTime);
			removeTimePointSelectionArgs[elementSelectionArgs.length+1] = Long.toString(cutEndTime);
			db.delete(historyTableName, removeTimePointSelection, removeTimePointSelectionArgs);
			return;
		}
		//determine lifetime intervals potentially overlapping the given interval
		String[] selectionArgs = new String[elementSelectionArgs.length+2];
		String selection = elementSelection + " AND " + COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ? ";
		for(int i = 0; i < elementSelectionArgs.length; ++i){
			selectionArgs[i] = elementSelectionArgs[i];
		}
		selectionArgs[elementSelectionArgs.length] = Long.toString(cutEndTime);
		selectionArgs[elementSelectionArgs.length+1] = Long.toString(cutStartTime);
		Cursor cursor = db.query(historyTableName, 
				new String[]{COL_TIME_START, COL_TIME_END, COL_DATUM_ID}, 
				selection, selectionArgs, null, null, null);
		if(cursor.moveToFirst()){			
			while(!cursor.isAfterLast()){//iterate over all intervals that potentially overlap the cut interval
				long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_START));
				long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_END));
				if(overlap(startTime, endTime, cutStartTime, cutEndTime)){
					//it overlaps the cut interval
					String oldIntervalDatumID = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATUM_ID));
					String oldTimeStampStart = getSecondaryAttributeValue(oldIntervalDatumID, 
							getSecondaryAttributeNameTimestampStart());
					String oldTimeStampEnd = getSecondaryAttributeValue(oldIntervalDatumID, 
							getSecondaryAttributeNameTimestampEnd());
					//delete that interval
					selection = elementSelection + " AND " + COL_TIME_START + " = ? AND " +
							COL_TIME_END + " = ? ";
					selectionArgs[elementSelectionArgs.length] = Long.toString(startTime);
					selectionArgs[elementSelectionArgs.length+1] = Long.toString(endTime);
					db.delete(historyTableName, selection, selectionArgs);
					//TODO: delete associated secondary attributes!!!
					if(startTime < cutStartTime){//something remains on the left-hand side
						values.put(COL_TIME_START, startTime);
						values.put(COL_TIME_END, cutStartTime);
						String newIntervalDatumID = getNextDatumIDAndIncrement();
						values.put(COL_DATUM_ID, newIntervalDatumID);
						db.insert(historyTableName, null, values);
						setSecondaryAttributeValue(newIntervalDatumID, 
								getSecondaryAttributeNameTimestampStart(), oldTimeStampStart);//that's the old setting
						setSecondaryAttributeValue(newIntervalDatumID, 
								getSecondaryAttributeNameTimestampEnd(), currentTimeStamp);//that's a new setting
					}
					if(cutEndTime < endTime){//something remains on the right-hand side
						values.put(COL_TIME_START, cutEndTime);
						values.put(COL_TIME_END, endTime);
						String newIntervalDatumID = getNextDatumIDAndIncrement();
						values.put(COL_DATUM_ID, newIntervalDatumID);
						db.insert(historyTableName, null, values);
						setSecondaryAttributeValue(newIntervalDatumID, 
								getSecondaryAttributeNameTimestampStart(), currentTimeStamp);//that's a new setting
						setSecondaryAttributeValue(newIntervalDatumID, 
								getSecondaryAttributeNameTimestampEnd(), oldTimeStampEnd);//that's the old setting
					}
				}
				cursor.moveToNext();
			}
		}
		cursor.close();
	}

	/**
	 * Sets the lifetime of the specified element (by the selection clause) in the given table
	 * to the union of its previous lifetime with the given interval.
	 * 
	 * The content values must have set all columns that identify the element.
	 *
	 */
	private void unionWithLifetime(TimeInterval interval,
			String historyTableName, String elementSelection,
			String[] elementSelectionArgs, ContentValues values) {
		long newStartTime = interval.getStartTime();
		long newEndTime = interval.getEndTime();
		String timeStampStart = null;
		String timeStampEnd = null;
		String currentTimeStamp = Long.toString(System.currentTimeMillis());
		//determine overlapping intervals (if any)
		String[] selectionArgs = new String[elementSelectionArgs.length+2];
		String selection = elementSelection + " AND " + COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ? ";
		for(int i = 0; i < elementSelectionArgs.length; ++i){
			selectionArgs[i] = elementSelectionArgs[i];
		}
		selectionArgs[elementSelectionArgs.length] = Long.toString(interval.getEndTime());
		selectionArgs[elementSelectionArgs.length+1] = Long.toString(interval.getStartTime());
		Cursor cursor = db.query(historyTableName, 
				new String[]{COL_TIME_START, COL_TIME_END, COL_DATUM_ID}, 
				selection, selectionArgs, null, null, null);
		if(cursor.moveToFirst()){
			while(!cursor.isAfterLast()){
				long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_START));
				long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_END));
				if(overlapOrAreContiguous(startTime, endTime, newStartTime, newEndTime)){
					//it overlaps the new interval
					String oldIntervalDatumID = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATUM_ID));
					String tmpTimeStampStart = getSecondaryAttributeValue(oldIntervalDatumID, 
							getSecondaryAttributeNameTimestampStart());
					String tmpTimeStampEnd = getSecondaryAttributeValue(oldIntervalDatumID, 
							getSecondaryAttributeNameTimestampEnd());
					//delete the old interval
					selection = elementSelection + " AND " + COL_TIME_START + " = ? AND " +
							COL_TIME_END + " = ? ";
					selectionArgs[elementSelectionArgs.length] = Long.toString(startTime);
					selectionArgs[elementSelectionArgs.length+1] = Long.toString(endTime);
					db.delete(historyTableName, selection, selectionArgs);
					//TODO: delete associated secondary attributes!!!
					if(startTime < newStartTime){
						newStartTime = startTime;
						timeStampStart = tmpTimeStampStart;
					}
					if(endTime > newEndTime){
						newEndTime = endTime;
						timeStampEnd = tmpTimeStampEnd;
					}
				}
				cursor.moveToNext();
			}
		}
		cursor.close();
		//add the new interval respectively its union with the older intervals
		if(timeStampStart == null)
			timeStampStart = currentTimeStamp;
		if(timeStampEnd == null)
			timeStampEnd = currentTimeStamp;
		values.put(COL_TIME_START, newStartTime);
		values.put(COL_TIME_END, newEndTime);
		String newDatumID = getNextDatumIDAndIncrement();
		values.put(COL_DATUM_ID, newDatumID);
		db.insert(historyTableName, null, values);
		setSecondaryAttributeValue(newDatumID, getSecondaryAttributeNameTimestampStart(), timeStampStart);
		setSecondaryAttributeValue(newDatumID, getSecondaryAttributeNameTimestampEnd(), timeStampEnd);
	}

	/**
	 *Returns true if and only if the given interval overlaps the lifetime of the element specified
	 *by the selection strings.
	 * 
	 */
	private boolean overlapsLifetime(TimeInterval interval,
			String historyTableName, String elementSelection,
			String[] elementSelectionArgs) {
		String selection = elementSelection + " AND " + COL_TIME_START + " < ?";
		String[] selectionArgs = new String[elementSelectionArgs.length+1];
		for(int i = 0; i < elementSelectionArgs.length; ++i){
			selectionArgs[i] = elementSelectionArgs[i];
		}
		selectionArgs[elementSelectionArgs.length] = Long.toString(interval.getEndTime());
		String orderBy = COL_TIME_START + " DESC";
		String limit = "1";
		Cursor cursor = db.query(historyTableName, new String[]{COL_TIME_START, COL_TIME_END}, 
				selection, selectionArgs, null, null, orderBy, limit);
		if(cursor.moveToFirst()){
			// there is an interval below
			long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_START));
			long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_END));
			if(overlap(startTime, endTime, interval.getStartTime(), interval.getEndTime())){
				cursor.close();
				return true;
			}
		}
		cursor.close();
		//determine higher interval (if any)
		selection = elementSelection + " AND " + COL_TIME_START + " > ?";
		selectionArgs[elementSelectionArgs.length] = Long.toString(interval.getStartTime());
		orderBy = COL_TIME_START + " ASC";
		cursor = db.query(historyTableName, new String[]{COL_TIME_START, COL_TIME_END}, 
				selection, selectionArgs, null, null, orderBy, limit);
		if(cursor.moveToFirst()){
			long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_START));
			long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_END));
			if(overlap(startTime, endTime, interval.getStartTime(), interval.getEndTime())){
				cursor.close();
				return true;
			}
		}
		cursor.close();
		return false;
	}

	/**
	 *Returns the datum id of the time interval of the lifetime of the element specified
	 *by the selection strings, if the lifetime contains the given time point. 
	 *Returns null, else.
	 * 
	 */
	private String getLifetimeDatumIDAt(long timePoint,
			String historyTableName, String elementSelection,
			String[] elementSelectionArgs) {
		String selection = elementSelection + " AND " + COL_TIME_START + " <= ?";
		String[] selectionArgs = new String[elementSelectionArgs.length+1];
		for(int i = 0; i < elementSelectionArgs.length; ++i){
			selectionArgs[i] = elementSelectionArgs[i];
		}
		selectionArgs[elementSelectionArgs.length] = Long.toString(timePoint);
		String orderBy = COL_TIME_START + " DESC";
		String limit = "1";
		Cursor cursor = db.query(historyTableName, 
				new String[]{COL_TIME_START, COL_TIME_END, COL_DATUM_ID}, 
				selection, selectionArgs, null, null, orderBy, limit);
		if(cursor.moveToFirst()){
			// there is an interval below
			long startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_START));
			long endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_END));
			if(contains(startTime, endTime, timePoint)){
				String id = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATUM_ID));
				cursor.close();
				return id;
			}
		}
		cursor.close();
		return null;
	}

	/**
	 * Returns true if the two intervals defined by their start and end times overlap.
	 * 
	 * Same behavior as TimeInterval.overlaps(anotherInterval).
	 * 
	 */
	private boolean overlap(long start1, long end1, long start2, long end2){
		if(start1 == end1 && start2 == end2)
			return start1 == start2;
		if(start1 == end1)
			return start1 < end2 && start2 <= end1;
		if(start2 == end2)
			return start1 <= end2 && start2 < end1;
		return start1 < end2 && start2 < end1;
	}

	/**
	 * Returns true if the two intervals defined by their start and end times overlap or are contiguous.
	 * 
	 * Same behavior as TimeInterval.overlapsOrIsContiguous(anotherInterval).
	 * 
	 */
	private boolean overlapOrAreContiguous(long start1, long end1, long start2, long end2){
		return start1 <= end2 && start2 <= end1;
	}

	/**
	 * Returns true if the interval defined by its start and end times contains the given time point.
	 */
	private boolean contains(long intervalStart, long intervalEnd, long timePoint){
		if(intervalStart == intervalEnd)
			return intervalStart == timePoint;
		return intervalStart <= timePoint && timePoint < intervalEnd;
	}

	/**
	 * Sets the given value for the given time interval to textValue.trim()
	 * for the attribute/element pair which is defined by the tablename and selection strings.
	 * 
	 * Overwrites any previous values within interval (keeps all values outside of interval).
	 * 
	 * If textValue is null or has length zero or is equal to PersonalNetwork.VALUE_NOT_ASSIGNED
	 * then previous values within the interval are removed but no new value is set.
	 * 
	 * Does nothing if interval is a time point that is included in a previously set interval 
	 * which is not a point. Also see the behavior of Lifetime.cutOut for this case.

	 * If interval is a time point then either
	 * (1) the current support of this attribute contains a time-interval equal to this point 
	 *     whose value is then overwritten or
	 * (2) nothing is done (even if the support contains an interval that is not a time point and contains
	 * the point whose value has to be overwritten). 
	 * 
	 * The content values must have set all columns that identify the attribute/element pair so that
	 * new rows can be inserted after the start and end times have been set appropriately.
	 *
	 */
	private void setAttributeValueAt(TimeInterval interval, String textValue,
			String valueHistoryTableName, String elementSelection,
			String[] elementSelectionArgs, ContentValues dbContentValues) {
		long newStartTime = interval.getStartTime();
		long newEndTime = interval.getEndTime();
		String currentTimeStamp = Long.toString(System.currentTimeMillis()); //used as time stamps for newly created interval times
		String newTimeStampStart = currentTimeStamp;//might be set to an older timestamp if... 
		//...the new value is equal to an old value of an overlaping interval										
		String newTimeStampEnd = currentTimeStamp;//dito
		//determine overlapping intervals (if any)
		String[] selectionArgs = new String[elementSelectionArgs.length+2];
		String selection = elementSelection + " AND " + COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ? ";
		for(int i = 0; i < elementSelectionArgs.length; ++i){
			selectionArgs[i] = elementSelectionArgs[i];
		}
		selectionArgs[elementSelectionArgs.length] = Long.toString(newEndTime);
		selectionArgs[elementSelectionArgs.length+1] = Long.toString(newStartTime);
		Cursor cursor = db.query(valueHistoryTableName, 
				new String[]{COL_TIME_START, COL_TIME_END, 
				COL_DATUM_ID,
				ATTRIBUTES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		if(cursor.moveToFirst()){			
			while(!cursor.isAfterLast()){
				long oldStartTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_START));
				long oldEndTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_END));
				if(overlap(oldStartTime, oldEndTime, newStartTime, newEndTime)){
					//floor overlaps the new interval
					if(newStartTime == newEndTime && oldStartTime < oldEndTime){
						//the new interval is a time point which is included in an interval that is 
						//not a point ==> do nothing since otherwise we would end up with a left-open interval
						return;
					}
					String oldValue = cursor.getString(cursor.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE));
					String oldDatumID = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATUM_ID));
					String oldTimeStampStart = getSecondaryAttributeValue(oldDatumID, 
							getSecondaryAttributeNameTimestampStart());
					String oldTimeStampEnd = getSecondaryAttributeValue(oldDatumID, 
							getSecondaryAttributeNameTimestampEnd());
					selection = elementSelection + " AND " + COL_TIME_START + " = ? AND " +
							COL_TIME_END + " = ? ";
					selectionArgs[elementSelectionArgs.length] = Long.toString(oldStartTime);
					selectionArgs[elementSelectionArgs.length+1] = Long.toString(oldEndTime);
					db.delete(valueHistoryTableName, selection, selectionArgs);//delete the old interval
					//TODO: delete associated secondary attributes!!!
					if(oldStartTime < newStartTime){//something remains on the left-hand side
						if(!oldValue.equals(textValue)){
							dbContentValues.put(COL_TIME_START, oldStartTime);
							dbContentValues.put(COL_TIME_END, newStartTime);
							dbContentValues.put(ATTRIBUTES_COL_VALUE, oldValue);
							String newDatumID = getNextDatumIDAndIncrement();
							dbContentValues.put(COL_DATUM_ID, newDatumID);
							db.insert(valueHistoryTableName, null, dbContentValues);
							setSecondaryAttributeValue(newDatumID, 
									getSecondaryAttributeNameTimestampStart(), oldTimeStampStart);//that's the old setting
							setSecondaryAttributeValue(newDatumID, 
									getSecondaryAttributeNameTimestampEnd(), currentTimeStamp);//that's a new setting
						} else {//equal values ==> union the intervals (will be inserted later)
							newStartTime = oldStartTime;
							newTimeStampStart = oldTimeStampStart;
						}
					}
					if(newEndTime < oldEndTime){//something remains on the right-hand side
						if(!oldValue.equals(textValue)){
							dbContentValues.put(COL_TIME_START, newEndTime);
							dbContentValues.put(COL_TIME_END, oldEndTime);
							dbContentValues.put(ATTRIBUTES_COL_VALUE, oldValue);
							String newDatumID = getNextDatumIDAndIncrement();
							dbContentValues.put(COL_DATUM_ID, newDatumID);
							db.insert(valueHistoryTableName, null, dbContentValues);
							setSecondaryAttributeValue(newDatumID, 
									getSecondaryAttributeNameTimestampStart(), 
									currentTimeStamp);//that's a new setting
							setSecondaryAttributeValue(newDatumID, 
									getSecondaryAttributeNameTimestampEnd(), 
									oldTimeStampEnd);//that's the old setting
						} else {//equal values ==> union the intervals (will be inserted later)
							newEndTime = oldEndTime;
							newTimeStampEnd = oldTimeStampEnd;
						}
					}
				}
				cursor.moveToNext();
			}
		}
		cursor.close();
		//set the new value for the given interval
		if(textValue != null && textValue.trim().length() > 0 && !textValue.equals(PersonalNetwork.VALUE_NOT_ASSIGNED)){
			dbContentValues.put(COL_TIME_START, newStartTime);
			dbContentValues.put(COL_TIME_END, newEndTime);
			dbContentValues.put(ATTRIBUTES_COL_VALUE, textValue);
			String newDatumID = getNextDatumIDAndIncrement();
			dbContentValues.put(COL_DATUM_ID, newDatumID);
			db.insert(valueHistoryTableName, null, dbContentValues);
			setSecondaryAttributeValue(newDatumID, 
					getSecondaryAttributeNameTimestampStart(), 
					newTimeStampStart);
			setSecondaryAttributeValue(newDatumID, 
					getSecondaryAttributeNameTimestampEnd(), 
					newTimeStampEnd);
		}
	}

	/**
	 * Returns the value of all attributes for the element specified by the table name and 
	 * selection string at the given time point.
	 * 
	 * Only attributes whose values are set are included in the map
	 * 
	 * @return map from attribute names to values.
	 */
	private LinkedHashMap<String, String> getValuesOfAllAttributesAt(long timePoint, String valueHistoryTableName, 
			String elementSelection, String[] elementSelectionArgs){
		String[] selectionArgs = new String[elementSelectionArgs.length+2];
		String selection;
		if(elementSelectionArgs.length > 0)
			selection = elementSelection + " AND " + COL_TIME_START + " <= ? AND " + COL_TIME_END + " >= ?";
		else //happens for the element ego
			selection = COL_TIME_START + " <= ? AND " + COL_TIME_END + " >= ?";
		for(int i = 0; i < elementSelectionArgs.length; ++i){
			selectionArgs[i] = elementSelectionArgs[i];
		}
		selectionArgs[elementSelectionArgs.length] = Long.toString(timePoint);
		selectionArgs[elementSelectionArgs.length+1] = Long.toString(timePoint);
		String orderBy = COL_TIME_START + " DESC";
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		Cursor c = db.query(valueHistoryTableName, 
				new String[]{ATTRIBUTES_COL_NAME, COL_TIME_START, COL_TIME_END, ATTRIBUTES_COL_VALUE}, 
				selection, selectionArgs, null, null, orderBy);
		if(c.moveToFirst()){			
			while(!c.isAfterLast()){
				long intervalStart = c.getLong(c.getColumnIndexOrThrow(COL_TIME_START));
				long intervalEnd = c.getLong(c.getColumnIndexOrThrow(COL_TIME_END));
				if(contains(intervalStart, intervalEnd, timePoint)){
					String name = c.getString(c.getColumnIndexOrThrow(ATTRIBUTES_COL_NAME));
					String value = c.getString(c.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE));
					map.put(name, value);
				}
				c.moveToNext();
			}
		}
		c.close();
		return map;
	}

	/**
	 * Returns the value of the attribute specified by the table name and 
	 * selection string at the given time point or PersonalNetwork.VALUE_NOT_ASSIGNED if
	 * the lifetime of that attribute does not contain the given time point.
	 */
	private String getAttributeValueAt(long timePoint, String valueHistoryTableName, 
			String elementSelection, String[] elementSelectionArgs){
		String[] selectionArgs = new String[elementSelectionArgs.length+1];
		String selection = elementSelection + " AND " + COL_TIME_START + " <= ?";
		for(int i = 0; i < elementSelectionArgs.length; ++i){
			selectionArgs[i] = elementSelectionArgs[i];
		}
		selectionArgs[elementSelectionArgs.length] = Long.toString(timePoint);
		String orderBy = COL_TIME_START + " DESC";
		String limit = "1";
		Cursor cursor = db.query(valueHistoryTableName, 
				new String[]{COL_TIME_START, COL_TIME_END, ATTRIBUTES_COL_VALUE}, 
				selection, selectionArgs, null, null, orderBy, limit);
		if(cursor.moveToFirst()){			
			long intervalStart = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_START));
			long intervalEnd = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_END));
			if(contains(intervalStart, intervalEnd, timePoint)){
				String ret = cursor.getString(cursor.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE));
				cursor.close();
				return ret;
			}
		}
		cursor.close();
		return PersonalNetwork.VALUE_NOT_ASSIGNED;
	}

	/**
	 * Returns the datum id of the attribute specified by the table name and 
	 * selection string at the given time point or N/A if
	 * the support of that attribute for that element does not contain the given time point.
	 */
	private String getAttributeDatumIDAt(long timePoint, String valueHistoryTableName, 
			String elementSelection, String[] elementSelectionArgs){
		String[] selectionArgs = new String[elementSelectionArgs.length+1];
		String selection = elementSelection + " AND " + COL_TIME_START + " <= ?";
		for(int i = 0; i < elementSelectionArgs.length; ++i){
			selectionArgs[i] = elementSelectionArgs[i];
		}
		selectionArgs[elementSelectionArgs.length] = Long.toString(timePoint);
		String orderBy = COL_TIME_START + " DESC";
		String limit = "1";
		Cursor cursor = db.query(valueHistoryTableName, 
				new String[]{COL_TIME_START, COL_TIME_END, COL_DATUM_ID}, 
				selection, selectionArgs, null, null, orderBy, limit);
		if(cursor.moveToFirst()){			
			long intervalStart = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_START));
			long intervalEnd = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_END));
			if(contains(intervalStart, intervalEnd, timePoint)){
				String ret = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATUM_ID));
				cursor.close();
				return ret;
			}
		}
		cursor.close();
		return PersonalNetwork.VALUE_NOT_ASSIGNED;
	}

	/**
	 * Returns the limit-many most recent (ordered by the start time)
	 * time varying attribute values for the attribute / element specified by the selection strings.
	 */
	private TimeVaryingAttributeValues getRecentAttributeValues(String valueHistoryTableName, String elementSelection,
			String[] elementSelectionArgs, int limit) {
		if(limit < 1)
			limit = 1;
		String limitStr = Integer.toString(limit);
		Cursor c = db.query(valueHistoryTableName, 
				new String[]{COL_TIME_START, COL_TIME_END, ATTRIBUTES_COL_VALUE}, 
				elementSelection, elementSelectionArgs, null, null, 
				COL_TIME_START +" DESC", limitStr);
		int start_idx = c.getColumnIndexOrThrow(COL_TIME_START);
		int end_idx = c.getColumnIndexOrThrow(COL_TIME_END);
		int value_idx = c.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
		TimeVaryingAttributeValues values = new TimeVaryingAttributeValues();
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				values.setValueAt(new TimeInterval(c.getLong(start_idx), c.getLong(end_idx)),
						c.getString(value_idx));
				c.moveToNext();
			}
		}
		c.close();
		return values;
	}
	
	/**
	 * Returns the time varying attribute values for the attribute / element specified by the selection strings.
	 */
	private TimeVaryingAttributeValues getAttributeValues(String valueHistoryTableName, String elementSelection,
			String[] elementSelectionArgs) {
		Cursor c = db.query(valueHistoryTableName, 
				new String[]{COL_TIME_START, COL_TIME_END, ATTRIBUTES_COL_VALUE}, 
				elementSelection, elementSelectionArgs, null, null, null);
		int start_idx = c.getColumnIndexOrThrow(COL_TIME_START);
		int end_idx = c.getColumnIndexOrThrow(COL_TIME_END);
		int value_idx = c.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
		TimeVaryingAttributeValues values = new TimeVaryingAttributeValues();
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				values.setValueAt(new TimeInterval(c.getLong(start_idx), c.getLong(end_idx)),
						c.getString(value_idx));
				c.moveToNext();
			}
		}
		c.close();
		return values;
	}
	
	/**
	 * Returns a map from attribute names to the time varying attribute values for the element 
	 * specified by the selection strings.
	 */
	private LinkedHashMap<String, TimeVaryingAttributeValues> getValuesOfAllAttributes(
			String valueHistoryTableName, 
			String elementSelection,
			String[] elementSelectionArgs) {
		Cursor c = db.query(valueHistoryTableName, 
				new String[]{ATTRIBUTES_COL_NAME, COL_TIME_START, COL_TIME_END, ATTRIBUTES_COL_VALUE}, 
				elementSelection, elementSelectionArgs, null, null, null);
		int start_idx = c.getColumnIndexOrThrow(COL_TIME_START);
		int end_idx = c.getColumnIndexOrThrow(COL_TIME_END);
		int value_idx = c.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE);
		int name_idx = c.getColumnIndexOrThrow(ATTRIBUTES_COL_NAME);
		LinkedHashMap<String,TimeVaryingAttributeValues> map = 
				new LinkedHashMap<String, TimeVaryingAttributeValues>();
		if(c.moveToFirst()){
			while(!c.isAfterLast()){
				String attrName = c.getString(name_idx);
				TimeVaryingAttributeValues values = map.get(attrName);
				if(values == null){
					values = new TimeVaryingAttributeValues();
					map.put(attrName, values);
				}
				values.setValueAt(new TimeInterval(c.getLong(start_idx), c.getLong(end_idx)),
						c.getString(value_idx));
				c.moveToNext();
			}
		}
		c.close();
		return map;
	}
	
	/**
	 * Returns the set of attribute values that are assigned to the element (specified by the 
	 * selection strings) at some point in time in the given interval.
	 */
	private LinkedHashSet<String> getUniqueValuesForAttributeAt(TimeInterval interval, 
			String valueHistoryTableName, String elementSelection,
			String[] elementSelectionArgs) {
		long startTime = interval.getStartTime();
		long endTime = interval.getEndTime();
		//determine overlapping intervals (if any)
		String[] selectionArgs = new String[elementSelectionArgs.length+2];
		String selection = elementSelection + " AND " + COL_TIME_START + " <= ? AND " +
				COL_TIME_END + " >= ? ";
		for(int i = 0; i < elementSelectionArgs.length; ++i){
			selectionArgs[i] = elementSelectionArgs[i];
		}
		selectionArgs[elementSelectionArgs.length] = Long.toString(endTime);
		selectionArgs[elementSelectionArgs.length+1] = Long.toString(startTime);
		Cursor cursor = db.query(valueHistoryTableName, 
				new String[]{COL_TIME_START, COL_TIME_END, 
				ATTRIBUTES_COL_VALUE}, 
				selection, selectionArgs, null, null, null);
		LinkedHashSet<String> values = new LinkedHashSet<String>();
		if(cursor.moveToFirst()){			
			while(!cursor.isAfterLast()){
				long valueStartTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_START));
				long valueEndTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME_END));
				if(overlap(valueStartTime, valueEndTime, startTime, endTime)){
					values.add(cursor.getString(cursor.getColumnIndexOrThrow(ATTRIBUTES_COL_VALUE)));
				}
				cursor.moveToNext();
			}
		}
		cursor.close();
		return values;
	}

	/**
	 * Sets the allowed values of the given attribute (in the given choices table) equal to the union of
	 * the given choices (which will be trimmed) with the given current values.
	 */
	private void setAttributeChoices(String attributeName, String tableName,
			LinkedHashSet<String> choices, LinkedHashSet<String> currentValues) {
		db.beginTransaction();
		try{
			String selection = ATTRIBUTES_COL_NAME + " = ?";
			String[] selectionArgs = {attributeName};
			// remove all choices set so far for this attribute
			db.delete(tableName, selection, selectionArgs);
			for(String choice : choices){
				if(choice == null)
					choices.remove(choice);
				else{
					String choice_trimmed = choice.trim();
					if(!choice.equals(choice_trimmed)){
						choices.remove(choice);
						if(choice_trimmed.length() > 0)
							choices.add(choice_trimmed);
					}
				}
			}
			choices.addAll(currentValues);
			for(String choice : choices){
				ContentValues values = new ContentValues();
				values.put(ATTRIBUTES_COL_NAME, attributeName);
				values.put(ATTRIBUTES_COL_CHOICE, choice);
				db.insert(tableName, null, values);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	public boolean allValuesAreNumbers(Collection<String> values){
		for(String value : values){
			try {
				Double.parseDouble(value);
			} catch(Exception e){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Sets the current loaded study file. It's used in loading interviews associated with this study.
	 * @param study
	 */
	public void setStudyFile(EgonetQuestionnaireFile study) {
		this.studyFile = study;
	}
	
	/**
	 * Egonet interview has been loaded correctly?
	 * @return
	 */
	public boolean isInterviewLoaded() {
		return interviewLoaded;
	}
}
