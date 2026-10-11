package com.example.musictheory.reach
import android.app.Activity; import com.example.musictheory.data.ProgressRepo; import com.example.musictheory.data.SettingsRepo
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.first
object ReviewFlow {
    suspend fun maybeAsk(activity: Activity) { val ctx = activity.applicationContext
        if (ProgressRepo.gamesCount(ctx).first() < 5) return
        val last = ProgressRepo.allResults(ctx).first().firstOrNull() ?: return
        if (last.score < 70) return; if (!SettingsRepo.reviewCooldownOver(ctx)) return
        try { val mgr = ReviewManagerFactory.create(ctx)
            mgr.requestReviewFlow().addOnSuccessListener { info -> if (info != null) mgr.launchReviewFlow(activity, info) }
            SettingsRepo.markReviewAsked(ctx) } catch (_: Exception) {} }
}
