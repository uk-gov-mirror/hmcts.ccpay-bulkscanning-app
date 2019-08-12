package uk.gov.hmcts.reform.bulkscanning.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;

@Repository
public interface PaymentMetadataRepository extends CrudRepository<PaymentMetadata, Integer>, JpaSpecificationExecutor<PaymentMetadata> {

    <S extends PaymentMetadata> S save(S entity);
}
