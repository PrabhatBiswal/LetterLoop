// The app's only Activity. It turns on edge-to-edge drawing and hosts the Compose UI;
// all screens are Compose destinations inside it.
package com.prabhat.letterloop

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.prabhat.letterloop.ui.navigation.LetterLoopNavHost
import com.prabhat.letterloop.ui.theme.LetterLoopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Every screen has a coloured sky behind the status bar, so ask for white status and
        // navigation bar icons whatever the phone's own light/dark setting is.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            LetterLoopTheme {
                LetterLoopNavHost()
            }
        }
    }
}
