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
 * Implements Algorithm 1 from Brandes (2008) to compute betweenness centrality 
 * of a directed unweighted graph (add pairs of anti-parallel edges to apply to undirected graphs).
 * 
 * References:
 * Ulrik Brandes: On Variants of Shortest-Path Betweenness Centrality and their Generic Computation. 
 * Social Networks 30(2):136-145, 2008.
 * 
 * @author juergen
 *
 */
public class BetweennessCentralityAlgo {

	/**
	 * Computes vertex betweenness centrality of a directed unweighted graph using 
	 * Algorithm 1 from Brandes (2008).
	 * 
	 * @param neighborhoods Map from the set of all vertices of the graph (represented by unique Strings, their identifiers) to their 
	 * 			set of out-neighbors (represented by the String identifiers of vertices). A vertex w is an out-neighbor
	 * 			of a vertex v if (v,w) is a directed edge in the graph. 
	 * 			For undirected edges {v,w} put the vertex w into the out-neighborhood of v and put v into the
	 * 			out-neighborhood of w.
	 * @return Map from vertices (represented by their String identifiers) to their betweenness centrality (given as Doubles).
	 */
	public static HashMap<String, Double> computeBetweenness(HashMap<String, HashSet<String>> neighborhoods){
		Set<String> V = neighborhoods.keySet();//the vertex set (only to save typing)
		////////////////////////////////////////////////
		//initialize temporary datastructures
		////////////////////////////////////////////////
		Queue<String> Q = new ArrayDeque<String>(); //queue for shortest-path computations
		Deque<String> S = new ArrayDeque<String>(); //stack for accumulation of dependencies
		//mapping from vertices to their distance from the (current) source
		HashMap<String, Integer> distance = new HashMap<String, Integer>();
		//mapping from vertices to their list of predecessors on shortest paths from source
		HashMap<String, LinkedList<String>> pred = new HashMap<String, LinkedList<String>>(); 
		//mapping from vertices v to their number of shortest paths from source to v
		HashMap<String, Double> sigma = new HashMap<String, Double>();
		//mapping from vertices v to the dependency of source on v
		HashMap<String, Double> delta = new HashMap<String, Double>();
		//mapping from vertices v to their betweenness centrality
		HashMap<String, Double> betweenness = new HashMap<String, Double>();
		//initialize betweenness to zero
		for(String v : V){
			betweenness.put(v, Double.valueOf(0));
		}
		////////////////////////////////////////////////
		//do the computation
		////////////////////////////////////////////////
		for(String source : V){
			//compute single-source shortest-path (sssp) from source
			//initialize sssp from source
			for(String w : V){
				pred.put(w, new LinkedList<String>());
			}
			for(String t : V){
				distance.put(t, Integer.MAX_VALUE);//represents infinity (i.e., unreachability)
				sigma.put(t, Double.valueOf(0));
			}
			distance.put(source, Integer.valueOf(0));//distance of source to itself is zero
			sigma.put(source, Double.valueOf(1));//dependency of source on itself is one
			Q.add(source);//put the source into the queue
			//end init sssp from source
			while(!Q.isEmpty()){
				String v = Q.poll();//get (and remove) the first element v of the queue
				S.push(v);// put it onto the stack
				for(String w : neighborhoods.get(v)){
					//path discovery (is w found for the first time?)
					if(distance.get(w).equals(Integer.MAX_VALUE)){
						distance.put(w, distance.get(v)+1);//w is reached via v with one additional edge
						Q.add(w);
					}//end if w is found for the first time
					//path counting (is the edge (v,w) on a shortest path?)
					if(distance.get(w).equals(distance.get(v)+1)){
						sigma.put(w, sigma.get(w)+sigma.get(v));//update number of shortest paths
						pred.get(w).add(v);//append v to w's predecessors
					}//end if (v,w) is on a shortest path
				}//end for all out-neighbors w of v
			}//end while Q not empty
			//end compute single-source shortest-path from source
			//accumulation
			for(String v : V){
				//init dependencies
				delta.put(v, Double.valueOf(0));
			}
			//process vertices from the stack
			while(!S.isEmpty()){
				String w = S.pop();//get (and remove) the top element
				for(String v : pred.get(w)){//iterate over all predecessors v of w
					delta.put(v, delta.get(v) + sigma.get(v)/sigma.get(w)*(1 + delta.get(w)) );
				}//end for all predecessors v of w
				if(!w.equals(source)){
					betweenness.put(w, betweenness.get(w)+delta.get(w));
				}
			}//end while S not empty
			//end accumulation
		}//end for all s in V
		return betweenness;
	}
}
