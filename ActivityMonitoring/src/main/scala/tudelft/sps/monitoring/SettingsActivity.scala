package tudelft.sps.monitoring

import android.os.Bundle
import android.preference.PreferenceActivity

class SettingsActivity extends PreferenceActivity{
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.preference)
  }
}