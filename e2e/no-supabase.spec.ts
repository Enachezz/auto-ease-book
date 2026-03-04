import { test, expect } from './fixtures';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function findSupabaseReferences(dir: string): string[] {
  const results: string[] = [];
  const entries = fs.readdirSync(dir, { withFileTypes: true });

  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'node_modules') continue;
      results.push(...findSupabaseReferences(full));
    } else if (/\.(ts|tsx|js|jsx)$/.test(entry.name)) {
      const content = fs.readFileSync(full, 'utf-8');
      if (/supabase/i.test(content)) {
        results.push(full);
      }
    }
  }

  return results;
}

test.describe('Supabase Removal Verification', () => {

  test('no supabase references remain in frontend source', async () => {
    const srcDir = path.resolve(__dirname, '..', 'src');
    const matches = findSupabaseReferences(srcDir);
    expect(matches).toEqual([]);
  });

  test('no supabase dependencies in package.json', async () => {
    const pkgPath = path.resolve(__dirname, '..', 'package.json');
    const raw = fs.readFileSync(pkgPath, 'utf-8');
    const pkg = JSON.parse(raw);
    const allDeps = {
      ...pkg.dependencies,
      ...pkg.devDependencies,
    };
    const supabaseDeps = Object.keys(allDeps).filter((d) =>
      d.toLowerCase().includes('supabase')
    );
    expect(supabaseDeps).toEqual([]);
  });

  test('no supabase integration directory exists', async () => {
    const dirPath = path.resolve(__dirname, '..', 'src', 'integrations', 'supabase');
    expect(fs.existsSync(dirPath)).toBe(false);
  });
});
