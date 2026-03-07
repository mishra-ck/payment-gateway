package payment.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import payment.gateway.domain.model.Account;

import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

}
