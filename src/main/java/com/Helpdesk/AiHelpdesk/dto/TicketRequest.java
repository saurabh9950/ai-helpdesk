package com.Helpdesk.AiHelpdesk.dto;

import lombok.Data;

@Data
public class TicketRequest {
    private String action;
    private String summary;
    private String description;
    private String priority;
    private String category;
    private String email;
}