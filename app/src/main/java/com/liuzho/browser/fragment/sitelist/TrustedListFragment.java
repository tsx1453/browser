package com.liuzho.browser.fragment.sitelist;

import androidx.annotation.NonNull;

import com.liuzho.browser.R;
import com.liuzho.browser.browser.SiteList;
import com.liuzho.browser.browser.TrustedList;
import com.liuzho.browser.database.RecordAction;
import com.liuzho.browser.unit.RecordUnit;

import java.util.List;

public class TrustedListFragment extends BaseSiteListFragment {

    @NonNull
    @Override
    protected SiteList createSiteList() {
        return new TrustedList(requireContext());
    }

    @NonNull
    @Override
    protected CharSequence title() {
        return getString(R.string.libbrs_setting_title_profiles_trustedList);
    }

    @Override
    protected List<String> listDomains() {
        RecordAction action = new RecordAction(requireContext());
        action.open(false);
        List<String> list = action.listDomains(RecordUnit.TABLE_TRUSTED);
        action.close();
        return list;
    }

    @Override
    protected boolean checkDomain(String domain) {
        RecordAction action = new RecordAction(requireContext());
        action.open(true);
        boolean checkDomain = action.checkDomain(domain, RecordUnit.TABLE_TRUSTED);
        action.close();
        return checkDomain;
    }
}
