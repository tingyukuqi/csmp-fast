package com.csmp.supply.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csmp.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 采集执行日志
 *
 * @author csmp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supply_collect_log")
public class SupplyCollectLog extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long collectConfigId;

    private Long cloudPlatformId;

    private String collectScope;

    private String executeMode;

    private String jobInstanceId;

    private String traceId;

    private Long triggerUserId;

    private Date startTime;

    private Date endTime;

    private Long durationMs;

    private String resultStatus;

    private String syncStatus;

    private Integer resourceCount;

    private Integer cloudTenantCount;

    private Integer syncRecordCount;

    private String configSnapshot;

    private String errorMessage;

    @TableLogic
    private String delFlag;
}
