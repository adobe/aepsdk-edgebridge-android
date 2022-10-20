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

public class EdgeFragment extends Fragment {
    private static final String LOG_TAG = "EdgeFragment";

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.trackActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MobileCore.trackAction(
                        "add_to_cart",
                        new HashMap<String, String>() {
                            {
                                put("product.id", "12345");
                                put("product.add.event", "1");
                                put("product.name", "wide_brim_sunhat");
                                put("product.units", "1");
                            }
                        }
                );
            }
        });

        binding.trackStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MobileCore.trackState(
                        "hats/sunhat/wide_brim_sunhat_id12345",
                        new HashMap<String, String>() {
                            {
                                put("product.name", "wide_brim_sunhat");
                                put("product.id", "12345");
                                put("product.view.event", "1");
                            }
                        }
                );
            }
        });

        binding.triggerRuleButton.setOnClickListener(new View.OnClickListener() {
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
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}