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

package org.apache.fop.render.pdf;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.pdf.PDFColor;
import org.apache.fop.pdf.PDFDocument;
import org.apache.fop.pdf.PDFFilterList;
import org.apache.fop.pdf.PDFNumber;
import org.apache.fop.pdf.PDFResourceContext;
import org.apache.fop.pdf.PDFState;
import org.apache.fop.pdf.PDFStream;
import org.apache.fop.pdf.PDFTextUtil;
import org.apache.fop.pdf.PDFXObject;

/**
 * Generator class encapsulating all object references and state necessary to generate a
 * PDF content stream.
 */
public class PDFContentGenerator {

    /** Controls whether comments are written to the PDF stream. */
    protected static final boolean WRITE_COMMENTS = true;

    private PDFDocument document;
    private OutputStream outputStream;
    private PDFResourceContext resourceContext;

    /** the current stream to add PDF commands to */
    private PDFStream currentStream;

    /** drawing state */
    protected PDFState currentState = null;
    /** Text generation utility holding the current font status */
    protected PDFTextUtil textutil;


    /**
     * Main constructor. Creates a new PDF stream and additional helper classes for text painting
     * and state management.
     * @param document the PDF document
     * @param out the output stream the PDF document is generated to
     * @param resourceContext the resource context
     */
    public PDFContentGenerator(PDFDocument document, OutputStream out,
            PDFResourceContext resourceContext) {
        this.document = document;
        this.outputStream = out;
        this.resourceContext = resourceContext;
        this.currentStream = document.getFactory()
                .makeStream(PDFFilterList.CONTENT_FILTER, false);
        this.textutil = new PDFTextUtil() {
            protected void write(String code) {
                currentStream.add(code);
            }
        };

        this.currentState = new PDFState();
    }

    /**
     * Returns the applicable resource context for the generator.
     * @return the resource context
     */
    public PDFDocument getDocument() {
        return this.document;
    }

    /**
     * Returns the output stream the PDF document is written to.
     * @return the output stream
     */
    public OutputStream getOutputStream() {
        return this.outputStream;
    }

    /**
     * Returns the applicable resource context for the generator.
     * @return the resource context
     */
    public PDFResourceContext getResourceContext() {
        return this.resourceContext;
    }

    /**
     * Returns the {@code PDFStream} associated with this instance.
     * @return the PDF stream
     */
    public PDFStream getStream() {
        return this.currentStream;
    }

    /**
     * Returns the {@code PDFState} associated with this instance.
     * @return the PDF state
     */
    public PDFState getState() {
        return this.currentState;
    }

    /**
     * Returns the {@code PDFTextUtil} associated with this instance.
     * @return the text utility
     */
    public PDFTextUtil getTextUtil() {
        return this.textutil;
    }

    /**
     * Flushes all queued PDF objects ready to be written to the output stream.
     * @throws IOException if an error occurs while flushing the PDF objects
     */
    public void flushPDFDoc() throws IOException {
        this.document.output(this.outputStream);
    }

    /**
     * Writes out a comment.
     * @param text text for the comment
     */
    protected void comment(String text) {
        if (WRITE_COMMENTS) {
            currentStream.add("% " + text + "\n");
        }
    }

    /** {@inheritDoc} */
    protected void saveGraphicsState() {
        endTextObject();
        currentState.push();
        currentStream.add("q\n");
    }

    /**
     * Restored the graphics state valid before the previous {@code #saveGraphicsState()}.
     * @param popState true if the state should also be popped, false if only the PDF command
     *           should be issued
     */
    protected void restoreGraphicsState(boolean popState) {
        endTextObject();
        currentStream.add("Q\n");
        if (popState) {
            currentState.pop();
        }
    }

    /** {@inheritDoc} */
    protected void restoreGraphicsState() {
        restoreGraphicsState(true);
    }

    /** Indicates the beginning of a text object. */
    protected void beginTextObject() {
        if (!textutil.isInTextObject()) {
            textutil.beginTextObject();
        }
    }

    /** Indicates the end of a text object. */
    protected void endTextObject() {
        if (textutil.isInTextObject()) {
            textutil.endTextObject();
        }
    }

    /**
     * Concatenates the given transformation matrix with the current one.
     * @param transform the transformation matrix (in points)
     */
    public void concatenate(AffineTransform transform) {
        concatenate(transform, false);
    }

    /**
     * Concatenates the given transformation matrix with the current one.
     * @param transform the transformation matrix
     * @param convertMillipoints true if the coordinates are in millipoints and need to be
     *          converted to points
     */
    public void concatenate(AffineTransform transform, boolean convertMillipoints) {
        if (!transform.isIdentity()) {
            currentState.concatenate(transform);
            currentStream.add(CTMHelper.toPDFString(transform, convertMillipoints) + " cm\n");
        }
    }

    /**
     * Adds content to the stream.
     * @param content the PDF content
     */
    public void add(String content) {
        currentStream.add(content);
    }

    /**
     * Formats a float value (normally coordinates in points) as Strings.
     * @param value the value
     * @return the formatted value
     */
    protected static String format(float value) {
        return PDFNumber.doubleOut(value);
    }

    /**
     * Sets the current line width in points.
     * @param width line width in points
     */
    public void updateLineWidth(float width) {
        if (currentState.setLineWidth(width)) {
            //Only write if value has changed WRT the current line width
            currentStream.add(format(width) + " w\n");
        }
    }

    /**
     * Establishes a new foreground or fill color. In contrast to updateColor
     * this method does not check the PDFState for optimization possibilities.
     * @param col the color to apply
     * @param fill true to set the fill color, false for the foreground color
     * @param pdf StringBuffer to write the PDF code to
     *//*
    public void setColor(Color col, boolean fill, StringBuffer pdf) {
        assert pdf != null;
    }*/

    /**
     * Establishes a new foreground or fill color.
     * @param col the color to apply
     * @param fill true to set the fill color, false for the foreground color
     * @param stream the PDFStream to write the PDF code to
     */
    public void setColor(Color col, boolean fill, PDFStream stream) {
        assert stream != null;
        PDFColor color = new PDFColor(this.document, col);
        stream.add(color.getColorSpaceOut(fill));
    }

    /**
     * Establishes a new foreground or fill color.
     * @param col the color to apply
     * @param fill true to set the fill color, false for the foreground color
     */
    public void setColor(Color col, boolean fill) {
        setColor(col, fill, getStream());
    }

    /**
     * Establishes a new foreground or fill color. In contrast to updateColor
     * this method does not check the PDFState for optimization possibilities.
     * @param col the color to apply
     * @param fill true to set the fill color, false for the foreground color
     * @param pdf StringBuffer to write the PDF code to, if null, the code is
     *     written to the current stream.
     */
    protected void setColor(Color col, boolean fill, StringBuffer pdf) {
        if (pdf != null) {
            PDFColor color = new PDFColor(this.document, col);
            pdf.append(color.getColorSpaceOut(fill));
        } else {
            setColor(col, fill, this.currentStream);
        }
    }

    /**
     * Establishes a new foreground or fill color.
     * @param col the color to apply (null skips this operation)
     * @param fill true to set the fill color, false for the foreground color
     * @param pdf StringBuffer to write the PDF code to, if null, the code is
     *     written to the current stream.
     */
    public void updateColor(Color col, boolean fill, StringBuffer pdf) {
        if (col == null) {
            return;
        }
        boolean update = false;
        if (fill) {
            update = getState().setBackColor(col);
        } else {
            update = getState().setColor(col);
        }

        if (update) {
            setColor(col, fill, pdf);
        }
    }

    /**
     * Places a previously registered image at a certain place on the page.
     * @param x X coordinate
     * @param y Y coordinate
     * @param w width for image
     * @param h height for image
     * @param xobj the image XObject
     */
    public void placeImage(float x, float y, float w, float h, PDFXObject xobj) {
        saveGraphicsState();
        add(format(w) + " 0 0 "
                          + format(-h) + " "
                          + format(x) + " "
                          + format(y + h)
                          + " cm\n" + xobj.getName() + " Do\n");
        restoreGraphicsState();
    }


}