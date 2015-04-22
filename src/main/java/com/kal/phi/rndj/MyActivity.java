package com.kal.phi.rndj;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MyActivity extends Activity {

    // static vars
    private static final int READ_REQUEST_CODE = 4254;
    private final static int buttons[] = {R.id.button1, R.id.button2, R.id.button3, R.id.button4};

    // General vars
    int tries, right;
    String correct;
    JSONObject map = new JSONObject();

    // View vars
    TextView status;
    Button word;
    ProgressBar points;
    Button[] button = new Button[4];

    // misc vars
    SharedPreferences pref;
    int default_color = Color.BLACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        // Main button
        word = (Button) findViewById(R.id.text);
        word.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validate(null);
            }
        });
        word.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                loadWords(true);
                return false;
            }
        });

        // Option buttons
        ViewGroup group = (ViewGroup) findViewById(R.id.buttons);
        for (int i = 0; i < 4; i++)
            (button[i] = (Button) group.findViewById(buttons[i])).setOnClickListener(new SpecialClickListener(i));

        // Preferences
        pref = PreferenceManager.getDefaultSharedPreferences(getApplication());
        tries = pref.getInt("tries", 0);
        right = pref.getInt("right", 0);

        // Status
        status = (TextView) findViewById(R.id.textView2);
        points = (ProgressBar) findViewById(R.id.progressBar);
        status.setText(right + " / " + tries);
        points.setMax(tries);
        points.setProgress(right);
        default_color = status.getTextColors().getDefaultColor();

        loadWords(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Work with selected file
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                pref.edit()
                        .putString("words", data.getData().toString())
                        .apply();
                validate(null);
                loadWords(false);
            } else new AlertDialog.Builder(getApplicationContext())
                    .setMessage(getString(R.string.nores))
                    .setTitle("Error")
                    .create()
                    .show();
        }
    }

    void start() {
        try {
            // Get random element from map
            int counter = 0, wordIndex = new Random().nextInt(map.length());

            Iterator<String> itr = map.keys();
            String key;
            while (itr.hasNext()) {
                // Iterate to index
                key = itr.next();
                if (counter++ < wordIndex) continue;
                correct = map.getString(key);
                word.setText(key);
                break;
            }

            // Random placement for right button
            int num = new Random().nextInt(4);

            button[num].setText(correct);

            // Already used keys
            ArrayList<Integer> list = new ArrayList<Integer>();

            for (int i = 0; i < 4; i++) {
                // don't reset right button
                if (i == num) continue;
                while (true) {
                    int index = new Random().nextInt(map.length());
                    if (list.contains(index) || index == wordIndex) continue;
                    list.add(index);
                    counter = 0; // reset counter
                    itr = map.keys();
                    while (itr.hasNext()) {
                        String x = itr.next();
                        if (counter++ < index) continue;
                        button[i].setText(map.getString(x));
                        break;
                    }
                    break;
                }
            }
        } catch (JSONException je) {
            je.printStackTrace();
        }

    }

    void validate(String result) {
        // Reset
        if (result == null) {
            points.setProgress(right = 0);
            points.setMax(tries = 0);
            status.setText("0 / 0");
            status.setTextColor(default_color);
            return;
        }

        // standardize
        result = result.trim().toLowerCase();
        correct = correct.trim().toLowerCase();

        points.setMax(++tries);

        if (correct.equals(result)) {
            points.setProgress(++right);
            status.setTextColor(Color.GREEN);
            status.setText(right + " / " + tries);
        } else {
            status.setTextColor(Color.RED);
            status.setText("( " + word.getText().toString() + ": " + correct + " ) " + right + " / " + tries);
        }

        // Save progress
        pref.edit()
            .putInt("tries", tries)
            .putInt("right", right)
            .apply();

        start();
    }

    void loadWords(boolean open) {
        String wFile = pref.getString("words", null);
        // If forced (open == true) or wFile not defined
        if (open || wFile == null)
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*"), READ_REQUEST_CODE);
        else
            try {
                // Read file
                BufferedReader reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(Uri.parse(wFile))));
                String stringBuilder = "";
                for (String line = ""; line != null; line = reader.readLine())
                    stringBuilder += line;
                reader.close();

                // Interpreat as JSON
                map = new JSONObject(stringBuilder);
                if (map.length() < 5) throw new JSONException("Not enough items");

                start();
            } catch (Exception e) {
                new AlertDialog.Builder(this)
                        .setMessage(e.getMessage())
                        .setTitle("Error")
                        .create()
                        .show();
                e.printStackTrace();
            }
    }

    class SpecialClickListener implements View.OnClickListener {
        final int i;

        SpecialClickListener(int i) {
            this.i = i;
        }

        @Override
        public void onClick(View view) {
            validate(button[i].getText().toString());
        }
    }
}