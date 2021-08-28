// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

package com.tencent.yolov5ncnn;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

public class YoloV5Ncnn
{
    public native boolean Init(AssetManager mgr);

    public class Obj implements Comparable<Obj>
    {
        public float x;
        public float y;
        public float w;
        public float h;
        public String label;
        public float prob;

        @Override
        public int compareTo(Obj o) {
            if(this.prob > o.prob) {
                return 1;
            } else if (this.prob== o.prob) {
                return 0;
            } else {
                return -1 ;
            }
        }

        @NonNull
        @SuppressLint("DefaultLocale")
        public String toString() {
            return String.format("x=%.1f y=%.1f w=%.1f h=%.1f label: %s  prob=%.1f", x, y, w, h, label, prob);
        }
    }

    public native Obj[] Detect(Bitmap bitmap, boolean use_gpu);

    static {
        System.loadLibrary("yolov5ncnn");
    }
}
