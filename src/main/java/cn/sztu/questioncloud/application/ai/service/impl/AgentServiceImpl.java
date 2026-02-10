package cn.sztu.questioncloud.application.ai.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.ai.port.CollectionAssistantChatPort;
import cn.sztu.questioncloud.application.ai.service.AgentService;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.SessionVO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AgentServiceImpl implements AgentService {
    private final CollectionAssistantChatPort collectionAssistantChatPort;

    public AgentServiceImpl(CollectionAssistantChatPort collectionAssistantChatPort) {
        this.collectionAssistantChatPort = collectionAssistantChatPort;
    }

    /**
     * 测试用接口
     * @param message 用户消息
     * @return 流式响应
     */
    @Override
    public Flux<String> chatTest(String memoryId, String message) {
        return collectionAssistantChatPort.chat(memoryId, message);
    }

    /**
     * 创建新会话
     *
     * @return 会话视图对象
     */
    @Override
    public SessionVO createNewSession() {
        return SessionVO.builder()
                .memoryId(HutoolSnowflakeIdGenerator.generateId())
                .userId(StpUtil.getLoginIdAsLong())
                .build();
    }

}
