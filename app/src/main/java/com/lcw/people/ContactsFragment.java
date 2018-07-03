package com.lcw.people;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.lcw.people.Adapters.ContactsCursorAdapter;
import com.lcw.people.Helpers.PermissionRequestCode;

public class ContactsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView contactsRecyclerView;
    private RecyclerView.LayoutManager contactsLayoutManager;
    private LinearLayout emptyPanel;
    private String searchString = "";

    public ContactsFragment() {
        // Required empty public constructor
    }

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        contactsRecyclerView = view.findViewById(R.id.contactsRecyclerView);
        contactsRecyclerView.setHasFixedSize(true);

        contactsLayoutManager = new LinearLayoutManager(getActivity());
        contactsRecyclerView.setLayoutManager(contactsLayoutManager);

        emptyPanel = view.findViewById(R.id.emptyPanel);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView actionSearchView = (SearchView) searchItem.getActionView();

        actionSearchView.setIconifiedByDefault(true);
        actionSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                            PermissionRequestCode.READ_CONTACTS.getValue());
                } else {
                    searchString = newText;
                    getLoaderManager().restartLoader(0, null, ContactsFragment.this);
                }
                return true;
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                    PermissionRequestCode.READ_CONTACTS.getValue());
        } else {
            loadContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestCode.READ_CONTACTS.getValue()) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            }
        }
    }

    private void loadContacts() {
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).onContentChanged();
    }

    // LoaderManager.LoaderCallbacks<Cursor> Methods
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(),
                ContactsContract.Contacts.CONTENT_URI,
                null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?",
                new String[]{"%" + searchString + "%"},
                "UPPER(" + ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + ") ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        contactsRecyclerView.setAdapter(new ContactsCursorAdapter(getContext(), data));

        if (data.getCount() <= 0) {
            emptyPanel.setVisibility(View.VISIBLE);
        } else {
            emptyPanel.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
