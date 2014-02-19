package net.egosmart.scc.collect;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.AlterAlterDyad;
import net.egosmart.scc.data.Ego;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EgonetInterviewFile extends DefaultHandler {
	//names of XML elements
	private static final String elem_interview = "Interview";
	private static final String elem_name = "Name";
	private static final String elem_question_id = "QuestionId";
	private static final String elem_string = "String";
	private static final String elem_index = "Index";
	private static final String elem_adjacent = "Adjacent";
	private static final String elem_alters = "Alters";
	
	//names of XML attributes		
	//attributes of Index
	private static final String index_name = "name";
	//attributes of Interview
	private static final String interview_study_id = "StudyId";
	
	PersonalNetwork network;
	
	String parsedValue;
	String alterResponse; //we can't add a reponse of an alter question until we know alter name. 
	String[] alterPair;
	boolean areAdjacent;
	TimeInterval interval;		
	
	Question currentQuestion;
	EgonetQuestionnaireFile study;
	InterviewManager intManager;
	
	public EgonetInterviewFile(SCCMainActivity activity) {
		network = PersonalNetwork.getInstance(activity);
		intManager = InterviewManager.getInstance(activity);
		study = intManager.getCurrentStudy();
	}

	@Override
	public void startDocument() throws SAXException {
		areAdjacent = false;
		interval = TimeInterval.getRightUnbounded(System.currentTimeMillis());
	}
			
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException{
		if(elem_interview.equals(localName))
			startInterview(atts);
		if(elem_alters.equals(localName))
			startAlters(atts);
		if(elem_index.equals(localName))
			startIndex(atts);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(elem_name.equals(localName))
			endName();
		if(elem_question_id.equals(localName))
			endQuestionId();
		if(elem_string.equals(localName))
			endString();
		if(elem_alters.equals(localName))
			endAlters();
		if(elem_adjacent.equals(localName))
			endAdjacent();
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		//TODO: could it happen that the parser calls characters several times for 'one chunk' of PCDATA?
		//TODO: maybe it is saver to call StringBuffer.apend(ch, start, length)
		parsedValue = new String(ch, start,length); 
	}
	
	private void startInterview (Attributes atts) throws SAXException {
		//Check if current study matches with the study of interview. 
		long studyIdFromInterview = Long.valueOf(atts.getValue("", interview_study_id));
		if(studyIdFromInterview != study.studyId())
			//intManager.setErrorReasonAtLoad(R.string.error_load_interview_id);
			throw new SAXException("The study loaded and the study of the interview does not match");	
	}
	
	private void startAlters(Attributes atts) {
		alterPair = new String[2];
	}
	
	private void startIndex(Attributes atts) {
		String indexNameAttribute = atts.getValue("", index_name);
		if(indexNameAttribute == null)
			return;
		if(alterPair[0] == null)
			alterPair[0] = indexNameAttribute;
		else if(alterPair[1] == null)
			alterPair[1] = indexNameAttribute;
		else 
			return;
	}
	
	private void endQuestionId() {
		long questionId = Long.valueOf(parsedValue.toString().trim());
		currentQuestion = study.getQuestion(questionId);
	}
	
	private void endString() {
		String value = parsedValue.toString().trim();
		if(currentQuestion.type() == InterviewManager.Q_ABOUT_EGO)
			network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
					currentQuestion.title(), Ego.getInstance(), value);
		if(currentQuestion.type() == InterviewManager.Q_ABOUT_ALTERS)
			alterResponse = value;
	}
	
	private void endName() {
		String alterName = parsedValue.toString().trim(); 
		network.addToLifetimeOfAlter(interval, alterName);
	}
			
	private void endAdjacent() {
		areAdjacent = Boolean.valueOf(parsedValue.toString().trim()); 
	}	
	
	private void endAlters() {
		//Now we know alter names. We can store them answers.
		if (currentQuestion.type() == InterviewManager.Q_ABOUT_ALTERS)
			network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
					currentQuestion.title(), Alter.getInstance(alterPair[0]), alterResponse);
		if (currentQuestion.type() == InterviewManager.Q_ALTER_ALTER_TIES) {
			if (areAdjacent)
				network.addToLifetimeOfTie(interval,alterPair[0], alterPair[1]);
			//Add dyad value.
			network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(), 
					currentQuestion.title(), AlterAlterDyad.getInstance(alterPair[0], alterPair[1]),
					alterResponse);
		}	
	}
}
