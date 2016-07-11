package com.es.sensors.busticket;

import android.content.Intent;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.UUID;


public class DisplayActivity extends ActionBarActivity {
    // MQTT server address
    private static final String HOST = "tcp://192.168.160.71:1884";
    //private static final String HOST = "tcp://192.168.1.75:1883";

    // Generates UUID to client
    // TODO: this functionality was not properly implemented. Randomly generated for testing
    private static final String clientId = UUID.randomUUID().toString();

    private static final String logTag = "DispActivity";

    private ArrayList<NdefRecord> ndefRecordList = new ArrayList<>();
    private byte[] token = null;

    // Initializes MQTT client
    private MqttAndroidClient mqttClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        Intent validationIntent = getIntent();

        // Retrieves the 'enter|leave' action from the key 'result' in intent extras
        String result = validationIntent.getExtras().getString("result");
        // Retrieves ndefRecordList (tokens) from the key 'list' in intent extras
        ndefRecordList = validationIntent.getExtras().getParcelableArrayList("list");
        // Retrieves token bytes from the key 'token' in intent extras
        token = validationIntent.getExtras().getByteArray("token");

        // get TextView
        TextView resultText = (TextView) findViewById(R.id.resultText);

        JSONObject jsonObject = new JSONObject();

        // if leaving the bus
        if (result.equals("leave")) {
            try {
                jsonObject.put("status", "ok");
                jsonObject.put("token", byteArrayToHexString(token));
            } catch (JSONException e) {
                Log.d(logTag + ".onCreate",
                        "Error when creating token String for action '" + result + "'",
                        e);
            }

            resultText.setText("Tanks for traveling with US.");

            // publish to the MQTT broker
            publish("./DC/BUS/Exit", jsonObject.toString());

        // if entering the bus
        } else if (result.equals("enter")) {
            try {
                jsonObject.put("status", "ok");
                jsonObject.put("token", byteArrayToHexString(token));
            } catch (JSONException e) {
                Log.d(logTag + ".onCreate",
                        "Error when creating token String for action '" + result + "'",
                        e);
            }

            resultText.setText("Welcome. Have a nice travel!");

            // publish to the MQTT broker
            publish("./DC/BUS/Entrance", jsonObject.toString());
        } else {
            resultText.setText("Your ticket is not valid.");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display, menu);
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

    @Override
    protected void onResume() {
        super.onResume();

        // waints 5 seconds in the welcome/goodbye screen and goes back to the main activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(DisplayActivity.this, MainActivity.class);

                mainIntent.putExtra("list", ndefRecordList);
                startActivity(mainIntent);
                finish();
            }
        }, 5000);
    }

    /**
     * Publishes a message to a given topic for the host specified in HOST static var
     *
     * @param topic     Tópico de destino da mensagem.
     * @param message   Conteúdo da mensagem a ser enviada.
     */
    protected void publish(final String topic, final String message) {
        // LOG
        Log.d(logTag + ".publish", "MQTT Start");

        MemoryPersistence memoryPersistence = new MemoryPersistence();

        mqttClient = new MqttAndroidClient(getApplicationContext(), HOST, clientId, memoryPersistence);

        try {

            // Tries to connect with the MQTT broker
            mqttClient.connect(getApplicationContext(), new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken mqttToken) {
                    Log.d(logTag + ".onPubSuc", " MQTT Client connected");

                    try {
                        // Publishes the message (topic (String), message (byte[]), qos (0|1|2), retained (true|false))
                        mqttClient.publish(topic, message.getBytes(), 2, false);

                        Log.d(logTag + ".onPubSuc", "MQTT Message published");

                        // desliga
                        mqttClient.disconnect();
                        Log.d(logTag + ".onPubSuc", "client disconnected");
                    } catch (MqttPersistenceException e) {
                        Log.d(logTag + ".onPubSuc", "MQTT message could not be published", e);
                    } catch (MqttException e) {
                        Log.d(logTag + ".onPubSuc", "MQTT message could not be published", e);
                    }
                }

                @Override
                public void onFailure(IMqttToken arg0, Throwable throwable) {
                    Log.d(logTag, "MQTT Client connection failed: " + throwable);

                }
            });
        } catch (MqttSecurityException e) {
            Log.d(logTag, "MQTT Client connection failed: " + e);
        } catch (MqttException e) {
            Log.d(logTag, "MQTT Client connection failed: " + e);
        }

    }

    /**
     * Converts a byte array to an hexadecimal string
     *
     * @param bytes     Array com bytes a converter
     * @return          String com representação hexadecial dos bytes convertidos (incluindo zeros à esquerda)
     */
    protected String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return sb.toString();
    }
}