package com.Helpdesk.AiHelpdesk.service;

import com.Helpdesk.AiHelpdesk.dto.TicketRequest;
import com.Helpdesk.AiHelpdesk.entity.Priority;
import com.Helpdesk.AiHelpdesk.entity.Status;
import com.Helpdesk.AiHelpdesk.entity.Ticket;
import com.Helpdesk.AiHelpdesk.tools.EmailTool;
import com.Helpdesk.AiHelpdesk.tools.TicketDatabaseTool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Getter
@Setter
public class AIService {

    private final ChatClient chatClient;

    private final TicketDatabaseTool ticketDatabaseTool;
    private final EmailTool emailTool;
    private final ObjectMapper objectMapper;
    private final TicketService ticketService;

    @Value("classpath:/helpdesk-system.st")
    private Resource systemPromptResource;

    public String getResponseFromAssistant(String query, String conversationId) {



        String response = this.chatClient
                .prompt()
                .advisors(advisorSpec ->
                        advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId)
                )
                .tools(ticketDatabaseTool, emailTool)
                .system(systemPromptResource)
                .user(query)
                .call()
                .content();


        try {
            TicketRequest request = objectMapper.readValue(response, TicketRequest.class);

            if ("CREATE_TICKET".equalsIgnoreCase(request.getAction())) {

                Ticket ticket = Ticket.builder()
                        .summary(request.getSummary())
                        .description(request.getDescription())
                        .category(request.getCategory())
                        .priority(Priority.valueOf(request.getPriority()))
                        .status(Status.OPEN)
                        .email(request.getEmail())
                        .build();

                Ticket saved = ticketService.createTicket(ticket);

                return "Ticket Created Successfully!\nTicket ID: " + saved.getId();
            }

        } catch (Exception e) {
            // If response is not in expected format, just return the original response
            return response;
        }

        return response;
    }

    public Flux<String> streamResponseFromAssistant(String query, String conversationId) {


        return this.chatClient
                .prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))

                .tools(ticketDatabaseTool, emailTool)
                .system(systemPromptResource)
                .user(query)
                .stream().content();

    }


}
