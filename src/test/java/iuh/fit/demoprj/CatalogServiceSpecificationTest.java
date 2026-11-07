package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.repository.CategoryRepository;
import iuh.fit.demoprj.repository.OrderItemRepository;
import iuh.fit.demoprj.repository.ProductRepository;
import iuh.fit.demoprj.service.CatalogService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogServiceSpecificationTest {

    @Mock
    private ProductRepository products;
    @Mock
    private CategoryRepository categories;
    @Mock
    private OrderItemRepository orderItems;

    private CatalogService catalog;
    private Category category;

    @BeforeEach
    void setUp() {
        catalog = new CatalogService(products, categories, orderItems);
        category = new Category("Nhà bếp");
    }

    @Test
    void saveProductParsesSpecificationsAndAllowsMissingUnit() {
        when(categories.findById(1L)).thenReturn(Optional.of(category));
        when(products.existsByNameIgnoreCaseAndCategoryId("Nồi cơm", 1L)).thenReturn(false);
        catalog.saveProduct(null, "Nồi cơm", new BigDecimal("1000"), 5,
                null, null, null, "", null, null, null,
                "Dung tích | 1.8 | L\nKiểu nấu | Cơ bản |", 1L);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(products).save(saved.capture());
        assertEquals(2, saved.getValue().getSpecifications().size());
        assertEquals("dung-tich", saved.getValue().getSpecifications().get(0).getSpecKey());
        assertEquals("L", saved.getValue().getSpecifications().get(0).getUnit());
        assertNull(saved.getValue().getSpecifications().get(1).getUnit());
        assertEquals(saved.getValue(), saved.getValue().getSpecifications().get(0).getProduct());
    }

    @Test
    void saveProductRejectsMalformedAndDuplicateSpecificationRows() {
        assertThrows(IllegalArgumentException.class, () -> saveWithSpecifications("Dung tích | 5"));
        assertThrows(IllegalArgumentException.class, () -> saveWithSpecifications(
                "Dung tích | 5 | L\nDung tích | 6 | L"));
    }

    private void saveWithSpecifications(String specificationText) {
        catalog.saveProduct(null, "Nồi cơm", new BigDecimal("1000"), 5,
                null, null, null, "", null, null, null, specificationText, 1L);
    }
}
