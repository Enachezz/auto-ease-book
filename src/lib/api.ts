const API_URL = 'http://localhost:8080/api';

const STATUS_MESSAGES: Record<number, string> = {
  400: 'Cererea nu a putut fi procesată. Verifică datele introduse.',
  401: 'Credențiale invalide sau sesiune expirată.',
  403: 'Nu ai permisiunea de a efectua această acțiune.',
  404: 'Resursa solicitată nu a fost găsită.',
  409: 'Există deja o înregistrare cu aceste date.',
  422: 'Datele introduse nu sunt valide.',
  429: 'Prea multe cereri. Încearcă din nou mai târziu.',
  500: 'Eroare internă a serverului. Încearcă din nou mai târziu.',
};

function getUserFriendlyMessage(status: number, _rawText: string): string {
  return STATUS_MESSAGES[status] || `A apărut o eroare neașteptată (cod ${status}).`;
}

async function request<T = unknown>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...((options.headers as Record<string, string>) || {}),
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_URL}${path}`, { ...options, headers });

  if (!response.ok) {
    // Log raw error for debugging but show user-friendly messages
    const text = await response.text();
    console.error(`API error [${response.status}]: ${text}`);
    
    const userMessage = getUserFriendlyMessage(response.status, text);
    throw new Error(userMessage);
  }

  if (response.status === 204) return undefined as T;
  const text = await response.text();
  return text ? JSON.parse(text) : (undefined as T);
}

export const api = {
  get: <T = unknown>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T = unknown>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body != null ? JSON.stringify(body) : undefined }),
  put: <T = unknown>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body != null ? JSON.stringify(body) : undefined }),
  delete: <T = void>(path: string) => request<T>(path, { method: 'DELETE' }),
};
