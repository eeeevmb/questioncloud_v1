- 子题集

**题目导入接口实现**：

给出如下的题集模板，用户在填入题目数据后上传.tex文件，
应用层解析并抽取出题目原文，存入question、question_version表。
```latex
\begin{pro}
    计算 $I=\iint_D|\cos (x+y)| \mathrm{d} x \mathrm{~d} y$, 其中区域 $D$ 为: $0 \leq x \leq \frac{\pi}{2}, 0 \leq y \leq \frac{\pi}{2}$.
\end{pro}	

\begin{solu}
    如图, 用直线 $x+y=\frac{\pi}{2}$ 将区域 $D$ 分为 $D_1$ 和 $D_2$ 两个区域, 则

    \begin{minipage}{.75\textwidth}
        \begin{align*}
            \begin{aligned}
                I & =\iint_{D_1} \cos (x+y) \mathrm{d} x \mathrm{~d} y-\iint_{D_2} \cos (x+y) \mathrm{d} x \mathrm{~d} y \\
                & =\int_0^{\frac{\pi}{2}} \mathrm{~d} x \int_0^{\frac{\pi}{2}-x} \cos (x+y) \mathrm{d} y-\int_0^{\frac{\pi}{2}} \mathrm{~d} x \int_{\frac{\pi}{2}-x}^{\frac{\pi}{2}} \cos (x+y) \mathrm{d} y \\
                & =\int_0^{\frac{\pi}{2}}(1-\sin x) \mathrm{d} x-\int_0^{\frac{\pi}{2}}(\cos x-1) \mathrm{d} x=\pi-2 .
                \end{aligned}
        \end{align*}
    \end{minipage}%
    \begin{minipage}{.25\textwidth}
        \includegraphics[width = \textwidth]{../Level1/fig/fig1.png}
    \end{minipage}
\end{solu}
```
**实现难题**：
- 题干和解析会插入图片，入库时如何设计存储逻辑

**解决方案**:\
分段解析，tex 导入阶段只负责识别结构和占位符，
图片物理文件由用户在后续步骤上传
（这一步骤也可以供用户二次确认图片和题目是否正确）；
确认后提交请求，应用层统一batch入库。

先梳理这条导入链路的“状态”该怎么保存：

1. 解析阶段返回 importSessionId
    - 同时把“解析出来的草稿 + 资产占位清单”缓存在服务端，可以选：
        - Redis（天然带 TTL，支持多实例扩展）
    - Key 可以设为 question:import:{sessionId}，value 序列化存 List<QuestionDraftVO> 或更轻量的结构，TTL 设 15~30 分钟。
2. 资产上传阶段
    - 对于每个占位符（例如 solu-1-1），客户端单独上传图片：POST /api/v1/question/import/{sessionId}/assets 携带 slotId + MultipartFile。
    - 服务端根据 sessionId 取缓存 → 找到对应草稿 → 为该 slotId 写入上传结果（文件 ID、存储路径等），再更新回缓存。
    - 如果一次上传多个，也可以用 slotId -> fileId 的 JSON 批量更新，但核心还是通过 session 查取草稿并标记哪些 slot 已完成。
3. 二次确认（最终提交）接口
    - 请求体至少需要：

      {
      "sessionId": "...",
      "collectionId": 123,
      "typeCode": "...",
      "assets": [
      { "slotId": "solu-1-1", "fileId": 881 },
      ...
      ],
      "titleOverrides": [...] // 若用户手动补标题可选
      }
    - 服务端再从缓存取解析结果，校验：
        - sessionId 是否存在/未过期
        - 每个 slot 是否都找到上传好的文件（资产列表可由上传接口逐步填充，也可在确认时一次性提交）
    - 校验通过后，才把数据写入 question/question_version/collection 等表，并在完成后删除缓存里的 sessionId。
4. 为什么不靠消息队列
    - 队列擅长异步解耦，但这里是同步交互流程；MQ 反而增加复杂度。缓存就足够在“解析 → 上传 → 提交”之间共享状态。
    - 若后续想把“解析耗时任务”异步化，可以在解析阶段提交 MQ，生成 session 待结果回调，这属于另一个层面的优化。
5. 用户确认的说明
    - 因为缓存里仍保留原始草稿，前端拿到解析响应可以渲染编辑界面；最终确认时带上 sessionId 即可在服务端取回相同版本，保证不会因客户端修改
      内容而丢失信息。
6. 失败/超时处理
    - 如果 sessionId 过期或用户放弃提交，缓存自动回收；上传过的临时文件可以结合定时任务清理（按 sessionId 标记 owner）。
7. 额外扩展
    - 若解析结果较大，可把草稿存成压缩 JSON；也可以在 Redis 里按题目拆成多条记录 question:import:{sessionId}:{draftIndex}，便于并发更新。

总之，第二步、第三步的接口都以 sessionId 为索引，状态放缓存即可；不需要消息队列。你要做的就是设计好缓存结构和确认请求体，让上传/确认都能
基于 session 查到解析快照。

