# HomeGear

## AI vector index

The dev and production profiles use Google's `gemini-embedding-001` embedding model with 768 dimensions and the MariaDB Vector Store table `vector_store`. Dev initializes the vector schema; validation is disabled there because Spring AI validates before attempting initialization. Before starting with the `prod` profile, apply `src/main/resources/db/product_active.sql` and `src/main/resources/db/vector_store_mariadb.sql`.

Changing the embedding model or its dimensions requires creating a compatible vector table and reindexing all product and policy documents. Use the admin-only `POST /admin/ai/reindex` endpoint after deployment or policy changes. Product and category updates are synchronized after the database transaction commits. Product documents use a stable `product-{id}` key in metadata; MariaDB Vector Store requires physical document IDs to be UUIDs, so those IDs are deterministic UUIDs derived from the key. The vector table uses MariaDB's native vector index; no stored routines, functions, or check constraints are added.

Store chat retrieves up to four policy chunks on every turn using a 0.65 similarity threshold. This improves policy grounding but adds an embedding/search operation per turn and can add irrelevant context; if that trade-off proves costly or noisy, replace the always-on advisor with an on-demand `searchPolicy` tool.

The opt-in live semantic-search and policy-RAG smoke test is documented in `AI_INTEGRATION_PLAN.md`. It reindexes the local catalog and calls the configured Google AI provider, so it consumes API quota.

## AI chat memory

Chat history is kept in process, with a window of up to 20 messages. Store conversations are scoped to the signed-in customer or guest session; the admin assistant uses a separate namespace. The "Cuộc trò chuyện mới" action clears server-side memory before clearing the visible transcript. Guest history does not continue after login, and all in-memory history is lost when the application restarts.

## AI security controls

Chat endpoints require CSRF tokens and are limited to 10 requests per minute per authenticated account or guest IP, returning `429` with `Retry-After` when exceeded. The limiter is per application process; deployments with multiple instances should use a shared rate-limit store. Chat input is limited to 500 characters, provider requests time out after 30 seconds, tool calls are capped, and model output tokens are bounded per assistant. Tool audit logs record the tool name and account ID without request content. Configure provider credentials through `AI_API_KEY`; local `.env` files are ignored by Git.
