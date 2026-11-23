// The app's only Activity. It turns on edge-to-edge drawing and hosts the Compose UI;
// all screens are Compose destinations inside it.
package com.prabhat.letterloop

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.prabhat.letterloop.ui.theme.LetterLoopTheme
import com.prabhat.letterloop.ui.theme.LoopColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so ask for light status/navigation bar icons even when the
        // phone is in light mode; the default "auto" style would draw dark icons on our navy.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            LetterLoopTheme {
                Box(Modifier.fillMaxSize().background(LoopColors.Ground))
            }
        }
    }
}
