import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class FocusSessionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_session)

        // Keep the screen on during the focus session
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the flag to allow normal timeout when the session ends
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}