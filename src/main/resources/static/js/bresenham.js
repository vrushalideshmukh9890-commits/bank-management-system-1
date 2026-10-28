/**
 * ============================================================================
 * COMPUTER GRAPHICS ALGORITHM: Bresenham's Line Drawing Algorithm
 * ============================================================================
 * 
 * College Mini-Project Requirement:
 * This file implements the classic Bresenham Line Drawing Algorithm purely
 * on an HTML5 2D Canvas context without relying on native canvas ctx.lineTo().
 * 
 * ----------------------------------------------------------------------------
 * MATHEMATICAL FOUNDATION & WORKING PRINCIPLE:
 * ----------------------------------------------------------------------------
 * Developed by Jack Elton Bresenham in 1962, this algorithm rasterizes lines
 * using ONLY FAST INTEGER ADDITION, SUBTRACTION, AND BIT-SHIFT (multiplication by 2).
 * It avoids expensive floating-point arithmetic and division.
 * 
 * 1. Line Equation: y = m * x + c, where slope m = dy / dx.
 * 2. In octant 1 (0 <= slope <= 1, x1 < x2):
 *    As x increases by 1 step (pixel by pixel), the ideal y coordinate increases
 *    by m = dy/dx. At each step, the rasterizer must choose between two candidate
 *    pixels:
 *        - E (East pixel):        (x + 1, y)
 *        - NE (North-East pixel): (x + 1, y + 1)
 * 
 * 3. Midpoint & Decision Variable / Error Term (D):
 *    We test the distance of the true line from the midpoint between E and NE:
 *        Midpoint M = (x + 1, y + 0.5)
 *    Define decision parameter:
 *        D = 2 * dy - dx
 * 
 * 4. Error Term Update Rule:
 *    - If D <= 0:
 *        The true line is closer to the East pixel (y remains unchanged).
 *        Choose (x + 1, y).
 *        Next error term: D_new = D + 2 * dy
 *    - If D > 0:
 *        The true line is closer to the North-East pixel (y increments by 1).
 *        Choose (x + 1, y + 1).
 *        Next error term: D_new = D + 2 * (dy - dx)
 * 
 * 5. Generalization for All Octants (Any direction and slope):
 *    We calculate:
 *        dx = |x2 - x1|,  sx = sign(x2 - x1)
 *        dy = -|y2 - y1|, sy = sign(y2 - y1)
 *    and combine both into an integer error accumulator:
 *        err = dx + dy
 *    At each step:
 *        e2 = 2 * err
 *        if e2 >= dy: err += dy; x += sx;
 *        if e2 <= dx: err += dx; y += sy;
 * 
 * ----------------------------------------------------------------------------
 */

class BresenhamCanvasRenderer {
    /**
     * @param {HTMLCanvasElement} canvas 
     */
    constructor(canvas) {
        this.canvas = canvas;
        this.ctx = canvas.getContext('2d');
    }

    /**
     * Plots an individual pixel on the canvas using integer coordinates.
     * Optionally thickness can be simulated by drawing a small square.
     * 
     * @param {number} x - Integer X coordinate
     * @param {number} y - Integer Y coordinate
     * @param {string} color - Hex or rgb color string
     * @param {number} pixelSize - Size of pixel dot (default 1)
     */
    drawPixel(x, y, color = '#0f766e', pixelSize = 2) {
        this.ctx.fillStyle = color;
        // Integer quantization
        const px = Math.round(x);
        const py = Math.round(y);
        this.ctx.fillRect(px - Math.floor(pixelSize / 2), py - Math.floor(pixelSize / 2), pixelSize, pixelSize);
    }

    /**
     * Universal Generalized Bresenham's Line Algorithm
     * Handles lines in all 8 octants (positive, negative slopes, steep & shallow).
     * 
     * @param {number} x0 - Start X
     * @param {number} y0 - Start Y
     * @param {number} x1 - End X
     * @param {number} y1 - End Y
     * @param {string} color - Stroke color
     * @param {number} pixelSize - Width of the plotted line
     */
    drawLine(x0, y0, x1, y1, color = '#0d9488', pixelSize = 2) {
        x0 = Math.round(x0);
        y0 = Math.round(y0);
        x1 = Math.round(x1);
        y1 = Math.round(y1);

        // Absolute differences
        const dx = Math.abs(x1 - x0);
        const dy = -Math.abs(y1 - y0);

        // Step direction (+1 or -1)
        const sx = x0 < x1 ? 1 : -1;
        const sy = y0 < y1 ? 1 : -1;

        // Integer Error Term
        let err = dx + dy;

        while (true) {
            // Plot the current pixel determined by the algorithm
            this.drawPixel(x0, y0, color, pixelSize);

            // Reached destination point
            if (x0 === x1 && y0 === y1) break;

            const e2 = 2 * err;

            // Horizontal step condition
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }

            // Vertical step condition
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /**
     * Clears the entire canvas.
     */
    clear() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }

    /**
     * Renders a complete Line Chart using Bresenham's Algorithm.
     * Includes Coordinate Axes, Subtle Gridlines, Ticks, Value Labels, and Plotted Data Points.
     * 
     * @param {Array<{label: string, value: number}>} dataPoints 
     * @param {Object} options 
     */
    renderLineChart(dataPoints, options = {}) {
        const {
            padding = 50,
            lineColor = '#0f766e',
            gridColor = '#e2e8f0',
            axisColor = '#64748b',
            pointColor = '#d97706',
            textColor = '#334155',
            title = 'Account Balance Trend (Bresenham Algorithm)'
        } = options;

        this.clear();

        const w = this.canvas.width;
        const h = this.canvas.height;
        const plotWidth = w - 2 * padding;
        const plotHeight = h - 2 * padding;

        if (!dataPoints || dataPoints.length === 0) {
            this.ctx.fillStyle = textColor;
            this.ctx.font = '14px Inter, sans-serif';
            this.ctx.textAlign = 'center';
            this.ctx.fillText('No data available to plot', w / 2, h / 2);
            return;
        }

        // Determine min and max
        const values = dataPoints.map(p => p.value);
        let maxVal = Math.max(...values, 1000);
        let minVal = Math.min(...values, 0);
        if (minVal > 0) minVal = 0; // Baseline zero
        const valRange = (maxVal - minVal) || 1;

        // Draw title
        this.ctx.fillStyle = '#0f172a';
        this.ctx.font = '600 14px Inter, sans-serif';
        this.ctx.textAlign = 'left';
        this.ctx.fillText(title, padding, 26);

        // Subtitle badge
        this.ctx.fillStyle = '#0d9488';
        this.ctx.font = '500 11px Inter, sans-serif';
        this.ctx.fillText('• Pure Integer Bresenham Rasterizer Active', padding + 340, 26);

        // 1. Draw Grid Lines & Y-Axis Labels
        const gridSteps = 5;
        this.ctx.font = '11px Inter, sans-serif';
        this.ctx.textAlign = 'right';

        for (let i = 0; i <= gridSteps; i++) {
            const y = padding + plotHeight - (i / gridSteps) * plotHeight;
            const stepVal = minVal + (i / gridSteps) * valRange;

            // Draw horizontal grid line with Bresenham
            this.drawLine(padding, y, w - padding, y, gridColor, 1);

            // Label
            this.ctx.fillStyle = '#64748b';
            this.ctx.fillText('₹' + Math.round(stepVal).toLocaleString('en-IN'), padding - 10, y + 4);
        }

        // 2. Draw Main Axes (Bresenham)
        // Y Axis
        this.drawLine(padding, padding, padding, padding + plotHeight, axisColor, 2);
        // X Axis
        this.drawLine(padding, padding + plotHeight, w - padding, padding + plotHeight, axisColor, 2);

        // Calculate coordinates for each data point
        const coords = dataPoints.map((dp, idx) => {
            const x = (dataPoints.length === 1)
                ? padding + plotWidth / 2
                : padding + (idx / (dataPoints.length - 1)) * plotWidth;
            const y = padding + plotHeight - ((dp.value - minVal) / valRange) * plotHeight;
            return { x, y, label: dp.label, value: dp.value };
        });

        // 3. Connect points using Bresenham's Line Algorithm
        for (let i = 0; i < coords.length - 1; i++) {
            const p1 = coords[i];
            const p2 = coords[i + 1];
            this.drawLine(p1.x, p1.y, p2.x, p2.y, lineColor, 3);
        }

        // 4. Plot vertex nodes and X-axis labels
        this.ctx.textAlign = 'center';
        coords.forEach((pt) => {
            // Draw a distinct circular point on top of the rasterized line
            this.ctx.beginPath();
            this.ctx.arc(pt.x, pt.y, 5, 0, Math.PI * 2);
            this.ctx.fillStyle = pointColor;
            this.ctx.fill();
            this.ctx.lineWidth = 2;
            this.ctx.strokeStyle = '#ffffff';
            this.ctx.stroke();

            // Value badge above point
            this.ctx.fillStyle = '#0f172a';
            this.ctx.font = '600 11px Inter, sans-serif';
            this.ctx.fillText('₹' + Math.round(pt.value).toLocaleString('en-IN'), pt.x, pt.y - 10);

            // X-axis label
            this.ctx.fillStyle = '#64748b';
            this.ctx.font = '500 11px Inter, sans-serif';
            this.ctx.fillText(pt.label, pt.x, padding + plotHeight + 20);
        });
    }
}

// Export to window for global browser access
if (typeof window !== 'undefined') {
    window.BresenhamCanvasRenderer = BresenhamCanvasRenderer;
}
