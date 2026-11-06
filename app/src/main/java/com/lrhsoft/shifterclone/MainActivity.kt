package com.lrhsoft.shifterclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lrhsoft.shifterclone.ui.screens.CalendarScreen
import com.lrhsoft.shifterclone.ui.theme.ShifterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShifterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalendarScreen()
                }
            }
        }
    }
}
