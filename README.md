# Lunch

Automatically scrapes restaurant websites, extracts today's lunch menu using a local LLM (Ollama), and publishes the result to GitHub Pages.

## How it works

1. **GitHub Actions** runs on weekdays at 11:00 CEST
2. **Ollama** is started inside the runner via Docker
3. A **Java app** scrapes each restaurant URL and asks the LLM to extract the lunch menu
4. The result is published as a static HTML page to **GitHub Pages**

## Setup

### 1. Enable GitHub Pages

In your repo: **Settings → Pages → Source → Deploy from branch → `gh-pages`**

### 2. Add your restaurants

Edit `src/main/java/se/brpsystems/lunch/Main.java` and update the `RESTAURANTS` list:

```java
private static final List<Restaurant> RESTAURANTS = List.of(
    new Restaurant("My Restaurant", "https://myrestaurant.se/lunch"),
    new Restaurant("Another Place",  "https://anotherplace.se/dagens")
);
```

### 3. Trigger manually

Go to **Actions → Update Lunch Menus → Run workflow**
