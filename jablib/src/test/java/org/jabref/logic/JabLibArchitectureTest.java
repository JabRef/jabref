package org.jabref.logic;

import org.jabref.support.CommonArchitectureTest;
import org.jabref.support.DoNotIncludeHtmlToNode;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;

@AnalyzeClasses(packages = "org.jabref", importOptions = {ImportOption.DoNotIncludeTests.class, DoNotIncludeHtmlToNode.class})
public class JabLibArchitectureTest extends CommonArchitectureTest {
}
