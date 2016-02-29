package net.sf.jabref.logic.util.strings;

import java.util.HashMap;
import java.util.Map;

public class HTMLUnicodeConversionMaps {

    /*   Portions © International Organization for Standardization 1986:
     Permission to copy in any form is granted for use with
     conforming SGML systems and applications as defined in
     ISO 8879, provided this notice is included in all copies.
     */

    // most of the LaTeX commands can be read at http://en.wikibooks.org/wiki/LaTeX/Accents
    // The symbols can be seen at http://www.fileformat.info/info/unicode/char/a4/index.htm. Replace "a4" with the U+ number
    // http://detexify.kirelabs.org/classify.html and http://www.ctan.org/tex-archive/info/symbols/comprehensive/ might help to find the right LaTeX command
    // http://llg.cubic.org/docs/ent2latex.html and http://www.w3.org/TR/xml-entity-names/byalpha.html are also useful
    // as well as http://www.w3.org/Math/characters/unicode.xml

    // An array of arrays of strings in the format:
    // {"decimal number of HTML entity", "text HTML entity", "corresponding LaTeX command"}
    // Leaving a field empty is OK as it then will not be included
    private static final String[][] CONVERSION_LIST = new String[][] {{"160", "nbsp", "\\{~\\}"}, // no-break space = non-breaking space,
            //                                 U+00A0 ISOnum
            {"161", "iexcl", "\\{\\\\textexclamdown\\}"}, // inverted exclamation mark, U+00A1 ISOnum
            {"162", "cent", "\\{\\\\textcent\\}"}, // cent sign, U+00A2 ISOnum
            {"163", "pound", "\\{\\\\pounds\\}"}, // pound sign, U+00A3 ISOnum
            {"164", "curren", "\\{\\\\textcurrency\\}"}, // currency sign, U+00A4 ISOnum
            {"165", "yen", "\\{\\\\textyen\\}"}, // yen sign = yuan sign, U+00A5 ISOnum
            {"166", "brvbar", "\\{\\\\textbrokenbar\\}"}, // broken bar = broken vertical bar,
            //                                 U+00A6 ISOnum
            {"167", "sect", "\\{\\\\S\\}"}, // section sign, U+00A7 ISOnum
            {"168", "uml", "\\{\\\\\"\\{\\}\\}"}, // diaeresis = spacing diaeresis,
            //                                 U+00A8 ISOdia
            {"169", "copy", "\\{\\\\copyright\\}"}, // copyright sign, U+00A9 ISOnum
            {"170", "ordf", "\\{\\\\textordfeminine\\}"}, // feminine ordinal indicator, U+00AA ISOnum
            {"171", "laquo", "\\{\\\\guillemotleft\\}"}, // left-pointing double angle quotation mark
            //                                 = left pointing guillemet, U+00AB ISOnum
            {"172", "not", "\\$\\\\neg\\$"}, // not sign, U+00AC ISOnum
            {"173", "shy", "\\\\-"}, // soft hyphen = discretionary hyphen,
            //                                 U+00AD ISOnum
            {"174", "reg", "\\{\\\\textregistered\\}"}, // registered sign = registered trade mark sign,
            //                                 U+00AE ISOnum
            {"175", "macr", "\\{\\\\=\\{\\}\\}"}, // macron = spacing macron = overline
            //                                 = APL overbar, U+00AF ISOdia
            {"176", "deg", "\\$\\\\deg\\$"}, // degree sign, U+00B0 ISOnum
            {"177", "plusmn", "\\$\\\\pm\\$"}, // plus-minus sign = plus-or-minus sign,
            //                                 U+00B1 ISOnum
            {"178", "sup2", "\\\\textsuperscript\\{2\\}"}, // superscript two = superscript digit two
            //                                 = squared, U+00B2 ISOnum
            {"179", "sup3", "\\\\textsuperscript\\{3\\}"}, // superscript three = superscript digit three
            //                                 = cubed, U+00B3 ISOnum
            {"180", "acute", "\\{\\\\'\\{\\}\\}"}, // acute accent = spacing acute,
            //                                 U+00B4 ISOdia
            {"181", "micro", "\\$\\\\mu\\$"}, // micro sign, U+00B5 ISOnum
            {"", "mu", "\\$\\\\mu\\$"}, // micro sign, U+00B5 ISOnum
            {"182", "para", "\\{\\\\P\\}"}, // pilcrow sign = paragraph sign,
            //                                 U+00B6 ISOnum
            {"183", "middot", "\\$\\\\cdot\\$"}, // middle dot = Georgian comma
            //                                 = Greek middle dot, U+00B7 ISOnum
            {"184", "cedil", "\\{\\\\c\\{\\}\\}"}, // cedilla = spacing cedilla, U+00B8 ISOdia
            {"185", "sup1", "\\\\textsuperscript\\{1\\}"}, // superscript one = superscript digit one,
            //                                 U+00B9 ISOnum
            {"186", "ordm", "\\{\\\\textordmasculine\\}"}, // masculine ordinal indicator,
            //                                 U+00BA ISOnum
            {"187", "raquo", "\\{\\\\guillemotright\\}"}, // right-pointing double angle quotation mark
            //                                 = right pointing guillemet, U+00BB ISOnum
            {"188", "frac14", "\\$\\\\sfrac\\{1\\}\\{4\\}\\$"}, // vulgar fraction one quarter
            //                                 = fraction one quarter, U+00BC ISOnum
            {"189", "frac12", "\\$\\\\sfrac\\{1\\}\\{2\\}\\$"}, // vulgar fraction one half
            //                                 = fraction one half, U+00BD ISOnum
            {"190", "frac34", "\\$\\\\sfrac\\{3\\}\\{4\\}\\$"}, // vulgar fraction three quarters
            //                                 = fraction three quarters, U+00BE ISOnum
            {"191", "iquest", "\\{\\\\textquestiondown\\}"}, // inverted question mark
            //                                 = turned question mark, U+00BF ISOnum
            {"192", "Agrave", "\\{\\\\`\\{A\\}\\}"}, // latin capital letter A with grave
            //                                 = latin capital letter A grave,
            //                                 U+00C0 ISOlat1
            {"193", "Aacute", "\\{\\\\'\\{A\\}\\}"}, // latin capital letter A with acute,
            //                                 U+00C1 ISOlat1
            {"194", "Acirc", "\\{\\\\\\^\\{A\\}\\}"}, // latin capital letter A with circumflex,
            //                                 U+00C2 ISOlat1
            {"195", "Atilde", "\\{\\\\~\\{A\\}\\}"}, // latin capital letter A with tilde,
            //                                 U+00C3 ISOlat1
            {"196", "Auml", "\\{\\\\\"\\{A\\}\\}"}, // latin capital letter A with diaeresis,
            //                                 U+00C4 ISOlat1
            {"197", "Aring", "\\{\\{\\\\AA\\}\\}"}, // latin capital letter A with ring above
            //                                 = latin capital letter A ring,
            //                                 U+00C5 ISOlat1
            {"198", "AElig", "\\{\\\\AE\\}"}, // latin capital letter AE
            //                                 = latin capital ligature AE,
            //                                 U+00C6 ISOlat1
            {"199", "Ccedil", "\\{\\\\c\\{C\\}\\}"}, // latin capital letter C with cedilla,
            //                                 U+00C7 ISOlat1
            {"200", "Egrave", "\\{\\\\`\\{E\\}\\}"}, // latin capital letter E with grave,
            //                                 U+00C8 ISOlat1
            {"201", "Eacute", "\\{\\\\'\\{E\\}\\}"}, // latin capital letter E with acute,
            //                                 U+00C9 ISOlat1
            {"202", "Ecirc", "\\{\\\\\\^\\{E\\}\\}"}, // latin capital letter E with circumflex,
            //                                 U+00CA ISOlat1
            {"203", "Euml", "\\{\\\\\"\\{E\\}\\}"}, // latin capital letter E with diaeresis,
            //                                 U+00CB ISOlat1
            {"204", "Igrave", "\\{\\\\`\\{I\\}\\}"}, // latin capital letter I with grave,
            //                                 U+00CC ISOlat1
            {"205", "Iacute", "\\{\\\\'\\{I\\}\\}"}, // latin capital letter I with acute,
            //                                 U+00CD ISOlat1
            {"206", "Icirc", "\\{\\\\\\^\\{I\\}\\}"}, // latin capital letter I with circumflex,
            //                                 U+00CE ISOlat1
            {"207", "Iuml", "\\{\\\\\"\\{I\\}\\}"}, // latin capital letter I with diaeresis,
            //                                 U+00CF ISOlat1
            {"208", "ETH", "\\{\\\\DH\\}"}, // latin capital letter ETH, U+00D0 ISOlat1
            {"209", "Ntilde", "\\{\\\\~\\{N\\}\\}"}, // latin capital letter N with tilde,
            //                                 U+00D1 ISOlat1
            {"210", "Ograve", "\\{\\\\`\\{O\\}\\}"}, // latin capital letter O with grave,
            //                                 U+00D2 ISOlat1
            {"211", "Oacute", "\\{\\\\'\\{O\\}\\}"}, // latin capital letter O with acute,
            //                                 U+00D3 ISOlat1
            {"212", "Ocirc", "\\{\\\\\\^\\{O\\}\\}"}, // latin capital letter O with circumflex,
            //                                 U+00D4 ISOlat1
            {"213", "Otilde", "\\{\\\\~\\{O\\}\\}"}, // latin capital letter O with tilde,
            //                                 U+00D5 ISOlat1
            {"214", "Ouml", "\\{\\\\\"\\{O\\}\\}"}, // latin capital letter O with diaeresis,
            //                                 U+00D6 ISOlat1
            {"215", "times", "\\$\\\\times\\$"}, // multiplication sign, U+00D7 ISOnum
            {"216", "Oslash", "\\{\\\\O\\}"}, // latin capital letter O with stroke
            //                                 = latin capital letter O slash,
            //                                 U+00D8 ISOlat1
            {"217", "Ugrave", "\\{\\\\`\\{U\\}\\}"}, // latin capital letter U with grave,
            //                                 U+00D9 ISOlat1
            {"218", "Uacute", "\\{\\\\'\\{U\\}\\}"}, // latin capital letter U with acute,
            //                                 U+00DA ISOlat1
            {"219", "Ucirc", "\\{\\\\\\^\\{U\\}\\}"}, // latin capital letter U with circumflex,
            //                                 U+00DB ISOlat1
            {"220", "Uuml", "\\{\\\\\"\\{U\\}\\}"}, // latin capital letter U with diaeresis,
            //                                 U+00DC ISOlat1
            {"221", "Yacute", "\\{\\\\'\\{Y\\}\\}"}, // latin capital letter Y with acute,
            //                                 U+00DD ISOlat1
            {"222", "THORN", "\\{\\\\TH\\}"}, // latin capital letter THORN,
            //                                 U+00DE ISOlat1
            {"223", "szlig", "\\{\\\\ss\\}"}, // latin small letter sharp s = ess-zed,
            //                                 U+00DF ISOlat1
            {"224", "agrave", "\\{\\\\`\\{a\\}\\}"}, // latin small letter a with grave
            //                                 = latin small letter a grave,
            //                                 U+00E0 ISOlat1
            {"225", "aacute", "\\{\\\\'\\{a\\}\\}"}, // latin small letter a with acute,
            //                                 U+00E1 ISOlat1
            {"226", "acirc", "\\{\\\\\\^\\{a\\}\\}"}, // latin small letter a with circumflex,
            //                                 U+00E2 ISOlat1
            {"227", "atilde", "\\{\\\\~\\{a\\}\\}"}, // latin small letter a with tilde,
            //                                 U+00E3 ISOlat1
            {"228", "auml", "\\{\\\\\"\\{a\\}\\}"}, // latin small letter a with diaeresis,
            //                                 U+00E4 ISOlat1
            {"229", "aring", "\\{\\{\\\\aa\\}\\}"}, // latin small letter a with ring above
            //                                 = latin small letter a ring,
            //                                 U+00E5 ISOlat1
            {"230", "aelig", "\\{\\\\ae\\}"}, // latin small letter ae
            //                                 = latin small ligature ae, U+00E6 ISOlat1
            {"231", "ccedil", "\\{\\\\c\\{c\\}\\}"}, // latin small letter c with cedilla,
            //                                 U+00E7 ISOlat1
            {"232", "egrave", "\\{\\\\`\\{e\\}\\}"}, // latin small letter e with grave,
            //                                 U+00E8 ISOlat1
            {"233", "eacute", "\\{\\\\'\\{e\\}\\}"}, // latin small letter e with acute,
            //                                 U+00E9 ISOlat1
            {"234", "ecirc", "\\{\\\\\\^\\{e\\}\\}"}, // latin small letter e with circumflex,
            //                                 U+00EA ISOlat1
            {"235", "euml", "\\{\\\\\"\\{e\\}\\}"}, // latin small letter e with diaeresis,
            //                                 U+00EB ISOlat1
            {"236", "igrave", "\\{\\\\`\\{i\\}\\}"}, // latin small letter i with grave,
            //                                 U+00EC ISOlat1
            {"237", "iacute", "\\{\\\\'\\{i\\}\\}"}, // latin small letter i with acute,
            //                                 U+00ED ISOlat1
            {"238", "icirc", "\\{\\\\\\^\\{i\\}\\}"}, // latin small letter i with circumflex,
            //                                 U+00EE ISOlat1
            {"239", "iuml", "\\{\\\\\"\\{i\\}\\}"}, // latin small letter i with diaeresis,
            //                                 U+00EF ISOlat1
            {"240", "eth", "\\{\\\\dh\\}"}, // latin small letter eth, U+00F0 ISOlat1
            {"241", "ntilde", "\\{\\\\~\\{n\\}\\}"}, // latin small letter n with tilde,
            //                                 U+00F1 ISOlat1
            {"242", "ograve", "\\{\\\\`\\{o\\}\\}"}, // latin small letter o with grave,
            //                                 U+00F2 ISOlat1
            {"243", "oacute", "\\{\\\\'\\{o\\}\\}"}, // latin small letter o with acute,
            //                                 U+00F3 ISOlat1
            {"244", "ocirc", "\\{\\\\\\^\\{o\\}\\}"}, // latin small letter o with circumflex,
            //                                 U+00F4 ISOlat1
            {"245", "otilde", "\\{\\\\~\\{o\\}\\}"}, // latin small letter o with tilde,
            //                                 U+00F5 ISOlat1
            {"246", "ouml", "\\{\\\\\"\\{o\\}\\}"}, // latin small letter o with diaeresis,
            //                                 U+00F6 ISOlat1
            {"247", "divide", "\\$\\\\div\\$"}, // division sign, U+00F7 ISOnum
            {"248", "oslash", "\\{\\\\o\\}"}, // latin small letter o with stroke,
            //                                 = latin small letter o slash,
            //                                 U+00F8 ISOlat1
            {"249", "ugrave", "\\{\\\\`\\{u\\}\\}"}, // latin small letter u with grave,
            //                                 U+00F9 ISOlat1
            {"250", "uacute", "\\{\\\\'\\{u\\}\\}"}, // latin small letter u with acute,
            //                                 U+00FA ISOlat1
            {"251", "ucirc", "\\{\\\\\\^\\{u\\}\\}"}, // latin small letter u with circumflex,
            //                                 U+00FB ISOlat1
            {"252", "uuml", "\\{\\\\\"\\{u\\}\\}"}, // latin small letter u with diaeresis,
            //                                 U+00FC ISOlat1
            {"253", "yacute", "\\{\\\\'\\{y\\}\\}"}, // latin small letter y with acute,
            //                                 U+00FD ISOlat1
            {"254", "thorn", "\\{\\\\th\\}"}, // latin small letter thorn,
            //                                 U+00FE ISOlat1
            {"255", "yuml", "\\{\\\\\"\\{y\\}\\}"}, // latin small letter y with diaeresis,
            //                                 U+00FF ISOlat1
            {"332", "Omacro", "\\{\\\\=\\{O\\}\\}"}, // the small letter o with macron
            {"333", "omacro", "\\{\\\\=\\{o\\}\\}"}, // the big letter O with macron
            {"402", "fnof", "\\$f\\$"}, // latin small f with hook = function
            //                                   = florin, U+0192 ISOtech

            /* Greek */
            {"913", "Alpha", "\\{\\$\\\\Alpha\\$\\}"}, // greek capital letter alpha, U+0391
            {"914", "Beta", "\\{\\$\\\\Beta\\$\\}"}, // greek capital letter beta, U+0392
            {"915", "Gamma", "\\{\\$\\\\Gamma\\$\\}"}, // greek capital letter gamma,
            //                                   U+0393 ISOgrk3
            {"916", "Delta", "\\{\\$\\\\Delta\\$\\}"}, // greek capital letter delta,
            //                                   U+0394 ISOgrk3
            {"917", "Epsilon", "\\{\\$\\\\Epsilon\\$\\}"}, // greek capital letter epsilon, U+0395
            {"918", "Zeta", "\\{\\$\\\\Zeta\\$\\}"}, // greek capital letter zeta, U+0396
            {"919", "Eta", "\\{\\$\\\\Eta\\$\\}"}, // greek capital letter eta, U+0397
            {"920", "Theta", "\\{\\$\\\\Theta\\$\\}"}, // greek capital letter theta,
            //                                   U+0398 ISOgrk3
            {"921", "Iota", "\\{\\$\\\\Iota\\$\\}"}, // greek capital letter iota, U+0399
            {"922", "Kappa", "\\{\\$\\\\Kappa\\$\\}"}, // greek capital letter kappa, U+039A
            {"923", "Lambda", "\\{\\$\\\\Lambda\\$\\}"}, // greek capital letter lambda,
            //                                   U+039B ISOgrk3
            {"924", "Mu", "\\{\\$\\\\Mu\\$\\}"}, // greek capital letter mu, U+039C
            {"925", "Nu", "\\{\\$\\\\Nu\\$\\}"}, // greek capital letter nu, U+039D
            {"926", "Xi", "\\{\\$\\\\Xi\\$\\}"}, // greek capital letter xi, U+039E ISOgrk3
            {"927", "Omicron", "\\{\\$\\\\Omicron\\$\\}"}, // greek capital letter omicron, U+039F
            {"928", "Pi", "\\{\\$\\\\Pi\\$\\}"}, // greek capital letter pi, U+03A0 ISOgrk3
            {"929", "Rho", "\\{\\$\\\\Rho\\$\\}"}, // greek capital letter rho, U+03A1
            /* there is no Sigmaf, and no U+03A2 character either */
            {"931", "Sigma", "\\{\\$\\\\Sigma\\$\\}"}, // greek capital letter sigma,
            //                                   U+03A3 ISOgrk3
            {"932", "Tau", "\\{\\$\\\\Tau\\$\\}"}, // greek capital letter tau, U+03A4
            {"933", "Upsilon", "\\{\\$\\\\Upsilon\\$\\}"}, // greek capital letter upsilon,
            //                                   U+03A5 ISOgrk3
            {"934", "Phi", "\\{\\$\\\\Phi\\$\\}"}, // greek capital letter phi,
            //                                   U+03A6 ISOgrk3
            {"935", "Chi", "\\{\\$\\\\Chi\\$\\}"}, // greek capital letter chi, U+03A7
            {"936", "Psi", "\\{\\$\\\\Psi\\$\\}"}, // greek capital letter psi,
            //                                   U+03A8 ISOgrk3
            {"937", "Omega", "\\{\\$\\\\Omega\\$\\}"}, // greek capital letter omega,
            //                                   U+03A9 ISOgrk3

            {"945", "alpha", "\\$\\\\alpha\\$"}, // greek small letter alpha,
            //                                   U+03B1 ISOgrk3
            {"946", "beta", "\\$\\\\beta\\$"}, // greek small letter beta, U+03B2 ISOgrk3
            {"947", "gamma", "\\$\\\\gamma\\$"}, // greek small letter gamma,
            //                                   U+03B3 ISOgrk3
            {"948", "delta", "\\$\\\\delta\\$"}, // greek small letter delta,
            //                                   U+03B4 ISOgrk3
            {"949", "epsilon", "\\$\\\\epsilon\\$"}, // greek small letter epsilon,
            //                                   U+03B5 ISOgrk3
            {"950", "zeta", "\\$\\\\zeta\\$"}, // greek small letter zeta, U+03B6 ISOgrk3
            {"951", "eta", "\\$\\\\eta\\$"}, // greek small letter eta, U+03B7 ISOgrk3
            {"952", "theta", "\\$\\\\theta\\$"}, // greek small letter theta,
            //                                   U+03B8 ISOgrk3
            {"953", "iota", "\\$\\\\iota\\$"}, // greek small letter iota, U+03B9 ISOgrk3
            {"954", "kappa", "\\$\\\\kappa\\$"}, // greek small letter kappa,
            //                                   U+03BA ISOgrk3
            {"955", "lambda", "\\$\\\\lambda\\$"}, // greek small letter lambda,
            //                                   U+03BB ISOgrk3
            {"956", "mu", "\\$\\\\mu\\$"}, // greek small letter mu, U+03BC ISOgrk3
            {"957", "nu", "\\$\\\\nu\\$"}, // greek small letter nu, U+03BD ISOgrk3
            {"958", "xi", "\\$\\\\xi\\$"}, // greek small letter xi, U+03BE ISOgrk3
            {"959", "omicron", "\\$\\\\omicron\\$"}, // greek small letter omicron, U+03BF NEW
            {"960", "pi", "\\$\\\\phi\\$"}, // greek small letter pi, U+03C0 ISOgrk3
            {"961", "rho", "\\$\\\\rho\\$"}, // greek small letter rho, U+03C1 ISOgrk3
            {"962", "sigmaf", "\\$\\\\varsigma\\$"}, // greek small letter final sigma,
            //                                   U+03C2 ISOgrk3
            {"963", "sigma", "\\$\\\\sigma\\$"}, // greek small letter sigma,
            //                                   U+03C3 ISOgrk3
            {"964", "tau", "\\$\\\\tau\\$"}, // greek small letter tau, U+03C4 ISOgrk3
            {"965", "upsilon", "\\$\\\\upsilon\\$"}, // greek small letter upsilon,
            {"", "upsi", "\\$\\\\upsilon\\$"}, // alias
            //                                   U+03C5 ISOgrk3
            {"966", "phi", "\\$\\\\phi\\$"}, // greek small letter phi, U+03C6 ISOgrk3
            {"967", "chi", "\\$\\\\chi\\$"}, // greek small letter chi, U+03C7 ISOgrk3
            {"968", "psi", "\\$\\\\psi\\$"}, // greek small letter psi, U+03C8 ISOgrk3
            {"969", "omega", "\\$\\\\omega\\$"}, // greek small letter omega,
            //                                   U+03C9 ISOgrk3
            {"977", "thetasym", "\\$\\\\vartheta\\$"}, // greek small letter theta symbol,
            {"", "thetav", "\\$\\\\vartheta\\$"}, // greek small letter theta symbol,
            {"", "vartheta", "\\$\\\\vartheta\\$"}, // greek small letter theta symbol,
            //                                   U+03D1 NEW
            {"978", "upsih", "\\{\\$\\\\Upsilon\\$\\}"}, // greek upsilon with hook symbol,
            //                                   U+03D2 NEW
            {"982", "piv", "\\$\\\\varphi\\$"}, // greek pi symbol, U+03D6 ISOgrk3

            /* General Punctuation */
            {"8226", "bull", "\\$\\\\bullet\\$"}, // bullet = black small circle,
            //                                    U+2022 ISOpub
            /* bullet is NOT the same as bullet operator, U+2219 */
            {"8230", "hellip", "\\{\\\\ldots\\}"}, // horizontal ellipsis = three dot leader,
            //                                    U+2026 ISOpub
            {"8242", "prime", "\\$\\\\prime\\$"}, // prime = minutes = feet, U+2032 ISOtech
            {"8243", "Prime", "\\$\\{''\\}\\$"}, // double prime = seconds = inches,
            //                                    U+2033 ISOtech
            {"8254", "oline", "\\{\\\\=\\{\\}\\}"}, // overline = spacing overscore,
            //                                    U+203E NEW
            {"8260", "frasl", "/"}, // fraction slash, U+2044 NEW

            /* Letterlike Symbols */
            {"8472", "weierp", "\\$\\\\wp\\$"}, // script capital P = power set
            //                                    = Weierstrass p, U+2118 ISOamso
            {"8465", "image", "\\{\\$\\\\Im\\$\\}"}, // blackletter capital I = imaginary part,
            //                                    U+2111 ISOamso
            {"8476", "real", "\\{\\$\\\\Re\\$\\}"}, // blackletter capital R = real part symbol,
            //                                    U+211C ISOamso
            {"8482", "trade", "\\{\\\\texttrademark\\}"}, // trade mark sign, U+2122 ISOnum
            {"8501", "alefsym", "\\$\\\\aleph\\$"}, // alef symbol = first transfinite cardinal,
            //                                    U+2135 NEW
            /*    alef symbol is NOT the same as hebrew letter alef,
             U+05D0 although the same glyph could be used to depict both characters */
            /* Arrows */
            {"8592", "larr", "\\$\\\\leftarrow\\$"}, // leftwards arrow, U+2190 ISOnum
            {"8593", "uarr", "\\$\\\\uparrow\\$"}, // upwards arrow, U+2191 ISOnum
            {"8594", "rarr", "\\$\\\\rightarrow\\$"}, // rightwards arrow, U+2192 ISOnum
            {"8595", "darr", "\\$\\\\downarrow\\$"}, // downwards arrow, U+2193 ISOnum
            {"8596", "harr", "\\$\\\\leftrightarrow\\$"}, // left right arrow, U+2194 ISOamsa
            {"8629", "crarr", "\\$\\\\dlsh\\$"}, // downwards arrow with corner leftwards
            //                                    = carriage return, U+21B5 NEW - require mathabx
            {"8656", "lArr", "\\{\\$\\\\Leftarrow\\$\\}"}, // leftwards double arrow, U+21D0 ISOtech
            /*  ISO 10646 does not say that lArr is the same as the 'is implied by' arrow
             but also does not have any other character for that function. So ? lArr can
             be used for 'is implied by' as ISOtech suggests */
            {"8657", "uArr", "\\{\\$\\\\Uparrow\\$\\}"}, // upwards double arrow, U+21D1 ISOamsa
            {"8658", "rArr", "\\{\\$\\\\Rightarrow\\$\\}"}, // rightwards double arrow,
            //                                     U+21D2 ISOtech
            /*   ISO 10646 does not say this is the 'implies' character but does not have
             another character with this function so ?
             rArr can be used for 'implies' as ISOtech suggests */
            {"8659", "dArr", "\\{\\$\\\\Downarrow\\$\\}"}, // downwards double arrow, U+21D3 ISOamsa
            {"8660", "hArr", "\\{\\$\\\\Leftrightarrow\\$\\}"}, // left right double arrow,
            //                                     U+21D4 ISOamsa

            /* Mathematical Operators */
            {"8704", "forall", "\\$\\\\forall\\$"}, // for all, U+2200 ISOtech
            {"8706", "part", "\\$\\\\partial\\$"}, // partial differential, U+2202 ISOtech
            {"8707", "exist", "\\$\\\\exists\\$"}, // there exists, U+2203 ISOtech
            {"8709", "empty", "\\$\\\\emptyset\\$"}, // empty set = null set = diameter,
            //                                    U+2205 ISOamso
            {"8711", "nabla", "\\$\\\\nabla\\$"}, // nabla = backward difference,
            //                                    U+2207 ISOtech
            {"8712", "isin", "\\$\\\\in\\$"}, // element of, U+2208 ISOtech
            {"8713", "notin", "\\$\\\\notin\\$"}, // not an element of, U+2209 ISOtech
            {"8715", "ni", "\\$\\\\ni\\$"}, // contains as member, U+220B ISOtech
            /* should there be a more memorable name than 'ni'? */
            {"8719", "prod", "\\$\\\\prod\\$"}, // n-ary product = product sign,
            //                                    U+220F ISOamsb
            /*    prod is NOT the same character as U+03A0 'greek capital letter pi' though
             the same glyph might be used for both  */
            {"8721", "sum", "\\$\\\\sum\\$"}, // n-ary sumation, U+2211 ISOamsb
            /*    sum is NOT the same character as U+03A3 'greek capital letter sigma'
             though the same glyph might be used for both */
            {"8722", "minus", "\\$-\\$"}, // minus sign, U+2212 ISOtech
            {"8727", "lowast", "\\$\\\\ast\\$"}, // asterisk operator, U+2217 ISOtech
            {"8730", "radic", "\\$\\\\sqrt{}\\$"}, // square root = radical sign,
            //                                    U+221A ISOtech
            {"8733", "prop", "\\$\\\\propto\\$"}, // proportional to, U+221D ISOtech
            {"8734", "infin", "\\$\\\\infty\\$"}, // infinity, U+221E ISOtech
            {"8736", "ang", "\\$\\\\angle\\$"}, // angle, U+2220 ISOamso
            {"8743", "and", "\\$\\\\land\\$"}, // logical and = wedge, U+2227 ISOtech
            {"8744", "or", "\\$\\\\lor\\$"}, // logical or = vee, U+2228 ISOtech
            {"8745", "cap", "\\$\\\\cap\\$"}, // intersection = cap, U+2229 ISOtech
            {"8746", "cup", "\\$\\\\cup\\$"}, // union = cup, U+222A ISOtech
            {"8747", "int", "\\$\\\\int\\$"}, // integral, U+222B ISOtech
            {"8756", "there4", "\\$\\\\uptherefore\\$"}, // therefore, U+2234 ISOtech; only in LaTeX package MnSymbol
            {"8764", "sim", "\\$\\\\sim\\$"}, // tilde operator = varies with = similar to,
            //                                    U+223C ISOtech
            /*  tilde operator is NOT the same character as the tilde, U+007E,
             although the same glyph might be used to represent both   */
            {"8773", "cong", "\\$\\\\cong\\$"}, // approximately equal to, U+2245 ISOtech
            {"8776", "asymp", "\\$\\\\approx\\$"}, // almost equal to = asymptotic to,
            //                                    U+2248 ISOamsr
            {"8800", "ne", "\\$\\\\neq\\$"}, // not equal to, U+2260 ISOtech
            {"8801", "equiv", "\\$\\\\equiv\\$"}, // identical to, U+2261 ISOtech
            {"8804", "le", "\\$\\\\leq\\$"}, // less-than or equal to, U+2264 ISOtech
            {"8805", "ge", "\\$\\\\geq\\$"}, // greater-than or equal to,
            //                                    U+2265 ISOtech
            {"8834", "sub", "\\$\\\\subset\\$"}, // subset of, U+2282 ISOtech
            {"8835", "sup", "\\$\\\\supset\\$"}, // superset of, U+2283 ISOtech
            /*    note that nsup, 'not a superset of, U+2283' is not covered by the Symbol
             font encoding and is not included. Should it be, for symmetry?
             It is in ISOamsn   */
            {"8836", "nsub", "\\$\\\\nsubset\\$"}, // not a subset of, U+2284 ISOamsn
            {"8838", "sube", "\\$\\\\subseteq\\$"}, // subset of or equal to, U+2286 ISOtech
            {"8839", "supe", "\\$\\\\supseteq\\$"}, // superset of or equal to,
            //                                    U+2287 ISOtech
            {"8853", "oplus", "\\$\\\\oplus\\$"}, // circled plus = direct sum,
            //                                    U+2295 ISOamsb
            {"8855", "otimes", "\\$\\\\otimes\\$"}, // circled times = vector product,
            //                                    U+2297 ISOamsb
            {"8869", "perp", "\\$\\\\perp\\$"}, // up tack = orthogonal to = perpendicular,
            //                                    U+22A5 ISOtech
            {"8901", "sdot", "\\$\\\\cdot\\$"}, // dot operator, U+22C5 ISOamsb
            /* dot operator is NOT the same character as U+00B7 middle dot */
            {"8968", "lceil", "\\$\\\\lceil\\$"}, // left ceiling = apl upstile,
            //                                    U+2308 ISOamsc
            {"8969", "rceil", "\\$\\\\rceil\\$"}, // right ceiling, U+2309 ISOamsc
            {"8970", "lfloor", "\\$\\\\lfloor\\$"}, // left floor = apl downstile,
            //                                    U+230A ISOamsc
            {"8971", "rfloor", "\\$\\\\rfloor\\$"}, // right floor, U+230B ISOamsc

            /* Miscellaneous Technical */
            {"9001", "lang", "\\$\\\\langle\\$"}, // left-pointing angle bracket = bra,
            //                                    U+2329 ISOtech
            /*    lang is NOT the same character as U+003C 'less than'
             or U+2039 'single left-pointing angle quotation mark' */
            {"9002", "rang", "\\$\\\\rangle\\$"}, // right-pointing angle bracket = ket,
            //                                    U+232A ISOtech
            /*    rang is NOT the same character as U+003E 'greater than'
             or U+203A 'single right-pointing angle quotation mark' */
            /* Geometric Shapes */
            {"9674", "loz", "\\$\\\\lozenge\\$"}, // lozenge, U+25CA ISOpub

            /* Miscellaneous Symbols */
            {"9824", "spades", "\\$\\\\spadesuit\\$"}, // black spade suit, U+2660 ISOpub
            /* black here seems to mean filled as opposed to hollow */
            {"9827", "clubs", "\\$\\\\clubsuit\\$"}, // black club suit = shamrock,
            //                                    U+2663 ISOpub
            {"9829", "hearts", "\\$\\\\heartsuit\\$"}, // black heart suit = valentine,
            //                                    U+2665 ISOpub
            {"9830", "diams", "\\$\\\\diamondsuit\\$"}, // black diamond suit, U+2666 ISOpub
            {"34", "quot", "\""}, // quotation mark = APL quote,
            //                                   U+0022 ISOnum
            {"38", "amp", "\\\\&"}, // ampersand, U+0026 ISOnum
            {"60", "lt", "\\$<\\$"}, // less-than sign, U+003C ISOnum
            {"62", "gt", "\\$>\\$"}, // greater-than sign, U+003E ISOnum

            /* Latin Extended-A */
            {"338", "OElig", "\\{\\\\OE\\}"}, // latin capital ligature OE,
            //                                   U+0152 ISOlat2
            {"339", "oelig", "\\{\\\\oe\\}"}, // latin small ligature oe, U+0153 ISOlat2
            /* ligature is a misnomer, this is a separate character in some languages */
            {"352", "Scaron", "\\{\\\\v\\{S\\}\\}"}, // latin capital letter S with caron,
            //                                   U+0160 ISOlat2
            {"353", "scaron", "\\{\\\\v\\{s\\}\\}"}, // latin small letter s with caron,
            //                                   U+0161 ISOlat2
            {"376", "Yuml", "\\{\\\\\"\\{Y\\}\\}"}, // latin capital letter Y with diaeresis,
            //                                   U+0178 ISOlat2

            /* Spacing Modifier Letters */
            {"710", "circ", "\\{\\\\textasciicircum\\}"}, // modifier letter circumflex accent,
            //                                   U+02C6 ISOpub
            {"732", "tilde", "\\{\\\\textasciitilde\\}"}, // small tilde, U+02DC ISOdia

            /* General Punctuation */
            {"8194", "ensp", "\\\\hspace\\{0.5em\\}"}, // en space, U+2002 ISOpub
            {"8195", "emsp", "\\\\hspace\\{1em\\}"}, // em space, U+2003 ISOpub
            {"8201", "thinsp", "\\\\hspace\\{0.167em\\}"}, // thin space, U+2009 ISOpub
            {"8204", "zwnj", ""}, // zero width non-joiner,
            //                                   U+200C NEW RFC 2070
            {"8205", "zwj", ""}, // zero width joiner, U+200D NEW RFC 2070
            {"8206", "lrm", ""}, // left-to-right mark, U+200E NEW RFC 2070
            {"8207", "rlm", ""}, // right-to-left mark, U+200F NEW RFC 2070
            {"8211", "ndash", "--"}, // en dash, U+2013 ISOpub
            {"8212", "mdash", "---"}, // em dash, U+2014 ISOpub
            {"8216", "lsquo", "\\{\\\\textquoteleft\\}"}, // left single quotation mark,
            //                                   U+2018 ISOnum
            {"8217", "rsquo", "\\{\\\\textquoteright\\}"}, // right single quotation mark,
            //                                   U+2019 ISOnum
            {"8218", "sbquo", "\\{\\\\quotesinglbase\\}"}, // single low-9 quotation mark, U+201A NEW
            {"8220", "ldquo", "\\{\\\\textquotedblleft\\}"}, // left double quotation mark,
            //                                   U+201C ISOnum
            {"8221", "rdquo", "\\{\\\\textquotedblright\\}"}, // right double quotation mark,
            //                                   U+201D ISOnum
            {"8222", "bdquo", "\\{\\\\quotedblbase\\}"}, // double low-9 quotation mark, U+201E NEW
            {"8224", "dagger", "\\{\\\\dag\\}"}, // dagger, U+2020 ISOpub
            {"8225", "Dagger", "\\{\\\\ddag\\}"}, // double dagger, U+2021 ISOpub
            {"8240", "permil", "\\{\\\\textperthousand\\}"}, // per mille sign, U+2030 ISOtech
            {"8249", "lsaquo", "\\{\\\\guilsinglleft\\}"}, // single left-pointing angle quotation mark,
            //                                   U+2039 ISO proposed
            /* lsaquo is proposed but not yet ISO standardized */
            {"8250", "rsaquo", "\\{\\\\guilsinglright\\}"}, // single right-pointing angle quotation mark,
            //                                   U+203A ISO proposed
            /* rsaquo is proposed but not yet ISO standardized */
            {"8364", "euro", "\\{\\\\texteuro\\}"}, // euro sign, U+20AC NEW

            /* Manually added */
            {"35", "", "\\\\#"}, // Hash
            {"36", "dollar", "\\\\$"}, // Dollar
            {"37", "percnt", "\\\\%"}, // Percent
            {"39", "apos", "'"}, // Apostrophe
            {"40", "lpar", "("}, // Left bracket
            {"41", "rpar", ")"}, // Right bracket
            {"42", "", "*"}, // Asterisk
            {"43", "plus", "\\+"}, // Plus
            {"44", "comma", ","}, // Comma
            {"45", "hyphen", "-"}, // Hyphen
            {"46", "period", "\\."}, // Period
            {"47", "slash", "/"}, // Slash (solidus)
            {"58", "colon", ":"}, // Colon
            {"59", "semi", ";"}, // Semi colon
            {"61", "equals", "="}, // Equals to
            {"91", "lsqb", "\\["}, // Left square bracket
            {"92", "bsol", "\\{\\\\textbackslash\\}"}, // Backslash
            {"93", "rsqb", "\\]"}, // Right square bracket
            {"94", "Hat", "\\{\\\\\\^\\{\\}\\}"}, // Circumflex
            {"95", "lowbar", "\\\\_"}, // Underscore
            {"96", "grave", "\\{\\\\`\\{\\}\\}"}, // Grave
            {"123", "lbrace", "\\\\\\{"}, // Left curly bracket
            {"", "lcub", "\\\\\\{"}, // Left curly bracket
            {"124", "vert", "\\|"}, // Vertical bar
            {"", "verbar", "\\|"}, // Vertical bar
            {"", "VerticalLine", "\\|"}, // Vertical bar
            {"125", "rbrace", "\\\\\\}"}, // Right curly bracket
            {"", "rcub", "\\\\\\}"}, // Right curly bracket
            {"138", "", "\\{\\\\v\\{S\\}\\}"}, // Line tabulation set
            // {"141", "", ""}, // Reverse line feed
            {"145", "", "`"}, // Apostrophe
            {"146", "", "'"}, // Apostrophe
            {"147", "", "``"}, // Quotation mark
            {"148", "", "''"}, // Quotation mark
            {"150", "", "--"}, // En dash
            {"154", "", "\\{\\\\v\\{s\\}\\}"}, // Single character introducer
            {"260", "Aogon", "\\{\\\\k\\{A\\}\\}"}, // capital A with ogonek
            {"261", "aogon", "\\{\\\\k\\{a\\}\\}"}, // small a with ogonek
            {"262", "Cacute", "\\{\\\\'\\{C\\}\\}"}, // capital C with acute
            {"263", "cacute", "\\{\\\\'\\{c\\}\\}"}, // small C with acute
            {"264", "Ccirc", "\\{\\\\\\^\\{C\\}\\}"}, // capital C with circumflex
            {"265", "ccirc", "\\{\\\\\\^\\{c\\}\\}"}, // small C with circumflex
            {"266", "Cdot", "\\{\\\\\\.\\{C\\}\\}"}, // capital C with dot above
            {"267", "cdot", "\\{\\\\\\.\\{c\\}\\}"}, // small C with dot above
            {"268", "Ccaron", "\\{\\\\v\\{C\\}\\}"}, // capital C with caron
            {"269", "ccaron", "\\{\\\\v\\{c\\}\\}"}, // small C with caron
            {"272", "Dstrok", "\\{\\\\DJ\\}"}, // capital D with stroke
            {"273", "dstrok", "\\{\\\\dj\\}"}, // small d with stroke
            {"280", "Eogon", "\\{\\\\k\\{E\\}\\}"}, // capital E with ogonek
            {"281", "eogon", "\\{\\\\k\\{e\\}\\}"}, // small e with ogonek
            {"298", "Imacr", "\\{\\\\=\\{I\\}\\}"}, // capital I with macron
            {"299", "imacr", "\\{\\\\=\\{\\\\i\\}\\}"}, // small i with macron
            {"302", "Iogon", "\\{\\\\k\\{I\\}\\}"}, // capital I with ogonek
            {"303", "iogon", "\\{\\\\k\\{i\\}\\}"}, // small i with ogonek
            {"304", "Idot", "\\{\\\\.\\{I\\}\\}"}, // capital I with dot above
            {"305", "inodot", "\\{\\\\i\\}"}, // Small i without the dot
            {"", "imath", "\\{\\\\i\\}"}, // Small i without the dot
            {"306", "", "\\{\\\\IJ\\}"}, // Dutch di-graph IJ
            {"307", "", "\\{\\\\ij\\}"}, // Dutch di-graph ij
            {"312", "", "\\{\\\\textkra\\}"}, // Letter kra
            {"319", "Lmidot", "\\{\\\\Lmidot\\}"}, // upper case L with mid dot
            {"320", "lmidot", "\\{\\\\lmidot\\}"}, // lower case l with stroke
            {"321", "Lstrok", "\\{\\\\L\\}"}, // upper case L with stroke
            {"322", "lstrok", "\\{\\\\l\\}"}, // lower case l with stroke
            {"330", "", "\\{\\\\NG\\}"}, // upper case letter Eng
            {"331", "", "\\{\\\\ng\\}"}, // lower case letter Eng
            {"338", "", "\\{\\\\OE\\}"}, // OE-ligature
            {"339", "", "\\{\\\\oe\\}"}, // oe-ligature
            {"348", "Scirc", "\\{\\\\\\^\\{S\\}\\}"}, // upper case S with circumflex
            {"349", "scirc", "\\{\\\\\\^\\{s\\}\\}"}, // lower case s with circumflex
            {"370", "Uogon", "\\{\\\\k\\{U\\}\\}"}, // capital U with ogonek
            {"371", "uogon", "\\{\\\\k\\{u\\}\\}"}, // small u with ogonek
            {"381", "Zcaron", "\\{\\\\v\\{Z\\}\\}"}, // capital Z with caron
            {"382", "zcaron", "\\{\\\\v\\{z\\}\\}"}, // small z with caron
            {"405", "", "\\{\\\\hv\\}"}, // small letter Hv
            {"416", "", "\\{\\\\OHORN\\}"}, // capital O with horn
            {"417", "", "\\{\\\\ohorn\\}"}, // small o with horn
            {"431", "", "\\{\\\\UHORN\\}"}, // capital U with horn
            {"432", "", "\\{\\\\uhorn\\}"}, // small u with horn
            {"490", "Oogon", "\\{\\\\k\\{O\\}\\}"}, // capital letter O with ogonek
            {"491", "oogon", "\\{\\\\k\\{o\\}\\}"}, // small letter o with ogonek
            {"492", "", "\\{\\\\k\\{\\\\=\\{O\\}\\}\\}"}, // capital letter O with ogonek and macron
            {"493", "", "\\{\\\\k\\{\\\\=\\{o\\}\\}\\}"}, // small letter o with ogonek and macron
            {"536", "", "\\{\\\\cb\\{S\\}\\}"}, // capital letter S with comma below, require combelow
            {"537", "", "\\{\\\\cb\\{s\\}\\}"}, // small letter S with comma below, require combelow
            {"538", "", "\\{\\\\cb\\{T\\}\\}"}, // capital letter T with comma below, require combelow
            {"539", "", "\\{\\\\cb\\{t\\}\\}"}, // small letter T with comma below, require combelow
            {"727", "caron", "\\{\\\\v\\{\\}\\}"}, // Caron
            {"", "Hacek", "\\{\\\\v\\{\\}\\}"}, // Caron
            {"728", "breve", "\\{\\\\u\\{\\}\\}"}, // Breve
            {"", "Breve", "\\{\\\\u\\{\\}\\}"}, // Breve
            {"729", "dot", "\\{\\\\\\.\\{\\}\\}"}, // Dot above
            {"730", "ring", "\\{\\\\r\\{\\}\\}"}, // Ring above
            {"731", "ogon", "\\{\\\\k\\{\\}\\}"}, // Ogonek
            {"733", "dblac", "\\{\\\\H\\{\\}\\}"}, // Double acute
            {"949", "epsi", "\\$\\\\epsilon\\$"}, // Epsilon - double check
            {"1013", "epsiv", "\\$\\\\varepsilonup\\$"}, // lunate epsilon, requires txfonts
            {"1055", "", "\\{\\\\cyrchar\\\\CYRP\\}"}, // Cyrillic capital Pe
            {"1082", "", "\\{\\\\cyrchar\\\\cyrk\\}"}, // Cyrillic small Ka
            // {"2013", "", ""},    // NKO letter FA -- Maybe en dash = 0x2013?
            // {"2014", "", ""},    // NKO letter FA -- Maybe em dash = 0x2014?
            {"8192", "", "\\\\hspace\\{0.5em\\}"}, // en quad
            {"8193", "", "\\\\hspace\\{1em\\}"}, // em quad
            {"8196", "", "\\\\hspace\\{0.333em\\}"}, // Three-Per-Em Space
            {"8197", "", "\\\\hspace\\{0.25em\\}"}, // Four-Per-Em Space
            {"8198", "", "\\\\hspace\\{0.167em\\}"}, // Six-Per-Em Space
            {"8208", "hyphen", "-"}, // Hyphen
            {"8229", "nldr", "\\.\\."}, // Double dots - en leader
            {"8241", "", "\\{\\\\textpertenthousand\\}"}, // per ten thousands sign
            {"8244", "", "\\{\\\\prime\\\\prime\\\\prime\\}"}, // triple prime
            {"8251", "", "\\{\\\\textreferencemark\\}"}, {"8253", "", "\\{\\\\textinterrobang\\}"},
            {"8450", "complexes", "\\$\\\\mathbb\\{C\\}\\$"}, // double struck capital C -- requires e.g. amsfonts
            {"8451", "", "\\$\\\\deg\\$\\{C\\}"}, // Degree Celsius
            {"8459", "Hscr", "\\$\\\\mathcal\\{H\\}\\$"}, // script capital H -- possibly use \mathscr
            {"8460", "Hfr", "\\$\\\\mathbb\\{H\\}\\$"}, // black letter capital H -- requires e.g. amsfonts
            {"8466", "Lscr", "\\$\\\\mathcal\\{L\\}\\$"}, // script capital L -- possibly use \mathscr
            {"8467", "ell", "\\{\\\\ell\\}"}, // script small l
            {"8469", "naturals", "\\$\\\\mathbb\\{N\\}\\$"}, // double struck capital N -- requires e.g. amsfonts
            {"8474", "Qopf", "\\$\\\\mathbb\\{Q\\}\\$"}, // double struck capital Q -- requires e.g. amsfonts
            {"8477", "reals", "\\$\\\\mathbb\\{R\\}\\$"}, // double struck capital R -- requires e.g. amsfonts
            {"8486", "", "\\$\\{\\\\Omega\\}\\$"}, // Omega
            {"8491", "angst", "\\{\\\\AA\\}"}, // Angstrom
            {"8496", "Escr", "\\$\\\\mathcal\\{E\\}\\$"}, // script capital E
            {"8531", "frac13", "\\$\\\\sfrac\\{1\\}\\{3\\}\\$"}, // Vulgar fraction one third
            {"8532", "frac23", "\\$\\\\sfrac\\{2\\}\\{3\\}\\$"}, // Vulgar fraction two thirds
            {"8533", "frac15", "\\$\\\\sfrac\\{1\\}\\{5\\}\\$"}, // Vulgar fraction one fifth
            {"8534", "frac25", "\\$\\\\sfrac\\{2\\}\\{5\\}\\$"}, // Vulgar fraction two fifths
            {"8535", "frac35", "\\$\\\\sfrac\\{3\\}\\{5\\}\\$"}, // Vulgar fraction three fifths
            {"8536", "frac45", "\\$\\\\sfrac\\{4\\}\\{5\\}\\$"}, // Vulgar fraction four fifths
            {"8537", "frac16", "\\$\\\\sfrac\\{1\\}\\{6\\}\\$"}, // Vulgar fraction one sixth
            {"8538", "frac56", "\\$\\\\sfrac\\{5\\}\\{6\\}\\$"}, // Vulgar fraction five sixths
            {"8539", "frac18", "\\$\\\\sfrac\\{1\\}\\{8\\}\\$"}, // Vulgar fraction one eighth
            {"8540", "frac38", "\\$\\\\sfrac\\{3\\}\\{8\\}\\$"}, // Vulgar fraction three eighths
            {"8541", "frac58", "\\$\\\\sfrac\\{5\\}\\{8\\}\\$"}, // Vulgar fraction five eighths
            {"8542", "frac78", "\\$\\\\sfrac\\{7\\}\\{8\\}\\$"}, // Vulgar fraction seven eighths
            {"8710", "", "\\$\\\\triangle\\$"}, // Increment - could use a more appropriate symbol
            {"8714", "", "\\$\\\\in\\$"}, // Small element in
            {"8723", "mp", "\\$\\\\mp\\$"}, // Minus-plus
            {"8729", "bullet", "\\$\\\\bullet\\$"}, // Bullet operator
            {"8758", "ratio", ":"}, // Colon/ratio
            {"8771", "sime", "\\$\\\\simeq\\$"}, // almost equal to = asymptotic to,
            {"8776", "ap", "\\$\\\\approx\\$"}, // almost equal to = asymptotic to,
            {"8810", "ll", "\\$\\\\ll\\$"}, // Much less than
            {"", "Lt", "\\$\\\\ll\\$"}, // Much less than
            {"8811", "gg", "\\$\\\\gg\\$"}, // Much greater than
            {"", "Gt", "\\$\\\\gg\\$"}, // Much greater than
            {"8818", "lsim", "\\$\\\\lesssim\\$"}, // Less than or equivalent to
            {"8819", "gsim", "\\$\\\\gtrsim\\$"}, // Greater than or equivalent to
            {"8862", "boxplus", "\\$\\\\boxplus\\$"}, // Boxed plus -- requires amssymb
            {"8863", "boxminus", "\\$\\\\boxminus\\$"}, // Boxed minus -- requires amssymb
            {"8864", "boxtimes", "\\$\\\\boxtimes\\$"}, // Boxed times -- requires amssymb
            {"8882", "vltri", "\\$\\\\triangleleft\\$"}, // Left triangle
            {"8883", "vrtri", "\\$\\\\triangleright\\$"}, // Right triangle
            {"8896", "xwedge", "\\$\\\\bigwedge\\$"}, // Big wedge
            {"8897", "xvee", "\\$\\\\bigvee\\$"}, // Big vee
            {"8942", "vdots", "\\$\\\\vdots\\$"}, // vertical ellipsis U+22EE
            {"8943", "cdots", "\\$\\\\cdots\\$"}, // midline horizontal ellipsis U+22EF
            /*{"8944", "", "\\$\\\\ddots\\$"}, // up right diagonal ellipsis U+22F0 */
            {"8945", "ddots", "\\$\\\\ddots\\$"}, // down right diagonal ellipsis U+22F1

            {"9426", "circledc", "\\{\\\\copyright\\}"}, // circled small letter C
            {"9633", "square", "\\$\\\\square\\$"}, // White square
            {"9651", "xutri", "\\$\\\\bigtriangleup\\$"}, // White up-pointing big triangle
            {"9653", "utri", "\\$\\\\triangle\\$"}, // White up-pointing small triangle -- \vartriangle probably
            // better but requires amssymb
            {"10877", "les", "\\$\\\\leqslant\\$"}, // Less than slanted equal -- requires amssymb
            {"10878", "ges", "\\$\\\\geqslant\\$"}, // Less than slanted equal -- requires amssymb
            {"119978", "Oscr", "\\$\\\\mathcal\\{O\\}\\$"}, // script capital O -- possibly use \mathscr
            {"119984", "Uscr", "\\$\\\\mathcal\\{U\\}\\$"} // script capital U -- possibly use \mathscr

    };
    // List of combining accents
    private static final String[][] ACCENT_LIST = new String[][] {{"768", "`"}, // Grave
            {"769", "'"}, // Acute
            {"770", "\\^"}, // Circumflex
            {"771", "~"}, // Tilde
            {"772", "="}, // Macron
            {"773", "="}, // Overline - not completely correct
            {"774", "u"}, // Breve
            {"775", "\\."}, // Dot above
            {"776", "\""}, // Diaeresis
            {"777", "h"}, // Hook above
            {"778", "r"}, // Ring
            {"779", "H"}, // Double acute
            {"780", "v"}, // Caron
            {"781", "\\|"}, // Vertical line above
            {"782", "U"}, // Double vertical line above
            {"783", "G"}, // Double grave
            {"784", "textdotbreve"}, // Candrabindu
            {"785", "t"}, // Inverted breve
            //        {"786", ""},    // Turned comma above
            //        {"787", ""},    // Comma above
            {"788", "textrevcommaabove"}, // Reversed comma above
            {"789", "textcommaabover"}, // Comma above right
            {"790", "textsubgrave"}, // Grave accent below -requires tipa
            {"791", "textsubacute"}, // Acute accent below - requires tipa
            {"792", "textadvancing"}, // Left tack below - requires tipa
            {"793", "textretracting"}, // Right tack below - requires tipa
            {"794", "textlangleabove"}, // Left angle above
            {"795", "textrighthorn"}, // Horn
            {"796", "textsublhalfring"}, // Left half ring below - requires tipa
            {"797", "textraising"}, // Up tack below - requires tipa
            {"798", "textlowering"}, // Down tack below - requires tipa
            {"799", "textsubplus"}, // Plus sign below - requires tipa
            {"800", "textsubbar"}, // Minus sign below
            {"801", "textpalhookbelow"}, // Palatalized hook below
            {"802", "M"}, // Retroflex hook below - textrethookbelow?
            {"803", "d"}, // Dot below
            {"804", "textsubumlaut"}, // Diaeresis below - requires tipa
            {"805", "textsubring"}, // Ring below - requires tipa
            {"806", "cb"}, // Comma below - requires combelow
            {"807", "c"}, // Cedilla
            {"808", "k"}, // Ogonek
            {"809", "textsyllabic"}, // Vertical line below - requires tipa
            {"810", "textsubbridge"}, // Bridge below - requires tipa
            {"811", "textsubw"}, // Inverted double arch below - requires tipa
            {"812", "textsubwedge"}, // Caron below
            {"813", "textsubcircum"}, // Circumflex accent below - requires tipa
            {"814", "textsubbreve"}, // Breve below
            {"815", "textsubarch"}, // Inverted breve below - requires tipa
            {"816", "textsubtilde"}, // Tilde below - requires tipa
            {"817", "b"}, // Macron below - not completely correct
            {"818", "b"}, // Underline
            {"819", "subdoublebar"}, // Double low line -- requires extraipa
            {"820", "textsuperimposetilde"}, // Tilde overlay - requires tipa
            {"821", "B"}, // Short stroke overlay - textsstrokethru?
            {"822", "textlstrokethru"}, // Long stroke overlay
            {"823", "textsstrikethru"}, // Short solidus overlay
            {"824", "textlstrikethru"}, // Long solidus overlay
            {"825", "textsubrhalfring"}, // Right half ring below - requires tipa
            {"826", "textinvsubbridge"}, // inverted bridge below - requires tipa
            {"827", "textsubsquare"}, // Square below - requires tipa
            {"828", "textseagull"}, // Seagull below - requires tipa
            {"829", "textovercross"}, // X above - requires tipa
            //        {"830", ""},    // Vertical tilde
            //        {"831", ""},    // Double overline
            //        {"832", ""},    // Grave tone mark
            //        {"833", ""},    // Acute tone mark
            //        {"834", ""},    // Greek perispomeni
            //        {"835", ""},    // Greek koronis
            //        {"836", ""},    // Greek dialytika tonos
            //        {"837", ""},    // Greek ypogegrammeni
            {"838", "overbridge"}, // Bridge above - requires extraipa
            {"839", "subdoublebar"}, // Equals sign below - requires extraipa
            {"840", "subdoublevert"}, // Double vertical line below - requires extraipa
            {"841", "subcorner"}, // Left angle below - requires extraipa
            {"842", "crtilde"}, // Not tilde above - requires extraipa
            {"843", "dottedtilde"}, // Homothetic above - requires extraipa
            {"844", "doubletilde"}, // Almost equal to above - requires extraipa
            {"845", "spreadlips"}, // Left right arrow below - requires extraipa
            {"846", "whistle"}, // Upwards arrow below - requires extraipa
            {"861", "textdoublebreve"}, // Double breve
            {"862", "textdoublemacron"}, // Double macron
            {"863", "textdoublemacronbelow"}, // Double macron below
            {"864", "textdoubletilde"}, // Double tilde
            {"865", "texttoptiebar"}, // Double inverted breve
            {"866", "sliding"}, // Double rightwards arrow below - requires extraipa
    };

    public static final Map<String, String> HTML_LATEX_CONVERSION_MAP = new HashMap<>();
    public static final Map<Integer, String> ESCAPED_ACCENTS = new HashMap<>();
    public static final Map<Integer, String> NUMERICAL_LATEX_CONVERSION_MAP = new HashMap<>();
    public static final Map<Character, String> UNICODE_LATEX_CONVERSION_MAP = new HashMap<>();
    public static final Map<String, String> LATEX_HTML_CONVERSION_MAP = new HashMap<>();
    public static final Map<String, String> LATEX_UNICODE_CONVERSION_MAP = new HashMap<>();


    static {
        for (String[] aConversionList : CONVERSION_LIST) {
            if (!(aConversionList[2].isEmpty())) {
                String strippedLaTeX = cleanLaTeX(aConversionList[2]);
                if (!(aConversionList[1].isEmpty())) {
                    HTML_LATEX_CONVERSION_MAP.put("&" + aConversionList[1] + ";", aConversionList[2]);
                    if (!strippedLaTeX.isEmpty()) {
                        LATEX_HTML_CONVERSION_MAP.put(strippedLaTeX, "&" + aConversionList[1] + ";");
                    }
                } else if (!(aConversionList[0].isEmpty()) && !strippedLaTeX.isEmpty()) {
                    LATEX_HTML_CONVERSION_MAP.put(strippedLaTeX, "&#" + aConversionList[0] + ";");
                }
                if (!(aConversionList[0].isEmpty())) {
                    NUMERICAL_LATEX_CONVERSION_MAP.put(Integer.decode(aConversionList[0]), aConversionList[2]);
                    if (Integer.decode(aConversionList[0]) > 128) {
                        Character c = (char) Integer.decode(aConversionList[0]).intValue();
                        UNICODE_LATEX_CONVERSION_MAP.put(c, aConversionList[2]);
                        if (!strippedLaTeX.isEmpty()) {
                            LATEX_UNICODE_CONVERSION_MAP.put(strippedLaTeX, c.toString());
                        }
                    }
                }
            }
        }
        for (String[] anAccentList : ACCENT_LIST) {
            ESCAPED_ACCENTS.put(Integer.decode(anAccentList[0]), anAccentList[1]);
        }
        // Manually added values which are killed by cleanLaTeX
        LATEX_HTML_CONVERSION_MAP.put("$", "&dollar;");
        LATEX_UNICODE_CONVERSION_MAP.put("$", "$");

        // Manual corrections
        LATEX_HTML_CONVERSION_MAP.put("AA", "&Aring;"); // Overwritten by &angst; which is less supported
        LATEX_UNICODE_CONVERSION_MAP.put("AA", "Å"); // Overwritten by Ångstrom symbol
    }

    private static String cleanLaTeX(String escapedString) {
        // Get rid of \{}$ from the LaTeX-string
        return escapedString.replaceAll("[\\\\\\{\\}\\$]", "");
    }

}
