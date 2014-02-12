package net.egosmart.scc.collect;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import android.util.Log;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class EgonetQuestionnaireFile extends DefaultHandler{


	//name of the study (not important)
	private String name;
	//id of the study
	private long id;
	
	//in new versions, number of alters can be undeterminated, or in a range between
	//a minimum and maximum. Setting this min and max at same value means a fixed number of alters.
	private int minalters;
	private int maxalters;
	
	//if alterModeUnlimited is true, there is no maximum number of alters in the study. If false,
	//will be a maximum number of alters. always must be a minimum.
	private boolean altermodeunlimited;

	//altersamplingmodel and altersamplingparameter are not read (what are they for?)

	//BTW, the longs are the question ids (which are indeed longer than integer allows)
	private LinkedList<Long> egoQuestionOrder;
	private LinkedList<Long> nameGeneratorQuestionOrder;
	private LinkedList<Long> alterQuestionOrder;
	private LinkedList<Long> tieQuestionOrder;

	//that's the main information: all questions accessible by their ids (long)
	private HashMap<Long,Question> questions;

	//study definition files typically end with .ego
	public EgonetQuestionnaireFile(File studyDefinitionFile){
		//TODO: when is this necessary?
		System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
		try{
// alternative
//			SAXParserFactory saxfactory = SAXParserFactory.newInstance();
//			SAXParser saxparser = saxfactory.newSAXParser();
//			saxparser.parse(file, DefaultHandler);
			
			//even better:
			//android.util.XML und
			//android.sax.RootElement; *.Element, etc.

			//yet even better:
			//org.xmlpull.v1.XmlPullParser parser
			
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(this);
			reader.parse(new InputSource(new FileInputStream(studyDefinitionFile)));
		}catch(SAXException e){
			System.err.println("initialization failed");
			System.err.println("SAXException: " + e.getMessage());
			e.printStackTrace(System.err);
			Log.e("QuestionnaireFile", e.getMessage());
		}catch(IOException e){
			System.err.println("initialization failed");
			System.err.println("IOException: " + e.getMessage());
			e.printStackTrace(System.err);
			Log.e("QuestionnaireFile", e.getMessage(), e);
		}
	}

	public String name(){
		return name;
	}

	public Question getQuestion(long id){
		return questions.get(id);
	}

	/*
    public int getNumberOfAlterQuestions(){
	return alterQuestionOrder.size();
    }
	 */
	
	public long studyId() {
		return id;
	}
	
	public int maxNumberOfAlters(){
		return maxalters;
	}

	public int minNumberOfAlters(){
		return minalters;
	}
	
	public boolean getUnlimitedMode(){
		return altermodeunlimited;
	}
	
	public LinkedHashMap<Long, Question> getQuestionsInOrder(){
		LinkedHashMap<Long, Question> ret = getEgoQuestions();
		ret.putAll(getNameGeneratorQuestions());
		ret.putAll(getAlterQuestions());
		ret.putAll(getTieQuestions());
		return ret;
	}
	
	public LinkedHashMap<Long,Question> getEgoQuestions(){
		LinkedHashMap<Long, Question> egoQuestions = new LinkedHashMap<Long, Question>();
		for(Iterator<Long> it = egoQuestionOrder.iterator(); it.hasNext(); ){
			Long id = it.next();
			egoQuestions.put(id, questions.get(id));
		}
		return egoQuestions;
	}

	public LinkedHashMap<Long,Question> getNameGeneratorQuestions(){
		LinkedHashMap<Long, Question> nameGeneratorQuestions = new LinkedHashMap<Long, Question>();
		for(Iterator<Long> it = nameGeneratorQuestionOrder.iterator(); it.hasNext(); ){
			Long id = it.next();
			nameGeneratorQuestions.put(id, questions.get(id));
		}
		return nameGeneratorQuestions;
	}

	public LinkedHashMap<Long,Question> getAlterQuestions(){
		LinkedHashMap<Long, Question> alterQuestions = new LinkedHashMap<Long, Question>();
		for(Iterator<Long> it = alterQuestionOrder.iterator(); it.hasNext(); ){
			Long id = it.next();
			alterQuestions.put(id, questions.get(id));
		}
		return alterQuestions;
	}

	public LinkedHashMap<Long,Question> getTieQuestions(){
		LinkedHashMap<Long, Question> tieQuestions = new LinkedHashMap<Long, Question>();
		for(Iterator<Long> it = tieQuestionOrder.iterator(); it.hasNext(); ){
			Long id = it.next();
			tieQuestions.put(id, questions.get(id));
		}
		return tieQuestions;
	}

	///////////////////////////////////////////////
	//variables and methods for XML parsing////////
	///////////////////////////////////////////////

	//names of XML elements
	private static final String elem_package = "Package";
	private static final String elem_name = "name";
	private static final String elem_altermodeunlimited = "altermodeunlimited";
	private static final String elem_minalters = "minalters";
	private static final String elem_maxalters = "maxalters";
	private static final String elem_questionorder = "questionorder";
	private static final String elem_id = "id";// <id> element within <questionorder>
	private static final String elem_QuestionList = "QuestionList";
	private static final String elem_Question = "Question";
	private static final String elem_Id = "Id";//NO KIDDING! <Id> element within <Question>
	private static final String elem_QuestionType = "QuestionType";
	private static final String elem_AnswerType = "AnswerType";
	private static final String elem_QuestionTitle = "QuestionTitle";
	private static final String elem_QuestionText = "QuestionText";
	private static final String elem_Link = "Link";
	private static final String elem_AnswerText = "AnswerText";

	//names of XML attributs
	private static final String attr_id = "Id";
	private static final String attr_questiontype = "questiontype";//attribute of questionorder;
	private static final String attr_index = "index";//attribute of AnswerText
	private static final String attr_value = "value";//attribute of AnswerText
	private static final String attr_adjacent = "adjacent";//attribute of AnswerText

	//holds the current parsed character data if the parser is within a leaf element;
	//if the parser is anywhere else, the content is undefined, or curr_pcd might be null
	private StringBuffer curr_pcd;

	private LinkedList<Long> questionOrderCurrentlyToFillWithIds;//;-)

	private Question currentQuestion;

	private int curr_index;
	private int curr_value;
	private boolean curr_adjacent;

	private boolean withinLink;

	public void startDocument(){
		//System.out.println("start parsing study definition file");
		withinLink = false;
	}

	public void startElement(String uri, String localName, String qName, Attributes atts){
		curr_pcd = new StringBuffer();//empties the current parsed character data
		if(localName.equals(elem_package))
			startPackage(atts);
		if(localName.equals(elem_questionorder))
			startQuestionOrderElement(atts);
		if(localName.equals(elem_QuestionList))
			questions = new HashMap<Long, Question>();
		if(localName.equals(elem_Question))
			currentQuestion = new Question();
		if(localName.equals(elem_AnswerText))
			startAnswerTextElement(atts);
		if(localName.equals(elem_Link))
			withinLink = true;
	}

	public void endElement(String uri, String localName, String qName){
		//at the end of the leaf elements, the current parsed character data has to be saved in some variables, 
		if(localName.equals(elem_name))//name element occurs only once (is name of the study)
			name = curr_pcd.toString();
		if(localName.equals(elem_altermodeunlimited))
			altermodeunlimited = Boolean.parseBoolean(curr_pcd.toString());
		if(localName.equals(elem_minalters))
			minalters = Integer.parseInt(curr_pcd.toString());
		if(localName.equals(elem_maxalters))
			maxalters = Integer.parseInt(curr_pcd.toString());
		if(localName.equals(elem_id))// <id> element within <questionorder>
			questionOrderCurrentlyToFillWithIds.add(new Long(curr_pcd.toString()));
		if(localName.equals(elem_Question))//finished parsing of <Question> element 
			currentQuestion = null;//not important but maybe saver
		if(localName.equals(elem_Link))
			withinLink = false;
		if(localName.equals(elem_Id)){//that's the <Id> of a <Question> element (not to be confused with <id>)
			if(!withinLink){//have to check this since <Id> within Link means that the question is only to be answered conditionally on some other question TREAT THIS CASE LATER
				Long questionId = new Long(curr_pcd.toString());
				questions.put(questionId, currentQuestion);//only now can we put the current question into the list
				currentQuestion.setId(questionId.longValue());
			}
		}
		if(localName.equals(elem_QuestionType))
			currentQuestion.setType(Integer.parseInt(curr_pcd.toString()));
		if(localName.equals(elem_AnswerType))
			currentQuestion.setAnswerType(Integer.parseInt(curr_pcd.toString()));
		if(localName.equals(elem_QuestionTitle))
			currentQuestion.setTitle(curr_pcd.toString());
		if(localName.equals(elem_QuestionText))
			currentQuestion.setText(curr_pcd.toString());
		if(localName.equals(elem_AnswerText)){
			currentQuestion.addAnswerChoice(curr_index, curr_value, curr_adjacent, curr_pcd.toString());
		}
	}

	//called whenever the parser meets parsed character data
	public void characters(char[] ch, int start, int length){
		curr_pcd.append(ch, start, length);//has the same signature as characters (luckily!)
	}

	public void endDocument(){
		/*
	System.out.println("finished parsing");
	System.out.println("study name = " + name);
	System.out.println("number of alters = " + numalters);
	System.out.println("found " + egoQuestionOrder.size() + " questions about ego");
	System.out.println("found " + nameGeneratorQuestionOrder.size() + " name generator questions");
	System.out.println("found " + alterQuestionOrder.size() + " questions about alters");
	System.out.println("found " + tieQuestionOrder.size() + " alter-alter tie questions");
	System.out.println("together found " + questions.size() + " questions");
		 */
	}

	private void startAnswerTextElement(Attributes atts){
		curr_index = Integer.parseInt(atts.getValue(attr_index));
		curr_value = Integer.parseInt(atts.getValue(attr_value));
		curr_adjacent = Boolean.parseBoolean(atts.getValue(attr_adjacent));
	}

	private void startPackage(Attributes atts) {
		String idStudy = atts.getValue("", attr_id);
		id = Long.parseLong(idStudy);
	}
	
	private void startQuestionOrderElement(Attributes atts){
		int question_type = Integer.parseInt(atts.getValue(attr_questiontype));
		if(question_type == Questionnaire.Q_ABOUT_EGO){
			egoQuestionOrder = new LinkedList<Long>();
			questionOrderCurrentlyToFillWithIds = egoQuestionOrder;
		}
		if(question_type == Questionnaire.Q_NAME_GENERATOR){
			nameGeneratorQuestionOrder = new LinkedList<Long>();
			questionOrderCurrentlyToFillWithIds = nameGeneratorQuestionOrder;
		}
		if(question_type == Questionnaire.Q_ABOUT_ALTERS){
			alterQuestionOrder = new LinkedList<Long>();
			questionOrderCurrentlyToFillWithIds = alterQuestionOrder;
		}
		if(question_type == Questionnaire.Q_ALTER_ALTER_TIES){
			tieQuestionOrder = new LinkedList<Long>();
			questionOrderCurrentlyToFillWithIds = tieQuestionOrder;
		}
	}

}
