/**
 * 
 */
package net.egosmart.scc.data;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Class representing Ego. There is always at most one instance of it.
 * 
 * @author juergen
 *
 */
public class Ego extends Element {
	
	private static Ego instance;
	
	private Ego(){

	}
	
	/**
	 * Returns the single instance of Ego. 
	 * 
	 * @return
	 */
	public static Ego getInstance(){
		if(instance == null)
			instance = new Ego();
		return instance;
	}

	/** 
	 * Returns {@link PersonalNetwork#DOMAIN_EGO}
	 * 	 
	 */
	@Override
	public String getDomain() {
		return PersonalNetwork.DOMAIN_EGO;
	}

	@Override
	public boolean isDyadicElement() {
		return false;
	}

	@Override
	public Element getReverseElement() {
		return instance;
	}

	/** 
	 * Implements the default comparator used for ordering elements. 
	 * 
	 * The ordering of elements from the same domain is defined by lexicographic alphabetic ordering.
	 * For two elements from different domains it is Ego < Alter < EgoAlter < AlterAlter.
	 * 
	 * Returns a negative integer if this element is less than another, 0 if they are equal
	 * and a positive integer if this element is greater than another. 
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Element another) {
		if(!(another instanceof Ego))
			return -1; //Ego is considered to be less than any other Element
		return 0;// there is just one Ego and it is thus equal to this one
	}

	/**
	 * Returns true if another is an instance of Ego; false otherwise.
	 */
	@Override
	public boolean equals(Object another) {
		if(!(another instanceof Ego))
			return false;
		return true; //there is just one Ego and it is thus equal to this one
	}

	/** 
	 * Returns 11.	 
	 */
	@Override
	public int hashCode() {
		return 11;
	}

	@Override
	protected String getElementSelectionString() {
		return "";
	}

	@Override
	protected String[] getElementSelectionArgs() {
		return new String[0];
	}

	@Override
	protected String getAttributeElementSelectionString() {
		return PersonalNetwork.ATTRIBUTES_COL_NAME + " = ? ";
	}

	@Override
	protected String[] getAttributeElementSelectionArgs(String attributeName) {
		return new String[]{attributeName};
	}

	@Override
	protected ContentValues getAttributeElementContentValues(
			String attributeName) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(PersonalNetwork.ATTRIBUTES_COL_NAME, attributeName);
		return contentValues;
	}

	@Override
	protected String[] getElementColumnNames() {
		return new String[0];
	}

	@Override
	protected Ego getInstanceFromCursor(Cursor c) {
		return getInstance();
	}

}
