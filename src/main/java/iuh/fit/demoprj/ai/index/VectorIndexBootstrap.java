package iuh.fit.demoprj.ai.index;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "mariadb")
public class VectorIndexBootstrap implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(VectorIndexBootstrap.class);

    private final JdbcTemplate jdbcTemplate;
    private final VectorIndexService vectorIndex;
    private final String tableName;

    public VectorIndexBootstrap(
            JdbcTemplate jdbcTemplate,
            VectorIndexService vectorIndex,
            @Value("${spring.ai.vectorstore.mariadb.table-name:vector_store}") String tableName) {
        if (tableName == null || !tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid AI vector table name.");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.vectorIndex = vectorIndex;
        this.tableName = tableName;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT 1 FROM " + tableName + " LIMIT 1");
        if (!rows.isEmpty()) {
            logger.info("AI vector table '{}' already contains data; skipping startup ETL.", tableName);
            return;
        }
        logger.info("AI vector table '{}' is empty; starting initial ETL.", tableName);
        vectorIndex.reindexAllAsync();
    }
}
