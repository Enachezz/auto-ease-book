import { test, expect, DELAY } from './fixtures';
import { registerViaApi, setTokenInBrowser } from './helpers';

test.describe('Settings', () => {

  test('update phone number', async ({ page, step }) => {
    await step('Setting up: Creating a car owner account via API');
    const owner = await registerViaApi('CAR_OWNER', 'SettingsTester');
    await setTokenInBrowser(page, owner.token);

    await step('Waiting for the app to fully load with the user\'s session', DELAY.STEP_LONG);
    await expect(page.getByText('SettingsTester')).toBeVisible({ timeout: 15_000 });

    await step('Car owner navigates to the Settings page', DELAY.STEP_LONG);
    await page.evaluate(() => {
      window.history.pushState({}, '', '/settings');
      window.dispatchEvent(new PopStateEvent('popstate'));
    });
    await page.waitForTimeout(1000);
    await expect(page.getByRole('heading', { name: 'Setări' })).toBeVisible({ timeout: 15_000 });

    await step('Car owner types their phone number into the settings form');
    await page.getByPlaceholder('+40 XXX XXX XXX').fill('+40712345678');

    await step('Car owner clicks "Salvează" to save the phone number');
    await page.getByRole('button', { name: 'Salvează' }).click();

    await step('Checking: A notification confirms the phone number was saved', DELAY.STEP_LONG);
    await expect(page.getByRole('status').filter({ hasText: 'Telefon actualizat' }).first()).toBeVisible({ timeout: 10_000 });
  });
});
