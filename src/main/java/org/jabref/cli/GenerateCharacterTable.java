package org.jabref.cli;

import java.util.Map;
import java.util.TreeMap;

import org.jabref.logic.util.strings.HTMLUnicodeConversionMaps;

public class GenerateCharacterTable {

    private GenerateCharacterTable() {
    }

    public static void main(String[] args) {
        Map<Integer, String> characterMap = new TreeMap<>(HTMLUnicodeConversionMaps.NUMERICAL_LATEX_CONVERSION_MAP);
        System.out.println("\\documentclass[10pt, a4paper]{article}");
        System.out.println("\\usepackage[T5,T1]{fontenc}");
        System.out.println("\\usepackage{amssymb}");
        System.out.println("\\usepackage{amsmath}");
        System.out.println("\\usepackage{txfonts}");
        System.out.println("\\usepackage{xfrac}");
        System.out.println("\\usepackage{combelow}");
        System.out.println("\\usepackage{textcomp}");
        System.out.println("\\usepackage{mathspec}");
        System.out.println("\\usepackage{fontspec}");
        System.out.println("\\usepackage[a4paper,margin=1cm]{geometry}");
        System.out.println("\\usepackage{supertabular}");
        System.out.println("\\usepackage{mathabx}");
        System.out.println("\\fontspec{Cambria}");
        System.out.println("\\DeclareTextSymbolDefault{\\OHORN}{T5}");
        System.out.println("\\DeclareTextSymbolDefault{\\UHORN}{T5}");
        System.out.println("\\DeclareTextSymbolDefault{\\ohorn}{T5}");
        System.out.println("\\DeclareTextSymbolDefault{\\uhorn}{T5}");
        System.out.println("\\begin{document}");
        System.out.println("\\twocolumn");
        System.out.println("\\begin{supertabular}{c|c|c|c|c}");
        System.out.println("No. & Uni & Symb & \\LaTeX & Code \\\\ \n \\hline");

        for (Map.Entry<Integer, String> character : characterMap.entrySet()) {
            System.out
                    .println(
                            character.getKey() + " & "
                                    + ((character.getKey() > 128) ?
                                    String.valueOf(Character.toChars(character.getKey())) : "")
                                    + " & \\symbol{" + Integer.toString(character.getKey()) + "} & "
                                    + character.getValue() + " & \\verb¤" + character.getValue() + "¤ \\\\");
        }
        System.out.println("\\end{supertabular}");
        System.out.println("\\end{document}");
    }
}
