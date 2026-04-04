package com.auction.shared.message;

import com.auction.shared.constant.ActionType;

public class RequestMessage {
    private String payload;
    private ActionType action;

    public RequestMessage() {
    }

    public RequestMessage(ActionType action, String payload) {
        this.action = action;
        this.payload = payload;
    }

    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
        this.action = action;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

`
}
