package com.mdsol.mauth;

import com.mdsol.mauth.exceptions.MAuthSigningException;

import java.net.URI;
import java.util.Map;

public class SignerUtils {
    /**
     * Sign a request with the provided Signer. Request path and query parameter are extracted from the Java URI
     * This helper function is provided to avoid confusion with whether the path / query string needs to be encoded or not
     *
     * @param signer
     * @param httpVerb
     * @param uri
     * @param requestPayload
     * @return
     * @throws MAuthSigningException
     */
    public static Map<String, String> signWithUri(Signer signer, String httpVerb, URI uri, byte[] requestPayload) throws MAuthSigningException {
        return signer.generateRequestHeaders(
                httpVerb,
                uri.getRawPath(),
                requestPayload,
                uri.getRawQuery()
        );
    }
}
