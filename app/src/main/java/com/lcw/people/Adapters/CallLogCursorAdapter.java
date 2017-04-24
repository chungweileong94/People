package com.lcw.people.Adapters;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lcw.people.Helpers.PermissionRequestCode;
import com.lcw.people.R;

import java.util.Date;

public class CallLogCursorAdapter extends RecyclerView.Adapter<CallLogCursorAdapter.ViewHolder> {

    private Context context;
    private Cursor cursor;

    public CallLogCursorAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.call_log_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        cursor.moveToPosition(position);

        String phone = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));

        Date date = new Date(cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE)));
        String dateFormatted = (String) android.text.format.DateFormat.format("d MMM yyyy h:mm a", date);

        holder.phoneTextView.setText(phone);
        holder.datetimeTextView.setText(dateFormatted);
        holder.bind(phone);

        int drawableId = 0;

        switch (cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE))) {
            case CallLog.Calls.INCOMING_TYPE:
            case CallLog.Calls.ANSWERED_EXTERNALLY_TYPE:
                drawableId = R.drawable.ic_call_received_yellow_24dp;
                break;
            case CallLog.Calls.OUTGOING_TYPE:
                drawableId = R.drawable.ic_call_made_green_24dp;
                break;
            case CallLog.Calls.MISSED_TYPE:
                drawableId = R.drawable.ic_call_missed_red_24dp;
                break;
            case CallLog.Calls.VOICEMAIL_TYPE:
                drawableId = R.drawable.ic_voicemail_blue_24dp;
                break;
            case CallLog.Calls.REJECTED_TYPE:
                drawableId = R.drawable.ic_call_rejected_red_24dp;
                break;
            case CallLog.Calls.BLOCKED_TYPE:
                drawableId = R.drawable.ic_call_blocked_red_24dp;
                break;
            default:
                drawableId = R.drawable.ic_history_gray_24dp;
                break;
        }

        holder.typeImageView.setImageResource(drawableId);
    }

    @Override
    public int getItemCount() {
        return cursor.getCount();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView phoneTextView;
        private TextView datetimeTextView;
        private AppCompatImageView typeImageView;
        private String phoneNumber;

        public ViewHolder(View itemView) {
            super(itemView);

            phoneTextView = (TextView) itemView.findViewById(R.id.phoneTextView);
            datetimeTextView = (TextView) itemView.findViewById(R.id.datetimeTextView);
            typeImageView = (AppCompatImageView) itemView.findViewById(R.id.typeImageView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ActivityCompat.checkSelfPermission(v.getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) v.getContext(),
                                new String[]{Manifest.permission.CALL_PHONE},
                                PermissionRequestCode.CALL_PHONE.getValue());
                    } else {
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse("tel:" + phoneNumber));
                        v.getContext().startActivity(callIntent);
                    }
                }
            });
        }

        public void bind(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }
}
