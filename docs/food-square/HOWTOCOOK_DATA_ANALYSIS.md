# HowToCook 菜谱数据调研与提取方案（HOWTOCOOK_DATA_ANALYSIS）

> **状态**：方案已实施（2026-11-17 更新）。阶段 A/C/D/E 已完成——纯规则解析 + Flyway V12/V13 + `ApplicationRunner` 幂等导入 373 道 + 浏览/复制接口；阶段 B 采用纯规则（未启用 AI 兜底）；阶段 F 的 API/数据核验待后端重启后执行。
> **日期**：2026-11-16（调研）；2026-11-17（实施回写）。
> **事实口径（调研时，2026-11-16）**：本文档初稿仅做只读分析，未修改 Java 源码、`pom.xml`、数据库/Flyway 脚本，未导入任何数据，未开发前端，未改动 HowToCook 原始仓库。所有"可直接提取/需 AI 补充/待实施"均为分析结论，不代表已实现。
>
> **实施回写（2026-11-17）**：本方案已实施；本文主体（第 1~6 节）为调研分析，保留历史原貌，实施结果以第 7 节各阶段「实施结果」标注为准。

---

## 1. 数据来源与许可

- **来源仓库**：`Anduin2017/HowToCook`（本地克隆于项目根目录 `HowToCook/`）。
- **用途定位**：作为本系统"美食广场"系统公共菜谱（SYSTEM）的主要基础数据来源。
- **许可**：仓库根目录 `LICENSE` 为 **Unlicense**（公有领域/公共版权放弃声明），允许复制、修改、发布、商用。文字菜谱与仓库内图片原则上同受该协议约束。
- **图片许可注意**：HowToCook 图片由众多社区贡献者上传，虽然整体仓库采用 Unlicense，但图片来源混杂（个人拍摄、网络搜集等）。**不应默认所有图片都能无风险商用**，导入时需保留原始来源与文件路径用于追溯，并对存疑图片采取"保留文字、暂缓图片"或"仅内部课程演示使用"的策略（详见第 5 节）。
- **原始文件路径保留**：每个菜谱保留其相对路径（如 `HowToCook/dishes/vegetable_dish/西红柿炒鸡蛋.md`）作为来源追溯依据。

---

## 2. 数据目录结构与分类统计

`HowToCook/dishes/` 下按目录划分菜谱大类，每个菜谱为一个同名 Markdown 文件（可带同名子目录存放图片）。

| 分类 | 目录 | Markdown 菜谱数 | 图片数（约） | 说明 |
|---|---|---|---|---|
| 素菜 | `vegetable_dish/` | 65 | 50 | 家常素菜 |
| 荤菜 | `meat_dish/` | 111 | 148 | 占比最高，含畜/禽/肉类 |
| 水产 | `aquatic/` | 28 | 30 | 鱼、虾、蟹、贝 |
| 早餐 | `breakfast/` | 25 | 3 | 多数无图 |
| 主食 | `staple/` | 59 | 60 | 米饭、面、饼、饺子 |
| 半成品加工 | `semi-finished/` | 10 | 6 | 速冻/预制菜加工 |
| 汤与粥 | `soup/` | 24 | 12 | 汤、粥 |
| 饮料 | `drink/` | 23 | 16 | 含酒精饮品（需筛选） |
| 酱料和其它材料 | `condiment/` | 9 | 7 | 酱料、糖色 |
| 甜品 | `dessert/` | 19 | 22 | 烘焙、甜品 |
| 模板 | `template/` | 1（示例菜） | — | 非正式菜谱，作格式参考 |

**合计**：正式菜谱 **373 道**（`template/` 示例菜不计入），图片约 **350 张**（`.jpg/.jpeg/.png/.webp`）。

> 另：根目录 `README.md` 提供完整菜谱索引与分类说明；`tips/` 为厨房技巧（非菜谱，暂不纳入）；`LICENSE` 见第 1 节。

---

## 3. 菜谱内容格式分析

以模板（`dishes/template/示例菜/示例菜.md`）与多个实际菜谱抽样（西红柿炒鸡蛋、可乐鸡翅、麻婆豆腐等）为准，每个 Markdown 由固定章节组成：

| 章节 | 形式 | 提取价值 |
|---|---|---|
| 一级标题 | `# 菜名的做法` | → `name`（去"的做法"） |
| 主图 | `![菜名](./xxx.jpg)` | → `imageUrl`（可选，部分菜谱无图） |
| 简介段落 | 一段中文描述（特点/营养/难度/耗时） | → `description` |
| `预估烹饪难度：★★` | 1~5 星（★） | 难度（当前项目无字段） |
| `预估卡路里：XXX 大卡` | 数字 + "大卡" | 卡路里（健康信息，当前项目无字段） |
| `## 必备原料和工具` | 无序列表（食材 + 工具 + 可选标注） | 粗略原料清单（含工具，需甄别） |
| `## 计算` | 无序列表，精确用量 | → `DishIngredient`（核心数据，格式不统一） |
| `## 操作` | 有序列表 `1. 2. 3.`，可含嵌套子步骤 | → `DishStep` |
| `## 附加内容` | 注意事项、参考资料 | 可并入描述或忽略 |
| 结尾固定句 | "如果您遵循本指南…" | 需剔除 |

### 3.1 关键难点：`## 计算` 用量格式不统一

抽样发现的典型写法（需结构化才能映射到 `DishIngredient(name/amount/unit)`）：

- `* 西红柿 = 1 个（约 180g） * 份数`
- `* 鸡蛋 = 1.5 个 * 份数，向上取整`
- `* 鸡翅 10 ～ 12 只`
- `- 20-30g 五花肉`
- `- 500ml`
- `- 2 片生姜`
- `- 小葱挽成结`（无数字）

结论：**存在"份数公式 / 区间 / 单位前置后置 / 无数字"等多种情况**，纯正则难以覆盖，需「规则解析 + AI 兜底」组合（见第 4、7 节）。

### 3.2 其它需处理的细节

- **工具与食材混杂**：`必备原料和工具` 中混有锅、刀、微波炉等工具，需剔除（或仅取 `计算` 章节作为食材来源）。
- **可选食材**：`糖（可选）`、`葱花（可选）` 需打标；当前 `DishIngredient` 无"可选"字段，可保留在名称或忽略。
- **步骤嵌套**：`操作` 中有缩进子步骤与备注，提取时仅取顶层 `1. 2. 3.` 序号，子步骤可并入该步内容或忽略。
- **步骤图片**：HowToCook 步骤基本不含步骤图（仅主图），`DishStep.imageUrl` 多为空。

---

## 4. 与当前项目菜品实体的字段映射表

> 当前项目菜品模型（已读源码确认）：`Dish`、`DishIngredient`、`DishStep`、`DishCategory`（实体）、`DishRequest`/`DishResponse`（DTO）、`DishController`/`DishServiceImpl`（接口/逻辑）、`V1__create_homechef_schema.sql`（表结构）。

| HowToCook 内容 | 项目字段 | 可提取性 | 说明 |
|---|---|---|---|
| 标题 `# X 的做法` | `Dish.name` (100) | ✅ 直接 | 去掉"的做法"、与文件名一致 |
| 简介段落 | `Dish.description` (500) | ✅ 直接 | 超长需截断/摘要 |
| 主图 `![](./x.jpg)` | `Dish.imageUrl` (500) | ⚠️ 需处理 | 见第 5 节图片方案 |
| 目录分类 | `Dish.categoryId` → `DishCategory.name` (50) | ⚠️ 映射 | 映射为系统分类或复制时映射到家庭分类 |
| 预估难度 ★ | **无对应字段** | ❌ 缺失 | 需新增 `difficulty` 字段（可选） |
| 预估卡路里 | **无对应字段** | ❌ 缺失 | 健康信息，需谨慎，建议暂不采集 |
| `## 计算` 用量 | `DishIngredient(name/amount/unit/sortNo)` | ⚠️ 需解析 | `amount` 为 `BigDecimal` 必填，`unit` 必填（≤20），需结构化 |
| `## 操作` 步骤 | `DishStep(stepNo/content/imageUrl)` | ✅ 可提取 | 顶层序号连续从 1 起 |
| 来源/文件路径 | **无对应字段** | ❌ 缺失 | 需新增来源字段用于追溯 |
| 价格 | `Dish.price`（`NOT NULL`） | ❌ 缺失 | 系统菜谱无价格；复制到家庭时由用户定 |
| SYSTEM/FAMILY 归属 | `Dish.familyId`（`NOT NULL`） | ❌ 核心缺口 | 见第 4.1 节 |

### 4.1 核心架构约束：系统公共菜谱 ≠ 现有 `dishes` 表

当前 `dishes` 表对家庭归属是**强约束**（`V1__create_homechef_schema.sql` 实测）：

- `family_id BIGINT NOT NULL` + 外键 `fk_dishes_family → families(id)`；
- `created_by BIGINT NOT NULL` + 复合外键 `fk_dishes_creator_membership → family_members(family_id, user_id)`；
- `price DECIMAL(10,2) NOT NULL`；
- `uk_dishes_family_id UNIQUE(family_id, id)` 及 `fk_dishes_category_same_family`、`fk_dishes_creator_membership` 等均绑定 `family_id`。

**结论**：系统公共菜谱「不属于任何家庭、无家庭成员创建者、无价格」，**不能直接写入现有 `dishes` 表**，否则会破坏家庭数据隔离与订单快照设计。推荐方案（第 7 节）：

1. **新增独立表族 `system_dishes` + `system_dish_ingredients` + `system_dish_steps`**（`family_id` 可空/无 `family_id`），与 `dishes` 表物理隔离；
2. 「添加到我家」时，从 `system_dishes` 读取完整内容，**复制**到 `dishes`（设置目标 `familyId`、`createdBy`、用户自定 `price`），之后家庭可自由修改，订单仍只引用家庭自己的 `dishes`。

> 该方案不改动现有 `dishes` 表结构与约束，风险最低，符合"家庭订单不直接引用系统菜谱"的要求。

---

## 5. 图片处理建议

- **现状**：当前项目已实现图片上传服务 `POST /api/upload`（`UploadController`，限 `jpg/jpeg/png/webp/gif`、≤5MB、UUID 文件名）与静态映射 `WebConfig`（`/uploads/**` → 本地 `uploads/` 目录）；`Dish.imageUrl` 存储 URL 字符串。
- **HowToCook 图片**：菜谱图片为本地相对路径（如 `./1.jpeg`），不能直接被本项目 `imageUrl` 引用，需先落地。
- **建议策略（分档）**：
  1. **首选**：仅导入文字菜谱（名称/描述/食材/步骤），主图暂空或使用占位图，避免图片版权与体积问题；
  2. **次选**：挑选明确可用的菜品成品图，上传至本项目 `uploads/`，`imageUrl` 记录 `/uploads/{uuid}.jpg`，并在 `system_dishes` 记录原始来源路径与许可说明；
  3. **课程演示场景**：可本地引用 HowToCook 图片用于演示，但须在文档注明来源与 Unlicense 协议，不宣称商用。
- **体积与仓库约束**：HowToCook 图片约 350 张（数 MB~十数 MB），不应整体拷贝进本项目 `uploads/` 提交 Git；应选择性上传，且 `uploads/` 已加入 `.gitignore`。

---

## 6. 初步推荐的 30~50 道菜谱名单

聚焦家庭日常、食材易得、步骤清晰，覆盖主要分类（共 40 道，路径省略 `HowToCook/dishes/` 前缀）：

**素菜（9）**：西红柿炒鸡蛋、酸辣土豆丝、手撕包菜、地三鲜、干锅花菜、红烧茄子、蒜蓉西兰花、虎皮青椒、蚝油生菜。

**荤菜（12）**：可乐鸡翅、简易红烧肉、宫保鸡丁、回锅肉、鱼香肉丝、糖醋里脊、糖醋排骨、黄焖鸡、辣椒炒肉、土豆炖排骨、西红柿牛腩、水煮肉片。

**水产（6）**：清蒸鲈鱼、白灼虾、红烧鱼、油焖大虾、蒜蓉虾、水煮鱼。

**汤与粥（6）**：西红柿鸡蛋汤、紫菜蛋花汤、玉米排骨汤、排骨山药玉米汤、皮蛋瘦肉粥、罗宋汤。

**主食（8）**：蛋炒饭、扬州炒饭、炸酱面、葱油拌面、热干面、手工水饺、西红柿鸡蛋挂面、蛋包饭。

**早餐（5）**：煎饺、鸡蛋三明治、牛奶燕麦、水煮玉米、完美水煮蛋。

> 筛选原则：排除含酒精饮品（`drink/`）、需特殊工具/高难度的烘焙甜品、以及"半成品加工"依赖预制品的条目；后续实施时可按此名单先导 40 道，再逐步扩充。

---

## 7. 后续批量整理与导入 MySQL 的实施方案（待确认后实施）

> 以下为规划（2026-11-16 调研时），实施进度见本节末尾「实施结果」回写。

### 阶段 A：离线提取脚本（不改业务代码）
- 编写一次性解析工具（脚本/临时 Java 类），遍历 `HowToCook/dishes/**/*.md`，解析出结构化中间数据（`name`、`description`、主图路径、难度、卡路里、分类、`ingredients[]`、`steps[]`、原始相对路径）。
- 输出中间产物：JSON 或 SQL 种子文件，**保留原始路径**字段。

### 阶段 B：食材用量结构化（规则 + AI 兜底）
- 先用规则解析 `计算` 章节的常见格式（`数字 + 单位`、`数字-数字 + 单位`、`数字 个`）。
- 对规则无法覆盖的（如"小葱挽成结"、"向上取整"），**复用现有 AI L1 能力**（`POST /api/ai/parse-recipe`，Ollama `qwen2.5:7b`）离线批量结构化，人工抽检。

> **实施结果（2026-11-17）**：最终**未启用 AI 兜底**，改为纯规则解析（`HowToCookMarkdownParser`）。规则对「数字+单位」直接量化；对「份数公式 / 区间 / 多选 / 比例 / 约数 / 无数字」显式识别后降级为 `amount`/`unit` 置空、原文存 `amountText`，由「复制到我家」时人工补齐，避免小模型幻觉产生错误精确值（详见 `DECISION_LOG.md` D-030）。

### 阶段 C：数据库设计（新增 Flyway 迁移）
- 设计 `system_dishes` / `system_dish_ingredients` / `system_dish_steps` 表（含 `source_type`、`source_url`、`original_path`、`difficulty` 等溯源与扩展字段）。
- 形成设计文档并新增 `V 版迁移脚本`，经审批后执行（属高风险数据库变更，须人工批准）。

### 阶段 D：批量导入
- 用数据初始化器（`CommandLineRunner`/`ApplicationRunner` 或独立导入服务）读取中间产物，幂等写入 `system_dishes` 表族。

> **实施结果（2026-11-17）**：已实现。采用 `SystemDishImportRunner`（`ApplicationRunner`）+ 配置开关 `app.system-dish.import.enabled`（默认 `false`）；`SystemDishImportService` 遍历 `HowToCook/dishes`、按目录名映射分类（与 V13 种子一致）、以 `(source_type, original_path)` 幂等去重、每道菜独立事务（`TransactionTemplate`）。真实导入结果：**新增 373、跳过(已存在) 0、跳过(未知分类目录) 0、失败 0**（`template/` 示例菜不计入）。

### 阶段 E：后端「美食广场 + 添加到我家」
- 新增系统菜谱浏览接口（所有用户可读，不依赖 `X-Family-Id`）。
- 新增「添加到我家」复制接口：读取 `system_dishes` 完整内容 → 复制到 `dishes`（设置目标 `familyId`、`createdBy`、用户自定 `price`）→ 事务保证一致。

> **实施结果（2026-11-17）**：已实现。`SystemDishController` 提供 `GET /api/system-dish-categories`、`GET /api/system-dishes`（可选 `categoryId`/`keyword`）、`GET /api/system-dishes/{id}`、`POST /api/system-dishes/{id}/copy-to-family`（需 `X-Family-Id`）。

### 阶段 F：测试与验收
- 导入后核对 40 道菜谱字段完整性与抽样人工验收；端到端走查「浏览 → 添加到我家 → 家庭订单下单」。

> **实施结果（2026-11-17）**：解析器单测 7 例已通过、真实导入 373 道已成功；API 与数据库数据核验（分类数、菜谱总数、搜索命中、详情字段、中文无乱码）待后端重启后执行。

---

> **维护说明**：本文件为 `docs/food-square/` 首份文档。后续如新增数据库设计、API 设计等文档，需同步在 `docs/README.md` 索引登记（本次暂不登记，待方案经用户确认后统一处理）。
