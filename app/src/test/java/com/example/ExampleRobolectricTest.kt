package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GMK Player", appName)
  }

  @Test
  fun `main activity launches without crashing`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
    org.junit.Assert.assertNotNull(controller.get())
    controller.pause().stop().destroy()
  }

  @Test
  fun `storage volumes calculation is resilient`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storageList = com.example.filemanager.FileManagerHelper.getStorageVolumes(context)
    org.junit.Assert.assertNotNull(storageList)
  }

  @Test
  fun `audio and video viewmodels instantiate and clear cleanly`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val audioVm = com.example.player.audio.AudioPlayerViewModel(app)
    org.junit.Assert.assertNotNull(audioVm)
    org.junit.Assert.assertNull(audioVm.uiState.value.currentTrack)

    val videoVm = com.example.player.video.VideoPlayerViewModel(app)
    org.junit.Assert.assertNotNull(videoVm)
    org.junit.Assert.assertEquals(0L, videoVm.uiState.value.durationMs)
  }
}

