package org.jabref.model.entry.field;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FieldEditorsMultilinePropertyTest {

    private static final Pattern FIELD_PROPERTY_PATTERN = Pattern.compile("fieldProperties\\.contains\\s*\\(\\s*FieldProperty\\.(\\w+)\\s*\\)");
    private static final Pattern STANDARD_FIELD_PATTERN = Pattern.compile("==\\s*StandardField\\.(\\w+)");
    private static final Pattern INTERNAL_FIELD_PATTERN = Pattern.compile("==\\s*InternalField\\.(\\w+)");
    private static JavaParser PARSER;

    @BeforeAll
    public static void setUp() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        PARSER = new JavaParser(configuration);
    }

    /**
     * This test is somewhat fragile, as it depends on the structure of FieldEditors.java.
     * If the structure of FieldEditors.java is changed, this test might fail.
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
    public void fieldEditorsMatchMultilineProperty() throws Exception {
        Map<Path, List<FieldProperty>> result = getEditorsWithPropertiesInFieldEditors();
        for (Map.Entry<Path, List<FieldProperty>> entry : result.entrySet()) {
            // Now we have the file path and its properties, going to analyze the target Editor class
            Path filePath = entry.getKey();
            List<FieldProperty> properties = entry.getValue();
            CompilationUnit cu = PARSER.parse(filePath)
                                       .getResult()
                                       .orElseThrow(() ->
                                               new NullPointerException("Failed to parse "
                                                       + filePath
                                                       + ", java parser returned null CompilationUnit"
                                                       + ", please check if the file exists"));

            if (!implementedFieldEditorFX(cu)) {
                continue; // Make sure the class implements FieldEditorFX interface
            }

            if (properties.contains(FieldProperty.MULTILINE_TEXT)) {
                // If the editor has MULTILINE_TEXT property, we are going to check if the class holds a `TextInputControl` field
                // and have performed Text Area creation
                assertTrue(holdTextInputControlField(cu) && hasEditorTextAreaCreationExisted(cu),
                        "Class " + filePath + " should hold a TextInputControl field and have EditorTextArea creation");
            }
        }
    }

    /**
     * Parse FieldEditors.java to get all field editors and their properties in function getForField
     *
     * @return a map of field editor file path and its properties
     */
    private static Map<Path, List<FieldProperty>> getEditorsWithPropertiesInFieldEditors() throws Exception {
        final String filePath = "src/main/java/org/jabref/gui/fieldeditors/FieldEditors.java";
        Map<Path, List<FieldProperty>> result = new HashMap<>();
        CompilationUnit cu = PARSER.parse(Paths.get(filePath))
                                   .getResult()
                                   .orElseThrow(() ->
                                           new NullPointerException("Failed to parse FieldEditors.java"));

        // Locate getForField method in FieldEditors.java
        MethodDeclaration getForFieldCall = cu.findAll(MethodDeclaration.class).stream()
                .filter(methodDeclaration -> "getForField".equals(methodDeclaration.getNameAsString()))
                .findFirst()
                .orElseThrow(() -> new Exception("Failed to find getForField method in FieldEditors.java"));

        // Analyze all if statements in getForField method
        getForFieldCall.findAll(IfStmt.class).forEach(ifStmt -> {
            String condition = ifStmt.getCondition().toString();
            List<FieldProperty> properties = new ArrayList<>();
            // Match `fieldProperties.contains(FieldProperty.XXX)`
            Matcher propertyMatcher = FIELD_PROPERTY_PATTERN.matcher(condition);
            while (propertyMatcher.find()) {
                String propertyName = propertyMatcher.group(1);
                FieldProperty property = FieldProperty.valueOf(propertyName);
                properties.add(property);
            }
            // Match `== StandardField.XXX`
            Matcher standardFieldMatcher = STANDARD_FIELD_PATTERN.matcher(condition);
            if (standardFieldMatcher.find()) {
                String fieldName = standardFieldMatcher.group(1);
                StandardField standardField = StandardField.valueOf(fieldName);
                properties.addAll(standardField.getProperties());
            }
            // Match `== InternalField.XXX`
            Matcher internalFieldMatcher = INTERNAL_FIELD_PATTERN.matcher(condition);
            if (internalFieldMatcher.find()) {
                String fieldName = internalFieldMatcher.group(1);
                InternalField internalField = InternalField.valueOf(fieldName);
                properties.addAll(internalField.getProperties());
            }

            // Check if the return statement contains an object creation
            // If so, extract the created class name and its path
            ifStmt.getThenStmt().stream()
                  .filter(ReturnStmt.class::isInstance)
                  .map(ReturnStmt.class::cast)
                  .findFirst()
                  .flatMap(returnStmt ->
                        // Try to find the object creation in the return statement
                        returnStmt.stream()
                            .filter(ObjectCreationExpr.class::isInstance)
                            .map(ObjectCreationExpr.class::cast)
                            .findFirst()).ifPresent(creationExpr -> {
                                String createdClassName = creationExpr.getTypeAsString().replace("<>", "");
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
                            });
        });

        return result;
    }

    /**
     * Check if the class implements FieldEditorFX interface
     *
     * @param cu CompilationUnit
     * @return true if the class implements FieldEditorFX interface
     */
    private static boolean implementedFieldEditorFX(CompilationUnit cu) {
        return cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                .anyMatch(classDecl -> classDecl.getImplementedTypes().stream()
                        .anyMatch(type -> Objects.equals("FieldEditorFX", type.getNameAsString())));
    }

    /**
     * Check if the class has a new EditorTextArea creation
     *
     * @param cu CompilationUnit
     * @return true if the class has a new EditorTextArea creation
     */
    private static boolean hasEditorTextAreaCreationExisted(CompilationUnit cu) {
        return cu.findAll(ObjectCreationExpr.class).stream()
                .anyMatch(creation -> Objects.equals("EditorTextArea", creation.getType().toString()));
    }

    /**
     * Check if the class holds a TextInputControl field
     *
     * @param cu CompilationUnit
     * @return true if the class holds a TextInputControl field
     */
    private static boolean holdTextInputControlField(CompilationUnit cu) {
        // Since the class implements FieldEditorFX, we are going to check the first parameter when call
        // establishBinding method, which should be a TextInputControl
        AtomicBoolean hasTextInputControlField = new AtomicBoolean(false);
        cu.findAll(MethodCallExpr.class)
                .stream()
                .filter(methodCallExpr -> "establishBinding".equals(methodCallExpr.getNameAsString()))
                .findFirst()
                .ifPresent(methodCallExpr -> {
                    if (!methodCallExpr.getArguments().isEmpty()) {
                        String firstArgument = methodCallExpr.getArgument(0).toString();
                        cu.findAll(FieldDeclaration.class)
                                .stream()
                                .filter(fieldDeclaration -> fieldDeclaration.getVariables().stream()
                                        .anyMatch(variableDeclarator -> variableDeclarator.getNameAsString().equals(firstArgument)))
                                .findFirst()
                                .ifPresent(fieldDeclaration -> {
                                    String classType = fieldDeclaration.getElementType().asString();
                                    if ("TextInputControl".equals(classType)) {
                                        hasTextInputControlField.set(true);
                                    }
                                });
                    }
                });
        return hasTextInputControlField.get();
    }

    private static boolean holdEditorTextField(CompilationUnit compilationUnit) {
        AtomicBoolean hasEditorTextField = new AtomicBoolean(false);
        compilationUnit.findAll(MethodCallExpr.class).stream()
                .filter(methodCallExpr -> "establishBinding".equals(methodCallExpr.getNameAsString()))
                .findFirst()
                .ifPresent(establishBindingCall -> {
                    String firstArg = establishBindingCall.getArgument(0).toString();
                    compilationUnit.findAll(FieldDeclaration.class).stream()
                            .filter(field -> field.getVariable(0).getNameAsString().equals(firstArg))
                            .findFirst()
                            .ifPresent(fieldDeclaration -> {
                                String fieldType = fieldDeclaration.getElementType().asString();
                                if ("EditorTextField".equals(fieldType)) {
                                    hasEditorTextField.set(true);
                                }
                            });
                });
        return hasEditorTextField.get();
    }
}
