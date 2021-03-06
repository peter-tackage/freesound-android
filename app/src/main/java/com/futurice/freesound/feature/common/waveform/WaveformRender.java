/*
 * Copyright 2017 Futurice GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.futurice.freesound.feature.common.waveform;

import androidx.annotation.NonNull;

/**
 * Renders a waveform, allows abstractions to the rendering for different views.
 */
public interface WaveformRender {

    /**
     * Renders the given waveform.
     *
     * @param waveform the waveform amplitude array, values in range [-1.0, 1.0].
     */
    void setWaveform(@NonNull float[] waveform);

    /**
     * Clear the waveform render.
     */
    void clearWaveform();
}
