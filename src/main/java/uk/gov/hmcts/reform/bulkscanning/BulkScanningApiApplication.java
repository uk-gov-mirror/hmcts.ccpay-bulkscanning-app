package uk.gov.hmcts.reform.bulkscanning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import uk.gov.hmcts.reform.logging.spring.RequestLoggingAutoConfiguration;

@SpringBootApplication
@ComponentScan(basePackages = { "uk.gov.hmcts.reform.bulkscanning" },
    excludeFilters = { @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        value = { RequestLoggingAutoConfiguration.class }) })
public class BulkScanningApiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(BulkScanningApiApplication.class, args);
    }
}
