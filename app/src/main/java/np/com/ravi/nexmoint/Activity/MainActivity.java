package np.com.ravi.nexmoint.Activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import np.com.ravi.nexmoint.Config;
import np.com.ravi.nexmoint.Fragment.MainFragment;
import np.com.ravi.nexmoint.R;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (TextUtils.isEmpty(Config.NexmoAppId) || TextUtils.isEmpty(Config.NexmoSharedSecretKey))
            Toast.makeText(this, "Mandatory Nexmo config was not set.", Toast.LENGTH_LONG).show();
        else {
            setContentView(R.layout.activity_main);
            if (savedInstanceState == null)
                getFragmentManager().beginTransaction().add(R.id.container, new MainFragment()).commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}