/*
  Copyright 2022 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.bridge.testapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.edge.bridge.EdgeBridge

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val versionTextView = findViewById<TextView>(R.id.text_version)
        versionTextView.text = EdgeBridge.extensionVersion()

        findViewById<Button>(R.id.btn_track_action).setOnClickListener {
            // Dispatch an Analytics track action event which is handled by the
            // Edge Bridge extension which forwards it to the Edge Network.
            MobileCore.trackAction(
                "add_to_cart",
                mapOf<String, String>(
                    "product.id" to "12345",
                    "product.add.event" to "1",
                    "product.name" to "wide_brim_sunhat",
                    "product.units" to "1"
                )
            )
        }

        findViewById<Button>(R.id.btn_track_state).setOnClickListener {
            // Dispatch an Analytics track state event which is handled by the
            // Edge Bridge extension which forwards it to the Edge Network.
            MobileCore.trackState(
                "hats/sunhat/wide_brim_sunhat_id12345",
                mapOf<String, String>(
                    "product.name" to "wide_brim_sunhat",
                    "product.id" to "12345",
                    "product.view.event" to "1"
                )
            )
        }

        findViewById<Button>(R.id.btn_trigger_consequence).setOnClickListener {
            // Configure the Data Collection Mobile Property with a Rule to dispatch
            // an Analytics event when a PII event is dispatched in the SDK.
            // Without the rule, this button will not forward a track call to the Edge Network.
            MobileCore.collectPii(mapOf<String, String>("key" to "trigger"))
        }

        findViewById<Button>(R.id.btn_assurance).setOnClickListener {
            val intent = Intent(this, AssuranceActivity::class.java)
            startActivity(intent)
        }
    }
}
