package com.rodhilton.rectanglevisibility.util

import com.rodhilton.rectanglevisibility.domain.Rectangle
import com.rodhilton.rectanglevisibility.domain.VisibilityDiagram

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage

class Verifier {

    public static void main(String[] args) {
        File file = new File(args[0])
        String fileContents = file.readLines().join("\n")
        def matcher = fileContents =~ /(?mis)Rectangle Visibility Diagram on (\d+) rectangles with (\d+) edges.*\{(.*)\}/
        int size = matcher[0][1].toInteger()
        int edgeCount = matcher[0][2].toInteger()
        def rectLines = matcher[0][3].trim().split("\n")

        List<Rectangle> rects = new ArrayList<Rectangle>(size)
        for(String rectLine: rectLines) {
            def lineMatcher = rectLine =~ /\s*(\d+): \(([\d.]+),\s*([\d.]+),\s*([\d.]+),\s*([\d.]+)\s*\)/
            if(lineMatcher.matches()) {
                int index = lineMatcher[0][1].toInteger()
                int east = lineMatcher[0][2].toInteger()
                int north = lineMatcher[0][3].toInteger()
                int west = lineMatcher[0][4].toInteger()
                int south = lineMatcher[0][5].toInteger()
                rects[index-1] = new Rectangle(north: north, east: east, south: south, west: west)
            }
        }

        VisibilityDiagram diagram = new VisibilityDiagram(size, new Random(), rects)
        def (List visiblePairs, List invisiblePairs) = diagram.getPairs()

        println("Visibility Diagram's Calculated Fitness: ${diagram.fitness()}")
        println("Visibility Diagram's Believed Fitness: ${edgeCount}")

        def invisibleString = invisiblePairs.collect{ "(${it.bottomIndex}, ${it.topIndex})"}.join(", ")

        println("Invisible Rectangles: ${invisibleString}")

        for(int i=1;i<=size;i++) {
            BufferedImage image = diagram.render(600,600,i,-1, new Color(1f, 1f, 1f,0f))
            File outputfile = new File("saved${i}.png");
            ImageIO.write(image, "png", outputfile);
        }

    }
}
