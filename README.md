# HeartBeats

**An XR performance installation using real-time biosensor data to drive interactive audiovisual elements.**

Built with Unity 3D (OpenXR) and Android Studio.

![HeartBeats Banner](images/appimage-banner.jpeg)

## Project Overview

HeartBeats was developed as a collaboration between TU Berlin and Empiria Theatre Zagreb. The system translates live heart rate data into real-time audiovisual output within a performance context.

A dancer wears a Polar H10 chest sensor. An Android application connects via Bluetooth (Polar BLE SDK) and maps the heart rate to 18 audio files at different BPMs. The audience views the scene through an AR application (Noise Flowfield, Unity 3D) that creates a 3D particle visualization reacting to the audio environment: amplitude controls particle speed and rotation via linear interpolation, and the spectrum (8 bands) maps to particle color and size.

## Demo

[![HeartBeats Demo](images/OrhunShort_Moment3.jpg)](https://vimeo.com/690889831)

*Click the image to watch the demo on Vimeo.*

## System Architecture

![Concept](images/winterf.jpg)

**Polar H10 Sensor:** High-accuracy chest-worn heart rate sensor, connected via Bluetooth using the Polar BLE SDK.

**EKG Sound Generator (Android Studio):** Receives heart rate data, maps it to 18 audio files at different BPMs, and generates a heart animation scaled to heart rate.

**Noise Flowfield (Unity 3D, OpenXR):** AR application that extracts microphone input, uses the amplitude to control particle speed and rotation via linear interpolation, and maps the audio spectrum (8 bands) to particle color and size.

## Performance Documentation

![Performance](images/OrhunShort_Moment9.jpg)

Because heart rate is highly individual and influenced by emotional state, the sound mapping had to be calibrated specifically for the performer. The heart rate range during solo development differed significantly from rehearsal conditions with an audience present, requiring iterative adjustment of the BPM thresholds to account for performance-related arousal.

## Team

Orhi Kekiklerce, Elisabeth Oswald, Anna Petrouffa, Rhea Widmer (TU Berlin), Lorenzo Cocchia (Politecnico di Milano)

**Affiliation:** TU Berlin, in collaboration with Empiria Theatre Zagreb
