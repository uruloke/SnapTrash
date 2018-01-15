package dk.snaptrash.snaptrash.Models;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import dk.snaptrash.snaptrash.Utils.Geo.Coordinate;
import dk.snaptrash.snaptrash.Utils.Geo.Geo;
import lombok.Getter;
import lombok.Setter;

public class Trash extends Model<Trash> {

    @Getter private String id;
    @Getter private Coordinate location;
    @Getter private String pictureUrl;
    @Getter private String description;
    @Getter private String authorId;
    @Getter @Nullable String pickedUpById;

    public enum Status {
        SYNCHRONIZED,
        PENDING_REMOVAL_CONFIRMED
    }

    @Getter@Setter private Status status = Status.SYNCHRONIZED;

    public Trash(String id, Coordinate location, String pictureUrl, String description, String authorId) {
        this.id = id;
        this.location = location;
        this.pictureUrl = pictureUrl != null ? pictureUrl : "https://firebasestorage.googleapis.com/v0/b/snaptrash-1507812289113.appspot.com/o/IMG_20180110_144336.jpg?alt=media&token=2ff58097-37a5-45d3-8450-cabd65b6b229";
        this.pictureUrl = pictureUrl;
        this.description = description;
        this.authorId = authorId;
    }

    public Trash(String id, LatLng location, String pictureUrl, String description, String authorId) {
        this.id = id;
        this.location = new Coordinate(location.latitude, location.longitude);
        this.pictureUrl = pictureUrl;
        this.description = description;
    }

    public void loadPictureInto(ImageView imageView) {
        this.loadPictureInto(imageView.getContext(), imageView);
    }

    public void loadPictureInto(Context context, ImageView imageView) {
        this.loadPictureInto(context, imageView, null);
    }

    public void loadPictureInto(Context context, ImageView imageView, @Nullable Callback callback) {
        this.loadPicture(context).into(imageView, callback);
    }

    public RequestCreator loadPicture(Context context) {
        return Picasso.with(context).load(this.pictureUrl);
    }

    public LatLng toLatLng() {
        return Geo.toLatLng(this.getLocation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Trash trash = (Trash) o;

        return id != null ? id.equals(trash.id) : trash.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}
