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

public class PhoneDataEntriesAdapter extends RecyclerView.Adapter<PhoneDataEntriesAdapter.ViewHolder> {

    private Context context;
    private ArrayList<DataEntry> phoneDataEntries;

    public PhoneDataEntriesAdapter(Context context, ArrayList<DataEntry> phoneDataEntries) {
        this.context = context;
        this.phoneDataEntries = phoneDataEntries;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.phone_entry_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(phoneDataEntries.get(position), position);
    }

    @Override
    public int getItemCount() {
        return phoneDataEntries.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private boolean firstTime = true;
        private DataEntry dataEntry;
        public AppCompatImageView iconImageView;
        public EditText phoneEditText;
        public Spinner phoneTypeSpinner;

        public ViewHolder(View itemView) {
            super(itemView);

            iconImageView = itemView.findViewById(R.id.iconImageView);
            phoneEditText = itemView.findViewById(R.id.phoneEditText);
            phoneTypeSpinner = itemView.findViewById(R.id.phoneTypeSpinner);

            phoneEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (firstTime) return;

                    dataEntry.setValue(s.toString());

                    if (s.toString().length() > 0) {
                        //is last one
                        if (phoneDataEntries.indexOf(dataEntry) == phoneDataEntries.size() - 1) {
                            phoneDataEntries.add(new DataEntry());
                            notifyItemChanged(phoneDataEntries.size() - 1);
                        }
                    } else {
                        int index = phoneDataEntries.indexOf(dataEntry);
                        if (index != phoneDataEntries.size() - 1) {
                            phoneDataEntries.remove(dataEntry);

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

            phoneTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
            phoneEditText.setText(dataEntry.getValue());
            phoneTypeSpinner.setSelection(dataEntry.getType());
            this.firstTime = false;
        }
    }
}
