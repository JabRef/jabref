package org.jabref.logic.icore;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.logic.icore.ConferenceUtils.extractStringFromParentheses;
import static org.jabref.logic.icore.ConferenceUtils.generateAcronymCandidates;
import static org.jabref.logic.icore.ConferenceUtils.normalize;
import static org.jabref.logic.icore.ConferenceUtils.removeAllParenthesesWithContent;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConferenceUtilsTest {
    @ParameterizedTest(name = "Extract from \"{0}\" should return \"{1}\"")
    @CsvSource({
            "(SERA), SERA",                                                     // Simple acronym extraction
            "Extract conference from full title (SERA), SERA",                  // Acronym in a conference title
            "This (SERA) has multiple (CONF) acronyms, SERA",                   // Extract only the first acronym encountered
            "This (C++SER@-20_26) has special characters, C++SER@-20_26",       // Special characters in acronym
            "Input with whitespace (     ACR     )', ACR",                      // Extract strips whitespace around acronym
            "(Nested (parentheses (SERA))), SERA",                              // Extract the acronym in the deepest parentheses
            "This open paren ((SERA) is never closed, SERA",                    // Extract acronym from incomplete parentheses
            "Input with empty () parentheses, ",                                // Empty acronym inside parentheses
            "Input with empty (        ) whitespace in parens, ",               // Only whitespace inside parentheses
            "'', "                                                              // Empty string
    })
    void acronymExtraction(String input, String expectedResult) {
        assertEquals(Optional.ofNullable(expectedResult), extractStringFromParentheses(input));
    }

    @ParameterizedTest(name = "RemoveParentheses for \"{0}\" should return \"{1}\"")
    @CsvSource({
            "'', ''",                                                           // Empty string
            "(removed), ''",                                                    // only parentheses string in input returns empty
            "(Everything (here is (removed))), ''",                             // removes only nested parentheses string
            "This is not removed (this is), 'This is not removed '",            // single parentheses string is removed
            "Removes (one) both (CONF) parens, Removes  both  parens",          // multiple parentheses strings are removed
            "Removes (this is (deep)) nested parens, Removes  nested parens",   // nested parentheses strings are removed
            "Doesn't remove (this, Doesn't remove (this",                       // open with missing close paren is not removed
            "Doesn't remove )this either, Doesn't remove )this either",         // close with missing open paren is not removed
            "Remove (this) but not )this, Remove  but not )this",               // matched paren pair is removed while close with missing open paren is not removed
            "Multiple (one (two)) parens are (three) removed, Multiple  parens are  removed",   // combination of single and nested parens
            "Empty () parentheses, Empty  parentheses"                          // empty parentheses are removed
    })
    void removeParenthesesWithContent(String input, String expectedResult) {
        assertEquals(expectedResult, removeAllParenthesesWithContent(input));
    }

    @ParameterizedTest(name = "Acronym candidates for \"{0}\" with cutoff at \"{1}\" should return \"{2}\"")
    @MethodSource("generateAcronymCandidateTestData")
    void acronymCandidateGeneration(String input, int cutoff, Set<String> expectedResult) {
        assertEquals(expectedResult, generateAcronymCandidates(input, cutoff));
    }

    static Stream<Arguments> generateAcronymCandidateTestData() {
        return Stream.of(
                // Edge cases
                Arguments.of("", 2, Set.of()), // Empty string returns empty set
                Arguments.of("foo", -1, Set.of()), // Negative cutoff returns empty set
                Arguments.of("foo", 0, Set.of()), // Zero cutoff returns empty set
                Arguments.of("foo", 1, Set.of()), // Cutoff too small
                Arguments.of("bar", 3, Set.of("bar")),  // Cutoff same as input length
                Arguments.of("bar", 15, Set.of("bar")),  // Cutoff larger than input length
                Arguments.of("a", 1, Set.of("a")), // Single character
                Arguments.of("       ", 3, Set.of()), // Only whitespace in input

                // Basic delimiter cases (space)
                Arguments.of("ACR NYM", 3, Set.of("ACR", "NYM")), // Two acronyms, exact cutoff
                Arguments.of("ACR NYM", 2, Set.of()), // Cutoff too small for acronyms
                Arguments.of("ACR NYM", 7, Set.of("ACR NYM", "ACR", "NYM")), // Includes full string for large enough cutoff
                Arguments.of("A B C", 1, Set.of("A", "B", "C")), // Single chars
                Arguments.of("A B C", 3, Set.of("A", "B", "C", "A B", "B C")), // Partial subset combinations
                Arguments.of("A B C", 5, Set.of("A B C", "A B", "B C", "A", "B", "C")), // All subset combinations

                // Multiple delimiter types
                Arguments.of("ACRO_2012", 4, Set.of("ACRO", "2012")), // Underscore delimiter
                Arguments.of("ACRO.2012", 4, Set.of("ACRO", "2012")), // Dot delimiter
                Arguments.of("ACRO'2012", 4, Set.of("ACRO", "2012")), // Apostrophe delimiter
                Arguments.of("ACRO:NYM", 4, Set.of("ACRO", "NYM")), // Colon delimiter
                Arguments.of("ACRO-NYM", 4, Set.of("ACRO", "NYM")), // Hyphen delimiter
                Arguments.of("ACRO,NYM", 4, Set.of("ACRO", "NYM")), // Comma delimiter

                Arguments.of("ACRO+NYM@CONF#2018", 20, Set.of("ACRO+NYM@CONF#2018")), // Does not split non-delimiters

                // Delimiter trimming cases
                Arguments.of("____ACRO", 6, Set.of("ACRO")), // trim delimiter on beginning
                Arguments.of("ACRO____", 6, Set.of("ACRO")), // trim delimiter on end
                Arguments.of("____ACRO____", 6, Set.of("ACRO")), // trim delimiter on both sides
                Arguments.of("_,-:.ACRO.:-,_", 6, Set.of("ACRO")), // trim different adjacent delimiters on both sides
                Arguments.of("' _ : . , ", 9, Set.of()), // delimiter only string returns empty
                Arguments.of("_,-:.ACRO_-NYM.:-,_", 10, Set.of("ACRO_-NYM", "ACRO", "NYM")), // trimming keeps delimiters between acronym but strips other subsets
                Arguments.of("a.b_c:d", 3, Set.of("a.b", "b_c", "c:d", "a", "b", "c", "d")), // interleaved delimiters between valid characters preserves middle-delimiters in subsets

                // Subset generation with real-world examples
                Arguments.of("CLOSER'2022", 11, Set.of("CLOSER'2022", "CLOSER", "2022")), // keeps original string at the head if below cutoff
                Arguments.of("CLOSER'2022", 6, Set.of("CLOSER", "2022")), // discards original string if above cutoff
                Arguments.of("ACM_WiSec'2022", 11, Set.of("WiSec'2022", "ACM_WiSec", "WiSec", "2022", "ACM")), // keeps longer candidates as well
                Arguments.of("IEEE-IV'2022", 11, Set.of("IEEE-IV", "IV'2022", "2022", "IEEE", "IV")) // composite acronyms are pushed ahead for better matching
        );
    }

    @ParameterizedTest(name = "normalize(\"{0}\") should return \"{1}\"")
    @CsvSource({
            "'', ''",   // empty string
            "title, title",   // nothing to normalize
            "'   hello     world    ', helloworld",    // removes all space

            // Parentheses removal
            "'conference (icml 2018)', conference", // removes parentheses with content and strip
            "'(start) middle (end)', middle",   // removes all parentheses with content
            "'nested (inner (deep) content) text', nestedtext", // removes all parentheses with content regardless of depth
            "'multiple (first) (second) (third)', multiple",    // removes multiple consecutive parentheses with content
            "'keep this (but (not this)) (but do keep this', keepthisbutdokeepthis",    // doesn't remove content if open paren is never closed
            "'keep this (but (not this)) )but do keep this', keepthisbutdokeepthis",    // doesn't remove content for isolated close paren

            // Year removal (19XX and 20XX)
            "'conference 2018 workshop 1999', conferenceworkshop",  // removes years on their own
            "'start-2019end', startend",    // removes years between strings
            "'conference 1800 keeps old years', conference1800keepsoldyears",   // keeps non-matching years
            "'health conference 42 test', healthconference42test",    // does not remove non-matching numbers on their own
            "conference201workshop, conference201workshop", // does not remove other numbers between string

            // Ordinal removal (simple and LaTeX)
            "'1st 2nd 3rd 4th 21st 22nd 23rd 101st conference', conference",
            "'3\\textsuperscript{rd} 17\\textsuperscript{th} 1\\textsuperscript{st} meeting', meeting", // need to escape \t otherwise it is read as tab

            // Stopword removal
            "'proceedings volume part papers combined', combined",  // regular stopwords
            "'proceedings of the conference volume part', conference",  // interleaved with a false start
            "'january february march april may june july august september october november december conference', conference",   // all months
            "'conference january workshop february meeting', conferenceworkshopmeeting",    // months interleaved

            // False start removal
            "'of the conference', conference",
            "'of machine learning', machinelearning",
            "'the big conference', bigconference",
            "'ofthe combined', combined",
            "'the proceedings of the 25th international conference on machine learning', internationalconferenceonmachinelearning", // removes multiple false starts weaved between stopwords

            // Special characters and punctuation
            "'conference & workshop - machine learning: ai @ home # 1 . test _ more', conferenceworkshopmachinelearningaihome1testmore",
            "'symbols !@#$%^&*()_+-=[]{}|;:,.<>?', symbols",

            // Edge cases leading to empty string output
            "2018, ''", // only year
            "'(only parentheses)', ''", // only parentheses content
            "'proceedings volume part papers', ''", // only stop words
            "'1st 2nd 3rd', ''",    // only ordinals
            "'january february march', ''", // only months
            "'2018 (removed) 1st proceedings march', ''",   // combination of above

            // Some real-world examples with cases interleaved
            "'proceedings of the 3rd international conference on machine learning (icml 2018)', internationalconferenceonmachinelearning",
            "'21st annual conference on neural information processing (nips 2019)', annualconferenceonneuralinformationprocessing",
            "'part i: the 15th conference (2020) & workshop proceedings, january edition', itheconferenceworkshopedition"
    })
    void titleNormalization(String input, String expected) {
        assertEquals(expected, normalize(input));
    }
}
