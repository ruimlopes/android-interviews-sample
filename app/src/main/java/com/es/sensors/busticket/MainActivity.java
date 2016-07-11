package com.es.sensors.busticket;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {
    // NFC Adaptor
    private NfcAdapter nfcAdapter;
    // Intent Filter
    private IntentFilter[] writeTagFilters;
    // Intent
    private PendingIntent nfcPendingIntent;
    // Log Tag
    private String logTag = "MainActivity";

    // Passenger in and out list
    private ArrayList<NdefRecord> ndefRecordList;


    byte [] beamType = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent validation = getIntent();

        // Check it the app is being launched or if we're returning to Activity from another Activity
        if (validation.hasCategory(Intent.CATEGORY_LAUNCHER)) {
            ndefRecordList = new ArrayList<>();
            Log.d(logTag + ".onCreate", "NdefRecordList initilized empty");
        } else {
            ndefRecordList = validation.getExtras().getParcelableArrayList("list");
            Log.d(logTag + ".onCreate", "NdefRecordList initilized from previous activity");
        }

        // Initializes NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Initializes NFC tags filter
        nfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        // Filtros para os tipos de tags
        IntentFilter tagIntentFilter = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefIntentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techIntentFilter = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

        ndefIntentFilter.addCategory("DEFAULT");
        try {
            ndefIntentFilter.addDataType("text/plain");
            Log.d(logTag + ".onCreate", "NdefIntentFilter data type set as text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.d(logTag + ".onCreate", "NdefIntentFilter data type is invalid", e);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

        // Enable foreground dispatch to prioritize the activity then a NFC tag is discovered
        if (nfcAdapter != null)
            nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, writeTagFilters, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Inilializes intent Action and both final screen intent vlaues
        String intentAction = intent.getAction();
        String finalScreenIntentResultValue = "";
        byte[] finalScreenIntentToken = null;

        // Read Ndef message
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (intentAction.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            String intentType = intent.getType();
            Log.d(logTag + ".onNewIntent", "IntentAction is NDEF_DISCOVERED");

            if (intentType.equals("text/plain")) {
                Log.d(logTag + ".onNewIntent", "IntentType is text/plain");

                Ndef ndefMessage = Ndef.get(tag);
                if (ndefMessage == null) {
                    // NDEF is not supported by this Tag.
                    Toast.makeText(this, "The tag is Invalid", Toast.LENGTH_SHORT).show();

                    Log.d(logTag + ".onNewIntent", "NdefMessage could not be retrieved");

                    finalScreenIntentResultValue = "error";
                } else {

                    NdefMessage cachedNdefMessage = ndefMessage.getCachedNdefMessage();

                    if (cachedNdefMessage == null) {
                        Log.d(logTag + ".onNewIntent", "CachedMessage is null");
                        try {
                            cachedNdefMessage = ndefMessage.getNdefMessage();
                        } catch (IOException e) {
                            Log.d(logTag + ".onNewIntent", "cachedNdefMessage could not be retrieved", e);
                        } catch (FormatException e) {
                            Log.d(logTag + ".onNewIntent", "cachedNdefMessage could not be retrieved", e);
                        }
                    }

                    // Gets the old Ndef Message Records (cached ones)
                    NdefRecord[] cachedNdefMessageRecords = cachedNdefMessage.getRecords();

                    NdefRecord ticket = cachedNdefMessageRecords[cachedNdefMessageRecords.length - 1];

                    finalScreenIntentToken = ticket.getPayload();

                    // In case of Leaving the bus
                    if (ndefRecordList.remove(ticket)) {

                        finalScreenIntentResultValue = "leave";

                        Log.d(logTag + ".onNewIntent", "Leaving the bus." +
                                " Record[0] = " + cachedNdefMessageRecords[0].getPayload().toString() +
                                " Record[1] = " + cachedNdefMessageRecords[1].getPayload().toString() +
                                " BeamType = " + beamType.toString());
                        if (cachedNdefMessageRecords[0].getPayload().toString() != beamType.toString()) {
                            NdefRecord[] recordArray = new NdefRecord[cachedNdefMessageRecords.length - 1];

                            for (int i = 0; i < cachedNdefMessageRecords.length - 1; i++) {
                                recordArray[i] = cachedNdefMessageRecords[i];
                                Log.d(logTag + ".onNewIntent", "RecordArray["+ i + "] = " + recordArray[i].getPayload().toString());
                            }

                            NdefMessage message = new NdefMessage(recordArray);

                            try {
                                ndefMessage.connect();
                                ndefMessage.writeNdefMessage(message);
                                ndefMessage.close();
                            } catch (IOException e) {
                                Log.d(logTag + ".onNewIntent", "NdefMessage could not be retrieved", e);
                            } catch (FormatException e) {
                                Log.d(logTag + ".onNewIntent", "NdefMessage could not be retrieved", e);
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "Android Beam Test", Toast.LENGTH_SHORT).show();
                        }
                    // In case of entering the bus
                    } else {
                        ndefRecordList.add(ticket);

                        Toast.makeText(this, "Welcome on board!", Toast.LENGTH_SHORT).show();

                        finalScreenIntentResultValue = "enter";

                        Log.d(logTag + ".onNewIntent", "Entering the bus." +
                                " Record[0] = " + cachedNdefMessageRecords[0].getPayload().toString() +
                                " Record[1] = " + cachedNdefMessageRecords[1].getPayload().toString() +
                                " BeamType = " + beamType.toString());
                    }
                }
            }


        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intentAction)) {
            Log.d(logTag + ".onNewIntent", "Erro de validação");

            finalScreenIntentResultValue = "error";
        } else if (nfcAdapter.ACTION_TECH_DISCOVERED.equals(intentAction)) {
            // This functionality was not fully implemented
            Log.d(logTag + ".onNewIntent", "Testing Loyaty card functionality");
        }

        Intent finalScreenIntent = new Intent(this, DisplayActivity.class);

        Log.d(logTag + ".onNewIntent", "Building final screen intent." +
                " Result: " + finalScreenIntentResultValue +
                " Token: " + finalScreenIntentToken +
                " List: " + ndefRecordList.toString());

        finalScreenIntent.putExtra("result", finalScreenIntentResultValue);
        finalScreenIntent.putExtra("token", finalScreenIntentToken);
        finalScreenIntent.putParcelableArrayListExtra("list", ndefRecordList);
        startActivity(finalScreenIntent);
    }

}
