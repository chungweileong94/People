package com.lcw.people;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.lcw.people.Adapters.CallLogCursorAdapter;
import com.lcw.people.Helpers.PermissionRequestCode;

import java.util.ArrayList;
import java.util.Objects;


public class CallLogFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOOKUP_KEY = "lookup_key";

    private static final int PHONE_LOADER_ID = 0;
    private static final int LOG_LOADER_ID = 1;

    private String lookup_key;
    private ArrayList<String> phoneList;
    private RecyclerView logRecyclerView;
    private RecyclerView.LayoutManager logLayoutManager;
    private LinearLayout emptyPanel;

    public CallLogFragment() {
        phoneList = new ArrayList<>();
    }

    public static CallLogFragment newInstance(String lookup_key) {
        CallLogFragment fragment = new CallLogFragment();
        Bundle args = new Bundle();
        args.putString(LOOKUP_KEY, lookup_key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            lookup_key = getArguments().getString(LOOKUP_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call_log, container, false);

        logRecyclerView = view.findViewById(R.id.logRecyclerView);
        logLayoutManager = new LinearLayoutManager(getContext());
        logRecyclerView.setLayoutManager(logLayoutManager);

        emptyPanel = view.findViewById(R.id.emptyPanel);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle bundle = new Bundle();
        bundle.putString("lookup_key", lookup_key);

        getLoaderManager().initLoader(PHONE_LOADER_ID, bundle, this);
        Objects.requireNonNull(getLoaderManager().getLoader(PHONE_LOADER_ID)).onContentChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestCode.READ_CALL_LOG.getValue()) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Bundle bundle = new Bundle();
                bundle.putString("lookup_key", lookup_key);

                readLog(bundle);
            }
        }
    }

    private void readLog(Bundle bundle) {
        getLoaderManager().initLoader(LOG_LOADER_ID, bundle, this);
        Objects.requireNonNull(getLoaderManager().getLoader(LOG_LOADER_ID)).onContentChanged();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case PHONE_LOADER_ID:
                return new CursorLoader(getContext(),
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY + "= ?",
                        new String[]{String.valueOf(args.getString("lookup_key"))},
                        null);

            case LOG_LOADER_ID:
                String selection = null;
                String[] selectionArgs = null;
                if (phoneList.size() > 0) {
                    StringBuilder selectionBuilder = new StringBuilder(CallLog.Calls.NUMBER + " in (");
                    for (int i = 0; i < phoneList.size(); i++) selectionBuilder.append("?,");
                    selectionBuilder.replace(selectionBuilder.length() - 1, selectionBuilder.length(), ")");
                    selection = selectionBuilder.toString();

                    selectionArgs = new String[phoneList.size()];
                    selectionArgs = phoneList.toArray(selectionArgs);
                }

                return new CursorLoader(getContext(),
                        CallLog.Calls.CONTENT_URI,
                        null,
                        selection,
                        selectionArgs,
                        CallLog.Calls.DATE + " DESC");

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case PHONE_LOADER_ID:
                if (data.getCount() > 0) {
                    while (data.moveToNext()) {
                        String phone = data.getString(
                                data.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        phoneList.add(phone.replaceAll("\\D", ""));
                    }

                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) getContext(),
                                new String[]{Manifest.permission.READ_CALL_LOG},
                                PermissionRequestCode.READ_CALL_LOG.getValue());
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putString("lookup_key", lookup_key);
                        readLog(bundle);
                    }
                }

                break;

            case LOG_LOADER_ID:
                logRecyclerView.setAdapter(new CallLogCursorAdapter(getContext(), data));

                if (data.getCount() <= 0) {
                    emptyPanel.setVisibility(View.VISIBLE);
                } else {
                    emptyPanel.setVisibility(View.GONE);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
