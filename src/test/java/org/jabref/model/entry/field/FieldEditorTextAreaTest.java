package org.jabref.model.entry.field;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FieldEditorTextAreaTest {

    private static JavaParser parser;
    private static final Logger logger = Logger.getLogger(FieldEditorTextAreaTest.class.getName());

    @BeforeAll
    public static void setUp() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        parser = new JavaParser(configuration);
    }


    /**
     * This test performs the following steps:
     * 1. Use Java parser to parse FieldEditors.java and check all if statements in the getForField method.
     * 2. Match the conditions of if statements to extract the field properties.
     * 3. Match the created FieldEditor class name with field properties extracted from step 2. This creates a map where:
     *    - The key is the file path of the FieldEditor class (for example: ....UrlEditor.java)
     *    - The value is the list of properties of the FieldEditor class (for example: [FieldProperty.EXTERNAL])
     * 4. For every class in the map, when its properties contain MULTILINE_TEXT, check whether it:
     *    a) Holds a TextInputControl field
     *    b) Has an EditorTextArea object creation
     */
    @Test
    public void fieldEditorTextAreaTest() throws IOException {
        // get all field editors and their properties in FieldEditors.java
        Map<Path, List<FieldProperty>> result = getEditorsWithPropertiesInFieldEditors();
        for (Map.Entry<Path, List<FieldProperty>> entry : result.entrySet()) {
            // now we have the file path and its properties, going to analyze the target Editor class
            Path filePath = entry.getKey();
            List<FieldProperty> properties = entry.getValue();
            if (properties.contains(FieldProperty.MULTILINE_TEXT)) {
                // if the editor has MULTILINE_TEXT property, we are going to check if the class hold a `TextInputControl` field
                // and have performed Text Area creation
                    CompilationUnit cu = parser.parse(filePath).getResult().orElse(null);
                    if (cu == null) {
                        throw new RuntimeException("Failed to analyze " + filePath);
                    }
                    if (implementedFieldEditorFX(cu)) { // make sure the class implements FieldEditorFX interface
                        assertTrue(holdTextInputControlField(cu) && hasEditorTextAreaCreationExisted(cu),
                                "Class " + filePath + " should hold a TextInputControl field and have EditorTextArea creation");
                    }
            }
        }
    }

    private static final Pattern FIELD_PROPERTY_PATTERN = Pattern.compile("fieldProperties\\.contains\\s*\\(\\s*FieldProperty\\.(\\w+)\\s*\\)");
    private static final Pattern STANDARD_FIELD_PATTERN = Pattern.compile("==\\s*StandardField\\.(\\w+)");
    private static final Pattern INTERNAL_FIELD_PATTERN = Pattern.compile("==\\s*InternalField\\.(\\w+)");


    /**
     * Parse FieldEditors.java to get all field editors and their properties in function getForField
     *
     * @return a map of field editor file path and its properties
     */
    private static Map<Path, List<FieldProperty>> getEditorsWithPropertiesInFieldEditors() {
        final String filePath = "src/main/java/org/jabref/gui/fieldeditors/FieldEditors.java";
        Map<Path, List<FieldProperty>> result = new HashMap<>();

        try {
            CompilationUnit cu = parser.parse(Paths.get(filePath)).getResult().orElse(null);
            if (cu == null) {
                throw new RuntimeException("Failed to analyze FieldEditors.java");
            }

            // locate getForField method in FieldEditors.java
            MethodDeclaration getForFieldCall = cu.findAll(MethodDeclaration.class).stream()
                    .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals("getForField"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Failed to find getForField method in FieldEditors.java"));

            // analyze all if statements in getForField method
            getForFieldCall.findAll(IfStmt.class).forEach(ifStmt -> {
                String condition = ifStmt.getCondition().toString();
                List<FieldProperty> properties = new ArrayList<>();
                // match `fieldProperties.contains(FieldProperty.XXX)`
                Matcher propertyMatcher = FIELD_PROPERTY_PATTERN.matcher(condition);
                while (propertyMatcher.find()) {
                    String propertyName = propertyMatcher.group(1);
                    try {
                        FieldProperty property = FieldProperty.valueOf(propertyName);
                        properties.add(property);
                    } catch (IllegalArgumentException e) {
                        logger.warning("Unknown FieldProperty: " + propertyName);
                    }
                }
                // match `== StandardField.XXX`
                Matcher standardFieldMatcher = STANDARD_FIELD_PATTERN.matcher(condition);
                if (standardFieldMatcher.find()) {
                    String fieldName = standardFieldMatcher.group(1);
                    try {
                        StandardField standardField = StandardField.valueOf(fieldName);
                        properties.addAll(standardField.getProperties());
                    } catch (IllegalArgumentException e) {
                        logger.warning("Unknown StandardField: " + fieldName);
                    }
                }
                // match `== InternalField.XXX`
                Matcher internalFieldMatcher = INTERNAL_FIELD_PATTERN.matcher(condition);
                if (internalFieldMatcher.find()) {
                    String fieldName = internalFieldMatcher.group(1);
                    try {
                        InternalField internalField = InternalField.valueOf(fieldName);
                        properties.addAll(internalField.getProperties());
                    } catch (IllegalArgumentException e) {
                        logger.warning("Unknown InternalField: " + fieldName);
                    }
                }

                // get this if statement's return statement
                ReturnStmt returnStatement = ifStmt.getThenStmt().stream()
                        .filter(ReturnStmt.class::isInstance)
                        .map(ReturnStmt.class::cast)
                        .findFirst()
                        .orElse(null);
                if (returnStatement != null) {
                    // get the creation expression in the return statement
                    ObjectCreationExpr creationExpr = returnStatement.stream()
                            .filter(ObjectCreationExpr.class::isInstance)
                            .map(ObjectCreationExpr.class::cast)
                            .findFirst()
                            .orElse(null);
                    if (creationExpr != null) {
                        // get the created class name
                        String createdClassName = creationExpr.getTypeAsString().replace("<>", "");
                        // get the exact java file path from import statement
                        cu.findAll(ImportDeclaration.class)
                                .stream()
                                .filter(importDeclaration -> importDeclaration.getNameAsString().endsWith(createdClassName))
                                .findFirst()
                                .ifPresentOrElse(importDeclaration -> {
                                    String classPath = importDeclaration.getNameAsString();
                                    Path classFilePath = Paths.get("src/main/java/" + classPath.replace(".", "/") + ".java");
                                    result.put(classFilePath, properties);
                                }, () -> {
                                    Path classFilePath = Paths.get("src/main/java/org/jabref/gui/fieldeditors/" + createdClassName + ".java");
                                    result.put(classFilePath, properties);
                                });

                    }
                }

            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error parsing file: " + filePath, e);
        }

        return result;
    }

}
