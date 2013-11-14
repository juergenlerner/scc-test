/**
 * 
 */
package net.egosmart.scc.data;

/**
 * Represents an unordered pair of two strings: source and target.
 * 
 * @author juergen
 *
 */
public class UnorderedDyad {

	private String source;
	private String target;
	
	public UnorderedDyad(String source, String target){
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
	 * Returns true if and only if other is an instance of UnorderedDyad and
	 * [(other.source is equal to source and other.target is equal to target)
	 * or
	 * (other.target is equal to source and other.source is equal to target)]
	 */
	public boolean equals(Object other){
		if(!(other instanceof UnorderedDyad)){
			return false;
		}
		UnorderedDyad oDyad = (UnorderedDyad) other;
		return (this.source.equals(oDyad.source) && this.target.equals(oDyad.target))
				|| (this.source.equals(oDyad.target) && this.target.equals(oDyad.source));
	}
	
	/**
	 * Consistent with equals.
	 */
	public int hashCode(){
		//is symmetric
		return source.hashCode() + target.hashCode();
	}
}
