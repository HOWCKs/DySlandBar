package com.howck.dmi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import com.howck.dmi.ui.DmiScreen
import com.howck.dmi.ui.dynamicColorSchemeFor

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = dynamicColorSchemeFor(this@MainActivity)) {
                DmiScreen()
            }
        }
    }
}
