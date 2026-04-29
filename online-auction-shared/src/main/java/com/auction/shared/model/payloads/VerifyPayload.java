package com.auction.shared.model.payloads;

public class VerifyPayload {
    protected String email, otp;

    public VerifyPayload() {
    }

    public VerifyPayload(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }

    public String getEmail() {
        return email;
    }

    public String getOtp() {
        return otp;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
