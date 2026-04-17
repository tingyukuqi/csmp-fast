package com.csmp.supply.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csmp.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 事件处理日志
 *
 * @author csmp
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supply_event_log")
public class SupplyEventLog extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long subscriptionId;

    private Long cloudPlatformId;

    private String eventScope;

    private String eventKey;

    private String sourceEventId;

    private Date eventTime;

    private Date ingestTime;

    private String processStatus;

    private String traceId;

    private String rawPayload;

    private String normalizedPayload;

    private String errorMessage;

    @TableLogic
    private String delFlag;
}
