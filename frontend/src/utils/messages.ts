export type MessageType = 'info' | 'success' | 'error';
import { ElMessage } from 'element-plus';

function emit(type: MessageType, message: string) {
  if (!message) {
    return;
  }
  ElMessage({
    type,
    message,
    showClose: true
  });
}

export function showError(message: string) {
  emit('error', message);
}

export function showSuccess(message: string) {
  emit('success', message);
}

export function showInfo(message: string) {
  emit('info', message);
}
