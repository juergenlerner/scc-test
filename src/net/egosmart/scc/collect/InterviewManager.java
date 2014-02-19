package net.egosmart.scc.collect;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.AlterAlterDyad;
import net.egosmart.scc.data.Ego;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.gui.util.DynamicViewManager;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class InterviewManager {

	private static final String DATABASE_NAME_PREFIX = "questionnaire_db.";
	private static final int DATABASE_VERSION = 1;

	private static final String QUESTIONS_TABLE_NAME = "questions";
	private static final String QUESTIONS_COL_ID = "_ID";
	private static final String QUESTIONS_COL_NUMBER = "number";
	private static final String QUESTIONS_COL_TITLE = "title";
	private static final String QUESTIONS_COL_TEXT = "text";
	private static final String QUESTIONS_COL_TYPE = "type";
	private static final String QUESTIONS_COL_ANSWER_TYPE = "answer_type";
	private static final String QUESTIONS_TABLE_CREATE_CMD =
			"CREATE TABLE " + QUESTIONS_TABLE_NAME + " (" +
					QUESTIONS_COL_ID + " TEXT, " +
					QUESTIONS_COL_NUMBER + " INT NOT NULL, " + //is unique over all questions because different q_types start with different numbers
					QUESTIONS_COL_TITLE + " TEXT, " +
					QUESTIONS_COL_TEXT + " TEXT, " +
					QUESTIONS_COL_TYPE + " INT, " +
					QUESTIONS_COL_ANSWER_TYPE + " INT, " +
					"PRIMARY KEY (" + QUESTIONS_COL_ID + ")  );";

	private static final String ALTERS_TABLE_NAME = "alters";
	private static final String ALTERS_COL_NUMBER = "number";
	private static final String ALTERS_COL_NAME = "name";
	private static final String ALTERS_TABLE_CREATE_CMD =
			"CREATE TABLE " + ALTERS_TABLE_NAME + " (" +
					ALTERS_COL_NUMBER + " INT NOT NULL, " + 
					ALTERS_COL_NAME + " TEXT, " +
					"PRIMARY KEY (" + ALTERS_COL_NAME + ")  );";


	private static final String PROPERTIES_TABLE_NAME = "properties";
	private static final String PROPERTIES_COL_KEY = "property_key";
	private static final String PROPERTIES_COL_VALUE = "property_value";
	private static final String PROPERTIES_TABLE_CREATE_CMD =
			"CREATE TABLE " + PROPERTIES_TABLE_NAME + " (" +
					PROPERTIES_COL_KEY + " TEXT, " +
					PROPERTIES_COL_VALUE + " TEXT, " +
					"PRIMARY KEY (" + PROPERTIES_COL_KEY + ")  );";
	// these are the keys that identify the different properties
	private static final String PROPERTIES_KEY_QUESTIONNAIRE_NAME = "questionnaire_name";
	private static final String PROPERTIES_KEY_MINALTERS = "minimum_alters";
	private static final String PROPERTIES_KEY_MAXALTERS = "maximum_alters";
	private static final String PROPERTIES_KEY_ALTERMODE = "alter_mode";
	private static final String PROPERTIES_KEY_STUDY_ID = "study_id";
	private static final String PROPERTIES_KEY_NUMALTERS_BY_RESPONSE = "numalters_response";
	private static final String PROPERTIES_KEY_CURRENT_QUESTION_NUMBER = "current_question_number";
	private static final String PROPERTIES_KEY_NUMQUESTIONS = "number_of_questions";
	private static final String PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER = "current_first_alter_number";
	private static final String PROPERTIES_KEY_CURRENT_SECOND_ALTER_NUMBER = "current_second_alter_number";

	private static final String CHOICES_TABLE_NAME = "choices";
	private static final String CHOICES_COL_QUESTION_ID = "question_id";
	private static final String CHOICES_COL_INDEX = "choice_index";
	private static final String CHOICES_COL_VALUE = "value";
	private static final String CHOICES_COL_ADJACENT = "adjacent";
	private static final String CHOICES_COL_TEXT_VALUE = "text_value";
	private static final String CHOICES_TABLE_CREATE_CMD =
			"CREATE TABLE " + CHOICES_TABLE_NAME + " (" +
					CHOICES_COL_QUESTION_ID + " TEXT, " +
					CHOICES_COL_INDEX + " INT, " +
					CHOICES_COL_VALUE + " INT, " +
					CHOICES_COL_TEXT_VALUE + " TEXT, " +
					CHOICES_COL_ADJACENT + " INT, " + //TODO: check this! the type of adjacent is boolean
					"PRIMARY KEY (" + CHOICES_COL_QUESTION_ID + ", " + CHOICES_COL_INDEX + "), " +
					"FOREIGN KEY (" + CHOICES_COL_QUESTION_ID + ") REFERENCES " + QUESTIONS_TABLE_NAME + " (" + QUESTIONS_COL_ID + ") );";


	//constant integer values for the different question types
	public static final int Q_ABOUT_EGO = 1;
	public static final int Q_NAME_GENERATOR = 2;
	public static final int Q_ABOUT_ALTERS = 3;
	public static final int Q_ALTER_ALTER_TIES = 4;

	//constant integer values for the different answer types
	public static final int ANSWER_TYPE_CHOICE = 0;
	public static final int ANSWER_TYPE_NUMBER = 1;
	public static final int ANSWER_TYPE_TEXT = 2;

	private SCCMainActivity activity;

	private static InterviewManager instance;
	private SQLiteDatabase db;
	
	private static EgonetQuestionnaireFile currentStudy;
	private static boolean interviewLoaded;

	private InterviewManager(SCCMainActivity activity){
		this.activity = activity;
		QuestionnaireDBOpenHelper helper = new QuestionnaireDBOpenHelper(activity);
		db = helper.getWritableDatabase();
		interviewLoaded = false;
	}


	public static InterviewManager getInstance(SCCMainActivity activity){
		if(instance == null)
			instance = new InterviewManager(activity);
		return instance;
	}

	public void initFromFile(EgonetQuestionnaireFile qFile){
		clearAllTables();
		setProperty(PROPERTIES_KEY_STUDY_ID, Long.toString(qFile.studyId()));
		setProperty(PROPERTIES_KEY_QUESTIONNAIRE_NAME, qFile.name());
		setProperty(PROPERTIES_KEY_MAXALTERS, Integer.toString(qFile.maxNumberOfAlters()));
		setProperty(PROPERTIES_KEY_MINALTERS, Integer.toString(qFile.minNumberOfAlters()));
		setProperty(PROPERTIES_KEY_ALTERMODE, Boolean.toString(qFile.getUnlimitedMode()));
		setProperty(PROPERTIES_KEY_NUMALTERS_BY_RESPONSE, Integer.toString(0));
		PersonalNetwork network = PersonalNetwork.getInstance(activity);
		LinkedHashMap<Long, Question> questions = qFile.getQuestionsInOrder();
		int number = 0;
		for(Long id: questions.keySet()){
			Question question = questions.get(id);
			if(!question.title().startsWith(PersonalNetwork.ATTRIBUTE_PREFIX_EGOSMART)){
				String idStr = Long.toString(id);
				addQuestion(idStr, number, question.title(), question.text(), question.type(), question.answerType());
				if(question.answerType() == ANSWER_TYPE_CHOICE){
					HashMap<Integer,AnswerChoice> choices = question.answerChoices();
					for(Integer index : choices.keySet()){
						AnswerChoice choice = choices.get(index);
						addAnswerChoice(idStr, choice.index(), choice.value(), choice.adjacent(), choice.textValue());
					}
				}
				//potentially enter this question as an attribute in the personal network
				//(not for name generator questions)
				int attrType = 0;
				LinkedHashSet<String> choices = null; 
				if(question.answerType() == ANSWER_TYPE_TEXT)
					attrType = PersonalNetwork.ATTRIB_TYPE_TEXT;
				if(question.answerType() == ANSWER_TYPE_NUMBER)
					attrType = PersonalNetwork.ATTRIB_TYPE_NUMBER;
				if(question.answerType() == ANSWER_TYPE_CHOICE){
					attrType = PersonalNetwork.ATTRIB_TYPE_FINITE_CHOICE;
					choices = question.answerChoiceTextValuesAsSet();
				}
				if(question.type() == Q_ABOUT_EGO){
					network.addAttribute(PersonalNetwork.DOMAIN_EGO, 
							question.title(), question.text(), attrType,
							PersonalNetwork.DYAD_DIRECTION_SYMMETRIC,
							PersonalNetwork.ATTRIBUTE_DYNAMIC_TYPE_STATE);
					if(question.answerType() == ANSWER_TYPE_CHOICE)
						network.setAttributeChoices(PersonalNetwork.DOMAIN_EGO,
								question.title(), choices);
				}
				if(question.type() == Q_ABOUT_ALTERS){
					network.addAttribute(PersonalNetwork.DOMAIN_ALTER,
							question.title(), question.text(), attrType,
							PersonalNetwork.DYAD_DIRECTION_SYMMETRIC,
							PersonalNetwork.ATTRIBUTE_DYNAMIC_TYPE_STATE);
					if(question.answerType() == ANSWER_TYPE_CHOICE)
						network.setAttributeChoices(PersonalNetwork.DOMAIN_ALTER,
								question.title(), choices);
				}
				if(question.type() == Q_ALTER_ALTER_TIES){
					network.addAttribute(PersonalNetwork.DOMAIN_ALTER_ALTER,
							question.title(),  
							question.text(), attrType, PersonalNetwork.DYAD_DIRECTION_SYMMETRIC,
							PersonalNetwork.ATTRIBUTE_DYNAMIC_TYPE_STATE);
					if(question.answerType() == ANSWER_TYPE_CHOICE)
						network.setAttributeChoices(PersonalNetwork.DOMAIN_ALTER_ALTER,
								question.title(), choices);
				}
				++number;
			}
		}
		setProperty(PROPERTIES_KEY_NUMQUESTIONS, Integer.toString(number));
		if(number > 0)
			setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(0));
	}
	

	/**
	 * Sets the current loaded study file. It's used in loading interviews associated with this study.
	 * @param study
	 */
	public void setCurrentStudy(EgonetQuestionnaireFile study) {
		currentStudy = study;
	}

	/**
	 * Gets the study loaded currently
	 */
	public EgonetQuestionnaireFile getCurrentStudy() {
		return currentStudy;
	}
	
	/**
	 * Returns true if last try of load an interview has worked.
	 */
	public boolean isLastInterviewLoaded() {
		return interviewLoaded;
	}
	
	public String[] getAlterNames(){
		String orderBy = ALTERS_COL_NUMBER + " DESC";
		Cursor c = db.query(ALTERS_TABLE_NAME, null, null, null, null, null, orderBy);
		if(c == null || !c.moveToFirst()){
			return null;
		}
		String[] names = new String[c.getCount()];
		int i = 0;
		while(!c.isAfterLast()){
			names[i] = c.getString(c.getColumnIndexOrThrow(ALTERS_COL_NAME));
			++i;
			c.moveToNext();
		}
		if(c != null)
			c.close();
		return names;
	}

	public boolean hasAlter(String alterName){
		String selection = ALTERS_COL_NAME + "= ?";
		String[] args = {alterName};
		Cursor c = db.query(ALTERS_TABLE_NAME, null, selection, args, null, null, null);
		boolean ret = false;
		if(c != null){
			ret = c.getCount() > 0;
			c.close();
		}
		return ret;
	}

	public void addAlter(String alterName){
		if(hasAlter(alterName)) {
			activity.reportInfo("alter " + alterName + " is already included");
			return;
		}
		int curr_num_alters = getNumberOfAltersByResponse();
		ContentValues values = new ContentValues();
		values.put(ALTERS_COL_NUMBER, curr_num_alters);
		values.put(ALTERS_COL_NAME, alterName);
		db.insert(ALTERS_TABLE_NAME, null, values);
		if(curr_num_alters == 0)
			setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, Integer.toString(0));
		if(curr_num_alters == 1);
		setProperty(PROPERTIES_KEY_CURRENT_SECOND_ALTER_NUMBER, Integer.toString(1));
		++curr_num_alters;
		setProperty(PROPERTIES_KEY_NUMALTERS_BY_RESPONSE, Integer.toString(curr_num_alters));
	}

	public boolean shouldEnableNextQuestionButton(){
		// note that this is valid! In the last step it shows the thank you message
		return getCurrentQuestionNumber() < getNumberOfQuestions();
	}

	public boolean hasPreviousQuestion(){
		// TODO: this is only valid if there is at least one question before the first alter question!!!
		return getCurrentQuestionNumber() > 0;
	}

	public boolean hasCurrentQuestion(){
		int c = getCurrentQuestionNumber();
		int n = getNumberOfQuestions();
		return c >= 0 && c < n;
	}

	public boolean isAfterLastQuestion() {
		return getCurrentQuestionNumber() >= getNumberOfQuestions();
	}
	
	public Question getCurrentQuestion(){
		String selection = QUESTIONS_COL_NUMBER + " = ?";
		String[] args ={Integer.toString(getCurrentQuestionNumber())};
		Cursor cursor = db.query(QUESTIONS_TABLE_NAME, null, selection, args, null, null, null);
		if(cursor == null)
			return null;
		if(!cursor.moveToFirst()){
			cursor.close();
			return null;
		}
		long id = Long.parseLong(cursor.getString(cursor.getColumnIndexOrThrow(QUESTIONS_COL_ID)));
		String title = cursor.getString(cursor.getColumnIndexOrThrow(QUESTIONS_COL_TITLE));
		String text = cursor.getString(cursor.getColumnIndexOrThrow(QUESTIONS_COL_TEXT));
		int type = cursor.getInt(cursor.getColumnIndexOrThrow(QUESTIONS_COL_TYPE));
		int answerType = cursor.getInt(cursor.getColumnIndexOrThrow(QUESTIONS_COL_ANSWER_TYPE));
		cursor.close();
		Question q = new Question();
		q.setId(id);
		q.setTitle(title);
		q.setText(text);
		q.setType(type);
		q.setAnswerType(answerType);
		if(answerType == InterviewManager.ANSWER_TYPE_CHOICE){
			String choice_selection = CHOICES_COL_QUESTION_ID + " = ?";
			String[] choice_args = {Long.toString(id)};
			Cursor choice_cursor = db.query(CHOICES_TABLE_NAME, null, choice_selection, choice_args, null, null, null); 
			if(choice_cursor.moveToFirst()){
				while(!choice_cursor.isAfterLast()){
					int index = choice_cursor.getInt(choice_cursor.getColumnIndexOrThrow(CHOICES_COL_INDEX));
					int value = choice_cursor.getInt(choice_cursor.getColumnIndexOrThrow(CHOICES_COL_VALUE));
					String textValue = choice_cursor.getString(choice_cursor.getColumnIndexOrThrow(CHOICES_COL_TEXT_VALUE));
					int adjacent_int = choice_cursor.getInt(choice_cursor.getColumnIndexOrThrow(CHOICES_COL_ADJACENT));
					boolean adjacent = adjacent_int > 0;
					q.addAnswerChoice(index, value, adjacent, textValue);
					choice_cursor.moveToNext();
				}
			}
			if(choice_cursor != null)
				choice_cursor.close();
		}
		return q;
	}

	public String getCurrentFirstAlterName(){
		String selection = ALTERS_COL_NUMBER + " = ?";
		String[] args ={Integer.toString(getCurrentFirstAlterNumber())};
		Cursor cursor = db.query(ALTERS_TABLE_NAME, null, selection, args, null, null, null);
		if(cursor == null || !cursor.moveToFirst())
			return null;
		String name = cursor.getString(cursor.getColumnIndexOrThrow(ALTERS_COL_NAME));
		if(cursor != null)
			cursor.close();
		return name;
	}

	public String getCurrentSecondAlterName(){
		String selection = ALTERS_COL_NUMBER + " = ?";
		String[] args ={Integer.toString(getCurrentSecondAlterNumber())};
		Cursor cursor = db.query(ALTERS_TABLE_NAME, null, selection, args, null, null, null);
		if(cursor == null)
			return null;
		if(!cursor.moveToFirst()){
			cursor.close();
			return null;
		}
		String name = cursor.getString(cursor.getColumnIndexOrThrow(ALTERS_COL_NAME));
		cursor.close();
		return name;
	}

	private void enterResponseInDatabase(){
		if(hasCurrentQuestion()){
			PersonalNetwork network = PersonalNetwork.getInstance(activity);
			Question question = getCurrentQuestion();
			if(question.type() != Q_NAME_GENERATOR){
				if(question.answerType() == ANSWER_TYPE_NUMBER || question.answerType() == ANSWER_TYPE_TEXT){
					EditText textAnswer = DynamicViewManager.getSurveyAnswerEditText();
					if(textAnswer != null){
						String value = textAnswer.getText().toString().trim();
						if(question.answerType() == ANSWER_TYPE_NUMBER && value.length() == 0)
							return;
						if(question.type() == Q_ABOUT_EGO){
							network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
									question.title(), Ego.getInstance(), value);
						}
						if(question.type() == Q_ABOUT_ALTERS){
							network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
									question.title(), 
									Alter.getInstance(getCurrentFirstAlterName()), value);
						}
						if(question.type() == Q_ALTER_ALTER_TIES){
							String a = getCurrentFirstAlterName();
							String b = getCurrentSecondAlterName();
							//find out whether the value means adjacent or not
							boolean adjacent = false;
							Set<Integer> indicees = question.answerChoiceIndicees();
							for(Integer index : indicees){
								if(question.answerChoiceTextValue(index).equals(value)){
									adjacent = question.answerChoiceAdjacent(index);
								}
							}
							if(adjacent && !network.areAdjacentAt(TimeInterval.getCurrentTimePoint(), a, b))
								network.addToLifetimeOfTie(TimeInterval.getRightUnboundedFromNow(), a, b);
							if(!adjacent && network.areAdjacentAt(TimeInterval.getCurrentTimePoint(), a, b))
								network.removeTieAt(TimeInterval.getRightUnboundedFromNow(), a, b);
							//in any case set the dyad value
							network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(), 
									question.title(), AlterAlterDyad.getInstance(a, b), value);
						}													
						textAnswer.setText("");
					}
					InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(textAnswer.getWindowToken(), 
							InputMethodManager.HIDE_NOT_ALWAYS);
				}
			}
		}
	}

	public void gotoNextQuestion(){
		int c = getCurrentQuestionNumber();
		if(c < 0){//if we are before the first question, goto the first question 
			setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(0));
			return;
		}
		enterResponseInDatabase();
		Question q = getCurrentQuestion();
		if(q.type() == Q_ABOUT_EGO || q.type() == Q_NAME_GENERATOR){
			setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(c+1));
			if(hasCurrentQuestion()){
				Question q1 = getCurrentQuestion();
				if(q1.type() == Q_ABOUT_ALTERS || q1.type() == Q_ALTER_ALTER_TIES){
					if(getNumberOfAltersByResponse() < 1){
						// there are no alters --> there are no further questions
						setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, 
								Integer.toString(getNumberOfQuestions()));
						return;
					}
				}
			}
		}
		if(q.type() == Q_ABOUT_ALTERS){
			int curr_first_alter_number = getCurrentFirstAlterNumber();
			if(curr_first_alter_number < (getNumberOfAltersByResponse() - 1)){
				setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, 
						Integer.toString(curr_first_alter_number+1));
				return;
			} else {
				setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(c+1));
				if(hasCurrentQuestion()){
					Question q1 = getCurrentQuestion();
					setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, Integer.toString(0));
					if(q1.type() == Q_ALTER_ALTER_TIES){
						if(getNumberOfAltersByResponse() < 2){
							// there are no other alters --> there are no more questions
							setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, 
									Integer.toString(getNumberOfQuestions()));
							return;							
						}
					}
				}
			}
		}
		if(q.type() == Q_ALTER_ALTER_TIES){
			int curr_second_alter_number = getCurrentSecondAlterNumber();
			if(curr_second_alter_number < getNumberOfAltersByResponse() - 1){
				setProperty(PROPERTIES_KEY_CURRENT_SECOND_ALTER_NUMBER, 
						Integer.toString(curr_second_alter_number+1));
				return;				
			} else{
				int curr_first_alter_number = getCurrentFirstAlterNumber();
				if(curr_first_alter_number < getNumberOfAltersByResponse() -2){
					setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, 
							Integer.toString(curr_first_alter_number+1));
					setProperty(PROPERTIES_KEY_CURRENT_SECOND_ALTER_NUMBER, 
							Integer.toString(curr_first_alter_number+2));
					return;
				} else{
					//maybe we step out of the questions --> then there is no further question
					setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(c+1));
					if(hasCurrentQuestion()){
						setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, Integer.toString(0));
						setProperty(PROPERTIES_KEY_CURRENT_SECOND_ALTER_NUMBER, Integer.toString(1));
					}
				}
			}
		}
	}

	public void gotoPreviousQuestion(){
		if(!hasCurrentQuestion()){//then we stepped out of the questions on the right hand side
			setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(getNumberOfQuestions()-1));
			if(getNumberOfAltersByResponse() < 2){
				while(hasCurrentQuestion() && getCurrentQuestion().type() == Q_ALTER_ALTER_TIES){
					int c = getCurrentQuestionNumber();
					setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(c-1));
				}
			}
			if(getNumberOfAltersByResponse() < 1){
				while(hasCurrentQuestion() && getCurrentQuestion().type() == Q_ABOUT_ALTERS){
					int c = getCurrentQuestionNumber();
					setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(c-1));					
				}
			}
			return;			
		}
		enterResponseInDatabase();
		int c = getCurrentQuestionNumber();
		Question q = getCurrentQuestion();
		if(q.type() == Q_ABOUT_EGO || q.type() == Q_NAME_GENERATOR){
			setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(c-1));
			return;
		}
		if(q.type() == Q_ABOUT_ALTERS){
			//try to decrease the first alter number
			int first_alter_number = getCurrentFirstAlterNumber();
			if(first_alter_number > 0){
				setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, 
						Integer.toString(first_alter_number-1));
				return;
			} else {
				setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(c-1));
				if(hasCurrentQuestion()){
					Question q1 = getCurrentQuestion();
					if(q1.type() == Q_ABOUT_ALTERS && getNumberOfAltersByResponse() > 0){
						setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, 
								Integer.toString(getNumberOfAltersByResponse()-1));
						return;						
					}
					if(getNumberOfAltersByResponse() < 1){
						while(hasCurrentQuestion() && getCurrentQuestion().type() == Q_ABOUT_ALTERS){
							int number = getCurrentQuestionNumber();
							setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(number-1));					
						}
						return;
					}
				}
			}
		}
		if(q.type() == Q_ALTER_ALTER_TIES){
			int second_alter_number = getCurrentSecondAlterNumber();
			int first_alter_number = getCurrentFirstAlterNumber();
			if(second_alter_number > first_alter_number + 1){
				setProperty(PROPERTIES_KEY_CURRENT_SECOND_ALTER_NUMBER, 
						Integer.toString(second_alter_number-1));
				return;
			} else if(first_alter_number > 0){
				setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, 
						Integer.toString(first_alter_number-1));
				setProperty(PROPERTIES_KEY_CURRENT_SECOND_ALTER_NUMBER, 
						Integer.toString(getNumberOfAltersByResponse()-1));
				return;
			} else {
				setProperty(PROPERTIES_KEY_CURRENT_QUESTION_NUMBER, Integer.toString(c-1));
				if(hasCurrentQuestion()){
					Question q1 = getCurrentQuestion();
					if(q1.type() == Q_ALTER_ALTER_TIES && getNumberOfAltersByResponse() >= 2){
						setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, 
								Integer.toString(getNumberOfAltersByResponse()-2));
						setProperty(PROPERTIES_KEY_CURRENT_SECOND_ALTER_NUMBER, 
								Integer.toString(getNumberOfAltersByResponse()-1));
						return;
					} else if(q1.type() == Q_ABOUT_ALTERS && getNumberOfAltersByResponse() >= 1){
						setProperty(PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER, 
								Integer.toString(getNumberOfAltersByResponse()-1));				
						return; 
					}
				}
			}
		}
	}

	public void addAnswerChoice(String questionId, int index, int value, boolean adjacent, String textValue){
		//TODO: do checks
		ContentValues values = new ContentValues();
		values.put(CHOICES_COL_QUESTION_ID, questionId);
		values.put(CHOICES_COL_INDEX, index);
		values.put(CHOICES_COL_VALUE, value);
		values.put(CHOICES_COL_ADJACENT, adjacent);
		values.put(CHOICES_COL_TEXT_VALUE, textValue);
		db.insert(CHOICES_TABLE_NAME, null, values);
	}

	public void setProperty(String key, String value){
		ContentValues values = new ContentValues();
		values.put(PROPERTIES_COL_KEY, key);
		values.put(PROPERTIES_COL_VALUE, value);
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] args = {key};
		Cursor res = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_KEY}, 
				selection, args, null, null, null);
		if(res.getCount() > 0){
			db.update(PROPERTIES_TABLE_NAME, values, selection, args);
		} else {
			db.insert(PROPERTIES_TABLE_NAME, null, values);
		}
	}

	public String getName(){
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] args = {PROPERTIES_KEY_QUESTIONNAIRE_NAME};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, selection, 
				args, null, null, null);
		if(c == null)
			return null;
		if(!c.moveToFirst()){
			c.close();
			return null;
		}
		String ret = c.getString(c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE));
		c.close();
		return ret;
	}

	/*public int getNumberOfAltersByDesign(){
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] args = {PROPERTIES_KEY_NUMALTERS_BY_DESIGN};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, selection, 
				args, null, null, null);
		if(c == null)
			return -1;
		if(!c.moveToFirst()){
			c.close();
			return -1;
		}
		int ret = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE)));
		c.close();
		return ret;
	}*/

	public int getNumberOfAltersByResponse(){
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] args = {PROPERTIES_KEY_NUMALTERS_BY_RESPONSE};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, selection, 
				args, null, null, null);
		if(c == null)
			return -1;
		if(!c.moveToFirst()){
			c.close();
			return -1;
		}
		int ret = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE)));
		c.close();
		return ret;
	}
	
	/**
	 * Reads the content of the int file and load its data into application.
	 * 
	 * @param file Int file with the Egonet interview.
	 * @param study Ego file with the definition of the egonet study.
	 */
	public void importEgonetInterview(File file){
		SAXParserFactory saxfactory = SAXParserFactory.newInstance();
		SAXParser saxparser;
		try {
			interviewLoaded = false;
			saxparser = saxfactory.newSAXParser();
			DefaultHandler handler = new EgonetInterviewFile(activity);
			db.beginTransaction();
			saxparser.parse(file, handler);
			db.setTransactionSuccessful();
			interviewLoaded= true;
		} catch (ParserConfigurationException e) {
			Log.e("Import int", e.getMessage());
			e.printStackTrace();
		} catch (SAXException e) {
			Log.e("Import int", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("Import int", e.getMessage());
			e.printStackTrace();
		} finally {
			db.endTransaction();
		}
	}

	public int getNumberOfQuestions(){
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] args = {PROPERTIES_KEY_NUMQUESTIONS};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, selection, 
				args, null, null, null);
		if(c == null)
			return 0;
		if(!c.moveToFirst()){
			c.close();
			return 0;
		}
		int ret = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE)));
		c.close();
		return ret;
	}

	private int getCurrentFirstAlterNumber(){
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] args = {PROPERTIES_KEY_CURRENT_FIRST_ALTER_NUMBER};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, selection, 
				args, null, null, null);
		if(c == null)
			return -1;
		if(!c.moveToFirst()){
			c.close();
			return -1;
		}
		int ret = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE)));
		c.close();
		return ret;
	}

	private int getCurrentSecondAlterNumber(){
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] args = {PROPERTIES_KEY_CURRENT_SECOND_ALTER_NUMBER};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, selection, 
				args, null, null, null);
		if(c == null)
			return -1;
		if(!c.moveToFirst()){
			c.close();
			return -1;
		}
		int ret = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE)));
		c.close();
		return ret;
	}

	private int getCurrentQuestionNumber(){
		String selection = PROPERTIES_COL_KEY + " = ?";
		String[] args = {PROPERTIES_KEY_CURRENT_QUESTION_NUMBER};
		Cursor c = db.query(PROPERTIES_TABLE_NAME, new String[]{PROPERTIES_COL_VALUE}, selection, 
				args, null, null, null);
		if(c == null)
			return -1;
		if(!c.moveToFirst()){
			c.close();
			return -1;
		}
		int ret = Integer.parseInt(c.getString(c.getColumnIndexOrThrow(PROPERTIES_COL_VALUE)));
		c.close();
		return ret;
	}

	private void addQuestion(String id, int number, String title, String text, int type, int answerType){
		//TODO: do checks
		ContentValues values = new ContentValues();
		values.put(QUESTIONS_COL_ID, id);
		values.put(QUESTIONS_COL_NUMBER, number);
		values.put(QUESTIONS_COL_TITLE, title);
		values.put(QUESTIONS_COL_TEXT, text);
		values.put(QUESTIONS_COL_TYPE, type);
		values.put(QUESTIONS_COL_ANSWER_TYPE, answerType);
		db.insert(QUESTIONS_TABLE_NAME, null, values);
	}

	private void clearAllTables(){
		db.delete(QUESTIONS_TABLE_NAME, null, null);
		db.delete(CHOICES_TABLE_NAME, null, null);
		db.delete(ALTERS_TABLE_NAME, null, null);
		db.delete(PROPERTIES_TABLE_NAME, null, null);
	}
	
	private class QuestionnaireDBOpenHelper extends SQLiteOpenHelper {

		QuestionnaireDBOpenHelper(Context context) {
			super(context, DATABASE_NAME_PREFIX, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(QUESTIONS_TABLE_CREATE_CMD);
			db.execSQL(CHOICES_TABLE_CREATE_CMD);
			db.execSQL(PROPERTIES_TABLE_CREATE_CMD);
			db.execSQL(ALTERS_TABLE_CREATE_CMD);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub

		}
	}

}
