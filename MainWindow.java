package com.example.zita.bikeapplication;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainWindow extends Activity {
    public final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_window);
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

                            if(strMsgBody.charAt(0) == '1')turnOn(strMsgBody);
                            if(strMsgBody.charAt(0) == '4')turnArmed(strMsgBody);
                            if(strMsgBody.charAt(0) == '5'){
                                turnOn(strMsgBody);
                                turnArmed(strMsgBody);
                            }
                        }
                    }

                }
            }
        };
        IntentFilter smsFilter = new IntentFilter(ACTION_SMS_RECEIVED);
        registerReceiver(myActivityReceiver,smsFilter);
        Button armbutton = (Button)findViewById(R.id.button);
        Button disarmbutton = (Button)findViewById(R.id.button2);
        Button trackbutton = (Button)findViewById(R.id.button3);
        Button parentbutton = (Button)findViewById(R.id.button4);
        Button turnoffbutton = (Button)findViewById(R.id.button6);

        String mode = VariablesSingleton.getInstance().getMODE();
        if(mode.equals("ON")){
            armbutton.setEnabled(true);
            disarmbutton.setEnabled(false);
            trackbutton.setEnabled(true);
            parentbutton.setEnabled(true);
            turnoffbutton.setEnabled(true);
        }else if(mode.equals("PARENT")){
            armbutton.setEnabled(false);
            disarmbutton.setEnabled(false);
            trackbutton.setEnabled(true);
            parentbutton.setEnabled(true);
            turnoffbutton.setEnabled(true);
        }else if(mode.equals("OFF")){
            armbutton.setEnabled(false);
            disarmbutton.setEnabled(false);
            trackbutton.setEnabled(false);
            parentbutton.setEnabled(false);
            turnoffbutton.setEnabled(false);
        }else if(mode.equals("STOLEN")){
            armbutton.setEnabled(false);
            disarmbutton.setEnabled(true);
            trackbutton.setEnabled(true);
            parentbutton.setEnabled(false);
            turnoffbutton.setEnabled(true);
        }
    }
    @Override
    protected  void onResume(){
        super.onResume();
        TextView t = (TextView)findViewById(R.id.textView);
        t.setText(VariablesSingleton.getInstance().getBATTERY() + "%");

        if(VariablesSingleton.getInstance().getMODE().equals("PARENT")){
            Button arm = (Button)findViewById(R.id.button);
            arm.setEnabled(false);
            Button disarm = (Button)findViewById(R.id.button2);
            disarm.setEnabled(false);
        }else if(VariablesSingleton.getInstance().getMODE().equals("ARMED")){
            Button arm = (Button)findViewById(R.id.button);
            arm.setEnabled(false);
            Button disarm = (Button)findViewById(R.id.button2);
            disarm.setEnabled(true);
            Button parent = (Button)findViewById(R.id.button4);
            parent.setEnabled(false);
        }else if(VariablesSingleton.getInstance().getMODE().equals("ON")){
            Button arm = (Button)findViewById(R.id.button);
            arm.setEnabled(true);
            Button disarm = (Button)findViewById(R.id.button2);
            disarm.setEnabled(false);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_window, menu);
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

    public void onClickArm(View view){
        VariablesSingleton.getInstance().setMODE("ARMED");
        Button arm = (Button)findViewById(R.id.button);
        arm.setEnabled(false);

        Button disarm = (Button)findViewById(R.id.button2);
        disarm.setEnabled(true);
        VariablesSingleton.getInstance().sendTEXT();

        Button parent = (Button)findViewById(R.id.button4);
        parent.setEnabled(false);
        Toast tst = Toast.makeText(getApplicationContext(),"System is Armed.",Toast.LENGTH_LONG);
        tst.setGravity(Gravity.TOP|Gravity.LEFT,250,200);
        tst.show();

    }
    public void onClickDisarm(View view){
        VariablesSingleton.getInstance().setMODE("ON");
        Button disarm = (Button)findViewById(R.id.button2);
        disarm.setEnabled(false);

        Button arm = (Button)findViewById(R.id.button);
        arm.setEnabled(true);
        VariablesSingleton.getInstance().sendTEXT();

        Button parent = (Button)findViewById(R.id.button4);
        parent.setEnabled(true);

        Toast tst = Toast.makeText(getApplicationContext(),"System is Disarmed.",Toast.LENGTH_LONG);
        tst.setGravity(Gravity.TOP|Gravity.LEFT,250,200);
        tst.show();
    }
    public void onClickTrack(View view){
        Intent intent = new Intent(this,MapsActivity.class);
        startActivity(intent);
    }
    public void onClickParent(View view){
        Intent intent = new Intent(this,ParentMode.class);
        startActivity(intent);
    }
    public void onClickTurnOff(View view){
        VariablesSingleton.getInstance().setMODE("OFF");
        VariablesSingleton.getInstance().sendTEXT();

        Button armbutton = (Button)findViewById(R.id.button);
        Button disarmbutton = (Button)findViewById(R.id.button2);
        Button trackbutton = (Button)findViewById(R.id.button3);
        Button parentbutton = (Button)findViewById(R.id.button4);
        Button alarmbutton = (Button)findViewById(R.id.button5);
        Button turnoffbutton = (Button)findViewById(R.id.button6);
        Button databutton = (Button)findViewById(R.id.data_button);

        armbutton.setEnabled(false);
        disarmbutton.setEnabled(false);
        trackbutton.setEnabled(false);
        alarmbutton.setEnabled(false);
        parentbutton.setEnabled(false);
        turnoffbutton.setEnabled(false);
        databutton.setEnabled(false);
    }
    public void onClickData(View view){
        Intent intent = new Intent(this,RidingMode.class);
        startActivity(intent);
    }
    public void onClickTurnOn(View view){

    }
    public void turnOn(String text){
        VariablesSingleton.getInstance().setMODE("ON");
        String[] coords = text.split(",");
        float ticks = Float.parseFloat(coords[1]);
        double total = (4400.0 - 0.1707*ticks)/44.00;
        int total_ = (int)(total + 0.5);

        VariablesSingleton.getInstance().setBATTERY(Integer.toString(total_));
        TextView t = (TextView)findViewById(R.id.textView);
        t.setText(VariablesSingleton.getInstance().getBATTERY()+"%");

        Button armbutton = (Button)findViewById(R.id.button);
        Button disarmbutton = (Button)findViewById(R.id.button2);
        Button trackbutton = (Button)findViewById(R.id.button3);
        Button parentbutton = (Button)findViewById(R.id.button4);
        Button turnoffbutton = (Button)findViewById(R.id.button6);
        Button databutton = (Button)findViewById(R.id.data_button);

        armbutton.setEnabled(true);
        disarmbutton.setEnabled(false);
        trackbutton.setEnabled(true);
        parentbutton.setEnabled(true);
        turnoffbutton.setEnabled(true);
        databutton.setEnabled(true);

    }
    public void turnArmed(String text){
        VariablesSingleton.getInstance().setMODE("ARMED");
        String[] coords = text.split(",");
        float ticks = Float.parseFloat(coords[1]);
        double total = (4400.0 - 0.1707*ticks)/44.00;
        int total_ = (int)(total + 0.5);

        VariablesSingleton.getInstance().setBATTERY(Integer.toString(total_));
        TextView t = (TextView)findViewById(R.id.textView);
        t.setText(VariablesSingleton.getInstance().getBATTERY()+"%");

        Button armbutton = (Button)findViewById(R.id.button);
        Button disarmbutton = (Button)findViewById(R.id.button2);
        Button trackbutton = (Button)findViewById(R.id.button3);
        Button parentbutton = (Button)findViewById(R.id.button4);
        Button alarmbutton = (Button)findViewById(R.id.button5);
        Button turnoffbutton = (Button)findViewById(R.id.button6);

        armbutton.setEnabled(false);
        disarmbutton.setEnabled(true);
        trackbutton.setEnabled(true);
        alarmbutton.setEnabled(true);
        parentbutton.setEnabled(true);
        turnoffbutton.setEnabled(true);

    }
}
