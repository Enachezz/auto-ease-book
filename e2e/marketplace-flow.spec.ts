import { test, expect, DELAY } from './fixtures';
import {
  registerViaApi,
  setTokenInBrowser,
  addCarViaApi,
  createJobRequestViaApi,
  createGarageViaApi,
  approveGarageViaApi,
  registerAdminViaApi,
  getBookingsViaApi,
} from './helpers';

test.describe('Full Marketplace Flow', () => {

  test('owner creates request, garage quotes, owner accepts, booking and review', async ({ page, step }) => {
    test.setTimeout(300_000);

    await step('Setting up: Registering a new car owner via API');
    const owner = await registerViaApi('CAR_OWNER', 'Marketplace Owner');

    await step('Setting up: Adding a Toyota Corolla to the car owner\'s account');
    const car = await addCarViaApi(owner.token);

    await step('Setting up: Car owner creates an oil change job request');
    await createJobRequestViaApi(owner.token, car.id);

    await step('Setting up: Registering a garage owner and creating their profile');
    const garage = await registerViaApi('GARAGE', 'Marketplace Garage');
    const garageProfile = await createGarageViaApi(garage.token);

    await step('Setting up: An admin approves the garage so it can submit quotes');
    const admin = await registerAdminViaApi();
    await approveGarageViaApi(garageProfile.id, admin.token);

    // --- Garage submits quote via UI ---
    await step('Switching user: Now acting as the garage owner', DELAY.STEP_LONG);
    await setTokenInBrowser(page, garage.token);

    await step('Garage owner navigates to their management dashboard', DELAY.STEP_LONG);
    await page.goto('/garage');
    await expect(page.getByRole('heading', { name: 'Gestionare Service' })).toBeVisible({ timeout: 10_000 });

    await step('Garage owner switches to the "Jobs" tab to find open requests');
    await page.getByRole('tab', { name: 'Locuri de muncă' }).click();

    await step('Checking: The oil change job request from the car owner is visible');
    await expect(page.getByText('E2E Oil Change Request').first()).toBeVisible({ timeout: 10_000 });

    await step('Garage owner fills in the quote price: $250');
    await page.getByLabel('Price ($)').first().fill('250');

    await step('Garage owner estimates the duration: 2 hours');
    await page.getByLabel('Duration').first().fill('2 hours');

    await step('Garage owner describes the work: full synthetic oil change with OEM filter');
    await page.getByLabel('Details').first().fill(
      'Full synthetic oil change with OEM filter'
    );

    await step('Garage owner submits the quote');
    await page.locator('button:not([disabled])').filter({ hasText: 'Submit Quote' }).first().click();

    await step('Checking: A success notification confirms the quote was sent', DELAY.STEP_LONG);
    await expect(page.getByRole('status').filter({ hasText: 'Success' }).first()).toBeVisible({ timeout: 10_000 });

    // --- Owner accepts quote via UI ---
    await step('Switching user: Now acting as the car owner', DELAY.STEP_LONG);
    await setTokenInBrowser(page, owner.token);

    await step('Car owner navigates to My Requests to review quotes', DELAY.STEP_LONG);
    await page.goto('/my-requests');
    await expect(page.getByText('Cererile Mele de Serviciu')).toBeVisible({ timeout: 10_000 });

    await step('Checking: The car owner\'s oil change request is listed');
    await expect(page.getByText('E2E Oil Change Request')).toBeVisible({ timeout: 10_000 });

    await step('Car owner clicks "View Quotes" to see the garage\'s offer');
    await page.getByRole('button', { name: /View Quotes/ }).first().click();

    await step('Checking: The quote shows $250 from the marketplace garage');
    await expect(page.getByText('250')).toBeVisible({ timeout: 10_000 });

    await step('Car owner clicks "Accept Quote" to book the service');
    await page.getByRole('button', { name: 'Accept Quote' }).click();

    await step('Checking: A success notification confirms the quote was accepted', DELAY.STEP_LONG);
    await expect(page.getByRole('status').filter({ hasText: 'Success' }).first()).toBeVisible({ timeout: 10_000 });

    await step('Car owner returns to My Requests to verify the new status', DELAY.STEP_LONG);
    await page.goto('/my-requests');
    await expect(page.getByText('BOOKED')).toBeVisible({ timeout: 10_000 });

    // --- Verify booking via API ---
    await step('Behind the scenes: Verifying the booking was created in the system');
    const bookings = await getBookingsViaApi(owner.token);
    expect(bookings.length).toBeGreaterThanOrEqual(1);
    expect(bookings[0].status).toBe('CONFIRMED');
  });
});
