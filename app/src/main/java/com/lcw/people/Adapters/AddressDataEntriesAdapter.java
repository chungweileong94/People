package com.lcw.people.Adapters;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.BoolRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.lcw.people.AddContactActivity;
import com.lcw.people.Helpers.DataEntry;
import com.lcw.people.Helpers.PermissionRequestCode;
import com.lcw.people.R;

import java.util.ArrayList;

public class AddressDataEntriesAdapter extends RecyclerView.Adapter<AddressDataEntriesAdapter.ViewHolder> {

    private Context context;
    private ArrayList<DataEntry> addressDataEntries;
    private int itemPosition = -1;
    private final int PLACE_PICKER_REQUEST = 99;

    public AddressDataEntriesAdapter(Context context, ArrayList<DataEntry> addressDataEntries) {
        this.context = context;
        this.addressDataEntries = addressDataEntries;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.address_entry_item_view, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(addressDataEntries.get(position), position);
    }

    @Override
    public int getItemCount() {
        return addressDataEntries.size();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (itemPosition != -1) {
                try {
                    Place place = PlacePicker.getPlace(context, data);
                    addressDataEntries.get(itemPosition).setValue(place.getAddress().toString());
                    notifyItemChanged(itemPosition);

                    if (place.getAddress().toString().length() > 0) {
                        //is last one
                        if (itemPosition == addressDataEntries.size() - 1) {
                            addressDataEntries.add(new DataEntry());
                            notifyItemChanged(addressDataEntries.size() - 1);
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(context, R.string.place_picker_error, Toast.LENGTH_SHORT).show();
                } finally {
                    itemPosition = -1;
                }
            }
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private boolean firstTime = true;
        private DataEntry dataEntry;
        public AppCompatImageView iconImageView;
        public EditText addressEditText;
        public Spinner addressTypeSpinner;
        public Button pickButton;

        public ViewHolder(View itemView) {
            super(itemView);

            iconImageView = (AppCompatImageView) itemView.findViewById(R.id.iconImageView);
            addressEditText = (EditText) itemView.findViewById(R.id.addressEditText);
            addressTypeSpinner = (Spinner) itemView.findViewById(R.id.addressTypeSpinner);

            addressEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (firstTime) return;

                    dataEntry.setValue(s.toString());

                    if (s.toString().length() > 0) {
                        //is last one
                        if (addressDataEntries.indexOf(dataEntry) == addressDataEntries.size() - 1) {
                            addressDataEntries.add(new DataEntry());
                            notifyItemChanged(addressDataEntries.size() - 1);
                        }
                    } else {
                        int index = addressDataEntries.indexOf(dataEntry);
                        if (index != addressDataEntries.size() - 1) {
                            addressDataEntries.remove(dataEntry);

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

            addressTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    dataEntry.setType(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            pickButton = (Button) itemView.findViewById(R.id.pickButton);
            pickButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ContextCompat.checkSelfPermission(v.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) v.getContext(),
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PermissionRequestCode.ACCESS_FINE_LOCATION.getValue());
                    } else {
                        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                        try {
                            itemPosition = getAdapterPosition();
                            getActivity(v.getContext()).startActivityForResult(
                                    builder.build(getActivity(v.getContext())), PLACE_PICKER_REQUEST);
                        } catch (GooglePlayServicesRepairableException e) {
                            e.printStackTrace();
                        } catch (GooglePlayServicesNotAvailableException e) {
                            e.printStackTrace();
                        }
                    }
                }

                private Activity getActivity(Context context) {
                    while (context instanceof ContextWrapper) {
                        if (context instanceof Activity) {
                            return (Activity) context;
                        }
                        context = ((ContextWrapper) context).getBaseContext();
                    }
                    return null;
                }
            });
        }

        private void bind(DataEntry dataEntry, int position) {
            this.dataEntry = dataEntry;
            iconImageView.setVisibility(position == 0 ? View.VISIBLE : View.INVISIBLE);
            addressEditText.setText(dataEntry.getValue());
            addressTypeSpinner.setSelection(dataEntry.getType());
            this.firstTime = false;
        }
    }
}
