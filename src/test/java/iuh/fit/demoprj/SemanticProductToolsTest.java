package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import iuh.fit.demoprj.ai.tools.SemanticProductTools;
import iuh.fit.demoprj.ai.tools.AiToolInvocationLogger;
import iuh.fit.demoprj.model.entity.Category;
import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.service.CatalogService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

class SemanticProductToolsTest {

    @Test
    void semanticSearchFiltersVectorMetadataAndReturnsFreshDatabaseValues() {
        CatalogService catalog = Mockito.mock(CatalogService.class);
        VectorStore vectorStore = Mockito.mock(VectorStore.class);
        var tool = new SemanticProductTools(catalog, vectorStore, Mockito.mock(AiToolInvocationLogger.class));
        Document result = new Document("semantic-product",
                "Nồi chiên nấu ít dầu", Map.of("type", "product", "productId", 15L, "active", true));
        Document staleResult = new Document("stale-product",
                "Sản phẩm đã ngừng bán", Map.of("type", "product", "productId", 99L, "active", true));
        Category category = new Category("Nhà bếp");
        Product currentProduct = new Product("Nồi chiên", BigDecimal.valueOf(1_890_000), 7,
                "Nấu ít dầu", category);
        ReflectionTestUtils.setField(currentProduct, "id", 15L);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(result, staleResult));
        when(catalog.findActiveProductsByIds(List.of(15L, 99L))).thenReturn(List.of(currentProduct));

        var response = tool.searchProductsSemantic("máy nấu ăn ít dầu", null);

        assertEquals(1, response.data().size());
        assertEquals(BigDecimal.valueOf(1_890_000), response.data().get(0).price());
        assertEquals(7, response.data().get(0).stock());
        assertEquals("Nồi chiên", response.data().get(0).name());

        ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(request.capture());
        assertEquals("máy nấu ăn ít dầu", request.getValue().getQuery());
        assertEquals(30, request.getValue().getTopK());
        assertEquals(0.60, request.getValue().getSimilarityThreshold());
        assertTrue(request.getValue().hasFilterExpression());
    }

    @Test
    void maxPriceIsAppliedToCurrentCatalogPrice() {
        CatalogService catalog = Mockito.mock(CatalogService.class);
        VectorStore vectorStore = Mockito.mock(VectorStore.class);
        var tool = new SemanticProductTools(catalog, vectorStore, Mockito.mock(AiToolInvocationLogger.class));
        Document result = new Document("semantic-product",
                "Nồi chiên nấu ít dầu", Map.of("type", "product", "productId", 15L, "active", true));
        Product currentProduct = new Product("Nồi chiên", BigDecimal.valueOf(1_890_000), 7,
                "Nấu ít dầu", new Category("Nhà bếp"));
        ReflectionTestUtils.setField(currentProduct, "id", 15L);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(result));
        when(catalog.findActiveProductsByIds(List.of(15L))).thenReturn(List.of(currentProduct));

        var response = tool.searchProductsSemantic("nấu ít dầu", BigDecimal.valueOf(1_000_000));

        assertTrue(response.data().isEmpty());
        assertTrue(response.message().contains("Không tìm thấy"));
    }
}
