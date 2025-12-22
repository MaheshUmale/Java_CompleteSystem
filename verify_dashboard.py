
from playwright.sync_api import Page, expect, sync_playwright

def verify_dashboard_layout(page: Page):
  """
  Navigates to the dashboard and takes a screenshot to verify the layout.
  """
  # 1. Arrange: Go to the dashboard homepage.
  page.goto("http://localhost:5173")

  # 2. Wait for the header to be visible, indicating the page has loaded.
  expect(page.get_by_text("JULES-HF-ATS")).to_be_visible()

  # 3. Take a screenshot.
  page.screenshot(path="verification.png")

if __name__ == "__main__":
  with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    try:
      verify_dashboard_layout(page)
    finally:
      browser.close()
