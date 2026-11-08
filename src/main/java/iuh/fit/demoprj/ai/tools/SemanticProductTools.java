package iuh.fit.demoprj.ai.tools;

import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.service.CatalogService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "mariadb")
public class SemanticProductTools {
    private static final Logger logger = LoggerFactory.getLogger(SemanticProductTools.class);
    private static final int SEARCH_LIMIT = 30;
    private static final int RESULT_LIMIT = 10;

    private final CatalogService catalog;
    private final VectorStore vectorStore;
    private final AiToolInvocationLogger invocationLogger;

    public SemanticProductTools(CatalogService catalog, VectorStore vectorStore,
                                AiToolInvocationLogger invocationLogger) {
        this.catalog = catalog;
        this.vectorStore = vectorStore;
        this.invocationLogger = invocationLogger;
    }

    @Tool(name = "searchProductsSemantic", description = """
            Tìm sản phẩm đang kinh doanh theo ý nghĩa hoặc nhu cầu sử dụng, kể cả khi khách không biết tên chính xác.
            Dùng cho mô tả như nấu ít dầu, làm mát phòng nhỏ hoặc thiết bị tiết kiệm điện.
            Kết quả gồm giá và tồn kho hiện tại lấy từ cơ sở dữ liệu. Có thể lọc thêm mức giá tối đa.
            """)
    public StoreTools.ToolResult<List<StoreTools.ProductSummary>> searchProductsSemantic(
            @ToolParam(description = "Nhu cầu hoặc đặc điểm sản phẩm khách đang tìm") String query,
            @ToolParam(description = "Giá tối đa bằng VND, nếu khách nêu", required = false)
            BigDecimal maxPrice) {
        invocationLogger.logInvocation("searchProductsSemantic");
        if (query == null || query.isBlank() || query.trim().length() > 300) {
            return new StoreTools.ToolResult<>("Vui lòng cung cấp nhu cầu tìm kiếm (tối đa 300 ký tự).", List.of());
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            return new StoreTools.ToolResult<>("Mức giá tối đa không hợp lệ.", List.of());
        }

        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query(query.trim())
                .topK(SEARCH_LIMIT)
                .similarityThreshold(0.60)
                .filterExpression(filter.and(
                        filter.eq("type", "product"),
                        filter.eq("active", true)).build())
                .build();
        List<Document> matches = vectorStore.similaritySearch(request);
        List<Long> productIds = new ArrayList<>();
        for (Document document : matches) {
            Object value = document.getMetadata().get("productId");
            if (value instanceof Number number && number.longValue() > 0) {
                productIds.add(number.longValue());
            } else {
                logger.warn("Skipping semantic product result without a valid productId: {}", document.getId());
            }
        }
        List<Long> orderedIds = productIds.stream().distinct().toList();
        Map<Long, Product> currentProducts = new LinkedHashMap<>();
        for (Product product : catalog.findActiveProductsByIds(orderedIds)) {
            currentProducts.put(product.getId(), product);
        }
        List<StoreTools.ProductSummary> results = new ArrayList<>();
        for (Long id : orderedIds) {
            Product product = currentProducts.get(id);
            if (product == null || maxPrice != null && product.getPrice().compareTo(maxPrice) > 0) {
                continue;
            }
            results.add(new StoreTools.ProductSummary(product.getId(), product.getName(), product.getPrice(),
                    product.getStock(), product.getCategory().getName(), product.getCanonicalPath()));
            if (results.size() == RESULT_LIMIT) {
                break;
            }
        }
        String message = results.isEmpty()
                ? "Không tìm thấy sản phẩm đang kinh doanh phù hợp với mô tả và mức giá."
                : "Sản phẩm gần với nhu cầu; giá và tồn kho được đọc từ dữ liệu hiện tại.";
        return new StoreTools.ToolResult<>(message, results);
    }
}
