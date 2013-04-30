package com.rodhilton.rectanglevisibility.domain

class Rectangle implements Serializable {
    static final long serialVersionUID = 50L;

    double east, north, west, south

    def String toString() {
        "(${east}, ${north}, ${west}, ${south})"
    }
}
