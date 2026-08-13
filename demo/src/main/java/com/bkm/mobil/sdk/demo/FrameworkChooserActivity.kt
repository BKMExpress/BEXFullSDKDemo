package com.bkm.mobil.sdk.demo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class FrameworkChooserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_framework_chooser)

        findViewById<Button>(R.id.btn_start_xml).setOnClickListener {
            val intent = Intent().setClassName(
                this,
                "com.bkm.mobil.sdk.demo.XmlExampleActivity"
            )
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_start_compose).setOnClickListener {
            startActivity(Intent(this, ComposeExampleActivity::class.java))
        }
    }
}
