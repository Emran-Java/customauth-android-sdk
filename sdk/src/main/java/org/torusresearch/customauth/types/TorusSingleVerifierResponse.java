package org.torusresearch.customauth.types;

public class TorusSingleVerifierResponse {
    private final TorusVerifierUnionResponse userInfo;

    public TorusSingleVerifierResponse(TorusVerifierUnionResponse userInfo) {
        this.userInfo = userInfo;
    }

    public TorusVerifierUnionResponse getUserInfo() {
        return userInfo;
    }
}
