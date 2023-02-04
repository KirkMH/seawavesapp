package com.asu.seawavesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.asu.seawavesapp.api.ApiClient;
import com.asu.seawavesapp.api.RestApi;
import com.asu.seawavesapp.data.Boat;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etName;
    private TextInputEditText etOwner;
    private TextInputEditText etOwnerContact;
    private TextInputEditText etLength;
    private TextInputEditText etWidth;
    private TextInputEditText etHeight;
    private Button btRegister;
    private RestApi restApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // disable night mode to ensure that texts are readable
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        restApi = ApiClient.getApi();
        initUi();

        btRegister.setOnClickListener(view -> {
            // ensure that required fields are filled in
            Boat boat = getData();
            if (boat != null) {
                processData(boat);
            } else {
                Toast.makeText(getApplicationContext(), "Please fill-in all the fields.", Toast.LENGTH_SHORT).show();
            }
        });

        // display the Disclaimer first
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Disclaimer");
        builder.setMessage(
                "This SEAWAVeS Mobile Application is part of the project “Design and Development of a Low-Cost Mobile Data Acquisition System for Small Crafts” of the Aklan State University - College of Industrial Technology, DOST VI, and DOST-PCIEERD. The use of this project, and any other form of documents as a reference is only permitted after consulting the creators.\n\n" +
                        "We are committed to protect and secure personal information and uphold the rights of our data subjects (i.e. employees, partners, and other stakeholders) in accordance with the Data Privacy Act of 2012 (Republic Act No. 10173), and its Implementing Rules and Regulation.\n\n" +
                        "We observe utmost compliance to the strictest standards of security and confidentiality with respect to all personal information and data submitted by our data subjects."
        );
        builder.setIcon(R.drawable.information);
        // Accept (Positive) action
        builder.setPositiveButton("Accept", null);
        // Close (Negative) action
        builder.setNegativeButton("Close Application", (dialogInterface, i) -> finish());
        // Create the AlertDialog
        AlertDialog alertDialog = builder.create();
        // set other dialog properties
        alertDialog.setCancelable(false);
        alertDialog.show();

        // setup the Accept action
        Button posBtn = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        posBtn.setOnClickListener(view -> alertDialog.cancel());
    }

    /**
     * Initializes the references to the views.
     */
    private void initUi() {
        etName = findViewById(R.id.regName);
        etOwner = findViewById(R.id.regOwner);
        etOwnerContact = findViewById(R.id.regOwnerContact);
        etLength = findViewById(R.id.regLength);
        etWidth = findViewById(R.id.regWidth);
        etHeight = findViewById(R.id.regHeight);
        btRegister = findViewById(R.id.btRegister);
    }

    /**
     * Retrieves the data filled in by the user and returns an instance of the <code>Boat</code> class.
     * However, when there is an unfilled field, it will return <code>null</code>.
     *
     * @return an instance of the <code>Boat</code> class when all fields are filled; <code>null</code> otherwise
     */
    private Boat getData() {
        String name = etName.getText().toString();
        String owner = etOwner.getText().toString();
        String ownerContact = etOwnerContact.getText().toString();
        String length = etLength.getText().toString();
        String width = etWidth.getText().toString();
        String height = etHeight.getText().toString();

        return (name.isEmpty() || owner.isEmpty() || ownerContact.isEmpty() || length.isEmpty() || width.isEmpty() || height.isEmpty()) ?
                null :
                new Boat(
                        null,
                        name,
                        owner,
                        ownerContact,
                        Double.parseDouble(length),
                        Double.parseDouble(width),
                        Double.parseDouble(height),
                        null, true
                );
    }

    /**
     * Saves the details of the boat to the <code>SharedPreferences</code>.
     *
     * @param boat - record to be saved
     */
    private void saveBoatDetails(Boat boat) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        pref.edit()
                .putString(getResources().getString(R.string.id_key), boat.id.toString())
                .putString(getResources().getString(R.string.boat_key), boat.name)
                .putString(getResources().getString(R.string.owner_key), boat.owner)
                .putString(getResources().getString(R.string.contact_key), boat.ownerContact)
                .putString(getResources().getString(R.string.length_key), boat.length.toString())
                .putString(getResources().getString(R.string.width_key), boat.width.toString())
                .putString(getResources().getString(R.string.height_key), boat.height.toString())
                .apply();
    }

    /**
     * Saves the boat data into the server. When saving is successful, it will also launch the MainActivity.
     *
     * @param boat - record to be saved
     */
    private void processData(Boat boat) {
        Call<Boat> call = restApi.addBoat(boat);
        btRegister.setEnabled(false);
        call.enqueue(new Callback<Boat>() {
            @Override
            public void onResponse(Call<Boat> call, Response<Boat> response) {
                Boat rBoat = response.body();
                if (rBoat != null) {
                    saveBoatDetails(rBoat);

                    // launch MainActivity
                    finish();
                    startActivity(new Intent(getApplicationContext(), SetupActivity.class));
                }
            }

            @Override
            public void onFailure(Call<Boat> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Failed to submit registration. Please make sure you have a stable connection and try again.", Toast.LENGTH_SHORT).show();
                call.cancel();
                btRegister.setEnabled(true);
            }
        });
    }
}