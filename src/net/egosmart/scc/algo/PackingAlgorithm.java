package net.egosmart.scc.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class PackingAlgorithm {

	private HashMap<String, Float> alterNameX;
	private HashMap<String, Float> alterNameY;
	private HashMap<HashSet<String>, Float[]> bBoxes = new HashMap<HashSet<String>, Float[]>();
	private ArrayList<SortObject> sortedBoxes = new ArrayList<SortObject>();
	private Float[] enclosingRectangle = { 0f, 0f };
	private Float totalArea;
	Float[] bestEnclosingRectangle;
	Float bestArea;
	Float widest = 0f;
	Float ratio;

	/**
	 * constructor with the coordinates of the nodes and the width/height ratio
	 * of the canvas.
	 * 
	 * @param alterNameX
	 * @param alterNameY
	 * @param ratio
	 */
	public PackingAlgorithm(HashMap<String, Float> alterNameX,
			HashMap<String, Float> alterNameY, Float ratio) {
		bBoxes.clear();
		sortedBoxes.clear();
		this.alterNameX = alterNameX;
		this.alterNameY = alterNameY;
		this.ratio = ratio;
	}

	/**
	 * Arranges the components of the network by putting a bounding box around
	 * them. Those boxes get sorted by height and placed in the enclosing
	 * rectangle. Then the enclosing rectangle is made less wide but higher
	 * until an optimal solution is found.
	 * 
	 * @param components
	 */
	public void packingAlgo(HashSet<HashSet<String>> components) {
		// Calculate bounding boxes
		boundingBoxes(components);

		// Sort boxes by height
		bBoxSort();

		// calculate area of the rectangles
		totalArea();

		// calculate the widest rectangle
		widestRectangle();

		// Initial enclosing rectangle width x height

		enclosingRectangle[1] = (float) Math.ceil(Math.sqrt(totalArea / ratio));
		enclosingRectangle[0] = (float) Math
				.ceil(enclosingRectangle[1] * ratio);
		if (enclosingRectangle[0] < widest) {
			enclosingRectangle[0] = widest;
			enclosingRectangle[1] = enclosingRectangle[0] / ratio;
		}

		// place rectangles one by one in the enclosing rectangle
		// fit the enclosing rectangle to the filled space
		while (!placeRectangles()) {
			enclosingRectangle[1] += 1;
			enclosingRectangle[0] += ratio;
		}

	}

	private void widestRectangle() {
		for (SortObject rectangle : sortedBoxes) {
			Float width = rectangle.getbBox()[0];
			if (width > widest)
				widest = width;
		}

	}

	/**
	 * calculates the total area needed for all the rectangles
	 */
	private void totalArea() {
		Float area = 0f;
		for (SortObject rectangle : getSortedBoxes()) {
			Float[] bBox = rectangle.getbBox();
			area += bBox[0] * bBox[1];
		}
		totalArea = area;
	}

	/**
	 * places the rectangles in the enclosing rectangle and splits it in smaller
	 * rectangles
	 * 
	 * @param encRec
	 * @param bBoxes
	 * @param sortedBoxes
	 */
	private Boolean placeRectangles() {
		// dynamic boolean Matrix to store if a cell is occupied, columns(rows)

		ArrayList<ArrayList<Boolean>> occupation = new ArrayList<ArrayList<Boolean>>();
		occupation.add(new ArrayList<Boolean>());
		occupation.get(0).add(false);

		// ArrayList of width of columns and heights of rows
		ArrayList<Float> rows = new ArrayList<Float>();
		ArrayList<Float> columns = new ArrayList<Float>();
		rows.add(enclosingRectangle[1]);
		columns.add(enclosingRectangle[0]);

		// place the rectangles
		for (SortObject rectangle : getSortedBoxes()) {
			// the row and column to fill the rectangle in and how many
			// neighboring cells are needed
			int row = -1;

			int column = -1;

			Float[] bBox = rectangle.getbBox();
			Boolean rectanglePlaced = false;
			int[] cells = findCells(occupation, columns, rows, bBox);
			if (cells[1] < 1 || cells[3] < 1) {
				return false;
			}
			column = cells[0];
			row = cells[2];

			if (row >= 0 && column >= 0) {

				calculateCoordinates(rectangle, columns, rows, cells);

				splitCells(cells, columns, rows, bBox);

				updateOccupation(occupation, cells);

				rectanglePlaced = true;
			}
			if (!rectanglePlaced)
				return false;
		}
		Float width = 0f;
		for (int i = 0; i < columns.size() - 1; i++) {
			width += columns.get(i);
		}
		enclosingRectangle[0] = width;
		return true;
	}

	/**
	 * Calculate the coordinates where the rectangle is placed to store it for
	 * later use
	 * 
	 * @param rectangle
	 * @param columns
	 * @param rows
	 * @param cells
	 */
	private void calculateCoordinates(SortObject rectangle,
			ArrayList<Float> columns, ArrayList<Float> rows, int[] cells) {
		int column = cells[0];
		int row = cells[2];

		// calculate the coordinates of the left upper edge of the
		// placed rectangle
		Float xCoordinate = 0f;
		for (int i = 0; i < column; i++) {
			xCoordinate += columns.get(i);
		}
		Float yCoordinate = 0f;
		for (int i = 0; i < row; i++) {
			yCoordinate += rows.get(i);
		}
		Float[] coordinates = { xCoordinate, yCoordinate };
		rectangle.setCoordinates(coordinates);

	}

	/**
	 * updates the occupation matrix when a rectangle is placed row and column
	 * of the cell are split and the left top of those cells is occupied
	 * 
	 * @param occupation
	 * @param cells
	 */
	private void updateOccupation(ArrayList<ArrayList<Boolean>> occupation,
			int[] cells) {
		int column = cells[0];
		int amountColumns = cells[1];
		int row = cells[2];
		int amountRows = cells[3];
		// add row and column in occupation and set left upper cell
		// to true
		ArrayList<Boolean> temp = occupation.get(column + amountColumns - 1);
		ArrayList<Boolean> clone = new ArrayList<Boolean>();

		for (Boolean b : temp) {
			clone.add(b);
		}

		occupation.add(column + amountColumns - 1, clone);
		for (ArrayList<Boolean> c : occupation) {
			if (c.get(row + amountRows - 1)) {
				c.add(row + amountRows - 1, true);
			} else {
				c.add(row + amountRows - 1, false);
			}
		}

		for (int j = 0; j < amountColumns; j++) {
			for (int k = 0; k < amountRows; k++) {
				occupation.get(column + j).set(row + k, true);
			}
		}

	}

	/**
	 * splits the a cell in two when a rectangle ends within that cell
	 * 
	 * @param cells
	 * @param columns
	 * @param rows
	 * @param bBox
	 */
	private void splitCells(int[] cells, ArrayList<Float> columns,
			ArrayList<Float> rows, Float[] bBox) {
		int column = cells[0];
		int amountColumns = cells[1];
		int row = cells[2];
		int amountRows = cells[3];
		// split row and column
		int i = 1;
		Float width = bBox[0];
		// find width that is needed in the last cell that is used to
		// place rectangle
		while (i < amountColumns) {
			width -= columns.get(column + i - 1);
			i++;
		}

		// find height that is needed in the last cell that is used to
		// place rectangle
		i = 1;
		Float height = bBox[1];
		while (i < amountRows) {
			height -= rows.get(row + i - 1);
			i++;
		}

		columns.set(column + amountColumns - 1,
				columns.get(column + amountColumns - 1) - width);
		columns.add(column + amountColumns - 1, width);

		rows.set(row + amountRows - 1, rows.get(row + amountRows - 1) - height);
		rows.add(row + amountRows - 1, height);

	}

	/**
	 * finds the cells that are need to place the rectangle specified by the
	 * bounding box
	 * 
	 * @param occupation
	 *            matrix of enclosing rectangle
	 * @param columns
	 * @param rows
	 * @param bBox
	 *            bounding box
	 * @return
	 */
	private int[] findCells(ArrayList<ArrayList<Boolean>> occupation,
			ArrayList<Float> columns, ArrayList<Float> rows, Float[] bBox) {
		int[] cells = new int[4];
		// columns
		for (int i = 0; i < occupation.size(); i++) {
			Boolean found = false;
			// rows
			for (int j = 0; j < occupation.get(0).size(); j++) {
				// cell is occupied
				if (occupation.get(i).get(j))
					continue;

				// cell is too narrow
				Float width = columns.get(i);
				int k = 1;
				while (bBox[0] > width && k + i < occupation.size()) {
					if (occupation.get(i).get(j))
						break;
					width += columns.get(i + k);
					k++;
				}
				if (width >= bBox[0]) {
					cells[0] = i;
					cells[1] = k;
				} else {
					continue;
				}

				// cell is not high enough
				Float height = rows.get(j);
				k = 1;
				while (bBox[1] > height && k + j < occupation.get(i).size()) {
					if (occupation.get(i).get(j + k))
						break;
					height += rows.get(j + k);
					k++;
				}

				if (height >= bBox[1]) {
					cells[2] = j;
					cells[3] = k;
					found = true;
				} else {
					continue;
				}
			}
			if (found)
				break;
		}
		return cells;
	}

	/**
	 * Class to store the component, its bounding box and the coordinates where
	 * its placed.
	 * 
	 * @author raffaelwagner
	 * 
	 */
	private final class SortObject implements Comparable<SortObject> {
		private HashSet<String> component;
		private Float[] bBox;
		private Float[] coordinates;

		public SortObject(HashSet<String> component, Float[] bBox) {
			this.setComponent(component);
			this.setbBox(bBox);
		}

		@Override
		public int compareTo(SortObject arg0) {
			Float a = bBox[1];
			Float b = arg0.getbBox()[1];
			if (a < b)
				return -1;
			if (a > b)
				return 1;
			return 0;
		}

		public Float[] getbBox() {
			return bBox;
		}

		private void setbBox(Float[] bBox) {
			this.bBox = bBox;
		}

		public HashSet<String> getComponent() {
			return component;
		}

		private void setComponent(HashSet<String> component) {
			this.component = component;
		}

		private Float[] getCoordinates() {
			return coordinates;
		}

		private void setCoordinates(Float[] coordinates) {
			this.coordinates = coordinates;
		}

	}

	/**
	 * sorts the rectangles by height with a quicksort algorithm
	 * 
	 * @param bBoxes
	 * @return
	 */
	private void bBoxSort() {
		ArrayList<SortObject> sortedRectangles = new ArrayList<SortObject>();
		for (HashSet<String> component : getbBoxes().keySet()) {
			SortObject sort = new SortObject(component, getbBoxes().get(
					component));
			sortedRectangles.add(sort);
		}

		quicksort(0, sortedRectangles.size() - 1, sortedRectangles);

		setSortedBoxes(sortedRectangles);
	}

	/**
	 * simple quicksort algorithm adapted to the rectangle problem
	 * 
	 * @param low
	 * @param high
	 * @param sortedRectangles
	 */
	private void quicksort(int low, int high,
			ArrayList<SortObject> sortedRectangles) {
		int i = low, j = high;
		// Get the pivot element from the middle of the list

		SortObject pivot = sortedRectangles.get(low + (high - low) / 2);

		// Divide into two lists
		while (i <= j) {
			// If the current value from the left list is larger then the pivot
			// element then get the next element from the left list
			while (pivot.compareTo(sortedRectangles.get(i)) < 0) {
				i++;
			}
			// If the current value from the right list is smaller then the
			// pivot
			// element then get the next element from the right list
			while (pivot.compareTo(sortedRectangles.get(j)) > 0) {
				j--;
			}

			// If we have found a values in the left list which is smaller then
			// the pivot element and if we have found a value in the right list
			// which is larger then the pivot element then we exchange the
			// values.
			// As we are done we can increase i and j
			if (i <= j) {
				exchange(i, j, sortedRectangles);
				i++;
				j--;
			}
		}
		// Recursion
		if (low < j)
			quicksort(low, j, sortedRectangles);
		if (i < high)
			quicksort(i, high, sortedRectangles);
	}

	/**
	 * swaps two positions in the array
	 * 
	 * @param i
	 * @param j
	 * @param sortedRectangles
	 */
	private void exchange(int i, int j, ArrayList<SortObject> sortedRectangles) {
		SortObject tempSet = sortedRectangles.get(i);
		sortedRectangles.set(i, sortedRectangles.get(j));
		sortedRectangles.set(j, tempSet);
	}

	/**
	 * finds the most outer points and calculates how big the bounding box
	 * around the component is.
	 * 
	 * @param components
	 * @return
	 */
	private void boundingBoxes(HashSet<HashSet<String>> components) {
		for (HashSet<String> component : components) {
			Float maxX = 0f;
			Float maxY = 0f;
			Float minX = Float.MAX_VALUE;
			Float minY = Float.MAX_VALUE;
			for (String n : component) {
				Float x = alterNameX.get(n);
				Float y = alterNameY.get(n);
				if (x > maxX)
					maxX = x;
				if (x < minX)
					minX = x;
				if (y > maxY)
					maxY = y;
				if (y < minY)
					minY = y;
			}
			
			Float[] bBox = { (maxX - minX)*1.2f, (maxY - minY)*1.2f };
			// get rid of negative numbers
			if (minX < 0 && minY < 0) {
				for (String n : component) {
					Float x = alterNameX.get(n);
					Float y = alterNameY.get(n);
					alterNameX.put(n, x - minX);
					alterNameY.put(n, y - minY);
				}
			} else if(minX<0){
				for (String n : component) {
					Float x = alterNameX.get(n);
					alterNameX.put(n, x - minX);
				}
			} else if(minY<0){
				for (String n : component) {
					Float y = alterNameY.get(n);
					alterNameY.put(n, y - minY);
				}
			}
			getbBoxes().put(component, bBox);
		}
	}

	public void adjustCoordinates(HashMap<String, Float> alterNameX,
			HashMap<String, Float> alterNameY) {
		for (SortObject rectangle : sortedBoxes) {
			HashSet<String> component = rectangle.getComponent();
			Float[] coordinates = rectangle.getCoordinates();
			for (String node : component) {
				Float x = alterNameX.get(node);
				Float y = alterNameY.get(node);
				alterNameX.put(node, x + coordinates[0]);
				alterNameY.put(node, y + coordinates[1]);
			}
		}
	}

	public void scaleCoordinates(HashMap<String, Float> alterNameX,
			HashMap<String, Float> alterNameY) {
		Float scaleFactorX = enclosingRectangle[0];
		Float scaleFactorY = enclosingRectangle[1];
		for (String node : alterNameX.keySet()) {
			Float x = alterNameX.get(node);
			Float y = alterNameY.get(node);
			alterNameX.put(node, x / scaleFactorX);
			alterNameY.put(node, y / scaleFactorY);
		}
	}

	private HashMap<HashSet<String>, Float[]> getbBoxes() {
		return bBoxes;
	}

	private ArrayList<SortObject> getSortedBoxes() {
		return sortedBoxes;
	}

	private void setSortedBoxes(ArrayList<SortObject> sortedBoxes) {
		this.sortedBoxes = sortedBoxes;
	}
}