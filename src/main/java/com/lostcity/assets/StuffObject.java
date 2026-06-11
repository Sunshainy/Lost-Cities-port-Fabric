package com.lostcity.assets;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StuffObject {
    public final String name;
    public final List<String> tags;
    public final String column;
    public final Integer minheight;
    public final Integer maxheight;
    public final int mincount;
    public final int maxcount;
    public final int attempts;
    public final Boolean inbuilding;
    public final Boolean seesky;
    
    public final Matcher<Biome> biomeMatcher;
    public final BlockMatcher blockMatcher;
    public final BlockMatcher upperBlockMatcher;
    public final Matcher<String> buildingMatcher;

    public StuffObject(String name, StuffJson json) {
        this.name = name;
        this.tags = json.tags != null ? json.tags : List.of();
        this.column = json.column;
        this.minheight = json.minheight;
        this.maxheight = json.maxheight;
        this.mincount = json.mincount != null ? json.mincount : 1;
        this.maxcount = json.maxcount != null ? json.maxcount : 1;
        this.attempts = json.attempts != null ? json.attempts : 1;
        this.inbuilding = json.inbuilding;
        this.seesky = json.seesky;

        this.biomeMatcher = new Matcher<>(json.biomes, false);
        this.blockMatcher = new BlockMatcher(json.blocks);
        this.upperBlockMatcher = new BlockMatcher(json.upperblocks);
        this.buildingMatcher = new Matcher<>(json.buildings, true);
    }

    public static class Matcher<T> {
        private final boolean any;
        private final Set<String> names = new HashSet<>();
        private final Set<String> tags = new HashSet<>();
        private final Set<String> excluding = new HashSet<>();
        private final boolean isString;

        public Matcher(StuffJson.MatcherJson json, boolean isString) {
            this.isString = isString;
            if (json == null) {
                this.any = true;
            } else {
                this.any = false;
                if (json.blocks != null) names.addAll(json.blocks);
                if (json.tag != null) tags.addAll(json.tag);
                if (json.excluding != null) excluding.addAll(json.excluding);
            }
        }

        public boolean isAny() {
            return any;
        }

        public boolean test(T object, Identifier id, List<Identifier> objectTags) {
            if (any) return true;
            String nameStr = id != null ? id.toString() : (isString ? (String) object : "");
            
            if (!excluding.isEmpty()) {
                if (excluding.contains(nameStr)) return false;
                if (objectTags != null) {
                    for (Identifier tag : objectTags) {
                        if (excluding.contains(tag.toString())) return false;
                        if (excluding.contains("#" + tag.toString())) return false;
                    }
                }
            }
            
            if (names.isEmpty() && tags.isEmpty()) return true;
            
            if (names.contains(nameStr)) return true;
            
            if (objectTags != null) {
                for (Identifier tag : objectTags) {
                    if (tags.contains(tag.toString())) return true;
                    if (tags.contains("#" + tag.toString())) return true;
                }
            }
            
            return false;
        }
    }

    public static class BlockMatcher {
        private final Matcher<BlockState> internal;

        public BlockMatcher(StuffJson.MatcherJson json) {
            this.internal = new Matcher<>(json, false);
        }

        public boolean isAny() {
            return internal.isAny();
        }

        public boolean test(BlockState state) {
            if (internal.isAny()) return true;
            Block block = state.getBlock();
            Identifier id = Registries.BLOCK.getId(block);
            List<Identifier> tags = state.streamTags().map(TagKey::id).toList();
            return internal.test(state, id, tags);
        }
    }
}