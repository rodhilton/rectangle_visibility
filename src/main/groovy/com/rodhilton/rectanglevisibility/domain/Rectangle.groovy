package com.rodhilton.rectanglevisibility.domain

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Rectangle implements Serializable {
    static final long serialVersionUID = 59L;

    int east, north, west, south

    def String toString() {
        "(${east}, ${north}, ${west}, ${south})"
    }

    Rectangle copy() {
        return new Rectangle(east: east, north: north, west: west, south: south)
    }
}
