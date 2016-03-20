package com.example.zita.bikeapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.Html;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/* For Testing Purposes Only*/
public class RidingMode extends Activity {
    private Button getData;
    public final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static String data_to_display = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riding_mode);
        getData = (Button)findViewById(R.id.request_button);

        BroadcastReceiver myActivityReceiver = new BroadcastReceiver(){
            private static final String TAG = "smsssreceiver";
            @Override
            public void onReceive(Context context, Intent intent){

                Bundle extras = intent.getExtras();

                String strMessage = "";

                if ( extras != null )
                {
                    Object[] smsextras = (Object[]) extras.get( "pdus" );

                    for ( int i = 0; i < smsextras.length; i++ )
                    {
                        SmsMessage smsmsg = SmsMessage.createFromPdu((byte[]) smsextras[i]);

                        String strMsgBody = smsmsg.getMessageBody().toString();
                        String strMsgSrc = smsmsg.getOriginatingAddress();

                        strMessage += "SMS from " + strMsgSrc + " : " + strMsgBody;
                        if( strMsgSrc.equals("+19517437456")){

                            if(strMsgBody.charAt(0) == '2')setData(strMsgBody);

                        }
                    }

                }
            }
        };

        IntentFilter smsFilter = new IntentFilter(ACTION_SMS_RECEIVED);
        registerReceiver(myActivityReceiver,smsFilter);
    }

    @Override
    protected void onResume(){
        super.onResume();
        getData = (Button)findViewById(R.id.request_button);

        if(data_to_display != null){
            TextView t = (TextView)findViewById(R.id.dataText);
            t.setText(Html.fromHtml(data_to_display));
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_riding_mode, menu);
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
    public void onClickRequest(View view){
        VariablesSingleton.getInstance().setSENDDATA(true);
        VariablesSingleton.getInstance().sendTEXT();
        Toast tst = Toast.makeText(getApplicationContext(),"Request Sent",Toast.LENGTH_LONG);
        tst.setGravity(Gravity.TOP|Gravity.LEFT,250,200);
        tst.show();
        getData.setEnabled(false);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                getData.setEnabled(true);
            }
        }, 5000);
    }
    public void onClickBack(View view){
        Intent intent = new Intent(this,MainWindow.class);
        startActivity(intent);
    }
    public void setData(String s){
        String[] data = s.split(",");

        String BER = data[1];
        String CSQ = data[2];
        String Satellites = data[3];

        double ticks = Integer.parseInt(data[4]);
        double total = (4400 - ticks*0.1707)/44;
        int total_ = (int)total;

        double current_off = Double.parseDouble(data[5]);
        double current_armed = Double.parseDouble(data[6]);
        double current_stolen = Double.parseDouble(data[7]);
        double current_parent = Double.parseDouble(data[8]);
        double current_on = Double.parseDouble(data[9]);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("HH:mm:ss a");

        date.setTimeZone(TimeZone.getTimeZone("UTC-08:00"));

        String localTime = date.format(currentLocalTime);

        data_to_display = "Last updated: " + localTime + "<br>" +
                "<br><b>GPRS</b>" +
                "<br>BER: " + BER +
                "<br>CSQ: " + handleCSQ(CSQ) +

                "<br><b>GPS</b>" +
                "<br>Satellites detecting: " + Satellites +
                "<br>Battery Life: " + Integer.toString(total_) + " %" +

                "<br><br><b>Average Currents</b>" +
                "<br>Current OFF: " + Double.toString(current_off) + " mA" +
                "<br>Current ARMED: " + Double.toString(current_armed) + " mA" +
                "<br>Current STOLEN: " + Double.toString(current_stolen) + " mA" +
                "<br>Current PARENT: " + Double.toString(current_parent) + " mA" +
                "<br>Current IDLE: " + Double.toString(current_on) + " mA";

        TextView t = (TextView)findViewById(R.id.dataText);
        t.setText(Html.fromHtml(data_to_display));

        t.setEnabled(true);
    }
    String handleCSQ(String CSQ){
        int csq_ = Integer.parseInt(CSQ);
        String state = "";
        if(csq_< 0) return "Cannot be found";
        else if(csq_ < 10)state = "Marginal";
        else if(csq_ < 15)state = "OK";
        else if(csq_ < 20)state = "Good";
        else if(csq_ < 31)state = "Exellent";
        int dbm = -113 + 2*csq_;

        return Integer.toString(dbm) + " dBm  " + state;
    }
}
