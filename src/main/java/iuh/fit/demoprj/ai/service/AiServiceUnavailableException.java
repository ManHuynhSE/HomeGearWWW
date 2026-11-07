package iuh.fit.demoprj.ai.service;

public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException() {
        super("Dịch vụ AI chưa được cấu hình.");
    }
}
