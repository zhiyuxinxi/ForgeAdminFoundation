# Single-Table CRUD Reference

## Inputs to Confirm

Before generating files, identify:

- Module/plugin name, business object name, table name, route path, component path, menu parent, and permission prefix.
- Field list with DB type, Java type, frontend component type, validation, searchable/list/edit/import/export flags, and sort/index needs.
- Dictionary fields and whether each dictionary reuses an existing `dict_type` or needs new seed data.
- Sensitive fields that require API encryption/decryption or response masking.
- Import/export enablement and Excel `config_key`.

## Backend File Layout

Follow the existing plugin package layout:

```text
forge/forge-framework/forge-plugin-parent/forge-plugin-<module>/
├── src/main/java/com/mdframe/forge/plugin/<module>/
│   ├── controller/<Business>Controller.java
│   ├── domain/entity/<Business>.java
│   ├── dto/<Business>DTO.java
│   ├── dto/<Business>Query.java
│   ├── mapper/<Business>Mapper.java
│   ├── service/<Business>Service.java
│   ├── service/impl/<Business>ServiceImpl.java
│   └── vo/<Business>VO.java
└── src/main/resources/mapper/<Business>Mapper.xml
```

If the target module has a different established package pattern, follow the module pattern instead of introducing a parallel convention.

## Entity Pattern

Use `TenantEntity` for business tables with `tenant_id`.

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_example")
public class BizExample extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String exampleName;

    private String status;
}
```

Do not duplicate `createBy`, `createTime`, `createDept`, `updateBy`, `updateTime`, or `tenantId` fields when extending `TenantEntity`.

## Controller Contract

Generate Forge codegen-safe endpoints. Do not use `PUT` or `DELETE` for generated CRUD modules because project gateway and security policies expect POST for detail, update, and delete operations.

Ordinary maintainable ledgers require actual page/filter/detail/add/edit/single-delete support under coding-style 8.7. Each API must bind the real action permission (annotation or exact API resource), validate its DTO/ID, and enforce trusted tenant/data scope. Detail/edit/delete check the original record; delete also enforces actual reference and state restrictions with explicit failure feedback. Read-only/archive/audit/approval exceptions need a business basis; a missing route is not such evidence. The protocol sketch below does not replace those checks or authorize copying a native module's tenant-admin bypasses.

```java
@Slf4j
@RestController
@RequestMapping("/biz/example")
@RequiredArgsConstructor
public class BizExampleController {

    private final BizExampleService exampleService;

    @GetMapping("/page")
    @OperationLog(module = "示例管理", type = OperationType.QUERY, desc = "分页查询示例")
    public RespInfo<Page<BizExampleVO>> page(PageQuery pageQuery, BizExampleQuery query) {
        return RespInfo.success(exampleService.page(pageQuery, query));
    }

    @PostMapping("/getById")
    @OperationLog(module = "示例管理", type = OperationType.QUERY, desc = "查询示例详情")
    public RespInfo<BizExampleVO> detail(@RequestParam Long id) {
        return RespInfo.success(exampleService.getDetail(id));
    }

    @PostMapping("/add")
    @OperationLog(module = "示例管理", type = OperationType.ADD, desc = "新增示例")
    public RespInfo<Long> create(@Valid @RequestBody BizExampleDTO dto) {
        return RespInfo.success(exampleService.create(dto));
    }

    @PostMapping("/edit")
    @OperationLog(module = "示例管理", type = OperationType.UPDATE, desc = "修改示例")
    public RespInfo<Void> update(@Valid @RequestBody BizExampleDTO dto) {
        exampleService.update(dto);
        return RespInfo.success();
    }

    @PostMapping("/remove/{id}")
    @OperationLog(module = "示例管理", type = OperationType.DELETE, desc = "删除示例")
    public RespInfo<Void> delete(@PathVariable Long id) {
        exampleService.delete(id);
        return RespInfo.success();
    }

    // Optional: generate only when the agreed contract needs a backend batch endpoint.
    @PostMapping("/removeBatch")
    @OperationLog(module = "示例管理", type = OperationType.DELETE, desc = "批量删除示例")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        exampleService.deleteBatch(ids);
        return RespInfo.success();
    }
}
```

Add class-level or method-level `@ApiDecrypt` and `@ApiEncrypt` when fields or the page use encrypted requests. For mixed endpoints, encrypt reads with `@ApiEncrypt` and decrypt mutating request bodies with `@ApiDecrypt`.

## Service and Mapper Rules

- Service methods handle validation, uniqueness checks, DTO-to-entity mapping, and transaction boundaries.
- Mapper XML handles page/list/detail queries and any joins required for VO rendering.
- Ordinary multi-select deletion may call the existing single-delete API through the public frontend per-item handler; do not create a batch endpoint merely because selection is enabled. An actual batch API validates nonempty bounded IDs, applies the same permission/scope/reference/state rules, and declares atomic or per-item outcomes. Never discard a single-delete failure and report whole-batch success; use bulk persistence only after those rules and the transaction contract are satisfied.
- Do not inject two Services into each other. Put orchestration in an application Service or a Manager; the Controller delegates rather than owning cross-domain rules.

Mapper XML page query skeleton:

```xml
<select id="selectPage" resultMap="BaseResultMap">
    SELECT
    <include refid="Base_Columns"/>
    FROM biz_example
    WHERE tenant_id = #{tenantId}
    <if test="query.exampleName != null and query.exampleName != ''">
      AND example_name LIKE CONCAT('%', #{query.exampleName}, '%')
    </if>
    <if test="query.status != null and query.status != ''">
      AND status = #{query.status}
    </if>
    ORDER BY update_time DESC, id DESC
</select>
```

For fields named `region_code`, include the `ALL` virtual organization rule from `AGENTS.md` in Mapper XML.

## Frontend Page Pattern

Use `AiCrudPage`; a custom wrapper may compose existing canonical owners, but must not replace shared search, table, pagination, or form behavior. Extend only the specialized business area that the existing owners cannot support; do not hand-roll the common add, edit, or delete flow.

```vue
<template>
  <AiCrudPage
    :api-config="{
      list: 'get@/api/biz/example/page',
      detail: 'post@/api/biz/example/getById',
      add: 'post@/api/biz/example/add',
      update: 'post@/api/biz/example/edit',
      delete: 'post@/api/biz/example/remove/:id',
      export: 'post@/api/excel/export/biz_example_export',
      import: 'post@/api/excel/import/biz_example_export',
      importTemplate: 'get@/api/excel/template/biz_example_export',
    }"
    :search-schema="searchSchema"
    :columns="tableColumns"
    :edit-schema="editSchema"
    row-key="id"
    :load-detail-on-edit="true"
    :show-import="true"
    :show-export="true"
  />
</template>

<script setup>
import { computed, h } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

const { dict } = useDict('sys_enable_disable')
const statusOptions = computed(() => dict.value.sys_enable_disable || [])

const searchSchema = computed(() => [
  { field: 'exampleName', label: '示例名称', type: 'input', props: { clearable: true, placeholder: '请输入示例名称' } },
  { field: 'status', label: '状态', type: 'select', props: { options: statusOptions.value, clearable: true } },
])

const tableColumns = computed(() => [
  { prop: 'exampleName', label: '示例名称', minWidth: 160 },
  { prop: 'status', label: '状态', width: 100, render: row => h(DictTag, { dictType: 'sys_enable_disable', value: row.status }) },
  { prop: 'createTime', label: '创建时间', width: 160 },
])

const editSchema = computed(() => [
  { field: 'exampleName', label: '示例名称', type: 'input', rules: [{ required: true, message: '请输入示例名称', trigger: 'blur' }] },
  { field: 'status', label: '状态', type: 'select', props: { options: statusOptions.value } },
])
</script>
```

If a numeric DB field uses dictionary values stored as strings, convert options through a local computed helper so submitted values match backend types.

## Import and Export

For fixed generated CRUD, prefer the common Excel endpoints when no business-specific permission wrapping is required:

- `POST /api/excel/export/{configKey}`
- `GET /api/excel/template/{configKey}`
- `POST /api/excel/import/{configKey}`

If the business module needs custom permission, validation, or persistence logic, generate wrapper endpoints under the business Controller and keep the same request/response expectations used by AiCrudPage.

Always generate matching SQL for `sys_excel_export_config` and `sys_excel_column_config` when enabling import/export.
