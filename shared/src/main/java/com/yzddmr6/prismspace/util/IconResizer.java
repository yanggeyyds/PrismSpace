package com.yzddmr6.prismspace.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;

import androidx.annotation.UiThread;

public class IconResizer {

	private final int mIconWidth, mIconHeight;
	private final Rect mOldBounds = new Rect();
	private final Canvas mCanvas = new Canvas();

	public IconResizer() {
		this((int) Resources.getSystem().getDimension(android.R.dimen.app_icon_size));
	}

	public IconResizer(final int size) {
		mCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG));
		mIconWidth = mIconHeight = size;
	}

	@UiThread public Drawable createIconThumbnail(Drawable icon) {
		if (icon == null) return null;
		int width = mIconWidth;
		int height = mIconHeight;

		final int iconWidth = icon.getIntrinsicWidth();
		final int iconHeight = icon.getIntrinsicHeight();

		if (icon instanceof PaintDrawable) {
			final PaintDrawable painter = (PaintDrawable) icon;
			painter.setIntrinsicWidth(width);
			painter.setIntrinsicHeight(height);
		}

		if (width > 0 && height > 0) {
			if (width < iconWidth || height < iconHeight) {
				final float ratio = (float) iconWidth / iconHeight;
				if (iconWidth > iconHeight) height = (int) (width / ratio);
				else if (iconHeight > iconWidth) width = (int) (height * ratio);
				icon = drawIconToBitmap(icon, width, height, icon.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
			} else if (iconWidth < width && iconHeight < height) {
				icon = drawIconToBitmap(icon, iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
			}
		}
		return icon;
	}

	private Drawable drawIconToBitmap(final Drawable icon, final int width, final int height, final Bitmap.Config config) {
		final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, config);
		final Canvas canvas = mCanvas;
		canvas.setBitmap(thumb);
		mOldBounds.set(icon.getBounds());
		final int x = (mIconWidth - width) / 2;
		final int y = (mIconHeight - height) / 2;
		icon.setBounds(x, y, x + width, y + height);
		icon.draw(canvas);
		icon.setBounds(mOldBounds);
		canvas.setBitmap(null);
		return new BitmapDrawable(Resources.getSystem(), thumb);
	}
}
