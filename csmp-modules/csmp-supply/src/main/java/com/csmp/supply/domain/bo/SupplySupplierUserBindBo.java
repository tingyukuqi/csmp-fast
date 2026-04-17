package com.csmp.supply.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 供应商用户绑定请求
 *
 * @author csmp
 */
@Data
public class SupplySupplierUserBindBo {

    @NotEmpty(message = "用户ID不能为空")
    private List<Long> userIds;
}
