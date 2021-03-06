package com.osramd.lwss;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import com.osramd.lwss.sprites.NyanDroid;
import com.osramd.lwss.sprites.Rainbow;
import com.osramd.lwss.sprites.Stars;

public class NyanPaper extends WallpaperService {

	public static final String SHARED_PREFS_NAME = "nyandroidsettings";
	private static final String TAG = "NyanPaper";
	private final Handler mDroidHandler = new Handler();
	private static int mWidth;
	
	@Override
	public Engine onCreateEngine() {
		return new NyanEngine();
	}

	class NyanEngine extends Engine implements
			SharedPreferences.OnSharedPreferenceChangeListener {
		private final Paint mPaint = new Paint();

		private boolean mVisible;
		private boolean hasCenteredImages;
        private boolean hasLoadedImages;
		private SharedPreferences mPrefs;
		private boolean mPreferencesChanged;

		private NyanDroid mNyanDroid;
		private Rainbow mRainbow;
		private Stars mStars;
		
		private String mDroidImage;
		private String mRainbowImage;
		private String mStarImage;

        private int mAnimationSpeed;
		private int mSizeMod;
		private int mMaxDim;


        private int frameCount;

		private final Runnable mDrawFrame = new Runnable() {
			public void run() {
				drawFrame();
			}
		};

		NyanEngine() {
			mPaint.setColor(0xffffffff);
			mPrefs = NyanPaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(mPrefs, null);
			setupPrefs();
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs,
				String key) {
			Log.d(TAG, "prefs changed");
			setupPrefs();
			mPreferencesChanged = true;
		}

        private boolean mShowDroid;
        private boolean mShowRainbow;
        private boolean mShowStars;

		private void setupPrefs() {
			mDroidImage = mPrefs.getString("droid_image", "nyanwich");
			mRainbowImage = mPrefs.getString("rainbow_image", "rainbow");
            mStarImage = mPrefs.getString("star_image", "white");
			mSizeMod = mPrefs.getInt("size_mod", 5);
			mAnimationSpeed = mPrefs.getInt("animation_speed", 3);

            mShowDroid = !"none".equals(mDroidImage);
            mShowRainbow = !"none".equals(mRainbowImage);
            mShowStars = !"none".equals(mStarImage);
		}
		
		@Override
		public void onDestroy() {
			super.onDestroy();
			mDroidHandler.removeCallbacks(mDrawFrame);
		}
		

		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible) {
				drawFrame();
			} else {
				mDroidHandler.removeCallbacks(mDrawFrame);
			}
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			mWidth = width;
			hasCenteredImages = false;
			mPreferencesChanged = true;
		}

		private void setupAnimations() {
            hasLoadedImages = false;
            new AsyncTask<Void,Void,Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Context c = getApplicationContext();
                    mMaxDim = 64 * mSizeMod;
                    int width = c.getResources().getDisplayMetrics().widthPixels;
                    mMaxDim = mMaxDim < width ? mMaxDim : width - 64;
                    mNyanDroid = new NyanDroid(c, mMaxDim, mPaint, mDroidImage);

                    // initialize Rainbow
                    mMaxDim = (int) (mNyanDroid.getFrameHeight() * .4);
                    mRainbow = new Rainbow(c, mMaxDim, mPaint, mRainbowImage);

                    // remember offset for when drawing rainbows
                    mRainbow.setOffset((mNyanDroid.getFrameWidth() / 2)
                            - mRainbow.getFrameWidth());

                    mStars = new Stars(c, mMaxDim, mPaint, mStarImage, mAnimationSpeed);
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    hasLoadedImages = true;
                }

            }.execute();

		}
		
		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			mDroidHandler.removeCallbacks(mDrawFrame);
		}

		/**
		 * Draw a single animation frame.
		 */
		void drawFrame() {
			final SurfaceHolder holder = getSurfaceHolder();

			if (mPreferencesChanged) {
				setupAnimations();
				mPreferencesChanged = false;
				//must reset centers
				hasCenteredImages = false;
			}

			Canvas c = null;
			try {
				c = holder.lockCanvas();
				synchronized (holder) {
					frameCount++;
					if (c != null && hasLoadedImages) {
						if (!hasCenteredImages) {
							mRainbow.setCenter(c.getWidth() / 2,
									c.getHeight() / 2);
							mNyanDroid.setCenter(c.getWidth() / 2,
									c.getHeight() / 2);
							hasCenteredImages = true;
						}

						c.drawColor(getResources().getColor(R.color.nyanblue));

                        if (mShowStars) {
						    mStars.draw(c);
                        }

                        // This is ugly and dumb
                        boolean animateFrame = frameCount == 3;

                        if (mShowRainbow) {
                            mRainbow.draw(c, animateFrame);
                        }

                        if (mShowDroid) {
                            mNyanDroid.draw(c, animateFrame);
                        }

					}
					frameCount %= 3;
				}
			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

			// Reschedule the next redraw
			mDroidHandler.removeCallbacks(mDrawFrame);
			if (mVisible) {
				// approx 30 fps
                mDroidHandler.postDelayed(mDrawFrame, 1000 / 30);
			}
		}
	}
}
