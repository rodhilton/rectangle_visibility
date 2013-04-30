package com.rodhilton.rectanglevisibility.domain

import com.rodhilton.metaheuristics.algorithms.MetaheuristicAlgorithm
import com.rodhilton.metaheuristics.collections.ScoredSet

import java.awt.*
import java.awt.image.BufferedImage
import java.util.List

class VisibilityDiagram implements Serializable, MetaheuristicAlgorithm<VisibilityDiagram> {
    static final long serialVersionUID = 51L;

    private int size
    private Random random
    private List<Rectangle> rects;
    private final double RENDER_PADDING=0.05

    public VisibilityDiagram(int size, Random random) {
        this.random = random
        this.size = size
        this.rects = new ArrayList<Rectangle>(size)

        rects.add(new Rectangle(north: 1.0, east: 1.0, south: 1.0, west: 1.0))

        for (int i = 1; i < size-1; i++) {
            double north = random.nextDouble()
            double east =  random.nextDouble()
            double west =  random.nextDouble()
            double south =  random.nextDouble()

            rects.add(new Rectangle(
                north: north,
                east: east,
                south: south,
                west: west
            ))
        }

        rects.add(new Rectangle(north: 1.0, east: 1.0, south: 1.0, west: 1.0))
    }

    protected VisibilityDiagram(int size, Random random, List<Rectangle> rects) {
        this.size = size
        this.random = random
        this.rects = rects
    }

    public int getSize() {
        size
    }

    public int getGoal() {
        (size * (size - 1)) / 2
    }

    @Override
    Number fitness() {
        def (List visiblePairs, List invisiblePairs) = getPairs()
        return visiblePairs.size()
    }

    public List getPairs() {
        def visiblePairs = []
        def invisiblePairs = []

        for (int a = 0; a < size; a++) {
            for (int b = a + 1; b < size; b++) {
                Rectangle bottom = rects[a]
                Rectangle top = rects[b]
                List<Rectangle> inbetween = new ArrayList<Rectangle>()
                if (b > a + 1) {
                    inbetween = rects[a + 1..b - 1]
                }

                if (isFreeCornerBetween(bottom, top, inbetween)) {
                    visiblePairs << [bottom: bottom, top: top, bottomIndex: a + 1, topIndex: b + 1]
                } else {
                    invisiblePairs << [bottom: bottom, top: top, bottomIndex: a + 1, topIndex: b + 1]
                }
            }
        }
        [visiblePairs, invisiblePairs]
    }


    @Override
    List<VisibilityDiagram> combine(ScoredSet<VisibilityDiagram> scoredGeneration) {
        def parents = scoredGeneration.getTop(2);

        int pivot = random.nextInt(size - 1)

        def parent1Bottom = parents[0].rects[0..pivot]
        def parent2Bottom = parents[1].rects[0..pivot]
        def parent1Top = parents[0].rects[pivot + 1..-1]
        def parent2Top = parents[1].rects[pivot + 1..-1]

        VisibilityDiagram child1 = new VisibilityDiagram(size, random, parent1Bottom + parent2Top)
        VisibilityDiagram child2 = new VisibilityDiagram(size, random, parent2Bottom + parent1Top)

        int mutations = scoredGeneration.size() / 2

        List<VisibilityDiagram> child1Offspring = []
        List<VisibilityDiagram> child2Offspring = []

        for (int i = 0; i < mutations; i++) {
            child1Offspring << child1.mutate()
            child2Offspring << child2.mutate()
        }

        def things = child1Offspring + child2Offspring[0..-2] + scoredGeneration.getBest()
        return things
    }

    @Override
    String toString() {
        StringBuilder sb = new StringBuilder()
        sb.append("Rectangle Visibility Diagram on ${rects.size()} rectangles with ${fitness()} edges in order (E,N,W,S):\r\n")
        sb.append(" { \n")
        for (int i = rects.size() - 1; i >= 0; i--) {
            Rectangle rect = rects[i]
            def (int east, int north, int west, int south) = normalizedValues(rect)
            sb.append("   ${sprintf('%02d', i + 1)}: (${east}, ${north}, ${west}, ${south})\n")
        }

        sb.append(" } \n")
    }

    public String fullString() {
        return RectUtils.printRectangles((Rectangle[]) rects.toArray())
    }

    public BufferedImage render(int imageWidth, int imageHeight, int rectCount = rects.size(), int highlightRect = -1, Color background=new Color(1f,1f,1f,1f)) {
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)

        Graphics2D graphics = image.createGraphics()
        //Draw background white
        //graphics.setPaint(new Color(1f, 1f, 1f,0f));
        graphics.setPaint(background)
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

        float hueGapSize = 1 / ((float) rects.size())
        float[] hues = new float[rects.size()]
        for (int i = 0; i < rects.size(); i++) {
            hues[i] = i * hueGapSize
        }

        for (int i = 0; i < rectCount; i++) {
            def (int scaleWidth, int scaleHeight, int scaleX, int scaleY) = getScaledRect(i, imageWidth, imageHeight)

            //This is nearly impossible to explain, basically we don't want to go straight up the hues array, because overlapping
            //colors will be too close.  We effectively want to ping-pong back and forth, from the first to the middle, then the first
            //plus one, middle plus one, etc.  This formula does that, surprisingly.
            //final float hue = hues[((i*(int)(size/2))+(int)(1-(size%2)/2))%size]
            final float hue = hues[i]
            final float saturation = 0.7f;
            final float luminance = 0.7f;
            final Color color = Color.getHSBColor(hue, saturation, luminance);

            graphics.setColor(color)

            //Top rectangle should be somewhat transparent, all others much less so
            int innerOpacity = (i == rectCount - 1) ? 192 : 255
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), innerOpacity))
            graphics.fillRect(scaleX, scaleY, scaleWidth, scaleHeight)
            int extraThickness = (i == rectCount - 1) ? 1 : 0
            java.awt.Rectangle borderRect = new java.awt.Rectangle(scaleX + extraThickness, scaleY + extraThickness, scaleWidth - (extraThickness), scaleHeight - (extraThickness));
            graphics.setStroke(new BasicStroke(extraThickness + 1));
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255).darker())
            graphics.draw(borderRect)

            //Draw a drop shadow around each
            int shadowDepth = 3
            graphics.setColor(new Color(0, 0, 0, 32))
            //Two rectangles to make the shadow
            graphics.fillRect(scaleX + scaleWidth + 1, scaleY + shadowDepth, shadowDepth, scaleHeight + 1)
            graphics.fillRect(scaleX + shadowDepth, scaleY + scaleHeight + 1, scaleWidth - shadowDepth + 1, shadowDepth)

            if (i == rectCount - 1) {
                graphics.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND))
                graphics.setColor(new Color(255, 255, 255, 32))
                java.awt.Rectangle outlineRect = new java.awt.Rectangle(scaleX, scaleY, scaleWidth, scaleHeight);
                graphics.draw(outlineRect)
            }
        }

        if (highlightRect > -1) {
            def (int scaleWidth, int scaleHeight, int scaleX, int scaleY) = getScaledRect(highlightRect, imageWidth, imageHeight)
            java.awt.Rectangle myRect = new java.awt.Rectangle(scaleX, scaleY, scaleWidth, scaleHeight);
            graphics.setColor(Color.WHITE)
            float[] dash = [10.0f];
            graphics.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            graphics.draw(myRect)

            Font currentFont = graphics.getFont()
            def bigFont = new Font(currentFont.name, currentFont.style, currentFont.size * 2)
            FontMetrics fm = graphics.getFontMetrics(bigFont)
            String rectLabel = "${highlightRect + 1}"
            def labelWidth = fm.stringWidth(rectLabel)
            def labelHeight = fm.getHeight();
            if (scaleWidth > labelWidth && scaleHeight > labelHeight) {
                graphics.setFont(bigFont)
                graphics.drawString(rectLabel, scaleX + (int) ((scaleWidth - labelWidth) / 2), fm.getAscent() + scaleY + (int) ((scaleHeight - labelHeight) / 2))
                graphics.setFont(currentFont)
            }
        }

        return image
    }

    public int getTopRectangleNumberContaining(int x, int y, int width, int height, int maxLevel) {
        for (int i = maxLevel - 1; i >= 0; i--) {
            if(rectContains(i, width, height, x, y)) {
                return i;
            }

        }

        //If nothing else is considered the top rectangle, but the actual top rectangle contains the point, use that
        if(rectContains(maxLevel, width, height, x, y)) {
            return maxLevel;
        }

        return -1;
    }

    private boolean rectContains(int i, int width, int height, int x, int y) {
        def (int scaleWidth, int scaleHeight, int scaleX, int scaleY) = getScaledRect(i, width, height)

        if (x > scaleX && x < scaleX + scaleWidth && y > scaleY && y < scaleY + scaleHeight) {
            return true;
        }
        return false
    }

    private boolean isFreeCornerBetween(Rectangle a, Rectangle b, List<Rectangle> inbetween) {
        int fcnecount = 0;
        int fcnwcount = 0;
        int fcswcount = 0;
        int fcsecount = 0;
        for (Rectangle r : inbetween) {
            boolean fcne = r.east < Math.min(a.east, b.east) || r.north < Math.min(a.north, b.north)
            boolean fcnw = r.north < Math.min(a.north, b.north) || r.west < Math.min(a.west, b.west)
            boolean fcsw = r.west < Math.min(a.west, b.west) || r.south < Math.min(a.south, b.south)
            boolean fcse = r.south < Math.min(a.south, b.south) || r.east < Math.min(a.east, b.east)
            if (fcne) fcnecount++
            if (fcnw) fcnwcount++
            if (fcsw) fcswcount++
            if (fcse) fcsecount++
        }
        //One of those conditions must hold for ALL the in betweens, meaning that one of the counts must be == the # of inbetweens
        fcnecount == inbetween.size() ||
                fcnwcount == inbetween.size() ||
                fcswcount == inbetween.size() ||
                fcsecount == inbetween.size()
    }

    private VisibilityDiagram mutate() {
        int whichRect = random.nextInt(size-2)+1;
        int whichDimension = random.nextInt(4);
        List<Rectangle> copyRects = new ArrayList<Rectangle>()
        for (Rectangle rectToCopy : rects) {
            Rectangle newRect = new Rectangle(
                    north: rectToCopy.north,
                    east: rectToCopy.east,
                    south: rectToCopy.south,
                    west: rectToCopy.west,
            )
            copyRects.add(newRect);
        }
        Rectangle mutating = copyRects[whichRect]

        switch (whichDimension) {
            case 0: //north
                mutating.setNorth(random.nextDouble())
                break;
            case 1: //east
                mutating.setEast(random.nextDouble())
                break;
            case 2: //south
                mutating.setSouth(random.nextDouble())
                break;
            case 3: //west
                mutating.setWest(random.nextDouble())
                break;
        }

        copyRects.set(whichRect, mutating)

        return new VisibilityDiagram(size, random, copyRects)
    }

    private List getScaledRect(int i, int imageWidth, int imageHeight) {
        int paddingX = RENDER_PADDING*imageWidth
        int paddingY = RENDER_PADDING*imageHeight

        int paddedWidth = imageWidth-paddingX*2
        int paddedHeight = imageHeight-paddingY*2

        Rectangle rect = rects[i]
        def (int east, int north, int west, int south) = normalizedValues(rect)
        double x = (size-west) / ((double) (size * 2) + 1)
        double y = (size-north) / ((double) (size * 2) + 1)
        double width = (east + west + 1) / ((double) (size * 2) + 1)
        double height = (north + south + 1) / ((double) (size * 2) + 1)


        int scaleX = x * paddedWidth +paddingX
        int scaleY = y * paddedHeight +paddingY
        int scaleWidth = width * paddedWidth
        int scaleHeight = height * paddedHeight

        [scaleWidth, scaleHeight, scaleX, scaleY]
    }


    //THE CAKE IS A LIE
    //Storing as doubles and then normalizing for output purposes forces the values to be unique
    //Which seems to improve performance slightly, even if it is a lie (since the values used are not actually ints <= size
    private List normalizedValues(Rectangle rect) {
        BigDecimal e = 1D*rect.east
        BigDecimal n = 1D*rect.north
        BigDecimal w = 1D*rect.west
        BigDecimal s = 1D*rect.south

        if(e == 1D || n == 1D || w == 1D || s==1D) return [size-1, size-1,size-1,size-1]

        def easts = rects[1..-2].collect{ 1D*it.east }.sort()
        def norths = rects[1..-2].collect{ 1D*it.north }.sort()
        def wests  = rects[1..-2].collect{1D*it.west }.sort()
        def souths = rects[1..-2].collect{ 1D*it.south }.sort()

        [easts.indexOf(1D*rect.east)+1, norths.indexOf(1D*rect.north)+1, wests.indexOf(1D*rect.west)+1, souths.indexOf(1D*rect.south)+1]
    }
}