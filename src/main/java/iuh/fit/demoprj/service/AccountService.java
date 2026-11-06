package iuh.fit.demoprj.service;

import iuh.fit.demoprj.model.entity.*;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(String fullName, String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();
        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email đã được sử dụng.");
        }
        return repository.save(new UserAccount(fullName.trim(), normalizedEmail,
                passwordEncoder.encode(rawPassword), Role.CUSTOMER));
    }

    public UserAccount getByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
    }
}



