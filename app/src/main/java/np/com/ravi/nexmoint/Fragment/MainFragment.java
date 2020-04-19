
package np.com.ravi.nexmoint.Fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nexmo.sdk.verify.client.VerifyClient;
import com.nexmo.sdk.verify.event.SearchListener;
import com.nexmo.sdk.verify.event.UserObject;
import com.nexmo.sdk.verify.event.UserStatus;
import com.nexmo.sdk.verify.event.VerifyClientListener;
import com.nexmo.sdk.verify.event.VerifyError;

import java.io.IOException;

import np.com.ravi.nexmoint.Country.CountriesAdapter;
import np.com.ravi.nexmoint.Country.CountryList;
import np.com.ravi.nexmoint.R;
import np.com.ravi.nexmoint.SampleApplication;

/**
 * Main fragment containing a simple view for initiating a verify request.
 * The country code spinner and the phone number input field are pre-filled when possible.
 *
 * The SIGN IN button triggers a getUserStatus request that checks for the current user status.
 * In case the user status is not VERIFIED, an automatic getVerifiedUser is triggered.
 * If the verification can be initiated, the CheckCodeFragment is displayed that allows
 * input for the PIN code.
 *
 * All the events are printed in logcat and displayed as Toast messages.
 */
public class MainFragment extends Fragment {

    public static final String TAG = MainFragment.class.getSimpleName();
    private CountryList countries;

    public MainFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        this.countries = new CountryList(getActivity());
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        return v;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Make sure to remove all the listeners attached, if any.
        SampleApplication application = (SampleApplication) getActivity().getApplication();
        if (application.getVerifyClient() != null)
            application.getVerifyClient().removeVerifyListeners();
    }

    @Override
    public void onResume(){
        super.onResume();

        final Activity activity = getActivity();
        final SampleApplication application = (SampleApplication) activity.getApplication();
        application.getVerifyClient().addVerifyListener(new VerifyClientListener() {
            @Override
            public void onVerifyInProgress(final VerifyClient verifyClient, final UserObject userObject) {
                Log.d(TAG, "onVerifyInProgress");
                // When verification is in progress we can jump to the next screen that allows PIN code input.
                getFragmentManager().beginTransaction().replace(R.id.container, new CheckCodeFragment()).addToBackStack("check").commit();
            }

            @Override
            public void onUserVerified(final VerifyClient verifyClient, final UserObject userObject) {
                Log.d(TAG, "This User is already verified!");
                showToast("This User is already verified!");
            }

            @Override
            public void onError(final VerifyClient verifyClient, final VerifyError errorCode, final UserObject userObject) {
                Log.d(TAG, "onError " + errorCode);
                // If  the verification is already in progress, switch to CheckCodeFragment.
                if(errorCode == VerifyError.VERIFICATION_ALREADY_STARTED)
                    getFragmentManager().beginTransaction().replace(R.id.container, new CheckCodeFragment()).addToBackStack("check").commit();
                else
                    showToast("onError.code: " + errorCode.toString());
            }

            @Override
            public void onException(final IOException exception) {
                Log.d(TAG, "onException " + exception.getMessage());
                showToast("No internet connectivity.");
            }
        });
    }

    @Override
    public void onPause(){
        super.onPause();

        final Activity activity = getActivity();
        final SampleApplication application = (SampleApplication) activity.getApplication();
        application.getVerifyClient().removeVerifyListeners();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();
        final InputMethodManager inputMethodManager = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        // Set default prefix and phone number if possible.
        final Spinner countrySpinner = (Spinner) activity.findViewById(R.id.country_spinner);
        CountriesAdapter adapter = new CountriesAdapter(activity, android.R.layout.simple_spinner_item, this.countries);
        countrySpinner.setAdapter(adapter);
        countrySpinner.setSelection(this.countries.getCountryCodePosition());

        final EditText phoneNumber_et = (EditText) activity.findViewById(R.id.phone_number);
        phoneNumber_et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                // If DONE or Enter were pressed, validate the input.
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                    inputMethodManager.hideSoftInputFromWindow(phoneNumber_et.getWindowToken(), 0);
                    Editable phoneNumber = phoneNumber_et.getText();
                    if (TextUtils.isEmpty(phoneNumber.toString()) || phoneNumber.toString().length() < 5)
                        phoneNumber_et.setError(getResources().getString(R.string.error_phone_number));
                        return true;
                }
                return false;
            }
        });

        Button sign_btn = (Button) activity.findViewById(R.id.button_sign_in);
        sign_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputMethodManager.hideSoftInputFromWindow(phoneNumber_et.getWindowToken(), 0);
                final String prefix = countries.getCode(countrySpinner.getSelectedItemPosition());
                Editable phoneNumberEdit = phoneNumber_et.getText();

                if(phoneNumberEdit != null) {
                    final String phoneNumber = phoneNumberEdit.toString();
                    if(TextUtils.isEmpty(prefix) || TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 5)
                        phoneNumber_et.setError(getResources().getString(R.string.error_phone_number));
                    else {
                        initiateGetUserStatus(prefix, phoneNumber);
                    }
                }
            }
        });
    }

    private void initiateGetUserStatus(final String prefix, final String phoneNumber) {
        final SampleApplication application = (SampleApplication) getActivity().getApplication();

        application.getVerifyClient().getUserStatus(prefix, phoneNumber, new SearchListener() {
            @Override
            public void onUserStatus(final UserStatus userStatus) {
                Log.d(TAG, "onUserStatus " + userStatus.toString());
                showToast("onUserStatus: " + userStatus.toString());

                if(userStatus != UserStatus.USER_VERIFIED) {
                    application.getVerifyClient().getVerifiedUser(prefix, phoneNumber);
                }
            }

            @Override
            public void onError(final VerifyError errorCode, final String errorMessage) {
                Log.d(TAG, "onSearchError " + errorCode);
                showToast("onSearchError.message: " + errorMessage);
            }

            @Override
            public void onException(IOException exception) {
                Log.d(TAG, "onException " + exception.getMessage());
                showToast("No internet connectivity.");
            }
        });
    }

    private void showToast(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
