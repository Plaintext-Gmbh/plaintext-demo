package ch.plaintext.demo.tabelle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemoProductRepository extends JpaRepository<DemoProduct, Long> {

    List<DemoProduct> findByMandatAndDeletedFalseOrderBySku(String mandat);

    long countByMandat(String mandat);
}
