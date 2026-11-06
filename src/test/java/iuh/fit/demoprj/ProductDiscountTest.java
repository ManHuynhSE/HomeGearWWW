package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductDiscountTest {
    @Test
    void calculatesDiscountOnlyWhenOriginalPriceExceedsSellingPrice() {
        Product product = new Product(
                "Test product", new BigDecimal("1890000"), 1, "", new Category("Test category"));
        product.setOriginalPrice(new BigDecimal("2100000"));
        assertEquals(10, product.getDiscountPercent());

        product.setOriginalPrice(new BigDecimal("1800000"));
        assertEquals(0, product.getDiscountPercent());
    }
}
