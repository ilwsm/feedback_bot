// OpenAIClassifier.java
package org.example.openai;

import com.openai.models.ChatModel;
import com.openai.models.chat.completions.*;

import java.util.Objects;

public class OpenAIClassifier {

    private static final String SYSTEM_PROMPT = """
            You are a system that analyzes customer service feedback written in Ukrainian.
            You must extract structured data in JSON format with the following fields:
                           
            - role: string, one of ["MECHANIC", "ELECTRICIAN", "MANAGER"]
            - branch: string, the branch or location mentioned (if unknown, return "UNKNOWN")
            - sentiment: string, one of ["NEGATIVE", "NEUTRAL", "POSITIVE"]
            - criticality: integer, from 1 (very low) to 5 (very high)
            - recommendation: string, a short actionable suggestion how to resolve the issue
                           
            Rules:
            - Always return a valid JSON object (no extra text).
            - If the feedback is ambiguous, make the best reasonable guess.
            - Answer strictly in English for the JSON field names, but values should be in Ukrainian if taken from user text.                                                                                         
             """;

    private final OpenAIService service;

    public OpenAIClassifier(OpenAIService service) {
        this.service = service;
    }

    public FeedbackAnalysis analyze(String freeText) {


        ChatCompletionSystemMessageParam system = ChatCompletionSystemMessageParam.builder()
                .content(SYSTEM_PROMPT)
                .build();

        ChatCompletionUserMessageParam user = ChatCompletionUserMessageParam.builder()
                .content("Text: " + freeText)
                .build();

        StructuredChatCompletionCreateParams<FeedbackAnalysis> params =
                ChatCompletionCreateParams
                        .builder()
                        .model(ChatModel.GPT_5_MINI)
                        .addMessage(system)
                        .addMessage(user)
                        .responseFormat(FeedbackAnalysis.class)
                        .build();

        StructuredChatCompletion<FeedbackAnalysis> completion = service.client().chat().completions().create(params);

        return completion
                .choices()
                .stream()
                .flatMap(c -> c.message().content().stream())
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No structured result"));
    }
}
