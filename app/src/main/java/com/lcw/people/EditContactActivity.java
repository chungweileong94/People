package com.lcw.people;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.LoaderManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lcw.people.Adapters.AddressDataEntriesAdapter;
import com.lcw.people.Adapters.EmailDataEntriesAdapter;
import com.lcw.people.Adapters.PhoneDataEntriesAdapter;
import com.lcw.people.Helpers.DataEntry;
import com.lcw.people.Helpers.PermissionRequestCode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class EditContactActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int DETAILS_LOADER_ID = 0;
    private static final int PHONE_LOADER_ID = 1;
    private static final int EMAIL_LOADER_ID = 2;
    private static final int ADDRESS_LOADER_ID = 3;
    private static final int BIRTHDAY_LOADER_ID = 4;

    private final int PICK_PHOTO = 0;
    private final int REQUEST_IMAGE_CAPTURE = 1;
    private final int PLACE_PICKER_REQUEST = 99;

    private String lookup_key, contact_id, raw_contact_id;
    private boolean isProfilePhotoExist = false;

    private LinearLayout profileImageClick;
    private ImageView profileImageView;
    private EditText nameEditText;
    private Bitmap profileBitmap;
    private Uri photoURI;

    //phone entry ui
    private RecyclerView phoneRecyclerView;
    private RecyclerView.LayoutManager phoneLayoutManager;
    private ArrayList<DataEntry> phoneDataEntries;

    //email entry ui
    private RecyclerView emailRecyclerView;
    private RecyclerView.LayoutManager emailLayoutManager;
    private ArrayList<DataEntry> emailDataEntries;

    //address entry ui
    private RecyclerView addressRecyclerView;
    private RecyclerView.LayoutManager addressLayoutManager;
    private AddressDataEntriesAdapter addressAdapter; //for place picker usage
    private ArrayList<DataEntry> addressDataEntries;

    //add more ui
    private FloatingActionButton addMoreButton;
    private LinearLayout birthdayLayout;
    private TextView birthdayTextView;
    private FloatingActionButton removeBirthdayButton;
    private String birthday = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_contact);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        lookup_key = getIntent().getStringExtra("lookup_key");
        contact_id = getIntent().getStringExtra("contact_id");
        raw_contact_id = getRawContactId(contact_id);

        //phone section setup
        phoneRecyclerView = findViewById(R.id.phoneRecyclerView);
        phoneLayoutManager = new LinearLayoutManager(this);
        phoneRecyclerView.setLayoutManager(phoneLayoutManager);
        phoneDataEntries = new ArrayList<>();

        //email section setup
        emailRecyclerView = findViewById(R.id.emailRecyclerView);
        emailLayoutManager = new LinearLayoutManager(this);
        emailRecyclerView.setLayoutManager(emailLayoutManager);
        emailDataEntries = new ArrayList<>();

        //address section setup
        addressRecyclerView = findViewById(R.id.addressRecyclerView);
        addressLayoutManager = new LinearLayoutManager(this);
        addressRecyclerView.setLayoutManager(addressLayoutManager);
        addressDataEntries = new ArrayList<>();

        //general section setup
        profileImageView = findViewById(R.id.profileImageView);
        nameEditText = findViewById(R.id.nameEditText);

        profileImageClick = findViewById(R.id.profileImageClick);
        profileImageClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                String[] options = {"Pick a photo", "Take a photo"};

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.add_photo);
                builder.setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (ActivityCompat.checkSelfPermission(v.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions((Activity) v.getContext(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    PermissionRequestCode.READ_EXTERNAL_STORAGE.getValue());
                        } else {
                            pickPhoto();
                        }
                    } else {
                        if (ActivityCompat.checkSelfPermission(v.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions((Activity) v.getContext(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    PermissionRequestCode.WRITE_EXTERNAL_STORAGE.getValue());
                        } else {
                            takePhotoByCamera();
                        }
                    }
                });
                builder.create().show();
            }
        });

        //extra section setup
        birthdayLayout = findViewById(R.id.birthdayLayout);
        birthdayTextView = findViewById(R.id.birthdayTextView);
        birthdayTextView.setOnClickListener(v -> {
            String dateString = (String) birthdayTextView.getText();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d");

            Calendar calendar = Calendar.getInstance();

            if (dateString.length() > 0) {
                try {
                    calendar.setTime(dateFormat.parse(dateString));
                } catch (ParseException e) {
                }
            }

            openBirthdayDatePicker(calendar);
        });

        addMoreButton = findViewById(R.id.addMoreButton);
        addMoreButton.setOnClickListener(v -> {

            //these lines of code need to modify if there are more options
            if (birthday.equals("")) {
                final String[] options = {"Birthday"};

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.add_more);
                builder.setItems(options, (dialog, which) -> {
                    if (options[which].equals("Birthday")) {
                        openBirthdayDatePicker(Calendar.getInstance());
                    }
                });
                builder.create().show();
            }
        });

        removeBirthdayButton = findViewById(R.id.removeBirthdayButton);
        removeBirthdayButton.setOnClickListener(v -> {
            birthday = "";
            refreshExtraFieldUI();
        });

        Bundle args = new Bundle();
        args.putString("lookup_key", lookup_key);
        getLoaderManager().initLoader(DETAILS_LOADER_ID, args, this);
        getLoaderManager().getLoader(DETAILS_LOADER_ID).onContentChanged();

        getLoaderManager().initLoader(PHONE_LOADER_ID, args, this);
        getLoaderManager().getLoader(PHONE_LOADER_ID).onContentChanged();

        getLoaderManager().initLoader(EMAIL_LOADER_ID, args, this);
        getLoaderManager().getLoader(EMAIL_LOADER_ID).onContentChanged();

        getLoaderManager().initLoader(ADDRESS_LOADER_ID, args, this);
        getLoaderManager().getLoader(ADDRESS_LOADER_ID).onContentChanged();

        getLoaderManager().initLoader(BIRTHDAY_LOADER_ID, args, this);
        getLoaderManager().getLoader(BIRTHDAY_LOADER_ID).onContentChanged();
    }

    @Override
    public void onBackPressed() {
        discardAndFinish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                discardAndFinish();
                return true;

            case R.id.action_done:
                ArrayList<ContentProviderOperation> removeOldOps = new ArrayList<>();
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                //name
                if (!nameEditText.getText().toString().equals("")) {
                    ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(ContactsContract.Data.CONTACT_ID + "= ? and " + ContactsContract.Data.MIMETYPE + "= ?",
                                    new String[]{contact_id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
                            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, nameEditText.getText().toString())
                            .build());
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.no_name);
                    builder.setMessage(R.string.no_name_message);
                    builder.setPositiveButton(R.string.ok, null);
                    builder.show();

                    return true;
                }

                //phone
                removeOldOps.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.LOOKUP_KEY + "= ? and " + ContactsContract.Data.MIMETYPE + "= ?",
                                new String[]{lookup_key, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE})
                        .build());

                for (DataEntry phone : phoneDataEntries) {
                    if (!phone.getValue().equals("")) {
                        int type = 1;

                        switch (phone.getType()) {
                            case 0:
                                type = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
                                break;
                            case 1:
                                type = ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
                                break;
                            case 2:
                                type = ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
                                break;
                            case 3:
                                type = ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
                                break;
                        }

                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, raw_contact_id)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getValue())
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, type)
                                .build());
                    }
                }

                //email
                removeOldOps.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.LOOKUP_KEY + "= ? and " + ContactsContract.Data.MIMETYPE + "= ?",
                                new String[]{lookup_key, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE})
                        .build());

                for (DataEntry email : emailDataEntries) {
                    if (!email.getValue().equals("")) {
                        int type = 1;

                        switch (email.getType()) {
                            case 0:
                                type = ContactsContract.CommonDataKinds.Email.TYPE_HOME;
                                break;
                            case 1:
                                type = ContactsContract.CommonDataKinds.Email.TYPE_WORK;
                                break;
                            case 2:
                                type = ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
                                break;
                        }

                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, raw_contact_id)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getValue())
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, type)
                                .build());
                    }
                }

                //address
                removeOldOps.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.LOOKUP_KEY + "= ? and " + ContactsContract.Data.MIMETYPE + "= ?",
                                new String[]{lookup_key, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE})
                        .build());

                for (DataEntry address : addressDataEntries) {
                    if (!address.getValue().equals("")) {
                        int type = 1;

                        switch (address.getType()) {
                            case 0:
                                type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME;
                                break;
                            case 1:
                                type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK;
                                break;
                            case 2:
                                type = ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER;
                                break;
                        }

                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, raw_contact_id)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.getValue())
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, type)
                                .build());
                    }
                }


                //profile image
                if (profileBitmap != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    profileBitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream);

                    if (isProfilePhotoExist) {
                        ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection(ContactsContract.Data.CONTACT_ID + "= ? and " + ContactsContract.Data.MIMETYPE + "= ?",
                                        new String[]{contact_id, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE})
                                .withValue(ContactsContract.Data.IS_SUPER_PRIMARY, 1)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray())
                                .build());
                    } else {
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, raw_contact_id)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray())
                                .build());
                    }
                }

                //birthday
                removeOldOps.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.RAW_CONTACT_ID + "= ? AND " +
                                        ContactsContract.Data.MIMETYPE + "= ? AND " +
                                        ContactsContract.CommonDataKinds.Event.TYPE + "=" + ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                                new String[]{raw_contact_id, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE})
                        .build());

                if (!birthday.equals("")) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, raw_contact_id)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
                            .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, birthday)
                            .build());
                }

                //update contact
                boolean isOldDataRemoved = false;

                try {
                    ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, removeOldOps);
                    if (res[0] != null) {
                        isOldDataRemoved = true;
                    }
                } catch (RemoteException | OperationApplicationException e) {
                    e.printStackTrace();
                }

                if (isOldDataRemoved) {
                    try {
                        ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                        if (res[0] != null) {
                            finish();
                            Toast.makeText(this, R.string.contact_updated, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, R.string.contact_update_failed, Toast.LENGTH_LONG).show();
                        }
                    } catch (RemoteException | OperationApplicationException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, R.string.contact_update_failed, Toast.LENGTH_LONG).show();
                }

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PICK_PHOTO:
                if (resultCode == RESULT_OK) {
                    photoURI = data.getData();

                    if (photoURI != null) {
                        try {
                            profileBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoURI);
                            profileBitmap = rotateImage(profileBitmap, getRealPathFromURI(photoURI));
                            profileBitmap = cropSquareBitmap(profileBitmap);
                            profileImageView.setImageBitmap(profileBitmap);
                        } catch (IOException e) {
                            // no photo
                            profileBitmap = null;
                        }
                    }
                }
                break;
            case REQUEST_IMAGE_CAPTURE:
                try {
                    Uri uri = photoURI;

                    if (uri != null) {
                        try {
                            profileBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            profileBitmap = rotateImage(profileBitmap, photoURI.getPath());
                            profileBitmap = cropSquareBitmap(profileBitmap);
                            profileImageView.setImageBitmap(profileBitmap);
                        } catch (IOException e) {
                            // no photo
                            profileBitmap = null;
                        }
                    }
                } catch (Exception e) {
                }

                break;
            case PLACE_PICKER_REQUEST:
                if (resultCode == RESULT_OK) {
                    addressAdapter.onActivityResult(requestCode, resultCode, data);
                }
                break;
        }
    }

    ////////////////////////////////////////
    //helper methods
    ////////////////////////////////////////
    private void pickPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PHOTO);
    }

    private void takePhotoByCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "/People");
                if (!dir.exists()) dir.mkdirs();
                photoFile = File.createTempFile("temp", ".jpg", dir);

                photoURI = Uri.parse("file:" + photoFile.getAbsolutePath());

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", photoFile));
                } else {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                }

                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException ex) {
                Toast.makeText(this, R.string.camera_failed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private Bitmap cropSquareBitmap(Bitmap bitmap) {
        int size = 0;
        if (bitmap.getHeight() > bitmap.getWidth()) {
            size = bitmap.getWidth();
        } else {
            size = bitmap.getHeight();
        }

        return ThumbnailUtils.extractThumbnail(profileBitmap, size, size);
    }

    private Bitmap rotateImage(Bitmap bitmap, String path) {
        ExifInterface exifInterface = null;

        try {
            exifInterface = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int orientation = Objects.requireNonNull(exifInterface).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270);
                break;
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = Objects.requireNonNull(cursor).getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Bitmap getProfileImage(Context context, Cursor cursor) {
        String uriString = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));
        isProfilePhotoExist = true;
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            if (uri != null) {
                try {
                    return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                } catch (IOException e) {
                    // no profile photo
                    isProfilePhotoExist = false;
                    return BitmapFactory.decodeResource(getResources(), R.drawable.person_large);
                }
            }
        }

        isProfilePhotoExist = false;
        return BitmapFactory.decodeResource(getResources(), R.drawable.person_large);
    }

    public String getRawContactId(String contactId) {
        String res = "";
        Uri uri = ContactsContract.RawContacts.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.RawContacts.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[]{contactId};
        Cursor c = getContentResolver().query(uri, projection, selection, selectionArgs, null);

        if (c != null && c.moveToFirst()) {
            res = c.getString(c.getColumnIndex(ContactsContract.RawContacts._ID));
            c.close();
        }

        return res;
    }

    private void openBirthdayDatePicker(Calendar initialDate) {
        int year = initialDate.get(Calendar.YEAR),
                month = initialDate.get(Calendar.MONTH),
                day = initialDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog birthdayDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            birthday = year1 + "-" + (month1 + 1) + "-" + dayOfMonth;
            refreshExtraFieldUI();
        }, year, month, day);

        birthdayDialog.getDatePicker().setMaxDate(new Date().getTime());

        birthdayDialog.setCancelable(false);
        birthdayDialog.show();
    }

    private void refreshExtraFieldUI() {
        addMoreButton.setVisibility(!birthday.equals("") ? View.GONE : View.VISIBLE);
        birthdayTextView.setText(birthday);
        birthdayLayout.setVisibility(birthday.equals("") ? View.GONE : View.VISIBLE);
    }

    public void discardAndFinish() {
        AlertDialog.Builder discardBuilder = new AlertDialog.Builder(this);
        discardBuilder.setTitle(R.string.unsave);
        discardBuilder.setMessage(R.string.unsaveMessage);
        discardBuilder.setPositiveButton(R.string.discard, (dialog, which) -> finish());
        discardBuilder.setNegativeButton(R.string.keep, null);
        discardBuilder.show();
    }

    ////////////////////////////////////////
    //loader
    ////////////////////////////////////////
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case DETAILS_LOADER_ID:
                return new CursorLoader(this,
                        ContactsContract.Contacts.CONTENT_URI,
                        null,
                        ContactsContract.Contacts.LOOKUP_KEY + "= ?",
                        new String[]{args.getString("lookup_key")},
                        null);

            case PHONE_LOADER_ID:
                return new CursorLoader(this,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY + "= ?",
                        new String[]{args.getString("lookup_key")},
                        null);

            case EMAIL_LOADER_ID:
                return new CursorLoader(this,
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.LOOKUP_KEY + "= ?",
                        new String[]{args.getString("lookup_key")},
                        null);

            case ADDRESS_LOADER_ID:
                return new CursorLoader(this,
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.StructuredPostal.LOOKUP_KEY + "= ?",
                        new String[]{args.getString("lookup_key")},
                        null);

            case BIRTHDAY_LOADER_ID:
                return new CursorLoader(this,
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
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {

        switch (loader.getId()) {
            case DETAILS_LOADER_ID:
                //if the contact removed by other app
                if (data.getCount() < 1) {
                    finish();
                    return;
                }

                data.moveToFirst();

                //name
                nameEditText.setText(data.getString(data.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)));

                //profile image
                profileImageView.setImageBitmap(getProfileImage(this, data));

                break;

            case PHONE_LOADER_ID:
                phoneDataEntries.clear();

                if (data.getCount() > 0) {
                    while (data.moveToNext()) {
                        String phone = data.getString(
                                data.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        int type_id = data.getInt(
                                data.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));

                        int type = 1;

                        switch (type_id) {
                            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                type = 0;
                                break;

                            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                                type = 1;
                                break;

                            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                                type = 2;
                                break;

                            default:
                                type = 3;
                                break;
                        }

                        DataEntry dataEntry = new DataEntry();
                        dataEntry.setValue(phone);
                        dataEntry.setType(type);

                        phoneDataEntries.add(dataEntry);
                    }
                }

                phoneDataEntries.add(new DataEntry());
                phoneRecyclerView.setAdapter(new PhoneDataEntriesAdapter(this, phoneDataEntries));

                break;

            case EMAIL_LOADER_ID:
                emailDataEntries.clear();

                if (data.getCount() > 0) {
                    while (data.moveToNext()) {
                        String email = data.getString(
                                data.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                        int type_id = data.getInt(
                                data.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));

                        int type = 1;

                        switch (type_id) {
                            case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
                                type = 0;
                                break;

                            case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
                                type = 1;
                                break;

                            default:
                                type = 2;
                                break;
                        }

                        DataEntry dataEntry = new DataEntry();
                        dataEntry.setValue(email);
                        dataEntry.setType(type);

                        emailDataEntries.add(dataEntry);
                    }
                }

                emailDataEntries.add(new DataEntry());
                emailRecyclerView.setAdapter(new EmailDataEntriesAdapter(this, emailDataEntries));

                break;

            case ADDRESS_LOADER_ID:
                addressDataEntries.clear();

                if (data.getCount() > 0) {
                    while (data.moveToNext()) {
                        String address = data.getString(
                                data.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
                        int type_id = data.getInt(
                                data.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));

                        int type = 1;

                        switch (type_id) {
                            case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME:
                                type = 0;
                                break;

                            case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK:
                                type = 1;
                                break;

                            default:
                                type = 2;
                                break;
                        }

                        DataEntry dataEntry = new DataEntry();
                        dataEntry.setValue(address);
                        dataEntry.setType(type);

                        addressDataEntries.add(dataEntry);
                    }
                }

                addressDataEntries.add(new DataEntry());
                addressAdapter = new AddressDataEntriesAdapter(this, addressDataEntries);
                addressRecyclerView.setAdapter(addressAdapter);

                break;

            case BIRTHDAY_LOADER_ID:
                if (data.getCount() > 0) {
                    data.moveToFirst();
                    birthday = data.getString(data.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE));
                    refreshExtraFieldUI();
                } else {
                    refreshExtraFieldUI();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {

    }
}
