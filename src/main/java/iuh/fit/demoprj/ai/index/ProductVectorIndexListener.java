package iuh.fit.demoprj.ai.index;

import iuh.fit.demoprj.service.ProductCatalogChangedEvent;
import iuh.fit.demoprj.service.CategoryCatalogChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "mariadb")
public class ProductVectorIndexListener {
    private static final Logger logger = LoggerFactory.getLogger(ProductVectorIndexListener.class);

    private final VectorIndexService vectorIndex;

    public ProductVectorIndexListener(VectorIndexService vectorIndex) {
        this.vectorIndex = vectorIndex;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductChanged(ProductCatalogChangedEvent event) {
        try {
            vectorIndex.indexProduct(event.productId());
        } catch (RuntimeException ex) {
            logger.error("Could not sync product {} to the AI vector index; retry with POST /admin/ai/reindex.",
                    event.productId(), ex);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryChanged(CategoryCatalogChangedEvent event) {
        try {
            vectorIndex.indexCategory(event.categoryId());
        } catch (RuntimeException ex) {
            logger.error("Could not sync products in category {} to the AI vector index; retry with POST /admin/ai/reindex.",
                    event.categoryId(), ex);
        }
    }
}
