package com.example.zita.bikeapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.plus.Plus;

import java.util.ArrayList;
import java.util.List;


public class ParentMode extends FragmentActivity{

    private GoogleMap pMap;
    public final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "smsreceiver";

    private static Circle circle = null;
    private static SeekBar radiusControl = null;
    private static int Radius = 50;
    private static LatLng coords = null;
    private static CameraPosition cam = null;

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        TextView t = (TextView)findViewById(R.id.textView3);
        t.setText(VariablesSingleton.getInstance().getBATTERY() + "%");

        pMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {//when the user clicks ont he map
                if( circle == null) {//add circle if there is none on there
                    circle = pMap.addCircle(new CircleOptions()
                            .center(latLng)
                            .radius(Radius)
                            .visible(true));
                }else{//otherwise move the circle
                    circle.setCenter(latLng);
                    circle.setRadius(Radius);
                }
                coords = latLng;
            }
        });

        if( cam != null){//change the camara position
            pMap.moveCamera(CameraUpdateFactory.newCameraPosition(cam));
        }
        if(circle != null){
            circle = pMap.addCircle( new CircleOptions()
                    .center(coords)
                    .radius(Radius)
                    .visible(true));
        }

        radiusControl.setProgress((Radius-50)/3);

        if(VariablesSingleton.getInstance().getMODE().equals("ON")){
            Button activate = (Button)findViewById(R.id.activate_button);
            activate.setEnabled(true);
            Button deactivate = (Button)findViewById(R.id.deactivate_button);
            deactivate.setEnabled(false);
        }else if(VariablesSingleton.getInstance().getMODE().equals("PARENT")){
            Button activate = (Button)findViewById(R.id.activate_button);
            activate.setEnabled(false);
            Button deactivate = (Button)findViewById(R.id.deactivate_button);
            deactivate.setEnabled(true);
        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {//called when created
        Log.d(TAG,"TEST");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_mode);

        radiusControl = (SeekBar)findViewById(R.id.seekBar);//seekbar sets circle radius
        radiusControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(circle == null){
                    Log.d(TAG,"circle == null");
                    Radius = progress*3+50;//linear progression tick
                }else {
                    Log.d(TAG,"circle != null");
                    Radius = progress*3+50;
                    circle.setRadius(Radius);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {//not used

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {//not used

            }
        });

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_parent_mode, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void onClickBack(View view){
        cam = pMap.getCameraPosition();
        Intent intent = new Intent(this,MainWindow.class);
        startActivity(intent);
    }//user clicks back
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (pMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            pMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map1))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (pMap != null) {
                setUpMap();
            }
        }
    }
    private void setUpMap() {
        pMap.setMyLocationEnabled(true);
    }
    public void onClickActivate(View view){//user clicks on activate button
        VariablesSingleton.getInstance().setRADIUS(Radius);//set up the text that needs to be sent
        VariablesSingleton.getInstance().setREFCOORDS(coords);
        VariablesSingleton.getInstance().setMODE("PARENT");
        VariablesSingleton.getInstance().sendTEXT();//sends the text
        cam = pMap.getCameraPosition();
        Intent intent = new Intent(this,MainWindow.class);//returns user to main window
        startActivity(intent);


    }
    public void onClickDeactivate(View view){//user clicks on deactivate button
        cam = pMap.getCameraPosition();
        VariablesSingleton.getInstance().setMODE("ON");//prepare text to be sent
        VariablesSingleton.getInstance().sendTEXT();//send text
        Intent intent = new Intent(this,MainWindow.class);//returns user to main window
        startActivity(intent);

        /*final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                VariablesSingleton.getInstance().sendTEXT();
            }
        }, 1000);*/
    }
}
