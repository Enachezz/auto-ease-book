import { test as base, expect } from '@playwright/test';
import type { Page, Locator } from '@playwright/test';

export const DELAY = {
  INTERACTION: 500,
  ACTION: 1000,
  NAVIGATION: 2000,
  STEP: 1500,
  STEP_LONG: 2500,
} as const;

const isHeadedWithDelay =
  process.env.PW_HEADED === '1' || process.env.PW_HEADED === 'true';

const pause = (ms: number) => new Promise((r) => setTimeout(r, ms));

const FAST_ACTIONS: readonly string[] = ['fill', 'check', 'uncheck', 'selectOption', 'hover'];
const SLOW_ACTIONS: readonly string[] = ['click', 'dblclick', 'press', 'tap'];
const ALL_ACTIONS = [...FAST_ACTIONS, ...SLOW_ACTIONS];

const LOCATOR_METHODS = [
  'locator', 'getByRole', 'getByText', 'getByLabel',
  'getByPlaceholder', 'getByAltText', 'getByTitle', 'getByTestId',
  'first', 'last', 'nth', 'filter', 'and', 'or',
] as const;

const E2E_OVERLAY_SCRIPT = () => {
  if ((window as unknown as { __e2eOverlay?: boolean }).__e2eOverlay) return;
  (window as unknown as { __e2eOverlay: boolean }).__e2eOverlay = true;
  const init = () => {
    if (document.getElementById('e2e-step-overlay')) return;
    const el = document.createElement('div');
    el.id = 'e2e-step-overlay';
    el.setAttribute('aria-hidden', 'true');
    el.setAttribute('role', 'presentation');
    el.setAttribute(
      'style',
      'position:fixed;top:0;left:50%;transform:translateX(-50%);z-index:2147483647;' +
        'background:rgba(255,255,200,0.95);color:#333;padding:10px 24px;border-radius:0;' +
        'border:3px ridge orange;' +
        'font-family:system-ui,sans-serif;font-size:14px;font-weight:600;pointer-events:none;' +
        'max-width:90vw;text-align:center;' +
        'text-shadow:1px 1px 2px rgba(0,0,0,0.3);' +
        'transition:opacity 0.25s ease;'
    );
    el.textContent = 'E2E test running\u2026';
    document.body?.appendChild(el);
    (window as unknown as { __e2eShowStep?: (msg: string) => void }).__e2eShowStep = (msg: string) => {
      el.textContent = msg;
    };
  };
  if (document.body) init();
  else document.addEventListener('DOMContentLoaded', init);
};

function showOverlay(page: Page, message: string): Promise<void> {
  return page.evaluate(
    (msg: string) => {
      const show = (window as unknown as { __e2eShowStep?: (m: string) => void }).__e2eShowStep;
      if (show) show(msg);
    },
    message
  );
}

function wrapLocator(loc: Locator, page: Page): Locator {
  return new Proxy(loc, {
    get(target, prop: string) {
      const v = (target as Record<string, unknown>)[prop];
      if (typeof v !== 'function') return v;
      if (ALL_ACTIONS.includes(prop)) {
        const delayMs = FAST_ACTIONS.includes(prop) ? DELAY.INTERACTION : DELAY.ACTION;
        return async (...args: unknown[]) => {
          await (v as (...a: unknown[]) => Promise<unknown>).apply(target, args);
          await pause(delayMs);
        };
      }
      if (LOCATOR_METHODS.includes(prop as (typeof LOCATOR_METHODS)[number])) {
        return (...args: unknown[]) => {
          const result = (v as (...a: unknown[]) => Locator).apply(target, args);
          return result != null ? wrapLocator(result, page) : result;
        };
      }
      return v;
    },
  }) as Locator;
}

function wrapPageWithSlowInteractions(page: Page): Page {
  page.addInitScript(E2E_OVERLAY_SCRIPT);

  const locatorMethods = [
    'locator', 'getByRole', 'getByText', 'getByLabel',
    'getByPlaceholder', 'getByAltText', 'getByTitle', 'getByTestId',
  ] as const;
  for (const method of locatorMethods) {
    const original = page[method].bind(page);
    (page as Record<string, unknown>)[method] = (...args: unknown[]) =>
      wrapLocator(original(...(args as Parameters<Page['getByRole']>)), page);
  }

  const originalGoto = page.goto.bind(page);
  (page as Record<string, unknown>).goto = async (...args: unknown[]) => {
    const result = await (originalGoto as (...a: unknown[]) => Promise<unknown>)(...args);
    await pause(DELAY.NAVIGATION);
    return result;
  };

  return page;
}

type StepFn = (label: string, pauseMs?: number) => Promise<void>;

export const test = base.extend<{ page: Page; step: StepFn }>({
  page: async ({ page }, use) => {
    if (isHeadedWithDelay) {
      wrapPageWithSlowInteractions(page);
    }
    await use(page);
  },
  step: async ({ page }, use) => {
    const stepFn: StepFn = async (label: string, pauseMs?: number) => {
      if (isHeadedWithDelay) {
        await showOverlay(page, label);
        await pause(pauseMs ?? DELAY.STEP);
      }
    };
    await use(stepFn);
  },
});

export { expect };
