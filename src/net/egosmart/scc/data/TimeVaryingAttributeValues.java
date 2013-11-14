/**
 * 
 */
package net.egosmart.scc.data;

import java.util.NavigableMap;
import java.util.TreeMap;


/**
 * 
 * Represents the values of one attribute (for one element of its domain) over its lifetime.
 * 
 * @author juergen
 *
 */
public class TimeVaryingAttributeValues {

	private TreeMap<TimeInterval, String> valueMap;

	public TimeVaryingAttributeValues(){
		valueMap = new TreeMap<TimeInterval, String>();
	}

	/**
	 * Returns a Lifetime representing the union of time intervals that have associated values.
	 * @return
	 */
	public Lifetime getSupport(){
		return new Lifetime(valueMap.navigableKeySet());
	}

	/**
	 * 
	 * @param timePoint
	 * @return true if the support of these attribute values contains the given time point
	 */
	public boolean hasValueSetAt(long timePoint){
		TimeInterval point = TimeInterval.getTimePoint(timePoint);
		TimeInterval floor = valueMap.floorKey(point);
		if(floor != null && floor.contains(timePoint))
			return true;
		TimeInterval ceil = valueMap.ceilingKey(point);
		if(ceil != null && ceil.contains(timePoint))
			return true;
		return false;
	}
	
	/**
	 * Sets the given value for the given time interval to textValue.trim()
	 * 
	 * Overwrites any previous values within interval (keeps all values outside of interval).
	 * 
	 * If textValue is null or has length zero or is equal to PersonalNetwork.VALUE_NOT_ASSIGNED
	 * then previous values within the interval are removed but no new value is set.
	 * 
	 * Does nothing if interval is a time point that is included in a previously set interval 
	 * which is not a point. Also see the behavior of Lifetime.cutOut for this case.
	 * 
	 * @param interval
	 * @param textValue
	 */
	public void setValueAt(TimeInterval interval, String textValue){
		TimeInterval floor = valueMap.floorKey(interval);
		if(floor != null && floor.overlaps(interval)){
			if(interval.isTimePoint() && !floor.isTimePoint())
				return; //otherwise this would lead to a left-open interval
			String floorsValue = valueMap.get(floor);
			valueMap.remove(floor);
			if(floor.getStartTime() < interval.getStartTime()){
				valueMap.put(new TimeInterval(floor.getStartTime(), interval.getStartTime()), floorsValue);
			}
			if(interval.getEndTime() < floor.getEndTime()){
				valueMap.put(new TimeInterval(interval.getEndTime(), floor.getEndTime()), floorsValue);
			}
		}
		NavigableMap<TimeInterval,String> overlaping = valueMap.
				subMap(TimeInterval.getTimePoint(interval.getStartTime()), true, 
						TimeInterval.getTimePoint(interval.getEndTime()), true);
		for(TimeInterval oldInterval : overlaping.navigableKeySet()){
			if(oldInterval.overlaps(interval)){
				if(interval.isTimePoint() && !oldInterval.isTimePoint())
					return; //otherwise this would lead to a left-open interval
				String ceilsValue = overlaping.get(oldInterval);
				valueMap.remove(oldInterval);
				if(interval.getEndTime() < oldInterval.getEndTime()){
					valueMap.put(new TimeInterval(interval.getEndTime(), oldInterval.getEndTime()), ceilsValue);
				}
			}
		}
		if(textValue != null && textValue.trim().length() > 0 && !textValue.equals(PersonalNetwork.VALUE_NOT_ASSIGNED))
			valueMap.put(interval, textValue.trim());
	}

	/**
	 * Returns the value of the attribute at the given time point or PersonalNetwork.VALUE_NOT_ASSIGNED if
	 * the lifetime does not contain the given time point.
	 * @param timePoint
	 * @return
	 */
	public String getValueAt(long timePoint){
		TimeInterval floor = valueMap.floorKey(new TimeInterval(timePoint, timePoint));
		if(floor != null && floor.contains(timePoint))
			return valueMap.get(floor);
		TimeInterval ceil = valueMap.ceilingKey(new TimeInterval(timePoint, timePoint));
		if(ceil != null && ceil.contains(timePoint))
			return valueMap.get(ceil);
		return PersonalNetwork.VALUE_NOT_ASSIGNED;
	}
	
	public String getNewestValue(){
		TimeInterval time = valueMap.lastKey();
		if(time == null)
			return PersonalNetwork.VALUE_NOT_ASSIGNED;
		return valueMap.get(time);
	}

	public String getOldestValue(){
		TimeInterval time = valueMap.firstKey();
		if(time == null)
			return PersonalNetwork.VALUE_NOT_ASSIGNED;
		return valueMap.get(time);
	}

	/**
	 *
	 * @return true if and only if the support is empty that is no values is assigned for any time point.
	 */
	public boolean isEmpty(){
		return valueMap.isEmpty();
	}
	
}
