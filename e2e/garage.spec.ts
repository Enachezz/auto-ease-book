import { test, expect, DELAY } from './fixtures';
import {
  registerViaApi,
  setTokenInBrowser,
  addCarViaApi,
  createJobRequestViaApi,
  createGarageViaApi,
  approveGarageViaApi,
  registerAdminViaApi,
  submitQuoteViaApi,
} from './helpers';

test.describe('Garage Management', () => {

  test('register as garage and create garage profile', async ({ page, step }) => {
    await step('Setting up: Creating a new garage owner account via API');
    const garage = await registerViaApi('GARAGE', 'GarageProfileTester');
    await setTokenInBrowser(page, garage.token);

    await step('Garage owner opens the Garage Management page', DELAY.STEP_LONG);
    await page.goto('/garage');

    await step('Waiting for the garage page to fully load');
    const profileHeading = page.getByRole('heading', { name: 'Garage Profile' });
    const setupText = page.getByRole('heading', { name: 'Set up your garage profile' });
    await expect(profileHeading.or(setupText)).toBeVisible({ timeout: 10_000 });

    const setupBtn = page.getByRole('button', { name: 'Create Garage Profile' });
    if (await setupBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await setupBtn.click();
    }

    await step('Garage owner fills out the business profile form');
    await page.getByLabel('Business Name').fill('AutoFix E2E Garage');
    await page.getByLabel('Description').fill('Premium auto repair services');
    await page.getByLabel('Phone').fill('+40741000000');
    await page.getByLabel('Postal Code').fill('400001');
    await page.getByLabel('Address').fill('Str. Testului 42');
    await page.getByLabel('City').fill('Cluj-Napoca');
    await page.getByLabel('State').fill('Cluj');

    await step('Garage owner selects the services they offer');
    await page.getByRole('button', { name: 'Oil Change' }).click();
    await page.getByRole('button', { name: 'Brake Service' }).click();

    await step('Garage owner clicks "Create Profile" to save');
    await page.getByRole('button', { name: 'Create Profile' }).click();

    await step('Checking: A success notification confirms the profile was created', DELAY.STEP_LONG);
    await expect(page.getByRole('status').filter({ hasText: 'Success' }).first()).toBeVisible({ timeout: 10_000 });
  });

  test('update garage profile', async ({ page, step }) => {
    await step('Setting up: Creating a garage owner account via API');
    const garage = await registerViaApi('GARAGE', 'GarageUpdateTester');
    await setTokenInBrowser(page, garage.token);

    await step('Garage owner creates an initial profile through the UI');
    await page.goto('/garage');
    const profileHeading = page.getByRole('heading', { name: 'Garage Profile' });
    const setupText = page.getByRole('heading', { name: 'Set up your garage profile' });
    await expect(profileHeading.or(setupText)).toBeVisible({ timeout: 10_000 });

    const setupBtn = page.getByRole('button', { name: 'Create Garage Profile' });
    if (await setupBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await setupBtn.click();
    }

    await page.getByLabel('Business Name').fill('Old Garage Name');
    await page.getByLabel('Phone').fill('+40741111111');
    await page.getByLabel('Postal Code').fill('100001');
    await page.getByLabel('Address').fill('Str. Veche 1');
    await page.getByLabel('City').fill('Bucuresti');
    await page.getByLabel('State').fill('Ilfov');
    await page.getByRole('button', { name: 'Create Profile' }).click();
    await expect(page.getByRole('status').filter({ hasText: 'Success' }).first()).toBeVisible({ timeout: 10_000 });

    await step('Garage owner reloads the page to see their saved profile', DELAY.STEP_LONG);
    await page.goto('/garage');
    await expect(page.getByRole('heading', { name: 'Garage Profile' })).toBeVisible({ timeout: 10_000 });

    await step('Garage owner updates the description field');
    await page.getByLabel('Description').fill('Updated premium auto repair');

    await step('Garage owner clicks "Update Profile" to save changes');
    await page.getByRole('button', { name: 'Update Profile' }).click();

    await step('Checking: A success notification confirms the profile was updated', DELAY.STEP_LONG);
    await expect(page.getByRole('status').filter({ hasText: 'Success' }).first()).toBeVisible({ timeout: 10_000 });
  });

  test('garage sees open job requests', async ({ page, step }) => {
    await step('Setting up: A car owner creates a job request via API');
    const owner = await registerViaApi('CAR_OWNER', 'JobCreator');
    const car = await addCarViaApi(owner.token);
    await createJobRequestViaApi(owner.token, car.id);

    await step('Setting up: Creating a garage owner account via API');
    const garage = await registerViaApi('GARAGE', 'JobViewer');
    await setTokenInBrowser(page, garage.token);

    await step('Garage owner creates a profile so they can view jobs', DELAY.STEP_LONG);
    await page.goto('/garage');
    const profileHeading = page.getByRole('heading', { name: 'Garage Profile' });
    const setupText = page.getByRole('heading', { name: 'Set up your garage profile' });
    await expect(profileHeading.or(setupText)).toBeVisible({ timeout: 10_000 });

    const setupBtn = page.getByRole('button', { name: 'Create Garage Profile' });
    if (await setupBtn.isVisible({ timeout: 2_000 }).catch(() => false)) {
      await setupBtn.click();
    }

    await page.getByLabel('Business Name').fill('Job Viewer Garage');
    await page.getByLabel('Phone').fill('+40741222222');
    await page.getByLabel('Postal Code').fill('200001');
    await page.getByLabel('Address').fill('Str. Service 10');
    await page.getByLabel('City').fill('Iasi');
    await page.getByLabel('State').fill('Iasi');
    await page.getByRole('button', { name: 'Create Profile' }).click();
    await expect(page.getByRole('status').filter({ hasText: 'Success' }).first()).toBeVisible({ timeout: 10_000 });

    await step('Garage owner switches to the "Jobs" tab to browse open requests', DELAY.STEP_LONG);
    await page.goto('/garage');
    await expect(page.getByRole('heading', { name: /Gestionare Service/ })).toBeVisible({ timeout: 10_000 });
    await page.getByRole('tab', { name: /Locuri de munc/ }).click();

    await step('Checking: The car owner\'s oil change request should be visible');
    await expect(page.getByText('E2E Oil Change Request').first()).toBeVisible({ timeout: 10_000 });
  });

  test('garage views submitted quotes in My Quotes tab', async ({ page, step }) => {
    await step('Setting up: A car owner creates a job request');
    const owner = await registerViaApi('CAR_OWNER', 'QuoteTabOwner');
    const car = await addCarViaApi(owner.token);
    const job = await createJobRequestViaApi(owner.token, car.id);

    await step('Setting up: A garage submits a quote for that job via API');
    const garage = await registerViaApi('GARAGE', 'QuoteTabGarage');
    const garageProfile = await createGarageViaApi(garage.token);
    const admin = await registerAdminViaApi();
    await approveGarageViaApi(garageProfile.id, admin.token);
    await submitQuoteViaApi(garage.token, job.id, 350, 'Premium oil change package');

    await step('Garage owner logs into the browser', DELAY.STEP_LONG);
    await setTokenInBrowser(page, garage.token);

    await step('Garage owner navigates to their management page', DELAY.STEP_LONG);
    await page.goto('/garage');
    await expect(page.getByRole('heading', { name: /Gestionare Service/ })).toBeVisible({ timeout: 10_000 });

    await step('Garage owner clicks the "Ofertele mele" (My Quotes) tab');
    await page.getByRole('tab', { name: /Ofertele mele/ }).click();

    await step('Checking: The submitted quote should be visible with the correct price', DELAY.STEP_LONG);
    await expect(page.getByText('350')).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText('PENDING')).toBeVisible({ timeout: 5_000 });
  });
});
