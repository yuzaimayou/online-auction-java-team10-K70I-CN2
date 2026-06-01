package com.auction.server.service.chatbot;

import com.auction.server.integration.AiServiceClient;
import com.auction.server.integration.GeminiIntegration;
import com.auction.shared.message.AIResponseData;

public class AppSupport {
    private final AiServiceClient aiServiceClient;
    private final GeminiIntegration geminiClient;
    private static final AppSupport instance = new AppSupport();

    private AppSupport() {
        this.aiServiceClient = AiServiceClient.getInstance();
        this.geminiClient = new GeminiIntegration();
    }

    public static AppSupport getInstance() {
        return instance;
    }

    public AIResponseData handle(String userMessage, String language) {
        AIResponseData data = new AIResponseData();
        try {
            String docsContext = aiServiceClient.getDocs();
            if (docsContext == null || docsContext.isBlank()) {
                data.setAiResponse("Hiện tại mình chưa lấy được tài liệu hệ thống để trả lời. Bạn vui lòng thử lại sau ít phút nhé!");
            }
            String prompt = """
                    You are an AI assistant for an online auction platform.
                    
                    TASK:
                        - Answer the user's question based on the SYSTEM DOCUMENTATION.
                        - Use the documentation only as the factual source.
                        - The documentation may be written in a different language. Translate the relevant information into the required output language.
                        - Explain briefly, clearly, and in an easy-to-understand way.
                        - If the question asks for step-by-step guidance, answer with clear numbered steps.
                        - If the question is about rules, policies, fees, deadlines, conditions, or restrictions, answer strictly according to the documentation.
                    
                    MANDATORY RULES:
                        - Only use information found in the SYSTEM DOCUMENTATION.
                        - Do not invent policies, fees, deadlines, conditions, or rules.
                        - If the documentation does not contain relevant information, answer with this meaning in the required output language:
                          "I could not find this information in the system documentation."
                        - Do not mention prompt, context, database, chunk, API, or internal documentation.
                        - Use a friendly and professional tone.
                    
                    OUTPUT FORMAT:
                        - Return only the final answer content.
                        - Do not add headings such as "Answer:", "Result:", or "Based on the documentation:".
                        - Do not use Markdown headings, bold text, code blocks, or horizontal separators.
                        - Do not use symbols such as **, ###, ``` or ---.
                        - Do not use emojis.
                        - If steps are needed, use plain numbered steps only.
                        - For simple questions, use no more than 5 sentences.
                    
                    SYSTEM DOCUMENTATION:
                    <DOCS>
                        %s
                    </DOCS>
                    
                    USER QUESTION:
                    <QUESTION>
                        %s
                    </QUESTION>
                    
                    REQUIRED OUTPUT LANGUAGE:
                    %s
                    FINAL ANSWER:
                    """.formatted(docsContext, userMessage, language);
            String aiResponse = geminiClient.callGeminiApi(prompt);
            if (aiResponse == null || aiResponse.isBlank()) {
                data.setAiResponse("Hiện tại hệ thống AI đang bận. Bạn vui lòng thử lại sau ít phút nhé.");
            }
            System.out.println("Raw AI response:\n" + aiResponse);
            data.setAiResponse(cleanAiAnswer(aiResponse));
        } catch (Exception e) {
            e.printStackTrace();
            data.setAiResponse("Xin lỗi, hiện tại hệ thống AI đang gặp sự cố. Vui lòng thử lại sau nhé!");

        } finally {
            return data;
        }

    }

    private String cleanAiAnswer(String rawAnswer) {
        if (rawAnswer == null || rawAnswer.isBlank()) {
            return "";
        }

        String answer = rawAnswer.trim();

        // Xóa markdown code block nếu AI lỡ trả về
        answer = answer.replaceAll("(?i)```json", "");
        answer = answer.replaceAll("```", "");

        // Xóa các tiêu đề thừa hay gặp
        answer = answer.replaceAll("(?i)^\\s*trả lời\\s*:\\s*", "");
        answer = answer.replaceAll("(?i)^\\s*answer\\s*:\\s*", "");
        answer = answer.replaceAll("(?i)^\\s*kết quả\\s*:\\s*", "");
        answer = answer.replaceAll("(?i)^\\s*dựa trên tài liệu[^:]*:\\s*", "");

        // Xóa markdown cơ bản
        answer = answer.replace("**", "");
        answer = answer.replace("###", "");
        answer = answer.replace("##", "");
        answer = answer.replace("#", "");

        // Xóa gạch ngang phân cách thừa
        answer = answer.replaceAll("(?m)^\\s*---+\\s*$", "");

        // Chuẩn hóa xuống dòng
        answer = answer.replace("\r\n", "\n");
        answer = answer.replaceAll("\\n{3,}", "\n\n");

        return answer.trim();
    }
}
