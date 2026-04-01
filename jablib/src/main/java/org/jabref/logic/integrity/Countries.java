package org.jabref.logic.integrity;

import java.util.Set;

/// A hard-coded set of country names used by the integrity checker to detect
/// location information (countries) embedded in booktitle fields.
///
/// The list covers all UN-recognized sovereign states and several widely used
/// territories. It can be regenerated from WikiData or a similar authoritative
/// source when updates are required.
public final class Countries {

    /// Set of country names (in English). All entries are stored in lower-case
    /// so that comparisons can be made case-insensitively.
    public static final Set<String> COUNTRY_NAMES = Set.of(
            "afghanistan", "albania", "algeria", "andorra", "angola",
            "antigua and barbuda", "argentina", "armenia", "australia", "austria",
            "azerbaijan", "bahamas", "bahrain", "bangladesh", "barbados",
            "belarus", "belgium", "belize", "benin", "bhutan",
            "bolivia", "bosnia and herzegovina", "botswana", "brazil", "brunei",
            "bulgaria", "burkina faso", "burundi", "cabo verde", "cambodia",
            "cameroon", "canada", "central african republic", "chad", "chile",
            "china", "colombia", "comoros", "congo", "costa rica",
            "croatia", "cuba", "cyprus", "czech republic", "czechia",
            "denmark", "djibouti", "dominica", "dominican republic", "ecuador",
            "egypt", "el salvador", "equatorial guinea", "eritrea", "estonia",
            "eswatini", "ethiopia", "fiji", "finland", "france",
            "gabon", "gambia", "georgia", "germany", "ghana",
            "greece", "grenada", "guatemala", "guinea", "guinea-bissau",
            "guyana", "haiti", "honduras", "hungary", "iceland",
            "india", "indonesia", "iran", "iraq", "ireland",
            "israel", "italy", "jamaica", "japan", "jordan",
            "kazakhstan", "kenya", "kiribati", "kuwait", "kyrgyzstan",
            "laos", "latvia", "lebanon", "lesotho", "liberia",
            "libya", "liechtenstein", "lithuania", "luxembourg", "madagascar",
            "malawi", "malaysia", "maldives", "mali", "malta",
            "marshall islands", "mauritania", "mauritius", "mexico", "micronesia",
            "moldova", "monaco", "mongolia", "montenegro", "morocco",
            "mozambique", "myanmar", "namibia", "nauru", "nepal",
            "netherlands", "new zealand", "nicaragua", "niger", "nigeria",
            "north korea", "north macedonia", "norway", "oman", "pakistan",
            "palau", "palestine", "panama", "papua new guinea", "paraguay",
            "peru", "philippines", "poland", "portugal", "qatar",
            "romania", "russia", "rwanda", "saint kitts and nevis", "saint lucia",
            "saint vincent and the grenadines", "samoa", "san marino", "sao tome and principe", "saudi arabia",
            "senegal", "serbia", "seychelles", "sierra leone", "singapore",
            "slovakia", "slovenia", "solomon islands", "somalia", "south africa",
            "south korea", "south sudan", "spain", "sri lanka", "sudan",
            "suriname", "sweden", "switzerland", "syria", "taiwan",
            "tajikistan", "tanzania", "thailand", "timor-leste", "togo",
            "tonga", "trinidad and tobago", "tunisia", "turkey", "turkmenistan",
            "tuvalu", "uganda", "ukraine", "united arab emirates", "united kingdom",
            "united states", "usa", "uk", "uae", "uruguay",
            "uzbekistan", "vanuatu", "venezuela", "vietnam", "yemen",
            "zambia", "zimbabwe"
    );

    private Countries() {
        // utility class
    }
}
