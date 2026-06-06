package com.overtime.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.overtime.tracker.ui.navigation.AppNavGraph
import com.overtime.tracker.ui.theme.DeepBlue
import com.overtime.tracker.ui.theme.OvertimeTrackerTheme
import com.overtime.tracker.widget.OvertimeWidgetProvider

/**
 * 主 Activity
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 打开 App 时主动刷新所有小组件状态
        OvertimeWidgetProvider.updateAllWidgets(this)

        setContent {
            OvertimeTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DeepBlue
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(navController = navController)
                }
            }
        }
    }
}
