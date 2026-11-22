# --- Arinara Network (c) 2026 ---
# Exclusive property of Arinara Network.
# Unauthorized use, reproduction, distribution, or modification of this code,
# in whole or in part, for any purpose, is strictly prohibited without prior
# written consent from Arinara Network as sole legal owner of this codebase.

param(
    [string]$Version = "2.6.6",
    [string]$Codename = "Hyperion",
    [string]$Description = "Class Schedule import fix, widget Space picker, Support Goal page, right-aligned folder pin, and sanitized FTR-265-S1 build.",
    [string]$ReleaseDate = "2026-11-22",
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$TemplatePath = Join-Path $ProjectRoot "Assets\Banners\Banner Template.jpg"
if (-not (Test-Path $TemplatePath)) {
    $TemplatePath = Join-Path $ProjectRoot "Assets\Banners\Template New Banner (1).jpg"
}
$ExamplePath  = Join-Path $ProjectRoot "Assets\Banners\Banner Example.jpg"
$FontBoldPath = Join-Path $ProjectRoot "app\src\main\res\font\elms_sans_bold.ttf"
$FontMedPath  = Join-Path $ProjectRoot "app\src\main\res\font\elms_sans_medium.ttf"
$FontRegPath  = Join-Path $ProjectRoot "app\src\main\res\font\elms_sans_regular.ttf"

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $ProjectRoot "Assets\Banners\FotaraBanner_${Version}_${ReleaseDate}.png"
}

if (-not (Test-Path $TemplatePath)) { throw "Missing template: $TemplatePath" }
if (-not (Test-Path $FontBoldPath)) { throw "Missing ElmsSans Bold font: $FontBoldPath" }

$baseImg = [System.Drawing.Bitmap]::new($TemplatePath)
$W = $baseImg.Width
$H = $baseImg.Height

$bullet = [char]0x2022
$deg    = [char]0x00B0

# Load bundled ElmsSans fonts
$pfc = [System.Drawing.Text.PrivateFontCollection]::new()
$pfc.AddFontFile($FontBoldPath)
if (Test-Path $FontMedPath) { $pfc.AddFontFile($FontMedPath) }
if (Test-Path $FontRegPath) { $pfc.AddFontFile($FontRegPath) }
$elmsFamily = $pfc.Families[0]

# Snapshot top-left pre-baked logo region (X=192..302, Y=258..302 on 2460x1000) to guarantee 0% modification
$logoGuardRect = [System.Drawing.Rectangle]::new([int]($W * 0.072), [int]($H * 0.24), [int]($W * 0.065), [int]($H * 0.08))
$logoGuardSnapshot = $baseImg.Clone($logoGuardRect, $baseImg.PixelFormat)

$canvas = [System.Drawing.Bitmap]::new($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($canvas)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

# 1. Draw untouched base template (preserves top-left logos and soft blue bottom gradient)
$g.DrawImage($baseImg, 0, 0, $W, $H)

function New-RoundedRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $maxR = [Math]::Min($w, $h) / 2.0
    $safeR = [Math]::Min($r, $maxR)
    $d = [float]($safeR * 2.0)
    if ($d -le 1.0) {
        $path.AddRectangle([System.Drawing.RectangleF]::new($x, $y, $w, $h))
        return $path
    }
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-TopRoundedRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $maxR = [Math]::Min($w, $h) / 2.0
    $safeR = [Math]::Min($r, $maxR)
    $d = [float]($safeR * 2.0)
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddLine($x + $w, $y + $safeR, $x + $w, $y + $h)
    $path.AddLine($x + $w, $y + $h, $x, $y + $h)
    $path.AddLine($x, $y + $h, $x, $y + $safeR)
    $path.CloseFigure()
    return $path
}

# Electric Slate Dark Theme Palette
$cBgCanvas       = [System.Drawing.Color]::FromArgb(255, 13, 17, 23)     # #0D1117
$cSurfacePanel   = [System.Drawing.Color]::FromArgb(255, 22, 28, 38)     # #161C26
$cSurfaceElev    = [System.Drawing.Color]::FromArgb(255, 31, 39, 54)     # #1F2736
$cBorderSubtle   = [System.Drawing.Color]::FromArgb(255, 38, 47, 62)     # #262F3E
$cTrackInactive  = [System.Drawing.Color]::FromArgb(255, 40, 50, 69)     # #283245
$cTextPrimary    = [System.Drawing.Color]::FromArgb(255, 248, 250, 252)  # #F8FAFC
$cTextMuted      = [System.Drawing.Color]::FromArgb(255, 148, 163, 184)  # #94A3B8
$cPrimaryAccent  = [System.Drawing.Color]::FromArgb(255, 122, 90, 248)   # #7A5AF8
$cAccentSoft     = [System.Drawing.Color]::FromArgb(48, 122, 90, 248)
$cEmerald        = [System.Drawing.Color]::FromArgb(255, 16, 185, 129)

$bBgCanvas      = [System.Drawing.SolidBrush]::new($cBgCanvas)
$bSurfacePanel  = [System.Drawing.SolidBrush]::new($cSurfacePanel)
$bSurfaceElev   = [System.Drawing.SolidBrush]::new($cSurfaceElev)
$bTrackInactive = [System.Drawing.SolidBrush]::new($cTrackInactive)
$bTextPrimary   = [System.Drawing.SolidBrush]::new($cTextPrimary)
$bTextMuted     = [System.Drawing.SolidBrush]::new($cTextMuted)
$bPrimaryAccent = [System.Drawing.SolidBrush]::new($cPrimaryAccent)
$bAccentSoft    = [System.Drawing.SolidBrush]::new($cAccentSoft)
$bEmerald       = [System.Drawing.SolidBrush]::new($cEmerald)
$pBorderSubtle  = [System.Drawing.Pen]::new($cBorderSubtle, 2.0)
$pAccentBorder  = [System.Drawing.Pen]::new($cPrimaryAccent, 2.5)

# Exact slot coordinates measured from updated Assets\Banners\Banner Example.jpg (2460x1000)
# Left Slot (Brown rectangle: X=1266..1686, Y=274..1000 — Back/Mid Layer, anchored to bottom edge)
$slot1X = [float][Math]::Round($W * (1266.0 / 2460.0))
$slot1Y = [float][Math]::Round($H * (274.0  / 1000.0))
$slot1W = [float][Math]::Round($W * (420.0  / 2460.0))
$slot1H = [float]($H - $slot1Y)

# Far-Right Slot (Light-Blue rectangle: X=1872..2336, Y=192..1000 — Tallest Layer, anchored to bottom edge)
$slot3X = [float][Math]::Round($W * (1872.0 / 2460.0))
$slot3Y = [float][Math]::Round($H * (192.0  / 1000.0))
$slot3W = [float][Math]::Round($W * (464.0  / 2460.0))
$slot3H = [float]($H - $slot3Y)

# Center-Right Slot (Dark-Gray rectangle: X=1554..1866, Y=400..1000 — Foreground Layer overlapping Slot 1 & Slot 3)
$slot2X = [float][Math]::Round($W * (1554.0 / 2460.0))
$slot2Y = [float][Math]::Round($H * (400.0  / 1000.0))
$slot2W = [float][Math]::Round($W * (312.0  / 2460.0))
$slot2H = [float]($H - $slot2Y)

$fCardHeader = [System.Drawing.Font]::new($elmsFamily, [float]($W * 0.0085), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fCardSub    = [System.Drawing.Font]::new($elmsFamily, [float]($W * 0.0060), [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$fCardLabel  = [System.Drawing.Font]::new($elmsFamily, [float]($W * 0.0052), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fCardBody   = [System.Drawing.Font]::new($elmsFamily, [float]($W * 0.0064), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)

# ==============================================================================
# 2A. Render Slot 1 (Back/Mid Layer): Support Fotara (Active Goal & QRIS, No History)
# ==============================================================================
$pathSlot1 = New-TopRoundedRectPath $slot1X $slot1Y $slot1W $slot1H 22.0
$state1 = $g.Save()
$g.SetClip($pathSlot1)
$g.FillPath($bBgCanvas, $pathSlot1)

$g.FillRectangle($bTrackInactive, $slot1X, $slot1Y, $slot1W, 6.0)
$g.FillRectangle($bPrimaryAccent, $slot1X, $slot1Y, [float]($slot1W * 0.64), 6.0)

$pad1 = [float]($slot1W * 0.065)
$curY1 = $slot1Y + 18.0

$g.DrawString("SUPPORT FOTARA $bullet NOTE REUSE", $fCardLabel, $bPrimaryAccent, $slot1X + $pad1, $curY1)
$curY1 += [float]($W * 0.0072)
$g.DrawString("Active Support Goal", $fCardHeader, $bTextPrimary, $slot1X + $pad1, $curY1)
$curY1 += [float]($W * 0.0135)

# Active Goal Card Preview
$goalCardH = [float]($H * 0.24)
$goalCardPath = New-RoundedRectPath ($slot1X + $pad1) $curY1 ($slot1W - 2 * $pad1) $goalCardH 16.0
$g.FillPath($bSurfacePanel, $goalCardPath)
$g.DrawPath($pAccentBorder, $goalCardPath)

$gBadge = New-RoundedRectPath ($slot1X + $pad1 + 12.0) ($curY1 + 12.0) 118.0 24.0 12.0
$g.FillPath($bSurfaceElev, $gBadge)
$g.DrawPath($pBorderSubtle, $gBadge)
$g.DrawString("ACTIVE GOAL", $fCardLabel, $bTextPrimary, $slot1X + $pad1 + 22.0, $curY1 + 17.0)
$gBadge.Dispose()

$pctBadge = New-RoundedRectPath ($slot1X + $pad1 + 138.0) ($curY1 + 12.0) 56.0 24.0 12.0
$g.FillPath($bPrimaryAccent, $pctBadge)
$g.DrawString("64%", $fCardLabel, $bTextPrimary, $slot1X + $pad1 + 150.0, $curY1 + 17.0)
$pctBadge.Dispose()

$g.DrawString("Server & Cloud Upgrade", $fCardBody, $bTextPrimary, $slot1X + $pad1 + 12.0, $curY1 + 48.0)
$g.DrawString("Fast, offline-ready & ad-free", $fCardSub, $bTextMuted, $slot1X + $pad1 + 12.0, $curY1 + 74.0)

$barW = [float]($slot1W - 2 * $pad1 - 24.0)
$g.FillRectangle($bTrackInactive, $slot1X + $pad1 + 12.0, $curY1 + 106.0, $barW, 7.0)
$g.FillRectangle($bPrimaryAccent, $slot1X + $pad1 + 12.0, $curY1 + 106.0, [float]($barW * 0.64), 7.0)

$g.DrawString("Rp 320.000", $fCardBody, $bTextPrimary, $slot1X + $pad1 + 12.0, $curY1 + 124.0)
$g.DrawString("Target Rp 500.000", $fCardSub, $bTextMuted, $slot1X + $pad1 + 12.0, $curY1 + 148.0)
$goalCardPath.Dispose()
$curY1 += $goalCardH + 12.0

$g.DrawString("QRIS $bullet OWNER GOAL NOTE", $fCardLabel, $bTextMuted, $slot1X + $pad1, $curY1)
$curY1 += [float]($W * 0.0085)

$qrisSlots = @(
    @{ Name = "QRIS Code Card"; Time = "Expand"; Meta = "Download QRIS $bullet Clean View" },
    @{ Name = "Goal Rich Note"; Time = "View"; Meta = "Note Editor / Viewer Reuse" },
    @{ Name = "Owner Dev Edit"; Time = "Sync"; Meta = "Signed Supabase WebP Upload" }
)
foreach ($c in $qrisSlots) {
    $rowH = [float]($H * 0.072)
    $rowPath = New-RoundedRectPath ($slot1X + $pad1) $curY1 ($slot1W - 2 * $pad1) $rowH 12.0
    $g.FillPath($bSurfaceElev, $rowPath)
    $g.DrawPath($pBorderSubtle, $rowPath)
    $g.DrawString($c.Name, $fCardBody, $bTextPrimary, $slot1X + $pad1 + 10.0, $curY1 + 7.0)
    $g.DrawString($c.Time, $fCardLabel, $bPrimaryAccent, $slot1X + $pad1 + ($slot1W * 0.46), $curY1 + 8.0)
    $g.DrawString($c.Meta, $fCardSub, $bTextMuted, $slot1X + $pad1 + 10.0, $curY1 + ($rowH * 0.52))
    $rowPath.Dispose()
    $curY1 += $rowH + 8.0
}

$g.Restore($state1)
$g.DrawPath($pBorderSubtle, $pathSlot1)
$pathSlot1.Dispose()

# ==============================================================================
# 2B. Render Slot 3 (Far-Right Tallest Layer): Schedule 2-Col Import & FTR-265-S1
# ==============================================================================
$pathSlot3 = New-TopRoundedRectPath $slot3X $slot3Y $slot3W $slot3H 22.0
$state3 = $g.Save()
$g.SetClip($pathSlot3)
$g.FillPath($bBgCanvas, $pathSlot3)

$pad3 = [float]($slot3W * 0.065)
$curY3 = $slot3Y + 18.0

$g.DrawString("Class Schedule", $fCardHeader, $bTextPrimary, $slot3X + $pad3, $curY3)
$curY3 += [float]($W * 0.013)

$g.DrawString("2-COLUMN TIME $bullet SAX PARSER $bullet DB V34", $fCardLabel, $bTextMuted, $slot3X + $pad3, $curY3)
$curY3 += [float]($W * 0.0085)

$simH = [float]($H * 0.175)
$simPath = New-RoundedRectPath ($slot3X + $pad3) $curY3 ($slot3W - 2 * $pad3) $simH 16.0
$g.FillPath($bSurfacePanel, $simPath)
$g.DrawPath($pAccentBorder, $simPath)

$bdgPath = New-RoundedRectPath ($slot3X + $pad3 + 12.0) ($curY3 + 12.0) 104.0 24.0 12.0
$g.FillPath($bAccentSoft, $bdgPath)
$g.DrawPath($pAccentBorder, $bdgPath)
$g.DrawString("FTR-265-S1", $fCardLabel, $bPrimaryAccent, $slot3X + $pad3 + 22.0, $curY3 + 17.0)
$bdgPath.Dispose()

$gifPill = New-RoundedRectPath ($slot3X + $slot3W - $pad3 - 102.0) ($curY3 + 12.0) 90.0 24.0 12.0
$g.FillPath($bPrimaryAccent, $gifPill)
$g.DrawString("SANITIZED", $fCardLabel, $bTextPrimary, $slot3X + $slot3W - $pad3 - 92.0, $curY3 + 17.0)
$gifPill.Dispose()

$g.DrawString("XLSX / DOCX Import", $fCardBody, $bTextPrimary, $slot3X + $pad3 + 12.0, $curY3 + ($simH * 0.50))
$g.DrawString("Start + End columns & auto-day focus", $fCardSub, $bTextMuted, $slot3X + $pad3 + 12.0, $curY3 + ($simH * 0.72))
$simPath.Dispose()
$curY3 += $simH + 12.0

$g.DrawString("IMPORTED SUBJECTS & STORAGE", $fCardLabel, $bTextMuted, $slot3X + $pad3, $curY3)
$curY3 += [float]($W * 0.0085)

$hsvH = [float]($H * 0.165)
$hsvRect = [System.Drawing.RectangleF]::new($slot3X + $pad3, $curY3, $slot3W - 2 * $pad3, $hsvH)
$hsvPath = New-RoundedRectPath $hsvRect.X $hsvRect.Y $hsvRect.Width $hsvRect.Height 14.0
$g.FillPath($bSurfacePanel, $hsvPath)
$g.DrawPath($pBorderSubtle, $hsvPath)
$g.DrawString("Physics II & Calculus III", $fCardBody, $bTextPrimary, $hsvRect.X + 12.0, $hsvRect.Y + 10.0)
$g.FillEllipse($bEmerald, $hsvRect.X + $hsvRect.Width - 28.0, $hsvRect.Y + 10.0, 16.0, 16.0)
$g.DrawString("08:00 Start $bullet 09:40 End Time", $fCardSub, $bPrimaryAccent, $hsvRect.X + 12.0, $hsvRect.Y + ($hsvH * 0.44))
$g.DrawString("Active Space Adoption Verified", $fCardSub, $bEmerald, $hsvRect.X + 12.0, $hsvRect.Y + ($hsvH * 0.72))
$hsvPath.Dispose()
$curY3 += $hsvH + 10.0

$hexRowH = [float]($H * 0.068)
$hexPath = New-RoundedRectPath ($slot3X + $pad3) $curY3 ($slot3W - 2 * $pad3) $hexRowH 12.0
$g.FillPath($bSurfaceElev, $hexPath)
$g.DrawPath($pBorderSubtle, $hexPath)
$g.DrawString("Biology Lab $bullet Excel Serial Fractions", $fCardBody, $bTextPrimary, $slot3X + $pad3 + 12.0, $curY3 + ($hexRowH * 0.26))
$hexPath.Dispose()
$curY3 += $hexRowH + 8.0

$syncPath = New-RoundedRectPath ($slot3X + $pad3) $curY3 ($slot3W - 2 * $pad3) $hexRowH 12.0
$g.FillPath($bSurfaceElev, $syncPath)
$g.DrawPath($pBorderSubtle, $syncPath)
$g.DrawString("Organic Chemistry $bullet Docx Table", $fCardBody, $bTextPrimary, $slot3X + $pad3 + 12.0, $curY3 + ($hexRowH * 0.26))
$g.FillEllipse($bPrimaryAccent, $slot3X + $slot3W - $pad3 - 26.0, $curY3 + ($hexRowH * 0.25), 16.0, 16.0)
$syncPath.Dispose()
$curY3 += $hexRowH + 8.0

$lockPath = New-RoundedRectPath ($slot3X + $pad3) $curY3 ($slot3W - 2 * $pad3) $hexRowH 12.0
$g.FillPath($bSurfaceElev, $lockPath)
$g.DrawPath($pBorderSubtle, $lockPath)
$g.DrawString("SQLite insertOrThrow Self-Heal", $fCardBody, $bTextPrimary, $slot3X + $pad3 + 12.0, $curY3 + ($hexRowH * 0.26))
$lockPath.Dispose()

$g.Restore($state3)
$g.DrawPath($pBorderSubtle, $pathSlot3)
$pathSlot3.Dispose()

# ==============================================================================
# 2C. Render Slot 2 (Center-Right Foreground Layer): Widget Space Picker & Folder Pin Right
# ==============================================================================
$pathSlot2 = New-TopRoundedRectPath $slot2X $slot2Y $slot2W $slot2H 20.0
$state2 = $g.Save()
$g.SetClip($pathSlot2)
$g.FillPath($bSurfacePanel, $pathSlot2)

$pad2 = [float]($slot2W * 0.075)
$curY2 = $slot2Y + 16.0

$g.DrawString("Widget Space", $fCardBody, $bTextPrimary, $slot2X + $pad2, $curY2)
$curY2 += [float]($W * 0.0085)
$g.DrawString("Per-Instance Config $bullet Vault Safe", $fCardSub, $bTextMuted, $slot2X + $pad2, $curY2)
$curY2 += [float]($W * 0.0105)

$cropSize = [float]($slot2W - 2 * $pad2)
$cropH = [float]($cropSize * 0.72)
$cropPath = New-RoundedRectPath ($slot2X + $pad2) $curY2 $cropSize $cropH 14.0
$g.FillPath($bBgCanvas, $cropPath)
$g.DrawPath($pAccentBorder, $cropPath)

$noteBadge = New-RoundedRectPath ($slot2X + $pad2 + 10.0) ($curY2 + 10.0) 86.0 24.0 12.0
$g.FillPath($bSurfaceElev, $noteBadge)
$g.DrawPath($pBorderSubtle, $noteBadge)
$g.DrawString("Math", $fCardLabel, $bTextPrimary, $slot2X + $pad2 + 22.0, $curY2 + 15.0)
$noteBadge.Dispose()

$lblBadge = New-RoundedRectPath ($slot2X + $pad2 + $cropSize - 98.0) ($curY2 + 10.0) 88.0 24.0 12.0
$g.FillPath($bPrimaryAccent, $lblBadge)
$g.DrawString("Pin Right", $fCardLabel, $bTextPrimary, $slot2X + $pad2 + $cropSize - 82.0, $curY2 + 15.0)
$lblBadge.Dispose()

$g.DrawString("Physics Space", $fCardBody, $bTextPrimary, $slot2X + $pad2 + 12.0, $curY2 + ($cropH * 0.46))
$g.DrawString("6 classes $bullet Right Pin Layout", $fCardSub, $bTextMuted, $slot2X + $pad2 + 12.0, $curY2 + ($cropH * 0.66))
$cropPath.Dispose()
$curY2 += $cropH + 10.0

$g.DrawString("APPWIDGET SPACE PICKER", $fCardLabel, $bPrimaryAccent, $slot2X + $pad2, $curY2)
$curY2 += [float]($W * 0.008)
$g.FillRectangle($bTrackInactive, $slot2X + $pad2, $curY2, $cropSize, 5.0)
$g.FillRectangle($bPrimaryAccent, $slot2X + $pad2, $curY2, [float]($cropSize * 0.78), 5.0)
$curY2 += 12.0

$chipW = [float](($cropSize - 8.0) / 3.0)
$chipH = [float]($H * 0.048)
$chips = @("Active", "Space 1", "Space 2")
for ($i = 0; $i -lt 3; $i++) {
    $cx = $slot2X + $pad2 + $i * ($chipW + 4.0)
    $cp = New-RoundedRectPath $cx $curY2 $chipW $chipH ($chipH / 2.0)
    $g.FillPath($(if ($i -eq 1) { $bAccentSoft } else { $bSurfaceElev }), $cp)
    $g.DrawPath($(if ($i -eq 1) { $pAccentBorder } else { $pBorderSubtle }), $cp)
    $g.DrawString($chips[$i], $fCardLabel, $bTextPrimary, $cx + 8.0, $curY2 + ($chipH * 0.24))
    $cp.Dispose()
}
$curY2 += $chipH + 10.0

$wsH = [float]($H * 0.062)
$wsPath = New-RoundedRectPath ($slot2X + $pad2) $curY2 $cropSize $wsH 12.0
$g.FillPath($bSurfaceElev, $wsPath)
$g.DrawPath($pBorderSubtle, $wsPath)
$g.DrawString("Widget Ready", $fCardLabel, $bTextPrimary, $slot2X + $pad2 + 8.0, $curY2 + ($wsH * 0.28))
$prevH = [float]($wsH * 0.64)
$prevPill = New-RoundedRectPath ($slot2X + $pad2 + $cropSize * 0.54) ($curY2 + $wsH * 0.18) ($cropSize * 0.42) $prevH ($prevH / 2.0)
$g.FillPath($bPrimaryAccent, $prevPill)
$g.DrawString("Apply", $fCardLabel, $bTextPrimary, $slot2X + $pad2 + $cropSize * 0.66, $curY2 + ($wsH * 0.28))
$prevPill.Dispose()
$wsPath.Dispose()

$g.Restore($state2)
$g.DrawPath($pAccentBorder, $pathSlot2)
$pathSlot2.Dispose()

# ==============================================================================
# 3. Left-Column ElmsSans Typography (Aligned to Pre-Baked Top-Left Logo X=188 on 2460x1000)
# ==============================================================================
$textLeftX = [float][Math]::Round($W * (188.0 / 2460.0))
$titleTopY = [float][Math]::Round($H * (352.0 / 1000.0))

$fontTitleSize = [float][Math]::Round($W * 0.0525)
$fontDescSize  = [float][Math]::Round($W * 0.0172)
$fTitle = [System.Drawing.Font]::new($elmsFamily, $fontTitleSize, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fDesc  = [System.Drawing.Font]::new($elmsFamily, $fontDescSize, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)

$sfTypo = [System.Drawing.StringFormat]::GenericTypographic
$sfTypo.FormatFlags = $sfTypo.FormatFlags -bor [System.Drawing.StringFormatFlags]::MeasureTrailingSpaces

$verSize = $g.MeasureString($Version, $fTitle, [System.Drawing.PointF]::new(0, 0), $sfTypo)

$bPureWhite = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
$g.DrawString($Version, $fTitle, $bPureWhite, $textLeftX, $titleTopY, $sfTypo)

# Signature Blue Gradient (#1E88E5 to #42A5F5) for Codename ("Hyperion")
$codeX = $textLeftX + $verSize.Width + [float]($W * 0.016)
$codeSize = $g.MeasureString($Codename, $fTitle, [System.Drawing.PointF]::new(0, 0), $sfTypo)
$codeRect = [System.Drawing.RectangleF]::new($codeX, $titleTopY, [Math]::Max($codeSize.Width, 40.0), [Math]::Max($codeSize.Height, 40.0))
$bCodeGrad = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    $codeRect,
    [System.Drawing.Color]::FromArgb(255, 30, 136, 229),  # #1E88E5
    [System.Drawing.Color]::FromArgb(255, 66, 165, 245),  # #42A5F5
    [System.Drawing.Drawing2D.LinearGradientMode]::Horizontal
)
$g.DrawString($Codename, $fTitle, $bCodeGrad, $codeX, $titleTopY, $sfTypo)

# Concise English Subtitle Description directly below 2.6.0
$descTopY = $titleTopY + $verSize.Height + [float]($H * 0.032)
$descMaxW = [float]($slot1X - $textLeftX - ($W * 0.035))
$descRect = [System.Drawing.RectangleF]::new($textLeftX, $descTopY, $descMaxW, [float]($H * 0.28))
$g.DrawString($Description, $fDesc, $bPureWhite, $descRect, $sfTypo)

# ==============================================================================
# 4. Verify Pre-Baked Top-Left Logos Were Never Modified
# ==============================================================================
for ($ly = 0; $ly -lt $logoGuardRect.Height; $ly += 4) {
    for ($lx = 0; $lx -lt $logoGuardRect.Width; $lx += 4) {
        $origPx = $logoGuardSnapshot.GetPixel($lx, $ly)
        $currPx = $canvas.GetPixel($logoGuardRect.X + $lx, $logoGuardRect.Y + $ly)
        if ($origPx.ToArgb() -ne $currPx.ToArgb()) {
            throw "Logo protection rule violated at ($($logoGuardRect.X + $lx), $($logoGuardRect.Y + $ly))"
        }
    }
}

$outDir = Split-Path -Parent $OutputPath
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
$canvas.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)

$bCodeGrad.Dispose()
$bPureWhite.Dispose()
$fTitle.Dispose()
$fDesc.Dispose()
$fCardHeader.Dispose()
$fCardSub.Dispose()
$fCardLabel.Dispose()
$fCardBody.Dispose()
$logoGuardSnapshot.Dispose()
$g.Dispose()
$canvas.Dispose()
$baseImg.Dispose()
$pfc.Dispose()

Write-Output "Generated v2.6+ Redesigned Banner: $OutputPath"
