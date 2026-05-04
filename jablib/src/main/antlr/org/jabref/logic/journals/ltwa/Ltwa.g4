grammar Ltwa;
options { caseInsensitive=true; }

@header{
package org.jabref.logic.journals.ltwa;
}

@lexer::members {
    private boolean isNextBoundary() {
        int lookAhead = _input.LA(1);
        return lookAhead == EOF ||
               lookAhead == ' ' || lookAhead == '\t' || lookAhead == '\r' || lookAhead == '\n' ||
               lookAhead == '-' || lookAhead == '\u2013' || lookAhead == '\u2014' ||
               lookAhead == '_' || lookAhead == '.' || lookAhead == ',' ||
               lookAhead == ':' || lookAhead == ';' || lookAhead == '!' ||
               lookAhead == '|' || lookAhead == '=' || lookAhead == '+' ||
               lookAhead == '*' || lookAhead == '\\' || lookAhead == '/' ||
               lookAhead == '"' || lookAhead == '(' || lookAhead == ')' ||
               lookAhead == '&' || lookAhead == '#' || lookAhead == '%' ||
               lookAhead == '@' || lookAhead == '$' || lookAhead == '?';
    }

    private boolean isNotHyphenated() {
        int lookAhead = _input.LA(1);
        return lookAhead == EOF || lookAhead != '-';
    }
}

fragment COMMON_ABBR_FRAGMENT options { caseInsensitive=false; }: 'St' | 'Mr' | 'Ms' | 'Mrs' | 'Mx' | 'Dr' | 'Prof' | 'vs';
fragment PART_ABBR_FRAGMENT: 'ser' | 'sect' | 'sec';

ABBREVIATION
    : (COMMON_ABBR_FRAGMENT | PART_ABBR_FRAGMENT) '.' {isNextBoundary()}?
    | ([A-Z] '.')+ {isNextBoundary()}?;

PART: ('series' | 'serie' | 'part' | 'section' | 'série' | 'supplemento' | 'chapter' | 'parte') {isNextBoundary()}?;

ORDINAL options { caseInsensitive=false; }: ([IVXivx]+ | [A-Z]) {isNextBoundary()}?;

ARTICLE: ('l\'' | 'd\'' | 'dell\'' | 'nell\'')
       | ('a' | 'an' | 'the'
       | 'der' | 'die' | 'das' | 'des' | 'dem' | 'den'
       | 'el' | 'la' | 'los' | 'las' | 'un' | 'una' | 'unos' | 'unas'
       | 'le' | 'la' | 'les' | 'un' | 'une' | 'des' | 'du' | 'de la' | 'au' | 'aux'
       | 'dell' | 'nell') {isNextBoundary()}? {isNotHyphenated()}?;

STOPWORD: ('a' | 'an' | 'the' | 'and' | 'but' | 'or' | 'for' | 'nor' | 'so' | 'yet' | 'though'
        | 'when' | 'whenever' | 'where' | 'whereas' | 'wherever' | 'while' | 'about' | 'afore'
        | 'after' | 'ago' | 'along' | 'amid' | 'among' | 'amongst' | 'apropos' | 'as' | 'at'
        | 'atop' | 'by' | 'ca' | 'circa' | 'from' | 'hence' | 'in' | 'into'
        | 'like' | 'of' | 'off' | 'on' | 'onto' | 'ontop' | 'out' | 'over' | 'per' | 'since'
        | 'than' | 'til' | 'till' | 'to' | 'unlike' | 'until' | 'unto' | 'up' | 'upon' | 'upside'
        | 'versus' | 'via' | 'vis-a-vis' | 'vs' | 'with' | 'within' | 'für' | 'und' | 'aus'
        | 'zu' | 'zur' | 'im' | 'de' | 'et' | 'y' | 'del' | 'en' | 'di' | 'e' | 'da' | 'delle'
        | 'della' | 'sue' | 'el' | 'do' | 'og' | 'i' | 'voor' | 'van' | 'dell\'' | 'dell' | 'ed'
        | 'för' | 'tot' | 'vir' | 'o' | 'its' | 'sul') {isNextBoundary()}? {isNotHyphenated()}?;

HYPHEN: '-';

SYMBOLS: [.,;!?&+=*#%@$] | '\'';

fragment LETTER:
    [A-Z]
  | [a-z]
  | '\u00C0'..'\u00D6'   // À–Ö
  | '\u00D8'..'\u00F6'   // Ø–ö
  | '\u00F8'..'\u00FF'   // ø–ÿ
  | '\u0100'..'\u017F'   // Extended Latin letters: Ā–ſ
  | '\u4E00'..'\u9FFF';  // Chinese/Japanese/Korean characters

WORD: (LETTER+ '\'' + [a-z]) {isNextBoundary()}?              // e.g., Shi'a, parent's
    | (LETTER+ '\'') {isNextBoundary()}?                      // Word ending with apostrophe, e.g., Parents' (plural possessive)
    | LETTER + ('.' + LETTER+)+ {isNextBoundary()}?           // e.g., Humana.Mente
    | (LETTER | [0-9])+ {isNextBoundary()}?;                  // Regular word

WS: [ \t\r\n]+ -> skip;

// Parser rules
title
    : singleWordTitle EOF  #SingleWordTitleFull
    | stopwordPlusAny EOF #StopwordPlusTitleFull
    | anyPlusSymbols EOF  #AnyPlusSymbolsFull
    | normalTitle EOF     #NormalTitleFull
    ;

singleWordTitle
    : (WORD | STOPWORD | PART | ORDINAL | ABBREVIATION)
    ;

stopwordPlusAny
    : STOPWORD (WORD | PART | ORDINAL | ABBREVIATION)
    ;

anyPlusSymbols
    : (WORD | STOPWORD | PART | ORDINAL | ABBREVIATION) SYMBOLS
    ;

normalTitle
    : titleElement+
    ;

titleElement
    : article      #ArticleElement
    | stopword     #StopwordElement
    | symbols      #SymbolsElement
    | ordinal      #OrdinalElement
    | word         #WordElement
    | hyphen       #HyphenElement
    | part         #PartElement
    | abbreviation #AbbreviationElement
    ;

// Rules for each token type
article      : ARTICLE;
stopword     : STOPWORD;
symbols      : SYMBOLS;
ordinal      : ORDINAL;
word         : WORD;
hyphen       : HYPHEN;
part         : PART;
abbreviation : ABBREVIATION;
