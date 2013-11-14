/**
 * 
 */
package net.egosmart.scc.data;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Class representing a dyad connecting Ego with Alter identified by the
 * alter name and the direction which is either OUT (direction from ego to alter)
 * or IN (direction from alter to ego).
 * 
 * @author juergen
 *
 */
public class EgoAlterDyad extends Element {
	
	private String alterName;
	
	private String direction;
	
	private EgoAlterDyad(String alterName, String direction){
		this.alterName = alterName;
		this.direction = direction;
	}
	
	public static EgoAlterDyad getInstance(String alterName, String direction){
		if(alterName == null || alterName.trim().length() == 0)
			throw new IllegalArgumentException("alter name must not be null nor of length zero");
		alterName = alterName.trim();
		if(!PersonalNetwork.DYAD_DIRECTION_OUT.equals(direction) &&
				!PersonalNetwork.DYAD_DIRECTION_IN.equals(direction))
			throw new IllegalArgumentException("illegal direction: " + direction);
		return new EgoAlterDyad(alterName, direction);
	}
	
	/**
	 * Returns an instance of an EgoAlterDyad connecting ego to an alter (identified by its name)
	 * in the direction from ego to alter. 
	 * 
	 * Name is trimmed. 
	 * 
	 * @throws IllegalArgumentException if the alter name is null or the trimmed alter name 
	 * has length zero.
	 * 
	 * @return
	 */
	public static EgoAlterDyad getOutwardInstance(String alterName){
		if(alterName == null || alterName.trim().length() == 0)
			throw new IllegalArgumentException("alter name must not be null nor of length zero");
		alterName = alterName.trim();
		return new EgoAlterDyad(alterName, PersonalNetwork.DYAD_DIRECTION_OUT);
	}

	/**
	 * Returns an instance of an EgoAlterDyad connecting alter (identified by its name) to ego
	 * in the direction from alter to ego. 
	 * 
	 * Name is trimmed. 
	 * 
	 * @throws IllegalArgumentException if the alter name is null or the trimmed alter name 
	 * has length zero.
	 * 
	 * @return
	 */
	public static EgoAlterDyad getInwardInstance(String alterName){
		if(alterName == null || alterName.trim().length() == 0)
			throw new IllegalArgumentException("alter name must not be null nor of length zero");
		alterName = alterName.trim();
		return new EgoAlterDyad(alterName, PersonalNetwork.DYAD_DIRECTION_IN);
	}

	/** 
	 * Returns {@link PersonalNetwork#DOMAIN_EGO_ALTER}
	 * 	 
	 */
	@Override
	public String getDomain() {
		return PersonalNetwork.DOMAIN_EGO_ALTER;
	}

	@Override
	public boolean isDyadicElement() {
		return true;
	}

	@Override
	public Element getReverseElement() {
		String reverseDirection = PersonalNetwork.DYAD_DIRECTION_OUT;
		if(PersonalNetwork.DYAD_DIRECTION_OUT.equals(direction))
			reverseDirection = PersonalNetwork.DYAD_DIRECTION_IN;
		return new EgoAlterDyad(alterName, reverseDirection);
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
	 * The ego alter dyad is first compared by the alter name; if these are equal then the out
	 * direction is less than the in direction.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Element another) {
		if(!(another instanceof EgoAlterDyad)){
			if(another instanceof AlterAlterDyad)
				return -1;
			else
				return 1; //another is ego or an alter
		}
		EgoAlterDyad o = (EgoAlterDyad) another;
		if(!alterName.equals(o.alterName))
			return alterName.compareTo(o.alterName);
		if(direction.equals(o.direction))
			return 0;
		if(direction.equals(PersonalNetwork.DYAD_DIRECTION_OUT))
			return -1;
		else 
			return 1;
	}

	/**
	 * Returns true if another is an instance of EgoAlterDyad with the same
	 * name and direction; false otherwise.
	 */
	@Override
	public boolean equals(Object another) {
		if(!(another instanceof EgoAlterDyad))
			return false;
		EgoAlterDyad o = (EgoAlterDyad) another;
		return alterName.equals(o.alterName) && direction.equals(o.direction); 
	}

	/** 
	 * Returns the hash code of the alter name plus the hash code of the direction.
	 */
	@Override
	public int hashCode() {
		return alterName.hashCode() + direction.hashCode();
	}
	
	/**
	 * 
	 * @return the alter name
	 */
	public String getName(){
		return alterName;
	}

	/**
	 * 
	 * @return the direction of the dyad
	 */
	public String getDirection(){
		return direction;
	}

	@Override
	protected String getElementSelectionString() {
		return PersonalNetwork.ALTERS_COL_NAME + " = ? AND " +
				PersonalNetwork.ATTRIBUTES_COL_DIRECTION_TYPE + " = ? ";
	}

	@Override
	protected String[] getElementSelectionArgs() {
		return new String[]{alterName, direction};
	}

	@Override
	protected String getAttributeElementSelectionString() {
		return PersonalNetwork.ATTRIBUTES_COL_NAME + " = ? AND " + 
				PersonalNetwork.ALTERS_COL_NAME + " = ? AND " +
				PersonalNetwork.ATTRIBUTES_COL_DIRECTION_TYPE + " = ? ";
	}

	@Override
	protected String[] getAttributeElementSelectionArgs(String attributeName) {
		return new String[]{attributeName, alterName, direction};
	}

	@Override
	protected ContentValues getAttributeElementContentValues(
			String attributeName) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(PersonalNetwork.ATTRIBUTES_COL_NAME, attributeName);
		contentValues.put(PersonalNetwork.ALTERS_COL_NAME, alterName);
		contentValues.put(PersonalNetwork.ATTRIBUTES_COL_DIRECTION_TYPE, direction);
		return contentValues;
	}

	@Override
	protected String[] getElementColumnNames() {
		return new String[]{PersonalNetwork.ALTERS_COL_NAME, 
				PersonalNetwork.ATTRIBUTES_COL_DIRECTION_TYPE};
	}

	@Override
	protected EgoAlterDyad getInstanceFromCursor(Cursor c) {
		return new EgoAlterDyad(c.getString(c.getColumnIndexOrThrow(PersonalNetwork.ALTERS_COL_NAME)), 
				c.getString(c.getColumnIndexOrThrow(PersonalNetwork.ATTRIBUTES_COL_DIRECTION_TYPE)));
	}

}
