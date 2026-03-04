import { test, expect, DELAY } from './fixtures';
import { registerViaApi, setTokenInBrowser, addCarViaApi, createJobRequestViaApi } from './helpers';

test.describe('Car Owner - Job Requests', () => {

  test('create a job request via UI', async ({ page, step }) => {
    await step('Setting up: Creating a car owner with a registered car');
    const owner = await registerViaApi('CAR_OWNER', 'JobRequestTester');
    const car = await addCarViaApi(owner.token);
    await setTokenInBrowser(page, owner.token);

    await step('Car owner opens the "Request Service" page', DELAY.STEP_LONG);
    await page.goto('/request-service');
    await expect(page.getByRole('heading', { name: 'Solicită Service' })).toBeVisible({ timeout: 10_000 });

    await step('Car owner picks the maintenance service category');
    await page.getByText('Întreținere și ITP').click();

    await step('The service details form appears — car owner starts filling it out', DELAY.STEP_LONG);
    await expect(page.getByRole('heading', { name: 'Detalii Service' })).toBeVisible({ timeout: 10_000 });

    await step('Car owner selects their car from the dropdown');
    await page.getByText('Alege mașina').click();
    await page.getByRole('option').first().click();

    await step('Car owner describes the problem in detail');
    await page.getByLabel('Descrierea problemei *').fill(
      'Full oil change and filter replacement needed for E2E test'
    );

    await step('Car owner sets urgency to "Urgent"');
    await page.getByText('Urgent', { exact: true }).click();

    await step('Car owner types their preferred location');
    await page.getByPlaceholder('Ex: București, Sector 1').fill('București, Sector 3');

    await step('Car owner submits the service request');
    await page.getByRole('button', { name: 'Trimite Cererea' }).click();

    await step('Checking: Car owner should be redirected to My Requests', DELAY.STEP_LONG);
    await expect(page).toHaveURL('/my-requests', { timeout: 15_000 });

    await step('Checking: The new request appears in the list');
    await expect(page.getByText('Întreținere și ITP')).toBeVisible({ timeout: 10_000 });
  });

  test('edit a job request', async ({ page, step }) => {
    await step('Setting up: Creating a car owner with a car and a job request');
    const owner = await registerViaApi('CAR_OWNER', 'EditJobTester');
    const car = await addCarViaApi(owner.token);
    const job = await createJobRequestViaApi(owner.token, car.id);
    await setTokenInBrowser(page, owner.token);

    await step('Car owner opens My Requests to see their existing job', DELAY.STEP_LONG);
    await page.goto('/my-requests');
    await expect(page.getByText('E2E Oil Change Request').first()).toBeVisible({ timeout: 10_000 });

    await step('Car owner opens the actions menu on their job request');
    await page.locator('main').locator('button[aria-haspopup="menu"]').first().click();

    await step('Car owner clicks "Modifică" to edit the request');
    await page.getByRole('menuitem', { name: /Modifică/ }).click();

    await step('The edit form loads with the existing data', DELAY.STEP_LONG);
    await expect(page.getByRole('heading', { name: 'Modifică Cererea' })).toBeVisible({ timeout: 10_000 });

    await step('Car owner updates the problem description');
    await page.getByLabel('Descrierea problemei *').fill(
      'Updated: Need full synthetic oil + premium filter'
    );

    await step('Car owner changes urgency to "Urgent"');
    await page.getByText('Urgent', { exact: true }).click();

    await step('Car owner updates the location');
    await page.getByLabel('Locația (oraș, județ)').fill('Cluj-Napoca, Cluj');

    await step('Car owner saves the changes');
    await page.getByRole('button', { name: 'Salvează Modificările' }).click();

    await step('Checking: Car owner is redirected back to My Requests', DELAY.STEP_LONG);
    await expect(page).toHaveURL('/my-requests', { timeout: 15_000 });
  });

  test('delete a job request', async ({ page, step }) => {
    await step('Setting up: Creating a car owner with a car');
    const owner = await registerViaApi('CAR_OWNER', 'DeleteTester');
    const car = await addCarViaApi(owner.token);
    await setTokenInBrowser(page, owner.token);

    await step('Car owner creates a brake repair request through the UI');
    await page.goto('/request-service');
    await expect(page.getByRole('heading', { name: 'Solicită Service' })).toBeVisible({ timeout: 10_000 });
    await page.getByText('Reparații Frâne').click();
    await expect(page.getByRole('heading', { name: 'Detalii Service' })).toBeVisible({ timeout: 10_000 });
    await page.getByText('Alege mașina').click();
    await page.getByRole('option').first().click();
    await page.getByLabel('Descrierea problemei *').fill('Brake pads replacement for E2E');
    await page.getByRole('button', { name: 'Trimite Cererea' }).click();
    await expect(page).toHaveURL('/my-requests', { timeout: 15_000 });
    await expect(page.getByText('Reparații Frâne')).toBeVisible({ timeout: 10_000 });

    await step('Car owner opens the actions menu on the brake repair request', DELAY.STEP_LONG);
    await page.locator('main').locator('button[aria-haspopup="menu"]').first().click();

    await step('Car owner selects "Șterge" from the dropdown');
    await page.getByRole('menuitem', { name: /Șterge/ }).click();

    await step('Car owner confirms deletion in the alert dialog');
    await page.locator('[role="alertdialog"]').getByRole('button', { name: /Șterge/ }).click();

    await step('Checking: A success notification confirms the request was deleted', DELAY.STEP_LONG);
    await expect(page.getByRole('status').filter({ hasText: 'Succes' }).first()).toBeVisible({ timeout: 10_000 });
  });
});
