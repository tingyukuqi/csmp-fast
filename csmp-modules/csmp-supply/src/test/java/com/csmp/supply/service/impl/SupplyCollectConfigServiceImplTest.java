package com.csmp.supply.service.impl;

import com.csmp.common.core.exception.ServiceException;
import com.csmp.common.core.utils.SpringUtils;
import com.csmp.supply.api.domain.bo.CollectExecuteBo;
import com.csmp.supply.domain.SupplyCollectConfig;
import com.csmp.supply.domain.SupplyCollectLog;
import com.csmp.supply.mapper.SupplyCloudPlatformMapper;
import com.csmp.supply.mapper.SupplyCloudTenantMapper;
import com.csmp.supply.mapper.SupplyCollectConfigMapper;
import com.csmp.supply.mapper.SupplyCollectLogMapper;
import com.csmp.supply.support.SupplyCollectExecutor;
import com.csmp.supply.support.SupplyCollectResult;
import com.csmp.supply.support.SupplyIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
class SupplyCollectConfigServiceImplTest {

    private static final String TENANT_ID = "00000000000000000001";
    private static AnnotationConfigApplicationContext applicationContext;

    @Mock
    private SupplyCollectConfigMapper collectConfigMapper;
    @Mock
    private SupplyCollectLogMapper collectLogMapper;
    @Mock
    private SupplyCloudTenantMapper cloudTenantMapper;
    @Mock
    private SupplyCloudPlatformMapper cloudPlatformMapper;
    @Mock
    private SupplyCollectExecutor collectExecutor;
    @Mock
    private SupplyIdGenerator idGenerator;

    private SupplyCollectConfigServiceImpl collectConfigService;

    @BeforeAll
    static void initSpringContext() {
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.registerBean(SpringUtils.class);
        applicationContext.registerBean(ObjectMapper.class, () -> new ObjectMapper());
        applicationContext.refresh();
    }

    @AfterAll
    static void closeSpringContext() {
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    @BeforeEach
    void setUp() {
        collectConfigService = spy(new SupplyCollectConfigServiceImpl(
            collectConfigMapper,
            collectLogMapper,
            cloudTenantMapper,
            cloudPlatformMapper,
            collectExecutor,
            idGenerator
        ));
        doReturn(TENANT_ID).when(collectConfigService).currentTenantId();
    }

    @Test
    void executeCollectShouldRejectDisabledConfig() {
        SupplyCollectConfig config = new SupplyCollectConfig();
        config.setId(10L);
        config.setStatus("1");

        when(collectConfigMapper.selectOne(any())).thenReturn(config);

        CollectExecuteBo bo = new CollectExecuteBo();
        bo.setCollectConfigId(10L);

        assertThrows(ServiceException.class, () -> collectConfigService.executeCollect(bo));
    }

    @Test
    void executeCollectShouldCreateSuccessLogAndUpdateConfig() {
        SupplyCollectConfig config = new SupplyCollectConfig();
        config.setId(10L);
        config.setCloudPlatformId(20L);
        config.setProviderCode("unicom_cloud");
        config.setCollectScope("tenant");
        config.setConnectorCode("openstack_v3");
        config.setStatus("0");

        when(collectConfigMapper.selectOne(any())).thenReturn(config);
        when(idGenerator.nextId()).thenReturn(100L, 101L);
        when(idGenerator.nextTraceId()).thenReturn("trace-001");
        when(collectExecutor.execute(any())).thenReturn(SupplyCollectResult.success(3, 2, 5));

        CollectExecuteBo bo = new CollectExecuteBo();
        bo.setCollectConfigId(10L);
        bo.setExecuteMode("manual");
        bo.setTriggerUserId(99L);

        collectConfigService.executeCollect(bo);

        ArgumentCaptor<SupplyCollectLog> logCaptor = ArgumentCaptor.forClass(SupplyCollectLog.class);
        verify(collectLogMapper).insert(logCaptor.capture());
        assertEquals("success", logCaptor.getValue().getResultStatus());
        assertEquals(2, logCaptor.getValue().getCloudTenantCount());
        verify(collectConfigMapper).updateById(any(SupplyCollectConfig.class));
    }
}
