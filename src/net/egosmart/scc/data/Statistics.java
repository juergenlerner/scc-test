package net.egosmart.scc.data;

import java.util.Iterator;
import java.util.LinkedHashMap;

import net.egosmart.scc.R;
import net.egosmart.scc.SCCMainActivity;

public class Statistics {
	//References 
	private PersonalNetwork network;
	private SCCMainActivity activity;
	//Statistics
	private float manPercentage;
	private float womanPercentage;
	private float weakTiesPercentage;
	private float strongTiesPercentage;
	private float graphDensity;
	//Values for the ideal case.		
	private float idealManPercentage;
	private float idealWomanPercentage;
	private float idealGraphDensity;
	
	public Statistics(PersonalNetwork network, SCCMainActivity activity) {
		this.network = network;
		this.activity = activity;
		//Default values
		graphDensity = 0;
		manPercentage = 0;
		womanPercentage = 0;
		//Get ideal case from SCCProperties.
		idealManPercentage = SCCProperties.getInstance(activity).getIdealValueMaleStatistics();
		idealWomanPercentage = SCCProperties.getInstance(activity).getIdealValueFemaleStatistics();
		idealGraphDensity = SCCProperties.getInstance(activity).getIdealValueDensityStatistics();		
	}
	
	public float getIdealMalePercentage() {
		return idealManPercentage;
	}
	
	public float getIdealFemalePercentage() {
		return idealWomanPercentage;
	}
	
	public float getIdealGraphDensity() {
		return idealGraphDensity;
	}
	
	public void setIdealGraphDensity(float value) {
		SCCProperties.getInstance(activity).setIdealValueDensityStatistics(value);
	}
	
	public void setIdealFemalePercentage(float value) {
		SCCProperties.getInstance(activity).setIdealValueFemaleStatistics(value);
	}
	
	public void setIdealMalePercentage(float value) {
		SCCProperties.getInstance(activity).setIdealValueMaleStatistics(value);
	}
	
	public float getMalePercentage() {
		return manPercentage;
	}
	
	public float getFemalePercentage() {
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
			if(gender.equals(activity.getString(R.string.alter_attribute_gender_male)))
				altersMasculine++;
			if(gender.equals(activity.getString(R.string.alter_attribute_gender_female)))
				altersFeminine++;			
		}
		//Just checking is not 0/0 (NaN).
		if(altersMasculine > 0 || altersFeminine > 0 ) {
			manPercentage = altersMasculine/(altersMasculine+altersFeminine);
			womanPercentage = altersFeminine/(altersMasculine+altersFeminine);
		}
	}
	
	public void calculateGraphDensityAt(long timePoint) {
		TimeInterval interval = new TimeInterval(timePoint, timePoint);
		float numberOfEdges = network.getNumberOfUndirectedTiesAt(interval);
		float numberOfVertices = network.getAltersAt(interval).size();
		if(numberOfVertices > 0)
			graphDensity = (2*numberOfEdges)/(numberOfVertices*(numberOfVertices-1));
	}
	
	public void calculateWeakTiesPercentage(long timePoint) {
		
	}
	
	public void calculateStrongTiesPercentage(long timePoint) {
		
	}
}
