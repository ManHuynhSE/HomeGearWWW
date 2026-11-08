package iuh.fit.demoprj.ai.index;

import iuh.fit.demoprj.model.entity.Product;
import iuh.fit.demoprj.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "mariadb")
public class VectorIndexService {
    private static final Logger logger = LoggerFactory.getLogger(VectorIndexService.class);
    private static final int BATCH_SIZE = 50;

    private final ProductRepository products;
    private final ProductDocumentMapper productMapper;
    private final PolicyDocumentLoader policyLoader;
    private final VectorStore vectorStore;

    public VectorIndexService(ProductRepository products, ProductDocumentMapper productMapper,
                              PolicyDocumentLoader policyLoader,
                              @Qualifier("vectorStore") VectorStore vectorStore) {
        this.products = products;
        this.productMapper = productMapper;
        this.policyLoader = policyLoader;
        this.vectorStore = vectorStore;
    }

    public void reindexAll() {
        List<Document> productDocuments = products.findAllActiveForVectorIndex().stream()
                .map(productMapper::toDocument).toList();
        List<Document> policyDocuments = policyLoader.load();

        vectorStore.delete(typeFilter("product"));
        vectorStore.delete(typeFilter("policy"));
        addInBatches(productDocuments);
        addInBatches(policyDocuments);
        logger.info("AI vector reindex completed: {} active products and {} policy chunks.",
                productDocuments.size(), policyDocuments.size());
    }

    @Async("aiReindexExecutor")
    public void reindexAllAsync() {
        try {
            reindexAll();
        } catch (RuntimeException ex) {
            logger.error("AI vector reindex failed; retry it with POST /admin/ai/reindex.", ex);
            throw ex;
        }
    }

    public void indexProduct(Long productId) {
        Optional<Product> product = products.findForVectorIndexById(productId)
                .filter(Product::isActive);
        if (product.isPresent()) {
            vectorStore.delete(List.of(productMapper.documentId(productId)));
            vectorStore.add(List.of(productMapper.toDocument(product.get())));
        } else {
            removeProduct(productId);
        }
    }

    public void indexCategory(Long categoryId) {
        List<Product> productsInCategory = products.findActiveForVectorIndexByCategoryId(categoryId);
        if (productsInCategory.isEmpty()) {
            return;
        }
        vectorStore.delete(productsInCategory.stream()
                .map(product -> productMapper.documentId(product.getId())).toList());
        addInBatches(productsInCategory.stream().map(productMapper::toDocument).toList());
    }

    public void removeProduct(Long productId) {
        vectorStore.delete(List.of(productMapper.documentId(productId)));
    }

    private Filter.Expression typeFilter(String type) {
        return new FilterExpressionBuilder().eq("type", type).build();
    }

    private void addInBatches(List<Document> documents) {
        for (int start = 0; start < documents.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, documents.size());
            vectorStore.add(new ArrayList<>(documents.subList(start, end)));
        }
    }
}
