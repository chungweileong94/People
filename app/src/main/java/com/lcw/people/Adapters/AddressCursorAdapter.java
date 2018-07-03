package com.lcw.people.Adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.lcw.people.MapActivity;
import com.lcw.people.R;

import java.util.Objects;


public class AddressCursorAdapter extends RecyclerView.Adapter<AddressCursorAdapter.ViewHolder> {

    private Context context;
    private Cursor cursor;

    public AddressCursorAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.address_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        cursor.moveToPosition(position);

        String address = cursor.getString(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
        int type_id = cursor.getInt(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));

        holder.addressTextView.setText(address);
        holder.typeTextView.setText(ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabelResource(type_id));
        holder.bind(address);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {

        private TextView addressTextView;
        private TextView typeTextView;
        private String address;

        public ViewHolder(View itemView) {
            super(itemView);
            addressTextView = itemView.findViewById(R.id.addressTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);

            itemView.setOnClickListener(v -> {
                //map
                Intent intent = new Intent(v.getContext(), MapActivity.class);
                intent.putExtra("address", address);

                v.getContext().startActivity(intent);
            });

            itemView.setOnCreateContextMenuListener(this);
        }

        public void bind(String address) {
            this.address = address;
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(0, R.id.action_copy, 0, R.string.action_copy).setOnMenuItemClickListener(this);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            if (item.getItemId() == R.id.action_copy) {
                ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                Objects.requireNonNull(clipboardManager).setPrimaryClip(ClipData.newPlainText(address, address));
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        }
    }
}
