<#
.SYNOPSIS
    Repeatable alpha-channel measurement script for Fotara profile borders.
    Extracts exact bounding extents, optimal inner inscribed circle, ring outer diameter,
    radial thickness, and average luminance for Kotlin ProfileBorder table entries.

.DESCRIPTION
    Directly locks Bitmap bytes in memory via C# P/Invoke for sub-second precision measurement.
    Examines:
    1. Alpha > 10 bounding box for decorative extents (wings, sparkles).
    2. Horizontal & vertical equatorial spans to locate inner transparent hole center (cx, cy).
    3. 360-degree radial raycasting to measure solid ring outer edge and luminance profile.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File app/src/test/scripts/MeasureBorderAlpha.ps1
#>

$csharpCode = @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
using System.Collections.Generic;

public class DetailedBorderAnalyzer {
    public class Report {
        public string Id;
        public string FileName;
        public int Width;
        public int Height;
        public double ExtentLeft;
        public double ExtentTop;
        public double ExtentRight;
        public double ExtentBottom;
        public double CenterX;
        public double CenterY;
        public double InnerDiameter;
        public double InnerDiameterRatio;
        public double HorizHoleWidth;
        public double VertHoleHeight;
        public double CircularityErrorPx;
        public double RingOuterDiameter;
        public double RingOuterDiameterRatio;
        public double RingThicknessPx;
        public double RingThicknessRatio;
        public double MinLum;
        public double AvgLum;
        public double MaxLum;
    }

    public static Report Analyze(string filePath, string id) {
        using (var bmp = new Bitmap(filePath)) {
            int w = bmp.Width;
            int h = bmp.Height;

            var rect = new Rectangle(0, 0, w, h);
            var bmpData = bmp.LockBits(rect, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
            byte[] bytes = new byte[bmpData.Stride * h];
            Marshal.Copy(bmpData.Scan0, bytes, 0, bytes.Length);
            bmp.UnlockBits(bmpData);
            int stride = bmpData.Stride;

            // 1. Extent bounding box (alpha > 10)
            int minX = w, maxX = 0, minY = h, maxY = 0;
            for (int y = 0; y < h; y++) {
                int rowOffset = y * stride;
                for (int x = 0; x < w; x++) {
                    byte a = bytes[rowOffset + x * 4 + 3];
                    if (a > 10) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            // 2. Measure horizontal & vertical inner hole spans across center region
            double maxHoleW = 0;
            int bestHoleY = h / 2;
            double holeMidX = w / 2.0;

            for (int y = (int)(h * 0.35); y < (int)(h * 0.65); y++) {
                int start = -1, end = -1;
                bool inHole = false;
                for (int x = (int)(w * 0.1); x < (int)(w * 0.9); x++) {
                    byte a = bytes[y * stride + x * 4 + 3];
                    if (!inHole && a <= 30) {
                        inHole = true;
                        start = x;
                    } else if (inHole && a > 30) {
                        end = x;
                        break;
                    }
                }
                if (start > 0 && end > start) {
                    double span = end - start;
                    if (span > maxHoleW) {
                        maxHoleW = span;
                        bestHoleY = y;
                        holeMidX = (start + end) / 2.0;
                    }
                }
            }

            double maxHoleH = 0;
            int bestHoleX = w / 2;
            double holeMidY = h / 2.0;

            for (int x = (int)(w * 0.35); x < (int)(w * 0.65); x++) {
                int start = -1, end = -1;
                bool inHole = false;
                for (int y = (int)(h * 0.1); y < (int)(h * 0.9); y++) {
                    byte a = bytes[y * stride + x * 4 + 3];
                    if (!inHole && a <= 30) {
                        inHole = true;
                        start = y;
                    } else if (inHole && a > 30) {
                        end = y;
                        break;
                    }
                }
                if (start > 0 && end > start) {
                    double span = end - start;
                    if (span > maxHoleH) {
                        maxHoleH = span;
                        bestHoleX = x;
                        holeMidY = (start + end) / 2.0;
                    }
                }
            }

            double cx = holeMidX;
            double cy = holeMidY;
            double innerDiam = (maxHoleW + maxHoleH) / 2.0;
            double circError = Math.Abs(maxHoleW - maxHoleH);

            // 3. Radial profile analysis (360 degrees)
            List<double> ringOuterRadii = new List<double>();
            List<double> lums = new List<double>();

            for (int deg = 0; deg < 360; deg += 2) {
                double rad = deg * Math.PI / 180.0;
                double cos = Math.Cos(rad);
                double sin = Math.Sin(rad);

                int rStart = (int)(innerDiam / 2.0);
                int rOut = rStart;
                int consecTransp = 0;

                for (int r = rStart; r < (int)(w * 0.7); r++) {
                    int px = (int)(cx + r * cos);
                    int py = (int)(cy + r * sin);
                    if (px >= 0 && px < w && py >= 0 && py < h) {
                        int idx = py * stride + px * 4;
                        byte b = bytes[idx];
                        byte g = bytes[idx + 1];
                        byte rVal = bytes[idx + 2];
                        byte a = bytes[idx + 3];

                        if (a <= 30) {
                            consecTransp++;
                            if (consecTransp >= 10) {
                                rOut = r - 10;
                                break;
                            }
                        } else {
                            consecTransp = 0;
                            rOut = r;
                            double lum = 0.299 * rVal + 0.587 * g + 0.114 * b;
                            lums.Add(lum);
                        }
                    } else {
                        rOut = r;
                        break;
                    }
                }
                ringOuterRadii.Add(rOut);
            }

            ringOuterRadii.Sort();
            lums.Sort();

            double medianOuterRadius = ringOuterRadii[(int)(ringOuterRadii.Count * 0.4)];
            double ringOuterDiam = medianOuterRadius * 2.0;

            double sumLum = 0;
            foreach (var l in lums) sumLum += l;

            var rpt = new Report();
            rpt.Id = id;
            rpt.FileName = System.IO.Path.GetFileName(filePath);
            rpt.Width = w;
            rpt.Height = h;
            rpt.ExtentLeft = Math.Round((double)minX / w, 4);
            rpt.ExtentTop = Math.Round((double)minY / h, 4);
            rpt.ExtentRight = Math.Round((double)maxX / w, 4);
            rpt.ExtentBottom = Math.Round((double)maxY / h, 4);
            rpt.CenterX = Math.Round(cx / w, 4);
            rpt.CenterY = Math.Round(cy / h, 4);
            rpt.InnerDiameter = Math.Round(innerDiam, 1);
            rpt.InnerDiameterRatio = Math.Round(innerDiam / w, 4);
            rpt.HorizHoleWidth = Math.Round(maxHoleW, 1);
            rpt.VertHoleHeight = Math.Round(maxHoleH, 1);
            rpt.CircularityErrorPx = Math.Round(circError, 1);
            rpt.RingOuterDiameter = Math.Round(ringOuterDiam, 1);
            rpt.RingOuterDiameterRatio = Math.Round(ringOuterDiam / w, 4);
            rpt.RingThicknessPx = Math.Round((ringOuterDiam - innerDiam) / 2.0, 1);
            rpt.RingThicknessRatio = Math.Round((rpt.RingOuterDiameterRatio - rpt.InnerDiameterRatio) / 2.0, 4);
            rpt.MinLum = Math.Round(lums[0], 1);
            rpt.AvgLum = Math.Round(sumLum / lums.Count, 1);
            rpt.MaxLum = Math.Round(lums[lums.Count - 1], 1);

            return rpt;
        }
    }
}
"@

Add-Type -TypeDefinition $csharpCode -ReferencedAssemblies "System.Drawing.dll"

$projectRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)))
if (-not (Test-Path "$projectRoot\Assets\Border")) {
    $projectRoot = "c:\Users\sepli\OneDrive\Documents\File DD\Coding\Fotara"
}

$files = Get-ChildItem "$projectRoot\Assets\Border\*.png"
$reports = @()
foreach ($f in $files) {
    $id = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    $r = [DetailedBorderAnalyzer]::Analyze($f.FullName, $id)
    $reports += $r
}

$reports | Format-List
