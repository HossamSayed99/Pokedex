package edu.harvard.cs50.pokedex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public class PokemonActivity extends AppCompatActivity {
    // This class is responsible for handling the first Json request with the url handeled from the Pokemon activity
    private class myJsonRequestListener implements Response.Listener<JSONObject>, Response.ErrorListener {
        @Override
        public void onResponse(JSONObject response) {
            try {
                name = response.getString("name");
                nameTextView.setText(name);
                int id = response.getInt("id");
                // This is the url for the second Json request to get the description about the pokemon
                String infoUrl = "https://pokeapi.co/api/v2/pokemon-species/" + id;
                Log.d("cs50", "info url:" + infoUrl);
                numberTextView.setText(String.format("#%03d", id));
                JSONArray typeEntries = response.getJSONArray("types");
                for (int i = 0; i < typeEntries.length(); i++) {
                    JSONObject typeEntry = typeEntries.getJSONObject(i);
                    int slot = typeEntry.getInt("slot");
                    String type = typeEntry.getJSONObject("type").getString("name");
                    if (slot == 1) {
                        type1TextView.setText(type);
                    } else if (slot == 2) {
                        type2TextView.setText(type);
                    }
                }
                // This is to check if the pokemon is caught or not
                isCaught = getPreferences(Context.MODE_PRIVATE).getString(name, "false");
                if (isCaught.equals("false"))
                    catchButton.setText("Catch");
                else
                    catchButton.setText("Release");
                isDataFetched = true;
                // This is for getting the picture
                JSONObject sprites = response.getJSONObject("sprites");
                String imageUrl = (String) sprites.getString("front_default");
                new DownloadSpriteTask().execute(imageUrl);
                // This is for getting the description
                getInfo info = new getInfo();
                JsonObjectRequest infoRequest = new JsonObjectRequest(Request.Method.GET, infoUrl, null, info, info);
                requestQueue.add(infoRequest);
            } catch (JSONException e) {
                Log.e("cs50", "Pokemon json error", e);
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("cs50", "Pokemon details error", error);
        }
    }

    // This class is to get the description of the pokemon
    private class getInfo implements Response.Listener<JSONObject>, Response.ErrorListener {

        @Override
        public void onResponse(JSONObject response) {
            try {
                JSONArray ftext = response.getJSONArray("flavor_text_entries");
                for (int i = 0; i < ftext.length(); i++) {
                    JSONObject result = ftext.getJSONObject(i);
                    if (result.getJSONObject("language").getString("name").equals("en")) {
                        String text = result.getString("flavor_text");
                        infoTextView.setText(text);
                        break;
                    }
                }
            } catch (JSONException e) {
                Log.d("cs50", "Could not get the info");
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e("cs50", "Pokemon info error", error);
        }
    }

    // This is for downloading the image of  the pokemon
    private class DownloadSpriteTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);
                return BitmapFactory.decodeStream(url.openStream());
            } catch (IOException e) {
                Log.e("cs50", "Download sprite error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // load the bitmap into the ImageView!
            pokemonImage.setImageBitmap(bitmap);
        }
    }

    private TextView nameTextView;
    private TextView numberTextView;
    private TextView type1TextView;
    private TextView type2TextView;
    private TextView infoTextView;
    private Button catchButton;
    private ImageView pokemonImage;
    private String url;
    private RequestQueue requestQueue;
    private String isCaught = "false";
    private String name;
    private boolean isDataFetched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pokemon);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        url = getIntent().getStringExtra("url");
        nameTextView = findViewById(R.id.pokemon_name);
        numberTextView = findViewById(R.id.pokemon_number);
        type1TextView = findViewById(R.id.pokemon_type1);
        type2TextView = findViewById(R.id.pokemon_type2);
        catchButton = findViewById(R.id.catchButton);
        pokemonImage = findViewById(R.id.pokemon_picture);
        infoTextView = findViewById(R.id.infoTextView);
        load();
    }

    public void load() {
        type1TextView.setText("");
        type2TextView.setText("");
        infoTextView.setText("");
        // This makes the first Json request which in turn makes the second Json request
        myJsonRequestListener listener = new myJsonRequestListener();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, listener, listener);
        requestQueue.add(request);
    }

    // This is for handling the catch and release problem
    @Override
    protected void onResume() {
        super.onResume();
        if (!isDataFetched) return;
        Log.d("Current PokeName", name);
        isCaught = getPreferences(Context.MODE_PRIVATE).getString(name, "false");
        Log.d("isCaughtState", isCaught);
        if (isCaught.equals("false"))
            catchButton.setText("Catch");
        else
            catchButton.setText("Release");

    }

    public void toggleCatch(View view) {
        isCaught = getPreferences(Context.MODE_PRIVATE).getString(name, "false");
        if (isCaught.equals("false")) {
            getPreferences(Context.MODE_PRIVATE).edit().putString(name, "caught").apply();
            catchButton.setText("Release");
        } else {
            getPreferences(Context.MODE_PRIVATE).edit().remove(name).apply();
            catchButton.setText("Catch");
        }
    }
}
