// The Application class. It exists to own the single AppContainer, so every screen
// shares one set of repositories for the life of the process.
package com.prabhat.letterloop

import android.app.Application

class LetterLoopApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
