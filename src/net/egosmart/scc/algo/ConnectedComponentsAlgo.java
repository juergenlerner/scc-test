/**
 * 
 */
package net.egosmart.scc.algo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

/**
 * 
 * Computes the connected components of an undirected graph.
 * 
 * @author juergen
 *
 */
public class ConnectedComponentsAlgo {

	/**
	 * Computes the connected components of an undirected graph. 
	 * 
	 * @param neighborhoods Map from the set of all vertices of the graph (represented by unique Strings, their identifiers) to their 
	 * 			set of neighbors (represented by the String identifiers of vertices). A vertex w is a neighbor
	 * 			of a vertex v if {v,w} is an undirected edge in the graph. Note that if w is a neighbor of v, then 
	 * 			v must be a neighbor of w. However this property is not checked and calling this method with asymmetric
	 * 			neighborhoods results in undefined behavior. 
	 * @return Map from vertices (represented by their String identifiers) to an integer indicating component membership. 
	 * 			The component indices range from zero to the number of connected components minus one.
	 */
	public static HashMap<String, Integer> computeComponents(HashMap<String, HashSet<String>> neighborhoods){
		Set<String> V = neighborhoods.keySet();//the vertex set
		Set<String> vertices_left =  new HashSet<String>(V);//vertices not yet assigned to a component
		////////////////////////////////////////////////
		//initialize temporary datastructures
		////////////////////////////////////////////////
		Queue<String> Q = new ArrayDeque<String>(); //queue for shortest-path computations
		HashMap<String, Integer> components = new HashMap<String, Integer>();
		////////////////////////////////////////////////
		//do the computation
		////////////////////////////////////////////////
		int index = 0;//counter for components
		while(!vertices_left.isEmpty()){
			// get some vertex to start a component
			String source = vertices_left.iterator().next();
			vertices_left.remove(source);
			components.put(source, index);
			Q.add(source);
			while(!Q.isEmpty()){
				String v = Q.poll();//get (and remove) some element v of the queue
				for(String w : neighborhoods.get(v)){
					//is w found for the first time?
					if(vertices_left.contains(w)){
						vertices_left.remove(w);
						components.put(w, index);
						Q.add(w);
					}//end if w is found for the first time
				}//end for all neighbors w of v
			}//end while Q not empty (no more vertices are reachable from source)
			++index;//increase counter for the next component (if any)
		}//no more vertices are left
		return components;
	}

	/**
	 * Computes the number of connected components of an undirected graph. 
	 * 
	 * @param neighborhoods Map from the set of all vertices of the graph (represented by unique Strings, their identifiers) to their 
	 * 			set of neighbors (represented by the String identifiers of vertices). A vertex w is a neighbor
	 * 			of a vertex v if {v,w} is an undirected edge in the graph. Note that if w is a neighbor of v, then 
	 * 			v must be a neighbor of w. However this property is not checked and calling this method with asymmetric
	 * 			neighborhoods results in undefined behavior. 
	 * @return number of connected components
	 */
	public static int computeNumberOfComponents(HashMap<String, HashSet<String>> neighborhoods){
		Set<String> V = neighborhoods.keySet();//the vertex set
		Set<String> vertices_left =  new HashSet<String>(V);//vertices not yet assigned to a component
		////////////////////////////////////////////////
		//initialize temporary datastructures
		////////////////////////////////////////////////
		Queue<String> Q = new ArrayDeque<String>(); //queue for shortest-path computations
		////////////////////////////////////////////////
		//do the computation
		////////////////////////////////////////////////
		int index = 0;//counter for components
		while(!vertices_left.isEmpty()){
			// get some vertex to start a component
			String source = vertices_left.iterator().next();
			vertices_left.remove(source);
			Q.add(source);
			while(!Q.isEmpty()){
				String v = Q.poll();//get (and remove) some element v of the queue
				for(String w : neighborhoods.get(v)){
					//is w found for the first time?
					if(vertices_left.contains(w)){
						vertices_left.remove(w);
						Q.add(w);
					}//end if w is found for the first time
				}//end for all neighbors w of v
			}//end while Q not empty (no more vertices are reachable from source)
			++index;//increase counter for the next component (if any)
		}//no more vertices are left
		return index;
	}
}
