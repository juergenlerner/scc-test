package net.egosmart.scc.data;

import java.util.Iterator;
import java.util.LinkedHashMap;

import net.egosmart.scc.R;
import net.egosmart.scc.SCCMainActivity;

public class Statistics {
	
	private PersonalNetwork network;
	private SCCMainActivity activity;
	
	//Statistics
	private float manPercentage;
	private float womanPercentage;
	private float weakTiesPercentage;
	private float strongTiesPercentage;
	private float graphDensity;
			
	//TODO: save the ideal values to database.
	private float idealManPercentage;
	private float idealWomanPercentage;
	private float idealGraphDensity;
	private float idealStrongTiesPercentage;
	private float idealWeakTiesPercentage;
	
	public Statistics(PersonalNetwork network, SCCMainActivity activity) {
		this.network = network;
		this.activity = activity;
		
		//Default values at first use of app.
		idealManPercentage = (float) 0.50;
		idealWomanPercentage = (float) 0.50;
		idealGraphDensity = (float) 0.50;		
	}
	
	public float getIdealManPercentage() {
		return idealManPercentage;
	}
	
	public float getIdealWomanPercentage() {
		return idealWomanPercentage;
	}
	
	public float getIdealGraphDensity() {
		return idealGraphDensity;
	}
	
	public float getManPercentage() {
		return manPercentage;
	}
	
	public float getWomanPercentage() {
		return womanPercentage;
	}
	
	public float weakTiesPercentage() {
		return weakTiesPercentage;
	}
	
	public float getStrongTiesPercentage() {
		return strongTiesPercentage;
	}
	
	public float getGraphDensity() {
		return graphDensity;
	}
	
	public void calculateAllStatisticsAt(long timePoint) {
		calculateGraphDensityAt(timePoint);
		calculateGenderPercentageAt(timePoint);
		calculateWeakTiesPercentage(timePoint);
		calculateStrongTiesPercentage(timePoint);
	}
	
	public void calculateGenderPercentageAt(long timePoint) {
		float altersMasculine = 0;
		float altersFeminine = 0;
		String genderAttribute = activity.getString(R.string.alter_attribute_gender_name);
		//Get gender value for every alter.
		LinkedHashMap <Alter, String> genderValues = network.getValuesOfAttributeForAllElementsAt(
				timePoint, Alter.getInstance("element"), genderAttribute);
		Iterator<String> it = genderValues.values().iterator();
		while(it.hasNext()) {
			String gender = it.next();
			if(gender.equals(activity.getString(R.string.alter_attribute_gender_man)))
				altersMasculine++;
			if(gender.equals(activity.getString(R.string.alter_attribute_gender_woman)))
				altersFeminine++;			
		}
		manPercentage = altersMasculine/(altersMasculine+altersFeminine);
		womanPercentage = altersFeminine/(altersMasculine+altersFeminine);
	}
	
	public void calculateGraphDensityAt(long timePoint) {
		TimeInterval interval = new TimeInterval(timePoint, timePoint);
		float numberOfEdges = network.getNumberOfUndirectedTiesAt(interval);
		float numberOfVertices = network.getAltersAt(interval).size();
		graphDensity = (2*numberOfEdges)/(numberOfVertices*(numberOfVertices-1));
	}
	
	public void calculateWeakTiesPercentage(long timePoint) {
		
	}
	
	public void calculateStrongTiesPercentage(long timePoint) {
		
	}
}
