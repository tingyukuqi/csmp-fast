package com.csmp.supply.service.impl;

import com.csmp.common.core.exception.ServiceException;
import com.csmp.supply.domain.bo.SupplyCloudPlatformBo;
import com.csmp.supply.mapper.SupplyCloudPlatformMapper;
import com.csmp.supply.mapper.SupplyCollectConfigMapper;
import com.csmp.supply.mapper.SupplyEventSubscriptionMapper;
import com.csmp.supply.support.SupplyIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
class SupplyCloudPlatformServiceImplTest {

    private static final String TENANT_ID = "00000000000000000001";

    @Mock
    private SupplyCloudPlatformMapper cloudPlatformMapper;
    @Mock
    private SupplyCollectConfigMapper collectConfigMapper;
    @Mock
    private SupplyEventSubscriptionMapper eventSubscriptionMapper;
    @Mock
    private SupplyIdGenerator idGenerator;

    private SupplyCloudPlatformServiceImpl cloudPlatformService;

    @BeforeEach
    void setUp() {
        cloudPlatformService = spy(new SupplyCloudPlatformServiceImpl(
            cloudPlatformMapper,
            collectConfigMapper,
            eventSubscriptionMapper,
            idGenerator
        ));
        lenient().doReturn(TENANT_ID).when(cloudPlatformService).currentTenantId();
    }

    @Test
    void insertByBoShouldRejectInvalidAccessUrl() {
        SupplyCloudPlatformBo bo = new SupplyCloudPlatformBo();
        bo.setPlatformCode("PLAT-001");
        bo.setPlatformName("上海联通云");
        bo.setPlatformType("private_openstack");
        bo.setProviderCode("unicom_cloud");
        bo.setAccessUrl("collector.internal/platform");

        assertThrows(ServiceException.class, () -> cloudPlatformService.insertByBo(bo));
    }
}
