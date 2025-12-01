package org.jabref.gui.citedrive;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

public record Tokens(
        AccessToken accessToken,
        RefreshToken refreshToken
) {
}
