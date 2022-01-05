package com.liuzho.browser.fragment.sitelist;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.liuzho.browser.R;
import com.liuzho.browser.browser.SiteList;
import com.liuzho.browser.unit.BrowserUnit;
import com.liuzho.browser.view.NinjaToast;
import com.liuzho.browser.view.WhitelistAdapter;

import java.util.List;
import java.util.Objects;

public abstract class BaseSiteListFragment extends Fragment {

    private WhitelistAdapter adapter;
    private List<String> list;
    private SiteList siteList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.libbrs_fragment_profile_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        siteList = createSiteList();

        list = listDomains();

        ListView listView = view.findViewById(R.id.whitelist);
        listView.setEmptyView(view.findViewById(R.id.whitelist_empty));

        adapter = new WhitelistAdapter(requireContext(), list) {
            @Override
            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View itemView = super.getView(position, convertView, parent);
                ImageButton btnDelete = itemView.findViewById(R.id.whitelist_item_cancel);
                btnDelete.setOnClickListener(v -> {
                    siteList.removeDomain(list.get(position));
                    list.remove(position);
                    notifyDataSetChanged();
                });
                return itemView;
            }
        };
        listView.setAdapter(adapter);

        Button button = view.findViewById(R.id.whitelist_add);
        button.setOnClickListener(v -> {
            EditText editText = view.findViewById(R.id.whitelist_edit);
            String domain = editText.getText().toString().trim();
            if (domain.isEmpty()) {
                NinjaToast.show(requireContext(), R.string.libbrs_toast_input_empty);
            } else if (!BrowserUnit.isURL(domain)) {
                NinjaToast.show(requireContext(), R.string.libbrs_toast_invalid_domain);
            } else {
                if (checkDomain(domain)) {
                    NinjaToast.show(requireContext(), R.string.libbrs_toast_domain_already_exists);
                } else {
                    createSiteList().addDomain(domain.trim());
                    list.add(0, domain.trim());
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(title());
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.libbrs_menu_whitelist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_clear) {
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setIcon(R.drawable.libbrs_icon_alert)
                    .setTitle(R.string.libbrs_menu_delete)
                    .setMessage(R.string.libbrs_hint_database)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        createSiteList().clearDomains();
                        list.clear();
                        adapter.notifyDataSetChanged();
                    })
                    .setNegativeButton(android.R.string.cancel, (d, w) -> d.cancel())
                    .show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    protected abstract SiteList createSiteList();

    @NonNull
    protected abstract CharSequence title();

    protected abstract List<String> listDomains();

    protected abstract boolean checkDomain(String domain);
}
