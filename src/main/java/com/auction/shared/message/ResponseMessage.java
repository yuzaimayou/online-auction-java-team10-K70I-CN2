package com.auction.shared.message;

public class ResponseMessage {
    private String status, message,data;
    public ResponseMessage(){}

    public ResponseMessage(String status,String message, String data){
        this.status=status;
        this.message=message;
        this.data=data;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

}
