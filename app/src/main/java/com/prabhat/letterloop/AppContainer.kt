// Manual dependency wiring, used instead of Hilt (Decision log #3).
// It creates each repository once; ViewModel factories read their dependencies from here.
package com.prabhat.letterloop

class AppContainer
