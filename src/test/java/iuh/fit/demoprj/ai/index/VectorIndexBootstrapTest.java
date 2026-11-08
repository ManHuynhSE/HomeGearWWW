package iuh.fit.demoprj.ai.index;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

class VectorIndexBootstrapTest {
    private static final String EMPTY_TABLE_QUERY = "SELECT 1 FROM vector_store LIMIT 1";

    @Test
    void startsInitialEtlWhenVectorTableIsEmpty() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        VectorIndexService vectorIndex = mock(VectorIndexService.class);
        when(jdbcTemplate.queryForList(EMPTY_TABLE_QUERY)).thenReturn(List.of());
        VectorIndexBootstrap bootstrap = new VectorIndexBootstrap(jdbcTemplate, vectorIndex, "vector_store");

        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(vectorIndex).reindexAllAsync();
    }

    @Test
    void skipsInitialEtlWhenVectorTableAlreadyContainsData() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        VectorIndexService vectorIndex = mock(VectorIndexService.class);
        when(jdbcTemplate.queryForList(EMPTY_TABLE_QUERY)).thenReturn(List.of(Map.of("1", 1)));
        VectorIndexBootstrap bootstrap = new VectorIndexBootstrap(jdbcTemplate, vectorIndex, "vector_store");

        bootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(vectorIndex, org.mockito.Mockito.never()).reindexAllAsync();
    }
}
