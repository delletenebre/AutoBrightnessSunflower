package kg.delletenebre.autobrightnesssunflower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CommandsReceiver extends BroadcastReceiver {
    private final String TAG = getClass().getName();

    @Override
    public void onReceive(Context context, Intent intent) {


        if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            Log.i(TAG, "************ACTION_USER_PRESENT************");
            Intent mIntent = new Intent(context, HiddenActivity.class);
            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mIntent);

        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Log.i(TAG, "************ACTION_SCREEN_OFF************");
            context.stopService(new Intent(context, CheckBrightnessService.class));

        }

    }
}
