package com.lostcity.assets;

import java.util.List;

/**
 * Части магистралей: tunnel, open, bridge (+ _bi для bidirectional).
 * Оригинал: mcjty.lostcities.worldgen.lost.regassets.data.HighwayParts.
 */
public record HighwayParts(
    List<String> tunnel,
    List<String> open,
    List<String> bridge,
    List<String> tunnelBi,
    List<String> openBi,
    List<String> bridgeBi
) {
    public static final HighwayParts DEFAULT = new HighwayParts(
        List.of("lostcities:highway_tunnel"),
        List.of("lostcities:highway_open"),
        List.of("lostcities:highway_bridge"),
        List.of("lostcities:highway_tunnel_bi"),
        List.of("lostcities:highway_open_bi"),
        List.of("lostcities:highway_bridge_bi")
    );

    public List<String> tunnel(boolean bidirectional) {
        return bidirectional ? tunnelBi : tunnel;
    }

    public List<String> open(boolean bidirectional) {
        return bidirectional ? openBi : open;
    }

    public List<String> bridge(boolean bidirectional) {
        return bidirectional ? bridgeBi : bridge;
    }
}
