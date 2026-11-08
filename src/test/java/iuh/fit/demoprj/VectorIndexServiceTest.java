package iuh.fit.demoprj;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.ai.index.PolicyDocumentLoader;
import iuh.fit.demoprj.ai.index.ProductDocumentMapper;
import iuh.fit.demoprj.ai.index.VectorIndexService;
import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

class VectorIndexServiceTest {

    @Test
    void indexesActiveProductsAndRemovesInactiveProductsByStableId() {
        ProductRepository products = org.mockito.Mockito.mock(ProductRepository.class);
        PolicyDocumentLoader policyLoader = org.mockito.Mockito.mock(PolicyDocumentLoader.class);
        VectorStore vectorStore = org.mockito.Mockito.mock(VectorStore.class);
        var mapper = new ProductDocumentMapper();
        var index = new VectorIndexService(products, mapper, policyLoader, vectorStore);
        Category category = new Category("Nhà bếp");
        ReflectionTestUtils.setField(category, "id", 3L);
        Product product = new Product("Nồi chiên", BigDecimal.valueOf(1_000_000), 2,
                "Nấu ít dầu", category);
        ReflectionTestUtils.setField(product, "id", 9L);

        when(products.findForVectorIndexById(9L)).thenReturn(Optional.of(product));
        index.indexProduct(9L);

        verify(vectorStore).delete(List.of(mapper.documentId(9L)));
        verify(vectorStore).add(any(List.class));

        product.setActive(false);
        index.indexProduct(9L);
        verify(vectorStore, org.mockito.Mockito.times(2)).delete(List.of(mapper.documentId(9L)));
    }

    @Test
    void fullReindexDeletesOldKindsThenAddsProductsAndPolicies() {
        ProductRepository products = org.mockito.Mockito.mock(ProductRepository.class);
        PolicyDocumentLoader policyLoader = org.mockito.Mockito.mock(PolicyDocumentLoader.class);
        VectorStore vectorStore = org.mockito.Mockito.mock(VectorStore.class);
        var index = new VectorIndexService(products, new ProductDocumentMapper(), policyLoader, vectorStore);
        Document policy = new Document("Policy text");
        when(products.findAllActiveForVectorIndex()).thenReturn(List.of());
        when(policyLoader.load()).thenReturn(List.of(policy));

        index.reindexAll();

        verify(vectorStore, org.mockito.Mockito.times(2))
                .delete(any(org.springframework.ai.vectorstore.filter.Filter.Expression.class));
        verify(vectorStore).add(List.of(policy));
    }
}
