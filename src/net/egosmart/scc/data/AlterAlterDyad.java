/**
 * 
 */
package net.egosmart.scc.data;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Class representing a directed dyad connecting two Alters: the source with the target.
 * 
 * @author juergen
 *
 */
public class AlterAlterDyad extends Element {

	private String sourceName;
	private String targetName;

	private AlterAlterDyad(String sourceName, String targetName){
		this.sourceName = sourceName;
		this.targetName = targetName;
	}

	/**
	 * Returns an instance of an AlterAlterDyad. 
	 * 
	 * Names are trimmed. 
	 * 
	 * @throws IllegalArgumentException if one of the alters' names is null or the trimmed alter name 
	 * has length zero.
	 * 
	 * @return
	 */
	public static AlterAlterDyad getInstance(String sourceName, String targetName){
		if(sourceName == null || sourceName.trim().length() == 0)
			throw new IllegalArgumentException("alter name must not be null nor of length zero");
		sourceName = sourceName.trim();
		if(targetName == null || targetName.trim().length() == 0)
			throw new IllegalArgumentException("alter name must not be null nor of length zero");
		targetName = targetName.trim();
		return new AlterAlterDyad(sourceName, targetName);
	}

	/** 
	 * Returns {@link PersonalNetwork#DOMAIN_ALTER_ALTER}
	 * 	 
	 */
	@Override
	public String getDomain() {
		return PersonalNetwork.DOMAIN_ALTER_ALTER;
	}

	@Override
	public boolean isDyadicElement() {
		return true;
	}

	@Override
	public Element getReverseElement() {
		return new AlterAlterDyad(targetName, sourceName);
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
	 * The alter alter dyad is first compared by the source alter name; if these are equal then by
	 * the target alter name.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Element another) {
		if(!(another instanceof AlterAlterDyad)){
			if(PersonalNetwork.DOMAIN_EGO.equals(another.getDomain()) ||
					PersonalNetwork.DOMAIN_ALTER.equals(another.getDomain()) ||
					PersonalNetwork.DOMAIN_EGO_ALTER.equals(another.getDomain()))	
				return 1;
		}
		AlterAlterDyad o = (AlterAlterDyad) another;
		if(!sourceName.equals(o.sourceName))
			return sourceName.compareTo(o.sourceName);
		return targetName.compareTo(o.targetName);
	}

	/**
	 * Returns true if another is an instance of AlterAlterDyad with the same
	 * source and target; false otherwise.
	 */
	@Override
	public boolean equals(Object another) {
		if(!(another instanceof AlterAlterDyad))
			return false;
		AlterAlterDyad o = (AlterAlterDyad) another;
		return sourceName.equals(o.sourceName) && targetName.equals(o.targetName); 
	}

	/** 
	 * Returns the hash code of the source name plus three times the hash code of the target name
	 */
	@Override
	public int hashCode() {
		return sourceName.hashCode() + 3*targetName.hashCode(); // is asymmetric
	}

	/**
	 * 
	 * @return the source name
	 */
	public String getSourceName(){
		return sourceName;
	}

	/**
	 * 
	 * @return the target name
	 */
	public String getTargetName(){
		return targetName;
	}

	@Override
	protected String getElementSelectionString() {
		return PersonalNetwork.DYADS_COL_SOURCE + " = ? AND " +
				PersonalNetwork.DYADS_COL_TARGET + " = ? ";
	}

	@Override
	protected String[] getElementSelectionArgs() {
		return new String[]{sourceName, targetName};
	}

	@Override
	protected String getAttributeElementSelectionString() {
		return PersonalNetwork.ATTRIBUTES_COL_NAME + " = ? AND " + 
				PersonalNetwork.DYADS_COL_SOURCE + " = ? AND " +
				PersonalNetwork.DYADS_COL_TARGET + " = ? ";
	}

	@Override
	protected String[] getAttributeElementSelectionArgs(String attributeName) {
		return new String[]{attributeName, sourceName, targetName};
	}

	@Override
	protected ContentValues getAttributeElementContentValues(
			String attributeName) {
		ContentValues contentValues = new ContentValues();
		contentValues.put(PersonalNetwork.ATTRIBUTES_COL_NAME, attributeName);
		contentValues.put(PersonalNetwork.DYADS_COL_SOURCE, sourceName);
		contentValues.put(PersonalNetwork.DYADS_COL_TARGET, targetName);
		return contentValues;
	}

	@Override
	protected String[] getElementColumnNames() {
		return new String[]{PersonalNetwork.DYADS_COL_SOURCE, PersonalNetwork.DYADS_COL_TARGET};
	}

	@Override
	protected AlterAlterDyad getInstanceFromCursor(Cursor c) {
		return getInstance(c.getString(c.getColumnIndexOrThrow(PersonalNetwork.DYADS_COL_SOURCE)), 
				c.getString(c.getColumnIndexOrThrow(PersonalNetwork.DYADS_COL_SOURCE)));
	}

}
