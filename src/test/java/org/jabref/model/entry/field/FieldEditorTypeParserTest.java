package org.jabref.model.entry.field;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.IfStmt;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FieldEditorTypeParserTest {

    private static JavaParser parser;

//    @BeforeAll
//    public static void setUp() {
//        ParserConfiguration configuration = new ParserConfiguration();
//        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
//        parser = new JavaParser(configuration);
//    }
//
//    @Test
//    public void analyzeFieldEditors() throws IOException {
//        CompilationUnit cu = StaticJavaParser.parse(Paths.get("src/main/java/org/jabref/gui/fieldeditors/FieldEditors.java"));
//
//        Map<String, Set<FieldProperty>> editorToFieldProperties = new HashMap<>();
//        Map<String, String> editorTypeToPath = new HashMap<>();
//
//        // Extract import statements to map editor types to their full paths
//        cu.findAll(ImportDeclaration.class).forEach(importDecl -> {
//            String importPath = importDecl.getNameAsString();
//            String[] parts = importPath.split("\\.");
//            String editorType = parts[parts.length - 1];
//            editorTypeToPath.put(editorType, importPath);
//        });
//
//        cu.findAll(MethodDeclaration.class).stream()
//                .filter(method -> method.getNameAsString().equals("getForField"))
//                .forEach(method -> {
//                    method.findAll(IfStmt.class).forEach(ifStmt -> {
//                        String condition = ifStmt.getCondition().toString();
//                        String editorType = extractEditorType(ifStmt);
//                        Set<FieldProperty> properties = extractProperties(condition);
//                        if (editorType != null) {
//                            String editorPath = editorTypeToPath.getOrDefault(editorType, "org.jabref.gui.fieldeditors." + editorType);
//                            editorToFieldProperties.put(editorPath, properties);
//                            System.out.println(editorPath + " -> " + properties);
//                        }
//                    });
//                });
//
//        for (Map.Entry<String, Set<FieldProperty>> entry : editorToFieldProperties.entrySet()) {
//            String editorPath = entry.getKey();
//            Set<FieldProperty> properties = entry.getValue();
//
//            CompilationUnit editorCu = parser.parse(Paths.get("src/main/java/" + editorPath.replace('.', '/') + ".java")).getResult().orElse(null);
//
//            Optional<MethodCallExpr> establishBindingCall = editorCu.findAll(MethodCallExpr.class).stream()
//                    .filter(methodCall -> methodCall.getNameAsString().equals("establishBinding"))
//                    .findFirst();
//            if (establishBindingCall.isPresent()) {
//                String firstArg = establishBindingCall.get().getArgument(0).toString();
//
//                Optional<FieldDeclaration> fxComponentClass = editorCu.findAll(FieldDeclaration.class).stream()
//                        .filter(field -> field.getVariable(0).getNameAsString().equals(firstArg))
//                        .findFirst();
//
//                if (fxComponentClass.isPresent()) {
//                    String fxComponentType = fxComponentClass.get().getElementType().toString();
//
//                    boolean isMultiLine = properties.contains(FieldProperty.MULTILINE_TEXT);
//                    boolean isEditorTextFiled = fxComponentType.equals("EditorTextField");
//                    boolean isEditorTextAreaCreationExisted = editorCu.findAll(ObjectCreationExpr.class).stream()
//                            .anyMatch(creation -> creation.getType().toString().equals("EditorTextArea"));
//
//                    if (isMultiLine) {
//                        assertTrue(isEditorTextAreaCreationExisted, "Editor with MULTILINE_TEXT property should use EditorTextArea");
//                    } else {
//                        assertTrue(isEditorTextFiled, "Editor without MULTILINE_TEXT property should use EditorTextField");
//                    }
//                }
//            }
//        }
//    }
//
//    private String extractEditorType(IfStmt ifStmt) {
//        return ifStmt.getThenStmt().findFirst(ReturnStmt.class)
//                .flatMap(returnStmt -> returnStmt.getExpression())
//                .map(this::getEditorTypeFromExpression)
//                .orElse(null);
//    }
//
//    private String getEditorTypeFromExpression(Expression expr) {
//        if (expr instanceof ObjectCreationExpr newExpr) {
//            return newExpr.getType().getNameAsString();
//        } else if (expr instanceof MethodCallExpr methodCall) {
//            if (methodCall.getNameAsString().equals("new")) {
//                return methodCall.getTypeArguments()
//                        .flatMap(types -> types.getFirst())
//                        .map(Object::toString)
//                        .orElse(methodCall.getArguments().get(0).toString());
//            }
//        }
//        return expr.toString();
//    }

    private static List<Path> targetClasses = new ArrayList<>();
    private static Logger logger = Logger.getLogger(FieldEditorTypeParserTest.class.getName());

    @BeforeAll
    public static void setUp() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        parser = new JavaParser(configuration);
        targetClasses = getAllClassPathImplementFX();
    }

    @Test
    public void analyzeFieldEditorFX() throws IOException {
        for (Path targetClass : targetClasses) {
            parser.parse(targetClass)
                    .getResult()
                    .ifPresent(cu -> {
                        cu.findAll(MethodCallExpr.class).stream()
                                .filter(methodCallExpr -> methodCallExpr.getNameAsString().equals("establishBinding"))
                                .findFirst()
                                .ifPresent(establishBindingCall -> {
                                    String firstArg = establishBindingCall.getArgument(0).toString();
                                    cu.findAll(FieldDeclaration.class).stream()
                                            .filter(field -> field.getVariable(0).getNameAsString().equals(firstArg))
                                            .findFirst()
                                            .ifPresent(fxComponentClass -> {
                                                Assumptions.assumeTrue(hasMULTILINE_TEXTPropertyCheck(cu), "For the class " + targetClass.getFileName() + " in " + targetClass.getParent() + ", it should have MULTILINE_TEXT property check");
                                                assertTrue(hasEditorTextAreaCreationExisted(cu), "For the class " + targetClass.getFileName() + " in " + targetClass.getParent() + ", it should use EditorTextArea for MULTILINE_TEXT property");
                                            });
                                });
                    });
        }
    }

    private static boolean hasEditorTextAreaCreationExisted(CompilationUnit cu) {
        return cu.findAll(ObjectCreationExpr.class).stream()
                .anyMatch(creation -> creation.getType().toString().equals("EditorTextArea"));
    }

    private static boolean hasMULTILINE_TEXTPropertyCheck(CompilationUnit cu) {
        // match "contains(FieldProperty.PERSON_NAMES)"
        AtomicBoolean hasCheck = new AtomicBoolean(false);
        cu.findAll(ConditionalExpr.class).stream()
                .filter(conditionalExpr -> conditionalExpr.getCondition().toString().contains("contains(FieldProperty.MULTILINE_TEXT)"))
                .findFirst()
                .ifPresent(conditionalExpr -> hasCheck.set(true));
        cu.findAll(IfStmt.class).stream()
                .filter(ifStmt -> ifStmt.getCondition().toString().contains("contains(FieldProperty.MULTILINE_TEXT)"))
                .findFirst()
                .ifPresent(ifStmt -> hasCheck.set(true));
        return hasCheck.get();
    }

    private static List<Path> getAllClassPathImplementFX() {
        List<Path> classPaths = new ArrayList<>();
        Path path = Paths.get("src/main/java");
        // walk the path
        try {
            Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            parser.parse(p)
                                    .getResult()
                                    .ifPresent(cu ->
                                            cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                                                    .filter(classDecl -> classDecl.getImplementedTypes().stream()
                                                            .anyMatch(type -> type.getNameAsString().equals("FieldEditorFX")))
                                                    .forEach(classDecl -> {
                                                        classPaths.add(p);
                                                    }));

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classPaths;
    }

//    private Set<FieldProperty> extractProperties(String condition) {
//        Set<FieldProperty> properties = new HashSet<>();
//
//        Pattern fieldPropertyPattern = Pattern.compile("FieldProperty\\.(\\w+)");
//        Matcher fieldPropertyMatcher = fieldPropertyPattern.matcher(condition);
//        while (fieldPropertyMatcher.find()) {
//            String propertyName = fieldPropertyMatcher.group(1);
//            try {
//                properties.add(FieldProperty.valueOf(propertyName));
//            } catch (IllegalArgumentException e) {
//                System.err.println("Unknown FieldProperty: " + propertyName);
//            }
//        }
//
//        Pattern fieldPattern = Pattern.compile("(StandardField|InternalField)\\.(\\w+)");
//        Matcher fieldMatcher = fieldPattern.matcher(condition);
//        while (fieldMatcher.find()) {
//            String fieldType = fieldMatcher.group(1);
//            String fieldName = fieldMatcher.group(2);
//            try {
//                Field field;
//                if ("StandardField".equals(fieldType)) {
//                    field = StandardField.valueOf(fieldName);
//                } else {
//                    field = InternalField.valueOf(fieldName);
//                }
//                properties.addAll(field.getProperties());
//            } catch (IllegalArgumentException e) {
//                System.err.println("Unknown " + fieldType + ": " + fieldName);
//            }
//        }
//
//        return properties;
//    }
}
