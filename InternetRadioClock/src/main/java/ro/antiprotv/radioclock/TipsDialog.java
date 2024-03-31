package ro.antiprotv.radioclock;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicInteger;

public class TipsDialog extends AlertDialog {
  public TipsDialog(Context context) {
    super(context);
    AtomicInteger tipPosition = new AtomicInteger(0);

    final String[] tips = context.getResources().getStringArray(R.array.tips);
    View view = LayoutInflater.from(context).inflate(R.layout.dialog_main_help, null);
    setView(view);
    setTitle("Tips & Tricks");
    Button previous = view.findViewById(R.id.button_tips_previousTip);
    Button next = view.findViewById(R.id.button_tips_nextTip);
    Button ok = view.findViewById(R.id.button_tips_ok);
    TextView tip = view.findViewById(R.id.tipTxt);

    previous.setOnClickListener(
        v -> {
          int counter = tipPosition.getAndDecrement();
          if (counter == 0) {
            counter = tips.length - 1;
            tipPosition.set(tips.length - 1);
          } else {
            counter--;
          }

          tip.setText(tips[counter]);
        });

    next.setOnClickListener(
        v -> {
          int counter = tipPosition.getAndIncrement();
          if (counter == tips.length - 1) {
            counter = 0;
            tipPosition.set(0);
          } else {
            counter++;
          }

          tip.setText(tips[counter]);
        });
    ok.setOnClickListener(v -> cancel());
  }
}
