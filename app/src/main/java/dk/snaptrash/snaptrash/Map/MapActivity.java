package dk.snaptrash.snaptrash.Map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasFragmentInjector;
import dk.snaptrash.snaptrash.Map.Trash.TrashDialog;
import dk.snaptrash.snaptrash.Menu.ProfileDialog;
import dk.snaptrash.snaptrash.Menu.Routes.RouteDialog;
import dk.snaptrash.snaptrash.Models.Route;
import dk.snaptrash.snaptrash.Models.Trash;
import dk.snaptrash.snaptrash.PickUp.PickUpActivity;
import dk.snaptrash.snaptrash.R;
import dk.snaptrash.snaptrash.Services.SnapTrash.Auth.AuthProvider;
import dk.snaptrash.snaptrash.Services.SnapTrash.Route.RouteService;
import dk.snaptrash.snaptrash.Services.SnapTrash.Trash.TrashMapMap;
import dk.snaptrash.snaptrash.Services.SnapTrash.Trash.TrashService;
import dk.snaptrash.snaptrash.login.LoginActivity;

public class MapActivity
    extends Activity
implements HasFragmentInjector, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener,
    GoogleMap.OnMarkerClickListener, AccountHeader.OnAccountHeaderProfileImageListener,
    Drawer.OnDrawerItemClickListener, RouteDialog.Listener {

    private GoogleMap googleMap;

    @Inject DispatchingAndroidInjector<Fragment> fragmentInjector;
    public static GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;
    private Drawer leftSideMenu;
    private TextView leftSideMenuButton;
    @Inject AuthProvider auth;
    @Inject TrashService trashService;
    @Inject RouteService routeService;
    private TrashMapMap trashMarkerMap;
    private Location anchhor;
    private Route currentRoute;
    private ImageButton hasRouteButton;

    private boolean hasSetFirstPosition = false;

    private static final int ROUTE = 1;
    private static final int STORE = 2;
    private static final int SOCIAL = 3;
    private static final int SETTINGS = 4;
    private static final int HELP = 5;
    private static final int SIGN_OUT = 6;

    public MapActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_map);

        this.hasRouteButton = this.findViewById(R.id.hasRouteButton);
        this.hasRouteButton.setOnClickListener(this);

        Log.e("AUTH", auth.toString());

        // Create the Google Api Client with location services.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();

        if(!this.auth.loggedIn()) {
            Log.e("Authentication", "Not logged in!");
            Intent intent = new Intent(this, LoginActivity.class);
            Bundle extras = new Bundle();
            extras.putString(LoginActivity.loginReasonArgument, getString(R.string.SessionExpired));
            this.startActivity(intent);
            return;
        }

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        this.leftSideMenu = new DrawerBuilder()
            .withActivity(this)
            .withFullscreen(true)
            .addDrawerItems(
                new PrimaryDrawerItem().withName(R.string.menu_routes_title).withIcon(R.drawable.menu_routes_logo).withIdentifier(ROUTE),
                new PrimaryDrawerItem().withName(R.string.menu_store_title).withIcon(R.drawable.menu_store_logo).withIdentifier(STORE),
                new PrimaryDrawerItem().withName(R.string.menu_social_title).withIcon(R.drawable.menu_social_logo).withIdentifier(SOCIAL),
                new DividerDrawerItem(),
                new PrimaryDrawerItem().withName(R.string.menu_settings_title).withIcon(R.drawable.menu_settings_logo).withIdentifier(SETTINGS),
                new PrimaryDrawerItem().withName(R.string.menu_sign_out_title).withIcon(R.drawable.menu_logout_logo).withIdentifier(SIGN_OUT),
                new PrimaryDrawerItem().withName(R.string.menu_help_title).withIcon(R.drawable.menu_help_logo).withIdentifier(HELP)
            )
            .withAccountHeader(new AccountHeaderBuilder()
                .withSelectionListEnabled(false)
                .withActivity(this)
                .addProfiles(
                    new ProfileDrawerItem()
                        .withEmail(this.auth.getUser().getEmail())
                        .withName(this.auth.getUser().getUsername())
                        .withIcon(this.auth.getUser().getAvatarUrl())
                        .withNameShown(true)
                )
                .withOnAccountHeaderProfileImageListener(this)
                .build()
            )
            .withOnDrawerItemClickListener(this)
            .build();
        this.leftSideMenu.getSlider();

        this.leftSideMenu.deselect();

        this.leftSideMenuButton = findViewById(R.id.openSideMenuButton);
        this.leftSideMenuButton.setOnClickListener(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setMaxZoomPreference(40);
        googleMap.setMinZoomPreference(20);
        //googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setMyLocationEnabled(true);
        CameraPosition cameraPosition = new CameraPosition.Builder()
            .target(new LatLng(39.87266, -4.028275))
            .zoom(20)
            .tilt(67.5f)
            .bearing(314)
            .build();
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        this.googleMap = googleMap;

        this.trashMarkerMap = new TrashMapMap(
            this,
            trashService,
            this.googleMap,
            getDrawable(R.drawable.trash_icon)
        );
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        @NonNull String permissions[],
        @NonNull int[] grantResults
    ) {
        this.onMapReady(googleMap);
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }
        this.onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
            return;
        }

        locationRequest = new LocationRequest()
                .setInterval(5000)
                .setFastestInterval(500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("Connection failed", connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("mapactivity", "onlocationchanged");
        if(googleMap == null) {
            return;
        }
        Log.e("mapactivity", this.googleMap.toString());
        if(!hasSetFirstPosition) {
            Log.e("mapactivity", "new pos");
            hasSetFirstPosition = true;
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(
                new LatLng(location.getLatitude(), location.getLongitude())
            ));
        }
        if (this.anchhor == null || this.anchhor.distanceTo(location) >= 100) {
            Log.e("mapactivity", "new anchor");
            this.anchhor = location;
            this.trashService.trashes();
        }

    }

    @Override
    public void onClick(View view) {
        if (view == leftSideMenuButton) {
            leftSideMenu.openDrawer();
        } else if(view == hasRouteButton) {
            this.onHasRouteButtonClicked();
        }
    }

    private void onHasRouteButtonClicked() {
        this.hasRouteButton.setEnabled(false);

        routeService.abandonRoute(this.currentRoute).whenComplete((route1, throwable) -> {
            if(throwable == null) {
                this.runOnUiThread(() -> {
                    this.hasRouteButton.setVisibility(View.INVISIBLE);
                    Toast.makeText(this, "Route abandoned.", Toast.LENGTH_SHORT).show();
                });
            }
            else {
                this.runOnUiThread(() -> {
                    this.hasRouteButton.setEnabled(true);
                    Toast.makeText(this, "Failed abandoning route.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d("MarkerClicked", "CLICKED");

        Trash trash = this.trashMarkerMap.getTrash(marker);

        Log.e("mapactivity", String.valueOf(trash == null));

        TrashDialog trashDialog = TrashDialog.newInstance(
            trash
        );

        trashDialog.setOnUserInitiastesPickUpListener(
            pickedUp -> {
                Log.e("mapactivity", "user wants to pick up trash");
                Intent intent = new Intent(this, PickUpActivity.class);
                intent.putExtra(PickUpActivity.trashParameter, trash);
                MapActivity.this.startActivityForResult(intent, PickUpActivity.PICK_UP_CODE);
            }
        );

        trashDialog.show(this.getFragmentManager(), "TrashInfo");

        return true;
    }

    @Override
    public boolean onProfileImageClick(View view, IProfile profile, boolean current) {
        new ProfileDialog().show(getFragmentManager(), "ProfileDialog");
        return false;
    }

    @Override
    public boolean onProfileImageLongClick(View view, IProfile profile, boolean current) {
        return this.onProfileImageClick(view, profile, current);
    }

    @Override
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        switch ((int) drawerItem.getIdentifier()) {
            case ROUTE:
                new RouteDialog().setListener(this).show(getFragmentManager(), "RouteDialog");
                break;
            case SIGN_OUT:
                this.auth.signOut();
                this.startActivity(
                    new Intent(this, LoginActivity.class)
                );
                break;
            case STORE:
                Toast.makeText(this, "Store not yet implemented.", Toast.LENGTH_SHORT).show();
                break;
            case SOCIAL:
                Toast.makeText(this, "Social not yet implemented.", Toast.LENGTH_SHORT).show();
                break;
            case SETTINGS:
                Toast.makeText(this, "Settings not yet implemented.", Toast.LENGTH_SHORT).show();
                break;
            case HELP:
                Toast.makeText(this, "Help & feedback not yet implemented.", Toast.LENGTH_SHORT).show();
                break;
        }
        this.leftSideMenu.deselect();
        return false;
    }

    @Override
    public AndroidInjector<Fragment> fragmentInjector() {
        return this.fragmentInjector;
    }

    @Override
    public void onBackPressed() {
        Log.e("mapaktivity", "backpressed");
        this.finishAffinity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PickUpActivity.PICK_UP_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Trash trash = (Trash) extras.getSerializable(PickUpActivity.trashParameter);
                    if (trash != null) {
                        this.trashMarkerMap.getMarker(trash).setVisible(false);
                    }
                }
            }
        }
    }

    @Override
    public void onRouteSelected(Route route) {
        this.currentRoute = route;
        this.hasRouteButton.setEnabled(true);
        this.hasRouteButton.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Route selected, adding to map.", Toast.LENGTH_SHORT).show();
    }
}
