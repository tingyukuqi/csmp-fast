# 角色分级权限管理 API 文档

**版本**: 2.6.0
**模块**: csmp-system
**基础路径**: `/system/role`（经网关转发后为 `/role`）
**认证方式**: Bearer Token（Sa-Token）

---

## 目录

1. [通用结构](#1-通用结构)
2. [枚举值定义](#2-枚举值定义)
3. [数据模型](#3-数据模型)
4. [角色 CRUD 接口](#4-角色-crud-接口)
5. [角色分级权限接口（新增）](#5-角色分级权限接口新增)
6. [角色数据权限接口](#6-角色数据权限接口)
7. [角色用户授权接口](#7-角色用户授权接口)
8. [业务错误码](#8-业务错误码)
9. [前端开发指引](#9-前端开发指引)

---

## 1. 通用结构

### 1.1 统一响应 `R<T>`

所有非分页接口统一使用此结构：

```json
{
    "code": 200,
    "msg": "操作成功",
    "data": T
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码。200=成功，500=失败，601=警告 |
| msg | String | 提示消息 |
| data | T | 业务数据，无数据时为 null |

**判断逻辑**：`code === 200` 表示成功。

### 1.2 分页响应 `TableDataInfo<T>`

分页查询接口使用此结构（注意：外层不是 `R` 包裹）：

```json
{
    "code": 200,
    "msg": "查询成功",
    "total": 100,
    "rows": [T, T, ...]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200=成功 |
| msg | String | 提示消息 |
| total | long | 总记录数 |
| rows | List\<T\> | 当前页数据列表 |

### 1.3 分页请求参数

分页查询接口通过 Query 参数传递分页信息：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | Integer | 否 | 1 | 当前页码（从1开始） |
| pageSize | Integer | 否 | 无限制 | 每页条数 |
| orderByColumn | String | 否 | 无 | 排序字段（驼峰命名，自动转下划线） |
| isAsc | String | 否 | 无 | 排序方向：`asc` / `desc`，支持多字段如 `asc,desc` |

### 1.4 请求头

| Header | 必填 | 说明 |
|--------|------|------|
| Authorization | 是 | `Bearer {token}`，登录接口获取 |
| clientid | 是 | 客户端标识 |

---

## 2. 枚举值定义

### 2.1 角色状态 `status`

| 值 | 说明 |
|----|------|
| `0` | 正常 |
| `1` | 停用 |

### 2.2 数据范围 `dataScope`

| 值 | 名称 | 说明 |
|----|------|------|
| `1` | 全部数据权限 | 可查看所有数据 |
| `2` | 自定义数据权限 | 指定部门范围 |
| `3` | 本部门数据权限 | 仅本部门 |
| `4` | 本部门及以下数据权限 | 本部门+子部门 |
| `5` | 仅本人数据权限 | 仅自己创建的数据 |
| `6` | 部门及以下或本人数据权限 | 本部门+子部门 或 仅本人 |

### 2.3 有效菜单来源 `source`

| 值 | 说明 |
|----|------|
| `OWN` | 角色自有菜单（直接分配） |
| `INHERITED` | 从父角色继承的菜单 |

---

## 3. 数据模型

### 3.1 角色视图对象 `SysRoleVo`（响应用）

```typescript
interface SysRoleVo {
    roleId: number              // 角色ID
    roleName: string            // 角色名称
    roleKey: string             // 角色权限字符串（如 "admin"）
    roleSort: number            // 显示顺序
    dataScope: string           // 数据范围（见枚举 2.2）
    menuCheckStrictly: boolean  // 菜单树是否关联显示
    deptCheckStrictly: boolean  // 部门树是否关联显示
    status: string              // 状态（见枚举 2.1）
    remark: string              // 备注
    createTime: string          // 创建时间（ISO 8601）
    parentId: number | null     // 父角色ID，顶级为 null
    roleLevel: number           // 层级深度，顶级=0
    parentRoleName: string | null // 父角色名称（仅树形接口返回）
    children: SysRoleVo[] | null  // 子角色列表（仅树形接口返回）
    flag: boolean               // 是否已分配给当前用户（仅特定场景）
}
```

### 3.2 角色业务对象 `SysRoleBo`（请求用）

**新增/修改角色**时使用：

```typescript
interface SysRoleBo {
    roleId?: number             // 角色ID（新增时不传，修改时必传）
    roleName: string            // 角色名称（必填，最长30字符）
    roleKey: string             // 权限字符串（必填，最长100字符）
    roleSort: number            // 显示顺序（必填）
    dataScope?: string          // 数据范围（默认 "1"）
    menuCheckStrictly?: boolean // 菜单树关联显示
    deptCheckStrictly?: boolean // 部门树关联显示
    status?: string             // 状态（默认 "0"）
    remark?: string             // 备注
    menuIds?: number[]          // 自有菜单ID数组（菜单分配时传）
    deptIds?: number[]          // 自定义数据权限的部门ID数组（dataScope=2时传）
    parentId?: number | null    // 父角色ID（null或不传表示顶级角色）
    hiddenMenuIds?: number[]    // 隐藏的继承菜单ID数组
}
```

**校验规则**：
- `roleName`：必填，1-30字符
- `roleKey`：必填，1-100字符
- `roleSort`：必填
- `parentId` 不能等于 `roleId`（不能自己引用自己）
- 父角色必须存在且在同一租户内
- 不允许形成循环引用（A→B→C→A）
- 子角色 `dataScope` 不能超过父角色范围（见 [约束规则](#96-数据权限继承约束)）

### 3.3 角色有效菜单 `SysRoleEffectiveMenu`

```typescript
interface SysRoleEffectiveMenu {
    roleId: number              // 角色ID
    menuId: number              // 有效菜单ID
    source: string              // 来源："OWN" 或 "INHERITED"
    inheritFromRoleId: number | null // 继承来源角色ID，自有菜单为 null
}
```

---

## 4. 角色 CRUD 接口

### 4.1 获取角色分页列表

`GET /role/list`

**权限**: `system:role:list`

**Query 参数**（除分页参数外）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页条数 |
| roleName | String | 否 | 角色名称（模糊匹配） |
| roleKey | String | 否 | 权限字符串（模糊匹配） |
| status | String | 否 | 状态筛选 |
| params[beginTime] | String | 否 | 创建时间起（yyyy-MM-dd） |
| params[endTime] | String | 否 | 创建时间止（yyyy-MM-dd） |

**响应**: `TableDataInfo<SysRoleVo>`

```json
{
    "code": 200,
    "msg": "查询成功",
    "total": 5,
    "rows": [
        {
            "roleId": 1,
            "roleName": "超级管理员",
            "roleKey": "superadmin",
            "roleSort": 1,
            "dataScope": "1",
            "menuCheckStrictly": true,
            "deptCheckStrictly": false,
            "status": "0",
            "remark": "超级管理员",
            "createTime": "2024-01-01 00:00:00",
            "parentId": null,
            "roleLevel": 0,
            "parentRoleName": null,
            "children": null,
            "flag": false
        }
    ]
}
```

### 4.2 获取角色详情

`GET /role/{roleId}`

**权限**: `system:role:query`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleId | Long | 是 | 角色ID |

**响应**: `R<SysRoleVo>`

```json
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "roleId": 3,
        "roleName": "部门管理员",
        "roleKey": "dept_admin",
        "roleSort": 3,
        "dataScope": "4",
        "menuCheckStrictly": true,
        "deptCheckStrictly": false,
        "status": "0",
        "remark": "",
        "createTime": "2024-01-01 00:00:00",
        "parentId": 1,
        "roleLevel": 1,
        "parentRoleName": null,
        "children": null,
        "flag": false
    }
}
```

### 4.3 新增角色

`POST /role`

**权限**: `system:role:add`
**防重复提交**: 是

**请求体**: `SysRoleBo` (JSON)

```json
{
    "roleName": "运维人员",
    "roleKey": "ops_user",
    "roleSort": 5,
    "dataScope": "5",
    "status": "0",
    "remark": "运维岗位角色",
    "menuIds": [100, 101, 102, 103],
    "parentId": 3,
    "hiddenMenuIds": [105, 106]
}
```

**响应**: `R<Void>`

```json
// 成功
{ "code": 200, "msg": "操作成功", "data": null }

// 失败 — 角色名称重复
{ "code": 500, "msg": "新增角色'运维人员'失败，角色名称已存在", "data": null }

// 失败 — 权限字符串重复
{ "code": 500, "msg": "新增角色'运维人员'失败，角色权限已存在", "data": null }

// 失败 — 循环引用
{ "code": 500, "msg": "不允许设置循环的父角色关系!", "data": null }

// 失败 — 数据权限约束
{ "code": 500, "msg": "子角色数据权限范围不能超过父角色! 父角色数据范围: 仅本人, 当前设置: 本部门", "data": null }
```

### 4.4 修改角色

`PUT /role`

**权限**: `system:role:edit`
**防重复提交**: 是

**请求体**: `SysRoleBo` (JSON)，必须包含 `roleId`

```json
{
    "roleId": 5,
    "roleName": "运维人员",
    "roleKey": "ops_user",
    "roleSort": 5,
    "dataScope": "3",
    "status": "0",
    "remark": "运维岗位角色-修改",
    "menuIds": [100, 101, 102, 103, 104],
    "parentId": 3,
    "hiddenMenuIds": [105]
}
```

**响应**: `R<Void>`（同新增）

**注意**：修改角色后，该角色关联的在线用户会被强制下线重新登录以刷新权限。

### 4.5 删除角色

`DELETE /role/{roleIds}`

**权限**: `system:role:remove`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleIds | Long[] | 是 | 角色ID，多个用逗号分隔 |

**示例**: `DELETE /role/5,6,7`

**响应**: `R<Void>`

```json
// 成功
{ "code": 200, "msg": "操作成功", "data": null }

// 失败 — 角色已分配用户
{ "code": 500, "msg": "部门管理员已分配，不能删除!", "data": null }

// 失败 — 角色下存在子角色
{ "code": 500, "msg": "该角色下存在子角色，不能删除!", "data": null }
```

### 4.6 修改角色状态

`PUT /role/changeStatus`

**权限**: `system:role:edit`
**防重复提交**: 是

**请求体**:

```json
{
    "roleId": 5,
    "status": "1"
}
```

**响应**: `R<Void>`

### 4.7 获取角色下拉列表

`GET /role/optionselect`

**权限**: `system:role:query`

**Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleIds | Long[] | 否 | 指定角色ID列表（不传则返回所有） |

**响应**: `R<List<SysRoleVo>>`

```json
{
    "code": 200,
    "msg": "操作成功",
    "data": [
        {
            "roleId": 1,
            "roleName": "超级管理员",
            "roleKey": "superadmin",
            "roleSort": 1,
            "status": "0"
        }
    ]
}
```

### 4.8 导出角色

`POST /role/export`

**权限**: `system:role:export`

**请求体**: `SysRoleBo`（查询条件，同列表接口）

**响应**: Excel 文件流（`Content-Type: application/octet-stream`）

---

## 5. 角色分级权限接口（新增）

> 以下接口是本次角色分级权限新增的 API，前端需重点对接。

### 5.1 获取角色树形结构

获取所有角色的树形层级结构，用于**父角色选择器**。

`GET /role/tree`

**权限**: `system:role:list`

**Query 参数**: 无

**响应**: `R<List<SysRoleVo>>`

```json
{
    "code": 200,
    "msg": "操作成功",
    "data": [
        {
            "roleId": 1,
            "roleName": "超级管理员",
            "roleKey": "superadmin",
            "roleSort": 1,
            "parentId": null,
            "roleLevel": 0,
            "status": "0",
            "children": [
                {
                    "roleId": 3,
                    "roleName": "部门管理员",
                    "roleKey": "dept_admin",
                    "roleSort": 3,
                    "parentId": 1,
                    "roleLevel": 1,
                    "status": "0",
                    "children": [
                        {
                            "roleId": 5,
                            "roleName": "普通员工",
                            "roleKey": "staff",
                            "roleSort": 5,
                            "parentId": 3,
                            "roleLevel": 2,
                            "status": "0",
                            "children": []
                        }
                    ]
                }
            ]
        },
        {
            "roleId": 4,
            "roleName": "测试角色",
            "roleKey": "test",
            "roleSort": 4,
            "parentId": null,
            "roleLevel": 0,
            "status": "0",
            "children": []
        }
    ]
}
```

**前端用法**：
- 树形下拉选择器：展示 `roleName`，选中后取 `roleId` 作为 `parentId` 传给新增/修改接口
- 角色层级展示：使用 `children` 递归渲染树形组件
- 禁用自身：编辑角色时，树中不应出现自己及自己的子孙角色（避免循环引用）

### 5.2 获取角色有效菜单列表

获取某个角色的所有有效菜单（包含自有和继承），含来源标记。用于**角色编辑页面的菜单分配展示**。

`GET /role/{roleId}/effective-menus`

**权限**: `system:role:query`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleId | Long | 是 | 角色ID |

**响应**: `R<List<SysRoleEffectiveMenu>>`

```json
{
    "code": 200,
    "msg": "操作成功",
    "data": [
        {
            "roleId": 5,
            "menuId": 100,
            "source": "INHERITED",
            "inheritFromRoleId": 3
        },
        {
            "roleId": 5,
            "menuId": 101,
            "source": "INHERITED",
            "inheritFromRoleId": 3
        },
        {
            "roleId": 5,
            "menuId": 200,
            "source": "OWN",
            "inheritFromRoleId": null
        },
        {
            "roleId": 5,
            "menuId": 201,
            "source": "OWN",
            "inheritFromRoleId": null
        }
    ]
}
```

**前端用法**：
- `source === "OWN"` 的菜单：角色自有，可直接取消勾选
- `source === "INHERITED"` 的菜单：从父角色继承，不可直接取消，只能通过「隐藏」操作
- 用 `inheritFromRoleId` 追溯来源（可展示 tooltip："继承自 XXX 角色"）

### 5.3 批量隐藏继承菜单

将指定的继承菜单标记为隐藏。隐藏后该菜单不再出现在角色的有效权限中，关联用户的 Session 会被刷新。

`PUT /role/{roleId}/hide-menus`

**权限**: `system:role:edit`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleId | Long | 是 | 角色ID |

**请求体**: `Long[]`（菜单ID数组）

```json
[105, 106, 107]
```

**响应**: `R<Void>`

```json
{ "code": 200, "msg": "操作成功", "data": null }
```

**注意**：
- 只能隐藏继承来的菜单（`source === "INHERITED"`），不能隐藏自有菜单
- 隐藏操作会级联刷新该角色所有子孙角色的有效菜单
- 操作后关联在线用户会被强制下线

### 5.4 批量恢复继承菜单

将之前隐藏的继承菜单恢复为可见。

`PUT /role/{roleId}/restore-menus`

**权限**: `system:role:edit`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleId | Long | 是 | 角色ID |

**请求体**: `Long[]`（菜单ID数组）

```json
[105, 106]
```

**响应**: `R<Void>`

```json
{ "code": 200, "msg": "操作成功", "data": null }
```

**注意**：
- 恢复操作会级联刷新该角色所有子孙角色的有效菜单
- 操作后关联在线用户会被强制下线

---

## 6. 角色数据权限接口

### 6.1 修改数据权限

`PUT /role/dataScope`

**权限**: `system:role:edit`
**防重复提交**: 是

**请求体**:

```json
{
    "roleId": 5,
    "dataScope": "2",
    "deptIds": [200, 201, 202]
}
```

**响应**: `R<Void>`

**注意**: 当 `dataScope = "2"`（自定义）时，必须同时传 `deptIds`。

### 6.2 获取角色部门树

`GET /role/deptTree/{roleId}`

**权限**: `system:role:list`

**响应**: `R<DeptTreeSelectVo>`

```json
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "checkedKeys": [200, 201],
        "depts": [
            {
                "id": 100,
                "parentId": 0,
                "name": "总公司",
                "weight": 0,
                "children": [...]
            }
        ]
    }
}
```

### 6.3 数据权限继承约束

修改子角色 `dataScope` 时，后端会校验约束规则：

| 父角色 dataScope | 子角色允许的 dataScope |
|------------------|----------------------|
| `1`（全部数据） | 不限制 |
| `2`（自定义） | `2`, `3`, `4`, `5`, `6`（不能为1） |
| `3`（本部门） | `3`, `5` |
| `4`（本部门及子） | `3`, `4`, `5`, `6` |
| `5`（仅本人） | `5` |
| `6`（部门及子或本人） | `3`, `4`, `5`, `6` |

**前端应在提交前做预校验**，当父角色存在时，根据上表限制可选的 `dataScope` 选项。

---

## 7. 角色用户授权接口

### 7.1 查询已分配用户列表

`GET /role/authUser/allocatedList`

**权限**: `system:role:list`

**Query 参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleId | Long | 是 | 角色ID |
| userName | String | 否 | 用户名筛选 |
| phonenumber | String | 否 | 手机号筛选 |
| pageNum | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页条数 |

**响应**: `TableDataInfo<SysUserVo>`

### 7.2 查询未分配用户列表

`GET /role/authUser/unallocatedList`

参数和响应同 7.1。

### 7.3 取消授权用户

`PUT /role/authUser/cancel`

**权限**: `system:role:edit`
**防重复提交**: 是

**请求体**:

```json
{
    "userId": 10,
    "roleId": 5
}
```

**响应**: `R<Void>`

### 7.4 批量取消授权用户

`PUT /role/authUser/cancelAll`

**权限**: `system:role:edit`
**防重复提交**: 是

**Query 参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleId | Long | 是 | 角色ID |
| userIds | Long[] | 是 | 用户ID数组 |

**示例**: `PUT /role/authUser/cancelAll?roleId=5&userIds=10&userIds=11&userIds=12`

**响应**: `R<Void>`

### 7.5 批量授权用户

`PUT /role/authUser/selectAll`

**权限**: `system:role:edit`
**防重复提交**: 是

**Query 参数**: 同 7.4

**响应**: `R<Void>`

---

## 8. 业务错误码

角色分级权限相关的业务错误消息（通过 `msg` 字段返回，`code` 为 500）：

| 触发场景 | 错误消息 |
|----------|----------|
| 设置自己为父角色 | `不允许设置自己为父角色!` |
| 循环引用检测 | `不允许设置循环的父角色关系!` |
| 父角色不存在 | `父角色不存在!` |
| 跨租户引用 | `父角色不在同一租户内!` |
| dataScope 超出约束 | `子角色数据权限范围不能超过父角色! 父角色数据范围: {X}, 当前设置: {Y}` |
| 删除有子角色 | `该角色下存在子角色，不能删除!` |
| 删除已分配角色 | `{角色名}已分配，不能删除!` |
| 角色名重复 | `新增角色'{X}'失败，角色名称已存在` |
| 权限字符串重复 | `新增角色'{X}'失败，角色权限已存在` |
| 操作超管角色 | `不允许操作超级管理员角色` |
| 使用内置标识符 | `不允许使用系统内置管理员角色标识符!` |

---

## 9. 前端开发指引

### 9.1 角色管理页面改动

#### 新增/编辑角色弹窗

```
┌─────────────────────────────────────────┐
│  新增角色                                │
├─────────────────────────────────────────┤
│  角色名称: [____________]  *必填         │
│  权限字符: [____________]  *必填         │
│  显示顺序: [____]         *必填          │
│  父角色:   [▼ 请选择父角色 ]  ← 新增     │
│  状态:     ○正常 ○停用                   │
│  备注:     [____________]               │
│                                         │
│  ── 菜单权限 ──                          │
│  ☑ 系统管理                             │
│    ☑ 用户管理  (自有)                    │
│    ☑ 角色管理  (继承自XX)  ← 可隐藏      │
│    ☐ 日志管理  (继承自XX)  ← 已隐藏      │
│                                         │
│  ── 数据权限 ──                          │
│  数据范围: [▼ 请选择 ]                   │
│  部门选择: [树形选择] (范围=2时显示)      │
│                                         │
│              [取消]  [确定]              │
└─────────────────────────────────────────┘
```

#### 核心交互流程

```
1. 页面加载
   ├── GET /role/list        → 渲染角色列表
   └── GET /role/tree        → 缓存角色树（用于父角色选择器）

2. 点击"新增角色"
   ├── 弹窗中"父角色"下拉使用 GET /role/tree 的数据渲染树形选择器
   ├── "菜单权限"区域加载完整菜单树
   ├── 不选父角色 → 所有菜单为"自有"状态
   └── 选了父角色 → 自动展示继承的菜单（灰色勾选），可自行勾选额外菜单

3. 编辑角色
   ├── GET /role/{id}                    → 获取角色基础信息（含 parentId）
   ├── GET /role/{id}/effective-menus    → 获取有效菜单列表
   ├── 根据 source 区分：
   │   ├── OWN → 正常勾选，可取消
   │   └── INHERITED → 灰色勾选，不可直接取消
   │       └── 提供"隐藏"按钮 → 调用 PUT /role/{id}/hide-menus
   └── 已隐藏的菜单 → 显示为取消勾选状态，提供"恢复"按钮

4. 保存角色
   ├── 收集自有菜单ID → menuIds
   ├── 收集要隐藏的继承菜单ID → hiddenMenuIds
   └── POST 或 PUT /role 提交
```

### 9.2 菜单树的三态展示

| 状态 | 视觉效果 | 交互 | 数据来源 |
|------|----------|------|----------|
| 自有菜单 | 正常勾选（蓝色勾） | 可直接取消 | `effective-menus` 中 `source=OWN` |
| 继承菜单（未隐藏） | 灰色勾选 + 锁图标 | 点击显示 tooltip "继承自 XX 角色"，提供隐藏按钮 | `effective-menus` 中 `source=INHERITED` |
| 继承菜单（已隐藏） | 未勾选 + 虚线边框 | 显示 tooltip "已隐藏的继承菜单"，提供恢复按钮 | 上次 `hiddenMenuIds` |
| 未分配菜单 | 未勾选 | 可勾选新增为自有菜单 | 菜单树中不在 effective-menus 的节点 |

### 9.3 dataScope 联动

```javascript
// 当选择了父角色后，根据父角色的 dataScope 限制可选范围
const ALLOWED_SCOPE_MAP = {
    '1': ['1', '2', '3', '4', '5', '6'],     // 全部数据 → 不限制
    '2': ['2', '3', '4', '5', '6'],           // 自定义 → 不能选全部
    '3': ['3', '5'],                           // 本部门 → 只能更严格
    '4': ['3', '4', '5', '6'],                 // 本部门及子 → 不能比父宽松
    '5': ['5'],                                 // 仅本人 → 只能是仅本人
    '6': ['3', '4', '5', '6']                  // 部门及子或本人
}

// 获取父角色信息后过滤选项
function getAvailableScopes(parentDataScope) {
    if (!parentDataScope) return ALL_SCOPES // 顶级角色不限制
    return ALLOWED_SCOPE_MAP[parentDataScope] || ALL_SCOPES
}
```

### 9.4 典型前端请求封装（Axios 示例）

```typescript
// api/system/role.ts

/** 获取角色树 */
export function getRoleTree() {
    return request({ url: '/role/tree', method: 'get' })
}

/** 获取角色有效菜单 */
export function getRoleEffectiveMenus(roleId: number) {
    return request({ url: `/role/${roleId}/effective-menus`, method: 'get' })
}

/** 隐藏继承菜单 */
export function hideInheritedMenus(roleId: number, menuIds: number[]) {
    return request({ url: `/role/${roleId}/hide-menus`, method: 'put', data: menuIds })
}

/** 恢复继承菜单 */
export function restoreInheritedMenus(roleId: number, menuIds: number[]) {
    return request({ url: `/role/${roleId}/restore-menus`, method: 'put', data: menuIds })
}

/** 新增角色（含分级参数） */
export function addRole(data: SysRoleBo) {
    return request({ url: '/role', method: 'post', data })
}

/** 修改角色（含分级参数） */
export function updateRole(data: SysRoleBo) {
    return request({ url: '/role', method: 'put', data })
}
```

### 9.5 注意事项

1. **保存时合并提交**：新增/修改角色时，`menuIds`（自有菜单）和 `hiddenMenuIds`（隐藏菜单）在同一个请求中提交，不需要分开调用
2. **顶级角色**：不设置 `parentId`（或设为 `null`）即为顶级角色，层级深度为 0
3. **循环引用**：前端应在选择父角色时排除自身及其子孙角色，后端也有校验兜底
4. **刷新延迟**：隐藏/恢复菜单后，物化表异步刷新，通常 < 1 秒完成
5. **强制下线**：任何角色权限变更（菜单、层级、隐藏）都会导致关联用户被强制下线，用户需重新登录获取最新权限
