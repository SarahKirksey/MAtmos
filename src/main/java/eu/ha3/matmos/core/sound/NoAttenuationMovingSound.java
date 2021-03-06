package eu.ha3.matmos.core.sound;

import eu.ha3.matmos.core.SystemClock;
import eu.ha3.matmos.util.math.HelperFadeCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;

public class NoAttenuationMovingSound extends MovingSound implements StreamingSound {
    private boolean usesPause;
    private final HelperFadeCalculator helper = new HelperFadeCalculator(new SystemClock());
    private float desiredVolume;
    private float desiredPitch;

    protected NoAttenuationMovingSound(ResourceLocation myResource, float volume, float pitch, boolean isLooping, boolean usesPause) {
        super(SoundEvents.AMBIENT_CAVE, SoundCategory.MASTER);

        positionedSoundLocation = myResource;
        attenuationType = ISound.AttenuationType.NONE;
        repeat = isLooping;
        repeatDelay = 0;

        desiredVolume = volume;
        desiredPitch = pitch;
        this.volume = volume;
        this.pitch = pitch;

        this.usesPause = usesPause;
    }

    public NoAttenuationMovingSound copy() {
        return new NoAttenuationMovingSound(getSoundLocation(), desiredVolume, desiredPitch, repeat, usesPause);
    }

    @Override
    public void update() {
        Entity e = Minecraft.getMinecraft().player;

        xPosF = (float)e.posX;
        yPosF = (float)e.posY;
        zPosF = (float)e.posZ;

        volume = helper.calculateFadeFactor() * desiredVolume;

        if (volume < 0.01f && usesPause) {
            pitch = 0f;
        }

        if (volume > 0.01f && usesPause) {
            pitch = desiredPitch;
        }

        if (helper.isDoneFadingOut() && repeat && !isDonePlaying()) {
            dispose();
        }
    }

    @Override
    public void play(float fadeIn) {
        helper.fadeIn((long)(fadeIn * 1000));
    }

    @Override
    public void stop(float fadeOut) {
        helper.fadeOut((long)(fadeOut * 1000));
    }

    @Override
    public void applyVolume(float volumeMod) {
        volume = volumeMod;
    }

    @Override
    public void dispose() {
        donePlaying = true;
    }

    @Override
    public void interrupt() {
        donePlaying = true;
    }
}
