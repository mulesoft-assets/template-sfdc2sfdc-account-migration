package org.mule.kicks.util;

import java.util.Map;

import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * The function of this class is to establish a relation happens before between
 * two maps representing SFDC accounts.
 * 
 * It's assumed that these maps are well formed maps from SFDC thus they both
 * contain an entry with the expected key. Never the less validations are being
 * done.
 * 
 * @author cesargarcia
 */
public class AccountDateComparator {
	private static final String LAST_ACTIVITY_DATE = "LastActivityDate";

	/**
	 * Validate which contact has the latest last modification date.
	 * 
	 * @param contactA
	 *            SFDC contact map
	 * @param contactB
	 *            SFDC contact map
	 * @return true if the last modified date from contactA is after the one
	 *         from contact B
	 */
	public static boolean isAfter(Map<String, String> contactA, Map<String, String> contactB) {
		Validate.notNull(contactA, "The account A should not be null");
		Validate.notNull(contactB, "The account B should not be null");

		Validate.isTrue(contactA.containsKey(LAST_ACTIVITY_DATE), "The account A map should containt the key " + LAST_ACTIVITY_DATE);
		Validate.isTrue(contactB.containsKey(LAST_ACTIVITY_DATE), "The account B map should containt the key " + LAST_ACTIVITY_DATE);

		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		DateTime lastModifiedDateOfA = formatter.parseDateTime(contactA.get(LAST_ACTIVITY_DATE));
		DateTime lastModifiedDateOfB = formatter.parseDateTime(contactB.get(LAST_ACTIVITY_DATE));

		return lastModifiedDateOfA.isAfter(lastModifiedDateOfB);
	}
}
