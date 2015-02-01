package com.rodhilton.rectanglevisibility.gui

import com.rodhilton.rectanglevisibility.main.AppState
import com.rodhilton.rectanglevisibility.main.AppStateListener

import javax.swing.*
import javax.swing.event.MouseInputAdapter
import java.awt.*
import java.awt.event.*

import com.rodhilton.rectanglevisibility.domain.Rectangle as Rect

public class Gui {

    public static void createAndShowGUI(AppState appState) {
        JFrame infoPanel = new JFrame()
        JTextArea infoArea = new JTextArea()
        infoPanel.add(infoArea, BorderLayout.CENTER)

        JFrame frame = new JFrame()
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

        ViewPanel panel = new ViewPanel(appState)

        panel.setPreferredSize(new Dimension(500, 500))
        frame.getContentPane().add(panel, BorderLayout.CENTER)

        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.LINE_AXIS));

        JButton playButton = new JButton("Play")
        JButton pauseButton = new JButton("Pause")
        playButton.setEnabled(false)

        toolBar.add(playButton)
        toolBar.add(pauseButton)

        JScrollBar scrollBar = new JScrollBar(JScrollBar.HORIZONTAL)
        scrollBar.setEnabled(false)
        toolBar.add(new JLabel("Level:"))
        toolBar.add(scrollBar)

        JCheckBox labelCheckBox = new JCheckBox("Labels:", true)
        labelCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        toolBar.add(labelCheckBox)

        JCheckBox hoverCheckBox = new JCheckBox("Hover:", false)
        hoverCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        toolBar.add(hoverCheckBox)

        JButton exportButton = new JButton("Export")
        toolBar.add(exportButton)

        frame.getContentPane().add(toolBar, BorderLayout.PAGE_START)
        frame.pack();

        //Center in screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize()
        frame.setLocation((int) (dim.width / 2 - frame.getSize().width / 2), (int) (dim.height / 2 - frame.getSize().height / 2))

        ArrayList<Image> icons = new ArrayList<Image>()
        icons.add(new ImageIcon(getClass().getResource("/icon16.png")).getImage());
        icons.add(new ImageIcon(getClass().getResource("/icon32.png")).getImage());
        icons.add(new ImageIcon(getClass().getResource("/icon64.png")).getImage());
        icons.add(new ImageIcon(getClass().getResource("/icon128.png")).getImage());
        def bigIcon = new ImageIcon(getClass().getResource("/icon256.png")).getImage()
        icons.add(bigIcon);
        frame.setIconImages(icons)

        try {
            Object appleApplication = Class.forName("com.apple.eawt.Application").newInstance()
            def application = (com.apple.eawt.Application)appleApplication
            application.setDockIconImage(bigIcon);
        } catch(ClassNotFoundException e) {
            //Ignore, just means we're not running on an Apple
        }

        frame.setVisible(true)

        //UI Event Listeners
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            void componentResized(ComponentEvent componentEvent) {

                appState.updateSize(panel.size)
                matchInfoFrameToMainFrame(frame, infoPanel)
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                matchInfoFrameToMainFrame(frame, infoPanel)
            }
        })

        panel.addMouseMotionListener(new MouseMotionAdapter() {
                 //TODO: changing "which" selected moves all previously moved as well

                //TODO: resizing not working, but moving seems to be
                //TODO:info panel needs "what can't see each other"
                //TODO: need some easy visual way to connect what the info panel is saying to the view
                //TODO: remove 'hover' button, meaningless

                //TODO: changing rectangles might not actually "take" when resuming, look into
            @Override
            void mouseMoved(MouseEvent mouseEvent) {
                int threshold = 4
                boolean isMoving = false

                if (panel.currentDiagram != null && appState.paused && !isMoving) {

                    int highlighted = appState.currRect -1
                    Rect moveableRect = panel.currentDiagram.getRect(highlighted)
                    Rect originalRect = moveableRect.copy()


                    def (int scaleWidth, int scaleHeight, int scaleX, int scaleY) = panel.currentDiagram.getScaledRect(highlighted, panel.width, panel.height)

                    def mousePoint = mouseEvent.getPoint()

                    def mouseX = (int)mousePoint.getX()
                    def mouseY = (int)mousePoint.getY()

                    def northPos = scaleY
                    def southPos = scaleY+scaleHeight
                    def westPos = scaleX
                    def eastPos = scaleX + scaleWidth

                    boolean contained = mouseX < eastPos + threshold && mouseX > westPos - threshold &&
                        mouseY < southPos + threshold && mouseY > northPos - threshold

                    boolean movingNorth = contained && Math.abs(mouseY-northPos) < threshold
                    boolean movingSouth = contained && Math.abs(mouseY-southPos) < threshold
                    boolean movingWest = contained && Math.abs(mouseX-westPos) < threshold
                    boolean movingEast = contained && Math.abs(mouseX-eastPos) < threshold

                    boolean movingRectangle = contained && !movingNorth && !movingWest && !movingSouth && !movingEast

                    if(movingNorth && movingEast) {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    } else if(movingNorth && movingWest) {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    } else if(movingSouth && movingEast) {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    } else if(movingSouth && movingWest) {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                    } else if(movingSouth) {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR))
                    } else if(movingNorth) {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR))
                    } else if(movingWest) {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR))
                    } else if(movingEast) {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
                    } else if(movingRectangle) {
                        if (System.getProperty("os.name").startsWith("Mac OS X")) { //Dumb.
                            frame.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        } else {
                            frame.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR))
                        }
                    } else {
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
                    }

                    //Ugly
                    if(contained) {
                        def adapter = new MouseInputAdapter() {
                            @Override
                            void mouseDragged(MouseEvent mouseDraggedEvent) {
                                super.mouseDragged(mouseDraggedEvent)
                                isMoving=true

                                def (double originalScaledX, double originalScaledY) = panel.currentDiagram.convertPoint(mousePoint, panel.width, panel.height)

                                def (double scaledX, double scaledY) = panel.currentDiagram.convertPoint(mouseDraggedEvent.getPoint(), panel.width, panel.height)

                                def originalX = Math.round(originalScaledX)
                                def originalY = Math.round(originalScaledY)

                                def newX = Math.round(scaledX)
                                def newY = Math.round(scaledY)

                                def offsetX = Math.round(scaledX - originalScaledX)
                                def offsetY = Math.round(scaledY - originalScaledY)


                                    if(movingEast) {
                                        moveableRect.east = originalRect.east + offsetX
                                    } else if(movingWest) {
                                        moveableRect.west = originalRect.west - offsetX
                                    } else if(movingNorth) {
                                        moveableRect.north = originalRect.north - offsetY
                                    } else if(movingSouth) {
                                        moveableRect.south = originalRect.south + offsetY
                                    } else if (movingRectangle) {

                                            println("moving rectangle only?")
                                            moveableRect.east = originalRect.east + offsetX
                                            moveableRect.west = originalRect.west - offsetX
                                            moveableRect.north = originalRect.north - offsetY
                                            moveableRect.south = originalRect.south + offsetY

                                    }



                                panel.repaint()
                            }

                            @Override
                            void mouseReleased(MouseEvent mouseReleasedEvent) {
                                isMoving=false
                            }
                        }

                        panel.addMouseListener(adapter)
                        panel.addMouseMotionListener(adapter)
                    }

                    //We subtract one from currRect because we want to be able to highlight whatever we can "see" meaning see through the topmost rect
                    //int highlighted = panel.currentDiagram.getTopRectangleNumberContaining(mouseEvent.x, mouseEvent.y, panel.width, panel.height, appState.currRect - 1)
                    //appState.updateHighlighted(highlighted)
                }
            }
        })

        scrollBar.addAdjustmentListener(new AdjustmentListener() {
            @Override
            void adjustmentValueChanged(AdjustmentEvent adjustmentEvent) {
                appState.updateCurrentRectangle(adjustmentEvent.value)
            }
        })

        playButton.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent actionEvent) {
                playButton.setEnabled(false)
                pauseButton.setEnabled(true)
                appState.updatePaused(false)

                infoPanel.setVisible(false)
            }
        })

        labelCheckBox.addItemListener(new ItemListener() {
            @Override
            void itemStateChanged(ItemEvent itemEvent) {
                appState.updateShowLabels(itemEvent.stateChange == ItemEvent.SELECTED)
            }
        })

        hoverCheckBox.addItemListener(new ItemListener() {
            @Override
            void itemStateChanged(ItemEvent itemEvent) {
                appState.updateHover(itemEvent.stateChange == ItemEvent.SELECTED)
            }
        })

        pauseButton.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent actionEvent) {
                pauseButton.setEnabled(false)
                playButton.setEnabled(true)
                appState.updatePaused(true)

                matchInfoFrameToMainFrame(frame, infoPanel)
                infoPanel.setVisible(true)

                frame.toFront();
                frame.setState(Frame.NORMAL);
            }
        })

        exportButton.addActionListener(new ActionListener() {
            @Override
            void actionPerformed(ActionEvent actionEvent) {
                JFileChooser c = new JFileChooser();
                // Demonstrate "Open" dialog:
                int rVal = c.showSaveDialog(frame);
                if (rVal == JFileChooser.APPROVE_OPTION) {
                    def file = c.getSelectedFile()
                    file.withWriter {out ->
                        out.write(appState.diagram.toString())
                    }
                    JOptionPane.showMessageDialog(frame, "Saved to ${file.getAbsolutePath()}", "Saved", JOptionPane.INFORMATION_MESSAGE);
                }

            }
        })

        //App state Listener
        appState.register(new AppStateListener() {
            @Override
            void updateState(AppState state) {
                if(!scrollBar.enabled || state.maxRect > scrollBar.maximum + 1) {
                    scrollBar.setValues(appState.maxRect-1, 1, 1, appState.maxRect + 1)
                    scrollBar.enabled = true
                }
                if (state.completed) {
                    frame.setTitle(state.title + " - DONE!")
                    pauseButton.setEnabled(false)
                    playButton.setEnabled(false)
                } else {
                    frame.setTitle(state.title)
                }
            }
        })
    }

    public static matchInfoFrameToMainFrame(JFrame mainFrame, JFrame infoFrame) {
        int infoWidth = 150
        Rectangle mainBounds = mainFrame.getBounds()
        Point mainPoint = new Point((int)mainBounds.getLocation().getX()-infoWidth, (int)mainBounds.getLocation().getY());
        Dimension mainDimension = new Dimension(infoWidth, (int)mainBounds.getSize().getHeight())

        Rectangle infoBounds = new Rectangle(mainPoint, mainDimension);

        infoFrame.setBounds(infoBounds);
    }
}

