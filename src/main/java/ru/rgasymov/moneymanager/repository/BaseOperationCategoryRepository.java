package ru.rgasymov.moneymanager.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import ru.rgasymov.moneymanager.domain.entity.BaseOperationCategory;

@NoRepositoryBean
public interface BaseOperationCategoryRepository<T extends BaseOperationCategory>
    extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

  Optional<T> findByIdAndAccountId(Long id, Long accountId);

  void deleteByIdAndAccountId(Long id, Long accountId);

  void deleteAllByAccountId(Long accountId);

  boolean existsByAccountId(Long accountId);

  boolean existsByNameIgnoreCaseAndAccountId(String name, Long accountId);
}
