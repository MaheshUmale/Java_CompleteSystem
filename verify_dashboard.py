
from playwright.sync_api import sync_playwright, expect

def verify_dashboard(page):
    """
    Navigates to the dashboard, captures console logs, waits for the correct data, and takes a screenshot.
    """
    # Attach a listener to capture all console messages
    page.on("console", lambda msg: print(f"BROWSER LOG: {msg.text}"))

    # Navigate to the running frontend application on the correct Vite port
    page.goto("http://localhost:5173")

    # Wait for the correct spot price element to appear.
    # The log confirms the received spot price is 109.9.
    spot_price_element = page.get_by_text("109.9")

    try:
        # Use an assertion with a timeout to wait for the element to appear
        expect(spot_price_element).to_be_visible(timeout=20000)
        print("Verification successful: Spot price element is visible.")

        # Take a screenshot on success
        page.screenshot(path="/app/verification.png")

    except Exception as e:
        print(f"Verification failed: {e}")
        # Take a screenshot even on failure for debugging
        page.screenshot(path="/app/verification_failure.png")


if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            verify_dashboard(page)
        finally:
            browser.close()
