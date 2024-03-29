package com.example.monangesmsresponder;
// @source: https://android.jlelse.eu/detecting-sending-sms-on-android-8a154562597f
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.example.monangesmsresponder.tools.Tools;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;

    private Intent serviceIntent;

    public int state = -1;
    public boolean once = false;
    public String once_str = "";
    public boolean routine = true;

    public Tools tools = new Tools();


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        closeServiceNotification();
        // TODO update the switch routine
        ((Switch) findViewById(R.id.switch_routine)).setChecked(routine);
        checkForSmsPermission();
    }

    public void onRestart() {
        super.onRestart();
        serviceIntent = new Intent(MainActivity.this, NotificationService.class);
        stopService(serviceIntent);
        // Load preferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Update once variables
        once = preferences.getBoolean("once",false);
        once_str = preferences.getString("once_str", "");
        // Update TextInput and button once color
        ((TextInputEditText) findViewById(R.id.once_input)).setText(once_str);
        updateButtonOnceColor();

        // TODO Update state because routine
        if(routine) {
            state = (Integer) (tools.getStateUsingRoutine().get(0));
        }
        //state = preferences.getInt("state",-1);
        if(state != -1) {
            updateButtonsColor(state);
        }
        // TODO update the switch routine or not
        ((Switch) findViewById(R.id.switch_routine)).setChecked(routine);
        Log.d("restart","ok");
    } // onRestart()


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        routine = ((Switch) findViewById(R.id.switch_routine)).isChecked();
        if (state != -1 || routine) {
            serviceIntent = new Intent(MainActivity.this, NotificationService.class);
            if(routine) {
                state = (Integer) (tools.getStateUsingRoutine().get(0));
                serviceIntent.putExtra("state", state);
                serviceIntent.putExtra("state_str", ((Button) findViewById(state)).getText());
                //Log.d("Test", (String) ((Button) findViewById(state)).getText());
            }
            if(state != -1) {
                serviceIntent.putExtra("state", state);
                serviceIntent.putExtra("state_str", ((Button) findViewById(state)).getText());
            }
            serviceIntent.putExtra("once", once);
            serviceIntent.putExtra("once_str", (((EditText) findViewById(R.id.once_input)).getText().toString()));
            serviceIntent.putExtra("routine", routine);

            //Log.d("onSaveInstanceState", String.valueOf(once));
            //Log.d("onSaveInstanceState", ((EditText) findViewById(R.id.once_input)).getText().toString());
            // Read the last sms received
            List<Sms> allSms = readAllSMSFromMonAnge(this);
            // get body of last message from MonAnge
            String lastSmsFromAnge = allSms.get(allSms.size() - 1).getBody();
            // put this message on an Extra called "lastsmsfrommonange"
            serviceIntent.putExtra("lastsmsfrommonange", lastSmsFromAnge);

            // Save preferences
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            // Once variables
            editor.putBoolean("once", once);
            editor.putString("once_str", (((EditText) findViewById(R.id.once_input)).getText().toString()));
            editor.apply();

            startService(serviceIntent);
        }
    } // onSaveInstanceState()

    @Override
    public void onDestroy() {
        super.onDestroy();
        // remove the service if apps closed
        closeServiceNotification();
    }

    private void closeServiceNotification() {

        if(isMyServiceRunning(NotificationService.class)) {
            // stop the service
            serviceIntent = new Intent(MainActivity.this, NotificationService.class);
            stopService(serviceIntent);
            // Load preferences
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            // Update once variables
            state = preferences.getInt("state", -1);
            if (state != -1) {
                updateButtonsColor(state);
            }
            routine = preferences.getBoolean("routine", true);
            //Toast.makeText(this, "Service is running with state:" + String.valueOf(state), Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void updateTextUsingLastSms() {
        // TODO le texte doit être mis à jour lors de l'appui d'un bouton
        //Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show();
    }

// https://google-developer-training.github.io/android-developer-phone-sms-course/Lesson%202/2_p_sending_sms_messages.html
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void checkForSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            //Log.d(TAG, getString(R.string.permission_not_granted));
            // Permission not yet granted. Use requestPermissions().
            // MY_PERMISSIONS_REQUEST_SEND_SMS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.SEND_SMS,
                                Manifest.permission.READ_SMS},
                    MY_PERMISSIONS_REQUEST_SEND_SMS);
        } else {
            // Permission already granted. Enable the SMS button.
            updateTextUsingLastSms();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void buttonClick(View view) {
        switch (view.getId()) {
            case R.id.button_miam:
                // update colors and variable state
                updateState(R.id.button_miam);
                updateButtonsColor(R.id.button_miam);
                //updateTextUsingLastSms();
                //Toast.makeText(this, "Button miam Clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button_work:
                // update colors and variable state
                updateState(R.id.button_work);
                updateButtonsColor(R.id.button_work);
                //updateTextUsingLastSms();
                //Toast.makeText(this, "Button work Clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button_shopping:
                // update colors and variable state
                updateState(R.id.button_shopping);
                updateButtonsColor(R.id.button_shopping);
                //updateTextUsingLastSms();
                //Toast.makeText(this, "Button shopping Clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button_guitar:
                // update colors and variable state
                updateState(R.id.button_guitar);
                updateButtonsColor(R.id.button_guitar);
                //updateTextUsingLastSms();
                //Toast.makeText(this, "Button guitar Clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button_workout:
                // update colors and variable state
                updateState(R.id.button_workout);
                updateButtonsColor(R.id.button_workout);
                //updateTextUsingLastSms();
                //Toast.makeText(this, "Button workout Clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.button_sleep:
                // update colors and variable state
                updateState(R.id.button_sleep);
                updateButtonsColor(R.id.button_sleep);
                //updateTextUsingLastSms();
                //Toast.makeText(this, "Button sleep Clicked", Toast.LENGTH_SHORT).show();
                break;

            case R.id.button_once:
                // update colors and variable once
                updateOnce();
                updateButtonOnceColor();
                //updateTextUsingLastSms();
                TextInputEditText tiet = findViewById(R.id.once_input);
                Toast.makeText(this, "Button once Clicked with sms \n" + tiet.getText(), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void updateState(int id) {
        this.state = id;
    }

    public void updateOnce() {
        if (!this.once) this.once = true;
        else this.once = false;
    }

    public void updateDefaultButtonsColor() {
        Button button = findViewById(R.id.button_once);
        button.setBackgroundColor(Color.parseColor("#69F0AE"));
        button = findViewById(R.id.button_miam);
        button.setBackgroundColor(Color.parseColor("#8BC34A"));
        button = findViewById(R.id.button_guitar);
        button.setBackgroundColor(Color.parseColor("#E64A19"));
        button = findViewById(R.id.button_work);
        button.setBackgroundColor(Color.parseColor("#80D8FF"));
        button = findViewById(R.id.button_workout);
        button.setBackgroundColor(Color.parseColor("#FF4081"));
        button = findViewById(R.id.button_shopping);
        button.setBackgroundColor(Color.parseColor("#FFFF00"));
        button = findViewById(R.id.button_sleep);
        button.setBackgroundColor(Color.parseColor("#536DFE"));
    }

    public void updateButtonOnceColor() {
        if(!once && state == -1) {
            // set default color
            //Toast.makeText(this, "Set default color", Toast.LENGTH_SHORT).show();
            updateDefaultButtonsColor();
        }
        else if(!once && state != -1) {
            Button button = findViewById(R.id.button_once);
            button.setBackgroundColor(Color.GRAY);
        }
        else if(once && state == -1) {
            // case no buttons state clicked and button once clicked
            updateButtonsColor(R.id.button_once);
        }
        else if (once && state != 1) {
            // case no buttons state clicked and button once clicked
            Button button = findViewById(R.id.button_once);
            button.setBackgroundColor(Color.GREEN);
        }
    }

    public void updateButtonsColor(int id) {
        // update all buttons color to gray
        ViewGroup layout = findViewById(R.id.main_layout);
        for (int i = 0; i < layout.getChildCount(); i++) {

            View child = layout.getChildAt(i);
            if(child instanceof Button)
            {
                Button button = (Button) child;
                if((button.getId() == R.id.button_once && once) || button.getId() == R.id.switch_routine) {
                    // nothing to do
                }
                else {
                    button.setBackgroundColor(Color.GRAY);
                }
            }
        }
        // update current button state color to green
        Button button = findViewById(id);
        button.setBackgroundColor(Color.GREEN);
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public List<Sms> readAllSMSFromMonAnge(Context context) {
        // create a sms list
        List<Sms> allSMSFromMonAnge = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(Telephony.Sms.CONTENT_URI, null, null, null, null);
        int totalSMS;
        if (c != null) {
            totalSMS = c.getCount();
            if (c.moveToFirst()) {
                for (int j = 0; j < totalSMS; j++) {
                    String smsDate = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    String number = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    Date date= new Date(Long.valueOf(smsDate));
                    @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                    String strDate = dateFormat.format(date);

                    String type;
                    switch (Integer.parseInt(c.getString(c.getColumnIndexOrThrow(Telephony.Sms.TYPE)))) {
                        case Telephony.Sms.MESSAGE_TYPE_INBOX:
                            type = "inbox";
                            if(number.equals("+33646729562")) {
                                allSMSFromMonAnge.add(new Sms(number, strDate, body));
                                java.util.Collections.sort(allSMSFromMonAnge, new smsComparator());
                            }
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_SENT:
                            type = "sent";
                            break;
                        case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                            type = "outbox";
                            break;
                        default:
                            break;
                    }
                    c.moveToNext();
                }
            }

            c.close();

        } else {
            Toast.makeText(context, "No message to show!", Toast.LENGTH_SHORT).show();
        }

        // return the sms list
        return allSMSFromMonAnge;
    }

    // https://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
