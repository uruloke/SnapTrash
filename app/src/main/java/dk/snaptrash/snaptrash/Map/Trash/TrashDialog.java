package dk.snaptrash.snaptrash.Map.Trash;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.LocationServices;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dk.snaptrash.snaptrash.Models.Trash;
import dk.snaptrash.snaptrash.R;
import dk.snaptrash.snaptrash.Services.SnapTrash.Trash.TrashService;
import dk.snaptrash.snaptrash.Services.SnapTrash.User.UserService;
import dk.snaptrash.snaptrash.Utils.Geo;
import dk.snaptrash.snaptrash.Utils.TaskWrapper;
import lombok.Setter;

public class TrashDialog
extends
    DialogFragment
implements
    View.OnClickListener
{

    public interface OnUserInitiatesPickUpListener {
        public void userInitiatesPickUp(Trash trash);
    }

    private static final String trashParameter = "TRASH";

    @Setter private OnUserInitiatesPickUpListener onUserInitiastesPickUpListener;

    private Trash trash;

    @Inject UserService userService;
    @Inject TrashService trashService;

    public static TrashDialog newInstance(Trash trash) {

        Log.e("trashdialog", "newinstance");

        Log.e("trashdialog", String.valueOf(trash == null));

        Bundle args = new Bundle();

        args.putSerializable(TrashDialog.trashParameter, trash);

        TrashDialog fragment = new TrashDialog();
        fragment.setArguments(args);

        Log.e("trashdialog", String.valueOf(args.getSerializable(TrashDialog.trashParameter) == null));

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        Log.e("trashdialog", "oncreate");

        Bundle args = this.getArguments();

        Log.e("trashlog", String.valueOf(args == null));

        if (args != null) {
            this.trash = (Trash) args.getSerializable(TrashDialog.trashParameter);
        }

        Log.e("trashlog", String.valueOf(this.trash == null));

    }

    @Override
    public void onStart() {
        super.onStart();

        Log.e("trashdialog", "onstart");

        Bundle args = this.getArguments();

        Log.e("trashlog", String.valueOf(args == null));

        if (args != null) {
            this.trash = (Trash) args.getSerializable(TrashDialog.trashParameter);
        }

        Log.e("trashlog", String.valueOf(this.trash == null));

    }

    @SuppressLint("MissingPermission")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trash_dialog, container, false);

        CompletableFuture fetchAuthorName = this.userService.get(this.trash.getAuthorId()).thenAcceptAsync(
            user -> this.getActivity().runOnUiThread(
                () -> {
                    TextView textView = view.findViewById(R.id.SnappedByView);
                    textView.setText(
                        this.getString(
                            R.string.SnappedBy,
                            user.getEmail()
                        )
                    );
                    textView.setVisibility(View.VISIBLE);
                }
            )
        );

        CompletableFuture fetchTrashCanBePickedUp = TaskWrapper.wrapAsync(
            LocationServices.getFusedLocationProviderClient(this.getActivity()).getLastLocation()
        ).thenAccept(
            location -> this.trashService.trashInPickupRange(
                Geo.toLatLng(location)
            ).thenAccept(
                trashes -> {
                    if (trashes.contains(this.trash)) {
                        Log.e("trashdialog", "trash can be picked up: " + String.valueOf(trashes.size()));
                        this.getActivity().runOnUiThread(
                            () -> view.findViewById(R.id.PickUpTrashButton).setEnabled(true)
                        );
                    }
                }
            )
        );

        CompletableFuture.allOf(
            fetchAuthorName,
            fetchTrashCanBePickedUp
        ).thenRun(
            () -> this.getActivity().runOnUiThread(
                () -> view.findViewById(R.id.TrashDialogProgressBar).setVisibility(View.GONE)
            )
        );

        this.trash.loadPictureInto(
            view.findViewById(R.id.TrashImageView)
        );

        view.findViewById(R.id.PickUpTrashButton).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        Log.e("trashdialog", "button clicked");
        if (this.onUserInitiastesPickUpListener != null) {
            this.onUserInitiastesPickUpListener.userInitiatesPickUp(this.trash);
        }
        this.dismiss();
    }

}
