package iuh.fit.demoprj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoPrjApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
    }

    @Test
    void homeIsSeparateFromFilteredProductCatalog() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Danh mục sản phẩm"));
        assertTrue(!response.body().contains("Lọc sản phẩm"));
        assertTrue(!response.body().contains("catalog-filter"));
    }

    @Test
    void catalogFiltersSortAndPaginationRender() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        for (String path : new String[] {
                "/products?minPrice=0&maxPrice=100000000&inStockOnly=true&sort=priceAsc",
                "/products?keyword=missing-product&sort=bestSelling",
                "/products?page=1&sort=newest",
                "/products?minPrice=500&maxPrice=100&sort=priceAsc"
        }) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), path);
            assertTrue(response.body().contains("Lọc sản phẩm"), path);
        }
    }

    @Test
    void productCatalogRenders() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/products"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("HomeGear"));
        assertTrue(response.body().contains("Miễn phí vận chuyển"));
        assertTrue(response.body().contains("store-search-input"));
        assertTrue(response.body().contains("Lọc sản phẩm"));

        Matcher productLink = Pattern.compile("href=\"/products/(\\d+)\"").matcher(response.body());
        if (productLink.find()) {
            HttpRequest detailRequest = HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/products/" + productLink.group(1)))
                    .GET()
                    .build();
            HttpResponse<String> detailResponse = HttpClient.newHttpClient()
                    .send(detailRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, detailResponse.statusCode());
        }

        Matcher categoryLink = Pattern.compile("href=\"/products\\?categoryId=(\\d+)\"")
                .matcher(response.body());
        if (categoryLink.find()) {
            HttpRequest categoryRequest = HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/products?categoryId=" + categoryLink.group(1)))
                    .GET()
                    .build();
            HttpResponse<String> categoryResponse = HttpClient.newHttpClient()
                    .send(categoryRequest, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, categoryResponse.statusCode());
        }

        HttpRequest suggestionsRequest = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + "/products/suggestions?keyword=Hu"))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> suggestionsResponse = HttpClient.newHttpClient()
                .send(suggestionsRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, suggestionsResponse.statusCode());
        assertTrue(suggestionsResponse.headers().firstValue("Content-Type").orElse("")
                .contains("application/json"));
    }
}
