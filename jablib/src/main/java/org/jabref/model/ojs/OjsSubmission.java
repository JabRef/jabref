package org.jabref.model.ojs;

import java.util.Iterator;
import java.util.Optional;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Simple model object holding one submission as returned by the OJS 3.1+
/// `GET /api/v1/submissions` REST endpoint, reduced to the fields JabRef needs
/// for the submission tracker dashboard and the Entry Editor "OJS Tracking" tab.
///
/// The OJS response schema has drifted slightly between 3.1/3.2/3.3/3.4 (see
/// https://docs.pkp.sfu.ca/dev/api/ojs/3.4#tag/Submissions), so parsing here is
/// defensive: fields that cannot be found are left empty rather than failing
/// the whole submission.
@NullMarked
public record OjsSubmission(
        int id,
        String journalName,
        String title,
        Optional<String> doi,
        Optional<String> volume,
        Optional<OjsSubmissionStage> stage) {

    private static final Logger LOGGER = LoggerFactory.getLogger(OjsSubmission.class);

    /// Creates an [OjsSubmission] from one entry of the `items` array of a
    /// `submissions` API response.
    ///
    /// @param jsonObject  one submission object from the OJS API
    /// @param journalName the display name of the journal this submission belongs to
    ///                     (the OJS submission object itself does not carry it, since the
    ///                     endpoint is already scoped to a single journal context)
    /// @return the parsed submission
    public static OjsSubmission fromJSONObject(JSONObject jsonObject, String journalName) {
        int id = jsonObject.optInt("id", -1);
        int stageId = jsonObject.optInt("stageId", -1);

        JSONObject currentPublication = extractCurrentPublication(jsonObject);

        String title = extractLocalizedString(currentPublication, "title")
                .or(() -> extractLocalizedString(currentPublication, "fullTitle"))
                .orElse("");
        Optional<String> doi = extractOptionalString(currentPublication, "doi")
                .or(() -> extractOptionalString(currentPublication, "pub-id::doi"));
        // The submissions list endpoint does not consistently expose the issue volume;
        // populate it defensively where present. Left empty rather than incorrect.
        Optional<String> volume = extractOptionalString(currentPublication, "volume")
                .map(String::valueOf);

        return new OjsSubmission(id, journalName, title, doi, volume, OjsSubmissionStage.fromStageId(stageId));
    }

    /// Picks the "current" publication object out of a submission's `publications` array,
    /// preferring the one referenced by `currentPublicationId`, falling back to the last
    /// (most recent) entry if that id is absent or not found.
    @Nullable
    private static JSONObject extractCurrentPublication(JSONObject submission) {
        JSONArray publications = submission.optJSONArray("publications");
        if (publications == null || publications.isEmpty()) {
            return null;
        }

        int currentPublicationId = submission.optInt("currentPublicationId", -1);
        if (currentPublicationId != -1) {
            for (int i = 0; i < publications.length(); i++) {
                JSONObject publication = publications.optJSONObject(i);
                if (publication != null && publication.optInt("id", -1) == currentPublicationId) {
                    return publication;
                }
            }
        }

        return publications.optJSONObject(publications.length() - 1);
    }

    /// OJS localizes several fields (e.g. `title`) as an object keyed by locale, e.g.
    /// `{"en_US": "My Paper"}`. Some deployments/versions return a plain string instead.
    /// This picks the `en_US` value if present, otherwise the first available locale value.
    private static Optional<String> extractLocalizedString(@Nullable JSONObject source, String field) {
        if (source == null || !source.has(field)) {
            return Optional.empty();
        }

        Object rawValue = source.opt(field);
        if (rawValue instanceof String stringValue) {
            return Optional.of(stringValue);
        }
        if (rawValue instanceof JSONObject localizedValue) {
            if (localizedValue.has("en_US")) {
                return Optional.of(localizedValue.getString("en_US"));
            }
            Iterator<String> keys = localizedValue.keys();
            return keys.hasNext() ? Optional.of(localizedValue.getString(keys.next())) : Optional.empty();
        }

        LOGGER.debug("Unexpected type for localized OJS field '{}': {}", field, rawValue);
        return Optional.empty();
    }

    private static Optional<String> extractOptionalString(@Nullable JSONObject source, String field) {
        if (source == null || !source.has(field) || source.isNull(field)) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(source.opt(field)));
    }
}
