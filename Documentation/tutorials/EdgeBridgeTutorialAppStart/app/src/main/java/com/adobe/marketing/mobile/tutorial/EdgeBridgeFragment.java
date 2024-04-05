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

package com.adobe.marketing.mobile.tutorial;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.tutorial.databinding.FragmentFirstBinding;
import java.util.HashMap;

public class EdgeBridgeFragment extends Fragment {

	private FragmentFirstBinding binding;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = FragmentFirstBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		binding.trackActionButton.setOnClickListener(
			new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					MobileCore.trackAction(
						"purchase",
						new HashMap<String, String>() {
							{
								put("&&products", ";Running Shoes;1;69.95;event1|event2=55.99;eVar1=12345,;Running Socks;10;29.99;event2=10.95;eVar1=54321");
								put("&&events", "event5,purchase");
								put("myapp.promotion", "a0138");
							}
						}
					);
				}
			}
		);

		binding.trackStateButton.setOnClickListener(
			new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					MobileCore.trackState(
						"products/189025/runningshoes/12345",
						new HashMap<String, String>() {
							{
								put("&&products", ";Running Shoes;1;69.95;prodView|event2=55.99;eVar1=12345");
								put("myapp.category", "189025");
								put("myapp.promotion", "a0138");
							}
						}
					);
				}
			}
		);

		binding.triggerRuleButton.setOnClickListener(
			new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					MobileCore.collectPii(
						new HashMap<String, String>() {
							{
								put("key", "trigger");
							}
						}
					);
				}
			}
		);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}
