# AI 助手后端对接设计（AI_MODULE_DESIGN）

**状态：已实现（L1 + L2）。** 本文档已从设计稿演进为实现记录；L1 菜谱结构化与 L2 购物车整餐分析均已编码完成，后端编译与前端构建通过，前端入口已接入。L3 不做。

**依据：** 当前源码（`Dish` / `DishIngredient` / `DishStep` / `CartItem` / `CartServiceImpl` / `DishEditView.vue`）、`application.properties`、`pom.xml`（Spring Boot 3.3.2，已含 `spring-boot-starter-web`），以及本机 Ollama `qwen2.5:7b` 已验证的结构化输出效果。

---

## 1. 设计范围与目标

| 等级 | 能力 | 状态 |
|---|---|---|
| L1 | 粘贴菜谱文本 → 结构化（菜名 / 食材 / 步骤） | **已实现**（`POST /api/ai/parse-recipe`） |
| L2 | 购物车整餐分析（卡路里 / 营养 / 搭配建议） | **已实现**（`POST /api/ai/analyze-cart`） |
| L3 | 自然语言点单 / 多轮对话助手 | **不做**（性价比最低，且需 function calling + 会话管理） |

核心定位：**AI 是「增值辅助」，不是点单主流程的必经环节**。AI 关闭时，新建菜品 / 购物车 / 点单照常工作。

---

## 2. 总体架构

```
前端（DishEditView / CartView）
        │  POST /api/ai/...
        ▼
AiController ──► AiRecipeService（业务编排：鉴权、拼提示词、校验、降级）
                      │
                      ▼
                 OllamaClient（RestClient 封装，唯一对外 HTTP 调用点）
                      │  POST {base-url}/api/chat
                      ▼
                 本地 Ollama（qwen2.5:7b）
```

关键原则：

1. **前端永远不接触 Ollama**。所有 AI 调用走后端代理，便于统一做限流、缓存、日志、成本与异常处理。
2. **AI 只「解析」，不「建菜」**。L1 的 AI 输出是「建议草稿」，回填到 `DishEditView` 表单，用户确认后再走既有 `POST /api/dishes` 创建流程。好处：复用已有 Bean Validation + 家庭归属校验，用户保留最终确认权，AI 失败只是少一个便捷入口。

---

## 3. 关键技术决策

| 决策点 | 结论 | 理由 |
|---|---|---|
| HTTP 客户端 | Spring 6.1 自带 `RestClient` | `spring-boot-starter-web` 已含，**零依赖改动**，符合「不随便改 pom.xml」 |
| Ollama 接口 | 原生 `POST /api/chat` + `format: "json"`（第一版） | 本地 Ollama 最稳；`format` 是原生字段，能强制 JSON 输出，与命令行已验证行为一致；后续换云模型只需替换 `OllamaClient` 实现 |
| AI 是否直接写库 | 否，只返回解析草稿 | 见第 2 节原则 2 |
| 失败降级 | 解析失败 → 重试 → 降级到前端已有正则分步 | 保证功能不崩 |
| 健康信息 | 本地模型不算「发第三方」，但仍标「建议/估算」，不冒充医疗 | `AGENTS.md 6.7` |

> 备选：若想未来平滑切换到 DeepSeek 等云模型，可用 OpenAI 兼容接口 `/v1/chat/completions`。本次先按原生 `/api/chat` 设计，接口差异被 `OllamaClient` 隔离。

---

## 4. L1 菜谱结构化（本期完整设计）

### 4.1 API 设计

`POST /api/ai/parse-recipe`（需 JWT 认证，不依赖 `X-Family-Id`，因为不落库）

请求：

```json
{ "text": "番茄炒蛋：番茄两个，鸡蛋2个，盐少许。步骤：1.番茄切块；2.鸡蛋打散炒熟盛出；..." }
```

响应（成功）：

```json
{
  "name": "番茄炒蛋",
  "ingredients": [
    { "name": "番茄", "amount": 2, "unit": "个" },
    { "name": "鸡蛋", "amount": 2, "unit": "个" },
    { "name": "盐",   "amount": 0, "unit": "少许" }
  ],
  "steps": ["番茄切块", "鸡蛋打散炒熟盛出", "下番茄炒出汁，混合鸡蛋加盐出锅"]
}
```

响应（AI 失败 / 超时）：返回错误码，前端降级提示「AI 识别失败，可手动填写或使用「粘贴分步」」。

### 4.2 新增 DTO

| DTO | 字段 | 说明 |
|---|---|---|
| `AiRecipeParseRequest` | `text`（`@NotBlank`） | 用户粘贴的菜谱原文 |
| `AiRecipeParseResponse` | `name`、`ingredients[]`、`steps[]` | 与 `DishRequest` 字段对齐（`amount` 为 `BigDecimal`，`unit` 非空） |

> `ingredients[].amount` 采用 `BigDecimal`，`unit` 非空；「少许 / 适量」由 AI 约定为 `amount=0 + unit="少许"`，既能通过 `@DecimalMin("0.00")` 与 `@NotBlank`，又保留信息。这是「补丁式」约定（见 4.6 建模局限）。

### 4.3 `OllamaClient` 设计

职责：封装对本地 Ollama 的 HTTP 调用，**是唯一直接依赖 Ollama 的类**。

- 依赖：`RestClient`（构造时按 `app.ai.ollama.base-url` 构建）、`ObjectMapper`。
- 核心方法：`String chat(String systemPrompt, String userPrompt)`，内部：
  1. 组装请求体 `{ model, messages: [{role:"system",...},{role:"user",...}], format:"json", stream:false }`；
  2. POST 到 `{base-url}/api/chat`；
  3. 读取响应 JSON 的 `message.content`（Ollama 原生返回结构），返回字符串；
  4. 连接失败 / 非 2xx → 抛自定义 `AiServiceException`。
- 超时：单次请求设置较长超时（如 120s），因为 CPU 跑 7b 较慢。

### 4.4 提示词设计

提示词作为常量集中在 `AiRecipeService`（或独立常量类），不散落。核心约束（已在本机验证有效）：

```text
你是菜谱解析助手，只输出 JSON，不要任何解释、markdown、注释。
固定 JSON 结构严格如下，不能新增字段、不能修改结构：
{"name":"菜名","ingredients":[{"name":"食材名称","amount":数字,"unit":"单位"}],"steps":["步骤文字1","步骤文字2"]}
强制规则：
1. amount 必须是阿拉伯数字（整数/小数），禁止中文数字、禁止把单位写进 amount；
2. unit 不能为空，必须单独提取单位（个/克/勺/毫升等）；
3. 遇到少许、适量、若干这类模糊描述，amount 填 0，unit 填原文。
解析下面菜谱：
{用户粘贴文本}
```

### 4.5 解析、校验、重试、降级流程

```
AiRecipeService.parseRecipe(text)
  1. 调用 OllamaClient.chat(...) 拿原始 JSON 字符串
  2. ObjectMapper.readValue 解析为 AiRecipeParseResponse（try-catch）
  3. 解析失败 → 重试 1~2 次（同请求重发或追加「只输出 JSON」）
  4. 仍失败 → 抛 AiServiceException → 前端降级到「粘贴分步」/ 手动填写
  5. 解析成功 → 对 amount/unit/name/steps 做二次校验
     - name 空白则置空（前端可让用户补）
     - steps 为空则视为失败
     - ingredients 里 amount 为 null 或负 → 归 0
  6. 返回 AiRecipeParseResponse（stepNo 不在此生成，由前端提交时按顺序补 1,2,3…）
```

> **`stepNo` 不归模型管**：模型只输出 `steps` 数组；`DishStepRequest.stepNo`（`@NotNull @Min(1)`）由前端 `DishEditView` 提交时按 `idx + 1` 生成（现有代码已这么做）。

### 4.6 已知建模局限（不本期改）

`DishIngredient.amount` 为 `BigDecimal` 非空、`unit` 非空，强制「每个食材都有精确数字 + 单位」，对「少许盐 / 适量油」不友好，只能靠 `amount=0 + unit="少许"` 绕过。若后续想更贴近真实（允许无数量食材），需单独评估是否放宽 `amount`/`unit` 约束，属**改实体 + DTO + 迁移**，须走审批。

---

## 5. L2 购物车整餐分析（已实现）

### 5.1 API 设计

`POST /api/ai/analyze-cart`（需 JWT + `X-Family-Id`），请求体可空（后端自行取当前用户当前家庭购物车，默认不注入健康档案）。

请求（可空）：

```json
{ "includeHealthProfile": true }
```

响应（成功）：

```json
{
  "summary": "整体评价文本",
  "nutrition": "营养概况文本",
  "suggestions": ["建议1", "建议2"],
  "cautions": ["注意事项1"]
}
```

> `includeHealthProfile` 默认 `false`；为 `true` 时后端才把当前用户健康档案（忌口/过敏/疾病标签）注入提示词（见 5.3）。缺失 `X-Family-Id` 返回 400。

### 5.2 后端数据获取链路（`AiCartAnalyzeService.analyzeCart`）

1. `currentUserService.requireMembership(familyId)`——家庭数据隔离校验，不信任前端传入 `familyId`。
2. `cartItemRepository.findByUserIdAndFamilyIdOrderByCreatedAtAsc(userId, familyId)`——按「用户 + 家庭」取购物车。
3. `dishRepository.findAllById(dishIds)` 一次 IN 查全菜品，`Collectors.toMap(Dish::getId, ...)` 转 Map 做 O(1) 查找，避免双重循环。
4. 对每个菜品循环 `dishIngredientRepository.findByDishIdOrderBySortNoAsc(dishId)` 取食材（购物车菜品少，N+1 可接受，避免新增 Repository 查询方法）。
5. 拼成文本「用户购物车有：番茄炒蛋(食材:番茄2个、鸡蛋2个、盐少许)、…」；`amount` 用 `stripTrailingZeros().toPlainString()` 去尾零，`amount=0` 时只取 `unit`。
6. （可选）读用户 profile，拼健康档案前导提示词（见 5.3）。
7. 调 `OllamaClient.chat`，`ObjectMapper` 解析，失败重试 2 次，仍失败抛 `AiServiceException`（→ 502）。

### 5.3 健康档案前导提示词（合规关键）

后端在调用 AI 前读当前用户 profile（`dietaryPreferences` / `dietaryRestrictions` / `allergies` / `medicalConditions`，均为逗号分隔字符串，可能含英文枚举如 `DIABETES`/`NO_SPICY`），拼成前导提示词，并注明「标签可能为英文枚举，请按语义理解」。

合规约束（`AGENTS.md 6.7`）：

- 本地 Ollama 不算「发送给第三方 AI 服务」，但仍须遵守「AI 饮食建议不得冒充医疗诊断」。
- `includeHealthProfile` 默认 `false`——**用户不勾选就不注入任何健康信息**；前端面板提供「结合我的健康档案」勾选项，勾选后触发带健康档案的重新分析。
- 前端面板常驻「建议仅供饮食参考，不构成医疗建议」免责声明。
- 提示词强制「用约/估算等措辞，禁止输出精确医疗数据或医疗诊断结论」。

---

## 6. 配置项（`application.properties` 新增，需审批后改）

```properties
app.ai.enabled=true
app.ai.ollama.base-url=http://localhost:11434
app.ai.ollama.model=qwen2.5:7b
app.ai.ollama.timeout-ms=120000
```

- `app.ai.enabled=false` 时，`AiController` 直接返回「AI 未启用」，前端隐藏 AI 入口，实现一键降级。
- 生产部署时 `base-url` 指向服务器上的 Ollama 地址。

---

## 7. 实际涉及文件清单（已实现）

后端（6 新增 + 2 修改，不改 `pom.xml`）：

| 层 | 文件 | 说明 |
|---|---|---|
| Client | `service/ai/OllamaClient.java`（新增） | RestClient 封装，唯一 HTTP 出口 |
| Service | `service/AiRecipeService.java`（新增） | L1：拼提示词 + 解析 + 校验 + 重试 + 降级 |
| Service | `service/AiCartAnalyzeService.java`（新增） | L2：聚合购物车/食材 + 健康档案注入 + 解析 |
| Controller | `controller/AiController.java`（新增） | `POST /api/ai/parse-recipe` + `POST /api/ai/analyze-cart` |
| DTO | `dto/AiRecipeParseRequest/Response.java`、`dto/AiCartAnalyzeRequest/Response.java`（新增） | L1/L2 请求与响应 |
| Exception | `exception/AiServiceException.java`（新增） | AI 调用失败专用异常 |
| 修改 | `exception/GlobalExceptionHandler.java` | 加 `AiServiceException` → 502 |
| 修改 | `application.properties` | 追加第 6 节 4 行 AI 配置 |

前端（1 新增 + 3 修改）：

| 层 | 文件 | 说明 |
|---|---|---|
| API | `frontend/src/api/ai.js`（新增） | `parseRecipe` + `analyzeCart`，均覆盖 120s 超时 |
| 修改 | `DishEditView.vue` | L1 入口：加「AI 智能识别」按钮 + 弹出面板 + 回填 |
| 修改 | `CartView.vue` | L2 入口：加「AI 整餐分析」按钮 + 弹出面板 + 健康档案勾选 |
| 修改 | `styles/cart.css` | 加 `.cart-ai` / `.ai-analyze-*` 样式 |

> 不改 `pom.xml`、不改认证/授权、不改既有 Entity/DTO/迁移；L2 复用 `DishIngredientRepository.findByDishIdOrderBySortNoAsc` 循环查询，未新增 Repository 方法。

---

## 8. 错误处理与降级

| 场景 | 处理 |
|---|---|
| Ollama 未启动 / 连接失败 | `AiServiceException` → 前端提示「AI 服务不可用」→ 走手动/正则 |
| 模型返回非 JSON | 解析失败 → 重试 1~2 次 → 仍失败降级 |
| 响应超时 | 同连接失败 |
| `app.ai.enabled=false` | Controller 直接 503 / 明确错误码 |
| 未登录 | 走既有 JWT 过滤链 |

---

## 9. 安全与合规

- 前端不接触 Ollama 地址与调用，统一后端代理。
- L1 不涉及健康信息；L2 注入健康档案前必须有前端授权开关 + 免责声明（`AGENTS.md 6.7`）。
- AI 输出只作「草稿」，最终落库仍走既有 `Dish` 创建校验与家庭归属校验，AI 不绕过业务规则。
- 提示词中不得要求模型输出医疗诊断结论。

---

## 10. 风险与回滚

| 风险 | 缓解 |
|---|---|
| 小模型 JSON 输出不稳定 | `format:json` + 强提示词 + 重试 + 降级（见 4.5） |
| CPU 跑 7b 慢 | 设置 120s 超时；前端给 loading 提示；L1 低频可接受 |
| 健康信息合规 | 本地部署 + 授权开关 + 免责声明；L1 完全不碰 |
| 模型换云 | `OllamaClient` 隔离接口差异，替换实现即可 |

回滚：`app.ai.enabled=false` 一键关闭；新增文件独立，删除即可恢复，不影响既有模块。

---

## 11. 测试与验证结果（已执行）

| 验证项 | 结果 |
|---|---|
| 后端编译 | `mvn -o -q clean compile -DskipTests` → BUILD SUCCESS（exitCode 0） |
| 前端构建 | `npm --prefix frontend run build` → 成功（L1 403 modules；L2 403 modules，CartView 7.12 kB） |
| lint | 后端 5 文件 + 前端 3 文件 → 0 diagnostics |
| Ollama 结构化（阶段 0 预验证） | 本机 `qwen2.5:7b` 对菜谱结构化一次通过，输出 JSON 完全符合 schema（含「盐少许 → amount=0 + unit=少许」） |
| 前端人工验证 | L1 已在浏览器验证通过（用户确认）；L2 用户确认实现完成 |

待执行（需启动后端 + Ollama 的端到端 HTTP 实测）：

1. `POST /api/ai/parse-recipe` 端到端实测（Ollama 在线正常文本 / 非法 JSON 触发重试 / Ollama 停止返回「AI 不可用」不抛 500）。
2. `POST /api/ai/analyze-cart` 端到端实测（购物车聚合 + 健康档案注入）。
3. `app.ai.enabled=false` 时接口直接拒绝。

---

## 12. 决策点回顾（已拍板）

1. **Ollama 接口风格**：采用原生 `/api/chat` + `format:"json"`（已实现；`OllamaClient` 隔离接口差异，未来换云只需替换实现）。
2. **AI 是否直接建菜**：AI 只返回草稿、回填表单、用户确认后走既有 `POST /api/dishes` 创建（已按此实现）。
3. **本期范围**：L1 + L2 均已实现；L3 不做。
4. **是否落档**：本设计文档保留在 `docs/backend/`，并已纳入 `docs/README.md` 索引。
