package iuh.fit.demoprj.ai.index;

import iuh.fit.demoprj.model.entity.Product;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {

    public Document toDocument(Product product) {
        String recordId = recordId(product.getId());
        String specifications = product.getSpecifications().stream()
                .map(specification -> specification.getLabel() + ": " + specification.getValue()
                        + (specification.getUnit() == null ? "" : " " + specification.getUnit()))
                .collect(Collectors.joining("\n"));
        String text = """
                Sản phẩm: %s
                Danh mục: %s
                Mô tả: %s
                Thông số kỹ thuật:
                %s
                """.formatted(product.getName(), product.getCategory().getName(),
                product.getDescription(), specifications).trim();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", "product");
        metadata.put("productId", product.getId());
        metadata.put("categoryId", product.getCategory().getId());
        metadata.put("active", product.isActive());
        metadata.put("recordId", recordId);
        return new Document(documentId(recordId), text, metadata);
    }

    public String recordId(Long productId) {
        return "product-" + productId;
    }

    public String documentId(Long productId) {
        return documentId(recordId(productId));
    }

    private String documentId(String recordId) {
        return UUID.nameUUIDFromBytes(recordId.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
