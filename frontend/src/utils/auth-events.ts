type Handler = (message: string) => void;

let unauthorizedHandler: Handler | null = null;

export function setUnauthorizedHandler(handler: Handler) {
  unauthorizedHandler = handler;
}

export function triggerUnauthorized(message: string) {
  unauthorizedHandler?.(message);
}
