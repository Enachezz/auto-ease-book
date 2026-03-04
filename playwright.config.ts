import { defineConfig } from '@playwright/test';

const headed = process.env.PW_HEADED === '1' || process.env.PW_HEADED === 'true';

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
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
