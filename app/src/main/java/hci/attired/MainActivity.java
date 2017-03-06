package hci.attired;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String[] SHOPPINGLIST = new String[] {
            "Jeans", "Jacket", "Shirt", "T-Shirt", "Shoes"
    };

    public static final String ATTIRE_STORAGE = "attire_storage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        Window window = this.getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.statsbar));

        setContentView(R.layout.activity_fullscreen);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line , SHOPPINGLIST);

        final AutoCompleteTextView search = (AutoCompleteTextView) findViewById(R.id.shoppingList);
        search.setAdapter(adapter);

        Button searchBtn = (Button) findViewById(R.id.searchBtn);

        searchBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String selected = search.getText().toString();
                Toast.makeText(getApplicationContext(), selected, Toast.LENGTH_SHORT).show();

                SharedPreferences.Editor editor = getSharedPreferences(ATTIRE_STORAGE, MODE_PRIVATE).edit();
                editor.putString("item1", selected);
                editor.commit();

                Intent intent = new Intent(getApplicationContext(), NearBy.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        search.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                in.hideSoftInputFromWindow(v.getWindowToken(), 0);
                search.showDropDown();
            }
        });

    }
}
