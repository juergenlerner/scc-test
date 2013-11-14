/**
 * 
 */
package net.egosmart.scc.data;

/**
 * A time interval represents a pair of two long values, the startTime and the endTime. These longs usually
 * encode the number of milliseconds since Jan. 1, 1970 GMT. The endTime must be greater than or
 * equal to the startTime. If they are equal, then the interval reduces to a single time point.
 * 
 * Two intervals are equal if and only if their startTimes are equal and their endTimes are equal. 
 * 
 * Time intervals are ordered lexicographically first by the startTime then (if these are equal) by the endTime. Thus,
 * it holds that two intervals I and J are equal if and only if I.compareTo(J) == 0 .
 *  
 * Two time intervals
 * I and J are said to overlap if the startTime of I is strictly before the endTime of J and the startTime
 * of J is strictly before the endTime of I or if both I and J are time points that are equal.
 * 
 * The startTime is considered
 * to be included in the interval and the endTime is considered to be excluded from the interval.
 * If the interval is a time point then this single point is included.
 * In particular, two intervals I and J for which the startTime of I is equal to the endTime of J 
 * cannot overlap unless they are points.
 * 
 * The values Long.MIN_VALUE and Long.MAX_VALUE specify (semi-)unbounded intervals. 
 * 
 * @author juergen
 *
 */
public class TimeInterval implements Comparable<TimeInterval> {

	private long start;
	private long end;
	
	/**
	 * 
	 * @param startTime the number of milliseconds since Jan. 1, 1970 GMT until the start of the interval
	 * @param endTime the number of milliseconds since Jan. 1, 1970 GMT until the end of the interval
	 */
	public TimeInterval(long startTime, long endTime){
		if(endTime < startTime)
			throw new IllegalArgumentException("endTime must be greater than or equal to startTime");
		start = startTime;
		end = endTime;
	}
	
	public static TimeInterval getCurrentTimePoint(){
		long time = System.currentTimeMillis();
		return new TimeInterval(time, time);
	}
	
	public static TimeInterval getTimePoint(long timePoint){
		return new TimeInterval(timePoint, timePoint);
	}
	
	public static TimeInterval getLeftUnbounded(long endTime){
		return new TimeInterval(Long.MIN_VALUE, endTime);
	}
	
	public static TimeInterval getLeftUnboundedUntilNow(){
		long endTime = System.currentTimeMillis();
		return new TimeInterval(Long.MIN_VALUE, endTime);
	}
	
	public static TimeInterval getRightUnbounded(long startTime){
		return new TimeInterval(startTime, Long.MAX_VALUE);
	}
	
	public static TimeInterval getRightUnboundedFromNow(){
		long startTime = System.currentTimeMillis();
		return new TimeInterval(startTime, Long.MAX_VALUE);
	}
	
	/**
	 * 
	 * @return the interval with the minimum start time and the maximum end time
	 */
	public static TimeInterval getMaxInterval(){
		return new TimeInterval(Long.MIN_VALUE, Long.MAX_VALUE);
	}
	
	/**
	 * 
	 * @return interval end time in number of milliseconds since Jan. 1, 1970 GMT.
	 */
	public long getEndTime() {
		return end;
	}

	/**
	 * 
	 * @return interval start time in number of milliseconds since Jan. 1, 1970 GMT.
	 */
	public long getStartTime() {
		return start;
	}

	public boolean isTimePoint(){
		return start == end;
	}
	
	public boolean isBounded(){
		return isRightBounded() && isLeftBounded();
	}
	
	public boolean isRightBounded(){
		return end < Long.MAX_VALUE;
	}
	
	public boolean isLeftBounded(){
		return start > Long.MIN_VALUE;
	}
	
	public boolean contains(TimeInterval interval){
		if(interval.isTimePoint() && !isTimePoint())
			return start <= interval.start && interval.end < end;
		return start <= interval.start && interval.end <= end;
	}
	
	public boolean contains(long time){
		if(isTimePoint())
			return start == time;
		return start <= time && time < end;
	}
	
	public boolean overlaps(TimeInterval anotherInterval){
		if(isTimePoint() && anotherInterval.isTimePoint())
			return start == anotherInterval.start;
		if(isTimePoint())
			return start < anotherInterval.end && anotherInterval.start <= end;
		if(anotherInterval.isTimePoint())
			return start <= anotherInterval.end && anotherInterval.start < end;
		return start < anotherInterval.end && anotherInterval.start < end;
	}
	
	public boolean overlapsOrIsContiguousWith(TimeInterval anotherInterval){
		return start <= anotherInterval.end && anotherInterval.start <= end;
	}
	
	public TimeInterval getUnionWithContiguous(TimeInterval anotherInterval){
		if(!overlapsOrIsContiguousWith(anotherInterval))
			throw new IllegalArgumentException("getUnionWithContiguous can " +
					"be called only for overlapping or contiguous intervals");
		long newStart = Math.min(start, anotherInterval.start);
		long newEnd = Math.max(end, anotherInterval.end);
		return new TimeInterval(newStart, newEnd);
	}
	
	/**
	 * 
	 * @return the length of the interval, that is end time minus start time.
	 */
	public long duration(){
		return end - start;
	}
	
	/**
	 * 
	 */
	public boolean equals(Object o){
		if(!(o instanceof TimeInterval))
			return false;
		TimeInterval otherInterval = (TimeInterval) o;
		return start == otherInterval.start && end == otherInterval.end;
	}
	
	/** 
	 * 
	 */
	@Override
	public int compareTo(TimeInterval anotherInterval) {
		if(start < anotherInterval.start)
			return -1;
		if(start > anotherInterval.start)
			return 1;
		// now the start times are identical
		if(end < anotherInterval.end)
			return -1;
		if(end > anotherInterval.end)
			return 1;
		return 0;
	}

	/**
	 * 
	 */
	public int hashCode(){
		return  Long.valueOf(start).hashCode() + Long.valueOf(end).hashCode();
	}
}
