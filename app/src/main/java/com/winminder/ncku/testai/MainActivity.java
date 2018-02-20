package com.winminder.ncku.testai;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements AIListener {

    private String TAG = MainActivity.class.getSimpleName();
    private ProgressDialog pDialog;
    private ListView lv;
    private AIService aiService;
    private Button listenButton;
    private TextView resultTextView;
    private String queryMrt = "";
    final AIConfiguration config = new AIConfiguration("d69b2f7fcbb54e59a08dc7ab13bea231",
            AIConfiguration.SupportedLanguages.ChineseTaiwan,
            AIConfiguration.RecognitionEngine.System);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);

        listenButton = (Button) findViewById(R.id.listenButton);
        resultTextView = (TextView) findViewById(R.id.resultTextView);

        lv = (ListView) findViewById(R.id.list);
    }

    public void listenButtonOnClick(final View view) {
        aiService.startListening();
    }

    public void onResult(final AIResponse response) {
        Result result = response.getResult();

        // Get parameters
        String parameterString = "";
        String station = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
                station = entry.getValue().toString();

            }


        }


        queryMrt = "Query:" + result.getResolvedQuery() +
                "\nAction: " + result.getAction() +
                "\nParameters: " + parameterString;
        // Show results in TextView.
        resultTextView.setText(queryMrt);

        int id = 0;
        resultTextView.setText(station);
        switch (station) {

            case "\""+"技擊館"+"\"":
                Toast.makeText(getApplicationContext(), "查詢技擊館動態", Toast.LENGTH_SHORT).show();
                id = 208;
                break;

        }

        if(id>0){
            new getCertainTrain(id).execute();
        }else {
            Toast.makeText(getApplicationContext(), "查無結果", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onError(final AIError error) {
        resultTextView.setText(error.toString());
    }

    @Override
    public void onListeningStarted() {
    }

    @Override
    public void onListeningCanceled() {
    }

    @Override
    public void onListeningFinished() {
    }

    @Override
    public void onAudioLevel(final float level) {
    }


    /**
     * Async task class to get json by making HTTP call
     */
    private class getCertainTrain extends AsyncTask<Void, Void, Void> {

        // URL to get contacts JSON
        //String url = "https://api.androidhive.info/contacts/";

        int id;
        String url="";
        ArrayList<HashMap<String, String>> contactList = new ArrayList<>();

        public getCertainTrain(int id){
            this.url = "http://data.kaohsiung.gov.tw/Opendata/MrtJsonGet.aspx?site="+id;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Getting JSON Array node
                    JSONArray contacts = jsonObj.getJSONArray("MRT");

                    // looping through All Contacts
                    for (int i = 0; i < contacts.length(); i++) {
                        JSONObject c = contacts.getJSONObject(i);

                        String descr = c.getString("descr");
                        String arrival = c.getString("arrival");
                        String next_arrival = c.getString("next_arrival");

                        // tmp hash map for single contact
                        HashMap<String, String> contact = new HashMap<>();

                        // adding each child node to HashMap key => value
                        contact.put("descr", descr);
                        contact.put("arrival", arrival);
                        contact.put("next_arrival", next_arrival);

                        // adding contact to contact list
                        contactList.add(contact);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });

                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server. Check LogCat for possible errors!",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            ListAdapter adapter = new SimpleAdapter(MainActivity.this, contactList, R.layout.list_item,
                    new String[]{"descr", "arrival", "next_arrival"}, new int[]{R.id.descr, R.id.arrival, R.id.next_arrival});
            lv.setAdapter(adapter);
        }

    }

}
