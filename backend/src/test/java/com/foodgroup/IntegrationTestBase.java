package com.foodgroup;

import com.foodgroup.common.config.TestDynamoConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDynamoConfig.class)
public abstract class IntegrationTestBase {
}
