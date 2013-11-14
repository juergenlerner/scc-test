/**
 * 
 */
package net.egosmart.scc.data;

/**
 * Represents an ordered pair of two strings: source and target. 
 * 
 * @author juergen
 *
 */
public class OrderedDyad {

	private String source;
	private String target;
	
	public OrderedDyad(String source, String target){
		this.source = source;
		this.target = target;
	}
	
	public String target(){
		return target;
	}

	public String source(){
		return source;
	}
	
	/**
	 * Returns true if and only if other is an instance of OrderedDyad and other.source is equal to source
	 * and other.target is equal to target (where the inner equal function is that of String).
	 */
	public boolean equals(Object other){
		if(!(other instanceof OrderedDyad)){
			return false;
		}
		OrderedDyad oDyad = (OrderedDyad) other;
		return this.source.equals(oDyad.source) && this.target.equals(oDyad.target);
	}
	
	/**
	 * Consistent with equals.
	 */
	public int hashCode(){
		return source.hashCode() + 3*target.hashCode();
	}
}
