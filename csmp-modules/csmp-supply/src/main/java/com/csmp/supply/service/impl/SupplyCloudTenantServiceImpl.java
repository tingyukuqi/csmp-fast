package com.csmp.supply.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csmp.common.core.utils.StringUtils;
import com.csmp.common.mybatis.core.page.PageQuery;
import com.csmp.common.mybatis.core.page.TableDataInfo;
import com.csmp.supply.api.domain.bo.CollectExecuteBo;
import com.csmp.supply.api.domain.vo.CollectExecuteResultVo;
import com.csmp.supply.domain.SupplyCloudPlatform;
import com.csmp.supply.domain.SupplyCloudTenant;
import com.csmp.supply.domain.SupplyCollectConfig;
import com.csmp.supply.domain.SupplyOrgCloudTenantBind;
import com.csmp.supply.domain.bo.SupplyCloudTenantBo;
import com.csmp.supply.domain.vo.SupplyCloudTenantVo;
import com.csmp.supply.domain.vo.SupplyOptionVo;
import com.csmp.supply.mapper.SupplyCloudPlatformMapper;
import com.csmp.supply.mapper.SupplyCloudTenantMapper;
import com.csmp.supply.mapper.SupplyCollectConfigMapper;
import com.csmp.supply.mapper.SupplyOrgCloudTenantBindMapper;
import com.csmp.supply.service.ISupplyCloudTenantService;
import com.csmp.supply.service.ISupplyCollectConfigService;
import com.csmp.system.api.RemoteDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 云租户服务实现
 *
 * @author csmp
 */
@Service
@RequiredArgsConstructor
public class SupplyCloudTenantServiceImpl extends AbstractSupplyService implements ISupplyCloudTenantService {

    private final SupplyCloudTenantMapper cloudTenantMapper;
    private final SupplyCloudPlatformMapper cloudPlatformMapper;
    private final SupplyOrgCloudTenantBindMapper bindMapper;
    private final SupplyCollectConfigMapper collectConfigMapper;
    private final ISupplyCollectConfigService collectConfigService;
    private final RemoteDeptService remoteDeptService;

    @Override
    public TableDataInfo<SupplyCloudTenantVo> queryPageList(SupplyCloudTenantBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SupplyCloudTenant> lqw = Wrappers.lambdaQuery();
        lqw.eq(SupplyCloudTenant::getTenantId, currentTenantId());
        lqw.eq(Objects.nonNull(bo.getCloudPlatformId()), SupplyCloudTenant::getCloudPlatformId, bo.getCloudPlatformId());
        lqw.eq(StringUtils.isNotBlank(bo.getTenantStatus()), SupplyCloudTenant::getTenantStatus, bo.getTenantStatus());
        lqw.and(StringUtils.isNotBlank(bo.getKeyword()), w -> w.like(SupplyCloudTenant::getCloudTenantName, bo.getKeyword())
            .or().like(SupplyCloudTenant::getCloudTenantCode, bo.getKeyword()));
        lqw.orderByDesc(SupplyCloudTenant::getLastSyncTime);
        Page<SupplyCloudTenant> page = cloudTenantMapper.selectPage(pageQuery.build(), lqw);
        List<Long> platformIds = page.getRecords().stream()
            .map(SupplyCloudTenant::getCloudPlatformId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, String> platformNameMap = platformIds.isEmpty()
            ? Collections.emptyMap()
            : cloudPlatformMapper.selectBatchIds(platformIds).stream()
                .collect(Collectors.toMap(SupplyCloudPlatform::getId, SupplyCloudPlatform::getPlatformName, (a, b) -> a));
        List<Long> snapshotIds = page.getRecords().stream().map(SupplyCloudTenant::getId).toList();
        List<SupplyOrgCloudTenantBind> binds = snapshotIds.isEmpty()
            ? List.of()
            : bindMapper.selectList(Wrappers.<SupplyOrgCloudTenantBind>lambdaQuery()
                .eq(SupplyOrgCloudTenantBind::getTenantId, currentTenantId())
                .in(SupplyOrgCloudTenantBind::getCloudTenantSnapshotId, snapshotIds));
        Map<Long, SupplyOrgCloudTenantBind> bindMap = binds.stream().collect(Collectors.toMap(SupplyOrgCloudTenantBind::getCloudTenantSnapshotId, item -> item, (a, b) -> a));
        List<Long> deptIds = binds.stream().map(SupplyOrgCloudTenantBind::getOrgId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> deptNameMap = deptIds.isEmpty() ? Collections.emptyMap() : remoteDeptService.selectDeptNamesByIds(deptIds);
        List<SupplyCloudTenantVo> list = page.getRecords().stream().map(item -> toVo(item, platformNameMap, bindMap, deptNameMap)).toList();
        if (StringUtils.isNotBlank(bo.getBindStatus())) {
            list = list.stream().filter(item -> bo.getBindStatus().equals(item.getBindStatus())).toList();
        }
        return new TableDataInfo<>(list, page.getTotal());
    }

    @Override
    public CollectExecuteResultVo refreshByCloudPlatformId(Long cloudPlatformId, Long triggerUserId) {
        SupplyCollectConfig config = collectConfigMapper.selectOne(Wrappers.<SupplyCollectConfig>lambdaQuery()
            .eq(SupplyCollectConfig::getTenantId, currentTenantId())
            .eq(SupplyCollectConfig::getCloudPlatformId, cloudPlatformId)
            .eq(SupplyCollectConfig::getCollectScope, "tenant")
            .last("limit 1"));
        CollectExecuteBo bo = new CollectExecuteBo();
        bo.setTriggerUserId(triggerUserId);
        bo.setExecuteMode("refresh");
        if (config != null) {
            bo.setCollectConfigId(config.getId());
            return collectConfigService.executeCollect(bo);
        }
        CollectExecuteResultVo vo = new CollectExecuteResultVo();
        vo.setAccepted(Boolean.FALSE);
        vo.setMessage("当前云平台未配置云租户采集配置");
        return vo;
    }

    @Override
    public List<SupplyOptionVo> queryOptions(Long cloudPlatformId, String keyword, String bindStatus) {
        SupplyCloudTenantBo bo = new SupplyCloudTenantBo();
        bo.setCloudPlatformId(cloudPlatformId);
        bo.setKeyword(keyword);
        bo.setBindStatus(bindStatus);
        List<SupplyCloudTenantVo> list = queryPageList(bo, new PageQuery(Integer.MAX_VALUE, 1)).getRows();
        return list.stream().map(item -> {
            SupplyOptionVo vo = new SupplyOptionVo();
            vo.setLabel(item.getCloudTenantName());
            vo.setValue(item.getCloudTenantSnapshotId());
            vo.setExtra(Map.of("cloudTenantCode", item.getCloudTenantCode(), "bindStatus", item.getBindStatus()));
            return vo;
        }).toList();
    }

    private SupplyCloudTenantVo toVo(SupplyCloudTenant entity, Map<Long, String> platformNameMap,
                                     Map<Long, SupplyOrgCloudTenantBind> bindMap, Map<Long, String> deptNameMap) {
        SupplyCloudTenantVo vo = new SupplyCloudTenantVo();
        BeanUtil.copyProperties(entity, vo);
        vo.setCloudTenantSnapshotId(entity.getId());
        vo.setCloudPlatformName(platformNameMap.get(entity.getCloudPlatformId()));
        vo.setRawPayload(parseJsonObject(entity.getRawPayload()));
        SupplyOrgCloudTenantBind bind = bindMap.get(entity.getId());
        vo.setBindStatus(bind == null ? "unbound" : bind.getBindStatus());
        if (bind != null) {
            vo.setBoundOrgId(bind.getOrgId());
            vo.setBoundOrgName(deptNameMap.get(bind.getOrgId()));
        }
        return vo;
    }
}
