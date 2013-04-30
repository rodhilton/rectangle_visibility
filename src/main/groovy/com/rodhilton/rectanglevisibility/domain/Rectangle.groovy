package com.rodhilton.rectanglevisibility.domain

class Rectangle implements Serializable {
    static final long serialVersionUID = 57L;

    double east, north, west, south

    def String toString() {
        "(${east}, ${north}, ${west}, ${south})"
    }
}
