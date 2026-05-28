# Lunch

Automatically scrapes restaurant websites and publishes today's lunch menus as a static GitHub Pages site. Runs on weekdays via GitHub Actions — no LLM, no external services, just deterministic DOM scraping.

## How it works

1. **GitHub Actions** runs on weekdays
2. A **Java app** scrapes each restaurant URL using a per-site Playwright scraper
3. The result is published as a static HTML page to **GitHub Pages**

## Setup

### 1. Enable GitHub Pages

In your repo: **Settings → Pages → Source → Deploy from branch → `gh-pages`**

Run the workflow once manually to create the branch.

### 2. Add a restaurant

Add an entry to `src/main/resources/restaurants.yaml`:

```yaml
- location: My City
  restaurants:
    - name: My Restaurant
      url: https://myrestaurant.se/lunch
```

Then create a scraper class (see `CLAUDE.md` for the full pattern). If the site has a standard structure, `DefaultMenuScraper` may work automatically without a custom implementation.

### 3. Trigger manually

Go to **Actions → Update Lunch Menus → Run workflow**
