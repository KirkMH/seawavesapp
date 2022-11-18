package com.asu.seawavesapp;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class InfoActivity extends AppCompatActivity {
    class Info {
        private String description;
        private String value;

        public Info(String description, String value) {
            this.description = description;
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public String getValue() {
            return value;
        }
    }

    ListView lvInfo;

    ArrayList<Info> infoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        // initialize objects
        lvInfo = findViewById(R.id.lvInfo);

        // retrieve info
        retrieveInfoFromSharedPref();

        // display information
        bindAdapter(infoList, lvInfo);
    }

    private void retrieveInfoFromSharedPref() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Resources res = getApplicationContext().getResources();
        infoList.add(new Info("", "Boat Information"));
        infoList.add(new Info("Boat ID", pref.getString(res.getString(R.string.id_key), "0")));
        infoList.add(new Info("Boat Name", pref.getString(res.getString(R.string.boat_key), "Boat")));
        infoList.add(new Info("Boat Owner", pref.getString(res.getString(R.string.owner_key), "")));
        infoList.add(new Info("Owner Contact", pref.getString(res.getString(R.string.contact_key), "096900338822")));
        infoList.add(new Info("Boat Length",  pref.getString(res.getString(R.string.length_key), "0")));
        infoList.add(new Info("Boat Width",  pref.getString(res.getString(R.string.width_key), "0")));
        infoList.add(new Info("Boat Height", pref.getString(res.getString(R.string.height_key), "0")));

        infoList.add(new Info("", "Timer Information"));
        infoList.add(new Info("Reading Interval", pref.getString(res.getString(R.string.reading_key), "0")));
        infoList.add(new Info("SMS Interval", pref.getString(res.getString(R.string.sms_key), "0")));
        infoList.add(new Info("Saving Interval", pref.getString(res.getString(R.string.saving_key), "0")));
        infoList.add(new Info("Server Posting Interval", pref.getString(res.getString(R.string.post_key), "0")));

        infoList.add(new Info("", "Alert Information"));
        infoList.add(new Info("Pitch Angle Alert",  pref.getString(res.getString(R.string.pitch_key), "0")));
        infoList.add(new Info("Roll Angle Alert", pref.getString(res.getString(R.string.roll_key), "0")));
    }

    private void bindAdapter(ArrayList<Info> list, @NonNull ListView view) {
        ArrayAdapter<Info> adapter = new ArrayAdapter<Info>(getApplicationContext(),
                android.R.layout.simple_list_item_2, android.R.id.text1, list) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);
                Info info = list.get(position);
                if (info != null) {
                    if (info.getDescription().isEmpty()) {
                        text1.setText("");
                        text2.setText(info.getValue());
                        text2.setTextSize(18);
                        text2.setTypeface(null, Typeface.BOLD);
                        text2.setTextColor(Color.rgb(1, 135, 134));
                    }
                    else {
                        text2.setText(info.getDescription());
                        text1.setText(info.getValue());

                        text1.setTextSize(18);
                        text1.setTextColor(Color.BLACK);
                        text2.setTextSize(16);
                        text2.setTextColor(Color.GRAY);
                        text2.setTypeface(null, Typeface.NORMAL);
                    }
                }
                return view;
            }

            @Override
            public boolean isEnabled(int position) {
                return false;
            }
        };
        view.setAdapter(adapter);
    }
}