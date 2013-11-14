/**
 * 
 */
package net.egosmart.scc.data;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A Lifetime maintains a union of non-overlapping TimeIntervals.
 * 
 * @author juergen
 *
 */
public class Lifetime {

	private TreeSet<TimeInterval> intervals;
	
	/**
	 * Creates an empty lifetime.
	 */
	public Lifetime(){
		intervals = new TreeSet<TimeInterval>();
	}
	
	protected Lifetime(SortedSet<TimeInterval> set){
		intervals = new TreeSet<TimeInterval>(set);
	}
	
	/**
	 * Adds the given interval to the lifetime, thereby maintaining the property 
	 * that intervals are non-overlapping. 
	 * 
	 * @param interval
	 */
	public void union(TimeInterval interval){
		//check whether interval overlaps any existing interval
		//which can either be the floor interval and/or all intervals between the start and end points
		TimeInterval floor = intervals.floor(interval);
		if(floor != null && floor.overlapsOrIsContiguousWith(interval)){
			intervals.remove(floor);
			interval = floor.getUnionWithContiguous(interval);
		}
		NavigableSet<TimeInterval> overlaping = intervals.
				subSet(TimeInterval.getTimePoint(interval.getStartTime()), true, 
				TimeInterval.getTimePoint(interval.getEndTime()), true);
		for(TimeInterval oldInterval : overlaping){
			if(oldInterval.overlapsOrIsContiguousWith(interval)){
				intervals.remove(oldInterval);
				interval = oldInterval.getUnionWithContiguous(interval);
			}
		}
		intervals.add(interval);
	}
	
	public void union(Lifetime lifetime){
		Iterator<TimeInterval> it = lifetime.getIterator();
		while(it.hasNext()){
			union(it.next());
		}
	}
	
	/**
	 * Makes the lifetime to be equal to the old lifetime SETMINUS interval.
	 * 
	 * If interval is a time point then either
	 * (1) the lifetime contains a time-interval equal to this point which is then removed or
	 * (2) nothing is done (even if the lifetime contains an interval that is not a time point and contains
	 * the point to be removed). 
	 * 
	 * @param interval to be cut out of the lifetime
	 */
	public void cutOut(TimeInterval interval){
		if(interval.isTimePoint()){
			intervals.remove(interval);
			return;
		}
		TimeInterval floor = intervals.floor(interval);
		if(floor != null && floor.overlaps(interval)){
			intervals.remove(floor);
			if(floor.getStartTime() < interval.getStartTime()){
				intervals.add(new TimeInterval(floor.getStartTime(), interval.getStartTime()));
			}
			if(interval.getEndTime() < floor.getEndTime()){
				intervals.add(new TimeInterval(interval.getEndTime(), floor.getEndTime()));
			}
		}
		NavigableSet<TimeInterval> overlaping = intervals.
				subSet(TimeInterval.getTimePoint(interval.getStartTime()), true, 
				TimeInterval.getTimePoint(interval.getEndTime()), true);
		for(TimeInterval oldInterval : overlaping){
			if(oldInterval.overlaps(interval)){
				intervals.remove(oldInterval);
				if(interval.getEndTime() < oldInterval.getEndTime()){//this might happen at most for the last one
					intervals.add(new TimeInterval(interval.getEndTime(), oldInterval.getEndTime()));
				}
			}
		}
	}
	
	public boolean overlaps(TimeInterval interval){
		TimeInterval floor = intervals.floor(interval);
		if(floor != null && floor.overlaps(interval))
			return true;
		TimeInterval ceil = intervals.ceiling(interval);
		if(ceil != null && ceil.overlaps(interval))
			return true;
		return false;
	}
	
	public boolean contains(TimeInterval interval){
		TimeInterval floor = intervals.floor(interval);
		if(floor != null && floor.contains(interval))
			return true;
		TimeInterval ceil = intervals.ceiling(interval);
		if(ceil != null && ceil.contains(interval))
			return true;
		return false;
	}
	
	public boolean contains(long time){
		return contains(TimeInterval.getTimePoint(time));
	}
	
	public Iterator<TimeInterval> getIterator(){
		return intervals.iterator();
	}
	
	public Iterator<TimeInterval> getDescendingIterator(){
		return intervals.descendingIterator();
	}
	
	public TimeInterval getFirstTimeInterval(){
		return intervals.first();
	}
	
	public TimeInterval getLastTimeInterval(){
		return intervals.last();
	}
	
	public boolean isEmpty(){
		return intervals.isEmpty();
	}
	
	public int size(){
		return intervals.size();
	}
}
