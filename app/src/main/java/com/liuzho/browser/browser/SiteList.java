package com.liuzho.browser.browser;

public interface SiteList {
    boolean isWhite(String url);

    void addDomain(String domain);

    void removeDomain(String domain);

    void clearDomains();
}
