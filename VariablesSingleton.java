package com.example.zita.bikeapplication;

import android.telephony.SmsManager;

import com.google.android.gms.maps.model.LatLng;

public class VariablesSingleton {

    private int RADIUS = 50;
    private String MODE = "ON";
    private boolean isON = true;
    private boolean ALARM = false;
    private boolean SENDGPS = false;
    private String BATTERY = "100";
    private LatLng REFCOORDS = null;
    private String phone = "+19517437456";
    private boolean SENDDATA = false;



    static VariablesSingleton obj = new VariablesSingleton();
    private VariablesSingleton(){}

    public static VariablesSingleton getInstance(){return obj;}

    public int getRADIUS() {
        return RADIUS;
    }
    public String getMODE() {
        return MODE;
    }
    public boolean getisON(){
        return isON;
    }
    public boolean getALARM(){
        return ALARM;
    }
    public boolean getSENDGPS(){
        return SENDGPS;
    }
    public String getBATTERY(){return BATTERY;}
    public LatLng REFCOORDS(){return REFCOORDS;}
    public boolean SENDDATA(){return SENDDATA;}

    public void setALARM(boolean ALARM) {
        this.ALARM = ALARM;
    }
    public void setMODE(String MODE) {
        this.MODE = MODE;
    }
    public void setON(boolean isON) {
        this.isON = isON;
    }
    public void setRADIUS(int RADIUS) {
        this.RADIUS = RADIUS;
    }
    public void setSENDGPS(boolean SENDGPS) {
        this.SENDGPS = SENDGPS;
    }
    public void setBATTERY(String battery){this.BATTERY = battery;}
    public void setREFCOORDS(LatLng refcoords){this.REFCOORDS = refcoords;}
    public void setSENDDATA(boolean senddata){this.SENDDATA = senddata;}

    public String getTEXT(){
        String text;
        if(RADIUS < 100) text = "0" + RADIUS;
        else text = "" + RADIUS;

        if(SENDGPS) {
            text = "1" + text;
        }
        else text = "0" + text;

        if(ALARM) text = "1" + text;
        else text = "0" + text;

        text = "0" + text;

        if(SENDGPS) text = "9" + text;
        else if(MODE.equals( "ARMED")) text = "1" + text;
        else if(MODE.equals( "PARENT")) text = "2" + text;
        else if(MODE.equals( "STOLEN")) text = "3" + text;
        else if(MODE.equals("ON")) text = "4" + text;
        else if(MODE.equals("OFF"))text = "5" + text;
        else if(MODE.equals("RIDING")) text = "9" + text;

        if(SENDDATA) text = "1" + text;
        else text = "0" + text;

        if(MODE.equals( "PARENT")){
            double lon = REFCOORDS.longitude;
            double lat = REFCOORDS.latitude;
            text = text + getCorrectFormatting(lon) + "," + getCorrectFormatting(lat);
        }
        return text;
    }
    public void sendTEXT(){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone,null,"?" + VariablesSingleton.getInstance().getTEXT(),null,null);
        SENDGPS = false;
        SENDDATA = false;
    }
    private String getCorrectFormatting(double a){
        if(a<-99)return String.format("%.4f",a);
        else if(a<-9){
            return "-0" + String.format("%.4f",a*-1);
        }else if(a<0){
            return "-00" + String.format("%.4f",a*-1);
        }else if(a==0){
            return "000.0000";
        }else if (a<10){
            return "+00" + String.format("%.4f",a);
        }else if( a< 100){
            return "+0" + String.format("%.4f",a);
        }else return String.format("%.4f",a);
    }

}
