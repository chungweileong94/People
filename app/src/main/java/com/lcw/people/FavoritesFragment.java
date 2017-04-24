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
import com.lcw.people.Helpers.FavoritesSQLiteHelper;
import com.lcw.people.Helpers.PermissionRequestCode;

import java.util.ArrayList;


public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView favoritesRecyclerView;
    private RecyclerView.LayoutManager favoritesLayoutManager;
    private LinearLayout emptyPanel;
    private String searchString = "";

    public FavoritesFragment() {
        // Required empty public constructor
    }

    public static FavoritesFragment newInstance() {
        FavoritesFragment fragment = new FavoritesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},
                    PermissionRequestCode.READ_CONTACTS.getValue());
        } else {
            getLoaderManager().restartLoader(0, null, FavoritesFragment.this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        favoritesRecyclerView = (RecyclerView) view.findViewById(R.id.favoritesRecyclerView);

        favoritesLayoutManager = new LinearLayoutManager(getActivity());
        favoritesRecyclerView.setLayoutManager(favoritesLayoutManager);

        emptyPanel = (LinearLayout) view.findViewById(R.id.emptyPanel);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem searchItem = menu.findItem(R.id.search);
        searchItem.setVisible(false);
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
        ArrayList<String> lookup_keys = new FavoritesSQLiteHelper(getContext()).getAllContacts();

        String selection = null;
        String[] selectionArgs = null;

        if (lookup_keys.size() > 0) {
            StringBuilder selectionBuilder = new StringBuilder(ContactsContract.Contacts.LOOKUP_KEY + " in (");
            for (int i = 0; i < lookup_keys.size(); i++) selectionBuilder.append("?,");
            selectionBuilder.replace(selectionBuilder.length() - 1, selectionBuilder.length(), ")");
            selection = selectionBuilder.toString();

            selectionArgs = new String[lookup_keys.size()];
            selectionArgs = lookup_keys.toArray(selectionArgs);
        } else {
            selection = ContactsContract.Contacts.LOOKUP_KEY + " in ( ? )";
            selectionArgs = new String[]{"no_such_key"};
        }

        return new CursorLoader(getContext(),
                ContactsContract.Contacts.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        favoritesRecyclerView.setAdapter(new ContactsCursorAdapter(getContext(), data));

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
