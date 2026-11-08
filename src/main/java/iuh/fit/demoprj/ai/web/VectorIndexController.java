package iuh.fit.demoprj.ai.web;

import iuh.fit.demoprj.ai.index.VectorIndexService;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai")
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "mariadb")
public class VectorIndexController {
    private final VectorIndexService vectorIndex;

    public VectorIndexController(VectorIndexService vectorIndex) {
        this.vectorIndex = vectorIndex;
    }

    @PostMapping("/reindex")
    public ResponseEntity<Map<String, String>> reindex() {
        vectorIndex.reindexAllAsync();
        return ResponseEntity.accepted()
                .body(Map.of("message", "Đã đưa yêu cầu nạp lại dữ liệu AI vào hàng đợi."));
    }
}
