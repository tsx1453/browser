package com.liuzho.browser.browser;

import android.content.Context;

import com.liuzho.browser.database.RecordAction;
import com.liuzho.browser.unit.RecordUnit;

import java.util.ArrayList;
import java.util.List;

public class TrustedList implements SiteList {

    private final Context context;
    private static final List<String> listTrusted = new ArrayList<>();

    private synchronized static void loadDomains(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        listTrusted.clear();
        listTrusted.addAll(action.listDomains(RecordUnit.TABLE_TRUSTED));
        action.close();
    }

    public TrustedList(Context context) {
        this.context = context;
        loadDomains(context);
    }

    @Override
    public boolean isWhite(String url) {
        for (String domain : listTrusted) {
            if (url != null && url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void addDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.addDomain(domain, RecordUnit.TABLE_TRUSTED);
        action.close();
        listTrusted.add(domain);
    }

    @Override
    public synchronized void removeDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.deleteDomain(domain, RecordUnit.TABLE_TRUSTED);
        action.close();
        listTrusted.remove(domain);
    }

    @Override
    public synchronized void clearDomains() {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_TRUSTED);
        action.close();
        listTrusted.clear();
    }
}