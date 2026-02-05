export type MessageType = 'info' | 'success' | 'error';

function emit(type: MessageType, message: string) {
  if (!message) {
    return;
  }
  const prefix = type === 'error' ? '错误' : type === 'success' ? '成功' : '提示';
  window.alert(`${prefix}: ${message}`);
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
