package mcjty.lostcities.worldgen.lost.regassets.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a condition
 */
public class ConditionTest {
    private final Boolean top;
    private final Boolean ground;
    private final Boolean cellar;
    private final Boolean isbuilding;
    private final Boolean issphere;
    private final Integer floor;
    private final Integer chunkx;
    private final Integer chunkz;
    private final Set<String> belowPart;
    private final Set<String> inpart;
    private final Set<String> inbuilding;
    private final Set<String> inbiome;
    private final String range;

    public static final Codec<Either<List<String>, String>> LIST_OR_STRING_CODEC = Codec.either(Codec.list(Codec.STRING), Codec.STRING);

    public Boolean getTop() {
        return top;
    }

    public Boolean getGround() {
        return ground;
    }

    public Boolean getCellar() {
        return cellar;
    }

    public Boolean getIsbuilding() {
        return isbuilding;
    }

    public Boolean getIssphere() {
        return issphere;
    }

    public Integer getFloor() {
        return floor;
    }

    public Integer getChunkx() {
        return chunkx;
    }

    public Integer getChunkz() {
        return chunkz;
    }

    public Set<String> getBelowPart() {
        return belowPart;
    }

    public Set<String> getInpart() {
        return inpart;
    }

    public Set<String> getInbuilding() {
        return inbuilding;
    }

    public Set<String> getInbiome() {
        return inbiome;
    }

    public String getRange() {
        return range;
    }

    public ConditionTest(
            Optional<Boolean> top,
            Optional<Boolean> ground,
            Optional<Boolean> cellar,
            Optional<Boolean> isbuilding,
            Optional<Boolean> issphere,
            Optional<Integer> floor,
            Optional<Integer> chunkx,
            Optional<Integer> chunkz,
            Optional<Set<String>> belowPart,
            Optional<Set<String>> inpart,
            Optional<Set<String>> inbuilding,
            Optional<Set<String>> inbiome,
            Optional<String> range) {
        this.top = top.orElse(null);
        this.ground = ground.orElse(null);
        this.cellar = cellar.orElse(null);
        this.isbuilding = isbuilding.orElse(null);
        this.issphere = issphere.orElse(null);
        this.floor = floor.orElse(null);
        this.chunkx = chunkx.orElse(null);
        this.chunkz = chunkz.orElse(null);
        this.belowPart = belowPart.orElse(null);
        this.inpart = inpart.orElse(null);
        this.inbuilding = inbuilding.orElse(null);
        this.inbiome = inbiome.orElse(null);
        this.range = range.orElse(null);
    }

    protected static Optional<Either<List<String>, String>> convertSetOrString(Set<String> set) {
        if (set == null) {
            return Optional.empty();
        } else {
            return Optional.of(Either.left(List.copyOf(set)));
        }
    }

    protected static Optional<Set<String>> convertSetOrString(Optional<Either<List<String>, String>> either) {
        if (either.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(either.get().map(l -> Set.copyOf(l), s -> Set.of(s)));
    }
}
