package dk.snaptrash.snaptrash.login;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dk.snaptrash.snaptrash.Map.MapActivity;
import dk.snaptrash.snaptrash.R;
import dk.snaptrash.snaptrash.Services.SnapTrash.Auth.AuthProvider;


public class LogInFragment extends Fragment implements View.OnClickListener {

    private ProgressBar progressBar;
    private Button signInButton;
    private TextView signUp;
    private TextView username;
    private TextView password;

    @Inject
    AuthProvider auth;

    public LogInFragment() {
        // Required empty public constructor
    }

    public static LogInFragment newInstance() {
        LogInFragment fragment = new LogInFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_log_in, container, false);

        this.signInButton = view.findViewById(R.id.logIn);
        this.progressBar = view.findViewById(R.id.loading);
        this.signUp = view.findViewById(R.id.signUp);
        this.username = view.findViewById(R.id.userName);
        this.password = view.findViewById(R.id.password);

        this.signInButton.setOnClickListener(this);
        this.signUp.setOnClickListener(this);

        return view;
    }

    private void working() {
        this.progressBar.setVisibility(View.VISIBLE);
        this.signInButton.setEnabled(false);

    }

    private void idle() {
        this.progressBar.setVisibility(View.INVISIBLE);
        this.signInButton.setEnabled(true);
    }

    @Override
    public void onClick(View view) {
        if(view == signInButton) {
            this.working();
            ((AuthenticationActivity) this.getActivity()).login(
                username.getText().toString().trim(),
                password.getText().toString()
            ).whenCompleteAsync((user, throwable) -> {
                this.getActivity().runOnUiThread(
                    () -> {
                        if (throwable == null) {
                            this.startActivity(
                                new Intent(this.getActivity(), MapActivity.class)
                            );
                        } else {
                            this.idle();
                            Toast.makeText(
                                this.getActivity(),
                                "Failed login",
                                Toast.LENGTH_SHORT
                            ).show(); //TODO add actual error handling
                            Log.e("loginfragment", "pls", throwable);
                        }
                    }
                );
            });
        } else if(view == signUp) {
            this.getFragmentManager()
                .beginTransaction()
                .replace(R.id.screenView, SignUpFragment.newInstance())
                .commit();
        }
    }

}
