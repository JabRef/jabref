/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.imports;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.export.layout.LayoutFormatter;

public class HTMLConverter implements LayoutFormatter {

    /*   Portions © International Organization for Standardization 1986:
     Permission to copy in any form is granted for use with
     conforming SGML systems and applications as defined in
     ISO 8879, provided this notice is included in all copies.
    */


	// most of the LaTeX commands can be read at http://en.wikibooks.org/wiki/LaTeX/Accents
	// The symbols can be looked at http://www.fileformat.info/info/unicode/char/a4/index.htm. Replace "a4" with the U+ number
	// http://detexify.kirelabs.org/classify.html and http://www.ctan.org/tex-archive/info/symbols/comprehensive/ might help to find the right LaTeX command
    private String[][] conversionList = new String[][]{
        {"160", "nbsp", "\\{~\\}"}, // no-break space = non-breaking space, 
        //                                 U+00A0 ISOnum 
        {"161", "iexcl", "\\\\textexclamdown"}, // inverted exclamation mark, U+00A1 ISOnum
        {"162", "cent", ""}, // cent sign, U+00A2 ISOnum  
        {"163", "pound", "\\\\pounds"}, // pound sign, U+00A3 ISOnum
        {"164", "curren", ""}, // currency sign, U+00A4 ISOnum  
        {"165", "yen", ""}, // yen sign = yuan sign, U+00A5 ISOnum  
        {"166", "brvbar", ""}, // broken bar = broken vertical bar, 
        //                                 U+00A6 ISOnum 
        {"167", "sect", ""}, // section sign, U+00A7 ISOnum  
        {"168", "uml", ""}, // diaeresis = spacing diaeresis, 
        //                                 U+00A8 ISOdia 
        {"169", "copy", "\\\\copyright"}, // copyright sign, U+00A9 ISOnum
        {"170", "ordf", "\\\\textordfeminine"}, // feminine ordinal indicator, U+00AA ISOnum
        {"171", "laquo", "\\\\guillemotleft"}, // left-pointing double angle quotation mark
        //                                 = left pointing guillemet, U+00AB ISOnum 
        {"172", "not", ""}, // not sign, U+00AC ISOnum  
        {"173", "shy", ""}, // soft hyphen = discretionary hyphen, 
        //                                 U+00AD ISOnum 
        {"174", "reg", "\\textregistered"}, // registered sign = registered trade mark sign,
        //                                 U+00AE ISOnum 
        {"175", "macr", ""}, // macron = spacing macron = overline 
        //                                 = APL overbar, U+00AF ISOdia 
        {"176", "deg", "\\$\\\\deg\\$"}, // degree sign, U+00B0 ISOnum  
        {"177", "plusmn", "\\$\\\\pm\\$"}, // plus-minus sign = plus-or-minus sign, 
        //                                 U+00B1 ISOnum 
        {"178", "sup2", "\\$\\^2\\$"}, // superscript two = superscript digit two 
        //                                 = squared, U+00B2 ISOnum 
        {"179", "sup3", "\\$\\^3\\$"}, // superscript three = superscript digit three 
        //                                 = cubed, U+00B3 ISOnum 
        {"180", "acute", ""}, // acute accent = spacing acute, 
        //                                 U+00B4 ISOdia 
        {"181", "micro", "\\$\\\\mu\\$"}, // micro sign, U+00B5 ISOnum  
        {"182", "para", ""}, // pilcrow sign = paragraph sign, 
        //                                 U+00B6 ISOnum 
        {"183", "middot", ""}, // middle dot = Georgian comma 
        //                                 = Greek middle dot, U+00B7 ISOnum 
        {"184", "cedil", ""}, // cedilla = spacing cedilla, U+00B8 ISOdia  
        {"185", "sup1", "\\\\textsuperscript\\{1\\}"}, // superscript one = superscript digit one,
        //                                 U+00B9 ISOnum 
        {"186", "ordm", "\\\\textordmasculine"}, // masculine ordinal indicator,
        //                                 U+00BA ISOnum 
        {"187", "raquo", "\\\\guillemotright"}, // right-pointing double angle quotation mark
        //                                 = right pointing guillemet, U+00BB ISOnum 
        {"188", "frac14", "\\$\\sfrac\\{1\\}\\{4\\}\\$"}, // vulgar fraction one quarter 
        //                                 = fraction one quarter, U+00BC ISOnum 
        {"189", "frac12", "\\$\\sfrac\\{1\\}\\{2\\}\\$"}, // vulgar fraction one half 
        //                                 = fraction one half, U+00BD ISOnum 
        {"190", "frac34", "\\$\\sfrac\\{3\\}\\{4\\}\\$"}, // vulgar fraction three quarters 
        //                                 = fraction three quarters, U+00BE ISOnum 
        {"191", "iquest", ""}, // inverted question mark 
        //                                 = turned question mark, U+00BF ISOnum 
        {"192", "Agrave", "\\\\`\\{A\\}"}, // latin capital letter A with grave
        //                                 = latin capital letter A grave,
        //                                 U+00C0 ISOlat1 
        {"193", "Aacute", "\\\\'\\{A\\}"}, // latin capital letter A with acute, 
        //                                 U+00C1 ISOlat1 
        {"194", "Acirc", ""}, // latin capital letter A with circumflex, 
        //                                 U+00C2 ISOlat1 
        {"195", "Atilde", "\\\\~\\{A\\}"}, // latin capital letter A with tilde, 
        //                                 U+00C3 ISOlat1 
        {"196", "Auml", "\\\"\\{A\\}"}, // latin capital letter A with diaeresis, 
        //                                 U+00C4 ISOlat1 
        {"197", "Aring", ""}, // latin capital letter A with ring above 
        //                                 = latin capital letter A ring,
        //                                 U+00C5 ISOlat1 
        {"198", "AElig", "\\{\\\\AE\\}"}, // latin capital letter AE 
        //                                 = latin capital ligature AE,
        //                                 U+00C6 ISOlat1 
        {"199", "Ccedil", "\\\\c\\{C\\}"}, // latin capital letter C with cedilla,
        //                                 U+00C7 ISOlat1 
        {"200", "Egrave", "\\\\`\\{E\\}"}, // latin capital letter E with grave,
        //                                 U+00C8 ISOlat1 
        {"201", "Eacute", "\\\\'\\{E\\}"}, // latin capital letter E with acute, 
        //                                 U+00C9 ISOlat1 
        {"202", "Ecirc", ""}, // latin capital letter E with circumflex, 
        //                                 U+00CA ISOlat1 
        {"203", "Euml", "\\\"\\{E\\}"}, // latin capital letter E with diaeresis, 
        //                                 U+00CB ISOlat1 
        {"204", "Igrave", "\\\\`\\{I\\}"}, // latin capital letter I with grave,
        //                                 U+00CC ISOlat1 
        {"205", "Iacute", "\\\\'\\{I\\}"}, // latin capital letter I with acute, 
        //                                 U+00CD ISOlat1 
        {"206", "Icirc", ""}, // latin capital letter I with circumflex, 
        //                                 U+00CE ISOlat1 
        {"207", "Iuml", "\\\"\\{I\\}"}, // latin capital letter I with diaeresis, 
        //                                 U+00CF ISOlat1 
        {"208", "ETH", ""}, // latin capital letter ETH, U+00D0 ISOlat1  
        {"209", "Ntilde", "\\\\~\\{N\\}"}, // latin capital letter N with tilde, 
        //                                 U+00D1 ISOlat1 
        {"210", "Ograve", "\\\\`\\{O\\}"}, // latin capital letter O with grave,
        //                                 U+00D2 ISOlat1 
        {"211", "Oacute", "\\\\'\\{O\\}"}, // latin capital letter O with acute, 
        //                                 U+00D3 ISOlat1 
        {"212", "Ocirc", ""}, // latin capital letter O with circumflex, 
        //                                 U+00D4 ISOlat1 
        {"213", "Otilde", "\\\\~\\{O\\}"}, // latin capital letter O with tilde, 
        //                                 U+00D5 ISOlat1 
        {"214", "Ouml", "\\\\\"\\{O\\}"}, // latin capital letter O with diaeresis, 
        //                                 U+00D6 ISOlat1 
        {"215", "times", "\\$\\\\times\\$"}, // multiplication sign, U+00D7 ISOnum  
        {"216", "Oslash", ""}, // latin capital letter O with stroke 
        //                                 = latin capital letter O slash,
        //                                 U+00D8 ISOlat1 
        {"217", "Ugrave", "\\\\`\\{U\\}"}, // latin capital letter U with grave,
        //                                 U+00D9 ISOlat1 
        {"218", "Uacute", "\\\\'\\{U\\}"}, // latin capital letter U with acute, 
        //                                 U+00DA ISOlat1 
        {"219", "Ucirc", ""}, // latin capital letter U with circumflex, 
        //                                 U+00DB ISOlat1 
        {"220", "Uuml", "\\\\\"\\{U\\}"}, // latin capital letter U with diaeresis, 
        //                                 U+00DC ISOlat1 
        {"221", "Yacute", "\\\\'\\{Y\\}"}, // latin capital letter Y with acute, 
        //                                 U+00DD ISOlat1 
        {"222", "THORN", ""}, // latin capital letter THORN, 
        //                                 U+00DE ISOlat1 
        {"223", "szlig", "\\\\ss\\{\\}"}, // latin small letter sharp s = ess-zed,
        //                                 U+00DF ISOlat1 
        {"224", "agrave", "\\\\`\\{a\\}"}, // latin small letter a with grave
        //                                 = latin small letter a grave,
        //                                 U+00E0 ISOlat1 
        {"225", "aacute", "\\\\'\\{a\\}"}, // latin small letter a with acute, 
        //                                 U+00E1 ISOlat1 
        {"226", "acirc", ""}, // latin small letter a with circumflex, 
        //                                 U+00E2 ISOlat1 
        {"227", "atilde", "\\\\~\\{a\\}"}, // latin small letter a with tilde, 
        //                                 U+00E3 ISOlat1 
        {"228", "auml", "\\\\\"\\{a\\}"}, // latin small letter a with diaeresis, 
        //                                 U+00E4 ISOlat1 
        {"229", "aring", ""}, // latin small letter a with ring above 
        //                                 = latin small letter a ring,
        //                                 U+00E5 ISOlat1 
        {"230", "aelig", "\\{\\\\ae\\}"}, // latin small letter ae 
        //                                 = latin small ligature ae, U+00E6 ISOlat1 
        {"231", "ccedil", "\\\\c\\{c\\}"}, // latin small letter c with cedilla,
        //                                 U+00E7 ISOlat1 
        {"232", "egrave", "\\\\`\\{e\\}"}, // latin small letter e with grave,
        //                                 U+00E8 ISOlat1 
        {"233", "eacute", "\\\\'\\{e\\}"}, // latin small letter e with acute, 
        //                                 U+00E9 ISOlat1 
        {"234", "ecirc", ""}, // latin small letter e with circumflex, 
        //                                 U+00EA ISOlat1 
        {"235", "euml", "\\\\\"\\{e\\}"}, // latin small letter e with diaeresis, 
        //                                 U+00EB ISOlat1 
        {"236", "igrave", "\\\\`\\{i\\}"}, // latin small letter i with grave,
        //                                 U+00EC ISOlat1 
        {"237", "iacute", "\\\\'\\{i\\}"}, // latin small letter i with acute, 
        //                                 U+00ED ISOlat1 
        {"238", "icirc", ""}, // latin small letter i with circumflex, 
        //                                 U+00EE ISOlat1 
        {"239", "iuml", "\\\\\"\\{i\\}"}, // latin small letter i with diaeresis, 
        //                                 U+00EF ISOlat1 
        {"240", "eth", ""}, // latin small letter eth, U+00F0 ISOlat1  
        {"241", "ntilde", "\\\\~\\{n\\}"}, // latin small letter n with tilde, 
        //                                 U+00F1 ISOlat1 
        {"242", "ograve", "\\\\`\\{o\\}"}, // latin small letter o with grave,
        //                                 U+00F2 ISOlat1 
        {"243", "oacute", "\\\\'\\{o\\}"}, // latin small letter o with acute, 
        //                                 U+00F3 ISOlat1 
        {"244", "ocirc", ""}, // latin small letter o with circumflex, 
        //                                 U+00F4 ISOlat1 
        {"245", "otilde", "\\\\~\\{o\\}"}, // latin small letter o with tilde, 
        //                                 U+00F5 ISOlat1 
        {"246", "ouml", "\\\\\"\\{o\\}"}, // latin small letter o with diaeresis, 
        //                                 U+00F6 ISOlat1 
        {"247", "divide", ""}, // division sign, U+00F7 ISOnum  
        {"248", "oslash", ""}, // latin small letter o with stroke, 
        //                                 = latin small letter o slash,
        //                                 U+00F8 ISOlat1 
        {"249", "ugrave", "\\\\`\\{u\\}"}, // latin small letter u with grave,
        //                                 U+00F9 ISOlat1 
        {"250", "uacute", "\\\\'\\{u\\}"}, // latin small letter u with acute, 
        //                                 U+00FA ISOlat1 
        {"251", "ucirc", ""}, // latin small letter u with circumflex, 
        //                                 U+00FB ISOlat1 
        {"252", "uuml", "\\\\\"\\{u\\}"}, // latin small letter u with diaeresis, 
        //                                 U+00FC ISOlat1 
        {"253", "yacute", "\\\\'\\{y\\}"}, // latin small letter y with acute, 
        //                                 U+00FD ISOlat1 
        {"254", "thorn", ""}, // latin small letter thorn, 
        //                                 U+00FE ISOlat1 
        {"255", "yuml", "\\\\\"\\{y\\}"}, // latin small letter y with diaeresis, 
        //                                 U+00FF ISOlat1 
        {"402", "fnof", ""}, // latin small f with hook = function 
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
        {"962", "sigmaf", ""}, // greek small letter final sigma, 
        //                                   U+03C2 ISOgrk3 
        {"963", "sigma", "\\$\\\\sigma\\$"}, // greek small letter sigma, 
        //                                   U+03C3 ISOgrk3 
        {"964", "tau", "\\$\\\\tau\\$"}, // greek small letter tau, U+03C4 ISOgrk3  
        {"965", "upsilon", "\\$\\\\upsilon\\$"}, // greek small letter upsilon, 
        //                                   U+03C5 ISOgrk3 
        {"966", "phi", "\\$\\\\phi\\$"}, // greek small letter phi, U+03C6 ISOgrk3  
        {"967", "chi", "\\$\\\\chi\\$"}, // greek small letter chi, U+03C7 ISOgrk3  
        {"968", "psi", "\\$\\\\psi\\$"}, // greek small letter psi, U+03C8 ISOgrk3  
        {"969", "omega", "\\$\\\\omega\\$"}, // greek small letter omega, 
        //                                   U+03C9 ISOgrk3 
        {"977", "thetasym", ""}, // greek small letter theta symbol, 
        //                                   U+03D1 NEW 
        {"978", "upsih", ""}, // greek upsilon with hook symbol, 
        //                                   U+03D2 NEW 
        {"982", "piv", ""}, // greek pi symbol, U+03D6 ISOgrk3  

        /* General Punctuation */
        {"8226", "bull", ""}, // bullet = black small circle, 
        //                                    U+2022 ISOpub  
        /* bullet is NOT the same as bullet operator, U+2219 */
        {"8230", "hellip", ""}, // horizontal ellipsis = three dot leader, 
        //                                    U+2026 ISOpub  
        {"8242", "prime", ""}, // prime = minutes = feet, U+2032 ISOtech  
        {"8243", "Prime", ""}, // double prime = seconds = inches, 
        //                                    U+2033 ISOtech 
        {"8254", "oline", ""}, // overline = spacing overscore, 
        //                                    U+203E NEW 
        {"8260", "frasl", ""}, // fraction slash, U+2044 NEW  

        /* Letterlike Symbols */
        {"8472", "weierp", ""}, // script capital P = power set 
        //                                    = Weierstrass p, U+2118 ISOamso 
        {"8465", "image", ""}, // blackletter capital I = imaginary part, 
        //                                    U+2111 ISOamso 
        {"8476", "real", ""}, // blackletter capital R = real part symbol, 
        //                                    U+211C ISOamso 
        {"8482", "trade", "\\texttrademark"}, // trade mark sign, U+2122 ISOnum
        {"8501", "alefsym", ""}, // alef symbol = first transfinite cardinal, 
        //                                    U+2135 NEW 
        /*    alef symbol is NOT the same as hebrew letter alef,
         U+05D0 although the same glyph could be used to depict both characters */
        /* Arrows */
        {"8592", "larr", "\\\\leftarrow"}, // leftwards arrow, U+2190 ISOnum
        {"8593", "uarr", "\\\\uparrow"}, // upwards arrow, U+2191 ISOnum
        {"8594", "rarr", "\\\\rightarrow"}, // rightwards arrow, U+2192 ISOnum
        {"8595", "darr", "\\\\downarrow"}, // downwards arrow, U+2193 ISOnum
        {"8596", "harr", ""}, // left right arrow, U+2194 ISOamsa  
        {"8629", "crarr", ""}, // downwards arrow with corner leftwards 
        //                                    = carriage return, U+21B5 NEW 
        {"8656", "lArr", "\\\\Leftarrow"}, // leftwards double arrow, U+21D0 ISOtech
        /*  ISO 10646 does not say that lArr is the same as the 'is implied by' arrow
         but also does not have any other character for that function. So ? lArr can
         be used for 'is implied by' as ISOtech suggests */
        {"8657", "uArr", "\\\\Uparrow"}, // upwards double arrow, U+21D1 ISOamsa
        {"8658", "rArr", "\\\\Rightarrow"}, // rightwards double arrow,
        //                                     U+21D2 ISOtech 
        /*   ISO 10646 does not say this is the 'implies' character but does not have 
         another character with this function so ?
         rArr can be used for 'implies' as ISOtech suggests */
        {"8659", "dArr", ""}, // downwards double arrow, U+21D3 ISOamsa  
        {"8660", "hArr", ""}, // left right double arrow, 
        //                                     U+21D4 ISOamsa 

        /* Mathematical Operators */
        {"8704", "forall", "\\$\\\\forall\\$"}, // for all, U+2200 ISOtech  
        {"8706", "part", "\\$\\\\partial\\$"}, // partial differential, U+2202 ISOtech
        {"8707", "exist", "\\$\\\\exists\\$"}, // there exists, U+2203 ISOtech
        {"8709", "empty", "\\$\\\\emptyset\\$"}, // empty set = null set = diameter,
        //                                    U+2205 ISOamso 
        {"8711", "nabla", ""}, // nabla = backward difference, 
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
        {"8727", "lowast", ""}, // asterisk operator, U+2217 ISOtech  
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
        {"8773", "cong", ""}, // approximately equal to, U+2245 ISOtech  
        {"8776", "asymp", ""}, // almost equal to = asymptotic to, 
        //                                    U+2248 ISOamsr 
        {"8800", "ne", ""}, // not equal to, U+2260 ISOtech  
        {"8801", "equiv", ""}, // identical to, U+2261 ISOtech  
        {"8804", "le", "\\$\\\\leq\\$"}, // less-than or equal to, U+2264 ISOtech  
        {"8805", "ge", "\\$\\\\geq\\$"}, // greater-than or equal to, 
        //                                    U+2265 ISOtech 
        {"8834", "sub", ""}, // subset of, U+2282 ISOtech  
        {"8835", "sup", ""}, // superset of, U+2283 ISOtech  
        /*    note that nsup, 'not a superset of, U+2283' is not covered by the Symbol 
         font encoding and is not included. Should it be, for symmetry?
         It is in ISOamsn   */
        {"8836", "nsub", ""}, // not a subset of, U+2284 ISOamsn  
        {"8838", "sube", ""}, // subset of or equal to, U+2286 ISOtech  
        {"8839", "supe", ""}, // superset of or equal to, 
        //                                    U+2287 ISOtech 
        {"8853", "oplus", "\\$\\\\oplus\\$"}, // circled plus = direct sum, 
        //                                    U+2295 ISOamsb 
        {"8855", "otimes", "\\$\\\\otimes\\$"}, // circled times = vector product,
        //                                    U+2297 ISOamsb 
        {"8869", "perp", ""}, // up tack = orthogonal to = perpendicular, 
        //                                    U+22A5 ISOtech 
        {"8901", "sdot", ""}, // dot operator, U+22C5 ISOamsb  
        /* dot operator is NOT the same character as U+00B7 middle dot */
        /* Miscellaneous Technical */
        {"8968", "lceil", ""}, // left ceiling = apl upstile, 
        //                                    U+2308 ISOamsc  
        {"8969", "rceil", ""}, // right ceiling, U+2309 ISOamsc   
        {"8970", "lfloor", ""}, // left floor = apl downstile, 
        //                                    U+230A ISOamsc  
        {"8971", "rfloor", ""}, // right floor, U+230B ISOamsc   
        {"9001", "lang", ""}, // left-pointing angle bracket = bra, 
        //                                    U+2329 ISOtech 
        /*    lang is NOT the same character as U+003C 'less than' 
         or U+2039 'single left-pointing angle quotation mark' */
        {"9002", "rang", ""}, // right-pointing angle bracket = ket, 
        //                                    U+232A ISOtech 
        /*    rang is NOT the same character as U+003E 'greater than' 
         or U+203A 'single right-pointing angle quotation mark' */
        /* Geometric Shapes */
        {"9674", "loz", ""}, // lozenge, U+25CA ISOpub  

        /* Miscellaneous Symbols */
        {"9824", "spades", ""}, // black spade suit, U+2660 ISOpub  
        /* black here seems to mean filled as opposed to hollow */
        {"9827", "clubs", ""}, // black club suit = shamrock, 
        //                                    U+2663 ISOpub 
        {"9829", "hearts", ""}, // black heart suit = valentine, 
        //                                    U+2665 ISOpub 
        {"9830", "diams", ""}, // black diamond suit, U+2666 ISOpub  
        {"34", "quot", "\""}, // quotation mark = APL quote,
        //                                   U+0022 ISOnum 
        {"38", "amp", "&"}, // ampersand, U+0026 ISOnum 
        {"60", "lt", "<"}, // less-than sign, U+003C ISOnum 
        {"62", "gt", ">"}, // greater-than sign, U+003E ISOnum 

        /* Latin Extended-A */
        {"338", "OElig", "\\{\\\\OE\\}"}, // latin capital ligature OE,
        //                                   U+0152 ISOlat2 
        {"339", "oelig", "\\{\\\\oe\\}"}, // latin small ligature oe, U+0153 ISOlat2 
        /* ligature is a misnomer, this is a separate character in some languages */
        {"352", "Scaron", ""}, // latin capital letter S with caron,
        //                                   U+0160 ISOlat2 
        {"353", "scaron", ""}, // latin small letter s with caron,
        //                                   U+0161 ISOlat2 
        {"376", "Yuml", "\\\\\"\\{Y\\}"}, // latin capital letter Y with diaeresis,
        //                                   U+0178 ISOlat2 

        /* Spacing Modifier Letters */
        {"710", "circ", ""}, // modifier letter circumflex accent,
        //                                   U+02C6 ISOpub 
        {"732", "tilde", ""}, // small tilde, U+02DC ISOdia 

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
        {"8216", "lsquo", "`"}, // left single quotation mark, 
        //                                   U+2018 ISOnum 
        {"8217", "rsquo", "'"}, // right single quotation mark, 
        //                                   U+2019 ISOnum 
        {"8218", "sbquo", ""}, // single low-9 quotation mark, U+201A NEW  
        {"8220", "ldquo", "``"}, // left double quotation mark, 
        //                                   U+201C ISOnum 
        {"8221", "rdquo", "''"}, // right double quotation mark, 
        //                                   U+201D ISOnum 
        {"8222", "bdquo", ""}, // double low-9 quotation mark, U+201E NEW  
        {"8224", "dagger", ""}, // dagger, U+2020 ISOpub  
        {"8225", "Dagger", ""}, // double dagger, U+2021 ISOpub  
        {"8240", "permil", ""}, // per mille sign, U+2030 ISOtech  
        {"8249", "lsaquo", ""}, // single left-pointing angle quotation mark, 
        //                                   U+2039 ISO proposed 
        /* lsaquo is proposed but not yet ISO standardized */
        {"8250", "rsaquo", ""}, // single right-pointing angle quotation mark, 
        //                                   U+203A ISO proposed 
        /* rsaquo is proposed but not yet ISO standardized */
        {"8364", "euro", ""}, // euro sign, U+20AC NEW 
            
        /* Manually added */
        {"37", "percnt", "\\\\%"}, // Percent
        {"43", "", "\\+"}, // Plus
        {"123", "", "\\{"}, // Left curly bracket
        {"125", "", "\\}"}, // Right curly bracket
        {"305", "inodot", "\\{\\\\i\\}"},    // Small i without the dot
        {"769", "", "'"},    // Can be solved better as it is a combining accent
        {"774", "", ""},    // FIX: Breve - Can be solved better as it is a combining accent
        {"776", "", ""},    // FIX: Diaeresis - Can be solved better as it is a combining accent
        {"780", "", ""},    // FIX: Caron - Can be solved better as it is a combining accent
        {"8208", "", "-"}    // Hyphen
    };

        private HashMap<String, String> escapedSymbols = new HashMap<String, String>();
        private HashMap<Integer, String> numSymbols = new HashMap<Integer, String>();
        
        
	
	public HTMLConverter() {
		super();
                for (int i=0;i<conversionList.length;i++) {
                    if (conversionList[i][2].length() >= 1) {
                        if (conversionList[i][1].length() >= 1) {
                            escapedSymbols.put("&" + conversionList[i][1] + ";" , conversionList[i][2]);
                        }
                        if (conversionList[i][0].length() >= 1) {
                            numSymbols.put(Integer.decode(conversionList[i][0]) , conversionList[i][2]);
                        }
                    }
                }
	}
        
    public String format(String text) {
        if (text == null)
            return null;
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<text.length(); i++) {

            int c = text.charAt(i);

            if (c == '<') {
                i = readTag(text, sb, i);
            } else
                sb.append((char)c);

        }
        text = sb.toString();
        Set<String> patterns = escapedSymbols.keySet();
        for (String pattern: patterns) {
        	text = text.replaceAll(pattern, escapedSymbols.get(pattern));
        }
        
        Pattern escapedPattern = Pattern.compile("&#([x]*\\p{XDigit}+);");
        Matcher m = escapedPattern.matcher(text);
        while (m.find()) {
            int num = Integer.decode(m.group(1).replace("x", "#"));
            if(numSymbols.containsKey(num)) {
                text = text.replaceAll("&#" + m.group(1) + ";", numSymbols.get(num));
            } else {
                System.err.println("HTML escaped char not converted " + m.group(1) + ": " + Integer.toString(num));
            }
        }
	// Find non-covered special characters with alphabetic codes
        escapedPattern = Pattern.compile("&(\\w+);");
        m = escapedPattern.matcher(text);
        while (m.find()) {
	    System.err.println("HTML escaped char not converted " + m.group(1));
	}

        return text.trim();
    }

    private final int MAX_TAG_LENGTH = 30;
    /*private final int MAX_CHAR_LENGTH = 10;

    private int readHtmlChar(String text, StringBuffer sb, int position) {
        // Have just read the < character that starts the tag.
        int index = text.indexOf(';', position);
        if ((index > position) && (index-position < MAX_CHAR_LENGTH)) {
        	//String code = text.substring(position, index);
            //System.out.println("Removed code: "+text.substring(position, index));
            return index; // Just skip the tag.
        } else return position; // Don't do anything.
    }*/

    private int readTag(String text, StringBuffer sb, int position) {
        // Have just read the < character that starts the tag.
        int index = text.indexOf('>', position);
        if ((index > position) && (index-position < MAX_TAG_LENGTH)) {
            //System.out.println("Removed tag: "+text.substring(position, index));
            return index; // Just skip the tag.
        } else return position; // Don't do anything.
    }
}
