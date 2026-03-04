import { test, expect, DELAY } from './fixtures';
import { registerViaApi, setTokenInBrowser } from './helpers';

test.describe('Car Owner - Vehicle Management', () => {

  test('add a car after registration', async ({ page, step }) => {
    await step('Setting up: Creating a new car owner account via API');
    const owner = await registerViaApi('CAR_OWNER', 'Car Owner E2E');
    await setTokenInBrowser(page, owner.token);

    await step('Car owner navigates to the My Cars page', DELAY.STEP_LONG);
    await page.goto('/my-cars');
    await expect(page.getByText('My Cars')).toBeVisible({ timeout: 10_000 });

    await step('Car owner opens the "Add Car" dialog');
    await page.getByRole('button', { name: 'Adaugă Mașină' }).click();

    await step('Car owner selects Toyota as the make');
    await page.getByText('Select make').click();
    const modelsResponse = page.waitForResponse(
      (r) =>
        r.url().includes('/car-makes/') &&
        r.url().includes('/models') &&
        r.status() === 200,
      { timeout: 15_000 }
    );
    await page.getByRole('option', { name: 'Toyota' }).click();
    await modelsResponse;

    await step('Car owner selects Corolla as the model');
    await page.getByText('Select model').click();
    await page.getByRole('option', { name: 'Corolla' }).click();

    await step('Car owner clicks "Add Car" to save the new vehicle');
    await page.getByRole('button', { name: 'Add Car' }).click();

    await step('Checking: A success notification confirms the car was added', DELAY.STEP_LONG);
    await expect(page.getByRole('status').filter({ hasText: 'Success' }).first()).toBeVisible({ timeout: 10_000 });
  });

  test('car appears in the list after adding', async ({ page, step }) => {
    await step('Setting up: Creating a car owner and logging in');
    const owner = await registerViaApi('CAR_OWNER', 'List Tester');
    await setTokenInBrowser(page, owner.token);

    await step('Car owner opens My Cars and adds a Toyota Camry', DELAY.STEP_LONG);
    await page.goto('/my-cars');
    await expect(page.getByText('My Cars')).toBeVisible({ timeout: 10_000 });
    await page.getByRole('button', { name: 'Adaugă Mașină' }).click();

    await page.getByText('Select make').click();
    const modelsResponse = page.waitForResponse(
      (r) => r.url().includes('/car-makes/') && r.url().includes('/models') && r.status() === 200,
      { timeout: 15_000 }
    );
    await page.getByRole('option', { name: 'Toyota' }).click();
    await modelsResponse;
    await page.getByText('Select model').click();
    await page.getByRole('option', { name: 'Camry' }).click();
    await page.getByRole('button', { name: 'Add Car' }).click();
    await expect(page.getByRole('status').filter({ hasText: 'Success' }).first()).toBeVisible({ timeout: 10_000 });

    await step('Car owner reloads the page to verify the car persists', DELAY.STEP_LONG);
    await page.goto('/my-cars');
    await expect(page.getByText('Toyota Camry').first()).toBeVisible({ timeout: 10_000 });
  });

  test('edit an existing car', async ({ page, step }) => {
    await step('Setting up: Creating a car owner and logging in');
    const owner = await registerViaApi('CAR_OWNER', 'Edit Tester');
    await setTokenInBrowser(page, owner.token);

    await step('Car owner adds a Honda Civic through the UI');
    await page.goto('/my-cars');
    await expect(page.getByText('My Cars')).toBeVisible({ timeout: 10_000 });
    await page.getByRole('button', { name: 'Adaugă Mașină' }).click();
    await page.getByText('Select make').click();
    const modelsResponse = page.waitForResponse(
      (r) => r.url().includes('/car-makes/') && r.url().includes('/models') && r.status() === 200,
      { timeout: 15_000 }
    );
    await page.getByRole('option', { name: 'Honda' }).click();
    await modelsResponse;
    await page.getByText('Select model').click();
    await page.getByRole('option', { name: 'Civic' }).click();
    await page.getByRole('button', { name: 'Add Car' }).click();
    await expect(page.getByRole('status').filter({ hasText: 'Success' }).first()).toBeVisible({ timeout: 10_000 });

    await step('Car owner reloads and selects the Honda Civic from the list', DELAY.STEP_LONG);
    await page.goto('/my-cars');
    await expect(page.getByText('Honda Civic').first()).toBeVisible({ timeout: 10_000 });

    await step('Car owner clicks on the car to view its details');
    await page.getByText('Honda Civic').first().click();

    await step('Car owner opens the edit dialog');
    await page.getByRole('button', { name: 'Edit', exact: true }).click();

    await step('Car owner updates the mileage to 55,000 km');
    await page.getByLabel('Kilometraj Curent (Optional)').fill('55000');

    await step('Car owner saves the changes');
    await page.getByRole('button', { name: 'Salvează Modificările' }).click();

    await step('Checking: A success notification confirms the update', DELAY.STEP_LONG);
    await expect(page.getByRole('status').filter({ hasText: 'Succes' }).first()).toBeVisible({ timeout: 10_000 });
  });
});
