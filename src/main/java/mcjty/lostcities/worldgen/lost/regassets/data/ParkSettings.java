package mcjty.lostcities.worldgen.lost.regassets.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * For a city style this object represents settings for parks
 */
public class ParkSettings {
    private final Float parkChance;
    private final Boolean avoidFoliage;
    private final Boolean parkBorder;
    private final Boolean parkElevation;
    private final Integer parkStreetThreshold;
    private final Character parkElevationBlock;
    private final Character grassBlock;

    public static final Codec<ParkSettings> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.optionalFieldOf("parkchance").forGetter(l -> Optional.ofNullable(l.parkChance)),
                    Codec.BOOL.optionalFieldOf("avoidfoliage").forGetter(l -> Optional.ofNullable(l.avoidFoliage)),
                    Codec.BOOL.optionalFieldOf("parkborder").forGetter(l -> Optional.ofNullable(l.parkBorder)),
                    Codec.BOOL.optionalFieldOf("parkelevation").forGetter(l -> Optional.ofNullable(l.parkElevation)),
                    Codec.INT.optionalFieldOf("parkstreetthreshold").forGetter(l -> Optional.ofNullable(l.parkStreetThreshold)),
                    Codec.STRING.optionalFieldOf("elevation").forGetter(l -> DataTools.toNullable(l.parkElevationBlock)),
                    Codec.STRING.optionalFieldOf("grass").forGetter(l -> DataTools.toNullable(l.grassBlock))
            ).apply(instance, ParkSettings::new));

    public Float getParkChance() { return parkChance; }

    public Boolean getAvoidFoliage() { return avoidFoliage; }

    public Boolean getParkBorder() { return parkBorder; }

    public Boolean getParkElevation() { return parkElevation; }

    public Integer getParkStreetThreshold() { return parkStreetThreshold; }

    public Character getParkElevationBlock() { return parkElevationBlock; }

    public Character getGrassBlock() { return grassBlock; }

    public ParkSettings(Optional<Float> parkChance,
                        Optional<Boolean> avoidFoliage,
                        Optional<Boolean> parkBorder,
                        Optional<Boolean> parkElevation,
                        Optional<Integer> parkStreetThreshold,
                        Optional<String> parkElevationBlock,
                        Optional<String> grassBlock) {
        this.parkChance = parkChance.orElse(null);
        this.avoidFoliage = avoidFoliage.orElse(null);
        this.parkBorder = parkBorder.orElse(null);
        this.parkElevation = parkElevation.orElse(null);
        this.parkStreetThreshold = parkStreetThreshold.orElse(null);
        this.parkElevationBlock = DataTools.getNullableChar(parkElevationBlock);
        this.grassBlock = DataTools.getNullableChar(grassBlock);
    }
}
