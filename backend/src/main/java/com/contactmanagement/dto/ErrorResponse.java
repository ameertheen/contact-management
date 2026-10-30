package com.contactmanagement.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ErrorResponse {

    private int status;
    private String error;
    private List<String> messages;

    public ErrorResponse(int status, String error, List<String> messages) {
        this.status = status;
        this.error = error;
        this.messages = List.copyOf(messages);
    }
}
