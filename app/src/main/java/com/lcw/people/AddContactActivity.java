package com.lcw.people;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import android.support.annotation.NonNull;
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

public class AddContactActivity extends AppCompatActivity {

    private final int PICK_PHOTO = 0;
    private final int REQUEST_IMAGE_CAPTURE = 1;
    private final int PLACE_PICKER_REQUEST = 99;

    private LinearLayout profileImageClick;
    private ImageView profileImageView;
    private EditText nameEditText;
    private Bitmap profileBitmap;
    private Uri photoURI;

    //phone entry ui
    private RecyclerView phoneRecyclerView;
    private RecyclerView.LayoutManager phoneLayoutManager;
    private PhoneDataEntriesAdapter phoneAdapter;
    private ArrayList<DataEntry> phoneDataEntries;

    //email entry ui
    private RecyclerView emailRecyclerView;
    private RecyclerView.LayoutManager emailLayoutManager;
    private EmailDataEntriesAdapter emailAdapter;
    private ArrayList<DataEntry> emailDataEntries;

    //address entry ui
    private RecyclerView addressRecyclerView;
    private RecyclerView.LayoutManager addressLayoutManager;
    private AddressDataEntriesAdapter addressAdapter;
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
        setContentView(R.layout.activity_add_contact);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //phone section setup
        phoneRecyclerView = (RecyclerView) findViewById(R.id.phoneRecyclerView);
        phoneLayoutManager = new LinearLayoutManager(this);
        phoneRecyclerView.setLayoutManager(phoneLayoutManager);
        phoneDataEntries = new ArrayList<>();
        phoneDataEntries.add(new DataEntry());
        phoneAdapter = new PhoneDataEntriesAdapter(this, phoneDataEntries);
        phoneRecyclerView.setAdapter(phoneAdapter);

        //email section setup
        emailRecyclerView = (RecyclerView) findViewById(R.id.emailRecyclerView);
        emailLayoutManager = new LinearLayoutManager(this);
        emailRecyclerView.setLayoutManager(emailLayoutManager);
        emailDataEntries = new ArrayList<>();
        emailDataEntries.add(new DataEntry());
        emailAdapter = new EmailDataEntriesAdapter(this, emailDataEntries);
        emailRecyclerView.setAdapter(emailAdapter);

        //address section setup
        addressRecyclerView = (RecyclerView) findViewById(R.id.addressRecyclerView);
        addressLayoutManager = new LinearLayoutManager(this);
        addressRecyclerView.setLayoutManager(addressLayoutManager);
        addressDataEntries = new ArrayList<>();
        addressDataEntries.add(new DataEntry());
        addressAdapter = new AddressDataEntriesAdapter(this, addressDataEntries);
        addressRecyclerView.setAdapter(addressAdapter);

        //general section setup
        profileImageView = (ImageView) findViewById(R.id.profileImageView);

        if (profileBitmap != null) {
            profileImageView.setImageBitmap(profileBitmap);
        }
        nameEditText = (EditText) findViewById(R.id.nameEditText);

        profileImageClick = (LinearLayout) findViewById(R.id.profileImageClick);
        profileImageClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                String[] options = {"Pick a photo", "Take a photo"};

                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle(R.string.add_photo);
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
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
                    }
                });
                builder.create().show();
            }
        });

        //extra section setup
        birthdayLayout = (LinearLayout) findViewById(R.id.birthdayLayout);
        birthdayTextView = (TextView) findViewById(R.id.birthdayTextView);
        birthdayTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        addMoreButton = (FloatingActionButton) findViewById(R.id.addMoreButton);
        addMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //these lines of code need to modify if there are more options
                if (birthday.equals("")) {
                    final String[] options = {"Birthday"};

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle(R.string.add_more);
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int which) {
                            if (options[which] == "Birthday") {
                                openBirthdayDatePicker(Calendar.getInstance());
                            }
                        }
                    });
                    builder.create().show();
                }
            }
        });

        removeBirthdayButton = (FloatingActionButton) findViewById(R.id.removeBirthdayButton);
        removeBirthdayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                birthday = "";
                refreshExtraFieldUI();
            }
        });
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
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                int rawContactId = ops.size();

                ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build());

                //name
                if (!nameEditText.getText().toString().equals("")) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
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
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getValue())
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, type)
                                .build());
                    }
                }

                //email
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
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.getValue())
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, type)
                                .build());
                    }
                }

                //address
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
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
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

                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray())
                            .build());
                }

                //birthday
                if (!birthday.equals("")) {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
                            .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, birthday)
                            .build());
                }

                //add contact
                try {
                    ContentProviderResult[] res = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                    if (res != null && res[0] != null) {
                        finish();
                        Toast.makeText(this, R.string.contact_added, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.contact_add_failed, Toast.LENGTH_LONG).show();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (OperationApplicationException e) {
                    e.printStackTrace();
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
                            profileBitmap = rotateImage(profileBitmap, photoURI.getPath().toString());
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestCode.ACCESS_FINE_LOCATION.getValue()) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.pick_location_permission, Toast.LENGTH_SHORT).show();
            }
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

        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
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
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void openBirthdayDatePicker(Calendar initialDate) {
        int year = initialDate.get(Calendar.YEAR),
                month = initialDate.get(Calendar.MONTH),
                day = initialDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog birthdayDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                birthday = year + "-" + (month + 1) + "-" + dayOfMonth;
                refreshExtraFieldUI();
            }
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
        discardBuilder.setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        discardBuilder.setNegativeButton(R.string.keep, null);
        discardBuilder.show();
    }
}
