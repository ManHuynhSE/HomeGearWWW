package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iuh.fit.demoprj.ai.index.ProductDocumentMapper;
import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.model.entity.ProductSpecification;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProductDocumentMapperTest {
    private final ProductDocumentMapper mapper = new ProductDocumentMapper();

    @Test
    void mapsStableProductIdentityAndSearchableDetailsWithoutVolatileValues() {
        Category category = new Category("Nhà bếp");
        ReflectionTestUtils.setField(category, "id", 4L);
        Product product = new Product("Nồi chiên", BigDecimal.valueOf(1_000_000), 8,
                "Nấu ít dầu", category);
        ReflectionTestUtils.setField(product, "id", 22L);
        product.setSpecifications(List.of(new ProductSpecification(
                "dung-tich", "Dung tích", "5", "L", 0)));

        var document = mapper.toDocument(product);
        product.setPrice(BigDecimal.valueOf(2_000_000));
        product.setStock(2);
        var updatedDocument = mapper.toDocument(product);

        assertEquals("product-22", document.getMetadata().get("recordId"));
        assertEquals(22L, document.getMetadata().get("productId"));
        assertEquals(4L, document.getMetadata().get("categoryId"));
        assertEquals(document.getId(), updatedDocument.getId());
        assertEquals(document.getText(), updatedDocument.getText());
        assertTrue(document.getText().contains("Nồi chiên"));
        assertTrue(document.getText().contains("Dung tích: 5 L"));
        assertFalse(document.getText().contains("1000000"));
        assertFalse(document.getText().contains("8"));
    }
}
