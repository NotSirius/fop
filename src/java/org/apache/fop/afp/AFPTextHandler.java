/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.afp;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.afp.fonts.AFPFont;
import org.apache.fop.afp.fonts.AFPFontAttributes;
import org.apache.fop.afp.modca.GraphicsObject;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontInfo;
import org.apache.xmlgraphics.java2d.TextHandler;

/**
 * Specialized TextHandler implementation that the AFPGraphics2D class delegates to to paint text
 * using AFP GOCA text operations.
 */
public class AFPTextHandler implements TextHandler {

    /** logging instance */
    private static Log log = LogFactory.getLog(AFPTextHandler.class);

    private static final int X = 0;
    private static final int Y = 1;

    private AFPGraphics2D g2d = null;

    /** Overriding FontState */
    protected Font overrideFont = null;

    /**
     * Main constructor.
     * @param g2d the AFPGraphics2D instance
     */
    public AFPTextHandler(AFPGraphics2D g2d) {
        this.g2d = g2d;
    }

    /**
     * Return the font information associated with this object
     *
     * @return the FontInfo object
     */
    public FontInfo getFontInfo() {
        return g2d.getFontInfo();
    }

    /**
     * Add a text string to the current data object of the AFP datastream.
     * The text is painted using text operations.
     *
     * {@inheritDoc}
     */
    public void drawString(String str, float x, float y) throws IOException {
        log.debug("drawString() str=" + str + ", x=" + x + ", y=" + y);
        GraphicsObject graphicsObj = g2d.getGraphicsObject();
        Color color = g2d.getColor();

        AFPPaintingState paintingState = g2d.getPaintingState();
        if (paintingState.setColor(color)) {
            graphicsObj.setColor(color);
        }
        if (overrideFont != null) {
            FontInfo fontInfo = getFontInfo();
            AFPPageFonts pageFonts = paintingState.getPageFonts();
            String internalFontName = overrideFont.getFontName();
            int fontSize = overrideFont.getFontSize();
            if (paintingState.setFontName(internalFontName) || paintingState.setFontSize(fontSize)) {
                AFPFont font = (AFPFont)fontInfo.getFonts().get(internalFontName);
                AFPFontAttributes afpFontAttributes = pageFonts.registerFont(
                        internalFontName,
                        font,
                        fontSize
                );
                int fontReference = afpFontAttributes.getFontReference();
                graphicsObj.setCharacterSet(fontReference);
            }
        }

        // calculate x, y plotting coordinates from graphics context
        AffineTransform at = g2d.getTransform();
        float[] srcPts = new float[] { x, y };
        float[] dstPts = new float[srcPts.length];
        at.transform(srcPts, 0, dstPts, 0, 1);

        graphicsObj.addString(str, Math.round(dstPts[X]), Math.round(dstPts[Y]));
    }

    /**
     * Sets the overriding font.
     *
     * @param overrideFont Overriding Font to set
     */
    public void setOverrideFont(Font overrideFont) {
        this.overrideFont = overrideFont;
    }
}
