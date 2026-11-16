# AI 助手后端对接设计（AI_MODULE_DESIGN）

**状态：设计稿，未实现。** 本文档只做技术设计，未修改任何业务代码；经用户确认后才进入编码。

**依据：** 当前源码（`Dish` / `DishIngredient` / `DishStep` / `CartItem` / `CartServiceImpl` / `DishEditView.vue`）、`application.properties`、`pom.xml`（Spring Boot 3.3.2，已含 `spring-boot-starter-web`），以及本机 Ollama `qwen2.5:7b` 已验证的结构化输出效果。

---

## 1. 设计范围与目标

| 等级 | 能力 | 本设计是否覆盖 |
|---|---|---|
| L1 | 粘贴菜谱文本 → 结构化（菜名 / 食材 / 步骤） | **本期完整设计** |
| L2 | 购物车整餐分析（卡路里 / 营养 / 搭配建议） | 概要设计（后续细化） |
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

## 5. L2 购物车整餐分析（概要设计，后续细化）

### 5.1 API 设计

`POST /api/ai/analyze-cart`（需 JWT + `X-Family-Id`），请求体可空（后端自行取当前用户当前家庭购物车）。

响应：营养/热量/搭配建议文本或结构化 JSON（具体 schema 待细化）。

### 5.2 后端数据获取链路

```
CartServiceImpl 逻辑复用：
  1. currentUserService.requireMembership(familyId)
  2. 查 cart_items（userId + familyId）→ dishId 列表
  3. 查 dishes → 查 dish_ingredients（按 dishId）
  4. 拼成文本：「用户购物车有：番茄炒蛋(食材:番茄2个、鸡蛋2个…)、…」
```

> 注意：`CartResponse` 现不含食材，L2 需新增「购物车菜品 + 食材」的聚合查询（可复用 `DishIngredientRepository`，按 `dishId IN (...)` 查）。

### 5.3 健康档案前导提示词（合规关键）

后端在调用 AI 前读当前用户 profile（`dietaryPreferences` / `dietaryRestrictions` / `allergies` / `medicalConditions`），拼成前导提示词：

```text
用户情况：健康情况(糖尿病)，忌口(不吃辣)，过敏(海鲜)。请据此给出建议。
```

合规约束（`AGENTS.md 6.7`）：

- 本地 Ollama 不算「发送给第三方 AI 服务」，但仍须遵守「AI 饮食建议不得冒充医疗诊断」。
- 前端须提供**明确的授权开关 + 免责声明**（「建议仅供饮食参考，不构成医疗建议」），用户开启后才把健康信息注入提示词；关闭则只做不带个人健康信息的通用分析。
- 本期 L1（菜谱结构化）**不涉及**健康信息；L2 才需要上述授权。

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

## 7. 涉及文件清单（全部新增，不改 pom.xml）

| 层 | 文件 | 说明 |
|---|---|---|
| Config | `config/AiProperties.java`（或直接用 `@Value`） | 读取第 6 节配置 |
| Client | `service/ai/OllamaClient.java` | RestClient 封装，唯一 HTTP 出口 |
| Service | `service/AiRecipeService.java` | 拼提示词 + 解析 + 校验 + 重试 + 降级 |
| Controller | `controller/AiController.java` | `POST /api/ai/parse-recipe`（+ 后续 `analyze-cart`） |
| DTO | `dto/AiRecipeParseRequest.java`、`dto/AiRecipeParseResponse.java` | 请求/响应 |
| Exception | 复用/新增 `AiServiceException` | AI 调用失败专用异常 |
| 前端 | `frontend/src/api/ai.js` | api 封装 |
| 前端 | `DishEditView.vue` 增加「AI 智能识别」入口 | 与现有「粘贴分步」并列 |
| 配置 | `application.properties` 追加第 6 节 | 需审批 |

> 不改 `pom.xml`、不改认证/授权、不改既有 Entity/DTO/迁移。

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

## 11. 测试与验证计划（待实现后执行）

1. 后端 `mvn -o clean compile` 通过。
2. Ollama 本机 `qwen2.5:7b` 在线时，`POST /api/ai/parse-recipe` 实测：正常文本返回正确 JSON；非法 JSON 触发重试/降级。
3. Ollama 停止时，接口返回「AI 不可用」且不抛 500。
4. `app.ai.enabled=false` 时接口直接拒绝。
5. 前端构建通过；`DishEditView` 回填后走正常创建流程，`stepNo` 顺序正确。

---

## 12. 待用户拍板的决策点

1. **Ollama 接口风格**：原生 `/api/chat`（建议）还是 OpenAI 兼容 `/v1/chat/completions`（未来易换云）？
2. **AI 是否直接建菜**：本设计采用「AI 只返回草稿、回填表单、用户确认后走既有创建」（建议）。是否认可？
3. **本期范围**：先只做 L1，还是 L1 + L2 一起设计细化？
4. **是否落档**：本设计文档是否保留在 `docs/backend/`，并纳入 `docs/README.md` 索引？
