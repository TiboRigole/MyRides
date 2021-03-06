package com.example.tibo.myrides.UserActivities;


import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tibo.myrides.Entities.CurrentUser;
import com.example.tibo.myrides.HelperPackage.CustomNavigationView;
import com.example.tibo.myrides.HelperPackage.NetworkChangeReceiver;
import com.example.tibo.myrides.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import com.example.tibo.myrides.HelperPackage.PlaceAutocompleteAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class AddDriveActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    // INIT LAYOUT
    // keuze auto waarmee gereden wordt voor deze rit
    private LinearLayout autosLijst;
    private TextView selectedCar;
    private AutoCompleteTextView source;
    private AutoCompleteTextView destination;
    private Button checkRoutes;


    private BroadcastReceiver br;

    // adapter nodig om textveldjes automatisch aan te vullen
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;

    // grenzen van autocomplete places
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));


    // init firebase database handler
    private FirebaseFirestore db;

    //zijkantLayout
    private DrawerLayout mDrawerLayout;
    // init google api
    private GoogleApiClient mGoogleApiClient;

    private CurrentUser currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_drive);
        currentUser = CurrentUser.getInstance();

        // broadcastreceiver
        br= new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(br, filter);

        db = FirebaseFirestore.getInstance();

        // DEF LAYOUT
        // autocompletetextviews
        source = findViewById(R.id.source);
        destination = findViewById(R.id.destination);
        // linearlayout met alle auto's
        autosLijst =findViewById(R.id.autosList);
        // textview met geselecteerde auto
        selectedCar=findViewById(R.id.selectedCar);
        // "ga verder" knop
        checkRoutes = findViewById(R.id.routes);

        mDrawerLayout=findViewById(R.id.drawer_layout_adddrive);



        // LOGIC BUTTONS AND WIDGETS
        // setup autocompletetextviews
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);
        source.setAdapter(mPlaceAutocompleteAdapter);
        destination.setAdapter(mPlaceAutocompleteAdapter);


        //toolbar toevoegen aan de layout (nodig om de menuknop te hebben, die het zijkantmenu opkomt)
        Toolbar toolbar = findViewById(R.id.toolbar_AddDriveActivity);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Rit toevoegen");

        //menuknop toevoegen aan de toolbar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);

        //item tappen : zet het item op selected,  sluit de zijbar
        CustomNavigationView navigationView = (CustomNavigationView) findViewById(R.id.navigationzijkant_view);
        navigationView.setCheckedItem(R.id.nav_add_drive);
        navigationView.initSelect(this, mDrawerLayout);


        // vul lijst met auto's aan + maak items clickable om auto te selecteren
        // vul lijst met mijn auto's aan
        Task<QuerySnapshot> query= db.collection("autos").whereEqualTo("eigenaar", currentUser.getEmail()).get();
        query.addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                findViewById(R.id.loader_cars).setVisibility(View.GONE);
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                    TextView kentekenTextView= new TextView(getApplicationContext());
                    kentekenTextView.setTextColor(getColor(R.color.colorText));
                    kentekenTextView.setText(documentSnapshot.get("kenteken").toString());
                    kentekenTextView.setTextSize(20);
                    autosLijst.addView(kentekenTextView);

                    kentekenTextView.setClickable(true);
                    kentekenTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedCar.setText(documentSnapshot.get("kenteken").toString());
                        }
                    });
                }
            }
        });
        query.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
        // vul lijst met gedeelde auto's aan
        Task<QuerySnapshot> querySharedCars= db.collection("autos").whereArrayContains("sharedWithUsers", currentUser.getEmail()).get();
        querySharedCars.addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                    TextView kentekenTextView= new TextView(getApplicationContext());
                    kentekenTextView.setTextColor(getColor(R.color.colorText));
                    kentekenTextView.setTextSize(20);
                    kentekenTextView.setText(documentSnapshot.get("kenteken").toString());

                    TextView sharedByView= new TextView(getApplicationContext());
                    sharedByView.setTextColor(getColor(R.color.colorText));
                    sharedByView.setTextSize(12);
                    sharedByView.setText("(shared by " + documentSnapshot.get("eigenaar").toString()+")");

                    autosLijst.addView(kentekenTextView);
                    autosLijst.addView(sharedByView);

                    kentekenTextView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedCar.setText(documentSnapshot.get("kenteken").toString());
                        }
                    });
                }
            }
        });
        querySharedCars.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });

        // verder gaan naar map met routes
        checkRoutes.setOnClickListener((v) -> {
            // geef melding als veldjes niet zijn ingevuld, anders geef gegevens door naar MapActivity
            if (!source.getText().toString().equals("") && !destination.getText().toString().equals("") && !selectedCar.getText().toString().equals("Selecteer Auto")) {
                Intent intent = new Intent(AddDriveActivity.this, MapsRouteActivity.class);
                intent.putExtra("vertrek", source.getText().toString());
                intent.putExtra("aankomst", destination.getText().toString());
                intent.putExtra("autoKenteken", selectedCar.getText().toString());
                startActivity(intent);
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), "vul adressen in of selecteer auto", Toast.LENGTH_LONG);
                toast.show();
            }
        });

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //https://developer.android.com/training/implementing-navigation/nav-drawer#java
    //logica wanneer op menu knop geduwd wordt dat het sidebarmenu geopend wordt
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(false){
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.unregisterReceiver(br);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}