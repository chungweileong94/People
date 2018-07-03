package com.lcw.people.Adapters;

import android.Manifest;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.lcw.people.CustomViews.CircleImageView;
import com.lcw.people.DetailsActivity;
import com.lcw.people.Helpers.PermissionRequestCode;
import com.lcw.people.R;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.IOException;
import java.util.ArrayList;

public class ContactsCursorAdapter extends RecyclerView.Adapter<ContactsCursorAdapter.ViewHolder> implements FastScrollRecyclerView.SectionedAdapter {

    private final Context context;
    private Cursor cursor;

    public ContactsCursorAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.contacts_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        holder.nameTextView.setText(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)));
        holder.profileImageView.setImageBitmap(getProfileImage(cursor));
        holder.bind(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)),
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID)));
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    private Bitmap getProfileImage(Cursor cursor) {
        String uriString = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));

        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            if (uri != null) {
                try {
                    return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                } catch (IOException e) {
                    // no profile photo
                    return BitmapFactory.decodeResource(context.getResources(), R.drawable.person);
                }
            }
        }

        return BitmapFactory.decodeResource(context.getResources(), R.drawable.person);
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        cursor.moveToPosition(position);
        return String.valueOf(
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))
                        .substring(0, 1)
                        .toUpperCase());
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
        public TextView nameTextView;
        public CircleImageView profileImageView;
        private String lookup_key;
        private String contact_id;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            profileImageView = itemView.findViewById(R.id.profileImageView);

            itemView.setOnClickListener(v -> {
                if (lookup_key != null) {
                    Intent intent = new Intent(v.getContext(), DetailsActivity.class);
                    intent.putExtra("lookup_key", lookup_key);
                    intent.putExtra("contact_id", contact_id);
                    v.getContext().startActivity(intent);
                }
            });

            itemView.setOnCreateContextMenuListener(this);
        }

        public void bind(String lookup_key, String contact_id) {
            this.lookup_key = lookup_key;
            this.contact_id = contact_id;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(0, R.id.action_share, 0, R.string.action_share).setOnMenuItemClickListener(this);
            menu.add(0, R.id.action_Delete, 0, R.string.action_Delete).setOnMenuItemClickListener(this);
            menu.add(0, R.id.action_pin, 0, R.string.action_pin).setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_share:
                    Uri vcard_uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookup_key);

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType(ContactsContract.Contacts.CONTENT_VCARD_TYPE);
                    intent.putExtra(Intent.EXTRA_STREAM, vcard_uri);
                    itemView.getContext().startActivity(intent);
                    return true;

                case R.id.action_Delete:
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[]{Manifest.permission.WRITE_CONTACTS},
                                PermissionRequestCode.WRITE_CONTACTS.getValue());
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);

                        builder.setTitle(R.string.remove_contact);
                        builder.setMessage(R.string.remove_contact_message);
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                                ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                                        .withSelection(ContactsContract.RawContacts.CONTACT_ID + "= ?", new String[]{contact_id}).build());

                                try {
                                    context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                                    Toast.makeText(context, R.string.contact_deleted_successful, Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    Toast.makeText(context, R.string.contact_deleted_failed, Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.no, null);
                        builder.create().show();
                    }
                    return true;

                case R.id.action_pin:
                    Intent shortcutIntent = new Intent(context, DetailsActivity.class);
                    shortcutIntent.putExtra("lookup_key", lookup_key);
                    shortcutIntent.putExtra("contact_id", contact_id);
                    shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    Intent addIntent = new Intent();
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, nameTextView.getText());

                    Bitmap icon = getCircleBitmap(((BitmapDrawable) profileImageView.getDrawable()).getBitmap());
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);

                    addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    context.sendBroadcast(addIntent);

                    Toast.makeText(context, R.string.pin_contact_successful, Toast.LENGTH_LONG).show();

                    return true;

                default:
                    return false;
            }
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
    }
}
