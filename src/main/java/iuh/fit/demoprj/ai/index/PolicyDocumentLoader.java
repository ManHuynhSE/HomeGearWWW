package iuh.fit.demoprj.ai.index;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class PolicyDocumentLoader {
    private static final Pattern HEADING = Pattern.compile("(?m)^#{1,6}\\s+(.+)$");
    private static final int OVERLAP_WORDS = 70;

    private final ResourcePatternResolver resourceResolver;
    private final Map<String, String> replacements;
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(400)
            .withMinChunkSizeChars(1)
            .withMinChunkLengthToEmbed(1)
            .withMaxNumChunks(1000)
            .build();

    public PolicyDocumentLoader(
            ApplicationContext applicationContext,
            @Value("${homegear.shipping.free-threshold:500000}") BigDecimal freeShippingThreshold,
            @Value("${homegear.shipping.flat-fee:30000}") BigDecimal flatShippingFee,
            @Value("${homegear.store.payment.cod-enabled:false}") boolean codEnabled,
            @Value("${homegear.store.payment.bank-transfer-enabled:false}") boolean bankTransferEnabled,
            @Value("${homegear.store.payment.vnpay-enabled:false}") boolean vnpayEnabled,
            @Value("${homegear.store.payment.momo-enabled:false}") boolean momoEnabled,
            @Value("${homegear.store.contact.hotline:}") String hotline,
            @Value("${homegear.store.contact.email:}") String email) {
        this.resourceResolver = applicationContext;
        this.replacements = Map.of(
                "{{shipping.free-threshold}}", formatMoney(freeShippingThreshold),
                "{{shipping.flat-fee}}", formatMoney(flatShippingFee),
                "{{payment.cod}}", paymentStatus(codEnabled),
                "{{payment.bank-transfer}}", paymentStatus(bankTransferEnabled),
                "{{payment.vnpay}}", paymentStatus(vnpayEnabled),
                "{{payment.momo}}", paymentStatus(momoEnabled),
                "{{store.hotline}}", configuredValue(hotline),
                "{{store.email}}", configuredValue(email));
    }

    public List<Document> load() {
        try {
            Resource[] resources = resourceResolver.getResources("classpath:/ai/policies/*.md");
            Arrays.sort(resources, Comparator.comparing(resource -> resource.getFilename() == null
                    ? "" : resource.getFilename()));
            List<Document> documents = new ArrayList<>();
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    throw new IllegalStateException("Policy resource has no filename: " + resource);
                }
                String content;
                try (var input = resource.getInputStream()) {
                    content = StreamUtils.copyToString(input, StandardCharsets.UTF_8);
                }
                documents.addAll(splitPolicy(filename, applyReplacements(content)));
            }
            return documents;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load AI policy documents.", ex);
        }
    }

    private List<Document> splitPolicy(String filename, String markdown) {
        String[] sections = markdown.split("(?m)(?=^#{1,6}\\s+)", -1);
        List<Document> documents = new ArrayList<>();
        for (String section : sections) {
            String text = section.trim();
            if (text.isEmpty()) {
                continue;
            }
            Matcher heading = HEADING.matcher(text);
            String title = heading.find() ? heading.group(1).trim() : filename;
            String sectionKey = slug(title);
            List<Document> chunks = splitter.split(new Document(text));
            String previousChunk = "";
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i).getText();
                String overlap = i == 0 ? "" : trailingWords(previousChunk, OVERLAP_WORDS);
                String chunkText = overlap.isEmpty() ? chunk : overlap + "\n" + chunk;
                String recordId = "policy-" + filename + "-" + sectionKey + "-" + i;
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("type", "policy");
                metadata.put("source", filename);
                metadata.put("section", title);
                metadata.put("chunkIndex", i);
                metadata.put("recordId", recordId);
                documents.add(new Document(stableId(recordId), chunkText, metadata));
                previousChunk = chunk;
            }
        }
        return documents;
    }

    private String applyReplacements(String content) {
        String result = content;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            result = result.replace(replacement.getKey(), replacement.getValue());
        }
        return result;
    }

    private String trailingWords(String text, int wordCount) {
        String[] words = text.trim().split("\\s+");
        int start = Math.max(0, words.length - wordCount);
        return String.join(" ", Arrays.copyOfRange(words, start, words.length));
    }

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String stableId(String recordId) {
        return UUID.nameUUIDFromBytes(recordId.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String formatMoney(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " ₫";
    }

    private static String paymentStatus(boolean enabled) {
        return enabled ? "đang được hỗ trợ" : "hiện chưa được bật";
    }

    private static String configuredValue(String value) {
        return value == null || value.isBlank() ? "chưa được cấu hình" : value.trim();
    }
}
