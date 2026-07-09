package com.daftar.app.store

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.daftar.app.R

// F5 paper sounds — a page-flip on the day book's ‹ › and a soft tick on tab/segment swaps,
// so the app feels like her paper daftar. It DELIBERATELY respects the ringer: nothing plays
// in silent or vibrate mode. There's no in-app toggle by design (a maintainer rebuild swaps
// the samples or disables them) — see SPEC F5 / A2. The bundled samples are synthetic
// placeholders; replace app/src/main/res/raw/{page_flip,paper_tick}.wav with real recordings.
class DaftarSounds(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        ).build()
    private val flipId = pool.load(context, R.raw.page_flip, 1)
    private val tickId = pool.load(context, R.raw.paper_tick, 1)

    // Only in NORMAL ringer mode — silent and vibrate both mean "no sound" to her.
    private fun audible() = audio.ringerMode == AudioManager.RINGER_MODE_NORMAL

    fun flip() { if (audible()) pool.play(flipId, 0.7f, 0.7f, 1, 0, 1f) }
    fun tick() { if (audible()) pool.play(tickId, 0.4f, 0.4f, 0, 0, 1f) }
    fun release() = pool.release()
}

@Composable
fun rememberDaftarSounds(): DaftarSounds {
    val ctx = LocalContext.current
    val sounds = remember { DaftarSounds(ctx.applicationContext) }
    DisposableEffect(Unit) { onDispose { sounds.release() } }
    return sounds
}
