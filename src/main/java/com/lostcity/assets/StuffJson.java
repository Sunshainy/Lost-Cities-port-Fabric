package com.lostcity.assets;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StuffJson {
    public List<String> tags;
    public String column;
    public Integer minheight;
    public Integer maxheight;
    public Integer mincount;
    public Integer maxcount;
    public Integer attempts;
    public Boolean inbuilding;
    public Boolean seesky;
    
    // Simplification for matchers: 
    // Wait, let's implement full matching so it is 1:1 with Forge.
    public MatcherJson biomes;
    public MatcherJson blocks;
    public MatcherJson upperblocks;
    public MatcherJson buildings;

    public static class MatcherJson {
        public List<String> blocks;
        public List<String> tag;
        public List<String> excluding;
    }
}