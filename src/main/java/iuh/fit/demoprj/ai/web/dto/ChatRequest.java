package iuh.fit.demoprj.ai.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "Vui lòng nhập nội dung cần hỏi.")
        @Size(max = 500, message = "Nội dung tối đa 500 ký tự.")
        String message) {
}
