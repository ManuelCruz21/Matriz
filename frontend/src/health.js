const ERROR_MESSAGE = 'Não foi possível consultar o estado do serviço.';

export async function fetchHealth({ fetchImpl = globalThis.fetch, signal } = {}) {
  try {
    const response = await fetchImpl('/api/health', {
      method: 'GET',
      headers: { Accept: 'application/json' },
      cache: 'no-store',
      signal,
    });
    const body = await response.json();

    if (response.status === 200 && body?.status === 'UP') {
      return { status: 'UP' };
    }
    if (response.status === 503 && body?.status === 'DOWN') {
      return { status: 'DOWN' };
    }
    throw new Error(ERROR_MESSAGE);
  } catch (error) {
    if (signal?.aborted) {
      throw error;
    }
    // Never surface server bodies or network diagnostics in the interface.
    throw new Error(ERROR_MESSAGE);
  }
}
