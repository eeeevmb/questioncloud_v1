export type MessageRole = 'user' | 'assistant' | 'system';

export type ResultType = 'explain' | 'search' | 'generate' | 'general';

export interface MessageItem {
  id: string;
  role: MessageRole;
  content: string;
  status?: 'pending' | 'streaming' | 'done' | 'error';
  resultType?: ResultType;
}

export interface QuestionPickerQueryState {
  pageNum: number;
  pageSize: number;
  keyword: string;
  typeCode: string;
  levelMin?: number;
  levelMax?: number;
}

export interface AgentQuestionTypeOption {
  code: string;
  label: string;
}

export interface CreateQuestionFormState {
  questionType: string;
  title: string;
  stem: string;
  difficulty: number | null;
  options: { key: string; content: string }[];
  choiceCorrect: string[];
  judgeAnswer: 'T' | 'F';
  answer: string;
  solution: string;
}

export interface AiCreateFormState {
  topic: string;
  questionType: string;
  scenario: string;
  requirements: string;
}

export interface AiDraftFormState {
  typeCode: string;
  title: string;
  difficulty: number | null;
  stem: string;
  options: { key: string; content: string }[];
  correctOptions: string[];
  judgeAnswer: 'T' | 'F' | '';
  answer: string;
  solution: string;
  assumptions: string;
}