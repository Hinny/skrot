package dev.hinny.skrot

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hinny.skrot.data.prefs.Settings
import dev.hinny.skrot.ui.SkrotApp
import dev.hinny.skrot.ui.theme.SkrotTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SkrotApplication).container
        setContent {
            val settings by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = Settings())
            SkrotTheme(themeMode = settings.theme) {
                SkrotApp(container = container, settings = settings)
            }
        }
    }
}
