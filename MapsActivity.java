package com.example.zita.bikeapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.Text;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    public final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String TAG = "smsreceiver";
    private Polyline BikePath = null;
    private static List<LatLng> points = new ArrayList<LatLng>();
    private static CameraPosition cam = null;
    private Button tracking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {// is called when the screen is created
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        tracking = (Button)findViewById(R.id.trackingbuttontton7);

        BroadcastReceiver myActivityReceiver = new BroadcastReceiver(){// sets up the event listener that will read text messages
            private static final String TAG = "smsssreceiver";
            @Override
            public void onReceive(Context context, Intent intent){//when it receives an sms message

                Bundle extras = intent.getExtras();

                String strMessage = "";

                if ( extras != null )// there is something to read
                {
                    Object[] smsextras = (Object[]) extras.get( "pdus" );

                    for ( int i = 0; i < smsextras.length; i++ )// read the full message
                    {
                        SmsMessage smsmsg = SmsMessage.createFromPdu((byte[]) smsextras[i]);//to hold the message (src + body)

                        String strMsgBody = smsmsg.getMessageBody().toString();// body is the message itself
                        String strMsgSrc = smsmsg.getOriginatingAddress();// src is the phone number

                        strMessage += "SMS from " + strMsgSrc + " : " + strMsgBody;
                        if( strMsgSrc.equals("+19517437456")){// if it comes from the device

                            if(strMsgBody.charAt(0) == '?'){//does it have a question mark at the beginning?
                                String[] s = strMsgBody.split("\\?");//if it does, parse it out
                                plotCoordinatesFromText(s[1]);
                            }

                        }
                    }

                }
            }
        };
        IntentFilter smsFilter = new IntentFilter(ACTION_SMS_RECEIVED);// needed to move to register the listener in broadcast receiver
        registerReceiver(myActivityReceiver,smsFilter);

        BikePath = mMap.addPolyline(new PolylineOptions());//to plot the line on the google map
        BikePath.setPoints(points);

        for( int i=0; i<points.size();i++){//add all the markers that were there before in case the user exited the program
            mMap.addMarker(new MarkerOptions()
                            .title("Position")
                            .position(points.get(i))
            );
        }
        if( cam != null){//update the cmara as it was before in case the user exited the program
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cam));
        }
        Button find = (Button)findViewById(R.id.trackingbuttontton7);
        if(VariablesSingleton.getInstance().getMODE().equals("ON")){//disable or enable the tracking button depending on the mode of the system
            find.setEnabled(true);
        }else{
            find.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {//performed at the same time as onCreate i think
        super.onResume();
        setUpMapIfNeeded();
        TextView t = (TextView)findViewById(R.id.textView2);
        t.setText(VariablesSingleton.getInstance().getBATTERY()+"%");
        tracking = (Button)findViewById(R.id.trackingbuttontton7);
    }
    private void setUpMapIfNeeded() {// sets up the ghoogle map if its not there
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
    }
    public void mapType(View view){
        if( mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL){
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }else{
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }
    public void trackingOnClick(View view){//when the user clicks the tracking button
        VariablesSingleton.getInstance().setSENDGPS(true);//send the text message requestiing info
        VariablesSingleton.getInstance().sendTEXT();
        Toast tst = Toast.makeText(getApplicationContext(),"Request Sent",Toast.LENGTH_LONG);//display a toast for a short while
        tst.setGravity(Gravity.TOP|Gravity.LEFT,250,200);
        tst.show();
        tracking.setEnabled(false);//disable the button

        final Handler handler = new Handler();//enable the button after 5 s
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tracking.setEnabled(true);
            }
        }, 5000);
    }
    public void eraseOnClick(View view){// when the user clicks on the erase button
        mMap.clear();//clears the map
        points = new ArrayList<LatLng>();//resets the markers holder
    }
    public void onClickBack(View view){//when the user clicks on the back button
        cam = mMap.getCameraPosition();//saves camara position
        Intent intent = new Intent(this,MainWindow.class);//needed to send the user to another screen
        startActivity(intent);
    }
    public void plotCoordinatesFromText(String text){//parses out incoming texts
        String[] coords = text.split(",");
        float lat = Float.parseFloat(coords[1]);
        float lng = Float.parseFloat(coords[0]);
        float ticks = Float.parseFloat(coords[2]);

        double total = (4400.0 - 0.1707*ticks)/44.00;//computers battery life
        int total_ = (int)(total + 0.5);

        VariablesSingleton.getInstance().setBATTERY(Double.toString(total_));
        TextView t = (TextView)findViewById(R.id.textView2);
        t.setText(VariablesSingleton.getInstance().getBATTERY()+"%");//displays battery life

        LatLng pos = new LatLng(lat,lng);//
        points.add(pos);//adds the point on the google maps
        //BikePath = mMap.addPolyline(new PolylineOptions());
        BikePath.setPoints(points);//adds the line connecting the markers

        mMap.addMarker(new MarkerOptions()//displays the marker on google map
                .title("Position")
                .position(pos)
                 );
        Log.d(TAG,coords[0]);
        Log.d(TAG,coords[1]);
    }

}
