package com.csmp.supply.service.impl;

import com.csmp.common.core.exception.ServiceException;
import com.csmp.common.core.utils.StringUtils;
import com.csmp.common.json.utils.JsonUtils;
import com.csmp.common.satoken.utils.LoginHelper;
import com.csmp.common.tenant.helper.TenantHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 供应链服务基类
 *
 * @author csmp
 */
public abstract class AbstractSupplyService {

    protected String currentTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            tenantId = LoginHelper.getTenantId();
        }
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("当前租户上下文不存在");
        }
        return tenantId;
    }

    protected Long currentUserId() {
        return LoginHelper.getUserId();
    }

    protected Object parseJsonObject(String text) {
        return JsonUtils.parseMap(text);
    }

    protected Date parseDateTime(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(text);
        } catch (ParseException e) {
            throw new ServiceException("时间格式不正确: {}", text);
        }
    }
}
