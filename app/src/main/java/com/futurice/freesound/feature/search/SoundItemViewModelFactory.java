package com.futurice.freesound.feature.search;

import androidx.annotation.NonNull;

import com.futurice.freesound.feature.audio.AudioPlayer;
import com.futurice.freesound.feature.common.Navigator;
import com.futurice.freesound.network.api.FreeSoundApiClient;
import com.futurice.freesound.network.api.model.Sound;

final class SoundItemViewModelFactory {
    @NonNull
    private final Navigator navigator;
    @NonNull
    private final AudioPlayer audioPlayer;
    @NonNull
    private final FreeSoundApiClient freeSoundApiClient;

    SoundItemViewModelFactory(@NonNull Navigator navigator,
                              @NonNull AudioPlayer audioPlayer,
                              @NonNull FreeSoundApiClient freeSoundApiClient) {
        this.navigator = navigator;
        this.audioPlayer = audioPlayer;
        this.freeSoundApiClient = freeSoundApiClient;
    }

    public SoundItemViewModel create(Sound sound) {
        return new SoundItemViewModel(sound, navigator, audioPlayer, freeSoundApiClient);
    }
}
