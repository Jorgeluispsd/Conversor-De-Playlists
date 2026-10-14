package com.jorge.playlistconverter.youtube.authorization;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;

public class GooglePkceAuthorizationFlow extends AuthorizationCodeFlow {
    public GooglePkceAuthorizationFlow(
            AuthorizationCodeFlow.Builder builder
    ){
        super(builder);
    }

    @Override
    public AuthorizationCodeRequestUrl newAuthorizationUrl() {
        AuthorizationCodeRequestUrl url = super.newAuthorizationUrl();

        url.set("access_type", "offline");

        return url;
    }
}
