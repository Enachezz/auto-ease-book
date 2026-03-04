import { test, expect, DELAY } from './fixtures';

const uniqueEmail = () => `e2e-${Date.now()}@test.com`;

test.describe('Authentication', () => {

  test('register as car owner and redirect to home', async ({ page, step }) => {
    const email = uniqueEmail();

    await step('New visitor opens the sign-in / sign-up page');
    await page.goto('/auth');
    await expect(page.getByText('Bine ai venit')).toBeVisible();

    await step('Visitor switches to the registration tab');
    await page.getByRole('tab', { name: 'Înregistrare' }).click();

    await step('Visitor fills out the registration form as a car owner');
    await page.getByLabel('Nume Complet').fill('E2E Test User');
    await page.locator('#signup-email').fill(email);
    await page.locator('#signup-password').fill('password123');

    await step('Visitor clicks "Create Account" to submit registration');
    await page.getByRole('button', { name: 'Creează Cont' }).click();

    await step('Checking: Car owner should be redirected to the home page', DELAY.STEP_LONG);
    await expect(page).toHaveURL('/', { timeout: 15_000 });
  });

  test('register as garage owner and redirect to home', async ({ page, step }) => {
    const email = uniqueEmail();

    await step('New visitor opens the sign-in / sign-up page');
    await page.goto('/auth');

    await step('Visitor switches to the registration tab');
    await page.getByRole('tab', { name: 'Înregistrare' }).click();

    await step('Visitor fills out the registration form as a garage owner');
    await page.getByLabel('Nume Complet').fill('Garage E2E');
    await page.locator('#signup-email').fill(email);
    await page.locator('#signup-password').fill('password123');

    await step('Visitor selects "Service / Mecanic" from the account type dropdown');
    await page.getByRole('combobox').click();
    await page.getByRole('option', { name: /Service/ }).click();

    await step('Visitor clicks "Create Account" to submit registration');
    await page.getByRole('button', { name: 'Creează Cont' }).click();

    await step('Checking: Garage owner should be redirected to the home page', DELAY.STEP_LONG);
    await expect(page).toHaveURL('/', { timeout: 15_000 });
  });

  test('login with registered account', async ({ page, step }) => {
    const email = uniqueEmail();

    await step('Setting up: Registering a fresh account to test login flow');
    await page.goto('/auth');
    await page.getByRole('tab', { name: 'Înregistrare' }).click();
    await page.getByLabel('Nume Complet').fill('Login Tester');
    await page.locator('#signup-email').fill(email);
    await page.locator('#signup-password').fill('password123');
    await page.getByRole('button', { name: 'Creează Cont' }).click();
    await expect(page).toHaveURL('/', { timeout: 15_000 });

    await step('User logs out and returns to the auth page', DELAY.STEP_LONG);
    await page.evaluate(() => localStorage.removeItem('token'));
    await page.goto('/auth');

    await step('User types their email and password to log in');
    await page.locator('#signin-email').fill(email);
    await page.locator('#signin-password').fill('password123');
    await page.getByRole('button', { name: 'Conectare' }).click();

    await step('Checking: Logged-in user should be redirected to the home page', DELAY.STEP_LONG);
    await expect(page).toHaveURL('/', { timeout: 15_000 });
  });

  test('unauthenticated user sees auth page for protected routes', async ({ page, step }) => {
    await step('An unauthenticated visitor tries to open the Settings page directly');
    await page.goto('/settings');

    await step('Checking: The app should redirect them to the auth page', DELAY.STEP_LONG);
    await expect(page).toHaveURL('/auth', { timeout: 10_000 });
  });
});
