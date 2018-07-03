package com.lcw.people;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lcw.people.Adapters.AddressCursorAdapter;
import com.lcw.people.Adapters.EmailsCursorAdapter;
import com.lcw.people.Adapters.PhonesCursorAdapter;
import com.lcw.people.Helpers.PermissionRequestCode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;


public class DetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOOKUP_KEY = "lookup_key";

    private static final int PHONE_LOADER_ID = 0;
    private static final int EMAIL_LOADER_ID = 1;
    private static final int ADDRESS_LOADER_ID = 2;
    private static final int BIRTHDAY_LOADER_ID = 3;
    private String lookup_key;

    private CardView phone_messageCardView;
    private RecyclerView phonesRecyclerView;
    private RecyclerView.LayoutManager phonesLayoutManager;
    private LinearLayout messageButton;
    private CardView emailCardView;
    private RecyclerView emailsRecyclerView;
    private RecyclerView.LayoutManager emailsLayoutManager;
    private CardView addressCardView;
    private RecyclerView addressRecyclerView;
    private RecyclerView.LayoutManager addressLayoutManager;
    private CardView birthdayCardView;
    private TextView birthdayTextView;
    private LinearLayout emptyPanel;

    public DetailsFragment() {
        // Required empty public constructor
    }

    public static DetailsFragment newInstance(String lookup_key) {
        DetailsFragment fragment = new DetailsFragment();
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details, container, false);

        //phone
        phone_messageCardView = view.findViewById(R.id.phone_messageCardView);
        phonesRecyclerView = view.findViewById(R.id.phonesRecyclerView);
        phonesLayoutManager = new LinearLayoutManager(getContext());
        phonesRecyclerView.setLayoutManager(phonesLayoutManager);
        phonesRecyclerView.addItemDecoration(
                new RecyclerViewSeparatorDecoration(Objects.requireNonNull(getContext()), Color.LTGRAY, .8f));

        //message
        messageButton = view.findViewById(R.id.messageButton);

        //email
        emailCardView = view.findViewById(R.id.emailCardView);
        emailsRecyclerView = view.findViewById(R.id.emailsRecyclerView);
        emailsLayoutManager = new LinearLayoutManager(getContext());
        emailsRecyclerView.setLayoutManager(emailsLayoutManager);

        //address
        addressCardView = view.findViewById(R.id.addressCardView);
        addressRecyclerView = view.findViewById(R.id.addressRecyclerView);
        addressLayoutManager = new LinearLayoutManager(getContext());
        addressRecyclerView.setLayoutManager(addressLayoutManager);

        //birthday
        birthdayCardView = view.findViewById(R.id.birthdayCardView);
        birthdayTextView = view.findViewById(R.id.birthdayTextView);

        emptyPanel = view.findViewById(R.id.emptyPanel);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle bundle = new Bundle();
        bundle.putString("lookup_key", lookup_key);

        //phone & message
        getLoaderManager().initLoader(PHONE_LOADER_ID, bundle, this);
        Objects.requireNonNull(getLoaderManager().getLoader(PHONE_LOADER_ID)).onContentChanged();

        //email
        getLoaderManager().initLoader(EMAIL_LOADER_ID, bundle, this);
        Objects.requireNonNull(getLoaderManager().getLoader(EMAIL_LOADER_ID)).onContentChanged();

        //address
        getLoaderManager().initLoader(ADDRESS_LOADER_ID, bundle, this);
        Objects.requireNonNull(getLoaderManager().getLoader(ADDRESS_LOADER_ID)).onContentChanged();

        //birthday
        getLoaderManager().initLoader(BIRTHDAY_LOADER_ID, bundle, this);
        Objects.requireNonNull(getLoaderManager().getLoader(BIRTHDAY_LOADER_ID)).onContentChanged();
    }

    ////////////////////////////////////////
    //Helper method
    ////////////////////////////////////////
    private void sendMessage(final String phoneNumber) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (sharedPreferences.getBoolean(SettingsActivity.KEY_PREF_SMS_MODE, true)) {
            //use quick message feature
            if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getContext()), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Objects.requireNonNull(getActivity()),
                        new String[]{Manifest.permission.SEND_SMS},
                        PermissionRequestCode.SEND_SMS.getValue());
            } else {
                final Dialog dialog;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //to fix the dialog layout
                    dialog = new Dialog(getContext(), android.R.style.Theme_Material_Light_Dialog);
                } else {
                    dialog = new Dialog(getContext());
                }

                dialog.setContentView(R.layout.send_message_layout);
                dialog.setTitle(R.string.send_message_title);

                FloatingActionButton sendFab = dialog.findViewById(R.id.sendFab);
                sendFab.setOnClickListener(v -> {
                    String message = ((TextView) dialog.findViewById(R.id.messageTextView)).getText().toString();

                    if (message.length() == 0) return;

                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                        Toast.makeText(v.getContext(), R.string.send_message_success, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } catch (Exception e) {
                        Toast.makeText(v.getContext(), R.string.send_message_failed, Toast.LENGTH_LONG).show();
                    }
                });

                dialog.show();
            }
        } else {
            //use the default sms client
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phoneNumber));
            startActivity(intent);
        }
    }

    private void refreshEmptyPanel() {
        if (phone_messageCardView.getVisibility() == View.GONE &&
                emailCardView.getVisibility() == View.GONE &&
                addressCardView.getVisibility() == View.GONE &&
                birthdayCardView.getVisibility() == View.GONE) {
            emptyPanel.setVisibility(View.VISIBLE);
        } else {
            emptyPanel.setVisibility(View.GONE);
        }
    }

    ////////////////////////////////////////
    //loader
    ////////////////////////////////////////
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case PHONE_LOADER_ID:
                return new CursorLoader(Objects.requireNonNull(getContext()),
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY + "= ?",
                        new String[]{String.valueOf(args.getString("lookup_key"))},
                        null);

            case EMAIL_LOADER_ID:
                return new CursorLoader(Objects.requireNonNull(getContext()),
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.LOOKUP_KEY + "= ?",
                        new String[]{String.valueOf(args.getString("lookup_key"))},
                        null);

            case ADDRESS_LOADER_ID:
                return new CursorLoader(Objects.requireNonNull(getContext()),
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.StructuredPostal.LOOKUP_KEY + "= ?",
                        new String[]{String.valueOf(args.getString("lookup_key"))},
                        null);

            case BIRTHDAY_LOADER_ID:
                return new CursorLoader(Objects.requireNonNull(getContext()),
                        ContactsContract.Data.CONTENT_URI,
                        null,
                        ContactsContract.Data.LOOKUP_KEY + "= ? AND " +
                                ContactsContract.Data.MIMETYPE + "= ? AND " +
                                ContactsContract.CommonDataKinds.Event.TYPE + "=" +
                                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                        new String[]{String.valueOf(args.getString("lookup_key")),
                                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE},
                        null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case PHONE_LOADER_ID:
                if (data.getCount() > 0) {
                    phonesRecyclerView.setAdapter(new PhonesCursorAdapter(getContext(), data));

                    final ArrayList<String> phoneList = new ArrayList<>();

                    while (data.moveToNext()) {
                        phoneList.add(
                                data.getString(data.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                    }

                    messageButton.setOnClickListener(v -> {
                        if (phoneList.size() == 1) {
                            sendMessage(phoneList.get(0));
                        } else {
                            final String[] phoneArray = phoneList.toArray(new String[phoneList.size()]);
                            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                            builder.setTitle(R.string.pick_phone_number);
                            builder.setItems(phoneArray, (dialog, which) -> sendMessage(phoneArray[which]));
                            builder.create().show();
                        }
                    });
                    phone_messageCardView.setVisibility(View.VISIBLE);
                } else {
                    phone_messageCardView.setVisibility(View.GONE);
                }
                refreshEmptyPanel();
                break;

            case EMAIL_LOADER_ID:
                if (data.getCount() > 0) {
                    emailsRecyclerView.setAdapter(new EmailsCursorAdapter(getContext(), data));
                    emailCardView.setVisibility(View.VISIBLE);
                } else {
                    emailCardView.setVisibility(View.GONE);
                }
                refreshEmptyPanel();
                break;

            case ADDRESS_LOADER_ID:
                if (data.getCount() > 0) {
                    addressRecyclerView.setAdapter(new AddressCursorAdapter(getContext(), data));
                    addressCardView.setVisibility(View.VISIBLE);
                } else {
                    addressCardView.setVisibility(View.GONE);
                }
                refreshEmptyPanel();
                break;

            case BIRTHDAY_LOADER_ID:
                if (data.getCount() > 0) {
                    data.moveToFirst();
                    String birthday = data.getString(data.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE));
                    String formatDateString = birthday;
                    try {
                        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(birthday);
                        formatDateString = new SimpleDateFormat("d MMM yyyy").format(date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    birthdayTextView.setText(formatDateString);
                    birthdayCardView.setVisibility(View.VISIBLE);
                } else {
                    birthdayCardView.setVisibility(View.GONE);
                }
                refreshEmptyPanel();
                break;
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    ////////////////////////////////////////
    //classes
    ////////////////////////////////////////
    //custom Recycler View separator decoration (bottom of the last item)
    public class RecyclerViewSeparatorDecoration extends RecyclerView.ItemDecoration {

        private final Paint mPaint;

        RecyclerViewSeparatorDecoration(Context context, int color, float heightDp) {
            mPaint = new Paint();
            mPaint.setColor(color);
            mPaint.setAlpha(80);
            final float thickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    heightDp, context.getResources().getDisplayMetrics());
            mPaint.setStrokeWidth(thickness);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

            // we want to retrieve the position in the list
            final int position = params.getViewAdapterPosition();

            // and add a separator to any view but the last one
            if (position < state.getItemCount()) {
                outRect.set(0, 0, 0, (int) mPaint.getStrokeWidth()); // left, top, right, bottom
            } else {
                outRect.setEmpty(); // 0, 0, 0, 0
            }
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            // we set the stroke width before, so as to correctly draw the line we have to offset by width / 2
            final int offset = (int) (mPaint.getStrokeWidth() / 2);

            // this will iterate over every visible view
            for (int i = 0; i < parent.getChildCount(); i++) {
                // get the view
                final View view = parent.getChildAt(i);
                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) view.getLayoutParams();

                // get the position
                final int position = params.getViewAdapterPosition();

                // and finally draw the separator
                int margin = 30;
                if (position == state.getItemCount() - 1) {
                    c.drawLine(view.getLeft() + margin,
                            view.getBottom() + offset,
                            view.getRight() - margin,
                            view.getBottom() + offset,
                            mPaint);
                }
            }
        }
    }
}
