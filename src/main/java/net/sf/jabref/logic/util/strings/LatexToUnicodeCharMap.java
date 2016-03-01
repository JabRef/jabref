package net.sf.jabref.logic.util.strings;

import java.util.HashMap;

public class LatexToUnicodeCharMap extends HashMap<String, String> {
    public LatexToUnicodeCharMap() {
        put("`A", "À"); // #192
        put("'A", "Á"); // #193
        put("^A", "Â"); // #194
        put("~A", "Ã"); // #195
        put("\"A", "Ä"); // #196
        put("AA", "Å"); // #197
        put("AE", "Æ"); // #198
        put("cC", "Ç"); // #199
        put("`E", "È"); // #200
        put("'E", "É"); // #201
        put("^E", "Ê"); // #202
        put("\"E", "Ë"); // #203
        put("`I", "Ì"); // #204
        put("'I", "Í"); // #205
        put("^I", "Î"); // #206
        put("\"I", "Ï"); // #207
        put("DH", "Ð"); // #208
        put("~N", "Ñ"); // #209
        put("`O", "Ò"); // #210
        put("'O", "Ó"); // #211
        put("^O", "Ô"); // #212
        put("~O", "Õ"); // #213
        put("\"O", "Ö"); // #214
        // According to ISO 8859-1 the "\times" symbol should be placed here
        // (#215).
        // Omitting this, because it is a mathematical symbol.
        put("O", "Ø"); // #216
        put("`U", "Ù"); // #217
        put("'U", "Ú"); // #218
        put("^U", "Û"); // #219
        put("\"U", "Ü"); // #220
        put("'Y", "Ý"); // #221
        put("TH", "Þ"); // #222
        put("ss", "ß"); // #223
        put("`a", "à"); // #224
        put("'a", "á"); // #225
        put("^a", "â"); // #226
        put("~a", "ã"); // #227
        put("\"a", "ä"); // #228
        put("aa", "å"); // #229
        put("ae", "æ"); // #230
        put("cc", "ç"); // #231
        put("`e", "è"); // #232
        put("'e", "é"); // #233
        put("^e", "ê"); // #234
        put("\"e", "ë"); // #235
        put("`i", "ì"); // #236
        put("'i", "í"); // #237
        put("^i", "î"); // #238
        put("\"i", "ï"); // #239
        put("dh", "ð"); // #240
        put("~n", "ñ"); // #241
        put("`o", "ò"); // #242
        put("'o", "ó"); // #243
        put("^o", "ô"); // #244
        put("~o", "õ"); // #245
        put("\"o", "ö"); // #246
        // According to ISO 8859-1 the "\div" symbol should be placed here
        // (#247).
        // Omitting this, because it is a mathematical symbol.
        put("o", "ø"); // #248
        put("`u", "ù"); // #249
        put("'u", "ú"); // #250
        put("^u", "û"); // #251
        put("\"u", "ü"); // #252
        put("'y", "ý"); // #253
        put("th", "þ"); // #254
        put("\"y", "ÿ"); // #255

        // HTML special characters without names (UNICODE Latin Extended-A),
        // indicated by UNICODE number
        put("=A", "Ā"); // "Amacr"
        put("=a", "ā"); // "amacr"
        put("uA", "Ă"); // "Abreve"
        put("ua", "ă"); // "abreve"
        put("kA", "Ą"); // "Aogon"
        put("ka", "ą"); // "aogon"
        put("'C", "Ć"); // "Cacute"
        put("'c", "ć"); // "cacute"
        put("^C", "Ĉ"); // "Ccirc"
        put("^c", "ĉ"); // "ccirc"
        put(".C", "Ċ"); // "Cdot"
        put(".c", "ċ"); // "cdot"
        put("vC", "Č"); // "Ccaron"
        put("vc", "č"); // "ccaron"
        put("vD", "Ď"); // "Dcaron"
        // Symbol #271 (d) has no special Latex command
        put("DJ", "Đ"); // "Dstrok"
        put("dj", "đ"); // "dstrok"
        put("=E", "Ē"); // "Emacr"
        put("=e", "ē"); // "emacr"
        put("uE", "Ĕ"); // "Ebreve"
        put("ue", "ĕ"); // "ebreve"
        put(".E", "Ė"); // "Edot"
        put(".e", "ė"); // "edot"
        put("kE", "Ę"); // "Eogon"
        put("ke", "ę"); // "eogon"
        put("vE", "Ě"); // "Ecaron"
        put("ve", "ě"); // "ecaron"
        put("^G", "Ĝ"); // "Gcirc"
        put("^g", "ĝ"); // "gcirc"
        put("uG", "Ğ"); // "Gbreve"
        put("ug", "ğ"); // "gbreve"
        put(".G", "Ġ"); // "Gdot"
        put(".g", "ġ"); // "gdot"
        put("cG", "Ģ"); // "Gcedil"
        put("'g", "ģ"); // "gacute"
        put("^H", "Ĥ"); // "Hcirc"
        put("^h", "ĥ"); // "hcirc"
        put("Hstrok", "Ħ"); // "Hstrok"
        put("hstrok", "ħ"); // "hstrok"
        put("~I", "Ĩ"); // "Itilde"
        put("~i", "ĩ"); // "itilde"
        put("=I", "Ī"); // "Imacr"
        put("=i", "ī"); // "imacr"
        put("uI", "Ĭ"); // "Ibreve"
        put("ui", "ĭ"); // "ibreve"
        put("kI", "Į"); // "Iogon"
        put("ki", "į"); // "iogon"
        put(".I", "İ"); // "Idot"
        put("i", "ı"); // "inodot"
        // Symbol #306 (IJ) has no special Latex command
        // Symbol #307 (ij) has no special Latex command
        put("^J", "Ĵ"); // "Jcirc"
        put("^j", "ĵ"); // "jcirc"
        put("cK", "Ķ"); // "Kcedil"
        put("ck", "ķ"); // "kcedil"
        // Symbol #312 (k) has no special Latex command
        put("'L", "Ĺ"); // "Lacute"
        put("'l", "ĺ"); // "lacute"
        put("cL", "Ļ"); // "Lcedil"
        put("cl", "ļ"); // "lcedil"
        // Symbol #317 (L) has no special Latex command
        // Symbol #318 (l) has no special Latex command
        put("Lmidot", "Ŀ"); // "Lmidot"
        put("lmidot", "ŀ"); // "lmidot"
        put("L", "Ł"); // "Lstrok"
        put("l", "ł"); // "lstrok"
        put("'N", "Ń"); // "Nacute"
        put("'n", "ń"); // "nacute"
        put("cN", "Ņ"); // "Ncedil"
        put("cn", "ņ"); // "ncedil"
        put("vN", "Ň"); // "Ncaron"
        put("vn", "ň"); // "ncaron"
        // Symbol #329 (n) has no special Latex command
        put("NG", "Ŋ"); // "ENG"
        put("ng", "ŋ"); // "eng"
        put("=O", "Ō"); // "Omacr"
        put("=o", "ō"); // "omacr"
        put("uO", "Ŏ"); // "Obreve"
        put("uo", "ŏ"); // "obreve"
        put("HO", "Ő"); // "Odblac"
        put("Ho", "ő"); // "odblac"
        put("OE", "Œ"); // "OElig"
        put("oe", "œ"); // "oelig"
        put("'R", "Ŕ"); // "Racute"
        put("'r", "ŕ"); // "racute"
        put("cR", "Ŗ"); // "Rcedil"
        put("cr", "ŗ"); // "rcedil"
        put("vR", "Ř"); // "Rcaron"
        put("vr", "ř"); // "rcaron"
        put("'S", "Ś"); // "Sacute"
        put("'s", "ś"); // "sacute"
        put("^S", "Ŝ"); // "Scirc"
        put("^s", "ŝ"); // "scirc"
        put("cS", "Ş"); // "Scedil"
        put("cs", "ş"); // "scedil"
        put("vS", "Š"); // "Scaron"
        put("vs", "š"); // "scaron"
        put("cT", "Ţ"); // "Tcedil"
        put("ct", "ţ"); // "tcedil"
        put("vT", "Ť"); // "Tcaron"
        // Symbol #357 (t) has no special Latex command
        put("Tstrok", "Ŧ"); // "Tstrok"
        put("tstrok", "ŧ"); // "tstrok"
        put("~U", "Ũ"); // "Utilde"
        put("~u", "ũ"); // "utilde"
        put("=U", "Ū"); // "Umacr"
        put("=u", "ū"); // "umacr"
        put("uU", "Ŭ"); // "Ubreve"
        put("uu", "ŭ"); // "ubreve"
        put("rU", "Ů"); // "Uring"
        put("ru", "ů"); // "uring"
        put("HU", "ů"); // "Odblac"
        put("Hu", "ű"); // "odblac"
        put("kU", "Ų"); // "Uogon"
        put("ku", "ų"); // "uogon"
        put("^W", "Ŵ"); // "Wcirc"
        put("^w", "ŵ"); // "wcirc"
        put("^Y", "Ŷ"); // "Ycirc"
        put("^y", "ŷ"); // "ycirc"
        put("\"Y", "Ÿ"); // "Yuml"
        put("'Z", "Ź"); // "Zacute"
        put("'z", "ź"); // "zacute"
        put(".Z", "Ż"); // "Zdot"
        put(".z", "ż"); // "zdot"
        put("vZ", "Ž"); // "Zcaron"
        put("vz", "ž"); // "zcaron"
        // Symbol #383 (f) has no special Latex command
        put("%", "%"); // percent sign
    }

}
