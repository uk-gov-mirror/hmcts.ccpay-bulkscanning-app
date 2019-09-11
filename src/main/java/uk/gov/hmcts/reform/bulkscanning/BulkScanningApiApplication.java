package uk.gov.hmcts.reform.bulkscanning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@ComponentScan(basePackages = { "uk.gov.hmcts.reform.bulkscanning" },
//    excludeFilters = { @ComponentScan.Filter(
//        type = FilterType.ASSIGNABLE_TYPE,
//        value = { RequestLoggingAutoConfiguration.class }) })
public class BulkScanningApiApplication {

    public static void main(final String[] args) {
        SpringApplication.run(BulkScanningApiApplication.class, args);
    }
}
