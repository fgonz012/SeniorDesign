#include <SoftwareSerial.h>
#include <String.h>
#include <Adafruit_GPS.h>
#include <math.h>
#include <EEPROM.h>
SoftwareSerial GPRS(7,8);
SoftwareSerial GPSserial(5,4);
Adafruit_GPS GPS(&GPSserial);
int buzzer = 12;

#define GCD 1


String NMEA1;
String NMEA2;
float latitude=0.0;
float longitude=0.0;
float deg;
float degwhole;
float degdeci;
float reference_latitude = NULL;
float reference_longitude = NULL;
bool sendArmedFlag = false;

int ms_to_next_text;
boolean isSending = false;
boolean sendTurnOnText = false;
int pinON_Indicator = 19;
int pinPOLARITY_Indicator = 6;
int pinCANARM_Indicator = 18;
int pinGPSEN_Indicator = 10;
int GPRS_mem_index = 1;
int arm = 10;

//Data points
int CREG = -1;
int CSQ = -1;
int BER = -1;
int ticks = 0;
float current_stolen = 0;
float current_armed = 0;
float current_on = 0;
float current_off = 0;
float current_parent = 0;
float current_charging = 0;

//$ left
//operating_for = millis()
//batt life is 


bool turnON_flag = false;
double total_distance = 0;
double reference_distance = NULL;
int previous_time = NULL;

enum modes{OFF,ARMED,CHILD,STOLEN,ON, RIDING}mode = ON;

//Constants received from app
int CHILD_SAFETY_RADIUS = 300;
int SEND_GPS_FLAG = 0; //flag
int SEND_DATA_FLAG = 0;
int SOUND_ALARM_FLAG = 0;
int TURN_OFF_FLAG = 0;

int SENDING_TXT = 0;
int sound_alarm = 0;


boolean isMsgAvailable = false;
boolean isMsgAvailable2 = false;
String msg2;
String msg;

bool sendStartFlag = false;
/*  Continously checks to see if there is a msg to be sent by checking flag isMsgAvailable
 *  If there is a message to be sent it sends the message over the course of 0.3s
 *  sendMSG is used to send to send the msg
 */
int sendTXT=0;
int sendTXT_SM(int state){//Sends a text whenever isMsgAvailable is true. The msg should be
  static int i = 0;       //stored in the msg variable. It takes 300ms to send a msg.
  switch(state){
    case 0:
      if((isMsgAvailable || sendStartFlag || sendArmedFlag) && CREG == 1){
        
        isSending = true;
        GPRS.println("AT+CMGS = \"+14242039916\"");
        state = 1;
      }
    break;
    case 1:
      if(i>=100){
        state = 0;
        if(sendStartFlag && !sendArmedFlag){
          Serial.println("Strt");
          GPRS.println("1," + String(ticks) + String((char)26));
          sendStartFlag = false;
        }else if(sendArmedFlag && !sendStartFlag){
          Serial.println("Armd");
          GPRS.println("4," + String(ticks)+String((char)26));
          sendArmedFlag = false;
        }else if(sendArmedFlag && sendStartFlag){
          GPRS.println("5," + String(ticks)+String((char)26));
          sendArmedFlag = false;
          sendStartFlag = false;
        }else{
          GPRS.println(msg+String((char)26));
          isMsgAvailable = false;
        }
        isSending = false;
        i=0;
      }else i++;
      
    break;
  }
  return state;
}

bool sendMSG(String s){
    isMsgAvailable = true;
    msg=s;
    return true;
}


void sendTEXT_STOLEN(){//When in stolen mode, sends a msg continously every ms_to_next_text ms
  SetCoordinates();
  if(ms_to_next_text-- <= 0 && GPS.satellites >= 4 ){
    sendMSG("?" + String(longitude,5) + "," + String(latitude,5) + "," + String(ticks));
    ms_to_next_text = 4;
  }
  Serial.println("A");
}

void flushBuffer(char* b){
  int cnt = sizeof(b)/sizeof(char);
  for(int i=0; i<cnt; i++){
    b[i] = NULL;
  }
}

void flushGPRS(){
  while(GPRS.available()){
    char c = GPRS.read();
  }
}
void readAll(){
  if(isSending)return;
  static int i = 0;
  static int j = 0;
  if(i>1234){
    //These tasks are performed every (i>x)[from above] x times along with everythigng else
    flushGPRS();
    switch(j){
      case 0:{
        
        if(GPRS_mem_index >= 15){
          GPRS.println("AT+CMGD=1,3");
          char temp_buffer[50];
          int cnt = 0;
          unsigned long time_in = millis();
          while(1){
            if(GPRS.available()){// if GPRS has something to say
              char c = GPRS.read();
              temp_buffer[cnt++] = c;
              Serial.write(c);
              if(c == '\n'){// if its a newline character then we've read a sentence and we have to parse it out
                if(char *p = strstr(temp_buffer,"OK")){// if it has OK in it then it is done
                  GPRS_mem_index = 1;
                  break;
                  
                }else{
                  flushBuffer(temp_buffer);
                  cnt=0;
                }
                
              }
            }
            if( (millis() - time_in) > 10000) break;
          }
          GPRS_mem_index = 1;
        }else if( mode == CHILD || mode == ARMED){
          childMode();
        }else if( mode == STOLEN){
          sendTEXT_STOLEN();
        }else if( mode == ON){
          
          SetCoordinates();
        }
        j=1;
        break;
      }
      case 1:{
        GPRS.println("AT+CMGR="+String(GPRS_mem_index));
        char temp_buffer[200];
        int cnt = 0;
        unsigned long time_in = millis();
        while(1){
          if(GPRS.available()){// if GPRS has something to say
            char c = GPRS.read();
            temp_buffer[cnt++] = c;
            Serial.write(c);
            if(c == '\n'){// if its a newline character then we've read a sentence and we have to parse it out
              if(char *p = strstr(temp_buffer,"OK")){// if it has OK in it then it is done
                break;
                
              }else if(p = strchr(temp_buffer,'?')){// if it has '?' then it is an instruction and we have to parse that
                applySettings(p+1);
                GPRS_mem_index++;
                break;
                
              }else{
                flushBuffer(temp_buffer);
                cnt=0;
              }
              
            }
          }
          if( (millis() - time_in) > 10000) break;
        }
        j=2;
        break;
      }
      case 2:{
        GPRS.println("AT+CSQ");
        char temp_buffer[50];
        int cnt = 0;
        unsigned long time_in = millis();
        while(1){
          if(GPRS.available()){
            char c = GPRS.read();
            temp_buffer[cnt++] = c;
            Serial.write(c);
            if(c == '\n'){
              char *p;
              if(p = strstr(temp_buffer,"OK")){
                break;
                
              }else if(p = strstr(temp_buffer,"+CSQ: ")){
                parseCSQ(p+6);
                break;
              }else{
                flushBuffer(temp_buffer);
                cnt = 0;
              }
              
            }
          }
          if( (millis() - time_in) > 10000) break;
        }
        j=3;
        break;
      }
      case 3:
        GPRS.println("AT+CREG?");
        char temp_buffer[50];
        int cnt = 0;
        unsigned long time_in = millis();
        while(1){
          if(GPRS.available()){
            char c = GPRS.read();
            temp_buffer[cnt++] = c;
            Serial.write(c);
            if(c == '\n'){
              char *p;
              if(p = strstr(temp_buffer,"OK")){
                break;
                
              }else if(p = strstr(temp_buffer,"+CREG: ")){
                parseCREG(p+7);
                break;
              }else{
                flushBuffer(temp_buffer);
                cnt = 0;
              }
              
            }
          }
          if( (millis() - time_in) > 10000) break;
        }
        j=0;
      break;
    }
    i=0;
  }else i++;
}

void parseCREG(char* p){
  CREG = p[2] - '0';
}

void sendDataOnce(){
  String s= "2," + String(BER) + "," 
              + String(CSQ) + ","
              + String(GPS.satellites) + "," 
              + String(ticks) + ","
              + String(current_off,0) + ","
              + String(current_armed,0) + ","
              + String(current_stolen,0) + ","
              + String(current_parent,0) + ","
              + String(current_on,0);
  SEND_DATA_FLAG = 0;
  sendMSG(s);
}

int getRadius(int sat){
  if(mode == CHILD) return CHILD_SAFETY_RADIUS;

  if(sat < 9)return 40;
  else if(sat < 20)return 30;
  else return 20;
}

enum childMode_States{Searching, Armed} childMode_State = Searching;
int numSat_temp = 0;
void childMode(){// run while in CHILD MODE aka PARENT MODE, same as the function above except with CHILD_SAFETY RADIUS 
      static int i = 0;
      SetCoordinates();
      if(!GPS.fix)GPS.satellites = 0;
      Serial.println("ARMD");

      switch(childMode_State){
        case Searching:
          if(GPS.satellites > numSat_temp){
            reference_latitude = latitude;
            reference_longitude = longitude;
            numSat_temp = GPS.satellites;
          }
          Serial.println(i);
          if(i++ > 0){
            if(numSat_temp > 5){
              childMode_State = Armed;
              i=0;
            }
          }
        break;
        case Armed:
        digitalWrite(arm,HIGH);
        Serial.println("Ref:" + String(getRadius(numSat_temp)));
          if(getDistance(reference_latitude,reference_longitude) > getRadius(numSat_temp)) {
          mode = STOLEN;
          Serial.println("Md:STLN");
          reference_latitude = NULL;
          reference_longitude = NULL;
          numSat_temp=0;
          childMode_State = Searching;
        }
        break;
      }
}


void setup() {
  
  /*EEPROM.write(10, 0x00);
  EEPROM.write(11, 0x00);
  EEPROM.write(12, 0x00);
  EEPROM.write(13, 0x00);

  while(1);*/
  ticks = readTicksEEPROM();
  ms_to_next_text = 1;


  //Attach pins
  attachInterrupt(0,coulombCounter,LOW);
  pinMode(3,OUTPUT);//pin 3 is interrupt 1
  pinMode(arm,INPUT);
  digitalWrite(arm,HIGH);
  digitalWrite(3,HIGH);
  attachInterrupt(1,turnON_ISR,LOW);
  pinMode(pinPOLARITY_Indicator,INPUT);
  pinMode(pinON_Indicator,OUTPUT);
  pinMode(pinCANARM_Indicator,OUTPUT);
  pinMode(buzzer,OUTPUT);
  pinMode(pinGPSEN_Indicator,INPUT);
  
  GPRS.begin(19200); // Set up the different rates
  GPS.begin(9600);
  Serial.begin(38400);
  
  /*GPRS.println("AT+CMGF=1\r"); // Set the shield to SMS mode
  delay(1000);
  GPRS.println("AT+CMGDA = \"DEL ALL\"");
  delay(1000);*/
  const String a = "AT+CMGD=1,3";
  GPRS.println(a);
  GPS.sendCommand("$PGCMD,33,0*6D");//needed commands for the GPS to only send some data
  GPS.sendCommand(PMTK_SET_NMEA_UPDATE_10HZ);
  GPS.sendCommand(PMTK_SET_NMEA_OUTPUT_RMCGGA);
  GPRS.listen();

}


void sendGP(){//Sends 1 text when SEND_GPS_FLAG is on
  if(GPS.satellites > 5){
        msg = "?" + String(longitude,8) + "," + String(latitude,8) + "," + String(ticks);
        isMsgAvailable = true;
        SEND_GPS_FLAG = 0;
  }
}


void loop() {
  digitalWrite(arm,HIGH);
  if(digitalRead(arm) == LOW) Serial.println("LOWA");
  if(mode != OFF){
    
    readAll();
    buttonAndLightFunction();
    requestHandler();
    
    //receiveTXT = receiveTXT_SM(receiveTXT);
    sendTXT = sendTXT_SM(sendTXT);
  }else if( turnON_flag){
    turnON();
    turnON_flag = false;
  }
  delay(GCD);
}

void wTicks(){
    writeTicksEEPROM();
}


void SetCoordinates()
{
  float deg;
  float degwhole;
  float degdeci;
  
  GPSserial.listen();
  readGPS();
  GPRS.listen();
  if (GPS.fix == 1){
    
    degwhole = float(int(GPS.longitude/100));// Get longitude
    degdeci = (GPS.longitude - degwhole*100.0)/60.0;
    longitude = degwhole + degdeci;
    if(GPS.lon == 'W'){ longitude *= -1.00;}

    degwhole = float(int(GPS.latitude/100));   //Get latitude
    degdeci = (GPS.latitude - degwhole*100.0)/60.0;
    latitude = degwhole + degdeci;
    if(GPS.lat == 'S') { latitude *= -1.00;}
    Serial.println("Sat :" + String(GPS.satellites));
  }else{
    Serial.println("Not Fixed");
  }
}

void readGPS(){
  
  while(!GPS.newNMEAreceived()){
    char c=GPS.read();
  }
  GPS.parse(GPS.lastNMEA());
  NMEA1=GPS.lastNMEA();

  while(!GPS.newNMEAreceived()){
    char c=GPS.read();
  }
  GPS.parse(GPS.lastNMEA());
  NMEA2=GPS.lastNMEA();
}


void applySettings( char* msg){
  int m0 = msg[0]-'0';
  int m1 = msg[1]-'0';
  int m2 = msg[2]-'0';
  int m3 = msg[3]-'0';
  int m4 = msg[4]-'0';
  int m5 = msg[5]-'0';
  int m6 = msg[6]-'0';
  int m7 = msg[7]-'0';

  SEND_GPS_FLAG = m4;
  SEND_DATA_FLAG = m0;
  CHILD_SAFETY_RADIUS = 100*m5 + 10*m6 + m7;

 
 
 if(m1==1){
  Serial.println("Md:ARMD");
    mode = ARMED;
    
 }else if(m1==2){
  Serial.println("Md:PRNT");
    mode = CHILD;
    double multiplier = 1;
    if(msg[8] == '-')multiplier = -1;
    int m9 = msg[9] - '0';
    int m10 = msg[10] - '0';
    int m11 = msg[11] - '0';

    int m13 = msg[13] - '0';
    int m14 = msg[14] - '0';
    int m15 = msg[15] - '0';
    int m16 = msg[16] - '0';

    double multiplier_2 = 1;
    if(msg[18] == '-') multiplier_2 = -1;
    int m19 = msg[19] - '0';
    int m20 = msg[20] - '0';
    int m21 = msg[21] - '0';

    int m23 = msg[23] - '0';
    int m24 = msg[24] - '0';
    int m25 = msg[25] - '0';
    int m26 = msg[26] - '0';

    reference_longitude = multiplier*(100*m9 + 10*m10 + m11 + 0.1*m13 + 0.01*m14 + 0.001*m15 + 0.0001*m16);
    reference_latitude = multiplier_2*(100*m19 + 10*m20 + m21 + 0.1*m23 + 0.01*m24 + 0.001*m25 + 0.0001*m26);
 }else if(m1==3){
  Serial.println("Md:STLN");
    mode = STOLEN;
 }else if(m1==4){
    childMode_State = Searching;
    numSat_temp = 0;
    Serial.println("Md:ON");
    mode = ON;
    
    reference_latitude = NULL;
    reference_longitude = NULL;
 }else if(m1 == 5){
    childMode_State = Searching;
    Serial.println("Md:OFF");
    mode = OFF;
    turnOFFF();
 }

 SOUND_ALARM_FLAG = m3;
}

float getDistance( float lat, float lon){
  float dlon = (lon - longitude)*0.017453293;
  float dlat = (lat - latitude)*0.017453293;
  //Serial.println("dlon: " + String(dlon,15));

  float a = pow(sin(dlat/2.0000),2) + cos(lat)*cos(latitude)*pow(sin(dlon/2.0000),2);
  float c = 2 * asin(sqrt(a));
  float res = 6371000.00*c;
  Serial.println("d:" + String(res,5));
  return res;
}

double updateAverage(double S, double m, double avgC){
  //Serial.println("AvgC:" + String(((m-1)*S + avgC)/(m)));
  return ((m-1)*S + avgC)/(m) ;
}
void coulombCounter(){
  static int stolen_ticks = 0;
  static int armed_ticks = 0;
  static int off_ticks = 0;
  static int on_ticks = 0;
  static int parent_ticks = 0;
  static int charging_ticks = 0;
  
  static long int prev_time = 0;
  double dt = double(millis() - prev_time);
  prev_time = millis();
  
  Serial.println("ticks: " + String(ticks));
  double I = 614520/dt;
  if(digitalRead(pinPOLARITY_Indicator) == LOW){
    ticks = ( ticks >= 25776 ) ? 25776 : ticks+1;
    if(I>400)return;
    wTicks();
    if(mode == STOLEN) current_stolen = updateAverage(current_stolen,++stolen_ticks,I);
    else if(mode == ARMED) current_armed = updateAverage(current_armed,++armed_ticks,I);
    else if(mode == OFF) current_off = updateAverage(current_off,++off_ticks,I);
    else if(mode == ON) current_on = updateAverage(current_on,++on_ticks,I);
    else if(mode == CHILD) current_parent = updateAverage(current_parent,++parent_ticks,I);
  }else{
    
    ticks = (ticks <= 0) ? 0 : ticks-1;
    wTicks();
    current_charging = updateAverage(current_charging,++charging_ticks,I);
  }
}
void powerUpDown(uint8_t pin){
  digitalWrite(pin,LOW);
  delay(1000);
  digitalWrite(pin,HIGH);
  delay(2000);
  digitalWrite(pin,LOW);
  delay(3000);
}

void turnOFFF(){
  reference_latitude = NULL;
  reference_longitude = NULL;
  CSQ = -1;
  CREG = 0;
  digitalWrite(pinON_Indicator,LOW);
  pinMode(pinGPSEN_Indicator,OUTPUT);
  digitalWrite(pinGPSEN_Indicator,LOW);
  powerUpDown(9);
  Serial.println("Md:OFF");
  mode = OFF;
  CSQ = 0;
}
void turnON_ISR(){
  turnON_flag = true;
}
void turnON(){
    //Serial.println("Turning ON");
    mode = ON;
    Serial.println("Md:ON");
    powerUpDown(9);
    sendStartFlag = true;
    pinMode(pinGPSEN_Indicator,INPUT);
}

void parseCSQ(char* c){
  //Serial.println(c);
  if(c[2] == ','){
    int num1 = c[0] - '0';
    int num2 = c[1] - '0';
    int num3 = c[3] - '0';
    CSQ = num1*10 + num2;
    if(num3 < 9) BER =num3;
    else if( num3 == 9) BER = 99;
  }else if( c[1] == ','){
    int num1 = c[0] - '0';
    int num3 = c[2] - '0';
    CSQ = num1;

    if(num3 <9) BER = num3;
    else if(num3==9) BER = 99;
  }else{
    CSQ = -1;
  }
}
void flash_light1(int pin){
  static bool light = true;
  static int i = 0;

  if(i >500){
    if(light){
      digitalWrite(pin,LOW);
      light = false;
    }else{
      digitalWrite(pin,HIGH);
      light = true;
    }
    i=0;
  }else i++;
}
void flash_light(int pin){
  static bool light = true;
  static int i = 0;

  if(i >500){
    if(light){
      digitalWrite(pin,LOW);
      light = false;
    }else{
      digitalWrite(pin,HIGH);
      light = true;
    }
    i=0;
  }else i++;
}
void buttonAndLightFunction(){
  
  if(mode == ON){// lights the ON and CANARM lights
    digitalWrite(pinON_Indicator,HIGH);
    if( CREG == 1 && GPS.fix){
      digitalWrite(pinCANARM_Indicator,HIGH);
    }else{
       flash_light(pinCANARM_Indicator);
    }
  }else if(mode == OFF || mode == STOLEN){
    digitalWrite(pinON_Indicator,LOW);
    digitalWrite(pinCANARM_Indicator,LOW);
  }else if(mode == CHILD || mode == ARMED){
    if( CREG == 1 && GPS.fix){
      digitalWrite(pinCANARM_Indicator,HIGH);
    }else{
      flash_light(pinCANARM_Indicator);
     }
    flash_light1(pinON_Indicator);
  }
  
  if(SOUND_ALARM_FLAG || mode==STOLEN)digitalWrite(buzzer,HIGH);// Sounds the buzzer
  else digitalWrite(buzzer,LOW);

  if( digitalRead(arm) == LOW ) {
    if(mode == ON) {
      Serial.println("Md:RMD");
      sendArmedFlag = true;
    }
    mode = ARMED;
  }
  
  /*if(CSQ <= 0 || CSQ >= 99) digitalWrite(pinGPRS_StrengthIndicator,LOW);
  else digitalWrite(pinGPRS_StrengthIndicator,HIGH);*/
}

void requestHandler(){
  if(SEND_GPS_FLAG)sendGP();
  else if(SEND_DATA_FLAG)sendDataOnce();
}

int readTicksEEPROM(){
    long right_byte = EEPROM.read(10);
    long left_byte = EEPROM.read(11);
    
    return (right_byte & 0xFF) + ((left_byte << 8) & 0xFFFF); 
}

void writeTicksEEPROM(){

  byte right_byte = (ticks & 0xFF);
  byte left_byte = ((ticks >> 8) & 0xFF);
  
  EEPROM.write(10, right_byte);
  EEPROM.write(11, left_byte);
}







