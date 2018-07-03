package com.lcw.people.Adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.lcw.people.Helpers.DataEntry;
import com.lcw.people.R;

import java.util.ArrayList;

public class EmailDataEntriesAdapter extends RecyclerView.Adapter<EmailDataEntriesAdapter.ViewHolder> {

    private Context context;
    private ArrayList<DataEntry> emailDataEntries;

    public EmailDataEntriesAdapter(Context context, ArrayList<DataEntry> emailDataEntries) {
        this.context = context;
        this.emailDataEntries = emailDataEntries;
    }

    @Override
    public EmailDataEntriesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.email_entry_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(EmailDataEntriesAdapter.ViewHolder holder, int position) {
        holder.bind(emailDataEntries.get(position), position);
    }

    @Override
    public int getItemCount() {
        return emailDataEntries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private boolean firstTime = true;
        private DataEntry dataEntry;
        public AppCompatImageView iconImageView;
        public EditText emailEditText;
        public Spinner emailTypeSpinner;

        public ViewHolder(View itemView) {
            super(itemView);

            iconImageView = itemView.findViewById(R.id.iconImageView);
            emailEditText = itemView.findViewById(R.id.emailEditText);
            emailTypeSpinner = itemView.findViewById(R.id.emailTypeSpinner);

            emailEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (firstTime) return;

                    dataEntry.setValue(s.toString());

                    if (s.toString().length() > 0) {
                        //is last one
                        if (emailDataEntries.indexOf(dataEntry) == emailDataEntries.size() - 1) {
                            emailDataEntries.add(new DataEntry());
                            notifyItemChanged(emailDataEntries.size() - 1);
                        }
                    } else {
                        int index = emailDataEntries.indexOf(dataEntry);
                        if (index != emailDataEntries.size() - 1) {
                            emailDataEntries.remove(dataEntry);

                            notifyItemRemoved(index);

                            if (index == 0) {
                                notifyItemChanged(0);
                            }
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            emailTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    dataEntry.setType(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        private void bind(DataEntry dataEntry, int position) {
            this.dataEntry = dataEntry;
            iconImageView.setVisibility(position == 0 ? View.VISIBLE : View.INVISIBLE);
            emailEditText.setText(dataEntry.getValue());
            emailTypeSpinner.setSelection(dataEntry.getType());
            this.firstTime = false;
        }
    }
}
