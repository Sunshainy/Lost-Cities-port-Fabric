package com.lostcity.assets;

import java.util.List;

/**
 * Набор частей для улиц (street_full, street_straight, ...).
 * Портировано из StreetParts (оригинальный Lost Cities).
 */
public final class StreetParts {

    public static final StreetParts DEFAULT = new StreetParts(
            List.of("lostcities:street_full"),
            List.of("lostcities:street_straight"),
            List.of("lostcities:street_end"),
            List.of("lostcities:street_bend"),
            List.of("lostcities:street_t"),
            List.of("lostcities:street_none"),
            List.of("lostcities:street_all"));

    private final List<String> full;
    private final List<String> straight;
    private final List<String> end;
    private final List<String> bend;
    private final List<String> t;
    private final List<String> none;
    private final List<String> all;

    public StreetParts(List<String> full, List<String> straight, List<String> end,
                       List<String> bend, List<String> t, List<String> none, List<String> all) {
        this.full = full;
        this.straight = straight;
        this.end = end;
        this.bend = bend;
        this.t = t;
        this.none = none;
        this.all = all;
    }

    public List<String> full() { return full; }
    public List<String> straight() { return straight; }
    public List<String> end() { return end; }
    public List<String> bend() { return bend; }
    public List<String> t() { return t; }
    public List<String> none() { return none; }
    public List<String> all() { return all; }
}
