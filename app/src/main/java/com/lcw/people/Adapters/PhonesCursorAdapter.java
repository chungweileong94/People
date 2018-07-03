package com.lcw.people.Adapters;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.lcw.people.Helpers.PermissionRequestCode;
import com.lcw.people.R;

public class PhonesCursorAdapter extends RecyclerView.Adapter<PhonesCursorAdapter.ViewHolder> {

    private Context context;
    private Cursor cursor;

    public PhonesCursorAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.phone_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        String phoneNumber = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        int type_id = cursor.getInt(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
        holder.phoneTextView.setText(phoneNumber);
        holder.typeTextView.setText(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(type_id));
        holder.bind(phoneNumber);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {

        private TextView phoneTextView;
        private TextView typeTextView;
        private String phoneNumber;

        public ViewHolder(View itemView) {
            super(itemView);
            phoneTextView = itemView.findViewById(R.id.phoneTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);

            itemView.setOnClickListener(v -> {
                if (ActivityCompat.checkSelfPermission(v.getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions((Activity) v.getContext(),
                            new String[]{Manifest.permission.CALL_PHONE},
                            PermissionRequestCode.CALL_PHONE.getValue());
                } else {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + phoneNumber));
                    v.getContext().startActivity(callIntent);
                }
            });

            itemView.setOnCreateContextMenuListener(this);
        }

        public void bind(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(0, R.id.action_copy, 0, R.string.action_copy).setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (item.getItemId() == R.id.action_copy) {
                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText(phoneNumber, phoneNumber));
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        }
    }
}
