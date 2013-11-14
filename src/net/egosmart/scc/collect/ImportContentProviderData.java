package net.egosmart.scc.collect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.database.Cursor;
import android.database.CursorJoiner;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import net.egosmart.scc.R;
import net.egosmart.scc.SCCMainActivity;
import net.egosmart.scc.data.Alter;
import net.egosmart.scc.data.AlterAlterDyad;
import net.egosmart.scc.data.Ego;
import net.egosmart.scc.data.EgoAlterDyad;
import net.egosmart.scc.data.PersonalNetwork;
import net.egosmart.scc.data.TimeInterval;
import net.egosmart.scc.gui.util.AlterListBaseAdapter;

public class ImportContentProviderData {

	/**
	 * Fill the given list with alters that can be added to the network.
	 * 
	 * The suggested alters have at least one phone call or one attendance to a calendar events. 
	 * 
	 * Not more than maxAlters will be filled in.
	 * 
	 * @param activity
	 * @param altersList
	 */
	public static void fillAlterSuggestions(final SCCMainActivity activity, ListView altersList, int maxAlters){
		PersonalNetwork network = PersonalNetwork.getInstance(activity);
		HashSet<String> altersToAdd = new HashSet<String>();
		// add contact names with at least one phone call
		Uri uri = CallLog.Calls.CONTENT_URI;
		String sortOrder = CallLog.Calls.DATE + " DESC";
		Cursor cursor = activity.getContentResolver().query(uri, new String[] { CallLog.Calls.DATE, 
				CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER}, 
				null, null, sortOrder);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToPosition(-1);
			while (cursor.moveToNext() && altersToAdd.size() < maxAlters){
				int col_of_number = cursor.getColumnIndex(CallLog.Calls.NUMBER);
				String name = null;
				if(!cursor.isNull(col_of_number)){
					String number = cursor.getString(col_of_number).trim();
					if(number != null && number.length() > 0){
						name =  getDisplayNameByPhoneNumber(activity, number);
					}
				} 
				if(name == null){//try the cached name
					int col_cached_name = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
					if(!cursor.isNull(col_cached_name)){
						name = cursor.getString(col_cached_name).trim();
					}
				}
				if(name != null && name.length() > 0 && !network.hasAlterAt(
						TimeInterval.getMaxInterval(),name)){
					altersToAdd.add(name);
				}
			}
		}
		if(cursor != null)
			cursor.close();
		// add contact names with at least one calendar event
		uri = CalendarContract.Attendees.CONTENT_URI;
		cursor = activity.getContentResolver().query(uri, new String[] { CalendarContract.Attendees.ATTENDEE_EMAIL,
				CalendarContract.Attendees.ATTENDEE_NAME}, 
				null, null, null);
		if (cursor.getCount() > 0) {
			cursor.moveToPosition(-1);
			while (cursor.moveToNext() && altersToAdd.size() < maxAlters){
				int col_of_email = cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_EMAIL);
				int col_of_name = cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME);
				String name = null;
				if (cursor.isNull(col_of_name)) {
					if(!cursor.isNull(col_of_email)){
						name = cursor.getString(col_of_email);
						//name = getContactNameByEmail(activity, name);
					}
				} else {
					name = cursor.getString(col_of_name);
				}
				if(name != null && name.trim().length() > 0){
					String egoDisplayName = network.getAttributeValueAt(
							System.currentTimeMillis(), 
							PersonalNetwork.EGO_ATTRIBUTE_NAME_EGO_DISPLAY_NAME,
							Ego.getInstance());
					if(egoDisplayName == null || PersonalNetwork.VALUE_NOT_ASSIGNED.equals(egoDisplayName)){
						egoDisplayName = importEgoDisplayName(activity);
					}
					name = name.trim();
					if(	(egoDisplayName == null || !name.equals(egoDisplayName)) && !network.hasAlterAt(
							TimeInterval.getMaxInterval(), name)){
						altersToAdd.add(name);
					}
				}
			}
		}
		if(cursor != null)
			cursor.close();
		final ArrayList<Pair<String, String>> nameAttrPairs = new ArrayList<Pair<String,String>>();
		for(String alterToAdd : altersToAdd){
			// construct some descriptive string for the alter
			nameAttrPairs.add(new Pair<String, String>(alterToAdd, ""));
		}
		final BaseAdapter adapter = new AlterListBaseAdapter(activity, nameAttrPairs);
		altersList.setAdapter(adapter);
		altersList.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
				Pair<String, String> pair = (Pair<String, String>) parent.getItemAtPosition(position);
				importDataForAlterByName(activity, pair.first);
				nameAttrPairs.remove(position);
				adapter.notifyDataSetChanged();
				activity.updatePersonalNetworkViews();
			}
		});

	}

	private static String getDisplayNameByPhoneNumber(SCCMainActivity activity, String phoneNumber){
		Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
		String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?";
		String[] selectionArgs = new String[] {phoneNumber};
		Cursor cursor = activity.getContentResolver().query(uri, 
				new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID},
				selection, selectionArgs, null);
		if(cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();//TODO: this ignores that more than one contacts might have the same number
			int col_of_id = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
			if(!cursor.isNull(col_of_id)){
				String id = cursor.getString(col_of_id).trim();
				cursor.close();
				return getDisplayNameByContactId(activity, id);
			}
		}
		if(cursor != null)
			cursor.close();
		return null;
	}

	private static String getDisplayNameByContactId(SCCMainActivity activity, String contactId){
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String selection = ContactsContract.Contacts._ID + " = ?";
		String[] selectionArgs = new String[] {contactId};
		Cursor cursor = activity.getContentResolver().query(uri, 
				new String[]{ContactsContract.Contacts.DISPLAY_NAME},
				selection, selectionArgs, null);
		if(cursor != null && cursor.getCount() > 0){
			cursor.moveToFirst();
			int col_of_name = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
			if(!cursor.isNull(col_of_name)){
				String ret = cursor.getString(col_of_name).trim();
				cursor.close();
				return ret;
			}
		}
		if(cursor != null)
			cursor.close();
		return null;
	}

	/**
	 * Adds Android contact information to the given alter.
	 * 
	 * Adds the alter to the personal network if not already in.
	 *  
	 * @param activity
	 * @param alterDisplayName must be identical to the contact display name in the Android system 
	 * and to the alter name in the personal network
	 */
	public static void importDataForAlterByName(SCCMainActivity activity, String alterDisplayName){
		if(alterDisplayName == null || alterDisplayName.trim().length() == 0)
			return;
		alterDisplayName = alterDisplayName.trim();
		PersonalNetwork network = PersonalNetwork.getInstance(activity);
		if(!network.hasAlterAt(TimeInterval.getCurrentTimePoint(), alterDisplayName))
			network.addToLifetimeOfAlter(TimeInterval.getRightUnboundedFromNow(), alterDisplayName);
		Uri contactUri = ContactsContract.Contacts.CONTENT_URI;
		String[] projection = {ContactsContract.Contacts._ID, ContactsContract.Contacts.PHOTO_ID, 
				ContactsContract.Contacts.PHOTO_URI, ContactsContract.Contacts.PHOTO_THUMBNAIL_URI};
		String selection = ContactsContract.Contacts.DISPLAY_NAME + " = ?";
		String[] selectionArgs = {alterDisplayName};
		Cursor cursor = activity.getContentResolver()
				.query(contactUri, projection, selection, selectionArgs, null);
		if(cursor != null && cursor.moveToFirst()){//the only one
			String systemId = null;
			int col_index_id = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
			if(!cursor.isNull(col_index_id)){
				systemId = cursor.getString(col_index_id);
				network.setAttributeValueAt(
						TimeInterval.getRightUnboundedFromNow(), 
						PersonalNetwork.ALTER_ATTRIBUTE_NAME_ALTER_SYSTEM_ID, 
						Alter.getInstance(alterDisplayName), systemId);
			}
			int col_index_photo_id = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID);
			if(!cursor.isNull(col_index_photo_id)){
				String photoId = cursor.getString(col_index_photo_id);
				network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
						PersonalNetwork.ALTER_ATTRIBUTE_NAME_ALTER_PHOTO_ID, 
						Alter.getInstance(alterDisplayName), photoId);
			}
			int col_index_photo_uri = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI);
			if(!cursor.isNull(col_index_photo_uri)){
				String photoURI = cursor.getString(col_index_photo_uri);
				network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
						PersonalNetwork.ALTER_ATTRIBUTE_NAME_ALTER_PHOTO_URI, 
						Alter.getInstance(alterDisplayName), photoURI);
			}
			int col_index_photo_thumb_uri = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI);
			if(!cursor.isNull(col_index_photo_thumb_uri)){
				String photoThumbURI = cursor.getString(col_index_photo_thumb_uri);
				network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
						PersonalNetwork.ALTER_ATTRIBUTE_NAME_ALTER_PHOTO_THUMBNAIL_URI, 
						Alter.getInstance(alterDisplayName), photoThumbURI);
			}
			if(systemId != null){
				addAlterEmailAddressesBySystemId(activity, alterDisplayName, systemId);
				addAlterPhoneNumbers(activity, alterDisplayName, systemId);
			}
		}
		if(cursor != null)
			cursor.close();
	}

	/**
	 * Adds all available email addresses (of the contact identified by the system id) 
	 * to the alter identified by altername.
	 * 
	 * Multiple addresses are separated by ", ".
	 * 
	 * Automatically adds calendar events linked to the email addresses to the alter.
	 * 
	 * @param alterName
	 * @param systemID
	 */
	public static void addAlterEmailAddressesBySystemId(SCCMainActivity activity, String alterName, String systemID){
		Uri uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
		String selection = ContactsContract.CommonDataKinds.Email.CONTACT_ID
				+ " = ?";
		String[] selectionArgs = new String[] {systemID};
		Cursor cursor = activity.getContentResolver().query(uri, 
				new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS}, selection, selectionArgs, null);
		if(cursor == null)
			return;
		StringBuffer emails = new StringBuffer();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()){
			int col_index = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS);
			if(!cursor.isNull(col_index)){
				if(emails.length() > 0){ //there is already one email address
					emails.append(", ");
				}
				emails.append(cursor.getString(col_index));
			}
		}
		if(cursor != null)
			cursor.close();
		if(emails.length() > 0){
			PersonalNetwork.getInstance(activity).setAttributeValueAt(
					TimeInterval.getRightUnboundedFromNow(),
					PersonalNetwork.ALTER_ATTRIBUTE_NAME_ALTER_EMAIL_ADDRESSES, 
					Alter.getInstance(alterName), emails.toString());
			String[] emailArray = emails.toString().split(", ");
			for(int i = 0; i < emailArray.length; ++i){
				addAlterCalendarEventsByEmail(activity, alterName, emailArray[i]);
			}
		}
	}

	/**
	 * Adds the calendar events of the contact with the given email address to the alter of the given name.
	 * 
	 * Assumes that ego did participate in these events.
	 * 
	 * Automatically adds other event attendees to the network (but not their associated system data).
	 * 
	 * @param activity
	 * @param alterName identifies the alter in the personal network
	 * @param email identifies (case insensitive) the event attendee in the Android system
	 */
	public static void addAlterCalendarEventsByEmail(SCCMainActivity activity, String alterName, String email){
		Uri uri = CalendarContract.Attendees.CONTENT_URI;
		String selection = CalendarContract.Attendees.ATTENDEE_EMAIL + " = ?  COLLATE NOCASE"; // case insensitive
		String[] selectionArgs = {email};
		Cursor cursor = activity.getContentResolver().query(uri, 
				new String[] {CalendarContract.Attendees.EVENT_ID }, selection,
				selectionArgs, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToPosition(-1);
			while (cursor.moveToNext()){
				String eventID = cursor.getString(cursor
						.getColumnIndex(CalendarContract.Attendees.EVENT_ID));
				//now query the data of this event
				Uri events_uri = CalendarContract.Events.CONTENT_URI;
				String[] projection = new String[] {CalendarContract.Events.TITLE, 
						CalendarContract.Events.DTSTART,
						CalendarContract.Events.DTEND, 
						CalendarContract.Events.CALENDAR_DISPLAY_NAME };
				selection = CalendarContract.Events._ID + " = ?";
				selectionArgs = new String[]{eventID};
				String sortOrder = CalendarContract.Events.DTSTART + " DESC";
				Cursor events_cursor = activity.getContentResolver().query(events_uri, projection, selection,
						selectionArgs, sortOrder);
				if(events_cursor != null && events_cursor.moveToFirst()){//the only one
					int col_index_time = events_cursor.getColumnIndex(CalendarContract.Events.DTSTART);
					if(!events_cursor.isNull(col_index_time)){
						long time = events_cursor.getLong(col_index_time);
						int col_index_title = events_cursor.getColumnIndex(CalendarContract.Events.TITLE);
						String title = null;
						if(!events_cursor.isNull(col_index_title))
							title = events_cursor.getString(col_index_title);
						PersonalNetwork history = PersonalNetwork.getInstance(activity);
						String previousEventId = history.getAttributeDatumIDAt(time, 
								PersonalNetwork.getEgoAlterContactEventAttributeName(), 
								EgoAlterDyad.getOutwardInstance(alterName));
						if(previousEventId == null || 
								PersonalNetwork.VALUE_NOT_ASSIGNED.equals(previousEventId)){
							history.setAttributeValueAt(TimeInterval.getTimePoint(time), 
									PersonalNetwork.getEgoAlterContactEventAttributeName(), 
									EgoAlterDyad.getOutwardInstance(alterName), 
									"value"); //TODO: use value for other stuff 
							String datumID = history.getAttributeDatumIDAt(time,
									PersonalNetwork.getEgoAlterContactEventAttributeName(), 
									EgoAlterDyad.getOutwardInstance(alterName));
							history.setSecondaryAttributeValue(datumID, 
									PersonalNetwork.getSecondaryAttributeNameText(), 
									"<automatic input> \n" +
											"Calendar event: " + title);
							//get other event attendees
							String calendarName = events_cursor.getString(events_cursor.
									getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME));
							addOtherCalendarEventAttendees(activity, alterName, 
									eventID, time, title, calendarName);
						}
					}
				}
				if(events_cursor != null)
					events_cursor.close();
			}
		}
		if(cursor != null)
			cursor.close();
	}

	/**
	 * Adds other (than the initial attendee) event attendees to the network (if they are not already in)
	 * and adds contact events between ego and these other attendees and 
	 * among all attendees (including the initial attendee).
	 * 
	 * @param activity
	 * @param initialAttendee
	 * @param eventID
	 * @param eventTime
	 * @param eventTitle
	 * @param calendarName
	 */
	private static void addOtherCalendarEventAttendees(SCCMainActivity activity, 
			String initialAttendee, 
			String eventID, long eventTime, 
			String eventTitle, 
			String calendarName){
		Uri uri = CalendarContract.Attendees.CONTENT_URI;
		String[] projection = new String[] {
				CalendarContract.Attendees.ATTENDEE_NAME,
				CalendarContract.Attendees.ATTENDEE_EMAIL };
		String selection = CalendarContract.Attendees.EVENT_ID + " = ?";
		String[] selectionArgs = {eventID};
		Cursor cursor = activity.getContentResolver().query(uri, projection, selection,
				selectionArgs, null);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToPosition(-1);
			ArrayList<String> previousOtherAttendees = new ArrayList<String>();
			PersonalNetwork history = PersonalNetwork.getInstance(activity);
			PersonalNetwork network = PersonalNetwork.getInstance(activity);
			while (cursor.moveToNext()) {
				String otherAttendee = null;
				if (cursor.isNull(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME))) {
					if(!cursor.isNull(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_EMAIL))){
						otherAttendee = cursor.getString(cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_EMAIL));
						//otherAttendee = getContactNameByEmail(activity, otherAttendee);
					}
				} else {
					otherAttendee = cursor.getString(
							cursor.getColumnIndex(CalendarContract.Attendees.ATTENDEE_NAME));
				}
				String egoDisplayName = network.getAttributeValueAt(System.currentTimeMillis(),
						PersonalNetwork.EGO_ATTRIBUTE_NAME_EGO_DISPLAY_NAME,
						Ego.getInstance());
				if(egoDisplayName == null || PersonalNetwork.VALUE_NOT_ASSIGNED.equals(egoDisplayName)){
					egoDisplayName = importEgoDisplayName(activity);
				}
				if(otherAttendee != null){
					otherAttendee = otherAttendee.trim();
					if(otherAttendee.length() > 0 && 
							!initialAttendee.equals(otherAttendee) && 
							!otherAttendee.equals(calendarName) &&
							(egoDisplayName == null || !otherAttendee.equals(egoDisplayName))
							){
						if(!network.hasAlterAt(TimeInterval.getMaxInterval(), otherAttendee)){
							network.addToLifetimeOfAlter(TimeInterval.getRightUnboundedFromNow(),
									otherAttendee);
//							Toast.makeText(activity, "added " + otherAttendee + 
//									" who participated in the same event as " + initialAttendee, 
//									Toast.LENGTH_LONG).show();
						}
						//add the ego alter tie to this other attendee
						String previousEventId = history.getAttributeDatumIDAt(eventTime, 
								PersonalNetwork.getEgoAlterContactEventAttributeName(), 
								EgoAlterDyad.getOutwardInstance(otherAttendee));
						if(previousEventId == null || 
								PersonalNetwork.VALUE_NOT_ASSIGNED.equals(previousEventId)){
							history.setAttributeValueAt(TimeInterval.getTimePoint(eventTime), 
									PersonalNetwork.getEgoAlterContactEventAttributeName(), 
									EgoAlterDyad.getOutwardInstance(otherAttendee), 
									"value");//TODO: use value for something 
							String datumID = history.getAttributeDatumIDAt(eventTime, 
									PersonalNetwork.getEgoAlterContactEventAttributeName(), 
									EgoAlterDyad.getOutwardInstance(otherAttendee));
							history.setSecondaryAttributeValue(datumID, 
									PersonalNetwork.getSecondaryAttributeNameText(), 
									"<automatic input> \n" +
											"Calendar event: " + eventTitle);
						}
						//add the alter-alter tie to the initial attendee
						if(!network.areAdjacentAt(TimeInterval.getMaxInterval(),
								initialAttendee, otherAttendee))
							network.addToLifetimeOfTie(TimeInterval.getRightUnboundedFromNow(),
									initialAttendee, otherAttendee);
						previousEventId = history.getAttributeDatumIDAt(eventTime, 
								PersonalNetwork.getAlterAlterContactEventAttributeName(), 
								AlterAlterDyad.getInstance(initialAttendee, otherAttendee));
						if(previousEventId == null ||
								PersonalNetwork.VALUE_NOT_ASSIGNED.equals(previousEventId)){
							history.setAttributeValueAt(TimeInterval.getTimePoint(eventTime), 
									PersonalNetwork.getAlterAlterContactEventAttributeName(), 
									AlterAlterDyad.getInstance(initialAttendee, otherAttendee),
									"value");//TODO: use value for something
							String datumID = history.getAttributeDatumIDAt(eventTime, 
									PersonalNetwork.getAlterAlterContactEventAttributeName(), 
									AlterAlterDyad.getInstance(initialAttendee, otherAttendee));
							history.setSecondaryAttributeValue(datumID, 
									PersonalNetwork.getSecondaryAttributeNameText(), 
									"<automatic input> \n" +
											"Calendar event: " + eventTitle);
						}
						for(String previousOtherAttendee : previousOtherAttendees){
							//add the alter tie
							if(!network.areAdjacentAt(TimeInterval.getMaxInterval(),
									otherAttendee, previousOtherAttendee))
								network.addToLifetimeOfTie(TimeInterval.getRightUnboundedFromNow(),
										otherAttendee, previousOtherAttendee);
							previousEventId = history.getAttributeDatumIDAt(eventTime, 
									PersonalNetwork.getAlterAlterContactEventAttributeName(), 
									AlterAlterDyad.getInstance(otherAttendee, previousOtherAttendee));
							if(previousEventId == null || 
									PersonalNetwork.VALUE_NOT_ASSIGNED.equals(previousEventId)){
								history.setAttributeValueAt(TimeInterval.getTimePoint(eventTime), 
										PersonalNetwork.getAlterAlterContactEventAttributeName(), 
										AlterAlterDyad.getInstance(otherAttendee, previousOtherAttendee),
										"value");//TODO: use value for something
								String datumID = history.getAttributeDatumIDAt(eventTime, 
										PersonalNetwork.getAlterAlterContactEventAttributeName(), 
										AlterAlterDyad.getInstance(otherAttendee, previousOtherAttendee));
								history.setSecondaryAttributeValue(datumID, 
										PersonalNetwork.getSecondaryAttributeNameText(), 
										"<automatic input> \n" +
												"Calendar event: " + eventTitle);					
							}
						}
						previousOtherAttendees.add(otherAttendee);
					}
				}
			}
		}
		if(cursor != null)
			cursor.close();
	}

	private static String importEgoDisplayName(SCCMainActivity activity) {
		PersonalNetwork network = PersonalNetwork.getInstance(activity);
		if(!network.hasAttribute(PersonalNetwork.DOMAIN_EGO,
				PersonalNetwork.EGO_ATTRIBUTE_NAME_EGO_DISPLAY_NAME))
			network.addAttribute(PersonalNetwork.DOMAIN_EGO,
					PersonalNetwork.EGO_ATTRIBUTE_NAME_EGO_DISPLAY_NAME, 
					activity.getString(R.string.attribute_description_for_ego_display_name), 
					PersonalNetwork.ATTRIB_TYPE_TEXT,
					PersonalNetwork.DYAD_DIRECTION_SYMMETRIC,
					PersonalNetwork.ATTRIBUTE_DYNAMIC_TYPE_STATE);
		Uri uri = ContactsContract.Profile.CONTENT_URI;
		String[] projection = {ContactsContract.Profile.DISPLAY_NAME};
		Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null);
		if(cursor != null && cursor.moveToFirst()){
			String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
			if(displayName != null){
				displayName = displayName.trim();
				network.setAttributeValueAt(TimeInterval.getRightUnboundedFromNow(),
						PersonalNetwork.EGO_ATTRIBUTE_NAME_EGO_DISPLAY_NAME, 
						Ego.getInstance(), displayName);
				cursor.close();
				return displayName;
			}
		}
		if(cursor != null)
			cursor.close();
		return null;
	}

	//TODO: check!!!
	/**
	 * Returns the email if no contact name could have been found.
	 * @param activity
	 * @param email
	 * @return
	 */
	private static String getContactNameByEmail(SCCMainActivity activity, String email) {
		Uri uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI;
		String[] projection = new String[] { ContactsContract.CommonDataKinds.Email.CONTACT_ID };
		String selection = ContactsContract.CommonDataKinds.Email.ADDRESS
				+ " = ? COLLATE NOCASE"; // case insensitive
		String[] selectionArgs = new String[] { email };
		String sortOrder = ContactsContract.CommonDataKinds.Email.CONTACT_ID
				+ " ASC";

		Cursor cursor = activity.getContentResolver().query(uri, projection, selection,
				selectionArgs, sortOrder);

		if (cursor != null && cursor.getCount() == 1) {
			Uri uriName = ContactsContract.Contacts.CONTENT_URI;
			String[] projectionName = new String[] {
					ContactsContract.Contacts._ID,
					ContactsContract.Contacts.DISPLAY_NAME };
			//				String selectionName = ContactsContract.Contacts.IN_VISIBLE_GROUP
			//						+ " = '1'";
			//				String[] selectionArgsName = null;
			String sortOrderName = ContactsContract.Contacts._ID + " ASC";

			Cursor cursorName = activity.getContentResolver().query(uriName,
					projectionName, null, null, //selectionName, selectionArgsName,
					sortOrderName);
			if(cursorName == null)
				return email;
			CursorJoiner joiner = new CursorJoiner(cursor, projection,
					cursorName, new String[] { ContactsContract.Contacts._ID });

			for (CursorJoiner.Result joinerResult : joiner) {
				switch (joinerResult) {
				case BOTH:
					return cursorName.getString(cursorName.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				default:
					break;
				}
			}
			cursorName.close();
		}
		if(cursor != null)
			cursor.close();
		return email;
	}


	/**
	 * Adds all available phone numbers (of the contact identified by the system id) 
	 * to the alter identified by altername.
	 * 
	 * Multiple numbers separated by ", ".
	 * 
	 * @param alterName
	 * @param systemID
	 */
	public static void addAlterPhoneNumbers(SCCMainActivity activity, String alterName, String systemID){
		Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
		String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID
				+ " = ?";
		String[] selectionArgs = new String[] {systemID};
		Cursor cursor = activity.getContentResolver().query(uri, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
				selection, selectionArgs, null);
		if(cursor == null)
			return;
		StringBuffer numbers = new StringBuffer();
		cursor.moveToPosition(-1);
		while (cursor.moveToNext()){
			String number = cursor.getString(cursor
					.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
			if(number != null && number.trim().length() > 0){
				addEgoAlterPhoneCallEvents(activity, alterName, number);
				number = formatStringLeaveOnlyNumbers(number);
				if(numbers.length() > 0){
					numbers.append(", ");
				}
				numbers.append(number);
			}
		}
		if(cursor != null)
			cursor.close();
		if(numbers.length() > 0)
			PersonalNetwork.getInstance(activity).setAttributeValueAt(
					TimeInterval.getRightUnboundedFromNow(),
					PersonalNetwork.ALTER_ATTRIBUTE_NAME_ALTER_PHONE_NUMBERS, 
					Alter.getInstance(alterName), numbers.toString());
	}

	public static void addEgoAlterPhoneCallEvents(SCCMainActivity activity, String alterName, String altersPhoneNumber){
		//  CallLog.Calls.TYPE = '2' is outgoing calls
		// CallLog.Calls.Type = '1' is incoming
		// 3 is missed calls (incoming - probably)
		Uri uri = CallLog.Calls.CONTENT_URI;
		String selection = CallLog.Calls.NUMBER + "= ?";
		String[] selectionArgs = {altersPhoneNumber};
		String sortOrder = CallLog.Calls.DATE + " DESC";
		Cursor cursor = activity.getContentResolver().query(uri, new String[] { CallLog.Calls.DATE}, selection,
				selectionArgs, sortOrder);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToPosition(-1);
			while (cursor.moveToNext()){
				long callTime = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
				PersonalNetwork history = PersonalNetwork.getInstance(activity);
				history.setAttributeValueAt(
						TimeInterval.getTimePoint(callTime), 
						PersonalNetwork.getEgoAlterContactEventAttributeName(), 
						EgoAlterDyad.getOutwardInstance(alterName),//TODO: change this with the direction 
						"value"); //TODO: use value for something 
				String datumID = history.getAttributeDatumIDAt(
						callTime, 
						PersonalNetwork.getEgoAlterContactEventAttributeName(), 
						EgoAlterDyad.getOutwardInstance(alterName));//TODO: change this with the direction
				history.setSecondaryAttributeValue(datumID, 
						PersonalNetwork.getSecondaryAttributeNameContactForm(), 
						activity.getString(R.string.secondary_attribute_contact_form_choices_item_phone));
				history.setSecondaryAttributeValue(datumID, 
						PersonalNetwork.getSecondaryAttributeNameText(), 
						"<automatic input>");
			}
		}
		if(cursor != null)
			cursor.close();
	}

	private static String formatStringLeaveOnlyNumbers(String number) {
		ArrayList<String> matchList = new ArrayList<String>();
		Pattern regex = Pattern.compile("[0-9]");
		Matcher regexMatcher = regex.matcher(number);
		while (regexMatcher.find())
			matchList.add(regexMatcher.group());
		return TextUtils.join("", matchList);
	}



}
