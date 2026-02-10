package cn.sztu.questioncloud.application.ai.service;

import cn.sztu.questioncloud.web.rest.v1.ai.vo.SessionVO;
import reactor.core.publisher.Flux;

public interface AgentService {
    /**
     * 测试用接口
     *
     * @param memoryId 会话记忆ID
     * @param message  用户消息
     * @return 流式响应
     */
    Flux<String> chatTest(String memoryId, String message);

    /**
     * 创建新会话
     *
     * @return 会话视图对象
     */
    SessionVO createNewSession();
}
