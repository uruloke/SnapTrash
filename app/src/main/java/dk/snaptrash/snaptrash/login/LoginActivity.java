package dk.snaptrash.snaptrash.login;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasFragmentInjector;
import dk.snaptrash.snaptrash.MapActivity;
import dk.snaptrash.snaptrash.Models.User;
import dk.snaptrash.snaptrash.R;
import dk.snaptrash.snaptrash.Services.SnapTrash.Auth.AuthProvider;

public class LoginActivity extends AppCompatActivity implements HasFragmentInjector {
    @Inject DispatchingAndroidInjector<Fragment> fragmentInjector;

    @Inject AuthProvider auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        auth.addOnLoginListener(this);

        auth.login();

        setContentView(R.layout.activity_main);

        getFragmentManager().beginTransaction()
                .add(R.id.screenView, LogInFragment.newInstance())
                .commit();
    }

    @Override
    public AndroidInjector<Fragment> fragmentInjector() {
        return fragmentInjector;
    }

    @Override
    public void onComplete(@NonNull Task<User> task) {
        if (task.isSuccessful()) {
            Intent intent = new Intent(this, MapActivity.class);
            this.auth.removeOnLoginListener(this);
            this.startActivity(intent);
        }
        else {
            Log.e("AUTH", "FAILED LOGIN");
        }
    }
}
