package ro.antiprotv.radioclock.listener;

import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import ro.antiprotv.radioclock.TipsDialog;

public class HelpOnClickListener implements View.OnClickListener {
  private final AppCompatActivity activity;

  public HelpOnClickListener(AppCompatActivity activity) {
    this.activity = activity;
  }

  @Override
  public void onClick(View view) {
    android.app.AlertDialog tipsDialog = new TipsDialog(view.getContext());
    tipsDialog.show();
    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
    lp.copyFrom(tipsDialog.getWindow().getAttributes());
    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
    lp.height = WindowManager.LayoutParams.MATCH_PARENT;
    tipsDialog.getWindow().setAttributes(lp);

    // ShowCaseService service = new ShowCaseService(activity);
    // service.showCase();
    /*      TutoShowcase.from(activity)
    .on(mContentView)
    .displaySwipableLeft()
    .delayed(250)
    .animated(true)
    //.show()
    .on(mContentView)
    .displaySwipableRight()
    .delayed(500)
    .show();*/
  }
}
