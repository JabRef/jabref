/**
 *  
 *  JabRef Bibsonomy Plug-in - Plugin for the reference management 
 * 		software JabRef (http://jabref.sourceforge.net/) 
 * 		to fetch, store and delete entries from BibSonomy.
 *   
 *  Copyright (C) 2008 - 2011 Knowledge & Data Engineering Group, 
 *                            University of Kassel, Germany
 *                            http://www.kde.cs.uni-kassel.de/
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/**
 * 
 */
package org.bibsonomy.plugin.jabref.util;

import static org.bibsonomy.util.ValidationUtils.present;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.model.BibTex;
import org.bibsonomy.model.Group;
import org.bibsonomy.model.Post;
import org.bibsonomy.model.Resource;
import org.bibsonomy.model.Tag;
import org.bibsonomy.model.User;
import org.bibsonomy.model.util.PersonNameParser.PersonListParserException;
import org.bibsonomy.model.util.PersonNameUtils;
import org.bibsonomy.util.ExceptionUtils;

/**
 * Converts between BibSonomy's and JabRef's BibTeX model.
 * 
 * @author Waldemar Biller <wbi@cs.uni-kassel.de>
 * @version $Id: JabRefModelConverter.java,v 1.4 2011-05-04 08:21:51 dbe Exp $
 * 
 */
public class JabRefModelConverter {

	private static final Log log = LogFactory.getLog(JabRefModelConverter.class);

	private static final Set<String> EXCLUDE_FIELDS = new HashSet<String>(Arrays.asList(new String[] { "abstract", // added
																													// separately
			"bibtexAbstract", // added separately
			"bibtexkey", "entrytype", // added at beginning of entry
			"misc", // contains several fields; handled separately
			"month", // handled separately
			"openURL", // not added
			"simHash0", // not added
			"simHash1", // not added
			"simHash2", // not added
			"simHash3", // not added
			"description", "keywords", "comment", "id" }));

	/**
	 * date's in JabRef are stored as strings, in BibSonomy as Date objects. We
	 * have to supply two formats - the first is the one which exists when
	 * having downloaded entries from BibSonomy, the second one when entries
	 * were created from scratch within JabRef.
	 */
	private static final SimpleDateFormat bibsonomyDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

	private static final SimpleDateFormat jabrefDateFormat = new SimpleDateFormat("yyyy.MM.dd");

	/**
	 * separates tags
	 */
	private static final String jabRefKeywordSeparator = JabRefPreferences.getInstance().get("groupKeywordSeparator", ", ");

	/**
	 * Converts a list of posts in BibSonomy's format into JabRef's format.
	 * 
	 * @param posts
	 *            - a list of posts in BibSonomy's data model
	 * @return A list of posts in JabRef's data model.
	 */
	public static List<BibtexEntry> convertPosts(final List<Post<? extends Resource>> posts) {
		final List<BibtexEntry> entries = new ArrayList<BibtexEntry>();
		for (final Post<? extends Resource> post : posts) {
			entries.add(convertPost(post));
		}
		return entries;
	}

	/**
	 * Converts a BibSonomy post into a JabRef BibtexEntry
	 * 
	 * @param post
	 * @return
	 */
	public static BibtexEntry convertPost(final Post<? extends Resource> post) {

		try {
			// what we have
			final BibTex bibtex = (BibTex) post.getResource();
			// what we want
			final BibtexEntry entry = new BibtexEntry();
			/*
			 * each entry needs an ID (otherwise we get a NPE) ... let JabRef
			 * generate it
			 */
			copyStringProperties(entry, bibtex);

			entry.setField("author", PersonNameUtils.serializePersonNames(bibtex.getAuthor()));
			entry.setField("editor", PersonNameUtils.serializePersonNames(bibtex.getEditor()));

			/*
			 * convert entry type (Is never null but getType() returns null for
			 * unknown types and JabRef knows less types than we.)
			 * 
			 * FIXME: a nicer solution would be to implement the corresponding
			 * classes for the missing entrytypes.
			 */
			final BibtexEntryType entryType = BibtexEntryType.getType(bibtex.getEntrytype());
			entry.setType(entryType == null ? BibtexEntryType.OTHER : entryType);

			copyMiscProperties(entry, bibtex);

			copyMonth(entry, bibtex);

			final String bibAbstract = bibtex.getAbstract();
			if (present(bibAbstract))
				entry.setField("abstract", bibAbstract);

			copyTags(entry, post);

			copyGroups(entry, post);

			// set comment + description
			final String description = post.getDescription();
			if (present(description)) {
				entry.setField("description", post.getDescription());
				entry.setField("comment", post.getDescription());
			}

			if (present(post.getDate())) {
				entry.setField("timestamp", bibsonomyDateFormat.format(post.getDate()));
			}

			if (present(post.getUser()))
				entry.setField("username", post.getUser().getName());

			return entry;

		} catch (final Exception e) {
			log.error("Could not convert BibSonomy post into a JabRef BibTeX entry.", e);
		}

		return null;
	}

	public static void copyGroups(final BibtexEntry entry, final Post<? extends Resource> post) {
		// set groups - will be used in jabref when exporting to bibsonomy
		if (present(post.getGroups())) {
			final Set<Group> groups = post.getGroups();
			final StringBuffer groupsBuffer = new StringBuffer();
			for (final Group group : groups)
				groupsBuffer.append(group.getName() + " ");

			final String groupsBufferString = groupsBuffer.toString().trim();
			if (present(groupsBufferString))
				entry.setField("groups", groupsBufferString);
		}
	}

	public static void copyTags(final BibtexEntry entry, final Post<? extends Resource> post) {
		/*
		 * concatenate tags using the JabRef keyword separator
		 */
		final Set<Tag> tags = post.getTags();
		final StringBuffer tagsBuffer = new StringBuffer();
		for (final Tag tag : tags) {
			tagsBuffer.append(tag.getName() + jabRefKeywordSeparator);
		}
		/*
		 * remove last separator
		 */
		if (!tags.isEmpty()) {
			tagsBuffer.delete(tagsBuffer.lastIndexOf(jabRefKeywordSeparator), tagsBuffer.length());
		}
		final String tagsBufferString = tagsBuffer.toString();
		if (present(tagsBufferString))
			entry.setField("keywords", tagsBufferString);
	}

	public static void copyMonth(final BibtexEntry entry, final BibTex bibtex) {
		final String month = bibtex.getMonth();
		if (present(month)) {
			/*
			 * try to convert the month abbrev like JabRef does it
			 */
			final String longMonth = Globals.MONTH_STRINGS.get(month);
			if (present(longMonth)) {
				entry.setField("month", longMonth);
			} else {
				entry.setField("month", month);
			}
		}
	}

	public static void copyMiscProperties(final BibtexEntry entry, final BibTex bibtex) {
		if (present(bibtex.getMisc()) || present(bibtex.getMiscFields())) {

			// parse the misc fields and loop over them
			bibtex.parseMiscField();

			/*
			 * FIXME: if the misc field erroneously contains the intrahash, it
			 * is overwriting the correct one, which is set above!
			 */
			if (bibtex.getMiscFields() != null)
				for (final String key : bibtex.getMiscFields().keySet()) {
					if ("id".equals(key)) {
						// id is used by jabref
						entry.setField("misc_id", bibtex.getMiscField(key));
						continue;
					}

					if (key.startsWith("__")) // ignore fields starting with
												// __ - jabref uses them for
												// control
						continue;

					entry.setField(key, bibtex.getMiscField(key));
				}

		}
	}

	protected static void copyStringProperties(BibtexEntry entry, BibTex bibtex) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		/*
		 * we use introspection to get all fields ...
		 */
		final BeanInfo info = Introspector.getBeanInfo(bibtex.getClass());
		final PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

		/*
		 * iterate over all properties
		 */
		for (final PropertyDescriptor pd : descriptors) {

			final Method getter = pd.getReadMethod();

			// loop over all String attributes
			final Object o = getter.invoke(bibtex, (Object[]) null);

			if (String.class.equals(pd.getPropertyType()) && (o != null) && !JabRefModelConverter.EXCLUDE_FIELDS.contains(pd.getName())) {
				final String value = ((String) o);
				if (present(value)) {
					entry.setField(pd.getName().toLowerCase(), value);
				}
			}
		}
	}

	/**
	 * Convert a JabRef BibtexEntry into a BibSonomy post
	 * 
	 * @param entry
	 * @return
	 */
	public static Post<BibTex> convertEntry(final BibtexEntry entry) {
		final Post<BibTex> post = new Post<BibTex>();
		final BibTex bibtex = new BibTex();
		post.setResource(bibtex);

		bibtex.setMisc("");

		final List<String> knownFields = copyStringPropertiesToBibsonomyModel(bibtex, entry);

		try {
			bibtex.setAuthor(PersonNameUtils.discoverPersonNames(entry.getField("author")));
			bibtex.setEditor(PersonNameUtils.discoverPersonNames(entry.getField("editor")));
		} catch (PersonListParserException e) {
			ExceptionUtils.logErrorAndThrowRuntimeException(log, e, "Could not convert person names");
		}

		knownFields.add("author");
		knownFields.add("editor");

		// add unknown Properties to misc
		for (final String field : entry.getAllFields()) {
			if (!knownFields.contains(field) && !JabRefModelConverter.EXCLUDE_FIELDS.contains(field) && !field.startsWith("__")) {
				bibtex.addMiscField(field, entry.getField(field));
			}
		}

		bibtex.serializeMiscFields();

		// set the key
		bibtex.setBibtexKey(StringUtil.toUTF8(entry.getCiteKey()));
		bibtex.setEntrytype(StringUtil.toUTF8(entry.getType().getName().toLowerCase()));

		// set the date of the post
		final String timestamp = StringUtil.toUTF8(entry.getField("timestamp"));
		if (present(timestamp)) {
			try {
				post.setDate(bibsonomyDateFormat.parse(timestamp));
			} catch (ParseException ex) {
				log.debug("Could not parse BibSonomy date format - trying JabrefDateFormat...");
			}
			try {
				post.setDate(jabrefDateFormat.parse(timestamp));
			} catch (ParseException ex) {
				log.debug("Could not parse Jabref date format - set date to NULL");
				post.setDate(null); // this is null anyway, but just to make
									// it clear
			}
		}

		final String abstractt = StringUtil.toUTF8(entry.getField("abstract"));
		if (present(abstractt))
			bibtex.setAbstract(abstractt);

		final String keywords = StringUtil.toUTF8(entry.getField("keywords"));
		if (present(keywords)) {
			for (String keyword : keywords.split(jabRefKeywordSeparator)) {

				post.addTag(keyword);
			}
		}

		if (present(entry.getField("username")))
			post.setUser(new User(StringUtil.toUTF8(entry.getField("username"))));

		// Set the groups
		if (present(entry.getField("groups"))) {

			final String[] groupsArray = entry.getField("groups").split(" ");
			final Set<Group> groups = new HashSet<Group>();

			for (final String group : groupsArray)
				groups.add(new Group(StringUtil.toUTF8(group)));

			post.setGroups(groups);
		}

		final String description = StringUtil.toUTF8(entry.getField("description"));
		if (present(description))
			post.setDescription(description);

		final String comment = StringUtil.toUTF8(entry.getField("comment"));
		if (present(comment))
			post.setDescription(comment);

		final String month = StringUtil.toUTF8(entry.getField("month"));
		if (present(month))
			bibtex.setMonth(month);

		return post;
	}

	/**
	 * @param bibtex
	 *            target object
	 * @param entry
	 *            source object
	 * @return list of all copied property names
	 */
	public static List<String> copyStringPropertiesToBibsonomyModel(final BibTex bibtex, final BibtexEntry entry) {
		final List<String> knownFields = new ArrayList<String>(50);

		final BeanInfo info;
		try {
			info = Introspector.getBeanInfo(bibtex.getClass());
		} catch (IntrospectionException e) {
			ExceptionUtils.logErrorAndThrowRuntimeException(log, e, "could not introspect");
			return knownFields;
		}
		final PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

		// set all known properties of the BibTex
		for (PropertyDescriptor pd : descriptors) {
			if (String.class.equals(pd.getPropertyType()) == false) {
				continue;
			}
			if (present(entry.getField((pd.getName().toLowerCase()))) && !JabRefModelConverter.EXCLUDE_FIELDS.contains(pd.getName().toLowerCase())) {
				final Object value = entry.getField(pd.getName().toLowerCase());
				try {
					pd.getWriteMethod().invoke(bibtex, value);
				} catch (Exception e) {
					ExceptionUtils.logErrorAndThrowRuntimeException(log, e, "could not convert property " + pd.getName());
					return knownFields;
				}
				knownFields.add(pd.getName());
			}
		}
		return knownFields;
	}

}
