package com.csmp.supply.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csmp.common.core.exception.ServiceException;
import com.csmp.common.core.utils.StringUtils;
import com.csmp.common.mybatis.core.page.PageQuery;
import com.csmp.common.mybatis.core.page.TableDataInfo;
import com.csmp.supply.domain.SupplyCloudPlatform;
import com.csmp.supply.domain.SupplyPhysicalResource;
import com.csmp.supply.domain.SupplySupplier;
import com.csmp.supply.domain.SupplySupplierPlatformAccount;
import com.csmp.supply.domain.SupplySupplierUser;
import com.csmp.supply.domain.bo.SupplySupplierBo;
import com.csmp.supply.domain.bo.SupplySupplierPlatformAccountBo;
import com.csmp.supply.domain.bo.SupplySupplierUserBindBo;
import com.csmp.supply.domain.enums.EnableStatusEnum;
import com.csmp.supply.domain.vo.SupplyOptionVo;
import com.csmp.supply.domain.vo.SupplySupplierPlatformAccountVo;
import com.csmp.supply.domain.vo.SupplySupplierVo;
import com.csmp.supply.mapper.SupplyCloudPlatformMapper;
import com.csmp.supply.mapper.SupplyPhysicalResourceMapper;
import com.csmp.supply.mapper.SupplySupplierMapper;
import com.csmp.supply.mapper.SupplySupplierPlatformAccountMapper;
import com.csmp.supply.mapper.SupplySupplierUserMapper;
import com.csmp.supply.service.ISupplySupplierService;
import com.csmp.supply.support.SupplyIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 供应商服务实现
 *
 * @author csmp
 */
@Service
@RequiredArgsConstructor
public class SupplySupplierServiceImpl extends AbstractSupplyService implements ISupplySupplierService {

    private final SupplySupplierMapper supplierMapper;
    private final SupplySupplierPlatformAccountMapper supplierPlatformAccountMapper;
    private final SupplySupplierUserMapper supplierUserMapper;
    private final SupplyPhysicalResourceMapper physicalResourceMapper;
    private final SupplyCloudPlatformMapper cloudPlatformMapper;
    private final SupplyIdGenerator idGenerator;

    @Override
    public TableDataInfo<SupplySupplierVo> queryPageList(SupplySupplierBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SupplySupplier> lqw = Wrappers.lambdaQuery();
        lqw.eq(SupplySupplier::getTenantId, currentTenantId());
        lqw.eq(StringUtils.isNotBlank(bo.getSupplierCode()), SupplySupplier::getSupplierCode, bo.getSupplierCode());
        lqw.like(StringUtils.isNotBlank(bo.getSupplierName()), SupplySupplier::getSupplierName, bo.getSupplierName());
        lqw.eq(StringUtils.isNotBlank(bo.getCreditCode()), SupplySupplier::getCreditCode, bo.getCreditCode());
        lqw.eq(StringUtils.isNotBlank(bo.getCooperationType()), SupplySupplier::getCooperationType, bo.getCooperationType());
        lqw.eq(StringUtils.isNotBlank(bo.getSupplierType()), SupplySupplier::getSupplierType, bo.getSupplierType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SupplySupplier::getStatus, bo.getStatus());
        lqw.orderByDesc(SupplySupplier::getCreateTime);
        Page<SupplySupplier> page = supplierMapper.selectPage(pageQuery.build(), lqw);
        List<SupplySupplierVo> list = page.getRecords().stream().map(this::toSupplierVo).toList();
        return new TableDataInfo<>(list, page.getTotal());
    }

    @Override
    public SupplySupplierVo queryById(Long supplierId) {
        SupplySupplier supplier = getSupplierOrThrow(supplierId);
        return toSupplierVo(supplier);
    }

    @Override
    public boolean insertByBo(SupplySupplierBo bo) {
        validateWriteRules(bo);
        validateSupplierUnique(bo);
        SupplySupplier entity = new SupplySupplier();
        BeanUtil.copyProperties(bo, entity);
        entity.setCreditCode(StringUtils.trimToNull(bo.getCreditCode()));
        entity.setId(idGenerator.nextId());
        entity.setTenantId(currentTenantId());
        entity.setStatus(StringUtils.defaultIfBlank(bo.getStatus(), EnableStatusEnum.ENABLE.getCode()));
        return supplierMapper.insert(entity) > 0;
    }

    @Override
    public boolean updateByBo(SupplySupplierBo bo) {
        SupplySupplier entity = getSupplierOrThrow(bo.getSupplierId());
        validateWriteRules(bo);
        validateSupplierUnique(bo);
        entity.setSupplierCode(bo.getSupplierCode());
        entity.setSupplierName(bo.getSupplierName());
        entity.setSupplierShortName(bo.getSupplierShortName());
        entity.setSupplierType(bo.getSupplierType());
        entity.setCreditCode(StringUtils.trimToNull(bo.getCreditCode()));
        entity.setServiceScope(bo.getServiceScope());
        entity.setContactName(bo.getContactName());
        entity.setContactPhone(bo.getContactPhone());
        entity.setContactEmail(bo.getContactEmail());
        entity.setAddress(bo.getAddress());
        entity.setCooperationType(bo.getCooperationType());
        entity.setStatus(StringUtils.defaultIfBlank(bo.getStatus(), entity.getStatus()));
        entity.setOnboardTime(bo.getOnboardTime());
        entity.setOffboardTime(bo.getOffboardTime());
        entity.setRemark(bo.getRemark());
        return supplierMapper.updateById(entity) > 0;
    }

    @Override
    public boolean changeStatus(Long supplierId, String status) {
        SupplySupplier entity = getSupplierOrThrow(supplierId);
        entity.setStatus(status);
        return supplierMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        for (Long id : ids) {
            SupplySupplier supplier = getSupplierOrThrow(id);
            if (Boolean.TRUE.equals(isValid)) {
                boolean hasAccount = supplierPlatformAccountMapper.exists(Wrappers.<SupplySupplierPlatformAccount>lambdaQuery()
                    .eq(SupplySupplierPlatformAccount::getTenantId, currentTenantId())
                    .eq(SupplySupplierPlatformAccount::getSupplierId, id));
                if (hasAccount) {
                    throw new ServiceException("供应商 {} 已关联平台账号，不能删除", supplier.getSupplierName());
                }
                boolean hasResource = physicalResourceMapper.exists(Wrappers.<SupplyPhysicalResource>lambdaQuery()
                    .eq(SupplyPhysicalResource::getTenantId, currentTenantId())
                    .eq(SupplyPhysicalResource::getSupplierId, id));
                if (hasResource) {
                    throw new ServiceException("供应商 {} 已关联物理资源，不能删除", supplier.getSupplierName());
                }
            }
        }
        return supplierMapper.deleteByIds(ids) > 0;
    }

    @Override
    public List<SupplyOptionVo> queryOptions(String status) {
        List<SupplySupplier> list = supplierMapper.selectList(Wrappers.<SupplySupplier>lambdaQuery()
            .eq(SupplySupplier::getTenantId, currentTenantId())
            .eq(StringUtils.isNotBlank(status), SupplySupplier::getStatus, status)
            .orderByAsc(SupplySupplier::getSupplierName));
        return list.stream().map(item -> {
            SupplyOptionVo vo = new SupplyOptionVo();
            vo.setLabel(item.getSupplierName());
            vo.setValue(item.getId());
            vo.setExtra(Map.of("supplierCode", item.getSupplierCode(), "status", item.getStatus()));
            return vo;
        }).toList();
    }

    @Override
    public TableDataInfo<SupplySupplierPlatformAccountVo> queryPlatformAccountPage(Long supplierId, SupplySupplierPlatformAccountBo bo, PageQuery pageQuery) {
        getSupplierOrThrow(supplierId);
        LambdaQueryWrapper<SupplySupplierPlatformAccount> lqw = Wrappers.lambdaQuery();
        lqw.eq(SupplySupplierPlatformAccount::getTenantId, currentTenantId());
        lqw.eq(SupplySupplierPlatformAccount::getSupplierId, supplierId);
        lqw.eq(Objects.nonNull(bo.getCloudPlatformId()), SupplySupplierPlatformAccount::getCloudPlatformId, bo.getCloudPlatformId());
        lqw.like(StringUtils.isNotBlank(bo.getAccountName()), SupplySupplierPlatformAccount::getAccountName, bo.getAccountName());
        lqw.eq(StringUtils.isNotBlank(bo.getAccountStatus()), SupplySupplierPlatformAccount::getAccountStatus, bo.getAccountStatus());
        lqw.orderByDesc(SupplySupplierPlatformAccount::getCreateTime);
        Page<SupplySupplierPlatformAccount> page = supplierPlatformAccountMapper.selectPage(pageQuery.build(), lqw);
        List<Long> platformIds = page.getRecords().stream()
            .map(SupplySupplierPlatformAccount::getCloudPlatformId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, String> platformNameMap = platformIds.isEmpty() ? Collections.emptyMap() :
            cloudPlatformMapper.selectBatchIds(platformIds).stream().collect(Collectors.toMap(SupplyCloudPlatform::getId, SupplyCloudPlatform::getPlatformName));
        List<SupplySupplierPlatformAccountVo> list = page.getRecords().stream().map(item -> toPlatformAccountVo(item, platformNameMap)).toList();
        return new TableDataInfo<>(list, page.getTotal());
    }

    @Override
    public boolean insertPlatformAccount(SupplySupplierPlatformAccountBo bo) {
        getSupplierOrThrow(bo.getSupplierId());
        validatePlatformAccountUnique(bo);
        SupplySupplierPlatformAccount entity = new SupplySupplierPlatformAccount();
        BeanUtil.copyProperties(bo, entity);
        entity.setId(idGenerator.nextId());
        entity.setTenantId(currentTenantId());
        entity.setAccountStatus(StringUtils.defaultIfBlank(bo.getAccountStatus(), EnableStatusEnum.ENABLE.getCode()));
        return supplierPlatformAccountMapper.insert(entity) > 0;
    }

    @Override
    public boolean updatePlatformAccount(SupplySupplierPlatformAccountBo bo) {
        SupplySupplierPlatformAccount entity = getPlatformAccountOrThrow(bo.getAccountId());
        getSupplierOrThrow(bo.getSupplierId());
        validatePlatformAccountUnique(bo);
        entity.setSupplierId(bo.getSupplierId());
        entity.setCloudPlatformId(bo.getCloudPlatformId());
        entity.setAccountName(bo.getAccountName());
        entity.setAccountType(bo.getAccountType());
        entity.setAccountIdentifier(bo.getAccountIdentifier());
        entity.setAccountStatus(StringUtils.defaultIfBlank(bo.getAccountStatus(), entity.getAccountStatus()));
        entity.setRemark(bo.getRemark());
        return supplierPlatformAccountMapper.updateById(entity) > 0;
    }

    @Override
    public boolean deletePlatformAccounts(Collection<Long> ids) {
        return supplierPlatformAccountMapper.deleteByIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUsers(Long supplierId, SupplySupplierUserBindBo bo) {
        getSupplierOrThrow(supplierId);
        supplierUserMapper.delete(Wrappers.<SupplySupplierUser>lambdaQuery()
            .eq(SupplySupplierUser::getTenantId, currentTenantId())
            .eq(SupplySupplierUser::getSupplierId, supplierId));
        for (Long userId : bo.getUserIds()) {
            SupplySupplierUser bind = new SupplySupplierUser();
            bind.setId(idGenerator.nextId());
            bind.setTenantId(currentTenantId());
            bind.setSupplierId(supplierId);
            bind.setUserId(userId);
            supplierUserMapper.insert(bind);
        }
        return true;
    }

    private void validateWriteRules(SupplySupplierBo bo) {
        validateCreditCode(bo.getCreditCode());
    }

    private void validateSupplierUnique(SupplySupplierBo bo) {
        SupplySupplier duplicatedCode = supplierMapper.selectOne(Wrappers.<SupplySupplier>lambdaQuery()
            .eq(SupplySupplier::getTenantId, currentTenantId())
            .eq(SupplySupplier::getSupplierCode, bo.getSupplierCode())
            .ne(Objects.nonNull(bo.getSupplierId()), SupplySupplier::getId, bo.getSupplierId()));
        if (duplicatedCode != null) {
            throw new ServiceException("供应商编码已存在");
        }
        SupplySupplier duplicatedName = supplierMapper.selectOne(Wrappers.<SupplySupplier>lambdaQuery()
            .eq(SupplySupplier::getTenantId, currentTenantId())
            .eq(SupplySupplier::getSupplierName, bo.getSupplierName())
            .ne(Objects.nonNull(bo.getSupplierId()), SupplySupplier::getId, bo.getSupplierId()));
        if (duplicatedName != null) {
            throw new ServiceException("供应商名称已存在");
        }
    }

    private void validatePlatformAccountUnique(SupplySupplierPlatformAccountBo bo) {
        boolean exist = supplierPlatformAccountMapper.exists(Wrappers.<SupplySupplierPlatformAccount>lambdaQuery()
            .eq(SupplySupplierPlatformAccount::getTenantId, currentTenantId())
            .eq(SupplySupplierPlatformAccount::getAccountIdentifier, bo.getAccountIdentifier())
            .ne(Objects.nonNull(bo.getAccountId()), SupplySupplierPlatformAccount::getId, bo.getAccountId()));
        if (exist) {
            throw new ServiceException("账号标识已存在");
        }
    }

    private SupplySupplier getSupplierOrThrow(Long supplierId) {
        SupplySupplier supplier = supplierMapper.selectOne(Wrappers.<SupplySupplier>lambdaQuery()
            .eq(SupplySupplier::getTenantId, currentTenantId())
            .eq(SupplySupplier::getId, supplierId));
        if (supplier == null) {
            throw new ServiceException("供应商不存在");
        }
        return supplier;
    }

    private SupplySupplierPlatformAccount getPlatformAccountOrThrow(Long accountId) {
        SupplySupplierPlatformAccount account = supplierPlatformAccountMapper.selectOne(Wrappers.<SupplySupplierPlatformAccount>lambdaQuery()
            .eq(SupplySupplierPlatformAccount::getTenantId, currentTenantId())
            .eq(SupplySupplierPlatformAccount::getId, accountId));
        if (account == null) {
            throw new ServiceException("平台账号不存在");
        }
        return account;
    }

    private SupplySupplierVo toSupplierVo(SupplySupplier entity) {
        SupplySupplierVo vo = new SupplySupplierVo();
        BeanUtil.copyProperties(entity, vo);
        vo.setSupplierId(entity.getId());
        vo.setPlatformCount(supplierPlatformAccountMapper.selectCount(Wrappers.<SupplySupplierPlatformAccount>lambdaQuery()
            .eq(SupplySupplierPlatformAccount::getTenantId, entity.getTenantId())
            .eq(SupplySupplierPlatformAccount::getSupplierId, entity.getId())).intValue());
        vo.setAccountCount(vo.getPlatformCount());
        return vo;
    }

    private SupplySupplierPlatformAccountVo toPlatformAccountVo(SupplySupplierPlatformAccount entity, Map<Long, String> platformNameMap) {
        SupplySupplierPlatformAccountVo vo = new SupplySupplierPlatformAccountVo();
        BeanUtil.copyProperties(entity, vo);
        vo.setAccountId(entity.getId());
        vo.setCloudPlatformName(platformNameMap.get(entity.getCloudPlatformId()));
        return vo;
    }
}
