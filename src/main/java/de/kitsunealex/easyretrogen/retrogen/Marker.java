package de.kitsunealex.easyretrogen.retrogen;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class Marker {

    private final String marker;
    private final Set<String> classes;

    public Marker(String marker, Set<String> classes) {
        this.marker = marker;
        this.classes = classes;
    }

    public String getMarker() {
        return marker;
    }

    public ImmutableSet<String> getClasses() {
        return ImmutableSet.copyOf(classes);
    }

}
