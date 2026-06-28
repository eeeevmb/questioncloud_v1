const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export interface AgentSessionVO {
  sessionId: string;
  agentName: string;
  userId: string;
  title?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

interface ResultVO<T> {
  code: string;
  message: string;
  data: T;
}

export interface AgentChatMessageVO {
  type?: string;
  role?: string;
  text?: string | null;
  thinking?: string | null;
}

export interface AgentChatPayload {
  sessionId: string;
  message: string;
  agentName: string;
  context: {
    collectionIds: string[];
    selectedQuestionIds: string[];
  };
}

interface StreamCallbacks {
  onChunk: (text: string) => void;
  onDone?: () => void;
}

async function parseResultVO<T>(response: Response, fallbackMessage: string): Promise<T> {
  if (!response.ok) {
    throw new Error(`${fallbackMessage}：HTTP ${response.status}`);
  }
  const payload = (await response.json()) as ResultVO<T>;
  if (!payload || payload.code !== '200000') {
    throw new Error(payload?.message || fallbackMessage);
  }
  return payload.data;
}

export async function createAgentSession(agentName: string): Promise<AgentSessionVO> {
  const response = await fetch(`${apiBaseUrl}/api/v1/agent`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    body: JSON.stringify({ agentName })
  });
  return parseResultVO<AgentSessionVO>(response, '创建会话失败');
}

export async function fetchAgentSessions(): Promise<AgentSessionVO[]> {
  const response = await fetch(`${apiBaseUrl}/api/v1/agent`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include'
  });
  return parseResultVO<AgentSessionVO[]>(response, '获取会话列表失败');
}

export async function fetchSessionMessages(sessionId: string): Promise<AgentChatMessageVO[]> {
  const response = await fetch(`${apiBaseUrl}/api/v1/agent/sessions/${sessionId}/message`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include'
  });
  return parseResultVO<AgentChatMessageVO[]>(response, '获取会话历史失败');
}

export async function updateSessionTitle(sessionId: string, title: string): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/v1/agent/sessions/${sessionId}/title`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include',
    body: JSON.stringify({ title })
  });
  await parseResultVO<void>(response, '更新会话标题失败');
}

export async function deleteSession(sessionId: string): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/v1/agent/sessions/${sessionId}`, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json'
    },
    credentials: 'include'
  });
  await parseResultVO<void>(response, '删除会话失败');
}

export async function streamAgentChat(
  payload: AgentChatPayload,
  callbacks: StreamCallbacks,
  signal?: AbortSignal
): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/v1/agent/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream'
    },
    credentials: 'include',
    body: JSON.stringify(payload),
    signal
  });

  if (!response.ok || !response.body) {
    throw new Error(`聊天请求失败：HTTP ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });

    let separatorIndex = buffer.indexOf('\n\n');
    while (separatorIndex !== -1) {
      const chunk = buffer.slice(0, separatorIndex);
      buffer = buffer.slice(separatorIndex + 2);
      processSseEvent(chunk, callbacks);
      separatorIndex = buffer.indexOf('\n\n');
    }
  }
}

function processSseEvent(rawEvent: string, callbacks: StreamCallbacks) {
  const normalized = rawEvent.replace(/\r/g, '');
  const lines = normalized.split('\n');
  let eventName = 'message';
  const dataLines: string[] = [];

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim();
      continue;
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart());
    }
  }

  const data = dataLines.join('\n');
  if (eventName === 'done' || data === '[DONE]') {
    callbacks.onDone?.();
    return;
  }
  if (!data) {
    return;
  }
  callbacks.onChunk(data);
}
