package com.csmp.supply.service.impl;

import com.csmp.common.core.exception.ServiceException;
import com.csmp.supply.domain.SupplySupplier;
import com.csmp.supply.domain.bo.SupplySupplierBo;
import org.junit.jupiter.api.Assertions;
import com.csmp.supply.mapper.SupplyCloudPlatformMapper;
import com.csmp.supply.mapper.SupplyPhysicalResourceMapper;
import com.csmp.supply.mapper.SupplySupplierMapper;
import com.csmp.supply.mapper.SupplySupplierPlatformAccountMapper;
import com.csmp.supply.mapper.SupplySupplierUserMapper;
import com.csmp.supply.support.SupplyIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
class SupplySupplierServiceImplTest {

    private static final String TENANT_ID = "00000000000000000001";

    @Mock
    private SupplySupplierMapper supplierMapper;
    @Mock
    private SupplySupplierPlatformAccountMapper supplierPlatformAccountMapper;
    @Mock
    private SupplySupplierUserMapper supplierUserMapper;
    @Mock
    private SupplyPhysicalResourceMapper physicalResourceMapper;
    @Mock
    private SupplyCloudPlatformMapper cloudPlatformMapper;
    @Mock
    private SupplyIdGenerator idGenerator;

    private SupplySupplierServiceImpl supplierService;

    @BeforeEach
    void setUp() {
        supplierService = spy(new SupplySupplierServiceImpl(
            supplierMapper,
            supplierPlatformAccountMapper,
            supplierUserMapper,
            physicalResourceMapper,
            cloudPlatformMapper,
            idGenerator
        ));
        lenient().doReturn(TENANT_ID).when(supplierService).currentTenantId();
    }

    @Test
    void insertByBoShouldRejectDuplicateSupplierCode() {
        SupplySupplierBo bo = new SupplySupplierBo();
        bo.setSupplierCode("SUP-001");
        bo.setSupplierName("联通云");

        when(supplierMapper.selectOne(any())).thenReturn(new SupplySupplier());

        assertThrows(ServiceException.class, () -> supplierService.insertByBo(bo));
    }

    @Test
    void deleteWithValidByIdsShouldRejectReferencedSupplier() {
        SupplySupplier supplier = new SupplySupplier();
        supplier.setId(1L);
        supplier.setSupplierName("联通云");

        when(supplierMapper.selectOne(any())).thenReturn(supplier);
        when(supplierPlatformAccountMapper.exists(any())).thenReturn(true);

        assertThrows(ServiceException.class, () -> supplierService.deleteWithValidByIds(java.util.List.of(1L), true));
    }

    @Test
    void insertByBoShouldRejectInvalidCreditCode() {
        SupplySupplierBo bo = new SupplySupplierBo();
        bo.setSupplierCode("SUP-002");
        bo.setSupplierName("联通云");
        bo.setSupplierType("cloud_provider");
        bo.setCreditCode("invalid-credit-code");

        assertThrows(ServiceException.class, () -> supplierService.insertByBo(bo));
    }

    @Test
    void insertByBoShouldPersistNullWhenCreditCodeIsBlank() {
        SupplySupplierBo bo = new SupplySupplierBo();
        bo.setSupplierCode("SUP-003");
        bo.setSupplierName("联通云");
        bo.setSupplierType("cloud_provider");
        bo.setCreditCode("  ");

        when(idGenerator.nextId()).thenReturn(100L);
        when(supplierMapper.insert(any(SupplySupplier.class))).thenReturn(1);

        supplierService.insertByBo(bo);

        ArgumentCaptor<SupplySupplier> captor = ArgumentCaptor.forClass(SupplySupplier.class);
        verify(supplierMapper).insert(captor.capture());
        Assertions.assertNull(captor.getValue().getCreditCode());
    }

    @Test
    void insertByBoShouldAllowDuplicateCreditCode() {
        SupplySupplierBo bo = new SupplySupplierBo();
        bo.setSupplierCode("SUP-004");
        bo.setSupplierName("联通云二号");
        bo.setSupplierType("cloud_provider");
        bo.setCreditCode("91310000MA1ABCDE1X");

        when(supplierMapper.selectOne(any())).thenReturn(null, null);
        when(idGenerator.nextId()).thenReturn(101L);
        when(supplierMapper.insert(any(SupplySupplier.class))).thenReturn(1);

        supplierService.insertByBo(bo);

        verify(supplierMapper).insert(any(SupplySupplier.class));
    }

    @Test
    void updateByBoShouldPersistNullWhenCreditCodeIsBlank() {
        SupplySupplier existing = new SupplySupplier();
        existing.setId(1L);
        existing.setSupplierCode("SUP-001");
        existing.setSupplierName("联通云");
        existing.setSupplierType("cloud_provider");
        existing.setCreditCode("91310000MA1ABCDE1X");

        SupplySupplierBo bo = new SupplySupplierBo();
        bo.setSupplierId(1L);
        bo.setSupplierCode("SUP-001");
        bo.setSupplierName("联通云");
        bo.setSupplierType("cloud_provider");
        bo.setCreditCode(" ");

        when(supplierMapper.selectOne(any())).thenReturn(existing, null, null);
        when(supplierMapper.updateById(any(SupplySupplier.class))).thenReturn(1);

        supplierService.updateByBo(bo);

        ArgumentCaptor<SupplySupplier> captor = ArgumentCaptor.forClass(SupplySupplier.class);
        verify(supplierMapper).updateById(captor.capture());
        Assertions.assertNull(captor.getValue().getCreditCode());
    }
}
