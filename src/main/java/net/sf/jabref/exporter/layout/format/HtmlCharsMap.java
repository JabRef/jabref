package net.sf.jabref.exporter.layout.format;

import java.util.HashMap;

class HtmlCharsMap extends HashMap<String, String> {
    public HtmlCharsMap() {
        // HTML named entities from #192 - #255 (UNICODE Latin-1)
        put("`A", "&Agrave;"); // #192
        put("'A", "&Aacute;"); // #193
        put("^A", "&Acirc;"); // #194
        put("~A", "&Atilde;"); // #195
        put("\"A", "&Auml;"); // #196
        put("AA", "&Aring;"); // #197
        put("AE", "&AElig;"); // #198
        put("cC", "&Ccedil;"); // #199
        put("`E", "&Egrave;"); // #200
        put("'E", "&Eacute;"); // #201
        put("^E", "&Ecirc;"); // #202
        put("\"E", "&Euml;"); // #203
        put("`I", "&Igrave;"); // #204
        put("'I", "&Iacute;"); // #205
        put("^I", "&Icirc;"); // #206
        put("\"I", "&Iuml;"); // #207
        put("DH", "&ETH;"); // #208
        put("~N", "&Ntilde;"); // #209
        put("`O", "&Ograve;"); // #210
        put("'O", "&Oacute;"); // #211
        put("^O", "&Ocirc;"); // #212
        put("~O", "&Otilde;"); // #213
        put("\"O", "&Ouml;"); // #214
        // According to ISO 8859-1 the "\times" symbol should be placed here
        // (#215).
        // Omitting this, because it is a mathematical symbol.
        put("O", "&Oslash;"); // #216
        put("`U", "&Ugrave;"); // #217
        put("'U", "&Uacute;"); // #218
        put("^U", "&Ucirc;"); // #219
        put("\"U", "&Uuml;"); // #220
        put("'Y", "&Yacute;"); // #221
        put("TH", "&THORN;"); // #222
        put("ss", "&szlig;"); // #223
        put("`a", "&agrave;"); // #224
        put("'a", "&aacute;"); // #225
        put("^a", "&acirc;"); // #226
        put("~a", "&atilde;"); // #227
        put("\"a", "&auml;"); // #228
        put("aa", "&aring;"); // #229
        put("ae", "&aelig;"); // #230
        put("cc", "&ccedil;"); // #231
        put("`e", "&egrave;"); // #232
        put("'e", "&eacute;"); // #233
        put("^e", "&ecirc;"); // #234
        put("\"e", "&euml;"); // #235
        put("`i", "&igrave;"); // #236
        put("'i", "&iacute;"); // #237
        put("^i", "&icirc;"); // #238
        put("\"i", "&iuml;"); // #239
        put("dh", "&eth;"); // #240
        put("~n", "&ntilde;"); // #241
        put("`o", "&ograve;"); // #242
        put("'o", "&oacute;"); // #243
        put("^o", "&ocirc;"); // #244
        put("~o", "&otilde;"); // #245
        put("\"o", "&ouml;"); // #246
        // According to ISO 8859-1 the "\div" symbol should be placed here
        // (#247).
        // Omitting this, because it is a mathematical symbol.
        put("o", "&oslash;"); // #248
        put("`u", "&ugrave;"); // #249
        put("'u", "&uacute;"); // #250
        put("^u", "&ucirc;"); // #251
        put("\"u", "&uuml;"); // #252
        put("'y", "&yacute;"); // #253
        put("th", "&thorn;"); // #254
        put("\"y", "&yuml;"); // #255

        // HTML special characters without names (UNICODE Latin Extended-A),
        // indicated by UNICODE number
        put("=A", "&#256;"); // "Amacr"
        put("=a", "&#257;"); // "amacr"
        put("uA", "&#258;"); // "Abreve"
        put("ua", "&#259;"); // "abreve"
        put("kA", "&#260;"); // "Aogon"
        put("ka", "&#261;"); // "aogon"
        put("'C", "&#262;"); // "Cacute"
        put("'c", "&#263;"); // "cacute"
        put("^C", "&#264;"); // "Ccirc"
        put("^c", "&#265;"); // "ccirc"
        put(".C", "&#266;"); // "Cdot"
        put(".c", "&#267;"); // "cdot"
        put("vC", "&#268;"); // "Ccaron"
        put("vc", "&#269;"); // "ccaron"
        put("vD", "&#270;"); // "Dcaron"
        // Symbol #271 (d) has no special Latex command
        put("DJ", "&#272;"); // "Dstrok"
        put("dj", "&#273;"); // "dstrok"
        put("=E", "&#274;"); // "Emacr"
        put("=e", "&#275;"); // "emacr"
        put("uE", "&#276;"); // "Ebreve"
        put("ue", "&#277;"); // "ebreve"
        put(".E", "&#278;"); // "Edot"
        put(".e", "&#279;"); // "edot"
        put("kE", "&#280;"); // "Eogon"
        put("ke", "&#281;"); // "eogon"
        put("vE", "&#282;"); // "Ecaron"
        put("ve", "&#283;"); // "ecaron"
        put("^G", "&#284;"); // "Gcirc"
        put("^g", "&#285;"); // "gcirc"
        put("uG", "&#286;"); // "Gbreve"
        put("ug", "&#287;"); // "gbreve"
        put(".G", "&#288;"); // "Gdot"
        put(".g", "&#289;"); // "gdot"
        put("cG", "&#290;"); // "Gcedil"
        put("'g", "&#291;"); // "gacute"
        put("^H", "&#292;"); // "Hcirc"
        put("^h", "&#293;"); // "hcirc"
        put("Hstrok", "&#294;"); // "Hstrok"
        put("hstrok", "&#295;"); // "hstrok"
        put("~I", "&#296;"); // "Itilde"
        put("~i", "&#297;"); // "itilde"
        put("=I", "&#298;"); // "Imacr"
        put("=i", "&#299;"); // "imacr"
        put("uI", "&#300;"); // "Ibreve"
        put("ui", "&#301;"); // "ibreve"
        put("kI", "&#302;"); // "Iogon"
        put("ki", "&#303;"); // "iogon"
        put(".I", "&#304;"); // "Idot"
        put("i", "&#305;"); // "inodot"
        // Symbol #306 (IJ) has no special Latex command
        // Symbol #307 (ij) has no special Latex command
        put("^J", "&#308;"); // "Jcirc"
        put("^j", "&#309;"); // "jcirc"
        put("cK", "&#310;"); // "Kcedil"
        put("ck", "&#311;"); // "kcedil"
        // Symbol #312 (k) has no special Latex command
        put("'L", "&#313;"); // "Lacute"
        put("'l", "&#314;"); // "lacute"
        put("cL", "&#315;"); // "Lcedil"
        put("cl", "&#316;"); // "lcedil"
        // Symbol #317 (L) has no special Latex command
        // Symbol #318 (l) has no special Latex command
        put("Lmidot", "&#319;"); // "Lmidot"
        put("lmidot", "&#320;"); // "lmidot"
        put("L", "&#321;"); // "Lstrok"
        put("l", "&#322;"); // "lstrok"
        put("'N", "&#323;"); // "Nacute"
        put("'n", "&#324;"); // "nacute"
        put("cN", "&#325;"); // "Ncedil"
        put("cn", "&#326;"); // "ncedil"
        put("vN", "&#327;"); // "Ncaron"
        put("vn", "&#328;"); // "ncaron"
        // Symbol #329 (n) has no special Latex command
        put("NG", "&#330;"); // "ENG"
        put("ng", "&#331;"); // "eng"
        put("=O", "&#332;"); // "Omacr"
        put("=o", "&#333;"); // "omacr"
        put("uO", "&#334;"); // "Obreve"
        put("uo", "&#335;"); // "obreve"
        put("HO", "&#336;"); // "Odblac"
        put("Ho", "&#337;"); // "odblac"
        put("OE", "&#338;"); // "OElig"
        put("oe", "&#339;"); // "oelig"
        put("'R", "&#340;"); // "Racute"
        put("'r", "&#341;"); // "racute"
        put("cR", "&#342;"); // "Rcedil"
        put("cr", "&#343;"); // "rcedil"
        put("vR", "&#344;"); // "Rcaron"
        put("vr", "&#345;"); // "rcaron"
        put("'S", "&#346;"); // "Sacute"
        put("'s", "&#347;"); // "sacute"
        put("^S", "&#348;"); // "Scirc"
        put("^s", "&#349;"); // "scirc"
        put("cS", "&#350;"); // "Scedil"
        put("cs", "&#351;"); // "scedil"
        put("vS", "&#352;"); // "Scaron"
        put("vs", "&#353;"); // "scaron"
        put("cT", "&#354;"); // "Tcedil"
        put("ct", "&#355;"); // "tcedil"
        put("vT", "&#356;"); // "Tcaron"
        // Symbol #357 (t) has no special Latex command
        put("Tstrok", "&#358;"); // "Tstrok"
        put("tstrok", "&#359;"); // "tstrok"
        put("~U", "&#360;"); // "Utilde"
        put("~u", "&#361;"); // "utilde"
        put("=U", "&#362;"); // "Umacr"
        put("=u", "&#363;"); // "umacr"
        put("uU", "&#364;"); // "Ubreve"
        put("uu", "&#365;"); // "ubreve"
        put("rU", "&#366;"); // "Uring"
        put("ru", "&#367;"); // "uring"
        put("HU", "&#368;"); // "Odblac"
        put("Hu", "&#369;"); // "odblac"
        put("kU", "&#370;"); // "Uogon"
        put("ku", "&#371;"); // "uogon"
        put("^W", "&#372;"); // "Wcirc"
        put("^w", "&#373;"); // "wcirc"
        put("^Y", "&#374;"); // "Ycirc"
        put("^y", "&#375;"); // "ycirc"
        put("\"Y", "&#376;"); // "Yuml"
        put("'Z", "&#377;"); // "Zacute"
        put("'z", "&#378;"); // "zacute"
        put(".Z", "&#379;"); // "Zdot"
        put(".z", "&#380;"); // "zdot"
        put("vZ", "&#381;"); // "Zcaron"
        put("vz", "&#382;"); // "zcaron"
        // Symbol #383 (f) has no special Latex command
        put("%", "%"); // percent sign
    }

}
