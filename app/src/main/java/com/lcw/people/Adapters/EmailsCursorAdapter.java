package com.lcw.people.Adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.lcw.people.R;

public class EmailsCursorAdapter extends RecyclerView.Adapter<EmailsCursorAdapter.ViewHolder> {

    private Context context;
    private Cursor cursor;

    public EmailsCursorAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.email_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        cursor.moveToPosition(position);
        String email = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
        int type_id = cursor.getInt(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
        holder.emailTextView.setText(email);
        holder.typeTextView.setText(ContactsContract.CommonDataKinds.Email.getTypeLabelResource(type_id));
        holder.bind(email);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {

        private TextView emailTextView;
        private TextView typeTextView;
        private String email;

        public ViewHolder(View itemView) {
            super(itemView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                intent.setType("message/rfc822"); //for email type
                v.getContext().startActivity(
                        Intent.createChooser(intent, "Choose an Email client :"));
            });

            itemView.setOnCreateContextMenuListener(this);
        }

        public void bind(String email) {
            this.email = email;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(0, R.id.action_copy, 0, R.string.action_copy).setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (item.getItemId() == R.id.action_copy) {
                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(ClipData.newPlainText(email, email));
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        }
    }
}
