# 考试领域（Exam Domain）数据库表结构总结

本文档总结了考试系统核心模块的表结构定义，包含试卷模板、考试实例及其统计相关表。

---

## 1. 试卷模板相关

### 1.1 试卷基础表 (`paper`)
用于存储试卷的元数据信息。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| id | BIGINT UNSIGNED | 是 | - | 主键 ID (PK) |
| owner_id | BIGINT UNSIGNED | 是 | - | 创建者用户 ID |
| title | VARCHAR(255) | 是 | - | 试卷标题 |
| description | VARCHAR(255) | 否 | NULL | 试卷描述/简介 |
| status | TINYINT | 是 | 0 | 状态：0=草稿(draft), 1=已发布(published), 2=已归档(archived) |
| total_items | INT UNSIGNED | 是 | 0 | 题目总数 |
| total_score | DECIMAL(10,2) | 是 | 0 | 试卷总分 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

### 1.2 试卷题目明细表 (`paper_item`)
用于记录试卷模板中包含的具体题目及其顺序。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| paper_id | BIGINT UNSIGNED | 是 | - | 所属试卷 ID (复合主键) |
| seq | INT | 是 | - | 题号，从 1 开始 (复合主键) |
| question_id | BIGINT UNSIGNED | 是 | - | 题目 ID |
| question_version_id | BIGINT UNSIGNED | 是 | - | 绑定的题目版本 ID |
| score | DECIMAL(6,2) | 是 | 0 | 该题在试卷中的分值 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

---

## 2. 考试实例相关

### 2.1 考试实例表 (`exam`)
记录具体发生的某次考试活动。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| id | BIGINT UNSIGNED | 是 | - | 主键 ID (PK) |
| owner_id | BIGINT UNSIGNED | 是 | - | 负责人/创建者 ID |
| paper_id | BIGINT UNSIGNED | 否 | NULL | 来源试卷 ID（可选） |
| title | VARCHAR(255) | 是 | - | 考试名称 |
| status | TINYINT | 是 | 0 | 状态：0=草稿, 1=进行中, 2=已结束 |
| total_items | INT UNSIGNED | 是 | 0 | 题目总数 |
| total_score | DECIMAL(10,2) | 是 | 0 | 考试总分 |
| stats_applied | TINYINT | 是 | 0 | 统计回灌状态：0=未回灌, 1=已回灌 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

### 2.2 考试题目快照表 (`exam_item_snapshot`)
固化考试时题目版本和分值的快照。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| exam_id | BIGINT UNSIGNED | 是 | - | 考试实例 ID (复合主键) |
| seq | INT | 是 | - | 题号，从 1 开始 (复合主键) |
| question_id | BIGINT UNSIGNED | 是 | - | 题目 ID |
| question_version_id | BIGINT UNSIGNED | 是 | - | 固化的题目版本 ID |
| score | DECIMAL(6,2) | 是 | 0 | 固化的题目分值 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 快照生成时间 |

---

## 3. 统计相关

### 3.1 考试题目统计表 (`exam_question_stat`)
按题目维度聚合统计单次考试的得分分布情况。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| exam_id | BIGINT UNSIGNED | 是 | - | 考试实例 ID (复合主键) |
| seq | INT | 是 | - | 题号 (复合主键) |
| question_id | BIGINT UNSIGNED | 是 | - | 题目 ID |
| question_version_id | BIGINT UNSIGNED | 是 | - | 题目版本 ID |
| max_score | DECIMAL(6,2) | 是 | 0 | 该题理论最高分 |
| attempts | INT UNSIGNED | 是 | 0 | 作答总份数 |
| full_score_cnt | INT UNSIGNED | 是 | 0 | 满分份数 |
| score_sum | DECIMAL(12,2) | 是 | 0 | 该题得分总和 |
| score_sq_sum | DECIMAL(18,4) | 是 | 0 | 得分平方和（用于计算标准差/方差） |
| min_score | DECIMAL(6,2) | 否 | NULL | 实际观测到的最低得分 |
| max_score_observed | DECIMAL(6,2) | 否 | NULL | 实际观测到的最高得分 |
| histogram_json | JSON | 否 | NULL | 得分分布桶（JSON 格式数据） |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 统计更新时间 |

---

## 💡 开发提示：
1. **复合主键映射**：`paper_item`、`exam_item_snapshot` 和 `exam_question_stat` 均使用了复合主键（如 `exam_id + seq`），在编写 Entity 时需注意映射处理。
2. **精度处理**：分值字段使用了 `DECIMAL` 类型，Java 代码中推荐对应 `java.math.BigDecimal`。
3. **统计逻辑**：`exam_question_stat` 中的 `score_sq_sum` 可协助你实现进阶的物联网工程相关数据分析（如成绩波动的统计学评估）。