package com.leaddiscovery.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.annotation.PreDestroy;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Headless browser rendering service using Microsoft Playwright.
 * Provides fallback JavaScript/SPA rendering when Jsoup encounters empty client-side shells.
 * Respects access controls and anti-bot policies without bypassing protections.
 */
@Service
public class PlaywrightScraperService {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightScraperService.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 LeadDiscoveryBot/2.0";

    private static final int TIMEOUT_MILLIS = 15000;

    private Playwright playwright;
    private Browser browser;
    private boolean initialized = false;
    private boolean initializationFailed = false;

    /**
     * Attempts to render a JavaScript-rendered SPA page and returns the parsed Jsoup Document.
     * Returns null if Playwright cannot access the page, times out, or fails.
     */
    public Document renderPage(String url) {
        if (!ensureInitialized()) {
            log.warn("[PLAYWRIGHT_UNAVAILABLE] Playwright browser is not initialized. Skipping JS rendering for {}", url);
            return null;
        }

        try (var context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(USER_AGENT)
                .setJavaScriptEnabled(true))) {

            Page page = context.newPage();
            page.setDefaultNavigationTimeout(TIMEOUT_MILLIS);
            page.setDefaultTimeout(TIMEOUT_MILLIS);

            log.info("[PLAYWRIGHT_RENDER] Rendering SPA shell with Playwright: {}", url);

            // Navigate and wait for DOM content loaded
            var response = page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(TIMEOUT_MILLIS));

            if (response != null && (response.status() == 403 || response.status() == 429)) {
                log.warn("[PLAYWRIGHT_BLOCKED] {} returned HTTP {} in Playwright (access restricted)", url, response.status());
                return null;
            }

            // Short pause to allow client-side hydration scripts (React/Next/Vue) to mount
            try {
                page.waitForTimeout(1500);
            } catch (Exception ignored) {
            }

            String renderedHtml = page.content();
            if (renderedHtml != null && !renderedHtml.isBlank()) {
                log.info("[PLAYWRIGHT_SUCCESS] Successfully rendered {} (HTML size: {} bytes)", url, renderedHtml.length());
                return Jsoup.parse(renderedHtml, url);
            }
        } catch (Exception e) {
            log.warn("[PLAYWRIGHT_FAILED] Could not render {} with Playwright: {}", url, e.getMessage());
        }

        return null;
    }

    /**
     * Initializes Playwright and launches a headless Chromium instance in a thread-safe manner.
     */
    private synchronized boolean ensureInitialized() {
        if (initialized) {
            return true;
        }
        if (initializationFailed) {
            return false;
        }

        try {
            log.info("Initializing Microsoft Playwright and headless Chromium browser...");
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of(
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-dev-shm-usage",
                            "--disable-gpu"
                    )));
            initialized = true;
            log.info("Playwright headless Chromium initialized successfully.");
            return true;
        } catch (Throwable t) {
            log.warn("Playwright initialization failed (browsers may need installation: 'npx playwright install chromium'): {}", t.getMessage());
            initializationFailed = true;
            cleanup();
            return false;
        }
    }

    public synchronized boolean isAvailable() {
        return ensureInitialized();
    }

    @PreDestroy
    public synchronized void cleanup() {
        if (browser != null) {
            try {
                browser.close();
            } catch (Exception ignored) {
            }
            browser = null;
        }
        if (playwright != null) {
            try {
                playwright.close();
            } catch (Exception ignored) {
            }
            playwright = null;
        }
        initialized = false;
    }
}
