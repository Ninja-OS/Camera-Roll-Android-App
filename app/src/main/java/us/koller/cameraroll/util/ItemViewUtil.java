package us.koller.cameraroll.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.item.ViewHolder.GifViewHolder;
import us.koller.cameraroll.data.AlbumItem;
import us.koller.cameraroll.data.Photo;
import us.koller.cameraroll.data.Video;
import us.koller.cameraroll.ui.ItemActivity;

public class ItemViewUtil {

    public static ViewGroup inflateView(ViewGroup container) {
        return (ViewGroup) LayoutInflater.from(container.getContext())
                .inflate(R.layout.photo_view, container, false);
    }

    public static View bindSubsamplingImageView(SubsamplingScaleImageView imageView,
                                                Photo photo, final View placeholderView) {
        ImageViewState imageViewState = null;
        if (photo.getImageViewSavedState() != null) {
            imageViewState = (ImageViewState) photo.getImageViewSavedState();
            photo.putImageViewSavedState(null);
        }

        imageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
        imageView.setMinimumDpi(80);

        imageView.setImage(ImageSource.uri(photo.getPath()), imageViewState);

        if (placeholderView != null) {
            imageView.setOnImageEventListener(
                    new SubsamplingScaleImageView.DefaultOnImageEventListener() {
                        @Override
                        public void onImageLoaded() {
                            super.onImageLoaded();
                            placeholderView.setVisibility(View.INVISIBLE);
                        }
                    });
        }
        return imageView;
    }

    public static View bindTransitionView(final ImageView imageView,
                                          final AlbumItem albumItem) {

        int[] imageDimens = albumItem instanceof Video ?
                Util.getVideoDimensions(albumItem.getPath()) :
                Util.getImageDimensions(imageView.getContext(), albumItem.getPath());

        if (imageView.getContext() instanceof Activity) {
            int screenWidth = Util.getScreenWidth((Activity) imageView.getContext());
            float scale = ((float) screenWidth) / (float) imageDimens[0];
            scale = scale > 1.0f ? 1.0f : scale == 0.0f ? 1.0f : scale;
            imageDimens[0] =
                    (int) (imageDimens[0] * scale * 0.5f) > 0
                            ? (int) (imageDimens[0] * scale * 0.5f) : 1;
            imageDimens[1] =
                    (int) (imageDimens[1] * scale * 0.5f) > 0
                            ? (int) (imageDimens[1] * scale * 0.5f) : 1;
        } else {
            imageDimens[0] = imageDimens[0] / 2;
            imageDimens[1] = imageDimens[1] / 2;
        }

        Glide.with(imageView.getContext())
                .load(albumItem.getPath())
                .asBitmap()
                .override(imageDimens[0], imageDimens[1])
                .skipMemoryCache(true)
                .error(R.drawable.error_placeholder_tinted)
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<Bitmap> target,
                                               boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model,
                                                   Target<Bitmap> target, boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        if (albumItem.isSharedElement && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            albumItem.isSharedElement = false;
                            ((ItemActivity) imageView.getContext()).startPostponedEnterTransition();
                        }
                        return false;
                    }
                })
                .into(imageView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Handle timeout
            imageView.setTransitionName(albumItem.getPath());
            new Handler().postDelayed(new Runnable() {
                @Override
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                public void run() {
                    ((ItemActivity) imageView.getContext()).startPostponedEnterTransition();
                }
            }, 200);
        }

        return imageView;
    }

    public static View bindGif(final GifViewHolder gifViewHolder,
                               final ImageView imageView,
                               final AlbumItem albumItem) {
        Glide.with(imageView.getContext())
                .load(albumItem.getPath())
                .asGif()
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .error(R.drawable.error_placeholder_tinted)
                .listener(new RequestListener<String, GifDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model,
                                               Target<GifDrawable> target,
                                               boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, String model,
                                                   Target<GifDrawable> target, boolean isFromMemoryCache,
                                                   boolean isFirstResource) {
                        resource.start();
                        gifViewHolder.setAttacher(imageView);
                        return false;
                    }
                })
                .into(imageView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageView.setTransitionName(albumItem.getPath());
        }
        return imageView;
    }

    public static final String VIDEO_PLAY_BUTTON_TAG = "VIDEO_PLAY_BUTTON_TAG";

    public static ViewGroup bindImageViewForVideo(final ImageView imageView,
                                                  final AlbumItem albumItem) {
        int[] imageDimens = albumItem instanceof Video ?
                Util.getVideoDimensions(albumItem.getPath()) :
                Util.getImageDimensions(imageView.getContext(), albumItem.getPath());

        if (imageView.getContext() instanceof Activity) {
            int screenWidth = Util.getScreenWidth((Activity) imageView.getContext());
            float scale = ((float) screenWidth) / (float) imageDimens[0];
            scale = scale > 1.0f ? 1.0f : scale == 0.0f ? 1.0f : scale;
            imageDimens[0] = (int) (imageDimens[0] * scale);
            imageDimens[1] = (int) (imageDimens[1] * scale);
        } else {
            imageDimens[0] = imageDimens[0] / 2;
            imageDimens[1] = imageDimens[1] / 2;
        }

        Glide.with(imageView.getContext())
                .load(albumItem.getPath())
                .asBitmap()
                .override(imageDimens[0], imageDimens[1])
                .skipMemoryCache(true)
                .error(R.drawable.error_placeholder_tinted)
                .into(imageView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageView.setTransitionName(albumItem.getPath());
        }

        ImageView playButton = new ImageView(imageView.getContext());
        playButton.setTag(VIDEO_PLAY_BUTTON_TAG);
        playButton.setImageResource(R.drawable.ic_play_circle_filled_white_24dp);
        playButton.setAlpha(0.54f);
        RelativeLayout.LayoutParams params
                = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        playButton.setLayoutParams(params);

        ViewGroup v = (ViewGroup) imageView.getParent();
        v.addView(playButton);
        return v;
    }
}
