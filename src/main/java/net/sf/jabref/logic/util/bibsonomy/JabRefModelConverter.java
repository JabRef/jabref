package net.sf.jabref.logic.util.bibsonomy;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.MonthUtil;
import net.sf.jabref.model.strings.StringUtil;
import net.sf.jabref.preferences.JabRefPreferences;

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

import static org.bibsonomy.util.ValidationUtils.present;

/**
 * Converts between BibSonomy's and JabRef's BibTeX model.
 *
 * @author Waldemar Biller <wbi@cs.uni-kassel.de>
 * @version $Id: JabRefModelConverter.java,v 1.4 2011-05-04 08:21:51 dbe Exp $
 */
public class JabRefModelConverter {

	private static final Log LOGGER = LogFactory.getLog(JabRefModelConverter.class);

	private static final Set<String> EXCLUDE_FIELDS = new HashSet<>(Arrays.asList(new String[]{"abstract", // added
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
			"description", "keywords", "comment", "id"}));

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
	 * Converts a BibSonomy post into a JabRef BibEntry
	 *
	 <<<<<<< HEAD
	 * @param post A Post you need to convert
	 * @return An optional BibEntry
	 * @since 3.7
	 */
	public static Optional<BibEntry> convertPostOptional(final Post<? extends Resource> post) {
		BibEntry entry = convertPost(post);
		if (entry != null) {
			return Optional.of(entry);
		}
		return Optional.empty();
	}

	/**
	 * Converts a BibSonomy post into a JabRef BibEntry
	 *
	 * @param post The post you want to convert
	 * @return A compatible BibEntry for JabRef
	 */
	private static BibEntry convertPost(final Post<? extends Resource> post) {

		try {
			final BibTex bibtex = (BibTex) post.getResource();
			final BibEntry entry = new BibEntry();

			// JabRef generates an ID for the entry
			copyStringProperties(entry, bibtex);

			List<String> authorString = new LinkedList<>();
			bibtex.getAuthor().forEach(author -> authorString.add(author.toString()));
			if (!authorString.isEmpty()) {
				entry.setField(FieldName.AUTHOR, StringUtil.stripBrackets(authorString.toString()));
			} else {
				entry.clearField(FieldName.AUTHOR);
			}

			List<String> editorString = new LinkedList<>();
			bibtex.getEditor().forEach(editor -> editorString.add(editor.toString()));
			if (!editorString.isEmpty()) {
				entry.setField(FieldName.EDITOR, StringUtil.stripBrackets(editorString.toString()));
			} else {
				entry.clearField(FieldName.EDITOR);
			}

			/*
             * convert entry type (Is never null but getType() returns null for
			 * unknown types and JabRef knows less types than we.)
			 *
			 * FIXME: a nicer solution would be to implement the corresponding classes for the missing entrytypes.
			 */
			Optional<EntryType> optEntryType = BibtexEntryTypes.getType(bibtex.getEntrytype());
			if (optEntryType.isPresent()) {
				final EntryType entryType = optEntryType.get();
				entry.setType(entryType == null ? BibtexEntryTypes.MISC : entryType);

				copyMiscProperties(entry, bibtex);

				copyMonth(entry, bibtex);

				final String bibAbstract = bibtex.getAbstract();
				if (present(bibAbstract)) {
					entry.setField(FieldName.ABSTRACT, bibAbstract);
				}

				copyTags(entry, post);

				copyGroups(entry, post);

				// set comment + description
				final String description = post.getDescription();
				if (present(description)) {
					entry.setField(FieldName.DESCRIPTION, post.getDescription());
					entry.setField(FieldName.COMMENTS, post.getDescription());
				}

				if (present(post.getDate())) {
					entry.setField(FieldName.TIMESTAMP, bibsonomyDateFormat.format(post.getDate()));
				}

				if (present(post.getUser()))
					entry.setField(FieldName.USERNAME, post.getUser().getName());

				return entry;
			}
		} catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
			LOGGER.error("Could not convert BibSonomy post into a JabRef BibTeX entry.", e);
		}

		return null;
	}

	public static void copyGroups(final BibEntry entry, final Post<? extends Resource> post) {
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

	public static void copyTags(final BibEntry entry, final Post<? extends Resource> post) {
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

	public static void copyMonth(final BibEntry entry, final BibTex bibtex) {
		final String month = bibtex.getMonth();
		if (present(month)) {
			final String longMonth = MonthUtil.getMonth(month).fullName;
			if (present(longMonth)) {
				entry.setField("month", longMonth);
			} else {
				entry.setField("month", month);
			}
		}
	}

	public static void copyMiscProperties(final BibEntry entry, final BibTex bibtex) {
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

	protected static void copyStringProperties(BibEntry entry, BibTex bibtex) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

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
	 * Convert a JabRef BibEntry into a BibSonomy post
	 */
	public static Post<BibTex> convertEntry(final BibEntry entry) {
		final Post<BibTex> post = new Post<>();
		final BibTex bibtex = new BibTex();
		post.setResource(bibtex);

		bibtex.setMisc("");

		final List<String> knownFields = copyStringPropertiesToBibsonomyModel(bibtex, entry);

		Optional<String> authorName = entry.getField(FieldName.AUTHOR);
		Optional<String> editorName = entry.getField(FieldName.EDITOR);


		try {
			if (authorName.isPresent()) {
				bibtex.setAuthor(PersonNameUtils.discoverPersonNames(authorName.get()));
			}

			if (editorName.isPresent()) {
				bibtex.setEditor(PersonNameUtils.discoverPersonNames(editorName.get()));
			}
		} catch (PersonListParserException e) {
			ExceptionUtils.logErrorAndThrowRuntimeException(LOGGER, e, "Could not convert person names");
		}

		knownFields.add("author");
		knownFields.add("editor");

		// add unknown Properties to misc
		entry.getFieldNames().forEach(field -> {
			Optional<String> fieldName = entry.getField(field);
			if (!knownFields.contains(field) && !JabRefModelConverter.EXCLUDE_FIELDS.contains(field) && !field.startsWith("__")) {
				if(fieldName.isPresent()) {
					bibtex.addMiscField(field, fieldName.get());
				}
			}
		});

		bibtex.serializeMiscFields();

		// set the key
		Optional<String> citeKeyOpt = entry.getCiteKeyOptional();
		citeKeyOpt.ifPresent(citeKey ->  bibtex.setBibtexKey(StringUtil.toUTF8(citeKey)));

		Optional<String> typeOpt = entry.getField(FieldName.TYPE);
		typeOpt.ifPresent(type ->  bibtex.setEntrytype(StringUtil.toUTF8(type.toLowerCase())));

		// set the date of the post
		final Optional<String> entryTimestampOpt = entry.getField(FieldName.TIMESTAMP);
		entryTimestampOpt.ifPresent(entryTimestamp -> {
			try {
				post.setDate(bibsonomyDateFormat.parse(StringUtil.toUTF8(entryTimestamp)));
			} catch (ParseException ex) {
				LOGGER.debug("Could not parse BibSonomy date format - trying JabrefDateFormat...");
			}
			try {
				post.setDate(jabrefDateFormat.parse(StringUtil.toUTF8(entryTimestamp)));
			} catch (ParseException ex) {
				LOGGER.debug("Could not parse Jabref date format - set date to NULL");
				post.setDate(null); // this is null anyway, but just to make it clear
			}
		});

		final Optional<String> entryAbstractOpt = entry.getField(FieldName.ABSTRACT);
		entryAbstractOpt.ifPresent(abstractOpt -> bibtex.setAbstract(StringUtil.toUTF8(abstractOpt)));

		final Optional<String> entryKeywordsOpt = entry.getField(FieldName.KEYWORDS);
		entryKeywordsOpt.ifPresent(entryKeywords -> {
			for (String keyword : entryKeywords.split(jabRefKeywordSeparator)) {
				post.addTag(keyword);
			}
		});

		final Optional<String> entryUsernameOpt = entry.getField(FieldName.USERNAME);
		entryUsernameOpt.ifPresent(entryUsername -> post.setUser(new User(StringUtil.toUTF8(entryUsername))));


		// Set the groups
		final Optional<String> entryGroupsOpt = entry.getField(FieldName.GROUPS);
		entryGroupsOpt.ifPresent(entryGroups -> {
			final String[] groupsArray = entryGroups.split(" ");
			final Set<Group> groups = new HashSet<>();

			for (final String group : groupsArray)
				groups.add(new Group(StringUtil.toUTF8(group)));

			post.setGroups(groups);
		});

		final Optional<String> entryDescriptionOpt = entry.getField(FieldName.DESCRIPTION);
		entryDescriptionOpt.ifPresent(entryDescription -> post.setDescription(StringUtil.toUTF8(entryDescription)));


		final Optional<String> entryCommentOpt = entry.getField(FieldName.COMMENTS);
		entryCommentOpt.ifPresent(entryComment -> post.setDescription(StringUtil.toUTF8(entryComment)));

		final Optional<String> entryMonthOpt = entry.getField(FieldName.MONTH);
		entryMonthOpt.ifPresent(entryMonth -> bibtex.setMonth(StringUtil.toUTF8(entryMonth)));

		return post;
	}

	/**
	 * @param bibtex target object
	 * @param entry  source object
	 * @return list of all copied property names
	 */
	public static List<String> copyStringPropertiesToBibsonomyModel(final BibTex bibtex, final BibEntry entry) {
		final List<String> knownFields = new ArrayList<>(50);

		final BeanInfo info;
		try {
			info = Introspector.getBeanInfo(bibtex.getClass());
		} catch (IntrospectionException e) {
			ExceptionUtils.logErrorAndThrowRuntimeException(LOGGER, e, "could not introspect");
			return knownFields;
		}
		final PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

		// set all known properties of the BibTex
		for (PropertyDescriptor pd : descriptors) {
			if (!String.class.equals(pd.getPropertyType())) {
				continue;
			}
			if (present(entry.getField((pd.getName().toLowerCase()))) && !JabRefModelConverter.EXCLUDE_FIELDS.contains(pd.getName().toLowerCase())) {
				final Object value = entry.getField(pd.getName().toLowerCase());
				try {
					pd.getWriteMethod().invoke(bibtex, value);
				} catch (Exception e) {
					ExceptionUtils.logErrorAndThrowRuntimeException(LOGGER, e, "could not convert property " + pd.getName());
					return knownFields;
				}
				knownFields.add(pd.getName());
			}
		}
		return knownFields;
	}

}
