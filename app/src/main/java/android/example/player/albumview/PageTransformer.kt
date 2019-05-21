package android.example.player.albumview

import android.view.View
import androidx.viewpager.widget.ViewPager
import androidx.core.view.ViewCompat


private const val MIN_SCALE = 0.47f
private const val MAX_SCALE = 0.6f
private const val MIN_FADE = 0.4f

class PageTransformer : ViewPager.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        val pageWidth = view.width

        if (position < -1) {
            view.alpha = MIN_FADE
        } else if (position < 0) {
            view.alpha = 1 + position * (1 - MIN_FADE)
            view.translationX = (-0.5*pageWidth).toFloat() * MAX_SCALE * position
            ViewCompat.setTranslationZ(view, position)
            val scaleFactor = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1 - Math.abs(position))
            view.scaleX = 2 * scaleFactor
            view.scaleY = 2 * scaleFactor
        } else if (position == 0f) {
            view.alpha = 1f
            view.translationX = 0f
            view.scaleX = 2 * (MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1 - Math.abs(position)))
            ViewCompat.setTranslationZ(view, 0f)
            view.scaleY = 2 * (MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1 - Math.abs(position)))
        } else if (position <= 1) {
            ViewCompat.setTranslationZ(view, -position)
            view.alpha = 1 - position * (1 - MIN_FADE)
            view.translationX = (-0.5*pageWidth).toFloat() * MAX_SCALE * position

            val scaleFactor = MIN_SCALE + (MAX_SCALE - MIN_SCALE) * (1 - Math.abs(position))
            view.scaleX = 2 * scaleFactor
            view.scaleY = 2 * scaleFactor
        } else {
            view.alpha = MIN_FADE
        }
    }
}
