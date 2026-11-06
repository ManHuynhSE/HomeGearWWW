package iuh.fit.demoprj.repository;

import iuh.fit.demoprj.model.entity.*;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import iuh.fit.demoprj.model.enums.Role;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<UserAccount> findByRoleOrderByIdDesc(Role role);
}


