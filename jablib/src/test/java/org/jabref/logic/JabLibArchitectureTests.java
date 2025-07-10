package org.jabref.logic;

import org.jabref.support.CommonArchitectureTest;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "org.jabref", importOptions = ImportOption.DoNotIncludeTests.class)
public class JabLibArchitectureTests extends CommonArchitectureTest {
}
