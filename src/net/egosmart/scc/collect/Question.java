package net.egosmart.scc.collect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class Question {

	private long id;
	private int type;
	private int answerType;
	private String title;
	private String text;
	//answer choices identified by their index
	private HashMap<Integer,AnswerChoice> choices;//only if answerType == Study.ANSWER_TYPE_CHOICE

	public Question(){
		//set three values to smaller than zero to check if they are not defined in the file 
		id = -1L;
		type = -1;
		answerType = -1;
	}

	public void setId(long id){
		this.id = id;
	}

	public void setType(int type){
		if(type !=  Questionnaire.Q_ABOUT_EGO && type !=  Questionnaire.Q_NAME_GENERATOR && type !=  Questionnaire.Q_ABOUT_ALTERS && type !=  Questionnaire.Q_ALTER_ALTER_TIES)
			throw new IllegalArgumentException("unknown question type: " + type);
		this.type = type;
	}

	public void setAnswerType(int answerType){
		if(answerType !=  Questionnaire.ANSWER_TYPE_CHOICE && answerType !=  Questionnaire.ANSWER_TYPE_NUMBER && answerType !=  Questionnaire.ANSWER_TYPE_TEXT)
			throw new IllegalArgumentException("unknown answer type: " + answerType);
		this.answerType = answerType;
		if(this.answerType == Questionnaire.ANSWER_TYPE_CHOICE)
			choices = new HashMap<Integer,AnswerChoice>();
	}

	public void setTitle(String title){
		this.title = title;
	}

	//means the question text
	public void setText(String text){
		this.text = text;
	}
	//for each <AnswerText> element (subelements of <Answers>)
	//only allowed if answer typ is Study.ANSWER_TYPE_CHOICE
	public void addAnswerChoice(int index, int value, boolean adjacent, String textValue){
		if(this.answerType != Questionnaire.ANSWER_TYPE_CHOICE)
			throw new IllegalStateException("Question.addAnswerChoice may only be called if answer type is Questionnaire.ANSWER_TYPE_CHOICE");
		choices.put(new Integer(index), new AnswerChoice(index, value, adjacent, textValue));
	}

	public long id(){
		return id;
	}

	public int type(){
		return type;
	}

	public int answerType(){
		return answerType;
	}

	public String title(){
		return title;
	}

	public String text(){
		return text;
	}

	//get the value of a specific answer choice 
	//only allowed if answer typ is Study.ANSWER_TYPE_CHOICE
	public String answerChoiceTextValue(int index){
		if(this.answerType != Questionnaire.ANSWER_TYPE_CHOICE)
			throw new IllegalStateException("Question.answerChoiceValue may only be called if answer type is Questionnaire.ANSWER_TYPE_CHOICE");
		return choices.get(index).textValue();
	}
	
	//only allowed if answer typ is Study.ANSWER_TYPE_CHOICE
	public boolean answerChoiceAdjacent(int index){
		if(this.answerType != Questionnaire.ANSWER_TYPE_CHOICE)
			throw new IllegalStateException("Question.answerChoiceValue may only be called if answer type is Questionnaire.ANSWER_TYPE_CHOICE");
		return choices.get(index).adjacent();
	}
	
	public Set<Integer> answerChoiceIndicees(){
		if(this.answerType != Questionnaire.ANSWER_TYPE_CHOICE)
			throw new IllegalStateException("Question.answerChoiceValue may only be called if answer type is Questionnaire.ANSWER_TYPE_CHOICE");
		return choices.keySet();
	}
	
	public HashMap<Integer,AnswerChoice> answerChoices(){
		if(this.answerType != Questionnaire.ANSWER_TYPE_CHOICE)
			throw new IllegalStateException("Question.answerChoiceValue may only be called if answer type is Questionnaire.ANSWER_TYPE_CHOICE");
		return choices;
	}
	
	public LinkedHashSet<String> answerChoiceTextValuesAsSet(){
		if(this.answerType != Questionnaire.ANSWER_TYPE_CHOICE)
			throw new IllegalStateException("Question.answerChoiceValue may only be called if answer type is Questionnaire.ANSWER_TYPE_CHOICE");
		LinkedHashSet<String> ret = new LinkedHashSet<String>();
		for(AnswerChoice ac : choices.values()){
			ret.add(ac.textValue());
		}
		return ret;
	}
	
	public String[] answerChoiceTextValues(){
		if(this.answerType != Questionnaire.ANSWER_TYPE_CHOICE)
			throw new IllegalStateException("Question.answerChoiceValue may only be called if answer type is Questionnaire.ANSWER_TYPE_CHOICE");
		int maxIndex = -1;
		for(int index : choices.keySet()){
			if(index > maxIndex)
				maxIndex = index;
		}
		//sanity check
		if(maxIndex + 1 != choices.size())
			throw new IllegalStateException("indicees of answer choice are not contingent");
		String[] ret = new String[choices.size()];
		for(Integer index: choices.keySet()){
			ret[index] = choices.get(index).textValue();
		}
		return ret;
	}

}

class AnswerChoice{

	private int index;
	private int value;//scale of measurement unclear (ordered, binary, nominal, ...)
	private boolean adjacent;//only relevant for alter-alter tie questions
	private String textValue;

	protected AnswerChoice(int index, int value, boolean adjacent, String textValue){
		this.index = index;
		this.value = value;
		this.adjacent = adjacent;
		this.textValue = textValue;
	}

	protected int index(){
		return index;
	}
	protected int value(){
		return value;
	}
	protected boolean adjacent(){
		return adjacent;
	}
	protected String textValue(){
		return textValue;
	}
}