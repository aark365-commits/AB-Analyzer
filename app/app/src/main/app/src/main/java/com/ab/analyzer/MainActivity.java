package com.ab.analyzer;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 40, 25, 25);
        layout.setBackgroundColor(Color.rgb(15, 18, 30));

        TextView title = new TextView(this);
        title.setText("AB-ANALYZER");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Teen Patti Card Analyzer");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        addSpace(layout);

        EditText playerA = new EditText(this);
        playerA.setHint("Player A: AS KH 2D");
        playerA.setTextColor(Color.WHITE);
        playerA.setHintTextColor(Color.GRAY);
        layout.addView(playerA);

        EditText playerB = new EditText(this);
        playerB.setHint("Player B: QS QH 5C");
        playerB.setTextColor(Color.WHITE);
        playerB.setHintTextColor(Color.GRAY);
        layout.addView(playerB);

        Button analyze = new Button(this);
        analyze.setText("ANALYZE CARDS");
        layout.addView(analyze);

        TextView result = new TextView(this);
        result.setTextColor(Color.WHITE);
        result.setTextSize(20);
        result.setGravity(Gravity.CENTER);
        layout.addView(result);

        analyze.setOnClickListener(v -> {
            result.setText(
                "Cards received!\n\n" +
                "Player A: " + playerA.getText() +
                "\nPlayer B: " + playerB.getText() +
                "\n\nNext: Hand analysis engine"
            );
        });

        setContentView(layout);
    }

    private void addSpace(LinearLayout layout) {
        TextView space = new TextView(this);
        space.setText("\n");
        layout.addView(space);
    }
}
