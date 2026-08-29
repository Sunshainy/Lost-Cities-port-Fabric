package mcjty.lostcities.worldgen;

import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.*;

import java.util.Arrays;
import java.util.OptionalInt;

public class HeightGenOpt {

    public static int getBaseHeight(NoiseBasedChunkGenerator generator, int x, int z, WorldGenLevel level, RandomState rnd) {
        return iterateNoiseColumn(generator.generatorSettings().value(), level, rnd, x, z).orElse(level.getMinY());
    }

    private static OptionalInt iterateNoiseColumn(NoiseGeneratorSettings noise, WorldGenLevel pLevel, RandomState pRandom, int pX, int pZ) {
        NoiseSettings settings = noise.noiseSettings().clampToHeightAccessor(pLevel);
        int cellH = settings.getCellHeight();
        int minY = settings.minY();
        int cellMinY = Mth.floorDiv(minY, cellH);
        int cellHeight = Mth.floorDiv(settings.height(), cellH);
        if (cellHeight <= 0) {
            return OptionalInt.empty();
        } else {
            int cellWidth = settings.getCellWidth();
            int cellPX = Math.floorDiv(pX, cellWidth);
            int cellPZ = Math.floorDiv(pZ, cellWidth);
            int cellOX = Math.floorMod(pX, cellWidth);
            int cellOZ = Math.floorMod(pZ, cellWidth);
            int cellX = cellPX * cellWidth;
            int cellZ = cellPZ * cellWidth;
            double xFactor = (double)cellOX / (double)cellWidth;
            double zFactor = (double)cellOZ / (double)cellWidth;
            NoiseChunkOpt.FluidStatusV def = new NoiseChunkOpt.FluidStatusV(noise.seaLevel(), noise.defaultFluid());

            NoiseChunkOpt chunk = new NoiseChunkOpt(1, pRandom, cellX, cellZ, settings, BeardifierMarker.INSTANCE, noise, def);
            chunk.initializeForFirstCellX();
            chunk.advanceCellX(0);

            for(int y = cellHeight - 1; y >= 0; --y) {
                chunk.selectCellYZ(y, 0);

                for(int y2 = cellH - 1; y2 >= 0; --y2) {
                    int cellEndBlockY = (cellMinY + y) * cellH + y2;
                    double dY = (double)y2 / (double)cellH;
                    chunk.updateForYXZ(pX, cellEndBlockY, pZ, xFactor, dY, zFactor);
                    BlockState stateI = chunk.getInterpolatedState();
                    BlockState state = stateI == null ? noise.defaultBlock() : stateI;

                    if (state.blocksMotion()) {
                        chunk.stopInterpolation();
                        return OptionalInt.of(cellEndBlockY + 1);
                    }
                }
            }

            chunk.stopInterpolation();
            return OptionalInt.empty();
        }
    }

    protected enum BeardifierMarker implements DensityFunctions.BeardifierOrMarker {
        INSTANCE;

        private BeardifierMarker() {
        }

        public double compute(DensityFunction.FunctionContext p_208515_) {
            return (double)0.0F;
        }

        public void fillArray(double[] p_208517_, DensityFunction.ContextProvider p_208518_) {
            Arrays.fill(p_208517_, (double)0.0F);
        }

        public double minValue() {
            return (double)0.0F;
        }

        public double maxValue() {
            return (double)0.0F;
        }
    }

}
