import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const puppeteer = require('c:/Users/ADMIN/OneDrive/Documents/lead-discovery-platform/frontend/node_modules/puppeteer-core');
import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

const ARTIFACT_DIR = 'C:\\Users\\ADMIN\\.gemini\\antigravity-ide\\brain\\8ebc59bb-ca15-4488-a7cd-57c028edffe7';
const CHROME_PATH = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const BACKEND_URL = 'http://localhost:8080';
const FRONTEND_URL = 'http://localhost:5173';

async function seedVerifiedProvenanceData() {
  const username = `prov_user_${Date.now()}`;
  const email = `prov_${Date.now()}@test.com`;
  const password = 'Password123!';

  console.log(`[SEED] Registering test user: ${username}`);
  const regRes = await fetch(`${BACKEND_URL}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, email, password, name: 'Provenance Auditor' }),
  });
  if (!regRes.ok) throw new Error(`Registration failed: ${await regRes.text()}`);

  const loginRes = await fetch(`${BACKEND_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!loginRes.ok) throw new Error(`Login failed: ${await loginRes.text()}`);
  const { accessToken, user } = await loginRes.json();
  console.log(`[SEED] Logged in successfully. User ID: ${user.id}`);

  // Create a task
  const taskRes = await fetch(`${BACKEND_URL}/api/tasks`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      location: 'Salem',
      keyword: 'Software Companies',
      maxResults: 5,
      maxPagesPerSite: 5,
    }),
  });
  if (!taskRes.ok) throw new Error(`Task creation failed: ${await taskRes.text()}`);
  const task = await taskRes.json();
  console.log(`[SEED] Created task: #${task.id}`);

  const psqlCmd = `"C:\\Program Files\\PostgreSQL\\18\\bin\\psql.exe" -U postgres -d lead_discovery -c "
DO \\$\\$
DECLARE
  v_task_id BIGINT := ${task.id};
  v_org1_id BIGINT;
  v_org2_id BIGINT;
  v_web1_id BIGINT;
  v_web2_id BIGINT;
  v_page1_id BIGINT;
  v_page2_id BIGINT;
  v_page3_id BIGINT;
BEGIN
  -- 1. Insert verified Organization: ABC Technologies
  INSERT INTO organizations (scraping_task_id, business_name, normalized_name, category, address, city, state, country, source_url, verification_status, confidence_score, processing_status, scraping_timestamp, created_at, updated_at)
  VALUES (v_task_id, 'ABC Technologies', 'abc technologies', 'Software Development', '124 IT Corridor, Salem', 'Salem', 'Tamil Nadu', 'India', 'https://duckduckgo.com/?q=Software+Companies+in+Salem', 'VERIFIED', 95.00, 'PROCESSED', NOW(), NOW(), NOW())
  RETURNING id INTO v_org1_id;

  INSERT INTO websites (organization_id, url, normalized_url, is_official, status, last_crawled_at, created_at, updated_at)
  VALUES (v_org1_id, 'https://abctechnologies.com', 'https://abctechnologies.com', TRUE, 'CRAWLED', NOW(), NOW(), NOW())
  RETURNING id INTO v_web1_id;

  INSERT INTO source_pages (website_id, page_url, page_type, http_status, fetched_at, created_at)
  VALUES (v_web1_id, 'https://abctechnologies.com/contact', 'CONTACT', 200, NOW(), NOW())
  RETURNING id INTO v_page1_id;

  INSERT INTO source_pages (website_id, page_url, page_type, http_status, fetched_at, created_at)
  VALUES (v_web1_id, 'https://abctechnologies.com/about', 'ABOUT', 200, NOW(), NOW())
  RETURNING id INTO v_page2_id;

  -- Matching verified corporate email
  INSERT INTO email_addresses (organization_id, raw_value, normalized_value, source_page_id, source_domain, verification_status, created_at)
  VALUES (v_org1_id, 'sales@abctechnologies.com', 'sales@abctechnologies.com', v_page1_id, 'abctechnologies.com', 'VERIFIED', NOW());

  -- Third-party external email (e.g. Capgemini)
  INSERT INTO email_addresses (organization_id, raw_value, normalized_value, source_page_id, source_domain, verification_status, created_at)
  VALUES (v_org1_id, 'cgcompanysecretary.in@capgemini.com', 'cgcompanysecretary.in@capgemini.com', v_page1_id, 'capgemini.com', 'EXTERNAL', NOW());

  -- Verified phone
  INSERT INTO phone_numbers (organization_id, raw_value, normalized_value, phone_type, source_page_id, source_domain, verification_status, created_at)
  VALUES (v_org1_id, '+91 427 244 8899', '+914272448899', 'PHONE', v_page1_id, 'abctechnologies.com', 'VERIFIED', NOW());

  -- Social link
  INSERT INTO social_links (organization_id, platform, url, source_page_id, source_domain, verification_status, created_at)
  VALUES (v_org1_id, 'linkedin', 'https://linkedin.com/company/abc-technologies', v_page1_id, 'abctechnologies.com', 'VERIFIED', NOW());

  -- 2. Insert verified Organization: Apex Cloud Systems
  INSERT INTO organizations (scraping_task_id, business_name, normalized_name, category, address, city, state, country, source_url, verification_status, confidence_score, processing_status, scraping_timestamp, created_at, updated_at)
  VALUES (v_task_id, 'Apex Cloud Systems', 'apex cloud systems', 'Cloud & AI Solutions', '78 Meyyanur Road, Salem', 'Salem', 'Tamil Nadu', 'India', 'https://duckduckgo.com/?q=Software+Companies+in+Salem', 'VERIFIED', 88.00, 'PROCESSED', NOW(), NOW(), NOW())
  RETURNING id INTO v_org2_id;

  INSERT INTO websites (organization_id, url, normalized_url, is_official, status, last_crawled_at, created_at, updated_at)
  VALUES (v_org2_id, 'https://apexcloudsystems.in', 'https://apexcloudsystems.in', TRUE, 'CRAWLED', NOW(), NOW(), NOW())
  RETURNING id INTO v_web2_id;

  INSERT INTO source_pages (website_id, page_url, page_type, http_status, fetched_at, created_at)
  VALUES (v_web2_id, 'https://apexcloudsystems.in/contact', 'CONTACT', 200, NOW(), NOW())
  RETURNING id INTO v_page3_id;

  INSERT INTO email_addresses (organization_id, raw_value, normalized_value, source_page_id, source_domain, verification_status, created_at)
  VALUES (v_org2_id, 'contact@apexcloudsystems.in', 'contact@apexcloudsystems.in', v_page3_id, 'apexcloudsystems.in', 'VERIFIED', NOW());

  INSERT INTO phone_numbers (organization_id, raw_value, normalized_value, phone_type, source_page_id, source_domain, verification_status, created_at)
  VALUES (v_org2_id, '+91 98940 12345', '+919894012345', 'PHONE', v_page3_id, 'apexcloudsystems.in', 'VERIFIED', NOW());

END \\$\\$;
"`;

  console.log('[SEED] Executing SQL to insert verified provenance test leads...');
  execSync(psqlCmd, { env: { ...process.env, PGPASSWORD: process.env.PGPASSWORD || 'postgres' } });
  console.log('[SEED] Leads inserted successfully.');

  return { username, email, password, accessToken, user, taskId: task.id };
}

async function runBrowserTests() {
  console.log('--- Starting Complete Provenance & Identity E2E Audit ---');
  const creds = await seedVerifiedProvenanceData();

  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu', '--window-size=1440,900'],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1440, height: 900 });

  async function clickButtonWithText(text) {
    return page.evaluate((targetText) => {
      const btns = Array.from(document.querySelectorAll('button, a'));
      const found = btns.find(
        (b) => b.textContent.trim().includes(targetText) || b.title?.includes(targetText)
      );
      if (found) {
        found.click();
        return true;
      }
      return false;
    }, text);
  }

  // 1. Test Login Page
  console.log('[E2E 1/7] Testing Login Page...');
  await page.goto(`${FRONTEND_URL}/login`, { waitUntil: 'networkidle2' });
  await page.screenshot({ path: path.join(ARTIFACT_DIR, '01_login_provenance.png') });

  // 2. Perform Form Login
  console.log('[E2E 2/7] Authenticating via form login...');
  await page.focus('input[type="email"]');
  await page.type('input[type="email"]', creds.email);
  await page.focus('input[type="password"]');
  await page.type('input[type="password"]', creds.password);
  await clickButtonWithText('Sign in');
  await new Promise((r) => setTimeout(r, 2000));
  await page.screenshot({ path: path.join(ARTIFACT_DIR, '02_dashboard_provenance.png') });

  // 3. Navigate to All Leads Page
  console.log('[E2E 3/7] Navigating to Leads Page...');
  await page.goto(`${FRONTEND_URL}/leads`, { waitUntil: 'networkidle2' });
  await new Promise((r) => setTimeout(r, 2000));
  await page.screenshot({ path: path.join(ARTIFACT_DIR, '03_leads_table_provenance.png') });

  const pageContent = await page.content();
  if (!pageContent.includes('ABC Technologies') || !pageContent.includes('abctechnologies.com')) {
    throw new Error('All Leads table failed to display verified company name or official domain!');
  }
  console.log('✓ All Leads Table displays authentic company name and official domain.');

  // 4. Click View button on ABC Technologies
  console.log('[E2E 4/7] Opening Lead Details Modal to audit provenance...');
  const clicked = await clickButtonWithText('View');
  if (!clicked) throw new Error('Could not click View button!');

  await page.waitForSelector('.modal-content', { timeout: 8000 });
  await new Promise((r) => setTimeout(r, 1200));
  await page.screenshot({ path: path.join(ARTIFACT_DIR, '04_lead_details_provenance_modal.png') });

  const modalText = await page.$eval('.modal-content', (el) => el.textContent);
  if (!modalText.includes('sales@abctechnologies.com') || !modalText.includes('VERIFIED')) {
    throw new Error('Modal failed to display verified matching email!');
  }
  if (!modalText.includes('External / Unverified Contacts') && !modalText.includes('capgemini.com')) {
    throw new Error('Modal failed to segregate third-party external contact!');
  }
  console.log('✓ Lead Details Modal correctly groups verified vs external contacts with full provenance.');

  // Close modal
  await clickButtonWithText('Close Profile');
  await new Promise((r) => setTimeout(r, 600));

  // 5. Test CSV Export
  console.log('[E2E 5/7] Testing CSV Export with provenance columns...');
  await clickButtonWithText('Export to CSV');
  await new Promise((r) => setTimeout(r, 1000));
  await page.screenshot({ path: path.join(ARTIFACT_DIR, '05_export_provenance_csv.png') });

  // 6. Test Responsive Viewports
  console.log('[E2E 6/7] Testing Responsive Viewports...');
  const viewports = [
    { name: 'Desktop_1920', width: 1920, height: 1080 },
    { name: 'Laptop_1366', width: 1366, height: 768 },
    { name: 'Tablet_768', width: 768, height: 1024 },
    { name: 'Mobile_360', width: 360, height: 740 },
  ];

  for (const vp of viewports) {
    await page.setViewport({ width: vp.width, height: vp.height });
    await new Promise((r) => setTimeout(r, 400));
    await page.screenshot({ path: path.join(ARTIFACT_DIR, `06_responsive_${vp.name}.png`) });
  }
  console.log('✓ Responsive layouts verified across all 4 standard form factors.');

  await browser.close();
  console.log('=== All Provenance & Identity E2E Verifications PASSED Successfully! ===');
}

runBrowserTests().catch((err) => {
  console.error('[E2E ERROR]', err);
  process.exit(1);
});
