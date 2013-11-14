package net.egosmart.scc.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.LayoutSQLite;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import android.util.FloatMath;
import android.view.View;


public class NetworkLayout {

	private PersonalNetwork network;
	private Double[][] distanceMatrix;
	private LinkedList<String> pivotSet;
	private Double epsilon;
	private HashMap<String, Float> alterNameX = new HashMap<String, Float>();
	private HashMap<String, Float> alterNameY = new HashMap<String, Float>();
	private static Random rand = new Random();
	private HashMap<String, HashSet<String>> neighbors = new HashMap<String, HashSet<String>>();
	public static Boolean fromDatabase = false;
	private HashMap<String, HashMap<String, Double>> distances = new HashMap<String, HashMap<String, Double>>();

	/**
	 * creates a new LayoutView from either the database or calculates a new one
	 * depending on the last change of the network
	 * 
	 * @param activity
	 * @param ratio the width/height ratio of the canvas to be drawn on
	 */
	public NetworkLayout(SCCMainActivity activity, Float ratio) {
		network = PersonalNetwork.getInstance(activity);
		HashSet<String> alters = network.getAltersAt(TimeInterval
				.getCurrentTimePoint());
		LayoutSQLite layout = LayoutSQLite.getInstance(activity);
		// check if there is at least one alter
		if (alters.size() > 0) {
			// check if the database is uptodate or if new calculation is needed
			fromDatabase = (network.getLastChange() < layout.getTime());
			if (fromDatabase) {
				// get coordinates from DB
				HashMap<String, Float[]> coordinates = layout.getCoordinates();
				for (String node : coordinates.keySet()) {
					alterNameX.put(node, coordinates.get(node)[0]);
					alterNameY.put(node, coordinates.get(node)[1]);
				}
			} else {

				// fill the neighbors in the map
				for (String node : alters) {
					HashSet<String> neighborhood = network.getNeighborsAt(
							TimeInterval.getCurrentTimePoint(), node);
					neighbors.put(node, neighborhood);
				}
				// calculate the components
				HashSet<HashSet<String>> components = getConnectedComponent(alters);
				for (HashSet<String> component : components) {
					if (component.size() > 1) {
						int k = 50;
						if (k > component.size()) {
							k = component.size();
						}
						calculateDistances(component);
						// get the coordinates for the nodes in this component
						pivotMDS(component, k);
						stressMajorization(component);
					} else if (component.size() == 1) {
						for (String node : component) {
							alterNameX.put(node, 0.5f);
							alterNameY.put(node, 0.5f);
						}
					}
				}
				// if there are more than 1 component arrange them next to each
				// other

				if (components.size() > 1) {
					//parallelArrangment(components);
					
				}

				
				PackingAlgorithm packAlgo = new PackingAlgorithm(alterNameX, alterNameY, ratio);
				packAlgo.packingAlgo(components);
				packAlgo.adjustCoordinates(alterNameX, alterNameY);
				packAlgo.scaleCoordinates(alterNameX, alterNameY);



				// fill coordinates into DB
				HashMap<String, Float[]> coordinates = new HashMap<String, Float[]>();
				for (String node : alterNameX.keySet()) {
					Float x = alterNameX.get(node);
					Float y = alterNameY.get(node);
					coordinates.put(node, new Float[] { x, y });
				}
				layout.setCoordinates(coordinates);

			}
		}
	}

	/**
	 * calculates the final coordinates by applying a stress minimization
	 * algorithm called Stress Majorization. The nodes are recalculated in a
	 * loop to get closer to the prefered distances in the graph.
	 * 
	 * @param a
	 *            hashset of the nodes of a connected graph
	 */
	private void stressMajorization(HashSet<String> component) {

		epsilon = 0.01;
		Float max = 1f;

		while (max > epsilon) {
			max = 1f;
			for (String node : component) {
				// Calculate for x and y
				Float x = alterNameX.get(node);
				Float y = alterNameY.get(node);

				Float startX = x;
				Float startY = y;

				Float xi = 0f;
				Float yi = 0f;
				Float wSum = 0f;
				for (String j : component) {

					if (node != j) {
						Float xj = alterNameX.get(j);
						Float yj = alterNameY.get(j);
						Float dist = getEuclideanDistance(node, j);
						HashMap<String, Double> map = distances.get(node);
						Double temp = map.get(j);
						Float dij = new Float(temp);
						Float s = dij / dist;
						Float wij = 1 / (dij * dij);
						xi += wij * (xj + s * (x - xj));
						yi += wij * (yj + s * (y - yj));
						wSum += wij;
					}
				}
				x = xi / wSum;
				y = yi / wSum;
				alterNameX.put(node, x);
				alterNameY.put(node, y);
				Float dx = x - startX;
				Float dy = y - startY;
				Float d = FloatMath.sqrt(dx * dx + dy * dy);

				if (max == 1f) {
					max = d;
				} else {
					if (d > max) {
						max = d;
					}
				}
			}
		}


	}

	
	/**
	 * 
	 * @param alters
	 * @return
	 */
	private HashSet<HashSet<String>> getConnectedComponent(
			HashSet<String> alters) {
		HashSet<HashSet<String>> components = new HashSet<HashSet<String>>();
		HashSet<String> visited = new HashSet<String>();
		Queue<String> candidates = new LinkedList<String>();
		String[] nodes = new String[alters.size()];
		alters.toArray(nodes);
		candidates.add(nodes[0]);
		while (!candidates.isEmpty()) {
			HashSet<String> component = new HashSet<String>();
			getComponent(candidates.poll(), component);
			components.add(component);
			visited.addAll(component);
			for (String node : nodes) {
				if (!visited.contains(node)) {
					candidates.add(node);
					break;
				}
			}
		}
		return components;
	}

	private void getComponent(String node, HashSet<String> component) {
		component.add(node);
		HashSet<String> nodeNeighbors = neighbors.get(node);
		if (nodeNeighbors != null) {
			for (String neighbor : neighbors.get(node)) {
				if (!component.contains(neighbor)) {
					getComponent(neighbor, component);
				}
			}
		}
	}

	/**
	 * calculates the initial coordinates with Multidimensional Scaling.
	 * 
	 * @param alters
	 *            the set of nodes in the graph alters
	 * @param k
	 *            the number of pivot elements
	 */
	private void pivotMDS(HashSet<String> alters, int k) {
		// construct the pivot set
		constructPivotSet(alters, k);

		// calculate the Matrix C = 1/2 Jn D Jk where D is the pivot matrix with
		// all entries squared.
		if (distanceMatrix != null) {
			Double[][] distanceMatrixS = distanceMatrix;
			int n = distanceMatrix.length;
			int m = distanceMatrix[0].length;
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < m; j++) {
					distanceMatrixS[i][j] *= distanceMatrixS[i][j];
				}
			}

			// calculate the J_n matrix
			Double[][] centerMatrix1 = new Double[n][n];
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					Double d = new Double(n);
					if (i == j) {
						centerMatrix1[i][j] = (d - 1) / d;
					} else {
						centerMatrix1[i][j] = -1 / d;
					}
				}
			}

			// calculate the J_k matrix
			Double[][] centerMatrix2 = new Double[m][m];
			for (int i = 0; i < m; i++) {
				for (int j = 0; j < m; j++) {
					Double d = new Double(m);
					if (i == j) {
						centerMatrix2[i][j] = (d - 1) / d;
					} else {
						centerMatrix2[i][j] = -1 / d;
					}
				}
			}

			// calculate the C matrix
			Double[][] centeredMatrix = matrixMultiplikation(centerMatrix1,
					distanceMatrixS, n, m, n, n);
			centeredMatrix = matrixMultiplikation(centeredMatrix,
					centerMatrix2, m, m, n, m);
			centeredMatrix = matrixSkalarMultiplikation(centeredMatrix, n, m,
					-0.5);

			// fill y with random numbers
			Double[][] eigenVectors = new Double[k][2];
			for (Double[] row : eigenVectors) {
				row[0] = rand.nextDouble();
				row[1] = rand.nextDouble();
			}

			// calculate matrix H = C^T*C
			Double[][] centeredMatrixT = new Double[m][n];
			for (int i = 0; i < m; i++) {
				for (int j = 0; j < n; j++) {
					centeredMatrixT[i][j] = centeredMatrix[j][i];
				}
			}
			Double[][] matrixH = matrixMultiplikation(centeredMatrixT,
					centeredMatrix, n, m, m, n);

			// compute eigenvector
			// while there is a change in y_i
			Double max = 100.0;
			Double startNorm1 = 0.0;
			Double startNorm2 = 0.0;
			epsilon = 0.01;
			while (max > epsilon) {
				// calculate first eigenvector
				Double[][] eigenVector1 = new Double[k][1];
				for (int i = 0; i < k; i++) {
					eigenVector1[i][0] = eigenVectors[i][0];
				}

				// calculate second eigenvector
				Double[][] eigenVector2 = new Double[k][1];
				for (int i = 0; i < m; i++) {
					eigenVector2[i][0] = eigenVectors[i][1];
				}
				eigenVector1 = matrixMultiplikation(matrixH, eigenVector1, k,
						1, k, k);
				Double norm1 = calculateNorm(eigenVector1);

				eigenVector1 = matrixSkalarMultiplikation(eigenVector1, k, 1,
						1 / norm1);

				Double norm2 = calculateNorm(eigenVector2);

				eigenVector2 = matrixMultiplikation(matrixH, eigenVector2, k,
						1, k, k);
				eigenVector2 = matrixSkalarMultiplikation(eigenVector2, k, 1,
						1 / norm2);

				// translate the second eigenvector
				Double transform = 0.0;
				Double t1 = 0.0;
				Double t2 = 0.0;
				for (int i = 0; i < k; i++) {
					t1 += eigenVector1[i][0] * eigenVector2[i][0];
					t2 += eigenVector1[i][0] * eigenVector1[i][0];
				}
				transform = t1 / t2;
				Double[][] temp = matrixSkalarMultiplikation(eigenVector1, k,
						1, transform);
				for (int i = 0; i < k; i++) {
					eigenVector2[i][0] = eigenVector2[i][0] - temp[i][0];
				}

				// calculate the biggest change
				max = Math.max(Math.abs(norm1 - startNorm1),
						Math.abs(norm2 - startNorm2));
				startNorm1 = norm1;
				startNorm2 = norm2;
			}

			// fill the coordinates in

			Double[][] dCoordinates = matrixMultiplikation(centeredMatrix,
					eigenVectors, k, 2, n, k);

			Float[][] coordinates = new Float[n][2];
			for (int i = 0; i < dCoordinates.length; i++) {
				coordinates[i][0] = new Float(dCoordinates[i][0]);
				coordinates[i][1] = new Float(dCoordinates[i][1]);
			}

			int size = alters.size();
			String[] nodes = new String[size];
			alters.toArray(nodes);
			for (int i = 0; i < n; i++) {
				alterNameX.put(nodes[i], coordinates[i][0]);
				alterNameY.put(nodes[i], coordinates[i][1]);
			}
		}
	}

	/**
	 * calculates the euclidean distance between 2 nodes
	 * 
	 * @param node1
	 * @param node2
	 * @return
	 */
	private Float getEuclideanDistance(String node1, String node2) {
		Float dx = Math.abs(alterNameX.get(node1) - alterNameX.get(node2));
		Float dy = Math.abs(alterNameY.get(node1) - alterNameY.get(node2));
		Double dxd = dx.doubleValue();
		Double dyd = dy.doubleValue();
		Double result = Math.sqrt(dxd * dxd + dyd * dyd);
		return new Float(result);
	}

	/**
	 * multiplies a matrix m with a scalar x
	 * 
	 * @param m1
	 *            the matrix
	 * @param n
	 *            number of rows
	 * @param k
	 *            number of columns
	 * @param x
	 *            scalar
	 * @return
	 */
	private Double[][] matrixSkalarMultiplikation(Double[][] m1, int n, int k,
			Double x) {
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < k; j++) {
				m1[i][j] *= x;
			}
		}
		return m1;
	}

	/**
	 * calculates the norm of a matrix m
	 * 
	 * @param m
	 *            the matrix that is to be normed
	 * @return the norm
	 */
	private Double calculateNorm(Double[][] m) {
		Double norm = 0.0;
		int k = m.length;
		for (int i = 0; i < k; i++) {
			norm += m[i][0] * m[i][0];
		}
		norm = Math.sqrt(norm);
		return norm;
	}

	/**
	 * Calculates the pivot set by starting at a random node and then picking
	 * the furthest away node as next pivot element
	 * 
	 * @param alters
	 *            the set of nodes in the graph
	 * @param k
	 *            the number of pivot elements
	 */
	private void constructPivotSet(HashSet<String> alters, int k) {
		// get all nodes in the Graph

		if (!neighbors.isEmpty()) {
			int n = alters.size();
			String[] nodes = new String[n];
			alters.toArray(nodes);
			// Array to store the pivot sets
			pivotSet = new LinkedList<String>();

			// choose random first pivot element

			int randomInt = 2;
			if (n >= 1) {
				randomInt = rand.nextInt(n - 1);
			}
			String first = nodes[randomInt];

			pivotSet.add(first);

			// set all min distances to infinity
			HashMap<String, Double> minDistances = new HashMap<String, Double>();

			// for all columns in the distance matrix
			distanceMatrix = new Double[n][k];
			String max = first;
			Double maxValue = 0.0;
			for (int i = 0; i < k; i++) {
				for (String node : nodes) {
					minDistances.put(node, Double.POSITIVE_INFINITY);
				}
				// calculate the ith column by calculating the distances from
				// p_i to
				// all the other nodes
				for (int j = 0; j < n; j++) {
					Double dist = distances.get(pivotSet.get(i)).get(nodes[j]);

					if (dist != null) {
						distanceMatrix[j][i] = dist;

						// set min distances for all nodes to the min of the
						// calculated
						// distance from p_i and the former min

						if (minDistances.get(nodes[j]) > dist) {
							minDistances.put(nodes[j], dist);
							if (dist > maxValue && !pivotSet.contains(nodes[j])) {
								maxValue = dist;
								max = nodes[j];
							}
						}
					}
				}
				// set p_i+1 to the max min distance
				if (i < k - 1) {
					pivotSet.add(max);
					maxValue = 0.0;
				}
			}
		}
	}

	/**
	 * calculates all distances for a component by applying a breadth first
	 * search for every node
	 * 
	 * @param component
	 */
	private void calculateDistances(HashSet<String> component) {
		for (String node : component) {
			bfs(node);
		}

	}

	/**
	 * a breadth first search to determine the distances of all nodes to the
	 * chosen node
	 * 
	 * @param node
	 */
	private void bfs(String node) {
		LinkedList<String> queue = new LinkedList<String>();
		HashMap<String, String> parents = new HashMap<String, String>();
		HashMap<String, Double> dist = new HashMap<String, Double>();
		dist.put(node, 0.0);
		parents.put(node, "start");
		queue.add(node);
		while (!queue.isEmpty()) {
			String n = queue.poll();
			String parent = parents.get(n);
			if (parent != "start") {
				Double distance = 1 + dist.get(parent);
				dist.put(n, distance);
			}
			for (String neighbor : neighbors.get(n)) {
				if (!parents.containsKey(neighbor)) {
					queue.add(neighbor);
					parents.put(neighbor, n);
				}
			}
		}
		distances.put(node, dist);
	}

	/**
	 * calculates the result matrix of the matrix multiplikation of m1 and m2
	 * 
	 * @param m1
	 *            first matrix
	 * @param m2
	 *            second matrix
	 * @param k1
	 *            number of columns of m1
	 * @param k2
	 *            number of columns of m2
	 * @param n1
	 *            number of rows of m1
	 * @param n2
	 *            number of rows of m2
	 * @return
	 */
	public Double[][] matrixMultiplikation(Double[][] m1, Double[][] m2,
			int k1, int k2, int n1, int n2) {
		Double[][] resultMatrix = new Double[n1][k2];
		for (int i = 0; i < n1; i++) {
			for (int j = 0; j < k2; j++) {
				Double entry = 0.0;
				for (int k = 0; k < n2; k++) {
					entry += m1[i][k] * m2[k][j];
				}
				resultMatrix[i][j] = entry;
			}
		}
		return resultMatrix;
	}

	public HashMap<String, Float> getXCoordinates() {
		return alterNameX;
	}

	public HashMap<String, Float> getYCoordinates() {
		return alterNameY;
	}
}