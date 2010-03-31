/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import org.netbeans.lib.profiler.charts.ChartOverlay;
import org.netbeans.lib.profiler.charts.ChartSelectionListener;
import org.netbeans.lib.profiler.charts.ItemSelection;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.charts.ChartConfigurationListener;
import org.netbeans.lib.profiler.charts.ChartContext;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.XYItem;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineSelectionOverlay extends ChartOverlay {

    private TimelineChart chart;

    private int selectionExtent;

    private ConfigurationListener configurationListener;
    private SelectionListener selectionListener;
    private final Set<Point> highlightedValues;
    private final Set<Point> selectedValues;

    private Paint hMarkPaint;
    private Paint hOddPerfPaint;
    private Paint hEvenPerfPaint;
    private Paint sMarkPaint;
    private Paint sOddPerfPaint;
    private Paint sEvenPerfPaint;

    private Stroke markStroke;
    private Stroke oddPerfStroke;
    private Stroke evenPerfStroke;


    TimelineSelectionOverlay() {
        configurationListener = new ConfigurationListener();
        selectionListener = new SelectionListener();
        highlightedValues = new HashSet();
        selectedValues = new HashSet();
        initDefaultValues();
    }
    

    // --- Internal API --------------------------------------------------------

    final void registerChart(TimelineChart chart) {
        unregisterListener();
        this.chart = chart;
        registerListener();
    }

    final void unregisterChart(TimelineChart chart) {
        unregisterListener();
        this.chart = null;
    }


    // --- Private implementation ----------------------------------------------

    private void registerListener() {
        if (chart == null) return;
        chart.addConfigurationListener(configurationListener);
        chart.addRowListener(configurationListener);
        chart.getSelectionModel().addSelectionListener(selectionListener);
    }

    private void unregisterListener() {
        if (chart == null) return;
        chart.removeConfigurationListener(configurationListener);
        chart.removeRowListener(configurationListener);
        chart.getSelectionModel().removeSelectionListener(selectionListener);
    }

    private void initDefaultValues() {
        hMarkPaint = new Color(80, 80, 80);
        hOddPerfPaint = Color.BLACK;
        hEvenPerfPaint = Color.WHITE;

        sMarkPaint = new Color(40, 40, 120);
        sOddPerfPaint = new Color(120, 120, 120);
        sEvenPerfPaint = Color.WHITE;

        markStroke = new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        oddPerfStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 1.0f, 3.0f }, 0);
        evenPerfStroke = new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 1.0f, 3.0f }, 2);

        selectionExtent = 3;
    }


    public void paint(Graphics g) {
        if (highlightedValues.isEmpty() && selectedValues.isEmpty()) return;

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHints(chart.getRenderingHints());
        
        Iterator<Point> it = selectedValues.iterator();
        boolean linePainted = false;

        while (it.hasNext()) {
            Point p = it.next();

            if (!linePainted) {
                g2.setPaint(sEvenPerfPaint);
                g2.setStroke(evenPerfStroke);
                g2.drawLine(p.x, 0, p.x, getHeight());
                g2.setPaint(sOddPerfPaint);
                g2.setStroke(oddPerfStroke);
                g2.drawLine(p.x, 0, p.x, getHeight());

                g2.setPaint(sMarkPaint);
                g2.setStroke(markStroke);

                linePainted = true;
            }

            g2.fillOval(p.x - selectionExtent + 1, p.y - selectionExtent + 1,
                        selectionExtent * 2 - 1, selectionExtent * 2 - 1);
        }

        it = highlightedValues.iterator();
        linePainted = false;

        while (it.hasNext()) {
            Point p = it.next();

            if (!linePainted) {
                g2.setPaint(hEvenPerfPaint);
                g2.setStroke(evenPerfStroke);
                g2.drawLine(p.x, 0, p.x, getHeight());
                g2.setPaint(hOddPerfPaint);
                g2.setStroke(oddPerfStroke);
                g2.drawLine(p.x, 0, p.x, getHeight());

                g2.setPaint(hMarkPaint);
                g2.setStroke(markStroke);

                linePainted = true;
            }

            g2.fillOval(p.x - selectionExtent + 1, p.y - selectionExtent + 1,
                        selectionExtent * 2 - 1, selectionExtent * 2 - 1);
        }

    }

    private void vLineBoundsChanged(Set<Point> oldSelection, Set<Point> newSelection) {
        Point oldSel = oldSelection.isEmpty() ? null : oldSelection.iterator().next();
        Point newSel = newSelection.isEmpty() ? null : newSelection.iterator().next();

        if (oldSel != null) repaint(oldSel.x - selectionExtent, 0,
                                             selectionExtent * 2, getHeight());
        if (newSel != null) repaint(newSel.x - selectionExtent, 0,
                                             selectionExtent * 2, getHeight());
    }

    private static void updateValues(Set<Point> values,
                                     List<ItemSelection> selectedItems,
                                     TimelineChart chart) {
        values.clear();
        for (ItemSelection sel : selectedItems) {
            XYItemSelection xySel = (XYItemSelection)sel;
            XYItem item = xySel.getItem();
            TimelineXYPainter painter = (TimelineXYPainter)chart.getPaintersModel().getPainter(item);
            ChartContext context = chart.getChartContext(item);
            long xValue = item.getXValue(xySel.getValueIndex());
            long yValue = item.getYValue(xySel.getValueIndex());
//            long yValue = painter.getItemView(item.getYValue(xySel.getValueIndex()), item, context);
            values.add(new Point(Utils.checkedInt(Math.ceil(context.getViewX(xValue))),
                                 Utils.checkedInt(Math.ceil(painter.getItemView(yValue, item, context)))));
        }
    }


    private class ConfigurationListener extends ChartConfigurationListener.Adapter
                                        implements TimelineChart.RowListener {
        private final Runnable selectionUpdater = new Runnable() {
            public void run() {
                Set<Point> oldValues = new HashSet(selectedValues);
                updateValues(selectedValues, chart.getSelectionModel().
                             getSelectedItems(), chart);
                vLineBoundsChanged(oldValues, selectedValues);
                oldValues = new HashSet(highlightedValues);
                updateValues(highlightedValues, chart.getSelectionModel().
                             getHighlightedItems(), chart);
                vLineBoundsChanged(oldValues, highlightedValues);
            }
        };
        public void contentsUpdated(long offsetX, long offsetY,
                                    double scaleX, double scaleY,
                                    long lastOffsetX, long lastOffsetY,
                                    double lastScaleX, double lastScaleY,
                                    int shiftX, int shiftY) {
            if (highlightedValues.isEmpty() && selectedValues.isEmpty()) return;
            if (scaleX != lastScaleX || scaleY != lastScaleY || shiftX != 0 || shiftY != 0)
                SwingUtilities.invokeLater(selectionUpdater);
        }
        public void rowsAdded(List<TimelineChart.Row> rows) { selectionUpdater.run(); };

        public void rowsRemoved(List<TimelineChart.Row> rows) { selectionUpdater.run(); };

        public void rowsResized(List<TimelineChart.Row> rows) { selectionUpdater.run(); };
    }

    private class SelectionListener implements ChartSelectionListener {

        private final Runnable selectionUpdater = new Runnable() {
            public void run() {
                Set<Point> oldSelectedValues = new HashSet(selectedValues);
                updateValues(selectedValues, chart.getSelectionModel().
                             getSelectedItems(), chart);
                vLineBoundsChanged(oldSelectedValues, selectedValues);
            }
        };

        public void selectionModeChanged(int newMode, int oldMode) {}

        public void selectionBoundsChanged(Rectangle newBounds, Rectangle oldBounds) {}

        public void selectedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
            SwingUtilities.invokeLater(selectionUpdater);
        }

        public void highlightedItemsChanged(List<ItemSelection> currentItems,
              List<ItemSelection> addedItems, List<ItemSelection> removedItems) {
            Set<Point> oldHighlightedValues = new HashSet(highlightedValues);
            updateValues(highlightedValues, currentItems, chart);
            vLineBoundsChanged(oldHighlightedValues, highlightedValues);
        }

    }

}