import { defineConfig } from '@playwright/test';

const headed = process.env.PW_HEADED === '1' || process.env.PW_HEADED === 'true';

export default defineConfig({
  testDir: './e2e',
  /** Headed runs use 2s pauses between each `step()` in fixtures — needs a higher ceiling. */
  timeout: headed ? 600_000 : 60_000,
  /** One browser at a time when headed so only a single test window is visible. */
  workers: headed ? 1 : undefined,
  expect: {
    timeout: 10_000,
  },
  use: {
    baseURL: 'http://localhost:5173',
    headless: !headed,
    screenshot: 'only-on-failure',
  },
  webServer: [
    {
      command: 'cd auto-ease-api && mvn spring-boot:run',
      url: 'http://localhost:8080/api/service-categories',
      timeout: 120_000,
      reuseExistingServer: true,
    },
    {
      command: 'npm run dev',
      url: 'http://localhost:5173',
      timeout: 30_000,
      reuseExistingServer: true,
    },
  ],
});
