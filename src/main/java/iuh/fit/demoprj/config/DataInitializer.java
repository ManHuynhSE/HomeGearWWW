package iuh.fit.demoprj.config;

import iuh.fit.demoprj.model.entity.*;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.*;

import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(UserAccountRepository accounts, CategoryRepository categories,
                                ProductRepository products, PasswordEncoder encoder) {
        return args -> {
            if (accounts.count() == 0) {
                accounts.save(new UserAccount("Administrator", "admin@demoprj.local",
                        encoder.encode("Admin@123"), Role.ADMIN));
            }
            if (categories.count() == 0) {
                Category kitchen = categories.save(new Category("Thiết bị nhà bếp"));
                Category smart = categories.save(new Category("Thiết bị thông minh"));
                products.save(new Product("Nồi chiên không dầu", new BigDecimal("1890000"), 20,
                        "Nồi chiên dung tích lớn, tiết kiệm dầu.", kitchen));
                products.save(new Product("Ổ cắm thông minh Wi-Fi", new BigDecimal("350000"), 50,
                        "Điều khiển thiết bị từ xa bằng điện thoại.", smart));
            }
        };
    }
}



