/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.imports;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.FindUnlinkedFilesDialog;
import net.sf.jabref.FindUnlinkedFilesDialog.CheckableTreeNode;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Nosh&Dan
 * @version 09.11.2008 | 21:06:17
 */
public class DatabaseFileLookupTest {

    private BibtexDatabase database;
    private Collection<BibtexEntry> entries;

    private BibtexEntry entry1;
    private BibtexEntry entry2;


    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        ParserResult result = BibtexParser.parse(new FileReader(ImportDataTest.UNLINKED_FILES_TEST_BIB));
        database = result.getDatabase();
        entries = database.getEntries();

        entry1 = database.getEntryByKey("entry1");
        entry2 = database.getEntryByKey("entry2");
    }

    /**
     * Tests the prerequisites of this test-class itself.
     */
    @Test
    public void testTestDatabase() throws Exception {
        Assert.assertEquals(2, database.getEntryCount());
        Assert.assertEquals(2, entries.size());
        Assert.assertNotNull(entry1);
        Assert.assertNotNull(entry2);
    }

    @Test
    @Ignore
    public void testInsertTestData() throws Exception {

        entry1 = new BibtexEntry();
        JabRefPreferences jabRefPreferences = JabRefPreferences.getInstance();
        ExternalFileType fileType = jabRefPreferences.getExternalFileTypeByExt("PDF");
        FileListEntry fileListEntry = new FileListEntry("", ImportDataTest.FILE_IN_DATABASE.getAbsolutePath(), fileType);

        FileListTableModel model = new FileListTableModel();
        model.addEntry(0, fileListEntry);

        entry1.setField("file", model.getStringRepresentation());

        database.insertEntry(entry1);

        // #################### SETUP END ##################### //

        UnlinkedFilesCrawler crawler = new UnlinkedFilesCrawler(database);
        CheckableTreeNode treeNode = crawler.searchDirectory(ImportDataTest.EXISTING_FOLDER, new EntryFromPDFCreator());

        Assert.assertNotNull(treeNode);

        /**
         * Select all nodes manually.
         */
        @SuppressWarnings("unchecked")
        Enumeration<CheckableTreeNode> enumeration = treeNode.breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            CheckableTreeNode nextElement = enumeration.nextElement();
            nextElement.setSelected(true);
        }

        List<File> resultList = getFileListFromNode(treeNode);

        Assert.assertFalse(resultList.isEmpty());
        Assert.assertTrue(resultList.contains(ImportDataTest.FILE_NOT_IN_DATABASE));
        Assert.assertFalse(resultList.contains(ImportDataTest.FILE_IN_DATABASE));
    }

    /**
     * Connector-Method for the private method
     * {@link FindUnlinkedFilesDialog#getFileListFromNode(net.sf.jabref.FindUnlinkedFilesDialog.CheckableTreeNode)} of the dialog
     * {@link FindUnlinkedFilesDialog}. <br>
     * <br>
     * This method uses <b>reflection</b> to get access to that method.
     *
     * @see FindUnlinkedFilesDialog#getFileListFromNode(net.sf.jabref.FindUnlinkedFilesDialog.CheckableTreeNode)
     */
    private List<File> getFileListFromNode(CheckableTreeNode node) throws Exception {
        return DatabaseFileLookupTest.invokeMethod("getFileListFromNode", FindUnlinkedFilesDialog.class, node);
    }

    /**
     * Invokes a method in the supplied class with the given arguments, and
     * returnes the methods result in the desired type. <br>
     * <br>
     * The only requirement ist, that the type, on which the method is to be
     * called, has the default constructor. <br>
     * <br>
     * This method will create an instance of the provided class
     * <code>targetClass</code>, which is generally described by the generic
     * parameter <code>T</code> (for <i>Type</i>). The instance is created using
     * the <b>default constructor</b>. If the default constructor is not
     * declared, an Exception will be throwen. However, there is no requirement
     * on the visibility of the default constructor. <br>
     * Using this instance, the method specified by the string parameter
     * <code>methodName</code> will be invoked. Again, there is no requirement
     * on the visibility of the method. <br>
     * The method will be invoked, using the supplied parameter-list
     * <code>params</code>. <br>
     * <br>
     * The result will be returned as an object of the generic type
     * <code>R</code> (for <i>Result</i>), and as this type parameter
     * <code>R</code> is not further specified, the result my be any type and
     * does not need to be casted.
     *
     * @param <R>         The result type of the method. Does not need to be declared.
     * @param <T>         The type, on which the method will be invoked.
     * @param methodName  Method name to be invoked.
     * @param targetClass Class instance of the type, on which the method is to be
     *                    invoked.
     * @param params      Parameters for the invokation of the method.
     * @return The result of the method, that is invoked.
     */
    @SuppressWarnings("unchecked")
    public static <R, T> R invokeMethod(String methodName, Class<T> targetClass, Object... params) throws Exception {
        T instance = DatabaseFileLookupTest.getInstanceFromType(targetClass);
        if (instance == null) {
            throw new InstantiationException("The type '" + targetClass + "' could not be instantiated.");
        }
        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            paramTypes[i] = params[i].getClass();
        }
        Method method = targetClass.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return (R) method.invoke(instance, params);
    }

    private static <T> T getInstanceFromType(Class<T> targetClass) throws IllegalArgumentException {
        T instance;
        try {
            Constructor<? extends T> constructor;
            constructor = targetClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (Exception e) {
            instance = DatabaseFileLookupTest.getInstanceFromNonDefaultConstructor(targetClass);
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<? extends T>[] orderByParamCount(Constructor<? extends T>[] constructors) {
        List<Constructor<? extends T>> list = Arrays.asList(constructors);
        Collections.sort(list, new Comparator<Constructor<? extends T>>() {

            @Override
            public int compare(Constructor<? extends T> c1, Constructor<? extends T> c2) {
                return new Integer(c1.getParameterTypes().length).compareTo(c2.getParameterTypes().length);
            }
        });
        return new ArrayList<Constructor<? extends T>>(list).toArray(new Constructor[list.size()]);
    }

    private static <T> T getInstanceFromNonDefaultConstructor(Class<T> targetClass) {
        Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
        constructors = DatabaseFileLookupTest.orderByParamCount(constructors);
        for (Constructor<?> constructor : constructors) {
            constructor.setAccessible(true);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            try {
                /**
                 * Trying to invoke constructor with <code>null</code> values.
                 */
                @SuppressWarnings("unchecked")
                T instance = (T) constructor.newInstance(new Object[parameterTypes.length]);
                return instance;
            } catch (Exception ignored) {
            }
            /**
             * Creating proper instances for the parameter types.
             */
            Object[] arguments = DatabaseFileLookupTest.createArguments(parameterTypes, targetClass);
            if (arguments == null) {
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                T instance = (T) constructor.newInstance(arguments);
                return instance;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Creates an argument-array for the type-array <code>parameterTypes</code>
     * by trying to instanciate every single parameter type. <br>
     * <br>
     * If one of the instanciation fails, the <code>null</code> value will be written
     * into the argument-array.
     *
     * @param parameterTypes An Array of types, which shall be created.
     * @return An array of arguments.
     */
    private static <T> Object[] createArguments(Class<?>[] parameterTypes, Class<T> targetClass) {
        Object[] parameterValues = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> typeClass = parameterTypes[i];
            if (targetClass.equals(typeClass)) {
                return null;
            }
            try {
                parameterValues[i] = DatabaseFileLookupTest.getInstanceFromType(typeClass);
            } catch (Exception e) {
                parameterValues[i] = null;
            }
        }
        return parameterValues;
    }

}
