// Private E2E server: ephemeral loopback port, no reuse of the running application.
import { createServer } from 'vite';

const backendPort = Number(process.argv[2]);
if (!Number.isInteger(backendPort) || backendPort < 1 || backendPort > 65535) {
  throw new Error('Expected the isolated backend port');
}
const server = await createServer({
  configFile: false,
  envDir: false,
  server: {
    host: '127.0.0.1',
    port: 0,
    strictPort: true,
    proxy: { '/api': { target: `http://127.0.0.1:${backendPort}` } },
  },
});
await server.listen();
const { port } = server.httpServer.address();
console.log(`MATRIZ_E2E_URL=http://127.0.0.1:${port}`);
