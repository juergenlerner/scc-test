/**
 * 
 */
package net.egosmart.scc.data;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Class representing an Alter identified by its name.
 * 
 * @author juergen
 *
 */
public class Alter extends Element {
	
	private String name;
	
	private Alter(String name){
		this.name = name;
	}
	
	/**
	 * Returns an instance of Alter identified by its name. 
	 * 
	 * Name is trimmed. 
	 * 
	 * @throws IllegalArgumentException if {@code name} is null or {@code name.trim()} 
	 * has length zero.
	 * 
	 * @return
	 */
	public static Alter getInstance(String name){
		if(name == null || name.trim().length() == 0)
			throw new IllegalArgumentException("alter name must not be null nor of length zero");
		name = name.trim();
		return new Alter(name);
	}

	/** 
	 * Returns {@link PersonalNetwork#DOMAIN_ALTER}
	 * 	 
	 */
	@Override
	public String getDomain() {
		return PersonalNetwork.DOMAIN_ALTER;
	}

	@Override
	public boolean isDyadicElement() {
		return false;
	}

	@Override
	public Element getReverseElement() {
		return new Alter(name);
	}

	/** 
	 * Implements the default comparator used for ordering elements. 
	 * 
	 * Returns a negative integer if this element is less than another, 0 if they are equal
	 * and a positive integer if this element is greater than another. 
	 * 
	 * The ordering of elements from the same domain is defined by lexicographic alphabetic ordering.
	 * For two elements from different domains it is Ego < Alter < EgoAlter < AlterAlter.
	 * 
	 * Alters are compared by name.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Element another) {
		if(!(another instanceof Alter)){
			if(another instanceof Ego)
				return 1;
			else
				return -1; //another is an ego-alter dyad or an alter-alter dyad
		}
		Alter o = (Alter) another;
		return name.compareTo(o.name);
	}

	/**
	 * Returns true if another is an instance of Alter with the same name; false otherwise.
	 */
	@Override
	public boolean equals(Object another) {
		if(!(another instanceof Alter))
			return false;
		return name.equals(((Alter) another).name); 
	}

	/** 
	 * Returns the hash code of the alter name.
	 */
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	/**
	 * 
	 * @return the alter name
	 */
	public String getName(){
		return name;
	}

	@Override
	protected String getElementSelectionString() {
		return PersonalNetwork.ALTERS_COL_NAME + " = ? ";
	}

	@Override
	protected String[] getElementSelectionArgs() {
		return new String[]{name};
	}

	@Override
	protected String getAttributeElementSelectionString() {
		return PersonalNetwork.ATTRIBUTES_COL_NAME + " = ? AND " + 
				PersonalNetwork.ALTERS_COL_NAME + " = ? ";
	}

	@Override
	protected String[] getAttributeElementSelectionArgs(String attributeName) {
		return new String[]{attributeName, name};
	}

	@Override
	protected ContentValues getAttributeElementContentValues(
			String attributeName) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(PersonalNetwork.ATTRIBUTES_COL_NAME, attributeName);
		contentValues.put(PersonalNetwork.ALTERS_COL_NAME, name);
		return contentValues;
	}

	@Override
	protected Alter getInstanceFromCursor(Cursor c) {
		return getInstance(c.getString(c.getColumnIndexOrThrow(PersonalNetwork.ALTERS_COL_NAME)));
	}

	@Override
	protected String[] getElementColumnNames() {
		return new String[]{PersonalNetwork.ALTERS_COL_NAME};
	}

}
