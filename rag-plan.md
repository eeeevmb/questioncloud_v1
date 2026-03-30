- 当前目标：给题目抽取知识点并入库 

上下文:
user : 其实我想的是你一步步教我写，不需要帮我修改代码，而是让我学习如何去写
codex:
• 可以，后续我只讲思路和代码骨架，不再替你改。

先说明一下：我上一个回合已经在工作区里落了一版实现草稿，但我现在不会继续动它。你如果想完全自己手写，我可以基于现有项目一步步带你写；如果你想把我刚才的改动撤回，我也可以后面教你怎么安全回退。

你这个需求，我建议按这个最小闭环来做：

1. 前端调用一个接口，传 questionId
2. 后端根据 questionId 找到题目的当前版本，拿到 stem
3. 调用现成的 KnowledgeExtractorAiService
4. 得到 KnowledgePointDTO 列表
5. 把知识点存到 knowledge_points
6. 把题目和知识点关系存到 question_knowledge_rel

你仓库里已经有两个关键基础了：

- 现成的 AI 抽取接口：/G:/JAVA/Code/questioncloud_v1/src/main/java/cn/sztu/questioncloud/infrastructure/common/ai/service/KnowledgeExtractorAiService.java
- 空的 RAG 控制器：/G:/JAVA/Code/questioncloud_v1/src/main/java/cn/sztu/questioncloud/web/rest/v1/rag/RagController.java

第一步：先定接口，不要一上来写数据库                                                                                                                                                                   
我建议接口先这样设计：

@PostMapping("/questions/{questionId}/knowledge-points/extract")                                                                                                                                       
public ResultVO<?> extractQuestionKnowledgePoints(@PathVariable Long questionId)

为什么这里传 questionId，而不是直接传 stem：

- 你最终要建立“题目-知识点”关联
- 你还要做权限校验
- 后续重跑抽取时也更方便

所以第一步你先在 RagController 里把接口路径和方法签名定下来，风格直接参考 /G:/JAVA/Code/questioncloud_v1/src/main/java/cn/sztu/questioncloud/web/rest/v1/question/QuestionController.java。

第二步：先写应用服务接口                                                                                                                                                                               
不要在 Controller 里直接写业务。先新建一个应用服务，比如：

public interface RagKnowledgeAppService {                                                                                                                                                              
QuestionKnowledgeExtractionResult extractAndSaveQuestionKnowledgePoints(Long questionId, Long userId);                                                                                             
}

这里返回一个结果对象，而不是直接返回 List<KnowledgePointDTO>，原因是后面你大概率还想带上：

- questionId
- 抽取数量
- 抽取出的知识点列表

第三步：实现这个服务时，先只写“前半段”                                                                                                                                                                 
先别急着存库，只先完成这 3 件事：

QuestionEntity question = questionRepository.getById(questionId);                                                                                                                                      
QuestionVersionEntity version = questionVersionRepository.getCurrentVersionByQuestionId(questionId);                                                                                                   
List<KnowledgePointDTO> points = knowledgeExtractorAiService.extractKnowledgePoints(version.getStem());

这一层里你要先补三个判断：

- 题目不存在
- 当前用户不是题目 owner
- stem 为空，不能抽取

这一步的依赖你项目里也都有参考：

- /G:/JAVA/Code/questioncloud_v1/src/main/java/cn/sztu/questioncloud/application/question/port/QuestionRepository.java
- /G:/JAVA/Code/questioncloud_v1/src/main/java/cn/sztu/questioncloud/application/question/port/QuestionVersionRepository.java

你现在先自己写到这里最合适。也就是先完成：

- RagController 方法签名
- RagKnowledgeAppService 接口
- RagKnowledgeAppServiceImpl 里“查题目 -> 查题干 -> 调 LLM”这半段

你写完以后，把这 3 个文件贴给我，我再继续带你写下一步：
- knowledge_points 入库
- question_knowledge_rel 关联表入库
