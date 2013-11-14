/**
 * 
 */
package net.egosmart.scc.data;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Abstract superclass for the elements of a personal network.
 * 
 * Elements belong to one of the following domains:<br/>
 * {@link PersonalNetwork#DOMAIN_EGO} <br/>
 * {@link PersonalNetwork#DOMAIN_ALTER} <br/>
 * {@link PersonalNetwork#DOMAIN_EGO_ALTER} <br/>
 * {@link PersonalNetwork#DOMAIN_ALTER_ALTER} <br/>
 * 
 * Concrete subclasses of Element define a linear order by implementing 
 * the compareTo() method. This linear order must be consistent with the 
 * implementation of equals.
 * 
 * @author juergen
 *
 */
public abstract class Element implements Comparable<Element> {

	/**
	 * Returns the domain of the element which is one of: <br/>
	 * 
	 * {@link PersonalNetwork#DOMAIN_EGO} <br/>
	 * {@link PersonalNetwork#DOMAIN_ALTER} <br/>
	 * {@link PersonalNetwork#DOMAIN_EGO_ALTER} <br/>
	 * {@link PersonalNetwork#DOMAIN_ALTER_ALTER} <br/>
	 * 
	 * @return a String denoting the domain of the elements.
	 */
	public abstract String getDomain();

	
	/**
	 * Returns true if and only if the element is an ego-alter dyad or
	 * an alter-alter dyad.
	 * 
	 * @return
	 */
	public abstract boolean isDyadicElement();
	
	/**
	 * Returns the reverse dyad for ego-alter dyads and alter-alter dyads 
	 * and returns an element equal to this for ego and alter elements.
	 * 
	 * @return
	 */
	public abstract Element getReverseElement();
	
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
	public abstract int compareTo(Element another);

	/**
	 * Implement this method to make it consistent with compareTo.
	 */
	@Override
	public abstract boolean equals(Object another);
	
	/**
	 * Implement this method to make it consistent with equals
	 * (equal objects must have the same hash code).
	 */
	@Override 
	public abstract int hashCode();
	
	/**
	 * Returns the names of the columns in the personal network history db tables
	 * that hold the values identifying the element.
	 * @return
	 */
	protected abstract String[] getElementColumnNames();
	
	/**
	 * Returns a concrete subtype instantiated from the values of the cursor at its current position.
	 * 
	 * @param c
	 * @return
	 */
	protected abstract Element getInstanceFromCursor(Cursor c);
	
	/**
	 * Returns a String (to be supplemented by selection arguments) that can be used
	 * in a database query to identify the rows in the attributes value table
	 * where the values of this element are stored for any attribute. 
	 * @return
	 */
	protected abstract String getElementSelectionString();
	
	/**
	 * Returns a String array of arguments that can be used together with the selection string
	 * in a database query to identify the rows in the attributes value table
	 * where the values of this element are stored for any attribute. 
	 * @return
	 */
	protected abstract String[] getElementSelectionArgs();
	
	/**
	 * Returns a String (to be supplemented by selection arguments) that can be used
	 * in a database query to identify the rows in the attributes value table
	 * where the values of this element are stored for a given attribute. 
	 * @return
	 */
	protected abstract String getAttributeElementSelectionString();
	
	/**
	 * Returns a String array of arguments that can be used together with the selection string
	 * in a database query to identify the rows in the attributes value table
	 * where the values of this element are stored for a given attribute. 
	 * @return
	 */
	protected abstract String[] getAttributeElementSelectionArgs(String attributeName);
	
	/**
	 * Returns content values that can be used database insert or update of the  table
	 * where the values of this element are stored (after times have been set).
	 * @return
	 */
	protected abstract ContentValues getAttributeElementContentValues(String attributeName);

}
