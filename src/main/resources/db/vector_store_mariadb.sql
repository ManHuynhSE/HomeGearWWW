CREATE TABLE IF NOT EXISTS vector_store (
    id UUID NOT NULL DEFAULT uuid() PRIMARY KEY,
    content TEXT,
    metadata JSON,
    embedding VECTOR(768) NOT NULL,
    VECTOR INDEX vector_store_embedding_idx (embedding)
) ENGINE=InnoDB;
