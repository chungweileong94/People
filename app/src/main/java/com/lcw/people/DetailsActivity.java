package com.lcw.people;

import android.Manifest;
import android.app.LoaderManager;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.lcw.people.Helpers.FavoritesSQLiteHelper;
import com.lcw.people.Helpers.PermissionRequestCode;

import java.io.IOException;
import java.util.ArrayList;

public class DetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int DETAILS_LOADER_ID = 0;
    private String lookup_key, contact_id;
    private ImageView profileImageView;
    private CollapsingToolbarLayout toolbarLayout;
    private DetailsPageAdapter detailsPageAdapter;
    private ViewPager viewPager;
    private TabLayout tabs;
    private FloatingActionButton favoriteButton;
    FavoritesSQLiteHelper favoritesSQLiteHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        favoritesSQLiteHelper = new FavoritesSQLiteHelper(this);

        lookup_key = getIntent().getStringExtra("lookup_key");
        contact_id = getIntent().getStringExtra("contact_id");

        toolbarLayout = findViewById(R.id.toolbar_layout);
        profileImageView = findViewById(R.id.profileImageView);

        detailsPageAdapter = new DetailsPageAdapter(getSupportFragmentManager(), lookup_key);

        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(detailsPageAdapter);

        tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        favoriteButton = findViewById(R.id.favoriteButton);
        favoriteButton.setImageResource(favoritesSQLiteHelper.isContactExist(lookup_key) ?
                R.drawable.ic_star_white_24dp : R.drawable.ic_star_border_white_24dp);
        favoriteButton.setOnClickListener(v -> {
            if (!favoritesSQLiteHelper.isContactExist(lookup_key)) {
                favoritesSQLiteHelper.addContact(lookup_key);
                favoriteButton.setImageResource(R.drawable.ic_star_white_24dp);
                Snackbar.make(v, R.string.add_favorites, Snackbar.LENGTH_LONG).show();
            } else {
                favoritesSQLiteHelper.removeContact(lookup_key);
                favoriteButton.setImageResource(R.drawable.ic_star_border_white_24dp);
                Snackbar.make(v, R.string.remove_favorites, Snackbar.LENGTH_LONG).show();
            }
        });

        Bundle args = new Bundle();
        args.putString("lookup_key", lookup_key);
        getLoaderManager().initLoader(DETAILS_LOADER_ID, args, this);
        getLoaderManager().getLoader(DETAILS_LOADER_ID).onContentChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_Edit:
                Intent editIntent = new Intent(this, EditContactActivity.class);
                editIntent.putExtra("lookup_key", lookup_key);
                editIntent.putExtra("contact_id", contact_id);
                startActivity(editIntent);
                return true;

            case R.id.action_share:
                Uri vcard_uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookup_key);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
                intent.putExtra(Intent.EXTRA_STREAM, vcard_uri);
                startActivity(intent);
                return true;

            case R.id.action_Delete:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_CONTACTS},
                            PermissionRequestCode.WRITE_CONTACTS.getValue());
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    builder.setTitle(R.string.remove_contact);
                    builder.setMessage(R.string.remove_contact_message);
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                            ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                                    .withSelection(ContactsContract.RawContacts.CONTACT_ID + "= ?", new String[]{contact_id}).build());

                            try {
                                getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                                Toast.makeText(getBaseContext(), R.string.contact_deleted_successful, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(getBaseContext(), R.string.contact_deleted_failed, Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                        }
                    });
                    builder.setNegativeButton(R.string.no, null);
                    builder.create().show();
                }

                return true;

            case R.id.action_pin:
                Intent shortcutIntent = new Intent(getApplicationContext(), DetailsActivity.class);
                shortcutIntent.putExtra("lookup_key", lookup_key);
                shortcutIntent.putExtra("contact_id", contact_id);
                shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                Intent addIntent = new Intent();
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, toolbarLayout.getTitle());
                Bitmap icon = getCircleBitmap(((BitmapDrawable) profileImageView.getDrawable()).getBitmap());
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);

                addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                sendBroadcast(addIntent);

                Toast.makeText(getBaseContext(), R.string.pin_contact_successful, Toast.LENGTH_LONG).show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionRequestCode.CALL_PHONE.getValue()) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        getResources().getString(R.string.call_phone_permission),
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == PermissionRequestCode.SEND_SMS.getValue()) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        getResources().getString(R.string.send_sms_permission),
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == PermissionRequestCode.WRITE_CONTACTS.getValue()) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        getResources().getString(R.string.write_contact_permission),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    ////////////////////////////////////////
    //loader
    ////////////////////////////////////////
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case DETAILS_LOADER_ID:
                return new CursorLoader(getApplicationContext(),
                        ContactsContract.Contacts.CONTENT_URI,
                        null,
                        ContactsContract.Contacts.LOOKUP_KEY + "= ?",
                        new String[]{args.getString("lookup_key")},
                        null);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case DETAILS_LOADER_ID:
                //if the contact removed by other app
                if (data.getCount() < 1) {
                    finish();
                    return;
                }

                data.moveToFirst();
                Bundle bundle = new Bundle();
                bundle.putString("lookup_key", lookup_key);

                //name
                toolbarLayout.setTitle(data.getString(data.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)));

                //profile image
                profileImageView.setImageBitmap(getProfileImage(getBaseContext(), data));
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    ////////////////////////////////////////
    //Helper method
    ////////////////////////////////////////
    private Bitmap getProfileImage(Context context, Cursor cursor) {
        String uriString = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI));

        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            if (uri != null) {
                try {
                    return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                } catch (IOException e) {
                    // no profile photo
                    return BitmapFactory.decodeResource(getResources(), R.drawable.person_large);
                }
            }
        }

        return BitmapFactory.decodeResource(getResources(), R.drawable.person_large);
    }

    public Bitmap getCircleBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        Bitmap _bmp = Bitmap.createScaledBitmap(output, 128, 128, false);
        return _bmp;
        //return output;
    }

    ////////////////////////////////////////
    //classes
    ////////////////////////////////////////
    public class DetailsPageAdapter extends FragmentPagerAdapter {

        private String lookup_key;

        public DetailsPageAdapter(FragmentManager fm, String lookup_key) {
            super(fm);
            this.lookup_key = lookup_key;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return DetailsFragment.newInstance(lookup_key);
                case 1:
                    return CallLogFragment.newInstance(lookup_key);
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Details";
                case 1:
                    return "Call Log";
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
