package info.philibert.velib.monpetitvelib;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;


public class VelibMapsActivity extends FragmentActivity implements OnMarkerClickListener, OnMapReadyCallback {

    private static final String LOG_TAG = "MonPetitVelibApp";
    private GoogleMap mMap;
    private static boolean initialLoading = true;
    private static LatLng center = new LatLng(48.86, 2.34); // Add a marker in paris
    private static final String SERVICE_URL = "https://velib.philibert.info/api/stationList.api.php";
    private static final String signalerStationHS_API = "https://velib.philibert.info/api/signalerStationHS.api.php";
    private static final String stationRaccordee_api = "https://velib.philibert.info/api/stationRaccordee.api.php";

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static boolean mLocationPermissionGranted;
    private TextView mTextMessage;
    private FrameLayout frameStats;
    private FrameLayout  frameCarte;
    private WebView statsWebView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        frameStats = (FrameLayout) findViewById(R.id.Frame_stats);
        frameCarte = (FrameLayout) findViewById(R.id.Frame_Carte);
        statsWebView = (WebView) findViewById(R.id.StatsWebView);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        statsWebView.getSettings().setLoadsImagesAutomatically(true);
        statsWebView.getSettings().setJavaScriptEnabled(true);
        statsWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        statsWebView.loadUrl("https://velib.philibert.info/StatsWebview.php");
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
         mMap = googleMap;

        // On va ajouter des markeurs en masse
        new Thread(new Runnable() {
            public void run() {
                try {
                    retrieveAndAddStations();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Cannot retrive cities", e);
                    return;
                }
            }
        }).start();

        // Setting an info window adapter allows us to change the both the contents and look of the
        // info window.

        CustomInfoWindowAdapter CustomInfoWindow = new CustomInfoWindowAdapter();
        mMap.setInfoWindowAdapter(CustomInfoWindow);

        // Set a listener for marker click.
        mMap.setOnMarkerClickListener(this);

        // Prompt the user for permission.
        getLocationPermission();

        //et on affiche à paris
        if(initialLoading)
        {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 11));
            initialLoading=false;
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_Carte:
                    if(!initialLoading)
                    {
                        mMap.clear();
                        // On va ajouter des markeurs en masse
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    retrieveAndAddStations();
                                } catch (IOException e) {
                                    Log.e(LOG_TAG, "Cannot retrive cities", e);
                                    return;
                                }
                            }
                        }).start();
                    }
                    frameStats.setVisibility(View.INVISIBLE);
                    frameCarte.setVisibility(View.VISIBLE);
                    return true;
                case R.id.navigation_Stats:
                    statsWebView.reload();
                    frameStats.setVisibility(View.VISIBLE);
                    frameCarte.setVisibility(View.INVISIBLE);
                    return true;
            }
            return false;
        }
    };

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            mMap.setOnMyLocationButtonClickListener(onMyLocationButtonClickListener);
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }


    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    mMap.setOnMyLocationButtonClickListener(onMyLocationButtonClickListener);
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
        //updateLocationUI();
    }

    private GoogleMap.OnMyLocationButtonClickListener onMyLocationButtonClickListener =
            new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    //mMap.setMinZoomPreference(15);
                    mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
                    return false;
                }
            };


    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker"s info window to open, if it has one).

        int yMatrix = 200, xMatrix =40;

        DisplayMetrics metrics1 = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics1);
        switch(metrics1.densityDpi)
        {
            case DisplayMetrics.DENSITY_LOW:
                yMatrix = 80;
                xMatrix = 20;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                yMatrix = 100;
                xMatrix = 25;
                break;
            case DisplayMetrics.DENSITY_HIGH:
                yMatrix = 150;
                xMatrix = 30;
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                yMatrix = 200;
                xMatrix = 40;
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                yMatrix = 200;
                xMatrix = 50;
                break;
        }

        Projection projection = mMap.getProjection();
        LatLng latLng = marker.getPosition();
        Point point = projection.toScreenLocation(latLng);
        Point point2 = new Point(point.x,point.y-yMatrix);

        LatLng point3 = projection.fromScreenLocation(point2);
        CameraUpdate zoom1 = CameraUpdateFactory.newLatLng(point3);
        mMap.animateCamera(zoom1);
        marker.showInfoWindow();

        return true;
    }

    protected void retrieveAndAddStations() throws IOException {
        HttpURLConnection conn = null;
        final StringBuilder json = new StringBuilder();
        try {
            // Connect to the web service
            URL url = new URL(SERVICE_URL);
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());
            // Read the JSON data into the StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                json.append(buff, 0, read);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to service", e);
            throw new IOException("Error connecting to service", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // Create markers for the city data.
        // Must run this on the UI thread since it"s a UI operation.
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    createMarkersFromJson(json.toString());
                } catch (JSONException e) {
                   Log.e(LOG_TAG, "Error processing JSON", e);
                }
            }
        });
    }

    void createMarkersFromJson(String json) throws JSONException {
        // De-serialize the JSON string into an array of Station objects
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            // Create a marker for each city in the JSON data.
            JSONObject jsonObj = jsonArray.getJSONObject(i);

            MarkerInfo markerInfo = new MarkerInfo(jsonObj.getString("stationCode"),jsonObj.getInt("stationSignaleHS")> 0 ? true : false, jsonObj.getInt("stationConnected"), buildMarkerSnippet(jsonObj) );
            Gson gson = new Gson();
            String markerInfoString = gson.toJson(markerInfo);

            mMap.addMarker(new MarkerOptions()
                    .title(jsonObj.getString("stationCode") + " - " + jsonObj.getString("stationName"))
                    .snippet( markerInfoString )
                    .position(new LatLng(
                            jsonObj.getDouble("stationLat"),
                            jsonObj.getDouble("stationLon")
                    ))
                    .icon(BitmapDescriptorFactory.fromResource(buildMarkerIcon(jsonObj))
            ));
        }
    }

    String buildMarkerSnippet(JSONObject jsonObj) throws JSONException {
        String strSnippet ="";

        /*
        if(locations[i][19]=="1")
        {
            infoWindowContent = infoWindowContent + "   <img src="./images/electified.png" alt=""+locations[i][20]+"" width="20">";
        }
        */

        strSnippet =
                jsonObj.getString("stationAdress") + "\n"+
                "Cette station est officiellement ";
                        if(jsonObj.getString("stationState").equals("Operative"))
                            strSnippet = strSnippet + "en service." + "\n\n";
                        else if(jsonObj.getString("stationState").equals("Close"))
                            strSnippet = strSnippet + "Fermée." + "\n\n";
                        else
                            strSnippet = strSnippet + "en travaux." + "\n\n"
        ;

        strSnippet = strSnippet +
                "Nombre de velib: " +jsonObj.getString("stationNbBike")+ " ("+jsonObj.getString("stationNbBikeOverflow")+")\n"+
                "Nombre de VAE: " +jsonObj.getString("stationNbEBike")+ " ("+jsonObj.getString("stationNbEBikeOverflow")+")\n"+
                "Nombre de places libres: " +jsonObj.getString("nbFreeEDock")+ "\n\n"
        ;


        strSnippet = strSnippet +
                "Dernier Mouvement il y a : " +jsonObj.getString("timediff")+"\n"+
                "Dernier retrait il y a : " +jsonObj.getString("lastExistDiff")+ "\n"
        ;

        if(jsonObj.getInt("hourdiff")<1&&jsonObj.getInt("hourLastExistDiff")<1)
        {

        }
        else if(jsonObj.getInt("hourdiff")<1)
        {
            strSnippet = strSnippet + "Ca bouge mais les derniers mouvements sont des retours ";
        }
        else if(jsonObj.getInt("hourdiff")<4)
        {
            strSnippet = strSnippet + "Ca bouge pas beaucoup officiellement... Faut voir... ";
        }
        else if(jsonObj.getInt("hourdiff")<24)
        {
            strSnippet = strSnippet + "Il y a longtemps que rien n\"a été enregistré ici... \nPrudence!!!";
        }
        else
        {
            strSnippet = strSnippet + "Il y a très très longtemps que rien n\"a été enregistré ici... \nPrudence!!!";
        }

        strSnippet = strSnippet + "\n\nInformations communautaire:\n ";

        if(jsonObj.getInt("stationConnected")==1 || jsonObj.getInt("stationConnected")==0 ||jsonObj.getInt("stationSignaleHS")==1)
            strSnippet = strSnippet + "La station ";

        if(jsonObj.getInt("stationSignaleHS")==1)
        {
            strSnippet = strSnippet + "\n - a été signalée comme étant HS le " + jsonObj.getString("stationSignaleHSDate") +" à "+jsonObj.getString("stationSignaleHSHeure")  ;
            if(jsonObj.getInt("nrRetraitDepuisSignalement")>0)
                strSnippet = strSnippet + "\n   "+jsonObj.getString("nrRetraitDepuisSignalement")+ " retrait(s) depuis le signalement." ;
        }
        else
        {
            //infoWindowContent = infoWindowContent + "<br> - ne marche pas? <input type="button" id="button-"+locations[i][14]+"" value="Signaler" onclick="signaler("+locations[i][14]+")" />";
        }


        if(jsonObj.getInt("stationConnected")==1)
        {
            strSnippet = strSnippet + "\n - est signalée comme alimentée!";// <input type="button" id="button-elec-"+locations[i][14]+"" value="Signaler une erreur" onclick="signalerAlimentee("+locations[i][14]+",false)" />";
        }
        else if(jsonObj.getInt("stationConnected")==0)
        {
            strSnippet = strSnippet + "\n - est signalée comme non alimentée!";// <input type="button" id="button-elec-"+locations[i][14]+"" value="Signaler" onclick="signalerAlimentee("+locations[i][14]+",true)" />";
        }

        else
        {/*
            infoWindowContent = infoWindowContent + "<br> - est elle alimentée? <input type="button" id="button-elec-"+locations[i][14]+"" value="Oui" onclick="signalerAlimentee("+locations[i][14]+",true)" />";
            infoWindowContent = infoWindowContent + " <input type="button" id="button-elec-"+locations[i][14]+"" value="Non" onclick="signalerAlimentee("+locations[i][14]+",false)" />";
          */
        }

       // strSnippet = strSnippet + "\nPlus d\'infos: <a href=https://velib.nocle.fr/station.php?code="+jsonObj.getInt("stationCode")+" target=_blank>velib.nocle.fr</a> ";

        return strSnippet;



    }

    int buildMarkerIcon(JSONObject jsonObj) {

        String mDrawableName = "marker_";
        int drawableId = 0;

        try {
            if(jsonObj.getString("stationConnected").equals("1"))
                mDrawableName=mDrawableName+"p_";
            else if(jsonObj.getString("stationConnected").equals("2"))
                mDrawableName=mDrawableName+"u_";

            if(!jsonObj.getString("stationState").equals("Operative"))
                mDrawableName=mDrawableName+"grey"+jsonObj.getString("stationNbBike");
            else {
                //couleur
                if (jsonObj.getString("hourLastExistDiff").equals("0"))
                    mDrawableName=mDrawableName+"green";
                else if(jsonObj.getInt("hourLastExistDiff")<3  || jsonObj.getInt("hourdiff")<2 )
                    mDrawableName=mDrawableName+"yellow";
                else if(jsonObj.getInt("hourLastExistDiff")<16  || jsonObj.getInt("hourdiff")<8 )
                    mDrawableName=mDrawableName+"orange";
                else if(jsonObj.getInt("hourLastExistDiff")<32  || jsonObj.getInt("hourdiff")<16 )
                    mDrawableName=mDrawableName+"red";
                else
                    mDrawableName=mDrawableName+"purple";

                //hs
                if (jsonObj.getString("stationSignaleHS").equals("1"))
                    mDrawableName = mDrawableName + "x";
                //nombre de velib meca
                mDrawableName=mDrawableName+jsonObj.getString("stationNbBike");
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error processing JSON", e);
        }

        try {
            Class res = R.drawable.class;
            Field field = res.getField(mDrawableName);
            drawableId = field.getInt(null);
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Failure to get drawable id.", e);
        }
        return drawableId;
    }


    /** customizing the info window and/or its contents. */
    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        // These are both viewgroups containing an ImageView with id "badge" and two TextViews with id
        // "title" and "snippet".
        private final View mWindow;
        private final View mContents;

        CustomInfoWindowAdapter() {
            mWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
            mContents = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            render(marker, mWindow);
            return mWindow;
        }

        @Override
        public View getInfoContents(Marker marker) {
             render(marker, mContents);
            return mContents;
        }

        private void render(Marker marker, View view) {

            String title = marker.getTitle();
            TextView titleUi = view.findViewById(R.id.title);
            if (title != null) {
                // Spannable string allows us to edit the formatting of the text.
                SpannableString titleText = new SpannableString(title);
                titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
                titleUi.setText(titleText);
            } else {
                titleUi.setText("");
            }

            Gson gson = new Gson();

            MarkerInfo aMarkerInfo = gson.fromJson(marker.getSnippet(), MarkerInfo.class);

            String snippet = aMarkerInfo.getSnippet();
            TextView snippetUi = view.findViewById(R.id.snippet);


            if (snippet != null && snippet.length() > 12) {
                SpannableString snippetText = new SpannableString(snippet);
                //snippetText.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, 10, 0);
                //snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 12, snippet.length(), 0);
                snippetUi.setText(snippetText);
            } else {
                snippetUi.setText("");
            }
        }
    }

    //une classe pour passer des info à la custom info windows
    public class MarkerInfo {

        private String stationCode;
        private boolean stationHS;  // true : HS, false OK
        private int stationEnedisStatus; //0: false; 1: true, 2 : unknown
        private String snippet;

        public MarkerInfo(String stationCode, boolean stationHS,int stationEnedisStatus, String snippet )
        {
            this.stationCode = stationCode;
            this.stationHS = stationHS;
            this.stationEnedisStatus=stationEnedisStatus;
            this.snippet = snippet;
        }

        public String getStationCode() {
            return stationCode;
        }

        public boolean isStationHS() {
            return stationHS;
        }

        public int getStationEnedisStatus() {
            return stationEnedisStatus;
        }

        public String getSnippet() {
            return snippet;
        }
    }

}
